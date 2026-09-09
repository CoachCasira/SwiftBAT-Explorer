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
 * Fondale procedurale usato dal tema Black Hole.
 * Non rappresenta dati scientifici e non intercetta gli eventi del mouse.
 *
 * Il fondale pesante resta statico; il layer animato contiene solo polvere molto
 * fine. I due buchi neri usano una resa meno geometrica e piu' simile a un disco
 * di accrescimento con lente gravitazionale e anello fotonico.
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
            if (now - lastFrame < 50_000_000L) return; // ~20 fps
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
                Math.max(66, scale * 0.095), -7, 0.76);
        drawBlackHole(gc, width * 0.10, height * 0.83,
                Math.max(42, scale * 0.060), 14, 0.30);
    }

    private void drawNebulae(GraphicsContext gc, double width, double height) {
        gc.setFill(new RadialGradient(0, 0, width * 0.76, height * 0.14,
                Math.max(width, height) * 0.58, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(91, 38, 176, 0.20)),
                new Stop(0.28, Color.rgb(25, 80, 177, 0.11)),
                new Stop(0.60, Color.rgb(0, 205, 244, 0.045)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);

        gc.setFill(new RadialGradient(0, 0, width * 0.09, height * 0.78,
                Math.max(width, height) * 0.38, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(232, 40, 207, 0.075)),
                new Stop(0.42, Color.rgb(47, 76, 210, 0.060)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);
    }

    private void drawBlackHole(GraphicsContext gc, double cx, double cy,
                               double core, double angle, double opacity) {
        gc.save();
        gc.setGlobalAlpha(opacity);

        gc.setFill(new RadialGradient(0, 0, cx, cy, core * 4.35, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.TRANSPARENT),
                new Stop(0.32, Color.rgb(118, 49, 175, 0.075)),
                new Stop(0.58, Color.rgb(55, 79, 218, 0.095)),
                new Stop(0.82, Color.rgb(19, 172, 236, 0.040)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(cx - core * 4.35, cy - core * 4.35, core * 8.7, core * 8.7);

        gc.translate(cx, cy);
        gc.rotate(angle);

        // Back/lensed half of the disk.
        gc.setEffect(new GaussianBlur(Math.max(0.9, core * 0.028)));
        for (int index = 0; index < 46; index++) {
            double t = index / 45.0;
            double wobble = 1.0 + 0.012 * Math.sin(index * 2.21) + 0.006 * Math.cos(index * 4.73);
            double ringWidth = core * (2.65 + t * 4.8) * wobble;
            double ringHeight = core * (0.32 + t * 1.04);
            double centerBias = Math.max(0.0, 1.0 - Math.abs(t - 0.40) * 1.72);
            double alpha = 0.018 + centerBias * 0.16;
            gc.setStroke(diskColor(t, alpha));
            gc.setLineWidth(Math.max(0.55, core * (0.010 + (1.0 - t) * 0.010)));
            gc.strokeArc(-ringWidth / 2.0, -ringHeight / 2.0, ringWidth, ringHeight,
                    188, 164, ArcType.OPEN);
        }
        gc.setEffect(null);

        gc.setStroke(Color.rgb(225, 228, 250, 0.32));
        gc.setLineWidth(Math.max(0.8, core * 0.021));
        gc.strokeArc(-core * 1.45, -core * 1.02, core * 2.90, core * 2.04,
                198, 146, ArcType.OPEN);
        gc.restore();

        // Shadow and photon ring.
        gc.setGlobalAlpha(opacity);
        gc.setFill(Color.rgb(0, 0, 2, 0.998));
        gc.fillOval(cx - core, cy - core, core * 2.0, core * 2.0);
        gc.setLineWidth(Math.max(0.85, core * 0.022));
        gc.setStroke(Color.rgb(255, 225, 188, 0.44));
        gc.strokeArc(cx - core * 1.05, cy - core * 1.05, core * 2.10, core * 2.10,
                212, 158, ArcType.OPEN);
        gc.setStroke(Color.rgb(103, 169, 255, 0.34));
        gc.strokeArc(cx - core * 1.05, cy - core * 1.05, core * 2.10, core * 2.10,
                8, 202, ArcType.OPEN);

        // Front half of the disk.
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(angle);
        gc.setEffect(new GaussianBlur(Math.max(0.65, core * 0.020)));
        for (int index = 0; index < 52; index++) {
            double t = index / 51.0;
            double wobble = 1.0 + 0.010 * Math.sin(index * 2.53 + 0.5);
            double ringWidth = core * (2.58 + t * 4.9) * wobble;
            double ringHeight = core * (0.29 + t * 1.09);
            double centerBias = Math.max(0.0, 1.0 - Math.abs(t - 0.36) * 1.78);
            double alpha = 0.022 + centerBias * 0.21;
            gc.setStroke(diskColor(t, alpha));
            gc.setLineWidth(Math.max(0.55, core * (0.011 + (1.0 - t) * 0.010)));
            gc.strokeArc(-ringWidth / 2.0, -ringHeight / 2.0, ringWidth, ringHeight,
                    8, 164, ArcType.OPEN);
        }
        gc.setEffect(null);
        gc.restore();
        gc.setGlobalAlpha(1.0);
    }

    private Color diskColor(double t, double alpha) {
        if (t < 0.18) return Color.rgb(245, 241, 228, alpha);
        if (t < 0.38) return Color.rgb(255, 173, 117, alpha);
        if (t < 0.61) return Color.rgb(221, 78, 202, alpha * 0.90);
        if (t < 0.80) return Color.rgb(115, 83, 224, alpha * 0.82);
        return Color.rgb(49, 145, 241, alpha * 0.70);
    }

    private void drawStars(GraphicsContext gc, double width, double height) {
        for (int index = 0; index < 230; index++) {
            double x = ((index * 149L + 37) % 2039) / 2039.0 * width;
            double y = ((index * 263L + 71) % 2029) / 2029.0 * height;
            double size = 0.30 + ((index * 17) % 9) * 0.10;
            double alpha = 0.07 + ((index * 29) % 12) * 0.018;
            Color star = index % 17 == 0
                    ? Color.rgb(91, 224, 255, Math.min(0.34, alpha + 0.06))
                    : Color.rgb(211, 222, 255, Math.min(0.30, alpha));
            gc.setFill(star);
            gc.fillOval(x, y, size, size);
        }
    }

    private void drawMotion(double width, double height, double seconds) {
        GraphicsContext gc = motionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        double scale = Math.min(width, Math.max(620, height));
        drawOrbiters(gc, width * 0.78, Math.max(118, height * 0.12),
                Math.max(66, scale * 0.095), -7, seconds, 24, 0.62);
        drawOrbiters(gc, width * 0.10, height * 0.83,
                Math.max(42, scale * 0.060), 14, seconds * 0.72, 12, 0.26);
    }

    private void drawOrbiters(GraphicsContext gc, double cx, double cy, double core,
                              double tiltDegrees, double seconds, int count, double opacity) {
        double tilt = Math.toRadians(tiltDegrees);
        for (int i = 0; i < count; i++) {
            double radius = core * (1.62 + (i % 8) * 0.31);
            double angle = seconds * (0.055 + (i % 5) * 0.007) * Math.PI * 2.0
                    + i * 2.399963229728653;
            double ox = Math.cos(angle) * radius;
            double oy = Math.sin(angle) * radius * (0.27 + (i % 3) * 0.018);
            double x = cx + ox * Math.cos(tilt) - oy * Math.sin(tilt);
            double y = cy + ox * Math.sin(tilt) + oy * Math.cos(tilt);
            double front = (Math.sin(angle) + 1.0) * 0.5;
            double size = 0.28 + (i % 4) * 0.12 + front * 0.18;
            double alpha = opacity * (0.07 + front * 0.22);
            Color color = i % 9 == 0
                    ? Color.rgb(81, 211, 249, alpha)
                    : i % 13 == 0
                    ? Color.rgb(224, 100, 213, alpha * 0.84)
                    : Color.rgb(224, 232, 248, alpha * 0.72);
            gc.setFill(color);
            gc.fillOval(x - size / 2.0, y - size / 2.0, size, size);
        }
    }
}
