package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * Final owner for three Explorer regressions that depend on JavaFX skin timing.
 *
 * <p>The catalogue scrollbar keeps a large visual thumb but maps dragging from
 * the stable ListView, so replacing the internal ScrollBar/skin can no longer
 * fall back to the slow stock mapping. The catalogue caption is driven by the
 * live lists and immediately corrects stale loading text. Spectroscopy radio
 * filters expose their complete value through the same quick tooltip used by
 * the rest of the application.</p>
 */
public final class ExplorerUiRegressionFix {
    private static final String SCROLL_INSTALLED = ExplorerUiRegressionFix.class.getName() + ".scrollInstalled";
    private static final String SCROLL_SCHEDULED = ExplorerUiRegressionFix.class.getName() + ".scrollScheduled";
    private static final String CAPTION_INSTALLED = ExplorerUiRegressionFix.class.getName() + ".captionInstalled";
    private static final String TOOLTIP_BRIDGE = ExplorerUiRegressionFix.class.getName() + ".tooltipBridge";
    private static final String FIT_TOOLTIP = ExplorerUiRegressionFix.class.getName() + ".fitTooltip";
    private static final String UI_FACTORY_AUTO_TOOLTIP = UiFactory.class.getName() + ".autoTooltip";

    private static final double CATALOG_BAR_WIDTH = 16.0;
    private static final double CATALOG_THUMB_LENGTH = 112.0;

    private ExplorerUiRegressionFix() {
    }

    public static void install(MainView mainView) {
        if (mainView == null) return;
        Object value = readField(mainView, "explorerPage");
        if (!(value instanceof ExplorerPage explorer)) return;

        installCatalogCaption(explorer);
        installCatalogScrollbar(explorer);
        installSpectroscopyTooltips(explorer);
    }

    /* ---------------- Catalogue caption: no stale loading/flicker ---------------- */

    @SuppressWarnings("unchecked")
    private static void installCatalogCaption(ExplorerPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(CAPTION_INSTALLED))) return;

        Object captionValue = readField(page, "catalogCount");
        Object catalogValue = readField(page, "catalog");
        Object filteredValue = readField(page, "filteredCatalog");
        if (!(captionValue instanceof Label caption)
                || !(catalogValue instanceof ObservableList<?> rawCatalog)
                || !(filteredValue instanceof ObservableList<?> rawFiltered)) return;

        ObservableList<CatalogEntry> catalog = (ObservableList<CatalogEntry>) rawCatalog;
        ObservableList<CatalogEntry> filtered = (ObservableList<CatalogEntry>) rawFiltered;
        page.getProperties().put(CAPTION_INSTALLED, Boolean.TRUE);

        final boolean[] refreshing = {false};
        Runnable refresh = () -> {
            if (refreshing[0] || catalog.isEmpty()) return;
            refreshing[0] = true;
            try {
                int shown = filtered.size();
                I18n.setText(caption,
                        shown + " GRB visualizzati",
                        shown + " GRBs shown");
            } finally {
                refreshing[0] = false;
            }
        };

        catalog.addListener((ListChangeListener<CatalogEntry>) change -> refresh.run());
        filtered.addListener((ListChangeListener<CatalogEntry>) change -> refresh.run());
        I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> refresh.run());

        /*
         * Some late localization passes used to restore the constructor caption
         * ("Loading catalog...") for one pulse. Correct any non-authoritative text
         * synchronously once the catalogue is available, so it never reaches a
         * rendered frame and cannot flicker against the GRB count.
         */
        caption.textProperty().addListener((obs, oldText, newText) -> {
            if (refreshing[0] || catalog.isEmpty()) return;
            String expected = I18n.dynamic(
                    filtered.size() + " GRB visualizzati",
                    filtered.size() + " GRBs shown");
            if (!Objects.equals(expected, newText)) refresh.run();
        });

        if (!catalog.isEmpty()) refresh.run();
    }

    /* ---------------- Catalogue scrollbar: large thumb + native-speed drag ---------------- */

    @SuppressWarnings("unchecked")
    private static void installCatalogScrollbar(ExplorerPage page) {
        Object listValue = readField(page, "catalogList");
        if (!(listValue instanceof ListView<?> rawList)) return;
        ListView<CatalogEntry> list = (ListView<CatalogEntry>) rawList;
        if (Boolean.TRUE.equals(list.getProperties().get(SCROLL_INSTALLED))) return;
        list.getProperties().put(SCROLL_INSTALLED, Boolean.TRUE);

        Runnable schedule = () -> scheduleCatalogPolish(list);
        list.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule.run());
        list.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) schedule.run();
        });
        list.itemsProperty().addListener((obs, oldItems, newItems) -> {
            attachItemsListener(newItems, schedule);
            schedule.run();
        });
        list.heightProperty().addListener((obs, oldHeight, newHeight) -> schedule.run());
        list.widthProperty().addListener((obs, oldWidth, newWidth) -> schedule.run());
        list.visibleProperty().addListener((obs, oldVisible, visible) -> {
            if (visible) schedule.run();
        });
        attachItemsListener(list.getItems(), schedule);

        DragState drag = new DragState();

        /*
         * Handle the gesture at ListView level. This node is stable even when
         * VirtualFlow recreates its internal ScrollBar or thumb. Because this
         * filter is an ancestor of the live ScrollBar, consuming the gesture here
         * also prevents old/native thumb handlers from applying a second, slow
         * mapping on top of ours.
         */
        list.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            ScrollBar bar = verticalBar(list);
            Region thumb = liveRegion(bar, ".thumb");
            if (bar == null || thumb == null || !bar.isVisible()) return;
            Bounds thumbScene = thumb.localToScene(thumb.getBoundsInLocal());
            if (thumbScene == null || !containsScene(thumbScene, event.getSceneX(), event.getSceneY())) return;

            drag.dragging = true;
            drag.offset = event.getSceneY() - thumbScene.getMinY();
            event.consume();
        });

        list.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!drag.dragging || !event.isPrimaryButtonDown()) return;
            ScrollBar bar = verticalBar(list);
            Region thumb = liveRegion(bar, ".thumb");
            Region track = liveRegion(bar, ".track");
            if (bar == null || thumb == null || track == null) return;

            Bounds thumbScene = thumb.localToScene(thumb.getBoundsInLocal());
            Bounds trackScene = track.localToScene(track.getBoundsInLocal());
            if (thumbScene == null || trackScene == null) return;

            double available = Math.max(0.0, trackScene.getHeight() - thumbScene.getHeight());
            double thumbTop = event.getSceneY() - drag.offset;
            double rawRatio = available <= 0.0 ? 0.0
                    : (thumbTop - trackScene.getMinY()) / available;
            double ratio = Math.max(0.0, Math.min(1.0, rawRatio));
            double range = bar.getMax() - bar.getMin();
            bar.setValue(range <= 0.0 ? bar.getMin() : bar.getMin() + ratio * range);
            bar.requestLayout();
            event.consume();
        });

        list.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (!drag.dragging) return;
            drag.dragging = false;
            if (event.getButton() == MouseButton.PRIMARY) event.consume();
        });
        list.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            if (drag.dragging) event.consume();
        });

        schedule.run();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void attachItemsListener(ObservableList<?> items, Runnable schedule) {
        if (items == null || schedule == null) return;
        ((ObservableList) items).addListener((ListChangeListener) change -> schedule.run());
    }

    private static void scheduleCatalogPolish(ListView<?> list) {
        if (list == null || Boolean.TRUE.equals(list.getProperties().get(SCROLL_SCHEDULED))) return;
        list.getProperties().put(SCROLL_SCHEDULED, Boolean.TRUE);
        Platform.runLater(() -> {
            polishCatalogScrollbar(list);
            Platform.runLater(() -> {
                polishCatalogScrollbar(list);
                Platform.runLater(() -> {
                    polishCatalogScrollbar(list);
                    list.getProperties().remove(SCROLL_SCHEDULED);
                });
            });
        });
    }

    private static void polishCatalogScrollbar(ListView<?> list) {
        if (list == null || list.getScene() == null || list.getSkin() == null) return;
        try {
            ScrollBar bar = verticalBar(list);
            if (bar == null) return;

            bar.setMinWidth(CATALOG_BAR_WIDTH);
            bar.setPrefWidth(CATALOG_BAR_WIDTH);
            bar.setMaxWidth(CATALOG_BAR_WIDTH);

            Region thumb = liveRegion(bar, ".thumb");
            if (thumb == null) return;
            thumb.setMinHeight(CATALOG_THUMB_LENGTH);
            thumb.setPrefHeight(CATALOG_THUMB_LENGTH);
            if (thumb.getHeight() + 0.5 < CATALOG_THUMB_LENGTH) {
                thumb.resize(Math.max(1.0, thumb.getWidth()), CATALOG_THUMB_LENGTH);
            }
            bar.requestLayout();
        } catch (RuntimeException ignored) {
            // VirtualFlow may replace a skin between two JavaFX pulses.
        }
    }

    private static ScrollBar verticalBar(ListView<?> list) {
        if (list == null) return null;
        try {
            for (Node node : list.lookupAll(".scroll-bar")) {
                if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL) {
                    return bar;
                }
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static Region liveRegion(ScrollBar bar, String selector) {
        if (bar == null) return null;
        Node node = bar.lookup(selector);
        return node instanceof Region region ? region : null;
    }

    private static boolean containsScene(Bounds bounds, double x, double y) {
        return x >= bounds.getMinX() && x <= bounds.getMaxX()
                && y >= bounds.getMinY() && y <= bounds.getMaxY();
    }

    private static final class DragState {
        private boolean dragging;
        private double offset;
    }

    /* ---------------- Spectroscopy: complete labels on hover ---------------- */

    private static void installSpectroscopyTooltips(ExplorerPage page) {
        Object value = readField(page, "workspace");
        if (!(value instanceof StackPane workspace)) return;
        if (Boolean.TRUE.equals(workspace.getProperties().get(TOOLTIP_BRIDGE))) return;
        workspace.getProperties().put(TOOLTIP_BRIDGE, Boolean.TRUE);

        for (Node child : List.copyOf(workspace.getChildren())) visitForFitTooltips(child);
        workspace.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node child : List.copyOf(change.getAddedSubList())) visitForFitTooltips(child);
            }
        });
    }

    private static void visitForFitTooltips(Node node) {
        if (node == null) return;
        if (node instanceof RadioButton radio && insideSpectroscopy(radio)) {
            installFitTooltip(radio);
            return;
        }
        if (node instanceof ScrollPane scroll) {
            visitForFitTooltips(scroll.getContent());
            return;
        }
        if (node instanceof SplitPane split) {
            for (Node item : List.copyOf(split.getItems())) visitForFitTooltips(item);
            return;
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : List.copyOf(tabs.getTabs())) visitForFitTooltips(tab.getContent());
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) visitForFitTooltips(child);
        }
    }

    private static boolean insideSpectroscopy(Node node) {
        Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains("spectroscopy-pane")) return true;
            current = current.getParent();
        }
        return false;
    }

    private static void installFitTooltip(RadioButton radio) {
        if (radio == null || Boolean.TRUE.equals(radio.getProperties().get(FIT_TOOLTIP))) return;
        radio.getProperties().put(FIT_TOOLTIP, Boolean.TRUE);

        Object value = radio.getUserData();
        String source = value == null ? radio.getText() : value.toString();
        if (source == null || source.isBlank()) return;

        // Turn the auto-tooltip into an explicit one so later width/layout pulses
        // cannot remove it. quickTooltip keeps the source localized on IT/EN switch.
        radio.getProperties().remove(UI_FACTORY_AUTO_TOOLTIP);
        radio.setTooltip(UiFactory.quickTooltip(source));
    }

    /* ---------------- Reflection helper ---------------- */

    private static Object readField(Object target, String name) {
        if (target == null || name == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }
}
