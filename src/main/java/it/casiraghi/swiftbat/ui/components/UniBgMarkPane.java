package it.casiraghi.swiftbat.ui.components;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/**
 * Compact vector mark used next to the thesis credit.
 *
 * <p>The geometry follows the visual language of the current UniBG mark:
 * an open circular sign containing the Sant'Agostino facade, arches and sun.
 * It is intentionally simplified so it remains readable at 18–24 px in the
 * navigation footer.</p>
 */
public final class UniBgMarkPane extends Pane {
    private static final Color PRIMARY = Color.web("#7EDCFF");
    private static final Color SECONDARY = Color.web("#DCEBFF");

    public UniBgMarkPane(double size) {
        double s = Math.max(16, size);
        setMinSize(s, s);
        setPrefSize(s, s);
        setMaxSize(s, s);
        setMouseTransparent(true);

        double stroke = Math.max(1.0, s * 0.065);

        Arc ring = new Arc(s * 0.50, s * 0.50, s * 0.43, s * 0.43, 38, 286);
        ring.setType(ArcType.OPEN);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(PRIMARY);
        ring.setStrokeWidth(stroke);

        // Stylised Sant'Agostino facade.
        Line roofLeft = line(s * 0.27, s * 0.43, s * 0.50, s * 0.25, stroke, SECONDARY);
        Line roofRight = line(s * 0.50, s * 0.25, s * 0.73, s * 0.43, stroke, SECONDARY);
        Line wallLeft = line(s * 0.27, s * 0.43, s * 0.27, s * 0.72, stroke, SECONDARY);
        Line wallRight = line(s * 0.73, s * 0.43, s * 0.73, s * 0.72, stroke, SECONDARY);
        Line base = line(s * 0.20, s * 0.72, s * 0.80, s * 0.72, stroke, PRIMARY);

        // Three lower arches/portico.
        Arc leftArch = arch(s * 0.34, s * 0.72, s * 0.11, s * 0.14, stroke);
        Arc centerArch = arch(s * 0.50, s * 0.72, s * 0.11, s * 0.14, stroke);
        Arc rightArch = arch(s * 0.66, s * 0.72, s * 0.11, s * 0.14, stroke);

        Circle roseWindow = new Circle(s * 0.50, s * 0.46, s * 0.045);
        roseWindow.setFill(Color.TRANSPARENT);
        roseWindow.setStroke(PRIMARY);
        roseWindow.setStrokeWidth(stroke * 0.85);

        // Small sun, another recognisable element of the UniBG identity.
        Circle sun = new Circle(s * 0.73, s * 0.24, s * 0.035, PRIMARY);
        Line sunV = line(s * 0.73, s * 0.16, s * 0.73, s * 0.32, stroke * 0.65, PRIMARY);
        Line sunH = line(s * 0.65, s * 0.24, s * 0.81, s * 0.24, stroke * 0.65, PRIMARY);

        getChildren().addAll(ring, roofLeft, roofRight, wallLeft, wallRight, base,
                leftArch, centerArch, rightArch, roseWindow, sunV, sunH, sun);
    }

    private static Line line(double x1, double y1, double x2, double y2, double width, Color color) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(color);
        line.setStrokeWidth(width);
        line.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        return line;
    }

    private static Arc arch(double cx, double cy, double rx, double ry, double stroke) {
        Arc arc = new Arc(cx, cy, rx, ry, 0, 180);
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(SECONDARY);
        arc.setStrokeWidth(stroke * 0.8);
        return arc;
    }
}
