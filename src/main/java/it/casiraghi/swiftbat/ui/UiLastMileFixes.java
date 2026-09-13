package it.casiraghi.swiftbat.ui;

import javafx.animation.PauseTransition;
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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Single final JavaFX geometry controller for the accepted redesign.
 * Dynamic sizing stays in Java; CSS is left to colours, borders and typography.
 */
public final class UiLastMileFixes {
    private static final String PREPARED = UiLastMileFixes.class.getName() + ".prepared";
    private static final String WATCHED = UiLastMileFixes.class.getName() + ".watched";
    private static final String SCENE_WATCHED = UiLastMileFixes.class.getName() + ".sceneWatched";
    private static final String APPLY_PENDING = UiLastMileFixes.class.getName() + ".applyPending";
    private static final String POP_PENDING = UiLastMileFixes.class.getName() + ".populationPending";
    private static final String POP_TABS = UiLastMileFixes.class.getName() + ".populationTabs";
    private static final String POP_FILTER = UiLastMileFixes.class.getName() + ".populationFilter";
    private static final String TABLE_WRAPPED = UiLastMileFixes.class.getName() + ".tableWrapped";
    private static final String TABLE_INNER_BAR = UiLastMileFixes.class.getName() + ".innerBar";
    private static final String TOP_SPACER = UiLastMileFixes.class.getName() + ".topSpacer";

    private static final String FINAL_HISTOGRAM_DONE = FinalUiStabilityEnhancer.class.getName() + ".histogramDone";
    private static final String FINAL_FILTER_DONE = FinalUiStabilityEnhancer.class.getName() + ".filterDone";
    private static final String FINAL_POP_TABS_DONE = FinalUiStabilityEnhancer.class.getName() + ".populationTabsDone";
    private static final String RESPONSIVE_POPULATION_REFRESH = ResponsiveLayoutEnhancer.class.getName() + ".populationRefresh";

    private UiLastMileFixes() {
    }

    /** Called before FinalUiStabilityEnhancer so Population has one geometry owner. */
    public static void prepare(Parent root) {
        if (root != null) prepareNode(root);
    }

    /** Called after the other enhancers so this pass wins only on geometry. */
    public static void install(Parent root) {
        if (root == null) return;
        watch(root, root);
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            installSceneListener(root, newScene);
            if (newScene != null) requestApply(root);
        });
        installSceneListener(root, root.getScene());
        Platform.runLater(() -> applyAll(root));
    }

    private static void prepareNode(Node node) {
        if (node == null) return;
        if (node instanceof VBox box && box.getStyleClass().contains("population-histogram-card")) {
            box.getProperties().put(FINAL_HISTOGRAM_DONE, Boolean.TRUE);
        }
        if (node instanceof Region region && region.getStyleClass().contains("population-filter-card")) {
            region.getProperties().put(FINAL_FILTER_DONE, Boolean.TRUE);
        }
        if (node instanceof Button button && button.getStyleClass().contains("population-filter-restore")) {
            button.getProperties().put(FINAL_FILTER_DONE + ".restore", Boolean.TRUE);
        }
        if (node instanceof TabPane tabs && hasAncestorNamed(tabs, "PopulationPage")) {
            tabs.getProperties().put(FINAL_POP_TABS_DONE, Boolean.TRUE);
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
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) prepareNode(tab.getContent());
            tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        for (Tab tab : change.getAddedSubList()) if (tab.getContent() != null) prepareNode(tab.getContent());
                    }
                }
            });
        }
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) prepareNode(child);
    }

    private static void watch(Node node, Parent appRoot) {
        if (node == null) return;
        if (node instanceof TableView<?> table) installExternalTableScrollbar(table);
        if (node instanceof TabPane tabs && hasAncestorNamed(tabs, "PopulationPage")) {
            installPopulationTabListener(tabs, appRoot);
        }
        if (node instanceof Region region && region.getStyleClass().contains("population-filter-card")) {
            installPopulationFilterListener(region, appRoot);
        }
        if (node instanceof Button button && button.getStyleClass().contains("population-filter-restore")) {
            installPopulationFilterListener(button, appRoot);
        }

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) watch(tab.getContent(), appRoot);
            String key = WATCHED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(key))) {
                tabs.getProperties().put(key, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (Tab tab : change.getAddedSubList()) if (tab.getContent() != null) watch(tab.getContent(), appRoot);
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
                    for (Node added : List.copyOf(change.getAddedSubList())) watch(added, appRoot);
                }
            }
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child, appRoot);
    }

    private static void installSceneListener(Parent root, Scene scene) {
        if (scene == null || Boolean.TRUE.equals(scene.getProperties().get(SCENE_WATCHED))) return;
        scene.getProperties().put(SCENE_WATCHED, Boolean.TRUE);
        scene.widthProperty().addListener((obs, oldValue, newValue) -> requestApply(root));
        scene.heightProperty().addListener((obs, oldValue, newValue) -> requestApply(root));
    }

    private static void requestApply(Parent root) {
        if (root == null || Boolean.TRUE.equals(root.getProperties().get(APPLY_PENDING))) return;
        root.getProperties().put(APPLY_PENDING, Boolean.TRUE);
        Platform.runLater(() -> {
            root.getProperties().remove(APPLY_PENDING);
            applyAll(root);
        });
    }

    private static void applyAll(Parent root) {
        stopOldPopulationTimer(root);
        applyChrome(root);
        applyNavigation(root);
        applyPopulation(root);
    }

    /* ---------------- Top bar: never trust detached-root width after fullscreen ---------------- */

    private static void applyChrome(Parent root) {
        HBox bar = findHBoxWithStyle(root, "top-bar");
        if (bar == null) return;
        double width = effectiveSceneWidth(root);
        boolean roomy = width >= 1400;
        boolean veryWide = width >= 1800;
        toggleStyle(root, "layout-roomy", roomy);
        toggleStyle(root, "layout-very-wide", veryWide);

        bar.setSpacing(veryWide ? 10 : roomy ? 8 : 6);
        bar.setPadding(veryWide ? new Insets(9, 16, 9, 16)
                : roomy ? new Insets(8, 12, 8, 12)
                : new Insets(7, 9, 7, 9));

        HBox brand = findHBoxWithStyle(bar, "top-brand");
        if (brand != null) {
            double w = veryWide ? 252 : roomy ? 232 : 205;
            brand.setMinWidth(w);
            brand.setPrefWidth(w);
            brand.setMaxWidth(w);
        }

        TextField search = findTextFieldWithStyle(bar, "global-search-field");
        if (search != null) {
            search.setMinWidth(veryWide ? 300 : roomy ? 225 : 180);
            search.setPrefWidth(veryWide ? 610 : roomy ? 450 : 300);
            search.setMaxWidth(veryWide ? 760 : roomy ? 570 : 430);
            HBox.setHgrow(search, Priority.ALWAYS);
        }

        List<Button> topButtons = new ArrayList<>();
        for (Node child : bar.getChildren()) {
            if (child instanceof Button button && button.getStyleClass().contains("top-nav-button")) topButtons.add(button);
        }
        double[] compact = {104, 112, 96};
        double[] roomyWidths = {116, 126, 106};
        double[] wideWidths = {126, 138, 116};
        for (int i = 0; i < topButtons.size(); i++) {
            double[] widths = veryWide ? wideWidths : roomy ? roomyWidths : compact;
            double w = widths[Math.min(i, widths.length - 1)];
            Button button = topButtons.get(i);
            button.setMinWidth(w);
            button.setPrefWidth(w);
            button.setMaxWidth(w);
            button.setTextOverrun(OverrunStyle.CLIP);
        }

        HBox telemetry = findHBoxWithStyle(bar, "top-telemetry");
        if (telemetry != null) {
            telemetry.setSpacing(veryWide ? 5 : 3);
            for (Node child : telemetry.getChildren()) {
                if (child instanceof Labeled labeled) {
                    labeled.setMinWidth(Region.USE_PREF_SIZE);
                    labeled.setMaxWidth(Region.USE_PREF_SIZE);
                    labeled.setTextOverrun(OverrunStyle.CLIP);
                }
            }
        }
        ensureTopSpacer(bar, telemetry);
    }

    private static void ensureTopSpacer(HBox bar, HBox telemetry) {
        if (bar == null || telemetry == null) return;
        for (Node child : bar.getChildren()) {
            if (Boolean.TRUE.equals(child.getProperties().get(TOP_SPACER))) return;
            if (child.getStyleClass().contains("responsive-top-spacer")) {
                child.getProperties().put(TOP_SPACER, Boolean.TRUE);
                HBox.setHgrow(child, Priority.ALWAYS);
                return;
            }
        }
        int index = bar.getChildren().indexOf(telemetry);
        if (index < 0) return;
        Region spacer = new Region();
        spacer.getProperties().put(TOP_SPACER, Boolean.TRUE);
        spacer.getStyleClass().add("responsive-top-spacer");
        spacer.setMinWidth(0);
        spacer.setPrefWidth(0);
        spacer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().add(index, spacer);
    }

    private static double effectiveSceneWidth(Parent root) {
        Scene scene = root.getScene();
        if (scene != null && scene.getWidth() > 1) return scene.getWidth();
        return root instanceof Region region ? region.getWidth() : 0;
    }

    /* ---------------- Left navigation: buttons consume the free vertical band ---------------- */

    private static void applyNavigation(Parent root) {
        VBox navigation = findVBoxWithStyle(root, "main-navigation");
        if (navigation == null) return;
        double width = effectiveSceneWidth(root);
        boolean roomy = width >= 1400;
        boolean veryWide = width >= 1800;
        double navWidth = veryWide ? 282 : roomy ? 252 : 228;
        navigation.setMinWidth(navWidth - 12);
        navigation.setPrefWidth(navWidth);
        navigation.setMaxWidth(navWidth + 12);
        navigation.setPadding(veryWide ? new Insets(22, 15, 16, 15)
                : roomy ? new Insets(19, 13, 14, 13)
                : new Insets(16, 11, 12, 11));

        for (Node child : navigation.getChildren()) {
            if (child instanceof Button button && button.getStyleClass().contains("nav-button")) {
                button.setMinHeight(roomy ? 54 : 49);
                button.setPrefHeight(roomy ? 58 : 52);
                button.setMaxHeight(Double.MAX_VALUE);
                VBox.setVgrow(button, Priority.ALWAYS);
            } else if (child.getClass() == Region.class) {
                Region spacer = (Region) child;
                spacer.setMinHeight(7);
                spacer.setPrefHeight(7);
                spacer.setMaxHeight(7);
                VBox.setVgrow(spacer, Priority.NEVER);
            }
        }
    }

    /* ---------------- Population: no delayed second resize ---------------- */

    private static void installPopulationTabListener(TabPane tabs, Parent root) {
        if (Boolean.TRUE.equals(tabs.getProperties().get(POP_TABS))) return;
        tabs.getProperties().put(POP_TABS, Boolean.TRUE);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> requestPopulation(root));
    }

    private static void installPopulationFilterListener(Node node, Parent root) {
        if (Boolean.TRUE.equals(node.getProperties().get(POP_FILTER))) return;
        node.getProperties().put(POP_FILTER, Boolean.TRUE);
        node.visibleProperty().addListener((obs, oldValue, newValue) -> requestPopulation(root));
        node.managedProperty().addListener((obs, oldValue, newValue) -> requestPopulation(root));
    }

    private static void requestPopulation(Parent root) {
        stopOldPopulationTimer(root);
        if (root == null || Boolean.TRUE.equals(root.getProperties().get(POP_PENDING))) return;
        root.getProperties().put(POP_PENDING, Boolean.TRUE);
        Platform.runLater(() -> {
            root.getProperties().remove(POP_PENDING);
            stopOldPopulationTimer(root);
            applyPopulation(root);
        });
    }

    private static void stopOldPopulationTimer(Parent root) {
        if (root == null) return;
        Object timer = root.getProperties().get(RESPONSIVE_POPULATION_REFRESH);
        if (timer instanceof PauseTransition pause) pause.stop();
    }

    private static void applyPopulation(Parent root) {
        Region population = findRegionBySimpleName(root, "PopulationPage");
        if (population == null) return;
        TabPane tabs = findDescendant(population, TabPane.class);
        if (tabs == null) return;

        Button restore = findButtonWithStyle(population, "population-filter-restore");
        boolean collapsed = restore != null && restore.isVisible() && restore.isManaged();
        double sceneHeight = population.getScene() == null || population.getScene().getHeight() <= 0
                ? 900 : population.getScene().getHeight();
        double tabsHeight = collapsed
                ? clamp(sceneHeight - 150, 620, 820)
                : clamp(sceneHeight - 295, 545, 680);

        tabs.setMinHeight(0);
        tabs.setPrefHeight(tabsHeight);
        tabs.setMaxHeight(Double.MAX_VALUE);
        if (tabs.getParent() instanceof StackPane host) {
            host.setMinHeight(0);
            host.setPrefHeight(tabsHeight);
            host.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(host, Priority.ALWAYS);
        }

        Tab selected = tabs.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getContent() instanceof Parent content)) return;

        List<VBox> histograms = new ArrayList<>();
        collectVBoxWithStyle(content, "population-histogram-card", histograms);
        if (!histograms.isEmpty()) {
            double cardHeight = collapsed
                    ? clamp(sceneHeight * 0.49, 405, 475)
                    : clamp(sceneHeight * 0.42, 370, 430);
            double chartHeight = Math.max(285, cardHeight - 96);
            for (VBox card : histograms) {
                card.setMinHeight(0);
                card.setPrefHeight(cardHeight);
                card.setMaxHeight(cardHeight);
                card.setMinWidth(0);
                card.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(card, Priority.ALWAYS);
                BarChart<?, ?> chart = findDescendant(card, BarChart.class);
                if (chart != null) {
                    chart.setMinHeight(0);
                    chart.setPrefHeight(chartHeight);
                    chart.setMaxHeight(chartHeight);
                    chart.setMinWidth(0);
                    chart.setMaxWidth(Double.MAX_VALUE);
                    VBox.setVgrow(chart, Priority.NEVER);
                }
                if (card.getParent() instanceof HBox row) {
                    row.setFillHeight(false);
                    row.setAlignment(Pos.TOP_LEFT);
                }
            }
            return;
        }

        LineChart<?, ?> profile = findDescendant(content, LineChart.class);
        if (profile != null) {
            double chartHeight = collapsed
                    ? clamp(tabsHeight - 135, 430, 665)
                    : clamp(tabsHeight - 150, 350, 520);
            profile.setMinHeight(0);
            profile.setPrefHeight(chartHeight);
            profile.setMaxHeight(Double.MAX_VALUE);
            if (profile.getParent() instanceof VBox box) VBox.setVgrow(profile, Priority.ALWAYS);
            return;
        }

        TableView<?> table = findDescendant(content, TableView.class);
        if (table != null) {
            table.setMinHeight(320);
            table.setPrefHeight(Math.max(430, tabsHeight - 70));
            table.setMaxHeight(Double.MAX_VALUE);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /* ---------------- Every TableView: external vertical scrollbar ---------------- */

    private static void installExternalTableScrollbar(TableView<?> table) {
        if (table == null || Boolean.TRUE.equals(table.getProperties().get(TABLE_WRAPPED))) return;
        table.getProperties().put(TABLE_WRAPPED, Boolean.TRUE);
        normalizePopulationExport(table);

        ScrollBar external = new ScrollBar();
        external.setOrientation(Orientation.VERTICAL);
        external.setFocusTraversable(false);
        external.getStyleClass().add("table-external-scrollbar");
        external.setMinWidth(5.5);
        external.setPrefWidth(5.5);
        external.setMaxWidth(5.5);

        HBox shell = new HBox(5, table, external);
        shell.getStyleClass().addAll("stable-table-scroll", "external-table-scroll-shell");
        shell.setAlignment(Pos.TOP_LEFT);
        shell.setMinSize(0, 0);
        shell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        table.setMinWidth(0);
        table.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(table, Priority.ALWAYS);

        if (!replaceTableInParent(table, shell)) {
            table.getProperties().remove(TABLE_WRAPPED);
            return;
        }

        table.skinProperty().addListener((obs, oldSkin, newSkin) -> scheduleScrollbarWire(table, external));
        table.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) scheduleScrollbarWire(table, external);
        });
        scheduleScrollbarWire(table, external);
    }

    private static boolean replaceTableInParent(TableView<?> table, HBox shell) {
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
            Integer col = GridPane.getColumnIndex(table);
            Integer rowSpan = GridPane.getRowSpan(table);
            Integer colSpan = GridPane.getColumnSpan(table);
            Priority hGrow = GridPane.getHgrow(table);
            Priority vGrow = GridPane.getVgrow(table);
            HPos hAlign = GridPane.getHalignment(table);
            VPos vAlign = GridPane.getValignment(table);
            Insets margin = GridPane.getMargin(table);
            Boolean fillWidth = GridPane.isFillWidth(table);
            Boolean fillHeight = GridPane.isFillHeight(table);
            grid.getChildren().set(index, shell);
            if (row != null) GridPane.setRowIndex(shell, row);
            if (col != null) GridPane.setColumnIndex(shell, col);
            if (rowSpan != null) GridPane.setRowSpan(shell, rowSpan);
            if (colSpan != null) GridPane.setColumnSpan(shell, colSpan);
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
            shell.setLayoutX(table.getLayoutX());
            shell.setLayoutY(table.getLayoutY());
            shell.setPrefWidth(table.getWidth());
            shell.setPrefHeight(table.getHeight());
            pane.getChildren().set(index, shell);
            return true;
        }
        return false;
    }

    private static void scheduleScrollbarWire(TableView<?> table, ScrollBar external) {
        Platform.runLater(() -> {
            if (!wireScrollbar(table, external)) Platform.runLater(() -> wireScrollbar(table, external));
        });
    }

    private static boolean wireScrollbar(TableView<?> table, ScrollBar external) {
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

        Object previous = external.getProperties().get(TABLE_INNER_BAR);
        if (previous == internal) {
            collapseInternalBar(internal);
            return true;
        }
        if (previous instanceof ScrollBar oldBar) {
            try {
                external.valueProperty().unbindBidirectional(oldBar.valueProperty());
                external.minProperty().unbind();
                external.maxProperty().unbind();
                external.unitIncrementProperty().unbind();
                external.blockIncrementProperty().unbind();
                external.visibleProperty().unbind();
                external.managedProperty().unbind();
            } catch (RuntimeException ignored) {
                // A skin can disappear between two JavaFX pulses.
            }
        }

        external.getProperties().put(TABLE_INNER_BAR, internal);
        external.minProperty().bind(internal.minProperty());
        external.maxProperty().bind(internal.maxProperty());
        external.unitIncrementProperty().bind(internal.unitIncrementProperty());
        external.blockIncrementProperty().bind(internal.blockIncrementProperty());
        external.valueProperty().bindBidirectional(internal.valueProperty());
        external.visibleProperty().bind(internal.visibleProperty());
        external.managedProperty().bind(internal.visibleProperty());
        collapseInternalBar(internal);
        return true;
    }

    private static void collapseInternalBar(ScrollBar internal) {
        internal.setOpacity(0);
        internal.setMouseTransparent(true);
        internal.setMinWidth(0);
        internal.setPrefWidth(0);
        internal.setMaxWidth(0);
    }

    /* ---------------- Included GRBs export stays left of the table ---------------- */

    private static void normalizePopulationExport(TableView<?> table) {
        if (!isPopulationResultTable(table) || !(table.getParent() instanceof VBox container)) return;
        if (container.lookup(".included-export-toolbar") != null) return;
        List<Button> exports = new ArrayList<>();
        List<Node> rows = new ArrayList<>();
        FlowPane hidden = null;
        for (Node child : List.copyOf(container.getChildren())) {
            if (child instanceof FlowPane flow && flow.getStyleClass().contains("hidden-column-bar")) hidden = flow;
            if (child instanceof HBox row) {
                List<Button> inRow = new ArrayList<>();
                collectExcelButtons(row, inRow);
                if (!inRow.isEmpty()) {
                    exports.addAll(inRow);
                    rows.add(row);
                }
                FlowPane nested = findFlowPaneWithStyle(row, "hidden-column-bar");
                if (nested != null) hidden = nested;
            }
        }
        if (exports.isEmpty()) return;
        Button export = exports.get(0);
        detach(export);
        if (hidden != null) detach(hidden);
        for (Node row : rows) container.getChildren().remove(row);

        HBox exportRow = new HBox(export);
        exportRow.getStyleClass().add("population-simple-export-row");
        exportRow.setAlignment(Pos.CENTER_LEFT);
        exportRow.setMinHeight(38);
        exportRow.setPrefHeight(38);
        exportRow.setMaxWidth(Double.MAX_VALUE);
        container.getChildren().add(0, exportRow);
        if (hidden != null) container.getChildren().add(Math.min(1, container.getChildren().size()), hidden);
    }

    private static boolean isPopulationResultTable(TableView<?> table) {
        if (table == null || table.getColumns().size() < 5) return false;
        boolean grb = false, t90 = false, redshift = false, quality = false;
        for (TableColumn<?, ?> column : table.getColumns()) {
            String name = columnName(column).toLowerCase(Locale.ROOT);
            grb |= name.equals("grb");
            t90 |= name.contains("t90");
            redshift |= name.contains("redshift");
            quality |= name.contains("quality") || name.contains("qualità") || name.contains("flag");
        }
        return grb && t90 && redshift && quality;
    }

    private static String columnName(TableColumn<?, ?> column) {
        if (column == null) return "";
        String text = column.getText();
        if (text != null && !text.isBlank()) return text;
        Node graphic = column.getGraphic();
        if (graphic instanceof HBox header) {
            for (Node child : header.getChildren()) {
                if (child instanceof Labeled labeled && labeled.getText() != null && !labeled.getText().isBlank()) {
                    return labeled.getText();
                }
            }
        }
        if (graphic instanceof Labeled labeled) return labeled.getText() == null ? "" : labeled.getText();
        return "";
    }

    private static void collectExcelButtons(Node node, List<Button> result) {
        if (node instanceof Button button) {
            String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
            if (text.contains("excel") && (text.contains("export") || text.contains("esporta"))) result.add(button);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectExcelButtons(child, result);
        }
    }

    private static void detach(Node node) {
        if (node != null && node.getParent() instanceof Pane pane) pane.getChildren().remove(node);
    }

    /* ---------------- Tree helpers ---------------- */

    private static void toggleStyle(Node node, String styleClass, boolean enabled) {
        if (enabled) {
            if (!node.getStyleClass().contains(styleClass)) node.getStyleClass().add(styleClass);
        } else {
            node.getStyleClass().remove(styleClass);
        }
    }

    private static boolean hasAncestorNamed(Node node, String simpleName) {
        Node current = node;
        while (current != null) {
            if (current.getClass().getSimpleName().equals(simpleName)) return true;
            current = current.getParent();
        }
        return false;
    }

    private static Region findRegionBySimpleName(Parent root, String simpleName) {
        if (root == null) return null;
        if (root instanceof Region region && root.getClass().getSimpleName().equals(simpleName)) return region;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Region region && child.getClass().getSimpleName().equals(simpleName)) return region;
            if (child instanceof Parent parent) {
                Region found = findRegionBySimpleName(parent, simpleName);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static HBox findHBoxWithStyle(Parent root, String styleClass) {
        if (root == null) return null;
        if (root instanceof HBox box && root.getStyleClass().contains(styleClass)) return box;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof HBox box && child.getStyleClass().contains(styleClass)) return box;
            if (child instanceof Parent parent) {
                HBox found = findHBoxWithStyle(parent, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static VBox findVBoxWithStyle(Parent root, String styleClass) {
        if (root == null) return null;
        if (root instanceof VBox box && root.getStyleClass().contains(styleClass)) return box;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof VBox box && child.getStyleClass().contains(styleClass)) return box;
            if (child instanceof Parent parent) {
                VBox found = findVBoxWithStyle(parent, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static TextField findTextFieldWithStyle(Parent root, String styleClass) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof TextField field && child.getStyleClass().contains(styleClass)) return field;
            if (child instanceof Parent parent) {
                TextField found = findTextFieldWithStyle(parent, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Button findButtonWithStyle(Parent root, String styleClass) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button && child.getStyleClass().contains(styleClass)) return button;
            if (child instanceof Parent parent) {
                Button found = findButtonWithStyle(parent, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static FlowPane findFlowPaneWithStyle(Parent root, String styleClass) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof FlowPane flow && child.getStyleClass().contains(styleClass)) return flow;
            if (child instanceof Parent parent) {
                FlowPane found = findFlowPaneWithStyle(parent, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void collectVBoxWithStyle(Node node, String styleClass, List<VBox> result) {
        if (node instanceof VBox box && box.getStyleClass().contains(styleClass)) result.add(box);
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) collectVBoxWithStyle(tab.getContent(), styleClass, result);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectVBoxWithStyle(child, styleClass, result);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Node> T findDescendant(Parent root, Class<T> type) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) return (T) child;
            if (child instanceof Parent parent) {
                T found = findDescendant(parent, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
