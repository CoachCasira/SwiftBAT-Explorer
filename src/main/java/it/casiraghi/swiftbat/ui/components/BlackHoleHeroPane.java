package it.casiraghi.swiftbat.ui.components;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Animated procedural black hole used by the Home hero.
 * The event-horizon geometry is static; only lightweight accretion material is
 * redrawn every frame, keeping the animation smooth without stressing JavaFX.
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

        double cx = width * 0.57;
        double cy = height * 0.54;
        double core = Math.min(width, height) * 0.145;
        BlackHolePainter.drawStatic(gc, cx, cy, core, -5.5, 1.0);
    }

    private void drawMotion(double width, double height, double seconds) {
        GraphicsContext gc = motionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        double cx = width * 0.57;
        double cy = height * 0.54;
        double core = Math.min(width, height) * 0.145;
        BlackHolePainter.drawInfall(gc, cx, cy, core, -5.5, seconds, 66, 1.0);
    }

    private void drawStars(GraphicsContext gc, double width, double height) {
        for (int i = 0; i < 146; i++) {
            double x = ((i * 83L + 19) % 997) / 997.0 * width;
            double y = ((i * 151L + 47) % 991) / 991.0 * height;
            double size = 0.34 + ((i * 17) % 7) * 0.12;
            double alpha = 0.08 + ((i * 29) % 11) * 0.020;
            gc.setFill(i % 17 == 0
                    ? Color.rgb(117, 207, 255, Math.min(0.38, alpha + 0.08))
                    : Color.rgb(214, 226, 248, Math.min(0.30, alpha)));
            gc.fillOval(x, y, size, size);
        }
    }
}
