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
 * Sfondo procedurale leggero per il workspace.
 * È un elemento puramente grafico: non rappresenta un dato scientifico.
 */
public final class BlackHoleBackdropPane extends Region {
    private final Canvas canvas = new Canvas();

    public BlackHoleBackdropPane() {
        getStyleClass().add("black-hole-backdrop");
        setMouseTransparent(true);
        getChildren().add(canvas);
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
        drawStars(gc, width, height);

        double scale = Math.min(width, height);
        double cx = width * 0.84;
        double cy = height * 0.22;
        double core = Math.max(74, scale * 0.13);

        // Alone freddo molto tenue: resta dietro alle schede senza ridurne la leggibilità.
        gc.setFill(new RadialGradient(0, 0, cx, cy, core * 3.9, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(16, 8, 34, 0.08)),
                new Stop(0.35, Color.rgb(95, 57, 191, 0.12)),
                new Stop(0.66, Color.rgb(24, 162, 214, 0.08)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(cx - core * 3.9, cy - core * 3.9, core * 7.8, core * 7.8);

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-10);
        gc.setEffect(new GaussianBlur(Math.max(1.4, core * 0.045)));
        for (int index = 0; index < 38; index++) {
            double ratio = index / 37.0;
            double ringWidth = core * (3.1 + ratio * 3.0);
            double ringHeight = core * (0.43 + ratio * 0.72);
            double alpha = 0.03 + (1.0 - Math.abs(ratio - 0.42) * 1.7) * 0.13;
            Color color = ratio < 0.48
                    ? Color.rgb(255, 168, 72, Math.max(0.025, alpha))
                    : Color.rgb(128, 80, 246, Math.max(0.02, alpha * 0.74));
            gc.setStroke(color);
            gc.setLineWidth(Math.max(0.8, core * 0.018));
            gc.strokeOval(-ringWidth / 2.0, -ringHeight / 2.0, ringWidth, ringHeight);
        }
        gc.setEffect(null);
        gc.setStroke(Color.rgb(255, 194, 107, 0.28));
        gc.setLineWidth(Math.max(1.2, core * 0.032));
        gc.strokeArc(-core * 1.72, -core * 1.15, core * 3.44, core * 2.30,
                16, 150, ArcType.OPEN);
        gc.setStroke(Color.rgb(91, 214, 255, 0.15));
        gc.strokeArc(-core * 1.86, -core * 1.24, core * 3.72, core * 2.48,
                198, 144, ArcType.OPEN);
        gc.restore();

        gc.setFill(Color.rgb(0, 0, 2, 0.99));
        gc.fillOval(cx - core, cy - core, core * 2.0, core * 2.0);
        gc.setStroke(Color.rgb(255, 174, 75, 0.28));
        gc.setLineWidth(Math.max(1.2, core * 0.026));
        gc.strokeOval(cx - core * 1.06, cy - core * 1.06, core * 2.12, core * 2.12);
    }

    private void drawStars(GraphicsContext gc, double width, double height) {
        for (int index = 0; index < 128; index++) {
            double x = ((index * 127L + 31) % 1009) / 1009.0 * width;
            double y = ((index * 193L + 53) % 1013) / 1013.0 * height;
            double size = 0.45 + ((index * 11) % 6) * 0.14;
            double alpha = 0.08 + ((index * 23) % 8) * 0.018;
            gc.setFill(Color.rgb(195, 221, 255, alpha));
            gc.fillOval(x, y, size, size);
        }
    }
}
