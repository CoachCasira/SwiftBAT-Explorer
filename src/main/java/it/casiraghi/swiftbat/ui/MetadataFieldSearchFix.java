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
import javafx.scene.control.Separator;
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
 * Deterministic restoration of the metadata selector used before the late UI
 * polish passes. It identifies the real FITS keyword ChoiceBox by its contents,
 * so it is independent of language, tab timing and label text.
 */
public final class MetadataFieldSearchFix {
    private static final String INSTALLED = MetadataFieldSearchFix.class.getName() + ".installed";

    private MetadataFieldSearchFix() { }

    public static void install(Node root) {
        if (root == null) return;
        for (ChoiceBox<?> raw : findAll(root, ChoiceBox.class)) {
            if (!looksLikeMetadataKeywordChoice(raw)) continue;
            installOn(raw);
        }
    }

    @SuppressWarnings("unchecked")
    private static void installOn(ChoiceBox<?> raw) {
        ChoiceBox<String> original = (ChoiceBox<String>) raw;
        if (Boolean.TRUE.equals(original.getProperties().get(INSTALLED))) return;
        if (!(original.getParent() instanceof VBox parent)) return;

        original.getProperties().put(INSTALLED, Boolean.TRUE);
        List<String> master = original.getItems().stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct().toList();
        if (master.isEmpty()) return;

        int index = parent.getChildren().indexOf(original);
        original.setVisible(false);
        original.setManaged(false);

        ComboBox<String> search = new ComboBox<>();
        search.setEditable(true);
        search.setVisibleRowCount(14);
        search.getStyleClass().addAll("choice-box-modern", "metadata-search-combo");
        search.setMinWidth(0);
        search.setPrefWidth(245);
        search.setMaxWidth(Double.MAX_VALUE);
        search.setPromptText(I18n.dynamic("Cerca un campo metadata…", "Search a metadata field…"));
        HBox.setHgrow(search, Priority.ALWAYS);

        Button sort = UiFactory.button("A → Z", "ghost-button");
        sort.getStyleClass().add("metadata-sort-button");
        sort.setMinWidth(66);
        sort.setPrefWidth(66);
        sort.setMaxWidth(66);
        sort.setFocusTraversable(false);

        State state = new State(original, search, sort, master);
        search.setCellFactory(list -> state.groupedCell());
        search.setButtonCell(state.buttonCell());
        sort.setOnAction(event -> state.toggleSort());
        search.getEditor().textProperty().addListener((obs, oldValue, newValue) -> state.filter(newValue));
        search.getEditor().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                Platform.runLater(search.getEditor()::selectAll);
                state.filter(search.getEditor().getText());
            } else if (!state.isKnown(search.getEditor().getText())) {
                state.syncFromOriginal(original.getValue());
            }
        });
        search.setOnAction(event -> {
            if (state.syncing) return;
            String selected = search.getSelectionModel().getSelectedItem();
            if (state.isKnown(selected)) state.commit(selected);
        });
        original.valueProperty().addListener((obs, oldValue, newValue) -> state.syncFromOriginal(newValue));
        I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                search.setPromptText(I18n.dynamic("Cerca un campo metadata…", "Search a metadata field…")));

        HBox toolbar = new HBox(8, search, sort);
        toolbar.getStyleClass().add("metadata-field-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setMaxWidth(Double.MAX_VALUE);
        parent.getChildren().add(Math.max(0, index), toolbar);

        state.rebuild("");
        state.syncFromOriginal(original.getValue());
    }

    private static boolean looksLikeMetadataKeywordChoice(ChoiceBox<?> choice) {
        if (choice == null || choice.getItems() == null || choice.getItems().isEmpty()) return false;
        boolean simple = false, bitpix = false, naxis = false;
        for (Object value : choice.getItems()) {
            String text = value == null ? "" : value.toString().toUpperCase(Locale.ROOT);
            if ("SIMPLE".equals(text)) simple = true;
            if ("BITPIX".equals(text)) bitpix = true;
            if ("NAXIS".equals(text)) naxis = true;
        }
        return simple && bitpix && naxis;
    }

    private static final class State {
        private final ChoiceBox<String> original;
        private final ComboBox<String> search;
        private final Button sort;
        private final List<String> master;
        private boolean descending;
        private boolean syncing;

        State(ChoiceBox<String> original, ComboBox<String> search, Button sort, List<String> master) {
            this.original = original;
            this.search = search;
            this.sort = sort;
            this.master = new ArrayList<>(master);
        }

        void toggleSort() {
            descending = !descending;
            sort.setText(descending ? "Z → A" : "A → Z");
            rebuild(search.getEditor().getText());
        }

        void filter(String raw) {
            if (syncing) return;
            rebuild(raw);
        }

        void rebuild(String raw) {
            String query = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
            Comparator<String> comparator = Comparator.comparing(value -> value.toUpperCase(Locale.ROOT));
            if (descending) comparator = comparator.reversed();
            List<String> values = master.stream()
                    .filter(value -> query.isBlank() || value.toUpperCase(Locale.ROOT).contains(query))
                    .sorted(comparator)
                    .toList();

            String editorText = search.getEditor().getText();
            syncing = true;
            try {
                search.setItems(FXCollections.observableArrayList(values));
                search.getEditor().setText(editorText == null ? "" : editorText);
                search.getEditor().positionCaret(search.getEditor().getText().length());
            } finally {
                syncing = false;
            }
            if (search.getEditor().isFocused() && !values.isEmpty()) Platform.runLater(search::show);
            else if (values.isEmpty()) search.hide();
        }

        void commit(String value) {
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

        void syncFromOriginal(String value) {
            if (value == null || value.isBlank()) return;
            syncing = true;
            try {
                List<String> values = sortedMaster();
                search.setItems(FXCollections.observableArrayList(values));
                search.setValue(value);
                search.getEditor().setText(value);
                search.getEditor().positionCaret(value.length());
            } finally {
                syncing = false;
            }
        }

        boolean isKnown(String value) {
            return value != null && master.stream().anyMatch(item -> item.equalsIgnoreCase(value.trim()));
        }

        List<String> sortedMaster() {
            Comparator<String> comparator = Comparator.comparing(value -> value.toUpperCase(Locale.ROOT));
            if (descending) comparator = comparator.reversed();
            return master.stream().sorted(comparator).toList();
        }

        ListCell<String> buttonCell() {
            return new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(null);
                    setText(empty || item == null ? null : item);
                }
            };
        }

        ListCell<String> groupedCell() {
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
                        Label letter = new Label(current);
                        letter.getStyleClass().add("metadata-alpha-header");
                        Separator line = new Separator();
                        line.getStyleClass().add("metadata-alpha-separator");
                        line.setMaxWidth(Double.MAX_VALUE);
                        HBox.setHgrow(line, Priority.ALWAYS);
                        HBox header = new HBox(7, letter, line);
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

        private String group(String value) {
            if (value == null || value.isBlank()) return "#";
            char first = Character.toUpperCase(value.charAt(0));
            return Character.isLetter(first) ? String.valueOf(first) : "#";
        }
    }

    private static <T extends Node> List<T> findAll(Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collect(root, type, result);
        return result;
    }

    private static <T extends Node> void collect(Node node, Class<T> type, List<T> result) {
        if (node == null) return;
        if (type.isInstance(node)) result.add(type.cast(node));
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) collect(tab.getContent(), type, result);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collect(child, type, result);
        }
    }
}