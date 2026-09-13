package it.casiraghi.swiftbat.ui.components;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Procedural background used by the Black Hole / Reference redesign.
 * Heavy lensing geometry stays on a static canvas; only accreting material is
 * animated on a lightweight overlay.
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
            if (now - lastFrame < 45_000_000L) return; // ~22 fps is enough for a background
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
                ReferenceUiRuntimeHooks.install(newScene);
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
        BlackHolePainter.drawStatic(gc,
                width * 0.80, Math.max(125, height * 0.13),
                Math.max(62, scale * 0.088), -5.5, 0.74);
        BlackHolePainter.drawStatic(gc,
                width * 0.105, height * 0.84,
                Math.max(38, scale * 0.052), 8.0, 0.28);
    }

    private void drawMotion(double width, double height, double seconds) {
        GraphicsContext gc = motionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        double scale = Math.min(width, Math.max(620, height));
        BlackHolePainter.drawInfall(gc,
                width * 0.80, Math.max(125, height * 0.13),
                Math.max(62, scale * 0.088), -5.5, seconds, 34, 0.70);
        BlackHolePainter.drawInfall(gc,
                width * 0.105, height * 0.84,
                Math.max(38, scale * 0.052), 8.0, seconds * 0.82, 18, 0.26);
    }

    private void drawNebulae(GraphicsContext gc, double width, double height) {
        gc.setFill(new RadialGradient(0, 0, width * 0.77, height * 0.16,
                Math.max(width, height) * 0.58, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(91, 38, 176, 0.20)),
                new Stop(0.28, Color.rgb(25, 80, 177, 0.12)),
                new Stop(0.60, Color.rgb(0, 205, 244, 0.045)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);

        gc.setFill(new RadialGradient(0, 0, width * 0.09, height * 0.79,
                Math.max(width, height) * 0.40, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(232, 40, 207, 0.075)),
                new Stop(0.42, Color.rgb(47, 76, 210, 0.060)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);
    }

    private void drawStars(GraphicsContext gc, double width, double height) {
        for (int index = 0; index < 215; index++) {
            double x = ((index * 149L + 37) % 2039) / 2039.0 * width;
            double y = ((index * 263L + 71) % 2029) / 2029.0 * height;
            double size = 0.36 + ((index * 17) % 9) * 0.12;
            double alpha = 0.075 + ((index * 29) % 12) * 0.020;
            Color star = index % 11 == 0
                    ? Color.rgb(91, 224, 255, Math.min(0.36, alpha + 0.07))
                    : Color.rgb(211, 222, 255, Math.min(0.31, alpha));
            gc.setFill(star);
            gc.fillOval(x, y, size, size);
        }
    }
}
