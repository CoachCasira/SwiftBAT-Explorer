package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/** Preferenze persistenti per visibilita/larghezza colonne e convenzioni di allineamento. */
public final class TablePreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(TablePreferences.class);
    private static final String NAME_KEY = TablePreferences.class.getName() + ".columnName";
    private static final String INSTALLED_KEY = TablePreferences.class.getName() + ".installed";
    private static final String COLUMN_MENU_KEY = TablePreferences.class.getName() + ".columnMenu";
    private static final String RESET_ITEM_KEY = TablePreferences.class.getName() + ".resetItem";

    private TablePreferences() {}

    /**
     * Installa intestazioni native e una gestione colonne nel menu contestuale.
     *
     * <p>Le vecchie intestazioni erano composte da HBox + testo + pulsante X.
     * Su macOS il graphic della TableColumn veniva misurato con una larghezza
     * diversa dalla colonna e il risultato visivo era uno sfalsamento continuo
     * tra X, titolo e contenuto. Le intestazioni ora tornano native: un solo testo,
     * stesso padding delle celle e nessun nodo interno che possa spostarsi.</p>
     */
    public static FlowPane install(TableView<?> table, String tableKey) {
        FlowPane hiddenBar = new FlowPane(6, 6);
        hiddenBar.getStyleClass().add("hidden-column-bar");
        hiddenBar.setVisible(false);
        hiddenBar.setManaged(false);
        if (table == null || tableKey == null) return hiddenBar;

        table.setTableMenuButtonVisible(false);

        ContextMenu menu = table.getContextMenu();
        if (menu == null) menu = new ContextMenu();

        Menu columnsMenu = new Menu(I18n.dynamic("Colonne", "Columns"));
        MenuItem reset = new MenuItem(I18n.t("Ripristina colonne"));
        table.getProperties().put(COLUMN_MENU_KEY, columnsMenu);
        table.getProperties().put(RESET_ITEM_KEY, reset);

        reset.setOnAction(event -> {
            try { PREFS.node(tableKey).clear(); } catch (Exception ignored) { }
            for (TableColumn<?, ?> column : table.getColumns()) resetColumn(column);
            refreshHiddenBar(table, hiddenBar);
            refreshColumnsMenu(table);
        });

        menu.getItems().addAll(columnsMenu, new SeparatorMenuItem(), reset);
        table.setContextMenu(menu);

        Runnable apply = () -> {
            for (TableColumn<?, ?> column : table.getColumns()) installColumn(column, tableKey, table, hiddenBar);
            refreshHiddenBar(table, hiddenBar);
            refreshColumnsMenu(table);
            table.requestLayout();
        };
        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<?, ?>>) change -> apply.run());
        Platform.runLater(apply);

        I18n.languageProperty().addListener((obs, oldValue, newValue) -> {
            columnsMenu.setText(I18n.dynamic("Colonne", "Columns"));
            reset.setText(I18n.t("Ripristina colonne"));
            refreshColumnTitles(table);
            refreshHiddenBar(table, hiddenBar);
            refreshColumnsMenu(table);
        });
        return hiddenBar;
    }

    private static void installColumn(TableColumn<?, ?> column, String tableKey,
                                      TableView<?> table, FlowPane hiddenBar) {
        String name = columnName(column);
        String id = columnId(name);
        String installToken = tableKey + "|" + id;

        // Important: do this even when the column was already seen. A legacy
        // layout pass may have restored a graphic header after the first pulse.
        column.getProperties().put(NAME_KEY, name);
        makeNativeHeader(column, name);

        if (installToken.equals(column.getProperties().get(INSTALLED_KEY))) return;
        column.getProperties().put(INSTALLED_KEY, installToken);

        Preferences node = PREFS.node(tableKey);
        column.setVisible(node.getBoolean(id + ".visible", true));
        double savedWidth = node.getDouble(id + ".width", -1);
        if (savedWidth > 40) column.setPrefWidth(savedWidth);

        column.visibleProperty().addListener((obs, oldValue, newValue) -> {
            node.putBoolean(id + ".visible", newValue);
            refreshHiddenBar(table, hiddenBar);
            refreshColumnsMenu(table);
        });
        column.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() > 40) node.putDouble(id + ".width", newValue.doubleValue());
        });
        for (TableColumn<?, ?> child : column.getColumns()) {
            installColumn(child, tableKey + "." + id, table, hiddenBar);
        }
    }

    private static void makeNativeHeader(TableColumn<?, ?> column, String name) {
        if (column == null) return;
        column.setGraphic(null);
        column.setText(I18n.t(name));
        if (!column.getStyleClass().contains("native-aligned-column")) {
            column.getStyleClass().add("native-aligned-column");
        }
    }

    private static void refreshColumnTitles(TableView<?> table) {
        if (table == null) return;
        for (TableColumn<?, ?> column : table.getColumns()) refreshColumnTitle(column);
        table.requestLayout();
    }

    private static void refreshColumnTitle(TableColumn<?, ?> column) {
        if (column == null) return;
        String name = columnName(column);
        makeNativeHeader(column, name);
        for (TableColumn<?, ?> child : column.getColumns()) refreshColumnTitle(child);
    }

    private static void refreshColumnsMenu(TableView<?> table) {
        if (table == null) return;
        Object raw = table.getProperties().get(COLUMN_MENU_KEY);
        if (!(raw instanceof Menu columnsMenu)) return;

        List<TableColumn<?, ?>> leaves = new ArrayList<>();
        for (TableColumn<?, ?> column : table.getColumns()) collectLeaves(column, leaves);
        columnsMenu.getItems().clear();

        for (TableColumn<?, ?> column : leaves) {
            String name = columnName(column);
            CheckMenuItem item = new CheckMenuItem(I18n.t(name));
            item.setSelected(column.isVisible());
            item.setOnAction(event -> column.setVisible(item.isSelected()));
            columnsMenu.getItems().add(item);
        }
        columnsMenu.setDisable(columnsMenu.getItems().isEmpty());
    }

    private static void refreshHiddenBar(TableView<?> table, FlowPane hiddenBar) {
        if (table == null || hiddenBar == null) return;
        List<TableColumn<?, ?>> leaves = new ArrayList<>();
        for (TableColumn<?, ?> column : table.getColumns()) collectLeaves(column, leaves);
        hiddenBar.getChildren().clear();

        boolean hasHidden = false;
        for (TableColumn<?, ?> column : leaves) {
            if (column.isVisible()) continue;
            hasHidden = true;
            String name = columnName(column);
            Button restore = new Button("+ " + I18n.t(name));
            restore.getStyleClass().add("hidden-column-chip");
            restore.setOnAction(event -> column.setVisible(true));
            hiddenBar.getChildren().add(restore);
        }

        if (hasHidden) {
            Button restoreAll = new Button("↶ " + I18n.dynamic("Ripristina tutte", "Restore all"));
            restoreAll.getStyleClass().addAll("hidden-column-chip", "hidden-column-restore-all");
            restoreAll.setFocusTraversable(false);
            restoreAll.setOnAction(event -> {
                for (TableColumn<?, ?> column : leaves) column.setVisible(true);
            });
            hiddenBar.getChildren().add(restoreAll);
        }

        hiddenBar.setVisible(hasHidden);
        hiddenBar.setManaged(hasHidden);
    }

    private static void collectLeaves(TableColumn<?, ?> column, List<TableColumn<?, ?>> leaves) {
        if (column.getColumns().isEmpty()) {
            leaves.add(column);
            return;
        }
        for (TableColumn<?, ?> child : column.getColumns()) collectLeaves(child, leaves);
    }

    private static void resetColumn(TableColumn<?, ?> column) {
        column.setVisible(true);
        for (TableColumn<?, ?> child : column.getColumns()) resetColumn(child);
    }

    private static String columnName(TableColumn<?, ?> column) {
        Object saved = column.getProperties().get(NAME_KEY);
        if (saved instanceof String text && !text.isBlank()) return text;
        String text = column.getText();
        if ((text == null || text.isBlank()) && column.getGraphic() instanceof Label label) text = label.getText();
        if (text == null || text.isBlank()) text = "column" + System.identityHashCode(column);
        return text;
    }

    private static String columnId(String text) {
        return text.replaceAll("[^A-Za-z0-9_]+", "_").toLowerCase(Locale.ROOT);
    }

    public static boolean isColumnVisible(String tableKey, String columnName) {
        if (tableKey == null || tableKey.isBlank() || columnName == null || columnName.isBlank()) return true;
        return PREFS.node(tableKey).getBoolean(columnId(columnName) + ".visible", true);
    }

    public static List<TableColumn<?, ?>> visibleLeafColumns(TableView<?> table) {
        if (table == null) return List.of();
        List<TableColumn<?, ?>> leaves = new ArrayList<>();
        for (TableColumn<?, ?> column : table.getColumns()) collectLeaves(column, leaves);
        leaves.removeIf(column -> !column.isVisible());
        return List.copyOf(leaves);
    }

    public static String exportColumnName(TableColumn<?, ?> column) {
        return I18n.t(columnName(column));
    }

    public static boolean isNumeric(String text) {
        if (text == null || text.isBlank()) return false;
        String clean = text.trim()
                .replace('−', '-')
                .replace(',', '.')
                .replaceFirst("(?i)^z\\s*=\\s*", "")
                .replaceFirst("(?i)\\s*(?:%|s|ms|ks|deg|kev)$", "")
                .trim();
        String number = "[-+]?(?:(?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))(?:[eE][-+]?\\d+)?";
        String qualified = "[<>≤≥≈~]?\\s*" + number + "\\??";
        return clean.matches(qualified)
                || clean.matches(qualified + "\\s*±\\s*" + number)
                || clean.matches(qualified + "\\s*(?:–|—|\\.\\.|/|(?i:or))\\s*" + qualified)
                || clean.matches("[-+]?\\d+(?:\\.\\d+)?\\s*[×x]\\s*10(?:\\^?[-+]?\\d+|[⁻⁺]?[⁰¹²³⁴⁵⁶⁷⁸⁹]+)");
    }

    public static void alignCell(TableCell<?, ?> cell, String value) {
        if (cell != null) {
            cell.setAlignment(isNumeric(value) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        }
    }
}
