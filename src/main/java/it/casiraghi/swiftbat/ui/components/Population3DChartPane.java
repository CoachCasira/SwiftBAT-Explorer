package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.service.CumulativeAnalysisService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Vista interattiva tempo–rate–GRB del campione di popolazione. */
public final class Population3DChartPane extends BorderPane {
    private static final Color SHORT_COLOR = new Color(255, 174, 74);
    private static final Color LONG_COLOR = new Color(82, 216, 255);
    private static final Color UNKNOWN_COLOR = new Color(145, 157, 179);

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
                    "Tempo dal trigger (s)", "Rate normalizzato", "GRB ordinati per T90",
                    "Rate normalizzato", "", false, false, true, 8));
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
        BorderPane.setMargin(viewer, Insets.EMPTY);
        Platform.runLater(() -> syncRendererSize(viewer));
    }

    public void setData(List<CumulativeAnalysisService.NormalizedCurve> curves,
                        Map<String, SkyBurst> metadata,
                        double halfWindowSeconds) {
        Java2DWaterfallPanel.Dataset dataset = toDataset(curves, metadata, halfWindowSeconds);
        sampleLabel.setText(dataset.isEmpty() ? "Nessun campione" : dataset.bandCount() + " GRB");
        SwingUtilities.invokeLater(() -> renderer.setDataset(dataset));
    }

    private Java2DWaterfallPanel.Dataset toDataset(
            List<CumulativeAnalysisService.NormalizedCurve> curves,
            Map<String, SkyBurst> metadata,
            double halfWindowSeconds) {
        if (curves == null || curves.isEmpty()) {
            return Java2DWaterfallPanel.Dataset.empty();
        }
        Map<String, SkyBurst> safeMetadata = metadata == null ? Map.of() : metadata;
        List<CumulativeAnalysisService.NormalizedCurve> ordered = new ArrayList<>(curves);
        ordered.sort(Comparator
                .comparingDouble((CumulativeAnalysisService.NormalizedCurve curve) ->
                        t90For(curve.grbName(), safeMetadata))
                .thenComparing(CumulativeAnalysisService.NormalizedCurve::grbName));

        int start = (int) Math.ceil(-halfWindowSeconds);
        int end = (int) Math.floor(halfWindowSeconds);
        if (end < start) {
            return Java2DWaterfallPanel.Dataset.empty();
        }
        double[] times = new double[end - start + 1];
        for (int index = 0; index < times.length; index++) {
            times[index] = start + index;
        }

        double[][] rates = new double[ordered.size()][times.length];
        String[] labels = new String[ordered.size()];
        Color[] colors = new Color[ordered.size()];
        for (int curveIndex = 0; curveIndex < ordered.size(); curveIndex++) {
            CumulativeAnalysisService.NormalizedCurve curve = ordered.get(curveIndex);
            SkyBurst burst = safeMetadata.get(curve.grbName().toUpperCase(Locale.ROOT));
            labels[curveIndex] = curve.grbName() + " · " + shortClass(burst);
            colors[curveIndex] = colorFor(burst);
            for (int timeIndex = 0; timeIndex < times.length; timeIndex++) {
                rates[curveIndex][timeIndex] = analysisService.sampleAt(curve, times[timeIndex]);
            }
        }
        return new Java2DWaterfallPanel.Dataset(times, rates, labels, colors);
    }

    private VBox buildHeader() {
        Label title = UiFactory.label("Profilo di popolazione 3D", "overlay-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleRow = new HBox(10, title, spacer, sampleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label explanation = UiFactory.wrappedLabel(
                "Asse X = tempo dal trigger; asse Y = rate normalizzato; profondità = singoli GRB, ordinati dal T90 più corto al più lungo. Ogni linea resta un evento distinto: i segnali non vengono sommati.",
                "overlay-caption");
        FlowPane legend = new FlowPane(14, 6,
                legendItem("● Short · T90 ≤ 2 s", SHORT_COLOR),
                legendItem("● Long · T90 > 2 s", LONG_COLOR),
                legendItem("● T90 non disponibile", UNKNOWN_COLOR));

        VBox header = new VBox(8, titleRow, explanation, legend);
        header.getStyleClass().add("three-d-header");
        header.setPadding(new Insets(15, 17, 13, 17));
        return header;
    }

    private HBox buildFooter() {
        Label note = UiFactory.label(
                "La profondità separa i GRB del campione: non rappresenta distanza cosmologica né posizione nello spazio.",
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

    private double t90For(String name, Map<String, SkyBurst> metadata) {
        SkyBurst burst = metadata.get(name.toUpperCase(Locale.ROOT));
        return burst == null || burst.t90Sec() == null ? Double.POSITIVE_INFINITY : burst.t90Sec();
    }

    private String shortClass(SkyBurst burst) {
        if (burst == null || !burst.hasT90()) return "T90 n.d.";
        return burst.isShort() ? "short" : "long";
    }

    private Color colorFor(SkyBurst burst) {
        if (burst == null || !burst.hasT90()) return UNKNOWN_COLOR;
        return burst.isShort() ? SHORT_COLOR : LONG_COLOR;
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
