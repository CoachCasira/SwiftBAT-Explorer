package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.ui.UiFactory;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import javax.swing.SwingUtilities;
import java.awt.Dimension;

/**
 * Contenitore riutilizzabile per una vista scientifica 3D a barre.
 * La profondità separa le serie e non rappresenta una distanza.
 */
public final class ScientificBar3DPane extends BorderPane {
    private final SwingNode swingNode = new SwingNode();
    private final Java2DGroupedBarPanel renderer = new Java2DGroupedBarPanel();
    private final Java2DGroupedBarPanel.Dataset dataset;
    private final String footerNote;

    public ScientificBar3DPane(String title,
                               String explanation,
                               Java2DGroupedBarPanel.Dataset dataset,
                               String footerNote) {
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
        viewer.setPrefHeight(650);
        viewer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        viewer.widthProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));
        viewer.heightProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));

        setCenter(viewer);
        setBottom(buildFooter());
        Platform.runLater(() -> syncRendererSize(viewer));
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
        int width = (int) Math.max(540, viewer.getWidth());
        int height = (int) Math.max(360, viewer.getHeight());
        SwingUtilities.invokeLater(() -> {
            Dimension dimension = new Dimension(width, height);
            renderer.setPreferredSize(dimension);
            renderer.setSize(dimension);
            renderer.revalidate();
            renderer.repaint();
        });
    }
}
