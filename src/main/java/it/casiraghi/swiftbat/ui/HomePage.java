package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.BlackHoleHeroPane;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class HomePage extends ScrollPane {
    public HomePage(Runnable openExplorer,
                    Runnable openSky,
                    Runnable openPopulation,
                    Runnable openCompare,
                    Runnable openInfo,
                    ObservableValue<? extends String> catalogText,
                    ObservableValue<? extends String> sessionText,
                    ObservableValue<? extends String> connectionText) {
        getStyleClass().addAll("page-scroll", "home-page-scroll", "mockup-home-page");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setContent(buildContent(openExplorer, openSky, openPopulation, openCompare, openInfo,
                catalogText, sessionText, connectionText));
    }

    private Node buildContent(Runnable openExplorer,
                              Runnable openSky,
                              Runnable openPopulation,
                              Runnable openCompare,
                              Runnable openInfo,
                              ObservableValue<? extends String> catalogText,
                              ObservableValue<? extends String> sessionText,
                              ObservableValue<? extends String> connectionText) {
        VBox page = new VBox(14);
        page.getStyleClass().addAll("page-content", "home-content", "mockup-home-content");
        page.setPadding(new Insets(20, 26, 30, 26));

        HBox welcome = new HBox(18);
        welcome.setAlignment(Pos.CENTER_LEFT);
        VBox welcomeCopy = new VBox(3,
                gradientTitle("Benvenuto su ", "SwiftBAT Explorer"),
                bilingualWrapped(
                        "Esplora, analizza e interpreta i lampi di raggi gamma con i dati di Swift/BAT.",
                        "Explore, analyze and interpret gamma-ray bursts with Swift/BAT data.",
                        "mockup-welcome-subtitle"));
        HBox.setHgrow(welcomeCopy, Priority.ALWAYS);
        Label quote = bilingualWrapped(
                "“Dove l'Universo diventa estremo, inizia la scoperta.”",
                "“Where the Universe becomes extreme, discovery begins.”",
                "mockup-welcome-quote");
        quote.setMaxWidth(310);
        welcome.getChildren().addAll(welcomeCopy, quote);

        HBox metrics = new HBox(12);
        metrics.getChildren().addAll(
                metricCard("◇", "GRB nel catalogo", catalogText, "Eventi Swift/BAT", "cyan"),
                metricCard("✺", "Connessione", connectionText, "Accesso ai dati online", "purple"),
                metricCard("▱", "File in cache", sessionText, "Sessione e cache locale", "cyan"),
                staticMetricCard("◷", "Pipeline", "1 s", "Binning curve di luce", "purple"));
        for (Node node : metrics.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);

        HBox hero = new HBox(18);
        hero.getStyleClass().add("mockup-home-hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(24, 24, 22, 28));
        hero.setMinHeight(310);

        VBox heroCopy = new VBox(10);
        heroCopy.setAlignment(Pos.CENTER_LEFT);
        heroCopy.setMaxWidth(610);
        Label kicker = bilingualLabel(
                "AI CONFINI DELL'UNIVERSO PIÙ ESTREMO",
                "AT THE EDGE OF THE MOST EXTREME UNIVERSE",
                "mockup-hero-kicker");
        Label lineOne = bilingualLabel(
                "I lampi di raggi gamma",
                "Gamma-ray bursts",
                "mockup-hero-title");
        Label lineTwo = UiFactory.label("illuminano l'Universo estremo", "mockup-hero-title-secondary");
        Label subtitle = bilingualWrapped(
                "Esplora il catalogo Swift/BAT, visualizza gli eventi sulla mappa celeste e analizza curve di luce, spettroscopia e proprietà di popolazione senza uscire dall'app.",
                "Explore the Swift/BAT catalog, view events on the sky map and analyze light curves, spectroscopy and population properties without leaving the app.",
                "mockup-hero-body");
        subtitle.setMaxWidth(590);

        HBox actions = new HBox(10);
        Button explore = bilingualButton("⌕   Apri Esplora", "⌕   Open Explore", "primary-button");
        explore.getStyleClass().add("mockup-hero-action");
        explore.setOnAction(event -> openExplorer.run());
        Button sky = bilingualButton("⌾   Mappa celeste", "⌾   Sky map", "secondary-button");
        sky.getStyleClass().add("mockup-hero-action");
        sky.setOnAction(event -> openSky.run());
        Button analysis = bilingualButton("⌁   Nuova analisi", "⌁   New analysis", "secondary-button");
        analysis.getStyleClass().addAll("mockup-hero-action", "mockup-hero-action-magenta");
        analysis.setOnAction(event -> openPopulation.run());
        actions.getChildren().addAll(explore, sky, analysis);

        heroCopy.getChildren().addAll(kicker, lineOne, lineTwo, subtitle, actions);
        HBox.setHgrow(heroCopy, Priority.ALWAYS);

        BlackHoleHeroPane graphic = new BlackHoleHeroPane();
        graphic.getStyleClass().add("mockup-hero-graphic");
        graphic.setMinWidth(390);
        graphic.setPrefWidth(590);
        HBox.setHgrow(graphic, Priority.ALWAYS);
        hero.getChildren().addAll(heroCopy, graphic);

        HBox lower = new HBox(12);
        lower.getChildren().addAll(
                infoPanel("◷", "Sessione", new String[]{
                        "Eventi già aperti restano in RAM",
                        "I prodotti locali vengono riutilizzati",
                        "Il confronto usa i GRB della sessione"}, null),
                infoPanel("▱", "Dataset Swift/BAT", new String[]{
                        "Catalogo scientifico online",
                        "Curve a binning di 1 secondo",
                        "ASCII, FITS e metadati integrati"}, openExplorer),
                shortcutPanel(openExplorer, openSky, openPopulation, openCompare),
                infoPanel("◎", "Strumenti", new String[]{
                        "Mollweide 2D e sfera 3D",
                        "Analisi di popolazione",
                        "Confronto e spettroscopia"}, openInfo));
        for (Node node : lower.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);

        page.getChildren().addAll(welcome, metrics, hero, lower);
        return page;
    }

    private HBox gradientTitle(String prefix, String accent) {
        HBox title = new HBox(7,
                UiFactory.label(prefix, "mockup-welcome-title"),
                UiFactory.label(accent, "mockup-welcome-title-accent"));
        title.setAlignment(Pos.CENTER_LEFT);
        return title;
    }

    private VBox metricCard(String glyph, String title,
                            ObservableValue<? extends String> value,
                            String detail, String accent) {
        VBox card = metricShell(glyph, title, detail, accent);
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("mockup-metric-value");
        valueLabel.textProperty().bind(value);
        card.getChildren().add(1, valueLabel);
        return card;
    }

    private VBox staticMetricCard(String glyph, String title, String value, String detail, String accent) {
        VBox card = metricShell(glyph, title, detail, accent);
        card.getChildren().add(1, UiFactory.label(value, "mockup-metric-value"));
        return card;
    }

    private VBox metricShell(String glyph, String title, String detail, String accent) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("mockup-metric-card", "mockup-accent-" + accent);
        card.setPadding(new Insets(14, 16, 13, 16));
        card.setMinWidth(180);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox head = new HBox(9,
                UiFactory.label(glyph, "mockup-metric-icon"),
                UiFactory.label(title, "mockup-metric-title"));
        head.setAlignment(Pos.CENTER_LEFT);
        Label detailLabel = UiFactory.label(detail, "mockup-metric-detail");
        card.getChildren().addAll(head, detailLabel);
        return card;
    }

    private VBox infoPanel(String glyph, String title, String[] lines, Runnable action) {
        VBox panel = new VBox(9);
        panel.getStyleClass().add("mockup-lower-panel");
        panel.setPadding(new Insets(15));
        panel.setMaxWidth(Double.MAX_VALUE);
        HBox head = new HBox(8,
                UiFactory.label(glyph, "mockup-lower-icon"),
                UiFactory.label(title, "mockup-lower-title"));
        head.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().add(head);
        for (String line : lines) {
            panel.getChildren().add(UiFactory.wrappedLabel("›  " + line, "mockup-lower-line"));
        }
        if (action != null) {
            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);
            Button button = bilingualButton("Apri  →", "Open  →", "mockup-inline-link");
            button.setOnAction(event -> action.run());
            panel.getChildren().addAll(spacer, button);
            panel.setCursor(javafx.scene.Cursor.HAND);
            panel.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getTarget() != button) action.run();
            });
        }
        return panel;
    }

    private VBox shortcutPanel(Runnable openExplorer, Runnable openSky,
                               Runnable openPopulation, Runnable openCompare) {
        VBox panel = new VBox(5);
        panel.getStyleClass().add("mockup-lower-panel");
        panel.setPadding(new Insets(15));
        panel.setMaxWidth(Double.MAX_VALUE);
        HBox head = new HBox(8,
                UiFactory.label("⚡", "mockup-lower-icon"),
                UiFactory.label("Scorciatoie", "mockup-lower-title"));
        panel.getChildren().addAll(head,
                shortcut("⌕", "Cerca un GRB", openExplorer),
                shortcut("⌾", "Apri mappa celeste", openSky),
                shortcut("⌁", "Analisi di popolazione", openPopulation),
                shortcut("⇄", "Confronta eventi", openCompare));
        return panel;
    }

    private Button shortcut(String glyph, String text, Runnable action) {
        Button button = UiFactory.button(glyph + "   " + text + "    ›", "mockup-shortcut-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> action.run());
        return button;
    }

    private Label bilingualLabel(String italian, String english, String styleClass) {
        Label label = UiFactory.label("", styleClass);
        I18n.setText(label, italian, english);
        return label;
    }

    private Label bilingualWrapped(String italian, String english, String styleClass) {
        Label label = UiFactory.wrappedLabel("", styleClass);
        I18n.setText(label, italian, english);
        return label;
    }

    private Button bilingualButton(String italian, String english, String styleClass) {
        Button button = UiFactory.button("", styleClass);
        I18n.setText(button, italian, english);
        return button;
    }
}
