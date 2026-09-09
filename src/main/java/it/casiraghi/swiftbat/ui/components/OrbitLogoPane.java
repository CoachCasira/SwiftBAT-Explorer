package it.casiraghi.swiftbat.ui.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;

/**
 * Logo vettoriale/procedurale del redesign: piccolo buco nero con doppia orbita
 * cyan-magenta. Non dipende da asset esterni e resta nitido a qualsiasi scala.
 */
public final class OrbitLogoPane extends Region {
    private final Canvas canvas = new Canvas();

    public OrbitLogoPane() {
        getStyleClass().add("orbit-logo-pane");
        setMouseTransparent(true);
        getChildren().add(canvas);
        setMinSize(34, 34);
        setPrefSize(42, 42);
        setMaxSize(56, 56);
        widthProperty().addListener((obs, oldValue, newValue) -> requestLayout());
        heightProperty().addListener((obs, oldValue, newValue) -> requestLayout());
    }

    @Override
    protected void layoutChildren() {
        double width = Math.max(1, getWidth());
        double height = Math.max(1, getHeight());
        canvas.setWidth(width);
        canvas.setHeight(height);
        draw(width, height);
    }

    private void draw(double width, double height) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        double cx = width * 0.50;
        double cy = height * 0.50;
        double size = Math.min(width, height);
        double core = size * 0.205;

        gc.setFill(new RadialGradient(0, 0, cx, cy, size * 0.48, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(105, 67, 255, 0.18)),
                new Stop(0.45, Color.rgb(225, 63, 224, 0.12)),
                new Stop(0.74, Color.rgb(31, 220, 248, 0.08)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(cx - size * 0.48, cy - size * 0.48, size * 0.96, size * 0.96);

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-18);
        gc.setEffect(new GaussianBlur(Math.max(0.6, size * 0.018)));
        gc.setStroke(Color.rgb(49, 224, 249, 0.92));
        gc.setLineWidth(Math.max(1.1, size * 0.055));
        gc.strokeOval(-size * 0.40, -size * 0.145, size * 0.80, size * 0.29);
        gc.setStroke(Color.rgb(233, 77, 228, 0.88));
        gc.setLineWidth(Math.max(0.9, size * 0.042));
        gc.strokeArc(-size * 0.42, -size * 0.19, size * 0.84, size * 0.38,
                202, 166, ArcType.OPEN);
        gc.setEffect(null);
        gc.restore();

        gc.setFill(Color.rgb(0, 1, 7, 0.995));
        gc.fillOval(cx - core, cy - core, core * 2.0, core * 2.0);
        gc.setStroke(Color.rgb(181, 115, 255, 0.92));
        gc.setLineWidth(Math.max(1.0, size * 0.036));
        gc.strokeOval(cx - core * 1.10, cy - core * 1.10, core * 2.20, core * 2.20);

        gc.setFill(Color.rgb(225, 246, 255, 0.95));
        gc.fillOval(cx + size * 0.30, cy - size * 0.15, Math.max(1.3, size * 0.055), Math.max(1.3, size * 0.055));
    }
}
