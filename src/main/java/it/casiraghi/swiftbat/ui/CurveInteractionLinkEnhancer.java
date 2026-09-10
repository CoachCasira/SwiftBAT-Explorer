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
import javafx.scene.control.Tooltip;
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

/**
 * Keeps line-curve interaction coherent across embedded and in-place fullscreen
 * views. When a chart already has a focused subset, hovering a dimmed curve
 * temporarily restores its visibility so it can be identified before clicking.
 * Population selections are stored by series name so the same selection can be
 * applied to the 3D population renderer and back again.
 */
public final class CurveInteractionLinkEnhancer {
    private static final String WATCHED = CurveInteractionLinkEnhancer.class.getName() + ".watched";
    private static final String LINE_DONE = CurveInteractionLinkEnhancer.class.getName() + ".lineDone";
    private static final String HOVER = CurveInteractionLinkEnhancer.class.getName() + ".hover";
    private static final String CHART_FOCUS = ChartInteractionEnhancer.class.getName() + ".focus";
    private static final String CHART_LINE_DONE = ChartInteractionEnhancer.class.getName() + ".lineDone";
    private static final double HIT_RADIUS = 18.0;
    private static final DecimalFormat NUMBER_FORMAT;
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final LinkedHashSet<String> POPULATION_FOCUS = new LinkedHashSet<>();

    private static Parent installedRoot;
    private static Scene installedScene;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        NUMBER_FORMAT = new DecimalFormat("0.#####", symbols);
    }

    private CurveInteractionLinkEnhancer() { }

    public static void install(Parent root) {
        if (root == null) return;
        installedRoot = root;
        watch(root);
        observeScene(root);
        Platform.runLater(() -> scan(root));
    }

    public static Set<String> populationFocusedNames() {
        synchronized (POPULATION_FOCUS) {
            return Set.copyOf(POPULATION_FOCUS);
        }
    }

    public static void setPopulationFocusedNames(Set<String> names) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (names != null) {
            for (String name : names) {
                String canonical = canonicalPopulationName(name);
                if (canonical != null) normalized.add(canonical);
            }
        }
        synchronized (POPULATION_FOCUS) {
            POPULATION_FOCUS.clear();
            POPULATION_FOCUS.addAll(normalized);
        }
        Platform.runLater(CurveInteractionLinkEnhancer::refreshPopulationCharts);
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
            if (newRoot == null) return;
            Platform.runLater(() -> {
                watch(newRoot);
                scan(newRoot);
                refreshPopulationCharts();
            });
        });
        Parent current = scene.getRoot();
        if (current != null) Platform.runLater(() -> {
            watch(current);
            scan(current);
        });
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
            }
            Platform.runLater(() -> scan(parent));
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void scan(Node node) {
        if (node == null) return;
        enhance(node);
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) scan(child);
        }
    }

    private static void enhance(Node node) {
        if (node instanceof LineChart<?, ?> chart) installLineInteraction(chart);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installLineInteraction(LineChart<?, ?> rawChart) {
        if (Boolean.TRUE.equals(rawChart.getProperties().get(LINE_DONE))) return;
        rawChart.getProperties().put(LINE_DONE, Boolean.TRUE);
        LineChart chart = rawChart;

        Tooltip standaloneTooltip = Boolean.TRUE.equals(chart.getProperties().get(CHART_LINE_DONE)) ? null : new Tooltip();
        if (standaloneTooltip != null) {
            standaloneTooltip.setAutoHide(false);
            standaloneTooltip.setShowDelay(Duration.ZERO);
            standaloneTooltip.setHideDelay(Duration.ZERO);
        }

        chart.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            SeriesHit hit = nearest(chart, event.getX(), event.getY());
            Set<XYChart.Series> focus = focusedSeries(chart);
            XYChart.Series hovered = hit == null || !selectable(hit.series()) || focus.isEmpty() ? null : hit.series();
            chart.getProperties().put(HOVER, hovered);
            applyVisualState(chart);

            if (standaloneTooltip != null) {
                if (hit == null) {
                    standaloneTooltip.hide();
                } else {
                    String seriesName = hit.series().getName() == null ? I18n.dynamic("Curva", "Curve") : hit.series().getName();
                    String xLabel = chart.getXAxis().getLabel();
                    String yLabel = chart.getYAxis().getLabel();
                    standaloneTooltip.setText(seriesName + "\n"
                            + (xLabel == null || xLabel.isBlank() ? "X" : xLabel) + ": " + format(hit.data().getXValue()) + "\n"
                            + (yLabel == null || yLabel.isBlank() ? "Y" : yLabel) + ": " + format(hit.data().getYValue()));
                    if (!standaloneTooltip.isShowing()) {
                        standaloneTooltip.show(chart, event.getScreenX() + 14, event.getScreenY() + 14);
                    } else {
                        standaloneTooltip.setAnchorX(event.getScreenX() + 14);
                        standaloneTooltip.setAnchorY(event.getScreenY() + 14);
                    }
                }
            }
        });

        chart.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            chart.getProperties().remove(HOVER);
            applyVisualState(chart);
            if (standaloneTooltip != null) standaloneTooltip.hide();
        });

        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> Platform.runLater(() -> {
            if (isPopulationChart(chart)) capturePopulationFocus(chart);
            applyVisualState(chart);
        }));

        chart.getData().addListener((ListChangeListener<XYChart.Series>) change -> Platform.runLater(() -> {
            if (isPopulationChart(chart)) restorePopulationFocus(chart);
            applyVisualState(chart);
        }));

        Platform.runLater(() -> {
            if (isPopulationChart(chart)) restorePopulationFocus(chart);
            applyVisualState(chart);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SeriesHit nearest(LineChart chart, double mouseX, double mouseY) {
        if (chart.getScene() == null) return null;
        Axis xAxis = chart.getXAxis();
        Axis yAxis = chart.getYAxis();
        double best = HIT_RADIUS * HIT_RADIUS;
        SeriesHit result = null;
        for (Object rawSeries : chart.getData()) {
            XYChart.Series series = (XYChart.Series) rawSeries;
            if (series.getData() == null || series.getData().isEmpty()) continue;
            for (Object rawData : series.getData()) {
                XYChart.Data data = (XYChart.Data) rawData;
                Object xValue = data.getXValue();
                Object yValue = data.getYValue();
                if (xValue == null || yValue == null) continue;
                double xDisplay;
                double yDisplay;
                try {
                    xDisplay = xAxis.getDisplayPosition(xValue);
                    yDisplay = yAxis.getDisplayPosition(yValue);
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (!Double.isFinite(xDisplay) || !Double.isFinite(yDisplay)) continue;
                Point2D xScene = xAxis.localToScene(xDisplay, 0);
                Point2D yScene = yAxis.localToScene(0, yDisplay);
                if (xScene == null || yScene == null) continue;
                Point2D point = chart.sceneToLocal(xScene.getX(), yScene.getY());
                double dx = point.getX() - mouseX;
                double dy = point.getY() - mouseY;
                double distance = dx * dx + dy * dy;
                if (distance < best) {
                    best = distance;
                    result = new SeriesHit(series, data);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static boolean selectable(XYChart.Series series) {
        String name = series == null ? null : series.getName();
        return name == null || !isTrigger(name);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Set<XYChart.Series> focusedSeries(LineChart chart) {
        Object raw = chart.getProperties().get(CHART_FOCUS);
        if (raw instanceof Set<?> set) return (Set<XYChart.Series>) set;
        Set<XYChart.Series> created = new LinkedHashSet<>();
        chart.getProperties().put(CHART_FOCUS, created);
        return created;
    }

    @SuppressWarnings("rawtypes")
    private static void applyVisualState(LineChart chart) {
        Set<XYChart.Series> focus = focusedSeries(chart);
        Object hoverRaw = chart.getProperties().get(HOVER);
        XYChart.Series hovered = hoverRaw instanceof XYChart.Series series ? series : null;
        for (Object raw : chart.getData()) {
            XYChart.Series series = (XYChart.Series) raw;
            Node node = series.getNode();
            if (node == null) continue;
            String name = series.getName();
            boolean active = focus.isEmpty() || focus.contains(series) || series == hovered;
            node.setOpacity(active ? 1.0 : isTrigger(name) ? 0.50 : 0.09);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void capturePopulationFocus(LineChart chart) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (XYChart.Series series : focusedSeries(chart)) {
            String name = canonicalPopulationName(series.getName());
            if (name != null) names.add(name);
        }
        setPopulationFocusedNames(names);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restorePopulationFocus(LineChart chart) {
        Set<String> desired = populationFocusedNames();
        if (desired.isEmpty()) return;
        Set<XYChart.Series> focus = focusedSeries(chart);
        LinkedHashSet<XYChart.Series> matched = new LinkedHashSet<>();
        for (Object raw : chart.getData()) {
            XYChart.Series series = (XYChart.Series) raw;
            String name = canonicalPopulationName(series.getName());
            if (name != null && desired.contains(name)) matched.add(series);
        }
        if (!matched.isEmpty()) {
            focus.clear();
            focus.addAll(matched);
        }
    }

    private static void refreshPopulationCharts() {
        if (installedRoot != null) refreshPopulationNode(installedRoot);
        if (installedScene != null && installedScene.getRoot() != null && installedScene.getRoot() != installedRoot) {
            refreshPopulationNode(installedScene.getRoot());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void refreshPopulationNode(Node node) {
        if (node == null) return;
        if (node instanceof LineChart<?, ?> raw && isPopulationChart(raw)) {
            LineChart chart = raw;
            Set<String> desired = populationFocusedNames();
            Set<XYChart.Series> focus = focusedSeries(chart);
            focus.clear();
            if (!desired.isEmpty()) {
                for (Object item : chart.getData()) {
                    XYChart.Series series = (XYChart.Series) item;
                    String name = canonicalPopulationName(series.getName());
                    if (name != null && desired.contains(name)) focus.add(series);
                }
            }
            applyVisualState(chart);
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) refreshPopulationNode(child);
        }
    }

    private static boolean isPopulationChart(LineChart<?, ?> chart) {
        return chart != null && chart.getStyleClass().contains("population-chart");
    }

    private static boolean isTrigger(String name) {
        return name != null && (name.toLowerCase(Locale.ROOT).startsWith("trigger")
                || name.toLowerCase(Locale.ROOT).contains("t = 0"));
    }

    private static String canonicalPopulationName(String name) {
        if (name == null || name.isBlank() || isTrigger(name)) return null;
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.equals("mediana") || lower.equals("median")) return "Mediana";
        if (lower.contains("25") && lower.contains("percentile")) return "25° percentile";
        if (lower.contains("75") && lower.contains("percentile")) return "75° percentile";
        return name.trim();
    }

    private static String format(Object value) {
        if (value instanceof Number number) return NUMBER_FORMAT.format(number.doubleValue());
        return String.valueOf(value);
    }

    private record SeriesHit(XYChart.Series series, XYChart.Data data) { }
}
