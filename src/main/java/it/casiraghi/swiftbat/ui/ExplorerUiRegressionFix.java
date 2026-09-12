package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Chart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * Lightweight Explorer regression fixes.
 *
 * <p>This class deliberately does not resize JavaFX scrollbar internals or request
 * layout while Explorer is scrolling. The visual geometry remains owned by
 * {@link ExplorerScrollbarFix}; here we only add a stable, fast drag mapping at
 * ListView level. This avoids the layout feedback loop that made Explorer stutter.</p>
 */
public final class ExplorerUiRegressionFix {
    private static final String SCROLL_INSTALLED = ExplorerUiRegressionFix.class.getName() + ".scrollInstalled";
    private static final String CAPTION_INSTALLED = ExplorerUiRegressionFix.class.getName() + ".captionInstalled";
    private static final String CAPTION_REFRESH_SCHEDULED = ExplorerUiRegressionFix.class.getName() + ".captionRefreshScheduled";
    private static final String TOOLTIP_BRIDGE = ExplorerUiRegressionFix.class.getName() + ".tooltipBridge";
    private static final String FIT_TOOLTIP = ExplorerUiRegressionFix.class.getName() + ".fitTooltip";
    private static final String UI_FACTORY_AUTO_TOOLTIP = UiFactory.class.getName() + ".autoTooltip";

    private ExplorerUiRegressionFix() {
    }

    public static void install(MainView mainView) {
        if (mainView == null) return;
        Object value = readField(mainView, "explorerPage");
        if (!(value instanceof ExplorerPage explorer)) return;

        installSpectroscopyTooltips(explorer);
    }

    /* ---------------- Catalogue caption: authoritative and coalesced ---------------- */

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
            String expected = I18n.dynamic(
                    filtered.size() + " GRB visualizzati",
                    filtered.size() + " GRBs shown");
            if (Objects.equals(expected, caption.getText())) return;

            refreshing[0] = true;
            try {
                I18n.setText(caption,
                        filtered.size() + " GRB visualizzati",
                        filtered.size() + " GRBs shown");
            } finally {
                refreshing[0] = false;
            }
        };

        Runnable scheduleRefresh = () -> {
            if (catalog.isEmpty()
                    || Boolean.TRUE.equals(caption.getProperties().get(CAPTION_REFRESH_SCHEDULED))) return;
            caption.getProperties().put(CAPTION_REFRESH_SCHEDULED, Boolean.TRUE);
            Platform.runLater(() -> {
                caption.getProperties().remove(CAPTION_REFRESH_SCHEDULED);
                refresh.run();
            });
        };

        // The first non-empty catalogue transition is corrected immediately so the
        // loading caption cannot survive a rendered frame. Subsequent filter churn
        // is coalesced to one label update per JavaFX pulse.
        catalog.addListener((ListChangeListener<CatalogEntry>) change -> {
            if (!catalog.isEmpty()) refresh.run();
        });
        filtered.addListener((ListChangeListener<CatalogEntry>) change -> scheduleRefresh.run());
        I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> refresh.run());

        caption.textProperty().addListener((obs, oldText, newText) -> {
            if (refreshing[0] || catalog.isEmpty()) return;
            String expected = I18n.dynamic(
                    filtered.size() + " GRB visualizzati",
                    filtered.size() + " GRBs shown");
            if (!Objects.equals(expected, newText)) refresh.run();
        });

        if (!catalog.isEmpty()) refresh.run();
    }

    /* ---------------- Catalogue scrollbar: fast drag without layout churn ---------------- */

    @SuppressWarnings("unchecked")
    private static void installCatalogScrollbar(ExplorerPage page) {
        Object listValue = readField(page, "catalogList");
        if (!(listValue instanceof ListView<?> rawList)) return;
        ListView<CatalogEntry> list = (ListView<CatalogEntry>) rawList;
        if (Boolean.TRUE.equals(list.getProperties().get(SCROLL_INSTALLED))) return;
        list.getProperties().put(SCROLL_INSTALLED, Boolean.TRUE);

        // Geometry/style stays in one owner only. This call is idempotent and makes
        // first startup deterministic without touching the thumb from this class.
        ExplorerScrollbarFix.install(list);

        DragState drag = new DragState();

        list.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            ScrollBar bar = verticalBar(list);
            Region thumb = liveRegion(bar, ".thumb");
            Region track = liveRegion(bar, ".track");
            if (bar == null || thumb == null || track == null || !bar.isVisible()) return;

            Bounds thumbScene = thumb.localToScene(thumb.getBoundsInLocal());
            Bounds trackScene = track.localToScene(track.getBoundsInLocal());
            if (thumbScene == null || trackScene == null
                    || !containsScene(thumbScene, event.getSceneX(), event.getSceneY())) return;

            double available = Math.max(0.0, trackScene.getHeight() - thumbScene.getHeight());
            if (available <= 0.0) return;

            drag.dragging = true;
            drag.bar = bar;
            drag.trackStart = trackScene.getMinY();
            drag.available = available;
            drag.offset = event.getSceneY() - thumbScene.getMinY();
            event.consume();
        });

        list.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!drag.dragging || !event.isPrimaryButtonDown()) return;
            ScrollBar bar = drag.bar;
            if (bar == null || bar.getScene() == null) {
                drag.clear();
                return;
            }

            double thumbTop = event.getSceneY() - drag.offset;
            double rawRatio = (thumbTop - drag.trackStart) / drag.available;
            double ratio = Math.max(0.0, Math.min(1.0, rawRatio));
            double range = bar.getMax() - bar.getMin();
            double value = range <= 0.0 ? bar.getMin() : bar.getMin() + ratio * range;
            if (Math.abs(bar.getValue() - value) > 1e-9) bar.setValue(value);
            // Do not call requestLayout(): ScrollBar.value already invalidates only
            // the skin area that needs repainting. Forced layout on every mouse
            // pixel was the source of the Explorer-wide stutter.
            event.consume();
        });

        list.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (!drag.dragging) return;
            drag.clear();
            if (event.getButton() == MouseButton.PRIMARY) event.consume();
        });
        list.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            if (drag.dragging) event.consume();
        });
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
        private ScrollBar bar;
        private double offset;
        private double trackStart;
        private double available;

        private void clear() {
            dragging = false;
            bar = null;
            offset = 0.0;
            trackStart = 0.0;
            available = 0.0;
        }
    }

    /* ---------------- Spectroscopy: explicit tooltips, scoped traversal ---------------- */

    private static void installSpectroscopyTooltips(ExplorerPage page) {
        Object value = readField(page, "workspace");
        if (!(value instanceof StackPane workspace)) return;
        if (Boolean.TRUE.equals(workspace.getProperties().get(TOOLTIP_BRIDGE))) return;
        workspace.getProperties().put(TOOLTIP_BRIDGE, Boolean.TRUE);

        for (Node child : List.copyOf(workspace.getChildren())) inspectWorkspaceChild(child);
        workspace.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node child : List.copyOf(change.getAddedSubList())) inspectWorkspaceChild(child);
            }
        });
    }

    private static void inspectWorkspaceChild(Node node) {
        TabPane tabs = findExplorerTabPane(node);
        if (tabs == null) return;
        for (Tab tab : List.copyOf(tabs.getTabs())) {
            Node content = tab.getContent();
            if (content == null) continue;
            Node spectroscopy = findSpectroscopyRoot(content);
            if (spectroscopy != null) installFitTooltipsWithin(spectroscopy);
        }
    }

    private static TabPane findExplorerTabPane(Node node) {
        if (node == null) return null;
        if (node instanceof TabPane tabs) return tabs;
        if (node instanceof Chart || node instanceof ListView<?> || node instanceof TableView<?>) return null;
        if (node instanceof ScrollPane scroll) return findExplorerTabPane(scroll.getContent());
        if (node instanceof SplitPane split) {
            for (Node item : List.copyOf(split.getItems())) {
                TabPane found = findExplorerTabPane(item);
                if (found != null) return found;
            }
            return null;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                TabPane found = findExplorerTabPane(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Node findSpectroscopyRoot(Node node) {
        if (node == null) return null;
        if (node.getStyleClass().contains("spectroscopy-pane")) return node;
        if (node instanceof Chart || node instanceof ListView<?> || node instanceof TableView<?>) return null;
        if (node instanceof ScrollPane scroll) return findSpectroscopyRoot(scroll.getContent());
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                Node found = findSpectroscopyRoot(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void installFitTooltipsWithin(Node node) {
        if (node == null) return;
        if (node instanceof RadioButton radio) {
            installFitTooltip(radio);
            return;
        }
        if (node instanceof Chart || node instanceof ListView<?> || node instanceof TableView<?>) return;
        if (node instanceof ScrollPane scroll) {
            installFitTooltipsWithin(scroll.getContent());
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                installFitTooltipsWithin(child);
            }
        }
    }

    private static void installFitTooltip(RadioButton radio) {
        if (radio == null || Boolean.TRUE.equals(radio.getProperties().get(FIT_TOOLTIP))) return;
        radio.getProperties().put(FIT_TOOLTIP, Boolean.TRUE);

        Object value = radio.getUserData();
        String source = value == null ? radio.getText() : value.toString();
        if (source == null || source.isBlank()) return;

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
