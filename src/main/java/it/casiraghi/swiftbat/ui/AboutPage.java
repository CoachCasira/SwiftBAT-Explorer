package it.casiraghi.swiftbat.ui;

import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class AboutPage extends ScrollPane {
    public AboutPage(HostServices hostServices, Runnable openGlossary) {
        getStyleClass().add("page-scroll");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setContent(buildContent(hostServices, openGlossary));
    }

    private Node buildContent(HostServices hostServices, Runnable openGlossary) {
        VBox page = new VBox(20);
        page.getStyleClass().add("page-content");
        page.setPadding(new Insets(30, 36, 44, 36));

        HBox hero = new HBox(24);
        hero.getStyleClass().add("info-hero");
        hero.setPadding(new Insets(28));
        hero.setAlignment(Pos.CENTER_LEFT);
        VBox copy = new VBox(8,
                UiFactory.label("Informazioni", "page-title"),
                UiFactory.wrappedLabel(
                        "SwiftBAT Explorer è l'applicazione sviluppata per la tesi di Matteo Casiraghi per consultare e comprendere i prodotti pubblici Swift/BAT dei Gamma-Ray Burst.",
                        "page-subtitle"),
                UiFactory.label("Versione 1.2.0 · Java 17 · dati online", "definition-kicker"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        Button guide = UiFactory.button("Apri guida ai dati  →", "primary-button");
        guide.setOnAction(event -> openGlossary.run());
        hero.getChildren().addAll(copy, guide);

        FlowPane cards = new FlowPane(14, 14);
        cards.getChildren().addAll(
                infoCard("Dati", "Catalogo ufficiale NASA/GSFC Swift/BAT e prodotti DAT/FITS a binning di 1 secondo."),
                infoCard("Curve", "Visualizzazione delle curve di luce totali e nelle quattro bande energetiche disponibili."),
                infoCard("Volta celeste", "Mollweide 2D e sfera 3D costruite con le coordinate RA/DEC pubblicate da Swift/BAT."),
                infoCard("Sessione", "Gli eventi aperti restano in memoria finché l'app è in esecuzione, senza modificare i file sorgente."));

        VBox scope = UiFactory.card("Scopo scientifico", null,
                UiFactory.wrappedLabel(
                        "L'app facilita consultazione, controllo e confronto descrittivo dei dati. Gli indicatori e la soglia T90 = 2 s mostrati nell'interfaccia non sostituiscono una classificazione astrofisica validata.",
                        "explanation-text"));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button catalog = UiFactory.button("Catalogo Swift/BAT  ↗", "secondary-button");
        catalog.setOnAction(event -> hostServices.showDocument("https://swift.gsfc.nasa.gov/results/batgrbcat/"));
        Button fits = UiFactory.button("FITS / nom-tam-fits  ↗", "ghost-button");
        fits.setOnAction(event -> hostServices.showDocument("https://nom-tam-fits.github.io/nom-tam-fits/"));
        Button redshift = UiFactory.button("Tabella redshift BAT  ↗", "ghost-button");
        redshift.setOnAction(event -> hostServices.showDocument(
                "https://swift.gsfc.nasa.gov/results/batgrbcat/summary_cflux/summary_general_info/GRBlist_redshift_BAT.txt"));
        actions.getChildren().addAll(catalog, redshift, fits);

        page.getChildren().addAll(hero, cards, scope, actions);
        return page;
    }

    private VBox infoCard(String title, String text) {
        VBox card = new VBox(8);
        card.getStyleClass().add("home-action-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(310);
        card.getChildren().addAll(
                UiFactory.label(title, "home-action-title"),
                UiFactory.wrappedLabel(text, "home-action-text"));
        return card;
    }
}
