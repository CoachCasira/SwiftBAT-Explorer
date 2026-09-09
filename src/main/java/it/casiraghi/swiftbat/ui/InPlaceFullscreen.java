package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
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
            String italianTitle = title == null || title.isBlank() ? "Schermo intero" : title;
            Label heading = UiFactory.label("", "fullscreen-title");
            I18n.setText(heading, italianTitle, I18n.english(italianTitle));
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
            if (normalized.contains("Modello spettrale 3D")) {
                return wrapSpectralModel3D(content);
            }
            if (normalized.contains("Modello spettrale")) {
                return wrapSpectralModel2D(content);
            }
            if (normalized.contains("Flusso energetico 3D")) {
                return wrapFlux3D(content);
            }
            if (normalized.contains("Flusso energetico per banda")) {
                return wrapFlux2D(content);
            }
            if (normalized.contains("Mappa tempo–energia")) {
                return wrapTimeEnergy(content);
            }
            return content;
        }

        private Node wrapSpectralModel2D(Node content) {
            VBox reading = spectroscopyReadingCard(
                    "Come leggere il modello 2D",
                    "Curva — la linea arancione rappresenta la funzione spettrale ricostruita dal fit ufficiale BAT selezionato. Non è una successione di punti grezzi misurati dal rivelatore.",
                    "Asse X — mostra l'energia dei fotoni in keV, da 15 a 150 keV nella vista corrente.",
                    "Asse Y — mostra log₁₀ N(E), cioè il logaritmo del flusso fotonico differenziale. Valori negativi sono perfettamente normali e indicano N(E) < 1 nelle unità riportate.",
                    "Forma — la pendenza della curva descrive come il contributo previsto dal modello cambia con l'energia. Un andamento più ripido indica una diminuzione più rapida verso le energie elevate.",
                    "Confronto — questa è la stessa funzione visualizzata nella vista 3D: il 3D aggiunge soltanto prospettiva grafica e non introduce una nuova grandezza fisica.",
                    "Da ricordare — il grafico visualizza il modello ricostruito dai parametri del fit BAT; non deriva dalla somma delle quattro curve di luce ASCII."
            );
            return responsiveReadingLayout(content, reading);
        }

        private Node wrapSpectralModel3D(Node content) {
            VBox reading = spectroscopyReadingCard(
                    "Come leggere la vista 3D",
                    "Modello — la curva arancione rappresenta la stessa funzione spettrale ricostruita mostrata nella vista 2D. Non sono aggiunti nuovi punti osservativi.",
                    "Assi — X indica l'energia dei fotoni in keV; Y indica log₁₀ N(E), il logaritmo del flusso fotonico differenziale previsto dal fit.",
                    "Forma della curva — la pendenza mostra come il contributo del modello diminuisce o varia passando verso energie più elevate.",
                    "Profondità — il piano arretrato e i collegamenti servono soltanto alla prospettiva. Non rappresentano tempo, distanza, intensità o una terza variabile fisica.",
                    "Interazione — trascina per cambiare la prospettiva interna, usa la rotellina per lo zoom e fai doppio clic per ricentrare la vista.",
                    "Interpretazione — zoom e prospettiva cambiano soltanto la visualizzazione: energia, N(E) e parametri del fit rimangono invariati."
            );
            return responsiveReadingLayout(content, reading);
        }

        private Node wrapFlux2D(Node content) {
            VBox reading = spectroscopyReadingCard(
                    "Come leggere l'istogramma",
                    "Barre — ogni barra rappresenta il flusso energetico integrato pubblicato da BAT per una specifica banda energetica.",
                    "Asse X — separa le bande di energia riportate dal catalogo, così da confrontare rapidamente dove il modello concentra più flusso.",
                    "Asse Y — misura il flusso energetico in erg cm⁻² s⁻¹. Una barra più alta indica un flusso integrato maggiore nella banda corrispondente.",
                    "Intervalli al 90% — i limiti di confidenza ufficiali restano disponibili nella tabella e nel tooltip delle barre; l'altezza mostra il valore centrale pubblicato.",
                    "Confronto — questo istogramma e la curva spettrale descrivono due aspetti dello stesso fit ufficiale, ma non sono quattro curve di luce sommate.",
                    "Da ricordare — le barre derivano dai prodotti spettroscopici BAT/XSPEC e non dai rate ASCII a bin di un secondo."
            );
            return responsiveReadingLayout(content, reading);
        }

        private Node wrapFlux3D(Node content) {
            VBox reading = spectroscopyReadingCard(
                    "Come leggere il flusso 3D",
                    "Barre — sono gli stessi valori dell'istogramma 2D, disposti in prospettiva per facilitare il confronto visivo tra le bande energetiche.",
                    "Assi — X identifica la banda energetica; l'altezza della barra rappresenta il flusso energetico integrato pubblicato da BAT.",
                    "Scala — i valori sono mostrati in unità di 10⁻¹² erg cm⁻² s⁻¹ per mantenere una scala numerica leggibile senza alterare i rapporti tra le bande.",
                    "Profondità — serve esclusivamente a separare graficamente le barre. Non è una distanza e non aggiunge una nuova grandezza fisica.",
                    "Interazione — trascina per cambiare prospettiva, usa la rotellina per lo zoom e il pulsante Centra vista per tornare all'inquadratura iniziale.",
                    "Da ricordare — i limiti al 90% restano consultabili nella tabella 2D; la vista 3D mostra i valori centrali del fit ufficiale."
            );
            return responsiveReadingLayout(content, reading);
        }

        private Node wrapTimeEnergy(Node content) {
            VBox reading = spectroscopyReadingCard(
                    "Come leggere la mappa",
                    "Assi — X rappresenta il tempo rispetto al trigger t = 0; Y separa le quattro bande energetiche BAT.",
                    "Colore — arancio indica un rate netto positivo, blu una fluttuazione negativa dopo la sottrazione del fondo; i toni scuri indicano valori vicini a zero.",
                    "Dettaglio — spostando il mouse sulla mappa puoi leggere banda energetica, centro del bin, rate e larghezza della banda nel punto osservato.",
                    "Scala temporale — ogni cella deriva dai rate ASCII a bin di 1 secondo e la finestra visualizzata è la stessa scelta nella scheda Spettroscopia.",
                    "Confronto tra bande — leggendo verticalmente lo stesso istante puoi confrontare come il rate si distribuisce tra 15–25, 25–50, 50–100 e 100–350 keV. Le differenze di colore evidenziano variazioni relative del segnale tra i canali.",
                    "Interpretazione — una zona arancione intensa individua un intervallo temporale in cui il rate netto è elevato in quella banda. Il confronto resta descrittivo: per ottenere un flusso fisico servono risposta strumentale e fit spettroscopico.",
                    "Da ricordare — questa mappa descrive i rate BAT nel tempo: non è un fit XSPEC e non converte direttamente i conteggi in flusso fisico."
            );
            return responsiveReadingLayout(content, reading);
        }

        private Node responsiveReadingLayout(Node content, VBox reading) {
            if (content instanceof Region region) {
                region.setMinSize(0, 0);
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }

            ScrollPane readingScroll = new ScrollPane(reading);
            readingScroll.setFitToWidth(true);
            readingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            readingScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            readingScroll.setPannable(true);
            readingScroll.setMinSize(0, 0);
            readingScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
            reading.setMinHeight(Region.USE_PREF_SIZE);

            ToggleButton help = new ToggleButton(I18n.t("Mostra spiegazione"));
            help.getStyleClass().addAll("ghost-button", "help-toggle");
            HBox helpBar = new HBox(UiFactory.spacer(), help);
            helpBar.setAlignment(Pos.CENTER_RIGHT);

            BorderPane split = new BorderPane();
            split.setCenter(content);
            split.setMinSize(0, 0);
            split.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            Runnable relayout = () -> {
                if (!help.isSelected()) {
                    split.setRight(null);
                    split.setBottom(null);
                    BorderPane.setMargin(content, Insets.EMPTY);
                } else {
                    updateResponsiveReadingLayout(split, content, reading, readingScroll, split.getWidth());
                }
            };
            help.selectedProperty().addListener((obs, oldValue, selected) -> {
                help.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
                relayout.run();
                Platform.runLater(() -> {
                    relayout.run();
                    split.applyCss();
                    split.requestLayout();
                });
            });
            I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                    help.setText(I18n.t(help.isSelected() ? "Nascondi spiegazione" : "Mostra spiegazione")));
            split.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                if (help.isSelected()) updateResponsiveReadingLayout(
                        split, content, reading, readingScroll, newWidth.doubleValue());
            });

            VBox wrapper = new VBox(8, helpBar, split);
            wrapper.setMinSize(0, 0);
            wrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(split, Priority.ALWAYS);
            return wrapper;
        }

        private void updateResponsiveReadingLayout(BorderPane split, Node content, VBox reading,
                                                   ScrollPane readingScroll, double width) {
            boolean compact = width > 0 && width < 980;
            if (compact) {
                split.setRight(null);
                split.setBottom(readingScroll);
                BorderPane.setMargin(content, Insets.EMPTY);
                BorderPane.setMargin(readingScroll, new Insets(12, 0, 0, 0));

                readingScroll.setMinWidth(0);
                readingScroll.setPrefWidth(Region.USE_COMPUTED_SIZE);
                readingScroll.setMaxWidth(Double.MAX_VALUE);
                readingScroll.setMinHeight(210);
                readingScroll.setPrefHeight(280);
                readingScroll.setMaxHeight(320);

                reading.setMinWidth(0);
                reading.setPrefWidth(Region.USE_COMPUTED_SIZE);
                reading.setMaxWidth(Double.MAX_VALUE);
            } else {
                split.setBottom(null);
                split.setRight(readingScroll);
                BorderPane.setMargin(content, new Insets(0, 14, 0, 0));
                BorderPane.setMargin(readingScroll, Insets.EMPTY);

                double sideWidth = Math.max(320, Math.min(430, width * 0.255));
                readingScroll.setMinWidth(Math.min(300, sideWidth));
                readingScroll.setPrefWidth(sideWidth);
                readingScroll.setMaxWidth(sideWidth);
                readingScroll.setMinHeight(0);
                readingScroll.setPrefHeight(Region.USE_COMPUTED_SIZE);
                readingScroll.setMaxHeight(Double.MAX_VALUE);

                reading.setMinWidth(0);
                reading.setPrefWidth(sideWidth);
                reading.setMaxWidth(Double.MAX_VALUE);
            }
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
            card.setMinWidth(0);
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
            String italian = paragraph == null ? "" : paragraph;
            String english = I18n.english(italian);
            String[] itParts = splitReadingParagraph(italian);
            String[] enParts = splitReadingParagraph(english);

            Label sectionTitle = UiFactory.label("", "assistant-copy");
            I18n.setText(sectionTitle, itParts[0], enParts[0]);
            sectionTitle.setWrapText(true);
            sectionTitle.setMinHeight(Region.USE_PREF_SIZE);
            sectionTitle.setMaxWidth(Double.MAX_VALUE);
            sectionTitle.setStyle("-fx-text-fill: " + READING_COLORS[index % READING_COLORS.length]
                    + "; -fx-font-size: 13.5px; -fx-font-weight: bold;");

            Label sectionBody = UiFactory.wrappedLabel("", "assistant-copy");
            I18n.setText(sectionBody, itParts[1], enParts[1]);
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

        private String[] splitReadingParagraph(String paragraph) {
            String value = paragraph == null ? "" : paragraph;
            int separator = value.indexOf(" — ");
            if (separator < 0) return new String[]{value, ""};
            return new String[]{value.substring(0, separator).trim(), value.substring(separator + 3).trim()};
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
