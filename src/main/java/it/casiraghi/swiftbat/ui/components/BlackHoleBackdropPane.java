package it.casiraghi.swiftbat.ui.components;

import javafx.animation.AnimationTimer;
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
 * Fondale procedurale usato esclusivamente dal tema sperimentale Black Hole.
 * Non rappresenta dati scientifici e non intercetta gli eventi del mouse.
 *
 * Il fondale pesante resta statico; soltanto un overlay leggero anima stelle
 * e piccole particelle attorno ai due buchi neri decorativi.
 */
public final class BlackHoleBackdropPane extends Region {
    private final Canvas staticCanvas = new Canvas();
    private final Canvas motionCanvas = new Canvas();
    private long startNanos;
    private long lastFrame;

    private final AnimationTimer animation = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (startNanos == 0L) startNanos = now;
            if (now - lastFrame < 50_000_000L) return; // ~20 fps, sufficiente per il fondale
            lastFrame = now;
            double seconds = (now - startNanos) / 1_000_000_000.0;
            drawMotion(Math.max(1, getWidth()), Math.max(1, getHeight()), seconds);
        }
    };

    public BlackHoleBackdropPane() {
        getStyleClass().add("black-hole-backdrop");
        setMouseTransparent(true);
        getChildren().addAll(staticCanvas, motionCanvas);
        widthProperty().addListener((obs, oldValue, newValue) -> requestLayout());
        heightProperty().addListener((obs, oldValue, newValue) -> requestLayout());
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                animation.stop();
            } else {
                startNanos = 0L;
                lastFrame = 0L;
                animation.start();
            }
        });
    }

    @Override
    protected void layoutChildren() {
        double width = Math.max(1, getWidth());
        double height = Math.max(1, getHeight());
        staticCanvas.setWidth(width);
        staticCanvas.setHeight(height);
        motionCanvas.setWidth(width);
        motionCanvas.setHeight(height);
        drawStatic(width, height);
        drawMotion(width, height, 0.0);
    }

    private void drawStatic(double width, double height) {
        GraphicsContext gc = staticCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        drawNebulae(gc, width, height);
        drawStars(gc, width, height);

        double scale = Math.min(width, Math.max(620, height));
        drawBlackHole(gc, width * 0.78, Math.max(118, height * 0.12),
                Math.max(66, scale * 0.095), -7, 0.88);
        drawBlackHole(gc, width * 0.10, height * 0.83,
                Math.max(42, scale * 0.060), 14, 0.36);
    }

    private void drawNebulae(GraphicsContext gc, double width, double height) {
        gc.setFill(new RadialGradient(0, 0, width * 0.76, height * 0.14,
                Math.max(width, height) * 0.58, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(91, 38, 176, 0.24)),
                new Stop(0.28, Color.rgb(25, 80, 177, 0.13)),
                new Stop(0.60, Color.rgb(0, 205, 244, 0.055)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);

        gc.setFill(new RadialGradient(0, 0, width * 0.09, height * 0.78,
                Math.max(width, height) * 0.38, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(232, 40, 207, 0.10)),
                new Stop(0.42, Color.rgb(47, 76, 210, 0.075)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);
    }

    private void drawBlackHole(GraphicsContext gc, double cx, double cy,
                               double core, double angle, double opacity) {
        gc.save();
        gc.setGlobalAlpha(opacity);

        gc.setFill(new RadialGradient(0, 0, cx, cy, core * 4.2, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(5, 3, 18, 0.04)),
                new Stop(0.28, Color.rgb(214, 51, 222, 0.13)),
                new Stop(0.55, Color.rgb(71, 76, 239, 0.14)),
                new Stop(0.78, Color.rgb(0, 210, 255, 0.075)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(cx - core * 4.2, cy - core * 4.2, core * 8.4, core * 8.4);

        gc.translate(cx, cy);
        gc.rotate(angle);
        gc.setEffect(new GaussianBlur(Math.max(1.2, core * 0.042)));
        for (int index = 0; index < 54; index++) {
            double ratio = index / 53.0;
            double ringWidth = core * (2.7 + ratio * 4.5);
            double ringHeight = core * (0.34 + ratio * 0.92);
            double centerBias = Math.max(0.0, 1.0 - Math.abs(ratio - 0.43) * 1.85);
            double alpha = 0.035 + centerBias * 0.26;
            Color color;
            if (ratio < 0.24) {
                color = Color.rgb(255, 231, 185, alpha);
            } else if (ratio < 0.48) {
                color = Color.rgb(255, 116, 87, alpha);
            } else if (ratio < 0.75) {
                color = Color.rgb(222, 51, 220, alpha * 0.86);
            } else {
                color = Color.rgb(43, 112, 255, alpha * 0.72);
            }
            gc.setStroke(color);
            gc.setLineWidth(Math.max(0.75, core * (0.014 + (1.0 - ratio) * 0.013)));
            gc.strokeOval(-ringWidth / 2.0, -ringHeight / 2.0, ringWidth, ringHeight);
        }
        gc.setEffect(null);

        gc.setStroke(Color.rgb(255, 205, 142, 0.56));
        gc.setLineWidth(Math.max(1.4, core * 0.045));
        gc.strokeArc(-core * 1.66, -core * 1.26, core * 3.32, core * 2.52,
                20, 142, ArcType.OPEN);
        gc.setStroke(Color.rgb(105, 87, 255, 0.40));
        gc.strokeArc(-core * 1.82, -core * 1.38, core * 3.64, core * 2.76,
                199, 143, ArcType.OPEN);
        gc.restore();

        gc.setGlobalAlpha(opacity);
        gc.setFill(Color.rgb(0, 0, 3, 0.995));
        gc.fillOval(cx - core, cy - core, core * 2.0, core * 2.0);
        gc.setStroke(Color.rgb(194, 108, 255, 0.64));
        gc.setLineWidth(Math.max(1.2, core * 0.034));
        gc.strokeOval(cx - core * 1.055, cy - core * 1.055, core * 2.11, core * 2.11);
        gc.setGlobalAlpha(1.0);
    }

    private void drawStars(GraphicsContext gc, double width, double height) {
        for (int index = 0; index < 210; index++) {
            double x = ((index * 149L + 37) % 2039) / 2039.0 * width;
            double y = ((index * 263L + 71) % 2029) / 2029.0 * height;
            double size = 0.38 + ((index * 17) % 9) * 0.13;
            double alpha = 0.09 + ((index * 29) % 12) * 0.022;
            Color star = index % 11 == 0
                    ? Color.rgb(91, 224, 255, Math.min(0.42, alpha + 0.08))
                    : Color.rgb(211, 222, 255, Math.min(0.38, alpha));
            gc.setFill(star);
            gc.fillOval(x, y, size, size);
        }
    }

    private void drawMotion(double width, double height, double seconds) {
        GraphicsContext gc = motionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        double scale = Math.min(width, Math.max(620, height));
        drawOrbiters(gc, width * 0.78, Math.max(118, height * 0.12),
                Math.max(66, scale * 0.095), -7, seconds, 18, 0.82);
        drawOrbiters(gc, width * 0.10, height * 0.83,
                Math.max(42, scale * 0.060), 14, seconds * 0.72, 9, 0.36);
    }

    private void drawOrbiters(GraphicsContext gc, double cx, double cy, double core,
                              double tiltDegrees, double seconds, int count, double opacity) {
        double tilt = Math.toRadians(tiltDegrees);
        for (int i = 0; i < count; i++) {
            double radius = core * (1.7 + (i % 6) * 0.44);
            double angle = seconds * (0.24 + (i % 4) * 0.04) + i * 2.399963229728653;
            double ox = Math.cos(angle) * radius;
            double oy = Math.sin(angle) * radius * 0.30;
            double x = cx + ox * Math.cos(tilt) - oy * Math.sin(tilt);
            double y = cy + ox * Math.sin(tilt) + oy * Math.cos(tilt);
            double front = (Math.sin(angle) + 1.0) * 0.5;
            double size = 0.65 + (i % 3) * 0.45 + front * 0.50;
            double alpha = opacity * (0.20 + front * 0.55);
            Color color = i % 4 == 0
                    ? Color.rgb(51, 220, 255, alpha)
                    : i % 5 == 0
                    ? Color.rgb(240, 72, 226, alpha)
                    : Color.rgb(230, 236, 255, alpha);
            gc.setFill(color);
            gc.fillOval(x - size / 2.0, y - size / 2.0, size, size);
        }
    }
}
