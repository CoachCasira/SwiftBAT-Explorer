package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

/** Keeps the official-fit controls inside the visible area of nested Explorer scroll panes. */
final class StickySpectralControls {
    private static final PseudoClass STUCK = PseudoClass.getPseudoClass("stuck");
    private final Region controls;
    private final Region content;
    private final List<ScrollPane> scrolls = new ArrayList<>();
    private final InvalidationListener changed = obs -> update();
    private boolean updating;

    static void install(Region controls, Region content) {
        new StickySpectralControls(controls, content);
    }

    private StickySpectralControls(Region controls, Region content) {
        this.controls = controls;
        this.content = content;
        controls.setViewOrder(-1); // Paint above charts without changing VBox layout order.
        controls.sceneProperty().addListener((obs, before, after) -> rewire());
        // Observe the unshifted content with an eager change listener. An
        // invalidation-only listener on controls can stay invalid indefinitely.
        content.localToSceneTransformProperty().addListener((obs, before, after) -> update());
        controls.heightProperty().addListener(changed);
        content.heightProperty().addListener(changed);
        Platform.runLater(this::rewire);
    }

    private void rewire() {
        for (ScrollPane scroll : scrolls) {
            scroll.vvalueProperty().removeListener(changed);
            scroll.viewportBoundsProperty().removeListener(changed);
        }
        scrolls.clear();
        if (controls.getScene() != null) {
            for (Node node = content.getParent(); node != null; node = node.getParent()) {
                if (node instanceof ScrollPane scroll) {
                    scrolls.add(scroll);
                    scroll.vvalueProperty().addListener(changed);
                    scroll.viewportBoundsProperty().addListener(changed);
                }
            }
        }
        update();
    }

    private void update() {
        if (updating) return;
        updating = true;
        try {
            double offset = 0;
            if (controls.getScene() != null && !scrolls.isEmpty()) {
                double originalTop = content.localToScene(controls.getLayoutX(), controls.getLayoutY()).getY();
                double visibleTop = originalTop;
                for (ScrollPane scroll : scrolls) {
                    Node viewport = scroll.lookup(".viewport");
                    if (viewport == null) continue;
                    Bounds bounds = viewport.localToScene(viewport.getBoundsInLocal());
                    visibleTop = Math.max(visibleTop, bounds.getMinY());
                }
                double limit = Math.max(0, content.getHeight() - controls.getHeight() - controls.getLayoutY());
                offset = Math.max(0, Math.min(limit, visibleTop - originalTop));
            }
            controls.setTranslateY(offset);
            controls.pseudoClassStateChanged(STUCK, offset > 0.5);
        } finally {
            updating = false;
        }
    }
}
