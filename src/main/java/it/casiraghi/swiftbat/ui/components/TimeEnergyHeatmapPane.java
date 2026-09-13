package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.model.TabularData;
import it.casiraghi.swiftbat.ui.I18n;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Heatmap descrittiva tempo-energia costruita dai quattro rate ASCII a 1 s. */
public final class TimeEnergyHeatmapPane extends Region {
    private static final List<Band> BANDS = List.of(
            new Band("100–350 keV", "RATE_100_350_KEV", 250),
            new Band("50–100 keV", "RATE_50_100_KEV", 50),
            new Band("25–50 keV", "RATE_25_50_KEV", 25),
            new Band("15–25 keV", "RATE_15_25_KEV", 10));

    private static final double COLUMN_HEADER_HEIGHT = 17;
    private static final Color DIM_TARGET = Color.web("#091120");

    private final Canvas canvas = new Canvas();
    private final Label hoverCard = new Label();
    private final Set<Cell> selectedCells = new LinkedHashSet<>();
    private TabularData data = TabularData.empty();
    private double halfWindowSeconds = 60.0;
    private List<Row> visibleRows = List.of();
    private double plotLeft;
    private double plotTop;
    private double plotWidth;
    private double plotHeight;
    private double minimumTime;
    private double maximumTime;
    private int hoveredRowIndex = -1;
    private int hoveredBandIndex = -1;
    private int hoveredHeaderRowIndex = -1;
    private Cell pressedCell;
    private boolean pressedCellWasSelected;
    private boolean draggedSelection;

    public TimeEnergyHeatmapPane() {
        hoverCard.setManaged(false);
        hoverCard.setMouseTransparent(true);
        hoverCard.setVisible(false);
        hoverCard.setWrapText(false);
        hoverCard.setStyle(
                "-fx-background-color: rgba(8, 17, 34, 0.96);"
                        + "-fx-background-radius: 9;"
                        + "-fx-border-color: rgba(101, 153, 220, 0.55);"
                        + "-fx-border-radius: 9;"
                        + "-fx-text-fill: #e5efff;"
                        + "-fx-font-size: 11px;"
                        + "-fx-padding: 8 10;"
        );

        getChildren().addAll(canvas, hoverCard);
        setMinHeight(300);
        setPrefHeight(390);
        widthProperty().addListener(ignored -> draw());
        heightProperty().addListener(ignored -> draw());
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> draw());

        setOnMouseMoved(event -> updateHover(event.getX(), event.getY()));
        setOnMouseExited(event -> clearHover());
        setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY || visibleRows.isEmpty()) return;
            draggedSelection = false;
            pressedCell = cellAt(event.getX(), event.getY());
            pressedCellWasSelected = pressedCell != null && selectedCells.contains(pressedCell);
            if (pressedCell != null) {
                if (!pressedCellWasSelected) selectedCells.add(pressedCell);
                draw();
                return;
            }
            int column = headerColumnAt(event.getX(), event.getY());
            if (column >= 0) {
                toggleWholeColumn(column);
                draw();
            }
        });
        setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) return;
            draggedSelection = true;
            Cell cell = cellAt(event.getX(), event.getY());
            if (cell != null && selectedCells.add(cell)) draw();
            updateHover(event.getX(), event.getY());
        });
        setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            if (pressedCell != null && !draggedSelection && pressedCellWasSelected) {
                selectedCells.remove(pressedCell);
                draw();
            }
            pressedCell = null;
            pressedCellWasSelected = false;
        });
        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
                selectedCells.clear();
                clearHover();
                draw();
                event.consume();
            }
        });
    }

    public void setData(TabularData data) {
        this.data = data == null ? TabularData.empty() : data;
        clearSelectionState();
        draw();
    }

    public void setHalfWindowSeconds(double seconds) {
        halfWindowSeconds = seconds > 0 ? seconds : Double.POSITIVE_INFINITY;
        clearSelectionState();
        draw();
    }

    @Override
    protected void layoutChildren() {
        double width = Math.max(0, getWidth());
        double height = Math.max(0, getHeight());
        canvas.setWidth(width);
        canvas.setHeight(height);
        draw();
    }

    private void clearSelectionState() {
        selectedCells.clear();
        hoveredRowIndex = -1;
        hoveredBandIndex = -1;
        hoveredHeaderRowIndex = -1;
        pressedCell = null;
        hoverCard.setVisible(false);
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
        plotTop = 72;
        plotWidth = Math.max(20, width - plotLeft - 28);
        plotHeight = Math.max(40, height - plotTop - 112);
        if (visibleRows.isEmpty()) {
            graphics.setFill(Color.web("#94a3bd"));
            graphics.setFont(Font.font("System", FontWeight.BOLD, 14));
            graphics.fillText(I18n.t("Mappa non disponibile: servono i quattro canali ASCII."), 24, 64);
            hoverCard.setVisible(false);
            hoveredRowIndex = -1;
            hoveredBandIndex = -1;
            hoveredHeaderRowIndex = -1;
            return;
        }

        minimumTime = visibleRows.get(0).time();
        maximumTime = visibleRows.get(visibleRows.size() - 1).time();
        if (maximumTime <= minimumTime) maximumTime = minimumTime + 1;
        double scale = robustScale(visibleRows);
        double bandHeight = plotHeight / BANDS.size();

        drawColumnHeader(graphics);
        for (int bandIndex = 0; bandIndex < BANDS.size(); bandIndex++) {
            double y = plotTop + bandIndex * bandHeight;
            graphics.setFill(bandIndex % 2 == 0 ? Color.web("#0c172a") : Color.web("#0a1425"));
            graphics.fillRect(plotLeft, y, plotWidth, bandHeight);
            for (int rowIndex = 0; rowIndex < visibleRows.size(); rowIndex++) {
                Row row = visibleRows.get(rowIndex);
                Bounds bounds = cellBounds(rowIndex, bandIndex, bandHeight);
                Color color = colorFor(row.rates()[bandIndex], scale);
                if (!selectedCells.isEmpty() && !selectedCells.contains(new Cell(rowIndex, bandIndex))) {
                    color = color.interpolate(DIM_TARGET, 0.82);
                }
                graphics.setFill(color);
                graphics.fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            }
        }

        drawAxes(graphics, width, height, bandHeight);
        drawSelections(graphics, bandHeight);
        drawHoverHighlight(graphics, bandHeight);
    }

    private void drawColumnHeader(GraphicsContext graphics) {
        double top = headerTop();
        graphics.setFill(Color.web("#0b1830"));
        graphics.fillRoundRect(plotLeft, top, plotWidth, COLUMN_HEADER_HEIGHT, 6, 6);
        graphics.setStroke(Color.web("#2e496f"));
        graphics.setLineWidth(1);
        graphics.strokeRoundRect(plotLeft, top, plotWidth, COLUMN_HEADER_HEIGHT, 6, 6);

        for (int rowIndex = 0; rowIndex < visibleRows.size(); rowIndex++) {
            Bounds bounds = columnBounds(rowIndex);
            boolean selected = wholeColumnSelected(rowIndex);
            boolean partial = !selected && columnHasSelection(rowIndex);
            if (selected || partial || rowIndex == hoveredHeaderRowIndex) {
                Color fill = selected ? Color.web("#ffad57")
                        : partial ? Color.web("#8b5cf6") : Color.web("#35c7ff");
                graphics.setFill(fill.deriveColor(0, 1, 1, selected ? 0.86 : 0.60));
                graphics.fillRect(bounds.x(), top + 2, Math.max(1, bounds.width()), COLUMN_HEADER_HEIGHT - 4);
            }
        }
        graphics.setFill(Color.web("#8293b1"));
        graphics.setFont(Font.font("System", FontWeight.BOLD, 9));
        graphics.fillText("▾", plotLeft - 14, top + 12);
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
            graphics.fillText("trigger t = 0", Math.min(triggerX + 5, width - 88), 36);
        }

        graphics.setFill(Color.web("#b9c7df"));
        graphics.setFont(Font.font("System", FontWeight.BOLD, 11));
        graphics.fillText(I18n.t("Tempo dal trigger (s)"), plotLeft + plotWidth / 2 - 54, height - 55);

        double legendY = height - 29;
        double legendColumnWidth = plotWidth / 3.0;
        drawLegendItem(graphics, plotLeft, legendY, Color.web("#3b82f6"), I18n.t("Fluttuazione negativa"));
        drawLegendItem(graphics, plotLeft + legendColumnWidth, legendY, Color.web("#101a2b"), I18n.t("Rate circa zero"));
        drawLegendItem(graphics, plotLeft + 2 * legendColumnWidth, legendY, Color.web("#ff9f43"), I18n.t("Rate positivo"));

        graphics.setFill(Color.web("#7388aa"));
        graphics.setFont(Font.font("System", 9.5));
        graphics.fillText(I18n.t("Clic: fissa · trascina: aggiungi · clic in alto: seleziona l'istante · doppio clic: azzera"),
                plotLeft, height - 7);
    }

    private void drawSelections(GraphicsContext graphics, double bandHeight) {
        if (selectedCells.isEmpty()) return;
        for (Cell cell : selectedCells) {
            if (!valid(cell)) continue;
            Bounds bounds = cellBounds(cell.rowIndex(), cell.bandIndex(), bandHeight);
            graphics.setFill(Color.color(1.0, 0.70, 0.30, 0.08));
            graphics.fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            graphics.setStroke(Color.color(1.0, 0.69, 0.30, 0.80));
            graphics.setLineWidth(1.15);
            graphics.strokeRect(bounds.x() + 0.5, bounds.y() + 0.5,
                    Math.max(1, bounds.width() - 1), Math.max(1, bounds.height() - 1));
        }
    }

    private void drawHoverHighlight(GraphicsContext graphics, double bandHeight) {
        if (hoveredHeaderRowIndex >= 0 && hoveredHeaderRowIndex < visibleRows.size()
                && (selectedCells.isEmpty() || columnHasSelection(hoveredHeaderRowIndex))) {
            Bounds bounds = columnBounds(hoveredHeaderRowIndex);
            graphics.setStroke(Color.web("#d9efff"));
            graphics.setLineWidth(1.3);
            graphics.strokeRect(bounds.x() + 0.5, headerTop() + 0.5,
                    Math.max(1, bounds.width() - 1), COLUMN_HEADER_HEIGHT - 1);
        }
        if (hoveredRowIndex < 0 || hoveredRowIndex >= visibleRows.size()
                || hoveredBandIndex < 0 || hoveredBandIndex >= BANDS.size()) return;
        Cell hovered = new Cell(hoveredRowIndex, hoveredBandIndex);
        if (!selectedCells.isEmpty() && !selectedCells.contains(hovered)) return;

        Bounds bounds = cellBounds(hoveredRowIndex, hoveredBandIndex, bandHeight);
        graphics.setFill(Color.color(1.0, 0.84, 0.62, 0.08));
        graphics.fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        graphics.setStroke(Color.web("#fff2dc"));
        graphics.setLineWidth(1.8);
        graphics.strokeRect(bounds.x() + 1, bounds.y() + 1,
                Math.max(1, bounds.width() - 2), Math.max(1, bounds.height() - 2));
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
            if (!Double.isFinite(time) || (Double.isFinite(halfWindowSeconds) && Math.abs(time) > halfWindowSeconds)) continue;
            double[] rates = new double[BANDS.size()];
            for (int index = 0; index < rateIndices.length; index++) rates[index] = parse(source, rateIndices[index]);
            rows.add(new Row(time, rates));
        }
        rows.sort(Comparator.comparingDouble(Row::time));
        return List.copyOf(rows);
    }

    private double robustScale(List<Row> rows) {
        List<Double> values = new ArrayList<>();
        for (Row row : rows) for (double value : row.rates()) if (Double.isFinite(value)) values.add(Math.abs(value));
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

    private void updateHover(double x, double y) {
        if (visibleRows.isEmpty()) {
            clearHover();
            return;
        }
        int headerRow = headerColumnAt(x, y);
        if (headerRow >= 0) {
            if (!selectedCells.isEmpty() && !columnHasSelection(headerRow)) {
                clearHover();
                return;
            }
            hoveredHeaderRowIndex = headerRow;
            hoveredRowIndex = -1;
            hoveredBandIndex = -1;
            draw();
            showHoverCard(x, y, selectedCells.isEmpty() || wholeColumnSelected(headerRow)
                    ? columnSummary(headerRow) : selectedColumnSummary(headerRow));
            return;
        }

        Cell cell = cellAt(x, y);
        if (cell == null) {
            clearHover();
            return;
        }
        if (!selectedCells.isEmpty() && !selectedCells.contains(cell)) {
            clearHover();
            return;
        }
        hoveredHeaderRowIndex = -1;
        if (cell.rowIndex() != hoveredRowIndex || cell.bandIndex() != hoveredBandIndex) {
            hoveredRowIndex = cell.rowIndex();
            hoveredBandIndex = cell.bandIndex();
            draw();
        }

        String text = selectedCells.size() > 1 && selectedCells.contains(cell)
                ? selectionSummary() : cellSummary(cell);
        showHoverCard(x, y, text);
    }

    private String cellSummary(Cell cell) {
        Row row = visibleRows.get(cell.rowIndex());
        Band band = BANDS.get(cell.bandIndex());
        return I18n.t("Banda") + ": " + band.label()
                + "\n" + I18n.t("Centro bin") + ": " + String.format(Locale.ROOT, "%.3f s", row.time())
                + "\nRate: " + String.format(Locale.ROOT, "%.5g count/s", row.rates()[cell.bandIndex()])
                + "\n" + I18n.t("Larghezza banda") + ": " + String.format(Locale.ROOT, "%.0f keV", band.widthKeV());
    }

    private String selectionSummary() {
        Stats stats = stats(selectedCells);
        if (stats.count() == 0) return "";
        return I18n.t("Area selezionata")
                + "\n" + I18n.t("Celle selezionate") + ": " + stats.count()
                + "\n" + I18n.t("Rate medio") + ": " + String.format(Locale.ROOT, "%.5g count/s", stats.mean())
                + "\n" + I18n.t("Intervallo temporale") + ": " + formatTimeRange(stats.minTime(), stats.maxTime());
    }

    private String columnSummary(int rowIndex) {
        Set<Cell> column = new LinkedHashSet<>();
        for (int band = 0; band < BANDS.size(); band++) column.add(new Cell(rowIndex, band));
        Stats stats = stats(column);
        int dominantBand = dominantBand(rowIndex);
        return I18n.t("Istante") + ": " + String.format(Locale.ROOT, "%.3f s", visibleRows.get(rowIndex).time())
                + "\n" + I18n.t("Rate medio") + ": " + String.format(Locale.ROOT, "%.5g count/s", stats.mean())
                + "\n" + I18n.t("Banda dominante") + ": " + BANDS.get(dominantBand).label();
    }

    private String selectedColumnSummary(int rowIndex) {
        Set<Cell> selectedColumn = new LinkedHashSet<>();
        for (Cell cell : selectedCells) {
            if (cell.rowIndex() == rowIndex) selectedColumn.add(cell);
        }
        Stats stats = stats(selectedColumn);
        return I18n.t("Istante") + ": " + String.format(Locale.ROOT, "%.3f s", visibleRows.get(rowIndex).time())
                + "\n" + I18n.t("Celle selezionate") + ": " + stats.count()
                + "\n" + I18n.t("Rate medio") + ": " + String.format(Locale.ROOT, "%.5g count/s", stats.mean());
    }

    private Stats stats(Set<Cell> cells) {
        int count = 0;
        double sum = 0;
        double minTime = Double.POSITIVE_INFINITY;
        double maxTime = Double.NEGATIVE_INFINITY;
        for (Cell cell : cells) {
            if (!valid(cell)) continue;
            Row row = visibleRows.get(cell.rowIndex());
            double value = row.rates()[cell.bandIndex()];
            if (!Double.isFinite(value)) continue;
            count++;
            sum += value;
            minTime = Math.min(minTime, row.time());
            maxTime = Math.max(maxTime, row.time());
        }
        return new Stats(count, count == 0 ? Double.NaN : sum / count, minTime, maxTime);
    }

    private int dominantBand(int rowIndex) {
        int best = 0;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int band = 0; band < BANDS.size(); band++) {
            double value = Math.abs(visibleRows.get(rowIndex).rates()[band]);
            if (Double.isFinite(value) && value > maximum) {
                maximum = value;
                best = band;
            }
        }
        return best;
    }

    private String formatTimeRange(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max)) return "—";
        if (Math.abs(max - min) < 1e-9) return String.format(Locale.ROOT, "%.3f s", min);
        return String.format(Locale.ROOT, "%.3f – %.3f s", min, max);
    }

    private void showHoverCard(double x, double y, String text) {
        hoverCard.setText(text);
        hoverCard.applyCss();
        hoverCard.autosize();
        double targetX = x + 14;
        double targetY = y + 14;
        double cardWidth = hoverCard.getWidth();
        double cardHeight = hoverCard.getHeight();
        if (targetX + cardWidth > getWidth() - 8) targetX = x - cardWidth - 14;
        if (targetY + cardHeight > getHeight() - 8) targetY = y - cardHeight - 14;
        hoverCard.relocate(Math.max(8, targetX), Math.max(8, targetY));
        hoverCard.toFront();
        hoverCard.setVisible(true);
    }

    private void toggleWholeColumn(int rowIndex) {
        boolean allSelected = wholeColumnSelected(rowIndex);
        for (int band = 0; band < BANDS.size(); band++) {
            Cell cell = new Cell(rowIndex, band);
            if (allSelected) selectedCells.remove(cell);
            else selectedCells.add(cell);
        }
    }

    private boolean wholeColumnSelected(int rowIndex) {
        for (int band = 0; band < BANDS.size(); band++) {
            if (!selectedCells.contains(new Cell(rowIndex, band))) return false;
        }
        return true;
    }

    private boolean columnHasSelection(int rowIndex) {
        for (int band = 0; band < BANDS.size(); band++) {
            if (selectedCells.contains(new Cell(rowIndex, band))) return true;
        }
        return false;
    }

    private Cell cellAt(double x, double y) {
        if (x < plotLeft || x > plotLeft + plotWidth || y < plotTop || y > plotTop + plotHeight) return null;
        double time = minimumTime + (x - plotLeft) / plotWidth * (maximumTime - minimumTime);
        int rowIndex = nearestRowIndex(time);
        if (rowIndex < 0) return null;
        int bandIndex = Math.min(BANDS.size() - 1,
                Math.max(0, (int) ((y - plotTop) / (plotHeight / BANDS.size()))));
        return new Cell(rowIndex, bandIndex);
    }

    private int headerColumnAt(double x, double y) {
        if (x < plotLeft || x > plotLeft + plotWidth || y < headerTop() || y > headerTop() + COLUMN_HEADER_HEIGHT) return -1;
        double time = minimumTime + (x - plotLeft) / plotWidth * (maximumTime - minimumTime);
        return nearestRowIndex(time);
    }

    private int nearestRowIndex(double time) {
        if (visibleRows.isEmpty()) return -1;
        int bestIndex = 0;
        double bestDistance = Math.abs(visibleRows.get(0).time() - time);
        for (int index = 1; index < visibleRows.size(); index++) {
            double distance = Math.abs(visibleRows.get(index).time() - time);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private Bounds cellBounds(int rowIndex, int bandIndex, double bandHeight) {
        Bounds column = columnBounds(rowIndex);
        double y = plotTop + bandIndex * bandHeight + 1;
        return new Bounds(column.x(), y, Math.max(1, column.width()), Math.max(1, bandHeight - 2));
    }

    private Bounds columnBounds(int rowIndex) {
        Row row = visibleRows.get(rowIndex);
        double previous = rowIndex == 0 ? minimumTime
                : (visibleRows.get(rowIndex - 1).time() + row.time()) / 2.0;
        double next = rowIndex == visibleRows.size() - 1 ? maximumTime
                : (row.time() + visibleRows.get(rowIndex + 1).time()) / 2.0;
        double x1 = xFor(previous);
        double x2 = Math.max(x1 + 1, xFor(next));
        return new Bounds(x1, 0, Math.max(1, x2 - x1 + 0.5), 0);
    }

    private boolean valid(Cell cell) {
        return cell != null && cell.rowIndex() >= 0 && cell.rowIndex() < visibleRows.size()
                && cell.bandIndex() >= 0 && cell.bandIndex() < BANDS.size();
    }

    private void clearHover() {
        boolean hadHighlight = hoveredRowIndex >= 0 || hoveredBandIndex >= 0 || hoveredHeaderRowIndex >= 0;
        hoveredRowIndex = -1;
        hoveredBandIndex = -1;
        hoveredHeaderRowIndex = -1;
        hoverCard.setVisible(false);
        if (hadHighlight) draw();
    }

    private double headerTop() {
        return plotTop - COLUMN_HEADER_HEIGHT - 10;
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

    private record Band(String label, String column, double widthKeV) { }
    private record Row(double time, double[] rates) { }
    private record Cell(int rowIndex, int bandIndex) { }
    private record Bounds(double x, double y, double width, double height) { }
    private record Stats(int count, double mean, double minTime, double maxTime) { }
}
