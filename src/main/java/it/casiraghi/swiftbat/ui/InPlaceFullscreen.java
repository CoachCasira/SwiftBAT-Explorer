package it.casiraghi.swiftbat.ui;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Mostra un contenuto a schermo intero riutilizzando la finestra principale. */
public final class InPlaceFullscreen {
    private static final String ACTIVE_SESSION = InPlaceFullscreen.class.getName() + ".activeSession";

    private InPlaceFullscreen() {
    }

    public static void show(Node owner, String title, Node content) {
        if (owner == null || content == null || owner.getScene() == null
                || !(owner.getScene().getWindow() instanceof Stage stage)) {
            return;
        }
        Scene scene = owner.getScene();
        if (scene.getProperties().containsKey(ACTIVE_SESSION)) {
            return;
        }
        new Session(scene, stage, owner, title, content).open();
    }

    public static void close(Node owner) {
        if (owner == null || owner.getScene() == null) {
            return;
        }
        Object activeSession = owner.getScene().getProperties().get(ACTIVE_SESSION);
        if (activeSession instanceof Session session) {
            session.close(false);
        }
    }

    private static final class Session {
        private final Scene scene;
        private final Stage stage;
        private final Node previousFocus;
        private final Parent originalRoot;
        private final boolean originallyFullscreen;
        private final String originalExitHint;
        private final StackPane fullscreenRoot;
        private final ChangeListener<Boolean> fullscreenListener;
        private final EventHandler<KeyEvent> keyHandler;
        private boolean active;

        private Session(Scene scene, Stage stage, Node owner, String title, Node content) {
            this.scene = scene;
            this.stage = stage;
            this.previousFocus = scene.getFocusOwner() == null ? owner : scene.getFocusOwner();
            this.originalRoot = scene.getRoot();
            this.originallyFullscreen = stage.isFullScreen();
            this.originalExitHint = stage.getFullScreenExitHint();

            Button back = UiFactory.button("← Torna all'app", "secondary-button");
            Label heading = UiFactory.label(title == null || title.isBlank() ? "Schermo intero" : title,
                    "fullscreen-title");
            Label shortcut = UiFactory.label("ESC per uscire", "subtle-text");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox toolbar = new HBox(14, back, heading, spacer, shortcut);
            toolbar.setAlignment(Pos.CENTER_LEFT);
            toolbar.getStyleClass().add("fullscreen-toolbar");

            BorderPane layout = new BorderPane(content);
            layout.setTop(toolbar);
            BorderPane.setMargin(content, new Insets(14, 18, 18, 18));
            layout.getStyleClass().add("fullscreen-layout");
            fullscreenRoot = new StackPane(layout);
            fullscreenRoot.getStyleClass().addAll("app-root", "in-place-fullscreen");

            back.setOnAction(event -> close(false));
            keyHandler = this::handleKeyPressed;
            fullscreenListener = (observable, wasFullscreen, isFullscreen) -> {
                if (active && wasFullscreen && !isFullscreen) {
                    close(true);
                }
            };
        }

        private void open() {
            active = true;
            scene.getProperties().put(ACTIVE_SESSION, this);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
            stage.fullScreenProperty().addListener(fullscreenListener);
            scene.setRoot(fullscreenRoot);
            stage.setFullScreenExitHint("");
            if (!stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
        }

        private void handleKeyPressed(KeyEvent event) {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                close(true);
            }
        }

        private void close(boolean leaveFullscreen) {
            if (!active) {
                return;
            }
            active = false;
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
            stage.fullScreenProperty().removeListener(fullscreenListener);
            scene.getProperties().remove(ACTIVE_SESSION);
            scene.setRoot(originalRoot);
            stage.setFullScreenExitHint(originalExitHint);

            if (leaveFullscreen || !originallyFullscreen) {
                stage.setFullScreen(false);
            }
            if (previousFocus != null) {
                previousFocus.requestFocus();
            }
        }
    }
}
