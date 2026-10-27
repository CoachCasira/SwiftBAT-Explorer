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
        gc.setFill(Color.web("#02050e"));
        gc.fillRect(0, 0, width, height);

        double cx = width * 0.51;
        double cy = height * 0.405;
        double core = Math.min(width * 0.112, height * 0.15);

        // Atmospheric scattering: broad, soft radial gradients are rendered
        // ONCE into the static Canvas, never into the 25 fps motion layer.
        gc.setFill(new RadialGradient(0, 0, cx, cy,
                Math.max(width, height) * 0.59, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(57, 38, 113, 0.44)),
                new Stop(0.18, Color.rgb(40, 58, 130, 0.21)),
                new Stop(0.43, Color.rgb(9, 31, 71, 0.11)),
                new Stop(0.78, Color.rgb(4, 11, 28, 0.03)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);

        for (int i = 0; i < 260; i++) {
            double x = ((i * 769L + 191) % 2003) / 2003.0 * width;
            double y = ((i * 1277L + 53) % 2011) / 2011.0 * height;
            double size = i % 29 == 0 ? 1.70 : 0.42 + (i % 5) * 0.13;
            double alpha = i % 29 == 0 ? 0.57 : 0.10 + (i % 7) * 0.03;
            gc.setFill(i % 9 == 0 ? Color.rgb(91, 206, 255, alpha)
                    : Color.rgb(206, 220, 251, alpha));
            gc.fillOval(x, y, size, size);
        }

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-6.5);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // A diffuse torus of hot gas replaces the old thin-line "Saturn"
        // illustration. Layered broad ribbons create luminous turbulent plasma.
        // The far side is gravitationally lensed ABOVE the black shadow.
        gc.setFill(new RadialGradient(0, 0, -core * 0.40, -core * 0.35,
                core * 5.30, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(251, 130, 71, 0.04)),
                new Stop(0.28, Color.rgb(235, 95, 164, 0.095)),
                new Stop(0.58, Color.rgb(79, 84, 197, 0.09)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(-core * 5.7, -core * 4.3, core * 11.4, core * 8.6);

        // Gaussian-like wide glow around the rear lensed disk. Each layer is
        // a simple Bézier stroke; no per-frame blur, reflection or 3D scene.
        for (int g = 0; g < 16; g++) {
            double w = 42 - g * 2.25;
            double xExt = core * (3.48 + g * 0.028);
            double top = core * (1.73 + g * 0.025);
            gc.setStroke(g < 7 ? Color.rgb(251, 141, 92, 0.014)
                    : Color.rgb(129, 103, 239, 0.017));
            gc.setLineWidth(Math.max(2.0, w));
            gc.beginPath();
            gc.moveTo(-xExt, core * 0.25);
            gc.bezierCurveTo(-core * 2.2, -core * 0.43,
                    -core * 1.48, -top, 0, -top);
            gc.bezierCurveTo(core * 1.48, -top,
                    core * 2.2, -core * 0.43,
                    xExt, core * 0.25);
            gc.stroke();
        }

        for (int lane = 0; lane < 112; lane++) {
            double t = lane / 111.0;
            double jitter = Math.sin(lane * 2.91) * 0.027
                    + Math.sin(lane * 0.47) * 0.032;
            double xExt = core * (2.88 + t * 2.10);
            double dome = core * (1.25 + t * 1.10);
            double edge = core * (0.11 + t * 0.35);
            double intensity = Math.pow(Math.max(0, 1 - Math.abs(t - 0.40) * 1.45), 1.45);
            gc.setStroke(plasmaColor(t, 0.035 + intensity * 0.37));
            gc.setLineWidth(0.5 + 0.55 * intensity + (lane % 9 == 0 ? 1.3 : 0));
            gc.beginPath();
            gc.moveTo(-xExt, edge + core * jitter);
            gc.bezierCurveTo(-core * (2.50 + t * 0.35),
                    -core * (0.22 + t * 0.24),
                    -core * 1.13, -dome * 1.04, -core * 0.13, -dome);
            gc.bezierCurveTo(core * 1.15, -dome,
                    core * (2.56 + t * 0.33),
                    -core * (0.18 + t * 0.19),
                    xExt, edge - core * jitter);
            gc.stroke();
        }

        // Faint secondary lower lensed image, only partially exposed.
        for (int lane = 0; lane < 25; lane++) {
            double t = lane / 24.0;
            double d = core * (1.60 + t * 1.16);
            gc.setStroke(plasmaColor(1 - t, 0.018 + (1 - t) * 0.075));
            gc.setLineWidth(0.7);
            gc.beginPath();
            gc.moveTo(-d, core * 0.17);
            gc.bezierCurveTo(-core * 0.96, core * (1.12 + t * 0.50),
                    core * 0.96, core * (1.12 + t * 0.50), d, core * 0.17);
            gc.stroke();
        }

        // Shadow, gravitationally hot photon edge and color-dependent glow.
        gc.setFill(new RadialGradient(0, 0, 0, 0, core * 1.23,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(0, 1, 5)),
                new Stop(0.74, Color.rgb(1, 1, 5)),
                new Stop(0.90, Color.rgb(1, 2, 7)),
                new Stop(1.00, Color.rgb(7, 6, 17, 0.0))));
        gc.fillOval(-core * 1.23, -core * 1.23, core * 2.46, core * 2.46);
        gc.setFill(Color.rgb(0, 0, 3, 0.998));
        gc.fillOval(-core * 0.995, -core * 0.995, core * 1.99, core * 1.99);

        for (int i = 8; i >= 1; i--) {
            gc.setLineWidth(i * 1.08);
            gc.setStroke(i > 3 ? Color.rgb(240, 116, 79, 0.021)
                    : Color.rgb(255, 218, 168, 0.14));
            gc.strokeArc(-core * 1.03, -core * 1.03,
                    core * 2.06, core * 2.06, 185, 161, ArcType.OPEN);
            gc.setStroke(i > 3 ? Color.rgb(42, 151, 241, 0.026)
                    : Color.rgb(78, 177, 246, 0.16));
            gc.strokeArc(-core * 1.03, -core * 1.03,
                    core * 2.06, core * 2.06, 350, 205, ArcType.OPEN);
        }

        // Near-side accretion disk: optical depth increases toward the
        // approaching bright limb, which is warm white/orange (Doppler beaming).
        // A wide body and sharp turbulent filaments replace flat crossing lines.
        for (int layer = 0; layer < 20; layer++) {
            double widthStroke = 63 - layer * 2.75;
            double y = core * (0.56 + layer * 0.009);
            gc.setStroke(layer < 10
                    ? Color.rgb(243, 112, 81, 0.013 + layer * 0.0005)
                    : Color.rgb(249, 181, 112, 0.018));
            gc.setLineWidth(Math.max(1, widthStroke));
            gc.beginPath();
            gc.moveTo(-core * 4.70, y + core * 0.16);
            gc.bezierCurveTo(-core * 2.14, y - core * 0.24,
                    core * 1.70, y - core * 0.14,
                    core * 4.70, y - core * 0.15);
            gc.stroke();
        }

        for (int lane = 0; lane < 132; lane++) {
            double t = lane / 131.0;
            double wobble = 0.055 * Math.sin(lane * 2.17)
                    + 0.023 * Math.cos(lane * 0.53);
            double xExt = core * (3.22 + t * 1.78);
            double y = core * (0.35 + t * 0.55 + wobble);
            double brightness = Math.max(0, 1 - Math.abs(t - 0.38) * 1.65);
            gc.setStroke(plasmaColor(t * 0.91, 0.044 + brightness * 0.54));
            gc.setLineWidth(0.72 + brightness * 0.76 + (lane % 11 == 0 ? 1.0 : 0));
            gc.beginPath();
            gc.moveTo(-xExt, y + core * (0.10 + t * 0.06));
            gc.bezierCurveTo(-core * 2.1, y - core * 0.30,
                    core * 1.9, y - core * 0.12,
                    xExt, y - core * (0.12 + t * 0.04));
            gc.stroke();
        }

        // Dense amber-white Doppler hot spot, blended through layered
        // transparent gradients, with no costly live GaussianBlur effect.
        for (int glow = 5; glow >= 0; glow--) {
            double gx = -core * 1.95;
            double gy = core * 0.49;
            double radius = core * (0.78 + glow * 0.36);
            gc.setFill(new RadialGradient(0, 0, gx, gy,
                    radius, false, CycleMethod.NO_CYCLE,
                    new Stop(0.00, Color.rgb(255, 211, 154, 0.12)),
                    new Stop(0.19, Color.rgb(255, 160, 105, 0.095)),
                    new Stop(0.53, Color.rgb(242, 85, 134, 0.042)),
                    new Stop(1.00, Color.TRANSPARENT)));
            gc.fillOval(gx - radius, gy - radius, radius * 2, radius * 2);
        }

        // Fine dust/granulation provides volume: a broad, ragged plasma
        // distribution rather than ruler-straight lines.
        for (int i = 0; i < 1120; i++) {
            double u = fract(i * 0.61803398875 + 0.13);
            double v = fract(i * 0.41421356237 + 0.67);
            double x = core * (u * 8.9 - 4.45);
            double y = core * (0.42 + v * 0.68
                    - 0.13 * Math.cos(x / (core * 4.5) * Math.PI));
            double intensity = Math.pow(Math.max(0, 1 - Math.abs(v - 0.34) * 1.55), 2.0);
            double size = 0.32 + (i % 7) * 0.18;
            gc.setFill(plasmaColor(0.10 + v * 0.72,
                    Math.min(0.38, 0.025 + intensity * 0.32)));
            gc.fillOval(x, y, size, size);
        }
        gc.restore();

        // Vignette from outside the singularity keeps foreground titles crisp.
        gc.setFill(new RadialGradient(0, 0, cx, cy,
                Math.max(width, height) * 0.77, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.TRANSPARENT),
                new Stop(0.70, Color.rgb(0, 2, 10, 0.03)),
                new Stop(1.00, Color.rgb(0, 2, 10, 0.63))));
        gc.fillRect(0, 0, width, height);
    }

    private static Color plasmaColor(double position, double opacity) {
        // Hot bright approaching side, cooling toward pink, violet and cyan.
        double alpha = Math.max(0, Math.min(0.99, opacity));
        if (position < 0.16) return Color.rgb(255, 241, 201, alpha);
        if (position < 0.35) return Color.rgb(255, 184, 108, alpha);
        if (position < 0.55) return Color.rgb(246, 110, 156, alpha);
        if (position < 0.77) return Color.rgb(159, 110, 234, alpha);
        return Color.rgb(64, 190, 252, alpha);
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
            gc.setStroke(plasmaColor(phase * 0.8, 0.25 * Math.max(0, fade)));
            gc.setLineWidth(0.8 + phase * 0.7);
            gc.strokeLine(
                    Math.cos(prevAngle) * prevRadius,
                    Math.sin(prevAngle) * prevRadius * 0.28,
                    Math.cos(angle) * radius,
                    Math.sin(angle) * radius * 0.28);
        }
        gc.restore();
    }

    private static double fract(double number) {
        return number - Math.floor(number);
    }

    public void dispose() {
        disposed = true;
        animation.stop();
    }
}
