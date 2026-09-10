package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/** Preferenze persistenti per visibilita/larghezza colonne e convenzioni di allineamento. */
public final class TablePreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(TablePreferences.class);
    private static final String NAME_KEY = TablePreferences.class.getName() + ".columnName";
    private static final String INSTALLED_KEY = TablePreferences.class.getName() + ".installed";

    private TablePreferences() {}

    /**
     * Installa intestazioni con X e restituisce la barra dei campi nascosti.
     * Il chiamante puo ignorare il valore di ritorno per mantenere compatibilita.
     */
    public static FlowPane install(TableView<?> table, String tableKey) {
        FlowPane hiddenBar = new FlowPane(6, 6);
        hiddenBar.getStyleClass().add("hidden-column-bar");
        hiddenBar.setVisible(false);
        hiddenBar.setManaged(false);
        if (table == null || tableKey == null) return hiddenBar;

        table.setTableMenuButtonVisible(false);
        Runnable apply = () -> {
            for (TableColumn<?, ?> column : table.getColumns()) installColumn(column, tableKey, table, hiddenBar);
            refreshHiddenBar(table, hiddenBar);
        };
        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<?, ?>>) change -> apply.run());
        Platform.runLater(apply);

        MenuItem reset = new MenuItem(I18n.t("Ripristina colonne"));
        reset.setOnAction(event -> {
            try { PREFS.node(tableKey).clear(); } catch (Exception ignored) {}
            for (TableColumn<?, ?> column : table.getColumns()) resetColumn(column);
            refreshHiddenBar(table, hiddenBar);
        });
        ContextMenu menu = table.getContextMenu();
        if (menu == null) menu = new ContextMenu();
        menu.getItems().add(reset);
        table.setContextMenu(menu);
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> {
            reset.setText(I18n.t("Ripristina colonne"));
            refreshHiddenBar(table, hiddenBar);
        });
        return hiddenBar;
    }

    private static void installColumn(TableColumn<?, ?> column, String tableKey,
                                      TableView<?> table, FlowPane hiddenBar) {
        String name = columnName(column);
        String id = columnId(name);
        String installToken = tableKey + "|" + id;
        if (installToken.equals(column.getProperties().get(INSTALLED_KEY))) return;
        column.getProperties().put(INSTALLED_KEY, installToken);
        column.getProperties().put(NAME_KEY, name);

        Preferences node = PREFS.node(tableKey);
        column.setVisible(node.getBoolean(id + ".visible", true));
        double savedWidth = node.getDouble(id + ".width", -1);
        if (savedWidth > 40) column.setPrefWidth(savedWidth);

        Tooltip inheritedTooltip = null;
        if (column.getGraphic() instanceof Label oldLabel) inheritedTooltip = oldLabel.getTooltip();
        Label label = new Label(I18n.t(name));
        label.getStyleClass().add("table-header-label");
        label.setMaxWidth(Double.MAX_VALUE);
        if (inheritedTooltip != null) label.setTooltip(inheritedTooltip);
        HBox.setHgrow(label, Priority.ALWAYS);

        Button close = new Button("×");
        close.getStyleClass().add("column-close-button");
        close.setFocusTraversable(false);
        close.setOnAction(event -> column.setVisible(false));
        Tooltip.install(close, UiFactory.quickTooltip(I18n.t("Nascondi colonna")));

        HBox header = new HBox(5, label, close);
        header.getStyleClass().add("closable-column-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);
        column.setText("");
        column.setGraphic(header);
        I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> label.setText(I18n.t(name)));

        column.visibleProperty().addListener((obs, oldValue, newValue) -> {
            node.putBoolean(id + ".visible", newValue);
            refreshHiddenBar(table, hiddenBar);
        });
        column.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() > 40) node.putDouble(id + ".width", newValue.doubleValue());
        });
        for (TableColumn<?, ?> child : column.getColumns()) installColumn(child, tableKey + "." + id, table, hiddenBar);
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
        String clean = text.trim().replace(',', '.').replace("%", "");
        return clean.matches("[-+]?((\\d+(\\.\\d*)?)|(\\.\\d+))([eE][-+]?\\d+)?")
                || clean.matches("[-+]?\\d+(\\.\\d+)?\\s*[×x]\\s*10\\^?[-+]?\\d+");
    }

    public static void alignCell(TableCell<?, ?> cell, String value) {
        // Nella tabella Population tutte le intestazioni sono allineate a sinistra.
        // Mantenere anche i valori sulla stessa origine visiva evita l'effetto di
        // colonne "sfalsate" (in particolare T90 e Copertura) e rende i separatori
        // molto più facili da seguire con lo sguardo.
        if (cell != null && cell.getTableView() != null
                && cell.getTableView().getStyleClass().contains("population-result-table")) {
            cell.setAlignment(Pos.CENTER_LEFT);
            return;
        }
        cell.setAlignment(isNumeric(value) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    }
}
