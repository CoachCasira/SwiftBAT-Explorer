package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Restores the searchable metadata-field selector that was previously hidden
 * behind a fragile text-based UiRefinements lookup.
 *
 * <p>The installer is intentionally scoped to the Metadata tab. It therefore
 * works in both Italian and English and cannot accidentally replace ChoiceBoxes
 * belonging to Data, Spectroscopy or the 2D controls.</p>
 */
public final class MetadataSelectorRestoreFix {
    private static final String DONE = MetadataSelectorRestoreFix.class.getName() + ".done";
    // Mark the legacy UiRefinements enhancer as done too, otherwise a later
    // language/layout pulse could try to install a second toolbar.
    private static final String LEGACY_DONE = UiRefinements.class.getName() + ".metadataDone";

    private MetadataSelectorRestoreFix() { }

    public static void install(Node root) {
        if (root == null) return;
        for (TabPane tabs : findAll(root, TabPane.class)) {
            for (Tab tab : tabs.getTabs()) {
                String title = safe(tab.getText()).toLowerCase(Locale.ROOT);
                if (!title.contains("metadata") && !title.contains("metadat")) continue;
                installInMetadataContent(tab.getContent());
            }
        }
    }

    private static void installInMetadataContent(Node content) {
        if (content == null) return;
        for (VBox box : findAll(content, VBox.class)) {
            if (Boolean.TRUE.equals(box.getProperties().get(DONE))) continue;
            ChoiceBox<String> original = directChoiceBox(box);
            if (original == null || original.getItems().isEmpty()) continue;

            // The metadata side panel has one direct ChoiceBox and at least one
            // additional explanation node. Restricting to direct children keeps
            // this deterministic even if the tab grows new nested controls.
            if (box.getChildren().size() < 3) continue;
            installSelector(box, original);
            return;
        }
    }

    @SuppressWarnings("unchecked")
    private static ChoiceBox<String> directChoiceBox(VBox box) {
        for (Node child : box.getChildren()) {
            if (child instanceof ChoiceBox<?> raw) return (ChoiceBox<String>) raw;
        }
        return null;
    }

    private static void installSelector(VBox box, ChoiceBox<String> original) {
        box.getProperties().put(DONE, Boolean.TRUE);
        box.getProperties().put(LEGACY_DONE, Boolean.TRUE);

        List<String> master = original.getItems().stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        if (master.isEmpty()) return;

        int originalIndex = box.getChildren().indexOf(original);
        original.setVisible(false);
        original.setManaged(false);

        SearchController controller = new SearchController(original, master);
        HBox toolbar = controller.build();
        box.getChildren().add(Math.max(0, originalIndex + 1), toolbar);

        // One pulse later the ChoiceBox skin may finish its first layout on macOS;
        // keep the native control hidden and the replacement toolbar visible.
        Platform.runLater(() -> {
            original.setVisible(false);
            original.setManaged(false);
            toolbar.setVisible(true);
            toolbar.setManaged(true);
            toolbar.requestLayout();
        });
    }

    private static final class SearchController {
        private final ChoiceBox<String> original;
        private final List<String> master;
        private final ComboBox<String> search = new ComboBox<>();
        private final Button sort = new Button("A → Z");
        private boolean descending;
        private boolean syncing;

        SearchController(ChoiceBox<String> original, List<String> master) {
            this.original = original;
            this.master = List.copyOf(master);
        }

        HBox build() {
            search.setEditable(true);
            search.setVisibleRowCount(12);
            search.setMinWidth(190);
            search.setPrefWidth(270);
            search.setMaxWidth(Double.MAX_VALUE);
            search.getStyleClass().addAll("choice-box-modern", "metadata-search-combo");
            search.setPromptText(I18n.dynamic("Cerca un campo metadata…", "Search a metadata field…"));
            search.setCellFactory(list -> groupedCell());
            search.setButtonCell(simpleCell());
            HBox.setHgrow(search, Priority.ALWAYS);

            sort.getStyleClass().addAll("ghost-button", "metadata-sort-button");
            sort.setFocusTraversable(false);
            sort.setMinWidth(72);
            sort.setPrefWidth(78);
            sort.setMaxWidth(84);
            sort.setOnAction(event -> {
                descending = !descending;
                sort.setText(descending ? "Z → A" : "A → Z");
                rebuild(search.getEditor().getText());
            });
            sort.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                    "Inverti l'ordine alfabetico", "Reverse alphabetical order")));

            search.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                if (!syncing) rebuild(newValue);
            });
            search.getEditor().focusedProperty().addListener((obs, oldValue, focused) -> {
                if (focused) {
                    Platform.runLater(() -> {
                        search.getEditor().selectAll();
                        rebuild(search.getEditor().getText());
                    });
                } else if (!isKnown(search.getEditor().getText())) {
                    syncFromOriginal(original.getValue());
                }
            });
            search.setOnAction(event -> {
                if (syncing) return;
                String selected = search.getSelectionModel().getSelectedItem();
                if (isKnown(selected)) commit(selected);
            });
            original.valueProperty().addListener((obs, oldValue, newValue) -> syncFromOriginal(newValue));
            I18n.languageProperty().addListener((obs, oldValue, newValue) -> {
                search.setPromptText(I18n.dynamic("Cerca un campo metadata…", "Search a metadata field…"));
                sort.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                        "Inverti l'ordine alfabetico", "Reverse alphabetical order")));
            });

            rebuild("");
            syncFromOriginal(original.getValue());

            HBox toolbar = new HBox(8, search, sort);
            toolbar.getStyleClass().add("metadata-field-toolbar");
            toolbar.setAlignment(Pos.CENTER_LEFT);
            toolbar.setMaxWidth(Double.MAX_VALUE);
            return toolbar;
        }

        private void rebuild(String rawQuery) {
            String query = safe(rawQuery).trim().toUpperCase(Locale.ROOT);
            Comparator<String> comparator = Comparator.comparing(value -> value.toUpperCase(Locale.ROOT));
            if (descending) comparator = comparator.reversed();

            List<String> filtered = master.stream()
                    .filter(value -> query.isBlank() || value.toUpperCase(Locale.ROOT).contains(query))
                    .sorted(comparator)
                    .toList();

            syncing = true;
            try {
                String editorText = search.getEditor().getText();
                search.setItems(FXCollections.observableArrayList(filtered));
                search.getEditor().setText(editorText == null ? "" : editorText);
                search.getEditor().positionCaret(search.getEditor().getText().length());
            } finally {
                syncing = false;
            }

            if (search.getEditor().isFocused() && !filtered.isEmpty()) {
                Platform.runLater(search::show);
            } else if (filtered.isEmpty()) {
                search.hide();
            }
        }

        private void commit(String value) {
            if (!isKnown(value)) return;
            syncing = true;
            try {
                original.setValue(value);
                search.setValue(value);
                search.getEditor().setText(value);
                search.getEditor().positionCaret(value.length());
                search.hide();
            } finally {
                syncing = false;
            }
        }

        private void syncFromOriginal(String value) {
            if (value == null || value.isBlank()) return;
            syncing = true;
            try {
                search.setItems(FXCollections.observableArrayList(sortedMaster()));
                search.setValue(value);
                search.getEditor().setText(value);
                search.getEditor().positionCaret(value.length());
            } finally {
                syncing = false;
            }
        }

        private List<String> sortedMaster() {
            Comparator<String> comparator = Comparator.comparing(value -> value.toUpperCase(Locale.ROOT));
            if (descending) comparator = comparator.reversed();
            return master.stream().sorted(comparator).toList();
        }

        private boolean isKnown(String value) {
            return value != null && master.stream().anyMatch(item -> item.equalsIgnoreCase(value.trim()));
        }

        private ListCell<String> simpleCell() {
            return new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(null);
                    setText(empty || item == null ? null : item);
                }
            };
        }

        private ListCell<String> groupedCell() {
            return new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(null);
                    setGraphic(null);
                    if (empty || item == null) return;

                    VBox content = new VBox(2);
                    int index = getIndex();
                    String current = group(item);
                    String previous = index > 0 && index - 1 < getListView().getItems().size()
                            ? group(getListView().getItems().get(index - 1)) : "";
                    if (!current.equals(previous)) {
                        Label alpha = new Label(current + " —");
                        alpha.getStyleClass().add("metadata-alpha-header");
                        Separator separator = new Separator();
                        separator.getStyleClass().add("metadata-alpha-separator");
                        separator.setMaxWidth(Double.MAX_VALUE);
                        HBox.setHgrow(separator, Priority.ALWAYS);
                        HBox header = new HBox(7, alpha, separator);
                        header.setAlignment(Pos.CENTER_LEFT);
                        content.getChildren().add(header);
                    }
                    Label field = new Label(item);
                    field.getStyleClass().add("metadata-field-name");
                    content.getChildren().add(field);
                    setGraphic(content);
                }
            };
        }

        private static String group(String value) {
            if (value == null || value.isBlank()) return "#";
            char first = Character.toUpperCase(value.trim().charAt(0));
            return Character.isLetter(first) ? Character.toString(first) : "#";
        }
    }

    private static <T extends Node> List<T> findAll(Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collect(root, type, result);
        return result;
    }

    private static <T extends Node> void collect(Node node, Class<T> type, List<T> out) {
        if (node == null) return;
        if (type.isInstance(node)) out.add(type.cast(node));
        if (node instanceof ScrollPane scroll && scroll.getContent() != null) collect(scroll.getContent(), type, out);
        if (node instanceof SplitPane split) {
            for (Node item : split.getItems()) collect(item, type, out);
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) collect(tab.getContent(), type, out);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collect(child, type, out);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
