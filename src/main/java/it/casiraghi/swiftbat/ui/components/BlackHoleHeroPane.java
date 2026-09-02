package it.casiraghi.swiftbat.ui.components;

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
 * Non rappresenta un dato scientifico: è soltanto l'identità visiva dell'app.
 */
public final class BlackHoleHeroPane extends Region {
    private final Canvas canvas = new Canvas();

    public BlackHoleHeroPane() {
        getStyleClass().add("black-hole-hero-pane");
        getChildren().add(canvas);
        setMinSize(320, 280);
        setPrefSize(520, 390);
        widthProperty().addListener((obs, oldValue, newValue) -> requestLayout());
        heightProperty().addListener((obs, oldValue, newValue) -> requestLayout());
    }

    @Override
    protected void layoutChildren() {
        double width = Math.max(1, getWidth());
        double height = Math.max(1, getHeight());
        canvas.setWidth(width);
        canvas.setHeight(height);
        draw(width, height);
    }

    private void draw(double width, double height) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        drawStars(gc, width, height);

        double cx = width * 0.55;
        double cy = height * 0.50;
        double core = Math.min(width, height) * 0.145;
        double diskW = Math.min(width * 0.86, height * 1.52);
        double diskH = diskW * 0.22;

        // Alone gravitazionale esterno.
        gc.save();
        gc.setFill(new RadialGradient(0, 0, cx, cy, core * 3.35, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(91, 40, 154, 0.00)),
                new Stop(0.42, Color.rgb(109, 58, 193, 0.07)),
                new Stop(0.72, Color.rgb(35, 122, 186, 0.10)),
                new Stop(1.00, Color.TRANSPARENT)));
        gc.fillOval(cx - core * 3.35, cy - core * 3.35, core * 6.70, core * 6.70);
        gc.restore();

        // Disco di accrescimento: più archi sovrapposti generano profondità senza asset esterni.
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-8);
        gc.setEffect(new GaussianBlur(Math.max(1.2, core * 0.055)));
        for (int i = 0; i < 54; i++) {
            double t = i / 53.0;
            double w = diskW * (0.64 + 0.36 * t);
            double h = diskH * (0.42 + 0.58 * t);
            double alpha = 0.10 + 0.38 * (1.0 - Math.abs(t - 0.44) * 1.75);
            Color c;
            if (t < 0.34) {
                c = Color.rgb(255, 235, 185, Math.max(0.04, alpha));
            } else if (t < 0.68) {
                c = Color.rgb(255, 151, 62, Math.max(0.04, alpha));
            } else {
                c = Color.rgb(146, 78, 224, Math.max(0.04, alpha * 0.72));
            }
            gc.setStroke(c);
            gc.setLineWidth(Math.max(0.9, core * (0.025 + (1.0 - t) * 0.022)));
            gc.strokeOval(-w / 2.0, -h / 2.0, w, h);
        }
        gc.setEffect(null);

        // Raggio brillante vicino all'orizzonte degli eventi.
        gc.setStroke(Color.rgb(255, 224, 164, 0.74));
        gc.setLineWidth(Math.max(2.0, core * 0.08));
        gc.strokeOval(-core * 1.31, -core * 0.50, core * 2.62, core * 1.00);
        gc.restore();

        // Lente superiore/inferiore: suggerisce il disco piegato attorno al buco nero.
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(-8);
        gc.setStroke(Color.rgb(149, 96, 255, 0.32));
        gc.setLineWidth(Math.max(1.3, core * 0.035));
        gc.strokeArc(-core * 1.85, -core * 1.28, core * 3.70, core * 2.56, 20, 140, javafx.scene.shape.ArcType.OPEN);
        gc.setStroke(Color.rgb(75, 207, 255, 0.22));
        gc.strokeArc(-core * 1.98, -core * 1.37, core * 3.96, core * 2.74, 199, 142, javafx.scene.shape.ArcType.OPEN);
        gc.restore();

        // Ombra centrale + photon ring.
        gc.setFill(Color.rgb(0, 0, 0, 0.98));
        gc.fillOval(cx - core, cy - core, core * 2, core * 2);
        gc.setStroke(Color.rgb(255, 190, 94, 0.52));
        gc.setLineWidth(Math.max(1.5, core * 0.045));
        gc.strokeOval(cx - core * 1.08, cy - core * 1.08, core * 2.16, core * 2.16);

        // Piccola sorgente luminosa asimmetrica per rendere il disco meno artificiale.
        gc.setFill(new RadialGradient(0, 0, cx - diskW * 0.23, cy + diskH * 0.06,
                core * 0.72, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 239, 194, 0.35)),
                new Stop(1, Color.TRANSPARENT)));
        gc.fillOval(cx - diskW * 0.23 - core * 0.72, cy - core * 0.66,
                core * 1.44, core * 1.44);
    }

    private void drawStars(GraphicsContext gc, double width, double height) {
        for (int i = 0; i < 92; i++) {
            double x = ((i * 83L + 19) % 997) / 997.0 * width;
            double y = ((i * 151L + 47) % 991) / 991.0 * height;
            double size = 0.55 + ((i * 17) % 7) * 0.18;
            double alpha = 0.16 + ((i * 29) % 11) * 0.035;
            gc.setFill(Color.rgb(205, 224, 255, Math.min(0.55, alpha)));
            gc.fillOval(x, y, size, size);
        }
    }
}
