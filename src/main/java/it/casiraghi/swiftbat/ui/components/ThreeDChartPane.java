package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.model.TabularData;
import it.casiraghi.swiftbat.ui.InPlaceFullscreen;
import it.casiraghi.swiftbat.ui.UiFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contenitore JavaFX per il renderer scientifico Java2D tempo-energia.
 *
 * <p>Il grafico viene disegnato con Java2D dentro uno SwingNode. La vista normale
 * e quella a schermo intero usano lo stesso dataset e le stesse interazioni.
 * Lo schermo intero sostituisce temporaneamente il contenuto della finestra
 * principale, senza creare finestre secondarie.</p>
 */
public final class ThreeDChartPane extends BorderPane {
    private static final List<Band> BANDS = List.of(
            new Band("15–25 keV", "RATE_15_25_KEV", new Color(82, 216, 255)),
            new Band("25–50 keV", "RATE_25_50_KEV", new Color(110, 231, 183)),
            new Band("50–100 keV", "RATE_50_100_KEV", new Color(167, 139, 250)),
            new Band("100–350 keV", "RATE_100_350_KEV", new Color(251, 113, 133)));

    private static final Map<String, Double> WINDOWS = createWindows();
    private static final String DEFAULT_WINDOW = "±60 s dal trigger";

    private final ChoiceBox<String> windowChoice = new ChoiceBox<>(
            FXCollections.observableArrayList(WINDOWS.keySet()));
    private final SwingNode swingNode = new SwingNode();
    private final Java2DWaterfallPanel renderer = new Java2DWaterfallPanel();
    private final Label contextLabel = UiFactory.label("GRB", "three-d-context");
    private final boolean allowFullscreen;

    private TabularData sourceData = TabularData.empty();
    private String contextName = "GRB";

    public ThreeDChartPane() {
        this(true);
    }

    private ThreeDChartPane(boolean allowFullscreen) {
        this.allowFullscreen = allowFullscreen;
        getStyleClass().add("three-d-panel");
        setMinHeight(430);
        setPrefHeight(500);

        windowChoice.getStyleClass().add("choice-box-modern");
        windowChoice.setValue(DEFAULT_WINDOW);
        windowChoice.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> rebuildDataset());

        SwingUtilities.invokeLater(() -> swingNode.setContent(renderer));

        StackPane viewer = new StackPane(swingNode);
        viewer.getStyleClass().add("three-d-viewer");
        viewer.setMinHeight(330);
        viewer.setPrefHeight(390);
        viewer.widthProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));
        viewer.heightProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));

        setTop(buildHeader());
        setCenter(viewer);
        setBottom(buildFooter());
        BorderPane.setMargin(viewer, new Insets(0));

        widthProperty().addListener((observable, oldValue, newValue) -> repaintRenderer());
        heightProperty().addListener((observable, oldValue, newValue) -> repaintRenderer());
        Platform.runLater(() -> syncRendererSize(viewer));
    }

    public void setData(TabularData data) {
        sourceData = data == null ? TabularData.empty() : data;
        rebuildDataset();
    }

    public void setContextName(String contextName) {
        this.contextName = contextName == null || contextName.isBlank() ? "GRB" : contextName;
        contextLabel.setText(this.contextName);
    }

    private void rebuildDataset() {
        Java2DWaterfallPanel.Dataset dataset = toDataset(sourceData,
                WINDOWS.getOrDefault(windowChoice.getValue(), 60.0));
        SwingUtilities.invokeLater(() -> renderer.setDataset(dataset));
    }

    private Java2DWaterfallPanel.Dataset toDataset(TabularData data, double selectedWindow) {
        if (data == null || data.isEmpty()) {
            return Java2DWaterfallPanel.Dataset.empty();
        }

        int timeIndex = data.indexOf("TIME_FROM_TRIGGER_CENTER_S");
        if (timeIndex < 0) {
            return Java2DWaterfallPanel.Dataset.empty();
        }

        int[] bandIndices = new int[BANDS.size()];
        for (int band = 0; band < BANDS.size(); band++) {
            bandIndices[band] = data.indexOf(BANDS.get(band).field());
            if (bandIndices[band] < 0) {
                return Java2DWaterfallPanel.Dataset.empty();
            }
        }

        List<Sample> eligible = new ArrayList<>();
        for (List<String> row : data.rows()) {
            if (timeIndex >= row.size()) {
                continue;
            }
            double time = parse(row.get(timeIndex));
            if (!Double.isFinite(time)) {
                continue;
            }
            if (!Double.isInfinite(selectedWindow) && Math.abs(time) > selectedWindow) {
                continue;
            }

            double[] rates = new double[BANDS.size()];
            boolean valid = true;
            for (int band = 0; band < BANDS.size(); band++) {
                int column = bandIndices[band];
                if (column >= row.size()) {
                    valid = false;
                    break;
                }
                rates[band] = parse(row.get(column));
                if (!Double.isFinite(rates[band])) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                eligible.add(new Sample(time, rates));
            }
        }

        if (eligible.isEmpty()) {
            return Java2DWaterfallPanel.Dataset.empty();
        }

        List<Sample> samples = sampleRows(eligible, 520);
        double[] times = new double[samples.size()];
        double[][] rates = new double[BANDS.size()][samples.size()];
        for (int index = 0; index < samples.size(); index++) {
            Sample sample = samples.get(index);
            times[index] = sample.time();
            for (int band = 0; band < BANDS.size(); band++) {
                rates[band][index] = sample.rates()[band];
            }
        }

        String[] labels = BANDS.stream().map(Band::label).toArray(String[]::new);
        Color[] colors = BANDS.stream().map(Band::color).toArray(Color[]::new);
        return new Java2DWaterfallPanel.Dataset(times, rates, labels, colors);
    }

    private List<Sample> sampleRows(List<Sample> rows, int maximumSamples) {
        if (rows.size() <= maximumSamples) {
            return rows;
        }
        List<Sample> sampled = new ArrayList<>(maximumSamples);
        double step = (rows.size() - 1.0) / (maximumSamples - 1.0);
        for (int index = 0; index < maximumSamples; index++) {
            sampled.add(rows.get((int) Math.round(index * step)));
        }
        return sampled;
    }

    private VBox buildHeader() {
        VBox header = new VBox(8);
        header.getStyleClass().add("three-d-header");
        header.setPadding(new Insets(10, 14, 9, 14));

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = UiFactory.label("Paesaggio tempo–energia", "overlay-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(title, spacer, contextLabel);

        Label text = UiFactory.wrappedLabel(
                "Le quattro bande sono separate in profondità solo per renderle confrontabili. Trascina per cambiare prospettiva, usa la rotella per lo zoom e passa sui dati per leggere tempo e rate.",
                "overlay-caption");
        text.setMaxWidth(Double.MAX_VALUE);

        FlowPane legend = new FlowPane(12, 5);
        for (Band band : BANDS) {
            Label item = new Label("● " + band.label());
            item.setStyle("-fx-text-fill: " + toHex(band.color()) + ";");
            item.getStyleClass().add("legend-item");
            legend.getChildren().add(item);
        }
        header.getChildren().addAll(titleRow, text, legend);
        return header;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.getStyleClass().add("three-d-footer");
        footer.setMinHeight(50);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label note = UiFactory.label(
                "Profondità = banda energetica, non posizione spaziale.",
                "subtle-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label windowLabel = UiFactory.label("Finestra", "toolbar-label");
        Button reset = UiFactory.button("Centra vista", "secondary-button");
        reset.setOnAction(event -> SwingUtilities.invokeLater(renderer::resetView));

        footer.getChildren().addAll(note, spacer, windowLabel, windowChoice, reset);
        if (allowFullscreen) {
            Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");
            fullscreen.setOnAction(event -> openFullscreen());
            footer.getChildren().add(fullscreen);
        }
        return footer;
    }

    private void openFullscreen() {
        if (getScene() == null) {
            return;
        }
        ThreeDChartPane enlarged = new ThreeDChartPane(false);
        enlarged.setContextName(contextName);
        enlarged.windowChoice.setValue(windowChoice.getValue());
        enlarged.setData(sourceData);

        InPlaceFullscreen.show(this, "Vista 3D · " + contextName, enlarged);
    }

    private void syncRendererSize(StackPane viewer) {
        int width = (int) Math.max(620, viewer.getWidth());
        int height = (int) Math.max(430, viewer.getHeight());
        SwingUtilities.invokeLater(() -> {
            Dimension dimension = new Dimension(width, height);
            renderer.setPreferredSize(dimension);
            renderer.setSize(dimension);
            renderer.revalidate();
            renderer.repaint();
        });
    }

    private void repaintRenderer() {
        Platform.runLater(() -> SwingUtilities.invokeLater(renderer::repaint));
    }

    private double parse(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return Double.NaN;
        }
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Map<String, Double> createWindows() {
        Map<String, Double> windows = new LinkedHashMap<>();
        windows.put("±20 s dal trigger", 20.0);
        windows.put("±60 s dal trigger", 60.0);
        windows.put("±120 s dal trigger", 120.0);
        windows.put("Intera osservazione", Double.POSITIVE_INFINITY);
        return windows;
    }

    private record Band(String label, String field, Color color) { }
    private record Sample(double time, double[] rates) { }
}
