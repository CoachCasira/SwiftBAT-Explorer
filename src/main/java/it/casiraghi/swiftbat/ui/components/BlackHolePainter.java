package it.casiraghi.swiftbat.ui.components;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;

/**
 * Shared procedural black-hole renderer used by the redesign.
 *
 * <p>The geometry deliberately avoids complete Saturn-like ellipses. The rear
 * accretion flow is bent above and below the shadow to suggest gravitational
 * lensing, while a bright foreground stream crosses the event-horizon shadow.
 * The motion layer adds material spiralling inward instead of particles simply
 * orbiting at a fixed radius.</p>
 */
final class BlackHolePainter {
    private BlackHolePainter() { }

    static void drawStatic(GraphicsContext gc, double cx, double cy, double core,
                           double tiltDegrees, double opacity) {
        gc.save();
        gc.setGlobalAlpha(opacity);

        // Broad, low-contrast glow around the lensing region.
        gc.setFill(new RadialGradient(0, 0, cx, cy, core * 5.2, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(255, 187, 110, 0.035)),
                new Stop(0.19, Color.rgb(226, 72, 205, 0.10)),
                new Stop(0.46, Color.rgb(83, 78, 225, 0.10)),
                new Stop(0.72, Color.rgb(22, 159, 235, 0.05)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(cx - core * 5.2, cy - core * 5.2, core * 10.4, core * 10.4);

        gc.translate(cx, cy);
        gc.rotate(tiltDegrees);

        // Rear accretion flow. Curved Bezier streamlines climb above the shadow:
        // this is what visually separates the object from a ringed planet.
        gc.setGlobalBlendMode(BlendMode.ADD);
        gc.setEffect(new GaussianBlur(Math.max(0.7, core * 0.020)));
        for (int i = 0; i < 62; i++) {
            double t = i / 61.0;
            double extent = core * (3.15 + 1.35 * t);
            double crown = core * (1.34 + 1.12 * t);
            double edgeY = core * (0.10 + 0.28 * t);
            double jitter = core * (0.018 * Math.sin(i * 2.71) + 0.010 * Math.cos(i * 5.13));
            double alpha = 0.018 + 0.20 * Math.max(0.0, 1.0 - Math.abs(t - 0.40) * 1.75);

            gc.setStroke(streamColor(t, alpha));
            gc.setLineWidth(Math.max(0.48, core * (0.008 + (1.0 - t) * 0.008)));
            gc.beginPath();
            gc.moveTo(-extent, edgeY + jitter);
            gc.bezierCurveTo(-core * 2.55, -core * 0.08 + jitter,
                    -core * 1.46, -crown, 0, -crown - jitter * 0.45);
            gc.bezierCurveTo(core * 1.46, -crown,
                    core * 2.55, -core * 0.08 - jitter,
                    extent, edgeY - jitter);
            gc.stroke();
        }

        // Fainter lower lensed image of the rear disk.
        for (int i = 0; i < 26; i++) {
            double t = i / 25.0;
            double extent = core * (1.70 + 1.05 * t);
            double trough = core * (1.14 + 0.65 * t);
            double alpha = 0.012 + 0.065 * (1.0 - t);
            gc.setStroke(streamColor(0.20 + t * 0.55, alpha));
            gc.setLineWidth(Math.max(0.42, core * 0.006));
            gc.beginPath();
            gc.moveTo(-extent, core * 0.28);
            gc.bezierCurveTo(-core * 1.18, core * 0.62,
                    -core * 0.72, trough, 0, trough);
            gc.bezierCurveTo(core * 0.72, trough,
                    core * 1.18, core * 0.62,
                    extent, core * 0.28);
            gc.stroke();
        }
        gc.setEffect(null);
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);

        // Event-horizon shadow.
        gc.setFill(new RadialGradient(0, 0, 0, 0, core * 1.06, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(0, 0, 0, 1.0)),
                new Stop(0.83, Color.rgb(0, 0, 1, 1.0)),
                new Stop(1.00, Color.rgb(2, 1, 6, 0.998))));
        gc.fillOval(-core, -core, core * 2.0, core * 2.0);

        // Uneven photon ring / critical curve.
        gc.setGlobalBlendMode(BlendMode.ADD);
        gc.setEffect(new GaussianBlur(Math.max(0.45, core * 0.010)));
        gc.setLineWidth(Math.max(0.9, core * 0.022));
        gc.setStroke(Color.rgb(255, 226, 179, 0.67));
        gc.strokeArc(-core * 1.045, -core * 1.045, core * 2.09, core * 2.09,
                205, 154, ArcType.OPEN);
        gc.setStroke(Color.rgb(105, 177, 255, 0.47));
        gc.strokeArc(-core * 1.045, -core * 1.045, core * 2.09, core * 2.09,
                4, 205, ArcType.OPEN);
        gc.setEffect(null);

        // Foreground accretion stream. These are open curves, not complete rings.
        for (int i = 0; i < 74; i++) {
            double t = i / 73.0;
            double extent = core * (3.25 + 1.45 * t);
            double y = core * (-0.02 + 0.52 * t);
            double bend = core * (0.08 + 0.10 * t);
            double jitter = core * (0.012 * Math.sin(i * 3.17));
            double center = Math.max(0.0, 1.0 - Math.abs(t - 0.33) * 1.65);
            double alpha = 0.025 + center * 0.31;
            gc.setStroke(streamColor(t, alpha));
            gc.setLineWidth(Math.max(0.52, core * (0.009 + (1.0 - t) * 0.012)));
            gc.beginPath();
            gc.moveTo(-extent, y + bend + jitter);
            gc.bezierCurveTo(-core * 2.15, y - bend,
                    -core * 0.90, y - core * 0.06,
                    0, y);
            gc.bezierCurveTo(core * 0.90, y + core * 0.05,
                    core * 2.15, y + bend * 0.35,
                    extent, y - bend - jitter);
            gc.stroke();
        }
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.setEffect(null);
        gc.restore();

        // Doppler-brightened side: intentionally asymmetric.
        double hotspotX = cx + Math.cos(Math.toRadians(tiltDegrees)) * core * 2.45;
        double hotspotY = cy + Math.sin(Math.toRadians(tiltDegrees)) * core * 2.45;
        gc.setGlobalAlpha(opacity);
        gc.setFill(new RadialGradient(0, 0, hotspotX, hotspotY, core * 1.35, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(244, 250, 255, 0.42)),
                new Stop(0.18, Color.rgb(122, 216, 255, 0.24)),
                new Stop(0.50, Color.rgb(139, 73, 228, 0.10)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(hotspotX - core * 1.35, hotspotY - core * 1.35,
                core * 2.70, core * 2.70);
        gc.restore();
    }

    static void drawInfall(GraphicsContext gc, double cx, double cy, double core,
                           double tiltDegrees, double seconds, int count, double opacity) {
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(tiltDegrees);
        gc.setGlobalBlendMode(BlendMode.ADD);

        // Material spirals inward: radius shrinks continuously instead of staying
        // on a fixed orbit, so the animation reads as accretion / capture.
        for (int i = 0; i < count; i++) {
            double speed = 0.060 + (i % 7) * 0.0075;
            double phase = fract(seconds * speed + i * 0.0731);
            double previous = Math.max(0.0, phase - 0.030);

            Point now = spiralPoint(core, phase, i);
            Point before = spiralPoint(core, previous, i);
            double fadeIn = Math.min(1.0, phase / 0.10);
            double fadeOut = Math.min(1.0, (1.0 - phase) / 0.10);
            double fade = Math.max(0.0, Math.min(fadeIn, fadeOut));
            double front = 0.45 + 0.55 * ((Math.sin(now.theta) + 1.0) * 0.5);
            double alpha = opacity * fade * front * (0.18 + 0.34 * phase);

            Color color = (i % 9 == 0)
                    ? Color.rgb(82, 213, 255, alpha)
                    : (i % 5 == 0)
                    ? Color.rgb(234, 78, 214, alpha)
                    : Color.rgb(255, 207, 143, alpha);

            gc.setStroke(color);
            gc.setLineWidth(Math.max(0.48, core * (0.006 + phase * 0.010)));
            gc.strokeLine(before.x, before.y, now.x, now.y);

            if (i % 4 == 0) {
                double size = Math.max(0.7, core * (0.010 + 0.010 * phase));
                gc.setFill(color);
                gc.fillOval(now.x - size * 0.5, now.y - size * 0.5, size, size);
            }
        }

        // A handful of longer wisps make the inward flow visible even on large displays.
        gc.setEffect(new GaussianBlur(Math.max(0.45, core * 0.008)));
        for (int i = 0; i < 7; i++) {
            double phase = fract(seconds * (0.032 + i * 0.0028) + i * 0.131);
            gc.setStroke(i % 2 == 0
                    ? Color.rgb(89, 197, 255, opacity * 0.080)
                    : Color.rgb(235, 91, 211, opacity * 0.070));
            gc.setLineWidth(Math.max(0.42, core * 0.006));
            gc.beginPath();
            Point first = spiralPoint(core, Math.max(0.0, phase - 0.20), i + 31);
            gc.moveTo(first.x, first.y);
            for (int step = 1; step <= 10; step++) {
                double p = Math.max(0.0, phase - 0.20 + step * 0.020);
                Point point = spiralPoint(core, p, i + 31);
                gc.lineTo(point.x, point.y);
            }
            gc.stroke();
        }
        gc.setEffect(null);
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.restore();
    }

    private static Point spiralPoint(double core, double phase, int seed) {
        double clamped = Math.max(0.0, Math.min(1.0, phase));
        double radius = core * (4.65 - 3.38 * clamped);
        double theta = seed * 2.399963229728653 + clamped * Math.PI * (4.6 + (seed % 4) * 0.22);
        double flatten = 0.235 + (seed % 5) * 0.010;
        double x = Math.cos(theta) * radius;
        double y = Math.sin(theta) * radius * flatten;
        return new Point(x, y, theta);
    }

    private static Color streamColor(double t, double alpha) {
        if (t < 0.14) return Color.rgb(250, 247, 238, alpha);
        if (t < 0.34) return Color.rgb(255, 185, 112, alpha);
        if (t < 0.58) return Color.rgb(239, 88, 202, alpha * 0.96);
        if (t < 0.80) return Color.rgb(126, 82, 230, alpha * 0.88);
        return Color.rgb(56, 155, 248, alpha * 0.78);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private record Point(double x, double y, double theta) { }
}
