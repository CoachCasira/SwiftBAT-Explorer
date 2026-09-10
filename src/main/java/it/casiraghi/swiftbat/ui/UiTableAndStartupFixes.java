package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.TimeEnergyHeatmapPane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
 * Final owner for three behaviours that must not be split across enhancers:
 * external TableView scrollbars, first-layout sizing of the spectroscopy
 * time-energy map, and sky-map point selection versus card fullscreen.
 */
public final class UiTableAndStartupFixes {
    private static final String PREPARED = UiTableAndStartupFixes.class.getName() + ".prepared";
    private static final String WATCHED = UiTableAndStartupFixes.class.getName() + ".watched";
    private static final String TABLE_INSTALLED = UiTableAndStartupFixes.class.getName() + ".tableInstalled";
    private static final String HEATMAP_INSTALLED = UiTableAndStartupFixes.class.getName() + ".heatmapInstalled";
    private static final String SPECTRO_TABS_INSTALLED = UiTableAndStartupFixes.class.getName() + ".spectroTabsInstalled";
    private static final String INTERNAL_BAR = UiTableAndStartupFixes.class.getName() + ".internalBar";
    private static final String SKY_GUARD = UiTableAndStartupFixes.class.getName() + ".skyGuard";
    private static final String SKY_BUTTON_OLD_DISABLE = UiTableAndStartupFixes.class.getName() + ".skyButtonOldDisable";
    private static final double TABLE_SCROLLBAR_WIDTH = 12.0;
    private static final double TABLE_THUMB_MIN_LENGTH = 46.0;

    /* Exact keys used by the two legacy table passes. Setting them before those
       passes see a TableView makes this class the sole table geometry owner. */
    private static final String FINAL_TABLE_DONE = FinalUiStabilityEnhancer.class.getName() + ".tableDone";
    private static final String LAST_MILE_TABLE_WRAPPED = UiLastMileFixes.class.getName() + ".tableWrapped";

    private UiTableAndStartupFixes() {
    }

    /** Registers ownership markers before the legacy stability enhancers are installed. */
    public static void prepare(Parent root) {
        if (root != null) prepareNode(root);
    }

    /**
     * Must be installed before FinalUiStabilityEnhancer and UiLastMileFixes so
     * future dynamically-created tables are marked before either legacy watcher.
     */
    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> {
            watch(root);
            forceVisibleSpectroscopyLayout(root);
        });
    }

    private static void markTableOwned(TableView<?> table) {
        if (table == null) return;
        table.getProperties().put(FINAL_TABLE_DONE, Boolean.TRUE);
        table.getProperties().put(LAST_MILE_TABLE_WRAPPED, Boolean.TRUE);
    }

    private static void prepareNode(Node node) {
        if (node == null) return;
        if (node instanceof TableView<?> table) markTableOwned(table);

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) prepareNode(tab.getContent());
            }
            String key = PREPARED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(key))) {
                tabs.getProperties().put(key, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (Tab tab : change.getAddedSubList()) {
                                if (tab.getContent() != null) prepareNode(tab.getContent());
                            }
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

        if (node instanceof TableView<?> table) {
            markTableOwned(table);
            scheduleTableInstall(table);
        }
        if (node instanceof TimeEnergyHeatmapPane heatmap) installHeatmapFix(heatmap);
        if (node instanceof TabPane tabs && tabs.getStyleClass().contains("spectroscopy-tabs")) {
            installSpectroscopyTabsFix(tabs);
        }
        if (node instanceof Region region && isSkySurface(region)) installSkySelectionGuard(region);

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) watch(tab.getContent());
            }
            String key = WATCHED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(key))) {
                tabs.getProperties().put(key, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (Tab tab : change.getAddedSubList()) {
                                if (tab.getContent() != null) watch(tab.getContent());
                            }
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

    /* ---------------- Tables: one wrapper, scrollbar genuinely outside ---------------- */

    private static void scheduleTableInstall(TableView<?> table) {
        if (table == null || Boolean.TRUE.equals(table.getProperties().get(TABLE_INSTALLED))) return;
        table.getProperties().put(TABLE_INSTALLED, Boolean.TRUE);
        Platform.runLater(() -> installExternalScrollbar(table));
    }

    private static void installExternalScrollbar(TableView<?> table) {
        markTableOwned(table);
        if (table.getParent() == null) {
            table.getProperties().remove(TABLE_INSTALLED);
            return;
        }

        Parent currentParent = table.getParent();
        if (currentParent.getStyleClass().contains("external-table-scroll-shell")) {
            ScrollBar existing = findExternalScrollbar(currentParent);
            if (existing != null) {
                polishExternalScrollbar(existing);
                scheduleWire(table, existing);
            }
            return;
        }

        ScrollBar external = new ScrollBar();
        external.setOrientation(Orientation.VERTICAL);
        external.setFocusTraversable(false);
        external.getStyleClass().add("table-external-scrollbar");
        external.setMinWidth(TABLE_SCROLLBAR_WIDTH);
        external.setPrefWidth(TABLE_SCROLLBAR_WIDTH);
        external.setMaxWidth(TABLE_SCROLLBAR_WIDTH);
        external.setMaxHeight(Double.MAX_VALUE);

        HBox shell = new HBox(3);
        shell.getStyleClass().addAll("stable-table-scroll", "external-table-scroll-shell");
        shell.setAlignment(Pos.TOP_LEFT);
        shell.setFillHeight(true);
        shell.setMinSize(0, 0);
        shell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        if (!replaceInParent(table, shell)) {
            table.getProperties().remove(TABLE_INSTALLED);
            return;
        }

        shell.getChildren().addAll(table, external);
        table.setMinSize(0, 0);
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(table, Priority.ALWAYS);

        external.skinProperty().addListener((obs, oldSkin, newSkin) -> scheduleExternalPolish(external));
        external.heightProperty().addListener((obs, oldHeight, newHeight) -> scheduleExternalPolish(external));
        external.visibleAmountProperty().addListener((obs, oldValue, newValue) -> scheduleExternalPolish(external));
        table.skinProperty().addListener((obs, oldSkin, newSkin) -> scheduleWire(table, external));
        table.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) scheduleWire(table, external);
        });
        table.heightProperty().addListener((obs, oldHeight, newHeight) -> scheduleWire(table, external));
        scheduleExternalPolish(external);
        scheduleWire(table, external);
    }

    private static void scheduleExternalPolish(ScrollBar external) {
        Platform.runLater(() -> {
            polishExternalScrollbar(external);
            Platform.runLater(() -> polishExternalScrollbar(external));
        });
    }

    private static void polishExternalScrollbar(ScrollBar external) {
        if (external == null) return;
        external.setMinWidth(TABLE_SCROLLBAR_WIDTH);
        external.setPrefWidth(TABLE_SCROLLBAR_WIDTH);
        external.setMaxWidth(TABLE_SCROLLBAR_WIDTH);
        try {
            external.applyCss();
        } catch (RuntimeException ignored) {
            return;
        }
        Node thumb = external.lookup(".thumb");
        if (thumb instanceof Region region) {
            region.setMinHeight(TABLE_THUMB_MIN_LENGTH);
            region.setPrefWidth(10);
            region.setMinWidth(10);
        }
        external.requestLayout();
    }

    private static ScrollBar findExternalScrollbar(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof ScrollBar bar && bar.getStyleClass().contains("table-external-scrollbar")) return bar;
        }
        return null;
    }

    private static boolean replaceInParent(TableView<?> table, HBox shell) {
        Parent parent = table.getParent();
        if (parent == null || parent == shell) return false;

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
        markTableOwned(table);
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
        polishExternalScrollbar(external);
        scheduleExternalPolish(external);
        table.requestLayout();
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
            // A skin may disappear between two JavaFX pulses.
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

    /* ---------------- Spectroscopy: correct size from the first opening ---------------- */

    private static void installHeatmapFix(TimeEnergyHeatmapPane heatmap) {
        if (Boolean.TRUE.equals(heatmap.getProperties().get(HEATMAP_INSTALLED))) return;
        heatmap.getProperties().put(HEATMAP_INSTALLED, Boolean.TRUE);
        heatmap.setMinHeight(430);
        heatmap.setPrefHeight(470);
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
            if (newTab == null || newTab.getContent() == null) return;
            TimeEnergyHeatmapPane heatmap = findDescendant(newTab.getContent(), TimeEnergyHeatmapPane.class);
            if (heatmap != null) requestHeatmapLayout(heatmap);
        });
        tabs.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) Platform.runLater(() -> fitSelectedTimeEnergyTab(tabs));
        });
        Platform.runLater(() -> fitSelectedTimeEnergyTab(tabs));
    }

    private static void requestHeatmapLayout(TimeEnergyHeatmapPane heatmap) {
        Platform.runLater(() -> forceHeatmapLayout(heatmap));
    }

    private static void forceVisibleSpectroscopyLayout(Node node) {
        if (node == null) return;
        if (node instanceof TabPane tabs && tabs.getStyleClass().contains("spectroscopy-tabs")) {
            fitSelectedTimeEnergyTab(tabs);
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) forceVisibleSpectroscopyLayout(tab.getContent());
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) forceVisibleSpectroscopyLayout(child);
        }
    }

    private static void fitSelectedTimeEnergyTab(TabPane tabs) {
        if (tabs == null || tabs.getScene() == null) return;
        Tab selected = tabs.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getContent() == null) return;
        TimeEnergyHeatmapPane heatmap = findDescendant(selected.getContent(), TimeEnergyHeatmapPane.class);
        if (heatmap != null) forceHeatmapLayout(heatmap);
    }

    private static void forceHeatmapLayout(TimeEnergyHeatmapPane heatmap) {
        if (heatmap == null || heatmap.getScene() == null) return;

        heatmap.setMinHeight(430);
        heatmap.setPrefHeight(470);
        heatmap.setMaxHeight(Double.MAX_VALUE);

        TabPane spectroscopyTabs = null;
        Node current = heatmap;
        while (current != null) {
            if (current instanceof VBox box && box.getStyleClass().contains("time-energy-card")) {
                box.setMinHeight(540);
                box.setPrefHeight(560);
                box.setMaxHeight(Double.MAX_VALUE);
                VBox.setVgrow(heatmap, Priority.ALWAYS);
            }
            if (current instanceof TabPane tabs && tabs.getStyleClass().contains("spectroscopy-tabs")) spectroscopyTabs = tabs;
            if (current instanceof Region region) region.requestLayout();
            current = current.getParent();
        }

        if (spectroscopyTabs != null) {
            spectroscopyTabs.setMinHeight(760);
            spectroscopyTabs.setPrefHeight(Math.max(760, spectroscopyTabs.getPrefHeight()));
            spectroscopyTabs.setMaxHeight(Double.MAX_VALUE);
            if (spectroscopyTabs.getParent() != null) spectroscopyTabs.getParent().requestLayout();
        }

        Scene scene = heatmap.getScene();
        if (scene != null && scene.getRoot() != null) scene.getRoot().requestLayout();
        heatmap.requestLayout();
    }

    /* ---------------- Sky map: point selection wins over card fullscreen ---------------- */

    private static boolean isSkySurface(Region region) {
        String name = region.getClass().getSimpleName();
        return "MollweideSkyPane".equals(name) || "CelestialSpherePane".equals(name);
    }

    private static void installSkySelectionGuard(Region surface) {
        if (Boolean.TRUE.equals(surface.getProperties().get(SKY_GUARD))) return;
        surface.getProperties().put(SKY_GUARD, Boolean.TRUE);

        Button fullscreen = findFullscreenButtonAbove(surface);
        if (fullscreen == null) return;

        surface.cursorProperty().addListener((obs, oldCursor, newCursor) ->
                setSkySelectionGuard(fullscreen, newCursor == Cursor.CROSSHAIR));
        surface.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) setSkySelectionGuard(fullscreen, false);
        });
        setSkySelectionGuard(fullscreen, surface.getCursor() == Cursor.CROSSHAIR);
    }

    private static void setSkySelectionGuard(Button fullscreen, boolean active) {
        if (fullscreen == null) return;
        if (active) {
            if (!fullscreen.getProperties().containsKey(SKY_BUTTON_OLD_DISABLE)) {
                fullscreen.getProperties().put(SKY_BUTTON_OLD_DISABLE, fullscreen.isDisable());
            }
            fullscreen.setDisable(true);
            return;
        }
        Object previous = fullscreen.getProperties().remove(SKY_BUTTON_OLD_DISABLE);
        if (previous instanceof Boolean disabled) fullscreen.setDisable(disabled);
    }

    private static Button findFullscreenButtonAbove(Node node) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (current instanceof Parent parent) {
                Button found = findFullscreenButton(parent);
                if (found != null) return found;
            }
            current = current.getParent();
        }
        return null;
    }

    private static Button findFullscreenButton(Parent root) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.isVisible() && button.isManaged()) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase();
                String compact = text.replace(" ", "");
                if (text.contains("schermo intero") || text.contains("full screen") || compact.contains("fullscreen")) return button;
            }
            if (child instanceof Parent parent) {
                Button nested = findFullscreenButton(parent);
                if (nested != null) return nested;
            }
        }
        return null;
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
