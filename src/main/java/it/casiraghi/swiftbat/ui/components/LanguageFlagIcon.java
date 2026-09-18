package it.casiraghi.swiftbat.ui.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/** Piccola bandiera vettoriale usata sotto i pulsanti IT/EN. */
public final class LanguageFlagIcon extends Region {
    public enum Flag { ITALY, UNITED_KINGDOM }

    private final Canvas canvas = new Canvas();
    private final Flag flag;

    public LanguageFlagIcon(Flag flag) {
        this.flag = flag == null ? Flag.ITALY : flag;
        getStyleClass().add("language-flag-icon");
        setMouseTransparent(true);
        getChildren().add(canvas);
        setMinSize(18, 11);
        setPrefSize(20, 12);
        setMaxSize(24, 14);
        widthProperty().addListener((obs, oldValue, newValue) -> requestLayout());
        heightProperty().addListener((obs, oldValue, newValue) -> requestLayout());
    }

    @Override
    protected void layoutChildren() {
        double width = Math.max(1, getWidth());
        double height = Math.max(1, getHeight());
        canvas.setWidth(width);
        canvas.setHeight(height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        if (flag == Flag.ITALY) drawItaly(gc, width, height); else drawUk(gc, width, height);
    }

    private void drawItaly(GraphicsContext gc, double width, double height) {
        double third = width / 3.0;
        gc.setFill(Color.web("#169B62"));
        gc.fillRect(0, 0, third, height);
        gc.setFill(Color.web("#F5F5F5"));
        gc.fillRect(third, 0, third, height);
        gc.setFill(Color.web("#CE2B37"));
        gc.fillRect(third * 2.0, 0, width - third * 2.0, height);
        outline(gc, width, height);
    }

    private void drawUk(GraphicsContext gc, double width, double height) {
        gc.setFill(Color.web("#012169"));
        gc.fillRect(0, 0, width, height);
        gc.setLineCap(StrokeLineCap.BUTT);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(Math.max(2.2, height * 0.30));
        gc.strokeLine(0, 0, width, height);
        gc.strokeLine(width, 0, 0, height);

        gc.setStroke(Color.web("#C8102E"));
        gc.setLineWidth(Math.max(0.9, height * 0.13));
        gc.strokeLine(0, 0, width, height);
        gc.strokeLine(width, 0, 0, height);

        gc.setFill(Color.WHITE);
        gc.fillRect(0, height * 0.35, width, height * 0.30);
        gc.fillRect(width * 0.35, 0, width * 0.30, height);
        gc.setFill(Color.web("#C8102E"));
        gc.fillRect(0, height * 0.42, width, height * 0.16);
        gc.fillRect(width * 0.42, 0, width * 0.16, height);
        outline(gc, width, height);
    }

    private void outline(GraphicsContext gc, double width, double height) {
        gc.setStroke(Color.rgb(214, 232, 255, 0.48));
        gc.setLineWidth(0.7);
        gc.strokeRoundRect(0.35, 0.35, Math.max(0, width - 0.7), Math.max(0, height - 0.7), 2.2, 2.2);
    }
}
