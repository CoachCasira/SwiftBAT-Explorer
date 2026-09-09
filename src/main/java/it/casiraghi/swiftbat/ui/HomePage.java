package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.BlackHoleHeroPane;
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
    public HomePage(Runnable openExplorer, Runnable openSky, Runnable openCompare, Runnable openInfo) {
        getStyleClass().addAll("page-scroll", "home-page-scroll");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setContent(buildContent(openExplorer, openSky, openCompare, openInfo));
    }

    private Node buildContent(Runnable openExplorer, Runnable openSky, Runnable openCompare, Runnable openInfo) {
        VBox page = new VBox(24);
        page.getStyleClass().addAll("page-content", "home-content");
        page.setPadding(new Insets(28, 34, 42, 34));

        HBox hero = new HBox(28);
        hero.getStyleClass().add("event-horizon-hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(34, 34, 34, 38));
        hero.setMinHeight(430);

        VBox copy = new VBox(16);
        copy.setAlignment(Pos.CENTER_LEFT);
        copy.setMaxWidth(690);
        Label kicker = UiFactory.label("SWIFT / BAT  ·  LIVE DATA", "home-kicker");
        Label title = UiFactory.wrappedLabel("SwiftBAT\nExplorer", "home-title");
        Label subtitle = UiFactory.wrappedLabel(
                "Esplora i Gamma-Ray Burst dal catalogo al cielo. Curve di luce, dati FITS e coordinate celesti in un'unica app.",
                "home-subtitle");
        subtitle.setMaxWidth(620);

        HBox actions = new HBox(11);
        Button explore = UiFactory.button("Esplora i GRB  →", "primary-button");
        explore.getStyleClass().add("home-primary-action");
        explore.setOnAction(event -> openExplorer.run());
        Button sky = UiFactory.button("Apri la mappa celeste", "secondary-button");
        sky.setOnAction(event -> openSky.run());
        actions.getChildren().addAll(explore, sky);

        HBox trust = new HBox(12,
                microPill("● Online"),
                microPill("1 s binning"),
                microPill("DAT + FITS"));
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
        for (Node node : quickActions.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);

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

        page.getChildren().addAll(hero, quickActions, lower);
        return page;
    }

    private VBox actionCard(String glyph, String title, String text, String action, Runnable runnable) {
        VBox card = new VBox(10);
        card.getStyleClass().add("home-action-card");
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) runnable.run();
        });
        Label icon = UiFactory.label(glyph, "home-action-icon");
        Label titleLabel = UiFactory.label(title, "home-action-title");
        Label textLabel = UiFactory.wrappedLabel(text, "home-action-text");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        HBox actionHint = new HBox(5,
                UiFactory.label(action, "home-link-button"),
                UiFactory.label("→", "home-link-button"));
        actionHint.setMouseTransparent(true);
        card.getChildren().addAll(icon, titleLabel, textLabel, spacer, actionHint);
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
