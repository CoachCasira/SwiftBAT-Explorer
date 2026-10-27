package it.casiraghi.swiftbat.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * One-shot construction animation of the ALREADY INITIALIZED Home.
 *
 * This does not rebuild UI, replace Scene/Stage, add pulse listeners or alter
 * event/model loading. Only Node opacity and translation properties animate.
 * Once finished the Timeline is released and interaction is normal.
 */
public final class HomeEntranceReveal {
    private final BorderPane app;
    private final List<Node> prepared = new ArrayList<>();
    private final List<Node> navigation = new ArrayList<>();
    private final List<Node> homeSections = new ArrayList<>();
    private Timeline timeline;
    private boolean preparedOnce;
    private boolean completed;

    public HomeEntranceReveal(BorderPane app) {
        this.app = app;
    }

    /** Call after MainView.initialize(), while the cinematic overlay is opaque. */
    public void prepare() {
        if (preparedOnce) return;
        preparedOnce = true;

        Node top = app.getTop();
        Node left = app.getLeft();
        if (top != null) hide(top, 0);
        if (left != null) {
            hide(left, 0);
            if (left instanceof Parent parent) {
                for (Node node : parent.getChildrenUnmodifiable()) {
                    if (node.isManaged() && !(node instanceof javafx.scene.layout.Region
                            && node.getStyleClass().contains("soft-separator"))) {
                        hide(node, 8);
                        navigation.add(node);
                    }
                }
            }
        }

        // Only the four content groups, not nested individual chart controls:
        // welcome, live summary, central hero and footer shortcuts.
        Node home = app.lookup(".mockup-home-content");
        if (home instanceof VBox page) {
            for (Node section : page.getChildren()) {
                hide(section, 17);
                homeSections.add(section);
            }
        }
    }

    private void hide(Node node, double offsetY) {
        node.setOpacity(0);
        node.setTranslateY(offsetY);
        prepared.add(node);
    }

    public void play() {
        if (completed) return;
        if (!preparedOnce) prepare();
        app.setMouseTransparent(true);
        timeline = new Timeline();
        if (app.getTop() != null) fadeIn(app.getTop(), 70, 330, 0);
        if (app.getLeft() != null) fadeIn(app.getLeft(), 140, 300, 0);

        int position = 0;
        for (Node node : navigation) {
            // The first six are the navigation buttons; remaining elements
            // (footer, source text, external link) join at the end.
            long delay = position < 6 ? 230L + position * 95 : 680L + (position - 6) * 75;
            fadeIn(node, delay, 315, 8);
            position++;
        }

        for (int i = 0; i < homeSections.size(); i++) {
            fadeIn(homeSections.get(i), 320L + i * 175L, 520, 17);
        }

        timeline.setOnFinished(event -> restore());
        timeline.playFromStart();
    }

    /** Skip must immediately reveal the fully usable Home with no animation. */
    public void showImmediately() {
        if (completed) return;
        if (timeline != null) timeline.stop();
        restore();
    }

    private void fadeIn(Node node, long delayMs, long durationMs, double startY) {
        if (startY != 0) node.setTranslateY(startY);
        Duration from = Duration.millis(delayMs);
        Duration to = Duration.millis(delayMs + durationMs);
        timeline.getKeyFrames().addAll(
                new KeyFrame(from,
                        new KeyValue(node.opacityProperty(), 0),
                        new KeyValue(node.translateYProperty(), startY)),
                new KeyFrame(to,
                        new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT)));
    }

    private void restore() {
        if (completed) return;
        completed = true;
        for (Node node : prepared) {
            node.setOpacity(1);
            node.setTranslateY(0);
        }
        app.setMouseTransparent(false);
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        Platform.runLater(app::requestFocus);
    }
}
