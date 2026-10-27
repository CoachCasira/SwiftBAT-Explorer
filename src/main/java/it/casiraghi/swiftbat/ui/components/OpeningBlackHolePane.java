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

        // Nebula and gravitational lensing are static; only motion is redrawn.
        double centerX = width * 0.51;
        double centerY = height * 0.405;
        double core = Math.min(width * 0.112, height * 0.150);
        gc.setFill(new RadialGradient(0, 0, centerX, centerY,
                Math.max(width, height) * 0.57, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(55, 31, 107, 0.37)),
                new Stop(0.25, Color.rgb(19, 40, 100, 0.22)),
                new Stop(0.62, Color.rgb(5, 14, 39, 0.06)),
                new Stop(1, Color.TRANSPARENT)));
        gc.fillRect(0, 0, width, height);

        for (int i = 0; i < 205; i++) {
            // Stable, evenly dispersed pseudo-random field, no Random allocated.
            double x = ((i * 769L + 191) % 2003) / 2003.0 * width;
            double y = ((i * 1277L + 53) % 2011) / 2011.0 * height;
            double radius = i % 31 == 0 ? 1.55 : 0.45 + (i % 5) * 0.16;
            double alpha = (i % 31 == 0 ? 0.55 : 0.12 + (i % 7) * 0.033);
            gc.setFill(i % 9 == 0
                    ? Color.rgb(75, 206, 255, alpha)
                    : Color.rgb(199, 219, 255, alpha));
            gc.fillOval(x, y, radius, radius);
        }

        // Reuse the exact artistic black-hole language of the Home screen.
        BlackHolePainter.drawStatic(gc, centerX, centerY, core, -7.2, 1.0);

        // Thin vignette keeps the title and CTA readable over the accretion.
        gc.setFill(new RadialGradient(0, 0, centerX, centerY,
                Math.max(width, height) * 0.73, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.TRANSPARENT),
                new Stop(0.70, Color.rgb(0, 3, 12, 0.08)),
                new Stop(1.00, Color.rgb(0, 3, 12, 0.68))));
        gc.fillRect(0, 0, width, height);
    }

    private void paintParticles(double seconds) {
        GraphicsContext gc = particles.getGraphicsContext2D();
        double width = particles.getWidth();
        double height = particles.getHeight();
        if (width <= 0 || height <= 0) return;
        gc.clearRect(0, 0, width, height);
        double core = Math.min(width * 0.112, height * 0.150);
        BlackHolePainter.drawInfall(gc, width * 0.51, height * 0.405,
                core, -7.2, seconds, 44, 0.94);
    }

    public void dispose() {
        disposed = true;
        animation.stop();
    }
}
