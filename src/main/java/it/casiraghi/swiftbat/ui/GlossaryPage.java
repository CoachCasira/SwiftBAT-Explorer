package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.FieldDefinition;
import it.casiraghi.swiftbat.service.OnlineGrbService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Locale;

public final class GlossaryPage extends BorderPane {
    private final ObservableList<FieldDefinition> definitions = FXCollections.observableArrayList(OnlineGrbService.dictionary());
    private final FilteredList<FieldDefinition> filtered = new FilteredList<>(definitions, ignored -> true);
    private final ListView<FieldDefinition> list = new ListView<>(filtered);
    private final VBox detailHost = new VBox();

    public GlossaryPage() {
        getStyleClass().add("page-root");
        setPadding(new Insets(28, 34, 34, 34));
        setTop(buildHeader());
        setCenter(buildBody());
        BorderPane.setMargin(getCenter(), new Insets(22, 0, 0, 0));
        if (!definitions.isEmpty()) {
            list.getSelectionModel().selectFirst();
        }
    }

    private Node buildHeader() {
        VBox header = new VBox(7);
        header.getChildren().addAll(
                UiFactory.label("Dizionario dei dati", "page-title"),
                UiFactory.wrappedLabel(
                        "Ogni voce è spiegata prima in parole semplici e poi in modo tecnico. Qui puoi cercare TIME, RATE, FRACEXP, TRIGTIME, OBS_ID e gli altri campi presenti nelle tabelle e nei FITS.",
                        "page-subtitle"));
        return header;
    }

    private Node buildBody() {
        VBox left = new VBox(12);
        left.getStyleClass().add("dictionary-sidebar");
        left.setPadding(new Insets(18));
        TextField search = new TextField();
        search.setPromptText("Cerca un campo o un concetto…");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(item -> query.isBlank() || item.searchableText().contains(query));
        });

        Label count = UiFactory.label("", "sidebar-caption");
        Runnable refreshCount = () -> I18n.setText(count,
                filtered.size() + " voci documentate", filtered.size() + " documented entries");
        filtered.addListener((javafx.collections.ListChangeListener<FieldDefinition>) change -> refreshCount.run());
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> refreshCount.run());
        refreshCount.run();

        list.getStyleClass().add("dictionary-list");
        list.setCellFactory(ignored -> new DictionaryCell());
        list.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showDefinition(newValue);
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        left.getChildren().addAll(search, count, list);

        ScrollPane detailScroll = new ScrollPane(detailHost);
        detailScroll.getStyleClass().add("detail-scroll");
        detailScroll.setFitToWidth(true);
        detailHost.setPadding(new Insets(26));
        detailHost.setSpacing(18);

        SplitPane split = new SplitPane(left, detailScroll);
        split.getStyleClass().add("clean-split");
        split.setDividerPositions(0.31);
        return split;
    }

    private void showDefinition(FieldDefinition definition) {
        detailHost.getChildren().clear();
        HBox heading = new HBox(12);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(5,
                UiFactory.label(definition.field(), "definition-title"),
                UiFactory.label(I18n.t(definition.category()) + " · " + I18n.t(definition.source()), "definition-kicker"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label unit = UiFactory.label(definition.unit().isBlank() ? "senza unità" : definition.unit(), "unit-badge");
        heading.getChildren().addAll(titleBox, spacer, unit);

        VBox simple = explanationCard("In parole semplici", "◎", definition.simpleExplanation(), "explanation-simple");
        VBox technical = explanationCard("Descrizione tecnica", "⌁", definition.technicalExplanation(), "explanation-technical");
        VBox importance = explanationCard("Perché è utile", "↗", definition.whyItMatters(), "explanation-important");
        VBox caution = explanationCard("Attenzione a non confonderlo", "!", definition.caution(), "explanation-caution");

        detailHost.getChildren().addAll(heading, simple, technical, importance, caution);
    }

    private VBox explanationCard(String title, String icon, String text, String styleClass) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("explanation-card", styleClass);
        HBox titleRow = new HBox(9);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = UiFactory.label(icon, "explanation-icon");
        Label titleLabel = UiFactory.label(title, "explanation-title");
        titleRow.getChildren().addAll(iconLabel, titleLabel);
        card.getChildren().addAll(titleRow, UiFactory.wrappedLabel(text, "explanation-text"));
        return card;
    }

    private static final class DictionaryCell extends ListCell<FieldDefinition> {
        @Override
        protected void updateItem(FieldDefinition item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            VBox box = new VBox(4);
            Label field = UiFactory.label(item.field(), "dictionary-field");
            Label source = UiFactory.label(I18n.t(item.category()) + " · " + I18n.t(item.source()), "dictionary-source");
            Label simple = UiFactory.wrappedLabel(item.simpleExplanation(), "dictionary-preview");
            box.getChildren().addAll(field, source, simple);
            setGraphic(box);
        }
    }
}
