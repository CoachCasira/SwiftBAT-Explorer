package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ValueAxis;
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
    private static final String HIT_GEOMETRY = CurveInteractionLinkEnhancer.class.getName() + ".hitGeometry";
    private static final String LOCK_DONE = CurveInteractionLinkEnhancer.class.getName() + ".lockDone";
    private static final String LOCK_SYNC = CurveInteractionLinkEnhancer.class.getName() + ".lockSync";
    private static final String LOCK_ADDED = CurveInteractionLinkEnhancer.class.getName() + ".lockAdded";
    private static final String SPOTLIGHT_STYLE = CurveInteractionLinkEnhancer.class.getName() + ".spotlightStyle";
    private static final String SPOTLIGHT_ACTIVE = CurveInteractionLinkEnhancer.class.getName() + ".spotlightActive";
    private static final String CHART_FOCUS = ChartInteractionEnhancer.class.getName() + ".focus";
    private static final String CHART_LINE_DONE = ChartInteractionEnhancer.class.getName() + ".lineDone";
    private static final String CHART_EXPORT_DONE = ChartInteractionEnhancer.class.getName() + ".exportDone";
    private static final double HIT_RADIUS = 18.0;
    private static final DecimalFormat NUMBER_FORMAT;
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final LinkedHashSet<String> POPULATION_FOCUS = new LinkedHashSet<>();
    private static final Set<ToggleButton> POPULATION_LOCK_TOGGLES = Collections.newSetFromMap(new WeakHashMap<>());
    // Weak registry: selection refreshes only actual Population charts, not the
    // full application scene graph (including thousands of chart descendants).
    private static final Set<LineChart<?, ?>> POPULATION_CHARTS = Collections.newSetFromMap(new WeakHashMap<>());

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
        POPULATION_CHARTS.add(rawChart);
        if (Boolean.TRUE.equals(rawChart.getProperties().get(LINE_DONE))) {
            Platform.runLater(() -> {
                installPopulationLockControl(rawChart);
                installPopulationExport(rawChart);
            });
            return;
        }
        rawChart.getProperties().put(LINE_DONE, Boolean.TRUE);
        LineChart chart = rawChart;
        POPULATION_CHARTS.add(rawChart);

        Tooltip tooltip = new Tooltip();
        tooltip.setAutoHide(false);
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setHideDelay(Duration.ZERO);

        final long[] previousHoverFrameNanos = {0L};
        chart.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            // Cap tooltip hit testing to about 30 Hz for 100+ displayed curves.
            // Clicks still use exact unthrottled hit testing.
            long now = System.nanoTime();
            if (now - previousHoverFrameNanos[0] < 33_000_000L) return;
            previousHoverFrameNanos[0] = now;
            boolean locked = populationFocusLocked();
            String spotlight = locked ? populationSpotlightName() : null;
            SeriesHit hit = nearest(chart, event.getX(), event.getY(), locked, spotlight);
            Set<XYChart.Series> focus = focusedSeries(chart);
            XYChart.Series hovered = hit == null || focus.isEmpty() ? null : hit.series();
            if (chart.getProperties().get(HOVER) != hovered) {
                chart.getProperties().put(HOVER, hovered);
                applyVisualState(chart);
            }

            if (hit == null) {
                tooltip.hide();
                return;
            }
            String seriesName = hit.series().getName() == null ? I18n.dynamic("Curva", "Curve") : hit.series().getName();
            String xLabel = chart.getXAxis().getLabel();
            String yLabel = chart.getYAxis().getLabel();
            String description = seriesName + "\n"
                    + (xLabel == null || xLabel.isBlank() ? "X" : xLabel) + ": " + format(hit.xValue()) + "\n"
                    + (yLabel == null || yLabel.isBlank() ? "Y" : yLabel) + ": " + format(hit.yValue());
            if (!description.equals(tooltip.getText())) tooltip.setText(description);
            double tipX = event.getScreenX() + 14;
            double tipY = event.getScreenY() + 14;
            if (!tooltip.isShowing()) {
                tooltip.show(chart, tipX, tipY);
            } else {
                if (Math.abs(tooltip.getAnchorX() - tipX) > 3) tooltip.setAnchorX(tipX);
                if (Math.abs(tooltip.getAnchorY() - tipY) > 3) tooltip.setAnchorY(tipY);
            }
        });

        chart.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            if (chart.getProperties().remove(HOVER) != null) applyVisualState(chart);
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
            chart.getProperties().remove(HIT_GEOMETRY);
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

    private record CachedCurve(XYChart.Series series, List<XYChart.Data> source,
                               double[] x, double[] y, boolean ordered) { }

    private static final class HitGeometry {
        private final CoordinateMapper mapper;
        private final double xLow, xHigh, yLow, yHigh;
        private final double xWidth, yHeight;
        private final List<CachedCurve> curves;

        @SuppressWarnings({"rawtypes", "unchecked"})
        private HitGeometry(LineChart chart, CoordinateMapper transform) {
            this.mapper = transform;
            Axis xAxis = chart.getXAxis();
            Axis yAxis = chart.getYAxis();
            xLow = lowerBound(xAxis);
            xHigh = upperBound(xAxis);
            yLow = lowerBound(yAxis);
            yHigh = upperBound(yAxis);
            xWidth = xAxis.getWidth();
            yHeight = yAxis.getHeight();
            curves = new java.util.ArrayList<>(chart.getData().size());
            for (Object raw : chart.getData()) {
                XYChart.Series series = (XYChart.Series) raw;
                List<XYChart.Data> data = series.getData();
                if (data == null || data.isEmpty()) continue;
                double[] xp = new double[data.size()];
                double[] yp = new double[data.size()];
                boolean ordered = true;
                double prevTime = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < data.size(); i++) {
                    XYChart.Data point = data.get(i);
                    Object rawTime = point.getXValue();
                    if (!(rawTime instanceof Number time) || !Double.isFinite(time.doubleValue())
                            || time.doubleValue() < prevTime) ordered = false;
                    else prevTime = time.doubleValue();
                    Point2D projected = chartPoint(chart, xAxis, yAxis, point, transform);
                    xp[i] = projected == null ? Double.NaN : projected.getX();
                    yp[i] = projected == null ? Double.NaN : projected.getY();
                }
                curves.add(new CachedCurve(series, data, xp, yp, ordered));
            }
        }

        @SuppressWarnings("rawtypes")
        private boolean matches(LineChart chart, CoordinateMapper transform) {
            return java.util.Objects.equals(mapper, transform)
                    && Double.compare(xLow, lowerBound(chart.getXAxis())) == 0
                    && Double.compare(xHigh, upperBound(chart.getXAxis())) == 0
                    && Double.compare(yLow, lowerBound(chart.getYAxis())) == 0
                    && Double.compare(yHigh, upperBound(chart.getYAxis())) == 0
                    && Double.compare(xWidth, chart.getXAxis().getWidth()) == 0
                    && Double.compare(yHeight, chart.getYAxis().getHeight()) == 0
                    && curves.size() == chart.getData().size();
        }
    }

    private static double lowerBound(Axis<?> axis) {
        return axis instanceof ValueAxis<?> value ? value.getLowerBound() : Double.NaN;
    }

    private static double upperBound(Axis<?> axis) {
        return axis instanceof ValueAxis<?> value ? value.getUpperBound() : Double.NaN;
    }

    @SuppressWarnings("rawtypes")
    private static HitGeometry hitGeometry(LineChart chart, CoordinateMapper mapper) {
        Object raw = chart.getProperties().get(HIT_GEOMETRY);
        if (raw instanceof HitGeometry old && old.matches(chart, mapper)) return old;
        HitGeometry rebuilt = new HitGeometry(chart, mapper);
        chart.getProperties().put(HIT_GEOMETRY, rebuilt);
        return rebuilt;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SeriesHit nearest(LineChart chart, double mouseX, double mouseY,
                                     boolean focusedOnly, String spotlightOnly) {
        if (chart.getScene() == null) return null;
        Axis xAxis = chart.getXAxis();
        Axis yAxis = chart.getYAxis();
        Set<XYChart.Series> focus = focusedOnly ? focusedSeries(chart) : Set.of();
        CoordinateMapper mapper = coordinateMapper(chart, xAxis, yAxis);
        HitGeometry geometry = hitGeometry(chart, mapper);
        double best = HIT_RADIUS * HIT_RADIUS;
        SeriesHit result = null;

        double leftX = Double.NaN;
        double rightX = Double.NaN;
        try {
            Point2D scene = chart.localToScene(mouseX, mouseY);
            Point2D axisLocal = xAxis.sceneToLocal(scene);
            Object left = xAxis.getValueForDisplay(axisLocal.getX() - HIT_RADIUS);
            Object right = xAxis.getValueForDisplay(axisLocal.getX() + HIT_RADIUS);
            if (left instanceof Number low && right instanceof Number high
                    && Double.isFinite(low.doubleValue()) && Double.isFinite(high.doubleValue())) {
                leftX = Math.min(low.doubleValue(), high.doubleValue());
                rightX = Math.max(low.doubleValue(), high.doubleValue());
            }
        } catch (RuntimeException ignored) {
            // A chart in its initial layout may not expose stable axis transforms.
        }

        // Pixel coordinates are computed only when a chart is created, resized,
        // reranged or repopulated; ordinary hover does not reproject every GRB.
        for (CachedCurve curve : geometry.curves) {
            XYChart.Series series = curve.series();
            if (focusedOnly && !focus.contains(series)) continue;
            if (spotlightOnly != null && !spotlightOnly.equals(canonicalPopulationName(series.getName()))) continue;

            List<XYChart.Data> points = curve.source();
            int from = 0, until = points.size();
            if (curve.ordered() && Double.isFinite(leftX) && Double.isFinite(rightX)) {
                int lower = timeBound(points, leftX, false);
                int upper = timeBound(points, rightX, true);
                if (lower >= 0 && upper >= 0) {
                    from = Math.max(0, lower - 1);
                    until = Math.min(points.size(), upper + 1);
                }
            }
            int prevIndex = -1;
            for (int index = from; index < until; index++) {
                double px = curve.x()[index], py = curve.y()[index];
                if (!Double.isFinite(px) || !Double.isFinite(py)) {
                    prevIndex = -1;
                    continue;
                }
                double dx = px - mouseX, dy = py - mouseY;
                double pointDistance = dx * dx + dy * dy;
                if (pointDistance < best) {
                    best = pointDistance;
                    XYChart.Data point = points.get(index);
                    result = new SeriesHit(series, point.getXValue(), point.getYValue());
                }
                if (prevIndex >= 0) {
                    double ax = curve.x()[prevIndex], ay = curve.y()[prevIndex];
                    double vx = px - ax, vy = py - ay;
                    double lengthSquared = vx * vx + vy * vy;
                    double fraction = lengthSquared < 1e-12 ? 0
                            : Math.max(0, Math.min(1, ((mouseX - ax) * vx + (mouseY - ay) * vy) / lengthSquared));
                    double ex = ax + fraction * vx - mouseX, ey = ay + fraction * vy - mouseY;
                    double segmentDistance = ex * ex + ey * ey;
                    if (segmentDistance < best) {
                        best = segmentDistance;
                        XYChart.Data left = points.get(prevIndex), right = points.get(index);
                        result = new SeriesHit(series,
                                interpolateValue(left.getXValue(), right.getXValue(), fraction),
                                interpolateValue(left.getYValue(), right.getYValue(), fraction));
                    }
                }
                prevIndex = index;
            }
        }
        return result;
    }

    /** Returns -1 for a nonnumeric time axis, preserving the safe full-scan fallback. */
    private static int timeBound(List<XYChart.Data> points, double time, boolean upperInclusive) {
        int low = 0;
        int high = points.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            Object value = points.get(middle).getXValue();
            if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue())) return -1;
            double current = n.doubleValue();
            if (current < time || (upperInclusive && current == time)) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    /**
     * A single affine coordinate map per mouse event replaces thousands of
     * localToScene/sceneToLocal calls when many GRBs share the same time window.
     * This reproduces the original chartPoint transformation, including HiDPI
     * scale, because both axes and the chart use affine scene transforms.
     */
    private record CoordinateMapper(double x0, double y0,
                                    double xx, double xy, double yx, double yy) {
        Point2D point(double x, double y) {
            return new Point2D(x0 + xx * x + yx * y, y0 + xy * x + yy * y);
        }
    }

    @SuppressWarnings("rawtypes")
    private static CoordinateMapper coordinateMapper(LineChart chart, Axis xAxis, Axis yAxis) {
        try {
            Point2D x0 = xAxis.localToScene(0, 0);
            Point2D x1 = xAxis.localToScene(1, 0);
            Point2D y0 = yAxis.localToScene(0, 0);
            Point2D y1 = yAxis.localToScene(0, 1);
            if (x0 == null || x1 == null || y0 == null || y1 == null) return null;
            Point2D base = chart.sceneToLocal(x0.getX(), y0.getY());
            Point2D xUnit = chart.sceneToLocal(x1.getX(), y0.getY());
            Point2D yUnit = chart.sceneToLocal(x0.getX(), y1.getY());
            if (base == null || xUnit == null || yUnit == null) return null;
            return new CoordinateMapper(base.getX(), base.getY(),
                    xUnit.getX() - base.getX(), xUnit.getY() - base.getY(),
                    yUnit.getX() - base.getX(), yUnit.getY() - base.getY());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Point2D chartPoint(LineChart chart, Axis xAxis, Axis yAxis,
                                      XYChart.Data data, CoordinateMapper mapper) {
        Object xValue = data.getXValue();
        Object yValue = data.getYValue();
        if (xValue == null || yValue == null) return null;
        try {
            double xDisplay = xAxis.getDisplayPosition(xValue);
            double yDisplay = yAxis.getDisplayPosition(yValue);
            if (!Double.isFinite(xDisplay) || !Double.isFinite(yDisplay)) return null;
            if (mapper != null) return mapper.point(xDisplay, yDisplay);
            // Fallback for a chart undergoing initial layout.
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
            // Hit testing a JavaFX Path with hundreds of segments for every
            // mouse motion is expensive on macOS. Population uses its own
            // analytical nearest-curve tester, so JavaFX must not pick paths.
            if (!node.isMouseTransparent()) node.setMouseTransparent(true);
            String name = series.getName();
            boolean inFocus = focus.isEmpty() || focus.contains(series);
            boolean active = inFocus || (!locked && series == hovered);
            double desiredOpacity = active ? 1.0 : isTrigger(name) ? 0.50 : 0.07;
            if (Double.compare(node.getOpacity(), desiredOpacity) != 0) node.setOpacity(desiredOpacity);
            applySpotlightStyle(node, name, spotlight != null && spotlight.equals(canonicalPopulationName(name)));
        }
    }

    private static void applySpotlightStyle(Node seriesNode, String name, boolean spotlight) {
        boolean wasActive = Boolean.TRUE.equals(seriesNode.getProperties().get(SPOTLIGHT_ACTIVE));
        if (wasActive == spotlight) return; // No CSS lookup for unchanged styles.
        Node line = seriesNode.lookup(".chart-series-line");
        if (line == null) line = seriesNode;
        if (spotlight) {
            line.getProperties().put(SPOTLIGHT_STYLE, line.getStyle() == null ? "" : line.getStyle());
            String base = String.valueOf(line.getProperties().get(SPOTLIGHT_STYLE));
            line.setStyle(base + "; -fx-stroke: " + spotlightColor(name) + "; -fx-stroke-width: 3.4px;");
            line.setEffect(new Glow(0.88));
            seriesNode.getProperties().put(SPOTLIGHT_ACTIVE, Boolean.TRUE);
            seriesNode.toFront();
        } else {
            Object original = line.getProperties().remove(SPOTLIGHT_STYLE);
            if (original != null) line.setStyle(original.toString());
            line.setEffect(null);
            seriesNode.getProperties().remove(SPOTLIGHT_ACTIVE);
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
        // Global subtree traversal was O(all scene nodes) for every selection,
        // often repeated on the fullscreen root. Only mounted charts need a
        // visual update; the charts register when their interaction is installed.
        for (LineChart<?, ?> registered : List.copyOf(POPULATION_CHARTS)) {
            if (registered == null || registered.getScene() == null) continue;
            @SuppressWarnings("rawtypes") LineChart chart = registered;
            restorePopulationFocus(chart);
            chart.getProperties().remove(HOVER);
            applyVisualState(chart);
            installPopulationLockControl(chart);
            installPopulationExport(chart);
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
