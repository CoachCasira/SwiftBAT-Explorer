package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.TimeEnergyHeatmapPane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Owns two last-mile behaviours that must be stable from the first JavaFX pulse:
 * external TableView vertical scrollbars and the initial spectroscopy time-energy layout.
 *
 * <p>The preparatory pass marks tables before the older compatibility enhancers see
 * them. This prevents two different pieces of code from resizing/reparenting the
 * same TableView. The visible scrollbar is then placed in a dedicated gutter next
 * to the table while the native VirtualFlow scrollbar remains only as the bound
 * scrolling engine at zero width.</p>
 */
public final class UiTableAndStartupFixes {
    private static final String PREPARED = UiTableAndStartupFixes.class.getName() + ".prepared";
    private static final String WATCHED = UiTableAndStartupFixes.class.getName() + ".watched";
    private static final String TABLE_INSTALLED = UiTableAndStartupFixes.class.getName() + ".tableInstalled";
    private static final String HEATMAP_INSTALLED = UiTableAndStartupFixes.class.getName() + ".heatmapInstalled";
    private static final String SPECTRO_TABS_INSTALLED = UiTableAndStartupFixes.class.getName() + ".spectroTabsInstalled";
    private static final String INTERNAL_BAR = UiTableAndStartupFixes.class.getName() + ".internalBar";

    // Private flags used by the older passes. Reusing the exact property keys lets
    // this class take ownership without changing their public API.
    private static final String FINAL_TABLE_DONE = FinalUiStabilityEnhancer.class.getName() + ".tableDone";
    private static final String LAST_MILE_TABLE_WRAPPED = UiLastMileFixes.class.getName() + ".tableWrapped";

    private UiTableAndStartupFixes() {
    }

    /** Must run before FinalUiStabilityEnhancer and UiLastMileFixes.install. */
    public static void prepare(Parent root) {
        if (root != null) prepareNode(root);
    }

    /** Runs after the compatibility enhancers and becomes the sole table wrapper. */
    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> {
            watch(root);
            forceVisibleSpectroscopyLayout(root);
        });
    }

    private static void prepareNode(Node node) {
        if (node == null) return;
        if (node instanceof TableView<?> table) {
            table.getProperties().put(FINAL_TABLE_DONE, Boolean.TRUE);
            table.getProperties().put(LAST_MILE_TABLE_WRAPPED, Boolean.TRUE);
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) prepareNode(tab.getContent());
            String key = PREPARED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(key))) {
                tabs.getProperties().put(key, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (Tab tab : change.getAddedSubList()) if (tab.getContent() != null) prepareNode(tab.getContent());
                        }
                    }
                });
            }
        }
        if (!(node instanceof Parent parent)) return;
        if (!Boolean.TRUE.equals(parent.getProperties().get(PREPARED))) {
            parent.getProperties().put(PREPARED, Boolean.TRUE);
            parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        for (Node added : List.copyOf(change.getAddedSubList())) prepareNode(added);
                    }
                }
            });
        }
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) prepareNode(child);
    }

    private static void watch(Node node) {
        if (node == null) return;
        if (node instanceof TableView<?> table) scheduleTableInstall(table);
        if (node instanceof TimeEnergyHeatmapPane heatmap) installHeatmapFix(heatmap);
        if (node instanceof TabPane tabs && tabs.getStyleClass().contains("spectroscopy-tabs")) {
            installSpectroscopyTabsFix(tabs);
        }

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) watch(tab.getContent());
            String key = WATCHED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(key))) {
                tabs.getProperties().put(key, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (Tab tab : change.getAddedSubList()) if (tab.getContent() != null) watch(tab.getContent());
                        }
                    }
                });
            }
        }

        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
                }
            }
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    /* ---------------- Stable external TableView scrollbar ---------------- */

    private static void scheduleTableInstall(TableView<?> table) {
        if (table == null || Boolean.TRUE.equals(table.getProperties().get(TABLE_INSTALLED))) return;
        table.getProperties().put(TABLE_INSTALLED, Boolean.TRUE);
        Platform.runLater(() -> installExternalScrollbar(table));
    }

    private static void installExternalScrollbar(TableView<?> table) {
        if (table.getParent() == null) {
            table.getProperties().remove(TABLE_INSTALLED);
            return;
        }

        ScrollBar external = new ScrollBar();
        external.setOrientation(Orientation.VERTICAL);
        external.setFocusTraversable(false);
        external.getStyleClass().add("table-external-scrollbar");
        external.setMinWidth(7);
        external.setPrefWidth(7);
        external.setMaxWidth(7);
        external.setMaxHeight(Double.MAX_VALUE);

        StackPane shell = new StackPane();
        shell.getStyleClass().add("external-table-scroll-shell");
        shell.setMinSize(0, 0);
        shell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Replace first, then re-parent the table. Constructing a new parent with
        // the table before this step can detach it from its original container and
        // is the reason some tables vanished in the previous implementation.
        if (!replaceInParent(table, shell)) {
            table.getProperties().remove(TABLE_INSTALLED);
            return;
        }

        shell.getChildren().addAll(table, external);
        StackPane.setAlignment(table, Pos.CENTER_LEFT);
        StackPane.setMargin(table, new Insets(0, 11, 0, 0));
        StackPane.setAlignment(external, Pos.CENTER_RIGHT);
        StackPane.setMargin(external, new Insets(1, 1, 1, 0));
        table.setMinSize(0, 0);
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        table.skinProperty().addListener((obs, oldSkin, newSkin) -> scheduleWire(table, external));
        table.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) scheduleWire(table, external);
        });
        table.heightProperty().addListener((obs, oldHeight, newHeight) -> scheduleWire(table, external));
        scheduleWire(table, external);
    }

    private static boolean replaceInParent(TableView<?> table, StackPane shell) {
        Parent parent = table.getParent();
        if (parent == null) return false;

        if (parent instanceof VBox box) {
            int index = box.getChildren().indexOf(table);
            if (index < 0) return false;
            Priority grow = VBox.getVgrow(table);
            Insets margin = VBox.getMargin(table);
            box.getChildren().set(index, shell);
            if (grow != null) VBox.setVgrow(shell, grow);
            if (margin != null) VBox.setMargin(shell, margin);
            return true;
        }
        if (parent instanceof HBox box) {
            int index = box.getChildren().indexOf(table);
            if (index < 0) return false;
            Priority grow = HBox.getHgrow(table);
            Insets margin = HBox.getMargin(table);
            box.getChildren().set(index, shell);
            if (grow != null) HBox.setHgrow(shell, grow);
            if (margin != null) HBox.setMargin(shell, margin);
            return true;
        }
        if (parent instanceof StackPane pane) {
            int index = pane.getChildren().indexOf(table);
            if (index < 0) return false;
            Pos alignment = StackPane.getAlignment(table);
            Insets margin = StackPane.getMargin(table);
            pane.getChildren().set(index, shell);
            if (alignment != null) StackPane.setAlignment(shell, alignment);
            if (margin != null) StackPane.setMargin(shell, margin);
            return true;
        }
        if (parent instanceof BorderPane pane) {
            Insets margin = BorderPane.getMargin(table);
            if (pane.getCenter() == table) pane.setCenter(shell);
            else if (pane.getTop() == table) pane.setTop(shell);
            else if (pane.getBottom() == table) pane.setBottom(shell);
            else if (pane.getLeft() == table) pane.setLeft(shell);
            else if (pane.getRight() == table) pane.setRight(shell);
            else return false;
            if (margin != null) BorderPane.setMargin(shell, margin);
            return true;
        }
        if (parent instanceof GridPane grid) {
            int index = grid.getChildren().indexOf(table);
            if (index < 0) return false;
            Integer row = GridPane.getRowIndex(table);
            Integer column = GridPane.getColumnIndex(table);
            Integer rowSpan = GridPane.getRowSpan(table);
            Integer columnSpan = GridPane.getColumnSpan(table);
            Priority hGrow = GridPane.getHgrow(table);
            Priority vGrow = GridPane.getVgrow(table);
            HPos hAlign = GridPane.getHalignment(table);
            VPos vAlign = GridPane.getValignment(table);
            Insets margin = GridPane.getMargin(table);
            Boolean fillWidth = GridPane.isFillWidth(table);
            Boolean fillHeight = GridPane.isFillHeight(table);
            grid.getChildren().set(index, shell);
            if (row != null) GridPane.setRowIndex(shell, row);
            if (column != null) GridPane.setColumnIndex(shell, column);
            if (rowSpan != null) GridPane.setRowSpan(shell, rowSpan);
            if (columnSpan != null) GridPane.setColumnSpan(shell, columnSpan);
            if (hGrow != null) GridPane.setHgrow(shell, hGrow);
            if (vGrow != null) GridPane.setVgrow(shell, vGrow);
            if (hAlign != null) GridPane.setHalignment(shell, hAlign);
            if (vAlign != null) GridPane.setValignment(shell, vAlign);
            if (margin != null) GridPane.setMargin(shell, margin);
            if (fillWidth != null) GridPane.setFillWidth(shell, fillWidth);
            if (fillHeight != null) GridPane.setFillHeight(shell, fillHeight);
            return true;
        }
        if (parent instanceof AnchorPane pane) {
            int index = pane.getChildren().indexOf(table);
            if (index < 0) return false;
            Double top = AnchorPane.getTopAnchor(table);
            Double right = AnchorPane.getRightAnchor(table);
            Double bottom = AnchorPane.getBottomAnchor(table);
            Double left = AnchorPane.getLeftAnchor(table);
            pane.getChildren().set(index, shell);
            if (top != null) AnchorPane.setTopAnchor(shell, top);
            if (right != null) AnchorPane.setRightAnchor(shell, right);
            if (bottom != null) AnchorPane.setBottomAnchor(shell, bottom);
            if (left != null) AnchorPane.setLeftAnchor(shell, left);
            return true;
        }
        if (parent instanceof Pane pane) {
            int index = pane.getChildren().indexOf(table);
            if (index < 0) return false;
            double x = table.getLayoutX();
            double y = table.getLayoutY();
            double width = table.getWidth();
            double height = table.getHeight();
            pane.getChildren().set(index, shell);
            shell.setLayoutX(x);
            shell.setLayoutY(y);
            if (width > 1) shell.setPrefWidth(width);
            if (height > 1) shell.setPrefHeight(height);
            return true;
        }
        return false;
    }

    private static void scheduleWire(TableView<?> table, ScrollBar external) {
        Platform.runLater(() -> {
            if (!wire(table, external)) Platform.runLater(() -> wire(table, external));
        });
    }

    private static boolean wire(TableView<?> table, ScrollBar external) {
        if (table.getSkin() == null) return false;
        table.applyCss();
        ScrollBar internal = null;
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL) {
                internal = bar;
                break;
            }
        }
        if (internal == null) return false;

        Object previous = external.getProperties().get(INTERNAL_BAR);
        if (previous != internal) {
            if (previous instanceof ScrollBar old) unbind(external, old);
            external.getProperties().put(INTERNAL_BAR, internal);
            external.minProperty().bind(internal.minProperty());
            external.maxProperty().bind(internal.maxProperty());
            external.visibleAmountProperty().bind(internal.visibleAmountProperty());
            external.unitIncrementProperty().bind(internal.unitIncrementProperty());
            external.blockIncrementProperty().bind(internal.blockIncrementProperty());
            external.valueProperty().bindBidirectional(internal.valueProperty());
            external.visibleProperty().bind(internal.visibleProperty());
            external.managedProperty().bind(internal.visibleProperty());
        }
        collapseNativeBar(internal);
        return true;
    }

    private static void unbind(ScrollBar external, ScrollBar old) {
        try {
            external.valueProperty().unbindBidirectional(old.valueProperty());
            external.minProperty().unbind();
            external.maxProperty().unbind();
            external.visibleAmountProperty().unbind();
            external.unitIncrementProperty().unbind();
            external.blockIncrementProperty().unbind();
            external.visibleProperty().unbind();
            external.managedProperty().unbind();
        } catch (RuntimeException ignored) {
            // A skin can disappear between two JavaFX pulses.
        }
    }

    private static void collapseNativeBar(ScrollBar internal) {
        internal.setOpacity(0);
        internal.setMouseTransparent(true);
        internal.setMinWidth(0);
        internal.setPrefWidth(0);
        internal.setMaxWidth(0);
        internal.setStyle("-fx-opacity: 0; -fx-min-width: 0; -fx-pref-width: 0; -fx-max-width: 0; -fx-padding: 0;");
    }

    /* ---------------- Spectroscopy first-layout repair ---------------- */

    private static void installHeatmapFix(TimeEnergyHeatmapPane heatmap) {
        if (Boolean.TRUE.equals(heatmap.getProperties().get(HEATMAP_INSTALLED))) return;
        heatmap.getProperties().put(HEATMAP_INSTALLED, Boolean.TRUE);
        heatmap.setMinHeight(410);
        heatmap.setPrefHeight(455);
        heatmap.setMaxHeight(Double.MAX_VALUE);
        heatmap.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) requestHeatmapLayout(heatmap);
        });
        requestHeatmapLayout(heatmap);
    }

    private static void installSpectroscopyTabsFix(TabPane tabs) {
        if (Boolean.TRUE.equals(tabs.getProperties().get(SPECTRO_TABS_INSTALLED))) return;
        tabs.getProperties().put(SPECTRO_TABS_INSTALLED, Boolean.TRUE);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && newTab.getContent() != null) {
                TimeEnergyHeatmapPane heatmap = findDescendant(newTab.getContent(), TimeEnergyHeatmapPane.class);
                if (heatmap != null) requestHeatmapLayout(heatmap);
            }
        });
    }

    private static void requestHeatmapLayout(TimeEnergyHeatmapPane heatmap) {
        Platform.runLater(() -> forceHeatmapLayout(heatmap));
    }

    private static void forceVisibleSpectroscopyLayout(Node node) {
        if (node == null) return;
        if (node instanceof TimeEnergyHeatmapPane heatmap && heatmap.isVisible()) forceHeatmapLayout(heatmap);
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) forceVisibleSpectroscopyLayout(tab.getContent());
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) forceVisibleSpectroscopyLayout(child);
        }
    }

    private static void forceHeatmapLayout(TimeEnergyHeatmapPane heatmap) {
        if (heatmap == null || heatmap.getScene() == null) return;
        Node current = heatmap;
        while (current != null) {
            if (current instanceof VBox box && box.getStyleClass().contains("time-energy-card")) {
                box.setMinHeight(500);
                box.setPrefHeight(525);
                box.setMaxHeight(Double.MAX_VALUE);
                VBox.setVgrow(heatmap, Priority.ALWAYS);
            }
            if (current instanceof Region region) region.requestLayout();
            current = current.getParent();
        }
        Scene scene = heatmap.getScene();
        if (scene != null && scene.getRoot() != null) {
            scene.getRoot().applyCss();
            scene.getRoot().requestLayout();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Node> T findDescendant(Node node, Class<T> type) {
        if (node == null) return null;
        if (type.isInstance(node)) return (T) node;
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                T found = findDescendant(tab.getContent(), type);
                if (found != null) return found;
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findDescendant(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
