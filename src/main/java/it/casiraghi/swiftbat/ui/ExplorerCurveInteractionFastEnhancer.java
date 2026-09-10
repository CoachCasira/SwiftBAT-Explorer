package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

/** Low-overhead Explorer light-curve interaction owner. */
public final class ExplorerCurveInteractionFastEnhancer {
    private static final String WATCHED = ExplorerCurveInteractionFastEnhancer.class.getName() + ".watched";
    private static final String TAB_WATCHED = ExplorerCurveInteractionFastEnhancer.class.getName() + ".tabWatched";
    private static final String DONE = ExplorerCurveInteractionFastEnhancer.class.getName() + ".done";
    private static final String HOVER = ExplorerCurveInteractionFastEnhancer.class.getName() + ".hover";
    private static final String GENERIC_LINE_DONE = ChartInteractionEnhancer.class.getName() + ".lineDone";
    private static final double HIT_RADIUS = 18.0;
    private static final DecimalFormat NUMBER_FORMAT;
    private static final Glow HOVER_GLOW = new Glow(0.88);
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final LinkedHashSet<String> FOCUSED_NAMES = new LinkedHashSet<>();

    private static Parent installedRoot;
    private static Scene installedScene;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        NUMBER_FORMAT = new DecimalFormat("0.#####", symbols);
    }

    private ExplorerCurveInteractionFastEnhancer() { }

    public static void install(Parent root) {
        if (root == null) return;
        installedRoot = root;
        watch(root);
        observeScene(root);
    }

    private static void observeScene(Parent root) {
        if (root.getScene() != null) watchScene(root.getScene());
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) watchScene(newScene);
        });
    }

    private static void watchScene(Scene scene) {
        if (scene == null) return;
        installedScene = scene;
        if (!WATCHED_SCENES.add(scene)) return;
        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            // The normal application root is intentionally not scanned here.
            // Only the temporary fullscreen root needs Explorer interactions.
            if (!isFullscreenRoot(newRoot)) return;
            Platform.runLater(() -> {
                watch(newRoot);
                refreshNode(newRoot);
            });
        });
    }

    private static boolean isFullscreenRoot(Node node) {
        return node != null && node.getStyleClass().contains("in-place-fullscreen");
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);

        // Do not descend into chart/control skins: their transient nodes are not
        // targets and watching them adds listeners while charts/tables scroll.
        if (node instanceof LineChart<?, ?>
                || node instanceof ChoiceBox<?>
                || node instanceof ComboBox<?>
                || node instanceof ListView<?>
                || node instanceof TableView<?>) return;

        if (node instanceof ScrollPane scroll) {
            watch(scroll.getContent());
            if (!Boolean.TRUE.equals(scroll.getProperties().get(WATCHED))) {
                scroll.getProperties().put(WATCHED, Boolean.TRUE);
                scroll.contentProperty().addListener((obs, oldContent, newContent) -> watch(newContent));
            }
            return;
        }

        if (node instanceof TabPane tabs) {
            for (Tab tab : List.copyOf(tabs.getTabs())) watchTab(tab);
            if (!Boolean.TRUE.equals(tabs.getProperties().get(WATCHED))) {
                tabs.getProperties().put(WATCHED, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) if (change.wasAdded()) {
                        for (Tab tab : List.copyOf(change.getAddedSubList())) watchTab(tab);
                    }
                });
            }
            return;
        }

        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
            }
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void watchTab(Tab tab) {
        if (tab == null) return;
        watch(tab.getContent());
        if (Boolean.TRUE.equals(tab.getProperties().get(TAB_WATCHED))) return;
        tab.getProperties().put(TAB_WATCHED, Boolean.TRUE);
        tab.contentProperty().addListener((obs, oldContent, newContent) -> watch(newContent));
    }

    private static void enhance(Node node) {
        if (node instanceof LineChart<?, ?> chart && isExplorerCurve(chart)) installChart(chart);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installChart(LineChart<?, ?> rawChart) {
        rawChart.getProperties().put(GENERIC_LINE_DONE, Boolean.TRUE);
        if (Boolean.TRUE.equals(rawChart.getProperties().get(DONE))) return;
        rawChart.getProperties().put(DONE, Boolean.TRUE);
        LineChart chart = rawChart;

        Tooltip tooltip = new Tooltip();
        tooltip.setAutoHide(false);
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setHideDelay(Duration.ZERO);

        chart.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            SegmentHit hit = nearest(chart, event.getX(), event.getY());
            Object previous = chart.getProperties().get(HOVER);
            XYChart.Series next = hit == null ? null : hit.series();
            if (previous != next) {
                if (next == null) chart.getProperties().remove(HOVER);
                else chart.getProperties().put(HOVER, next);
                applyVisualState(chart);
            }

            if (hit == null) {
                tooltip.hide();
                return;
            }
            String seriesName = hit.series().getName() == null
                    ? I18n.dynamic("Curva", "Curve") : hit.series().getName();
            String xLabel = chart.getXAxis().getLabel();
            String yLabel = chart.getYAxis().getLabel();
            tooltip.setText(seriesName + "\n"
                    + (xLabel == null || xLabel.isBlank() ? "X" : xLabel) + ": " + format(hit.xValue()) + "\n"
                    + (yLabel == null || yLabel.isBlank() ? "Y" : yLabel) + ": " + format(hit.yValue()));
            if (!tooltip.isShowing()) {
                if (chart.getScene() != null && chart.getScene().getWindow() != null) {
                    tooltip.show(chart, event.getScreenX() + 14, event.getScreenY() + 14);
                }
            } else {
                tooltip.setAnchorX(event.getScreenX() + 14);
                tooltip.setAnchorY(event.getScreenY() + 14);
            }
        });

        chart.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            if (chart.getProperties().remove(HOVER) != null) applyVisualState(chart);
            tooltip.hide();
        });

        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            SegmentHit hit = nearest(chart, event.getX(), event.getY());
            if (event.getClickCount() >= 2) {
                tooltip.hide();
                if (hit == null && !FOCUSED_NAMES.isEmpty()) {
                    FOCUSED_NAMES.clear();
                    refreshAll();
                } else if (hit != null) {
                    openThreeD(chart);
                }
                event.consume();
                return;
            }
            if (hit == null || !selectable(hit.series())) return;
            String name = canonicalName(hit.series().getName());
            if (name == null) return;
            if (!FOCUSED_NAMES.add(name)) FOCUSED_NAMES.remove(name);
            chart.getProperties().remove(HOVER);
            tooltip.hide();
            refreshAll();
            event.consume();
        });

        chart.getData().addListener((ListChangeListener<XYChart.Series>) change ->
                Platform.runLater(() -> applyVisualState(chart)));
        Platform.runLater(() -> applyVisualState(chart));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SegmentHit nearest(LineChart chart, double mouseX, double mouseY) {
        if (chart.getScene() == null) return null;
        Axis xAxis = chart.getXAxis();
        Axis yAxis = chart.getYAxis();

        Point2D mouseScene = chart.localToScene(mouseX, mouseY);
        if (mouseScene == null) return null;
        Point2D mouseInX = xAxis.sceneToLocal(mouseScene);
        Point2D mouseInY = yAxis.sceneToLocal(mouseScene);
        if (mouseInX == null || mouseInY == null) return null;
        double plotX = mouseInX.getX();
        double plotY = mouseInY.getY();

        double best = HIT_RADIUS * HIT_RADIUS;
        SegmentHit result = null;
        for (Object rawSeries : chart.getData()) {
            XYChart.Series series = (XYChart.Series) rawSeries;
            if (!selectable(series) || series.getData() == null || series.getData().isEmpty()) continue;

            XYChart.Data previousData = null;
            Point2D previousPoint = null;
            for (Object rawData : series.getData()) {
                XYChart.Data data = (XYChart.Data) rawData;
                Point2D point = plotPoint(xAxis, yAxis, data);
                if (point == null) {
                    previousData = null;
                    previousPoint = null;
                    continue;
                }

                double pointDistance = squaredDistance(plotX, plotY, point.getX(), point.getY());
                if (pointDistance < best) {
                    best = pointDistance;
                    result = new SegmentHit(series, data.getXValue(), data.getYValue());
                }

                if (previousData != null && previousPoint != null) {
                    Projection projection = project(plotX, plotY, previousPoint, point);
                    if (projection.distanceSquared() < best) {
                        best = projection.distanceSquared();
                        result = new SegmentHit(series,
                                interpolate(previousData.getXValue(), data.getXValue(), projection.fraction()),
                                interpolate(previousData.getYValue(), data.getYValue(), projection.fraction()));
                    }
                }
                previousData = data;
                previousPoint = point;
            }
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static Point2D plotPoint(Axis xAxis, Axis yAxis, XYChart.Data data) {
        Object xValue = data.getXValue();
        Object yValue = data.getYValue();
        if (xValue == null || yValue == null) return null;
        try {
            double x = xAxis.getDisplayPosition(xValue);
            double y = yAxis.getDisplayPosition(yValue);
            if (!Double.isFinite(x) || !Double.isFinite(y)) return null;
            return new Point2D(x, y);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Projection project(double mouseX, double mouseY, Point2D start, Point2D end) {
        double vx = end.getX() - start.getX();
        double vy = end.getY() - start.getY();
        double lengthSquared = vx * vx + vy * vy;
        if (lengthSquared <= 1e-12) {
            return new Projection(0, squaredDistance(mouseX, mouseY, start.getX(), start.getY()));
        }
        double fraction = ((mouseX - start.getX()) * vx + (mouseY - start.getY()) * vy) / lengthSquared;
        fraction = Math.max(0, Math.min(1, fraction));
        double x = start.getX() + fraction * vx;
        double y = start.getY() + fraction * vy;
        return new Projection(fraction, squaredDistance(mouseX, mouseY, x, y));
    }

    private static double squaredDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private static Object interpolate(Object start, Object end, double fraction) {
        if (start instanceof Number a && end instanceof Number b) {
            return a.doubleValue() + (b.doubleValue() - a.doubleValue()) * fraction;
        }
        return fraction < 0.5 ? start : end;
    }

    @SuppressWarnings("rawtypes")
    private static void applyVisualState(LineChart chart) {
        Set<String> presentFocus = presentFocus(chart);
        Object rawHover = chart.getProperties().get(HOVER);
        XYChart.Series hovered = rawHover instanceof XYChart.Series series ? series : null;

        for (Object rawSeries : chart.getData()) {
            XYChart.Series series = (XYChart.Series) rawSeries;
            Node node = series.getNode();
            if (node == null) continue;
            boolean trigger = !selectable(series);
            String name = canonicalName(series.getName());
            boolean selected = presentFocus.isEmpty() || (name != null && presentFocus.contains(name));
            boolean hot = series == hovered && !trigger;
            node.setOpacity(trigger ? 0.55 : selected || hot ? 1.0 : 0.09);
            node.setEffect(hot ? HOVER_GLOW : null);
        }
    }

    @SuppressWarnings("rawtypes")
    private static Set<String> presentFocus(LineChart chart) {
        LinkedHashSet<String> available = new LinkedHashSet<>();
        for (Object rawSeries : chart.getData()) {
            XYChart.Series series = (XYChart.Series) rawSeries;
            String name = canonicalName(series.getName());
            if (name != null && FOCUSED_NAMES.contains(name)) available.add(name);
        }
        return available;
    }

    private static void refreshAll() {
        Platform.runLater(() -> {
            if (installedRoot != null) refreshNode(installedRoot);
            if (installedScene != null && isFullscreenRoot(installedScene.getRoot())) {
                refreshNode(installedScene.getRoot());
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private static void refreshNode(Node node) {
        if (node == null) return;
        if (node instanceof LineChart chart) {
            if (isExplorerCurve(chart)) {
                chart.getProperties().remove(HOVER);
                applyVisualState(chart);
            }
            return;
        }
        if (node instanceof ListView<?> || node instanceof TableView<?>
                || node instanceof ChoiceBox<?> || node instanceof ComboBox<?>) return;
        if (node instanceof ScrollPane scroll) {
            refreshNode(scroll.getContent());
            return;
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : List.copyOf(tabs.getTabs())) refreshNode(tab.getContent());
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) refreshNode(child);
        }
    }

    private static boolean isExplorerCurve(LineChart<?, ?> chart) {
        if (chart == null || !chart.getStyleClass().contains("lightcurve-chart")) return false;
        String title = chart.getTitle() == null ? "" : chart.getTitle().toLowerCase(Locale.ROOT);
        return title.contains("binning di 1 secondo")
                || title.contains("1-second-binned")
                || title.contains("1 second");
    }

    @SuppressWarnings("rawtypes")
    private static boolean selectable(XYChart.Series series) {
        String name = series == null ? null : series.getName();
        if (name == null) return true;
        String lower = name.toLowerCase(Locale.ROOT);
        return !lower.startsWith("trigger") && !lower.contains("t = 0");
    }

    private static String canonicalName(String name) {
        if (name == null || name.isBlank()) return null;
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.startsWith("trigger") || lower.contains("t = 0")) return null;
        if (lower.contains("15") && lower.contains("350")
                && (lower.contains("totale") || lower.contains("total"))) {
            return "TOTAL_15_350";
        }
        return name.trim();
    }

    private static boolean openThreeD(Node source) {
        Node current = source;
        for (int depth = 0; current != null && depth < 12; depth++, current = current.getParent()) {
            if (current instanceof TabPane tabs) {
                for (Tab tab : tabs.getTabs()) {
                    String text = tab.getText() == null ? "" : tab.getText().toLowerCase(Locale.ROOT);
                    if (!text.contains("3d")) continue;
                    tabs.getSelectionModel().select(tab);
                    Platform.runLater(() -> openTabFullscreen(tab));
                    return true;
                }
            }
            if (current.getStyleClass().contains("page-root")) break;
        }
        return false;
    }

    private static void openTabFullscreen(Tab tab) {
        Node content = tab == null ? null : tab.getContent();
        if (!(content instanceof Parent parent)) return;
        Button fullscreen = findFullscreenButton(parent);
        if (fullscreen != null && !fullscreen.isDisabled()) fullscreen.fire();
    }

    private static Button findFullscreenButton(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.isVisible() && button.isManaged()) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                String compact = text.replace(" ", "");
                if (text.contains("schermo intero") || text.contains("full screen") || compact.contains("fullscreen")) {
                    return button;
                }
            }
            if (child instanceof Parent parent) {
                Button nested = findFullscreenButton(parent);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static String format(Object value) {
        if (value instanceof Number number) return NUMBER_FORMAT.format(number.doubleValue());
        return value == null ? "—" : value.toString();
    }

    private record Projection(double fraction, double distanceSquared) { }
    private record SegmentHit(XYChart.Series<?, ?> series, Object xValue, Object yValue) { }
}
