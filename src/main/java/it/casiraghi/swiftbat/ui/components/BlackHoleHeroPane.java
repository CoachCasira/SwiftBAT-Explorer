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

/**
 * Illustrazione procedurale e scalabile per la Home.
 * Non rappresenta un dato scientifico: e' soltanto l'identita' visiva dell'app.
 *
 * Il buco nero rimane su un layer statico; un secondo canvas molto leggero anima
 * particelle e stelle su orbite ellittiche. In questo modo l'effetto resta fluido
 * senza ridisegnare ad ogni frame il disco di accrescimento completo.
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
        double cy = height * 0.51;
        double core = Math.min(width, height) * 0.155;
        double diskW = Math.min(width * 0.90, height * 1.62);
        double diskH = diskW * 0.22;

        gc.setFill(new RadialGradient(0, 0, cx, cy, core * 3.55, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(91, 40, 154, 0.00)),
                new Stop(0.34, Color.rgb(238, 55, 212, 0.15)),
                new Stop(0.62, Color.rgb(76, 78, 244, 0.18)),
                new Stop(0.83, Color.rgb(0, 214, 249, 0.10)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(cx - core * 3.55, cy - core * 3.55, core * 7.10, core * 7.10);

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-8);
        gc.setEffect(new GaussianBlur(Math.max(1.2, core * 0.05)));
        for (int i = 0; i < 66; i++) {
            double t = i / 65.0;
            double w = diskW * (0.61 + 0.39 * t);
            double h = diskH * (0.38 + 0.62 * t);
            double alpha = 0.08 + 0.42 * (1.0 - Math.abs(t - 0.42) * 1.72);
            Color c;
            if (t < 0.20) {
                c = Color.rgb(255, 241, 203, Math.max(0.04, alpha));
            } else if (t < 0.43) {
                c = Color.rgb(255, 133, 82, Math.max(0.04, alpha));
            } else if (t < 0.73) {
                c = Color.rgb(238, 55, 220, Math.max(0.04, alpha * 0.90));
            } else {
                c = Color.rgb(49, 120, 255, Math.max(0.04, alpha * 0.78));
            }
            gc.setStroke(c);
            gc.setLineWidth(Math.max(0.8, core * (0.021 + (1.0 - t) * 0.018)));
            gc.strokeOval(-w / 2.0, -h / 2.0, w, h);
        }
        gc.setEffect(null);
        gc.setStroke(Color.rgb(255, 224, 164, 0.78));
        gc.setLineWidth(Math.max(2.0, core * 0.075));
        gc.strokeOval(-core * 1.32, -core * 0.50, core * 2.64, core * 1.00);
        gc.setStroke(Color.rgb(239, 76, 242, 0.48));
        gc.setLineWidth(Math.max(1.2, core * 0.034));
        gc.strokeArc(-core * 1.90, -core * 1.30, core * 3.80, core * 2.60, 19, 142,
                javafx.scene.shape.ArcType.OPEN);
        gc.setStroke(Color.rgb(41, 208, 255, 0.38));
        gc.strokeArc(-core * 2.02, -core * 1.40, core * 4.04, core * 2.80, 198, 144,
                javafx.scene.shape.ArcType.OPEN);
        gc.restore();

        gc.setFill(Color.rgb(0, 0, 2, 0.995));
        gc.fillOval(cx - core, cy - core, core * 2, core * 2);
        gc.setStroke(Color.rgb(199, 105, 255, 0.70));
        gc.setLineWidth(Math.max(1.5, core * 0.047));
        gc.strokeOval(cx - core * 1.08, cy - core * 1.08, core * 2.16, core * 2.16);

        gc.setFill(new RadialGradient(0, 0, cx - diskW * 0.23, cy + diskH * 0.05,
                core * 0.80, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 239, 194, 0.42)),
                new Stop(1, Color.TRANSPARENT)));
        gc.fillOval(cx - diskW * 0.23 - core * 0.80, cy - core * 0.75,
                core * 1.60, core * 1.60);
    }

    private void drawMotion(double width, double height, double seconds) {
        GraphicsContext gc = motionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        double cx = width * 0.56;
        double cy = height * 0.51;
        double core = Math.min(width, height) * 0.155;

        // Stelle/particelle che orbitano realmente attorno al buco nero.
        for (int i = 0; i < 34; i++) {
            double ring = 1.70 + (i % 7) * 0.32;
            double speed = 0.15 + (i % 5) * 0.027;
            double angle = seconds * speed * Math.PI * 2.0 + i * 2.399963229728653;
            double rx = core * ring;
            double ry = core * ring * (0.34 + (i % 3) * 0.045);
            double tilt = Math.toRadians(-8);
            double ox = Math.cos(angle) * rx;
            double oy = Math.sin(angle) * ry;
            double x = cx + ox * Math.cos(tilt) - oy * Math.sin(tilt);
            double y = cy + ox * Math.sin(tilt) + oy * Math.cos(tilt);

            double front = (Math.sin(angle) + 1.0) * 0.5;
            double size = 0.8 + (i % 4) * 0.42 + front * 0.75;
            double alpha = 0.22 + front * 0.55;
            Color color = i % 4 == 0
                    ? Color.rgb(54, 224, 255, alpha)
                    : i % 5 == 0
                    ? Color.rgb(248, 83, 230, alpha)
                    : Color.rgb(225, 232, 255, alpha);

            gc.setFill(new RadialGradient(0, 0, x, y, size * 4.2, false, CycleMethod.NO_CYCLE,
                    new Stop(0, color), new Stop(0.26, color.deriveColor(0, 1, 1, 0.55)),
                    new Stop(1, Color.TRANSPARENT)));
            gc.fillOval(x - size * 4.2, y - size * 4.2, size * 8.4, size * 8.4);
            gc.setFill(color);
            gc.fillOval(x - size / 2.0, y - size / 2.0, size, size);
        }

        // Un paio di archi luminosi molto lenti danno l'idea che il disco respiri.
        double pulse = 0.50 + 0.50 * Math.sin(seconds * 0.85);
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-8);
        gc.setStroke(Color.rgb(232, 67, 238, 0.10 + pulse * 0.13));
        gc.setLineWidth(Math.max(1.0, core * 0.025));
        gc.strokeOval(-core * 2.75, -core * 0.78, core * 5.50, core * 1.56);
        gc.restore();
    }

    private void drawStars(GraphicsContext gc, double width, double height) {
        for (int i = 0; i < 116; i++) {
            double x = ((i * 83L + 19) % 997) / 997.0 * width;
            double y = ((i * 151L + 47) % 991) / 991.0 * height;
            double size = 0.52 + ((i * 17) % 7) * 0.17;
            double alpha = 0.13 + ((i * 29) % 11) * 0.035;
            gc.setFill(i % 13 == 0
                    ? Color.rgb(86, 224, 255, Math.min(0.58, alpha + 0.10))
                    : Color.rgb(211, 225, 255, Math.min(0.52, alpha)));
            gc.fillOval(x, y, size, size);
        }
    }
}
