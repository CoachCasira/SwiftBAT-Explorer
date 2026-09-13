package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.BrandLogoAsset;
import it.casiraghi.swiftbat.ui.components.UniBgMarkPane;
import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
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
        Node brandLogo = BrandLogoAsset.view(82);
        VBox copy = new VBox(8,
                UiFactory.label("Informazioni", "page-title"),
                UiFactory.wrappedLabel(
                        "SwiftBAT Explorer è l'applicazione sviluppata per la tesi di Matteo Casiraghi per consultare e comprendere i prodotti pubblici Swift/BAT dei Gamma-Ray Burst.",
                        "page-subtitle"),
                UiFactory.label("v1.3.0 · Java 17 · online", "definition-kicker"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        Button guide = UiFactory.button("Apri guida ai dati  →", "primary-button");
        guide.setOnAction(event -> openGlossary.run());
        hero.getChildren().addAll(brandLogo, copy, guide);

        GridPane cards = new GridPane();
        cards.setHgap(14);
        cards.setVgap(14);
        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(50);
        firstColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(50);
        secondColumn.setHgrow(Priority.ALWAYS);
        cards.getColumnConstraints().addAll(firstColumn, secondColumn);

        VBox data = infoCard("Dati", "Catalogo ufficiale NASA/GSFC Swift/BAT e prodotti DAT/FITS a binning di 1 secondo. I valori restano riconducibili alle sorgenti pubbliche usate dall'app.");
        VBox curves = infoCard("Curve", "Visualizzazione delle curve di luce totali e nelle quattro bande energetiche, con finestre temporali, zoom e viste dedicate per leggere meglio la struttura del burst.");
        VBox sky = infoCard("Volta celeste", "Mollweide 2D e sfera 3D costruite con le coordinate RA/DEC pubblicate da Swift/BAT. Le due viste mostrano lo stesso campione con rappresentazioni differenti.");
        VBox session = infoCard("Sessione", "Gli eventi aperti restano in memoria finché l'app è in esecuzione. La cache locale evita download ripetuti senza modificare i prodotti scientifici sorgente.");
        VBox scope = infoCard("Scopo scientifico",
                "L'app facilita consultazione, controllo e confronto descrittivo dei dati. Gli indicatori e la soglia T90 = 2 s mostrati nell'interfaccia non sostituiscono una classificazione astrofisica validata.");
        VBox reading = infoCard("Lettura dei risultati",
                "Grafici, mappe, filtri e assistenti di lettura servono a mettere in evidenza pattern e differenze nel campione. Le viste 2D e 3D sono strumenti esplorativi e mantengono sempre separata la rappresentazione grafica dall'interpretazione fisica.");

        cards.add(data, 0, 0);
        cards.add(curves, 1, 0);
        cards.add(sky, 0, 1);
        cards.add(session, 1, 1);
        cards.add(scope, 0, 2);
        cards.add(reading, 1, 2);

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

        Label creditText = UiFactory.label("", "info-credit-text");
        I18n.setText(creditText,
                "Realizzato da Matteo Casiraghi · UniBG",
                "Realized by Matteo Casiraghi · UniBG");
        creditText.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");

        Label creditSubtitle = UiFactory.label("SwiftBAT Explorer · thesis project", "info-credit-subtitle");
        creditSubtitle.setStyle("-fx-font-size: 9px;");

        VBox creditCopy = new VBox(1, creditText, creditSubtitle);
        HBox credit = new HBox(7, new UniBgMarkPane(27), creditCopy);
        credit.getStyleClass().add("info-credit-card");
        credit.setAlignment(Pos.CENTER_LEFT);
        credit.setPadding(new Insets(6, 10, 6, 10));

        HBox footer = new HBox(18, actions, UiFactory.spacer(), credit);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actions, Priority.NEVER);

        page.getChildren().addAll(hero, cards, footer);
        return page;
    }

    private VBox infoCard(String title, String text) {
        VBox card = new VBox(8);
        card.getStyleClass().add("info-static-card");
        card.setPadding(new Insets(21));
        card.setMinWidth(0);
        card.setMinHeight(124);
        card.setPrefHeight(124);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);
        GridPane.setVgrow(card, Priority.ALWAYS);
        card.getChildren().addAll(
                UiFactory.label(title, "home-action-title"),
                UiFactory.wrappedLabel(text, "home-action-text"));
        return card;
    }
}
