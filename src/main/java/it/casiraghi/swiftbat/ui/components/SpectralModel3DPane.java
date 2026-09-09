package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.ui.UiFactory;
import it.casiraghi.swiftbat.ui.I18n;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

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
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.DoubleConsumer;

/** Vista prospettica dedicata al modello spettrale ricostruito. */
public final class SpectralModel3DPane extends BorderPane {
    private final SwingNode swingNode = new SwingNode();
    private final SpectralRenderer renderer;
    private final Label zoomLabel = UiFactory.label("Zoom 100%", "three-d-zoom-inline");

    public SpectralModel3DPane(String modelCode, double[] energies, double[] logFluxes) {
        renderer = new SpectralRenderer(energies, logFluxes);

        getStyleClass().addAll("three-d-panel", "three-d-panel-fullscreen");
        setMinSize(0, 0);
        setPrefHeight(760);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        renderer.setZoomListener(value -> Platform.runLater(
                () -> zoomLabel.setText("Zoom " + Math.round(value * 100.0) + "%")));
        SwingUtilities.invokeLater(() -> swingNode.setContent(renderer));

        StackPane viewer = new StackPane(swingNode);
        viewer.getStyleClass().add("three-d-viewer");
        viewer.setMinSize(0, 0);
        viewer.setPrefHeight(680);
        viewer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        viewer.widthProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));
        viewer.heightProperty().addListener((obs, oldValue, newValue) -> syncRendererSize(viewer));

        setCenter(viewer);
        setBottom(buildFooter());
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> SwingUtilities.invokeLater(renderer::repaint));
        Platform.runLater(() -> syncRendererSize(viewer));
    }

    private HBox buildFooter() {
        Label interaction = UiFactory.wrappedLabel(
                "Trascina: prospettiva · Rotella: zoom · Doppio clic: centra",
                "subtle-text");
        interaction.setMinHeight(Region.USE_PREF_SIZE);
        interaction.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(interaction, Priority.ALWAYS);

        Button reset = UiFactory.button("Centra vista", "secondary-button");
        reset.setOnAction(event -> SwingUtilities.invokeLater(renderer::resetView));

        HBox footer = new HBox(12, zoomLabel, interaction, reset);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("three-d-footer");
        return footer;
    }

    private void syncRendererSize(StackPane viewer) {
        int width = (int) Math.max(560, viewer.getWidth());
        int height = (int) Math.max(380, viewer.getHeight());
        SwingUtilities.invokeLater(() -> {
            Dimension size = new Dimension(width, height);
            renderer.setPreferredSize(size);
            renderer.setSize(size);
            renderer.revalidate();
            renderer.repaint();
        });
    }

    private static final class SpectralRenderer extends JPanel {
        private static final Color BG_TOP = new Color(6, 13, 29);
        private static final Color BG_BOTTOM = new Color(9, 23, 48);
        private static final Color GRID = new Color(76, 104, 154, 80);
        private static final Color GRID_STRONG = new Color(113, 148, 210, 125);
        private static final Color TEXT = new Color(226, 236, 255);
        private static final Color MUTED = new Color(141, 164, 207);
        private static final Color CURVE = new Color(255, 173, 82);
        private static final DecimalFormat AXIS_FORMAT;

        static {
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
            AXIS_FORMAT = new DecimalFormat("0.##", symbols);
        }

        private final double[] energies;
        private final double[] values;
        private double zoom = 1.0;
        private double perspectiveX = 54;
        private double perspectiveY = -34;
        private int dragX;
        private int dragY;
        private double dragPerspectiveX;
        private double dragPerspectiveY;
        private DoubleConsumer zoomListener = value -> { };

        private SpectralRenderer(double[] energies, double[] values) {
            this.energies = energies == null ? new double[0] : energies.clone();
            this.values = values == null ? new double[0] : values.clone();
            setOpaque(true);
            setBackground(BG_TOP);
            setPreferredSize(new Dimension(980, 620));
            setMinimumSize(new Dimension(520, 360));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    dragX = event.getX();
                    dragY = event.getY();
                    dragPerspectiveX = perspectiveX;
                    dragPerspectiveY = perspectiveY;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    double dx = event.getX() - dragX;
                    double dy = event.getY() - dragY;
                    perspectiveX = clamp(dragPerspectiveX + dx * 0.28, -105, 120);
                    perspectiveY = clamp(dragPerspectiveY + dy * 0.24, -95, 70);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        resetView();
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    zoom = clamp(zoom * Math.pow(1.08, -event.getPreciseWheelRotation()), 0.65, 1.75);
                    zoomListener.accept(zoom);
                    repaint();
                    event.consume();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
        }

        private void setZoomListener(DoubleConsumer listener) {
            zoomListener = listener == null ? value -> { } : listener;
            zoomListener.accept(zoom);
        }

        private void resetView() {
            zoom = 1.0;
            perspectiveX = 54;
            perspectiveY = -34;
            zoomListener.accept(zoom);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM));
                g.fillRect(0, 0, getWidth(), getHeight());
                if (energies.length < 2 || values.length != energies.length) {
                    paintEmpty(g);
                    return;
                }
                paintChart(g);
            } finally {
                g.dispose();
            }
        }

        private void paintEmpty(Graphics2D g) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(MUTED);
            String text = I18n.t("Modello spettrale non disponibile.");
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, getHeight() / 2);
        }

        private void paintChart(Graphics2D g) {
            double minX = energies[0];
            double maxX = energies[energies.length - 1];
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            for (double value : values) {
                if (Double.isFinite(value)) {
                    minY = Math.min(minY, value);
                    maxY = Math.max(maxY, value);
                }
            }
            if (!Double.isFinite(minY) || !Double.isFinite(maxY)) {
                paintEmpty(g);
                return;
            }
            double yRange = Math.max(0.25, maxY - minY);
            minY -= yRange * 0.12;
            maxY += yRange * 0.12;

            double baseWidth = Math.max(440, getWidth() * 0.72) * zoom;
            double baseHeight = Math.max(260, getHeight() * 0.64) * zoom;
            double left = getWidth() * 0.47 - baseWidth / 2.0;
            double top = getHeight() * 0.48 - baseHeight / 2.0;
            double right = left + baseWidth;
            double bottom = top + baseHeight;

            drawPlane(g, left + perspectiveX, top + perspectiveY,
                    right + perspectiveX, bottom + perspectiveY, minX, maxX, minY, maxY, true);

            g.setColor(new Color(91, 122, 177, 95));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new Line2D.Double(left, top, left + perspectiveX, top + perspectiveY));
            g.draw(new Line2D.Double(right, top, right + perspectiveX, top + perspectiveY));
            g.draw(new Line2D.Double(left, bottom, left + perspectiveX, bottom + perspectiveY));
            g.draw(new Line2D.Double(right, bottom, right + perspectiveX, bottom + perspectiveY));

            drawPlane(g, left, top, right, bottom, minX, maxX, minY, maxY, false);
            drawCurve(g, left + perspectiveX, top + perspectiveY, right + perspectiveX,
                    bottom + perspectiveY, minX, maxX, minY, maxY, 70, 2.0f);
            drawCurve(g, left, top, right, bottom, minX, maxX, minY, maxY, 255, 3.2f);

            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(TEXT);
            String xLabel = I18n.t("Energia (keV)");
            g.drawString(xLabel,
                    (float) ((left + right) / 2 - g.getFontMetrics().stringWidth(xLabel) / 2.0),
                    (float) bottom + 54);

            Graphics2D gy = (Graphics2D) g.create();
            gy.rotate(-Math.PI / 2);
            String yLabel = "log₁₀ N(E) [ph cm⁻² s⁻¹ keV⁻¹]";
            gy.drawString(yLabel,
                    (float) (-(top + bottom) / 2 - gy.getFontMetrics().stringWidth(yLabel) / 2.0),
                    (float) left - 72);
            gy.dispose();
        }

        private void drawPlane(Graphics2D g, double left, double top, double right, double bottom,
                               double minX, double maxX, double minY, double maxY, boolean back) {
            g.setStroke(new BasicStroke(back ? 0.9f : 1.15f));
            g.setColor(back ? new Color(80, 106, 153, 55) : GRID);
            g.drawRect((int) Math.round(left), (int) Math.round(top),
                    (int) Math.round(right - left), (int) Math.round(bottom - top));

            int xTicks = 8;
            int yTicks = 5;
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int tick = 0; tick <= xTicks; tick++) {
                double f = tick / (double) xTicks;
                double x = left + (right - left) * f;
                g.setColor(back ? new Color(80, 106, 153, 42) : GRID);
                g.draw(new Line2D.Double(x, top, x, bottom));
                if (!back) {
                    String label = AXIS_FORMAT.format(minX + (maxX - minX) * f);
                    int w = g.getFontMetrics().stringWidth(label);
                    g.setColor(MUTED);
                    g.drawString(label, (float) x - w / 2f, (float) bottom + 20);
                }
            }
            for (int tick = 0; tick <= yTicks; tick++) {
                double f = tick / (double) yTicks;
                double y = bottom - (bottom - top) * f;
                g.setColor(back ? new Color(80, 106, 153, 42) : GRID);
                g.draw(new Line2D.Double(left, y, right, y));
                if (!back) {
                    double value = minY + (maxY - minY) * f;
                    String label = AXIS_FORMAT.format(value);
                    int w = g.getFontMetrics().stringWidth(label);
                    g.setColor(MUTED);
                    g.drawString(label, (float) left - w - 12, (float) y + 4);
                }
            }

            if (!back) {
                g.setColor(GRID_STRONG);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Line2D.Double(left, bottom, right, bottom));
                g.draw(new Line2D.Double(left, top, left, bottom));
            }
        }

        private void drawCurve(Graphics2D g, double left, double top, double right, double bottom,
                               double minX, double maxX, double minY, double maxY,
                               int alpha, float width) {
            Path2D curve = new Path2D.Double();
            boolean started = false;
            for (int index = 0; index < energies.length; index++) {
                if (!Double.isFinite(energies[index]) || !Double.isFinite(values[index])) {
                    started = false;
                    continue;
                }
                double x = left + (energies[index] - minX) / (maxX - minX) * (right - left);
                double y = bottom - (values[index] - minY) / (maxY - minY) * (bottom - top);
                if (!started) {
                    curve.moveTo(x, y);
                    started = true;
                } else {
                    curve.lineTo(x, y);
                }
            }
            g.setColor(new Color(CURVE.getRed(), CURVE.getGreen(), CURVE.getBlue(),
                    Math.max(0, Math.min(255, alpha))));
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(curve);
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
