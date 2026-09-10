package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Stand-alone metadata keyword selector used by Explorer.
 *
 * <p>This control deliberately does not use ChoiceBox/ComboBox popups. The
 * complete UI (editable search, alphabetical grouping, sort direction and
 * scrollable results) is made of ordinary JavaFX nodes, so its geometry is the
 * same on macOS and Windows.</p>
 */
public final class MetadataFieldSelector extends VBox {
    private static final int MAX_VISIBLE_ROWS = 8;

    private final TextField search = new TextField();
    private final Button sortButton = UiFactory.button("A → Z", "ghost-button");
    private final ListView<Entry> results = new ListView<>();
    private final ObservableList<Entry> displayed = FXCollections.observableArrayList();
    private final List<String> master;
    private final Consumer<String> selectionConsumer;

    private boolean ascending = true;
    private boolean internalTextChange;
    private String selectedValue;

    public MetadataFieldSelector(List<String> values, String initialValue, Consumer<String> selectionConsumer) {
        this.master = normalize(values);
        this.selectionConsumer = selectionConsumer == null ? ignored -> { } : selectionConsumer;

        getStyleClass().add("metadata-field-selector-v2");
        setSpacing(7);
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);

        search.getStyleClass().add("metadata-field-search-v2");
        search.setPromptText(I18n.dynamic("Cerca un campo metadata…", "Search a metadata field…"));
        search.setMinWidth(0);
        search.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(search, Priority.ALWAYS);

        sortButton.getStyleClass().add("metadata-sort-v2");
        sortButton.setMinWidth(72);
        sortButton.setPrefWidth(76);
        sortButton.setMaxWidth(80);
        sortButton.setFocusTraversable(false);
        sortButton.setOnAction(event -> {
            ascending = !ascending;
            sortButton.setText(ascending ? "A → Z" : "Z → A");
            rebuild(search.getText(), true);
            search.requestFocus();
        });

        HBox toolbar = new HBox(8, search, sortButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("metadata-field-toolbar-v2");

        results.setItems(displayed);
        results.getStyleClass().add("metadata-field-results-v2");
        results.setFocusTraversable(false);
        results.setMaxWidth(Double.MAX_VALUE);
        results.setMinHeight(0);
        results.setPrefHeight(220);
        results.setMaxHeight(220);
        results.setVisible(false);
        results.setManaged(false);
        results.setCellFactory(list -> new ResultCell());

        results.setOnMouseClicked(event -> {
            Entry entry = results.getSelectionModel().getSelectedItem();
            if (entry != null && !entry.header()) choose(entry.value());
        });

        search.textProperty().addListener((obs, oldValue, newValue) -> {
            if (internalTextChange) return;
            rebuild(newValue, true);
        });
        search.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                rebuild(search.getText(), true);
                Platform.runLater(search::selectAll);
            }
        });
        search.setOnMousePressed(event -> rebuild(search.getText(), true));
        search.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN) {
                showResults();
                selectNextSelectable(1);
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                showResults();
                selectNextSelectable(-1);
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                String candidate = selectedResultOrFirst();
                if (candidate != null) {
                    choose(candidate);
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                hideResults();
                event.consume();
            }
        });

        getChildren().addAll(toolbar, results);
        setSelectedValue(initialValue, false);

        I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) ->
                search.setPromptText(I18n.dynamic("Cerca un campo metadata…", "Search a metadata field…")));
    }

    public void setSelectedValue(String value, boolean notify) {
        if (value == null || value.isBlank()) return;
        selectedValue = value;
        internalTextChange = true;
        search.setText(value);
        search.positionCaret(value.length());
        internalTextChange = false;
        if (notify) selectionConsumer.accept(value);
        hideResults();
    }

    public String selectedValue() {
        return selectedValue;
    }

    private void choose(String value) {
        if (value == null || value.isBlank()) return;
        setSelectedValue(value, true);
    }

    private void rebuild(String query, boolean show) {
        String normalizedQuery = query == null ? "" : query.trim().toUpperCase(Locale.ROOT);
        List<String> matches = master.stream()
                .filter(value -> normalizedQuery.isBlank() || value.toUpperCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(ascending ? String.CASE_INSENSITIVE_ORDER : String.CASE_INSENSITIVE_ORDER.reversed())
                .toList();

        displayed.setAll(group(matches));
        if (show && !matches.isEmpty()) showResults();
        else hideResults();
    }

    private List<Entry> group(List<String> values) {
        List<Entry> entries = new ArrayList<>();
        String previous = null;
        for (String value : values) {
            String group = groupName(value);
            if (!group.equals(previous)) {
                entries.add(new Entry(group + " —", true));
                previous = group;
            }
            entries.add(new Entry(value, false));
        }
        return entries;
    }

    private void showResults() {
        results.setVisible(true);
        results.setManaged(true);
        int keywords = (int) displayed.stream().filter(entry -> !entry.header()).count();
        int rows = Math.min(MAX_VISIBLE_ROWS, Math.max(2, keywords + Math.min(keywords, 3)));
        results.setPrefHeight(Math.min(220, 30 + rows * 32.0));
    }

    private void hideResults() {
        results.setVisible(false);
        results.setManaged(false);
    }

    private void selectNextSelectable(int direction) {
        if (displayed.isEmpty()) return;
        int index = results.getSelectionModel().getSelectedIndex();
        if (index < 0) index = direction > 0 ? -1 : displayed.size();
        for (int step = 0; step < displayed.size(); step++) {
            index += direction;
            if (index < 0) index = displayed.size() - 1;
            if (index >= displayed.size()) index = 0;
            Entry entry = displayed.get(index);
            if (!entry.header()) {
                results.getSelectionModel().select(index);
                results.scrollTo(index);
                return;
            }
        }
    }

    private String selectedResultOrFirst() {
        Entry selected = results.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.header()) return selected.value();
        return displayed.stream().filter(entry -> !entry.header()).map(Entry::value).findFirst().orElse(null);
    }

    private String groupName(String value) {
        if (value == null || value.isBlank()) return "#";
        char first = Character.toUpperCase(value.charAt(0));
        return Character.isLetter(first) ? String.valueOf(first) : "#";
    }

    private List<String> normalize(List<String> values) {
        Set<String> unique = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) unique.add(value.trim());
            }
        }
        return List.copyOf(unique);
    }

    private record Entry(String value, boolean header) { }

    private static final class ResultCell extends ListCell<Entry> {
        @Override
        protected void updateItem(Entry entry, boolean empty) {
            super.updateItem(entry, empty);
            getStyleClass().removeAll("metadata-result-header", "metadata-result-value");
            if (empty || entry == null) {
                setText(null);
                setGraphic(null);
                setDisable(false);
                return;
            }
            setText(entry.value());
            setAlignment(Pos.CENTER_LEFT);
            setPadding(entry.header() ? new Insets(7, 12, 4, 12) : new Insets(7, 12, 7, 18));
            if (entry.header()) {
                getStyleClass().add("metadata-result-header");
                setDisable(true);
            } else {
                getStyleClass().add("metadata-result-value");
                setDisable(false);
            }
        }
    }
}
