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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.Set;

/** Stable Explorer scrollbar geometry and direct thumb dragging for macOS/Windows. */
public final class ExplorerScrollbarFix {
    private static final String INSTALLED = ExplorerScrollbarFix.class.getName() + ".installed";
    private static final String VIRTUAL_INSTALLED = ExplorerScrollbarFix.class.getName() + ".virtualInstalled";
    private static final String SCHEDULED = ExplorerScrollbarFix.class.getName() + ".scheduled";
    private static final String DIRECT_DRAG_INSTALLED = ExplorerScrollbarFix.class.getName() + ".directDragInstalled";
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
            Set<Node> bars = root.lookupAll(".scroll-bar");
            for (Node node : bars) {
                if (!(node instanceof ScrollBar bar)) continue;
                installStableSkin(bar);
                installBarLevelDirectDrag(bar);
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

    /**
     * Install the drag mapping on the ScrollBar itself, not on the current thumb node.
     * VirtualFlow can replace the thumb/skin while the Explorer list is rebuilt; the
     * old thumb handlers then disappear and macOS falls back to the stock mapping,
     * which is extremely slow with our enlarged visual thumb. This handler survives
     * skin replacement and resolves the live thumb/track on every gesture.
     */
    private static void installBarLevelDirectDrag(ScrollBar bar) {
        if (bar == null || Boolean.TRUE.equals(bar.getProperties().get(DIRECT_DRAG_INSTALLED))) return;
        bar.getProperties().put(DIRECT_DRAG_INSTALLED, Boolean.TRUE);

        final boolean[] dragging = {false};
        final double[] dragOffset = {0.0};

        bar.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            Region thumb = liveRegion(bar, ".thumb");
            if (thumb == null) return;
            Bounds thumbScene = thumb.localToScene(thumb.getBoundsInLocal());
            if (thumbScene == null || !containsScene(thumbScene, event.getSceneX(), event.getSceneY())) return;

            dragging[0] = true;
            dragOffset[0] = bar.getOrientation() == Orientation.VERTICAL
                    ? event.getSceneY() - thumbScene.getMinY()
                    : event.getSceneX() - thumbScene.getMinX();
            event.consume();
        });

        bar.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!dragging[0] || !event.isPrimaryButtonDown()) return;
            Region thumb = liveRegion(bar, ".thumb");
            Region track = liveRegion(bar, ".track");
            if (thumb == null || track == null) return;

            Bounds thumbScene = thumb.localToScene(thumb.getBoundsInLocal());
            Bounds trackScene = track.localToScene(track.getBoundsInLocal());
            if (thumbScene == null || trackScene == null) return;

            double start;
            double available;
            double pointer;
            if (bar.getOrientation() == Orientation.VERTICAL) {
                start = trackScene.getMinY();
                available = Math.max(0.0, trackScene.getHeight() - thumbScene.getHeight());
                pointer = event.getSceneY() - dragOffset[0];
            } else {
                start = trackScene.getMinX();
                available = Math.max(0.0, trackScene.getWidth() - thumbScene.getWidth());
                pointer = event.getSceneX() - dragOffset[0];
            }

            double rawRatio = available <= 0.0 ? 0.0 : (pointer - start) / available;
            double ratio = Math.max(0.0, Math.min(1.0, rawRatio));
            double range = bar.getMax() - bar.getMin();
            bar.setValue(range <= 0.0 ? bar.getMin() : bar.getMin() + ratio * range);
            bar.requestLayout();
            event.consume();
        });

        bar.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (!dragging[0]) return;
            dragging[0] = false;
            if (event.getButton() == MouseButton.PRIMARY) event.consume();
        });

        bar.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            if (dragging[0]) event.consume();
        });
    }

    private static Region liveRegion(ScrollBar bar, String selector) {
        Node node = bar.lookup(selector);
        return node instanceof Region region ? region : null;
    }

    private static boolean containsScene(Bounds bounds, double x, double y) {
        return x >= bounds.getMinX() && x <= bounds.getMaxX()
                && y >= bounds.getMinY() && y <= bounds.getMaxY();
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
        private Region dragThumb;
        private double dragOffset;

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
            ratio = clamp01(ratio);

            /* ScrollBarSkin also translates the thumb. Clear that translation and
               place the enlarged thumb once, inside the actual track bounds. */
            thumb.setTranslateX(0.0);
            thumb.setTranslateY(0.0);

            if (bar.getOrientation() == Orientation.VERTICAL) {
                double trackLength = Math.max(0.0, trackBounds.getHeight());
                if (trackLength <= 0.0) return;
                double length = Math.min(trackLength, Math.max(MIN_THUMB_LENGTH, thumb.getHeight()));
                double thickness = Math.min(THUMB_THICKNESS, Math.max(5.0, trackBounds.getWidth() - 2.0));
                double minY = trackBounds.getMinY();
                double maxY = Math.max(minY, trackBounds.getMaxY() - length);
                double px = trackBounds.getMinX() + (trackBounds.getWidth() - thickness) / 2.0;
                double py = minY + ratio * Math.max(0.0, maxY - minY);
                thumb.resizeRelocate(px, clamp(py, minY, maxY), thickness, length);
            } else {
                double trackLength = Math.max(0.0, trackBounds.getWidth());
                if (trackLength <= 0.0) return;
                double length = Math.min(trackLength, Math.max(MIN_THUMB_LENGTH, thumb.getWidth()));
                double thickness = Math.min(THUMB_THICKNESS, Math.max(5.0, trackBounds.getHeight() - 2.0));
                double minX = trackBounds.getMinX();
                double maxX = Math.max(minX, trackBounds.getMaxX() - length);
                double px = minX + ratio * Math.max(0.0, maxX - minX);
                double py = trackBounds.getMinY() + (trackBounds.getHeight() - thickness) / 2.0;
                thumb.resizeRelocate(clamp(px, minX, maxX), py, length, thickness);
            }
        }

        private void resolveParts(ScrollBar bar) {
            if (thumb != null && thumb.getParent() != null && track != null && track.getParent() != null) {
                installDirectDragHandlers(bar);
                return;
            }
            Node thumbNode = bar.lookup(".thumb");
            Node trackNode = bar.lookup(".track");
            thumb = thumbNode instanceof Region region ? region : null;
            track = trackNode instanceof Region region ? region : null;
            installDirectDragHandlers(bar);
        }

        /**
         * Kept as a skin-local fallback for JavaFX builds that dispatch directly
         * to the thumb. The bar-level handler above is the authoritative mapping
         * and consumes the normal gesture before this fallback is reached.
         */
        private void installDirectDragHandlers(ScrollBar bar) {
            if (thumb == null || track == null || dragThumb == thumb) return;
            dragThumb = thumb;

            thumb.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (!event.isPrimaryButtonDown()) return;
                Bounds thumbScene = thumb.localToScene(thumb.getBoundsInLocal());
                if (thumbScene == null) return;
                dragOffset = bar.getOrientation() == Orientation.VERTICAL
                        ? event.getSceneY() - thumbScene.getMinY()
                        : event.getSceneX() - thumbScene.getMinX();
                event.consume();
            });

            thumb.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
                if (!event.isPrimaryButtonDown()) return;
                Bounds trackScene = track.localToScene(track.getBoundsInLocal());
                Bounds thumbScene = thumb.localToScene(thumb.getBoundsInLocal());
                if (trackScene == null || thumbScene == null) return;

                double start;
                double available;
                double pointer;
                if (bar.getOrientation() == Orientation.VERTICAL) {
                    start = trackScene.getMinY();
                    available = Math.max(0.0, trackScene.getHeight() - thumbScene.getHeight());
                    pointer = event.getSceneY() - dragOffset;
                } else {
                    start = trackScene.getMinX();
                    available = Math.max(0.0, trackScene.getWidth() - thumbScene.getWidth());
                    pointer = event.getSceneX() - dragOffset;
                }

                double ratio = available <= 0.0 ? 0.0 : clamp01((pointer - start) / available);
                double range = bar.getMax() - bar.getMin();
                bar.setValue(range <= 0.0 ? bar.getMin() : bar.getMin() + ratio * range);
                bar.requestLayout();
                event.consume();
            });

            thumb.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                if (event.getButton() == MouseButton.PRIMARY) event.consume();
            });
        }

        private static double clamp01(double value) {
            return clamp(value, 0.0, 1.0);
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
