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

import java.util.List;

/** Single owner for table alignment across the application. */
public final class FinalTableAlignmentFix {
    private static final String WATCHED = FinalTableAlignmentFix.class.getName() + ".watched";
    private static final String TABLE_DONE = FinalTableAlignmentFix.class.getName() + ".tableDone";
    private static final String TITLE_KEY = FinalTableAlignmentFix.class.getName() + ".title";
    private static final String TAB_DONE = FinalTableAlignmentFix.class.getName() + ".tabDone";
    private static final String COLUMN_STYLE = "-fx-alignment: CENTER-LEFT;";

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
            for (Tab tab : List.copyOf(tabs.getTabs())) watchTab(tab);
            if (!Boolean.TRUE.equals(tabs.getProperties().get(WATCHED))) {
                tabs.getProperties().put(WATCHED, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) if (change.wasAdded()) {
                        for (Tab tab : List.copyOf(change.getAddedSubList())) watchTab(tab);
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

    private static void watchTab(Tab tab) {
        if (tab == null) return;
        watch(tab.getContent());
        if (Boolean.TRUE.equals(tab.getProperties().get(TAB_DONE))) return;
        tab.getProperties().put(TAB_DONE, Boolean.TRUE);
        tab.contentProperty().addListener((obs, oldContent, newContent) -> watch(newContent));
    }

    private static void normalizeTable(TableView<?> table) {
        if (!table.getStyleClass().contains("final-aligned-table")) {
            table.getStyleClass().add("final-aligned-table");
        }
        table.setMinWidth(0);
        table.setMaxWidth(Double.MAX_VALUE);

        Runnable normalize = () -> {
            for (TableColumn<?, ?> column : List.copyOf(table.getColumns())) normalizeColumn(column);
            applyHeaderGeometry(table);
            table.requestLayout();
        };
        normalize.run();

        if (!Boolean.TRUE.equals(table.getProperties().get(TABLE_DONE))) {
            table.getProperties().put(TABLE_DONE, Boolean.TRUE);
            table.getColumns().addListener((ListChangeListener<TableColumn<?, ?>>) change -> Platform.runLater(normalize));
            table.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                if (newSkin != null) Platform.runLater(normalize);
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

        if (column.getGraphic() != null) column.setGraphic(null);
        if (!finalTitle.equals(column.getText())) column.setText(finalTitle);
        if (!COLUMN_STYLE.equals(column.getStyle())) column.setStyle(COLUMN_STYLE);
        for (TableColumn<?, ?> child : column.getColumns()) normalizeColumn(child);
    }

    private static void applyHeaderGeometry(TableView<?> table) {
        if (table == null || table.getScene() == null || table.getSkin() == null) return;
        try {
            // Do not call applyCss() here. The skin callback runs after CSS has
            // already materialised the header nodes; forcing another CSS pass on
            // every table/column update was expensive while scrolling Explorer.
            for (Node node : table.lookupAll(".column-header .label")) {
                if (!(node instanceof Label label)) continue;
                label.setAlignment(Pos.CENTER_LEFT);
                label.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
                Insets target = new Insets(0, 9, 0, 5);
                if (!target.equals(label.getPadding())) label.setPadding(target);
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
}
