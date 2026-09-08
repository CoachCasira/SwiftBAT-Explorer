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
import javafx.scene.layout.VBox;
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

            Node preparedContent = prepareContent(title, content);
            BorderPane layout = new BorderPane(preparedContent);
            layout.setTop(toolbar);
            BorderPane.setMargin(preparedContent, new Insets(14, 18, 18, 18));
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

        private Node prepareContent(String title, Node content) {
            String normalized = title == null ? "" : title;
            if (normalized.contains("Modello spettrale") && !normalized.contains("3D")) {
                return wrapSpectralModel2D(content);
            }
            if (normalized.contains("Mappa tempo–energia") || normalized.contains("Modello spettrale 3D")) {
                polishSpectroscopyAssistant(content, normalized);
            }
            return content;
        }

        private Node wrapSpectralModel2D(Node content) {
            if (content instanceof Region region) {
                region.setMinSize(0, 0);
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }

            VBox reading = spectroscopyReadingCard(
                    "Come leggere il modello 2D",
                    "Curva — la linea arancione rappresenta la funzione spettrale ricostruita dal fit ufficiale BAT selezionato. Non è una successione di punti grezzi misurati dal rivelatore.",
                    "Asse X — mostra l'energia dei fotoni in keV, da 15 a 150 keV nella vista corrente.",
                    "Asse Y — mostra log₁₀ N(E), cioè il logaritmo del flusso fotonico differenziale. Valori negativi sono perfettamente normali e indicano N(E) < 1 nelle unità riportate.",
                    "Forma — la pendenza della curva descrive come il contributo previsto dal modello cambia con l'energia. Un andamento più ripido indica una diminuzione più rapida verso le energie elevate.",
                    "Confronto — questa è la stessa funzione visualizzata nella vista 3D: il 3D aggiunge soltanto prospettiva grafica e non introduce una nuova grandezza fisica.",
                    "Da ricordare — il grafico visualizza il modello ricostruito dai parametri del fit BAT; non deriva dalla somma delle quattro curve di luce ASCII."
            );

            HBox body = new HBox(14, content, reading);
            body.setMinSize(0, 0);
            body.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            body.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(content, Priority.ALWAYS);
            return body;
        }

        private VBox spectroscopyReadingCard(String title, String... paragraphs) {
            VBox card = new VBox(17);
            card.getStyleClass().addAll("card", "spectroscopy-assistant");
            card.setPadding(new Insets(22, 20, 22, 20));
            card.setMinWidth(330);
            card.setPrefWidth(380);
            card.setMaxWidth(430);
            card.setMaxHeight(Double.MAX_VALUE);
            card.setFillWidth(true);
            card.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, rgba(18, 40, 60, 0.98), rgba(31, 23, 58, 0.98));"
                            + "-fx-border-color: rgba(92, 212, 239, 0.42);"
                            + "-fx-border-width: 1;"
                            + "-fx-background-radius: 14;"
                            + "-fx-border-radius: 14;"
            );

            Label titleLabel = UiFactory.label(title, "card-title");
            titleLabel.setStyle("-fx-text-fill: #f7fbff; -fx-font-size: 18px; -fx-font-weight: bold;");
            card.getChildren().add(titleLabel);
            for (String paragraph : paragraphs) {
                Label label = UiFactory.wrappedLabel(paragraph, "assistant-copy");
                label.setMinHeight(Region.USE_PREF_SIZE);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setStyle("-fx-text-fill: #e3edfc; -fx-font-size: 13.5px; -fx-line-spacing: 3px;");
                card.getChildren().add(label);
            }
            return card;
        }

        private void polishSpectroscopyAssistant(Node node, String title) {
            if (node instanceof VBox box && box.getStyleClass().contains("spectroscopy-assistant")) {
                box.setSpacing(17);
                box.setPadding(new Insets(22, 20, 22, 20));
                box.setMinWidth(330);
                box.setPrefWidth(380);
                box.setMaxWidth(430);
                box.setMaxHeight(Double.MAX_VALUE);
                box.setFillWidth(true);
                box.setStyle(
                        "-fx-background-color: linear-gradient(to bottom right, rgba(18, 40, 60, 0.98), rgba(31, 23, 58, 0.98));"
                                + "-fx-border-color: rgba(92, 212, 239, 0.42);"
                                + "-fx-border-width: 1;"
                                + "-fx-background-radius: 14;"
                                + "-fx-border-radius: 14;"
                );
                for (Node child : box.getChildren()) {
                    if (child instanceof Label label) {
                        label.setMinHeight(Region.USE_PREF_SIZE);
                        label.setMaxWidth(Double.MAX_VALUE);
                        if (label.getStyleClass().contains("card-title")) {
                            label.setStyle("-fx-text-fill: #f7fbff; -fx-font-size: 18px; -fx-font-weight: bold;");
                        } else {
                            label.setStyle("-fx-text-fill: #e3edfc; -fx-font-size: 13.5px; -fx-line-spacing: 3px;");
                        }
                    }
                }
                if (title.contains("Mappa tempo–energia") && box.getChildren().size() < 9) {
                    Label comparison = UiFactory.wrappedLabel(
                            "Confronto tra bande — leggendo verticalmente lo stesso istante puoi confrontare come il rate si distribuisce tra 15–25, 25–50, 50–100 e 100–350 keV. Le differenze di colore evidenziano variazioni relative del segnale tra i canali.",
                            "assistant-copy");
                    comparison.setMinHeight(Region.USE_PREF_SIZE);
                    comparison.setMaxWidth(Double.MAX_VALUE);
                    comparison.setStyle("-fx-text-fill: #e3edfc; -fx-font-size: 13.5px; -fx-line-spacing: 3px;");
                    Label interpretation = UiFactory.wrappedLabel(
                            "Interpretazione — una zona arancione intensa individua un intervallo temporale in cui il rate netto è elevato in quella banda. Il confronto resta descrittivo: per ottenere un flusso fisico servono risposta strumentale e fit spettroscopico.",
                            "assistant-copy");
                    interpretation.setMinHeight(Region.USE_PREF_SIZE);
                    interpretation.setMaxWidth(Double.MAX_VALUE);
                    interpretation.setStyle("-fx-text-fill: #bfd0ea; -fx-font-size: 13px; -fx-line-spacing: 3px;");
                    box.getChildren().addAll(comparison, interpretation);
                }
            }
            if (node instanceof Parent parent) {
                for (Node child : parent.getChildrenUnmodifiable()) {
                    polishSpectroscopyAssistant(child, title);
                }
            }
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
