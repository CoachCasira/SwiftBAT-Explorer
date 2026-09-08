package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.BlackHoleHeroPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class HomePage extends ScrollPane {
    private final Label catalogValue = UiFactory.label("…", "home-stat-value");
    private final Label spectralValue = UiFactory.label("…", "home-stat-value");
    private final Label cacheValue = UiFactory.label("0", "home-stat-value");

    public HomePage(Runnable openExplorer, Runnable openSky, Runnable openCompare, Runnable openInfo) {
        getStyleClass().addAll("page-scroll", "home-page-scroll");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setContent(buildContent(openExplorer, openSky, openCompare, openInfo));
    }

    public void updateCatalogSummary(int count, boolean fallback) {
        catalogValue.setText(String.valueOf(Math.max(0, count)));
        catalogValue.setAccessibleText((fallback ? "Catalogo di emergenza: " : "Catalogo online: ")
                + Math.max(0, count) + " GRB");
    }

    public void updateSpectralSummary(int count) {
        spectralValue.setText(String.valueOf(Math.max(0, count)));
        spectralValue.setAccessibleText(Math.max(0, count) + " fit spettroscopici disponibili");
    }

    public void updateCacheSummary(int ramCount, int localCount) {
        cacheValue.setText(Math.max(0, ramCount) + " / " + Math.max(0, localCount));
        cacheValue.setAccessibleText(Math.max(0, ramCount) + " eventi in RAM e "
                + Math.max(0, localCount) + " nella cache locale");
    }

    private Node buildContent(Runnable openExplorer, Runnable openSky, Runnable openCompare, Runnable openInfo) {
        VBox page = new VBox(20);
        page.getStyleClass().addAll("page-content", "home-content");
        page.setPadding(new Insets(26, 30, 42, 30));

        HBox welcome = new HBox(16);
        welcome.setAlignment(Pos.CENTER_LEFT);
        VBox welcomeCopy = new VBox(4,
                UiFactory.label("Bentornato in SwiftBAT", "home-welcome-title"),
                UiFactory.wrappedLabel(
                        "Esplora il catalogo, confronta i burst e leggi i prodotti spettroscopici ufficiali.",
                        "home-welcome-subtitle"));
        Region welcomeSpacer = new Region();
        HBox.setHgrow(welcomeSpacer, Priority.ALWAYS);
        HBox availability = new HBox(8,
                microPill("● DATI LIVE"),
                microPill("NASA / GSFC"));
        welcome.getChildren().addAll(welcomeCopy, welcomeSpacer, availability);

        HBox stats = new HBox(12,
                statCard("CATALOGO GRB", catalogValue, "eventi indicizzati", "catalog-stat"),
                statCard("FIT SPETTRALI", spectralValue, "modelli ufficiali BAT", "spectral-stat"),
                statCard("COPERTURA", UiFactory.label("T90 · z", "home-stat-value"),
                        "durata, cielo e redshift", "coverage-stat"),
                statCard("CACHE", cacheValue, "RAM / locale", "cache-stat"));
        for (Node node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        HBox hero = new HBox(28);
        hero.getStyleClass().add("event-horizon-hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(32, 34, 32, 38));
        hero.setMinHeight(390);

        VBox copy = new VBox(16);
        copy.setAlignment(Pos.CENTER_LEFT);
        copy.setMaxWidth(690);
        Label kicker = UiFactory.label("EVENT HORIZON  ·  SWIFT / BAT", "home-kicker");
        Label title = UiFactory.wrappedLabel("Dal catalogo\nal segnale.", "home-title");
        Label subtitle = UiFactory.wrappedLabel(
                "Curve di luce, coordinate celesti, analisi di popolazione e spettroscopia descrittiva in un unico ambiente scientifico.",
                "home-subtitle");
        subtitle.setMaxWidth(620);

        HBox actions = new HBox(11);
        Button explore = UiFactory.button("Esplora i GRB  →", "primary-button");
        explore.getStyleClass().add("home-primary-action");
        explore.setOnAction(event -> openExplorer.run());
        Button sky = UiFactory.button("Apri la mappa celeste", "secondary-button");
        sky.setOnAction(event -> openSky.run());
        Button info = UiFactory.button("Come si usa", "ghost-button");
        info.setOnAction(event -> openInfo.run());
        actions.getChildren().addAll(explore, sky, info);

        HBox trust = new HBox(12,
                microPill("CURVE DAT"),
                microPill("PRODOTTI FITS"),
                microPill("PL · CPL"));
        copy.getChildren().addAll(kicker, title, subtitle, actions, trust);
        HBox.setHgrow(copy, Priority.ALWAYS);

        BlackHoleHeroPane graphic = new BlackHoleHeroPane();
        graphic.setMinWidth(300);
        graphic.setPrefWidth(470);
        HBox.setHgrow(graphic, Priority.ALWAYS);
        hero.getChildren().addAll(copy, graphic);

        HBox quickActions = new HBox(14);
        quickActions.getChildren().addAll(
                actionCard("✦", "Esplora", "Cerca un evento e apri curve, dati e metadati.", "Apri catalogo", openExplorer),
                actionCard("◎", "Mappa celeste", "Guarda i GRB sulla Mollweide o sulla sfera 3D.", "Esplora il cielo", openSky),
                actionCard("⇄", "Confronta", "Sovrapponi due eventi già aperti nella sessione.", "Confronta eventi", openCompare));
        for (Node node : quickActions.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        HBox lower = new HBox(14);
        VBox workflow = new VBox(14);
        workflow.getStyleClass().add("simple-panel");
        workflow.setPadding(new Insets(22));
        workflow.getChildren().addAll(
                UiFactory.label("Tre passaggi, niente file manuali", "section-title-compact"),
                simpleStep("01", "Scegli un GRB", "Cerca nome o Trigger ID."),
                simpleStep("02", "Aprilo", "L'app recupera e interpreta i prodotti Swift/BAT online."),
                simpleStep("03", "Esplora", "Passa da curva, 3D, tabelle, metadati e mappa celeste."));
        HBox.setHgrow(workflow, Priority.ALWAYS);

        VBox info = new VBox(14);
        info.getStyleClass().add("simple-panel");
        info.setPadding(new Insets(22));
        Label infoTitle = UiFactory.label("Serve una spiegazione?", "section-title-compact");
        Label infoText = UiFactory.wrappedLabel(
                "Le schermate mantengono il dato originale e affiancano spiegazioni brevi per trigger, rate, errori, FRACEXP, FITS, RA, DEC e T90.",
                "simple-panel-text");
        Button infoButton = UiFactory.button("Apri info e guida", "ghost-button");
        infoButton.setOnAction(event -> openInfo.run());
        info.getChildren().addAll(infoTitle, infoText, infoButton);
        HBox.setHgrow(info, Priority.ALWAYS);
        lower.getChildren().addAll(workflow, info);

        page.getChildren().addAll(welcome, stats, hero, quickActions, lower);
        return page;
    }

    private VBox statCard(String title, Label value, String detail, String accentClass) {
        VBox card = new VBox(6,
                UiFactory.label(title, "home-stat-label"),
                value,
                UiFactory.label(detail, "home-stat-detail"));
        card.getStyleClass().addAll("home-stat-card", accentClass);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(0);
        return card;
    }

    private VBox actionCard(String glyph, String title, String text, String action, Runnable runnable) {
        VBox card = new VBox(10);
        card.getStyleClass().add("home-action-card");
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);
        Label icon = UiFactory.label(glyph, "home-action-icon");
        Label titleLabel = UiFactory.label(title, "home-action-title");
        Label textLabel = UiFactory.wrappedLabel(text, "home-action-text");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button button = UiFactory.button(action + "  →", "home-link-button");
        button.setOnAction(event -> runnable.run());
        card.getChildren().addAll(icon, titleLabel, textLabel, spacer, button);
        return card;
    }

    private HBox simpleStep(String number, String title, String detail) {
        HBox row = new HBox(13);
        row.setAlignment(Pos.CENTER_LEFT);
        Label numberLabel = UiFactory.label(number, "simple-step-number");
        VBox copy = new VBox(3,
                UiFactory.label(title, "simple-step-title"),
                UiFactory.wrappedLabel(detail, "simple-step-text"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        row.getChildren().addAll(numberLabel, copy);
        return row;
    }

    private Label microPill(String text) {
        return UiFactory.label(text, "home-micro-pill");
    }
}
