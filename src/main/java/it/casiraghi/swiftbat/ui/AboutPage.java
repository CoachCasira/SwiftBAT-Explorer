package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.BrandLogoAsset;
import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
        cards.add(infoCard("Dati", "Catalogo ufficiale NASA/GSFC Swift/BAT e prodotti DAT/FITS a binning di 1 secondo. I valori restano riconducibili alle sorgenti pubbliche usate dall'app."), 0, 0);
        cards.add(infoCard("Curve", "Visualizzazione delle curve di luce totali e nelle quattro bande energetiche, con finestre temporali, zoom e viste dedicate per leggere meglio la struttura del burst."), 1, 0);
        cards.add(infoCard("Volta celeste", "Mollweide 2D e sfera 3D costruite con le coordinate RA/DEC pubblicate da Swift/BAT. Le due viste mostrano lo stesso campione con rappresentazioni differenti."), 0, 1);
        cards.add(infoCard("Sessione", "Gli eventi aperti restano in memoria finché l'app è in esecuzione. La cache locale evita download ripetuti senza modificare i prodotti scientifici sorgente."), 1, 1);

        VBox scope = UiFactory.card("Scopo scientifico", null,
                UiFactory.wrappedLabel(
                        "L'app facilita consultazione, controllo e confronto descrittivo dei dati. Gli indicatori e la soglia T90 = 2 s mostrati nell'interfaccia non sostituiscono una classificazione astrofisica validata.",
                        "explanation-text"));
        VBox reading = UiFactory.card("Lettura dei risultati", null,
                UiFactory.wrappedLabel(
                        "Grafici, mappe, filtri e assistenti di lettura servono a mettere in evidenza pattern e differenze nel campione. Le viste 2D e 3D sono strumenti esplorativi e mantengono sempre separata la rappresentazione grafica dall'interpretazione fisica.",
                        "explanation-text"));
        scope.setMaxWidth(Double.MAX_VALUE);
        reading.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(scope, Priority.ALWAYS);
        HBox.setHgrow(reading, Priority.ALWAYS);
        HBox scienceRow = new HBox(14, scope, reading);

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

        page.getChildren().addAll(hero, cards, scienceRow, actions);
        return page;
    }

    private VBox infoCard(String title, String text) {
        VBox card = new VBox(8);
        card.getStyleClass().add("info-static-card");
        card.setPadding(new Insets(21));
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().addAll(
                UiFactory.label(title, "home-action-title"),
                UiFactory.wrappedLabel(text, "home-action-text"));
        return card;
    }
}
