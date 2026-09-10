package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.List;

/**
 * Makes every TableView use the same visual origin for headers and cells.
 * The watcher is page-scoped and stops at TableView itself, so it never walks
 * JavaFX VirtualFlow/skin nodes while the user scrolls.
 */
public final class FinalTableAlignmentFix {
    private static final String WATCHED = FinalTableAlignmentFix.class.getName() + ".watched";
    private static final String TABLE_DONE = FinalTableAlignmentFix.class.getName() + ".tableDone";
    private static final String COLUMN_DONE = FinalTableAlignmentFix.class.getName() + ".columnDone";
    private static final String CSS = stylesheet();

    private FinalTableAlignmentFix() { }

    public static void install(Node root) {
        watch(root);
    }

    private static void watch(Node node) {
        if (node == null) return;
        if (node instanceof TableView<?> table) {
            normalizeTable(table);
            return; // never descend into VirtualFlow/skin internals
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
        if (CSS != null && !table.getStylesheets().contains(CSS)) table.getStylesheets().add(CSS);
        table.setMinWidth(0);
        table.setMaxWidth(Double.MAX_VALUE);

        Runnable normalize = () -> {
            for (TableColumn<?, ?> column : List.copyOf(table.getColumns())) normalizeColumn(column);
            table.requestLayout();
        };
        normalize.run();
        Platform.runLater(normalize);

        if (!Boolean.TRUE.equals(table.getProperties().get(TABLE_DONE))) {
            table.getProperties().put(TABLE_DONE, Boolean.TRUE);
            table.getColumns().addListener((ListChangeListener<TableColumn<?, ?>>) change -> normalize.run());
            table.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(normalize));
        }
    }

    private static void normalizeColumn(TableColumn<?, ?> column) {
        if (column == null) return;
        Node graphic = column.getGraphic();
        if (graphic instanceof HBox header && header.getStyleClass().contains("closable-column-header")) {
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(0, 2, 0, 9));
            header.setSpacing(4);
            header.setMinWidth(0);
            header.setMaxWidth(Double.MAX_VALUE);
            fitHeader(header, column.getWidth());

            for (Node child : header.getChildren()) {
                if (child instanceof Label label) {
                    label.setAlignment(Pos.CENTER_LEFT);
                    label.setPadding(Insets.EMPTY);
                    label.setMinWidth(0);
                    label.setMaxWidth(Double.MAX_VALUE);
                    label.setTextOverrun(OverrunStyle.ELLIPSIS);
                    label.setEllipsisString("…");
                    HBox.setHgrow(label, Priority.ALWAYS);
                } else if (child instanceof Button close) {
                    close.setMinWidth(18);
                    close.setPrefWidth(18);
                    close.setMaxWidth(18);
                    HBox.setHgrow(close, Priority.NEVER);
                }
            }

            if (!Boolean.TRUE.equals(column.getProperties().get(COLUMN_DONE))) {
                column.getProperties().put(COLUMN_DONE, Boolean.TRUE);
                column.widthProperty().addListener((obs, oldValue, newValue) ->
                        fitHeader(header, newValue.doubleValue()));
            }
        }
        for (TableColumn<?, ?> child : column.getColumns()) normalizeColumn(child);
    }

    private static void fitHeader(HBox header, double columnWidth) {
        // Account for the small padding used by the JavaFX ColumnHeader skin.
        header.setPrefWidth(Math.max(0, columnWidth - 10));
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
