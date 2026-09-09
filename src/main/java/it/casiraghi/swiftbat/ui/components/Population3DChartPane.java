package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.service.CumulativeAnalysisService;
import it.casiraghi.swiftbat.ui.I18n;
import it.casiraghi.swiftbat.ui.UiFactory;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
import java.util.List;

/** Vista interattiva 3D degli stessi elementi statistici mostrati nel profilo 2D. */
public final class Population3DChartPane extends BorderPane {
    // Exact colors used by PopulationPage.profileSeriesStyle(...) in the 2D view.
    private static final Color SINGLE_COLOR = new Color(84, 215, 255);    // #54d7ff
    private static final Color MEDIAN_COLOR = new Color(255, 174, 74);    // #ffae4a
    private static final Color QUARTILE_COLOR = new Color(170, 120, 219); // #aa78db

    private final SwingNode swingNode = new SwingNode();
    private final Java2DWaterfallPanel renderer = new Java2DWaterfallPanel();
    private final CumulativeAnalysisService analysisService = new CumulativeAnalysisService();
    private final Label sampleLabel = UiFactory.label("Nessun campione", "three-d-context");

    public Population3DChartPane() {
        getStyleClass().add("three-d-panel");
        setMinHeight(560);
        setPrefHeight(700);
        SwingUtilities.invokeLater(() -> {
            renderer.setPresentation(new Java2DWaterfallPanel.Presentation(
                    "Nessuna curva normalizzata disponibile per la vista 3D.",
                    "Tempo dal trigger (s)", "Rate normalizzato", "Elementi del profilo 2D",
                    "Rate normalizzato", "", false, false, true, 10));
            swingNode.setContent(renderer);
        });

        StackPane viewer = new StackPane(swingNode);
        viewer.getStyleClass().add("three-d-viewer");
        viewer.setMinHeight(480);
        viewer.setPrefHeight(590);
        viewer.widthProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));
        viewer.heightProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));
        setTop(buildHeader());
        setCenter(viewer);
        setBottom(buildFooter());
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> SwingUtilities.invokeLater(renderer::repaint));
        Platform.runLater(() -> syncRendererSize(viewer));
    }

    public void setData(List<CumulativeAnalysisService.NormalizedCurve> curves,
                        CumulativeAnalysisService.PopulationProfile profile,
                        double halfWindowSeconds) {
        Java2DWaterfallPanel.Dataset dataset = toDataset(curves, profile, halfWindowSeconds);
        int curveCount = curves == null ? 0 : curves.size();
        if (dataset.isEmpty()) {
            I18n.setText(sampleLabel, "Nessun campione", "No sample");
        } else {
            I18n.setText(sampleLabel,
                    curveCount + " GRB · mediana + fascia centrale",
                    curveCount + " GRBs · median + central band");
        }
        SwingUtilities.invokeLater(() -> renderer.setDataset(dataset));
    }

    private Java2DWaterfallPanel.Dataset toDataset(List<CumulativeAnalysisService.NormalizedCurve> curves,
                                                    CumulativeAnalysisService.PopulationProfile profile,
                                                    double halfWindowSeconds) {
        if (curves == null || curves.isEmpty()) return Java2DWaterfallPanel.Dataset.empty();
        int start = (int) Math.ceil(-halfWindowSeconds);
        int end = (int) Math.floor(halfWindowSeconds);
        if (end < start) return Java2DWaterfallPanel.Dataset.empty();
        double[] times = new double[end - start + 1];
        for (int i = 0; i < times.length; i++) times[i] = start + i;

        boolean hasProfile = profile != null && !profile.median().isEmpty()
                && !profile.lowerQuartile().isEmpty() && !profile.upperQuartile().isEmpty();
        int offset = hasProfile ? 3 : 0;
        double[][] rates = new double[curves.size() + offset][times.length];
        String[] labels = new String[curves.size() + offset];
        Color[] colors = new Color[curves.size() + offset];

        if (hasProfile) {
            labels[0] = "Mediana";
            labels[1] = "25° percentile";
            labels[2] = "75° percentile";
            colors[0] = MEDIAN_COLOR;
            colors[1] = QUARTILE_COLOR;
            colors[2] = QUARTILE_COLOR;
            for (int i = 0; i < times.length; i++) {
                rates[0][i] = valueAt(profile.median(), times[i]);
                rates[1][i] = valueAt(profile.lowerQuartile(), times[i]);
                rates[2][i] = valueAt(profile.upperQuartile(), times[i]);
            }
        }
        for (int c = 0; c < curves.size(); c++) {
            int band = c + offset;
            CumulativeAnalysisService.NormalizedCurve curve = curves.get(c);
            labels[band] = curve.grbName();
            colors[band] = SINGLE_COLOR;
            for (int i = 0; i < times.length; i++) rates[band][i] = analysisService.sampleAt(curve, times[i]);
        }
        return new Java2DWaterfallPanel.Dataset(times, rates, labels, colors);
    }

    private double valueAt(List<CumulativeAnalysisService.Point> points, double time) {
        double bestDistance = Double.POSITIVE_INFINITY;
        double bestValue = Double.NaN;
        for (CumulativeAnalysisService.Point point : points) {
            if (!Double.isFinite(point.time()) || !Double.isFinite(point.value())) continue;
            double distance = Math.abs(point.time() - time);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestValue = point.value();
            }
        }
        return bestDistance <= 0.51 ? bestValue : Double.NaN;
    }

    private VBox buildHeader() {
        Label title = UiFactory.label("Profilo di popolazione 3D", "overlay-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleRow = new HBox(10, title, spacer, sampleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label explanation = UiFactory.wrappedLabel(
                "La vista 3D riproduce gli stessi elementi del grafico 2D: singoli GRB in azzurro, mediana in arancio e limiti 25°/75° in viola. "
                        + "La profondità serve solo a separare visivamente le curve e non rappresenta T90, distanza o posizione nello spazio.",
                "overlay-caption");
        FlowPane legend = new FlowPane(14, 6,
                legendItem("— Singoli GRB", SINGLE_COLOR),
                legendItem("— Mediana", MEDIAN_COLOR),
                legendItem("- - Fascia centrale 25°–75°", QUARTILE_COLOR));
        VBox header = new VBox(8, titleRow, explanation, legend);
        header.getStyleClass().add("three-d-header");
        header.setPadding(new Insets(15, 17, 13, 17));
        return header;
    }

    private HBox buildFooter() {
        Label note = UiFactory.label(
                "Trascina per ruotare · rotella per zoom · doppio clic per centrare. La profondità è puramente grafica.",
                "subtle-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button reset = UiFactory.button("Centra vista", "secondary-button");
        reset.setOnAction(event -> SwingUtilities.invokeLater(renderer::resetView));
        HBox footer = new HBox(12, note, spacer, reset);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("three-d-footer");
        return footer;
    }

    private Label legendItem(String text, Color color) {
        Label label = UiFactory.label(text, "legend-item");
        label.setStyle("-fx-text-fill: " + toHex(color) + ";");
        return label;
    }

    private void syncRendererSize(StackPane viewer) {
        int width = (int) Math.max(680, viewer.getWidth());
        int height = (int) Math.max(440, viewer.getHeight());
        SwingUtilities.invokeLater(() -> {
            Dimension dimension = new Dimension(width, height);
            renderer.setPreferredSize(dimension);
            renderer.setSize(dimension);
            renderer.revalidate();
            renderer.repaint();
        });
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
