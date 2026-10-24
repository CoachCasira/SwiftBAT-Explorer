package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.ui.I18n;

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
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import javax.swing.Timer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/** Renderer scientifico 2.5D basato su Java2D. */
public final class Java2DWaterfallPanel extends JPanel {
    private static final long serialVersionUID = 1L;

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
    private final List<ProjectedSegment> projectedSegments = new ArrayList<>();
    private final List<ProjectedGap> projectedGaps = new ArrayList<>();
    private final Set<Integer> focusedBands = new LinkedHashSet<>();

    private double yaw = 0.32;
    private double pitch = 0.72;
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private int dragStartX;
    private int dragStartY;
    private double dragStartYaw;
    private double dragStartPitch;
    private boolean draggedSincePress;
    private boolean interactionLocked;
    private String spotlightLabel;
    private HoverPoint hover;
    private GapHover hoverGap;
    private DoubleConsumer zoomListener = value -> { };
    private Consumer<Set<String>> focusListener = ignored -> { };
    private Consumer<String> spotlightListener = ignored -> { };
    // Full-quality static frame. Hovering a curve must not redraw a hundred
    // glow paths. The rendered image is invalidated on any visual state change.
    private BufferedImage staticFrame;
    private boolean staticFrameDirty = true;
    private Bounds cachedBounds;
    private boolean draggingPreview;
    private boolean paintingHoverOverlay;
    private final Timer dragFrameTimer = new Timer(33, event -> repaint());
    private long lastHoverSearchNanos;

    public Java2DWaterfallPanel() {
        setOpaque(true);
        setBackground(BACKGROUND_TOP);
        setPreferredSize(new Dimension(900, 520));
        setMinimumSize(new Dimension(500, 330));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        dragFrameTimer.setRepeats(true);

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                dragStartX = event.getX();
                dragStartY = event.getY();
                dragStartYaw = yaw;
                dragStartPitch = pitch;
                draggedSincePress = false;
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override public void mouseReleased(MouseEvent event) {
                dragFrameTimer.stop();
                if (draggingPreview) {
                    draggingPreview = false;
                    invalidateStaticFrame();
                    repaint(); // Restore the exact full-quality view after the drag.
                }
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (Math.abs(event.getX() - dragStartX) <= 3 && Math.abs(event.getY() - dragStartY) <= 3
                        && !draggedSincePress) return;
                draggedSincePress = true;
                draggingPreview = true;
                yaw = clamp(dragStartYaw + (event.getX() - dragStartX) * 0.0028, -0.72, 0.72);
                pitch = clamp(dragStartPitch - (event.getY() - dragStartY) * 0.0032, 0.30, 1.12);
                hover = null;
                hoverGap = null;
                // One animation repaint per ~33 ms, not one per raw mouse event.
                if (!dragFrameTimer.isRunning()) {
                    repaint();
                    dragFrameTimer.start();
                }
            }

            @Override public void mouseMoved(MouseEvent event) {
                updateHover(event.getPoint());
            }

            @Override public void mouseExited(MouseEvent event) {
                if (hover != null || hoverGap != null) {
                    hover = null;
                    hoverGap = null;
                    repaint();
                }
            }

            @Override public void mouseClicked(MouseEvent event) {
                if (draggedSincePress) return;
                invalidateStaticFrame();

                if (interactionLocked) {
                    if (SwingUtilities.isRightMouseButton(event)) {
                        spotlightLabel = null;
                        hover = null;
                        notifySpotlightChanged();
                        repaint();
                        return;
                    }
                    if (!SwingUtilities.isLeftMouseButton(event)) return;
                    int band = nearestBand(event.getPoint(), true);
                    if (band >= 0 && focusedBands.contains(band)) {
                        String clicked = labelForBand(band);
                        spotlightLabel = clicked != null && clicked.equals(spotlightLabel) ? null : clicked;
                        hover = null;
                        notifySpotlightChanged();
                        repaint();
                    }
                    return;
                }

                if (!SwingUtilities.isLeftMouseButton(event)) return;
                int band = nearestBand(event.getPoint(), false);
                if (event.getClickCount() >= 2) {
                    if (band < 0 && !focusedBands.isEmpty()) {
                        focusedBands.clear();
                        hover = null;
                        notifyFocusChanged();
                        repaint();
                    } else {
                        focusedBands.clear();
                        notifyFocusChanged();
                        resetView();
                    }
                    return;
                }
                if (event.getClickCount() == 1 && band >= 0) {
                    if (!focusedBands.add(band)) focusedBands.remove(band);
                    hover = null;
                    notifyFocusChanged();
                    repaint();
                }
            }

            @Override public void mouseWheelMoved(MouseWheelEvent event) {
                zoomAt(event.getX(), event.getY(), event.getPreciseWheelRotation());
                hover = null;
                invalidateStaticFrame();
                zoomListener.accept(zoom);
                repaint();
                event.consume();
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
        hoverGap = null;
        cachedBounds = null;
        invalidateStaticFrame();
        focusedBands.clear();
        spotlightLabel = null;
        hover = null;
        repaint();
    }

    /** Applies a focus selection by dataset label without emitting a user-change callback. */
    public void setFocusedLabels(Set<String> labels) {
        if (!SwingUtilities.isEventDispatchThread()) {
            Set<String> copy = labels == null ? Set.of() : Set.copyOf(labels);
            SwingUtilities.invokeLater(() -> setFocusedLabels(copy));
            return;
        }
        focusedBands.clear();
        if (labels != null && !labels.isEmpty()) {
            for (int band = 0; band < dataset.labels().length; band++) {
                String label = dataset.labels()[band];
                if (label != null && labels.contains(label)) focusedBands.add(band);
            }
        }
        if (spotlightLabel != null && !focusedLabels().contains(spotlightLabel)) spotlightLabel = null;
        hover = null;
        hoverGap = null;
        invalidateStaticFrame();
        repaint();
    }

    public Set<String> focusedLabels() {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (Integer band : focusedBands) {
            if (band != null && band >= 0 && band < dataset.labels().length) labels.add(dataset.labels()[band]);
        }
        return Set.copyOf(labels);
    }

    public void setFocusListener(Consumer<Set<String>> listener) {
        focusListener = listener == null ? ignored -> { } : listener;
    }

    public void setSpotlightListener(Consumer<String> listener) {
        spotlightListener = listener == null ? ignored -> { } : listener;
    }

    public void setInteractionLocked(boolean locked) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setInteractionLocked(locked));
            return;
        }
        interactionLocked = locked && !focusedBands.isEmpty();
        if (!interactionLocked) spotlightLabel = null;
        hover = null;
        invalidateStaticFrame();
        repaint();
    }

    public void setSpotlightLabel(String label) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setSpotlightLabel(label));
            return;
        }
        if (!interactionLocked || label == null || !focusedLabels().contains(label)) spotlightLabel = null;
        else spotlightLabel = label;
        hover = null;
        invalidateStaticFrame();
        repaint();
    }

    public void setPresentation(Presentation newPresentation) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setPresentation(newPresentation));
            return;
        }
        presentation = newPresentation == null ? Presentation.explorerDefaults() : newPresentation;
        hover = null;
        invalidateStaticFrame();
        repaint();
    }

    public void setZoomListener(DoubleConsumer listener) {
        zoomListener = listener == null ? value -> { } : listener;
        zoomListener.accept(zoom);
    }

    public void resetView() {
        yaw = 0.32;
        pitch = 0.72;
        zoom = 1.0;
        panX = 0;
        panY = 0;
        hover = null;
        zoomListener.accept(zoom);
        invalidateStaticFrame();
        repaint();
    }

    private void notifyFocusChanged() {
        focusListener.accept(focusedLabels());
    }

    private void notifySpotlightChanged() {
        spotlightListener.accept(spotlightLabel);
    }

    private void zoomAt(double mouseX, double mouseY, double wheelRotation) {
        double oldZoom = zoom;
        double newZoom = clamp(oldZoom * Math.pow(1.08, -wheelRotation), 0.68, 1.85);
        if (Math.abs(newZoom - oldZoom) < 1e-9) return;
        double baseX = getWidth() * 0.45;
        double baseY = getHeight() * 0.74;
        double ratio = newZoom / oldZoom;
        panX = mouseX - baseX - ratio * (mouseX - baseX - panX);
        panY = mouseY - baseY - ratio * (mouseY - baseY - panY);
        panX = clamp(panX, -getWidth() * 1.5, getWidth() * 1.5);
        panY = clamp(panY, -getHeight() * 1.5, getHeight() * 1.5);
        zoom = newZoom;
    }

    /** Refreshes translated labels without rebuilding scientific data. */
    public void refreshLocalizedText() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshLocalizedText);
            return;
        }
        invalidateStaticFrame();
        repaint();
    }

    private void invalidateStaticFrame() {
        staticFrameDirty = true;
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            if (dataset.isEmpty()) {
                configure(g);
                paintBackground(g);
                paintEmpty(g);
                return;
            }
            if (draggingPreview) {
                // Temporary lightweight preview only while rotating the view.
                // No change to data or to the image rendered after mouse release.
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                paintBackground(g);
                paintChart(g);
                return;
            }
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;
            if (staticFrame == null || staticFrameDirty
                    || staticFrame.getWidth() != width || staticFrame.getHeight() != height) {
                BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D offscreen = frame.createGraphics();
                HoverPoint retainedHover = hover;
                try {
                    // Cache the frame without mouse highlighting. Hovering then
                    // paints only one curve, never all 100+ curves again.
                    hover = null;
                    configure(offscreen);
                    paintBackground(offscreen);
                    paintChart(offscreen);
                } finally {
                    hover = retainedHover;
                    offscreen.dispose();
                }
                staticFrame = frame;
                staticFrameDirty = false;
            }
            g.drawImage(staticFrame, 0, 0, null);
            if (hoverGap != null) {
                configure(g);
                paintGapHover(g, hoverGap);
            } else if (hover != null) {
                configure(g);
                int band = hover.band();
                if (band >= 0 && band < dataset.bandCount()) {
                    // The selected line is painted on top of the cached image,
                    // improving visibility without connecting invalid/missing data.
                    paintingHoverOverlay = true;
                    try {
                        Geometry geometry = new Geometry(getWidth(), getHeight(),
                                dataset.bandCount(), yaw, pitch, zoom, panX, panY);
                        paintBand(g, band, calculateBounds(), geometry);
                    } finally {
                        paintingHoverOverlay = false;
                    }
                }
                paintHover(g, hover);
            }
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
        String text = I18n.t(presentation.emptyMessage());
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, getHeight() / 2);
    }

    private void paintChart(Graphics2D g) {
        projectedPoints.clear();
        projectedSegments.clear();
        projectedGaps.clear();
        Bounds bounds = calculateBounds();
        Geometry geometry = new Geometry(getWidth(), getHeight(), dataset.bandCount(), yaw, pitch, zoom, panX, panY);
        paintGrid(g, bounds, geometry);
        paintTrigger(g, bounds, geometry);

        int spotlightBand = spotlightBandIndex();
        for (int band = dataset.bandCount() - 1; band >= 0; band--) {
            if (band != spotlightBand) paintBand(g, band, bounds, geometry);
        }
        if (spotlightBand >= 0) paintBand(g, spotlightBand, bounds, geometry);

        paintAxesAndLabels(g, bounds, geometry);
        paintOrientationHint(g);
    }

    private Bounds calculateBounds() {
        if (cachedBounds != null) return cachedBounds;
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
        if (maxTime <= minTime) maxTime = minTime + 1;
        if (maxRate <= 0) maxRate = 1;
        cachedBounds = new Bounds(minTime, maxTime, minRate < 0 ? minRate * 1.30 : -maxRate * 0.08, maxRate * 1.12);
        return cachedBounds;
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
        double zeroNorm = normalize(0, bounds.minRate(), bounds.maxRate());
        for (int band = 0; band < dataset.bandCount(); band++) {
            Point2D left = geometry.project(0, zeroNorm, band);
            Point2D right = geometry.project(1, zeroNorm, band);
            int baselineAlpha = presentation.denseSeries() ? 24 : band == 0 ? 90 : 55;
            g.setColor(withAlpha(dataset.colors()[band], focusAlpha(band, baselineAlpha)));
            g.setStroke(new BasicStroke(presentation.denseSeries() ? 0.8f : band == 0 ? 1.5f : 1f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{5f, 7f}, 0f));
            g.draw(new Line2D.Double(left, right));
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void paintTrigger(Graphics2D g, Bounds bounds, Geometry geometry) {
        if (0 < bounds.minTime() || 0 > bounds.maxTime()) return;
        double xNorm = normalize(0, bounds.minTime(), bounds.maxTime());
        Point2D bottomFront = geometry.project(xNorm, normalize(0, bounds.minRate(), bounds.maxRate()), 0);
        Point2D topBack = geometry.project(xNorm, 1, dataset.bandCount() - 1);
        g.setColor(new Color(TRIGGER.getRed(), TRIGGER.getGreen(), TRIGGER.getBlue(), focusedBands.isEmpty() ? 175 : 90));
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{7f, 7f}, 0f));
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
        Point2D[] points = presentation.showAreaFill() || presentation.showPeakMarkers()
                ? new Point2D[times.length] : null;
        boolean drawingSegment = false;
        boolean allFinite = true;
        Point2D previousPoint = null;
        int previousIndex = -1;
        // Rendering aid only: a dashed connector identifies a true gap
        // between two observed samples on the hovered population curve.
        // The rate series and the hit-test index remain untouched.
        boolean annotateGaps = presentation.denseSeries() && !draggingPreview
                && (paintingHoverOverlay || focusedBands.contains(band));
        List<Line2D.Double> missingDataBridges = annotateGaps ? new ArrayList<>() : null;
        Point2D lastObserved = null;
        boolean inMissingInterval = false;

        for (int index = 0; index < times.length; index++) {
            if (!Double.isFinite(times[index]) || !Double.isFinite(values[index])) {
                drawingSegment = false;
                allFinite = false;
                previousPoint = null;
                previousIndex = -1;
                if (lastObserved != null) inMissingInterval = true;
                continue;
            }
            Point2D point = geometry.project(normalize(times[index], bounds.minTime(), bounds.maxTime()),
                    normalize(values[index], bounds.minRate(), bounds.maxRate()), band);
            if (points != null) points[index] = point;
            if (missingDataBridges != null && inMissingInterval && lastObserved != null) {
                missingDataBridges.add(new Line2D.Double(lastObserved, point));
                if (!paintingHoverOverlay) projectedGaps.add(new ProjectedGap(lastObserved, point, band));
            }
            lastObserved = point;
            inMissingInterval = false;
            if (!drawingSegment) {
                line.moveTo(point.getX(), point.getY());
                drawingSegment = true;
            } else {
                line.lineTo(point.getX(), point.getY());
            }
            if (!draggingPreview && !paintingHoverOverlay) {
                projectedPoints.add(new ProjectedPoint(point, band, index));
                if (previousPoint != null && previousIndex >= 0) {
                    projectedSegments.add(new ProjectedSegment(previousPoint, point, band, previousIndex, index));
                }
            }
            previousPoint = point;
            previousIndex = index;
        }

        if (presentation.showAreaFill() && allFinite && points.length >= 2) {
            Path2D area = new Path2D.Double();
            Point2D firstBase = geometry.project(0, zeroNorm, band);
            area.moveTo(firstBase.getX(), firstBase.getY());
            for (Point2D point : points) area.lineTo(point.getX(), point.getY());
            Point2D lastBase = geometry.project(1, zeroNorm, band);
            area.lineTo(lastBase.getX(), lastBase.getY());
            area.closePath();
            g.setColor(withAlpha(color, focusAlpha(band, band == 0 ? 34 : 25)));
            g.fill(area);
        }

        boolean medianBand = isMedianBand(band);
        boolean quartileBand = isQuartileBand(band);
        boolean spotlight = isSpotlightBand(band);
        int glowAlpha = spotlight ? 150 : medianBand ? 70 : quartileBand ? 40 : presentation.denseSeries() ? 18 : 55;
        float glowWidth = spotlight ? 12.0f : medianBand ? 8.0f : quartileBand ? 5.0f : presentation.denseSeries() ? 3.0f : 7.5f;
        if (!draggingPreview) {
            g.setColor(withAlpha(color, focusAlpha(band, glowAlpha)));
            g.setStroke(new BasicStroke(glowWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(line);
        }

        int lineAlpha = spotlight ? 255 : medianBand ? 255 : quartileBand ? 235 : presentation.denseSeries() ? 105 : band == 0 ? 245 : 220;
        float lineWidth = spotlight ? 3.6f : medianBand ? 4.2f : quartileBand ? 2.4f : presentation.denseSeries() ? 1.05f : band == 0 ? 2.8f : 2.35f;
        if (!isBandActive(band)) lineWidth = Math.min(1.0f, lineWidth);
        g.setColor(withAlpha(color, focusAlpha(band, lineAlpha)));
        if (quartileBand) {
            g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{8f, 6f}, 0f));
        } else {
            g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g.draw(line);
        if (missingDataBridges != null && !missingDataBridges.isEmpty()) {
            // Dashed gaps are visual annotations, NOT interpolated data.
            g.setColor(withAlpha(color, 230));
            g.setStroke(new BasicStroke(1.65f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[]{3f, 6f}, 0f));
            for (Line2D.Double bridge : missingDataBridges) g.draw(bridge);
        }

        int peakIndex = presentation.showPeakMarkers() && isBandActive(band) ? peakIndex(values) : -1;
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

    private int spotlightBandIndex() {
        if (!interactionLocked || spotlightLabel == null) return -1;
        for (int band = 0; band < dataset.labels().length; band++) {
            if (spotlightLabel.equals(dataset.labels()[band])) return band;
        }
        return -1;
    }

    private boolean isSpotlightBand(int band) {
        return band == spotlightBandIndex();
    }

    private boolean isBandActive(int band) {
        return focusedBands.isEmpty() || focusedBands.contains(band)
                || (!interactionLocked && hover != null && hover.band() == band);
    }

    private int focusAlpha(int band, int normalAlpha) {
        return isBandActive(band) ? normalAlpha : Math.max(7, (int) Math.round(normalAlpha * 0.11));
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
        for (int tick = 0; tick <= 8; tick++) {
            double fraction = tick / 8.0;
            double time = lerp(bounds.minTime(), bounds.maxTime(), fraction);
            Point2D point = geometry.project(fraction, zeroNorm, 0);
            String label = formatAxis(time);
            g.drawString(label, (float) point.getX() - g.getFontMetrics().stringWidth(label) / 2f, (float) point.getY() + 20);
        }
        for (int tick = 0; tick <= 5; tick++) {
            double fraction = tick / 5.0;
            double rate = lerp(bounds.minRate(), bounds.maxRate(), fraction);
            Point2D point = geometry.project(0, fraction, 0);
            String label = formatAxis(rate);
            g.drawString(label, (float) point.getX() - g.getFontMetrics().stringWidth(label) - 10, (float) point.getY() + 4);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(TEXT);
        String xLabel = I18n.t(presentation.xAxisLabel());
        g.drawString(xLabel,
                (float) ((xStart.getX() + xEnd.getX()) / 2 - g.getFontMetrics().stringWidth(xLabel) / 2.0),
                (float) Math.max(xStart.getY(), xEnd.getY()) + 43);
        g.drawString(I18n.t(presentation.yAxisLabel()), (float) yTop.getX() - 42, (float) yTop.getY() - 12);

        if (!presentation.depthAxisLabel().isBlank()) {
            g.setColor(new Color(190, 166, 250));
            String depthLabel = I18n.t(presentation.depthAxisLabel());
            int labelWidth = g.getFontMetrics().stringWidth(depthLabel);
            g.drawString(depthLabel, (float) clamp(depthEnd.getX() + 8, 12, getWidth() - labelWidth - 12),
                    (float) clamp(depthEnd.getY() - 9, 20, getHeight() - 45));
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        int labelStep = labelStep(dataset.bandCount(), presentation.maxDepthLabels());
        for (int band = dataset.bandCount() - 1; band >= 0; band--) {
            if (band != 0 && band != dataset.bandCount() - 1 && band % labelStep != 0 && !isSpotlightBand(band)) continue;
            Point2D point = geometry.project(1, zeroNorm, band);
            Color labelColor = withAlpha(dataset.colors()[band], isBandActive(band) ? 255 : 48);
            g.setColor(labelColor);
            int diameter = isSpotlightBand(band) ? 10 : 8;
            g.fillOval((int) point.getX() + 8, (int) point.getY() - diameter / 2, diameter, diameter);
            g.drawString(I18n.t(dataset.labels()[band]), (float) point.getX() + 21, (float) point.getY() + 4);
        }
    }

    private void paintOrientationHint(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(MUTED.getRed(), MUTED.getGreen(), MUTED.getBlue(), 185));
        String hint = I18n.t("Trascina: ruota prospettiva   ·   Rotella: zoom   ·   Doppio clic: centra");
        g.drawString(hint, 18, getHeight() - 17);
    }

    private int nearestBand(Point mouse, boolean respectFocus) {
        HoverPoint nearest = nearestHit(mouse, respectFocus, -1);
        return nearest == null ? -1 : nearest.band();
    }

    private HoverPoint nearestHit(Point mouse, boolean respectFocus, int onlyBand) {
        HoverPoint nearest = null;
        double best = 15.0 * 15.0;

        for (ProjectedSegment segment : projectedSegments) {
            if (onlyBand >= 0 && segment.band() != onlyBand) continue;
            if (respectFocus && !focusedBands.contains(segment.band())) continue;
            SegmentProjection projection = project(mouse, segment.start(), segment.end());
            if (projection.distanceSquared() >= best) continue;
            best = projection.distanceSquared();
            double time = lerp(dataset.times()[segment.startIndex()], dataset.times()[segment.endIndex()], projection.fraction());
            double value = lerp(dataset.rates()[segment.band()][segment.startIndex()],
                    dataset.rates()[segment.band()][segment.endIndex()], projection.fraction());
            Point2D point = new Point2D.Double(
                    lerp(segment.start().getX(), segment.end().getX(), projection.fraction()),
                    lerp(segment.start().getY(), segment.end().getY(), projection.fraction()));
            nearest = new HoverPoint(point, segment.band(), time, value);
        }

        for (ProjectedPoint projected : projectedPoints) {
            if (onlyBand >= 0 && projected.band() != onlyBand) continue;
            if (respectFocus && !focusedBands.contains(projected.band())) continue;
            double dx = projected.point().getX() - mouse.x;
            double dy = projected.point().getY() - mouse.y;
            double distance = dx * dx + dy * dy;
            if (distance < best) {
                best = distance;
                nearest = new HoverPoint(projected.point(), projected.band(),
                        dataset.times()[projected.index()], dataset.rates()[projected.band()][projected.index()]);
            }
        }
        return nearest;
    }

    private SegmentProjection project(Point mouse, Point2D start, Point2D end) {
        double vx = end.getX() - start.getX();
        double vy = end.getY() - start.getY();
        double lengthSquared = vx * vx + vy * vy;
        if (lengthSquared < 1e-9) {
            double dx = start.getX() - mouse.x;
            double dy = start.getY() - mouse.y;
            return new SegmentProjection(0, dx * dx + dy * dy);
        }
        double fraction = ((mouse.x - start.getX()) * vx + (mouse.y - start.getY()) * vy) / lengthSquared;
        fraction = clamp(fraction, 0, 1);
        double x = start.getX() + fraction * vx;
        double y = start.getY() + fraction * vy;
        double dx = x - mouse.x;
        double dy = y - mouse.y;
        return new SegmentProjection(fraction, dx * dx + dy * dy);
    }

    private void updateHover(Point mouse) {
        // The scene may receive hundreds of mouse events per second on modern
        // trackpads. A 60 Hz hit-test cap preserves responsive tooltips.
        long now = System.nanoTime();
        if (now - lastHoverSearchNanos < 16_000_000L) return;
        lastHoverSearchNanos = now;
        boolean respectFocus = interactionLocked && !focusedBands.isEmpty();
        int onlyBand = interactionLocked && spotlightLabel != null ? spotlightBandIndex() : -1;
        // A dashed gap is not a measured value; give it its own explanation.
        GapHover gap = nearestGap(mouse, onlyBand);
        if (gap != null) {
            hover = null;
            hoverGap = gap;
            repaint();
            return;
        }
        HoverPoint nearest = nearestHit(mouse, respectFocus, onlyBand);
        boolean wasVisible = hover != null || hoverGap != null;
        hoverGap = null;
        if (wasVisible || nearest != null) {
            hover = nearest;
            repaint();
        }
    }

    private GapHover nearestGap(Point mouse, int onlyBand) {
        GapHover nearest = null;
        double closest = 10.0 * 10.0;
        for (ProjectedGap gap : projectedGaps) {
            if (onlyBand >= 0 && gap.band() != onlyBand) continue;
            if (!focusedBands.contains(gap.band())) continue;
            SegmentProjection result = project(mouse, gap.start(), gap.end());
            if (result.distanceSquared() >= closest) continue;
            closest = result.distanceSquared();
            Point2D center = new Point2D.Double(
                    lerp(gap.start().getX(), gap.end().getX(), result.fraction()),
                    lerp(gap.start().getY(), gap.end().getY(), result.fraction()));
            nearest = new GapHover(center, gap.band());
        }
        return nearest;
    }

    private void paintGapHover(Graphics2D g, GapHover gap) {
        Point2D point = gap.point();
        Color color = dataset.colors()[gap.band()];
        g.setColor(Color.WHITE);
        g.fillOval((int) point.getX() - 4, (int) point.getY() - 4, 8, 8);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f));
        g.drawOval((int) point.getX() - 7, (int) point.getY() - 7, 14, 14);
        String[] lines = {
                I18n.t(labelForBand(gap.band())),
                I18n.dynamic("Intervallo senza campioni validi", "Interval without valid samples"),
                I18n.dynamic("Tratteggio indicativo: nessuna interpolazione", "Dashed guide: no interpolation")
        };
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics metrics = g.getFontMetrics();
        int width = 0;
        for (String line : lines) width = Math.max(width, metrics.stringWidth(line));
        width += 24;
        int height = 70;
        int x = (int) point.getX() + 16;
        int y = (int) point.getY() - height - 12;
        if (x + width > getWidth() - 10) x = (int) point.getX() - width - 16;
        if (y < 10) y = (int) point.getY() + 16;
        g.setColor(TOOLTIP_BG);
        g.fillRoundRect(x, y, width, height, 14, 14);
        g.setColor(withAlpha(color, 180));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, y, width, height, 14, 14);
        g.setColor(color);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(lines[0], x + 12, y + 19);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString(lines[1], x + 12, y + 39);
        g.drawString(lines[2], x + 12, y + 57);
    }

    private void paintHover(Graphics2D g, HoverPoint hovered) {
        int band = hovered.band();
        Point2D point = hovered.point();
        g.setColor(Color.WHITE);
        g.fillOval((int) point.getX() - 4, (int) point.getY() - 4, 8, 8);
        g.setColor(dataset.colors()[band]);
        g.setStroke(new BasicStroke(2f));
        g.drawOval((int) point.getX() - 7, (int) point.getY() - 7, 14, 14);

        String[] lines = {
                I18n.t(dataset.labels()[band]),
                I18n.t("Tempo") + ": " + VALUE_FORMAT.format(hovered.time()) + " s",
                I18n.t(presentation.valueLabel()) + ": " + VALUE_FORMAT.format(hovered.value()) + presentation.valueUnit()
        };
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics metrics = g.getFontMetrics();
        int width = 0;
        for (String line : lines) width = Math.max(width, metrics.stringWidth(line));
        width += 24;
        int height = 66;
        int x = (int) point.getX() + 16;
        int y = (int) point.getY() - height - 12;
        if (x + width > getWidth() - 10) x = (int) point.getX() - width - 16;
        if (y < 10) y = (int) point.getY() + 16;
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
        if (count <= 1 || maximumLabels <= 1 || count <= maximumLabels) return 1;
        return Math.max(1, (int) Math.ceil((count - 1.0) / (maximumLabels - 1.0)));
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clampInt(alpha, 0, 255));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double normalize(double value, double min, double max) {
        return max <= min ? 0.5 : clamp((value - min) / (max - min), 0, 1);
    }

    private static double lerp(double start, double end, double fraction) {
        return start + (end - start) * fraction;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatAxis(double value) {
        double absolute = Math.abs(value);
        if (absolute >= 100) return String.format(Locale.US, "%.0f", value);
        if (absolute >= 10) return String.format(Locale.US, "%.1f", value);
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
            if (times.length == 0 || rates.length == 0 || labels.length != rates.length || colors.length != rates.length) return true;
            for (double[] series : rates) if (series == null || series.length != times.length) return true;
            return false;
        }

        public int bandCount() {
            return rates.length;
        }

        public Dataset copy() {
            double[][] copiedRates = new double[rates.length][];
            for (int index = 0; index < rates.length; index++) copiedRates[index] = Arrays.copyOf(rates[index], rates[index].length);
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
            return new Presentation("Nessun dato a quattro bande disponibile per la vista 3D.",
                    "Tempo dal trigger (s)", "Rate (count/s)", "Bande energetiche",
                    "Rate", " count/s", true, true, false, 8);
        }

        private static String textOr(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    private record Bounds(double minTime, double maxTime, double minRate, double maxRate) { }
    private record ProjectedPoint(Point2D point, int band, int index) { }
    private record ProjectedSegment(Point2D start, Point2D end, int band, int startIndex, int endIndex) { }
    private record ProjectedGap(Point2D start, Point2D end, int band) { }
    private record HoverPoint(Point2D point, int band, double time, double value) { }
    private record GapHover(Point2D point, int band) { }
    private record SegmentProjection(double fraction, double distanceSquared) { }

    private static final class Geometry {
        private final int bandCount;
        private final double plotWidth;
        private final double plotHeight;
        private final double centerX;
        private final double baselineY;
        private final double depthX;
        private final double depthY;

        private Geometry(int width, int height, int bandCount, double yaw, double pitch, double zoom,
                         double panX, double panY) {
            this.bandCount = Math.max(1, bandCount);
            this.plotWidth = Math.max(350, width * 0.68) * zoom;
            this.plotHeight = Math.max(190, height * 0.60) * zoom;
            this.centerX = width * 0.45 + panX;
            this.baselineY = height * 0.74 + panY;
            this.depthX = (38 + 70 * yaw) * zoom;
            this.depthY = (20 + 46 * pitch) * zoom;
        }

        private Point2D project(double xNorm, double yNorm, int band) {
            double zNorm = bandCount <= 1 ? 0 : band / (double) (bandCount - 1);
            return new Point2D.Double(centerX + (xNorm - 0.5) * plotWidth + zNorm * depthX,
                    baselineY - yNorm * plotHeight - zNorm * depthY);
        }
    }
}
