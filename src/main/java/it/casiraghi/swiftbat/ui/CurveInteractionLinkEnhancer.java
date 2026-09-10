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
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
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
 * Population selections and the optional focus lock are stored globally by
 * series name so normal 2D, fullscreen 2D and population 3D stay coherent.
 */
public final class CurveInteractionLinkEnhancer {
    private static final String WATCHED = CurveInteractionLinkEnhancer.class.getName() + ".watched";
    private static final String LINE_DONE = CurveInteractionLinkEnhancer.class.getName() + ".lineDone";
    private static final String HOVER = CurveInteractionLinkEnhancer.class.getName() + ".hover";
    private static final String LOCK_DONE = CurveInteractionLinkEnhancer.class.getName() + ".lockDone";
    private static final String LOCK_SYNC = CurveInteractionLinkEnhancer.class.getName() + ".lockSync";
    private static final String LOCK_ADDED = CurveInteractionLinkEnhancer.class.getName() + ".lockAdded";
    private static final String CHART_FOCUS = ChartInteractionEnhancer.class.getName() + ".focus";
    private static final String CHART_LINE_DONE = ChartInteractionEnhancer.class.getName() + ".lineDone";
    private static final double HIT_RADIUS = 18.0;
    private static final DecimalFormat NUMBER_FORMAT;
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final LinkedHashSet<String> POPULATION_FOCUS = new LinkedHashSet<>();
    private static final Set<ToggleButton> POPULATION_LOCK_TOGGLES = Collections.newSetFromMap(new WeakHashMap<>());

    private static Parent installedRoot;
    private static Scene installedScene;
    private static boolean populationLocked;

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

    public static boolean populationFocusLocked() {
        synchronized (POPULATION_FOCUS) {
            return populationLocked;
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
            if (POPULATION_FOCUS.isEmpty()) populationLocked = false;
        }
        Platform.runLater(() -> {
            refreshPopulationCharts();
            syncPopulationLockToggles();
        });
    }

    public static void setPopulationFocusLocked(boolean locked) {
        synchronized (POPULATION_FOCUS) {
            populationLocked = locked && !POPULATION_FOCUS.isEmpty();
        }
        Platform.runLater(() -> {
            refreshPopulationCharts();
            syncPopulationLockToggles();
        });
    }

    /** Creates one UI endpoint for the shared Population focus lock. */
    public static void bindPopulationLockToggle(ToggleButton toggle) {
        if (toggle == null) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> bindPopulationLockToggle(toggle));
            return;
        }
        synchronized (POPULATION_LOCK_TOGGLES) {
            POPULATION_LOCK_TOGGLES.add(toggle);
        }
        if (!toggle.getStyleClass().contains("population-curve-lock")) {
            toggle.getStyleClass().addAll("ghost-button", "population-curve-lock");
        }
        toggle.setFocusTraversable(false);
        toggle.setMinWidth(44);
        toggle.setPrefWidth(44);
        toggle.setMaxWidth(44);
        if (!Boolean.TRUE.equals(toggle.getProperties().get(LOCK_DONE))) {
            toggle.getProperties().put(LOCK_DONE, Boolean.TRUE);
            toggle.selectedProperty().addListener((obs, oldValue, selected) -> {
                if (Boolean.TRUE.equals(toggle.getProperties().get(LOCK_SYNC))) return;
                setPopulationFocusLocked(selected);
            });
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> syncPopulationLockToggle(toggle));
        }
        syncPopulationLockToggle(toggle);
    }

    private static void syncPopulationLockToggles() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(CurveInteractionLinkEnhancer::syncPopulationLockToggles);
            return;
        }
        synchronized (POPULATION_LOCK_TOGGLES) {
            for (ToggleButton toggle : List.copyOf(POPULATION_LOCK_TOGGLES)) {
                if (toggle != null) syncPopulationLockToggle(toggle);
            }
        }
    }

    private static void syncPopulationLockToggle(ToggleButton toggle) {
        boolean locked = populationFocusLocked();
        boolean hasSelection = !populationFocusedNames().isEmpty();
        toggle.getProperties().put(LOCK_SYNC, Boolean.TRUE);
        try {
            toggle.setSelected(locked);
            toggle.setDisable(!hasSelection && !locked);
            toggle.setText(locked ? "🔒" : "🔓");
            String hint = locked
                    ? I18n.dynamic("Sblocca la selezione delle curve", "Unlock curve selection")
                    : I18n.dynamic("Blocca le curve selezionate", "Lock selected curves");
            toggle.setAccessibleText(hint);
            Tooltip tooltip = toggle.getTooltip();
            if (tooltip == null) {
                tooltip = UiFactory.quickTooltip(hint);
                toggle.setTooltip(tooltip);
            } else {
                tooltip.setText(hint);
            }
        } finally {
            toggle.getProperties().remove(LOCK_SYNC);
        }
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
                syncPopulationLockToggles();
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
        if (Boolean.TRUE.equals(rawChart.getProperties().get(LINE_DONE))) {
            if (isPopulationChart(rawChart)) Platform.runLater(() -> installPopulationLockControl(rawChart));
            return;
        }
        rawChart.getProperties().put(LINE_DONE, Boolean.TRUE);
        LineChart chart = rawChart;

        if (isPopulationChart(chart)) Platform.runLater(() -> installPopulationLockControl(chart));

        Tooltip standaloneTooltip = Boolean.TRUE.equals(chart.getProperties().get(CHART_LINE_DONE)) ? null : new Tooltip();
        if (standaloneTooltip != null) {
            standaloneTooltip.setAutoHide(false);
            standaloneTooltip.setShowDelay(Duration.ZERO);
            standaloneTooltip.setHideDelay(Duration.ZERO);
        }

        chart.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            boolean lockedPopulation = isPopulationChart(chart) && populationFocusLocked();
            SeriesHit hit = nearest(chart, event.getX(), event.getY(), lockedPopulation);
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

        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (isPopulationChart(chart) && populationFocusLocked()
                    && event.getButton() == MouseButton.PRIMARY) {
                chart.getProperties().remove(HOVER);
                applyVisualState(chart);
                if (standaloneTooltip != null) standaloneTooltip.hide();
                event.consume();
                return;
            }
            Platform.runLater(() -> {
                if (isPopulationChart(chart)) capturePopulationFocus(chart);
                applyVisualState(chart);
            });
        });

        chart.getData().addListener((ListChangeListener<XYChart.Series>) change -> Platform.runLater(() -> {
            if (isPopulationChart(chart)) restorePopulationFocus(chart);
            applyVisualState(chart);
            if (isPopulationChart(chart)) installPopulationLockControl(chart);
        }));

        Platform.runLater(() -> {
            if (isPopulationChart(chart)) restorePopulationFocus(chart);
            applyVisualState(chart);
        });
    }

    /**
     * PopulationPage creates both the embedded and fullscreen 2D toolbars before
     * this enhancer sees the chart. Injecting the same bound toggle here avoids
     * duplicating state in the page and also covers dynamically-created fullscreen charts.
     */
    private static void installPopulationLockControl(LineChart<?, ?> chart) {
        if (chart == null || !isPopulationChart(chart) || chart.getScene() == null) return;
        Parent searchRoot = ancestorWithStyle(chart, "population-chart-card");
        if (searchRoot == null) searchRoot = ancestorWithStyle(chart, "in-place-fullscreen");
        if (searchRoot == null) return;
        if (containsLockToggle(searchRoot)) return;

        HBox toolbar = findPopulationToolbar(searchRoot);
        if (toolbar == null) return;
        ToggleButton lock = new ToggleButton();
        bindPopulationLockToggle(lock);

        int insertAt = 0;
        for (int index = 0; index < toolbar.getChildren().size(); index++) {
            Node child = toolbar.getChildren().get(index);
            if (child instanceof ButtonBase button) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                if (text.contains("3d") || text.contains("schermo intero") || text.contains("fullscreen")) {
                    insertAt = index;
                    break;
                }
                insertAt = index + 1;
            }
        }
        toolbar.getChildren().add(Math.max(0, Math.min(insertAt, toolbar.getChildren().size())), lock);
        chart.getProperties().put(LOCK_ADDED, Boolean.TRUE);
    }

    private static boolean containsLockToggle(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof ToggleButton toggle && toggle.getStyleClass().contains("population-curve-lock")) return true;
            if (child instanceof Parent parent && containsLockToggle(parent)) return true;
        }
        return false;
    }

    private static HBox findPopulationToolbar(Parent root) {
        HBox helpOnly = null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof HBox row) {
                boolean hasAction = false;
                boolean hasHelp = false;
                for (Node item : row.getChildren()) {
                    if (!(item instanceof ButtonBase button)) continue;
                    String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                    hasAction |= text.contains("3d") || text.contains("schermo intero") || text.contains("fullscreen");
                    hasHelp |= text.contains("spiegazione") || text.contains("explanation");
                }
                if (hasAction) return row;
                if (hasHelp) helpOnly = row;
            }
            if (child instanceof Parent parent) {
                HBox nested = findPopulationToolbar(parent);
                if (nested != null) {
                    boolean nestedHasAction = nested.getChildren().stream().anyMatch(item -> {
                        if (!(item instanceof ButtonBase button)) return false;
                        String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                        return text.contains("3d") || text.contains("schermo intero") || text.contains("fullscreen");
                    });
                    if (nestedHasAction) return nested;
                    if (helpOnly == null) helpOnly = nested;
                }
            }
        }
        return helpOnly;
    }

    private static Parent ancestorWithStyle(Node node, String styleClass) {
        Node current = node;
        while (current != null) {
            if (current instanceof Parent parent && current.getStyleClass().contains(styleClass)) return parent;
            current = current.getParent();
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SeriesHit nearest(LineChart chart, double mouseX, double mouseY, boolean focusedOnly) {
        if (chart.getScene() == null) return null;
        Axis xAxis = chart.getXAxis();
        Axis yAxis = chart.getYAxis();
        Set<XYChart.Series> focus = focusedOnly ? focusedSeries(chart) : Set.of();
        double best = HIT_RADIUS * HIT_RADIUS;
        SeriesHit result = null;
        for (Object rawSeries : chart.getData()) {
            XYChart.Series series = (XYChart.Series) rawSeries;
            if (focusedOnly && !focus.contains(series)) continue;
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
        boolean lockedPopulation = isPopulationChart(chart) && populationFocusLocked();
        for (Object raw : chart.getData()) {
            XYChart.Series series = (XYChart.Series) raw;
            Node node = series.getNode();
            if (node == null) continue;
            String name = series.getName();
            boolean active = focus.isEmpty() || focus.contains(series)
                    || (!lockedPopulation && series == hovered);
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
        Set<XYChart.Series> focus = focusedSeries(chart);
        if (desired.isEmpty()) {
            focus.clear();
            return;
        }
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
            chart.getProperties().remove(HOVER);
            applyVisualState(chart);
            installPopulationLockControl(chart);
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
