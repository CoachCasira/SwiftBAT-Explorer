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
 * Illustrazione procedurale e scalabile per la Home.
 * Non rappresenta un dato scientifico: e' soltanto l'identita' visiva dell'app.
 *
 * La resa cerca un aspetto piu' vicino a un disco di accrescimento reale:
 * emissione asimmetrica, anello fotonico, arco posteriore deformato dalla lente
 * gravitazionale e polvere molto fine in orbita. Il layer pesante resta statico.
 */
public final class BlackHoleHeroPane extends Region {
    private final Canvas staticCanvas = new Canvas();
    private final Canvas motionCanvas = new Canvas();
    private long startNanos;
    private long lastFrame;

    private final AnimationTimer animation = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (startNanos == 0L) startNanos = now;
            if (now - lastFrame < 33_000_000L) return; // ~30 fps
            lastFrame = now;
            double seconds = (now - startNanos) / 1_000_000_000.0;
            drawMotion(Math.max(1, getWidth()), Math.max(1, getHeight()), seconds);
        }
    };

    public BlackHoleHeroPane() {
        getStyleClass().add("black-hole-hero-pane");
        setMouseTransparent(true);
        getChildren().addAll(staticCanvas, motionCanvas);
        setMinSize(320, 260);
        setPrefSize(560, 370);
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
        drawStars(gc, width, height);

        double cx = width * 0.56;
        double cy = height * 0.52;
        double core = Math.min(width, height) * 0.155;
        double diskW = Math.min(width * 0.94, height * 1.72);
        double diskH = diskW * 0.205;
        double tilt = -7.5;

        // Diffuse halo: faint and broad, without the old cartoon-like perfect rings.
        gc.setFill(new RadialGradient(0, 0, cx, cy, core * 4.4, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(18, 12, 35, 0.00)),
                new Stop(0.28, Color.rgb(118, 54, 185, 0.10)),
                new Stop(0.52, Color.rgb(61, 77, 220, 0.13)),
                new Stop(0.76, Color.rgb(18, 169, 239, 0.065)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(cx - core * 4.4, cy - core * 4.4, core * 8.8, core * 8.8);

        // Rear half of the disk. It rises above the shadow as if gravitationally lensed.
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(tilt);
        gc.setEffect(new GaussianBlur(Math.max(1.0, core * 0.032)));
        for (int i = 0; i < 72; i++) {
            double t = i / 71.0;
            double wobble = 1.0 + 0.012 * Math.sin(i * 2.37) + 0.006 * Math.sin(i * 5.17);
            double w = diskW * (0.58 + 0.42 * t) * wobble;
            double h = diskH * (0.34 + 0.66 * t);
            double alpha = Math.max(0.02, 0.28 * (1.0 - Math.abs(t - 0.42) * 1.55));
            gc.setStroke(diskColor(t, alpha * 0.66));
            gc.setLineWidth(Math.max(0.55, core * (0.012 + (1.0 - t) * 0.010)));
            gc.strokeArc(-w / 2.0, -h / 2.0, w, h, 188, 164, ArcType.OPEN);
        }
        gc.setEffect(null);

        // Thin lensed light above the event-horizon shadow.
        gc.setStroke(Color.rgb(224, 226, 255, 0.48));
        gc.setLineWidth(Math.max(1.0, core * 0.028));
        gc.strokeArc(-core * 1.48, -core * 1.05, core * 2.96, core * 2.10, 198, 145, ArcType.OPEN);
        gc.restore();

        // Event-horizon shadow with a very subtle inner falloff.
        gc.setFill(new RadialGradient(0, 0, cx, cy, core * 1.02, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(0, 0, 1, 1.0)),
                new Stop(0.82, Color.rgb(0, 0, 2, 0.998)),
                new Stop(1.00, Color.rgb(4, 2, 10, 0.995))));
        gc.fillOval(cx - core, cy - core, core * 2.0, core * 2.0);

        // Photon ring: deliberately thin and uneven, with a brighter approaching side.
        gc.setEffect(new GaussianBlur(Math.max(0.5, core * 0.013)));
        gc.setLineWidth(Math.max(1.05, core * 0.025));
        gc.setStroke(Color.rgb(255, 228, 188, 0.72));
        gc.strokeArc(cx - core * 1.055, cy - core * 1.055, core * 2.11, core * 2.11,
                212, 158, ArcType.OPEN);
        gc.setStroke(Color.rgb(112, 178, 255, 0.56));
        gc.strokeArc(cx - core * 1.055, cy - core * 1.055, core * 2.11, core * 2.11,
                8, 202, ArcType.OPEN);
        gc.setEffect(null);

        // Front half of the accretion disk crosses in front of the shadow.
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(tilt);
        gc.setEffect(new GaussianBlur(Math.max(0.75, core * 0.023)));
        for (int i = 0; i < 82; i++) {
            double t = i / 81.0;
            double wobble = 1.0 + 0.010 * Math.sin(i * 2.11 + 0.8) + 0.006 * Math.cos(i * 4.83);
            double w = diskW * (0.56 + 0.44 * t) * wobble;
            double h = diskH * (0.31 + 0.69 * t);
            double center = Math.max(0.0, 1.0 - Math.abs(t - 0.36) * 1.72);
            double alpha = 0.035 + center * 0.36;
            gc.setStroke(diskColor(t, alpha));
            gc.setLineWidth(Math.max(0.65, core * (0.014 + (1.0 - t) * 0.012)));
            gc.strokeArc(-w / 2.0, -h / 2.0, w, h, 8, 164, ArcType.OPEN);
        }
        gc.setEffect(null);
        gc.restore();

        // Doppler-brightened hotspot on the approaching side of the disk.
        double hotspotX = cx + diskW * 0.29;
        double hotspotY = cy - diskH * 0.02;
        gc.setFill(new RadialGradient(0, 0, hotspotX, hotspotY, core * 1.15, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(239, 249, 255, 0.50)),
                new Stop(0.16, Color.rgb(115, 214, 255, 0.30)),
                new Stop(0.48, Color.rgb(115, 77, 236, 0.12)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(hotspotX - core * 1.15, hotspotY - core * 1.15,
                core * 2.30, core * 2.30);
    }

    private Color diskColor(double t, double alpha) {
        if (t < 0.17) return Color.rgb(246, 244, 236, alpha);
        if (t < 0.36) return Color.rgb(255, 181, 123, alpha);
        if (t < 0.58) return Color.rgb(229, 87, 205, alpha * 0.92);
        if (t < 0.78) return Color.rgb(119, 84, 231, alpha * 0.84);
        return Color.rgb(53, 154, 246, alpha * 0.72);
    }

    private void drawMotion(double width, double height, double seconds) {
        GraphicsContext gc = motionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        double cx = width * 0.56;
        double cy = height * 0.52;
        double core = Math.min(width, height) * 0.155;
        double tilt = Math.toRadians(-7.5);

        // Fine dust rather than large glowing beads. Front/back opacity also gives depth.
        for (int i = 0; i < 58; i++) {
            double ring = 1.58 + (i % 10) * 0.245 + ((i * 17) % 7) * 0.018;
            double speed = 0.038 + (i % 6) * 0.0065;
            double angle = seconds * speed * Math.PI * 2.0 + i * 2.399963229728653;
            double rx = core * ring;
            double ry = core * ring * (0.285 + (i % 4) * 0.018);
            double ox = Math.cos(angle) * rx;
            double oy = Math.sin(angle) * ry;
            double x = cx + ox * Math.cos(tilt) - oy * Math.sin(tilt);
            double y = cy + ox * Math.sin(tilt) + oy * Math.cos(tilt);
            double front = (Math.sin(angle) + 1.0) * 0.5;
            double size = 0.34 + (i % 5) * 0.14 + front * 0.28;
            double alpha = 0.06 + front * 0.28;
            Color color = i % 9 == 0
                    ? Color.rgb(85, 215, 255, alpha)
                    : i % 13 == 0
                    ? Color.rgb(229, 104, 218, alpha * 0.84)
                    : Color.rgb(225, 234, 250, alpha * 0.72);
            gc.setFill(color);
            gc.fillOval(x - size / 2.0, y - size / 2.0, size, size);
        }

        // Very faint moving wisps in the disk, enough to suggest rotation without looking artificial.
        double phase = (seconds * 7.0) % 360.0;
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-7.5);
        gc.setEffect(new GaussianBlur(Math.max(0.6, core * 0.014)));
        gc.setLineWidth(Math.max(0.55, core * 0.010));
        for (int i = 0; i < 5; i++) {
            double radius = core * (3.0 + i * 0.42);
            gc.setStroke(i % 2 == 0
                    ? Color.rgb(89, 183, 255, 0.055)
                    : Color.rgb(222, 92, 216, 0.050));
            gc.strokeArc(-radius, -radius * 0.22, radius * 2.0, radius * 0.44,
                    phase + i * 53.0, 42, ArcType.OPEN);
        }
        gc.setEffect(null);
        gc.restore();
    }

    private void drawStars(GraphicsContext gc, double width, double height) {
        for (int i = 0; i < 142; i++) {
            double x = ((i * 83L + 19) % 997) / 997.0 * width;
            double y = ((i * 151L + 47) % 991) / 991.0 * height;
            double size = 0.34 + ((i * 17) % 7) * 0.12;
            double alpha = 0.09 + ((i * 29) % 11) * 0.022;
            gc.setFill(i % 17 == 0
                    ? Color.rgb(117, 207, 255, Math.min(0.42, alpha + 0.08))
                    : Color.rgb(214, 226, 248, Math.min(0.34, alpha)));
            gc.fillOval(x, y, size, size);
        }
    }
}
