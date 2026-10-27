package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.OpeningBlackHolePane;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * Experimental one-window opening. It is an overlay on the existing Scene:
 * no second Stage/Scene, no web assets or blocking application-thread work.
 *
 * An entrance animation scales the existing Canvas as a texture; it does NOT
 * rerender thousands of graphics primitives at each transition frame.
 * Both Enter and Skip dispose the AnimationTimer and release this overlay.
 */
public final class BlackHoleOpening extends StackPane {
    private final OpeningBlackHolePane blackHole = new OpeningBlackHolePane();
    private final Scale horizonZoom = new Scale(1, 1);
    private final VBox captions;
    private final Button start;
    private final Button skip;
    private final Runnable onFinished;
    private final Runnable onSkipped;
    private Timeline transition;
    private Timeline reveal;
    private boolean finishing;

    public BlackHoleOpening(Runnable onFinished) {
        this(onFinished, onFinished);
    }

    public BlackHoleOpening(Runnable onFinished, Runnable onSkipped) {
        this.onFinished = onFinished == null ? () -> { } : onFinished;
        this.onSkipped = onSkipped == null ? this.onFinished : onSkipped;
        getStyleClass().add("opening-overlay");
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setFocusTraversable(true);

        blackHole.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        blackHole.getTransforms().add(horizonZoom);
        // Scale around the image's physical black-hole position, not
        // around the centre of the window (which would miss the horizon).
        horizonZoom.pivotXProperty().bind(blackHole.widthProperty().multiply(0.56));
        horizonZoom.pivotYProperty().bind(blackHole.heightProperty().multiply(0.31));

        HBox top = new HBox();
        top.getStyleClass().add("opening-topbar");
        top.setAlignment(Pos.CENTER_LEFT);
        Label brand = new Label("◇  NASA SWIFT / BAT   ·   GRB EXPLORATION");
        brand.getStyleClass().add("opening-brand");
        Region topGap = new Region();
        HBox.setHgrow(topGap, Priority.ALWAYS);
        Label edition = new Label("SWIFTBAT EXPLORER   /   1.3.0");
        edition.getStyleClass().add("opening-edition");
        top.getChildren().addAll(brand, topGap, edition);

        Label eyebrow = new Label(localized(
                "OLTRE L'ORIZZONTE DEGLI EVENTI",
                "BEYOND THE EVENT HORIZON"));
        eyebrow.getStyleClass().add("opening-eyebrow");

        Label heading = new Label("SWIFTBAT");
        heading.getStyleClass().add("opening-title");
        Label headingAccent = new Label("EXPLORER");
        headingAccent.getStyleClass().add("opening-title-accent");
        HBox title = new HBox(18, heading, headingAccent);
        title.getStyleClass().add("opening-title-row");
        title.setAlignment(Pos.CENTER);

        Label subheading = new Label(localized(
                "L'Universo estremo ti aspetta. Esplora i lampi di raggi gamma.",
                "The extreme universe is waiting. Explore gamma-ray bursts."));
        subheading.getStyleClass().add("opening-subtitle");

        start = new Button(localized("INIZIA L'ESPLORAZIONE   ↗", "BEGIN EXPLORATION   ↗"));
        start.getStyleClass().addAll("primary-button", "opening-start-button");
        start.setId("opening-start");
        start.setOnAction(event -> enter());

        skip = new Button(localized("Salta l'apertura", "Skip intro"));
        skip.getStyleClass().addAll("ghost-button", "opening-skip-button");
        skip.setId("opening-skip");
        skip.setOnAction(event -> skip());

        HBox actions = new HBox(16, start, skip);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("opening-actions");

        Label hint = new Label(localized(
                "INVIO PER INIZIARE   •   ESC PER SALTARE",
                "ENTER TO BEGIN   •   ESC TO SKIP"));
        hint.getStyleClass().add("opening-hint");

        captions = new VBox(9, eyebrow, title, subheading, actions, hint);
        captions.getStyleClass().add("opening-copy");
        captions.setAlignment(Pos.CENTER);
        captions.setPadding(new Insets(0, 25, 40, 25));
        captions.setOpacity(0);
        captions.setTranslateY(14);
        reveal = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(captions.opacityProperty(), 0),
                        new KeyValue(captions.translateYProperty(), 14)),
                new KeyFrame(Duration.millis(750),
                        new KeyValue(captions.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(captions.translateYProperty(), 0, Interpolator.EASE_OUT)));

        BorderPane layout = new BorderPane();
        layout.setMouseTransparent(false);
        layout.setTop(top);
        layout.setBottom(captions);
        BorderPane.setAlignment(captions, Pos.CENTER);
        // Raise typography and CTA slightly without overlapping the disk.
        BorderPane.setMargin(captions, new Insets(0, 0, 74, 0));

        getChildren().addAll(blackHole, layout);
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                enter();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                skip();
                event.consume();
            }
        });
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene == null && newScene != null && !finishing) {
                reveal.playFromStart();
            } else if (oldScene != null && newScene == null) {
                if (transition != null) transition.stop();
                reveal.stop();
                blackHole.dispose();
            }
        });
    }

    private static String localized(String italian, String english) {
        return I18n.language() == I18n.Language.IT ? italian : english;
    }

    /** Click Begin: a short zoom INTO the existing black hole, same Stage. */
    public void enter() {
        if (finishing) return;
        finishing = true;
        reveal.stop();
        start.setDisable(true);
        skip.setDisable(true);
        blackHole.pauseParticles(); // Transition uses cached images, not live Canvas rendering.

        // Enter precisely through the photographic event horizon.
        double shiftX = blackHole.getWidth() * (0.50 - 0.56);
        double shiftY = blackHole.getHeight() * (0.50 - 0.31);
        transition = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(horizonZoom.xProperty(), 1),
                        new KeyValue(horizonZoom.yProperty(), 1),
                        new KeyValue(blackHole.translateXProperty(), 0),
                        new KeyValue(blackHole.translateYProperty(), 0),
                        new KeyValue(blackHole.tunnelOpacityProperty(), 0),
                        new KeyValue(captions.opacityProperty(), 1),
                        new KeyValue(opacityProperty(), 1)),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(horizonZoom.xProperty(), 1.17, Interpolator.EASE_IN),
                        new KeyValue(horizonZoom.yProperty(), 1.17, Interpolator.EASE_IN),
                        new KeyValue(blackHole.tunnelOpacityProperty(), 0.04),
                        new KeyValue(captions.opacityProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(720),
                        new KeyValue(horizonZoom.xProperty(), 2.6, Interpolator.EASE_IN),
                        new KeyValue(horizonZoom.yProperty(), 2.6, Interpolator.EASE_IN),
                        new KeyValue(blackHole.translateXProperty(), shiftX, Interpolator.EASE_IN),
                        new KeyValue(blackHole.translateYProperty(), shiftY, Interpolator.EASE_IN),
                        new KeyValue(blackHole.tunnelOpacityProperty(), 0.93)),
                new KeyFrame(Duration.millis(1190),
                        new KeyValue(horizonZoom.xProperty(), 9.0, Interpolator.EASE_IN),
                        new KeyValue(horizonZoom.yProperty(), 9.0, Interpolator.EASE_IN),
                        new KeyValue(blackHole.tunnelOpacityProperty(), 1),
                        new KeyValue(opacityProperty(), 0, Interpolator.EASE_BOTH)));
        transition.setOnFinished(event -> complete(true));
        transition.play();
    }

    /** Escape / Skip: enter the already constructed application immediately. */
    public void skip() {
        if (finishing) return;
        finishing = true;
        reveal.stop();
        complete(false);
    }

    private void complete(boolean animateHome) {
        blackHole.dispose();
        if (getParent() instanceof javafx.scene.layout.Pane parent) {
            parent.getChildren().remove(this);
        }
        if (animateHome) onFinished.run();
        else onSkipped.run();
    }
}
