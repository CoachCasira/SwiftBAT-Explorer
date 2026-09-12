package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Region;

import java.util.Set;

/** Keeps the two FRACEXP percentage sliders visible inside their own filter cards. */
public final class PopulationFracexpSliderFix {
    private static final String INSTALLED = PopulationFracexpSliderFix.class.getName() + ".installed";

    private PopulationFracexpSliderFix() { }

    public static void install(PopulationPage page) {
        if (page == null || Boolean.TRUE.equals(page.getProperties().get(INSTALLED))) return;
        page.getProperties().put(INSTALLED, Boolean.TRUE);

        restore(page);
        page.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) schedule(page);
        });
        schedule(page);
    }

    private static void schedule(PopulationPage page) {
        Platform.runLater(() -> {
            restore(page);
            Platform.runLater(() -> restore(page));
        });
    }

    private static void restore(Parent root) {
        if (root == null) return;
        Set<Node> nodes = root.lookupAll(".fracexp-slider");
        for (Node node : nodes) {
            if (!(node instanceof ScrollBar slider)) continue;

            slider.setManaged(true);
            slider.setVisible(true);
            slider.setOpacity(1.0);
            slider.setMouseTransparent(false);
            slider.setDisable(false);
            slider.setVisibleAmount(10.0);
            slider.setMinHeight(22.0);
            slider.setPrefHeight(22.0);
            slider.setMaxHeight(22.0);
            slider.setMinWidth(110.0);
            slider.setMaxWidth(Double.MAX_VALUE);

            try {
                slider.applyCss();
                Node trackNode = slider.lookup(".track");
                Node thumbNode = slider.lookup(".thumb");
                if (trackNode instanceof Region track) {
                    track.setOpacity(1.0);
                    track.setMinHeight(6.0);
                    track.setPrefHeight(6.0);
                    track.setMaxHeight(6.0);
                    track.setStyle("-fx-background-color: rgba(91, 173, 226, 0.30);"
                            + "-fx-background-radius: 999px;");
                }
                if (thumbNode instanceof Region thumb) {
                    thumb.setOpacity(1.0);
                    thumb.setMinWidth(18.0);
                    thumb.setPrefWidth(18.0);
                    thumb.setMinHeight(18.0);
                    thumb.setPrefHeight(18.0);
                    thumb.setStyle("-fx-background-color: linear-gradient(to bottom right, #24dcf4, #c331e8);"
                            + "-fx-background-radius: 999px;"
                            + "-fx-effect: dropshadow(gaussian, rgba(61, 210, 245, 0.38), 8, 0.25, 0, 0);");
                }
                slider.requestLayout();
            } catch (RuntimeException ignored) {
                // The skin can be replaced briefly while the Population page is attached.
            }
        }
    }
}
