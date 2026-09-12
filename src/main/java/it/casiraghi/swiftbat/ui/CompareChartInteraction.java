package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.Locale;
import java.util.Objects;

/** Single-curve inspection shared by embedded and fullscreen Compare charts. */
final class CompareChartInteraction {
    record Sample(double rate, double error, double fracexp) { }
    private record Hit(XYChart.Series<Number, Number> series, XYChart.Data<Number, Number> point) { }

    private final LineChart<Number, Number> chart;
    private final StringProperty selected;
    private final Tooltip tooltip = new Tooltip();
    private final ChangeListener<String> selectionListener = (obs, before, after) -> {
        tooltip.hide();
        applyAppearance();
    };
    private XYChart.Series<Number, Number> hovered;

    static void install(LineChart<Number, Number> chart, StringProperty selected) {
        chart.getProperties().computeIfAbsent(CompareChartInteraction.class.getName(),
                key -> new CompareChartInteraction(chart, selected));
    }

    private CompareChartInteraction(LineChart<Number, Number> chart, StringProperty selected) {
        this.chart = chart;
        this.selected = selected;
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setHideDelay(Duration.ZERO);
        tooltip.setAutoHide(false);
        // The shared selection must not retain discarded fullscreen charts.
        selected.addListener(new WeakChangeListener<>(selectionListener));
        chart.addEventHandler(MouseEvent.MOUSE_MOVED, this::inspect);
        chart.addEventHandler(MouseEvent.MOUSE_EXITED, event -> clearHover());
        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || !event.isStillSincePress()) return;
            if (event.getClickCount() != 1) { event.consume(); return; }
            Hit hit = nearest(event);
            if (hit == null) return;
            String name = hit.series().getName();
            selected.set(Objects.equals(selected.get(), name) ? null : name);
            inspect(event);
            event.consume();
        });
        chart.sceneProperty().addListener((obs, before, after) -> clearHover());
        for (var series : chart.getData()) {
            series.nodeProperty().addListener((obs, before, after) -> applyAppearance());
        }
        Platform.runLater(this::applyAppearance);
    }

    private void clearHover() {
        hovered = null;
        tooltip.hide();
        chart.setCursor(Cursor.DEFAULT);
        applyAppearance();
    }

    private void inspect(MouseEvent event) {
        Hit hit = nearest(event);
        hovered = hit == null ? null : hit.series();
        chart.setCursor(hit == null ? Cursor.DEFAULT : Cursor.HAND);
        applyAppearance();
        if (hit == null || (selected.get() != null && !selected.get().equals(hit.series().getName()))) {
            tooltip.hide();
            return;
        }
        tooltip.setText(describe(hit));
        if (chart.getScene() == null || chart.getScene().getWindow() == null
                || !chart.getScene().getWindow().isShowing()) return;
        if (!tooltip.isShowing()) tooltip.show(chart, event.getScreenX() + 14, event.getScreenY() + 14);
        else {
            tooltip.setAnchorX(event.getScreenX() + 14);
            tooltip.setAnchorY(event.getScreenY() + 14);
        }
    }

    private String describe(Hit hit) {
        var point = hit.point();
        String text = hit.series().getName() + "\n" + chart.getXAxis().getLabel() + ": "
                + number(point.getXValue().doubleValue()) + "\n" + chart.getYAxis().getLabel() + ": "
                + number(point.getYValue().doubleValue());
        if (point.getExtraValue() instanceof Sample sample) {
            if (Boolean.TRUE.equals(chart.getProperties().get("compare.normalized"))) {
                text += "\n" + I18n.dynamic("Rate originale", "Original rate") + ": "
                        + number(sample.rate()) + " count/s";
            }
            if (Double.isFinite(sample.error())) {
                text += "\n" + I18n.dynamic("Errore sul rate originale", "Original rate error")
                        + ": ±" + number(sample.error()) + " count/s";
            }
            if (Double.isFinite(sample.fracexp())) text += "\nFRACEXP: " + number(sample.fracexp());
        }
        return text;
    }

    private static String number(double value) {
        return String.format(I18n.language() == I18n.Language.IT ? Locale.ITALY : Locale.US, "%.6g", value);
    }

    private void applyAppearance() {
        for (var series : chart.getData()) {
            Node node = series.getNode();
            if (node == null) continue;
            boolean active = Objects.equals(selected.get(), series.getName());
            boolean hover = series == hovered;
            node.setOpacity(selected.get() == null || active || hover ? 1.0 : 0.25);
            node.setStyle("-fx-stroke-width: " + (active ? 4.5 : hover ? 3.8 : 2.5) + "px;");
        }
    }

    /** Hit-test segments as well as points; tooltips always report a measured sample. */
    private Hit nearest(MouseEvent event) {
        Node plot = chart.lookup(".chart-plot-background");
        if (plot == null || !plot.contains(plot.sceneToLocal(event.getSceneX(), event.getSceneY()))) return null;
        Point2D mouse = chart.sceneToLocal(event.getSceneX(), event.getSceneY());
        double best = 12.0 * 12.0;
        Hit hit = null;
        for (var series : chart.getData()) {
            Point2D previous = null;
            XYChart.Data<Number, Number> previousData = null;
            for (var data : series.getData()) {
                double x = chart.getXAxis().getDisplayPosition(data.getXValue());
                double y = chart.getYAxis().getDisplayPosition(data.getYValue());
                if (!Double.isFinite(x) || !Double.isFinite(y)) { previous = null; continue; }
                Point2D xScene = chart.getXAxis().localToScene(x, 0);
                Point2D yScene = chart.getYAxis().localToScene(0, y);
                Point2D current = chart.sceneToLocal(xScene.getX(), yScene.getY());
                double distance = squared(mouse, current);
                XYChart.Data<Number, Number> sample = data;
                if (previous != null) {
                    Point2D segment = current.subtract(previous);
                    double length = segment.dotProduct(segment);
                    double fraction = length == 0 ? 1 : Math.max(0, Math.min(1,
                            mouse.subtract(previous).dotProduct(segment) / length));
                    distance = squared(mouse, previous.add(segment.multiply(fraction)));
                    if (fraction < 0.5) sample = previousData;
                }
                if (distance < best) { best = distance; hit = new Hit(series, sample); }
                previous = current;
                previousData = data;
            }
        }
        return hit;
    }

    private static double squared(Point2D a, Point2D b) {
        double dx = a.getX() - b.getX(), dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }
}
