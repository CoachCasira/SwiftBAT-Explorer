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
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
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
 * Owns Population temporal-profile interaction across embedded 2D, fullscreen 2D
 * and the 3D renderer. The selected subset is shared by series name. When locked,
 * excluded curves are not hit-testable; one locked curve can additionally be
 * spotlighted without hiding or dimming the other locked curves.
 */
public final class CurveInteractionLinkEnhancer {
    private static final String WATCHED = CurveInteractionLinkEnhancer.class.getName() + ".watched";
    private static final String LINE_DONE = CurveInteractionLinkEnhancer.class.getName() + ".lineDone";
    private static final String HOVER = CurveInteractionLinkEnhancer.class.getName() + ".hover";
    private static final String LOCK_DONE = CurveInteractionLinkEnhancer.class.getName() + ".lockDone";
    private static final String LOCK_SYNC = CurveInteractionLinkEnhancer.class.getName() + ".lockSync";
    private static final String LOCK_ADDED = CurveInteractionLinkEnhancer.class.getName() + ".lockAdded";
    private static final String SPOTLIGHT_STYLE = CurveInteractionLinkEnhancer.class.getName() + ".spotlightStyle";
    private static final String CHART_FOCUS = ChartInteractionEnhancer.class.getName() + ".focus";
    private static final String CHART_LINE_DONE = ChartInteractionEnhancer.class.getName() + ".lineDone";
    private static final String CHART_EXPORT_DONE = ChartInteractionEnhancer.class.getName() + ".exportDone";
    private static final double HIT_RADIUS = 18.0;
    private static final DecimalFormat NUMBER_FORMAT;
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final LinkedHashSet<String> POPULATION_FOCUS = new LinkedHashSet<>();
    private static final Set<ToggleButton> POPULATION_LOCK_TOGGLES = Collections.newSetFromMap(new WeakHashMap<>());

    private static Parent installedRoot;
    private static Scene installedScene;
    private static boolean populationLocked;
    private static String populationSpotlight;

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

    public static String populationSpotlightName() {
        synchronized (POPULATION_FOCUS) {
            return populationSpotlight;
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
            if (POPULATION_FOCUS.isEmpty()) {
                populationLocked = false;
                populationSpotlight = null;
            } else if (populationSpotlight != null && !POPULATION_FOCUS.contains(populationSpotlight)) {
                populationSpotlight = null;
            }
        }
        scheduleSharedRefresh();
    }

    public static void setPopulationFocusLocked(boolean locked) {
        synchronized (POPULATION_FOCUS) {
            populationLocked = locked && !POPULATION_FOCUS.isEmpty();
            if (!populationLocked) populationSpotlight = null;
        }
        scheduleSharedRefresh();
    }

    public static void setPopulationSpotlightName(String name) {
        synchronized (POPULATION_FOCUS) {
            String canonical = canonicalPopulationName(name);
            if (!populationLocked || canonical == null || !POPULATION_FOCUS.contains(canonical)) {
                populationSpotlight = null;
            } else {
                populationSpotlight = canonical;
            }
        }
        Platform.runLater(CurveInteractionLinkEnhancer::refreshPopulationCharts);
    }

    private static void scheduleSharedRefresh() {
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
        if (node instanceof LineChart<?, ?> chart && isPopulationChart(chart)) installPopulationLineInteraction(chart);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installPopulationLineInteraction(LineChart<?, ?> rawChart) {
        // This class is the sole interaction owner for Population line charts. Mark
        // the generic enhancer as already installed so two independent hit-testers
        // and tooltips cannot compete on the same chart.
        rawChart.getProperties().put(CHART_LINE_DONE, Boolean.TRUE);
        if (Boolean.TRUE.equals(rawChart.getProperties().get(LINE_DONE))) {
            Platform.runLater(() -> {
                installPopulationLockControl(rawChart);
                installPopulationExport(rawChart);
            });
            return;
        }
        rawChart.getProperties().put(LINE_DONE, Boolean.TRUE);
        LineChart chart = rawChart;

        Tooltip tooltip = new Tooltip();
        tooltip.setAutoHide(false);
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setHideDelay(Duration.ZERO);

        chart.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            boolean locked = populationFocusLocked();
            String spotlight = locked ? populationSpotlightName() : null;
            SeriesHit hit = nearest(chart, event.getX(), event.getY(), locked, spotlight);
            Set<XYChart.Series> focus = focusedSeries(chart);
            XYChart.Series hovered = hit == null || focus.isEmpty() ? null : hit.series();
            chart.getProperties().put(HOVER, hovered);
            applyVisualState(chart);

            if (hit == null) {
                tooltip.hide();
                return;
            }
            String seriesName = hit.series().getName() == null ? I18n.dynamic("Curva", "Curve") : hit.series().getName();
            String xLabel = chart.getXAxis().getLabel();
            String yLabel = chart.getYAxis().getLabel();
            tooltip.setText(seriesName + "\n"
                    + (xLabel == null || xLabel.isBlank() ? "X" : xLabel) + ": " + format(hit.xValue()) + "\n"
                    + (yLabel == null || yLabel.isBlank() ? "Y" : yLabel) + ": " + format(hit.yValue()));
            if (!tooltip.isShowing()) {
                tooltip.show(chart, event.getScreenX() + 14, event.getScreenY() + 14);
            } else {
                tooltip.setAnchorX(event.getScreenX() + 14);
                tooltip.setAnchorY(event.getScreenY() + 14);
            }
        });

        chart.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            chart.getProperties().remove(HOVER);
            applyVisualState(chart);
            tooltip.hide();
        });

        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            boolean locked = populationFocusLocked();
            if (locked && event.getButton() == MouseButton.SECONDARY) {
                setPopulationSpotlightName(null);
                chart.getProperties().remove(HOVER);
                tooltip.hide();
                event.consume();
                return;
            }
            if (event.getButton() != MouseButton.PRIMARY) return;

            if (locked) {
                SeriesHit hit = nearest(chart, event.getX(), event.getY(), true, null);
                if (hit != null && selectable(hit.series())) {
                    String clicked = canonicalPopulationName(hit.series().getName());
                    String current = populationSpotlightName();
                    setPopulationSpotlightName(clicked != null && clicked.equals(current) ? null : clicked);
                }
                chart.getProperties().remove(HOVER);
                tooltip.hide();
                event.consume();
                return;
            }

            SeriesHit hit = nearest(chart, event.getX(), event.getY(), false, null);
            if (event.getClickCount() >= 2) {
                tooltip.hide();
                if (hit == null && !focusedSeries(chart).isEmpty()) {
                    focusedSeries(chart).clear();
                    capturePopulationFocus(chart);
                    applyVisualState(chart);
                } else {
                    openThreeD(chart);
                }
                event.consume();
                return;
            }
            if (event.getClickCount() == 1 && hit != null && selectable(hit.series())) {
                Set<XYChart.Series> selected = focusedSeries(chart);
                if (!selected.add(hit.series())) selected.remove(hit.series());
                capturePopulationFocus(chart);
                applyVisualState(chart);
                event.consume();
            }
        });

        chart.getData().addListener((ListChangeListener<XYChart.Series>) change -> Platform.runLater(() -> {
            restorePopulationFocus(chart);
            applyVisualState(chart);
            installPopulationLockControl(chart);
            installPopulationExport(chart);
        }));

        Platform.runLater(() -> {
            restorePopulationFocus(chart);
            applyVisualState(chart);
            installPopulationLockControl(chart);
            installPopulationExport(chart);
        });
    }

    private static void installPopulationLockControl(LineChart<?, ?> chart) {
        if (chart == null || chart.getScene() == null) return;
        Parent searchRoot = ancestorWithStyle(chart, "population-chart-card");
        if (searchRoot == null) searchRoot = ancestorWithStyle(chart, "in-place-fullscreen");
        if (searchRoot == null || containsLockToggle(searchRoot)) return;

        HBox toolbar = findPopulationToolbar(searchRoot);
        if (toolbar == null) return;
        ToggleButton lock = new ToggleButton();
        bindPopulationLockToggle(lock);

        int insertAt = toolbar.getChildren().size();
        for (int index = 0; index < toolbar.getChildren().size(); index++) {
            Node child = toolbar.getChildren().get(index);
            if (child instanceof ButtonBase button) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                if (text.contains("3d") || text.contains("schermo intero") || text.contains("fullscreen")) {
                    insertAt = index;
                    break;
                }
            }
        }
        toolbar.getChildren().add(Math.max(0, Math.min(insertAt, toolbar.getChildren().size())), lock);
        chart.getProperties().put(LOCK_ADDED, Boolean.TRUE);
    }

    private static void installPopulationExport(LineChart<?, ?> chart) {
        if (chart == null || Boolean.TRUE.equals(chart.getProperties().get(CHART_EXPORT_DONE))) return;
        Parent searchRoot = ancestorWithStyle(chart, "population-chart-card");
        if (searchRoot == null) searchRoot = ancestorWithStyle(chart, "in-place-fullscreen");
        if (searchRoot == null) return;
        HBox toolbar = findPopulationToolbar(searchRoot);
        if (toolbar == null || containsExportPng(toolbar)) return;

        Button export = UiFactory.button("", "ghost-button");
        I18n.setText(export, "Esporta PNG", "Export PNG");
        String title = chart.getTitle() == null || chart.getTitle().isBlank() ? "population_chart" : chart.getTitle();
        export.setOnAction(event -> ExportSupport.exportPng(export, chart,
                title.replaceAll("[^A-Za-z0-9._-]+", "_") + "_2d.png"));

        int insertAt = toolbar.getChildren().size();
        for (int index = 0; index < toolbar.getChildren().size(); index++) {
            Node child = toolbar.getChildren().get(index);
            if (child instanceof ButtonBase button) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                if (text.contains("3d") || text.contains("schermo intero") || text.contains("fullscreen")) {
                    insertAt = index;
                    break;
                }
            }
        }
        toolbar.getChildren().add(Math.max(0, Math.min(insertAt, toolbar.getChildren().size())), export);
        chart.getProperties().put(CHART_EXPORT_DONE, Boolean.TRUE);
    }

    private static boolean containsExportPng(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof ButtonBase button) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                if (text.contains("export png") || text.contains("esporta png")) return true;
            }
            if (child instanceof Parent parent && containsExportPng(parent)) return true;
        }
        return false;
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
    private static SeriesHit nearest(LineChart chart, double mouseX, double mouseY,
                                     boolean focusedOnly, String spotlightOnly) {
        if (chart.getScene() == null) return null;
        Axis xAxis = chart.getXAxis();
        Axis yAxis = chart.getYAxis();
        Set<XYChart.Series> focus = focusedOnly ? focusedSeries(chart) : Set.of();
        double best = HIT_RADIUS * HIT_RADIUS;
        SeriesHit result = null;

        for (Object rawSeries : chart.getData()) {
            XYChart.Series series = (XYChart.Series) rawSeries;
            if (focusedOnly && !focus.contains(series)) continue;
            if (spotlightOnly != null && !spotlightOnly.equals(canonicalPopulationName(series.getName()))) continue;
            if (series.getData() == null || series.getData().isEmpty()) continue;

            XYChart.Data previousData = null;
            Point2D previousPoint = null;
            for (Object rawData : series.getData()) {
                XYChart.Data data = (XYChart.Data) rawData;
                Point2D point = chartPoint(chart, xAxis, yAxis, data);
                if (point == null) {
                    previousData = null;
                    previousPoint = null;
                    continue;
                }

                double pointDistance = squaredDistance(point.getX(), point.getY(), mouseX, mouseY);
                if (pointDistance < best) {
                    best = pointDistance;
                    result = new SeriesHit(series, data.getXValue(), data.getYValue());
                }

                if (previousPoint != null && previousData != null) {
                    SegmentProjection projection = project(mouseX, mouseY, previousPoint, point);
                    if (projection.distanceSquared() < best) {
                        best = projection.distanceSquared();
                        Object xValue = interpolateValue(previousData.getXValue(), data.getXValue(), projection.fraction());
                        Object yValue = interpolateValue(previousData.getYValue(), data.getYValue(), projection.fraction());
                        result = new SeriesHit(series, xValue, yValue);
                    }
                }
                previousData = data;
                previousPoint = point;
            }
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Point2D chartPoint(LineChart chart, Axis xAxis, Axis yAxis, XYChart.Data data) {
        Object xValue = data.getXValue();
        Object yValue = data.getYValue();
        if (xValue == null || yValue == null) return null;
        try {
            double xDisplay = xAxis.getDisplayPosition(xValue);
            double yDisplay = yAxis.getDisplayPosition(yValue);
            if (!Double.isFinite(xDisplay) || !Double.isFinite(yDisplay)) return null;
            Point2D xScene = xAxis.localToScene(xDisplay, 0);
            Point2D yScene = yAxis.localToScene(0, yDisplay);
            if (xScene == null || yScene == null) return null;
            return chart.sceneToLocal(xScene.getX(), yScene.getY());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static SegmentProjection project(double px, double py, Point2D a, Point2D b) {
        double vx = b.getX() - a.getX();
        double vy = b.getY() - a.getY();
        double lengthSquared = vx * vx + vy * vy;
        if (lengthSquared < 1e-9) {
            return new SegmentProjection(0, squaredDistance(a.getX(), a.getY(), px, py));
        }
        double fraction = ((px - a.getX()) * vx + (py - a.getY()) * vy) / lengthSquared;
        fraction = Math.max(0, Math.min(1, fraction));
        double x = a.getX() + fraction * vx;
        double y = a.getY() + fraction * vy;
        return new SegmentProjection(fraction, squaredDistance(x, y, px, py));
    }

    private static double squaredDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private static Object interpolateValue(Object first, Object second, double fraction) {
        if (first instanceof Number a && second instanceof Number b) {
            return a.doubleValue() + (b.doubleValue() - a.doubleValue()) * fraction;
        }
        return fraction < 0.5 ? first : second;
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
        boolean locked = populationFocusLocked();
        String spotlight = locked ? populationSpotlightName() : null;

        for (Object raw : chart.getData()) {
            XYChart.Series series = (XYChart.Series) raw;
            Node node = series.getNode();
            if (node == null) continue;
            String name = series.getName();
            boolean inFocus = focus.isEmpty() || focus.contains(series);
            boolean active = inFocus || (!locked && series == hovered);
            node.setOpacity(active ? 1.0 : isTrigger(name) ? 0.50 : 0.07);
            applySpotlightStyle(node, name, spotlight != null && spotlight.equals(canonicalPopulationName(name)));
        }
    }

    private static void applySpotlightStyle(Node seriesNode, String name, boolean spotlight) {
        Node line = seriesNode.lookup(".chart-series-line");
        if (line == null) line = seriesNode;
        if (spotlight) {
            if (!line.getProperties().containsKey(SPOTLIGHT_STYLE)) {
                line.getProperties().put(SPOTLIGHT_STYLE, line.getStyle() == null ? "" : line.getStyle());
            }
            String base = String.valueOf(line.getProperties().get(SPOTLIGHT_STYLE));
            line.setStyle(base + "; -fx-stroke: " + spotlightColor(name) + "; -fx-stroke-width: 3.4px;");
            line.setEffect(new Glow(0.88));
            seriesNode.toFront();
        } else if (line.getProperties().containsKey(SPOTLIGHT_STYLE)) {
            Object original = line.getProperties().remove(SPOTLIGHT_STYLE);
            line.setStyle(original == null ? "" : original.toString());
            line.setEffect(null);
        }
    }

    private static String spotlightColor(String name) {
        if (name == null) return "#66E4FF";
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.equals("mediana") || lower.equals("median")) return "#FFBE62";
        if (lower.contains("percentile")) return "#C79BFF";
        return "#66E4FF";
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
        focus.clear();
        if (desired.isEmpty()) return;
        for (Object raw : chart.getData()) {
            XYChart.Series series = (XYChart.Series) raw;
            String name = canonicalPopulationName(series.getName());
            if (name != null && desired.contains(name)) focus.add(series);
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
            restorePopulationFocus(chart);
            chart.getProperties().remove(HOVER);
            applyVisualState(chart);
            installPopulationLockControl(chart);
            installPopulationExport(chart);
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) refreshPopulationNode(child);
        }
    }

    private static boolean openThreeD(Node source) {
        Node current = source;
        for (int depth = 0; current != null && depth < 12; depth++, current = current.getParent()) {
            if (current instanceof Parent parent) {
                Button local = findThreeDButton(parent);
                if (local != null) {
                    local.fire();
                    return true;
                }
            }
            if (current.getStyleClass().contains("page-root")) break;
        }
        return false;
    }

    private static Button findThreeDButton(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.isVisible() && button.isManaged() && !button.isDisabled()) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                if (text.contains("3d")) return button;
            }
            if (child instanceof Parent parent) {
                Button nested = findThreeDButton(parent);
                if (nested != null) return nested;
            }
        }
        return null;
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
        return value == null ? "—" : String.valueOf(value);
    }

    @SuppressWarnings("rawtypes")
    private record SeriesHit(XYChart.Series series, Object xValue, Object yValue) { }
    private record SegmentProjection(double fraction, double distanceSquared) { }
}
