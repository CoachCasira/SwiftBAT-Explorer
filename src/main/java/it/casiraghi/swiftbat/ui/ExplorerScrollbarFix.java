package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ScrollBarSkin;
import javafx.scene.layout.Region;

import java.util.Set;

/**
 * Stable Explorer scrollbar geometry for macOS and Windows.
 *
 * <p>The stock JavaFX macOS skin can collapse the ScrollPane thumb to a tiny
 * dot on very long pages. CSS min-size values are not sufficient because the
 * skin resizes the thumb explicitly during layout. Explorer therefore installs
 * a small ScrollBarSkin subclass that performs the normal JavaFX layout first
 * and then enforces a real minimum thumb length while preserving the current
 * scroll position.</p>
 */
public final class ExplorerScrollbarFix {
    private static final String INSTALLED = ExplorerScrollbarFix.class.getName() + ".installed";
    private static final String BAR_INSTALLED = ExplorerScrollbarFix.class.getName() + ".barInstalled";

    private static final double BAR_THICKNESS = 14.0;
    private static final double THUMB_THICKNESS = 10.0;
    private static final double MIN_THUMB_LENGTH = 52.0;

    private ExplorerScrollbarFix() { }

    public static void install(Node root) {
        if (root == null) return;
        if (root instanceof Parent parent) schedule(parent);
        installOnScrollPanes(root);
    }

    private static void installOnScrollPanes(Node node) {
        if (node == null) return;
        if (node instanceof ScrollPane scroll) {
            if (!Boolean.TRUE.equals(scroll.getProperties().get(INSTALLED))) {
                scroll.getProperties().put(INSTALLED, Boolean.TRUE);
                scroll.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule(scroll));
                scroll.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> schedule(scroll));
                scroll.contentProperty().addListener((obs, oldContent, newContent) -> schedule(scroll));
            }
            schedule(scroll);
            if (scroll.getContent() != null) installOnScrollPanes(scroll.getContent());
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) installOnScrollPanes(child);
        }
    }

    private static void schedule(Parent root) {
        Platform.runLater(() -> {
            fixNow(root);
            Platform.runLater(() -> fixNow(root));
        });
    }

    private static void fixNow(Parent root) {
        try {
            root.applyCss();
            Set<Node> bars = root.lookupAll(".scroll-bar");
            for (Node node : bars) {
                if (!(node instanceof ScrollBar bar)) continue;
                installStableSkin(bar);
                styleBar(bar);
            }
        } catch (RuntimeException ignored) {
            // A ScrollPane may briefly be between skins during a tab replacement.
        }
    }

    private static void installStableSkin(ScrollBar bar) {
        if (bar == null) return;
        if (!(bar.getSkin() instanceof StableScrollBarSkin)) {
            try {
                bar.setSkin(new StableScrollBarSkin(bar));
            } catch (RuntimeException ignored) {
                // Keep the platform skin if replacement is temporarily unavailable.
            }
        }
        if (!Boolean.TRUE.equals(bar.getProperties().get(BAR_INSTALLED))) {
            bar.getProperties().put(BAR_INSTALLED, Boolean.TRUE);
            bar.valueProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.minProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.maxProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.visibleAmountProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.orientationProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
        }
    }

    private static void styleBar(ScrollBar bar) {
        if (bar.getOrientation() == Orientation.VERTICAL) {
            bar.setMinWidth(BAR_THICKNESS);
            bar.setPrefWidth(BAR_THICKNESS);
            bar.setMaxWidth(BAR_THICKNESS);
        } else {
            bar.setMinHeight(BAR_THICKNESS);
            bar.setPrefHeight(BAR_THICKNESS);
            bar.setMaxHeight(BAR_THICKNESS);
        }

        for (Node part : bar.lookupAll(".thumb")) {
            if (!(part instanceof Region thumb)) continue;
            thumb.setStyle(appendStyle(thumb.getStyle(),
                    "-fx-background-color: linear-gradient(to bottom, #ed22e9, #0ba7ff);"
                            + " -fx-background-radius: 8px;"));
        }
        for (Node part : bar.lookupAll(".track")) {
            if (!(part instanceof Region track)) continue;
            track.setStyle(appendStyle(track.getStyle(),
                    "-fx-background-color: rgba(4,18,34,0.72); -fx-background-radius: 8px;"));
        }
    }

    private static final class StableScrollBarSkin extends ScrollBarSkin {
        private StableScrollBarSkin(ScrollBar control) {
            super(control);
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            super.layoutChildren(x, y, w, h);
            enforceThumbGeometry();
        }

        private void enforceThumbGeometry() {
            ScrollBar bar = getSkinnable();
            if (bar == null) return;

            Node thumbNode = bar.lookup(".thumb");
            Node trackNode = bar.lookup(".track");
            if (!(thumbNode instanceof Region thumb) || !(trackNode instanceof Region track)) return;

            Bounds trackBounds = track.getBoundsInParent();
            if (trackBounds == null) return;

            double range = bar.getMax() - bar.getMin();
            double ratio = range <= 0.0 ? 0.0 : (bar.getValue() - bar.getMin()) / range;
            ratio = Math.max(0.0, Math.min(1.0, ratio));

            if (bar.getOrientation() == Orientation.VERTICAL) {
                double trackLength = Math.max(0.0, trackBounds.getHeight());
                if (trackLength <= 0.0) return;

                double nativeLength = thumb.getHeight();
                double length = Math.min(trackLength, Math.max(MIN_THUMB_LENGTH, nativeLength));
                double thickness = Math.min(THUMB_THICKNESS, Math.max(4.0, trackBounds.getWidth() - 2.0));
                double px = trackBounds.getMinX() + (trackBounds.getWidth() - thickness) / 2.0;
                double py = trackBounds.getMinY() + ratio * Math.max(0.0, trackLength - length);
                thumb.resizeRelocate(px, py, thickness, length);
                thumb.setMinHeight(MIN_THUMB_LENGTH);
            } else {
                double trackLength = Math.max(0.0, trackBounds.getWidth());
                if (trackLength <= 0.0) return;

                double nativeLength = thumb.getWidth();
                double length = Math.min(trackLength, Math.max(MIN_THUMB_LENGTH, nativeLength));
                double thickness = Math.min(THUMB_THICKNESS, Math.max(4.0, trackBounds.getHeight() - 2.0));
                double px = trackBounds.getMinX() + ratio * Math.max(0.0, trackLength - length);
                double py = trackBounds.getMinY() + (trackBounds.getHeight() - thickness) / 2.0;
                thumb.resizeRelocate(px, py, length, thickness);
                thumb.setMinWidth(MIN_THUMB_LENGTH);
            }
        }
    }

    private static String appendStyle(String current, String extra) {
        String base = current == null ? "" : current.trim();
        return base.isEmpty() ? extra : base + (base.endsWith(";") ? " " : "; ") + extra;
    }
}
