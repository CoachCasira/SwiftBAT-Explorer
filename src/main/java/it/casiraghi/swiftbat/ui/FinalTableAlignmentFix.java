package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.List;

/** Single owner for table alignment across the application. */
public final class FinalTableAlignmentFix {
    private static final String WATCHED = FinalTableAlignmentFix.class.getName() + ".watched";
    private static final String TABLE_DONE = FinalTableAlignmentFix.class.getName() + ".tableDone";
    private static final String TITLE_KEY = FinalTableAlignmentFix.class.getName() + ".title";
    private static final String CSS = stylesheet();

    private FinalTableAlignmentFix() { }

    public static void install(Node root) {
        watch(root);
    }

    private static void watch(Node node) {
        if (node == null) return;
        if (node instanceof TableView<?> table) {
            normalizeTable(table);
            return;
        }
        if (node instanceof ScrollPane scroll) {
            if (scroll.getContent() != null) watch(scroll.getContent());
            if (!Boolean.TRUE.equals(scroll.getProperties().get(WATCHED))) {
                scroll.getProperties().put(WATCHED, Boolean.TRUE);
                scroll.contentProperty().addListener((obs, oldValue, newValue) -> watch(newValue));
            }
            return;
        }
        if (node instanceof SplitPane split) {
            for (Node item : List.copyOf(split.getItems())) watch(item);
            if (!Boolean.TRUE.equals(split.getProperties().get(WATCHED))) {
                split.getProperties().put(WATCHED, Boolean.TRUE);
                split.getItems().addListener((ListChangeListener<Node>) change -> {
                    while (change.next()) if (change.wasAdded()) {
                        for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
                    }
                });
            }
            return;
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : List.copyOf(tabs.getTabs())) watch(tab.getContent());
            if (!Boolean.TRUE.equals(tabs.getProperties().get(WATCHED))) {
                tabs.getProperties().put(WATCHED, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) if (change.wasAdded()) {
                        for (Tab tab : List.copyOf(change.getAddedSubList())) watch(tab.getContent());
                    }
                });
            }
            return;
        }
        if (!(node instanceof Parent parent)) return;
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
        if (!Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) {
            parent.getProperties().put(WATCHED, Boolean.TRUE);
            parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
                while (change.next()) if (change.wasAdded()) {
                    for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
                }
            });
        }
    }

    private static void normalizeTable(TableView<?> table) {
        if (!table.getStyleClass().contains("final-aligned-table")) {
            table.getStyleClass().add("final-aligned-table");
        }
        if (CSS != null && !table.getStylesheets().contains(CSS)) {
            table.getStylesheets().add(CSS);
        }
        table.setMinWidth(0);
        table.setMaxWidth(Double.MAX_VALUE);

        Runnable normalize = () -> {
            for (TableColumn<?, ?> column : List.copyOf(table.getColumns())) normalizeColumn(column);
            applyHeaderGeometry(table);
            table.requestLayout();
        };
        normalize.run();
        Platform.runLater(normalize);

        if (!Boolean.TRUE.equals(table.getProperties().get(TABLE_DONE))) {
            table.getProperties().put(TABLE_DONE, Boolean.TRUE);
            table.getColumns().addListener((ListChangeListener<TableColumn<?, ?>>) change -> Platform.runLater(normalize));
            table.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(normalize));
            table.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) Platform.runLater(normalize);
            });
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> Platform.runLater(normalize));
        }
    }

    private static void normalizeColumn(TableColumn<?, ?> column) {
        if (column == null) return;
        String title = extractTitle(column);
        if (title != null && !title.isBlank()) column.getProperties().put(TITLE_KEY, title);
        Object saved = column.getProperties().get(TITLE_KEY);
        String finalTitle = saved instanceof String value ? value : (title == null ? "" : title);

        column.setGraphic(null);
        column.setText(finalTitle);
        column.setStyle("-fx-alignment: CENTER-LEFT;");
        for (TableColumn<?, ?> child : column.getColumns()) normalizeColumn(child);
    }

    private static void applyHeaderGeometry(TableView<?> table) {
        if (table == null || table.getScene() == null) return;
        try {
            table.applyCss();
            for (Node node : table.lookupAll(".column-header .label")) {
                if (!(node instanceof Label label)) continue;
                label.setAlignment(Pos.CENTER_LEFT);
                label.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
                // JavaFX's macOS header skin already contributes about 4 px on
                // the left. Body cells use 9 px, so 5 px here produces the same
                // final visual origin instead of shifting the header to the right.
                label.setPadding(new Insets(0, 9, 0, 5));
            }
        } catch (RuntimeException ignored) {
            // The table may briefly be between skins while a tab is replaced.
        }
    }

    private static String extractTitle(TableColumn<?, ?> column) {
        String text = column.getText();
        if (text != null && !text.isBlank()) return text;
        Node graphic = column.getGraphic();
        if (graphic instanceof Label label && label.getText() != null && !label.getText().isBlank()) {
            return label.getText();
        }
        if (graphic instanceof HBox box) {
            for (Node child : box.getChildren()) {
                if (child instanceof Label label && label.getText() != null && !label.getText().isBlank()) {
                    return label.getText();
                }
            }
        }
        Object stored = column.getProperties().get(TITLE_KEY);
        return stored instanceof String value ? value : "";
    }

    private static String stylesheet() {
        try {
            URL url = FinalTableAlignmentFix.class.getResource("/final-table-alignment.css");
            return url == null ? null : url.toExternalForm();
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
