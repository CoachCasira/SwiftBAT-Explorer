package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canvas renderer for dense Population Analysis samples.
 *
 * LineChart still owns the axes, title, median, quartiles and the same
 * interactive series identities. Its GRB series are lightweight proxies
 * containing only the original extrema needed for accurate autoranging.
 * The complete scientific samples remain intact here and are never resampled.
 *
 * The base canvas is painted only when samples, axes or selection change;
 * hover paints just a single individual GRB on a separate transparent canvas.
 */
public final class PopulationCanvasRenderer {
    public static final int MIN_CANVAS_CURVES = 50;
    private static final String KEY = PopulationCanvasRenderer.class.getName() + ".layer";
    private static final Color CYAN = Color.rgb(84, 215, 255);
    private static final Color EMPHASIS = Color.rgb(102, 228, 255);

    private PopulationCanvasRenderer() { }

    public static boolean enabled(LineChart<Number, Number> chart) {
        Layer layer = layer(chart);
        return layer != null && layer.enabled;
    }

    /**
     * Each proxy is a normal JavaFX series retaining its original name.
     * The two (real) extrema preserve the y-axis autorange without the cost
     * of 100 x 121 JavaFX data nodes. They are never displayed as a line.
     */
    public static XYChart.Series<Number, Number> proxy(
            String name, List<? extends XYChart.Data<Number, Number>> samples) {
        XYChart.Series<Number, Number> result = new XYChart.Series<>();
        result.setName(name);
        XYChart.Data<Number, Number> minimum = null;
        XYChart.Data<Number, Number> maximum = null;
        for (XYChart.Data<Number, Number> sample : samples) {
            if (!(sample.getYValue() instanceof Number y) || !Double.isFinite(y.doubleValue())) continue;
            if (minimum == null || y.doubleValue() < minimum.getYValue().doubleValue()) minimum = sample;
            if (maximum == null || y.doubleValue() > maximum.getYValue().doubleValue()) maximum = sample;
        }
        if (minimum != null) {
            result.getData().add(new XYChart.Data<>(minimum.getXValue(), minimum.getYValue()));
            if (maximum != null && maximum != minimum) {
                result.getData().add(new XYChart.Data<>(maximum.getXValue(), maximum.getYValue()));
            }
        }
        return result;
    }

    public static void install(LineChart<Number, Number> chart,
                               Map<XYChart.Series<Number, Number>,
                                   List<XYChart.Data<Number, Number>>> scientificSamples) {
        Layer layer = layer(chart);
        if (layer == null) {
            layer = new Layer(chart);
            chart.getProperties().put(KEY, layer);
        }
        layer.setSamples(scientificSamples);
    }

    /** Clear a previous dense dataset when switching back to a small sample. */
    public static void clear(LineChart<Number, Number> chart) {
        Layer layer = layer(chart);
        if (layer != null) layer.disable();
    }

    /**
     * Used by the existing hit-testing service; never returns the lightweight
     * proxy points if full scientific data are available.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<XYChart.Data> samples(LineChart chart, XYChart.Series series) {
        Layer layer = layer(chart);
        if (layer != null && layer.enabled) {
            List<XYChart.Data<Number, Number>> points = layer.samples.get(series);
            if (points != null) return (List) points;
        }
        return series.getData();
    }

    /** Preserve original values and series identities in the fullscreen copy. */
    public static List<XYChart.Data<Number, Number>> samplesForCopy(
            LineChart<Number, Number> chart, XYChart.Series<Number, Number> series) {
        Layer layer = layer(chart);
        if (layer != null && layer.enabled && layer.samples.containsKey(series)) {
            return layer.samples.get(series);
        }
        return List.copyOf(series.getData());
    }

    @SuppressWarnings("rawtypes")
    public static boolean visualState(LineChart chart, Set<XYChart.Series> selection,
                                      XYChart.Series hovered, String spotlight,
                                      boolean locked) {
        Layer layer = layer(chart);
        if (layer == null || !layer.enabled) return false;
        layer.visualState(selection, hovered, spotlight, locked);
        return true;
    }

    private static Layer layer(LineChart<?, ?> chart) {
        Object value = chart.getProperties().get(KEY);
        return value instanceof Layer layer ? layer : null;
    }

    private static final class Layer {
        private final LineChart<Number, Number> chart;
        private final Canvas base = new Canvas();
        private final Canvas highlight = new Canvas();
        private Group mounted;
        private boolean enabled;
        private boolean dirtyBase = true;
        private boolean pending;
        private final Map<XYChart.Series<Number, Number>,
                          List<XYChart.Data<Number, Number>>> samples = new LinkedHashMap<>();
        private Set<XYChart.Series<Number, Number>> selected = Set.of();
        private XYChart.Series<Number, Number> hovered;
        private String spotlight;
        private boolean locked;

        private Layer(LineChart<Number, Number> chart) {
            this.chart = chart;
            base.setManaged(false);
            highlight.setManaged(false);
            base.setMouseTransparent(true);
            highlight.setMouseTransparent(true);

            ChangeListener<Number> changed = (obs, oldValue, newValue) -> {
                dirtyBase = true;
                request();
            };
            chart.widthProperty().addListener(changed);
            chart.heightProperty().addListener(changed);
            chart.getXAxis().widthProperty().addListener(changed);
            chart.getYAxis().heightProperty().addListener(changed);
            if (chart.getXAxis() instanceof NumberAxis x) {
                x.lowerBoundProperty().addListener(changed);
                x.upperBoundProperty().addListener(changed);
            }
            if (chart.getYAxis() instanceof NumberAxis y) {
                y.lowerBoundProperty().addListener(changed);
                y.upperBoundProperty().addListener(changed);
            }
            chart.sceneProperty().addListener((obs, before, after) -> request());
            chart.skinProperty().addListener((obs, before, after) -> request());
            chart.layoutBoundsProperty().addListener((obs, before, after) -> request());
        }

        private void setSamples(Map<XYChart.Series<Number, Number>,
                                    List<XYChart.Data<Number, Number>>> data) {
            samples.clear();
            for (var entry : data.entrySet()) {
                samples.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            enabled = true;
            selected = Set.of();
            hovered = null;
            spotlight = null;
            locked = false;
            dirtyBase = true;
            request();
        }

        private void disable() {
            enabled = false;
            samples.clear();
            selected = Set.of();
            hovered = null;
            if (mounted != null) mounted.getChildren().removeAll(base, highlight);
            mounted = null;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void visualState(Set<XYChart.Series> focus, XYChart.Series hover,
                                 String newSpotlight, boolean isLocked) {
            Set<XYChart.Series<Number, Number>> next = new LinkedHashSet<>();
            for (XYChart.Series raw : focus) next.add(raw);
            if (!selected.equals(next) || locked != isLocked
                    || !java.util.Objects.equals(spotlight, newSpotlight)) {
                selected = Collections.unmodifiableSet(next);
                locked = isLocked;
                spotlight = newSpotlight;
                dirtyBase = true;
            }
            if (hovered != hover) hovered = hover;
            request();
        }

        private void request() {
            if (!enabled || pending) return;
            pending = true;
            Platform.runLater(() -> {
                pending = false;
                if (!enabled || chart.getScene() == null) return;
                if (!mount()) return;
                resize();
                if (dirtyBase) {
                    renderBase();
                    dirtyBase = false;
                }
                renderHighlight();
            });
        }

        private boolean mount() {
            Node node = chart.lookup(".plot-content");
            if (!(node instanceof Group target)) return false;
            if (mounted != target) {
                if (mounted != null) mounted.getChildren().removeAll(base, highlight);
                mounted = target;
                mounted.getChildren().add(0, base);
                mounted.getChildren().add(1, highlight);
                dirtyBase = true;
            }
            return true;
        }

        private void resize() {
            double width = Math.max(0, chart.getXAxis().getWidth());
            double height = Math.max(0, chart.getYAxis().getHeight());
            if (base.getWidth() != width || base.getHeight() != height) {
                base.setWidth(width);
                base.setHeight(height);
                highlight.setWidth(width);
                highlight.setHeight(height);
                dirtyBase = true;
            }
        }

        private void renderBase() {
            GraphicsContext g = base.getGraphicsContext2D();
            g.clearRect(0, 0, base.getWidth(), base.getHeight());
            for (var entry : samples.entrySet()) {
                XYChart.Series<Number, Number> series = entry.getKey();
                boolean inFocus = selected.isEmpty() || selected.contains(series);
                boolean isSpotlight = locked && spotlight != null && spotlight.equals(series.getName());
                if (isSpotlight) {
                    draw(g, entry.getValue(), EMPHASIS, 1.0, 3.4);
                } else {
                    // Same 20% cyan used by the original population series.
                    draw(g, entry.getValue(), CYAN, inFocus ? 0.20 : 0.014, 1.0);
                }
            }
        }

        private void renderHighlight() {
            GraphicsContext g = highlight.getGraphicsContext2D();
            g.clearRect(0, 0, highlight.getWidth(), highlight.getHeight());
            if (hovered == null || (locked && spotlight != null)) return;
            List<XYChart.Data<Number, Number>> points = samples.get(hovered);
            if (points == null) return;
            if (!selected.isEmpty() && !selected.contains(hovered)) return;
            draw(g, points, EMPHASIS, 0.88, 2.0);
        }

        private void draw(GraphicsContext g, List<XYChart.Data<Number, Number>> points,
                          Color color, double opacity, double strokeWidth) {
            if (points.size() < 2) return;
            g.setStroke(color);
            g.setGlobalAlpha(opacity);
            g.setLineWidth(strokeWidth);
            g.beginPath();
            boolean contiguous = false;
            for (XYChart.Data<Number, Number> point : points) {
                Number time = point.getXValue();
                Number rate = point.getYValue();
                if (time == null || rate == null
                        || !Double.isFinite(time.doubleValue()) || !Double.isFinite(rate.doubleValue())) {
                    contiguous = false;
                    continue;
                }
                double x = chart.getXAxis().getDisplayPosition(time);
                double y = chart.getYAxis().getDisplayPosition(rate);
                if (!Double.isFinite(x) || !Double.isFinite(y)) {
                    contiguous = false;
                    continue;
                }
                if (contiguous) g.lineTo(x, y);
                else { g.moveTo(x, y); contiguous = true; }
            }
            g.stroke();
            g.setGlobalAlpha(1.0);
        }
    }
}
