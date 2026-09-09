package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;

import java.util.Locale;
import java.util.prefs.Preferences;

/** Preferenze persistenti per visibilità/larghezza colonne e convenzioni di allineamento. */
public final class TablePreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(TablePreferences.class);

    private TablePreferences() {}

    public static void install(TableView<?> table, String tableKey) {
        if (table == null || tableKey == null) return;
        table.setTableMenuButtonVisible(true);
        Runnable apply = () -> {
            for (TableColumn<?, ?> column : table.getColumns()) installColumn(column, tableKey);
        };
        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<?, ?>>) change -> apply.run());
        Platform.runLater(apply);

        MenuItem reset = new MenuItem(I18n.t("Ripristina colonne"));
        reset.setOnAction(event -> {
            try { PREFS.node(tableKey).clear(); } catch (Exception ignored) {}
            for (TableColumn<?, ?> column : table.getColumns()) resetColumn(column);
        });
        ContextMenu menu = table.getContextMenu();
        if (menu == null) menu = new ContextMenu();
        menu.getItems().add(reset);
        table.setContextMenu(menu);
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> reset.setText(I18n.t("Ripristina colonne")));
    }

    private static void installColumn(TableColumn<?, ?> column, String tableKey) {
        String id = columnId(column);
        Preferences node = PREFS.node(tableKey);
        column.setVisible(node.getBoolean(id + ".visible", true));
        double savedWidth = node.getDouble(id + ".width", -1);
        if (savedWidth > 40) column.setPrefWidth(savedWidth);
        column.visibleProperty().addListener((obs, oldValue, newValue) -> node.putBoolean(id + ".visible", newValue));
        column.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() > 40) node.putDouble(id + ".width", newValue.doubleValue());
        });
        for (TableColumn<?, ?> child : column.getColumns()) installColumn(child, tableKey + "." + id);
    }

    private static void resetColumn(TableColumn<?, ?> column) {
        column.setVisible(true);
        for (TableColumn<?, ?> child : column.getColumns()) resetColumn(child);
    }

    private static String columnId(TableColumn<?, ?> column) {
        String text = column.getText();
        if ((text == null || text.isBlank()) && column.getGraphic() instanceof Label label) text = label.getText();
        if (text == null || text.isBlank()) text = "column" + System.identityHashCode(column);
        return text.replaceAll("[^A-Za-z0-9_]+", "_").toLowerCase(Locale.ROOT);
    }

    public static boolean isNumeric(String text) {
        if (text == null || text.isBlank()) return false;
        String clean = text.trim().replace(',', '.').replace("%", "");
        return clean.matches("[-+]?((\\d+(\\.\\d*)?)|(\\.\\d+))([eE][-+]?\\d+)?")
                || clean.matches("[-+]?\\d+(\\.\\d+)?\\s*[×x]\\s*10\\^?[-+]?\\d+");
    }

    public static void alignCell(TableCell<?, ?> cell, String value) {
        cell.setAlignment(isNumeric(value) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    }
}
