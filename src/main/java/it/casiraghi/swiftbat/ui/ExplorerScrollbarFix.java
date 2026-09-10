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

/**
 * Stable Explorer scrollbar geometry for macOS and Windows.
 *
 * <p>The Explorer has two different scrollbar sources: ordinary ScrollPane
 * controls in the detail/dashboard area and VirtualFlow scrollbars generated
 * lazily by ListView/TableView (notably the GRB catalog on the left). On macOS
 * those VirtualFlow bars do not exist when the page is first constructed, so a
 * one-shot lookup misses them and the thumb remains the native tiny dot. This
 * class listens to the owning control's skin/scene lifecycle and installs the
 * same stable scrollbar skin as soon as the internal bar is created.</p>
 */
public final class ExplorerScrollbarFix {
    private static final String INSTALLED = ExplorerScrollbarFix.class.getName() + ".installed";
    private static final String VIRTUAL_INSTALLED = ExplorerScrollbarFix.class.getName() + ".virtualInstalled";
    private static final String BAR_INSTALLED = ExplorerScrollbarFix.class.getName() + ".barInstalled";

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

        // ListView/TableView create their vertical bars only when their skin and
        // VirtualFlow are materialised. Hook those lifecycle points explicitly so
        // the Explorer is correct on the very first opening, not only after a tab
        // switch or a later layout pass.
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
            scroll.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> schedule(scroll));
            scroll.contentProperty().addListener((obs, oldContent, newContent) -> {
                if (newContent != null) installRecursively(newContent);
                schedule(scroll);
            });
        }
        schedule(scroll);
    }

    private static void installVirtualControl(Parent control) {
        if (Boolean.TRUE.equals(control.getProperties().get(VIRTUAL_INSTALLED))) {
            schedule(control);
            return;
        }
        control.getProperties().put(VIRTUAL_INSTALLED, Boolean.TRUE);

        if (control instanceof ListView<?> list) {
            list.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule(list));
            list.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) schedule(list);
            });
            list.itemsProperty().addListener((obs, oldItems, newItems) -> schedule(list));
            list.heightProperty().addListener((obs, oldValue, newValue) -> schedule(list));
        } else if (control instanceof TableView<?> table) {
            table.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule(table));
            table.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) schedule(table);
            });
            table.itemsProperty().addListener((obs, oldItems, newItems) -> schedule(table));
            table.heightProperty().addListener((obs, oldValue, newValue) -> schedule(table));
        }

        schedule(control);
    }

    private static void schedule(Parent root) {
        if (root == null) return;
        // Three pulses intentionally cover: attach to Scene -> CSS -> VirtualFlow
        // creation. No timer and no global observer are left running afterwards.
        Platform.runLater(() -> {
            fixNow(root);
            Platform.runLater(() -> {
                fixNow(root);
                Platform.runLater(() -> fixNow(root));
            });
        });
    }

    private static void fixNow(Parent root) {
        try {
            root.applyCss();
            root.layout();
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
        if (bar == null) return;
        if (!(bar.getSkin() instanceof StableScrollBarSkin)) {
            try {
                bar.setSkin(new StableScrollBarSkin(bar));
            } catch (RuntimeException ignored) {
                return;
            }
        }
        if (!Boolean.TRUE.equals(bar.getProperties().get(BAR_INSTALLED))) {
            bar.getProperties().put(BAR_INSTALLED, Boolean.TRUE);
            bar.valueProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.minProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.maxProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.visibleAmountProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.orientationProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.heightProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
            bar.widthProperty().addListener((obs, oldValue, newValue) -> bar.requestLayout());
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
                double thickness = Math.min(THUMB_THICKNESS, Math.max(5.0, trackBounds.getWidth() - 2.0));
                double px = trackBounds.getMinX() + (trackBounds.getWidth() - thickness) / 2.0;
                double py = trackBounds.getMinY() + ratio * Math.max(0.0, trackLength - length);
                thumb.resizeRelocate(px, py, thickness, length);
                thumb.setMinHeight(MIN_THUMB_LENGTH);
                thumb.setPrefHeight(length);
            } else {
                double trackLength = Math.max(0.0, trackBounds.getWidth());
                if (trackLength <= 0.0) return;

                double nativeLength = thumb.getWidth();
                double length = Math.min(trackLength, Math.max(MIN_THUMB_LENGTH, nativeLength));
                double thickness = Math.min(THUMB_THICKNESS, Math.max(5.0, trackBounds.getHeight() - 2.0));
                double px = trackBounds.getMinX() + ratio * Math.max(0.0, trackLength - length);
                double py = trackBounds.getMinY() + (trackBounds.getHeight() - thickness) / 2.0;
                thumb.resizeRelocate(px, py, length, thickness);
                thumb.setMinWidth(MIN_THUMB_LENGTH);
                thumb.setPrefWidth(length);
            }
        }
    }

    private static String appendStyle(String current, String extra) {
        String base = current == null ? "" : current.trim();
        return base.isEmpty() ? extra : base + (base.endsWith(";") ? " " : "; ") + extra;
    }
}
