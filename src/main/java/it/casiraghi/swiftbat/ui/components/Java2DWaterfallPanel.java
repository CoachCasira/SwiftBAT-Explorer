package it.casiraghi.swiftbat.ui.components;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Renderer scientifico 2.5D basato su Java2D.
 *
 * <p>Le quattro bande energetiche sono mostrate come curve "a cascata":
 * tempo sull'asse orizzontale, rate sull'asse verticale e profondità puramente
 * grafica per separare le bande. Non usa la scena 3D di JavaFX e quindi evita
 * artefatti di illuminazione, clipping e piani metà chiari/metà scuri.</p>
 */
public final class Java2DWaterfallPanel extends JPanel {
    private static final Color BACKGROUND_TOP = new Color(6, 13, 29);
    private static final Color BACKGROUND_BOTTOM = new Color(9, 23, 48);
    private static final Color GRID = new Color(78, 103, 151, 65);
    private static final Color GRID_STRONG = new Color(108, 144, 214, 105);
    private static final Color TEXT = new Color(224, 235, 255);
    private static final Color MUTED = new Color(139, 164, 207);
    private static final Color TRIGGER = new Color(255, 211, 106);
    private static final Color TOOLTIP_BG = new Color(9, 18, 38, 238);
    private static final DecimalFormat VALUE_FORMAT;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        VALUE_FORMAT = new DecimalFormat("0.####", symbols);
    }

    private Dataset dataset = Dataset.empty();
    private Presentation presentation = Presentation.explorerDefaults();
    private final List<ProjectedPoint> projectedPoints = new ArrayList<>();

    private double yaw = 0.32;
    private double pitch = 0.72;
    private double zoom = 1.0;
    private int dragStartX;
    private int dragStartY;
    private double dragStartYaw;
    private double dragStartPitch;
    private HoverPoint hover;

    public Java2DWaterfallPanel() {
        setOpaque(true);
        setBackground(BACKGROUND_TOP);
        setPreferredSize(new Dimension(900, 520));
        setMinimumSize(new Dimension(500, 330));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                dragStartX = event.getX();
                dragStartY = event.getY();
                dragStartYaw = yaw;
                dragStartPitch = pitch;
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                yaw = clamp(dragStartYaw + (event.getX() - dragStartX) * 0.0028, -0.72, 0.72);
                pitch = clamp(dragStartPitch - (event.getY() - dragStartY) * 0.0032, 0.30, 1.12);
                hover = null;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                updateHover(event.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hover = null;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    resetView();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                zoom = clamp(zoom * Math.pow(1.08, -event.getPreciseWheelRotation()), 0.68, 1.55);
                hover = null;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public void setDataset(Dataset newDataset) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setDataset(newDataset));
            return;
        }
        dataset = newDataset == null ? Dataset.empty() : newDataset.copy();
        hover = null;
        repaint();
    }

    /** Personalizza testi e densità del renderer senza cambiare la geometria dei dati. */
    public void setPresentation(Presentation newPresentation) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setPresentation(newPresentation));
            return;
        }
        presentation = newPresentation == null ? Presentation.explorerDefaults() : newPresentation;
        hover = null;
        repaint();
    }

    public void resetView() {
        yaw = 0.32;
        pitch = 0.72;
        zoom = 1.0;
        hover = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            configure(g);
            paintBackground(g);
            if (dataset.isEmpty()) {
                paintEmpty(g);
                return;
            }
            paintChart(g);
        } finally {
            g.dispose();
        }
    }

    private void configure(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private void paintBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, BACKGROUND_TOP, 0, getHeight(), BACKGROUND_BOTTOM));
        g.fillRect(0, 0, getWidth(), getHeight());

    }

    private void paintEmpty(Graphics2D g) {
        g.setColor(MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        String text = presentation.emptyMessage();
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, getHeight() / 2);
    }

    private void paintChart(Graphics2D g) {
        projectedPoints.clear();

        Bounds bounds = calculateBounds();
        Geometry geometry = new Geometry(getWidth(), getHeight(), dataset.bandCount(), yaw, pitch, zoom);

        paintGrid(g, bounds, geometry);
        paintTrigger(g, bounds, geometry);

        // Disegno dal fondo verso il fronte, così la profondità resta leggibile.
        for (int band = dataset.bandCount() - 1; band >= 0; band--) {
            paintBand(g, band, bounds, geometry);
        }

        paintAxesAndLabels(g, bounds, geometry);
        paintOrientationHint(g);
        if (hover != null) {
            paintHover(g, hover);
        }
    }

    private Bounds calculateBounds() {
        double minTime = dataset.times()[0];
        double maxTime = dataset.times()[dataset.times().length - 1];
        double minRate = 0;
        double maxRate = 0;
        for (double[] band : dataset.rates()) {
            for (double value : band) {
                if (Double.isFinite(value)) {
                    minRate = Math.min(minRate, value);
                    maxRate = Math.max(maxRate, value);
                }
            }
        }
        if (maxTime <= minTime) {
            maxTime = minTime + 1;
        }
        if (maxRate <= 0) {
            maxRate = 1;
        }
        double paddedMax = maxRate * 1.12;
        double paddedMin = minRate < 0 ? minRate * 1.30 : -maxRate * 0.08;
        return new Bounds(minTime, maxTime, paddedMin, paddedMax);
    }

    private void paintGrid(Graphics2D g, Bounds bounds, Geometry geometry) {
        g.setStroke(new BasicStroke(1f));

        int xTicks = 8;
        for (int tick = 0; tick <= xTicks; tick++) {
            double fraction = tick / (double) xTicks;
            double time = lerp(bounds.minTime(), bounds.maxTime(), fraction);
            Point2D front = geometry.project(fraction, 0, 0);
            Point2D back = geometry.project(fraction, 0, dataset.bandCount() - 1);
            g.setColor(Math.abs(time) < (bounds.maxTime() - bounds.minTime()) / 1000 ? GRID_STRONG : GRID);
            g.draw(new Line2D.Double(front, back));
        }

        int yTicks = 5;
        for (int tick = 0; tick <= yTicks; tick++) {
            double rate = lerp(bounds.minRate(), bounds.maxRate(), tick / (double) yTicks);
            double yNorm = normalize(rate, bounds.minRate(), bounds.maxRate());
            Point2D leftFront = geometry.project(0, yNorm, 0);
            Point2D rightFront = geometry.project(1, yNorm, 0);
            Point2D leftBack = geometry.project(0, yNorm, dataset.bandCount() - 1);
            Point2D rightBack = geometry.project(1, yNorm, dataset.bandCount() - 1);
            g.setColor(Math.abs(rate) < (bounds.maxRate() - bounds.minRate()) / 100 ? GRID_STRONG : GRID);
            g.draw(new Line2D.Double(leftFront, rightFront));
            g.draw(new Line2D.Double(leftBack, rightBack));
            g.draw(new Line2D.Double(leftFront, leftBack));
        }

        // Linee di base delle serie disposte in profondità.
        double zeroNorm = normalize(0, bounds.minRate(), bounds.maxRate());
        for (int band = 0; band < dataset.bandCount(); band++) {
            Point2D left = geometry.project(0, zeroNorm, band);
            Point2D right = geometry.project(1, zeroNorm, band);
            int baselineAlpha = presentation.denseSeries() ? 24 : band == 0 ? 90 : 55;
            g.setColor(withAlpha(dataset.colors()[band], baselineAlpha));
            g.setStroke(new BasicStroke(presentation.denseSeries() ? 0.8f : band == 0 ? 1.5f : 1f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 10f, new float[]{5f, 7f}, 0f));
            g.draw(new Line2D.Double(left, right));
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void paintTrigger(Graphics2D g, Bounds bounds, Geometry geometry) {
        if (0 < bounds.minTime() || 0 > bounds.maxTime()) {
            return;
        }
        double xNorm = normalize(0, bounds.minTime(), bounds.maxTime());
        Point2D bottomFront = geometry.project(xNorm, normalize(0, bounds.minRate(), bounds.maxRate()), 0);
        Point2D topBack = geometry.project(xNorm, 1, dataset.bandCount() - 1);
        g.setColor(new Color(TRIGGER.getRed(), TRIGGER.getGreen(), TRIGGER.getBlue(), 175));
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10f, new float[]{7f, 7f}, 0f));
        g.draw(new Line2D.Double(bottomFront, topBack));
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("trigger  t = 0", (float) topBack.getX() + 7, (float) topBack.getY() - 5);
    }

    private void paintBand(Graphics2D g, int band, Bounds bounds, Geometry geometry) {
        double[] times = dataset.times();
        double[] values = dataset.rates()[band];
        Color color = dataset.colors()[band];
        double zeroNorm = normalize(0, bounds.minRate(), bounds.maxRate());

        Path2D line = new Path2D.Double();
        Point2D[] points = new Point2D[times.length];
        boolean drawingSegment = false;
        boolean allFinite = true;
        for (int index = 0; index < times.length; index++) {
            if (!Double.isFinite(times[index]) || !Double.isFinite(values[index])) {
                drawingSegment = false;
                allFinite = false;
                continue;
            }
            double xNorm = normalize(times[index], bounds.minTime(), bounds.maxTime());
            double yNorm = normalize(values[index], bounds.minRate(), bounds.maxRate());
            Point2D point = geometry.project(xNorm, yNorm, band);
            points[index] = point;
            if (!drawingSegment) {
                line.moveTo(point.getX(), point.getY());
                drawingSegment = true;
            } else {
                line.lineTo(point.getX(), point.getY());
            }
            projectedPoints.add(new ProjectedPoint(point, band, index));
        }

        // Riempimento trasparente: dà profondità senza creare un pannello opaco.
        if (presentation.showAreaFill() && allFinite && points.length >= 2) {
            Path2D area = new Path2D.Double();
            Point2D firstBase = geometry.project(0, zeroNorm, band);
            area.moveTo(firstBase.getX(), firstBase.getY());
            for (Point2D point : points) {
                area.lineTo(point.getX(), point.getY());
            }
            Point2D lastBase = geometry.project(1, zeroNorm, band);
            area.lineTo(lastBase.getX(), lastBase.getY());
            area.closePath();
            g.setColor(withAlpha(color, band == 0 ? 34 : 25));
            g.fill(area);
        }

        // Glow sottile seguito dalla linea nitida. Nel profilo di popolazione
        // mediana e quartili mantengono la stessa gerarchia visiva della vista 2D.
        boolean medianBand = isMedianBand(band);
        boolean quartileBand = isQuartileBand(band);
        int glowAlpha = medianBand ? 70 : quartileBand ? 40 : presentation.denseSeries() ? 18 : 55;
        float glowWidth = medianBand ? 8.0f : quartileBand ? 5.0f
                : presentation.denseSeries() ? 3.0f : 7.5f;
        g.setColor(withAlpha(color, glowAlpha));
        g.setStroke(new BasicStroke(glowWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);

        int lineAlpha = medianBand ? 255 : quartileBand ? 235
                : presentation.denseSeries() ? 105 : band == 0 ? 245 : 220;
        float lineWidth = medianBand ? 4.2f : quartileBand ? 2.4f
                : presentation.denseSeries() ? 1.05f : band == 0 ? 2.8f : 2.35f;
        g.setColor(withAlpha(color, lineAlpha));
        if (quartileBand) {
            g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[]{8f, 6f}, 0f));
        } else {
            g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g.draw(line);

        // Evidenzia il massimo quando è utile per il tipo di grafico visualizzato.
        int peakIndex = presentation.showPeakMarkers() ? peakIndex(values) : -1;
        if (peakIndex >= 0 && points[peakIndex] != null) {
            Point2D peak = points[peakIndex];
            g.setColor(new Color(255, 255, 255, 220));
            g.fillOval((int) peak.getX() - 3, (int) peak.getY() - 3, 6, 6);
            g.setColor(withAlpha(color, 245));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval((int) peak.getX() - 5, (int) peak.getY() - 5, 10, 10);
        }
    }

    private boolean isMedianBand(int band) {
        return "Mediana".equals(labelForBand(band));
    }

    private boolean isQuartileBand(int band) {
        String label = labelForBand(band);
        return label != null && label.contains("percentile");
    }

    private String labelForBand(int band) {
        return band >= 0 && band < dataset.labels().length ? dataset.labels()[band] : null;
    }

    private void paintAxesAndLabels(Graphics2D g, Bounds bounds, Geometry geometry) {
        double zeroNorm = normalize(0, bounds.minRate(), bounds.maxRate());
        Point2D xStart = geometry.project(0, zeroNorm, 0);
        Point2D xEnd = geometry.project(1, zeroNorm, 0);
        Point2D yTop = geometry.project(0, 1, 0);
        Point2D depthEnd = geometry.project(0, zeroNorm, dataset.bandCount() - 1);

        g.setStroke(new BasicStroke(1.8f));
        g.setColor(new Color(90, 216, 255, 205));
        g.draw(new Line2D.Double(xStart, xEnd));
        g.setColor(new Color(228, 238, 255, 210));
        g.draw(new Line2D.Double(xStart, yTop));
        g.setColor(new Color(167, 139, 250, 190));
        g.draw(new Line2D.Double(xStart, depthEnd));

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(MUTED);
        int xTicks = 8;
        for (int tick = 0; tick <= xTicks; tick++) {
            double fraction = tick / (double) xTicks;
            double time = lerp(bounds.minTime(), bounds.maxTime(), fraction);
            Point2D point = geometry.project(fraction, zeroNorm, 0);
            String label = formatAxis(time);
            int width = g.getFontMetrics().stringWidth(label);
            g.drawString(label, (float) point.getX() - width / 2f, (float) point.getY() + 20);
        }

        int yTicks = 5;
        for (int tick = 0; tick <= yTicks; tick++) {
            double fraction = tick / (double) yTicks;
            double rate = lerp(bounds.minRate(), bounds.maxRate(), fraction);
            Point2D point = geometry.project(0, fraction, 0);
            String label = formatAxis(rate);
            int width = g.getFontMetrics().stringWidth(label);
            g.drawString(label, (float) point.getX() - width - 10, (float) point.getY() + 4);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(TEXT);
        String xLabel = presentation.xAxisLabel();
        g.drawString(xLabel, (float) ((xStart.getX() + xEnd.getX()) / 2 - g.getFontMetrics().stringWidth(xLabel) / 2.0),
                (float) Math.max(xStart.getY(), xEnd.getY()) + 43);
        g.drawString(presentation.yAxisLabel(), (float) yTop.getX() - 42, (float) yTop.getY() - 12);

        if (!presentation.depthAxisLabel().isBlank()) {
            g.setColor(new Color(190, 166, 250));
            int labelWidth = g.getFontMetrics().stringWidth(presentation.depthAxisLabel());
            float labelX = (float) clamp(depthEnd.getX() + 8, 12, getWidth() - labelWidth - 12);
            float labelY = (float) clamp(depthEnd.getY() - 9, 20, getHeight() - 45);
            g.drawString(presentation.depthAxisLabel(), labelX, labelY);
        }

        // Etichette delle serie all'estremità destra, diradate nei campioni numerosi.
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        int labelStep = labelStep(dataset.bandCount(), presentation.maxDepthLabels());
        for (int band = dataset.bandCount() - 1; band >= 0; band--) {
            if (band != 0 && band != dataset.bandCount() - 1 && band % labelStep != 0) {
                continue;
            }
            Point2D point = geometry.project(1, zeroNorm, band);
            g.setColor(dataset.colors()[band]);
            g.fillOval((int) point.getX() + 8, (int) point.getY() - 4, 8, 8);
            g.drawString(dataset.labels()[band], (float) point.getX() + 21, (float) point.getY() + 4);
        }
    }

    private void paintOrientationHint(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(MUTED.getRed(), MUTED.getGreen(), MUTED.getBlue(), 185));
        String hint = "Trascina: ruota prospettiva   ·   Rotella: zoom   ·   Doppio clic: centra";
        g.drawString(hint, 18, getHeight() - 17);
    }

    private void updateHover(Point mouse) {
        ProjectedPoint nearest = null;
        double best = 12 * 12;
        for (ProjectedPoint projected : projectedPoints) {
            double dx = projected.point().getX() - mouse.x;
            double dy = projected.point().getY() - mouse.y;
            double distance = dx * dx + dy * dy;
            if (distance < best) {
                best = distance;
                nearest = projected;
            }
        }
        if (nearest == null) {
            if (hover != null) {
                hover = null;
                repaint();
            }
            return;
        }
        hover = new HoverPoint(nearest.point(), nearest.band(), nearest.index());
        repaint();
    }

    private void paintHover(Graphics2D g, HoverPoint hovered) {
        int band = hovered.band();
        int index = hovered.index();
        Point2D point = hovered.point();

        g.setColor(Color.WHITE);
        g.fillOval((int) point.getX() - 4, (int) point.getY() - 4, 8, 8);
        g.setColor(dataset.colors()[band]);
        g.setStroke(new BasicStroke(2f));
        g.drawOval((int) point.getX() - 7, (int) point.getY() - 7, 14, 14);

        String[] lines = {
                dataset.labels()[band],
                "Tempo: " + VALUE_FORMAT.format(dataset.times()[index]) + " s",
                presentation.valueLabel() + ": " + VALUE_FORMAT.format(dataset.rates()[band][index])
                        + presentation.valueUnit()
        };
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics metrics = g.getFontMetrics();
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, metrics.stringWidth(line));
        }
        width += 24;
        int height = 66;
        int x = (int) point.getX() + 16;
        int y = (int) point.getY() - height - 12;
        if (x + width > getWidth() - 10) {
            x = (int) point.getX() - width - 16;
        }
        if (y < 10) {
            y = (int) point.getY() + 16;
        }

        g.setColor(TOOLTIP_BG);
        g.fillRoundRect(x, y, width, height, 14, 14);
        g.setColor(withAlpha(dataset.colors()[band], 180));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, y, width, height, 14, 14);

        g.setColor(dataset.colors()[band]);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(lines[0], x + 12, y + 19);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString(lines[1], x + 12, y + 38);
        g.drawString(lines[2], x + 12, y + 56);
    }

    private static int peakIndex(double[] values) {
        int result = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < values.length; index++) {
            if (Double.isFinite(values[index]) && values[index] > max) {
                max = values[index];
                result = index;
            }
        }
        return result;
    }

    private static int labelStep(int count, int maximumLabels) {
        if (count <= 1 || maximumLabels <= 1 || count <= maximumLabels) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil((count - 1.0) / (maximumLabels - 1.0)));
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clampInt(alpha, 0, 255));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double normalize(double value, double min, double max) {
        if (max <= min) {
            return 0.5;
        }
        return clamp((value - min) / (max - min), 0, 1);
    }

    private static double lerp(double start, double end, double fraction) {
        return start + (end - start) * fraction;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatAxis(double value) {
        double absolute = Math.abs(value);
        if (absolute >= 100) {
            return String.format(Locale.US, "%.0f", value);
        }
        if (absolute >= 10) {
            return String.format(Locale.US, "%.1f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    public record Dataset(double[] times, double[][] rates, String[] labels, Color[] colors) {
        public static Dataset empty() {
            return new Dataset(new double[0], new double[0][], new String[0], new Color[0]);
        }

        public Dataset {
            times = times == null ? new double[0] : times;
            rates = rates == null ? new double[0][] : rates;
            labels = labels == null ? new String[0] : labels;
            colors = colors == null ? new Color[0] : colors;
        }

        public boolean isEmpty() {
            if (times.length == 0 || rates.length == 0 || labels.length != rates.length
                    || colors.length != rates.length) {
                return true;
            }
            for (double[] series : rates) {
                if (series == null || series.length != times.length) {
                    return true;
                }
            }
            return false;
        }

        public int bandCount() {
            return rates.length;
        }

        public Dataset copy() {
            double[][] copiedRates = new double[rates.length][];
            for (int index = 0; index < rates.length; index++) {
                copiedRates[index] = Arrays.copyOf(rates[index], rates[index].length);
            }
            return new Dataset(Arrays.copyOf(times, times.length), copiedRates,
                    Arrays.copyOf(labels, labels.length), Arrays.copyOf(colors, colors.length));
        }
    }

    public record Presentation(String emptyMessage, String xAxisLabel, String yAxisLabel,
                               String depthAxisLabel, String valueLabel, String valueUnit,
                               boolean showPeakMarkers, boolean showAreaFill, boolean denseSeries,
                               int maxDepthLabels) {
        public Presentation {
            emptyMessage = textOr(emptyMessage, "Nessun dato disponibile per la vista 3D.");
            xAxisLabel = textOr(xAxisLabel, "Tempo dal trigger (s)");
            yAxisLabel = textOr(yAxisLabel, "Rate (count/s)");
            depthAxisLabel = depthAxisLabel == null ? "" : depthAxisLabel;
            valueLabel = textOr(valueLabel, "Rate");
            valueUnit = valueUnit == null ? "" : valueUnit;
            maxDepthLabels = Math.max(2, maxDepthLabels);
        }

        public static Presentation explorerDefaults() {
            return new Presentation(
                    "Nessun dato a quattro bande disponibile per la vista 3D.",
                    "Tempo dal trigger (s)", "Rate (count/s)", "Bande energetiche",
                    "Rate", " count/s", true, true, false, 8);
        }

        private static String textOr(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    private record Bounds(double minTime, double maxTime, double minRate, double maxRate) {
    }

    private record ProjectedPoint(Point2D point, int band, int index) {
    }

    private record HoverPoint(Point2D point, int band, int index) {
    }

    private static final class Geometry {
        private final int width;
        private final int height;
        private final int bandCount;
        private final double yaw;
        private final double pitch;
        private final double zoom;
        private final double plotWidth;
        private final double plotHeight;
        private final double centerX;
        private final double baselineY;
        private final double depthX;
        private final double depthY;

        private Geometry(int width, int height, int bandCount, double yaw, double pitch, double zoom) {
            this.width = width;
            this.height = height;
            this.bandCount = Math.max(1, bandCount);
            this.yaw = yaw;
            this.pitch = pitch;
            this.zoom = zoom;
            this.plotWidth = Math.max(350, width * 0.68) * zoom;
            this.plotHeight = Math.max(190, height * 0.60) * zoom;
            this.centerX = width * 0.45;
            this.baselineY = height * 0.74;
            this.depthX = (38 + 70 * yaw) * zoom;
            this.depthY = (20 + 46 * pitch) * zoom;
        }

        private Point2D project(double xNorm, double yNorm, int band) {
            double zNorm = bandCount <= 1 ? 0 : band / (double) (bandCount - 1);
            double x = centerX + (xNorm - 0.5) * plotWidth + zNorm * depthX;
            double y = baselineY - yNorm * plotHeight - zNorm * depthY;
            return new Point2D.Double(x, y);
        }
    }
}
