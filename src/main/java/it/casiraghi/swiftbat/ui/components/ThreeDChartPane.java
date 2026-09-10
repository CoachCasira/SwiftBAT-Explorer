package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.model.TabularData;
import it.casiraghi.swiftbat.ui.ExplorerBandSelectionEnhancer;
import it.casiraghi.swiftbat.ui.I18n;
import it.casiraghi.swiftbat.ui.InPlaceFullscreen;
import it.casiraghi.swiftbat.ui.UiFactory;
import it.casiraghi.swiftbat.ui.UiTranslations;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contenitore JavaFX per il renderer scientifico Java2D tempo-energia.
 */
public final class ThreeDChartPane extends BorderPane {
    /** Exact series palette of the Explorer 2D four-band chart. */
    private static final List<Band> BANDS = List.of(
            new Band("15–25 keV", "RATE_15_25_KEV", new Color(91, 220, 255)),
            new Band("25–50 keV", "RATE_25_50_KEV", new Color(110, 231, 183)),
            new Band("50–100 keV", "RATE_50_100_KEV", new Color(167, 139, 250)),
            new Band("100–350 keV", "RATE_100_350_KEV", new Color(251, 113, 133)));

    private static final Map<String, Double> WINDOWS = createWindows();
    private static final String DEFAULT_WINDOW = "±60 s dal trigger";

    private final ChoiceBox<String> windowChoice = new ChoiceBox<>(FXCollections.observableArrayList(WINDOWS.keySet()));
    private final SwingNode swingNode = new SwingNode();
    private final Java2DWaterfallPanel renderer = new Java2DWaterfallPanel();
    private final Label contextLabel = UiFactory.label("GRB", "three-d-context");
    private final Label zoomLabel = UiFactory.label("Zoom 100%", "three-d-zoom-inline");
    private final FlowPane legend = new FlowPane(12, 5);
    private final boolean allowFullscreen;
    private final StackPane viewer;

    private TabularData sourceData = TabularData.empty();
    private String contextName = "GRB";

    public ThreeDChartPane() {
        this(true);
    }

    public static ThreeDChartPane fullscreenView() {
        return new ThreeDChartPane(false);
    }

    private ThreeDChartPane(boolean allowFullscreen) {
        this.allowFullscreen = allowFullscreen;
        getStyleClass().add("three-d-panel");
        if (!allowFullscreen) getStyleClass().add("three-d-panel-fullscreen");
        setMinHeight(allowFullscreen ? 430 : 0);
        setPrefHeight(allowFullscreen ? 500 : 760);
        setMaxHeight(Double.MAX_VALUE);

        windowChoice.getStyleClass().add("choice-box-modern");
        UiTranslations.installChoiceBox(windowChoice);
        windowChoice.setValue(DEFAULT_WINDOW);
        windowChoice.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> rebuildDataset());

        SwingUtilities.invokeLater(() -> swingNode.setContent(renderer));

        zoomLabel.setMouseTransparent(true);
        renderer.setZoomListener(value -> Platform.runLater(
                () -> zoomLabel.setText("Zoom " + Math.round(value * 100.0) + "%")));

        viewer = new StackPane(swingNode);
        viewer.addEventHandler(ScrollEvent.SCROLL, event -> event.consume());
        viewer.getStyleClass().add("three-d-viewer");
        viewer.setMinHeight(allowFullscreen ? 330 : 0);
        viewer.setPrefHeight(allowFullscreen ? 390 : 650);
        viewer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        viewer.widthProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));
        viewer.heightProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));

        setTop(buildHeader());
        setCenter(viewer);
        setBottom(buildFooter());
        BorderPane.setMargin(viewer, new Insets(0));

        widthProperty().addListener((observable, oldValue, newValue) -> repaintRenderer());
        heightProperty().addListener((observable, oldValue, newValue) -> repaintRenderer());
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> repaintRenderer());
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

    /** Rebuilds this already-created 3D pane when the Explorer 2D band selector changes. */
    public void refreshBandSelection() {
        refreshLegend();
        rebuildDataset();
    }

    private void rebuildDataset() {
        Java2DWaterfallPanel.Dataset dataset = toDataset(sourceData,
                WINDOWS.getOrDefault(windowChoice.getValue(), 60.0));
        SwingUtilities.invokeLater(() -> renderer.setDataset(dataset));
    }

    private Java2DWaterfallPanel.Dataset toDataset(TabularData data, double selectedWindow) {
        if (data == null || data.isEmpty()) return Java2DWaterfallPanel.Dataset.empty();
        int timeIndex = data.indexOf("TIME_FROM_TRIGGER_CENTER_S");
        if (timeIndex < 0) return Java2DWaterfallPanel.Dataset.empty();

        Set<String> selectedLabels = ExplorerBandSelectionEnhancer.effectiveBandsFor3D();
        List<Band> activeBands = new ArrayList<>();
        for (Band band : BANDS) {
            if (selectedLabels.contains(band.label())) activeBands.add(band);
        }
        if (activeBands.isEmpty()) activeBands.addAll(BANDS);

        int[] bandIndices = new int[activeBands.size()];
        for (int band = 0; band < activeBands.size(); band++) {
            bandIndices[band] = data.indexOf(activeBands.get(band).field());
            if (bandIndices[band] < 0) return Java2DWaterfallPanel.Dataset.empty();
        }

        List<Sample> eligible = new ArrayList<>();
        for (List<String> row : data.rows()) {
            if (timeIndex >= row.size()) continue;
            double time = parse(row.get(timeIndex));
            if (!Double.isFinite(time)) continue;
            if (!Double.isInfinite(selectedWindow) && Math.abs(time) > selectedWindow) continue;

            double[] rates = new double[activeBands.size()];
            boolean valid = true;
            for (int band = 0; band < activeBands.size(); band++) {
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
            if (valid) eligible.add(new Sample(time, rates));
        }

        if (eligible.isEmpty()) return Java2DWaterfallPanel.Dataset.empty();

        List<Sample> samples = sampleRows(eligible, 520);
        double[] times = new double[samples.size()];
        double[][] rates = new double[activeBands.size()][samples.size()];
        for (int index = 0; index < samples.size(); index++) {
            Sample sample = samples.get(index);
            times[index] = sample.time();
            for (int band = 0; band < activeBands.size(); band++) rates[band][index] = sample.rates()[band];
        }

        String[] labels = activeBands.stream().map(Band::label).toArray(String[]::new);
        Color[] colors = activeBands.stream().map(Band::color).toArray(Color[]::new);
        return new Java2DWaterfallPanel.Dataset(times, rates, labels, colors);
    }

    private List<Sample> sampleRows(List<Sample> rows, int maximumSamples) {
        if (rows.size() <= maximumSamples) return rows;
        List<Sample> sampled = new ArrayList<>(maximumSamples);
        double step = (rows.size() - 1.0) / (maximumSamples - 1.0);
        for (int index = 0; index < maximumSamples; index++) sampled.add(rows.get((int) Math.round(index * step)));
        return sampled;
    }

    private VBox buildHeader() {
        VBox header = new VBox(allowFullscreen ? 8 : 12);
        header.getStyleClass().add("three-d-header");
        if (!allowFullscreen) header.getStyleClass().add("three-d-header-fullscreen");
        header.setPadding(allowFullscreen ? new Insets(10, 14, 9, 14) : new Insets(18, 22, 16, 22));

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = UiFactory.label("Curve di luce 3D per banda energetica", "overlay-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(title, spacer, contextLabel);

        Label text = UiFactory.wrappedLabel(
                "Asse X = tempo dal trigger; asse Y = rate; profondità = bande energetiche attive nel grafico 2D. "
                        + "Ogni linea è una curva di luce a bin di 1 secondo: la vista non rappresenta "
                        + "una distanza nello spazio né uno spettro continuo. Trascina per ruotare e usa la rotella per lo zoom.",
                "overlay-caption");
        text.setMaxWidth(Double.MAX_VALUE);

        refreshLegend();
        header.getChildren().addAll(titleRow, text, legend);
        return header;
    }

    private void refreshLegend() {
        Set<String> selectedLabels = ExplorerBandSelectionEnhancer.effectiveBandsFor3D();
        legend.getChildren().clear();
        for (Band band : BANDS) {
            if (!selectedLabels.contains(band.label())) continue;
            Label item = new Label("● " + band.label());
            item.setStyle("-fx-text-fill: " + toHex(band.color()) + ";");
            item.getStyleClass().add("legend-item");
            legend.getChildren().add(item);
        }
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.getStyleClass().add("three-d-footer");
        footer.setMinHeight(50);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label note = UiFactory.label("Profondità = banda energetica ASCII; non distanza spaziale.", "subtle-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label windowLabel = UiFactory.label("Finestra", "toolbar-label");
        Button reset = UiFactory.button("Centra vista", "secondary-button");
        reset.setOnAction(event -> SwingUtilities.invokeLater(renderer::resetView));

        Button export = UiFactory.button("Esporta PNG", "ghost-button");
        export.setOnAction(event -> exportViewerPng());

        footer.getChildren().addAll(zoomLabel, note, spacer, windowLabel, windowChoice, reset, export);
        if (allowFullscreen) {
            Button fullscreen = UiFactory.button("Schermo intero", "primary-button");
            fullscreen.setOnAction(event -> openFullscreen());
            footer.getChildren().add(fullscreen);
        }
        return footer;
    }

    private void openFullscreen() {
        if (getScene() == null) return;
        ThreeDChartPane enlarged = new ThreeDChartPane(false);
        enlarged.setContextName(contextName);
        enlarged.windowChoice.setValue(windowChoice.getValue());
        enlarged.setData(sourceData);
        enlarged.setMinSize(0, 0);
        enlarged.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        InPlaceFullscreen.show(this, UiTranslations.t("Confronto 3D dei rate") + " · " + contextName, enlarged);
    }

    private void exportViewerPng() {
        if (getScene() == null || viewer.getWidth() <= 1 || viewer.getHeight() <= 1) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(UiTranslations.t("Esporta vista 3D"));
        chooser.setInitialFileName(contextName.replaceAll("[^A-Za-z0-9._-]", "_") + "_vista_3D.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(UiTranslations.t("Immagine PNG"), "*.png"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            WritableImage image = viewer.snapshot(new SnapshotParameters(), null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException | RuntimeException error) {
            new Alert(Alert.AlertType.ERROR,
                    UiTranslations.t("Esportazione PNG non riuscita:") + " " + error.getMessage()).showAndWait();
        }
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
