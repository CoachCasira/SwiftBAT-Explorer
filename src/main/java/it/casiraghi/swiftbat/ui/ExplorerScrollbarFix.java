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
    private static final String STABLE_CLASS = "explorer-stable-scrollbar";

    private static final double BAR_THICKNESS = 16.0;
    private static final double THUMB_THICKNESS = 11.0;
    private static final double MIN_THUMB_LENGTH = 64.0;

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

    /** Coalesce first-layout requests; never force a whole-subtree CSS pass. */
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
            // No root.applyCss(): skins notify us when their internal scrollbars
            // materialise. Forcing CSS here made Explorer re-style large subtrees.
            Set<Node> bars = root.lookupAll(".scroll-bar");
            for (Node node : bars) {
                if (!(node instanceof ScrollBar bar)) continue;
                installStableSkin(bar);
                styleBar(bar);
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
            // Leave native skin in place if JavaFX is currently rebuilding it.
        }
    }

    private static void styleBar(ScrollBar bar) {
        if (!bar.getStyleClass().contains(STABLE_CLASS)) bar.getStyleClass().add(STABLE_CLASS);
        if (bar.getOrientation() == Orientation.VERTICAL) {
            if (bar.getPrefWidth() != BAR_THICKNESS) {
                bar.setMinWidth(BAR_THICKNESS);
                bar.setPrefWidth(BAR_THICKNESS);
                bar.setMaxWidth(BAR_THICKNESS);
            }
        } else if (bar.getPrefHeight() != BAR_THICKNESS) {
            bar.setMinHeight(BAR_THICKNESS);
            bar.setPrefHeight(BAR_THICKNESS);
            bar.setMaxHeight(BAR_THICKNESS);
        }
    }

    private static final class StableScrollBarSkin extends ScrollBarSkin {
        private Region thumb;
        private Region track;

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
            resolveParts(bar);
            if (thumb == null || track == null) return;

            Bounds trackBounds = track.getBoundsInParent();
            if (trackBounds == null) return;
            double range = bar.getMax() - bar.getMin();
            double ratio = range <= 0.0 ? 0.0 : (bar.getValue() - bar.getMin()) / range;
            ratio = Math.max(0.0, Math.min(1.0, ratio));

            if (bar.getOrientation() == Orientation.VERTICAL) {
                double trackLength = Math.max(0.0, trackBounds.getHeight());
                if (trackLength <= 0.0) return;
                double length = Math.min(trackLength, Math.max(MIN_THUMB_LENGTH, thumb.getHeight()));
                double thickness = Math.min(THUMB_THICKNESS, Math.max(5.0, trackBounds.getWidth() - 2.0));
                double px = trackBounds.getMinX() + (trackBounds.getWidth() - thickness) / 2.0;
                double py = trackBounds.getMinY() + ratio * Math.max(0.0, trackLength - length);
                thumb.resizeRelocate(px, py, thickness, length);
            } else {
                double trackLength = Math.max(0.0, trackBounds.getWidth());
                if (trackLength <= 0.0) return;
                double length = Math.min(trackLength, Math.max(MIN_THUMB_LENGTH, thumb.getWidth()));
                double thickness = Math.min(THUMB_THICKNESS, Math.max(5.0, trackBounds.getHeight() - 2.0));
                double px = trackBounds.getMinX() + ratio * Math.max(0.0, trackLength - length);
                double py = trackBounds.getMinY() + (trackBounds.getHeight() - thickness) / 2.0;
                thumb.resizeRelocate(px, py, length, thickness);
            }
        }

        private void resolveParts(ScrollBar bar) {
            if (thumb != null && thumb.getParent() != null && track != null && track.getParent() != null) return;
            Node thumbNode = bar.lookup(".thumb");
            Node trackNode = bar.lookup(".track");
            thumb = thumbNode instanceof Region region ? region : null;
            track = trackNode instanceof Region region ? region : null;
        }
    }
}
