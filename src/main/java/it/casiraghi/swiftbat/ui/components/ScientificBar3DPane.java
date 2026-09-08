package it.casiraghi.swiftbat.ui.components;

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

/**
 * Contenitore riutilizzabile per una vista scientifica 3D a barre.
 * La profondità separa le serie e non rappresenta una distanza.
 */
public final class ScientificBar3DPane extends BorderPane {
    private final SwingNode swingNode = new SwingNode();
    private final Java2DGroupedBarPanel renderer = new Java2DGroupedBarPanel();
    private final String title;
    private final String explanation;
    private final String footerNote;
    private final Java2DGroupedBarPanel.Dataset dataset;

    public ScientificBar3DPane(String title,
                               String explanation,
                               Java2DGroupedBarPanel.Dataset dataset,
                               String footerNote) {
        this.title = title;
        this.explanation = explanation;
        this.footerNote = footerNote;
        this.dataset = dataset == null ? Java2DGroupedBarPanel.Dataset.empty() : dataset;

        getStyleClass().addAll("three-d-panel", "three-d-panel-fullscreen");
        setMinSize(0, 0);
        setPrefHeight(720);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        SwingUtilities.invokeLater(() -> {
            renderer.setDataset(this.dataset);
            swingNode.setContent(renderer);
        });

        StackPane viewer = new StackPane(swingNode);
        viewer.getStyleClass().add("three-d-viewer");
        viewer.setMinSize(0, 0);
        viewer.setPrefHeight(600);
        viewer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        viewer.widthProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));
        viewer.heightProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));

        setTop(buildHeader());
        setCenter(viewer);
        setBottom(buildFooter());
        Platform.runLater(() -> syncRendererSize(viewer));
    }

    private VBox buildHeader() {
        Label heading = UiFactory.label(title, "overlay-title");
        Label caption = UiFactory.wrappedLabel(explanation, "overlay-caption");
        caption.setMinHeight(Region.USE_PREF_SIZE);
        caption.setMaxWidth(Double.MAX_VALUE);

        FlowPane legend = new FlowPane(14, 6);
        for (int index = 0; index < dataset.groups().length; index++) {
            Label item = UiFactory.label("● " + dataset.groups()[index], "legend-item");
            Color color = dataset.colors()[index];
            item.setStyle("-fx-text-fill: " + toHex(color) + ";");
            legend.getChildren().add(item);
        }

        VBox header = new VBox(8, heading, caption, legend);
        header.getStyleClass().addAll("three-d-header", "three-d-header-fullscreen");
        header.setPadding(new Insets(15, 17, 13, 17));
        return header;
    }

    private HBox buildFooter() {
        Label note = UiFactory.wrappedLabel(footerNote, "subtle-text");
        note.setMinHeight(Region.USE_PREF_SIZE);
        note.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(note, Priority.ALWAYS);

        Button reset = UiFactory.button("Centra vista", "secondary-button");
        reset.setOnAction(event -> SwingUtilities.invokeLater(renderer::resetView));

        HBox footer = new HBox(12, note, reset);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("three-d-footer");
        return footer;
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
