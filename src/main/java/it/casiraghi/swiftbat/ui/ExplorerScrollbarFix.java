package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.ScrollBarSkin;
import javafx.scene.layout.Region;

import java.util.Set;

/** Stable Explorer scrollbar geometry for macOS and Windows. */
public final class ExplorerScrollbarFix {
    private static final String INSTALLED = ExplorerScrollbarFix.class.getName() + ".installed";
    private static final String VIRTUAL_INSTALLED = ExplorerScrollbarFix.class.getName() + ".virtualInstalled";
    private static final String SCHEDULED = ExplorerScrollbarFix.class.getName() + ".scheduled";
    private static final String THUMB_STYLED = ExplorerScrollbarFix.class.getName() + ".thumbStyled";
    private static final String TRACK_STYLED = ExplorerScrollbarFix.class.getName() + ".trackStyled";

    private static final double BAR_THICKNESS = 16.0;
    private static final double THUMB_THICKNESS = 11.0;
    private static final double MIN_THUMB_LENGTH = 64.0;

    private static final String THUMB_STYLE =
            "-fx-background-color: linear-gradient(to bottom, #ed22e9, #0ba7ff);"
                    + " -fx-background-radius: 8px;";
    private static final String TRACK_STYLE =
            "-fx-background-color: rgba(4,18,34,0.72); -fx-background-radius: 8px;";

    private ExplorerScrollbarFix() { }

    public static void install(Node root) {
        if (root == null) return;
        installRecursively(root);
        if (root instanceof Parent parent) schedule(parent);
    }

    private static void installRecursively(Node node) {
        if (node == null) return;

        if (node instanceof ScrollPane scroll) {
            installScrollPane(scroll);
            if (scroll.getContent() != null) installRecursively(scroll.getContent());
            return;
        }
        if (node instanceof ListView<?> list) {
            installVirtualControl(list);
            return;
        }
        if (node instanceof TableView<?> table) {
            installVirtualControl(table);
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) installRecursively(child);
        }
    }

    private static void installScrollPane(ScrollPane scroll) {
        if (!Boolean.TRUE.equals(scroll.getProperties().get(INSTALLED))) {
            scroll.getProperties().put(INSTALLED, Boolean.TRUE);
            scroll.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule(scroll));
            scroll.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) schedule(scroll);
            });
            scroll.contentProperty().addListener((obs, oldContent, newContent) -> {
                if (newContent != null) installRecursively(newContent);
                schedule(scroll);
            });
        }
        schedule(scroll);
    }

    private static void installVirtualControl(Parent control) {
        if (!Boolean.TRUE.equals(control.getProperties().get(VIRTUAL_INSTALLED))) {
            control.getProperties().put(VIRTUAL_INSTALLED, Boolean.TRUE);
            if (control instanceof ListView<?> list) {
                list.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule(list));
                list.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) schedule(list);
                });
                list.itemsProperty().addListener((obs, oldItems, newItems) -> schedule(list));
            } else if (control instanceof TableView<?> table) {
                table.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule(table));
                table.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) schedule(table);
                });
                table.itemsProperty().addListener((obs, oldItems, newItems) -> schedule(table));
            }
        }
        schedule(control);
    }

    /**
     * Coalesces all first-layout requests. The previous implementation appended
     * CSS to the thumb on every pulse; the inline style string therefore grew
     * indefinitely until JavaFX's CSS parser exhausted the heap.
     */
    private static void schedule(Parent root) {
        if (root == null || Boolean.TRUE.equals(root.getProperties().get(SCHEDULED))) return;
        root.getProperties().put(SCHEDULED, Boolean.TRUE);
        Platform.runLater(() -> {
            fixNow(root);
            Platform.runLater(() -> {
                fixNow(root);
                root.getProperties().remove(SCHEDULED);
            });
        });
    }

    private static void fixNow(Parent root) {
        try {
            if (root.getScene() == null) return;
            root.applyCss();
            Set<Node> bars = root.lookupAll(".scroll-bar");
            for (Node node : bars) {
                if (!(node instanceof ScrollBar bar)) continue;
                installStableSkin(bar);
                styleBar(bar);
                bar.requestLayout();
            }
        } catch (RuntimeException ignored) {
            // A control may briefly be between skins during a tab replacement.
        }
    }

    private static void installStableSkin(ScrollBar bar) {
        if (bar == null || bar.getSkin() instanceof StableScrollBarSkin) return;
        try {
            bar.setSkin(new StableScrollBarSkin(bar));
        } catch (RuntimeException ignored) {
            // Leave the native skin in place if JavaFX is currently rebuilding it.
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
            if (!Boolean.TRUE.equals(thumb.getProperties().get(THUMB_STYLED))) {
                thumb.getProperties().put(THUMB_STYLED, Boolean.TRUE);
                thumb.setStyle(THUMB_STYLE);
            }
        }
        for (Node part : bar.lookupAll(".track")) {
            if (!(part instanceof Region track)) continue;
            if (!Boolean.TRUE.equals(track.getProperties().get(TRACK_STYLED))) {
                track.getProperties().put(TRACK_STYLED, Boolean.TRUE);
                track.setStyle(TRACK_STYLE);
            }
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
                double thickness = Math.min(THUMB_THICKNESS, Math.max(5.0, trackBounds.getWidth() - 2.0));
                double px = trackBounds.getMinX() + (trackBounds.getWidth() - thickness) / 2.0;
                double py = trackBounds.getMinY() + ratio * Math.max(0.0, trackLength - length);
                thumb.resizeRelocate(px, py, thickness, length);
            } else {
                double trackLength = Math.max(0.0, trackBounds.getWidth());
                if (trackLength <= 0.0) return;
                double nativeLength = thumb.getWidth();
                double length = Math.min(trackLength, Math.max(MIN_THUMB_LENGTH, nativeLength));
                double thickness = Math.min(THUMB_THICKNESS, Math.max(5.0, trackBounds.getHeight() - 2.0));
                double px = trackBounds.getMinX() + ratio * Math.max(0.0, trackLength - length);
                double py = trackBounds.getMinY() + (trackBounds.getHeight() - thickness) / 2.0;
                thumb.resizeRelocate(px, py, length, thickness);
            }
        }
    }
}
