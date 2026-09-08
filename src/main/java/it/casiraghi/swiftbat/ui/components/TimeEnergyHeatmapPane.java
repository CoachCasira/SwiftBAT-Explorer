package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.model.TabularData;
import it.casiraghi.swiftbat.ui.UiFactory;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Heatmap descrittiva tempo-energia costruita dai quattro rate ASCII a 1 s. */
public final class TimeEnergyHeatmapPane extends Region {
    private static final List<Band> BANDS = List.of(
            new Band("100–350 keV", "RATE_100_350_KEV"),
            new Band("50–100 keV", "RATE_50_100_KEV"),
            new Band("25–50 keV", "RATE_25_50_KEV"),
            new Band("15–25 keV", "RATE_15_25_KEV"));

    private final Canvas canvas = new Canvas();
    private final Tooltip tooltip = UiFactory.quickTooltip("");
    private TabularData data = TabularData.empty();
    private double halfWindowSeconds = 60.0;
    private List<Row> visibleRows = List.of();
    private double plotLeft;
    private double plotTop;
    private double plotWidth;
    private double plotHeight;
    private double minimumTime;
    private double maximumTime;

    public TimeEnergyHeatmapPane() {
        getChildren().add(canvas);
        setMinHeight(300);
        setPrefHeight(390);
        Tooltip.install(this, tooltip);
        widthProperty().addListener(ignored -> draw());
        heightProperty().addListener(ignored -> draw());
        setOnMouseMoved(event -> updateTooltip(event.getX(), event.getY()));
        setOnMouseExited(event -> tooltip.hide());
    }

    public void setData(TabularData data) {
        this.data = data == null ? TabularData.empty() : data;
        draw();
    }

    public void setHalfWindowSeconds(double seconds) {
        halfWindowSeconds = seconds > 0 ? seconds : Double.POSITIVE_INFINITY;
        draw();
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(Math.max(0, getWidth()));
        canvas.setHeight(Math.max(0, getHeight()));
        draw();
    }

    private void draw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width < 180 || height < 180) return;
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.web("#091120"));
        graphics.fillRect(0, 0, width, height);

        visibleRows = readRows();
        plotLeft = 112;
        plotTop = 34;
        plotWidth = Math.max(20, width - plotLeft - 28);
        plotHeight = Math.max(40, height - plotTop - 92);
        if (visibleRows.isEmpty()) {
            graphics.setFill(Color.web("#94a3bd"));
            graphics.setFont(Font.font("System", FontWeight.BOLD, 14));
            graphics.fillText("Mappa non disponibile: servono i quattro canali ASCII.", 24, 54);
            return;
        }

        minimumTime = visibleRows.get(0).time();
        maximumTime = visibleRows.get(visibleRows.size() - 1).time();
        if (maximumTime <= minimumTime) maximumTime = minimumTime + 1;
        double scale = robustScale(visibleRows);
        double bandHeight = plotHeight / BANDS.size();

        for (int bandIndex = 0; bandIndex < BANDS.size(); bandIndex++) {
            double y = plotTop + bandIndex * bandHeight;
            graphics.setFill(bandIndex % 2 == 0 ? Color.web("#0c172a") : Color.web("#0a1425"));
            graphics.fillRect(plotLeft, y, plotWidth, bandHeight);
            for (int rowIndex = 0; rowIndex < visibleRows.size(); rowIndex++) {
                Row row = visibleRows.get(rowIndex);
                double previous = rowIndex == 0 ? minimumTime
                        : (visibleRows.get(rowIndex - 1).time() + row.time()) / 2.0;
                double next = rowIndex == visibleRows.size() - 1 ? maximumTime
                        : (row.time() + visibleRows.get(rowIndex + 1).time()) / 2.0;
                double x1 = xFor(previous);
                double x2 = Math.max(x1 + 1, xFor(next));
                graphics.setFill(colorFor(row.rates()[bandIndex], scale));
                graphics.fillRect(x1, y + 1, x2 - x1 + 0.5, Math.max(1, bandHeight - 2));
            }
        }

        drawAxes(graphics, width, height, bandHeight);
    }

    private void drawAxes(GraphicsContext graphics, double width, double height, double bandHeight) {
        graphics.setFont(Font.font("System", 11));
        graphics.setStroke(Color.web("#344867"));
        graphics.setFill(Color.web("#b9c7df"));
        graphics.setLineWidth(1);

        for (int index = 0; index <= BANDS.size(); index++) {
            double y = plotTop + index * bandHeight;
            graphics.strokeLine(plotLeft, y, plotLeft + plotWidth, y);
        }
        for (int index = 0; index < BANDS.size(); index++) {
            graphics.fillText(BANDS.get(index).label(), 12, plotTop + (index + 0.58) * bandHeight);
        }

        int tickCount = 6;
        for (int index = 0; index <= tickCount; index++) {
            double fraction = index / (double) tickCount;
            double time = minimumTime + fraction * (maximumTime - minimumTime);
            double x = plotLeft + fraction * plotWidth;
            graphics.setStroke(Color.web("#22334e"));
            graphics.strokeLine(x, plotTop, x, plotTop + plotHeight);
            graphics.setFill(Color.web("#91a2c0"));
            graphics.fillText(String.format(Locale.ROOT, "%.0f", time), x - 9, plotTop + plotHeight + 19);
        }

        if (minimumTime <= 0 && maximumTime >= 0) {
            double triggerX = xFor(0);
            graphics.setStroke(Color.web("#f8bd61"));
            graphics.setLineWidth(2);
            graphics.strokeLine(triggerX, plotTop, triggerX, plotTop + plotHeight);
            graphics.setFill(Color.web("#ffc66e"));
            graphics.fillText("trigger t = 0", Math.min(triggerX + 5, width - 88), plotTop - 10);
        }

        graphics.setFill(Color.web("#b9c7df"));
        graphics.setFont(Font.font("System", FontWeight.BOLD, 11));
        graphics.fillText("Tempo dal trigger (s)", plotLeft + plotWidth / 2 - 54, height - 38);

        double legendY = height - 13;
        double legendColumnWidth = plotWidth / 3.0;
        drawLegendItem(graphics, plotLeft, legendY, Color.web("#3b82f6"), "Fluttuazione negativa");
        drawLegendItem(graphics, plotLeft + legendColumnWidth, legendY, Color.web("#101a2b"), "Rate circa zero");
        drawLegendItem(graphics, plotLeft + 2 * legendColumnWidth, legendY, Color.web("#ff9f43"), "Rate positivo");
    }

    private void drawLegendItem(GraphicsContext graphics, double x, double y, Color color, String text) {
        graphics.setFill(color);
        graphics.fillRoundRect(x, y - 9, 11, 11, 3, 3);
        graphics.setStroke(Color.web("#8293b1"));
        graphics.setLineWidth(0.8);
        graphics.strokeRoundRect(x, y - 9, 11, 11, 3, 3);
        graphics.setFill(Color.web("#8da2c4"));
        graphics.setFont(Font.font("System", 10));
        graphics.fillText(text, x + 17, y);
    }

    private List<Row> readRows() {
        if (data == null || data.isEmpty()) return List.of();
        int timeIndex = data.indexOf("TIME_FROM_TRIGGER_CENTER_S");
        int[] rateIndices = BANDS.stream().mapToInt(band -> data.indexOf(band.column())).toArray();
        if (timeIndex < 0) return List.of();
        for (int index : rateIndices) if (index < 0) return List.of();

        List<Row> rows = new ArrayList<>();
        for (List<String> source : data.rows()) {
            double time = parse(source, timeIndex);
            if (!Double.isFinite(time) || (Double.isFinite(halfWindowSeconds) && Math.abs(time) > halfWindowSeconds)) {
                continue;
            }
            double[] rates = new double[BANDS.size()];
            for (int index = 0; index < rateIndices.length; index++) rates[index] = parse(source, rateIndices[index]);
            rows.add(new Row(time, rates));
        }
        rows.sort(Comparator.comparingDouble(Row::time));
        return List.copyOf(rows);
    }

    private double robustScale(List<Row> rows) {
        List<Double> values = new ArrayList<>();
        for (Row row : rows) for (double value : row.rates()) {
            if (Double.isFinite(value)) values.add(Math.abs(value));
        }
        if (values.isEmpty()) return 1;
        values.sort(Double::compareTo);
        double percentile = values.get(Math.min(values.size() - 1, (int) Math.floor(values.size() * 0.98)));
        return percentile > 0 ? percentile : 1;
    }

    private Color colorFor(double value, double scale) {
        if (!Double.isFinite(value)) return Color.web("#111b2c");
        double strength = Math.min(1, Math.sqrt(Math.abs(value) / scale));
        Color zero = Color.web("#101a2b");
        Color target = value >= 0 ? Color.web("#ff9f43") : Color.web("#3b82f6");
        return zero.interpolate(target, strength);
    }

    private void updateTooltip(double x, double y) {
        if (visibleRows.isEmpty() || x < plotLeft || x > plotLeft + plotWidth
                || y < plotTop || y > plotTop + plotHeight) {
            tooltip.setText("Passa sopra una cella per leggere tempo, banda e rate.");
            return;
        }
        double time = minimumTime + (x - plotLeft) / plotWidth * (maximumTime - minimumTime);
        Row nearest = visibleRows.stream().min(Comparator.comparingDouble(row -> Math.abs(row.time() - time)))
                .orElse(visibleRows.get(0));
        int bandIndex = Math.min(BANDS.size() - 1,
                Math.max(0, (int) ((y - plotTop) / (plotHeight / BANDS.size()))));
        tooltip.setText(String.format(Locale.ITALIAN, "%s%nCentro bin: %.3f s%nRate: %.5g count/s",
                BANDS.get(bandIndex).label(), nearest.time(), nearest.rates()[bandIndex]));
    }

    private double xFor(double time) {
        return plotLeft + (time - minimumTime) / (maximumTime - minimumTime) * plotWidth;
    }

    private static double parse(List<String> row, int index) {
        if (index < 0 || index >= row.size()) return Double.NaN;
        try {
            return Double.parseDouble(row.get(index));
        } catch (Exception ignored) {
            return Double.NaN;
        }
    }

    private record Band(String label, String column) {
    }

    private record Row(double time, double[] rates) {
    }
}
