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

import java.util.ArrayList;
import java.util.List;

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
        private static final String[] READING_COLORS = {
                "#ffb45f", "#6edcff", "#c8a2ff", "#79e7b5", "#ffd36f", "#ff9ec7", "#8fb8ff", "#f7a96b"
        };

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
            body.setFillHeight(true);
            HBox.setHgrow(content, Priority.ALWAYS);
            return body;
        }

        private VBox spectroscopyReadingCard(String title, String... paragraphs) {
            VBox card = new VBox(10);
            configureReadingCard(card);

            Label titleLabel = UiFactory.label(title, "card-title");
            styleReadingTitle(titleLabel);
            card.getChildren().add(titleLabel);
            for (int index = 0; index < paragraphs.length; index++) {
                VBox section = readingSection(paragraphs[index], index);
                VBox.setVgrow(section, Priority.ALWAYS);
                card.getChildren().add(section);
            }
            return card;
        }

        private void configureReadingCard(VBox card) {
            card.getStyleClass().addAll("card", "spectroscopy-assistant");
            card.setSpacing(10);
            card.setPadding(new Insets(20, 18, 20, 18));
            card.setMinWidth(340);
            card.setPrefWidth(395);
            card.setMaxWidth(440);
            card.setMinHeight(0);
            card.setMaxHeight(Double.MAX_VALUE);
            card.setFillWidth(true);
            card.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, rgba(18, 40, 60, 0.99), rgba(31, 23, 58, 0.99));"
                            + "-fx-border-color: rgba(92, 212, 239, 0.48);"
                            + "-fx-border-width: 1;"
                            + "-fx-background-radius: 14;"
                            + "-fx-border-radius: 14;"
            );
        }

        private VBox readingSection(String paragraph, int index) {
            String heading = paragraph == null ? "" : paragraph;
            String body = "";
            int separator = heading.indexOf(" — ");
            if (separator >= 0) {
                body = heading.substring(separator + 3).trim();
                heading = heading.substring(0, separator).trim();
            }

            Label sectionTitle = UiFactory.label(heading, "assistant-copy");
            sectionTitle.setWrapText(true);
            sectionTitle.setMinHeight(Region.USE_PREF_SIZE);
            sectionTitle.setMaxWidth(Double.MAX_VALUE);
            sectionTitle.setStyle("-fx-text-fill: " + READING_COLORS[index % READING_COLORS.length]
                    + "; -fx-font-size: 13.5px; -fx-font-weight: bold;");

            Label sectionBody = UiFactory.wrappedLabel(body, "assistant-copy");
            sectionBody.setMinHeight(Region.USE_PREF_SIZE);
            sectionBody.setMaxWidth(Double.MAX_VALUE);
            sectionBody.setStyle("-fx-text-fill: #e7f0ff; -fx-font-size: 13.2px; -fx-line-spacing: 3px;");

            VBox section = new VBox(5, sectionTitle, sectionBody);
            section.setPadding(new Insets(9, 10, 9, 10));
            section.setFillWidth(true);
            section.setMinHeight(Region.USE_PREF_SIZE);
            section.setMaxHeight(Double.MAX_VALUE);
            section.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.028);"
                            + "-fx-background-radius: 9;"
                            + "-fx-border-color: rgba(255,255,255,0.035);"
                            + "-fx-border-radius: 9;"
            );
            return section;
        }

        private void styleReadingTitle(Label label) {
            label.setMinHeight(Region.USE_PREF_SIZE);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setStyle("-fx-text-fill: #f8fbff; -fx-font-size: 18px; -fx-font-weight: bold;");
        }

        private void polishSpectroscopyAssistant(Node node, String title) {
            if (node instanceof VBox box && box.getStyleClass().contains("spectroscopy-assistant")) {
                configureReadingCard(box);

                if (title.contains("Mappa tempo–energia")) {
                    rebuildMapReadingCard(box);
                } else {
                    for (Node child : box.getChildren()) {
                        if (child instanceof Label label) {
                            label.setMinHeight(Region.USE_PREF_SIZE);
                            label.setMaxWidth(Double.MAX_VALUE);
                            if (label.getStyleClass().contains("card-title")) {
                                styleReadingTitle(label);
                            } else {
                                label.setStyle("-fx-text-fill: #e7f0ff; -fx-font-size: 13.2px; -fx-line-spacing: 3px;");
                            }
                        }
                    }
                }
            }
            if (node instanceof Parent parent) {
                for (Node child : parent.getChildrenUnmodifiable()) {
                    polishSpectroscopyAssistant(child, title);
                }
            }
        }

        private void rebuildMapReadingCard(VBox box) {
            Label titleLabel = null;
            List<String> paragraphs = new ArrayList<>();
            for (Node child : List.copyOf(box.getChildren())) {
                if (child instanceof Label label) {
                    if (label.getStyleClass().contains("card-title") && titleLabel == null) {
                        titleLabel = label;
                    } else if (label.getText() != null && !label.getText().isBlank()) {
                        paragraphs.add(label.getText());
                    }
                }
            }

            if (paragraphs.stream().noneMatch(text -> text.startsWith("Confronto tra bande"))) {
                paragraphs.add("Confronto tra bande — leggendo verticalmente lo stesso istante puoi confrontare come il rate si distribuisce tra 15–25, 25–50, 50–100 e 100–350 keV. Le differenze di colore evidenziano variazioni relative del segnale tra i canali.");
            }
            if (paragraphs.stream().noneMatch(text -> text.startsWith("Interpretazione"))) {
                paragraphs.add("Interpretazione — una zona arancione intensa individua un intervallo temporale in cui il rate netto è elevato in quella banda. Il confronto resta descrittivo: per ottenere un flusso fisico servono risposta strumentale e fit spettroscopico.");
            }

            box.getChildren().clear();
            if (titleLabel == null) {
                titleLabel = UiFactory.label("Come leggere la mappa", "card-title");
            }
            styleReadingTitle(titleLabel);
            box.getChildren().add(titleLabel);
            for (int index = 0; index < paragraphs.size(); index++) {
                VBox section = readingSection(paragraphs.get(index), index);
                VBox.setVgrow(section, Priority.ALWAYS);
                box.getChildren().add(section);
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
