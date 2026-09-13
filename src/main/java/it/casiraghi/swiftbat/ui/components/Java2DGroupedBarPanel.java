package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.ui.UiTranslations;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Istogramma raggruppato interattivo con proiezione prospettica. */
public final class Java2DGroupedBarPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND_TOP = new Color(6, 13, 29);
    private static final Color BACKGROUND_BOTTOM = new Color(9, 23, 48);
    private static final Color GRID = new Color(84, 111, 160, 72);
    private static final Color TEXT = new Color(226, 236, 253);
    private static final Color MUTED = new Color(137, 159, 198);
    private static final Color TOOLTIP_BG = new Color(9, 18, 38, 240);

    // Exact cyan -> blue -> purple language used by the 2D bars.
    private static final Color SPECTRAL_CYAN = new Color(17, 207, 233);
    private static final Color SPECTRAL_BLUE = new Color(40, 127, 242);
    private static final Color SPECTRAL_PURPLE = new Color(174, 93, 244);

    // Population 3D uses the same 2D palette, but one anchor per depth band
    // so short/long/n.d. (or with/without redshift) remain distinguishable.
    private static final Color POPULATION_CYAN = SPECTRAL_CYAN;
    private static final Color POPULATION_BLUE = SPECTRAL_BLUE;
    private static final Color POPULATION_PURPLE = SPECTRAL_PURPLE;

    private Dataset dataset = Dataset.empty();
    private final List<BarHit> bars = new ArrayList<>();
    private double yaw = 0.34;
    private double pitch = 0.70;
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private int dragStartX;
    private int dragStartY;
    private double dragStartYaw;
    private double dragStartPitch;
    private BarHit hover;

    public Java2DGroupedBarPanel() {
        setOpaque(true);
        setBackground(BACKGROUND_TOP);
        setPreferredSize(new Dimension(900, 540));
        setMinimumSize(new Dimension(560, 350));
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
                pitch = clamp(dragStartPitch - (event.getY() - dragStartY) * 0.0032, 0.28, 1.14);
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
                if (event.getClickCount() == 2) resetView();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                zoomAt(event.getX(), event.getY(), event.getPreciseWheelRotation());
                hover = null;
                repaint();
                event.consume();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public void setDataset(Dataset value) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setDataset(value));
            return;
        }
        dataset = value == null ? Dataset.empty() : value.copy();
        hover = null;
        repaint();
    }

    public void resetView() {
        yaw = 0.34;
        pitch = 0.70;
        zoom = 1.0;
        panX = 0;
        panY = 0;
        hover = null;
        repaint();
    }

    private void zoomAt(double mouseX, double mouseY, double wheelRotation) {
        double oldZoom = zoom;
        double newZoom = clamp(oldZoom * Math.pow(1.08, -wheelRotation), 0.68, 1.85);
        if (Math.abs(newZoom - oldZoom) < 1e-9) return;

        // Projection is affine around this base origin. Correcting the pan after
        // scaling keeps the point currently under the pointer visually fixed.
        double baseX = getWidth() * 0.43;
        double baseY = getHeight() * 0.76;
        double ratio = newZoom / oldZoom;
        panX = mouseX - baseX - ratio * (mouseX - baseX - panX);
        panY = mouseY - baseY - ratio * (mouseY - baseY - panY);
        panX = clamp(panX, -getWidth() * 1.5, getWidth() * 1.5);
        panY = clamp(panY, -getHeight() * 1.5, getHeight() * 1.5);
        zoom = newZoom;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setPaint(new java.awt.GradientPaint(0, 0, BACKGROUND_TOP, 0, getHeight(), BACKGROUND_BOTTOM));
            g.fillRect(0, 0, getWidth(), getHeight());
            if (dataset.isEmpty()) paintEmpty(g); else paintChart(g);
        } finally {
            g.dispose();
        }
    }

    private void paintEmpty(Graphics2D g) {
        String text = UiTranslations.t("Nessun dato disponibile per questa distribuzione 3D.");
        g.setColor(MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, getHeight() / 2);
    }

    private void paintChart(Graphics2D g) {
        bars.clear();
        int maxCount = Math.max(1, dataset.maximumCount());
        Geometry geometry = new Geometry(getWidth(), getHeight(), yaw, pitch, zoom, panX, panY);
        paintGrid(g, geometry, maxCount);

        int groups = dataset.groups().length;
        int categories = dataset.categories().length;
        double categoryWidth = 1.0 / Math.max(1, categories);
        double barWidth = categoryWidth * 0.62;
        double depthThickness = groups <= 1 ? 0.12 : Math.min(0.16, 0.42 / groups);

        for (int group = groups - 1; group >= 0; group--) {
            double zCenter = groups == 1 ? 0.18 : group / (double) (groups - 1);
            double z0 = clamp(zCenter - depthThickness / 2.0, 0, 1);
            double z1 = clamp(zCenter + depthThickness / 2.0, 0, 1);
            for (int category = 0; category < categories; category++) {
                int count = dataset.counts()[group][category];
                double xCenter = (category + 0.5) * categoryWidth;
                double x0 = xCenter - barWidth / 2.0;
                double x1 = xCenter + barWidth / 2.0;
                double height = count / (double) maxCount;
                paintBar(g, geometry, group, category, count, x0, x1, z0, z1, height,
                        displayColor(group));
            }
        }

        paintAxes(g, geometry, maxCount);
        paintHint(g);
        if (hover != null) paintTooltip(g, hover);
    }

    private void paintGrid(Graphics2D g, Geometry geometry, int maxCount) {
        g.setStroke(new BasicStroke(1f));
        for (int tick = 0; tick <= 5; tick++) {
            double y = tick / 5.0;
            Point2D frontLeft = geometry.project(0, y, 0);
            Point2D frontRight = geometry.project(1, y, 0);
            Point2D backLeft = geometry.project(0, y, 1);
            Point2D backRight = geometry.project(1, y, 1);
            g.setColor(GRID);
            g.draw(new Line2D.Double(frontLeft, frontRight));
            g.draw(new Line2D.Double(backLeft, backRight));
            g.draw(new Line2D.Double(frontLeft, backLeft));

            String label = Integer.toString((int) Math.round(maxCount * y));
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(MUTED);
            g.drawString(label, (float) frontLeft.getX() - g.getFontMetrics().stringWidth(label) - 9,
                    (float) frontLeft.getY() + 4);
        }
        int categories = dataset.categories().length;
        for (int index = 0; index <= categories; index++) {
            double x = index / (double) categories;
            Point2D front = geometry.project(x, 0, 0);
            Point2D back = geometry.project(x, 0, 1);
            g.setColor(GRID);
            g.draw(new Line2D.Double(front, back));
        }
    }

    private void paintBar(Graphics2D g, Geometry geometry, int group, int category, int count,
                          double x0, double x1, double z0, double z1, double height, Color color) {
        Point2D frontBottomLeft = geometry.project(x0, 0, z0);
        Point2D frontBottomRight = geometry.project(x1, 0, z0);
        Point2D frontTopRight = geometry.project(x1, height, z0);
        Point2D frontTopLeft = geometry.project(x0, height, z0);
        Point2D backBottomRight = geometry.project(x1, 0, z1);
        Point2D backTopRight = geometry.project(x1, height, z1);
        Point2D backTopLeft = geometry.project(x0, height, z1);

        Polygon front = polygon(frontBottomLeft, frontBottomRight, frontTopRight, frontTopLeft);
        Polygon side = polygon(frontBottomRight, backBottomRight, backTopRight, frontTopRight);
        Polygon top = polygon(frontTopLeft, frontTopRight, backTopRight, backTopLeft);

        if (dataset.spectralGradient() && count != 0) {
            float bottomY = (float) Math.max(frontBottomLeft.getY(), frontBottomRight.getY());
            float topY = (float) Math.min(frontTopLeft.getY(), frontTopRight.getY());
            if (Math.abs(bottomY - topY) < 1f) topY = bottomY - 1f;
            g.setPaint(new LinearGradientPaint(0f, bottomY, 0f, topY,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{SPECTRAL_CYAN, SPECTRAL_BLUE, SPECTRAL_PURPLE}));
            g.fill(front);
            g.setColor(darken(SPECTRAL_BLUE, 0.72));
            g.fill(side);
            g.setColor(lighten(SPECTRAL_PURPLE, 0.18));
            g.fill(top);
            g.setColor(withAlpha(SPECTRAL_CYAN, 245));
        } else if (isPopulationHistogram() && count != 0) {
            // Same neon language as the 2D histogram, while the base hue keeps
            // each depth band immediately recognisable.
            float bottomY = (float) Math.max(frontBottomLeft.getY(), frontBottomRight.getY());
            float topY = (float) Math.min(frontTopLeft.getY(), frontTopRight.getY());
            if (Math.abs(bottomY - topY) < 1f) topY = bottomY - 1f;
            g.setPaint(new LinearGradientPaint(0f, bottomY, 0f, topY,
                    new float[]{0f, 0.58f, 1f},
                    new Color[]{darken(color, 0.76), color, lighten(color, 0.30)}));
            g.fill(front);
            g.setColor(withAlpha(darken(color, 0.62), 215));
            g.fill(side);
            g.setColor(withAlpha(lighten(color, 0.34), 235));
            g.fill(top);
            g.setColor(withAlpha(color, 250));
        } else {
            g.setColor(withAlpha(color, count == 0 ? 35 : 205));
            g.fill(front);
            g.setColor(withAlpha(darken(color, 0.72), count == 0 ? 25 : 190));
            g.fill(side);
            g.setColor(withAlpha(lighten(color, 0.24), count == 0 ? 45 : 220));
            g.fill(top);
            g.setColor(withAlpha(color, count == 0 ? 70 : 245));
        }

        g.setStroke(new BasicStroke(1.1f));
        g.draw(front);
        g.draw(side);
        g.draw(top);
        bars.add(new BarHit(front, group, category, count, frontTopLeft));
    }

    private void paintAxes(Graphics2D g, Geometry geometry, int maxCount) {
        Point2D origin = geometry.project(0, 0, 0);
        Point2D xEnd = geometry.project(1, 0, 0);
        Point2D yEnd = geometry.project(0, 1, 0);
        Point2D zEnd = geometry.project(0, 0, 1);
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(new Color(90, 216, 255, 210));
        g.draw(new Line2D.Double(origin, xEnd));
        g.setColor(TEXT);
        g.draw(new Line2D.Double(origin, yEnd));
        g.setColor(new Color(181, 146, 245));
        g.draw(new Line2D.Double(origin, zEnd));

        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(MUTED);
        double width = 1.0 / dataset.categories().length;
        for (int category = 0; category < dataset.categories().length; category++) {
            Point2D point = geometry.project((category + 0.5) * width, 0, 0);
            String label = UiTranslations.t(dataset.categories()[category]);
            int textWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(label, (float) point.getX() - textWidth / 2f, (float) point.getY() + 20);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(TEXT);
        String xLabel = UiTranslations.t(dataset.xAxisLabel());
        g.drawString(xLabel,
                (float) ((origin.getX() + xEnd.getX()) / 2.0 - g.getFontMetrics().stringWidth(xLabel) / 2.0),
                (float) Math.max(origin.getY(), xEnd.getY()) + 43);
        String yLabel = UiTranslations.t(dataset.valueLabel());
        Graphics2D verticalAxis = (Graphics2D) g.create();
        verticalAxis.rotate(-Math.PI / 2);
        double yCenter = (origin.getY() + yEnd.getY()) / 2.0;
        double labelX = Math.max(24, yEnd.getX() - 64);
        verticalAxis.drawString(yLabel,
                (float) (-yCenter - verticalAxis.getFontMetrics().stringWidth(yLabel) / 2.0),
                (float) labelX);
        verticalAxis.dispose();
        g.setColor(new Color(198, 174, 250));
        g.drawString(UiTranslations.t(dataset.depthAxisLabel()), (float) zEnd.getX() - 20, (float) zEnd.getY() - 10);

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        for (int group = 0; group < dataset.groups().length; group++) {
            double z = dataset.groups().length == 1 ? 0.18 : group / (double) (dataset.groups().length - 1);
            Point2D point = geometry.project(1, 0, z);
            g.setColor(legendColor(group));
            g.fillOval((int) point.getX() + 8, (int) point.getY() - 4, 8, 8);
            g.drawString(UiTranslations.t(dataset.groups()[group]), (float) point.getX() + 21, (float) point.getY() + 4);
        }
    }

    private boolean isPopulationHistogram() {
        if (dataset.spectralGradient()) return false;
        String value = dataset.valueLabel() == null ? "" : dataset.valueLabel().toLowerCase(java.util.Locale.ROOT);
        return value.contains("numero di grb") || value.contains("number of grbs");
    }

    private Color displayColor(int group) {
        if (!isPopulationHistogram()) return dataset.colors()[group];
        int groups = Math.max(1, dataset.groups().length);
        if (groups == 1) return POPULATION_CYAN;
        if (groups == 2) return group == 0 ? POPULATION_CYAN : POPULATION_PURPLE;
        if (group <= 0) return POPULATION_CYAN;
        if (group >= groups - 1) return POPULATION_PURPLE;
        if (groups == 3) return POPULATION_BLUE;

        double fraction = group / (double) (groups - 1);
        return fraction <= 0.5
                ? interpolate(POPULATION_CYAN, POPULATION_BLUE, fraction * 2.0)
                : interpolate(POPULATION_BLUE, POPULATION_PURPLE, (fraction - 0.5) * 2.0);
    }

    private Color legendColor(int group) {
        if (dataset.spectralGradient()) return SPECTRAL_BLUE;
        return displayColor(group);
    }

    private void paintHint(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(MUTED.getRed(), MUTED.getGreen(), MUTED.getBlue(), 185));
        g.drawString(UiTranslations.t("Trascina: ruota prospettiva   ·   Rotella: zoom   ·   Doppio clic: centra"),
                18, getHeight() - 17);
    }

    private void updateHover(Point point) {
        BarHit selected = null;
        for (int index = bars.size() - 1; index >= 0; index--) {
            if (bars.get(index).shape().contains(point)) {
                selected = bars.get(index);
                break;
            }
        }
        if (hover != selected) {
            hover = selected;
            repaint();
        }
    }

    private void paintTooltip(Graphics2D g, BarHit selected) {
        String[] lines = {
                UiTranslations.t(dataset.groups()[selected.group()]),
                UiTranslations.t(dataset.xAxisLabel()) + ": " + UiTranslations.t(dataset.categories()[selected.category()]),
                UiTranslations.t(dataset.valueLabel()) + ": " + selected.count()
        };
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics metrics = g.getFontMetrics();
        int width = Arrays.stream(lines).mapToInt(metrics::stringWidth).max().orElse(120) + 24;
        int height = 66;
        int x = (int) selected.anchor().getX() + 16;
        int y = (int) selected.anchor().getY() - height - 12;
        if (x + width > getWidth() - 10) x = (int) selected.anchor().getX() - width - 16;
        if (y < 10) y = (int) selected.anchor().getY() + 16;

        g.setColor(TOOLTIP_BG);
        g.fillRoundRect(x, y, width, height, 14, 14);
        g.setColor(legendColor(selected.group()));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, y, width, height, 14, 14);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(lines[0], x + 12, y + 19);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString(lines[1], x + 12, y + 38);
        g.drawString(lines[2], x + 12, y + 56);
    }

    private static Polygon polygon(Point2D... points) {
        Polygon polygon = new Polygon();
        for (Point2D point : points) polygon.addPoint((int) Math.round(point.getX()), (int) Math.round(point.getY()));
        return polygon;
    }

    private static Color darken(Color color, double factor) {
        return new Color((int) (color.getRed() * factor), (int) (color.getGreen() * factor),
                (int) (color.getBlue() * factor));
    }

    private static Color lighten(Color color, double fraction) {
        return new Color(
                (int) (color.getRed() + (255 - color.getRed()) * fraction),
                (int) (color.getGreen() + (255 - color.getGreen()) * fraction),
                (int) (color.getBlue() + (255 - color.getBlue()) * fraction));
    }

    private static Color interpolate(Color start, Color end, double fraction) {
        double f = clamp(fraction, 0, 1);
        return new Color(
                (int) Math.round(start.getRed() + (end.getRed() - start.getRed()) * f),
                (int) Math.round(start.getGreen() + (end.getGreen() - start.getGreen()) * f),
                (int) Math.round(start.getBlue() + (end.getBlue() - start.getBlue()) * f));
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Dataset(String[] categories, String[] groups, int[][] counts, Color[] colors,
                          String xAxisLabel, String depthAxisLabel, String valueLabel,
                          boolean spectralGradient) {
        public Dataset {
            categories = categories == null ? new String[0] : categories;
            groups = groups == null ? new String[0] : groups;
            counts = counts == null ? new int[0][] : counts;
            colors = colors == null ? new Color[0] : colors;
            xAxisLabel = xAxisLabel == null ? "Intervallo" : xAxisLabel;
            depthAxisLabel = depthAxisLabel == null ? "Gruppo" : depthAxisLabel;
            valueLabel = valueLabel == null || valueLabel.isBlank() ? "Numero di GRB" : valueLabel;
        }

        public Dataset(String[] categories, String[] groups, int[][] counts, Color[] colors,
                       String xAxisLabel, String depthAxisLabel, String valueLabel) {
            this(categories, groups, counts, colors, xAxisLabel, depthAxisLabel, valueLabel,
                    isSpectralFlux(valueLabel));
        }

        public Dataset(String[] categories, String[] groups, int[][] counts, Color[] colors,
                       String xAxisLabel, String depthAxisLabel) {
            this(categories, groups, counts, colors, xAxisLabel, depthAxisLabel, "Numero di GRB", false);
        }

        public static Dataset empty() {
            return new Dataset(new String[0], new String[0], new int[0][], new Color[0],
                    "Intervallo", "Gruppo", "Numero di GRB", false);
        }

        public boolean isEmpty() {
            if (categories.length == 0 || groups.length == 0 || counts.length != groups.length
                    || colors.length != groups.length) return true;
            for (int[] row : counts) if (row == null || row.length != categories.length) return true;
            return false;
        }

        public int maximumCount() {
            int maximum = 0;
            for (int[] row : counts) for (int value : row) maximum = Math.max(maximum, value);
            return maximum;
        }

        public Dataset copy() {
            int[][] copied = new int[counts.length][];
            for (int index = 0; index < counts.length; index++) copied[index] = Arrays.copyOf(counts[index], counts[index].length);
            return new Dataset(Arrays.copyOf(categories, categories.length), Arrays.copyOf(groups, groups.length),
                    copied, Arrays.copyOf(colors, colors.length), xAxisLabel, depthAxisLabel, valueLabel, spectralGradient);
        }

        private static boolean isSpectralFlux(String valueLabel) {
            if (valueLabel == null) return false;
            String lower = valueLabel.toLowerCase(java.util.Locale.ROOT);
            return lower.contains("flusso energetico") || lower.contains("energy flux");
        }
    }

    private record BarHit(Polygon shape, int group, int category, int count, Point2D anchor) { }

    private static final class Geometry {
        private final double plotWidth;
        private final double plotHeight;
        private final double centerX;
        private final double baselineY;
        private final double depthX;
        private final double depthY;

        private Geometry(int width, int height, double yaw, double pitch, double zoom,
                         double panX, double panY) {
            plotWidth = Math.max(360, width * 0.66) * zoom;
            plotHeight = Math.max(200, height * 0.57) * zoom;
            centerX = width * 0.43 + panX;
            baselineY = height * 0.76 + panY;
            depthX = (46 + 92 * yaw) * zoom;
            depthY = (24 + 58 * pitch) * zoom;
        }

        private Point2D project(double x, double y, double z) {
            return new Point2D.Double(centerX + (x - 0.5) * plotWidth + z * depthX,
                    baselineY - y * plotHeight - z * depthY);
        }
    }
}
