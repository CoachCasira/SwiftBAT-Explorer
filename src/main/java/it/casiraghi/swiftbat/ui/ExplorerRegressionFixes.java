package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.ThreeDChartPane;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.control.OverrunStyle;
import javafx.util.StringConverter;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Small regression layer for Explorer behaviours that must survive the first
 * layout pulse on macOS as well as later workspace rebuilds.
 */
public final class ExplorerRegressionFixes {
    private static final String HOST_WATCHED = ExplorerRegressionFixes.class.getName() + ".hostWatched";
    private static final String WORKSPACE_WATCHED = ExplorerRegressionFixes.class.getName() + ".workspaceWatched";
    private static final String CACHE_COMBO_DONE = ExplorerRegressionFixes.class.getName() + ".cacheComboDone";
    private static final String TABLE_DONE = ExplorerRegressionFixes.class.getName() + ".tableDone";
    private static final String COLUMN_DONE = ExplorerRegressionFixes.class.getName() + ".columnDone";
    private static final String COLUMN_NAME = ExplorerRegressionFixes.class.getName() + ".columnName";
    private static final String METADATA_HORIZONTAL = ExplorerRegressionFixes.class.getName() + ".metadataHorizontal";
    private static final String SWING_CLOSE_ACTIVE = ExplorerRegressionFixes.class.getName() + ".swingCloseActive";
    private static final Set<Scene> SCENES = Collections.newSetFromMap(new WeakHashMap<>());

    private ExplorerRegressionFixes() { }

    public static void install(Parent root) {
        if (root == null) return;
        attachScene(root.getScene());
        root.sceneProperty().addListener((obs, oldScene, newScene) -> attachScene(newScene));
        Platform.runLater(() -> installHost(root));
    }

    private static void installHost(Parent root) {
        Parent host = findParentWithStyleClass(root, "page-host");
        if (host == null) {
            Platform.runLater(() -> installHost(root));
            return;
        }
        inspectHost(host);
        if (Boolean.TRUE.equals(host.getProperties().get(HOST_WATCHED))) return;
        host.getProperties().put(HOST_WATCHED, Boolean.TRUE);
        host.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) inspectPage(added);
            }
        });
    }

    private static void inspectHost(Parent host) {
        for (Node child : List.copyOf(host.getChildrenUnmodifiable())) inspectPage(child);
    }

    /** One bounded scan per visible page; only the Explorer workspace itself is watched afterwards. */
    private static void inspectPage(Node page) {
        if (page == null) return;
        Deque<Node> queue = new ArrayDeque<>();
        queue.add(page);
        while (!queue.isEmpty()) {
            Node current = queue.removeFirst();
            enhance(current);
            if (current instanceof Parent parent) {
                if (parent.getStyleClass().contains("workspace-host")) watchWorkspace(parent);
                queue.addAll(List.copyOf(parent.getChildrenUnmodifiable()));
            }
        }
    }

    private static void watchWorkspace(Parent workspace) {
        if (Boolean.TRUE.equals(workspace.getProperties().get(WORKSPACE_WATCHED))) return;
        workspace.getProperties().put(WORKSPACE_WATCHED, Boolean.TRUE);
        workspace.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) Platform.runLater(() -> inspectPage(added));
            }
        });
    }

    private static void enhance(Node node) {
        if (node instanceof ComboBox<?> combo) installCacheComboIfNeeded(combo);
        if (node instanceof TableView<?> table) installManagedTable(table);
    }

    @SuppressWarnings("unchecked")
    private static void installCacheComboIfNeeded(ComboBox<?> rawCombo) {
        if (rawCombo == null || Boolean.TRUE.equals(rawCombo.getProperties().get(CACHE_COMBO_DONE))) return;
        boolean cacheFilter = rawCombo.getItems().stream().map(String::valueOf)
                .anyMatch("Solo in cache"::equals)
                && rawCombo.getItems().stream().map(String::valueOf).anyMatch("Da scaricare"::equals);
        if (!cacheFilter) return;

        ComboBox<String> combo = (ComboBox<String>) rawCombo;
        combo.getProperties().put(CACHE_COMBO_DONE, Boolean.TRUE);
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(String value) {
                return value == null ? "" : I18n.t(value);
            }
            @Override public String fromString(String text) {
                if (text == null) return combo.getValue();
                for (String value : combo.getItems()) {
                    if (text.equals(value) || text.equals(I18n.t(value))) return value;
                }
                return combo.getValue();
            }
        });
        combo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : I18n.t(item));
            }
        });

        ListCell<String> buttonCell = new ListCell<>();
        buttonCell.setAlignment(Pos.CENTER_LEFT);
        buttonCell.textProperty().bind(Bindings.createStringBinding(
                () -> combo.getValue() == null ? "" : I18n.t(combo.getValue()),
                combo.valueProperty(), I18n.languageProperty()));
        combo.setButtonCell(buttonCell);
        combo.requestLayout();
    }

    private static void installManagedTable(TableView<?> table) {
        if (table == null || Boolean.TRUE.equals(table.getProperties().get(TABLE_DONE))) return;
        if (!hasColumnManagement(table.getContextMenu())) return;
        table.getProperties().put(TABLE_DONE, Boolean.TRUE);

        table.getColumns().addListener((ListChangeListener<TableColumn<?, ?>>) change ->
                Platform.runLater(() -> applyTableFixes(table)));
        I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) ->
                Platform.runLater(() -> applyTableFixes(table)));
        Platform.runLater(() -> applyTableFixes(table));
    }

    private static boolean hasColumnManagement(ContextMenu menu) {
        if (menu == null) return false;
        return menu.getItems().stream().anyMatch(item -> item instanceof Menu);
    }

    private static void applyTableFixes(TableView<?> table) {
        if (table == null) return;
        if (isMetadataTable(table)) installMetadataHorizontalScrolling(table);
        List<TableColumn<?, ?>> leaves = new ArrayList<>();
        for (TableColumn<?, ?> column : table.getColumns()) collectLeaves(column, leaves);
        for (TableColumn<?, ?> column : leaves) installHideHeader(column);
        table.requestLayout();
    }

    private static boolean isMetadataTable(TableView<?> table) {
        List<String> names = new ArrayList<>();
        for (TableColumn<?, ?> column : table.getColumns()) {
            String name = sourceColumnName(column).toLowerCase(Locale.ROOT);
            if (!name.isBlank()) names.add(name);
        }
        boolean hdu = names.stream().anyMatch(name -> name.equals("hdu"));
        boolean keyword = names.stream().anyMatch(name -> name.contains("keyword"));
        boolean value = names.stream().anyMatch(name -> name.equals("valore") || name.equals("value"));
        boolean comment = names.stream().anyMatch(name -> name.contains("comment"));
        return hdu && keyword && value && comment;
    }

    private static void installMetadataHorizontalScrolling(TableView<?> table) {
        if (Boolean.TRUE.equals(table.getProperties().get(METADATA_HORIZONTAL))) return;
        table.getProperties().put(METADATA_HORIZONTAL, Boolean.TRUE);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setMinWidth(0);
        table.setMaxWidth(Double.MAX_VALUE);
        table.requestLayout();
    }

    private static void installHideHeader(TableColumn<?, ?> column) {
        if (column == null) return;
        String source = sourceColumnName(column);
        if (source.isBlank()) return;
        column.getProperties().putIfAbsent(COLUMN_NAME, source);

        boolean alreadyOurs = Boolean.TRUE.equals(column.getProperties().get(COLUMN_DONE))
                && column.getGraphic() != null
                && column.getGraphic().getStyleClass().contains("removable-column-header");
        if (alreadyOurs) {
            refreshHeader(column);
            return;
        }
        column.getProperties().put(COLUMN_DONE, Boolean.TRUE);

        Label title = new Label(I18n.t(source));
        title.getStyleClass().add("table-header-label");
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        title.setMinWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);

        ButtonBase hide = new javafx.scene.control.Button("×");
        hide.getStyleClass().add("column-hide-action");
        hide.setFocusTraversable(false);
        hide.setMinSize(20, 20);
        hide.setPrefSize(20, 20);
        hide.setMaxSize(20, 20);
        hide.setTooltip(new Tooltip(I18n.dynamic("Nascondi colonna", "Hide column")));
        hide.setStyle("-fx-background-color: transparent; -fx-text-fill: #86a0c4; -fx-padding: 0; -fx-font-size: 13px; -fx-cursor: hand;");
        hide.setOnAction(event -> column.setVisible(false));

        HBox header = new HBox(5, title, hide);
        header.getStyleClass().add("removable-column-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMinWidth(0);
        header.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        column.setText("");
        column.setGraphic(header);
        syncHeaderWidth(column, header);
        if (!Boolean.TRUE.equals(column.getProperties().get(COLUMN_DONE + ".width"))) {
            column.getProperties().put(COLUMN_DONE + ".width", Boolean.TRUE);
            column.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                Node graphic = column.getGraphic();
                if (graphic instanceof Region region
                        && graphic.getStyleClass().contains("removable-column-header")) {
                    syncHeaderWidth(column, region);
                }
            });
        }
    }

    private static void refreshHeader(TableColumn<?, ?> column) {
        if (!(column.getGraphic() instanceof HBox header) || header.getChildren().isEmpty()) return;
        if (header.getChildren().get(0) instanceof Label label) label.setText(I18n.t(sourceColumnName(column)));
        if (header.getChildren().size() > 1 && header.getChildren().get(1) instanceof ButtonBase hide
                && hide.getTooltip() != null) {
            hide.getTooltip().setText(I18n.dynamic("Nascondi colonna", "Hide column"));
        }
        column.setText("");
        syncHeaderWidth(column, header);
    }

    private static void syncHeaderWidth(TableColumn<?, ?> column, Region header) {
        double available = Math.max(34.0, column.getWidth() - 18.0);
        header.setPrefWidth(available);
        header.setMaxWidth(available);
    }

    private static String sourceColumnName(TableColumn<?, ?> column) {
        Object saved = column.getProperties().get(COLUMN_NAME);
        if (saved instanceof String text && !text.isBlank()) return text;
        String text = column.getText();
        if (text != null && !text.isBlank()) return text;
        if (column.getGraphic() instanceof Label label && label.getText() != null) return label.getText();
        if (column.getGraphic() instanceof HBox header && !header.getChildren().isEmpty()
                && header.getChildren().get(0) instanceof Label label && label.getText() != null) {
            return label.getText();
        }
        return "";
    }

    private static void collectLeaves(TableColumn<?, ?> column, List<TableColumn<?, ?>> target) {
        if (column.getColumns().isEmpty()) {
            target.add(column);
            return;
        }
        for (TableColumn<?, ?> child : column.getColumns()) collectLeaves(child, target);
    }

    /* ---------------- Swing-backed 3D fullscreen close on macOS ---------------- */

    private static void attachScene(Scene scene) {
        if (scene == null || !SCENES.add(scene)) return;
        scene.addEventFilter(ActionEvent.ACTION, event -> {
            if (!(event.getTarget() instanceof ButtonBase button) || !isFullscreenBackButton(button)) return;
            Parent root = scene.getRoot();
            if (!isSwingThreeDFullscreen(root)) return;
            event.consume();
            requestSafeSwingClose(scene, root);
        });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ESCAPE) return;
            Parent root = scene.getRoot();
            if (!isSwingThreeDFullscreen(root)) return;
            event.consume();
            requestSafeSwingClose(scene, root);
        });
    }

    private static boolean isFullscreenBackButton(ButtonBase button) {
        Parent parent = button.getParent();
        return parent != null
                && parent.getStyleClass().contains("fullscreen-toolbar")
                && button.getText() != null
                && button.getText().trim().startsWith("←");
    }

    private static boolean isSwingThreeDFullscreen(Parent root) {
        return root != null
                && root.getStyleClass().contains("in-place-fullscreen")
                && findDescendant(root, ThreeDChartPane.class) != null
                && findDescendant(root, SwingNode.class) != null;
    }

    private static void requestSafeSwingClose(Scene scene, Parent fullscreenRoot) {
        if (scene == null || fullscreenRoot == null
                || Boolean.TRUE.equals(fullscreenRoot.getProperties().get(SWING_CLOSE_ACTIVE))) return;
        fullscreenRoot.getProperties().put(SWING_CLOSE_ACTIVE, Boolean.TRUE);

        ThreeDChartPane pane = findDescendant(fullscreenRoot, ThreeDChartPane.class);
        SwingNode swing = findDescendant(fullscreenRoot, SwingNode.class);
        if (pane == null || swing == null) {
            InPlaceFullscreen.close(fullscreenRoot);
            return;
        }

        JComponent content = swing.getContent();
        Runnable detachOnFx = () -> Platform.runLater(() -> {
            if (scene.getRoot() != fullscreenRoot) return;
            try {
                swing.setContent(null);
            } catch (RuntimeException ignored) {
            }
            detachPane(pane);
            Platform.runLater(() -> {
                if (scene.getRoot() == fullscreenRoot) InPlaceFullscreen.close(fullscreenRoot);
            });
        });

        if (content == null) {
            detachOnFx.run();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                content.setEnabled(false);
                content.setVisible(false);
            } finally {
                detachOnFx.run();
            }
        });
    }

    private static void detachPane(ThreeDChartPane pane) {
        Parent parent = pane.getParent();
        if (parent instanceof BorderPane border) {
            StackPane placeholder = new StackPane();
            placeholder.setMinSize(0, 0);
            if (border.getCenter() == pane) border.setCenter(placeholder);
            else if (border.getTop() == pane) border.setTop(placeholder);
            else if (border.getBottom() == pane) border.setBottom(placeholder);
            else if (border.getLeft() == pane) border.setLeft(placeholder);
            else if (border.getRight() == pane) border.setRight(placeholder);
            return;
        }
        if (parent instanceof Pane container) container.getChildren().remove(pane);
    }

    private static Parent findParentWithStyleClass(Parent root, String styleClass) {
        Node node = findDescendantMatching(root, candidate -> candidate instanceof Parent
                && candidate.getStyleClass().contains(styleClass));
        return node instanceof Parent parent ? parent : null;
    }

    private static <T extends Node> T findDescendant(Node root, Class<T> type) {
        Node node = findDescendantMatching(root, type::isInstance);
        return node == null ? null : type.cast(node);
    }

    private static Node findDescendantMatching(Node root, java.util.function.Predicate<Node> predicate) {
        if (root == null) return null;
        Deque<Node> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Node current = queue.removeFirst();
            if (predicate.test(current)) return current;
            if (current instanceof Parent parent) queue.addAll(List.copyOf(parent.getChildrenUnmodifiable()));
        }
        return null;
    }
}
