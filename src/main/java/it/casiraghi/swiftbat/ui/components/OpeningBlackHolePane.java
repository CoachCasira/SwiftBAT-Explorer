package it.casiraghi.swiftbat.ui.components;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;

/**
 * Experimental splash illustration for the single-window opening.
 *
 * The costly lensing geometry and the star field are painted only when this
 * Region is laid out/resized. A separate transparent Canvas animates a small
 * number of accretion particles at a capped ~25 fps. No external resources,
 * video decoders, web calls, JavaFX chart nodes, or timers survive disposal.
 */
public final class OpeningBlackHolePane extends Region {
    private final Canvas backdrop = new Canvas();
    private final Canvas particles = new Canvas();
    private long firstFrame;
    private long lastFrame;
    private boolean disposed;

    private final AnimationTimer animation = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (disposed || getScene() == null || !isVisible()) return;
            if (lastFrame != 0 && now - lastFrame < 40_000_000L) return;
            if (firstFrame == 0) firstFrame = now;
            lastFrame = now;
            paintParticles((now - firstFrame) / 1_000_000_000.0);
        }
    };

    public OpeningBlackHolePane() {
        getStyleClass().add("opening-black-hole");
        setMouseTransparent(true);
        setMinSize(0, 0);
        getChildren().addAll(backdrop, particles);
        widthProperty().addListener((obs, before, after) -> requestLayout());
        heightProperty().addListener((obs, before, after) -> requestLayout());
        sceneProperty().addListener((obs, before, after) -> {
            if (disposed || after == null) animation.stop();
            else {
                firstFrame = 0L;
                lastFrame = 0L;
                animation.start();
            }
        });
    }

    @Override
    protected void layoutChildren() {
        final double width = Math.max(1, getWidth());
        final double height = Math.max(1, getHeight());
        if (backdrop.getWidth() == width && backdrop.getHeight() == height) return;
        backdrop.setWidth(width);
        backdrop.setHeight(height);
        particles.setWidth(width);
        particles.setHeight(height);
        paintStatic(width, height);
        paintParticles(0);
    }

    private void paintStatic(double width, double height) {
        GraphicsContext gc = backdrop.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#020610"));
        gc.fillRect(0, 0, width, height);

        // All light-transport geometry is rendered ONCE. Never use blur or
        // software effects on a giant Canvas during application startup.
        double cx = width * 0.51;
        double cy = height * 0.405;
        double core = Math.min(width * 0.112, height * 0.150);

        gc.setFill(new RadialGradient(0, 0, cx, cy,
                Math.max(width, height) * 0.58, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(91, 40, 150, 0.26)),
                new Stop(0.22, Color.rgb(33, 67, 144, 0.18)),
                new Stop(0.55, Color.rgb(10, 25, 68, 0.045)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);

        for (int i = 0; i < 178; i++) {
            double x = ((i * 769L + 191) % 2003) / 2003.0 * width;
            double y = ((i * 1277L + 53) % 2011) / 2011.0 * height;
            double size = i % 33 == 0 ? 1.55 : 0.45 + (i % 5) * 0.14;
            double alpha = i % 33 == 0 ? 0.52 : 0.12 + (i % 7) * 0.03;
            gc.setFill(i % 9 == 0
                    ? Color.rgb(95, 213, 255, alpha)
                    : Color.rgb(200, 218, 255, alpha));
            gc.fillOval(x, y, size, size);
        }

        // Rear disk with gravitational lensing rising behind the shadow.
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-7.2);
        for (int i = 0; i < 23; i++) {
            double t = i / 22.0;
            double extent = core * (3.0 + t * 1.46);
            double crown = core * (1.35 + t * 0.95);
            double alpha = 0.07 + 0.30 * (1.0 - Math.abs(t - 0.44) * 1.5);
            gc.setStroke(diskColor(t, Math.max(0.02, alpha)));
            gc.setLineWidth(0.8 + (1 - t) * 1.4);
            gc.beginPath();
            gc.moveTo(-extent, core * (0.19 + t * 0.18));
            gc.bezierCurveTo(-core * 2.2, -core * 0.12,
                    -core * 1.2, -crown, 0, -crown);
            gc.bezierCurveTo(core * 1.2, -crown,
                    core * 2.2, -core * 0.12,
                    extent, core * (0.19 + t * 0.18));
            gc.stroke();
        }
        for (int i = 0; i < 10; i++) {
            double t = i / 9.0;
            gc.setStroke(diskColor(t, 0.045 + (1 - t) * 0.06));
            gc.setLineWidth(0.7);
            gc.beginPath();
            gc.moveTo(-core * (1.7 + t), core * 0.24);
            gc.bezierCurveTo(-core * 1.2, core * 1.5,
                    core * 1.2, core * 1.5,
                    core * (1.7 + t), core * 0.24);
            gc.stroke();
        }

        // Central black shadow, with a narrow photon ring and asymmetric
        // cyan/orange edge. The foreground disk is drawn AFTER this shadow.
        gc.setFill(new RadialGradient(0, 0, 0, 0,
                core * 1.06, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.BLACK),
                new Stop(0.91, Color.rgb(1, 2, 9)),
                new Stop(1.00, Color.rgb(6, 12, 25))));
        gc.fillOval(-core, -core, core * 2, core * 2);
        gc.setLineWidth(Math.max(1.2, core * 0.018));
        gc.setStroke(Color.rgb(255, 222, 175, 0.72));
        gc.strokeArc(-core * 1.034, -core * 1.034,
                core * 2.068, core * 2.068, 210, 155, ArcType.OPEN);
        gc.setStroke(Color.rgb(74, 198, 255, 0.62));
        gc.strokeArc(-core * 1.034, -core * 1.034,
                core * 2.068, core * 2.068, 4, 203, ArcType.OPEN);

        // Broad foreground accretion stream; only a few translucent splines.
        for (int i = 0; i < 34; i++) {
            double t = i / 33.0;
            double extent = core * (3.15 + t * 1.36);
            double y = core * (-0.10 + t * 0.54);
            double bend = core * (0.07 + t * 0.15);
            double centerGlow = Math.max(0.0, 1.0 - Math.abs(t - 0.32) * 1.58);
            gc.setStroke(diskColor(t, 0.06 + centerGlow * 0.47));
            gc.setLineWidth(0.9 + centerGlow * 1.4);
            gc.beginPath();
            gc.moveTo(-extent, y + bend);
            gc.bezierCurveTo(-core * 2.0, y - bend,
                    -core * 0.80, y - core * 0.035, 0, y);
            gc.bezierCurveTo(core * 0.80, y + core * 0.035,
                    core * 2.0, y + bend * 0.40,
                    extent, y - bend);
            gc.stroke();
        }
        gc.restore();
    }

    private void paintParticles(double seconds) {
        GraphicsContext gc = particles.getGraphicsContext2D();
        double width = particles.getWidth();
        double height = particles.getHeight();
        if (width <= 0 || height <= 0) return;
        gc.clearRect(0, 0, width, height);
        double core = Math.min(width * 0.112, height * 0.150);
        double cx = width * 0.51;
        double cy = height * 0.405;

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-7.2);
        // Pure line segments, no effects/blur: deliberately cheap at ~25fps
        // on Windows and macOS with either GPU or software rendering.
        for (int i = 0; i < 38; i++) {
            double phase = fract(seconds * (0.07 + (i % 6) * 0.006) + i * 0.061);
            double before = Math.max(0, phase - 0.035);
            double angle = i * 2.399963 + phase * Math.PI * 4.2;
            double prevAngle = i * 2.399963 + before * Math.PI * 4.2;
            double radius = core * (4.35 - phase * 3.1);
            double prevRadius = core * (4.35 - before * 3.1);
            double fade = Math.min(Math.min(phase / 0.11, (1 - phase) / 0.14), 1.0);
            gc.setStroke(diskColor(phase, 0.25 * Math.max(0, fade)));
            gc.setLineWidth(0.8 + phase * 0.7);
            gc.strokeLine(
                    Math.cos(prevAngle) * prevRadius,
                    Math.sin(prevAngle) * prevRadius * 0.28,
                    Math.cos(angle) * radius,
                    Math.sin(angle) * radius * 0.28);
        }
        gc.restore();
    }

    private static Color diskColor(double position, double alpha) {
        if (position < 0.25) return Color.rgb(255, 227, 181, alpha);
        if (position < 0.50) return Color.rgb(250, 133, 183, alpha);
        if (position < 0.73) return Color.rgb(157, 96, 234, alpha);
        return Color.rgb(66, 194, 255, alpha);
    }

    private static double fract(double number) {
        return number - Math.floor(number);
    }

    public void dispose() {
        disposed = true;
        animation.stop();
    }
}
