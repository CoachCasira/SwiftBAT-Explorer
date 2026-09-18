package it.casiraghi.swiftbat.ui.components;

import it.casiraghi.swiftbat.model.MollweidePoint;
import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.service.SkyCoordinates;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mappa celeste 2D in proiezione Mollweide, con RA crescente verso sinistra.
 *
 * <p>La vista supporta selezione dei GRB, zoom centrato sul puntatore,
 * trascinamento/pan e ripristino con doppio clic.</p>
 */
public final class MollweideSkyPane extends Pane {
    private static final double SQRT_2 = Math.sqrt(2.0);
    private static final Color BACKGROUND = Color.web("#07101f");
    private static final Color GRID = Color.web("#263b5f", 0.75);
    private static final Color BORDER = Color.web("#5579a8", 0.90);
    private static final Color LONG_COLOR = Color.web("#54d7ff", 0.92);
    private static final Color SHORT_COLOR = Color.web("#ffae4a", 0.96);
    private static final Color UNKNOWN_COLOR = Color.web("#9aa8bf", 0.72);
    private static final Color GALACTIC_COLOR = Color.web("#bd82ff", 0.92);
    private static final Color SELECTED_COLOR = Color.WHITE;

    private final Canvas canvas = new Canvas();
    private final List<RenderedPoint> renderedPoints = new ArrayList<>();
    private List<SkyBurst> bursts = List.of();
    private boolean showGalacticPlane = true;
    private Consumer<SkyBurst> onSelect = burst -> { };
    private SkyBurst selected;
    private SkyBurst hovered;

    private double zoom = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double dragStartX;
    private double dragStartY;
    private double dragStartPanX;
    private double dragStartPanY;
    private boolean dragging;

    public MollweideSkyPane() {
        getChildren().add(canvas);
        setMinHeight(500);
        setPrefHeight(650);
        getStyleClass().add("sky-map-surface");
        setCursor(Cursor.HAND);

        widthProperty().addListener((obs, oldValue, newValue) -> resizeCanvas());
        heightProperty().addListener((obs, oldValue, newValue) -> resizeCanvas());

        canvas.setOnMouseMoved(this::handleMove);
        canvas.setOnMouseExited(event -> {
            hovered = null;
            if (!dragging) {
                setCursor(Cursor.HAND);
            }
            redraw();
        });
        canvas.setOnMousePressed(this::handlePressed);
        canvas.setOnMouseDragged(this::handleDragged);
        canvas.setOnMouseReleased(this::handleReleased);
        canvas.setOnMouseClicked(this::handleClick);
        canvas.setOnScroll(this::handleScroll);
    }

    public void setBursts(List<SkyBurst> bursts) {
        this.bursts = bursts == null ? List.of() : List.copyOf(bursts);
        if (selected != null && this.bursts.stream().noneMatch(b -> b.grbName().equals(selected.grbName()))) {
            selected = null;
        }
        redraw();
    }

    public void setShowGalacticPlane(boolean show) {
        this.showGalacticPlane = show;
        redraw();
    }

    public void setOnSelect(Consumer<SkyBurst> onSelect) {
        this.onSelect = onSelect == null ? burst -> { } : onSelect;
    }

    public void select(SkyBurst burst) {
        selected = burst;
        redraw();
    }

    public void resetView() {
        zoom = 1.0;
        panX = 0.0;
        panY = 0.0;
        redraw();
    }

    private void resizeCanvas() {
        double width = Math.max(100, getWidth());
        double height = Math.max(100, getHeight());
        canvas.setWidth(width);
        canvas.setHeight(height);
        redraw();
    }

    private void redraw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 1 || height <= 1) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, width, height);
        renderedPoints.clear();

        Projection projection = projection(width, height);
        drawGrid(gc, projection);
        if (showGalacticPlane) {
            drawGalacticPlane(gc, projection);
        }
        drawBursts(gc, projection);
        drawLabels(gc, projection);
    }

    private Projection projection(double width, double height) {
        double marginX = 52.0;
        double marginY = 50.0;
        double usableW = Math.max(80, width - 2 * marginX);
        double usableH = Math.max(80, height - 2 * marginY);
        double baseScale = Math.min(usableW / (4.0 * SQRT_2), usableH / (2.0 * SQRT_2));
        return new Projection(width / 2.0 + panX, height / 2.0 + panY, baseScale * zoom, baseScale);
    }

    private void drawGrid(GraphicsContext gc, Projection projection) {
        double ellipseW = 4.0 * SQRT_2 * projection.scale();
        double ellipseH = 2.0 * SQRT_2 * projection.scale();
        gc.setLineWidth(1.0);
        gc.setStroke(GRID);

        for (int dec = -60; dec <= 60; dec += 30) {
            final int parallel = dec;
            drawProjectedCurve(gc, projection, 0, 180, 2, ra -> new double[]{ra, parallel});
            drawProjectedCurve(gc, projection, 180, 360, 2, ra -> new double[]{ra, parallel});
        }
        for (int ra = 30; ra < 360; ra += 30) {
            if (ra == 180) {
                continue;
            }
            final int meridian = ra;
            drawProjectedCurve(gc, projection, -89, 89, 2, dec -> new double[]{meridian, dec});
        }

        gc.setStroke(BORDER);
        gc.setLineWidth(1.7);
        gc.strokeOval(projection.cx() - ellipseW / 2.0, projection.cy() - ellipseH / 2.0, ellipseW, ellipseH);
        gc.setStroke(Color.web("#6a8db8", 0.75));
        gc.setLineWidth(1.15);
        ScreenPoint equator = screen(SkyCoordinates.mollweide(0, 0), projection);
        gc.strokeLine(projection.cx() - ellipseW / 2.0, equator.y(), projection.cx() + ellipseW / 2.0, equator.y());
    }

    private void drawGalacticPlane(GraphicsContext gc, Projection projection) {
        gc.setStroke(GALACTIC_COLOR);
        gc.setLineWidth(2.1);
        boolean started = false;
        double previousX = 0.0;
        for (int l = 0; l <= 360; l += 2) {
            double[] eq = SkyCoordinates.galacticPlaneRaDec(l % 360);
            ScreenPoint p = screen(SkyCoordinates.mollweide(eq[0], eq[1]), projection);
            if (!started || Math.abs(p.x() - previousX) > 0.9 * 2.0 * SQRT_2 * projection.scale()) {
                if (started) {
                    gc.stroke();
                }
                gc.beginPath();
                gc.moveTo(p.x(), p.y());
                started = true;
            } else {
                gc.lineTo(p.x(), p.y());
            }
            previousX = p.x();
        }
        if (started) {
            gc.stroke();
        }
    }

    private void drawBursts(GraphicsContext gc, Projection projection) {
        double markerScale = Math.max(0.9, Math.min(1.45, Math.sqrt(zoom)));
        for (SkyBurst burst : bursts) {
            ScreenPoint point = screen(SkyCoordinates.mollweide(burst.raDeg(), burst.decDeg()), projection);
            double radius = (burst.isShort() ? 3.4 : 2.65) * markerScale;
            Color color = burst.isShort() ? SHORT_COLOR : burst.isLong() ? LONG_COLOR : UNKNOWN_COLOR;
            if (hovered != null && hovered.grbName().equals(burst.grbName())) {
                radius += 2.0;
            }
            gc.setFill(color);
            gc.fillOval(point.x() - radius, point.y() - radius, 2 * radius, 2 * radius);
            if (selected != null && selected.grbName().equals(burst.grbName())) {
                gc.setStroke(SELECTED_COLOR);
                gc.setLineWidth(1.8);
                gc.strokeOval(point.x() - radius - 3.0, point.y() - radius - 3.0,
                        2 * radius + 6.0, 2 * radius + 6.0);
            }
            renderedPoints.add(new RenderedPoint(burst, point.x(), point.y()));
        }
    }

    private void drawLabels(GraphicsContext gc, Projection projection) {
        gc.setFill(Color.web("#8196b8"));
        gc.setFont(javafx.scene.text.Font.font(11));
        gc.fillText("RA 0h", projection.cx() - 17, projection.cy() + SQRT_2 * projection.scale() + 25);
        gc.fillText("+90° DEC", projection.cx() + 8, projection.cy() - SQRT_2 * projection.scale() + 6);
        gc.fillText("−90° DEC", projection.cx() + 8, projection.cy() + SQRT_2 * projection.scale() - 4);

        gc.setFill(Color.web("#7187aa"));
        gc.fillText("Zoom " + Math.round(zoom * 100) + "%", 16, canvas.getHeight() - 16);
    }

    private void drawProjectedCurve(GraphicsContext gc, Projection projection,
                                    int start, int end, int step, CoordinateProvider provider) {
        gc.beginPath();
        boolean first = true;
        for (int v = start; v <= end; v += step) {
            double[] coords = provider.coordinate(v);
            ScreenPoint p = screen(SkyCoordinates.mollweide(coords[0], coords[1]), projection);
            if (first) {
                gc.moveTo(p.x(), p.y());
                first = false;
            } else {
                gc.lineTo(p.x(), p.y());
            }
        }
        gc.stroke();
    }

    private ScreenPoint screen(MollweidePoint point, Projection projection) {
        return new ScreenPoint(
                projection.cx() + point.x() * projection.scale(),
                projection.cy() - point.y() * projection.scale());
    }

    private void handleMove(MouseEvent event) {
        if (dragging) {
            return;
        }
        hovered = nearest(event.getX(), event.getY(), 10.0);
        setCursor(hovered == null ? Cursor.HAND : Cursor.CROSSHAIR);
        redraw();
    }

    private void handlePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        dragging = false;
        dragStartX = event.getX();
        dragStartY = event.getY();
        dragStartPanX = panX;
        dragStartPanY = panY;
    }

    private void handleDragged(MouseEvent event) {
        if (!event.isPrimaryButtonDown()) {
            return;
        }
        double dx = event.getX() - dragStartX;
        double dy = event.getY() - dragStartY;
        if (Math.abs(dx) + Math.abs(dy) > 3.0) {
            dragging = true;
            hovered = null;
            panX = dragStartPanX + dx;
            panY = dragStartPanY + dy;
            setCursor(Cursor.MOVE);
            redraw();
            event.consume();
        }
    }

    private void handleReleased(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            setCursor(hovered == null ? Cursor.HAND : Cursor.CROSSHAIR);
        }
    }

    private void handleClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (event.getClickCount() == 2) {
            resetView();
            dragging = false;
            event.consume();
            return;
        }
        if (dragging) {
            dragging = false;
            return;
        }
        SkyBurst hit = nearest(event.getX(), event.getY(), 11.0);
        if (hit != null) {
            selected = hit;
            onSelect.accept(hit);
            redraw();
            // Selecting a GRB is an interaction with the plot, not a request to
            // open the surrounding visualization card fullscreen.
            event.consume();
        }
    }

    private void handleScroll(ScrollEvent event) {
        Projection before = projection(canvas.getWidth(), canvas.getHeight());
        double oldZoom = zoom;
        double factor = Math.pow(1.12, event.getDeltaY() / 40.0);
        double newZoom = clamp(oldZoom * factor, 0.75, 6.0);
        if (Math.abs(newZoom - oldZoom) < 1e-8) {
            event.consume();
            return;
        }

        // Mantiene sotto il puntatore lo stesso punto della proiezione durante lo zoom.
        double normalizedX = (event.getX() - before.cx()) / before.scale();
        double normalizedY = (event.getY() - before.cy()) / before.scale();
        zoom = newZoom;
        double newScale = before.baseScale() * zoom;
        panX = event.getX() - normalizedX * newScale - canvas.getWidth() / 2.0;
        panY = event.getY() - normalizedY * newScale - canvas.getHeight() / 2.0;
        hovered = null;
        redraw();
        event.consume();
    }

    private SkyBurst nearest(double x, double y, double maxDistance) {
        SkyBurst best = null;
        double bestDistance2 = maxDistance * maxDistance;
        for (RenderedPoint point : renderedPoints) {
            double dx = x - point.x();
            double dy = y - point.y();
            double distance2 = dx * dx + dy * dy;
            if (distance2 <= bestDistance2) {
                bestDistance2 = distance2;
                best = point.burst();
            }
        }
        return best;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @FunctionalInterface
    private interface CoordinateProvider {
        double[] coordinate(int value);
    }

    private record Projection(double cx, double cy, double scale, double baseScale) { }
    private record ScreenPoint(double x, double y) { }
    private record RenderedPoint(SkyBurst burst, double x, double y) { }
}
