package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.FieldDefinition;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.MetadataItem;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

/**
 * Metadata tab rebuilt as a self-contained workspace.
 *
 * <p>No legacy ChoiceBox/ComboBox is used for the metadata-field selector.
 * The selector is a real editable TextField + A/Z button + in-layout ListView,
 * so macOS cannot fall back to the old native popup.</p>
 */
public final class MetadataWorkspaceV3 {
    private MetadataWorkspaceV3() { }

    public static Node build(GrbData data) {
        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("metadata-workspace-v3");
        pane.setPadding(new Insets(18));

        TextField globalSearch = new TextField();
        globalSearch.setPromptText(I18n.dynamic(
                "Cerca keyword, valore, HDU o commento…",
                "Search keyword, value, HDU or comment…"));
        globalSearch.getStyleClass().add("search-field");
        pane.setTop(globalSearch);
        BorderPane.setMargin(globalSearch, new Insets(0, 0, 12, 0));

        TableView<MetadataItem> table = new TableView<>();
        table.getStyleClass().addAll("data-table", "metadata-table-v3");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinWidth(0);
        table.setMaxWidth(Double.MAX_VALUE);

        FlowPane hiddenColumns = TablePreferences.install(table, "explorer.metadata.v3." + data.grbName());

        TableColumn<MetadataItem, String> hdu = column(
                I18n.dynamic("HDU", "HDU"), MetadataItem::hduName, 92, 78);
        TableColumn<MetadataItem, String> keyword = column(
                I18n.dynamic("Keyword", "Keyword"), MetadataItem::keyword, 150, 118);
        TableColumn<MetadataItem, String> value = column(
                I18n.dynamic("Valore", "Value"), MetadataItem::value, 245, 170);
        TableColumn<MetadataItem, String> comment = column(
                I18n.dynamic("Commento originale", "Original comment"), MetadataItem::comment, 420, 245);
        table.getColumns().setAll(List.of(hdu, keyword, value, comment));

        FilteredList<MetadataItem> filtered = new FilteredList<>(
                FXCollections.observableArrayList(data.metadata()), ignored -> true);
        table.setItems(filtered);
        globalSearch.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(item -> query.isBlank()
                    || (safe(item.hduName()) + " " + safe(item.keyword()) + " "
                    + safe(item.value()) + " " + safe(item.comment()))
                    .toLowerCase(Locale.ROOT).contains(query));
        });

        VBox explanation = new VBox(12);
        explanation.setPadding(new Insets(16));
        explanation.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        showIntro(explanation);

        List<String> keywords = data.metadata().stream()
                .map(MetadataItem::keyword)
                .filter(valueText -> valueText != null && !valueText.isBlank())
                .distinct()
                .toList();
        String initialKeyword = keywords.isEmpty() ? null : keywords.get(0);

        final MetadataFieldSelector[] selectorRef = new MetadataFieldSelector[1];
        MetadataFieldSelector selector = new MetadataFieldSelector(
                keywords,
                initialKeyword,
                selectedField -> selectKeyword(data, table, globalSearch, explanation, selectedField));
        selectorRef[0] = selector;

        VBox side = new VBox(10,
                UiFactory.label("Campo metadata", "filter-label"),
                selector,
                explanation);
        side.getStyleClass().add("metadata-side-v3");
        side.setPadding(new Insets(4, 0, 4, 10));
        side.setMinWidth(286);
        side.setPrefWidth(310);
        side.setMaxWidth(330);

        ScrollPane sideScroll = new ScrollPane(side);
        sideScroll.getStyleClass().addAll("detail-scroll", "side-detail-scroll", "metadata-side-scroll-v3");
        sideScroll.setFitToWidth(true);
        sideScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sideScroll.setMinWidth(286);
        sideScroll.setPrefWidth(310);
        sideScroll.setMaxWidth(330);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, item) -> {
            if (item == null) return;
            selectorRef[0].setSelectedValue(item.keyword(), false);
            showExplanation(explanation, item, findDefinition(data, item.keyword()));
        });

        ToggleButton explanationToggle = new ToggleButton(I18n.t("Mostra spiegazione"));
        explanationToggle.getStyleClass().addAll("ghost-button", "help-toggle");
        HBox tools = new HBox(8, hiddenColumns, UiFactory.spacer(), explanationToggle);
        tools.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(hiddenColumns, Priority.ALWAYS);

        VBox tableArea = new VBox(7, tools, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        BorderPane content = new BorderPane(tableArea);
        content.setMinWidth(0);

        explanationToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            explanationToggle.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
            if (selected) {
                content.setRight(sideScroll);
                BorderPane.setMargin(sideScroll, new Insets(0, 0, 0, 10));
            } else {
                content.setRight(null);
            }
        });
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> {
            globalSearch.setPromptText(I18n.dynamic(
                    "Cerca keyword, valore, HDU o commento…",
                    "Search keyword, value, HDU or comment…"));
            explanationToggle.setText(I18n.t(explanationToggle.isSelected()
                    ? "Nascondi spiegazione" : "Mostra spiegazione"));
        });

        if (!data.metadata().isEmpty()) {
            MetadataItem first = data.metadata().get(0);
            table.getSelectionModel().select(first);
            selector.setSelectedValue(first.keyword(), false);
            showExplanation(explanation, first, findDefinition(data, first.keyword()));
        }

        pane.setCenter(content);
        FinalTableAlignmentFix.install(pane);
        ExplorerScrollbarFix.install(pane);
        return pane;
    }

    private static void selectKeyword(GrbData data,
                                      TableView<MetadataItem> table,
                                      TextField globalSearch,
                                      VBox explanation,
                                      String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        globalSearch.clear();
        data.metadata().stream()
                .filter(item -> keyword.equalsIgnoreCase(item.keyword()))
                .findFirst()
                .ifPresent(item -> {
                    table.getSelectionModel().select(item);
                    table.scrollTo(item);
                    showExplanation(explanation, item, findDefinition(data, item.keyword()));
                });
    }

    private static TableColumn<MetadataItem, String> column(
            String title,
            java.util.function.Function<MetadataItem, String> mapper,
            double prefWidth,
            double minWidth) {
        TableColumn<MetadataItem, String> column = new TableColumn<>(title);
        column.setMinWidth(minWidth);
        column.setPrefWidth(prefWidth);
        column.setCellValueFactory(value -> new ReadOnlyStringWrapper(safe(mapper.apply(value.getValue()))));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(value);
                setAlignment(Pos.CENTER_LEFT);
                TablePreferences.alignCell(this, value);
                setTooltip(value.length() > 24 ? UiFactory.quickTooltip(value) : null);
            }
        });
        return column;
    }

    private static FieldDefinition findDefinition(GrbData data, String keyword) {
        if (keyword == null) return null;
        FieldDefinition direct = data.definition(keyword);
        if (direct != null) return direct;
        if (keyword.equalsIgnoreCase("RA_OBJ") || keyword.equalsIgnoreCase("DEC_OBJ")) {
            return data.definition("RA_OBJ / DEC_OBJ");
        }
        return null;
    }

    private static void showIntro(VBox host) {
        host.getChildren().setAll(
                UiFactory.wrappedLabel("Come leggere i metadati", "definition-title"),
                UiFactory.wrappedLabel(
                        "Seleziona una riga. I metadati sono il registro tecnico del file FITS: descrivono provenienza, tempi, coordinate, struttura e passaggi di elaborazione.",
                        "explanation-text"));
    }

    private static void showExplanation(VBox host, MetadataItem item, FieldDefinition definition) {
        host.getChildren().clear();
        Label title = UiFactory.wrappedLabel(
                item.keyword() == null || item.keyword().isBlank()
                        ? I18n.dynamic("Voce FITS", "FITS field")
                        : item.keyword(),
                "definition-title");
        host.getChildren().addAll(
                title,
                UiFactory.label("HDU " + safe(item.hduIndex()) + " · " + safe(item.hduName()), "definition-kicker"),
                compactInfo(I18n.dynamic("Valore", "Value"), safe(item.value())),
                compactInfo(I18n.dynamic("Commento FITS", "FITS comment"), safe(item.comment())));
        if (definition != null) {
            host.getChildren().addAll(
                    miniExplanation(I18n.dynamic("In parole semplici", "In simple terms"), definition.simpleExplanation()),
                    miniExplanation(I18n.dynamic("Perché serve", "Why it matters"), definition.whyItMatters()),
                    miniExplanation(I18n.dynamic("Attenzione", "Caution"), definition.caution()));
        } else {
            host.getChildren().add(UiFactory.wrappedLabel(
                    "Questa keyword non è ancora inclusa nel dizionario didattico. Il commento originale del FITS rimane comunque visibile.",
                    "subtle-text"));
        }
    }

    private static HBox compactInfo(String key, String value) {
        HBox row = UiFactory.infoRow(key, value == null || value.isBlank() ? "n.d." : value);
        if (!row.getChildren().isEmpty() && row.getChildren().get(0) instanceof Label label) {
            label.setMinWidth(112);
            label.setPrefWidth(112);
            label.setMaxWidth(112);
        }
        return row;
    }

    private static VBox miniExplanation(String title, String text) {
        VBox box = new VBox(5,
                UiFactory.label(title, "mini-explanation-title"),
                UiFactory.wrappedLabel(text == null ? "" : text, "mini-explanation-text"));
        box.getStyleClass().add("mini-explanation");
        return box;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
