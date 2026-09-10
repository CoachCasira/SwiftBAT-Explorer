package it.casiraghi.swiftbat.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Responsive layout layer for the application shell and Population Analysis.
 *
 * <p>The compact geometry remains the baseline for laptop-sized windows. When
 * the application has substantially more horizontal room, this enhancer uses
 * that room instead of leaving it empty: navigation, top-bar controls and
 * supporting labels become larger while the right-side status controls are
 * pushed to the real right edge.</p>
 *
 * <p>It also removes the fixed-height bottlenecks left by the original
 * Population cards. Hiding the filters therefore gives the released vertical
 * space to the selected chart instead of producing an empty area.</p>
 */
public final class ResponsiveLayoutEnhancer {
    private static final double WIDE_WIDTH = 1700.0;
    private static final String WATCHED = ResponsiveLayoutEnhancer.class.getName() + ".watched";
    private static final String TABS_WATCHED = ResponsiveLayoutEnhancer.class.getName() + ".tabsWatched";
    private static final String RESTORE_WATCHED = ResponsiveLayoutEnhancer.class.getName() + ".restoreWatched";
    private static final String TABLE_WATCHED = ResponsiveLayoutEnhancer.class.getName() + ".tableWatched";
    private static final String POPULATION_TABS_WATCHED = ResponsiveLayoutEnhancer.class.getName() + ".populationTabsWatched";
    private static final String TOP_SPACER = ResponsiveLayoutEnhancer.class.getName() + ".topSpacer";
    private static final String POPULATION_REFRESH = ResponsiveLayoutEnhancer.class.getName() + ".populationRefresh";
    private static final String EXPORT_NORMALIZED = ResponsiveLayoutEnhancer.class.getName() + ".exportNormalized";

    private ResponsiveLayoutEnhancer() {
    }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root, root);
        if (root instanceof Region region) {
            region.widthProperty().addListener((obs, oldValue, newValue) -> requestApply(root));
            region.heightProperty().addListener((obs, oldValue, newValue) -> requestApply(root));
        }
        installSceneListeners(root, root.getScene());
        root.sceneProperty().addListener((obs, oldScene, newScene) -> installSceneListeners(root, newScene));
        Platform.runLater(() -> apply(root));
    }

    private static void installSceneListeners(Parent root, Scene scene) {
        if (scene == null) return;
        String key = ResponsiveLayoutEnhancer.class.getName() + ".scene." + System.identityHashCode(scene);
        if (Boolean.TRUE.equals(root.getProperties().get(key))) return;
        root.getProperties().put(key, Boolean.TRUE);
        scene.widthProperty().addListener((obs, oldValue, newValue) -> requestApply(root));
        scene.heightProperty().addListener((obs, oldValue, newValue) -> requestApply(root));
    }

    private static void watch(Node node, Parent root) {
        if (node == null) return;

        if (node instanceof Button button && button.getStyleClass().contains("population-filter-restore")) {
            installRestoreWatcher(button, root);
        }
        if (node instanceof TableView<?> table && isPopulationResultTable(table)) {
            installPopulationTableWatcher(table, root);
        }
        if (node instanceof TabPane tabs) {
            installTabWatcher(tabs, root);
        }

        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) {
                    watch(added, root);
                }
            }
            requestApply(root);
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
            watch(child, root);
        }
    }

    private static void installTabWatcher(TabPane tabs, Parent root) {
        if (Boolean.TRUE.equals(tabs.getProperties().get(TABS_WATCHED))) return;
        tabs.getProperties().put(TABS_WATCHED, Boolean.TRUE);
        for (Tab tab : tabs.getTabs()) {
            if (tab.getContent() != null) watch(tab.getContent(), root);
        }
        tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Tab tab : change.getAddedSubList()) {
                    if (tab.getContent() != null) watch(tab.getContent(), root);
                }
            }
            requestApply(root);
        });
        if (hasAncestorNamed(tabs, "PopulationPage")
                && !Boolean.TRUE.equals(tabs.getProperties().get(POPULATION_TABS_WATCHED))) {
            tabs.getProperties().put(POPULATION_TABS_WATCHED, Boolean.TRUE);
            tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) ->
                    requestPopulationApply(root));
        }
    }

    private static void installRestoreWatcher(Button restore, Parent root) {
        if (Boolean.TRUE.equals(restore.getProperties().get(RESTORE_WATCHED))) return;
        restore.getProperties().put(RESTORE_WATCHED, Boolean.TRUE);
        restore.visibleProperty().addListener((obs, oldValue, newValue) -> requestPopulationApply(root));
        restore.managedProperty().addListener((obs, oldValue, newValue) -> requestPopulationApply(root));
    }

    private static void installPopulationTableWatcher(TableView<?> table, Parent root) {
        if (Boolean.TRUE.equals(table.getProperties().get(TABLE_WATCHED))) return;
        table.getProperties().put(TABLE_WATCHED, Boolean.TRUE);
        table.widthProperty().addListener((obs, oldValue, newValue) -> requestApply(root));
        table.getColumns().addListener((ListChangeListener<TableColumn<?, ?>>) change -> requestApply(root));
        Platform.runLater(() -> {
            normalizePopulationExport(table);
            requestApply(root);
        });
        PauseTransition delayed = new PauseTransition(Duration.millis(180));
        delayed.setOnFinished(event -> {
            normalizePopulationExport(table);
            requestApply(root);
        });
        delayed.playFromStart();
    }

    private static void requestApply(Parent root) {
        Platform.runLater(() -> apply(root));
    }

    private static void requestPopulationApply(Parent root) {
        requestApply(root);
        PauseTransition pause;
        Object stored = root.getProperties().get(POPULATION_REFRESH);
        if (stored instanceof PauseTransition existing) {
            pause = existing;
        } else {
            pause = new PauseTransition(Duration.millis(360));
            pause.setOnFinished(event -> apply(root));
            root.getProperties().put(POPULATION_REFRESH, pause);
        }
        pause.playFromStart();
    }

    private static void apply(Parent root) {
        ensureTopSpacer(root);
        double width = root instanceof Region region && region.getWidth() > 0
                ? region.getWidth() : root.getScene() == null ? 0 : root.getScene().getWidth();
        boolean wide = width >= WIDE_WIDTH;
        setStyle(root, "layout-wide", wide);
        applyShellGeometry(root, wide);

        Region population = findRegionBySimpleName(root, "PopulationPage");
        if (population != null) {
            applyPopulationLayout(population, wide);
            for (TableView<?> table : populationTables(population)) {
                normalizePopulationExport(table);
                resizePopulationColumns(table, wide);
            }
        }
    }

    /* ---------------- Shell ---------------- */

    private static void ensureTopSpacer(Parent root) {
        HBox topBar = findHBoxWithStyle(root, "top-bar");
        if (topBar == null) return;
        for (Node child : topBar.getChildren()) {
            if (Boolean.TRUE.equals(child.getProperties().get(TOP_SPACER))) return;
        }
        int telemetryIndex = -1;
        for (int index = 0; index < topBar.getChildren().size(); index++) {
            if (topBar.getChildren().get(index).getStyleClass().contains("top-telemetry")) {
                telemetryIndex = index;
                break;
            }
        }
        if (telemetryIndex < 0) return;
        Region spacer = new Region();
        spacer.getProperties().put(TOP_SPACER, Boolean.TRUE);
        spacer.getStyleClass().add("responsive-top-spacer");
        spacer.setMinWidth(0);
        spacer.setPrefWidth(0);
        spacer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().add(telemetryIndex, spacer);
    }

    private static void applyShellGeometry(Parent root, boolean wide) {
        VBox navigation = findVBoxWithStyle(root, "main-navigation");
        if (navigation != null) {
            navigation.setMinWidth(wide ? 258 : 214);
            navigation.setPrefWidth(wide ? 276 : 228);
            navigation.setMaxWidth(wide ? 292 : 242);
            navigation.setPadding(wide
                    ? new Insets(24, 16, 18, 16)
                    : new Insets(18, 12, 14, 12));
            VBox footer = findVBoxWithStyle(navigation, "nav-footer-card");
            if (footer != null) footer.setPadding(wide ? new Insets(15) : new Insets(11));
        }

        HBox topBar = findHBoxWithStyle(root, "top-bar");
        if (topBar == null) return;
        TextField search = findTextFieldWithStyle(topBar, "global-search-field");
        if (search != null) {
            search.setMinWidth(wide ? 340 : 245);
            search.setPrefWidth(wide ? 560 : 410);
            search.setMaxWidth(wide ? 720 : 520);
            HBox.setHgrow(search, Priority.ALWAYS);
        }
        HBox brand = findHBoxWithStyle(topBar, "top-brand");
        if (brand != null) {
            brand.setMinWidth(wide ? 250 : 220);
            brand.setPrefWidth(wide ? 276 : 232);
            brand.setMaxWidth(wide ? 305 : 242);
        }
        for (Node child : topBar.getChildren()) {
            if (!(child instanceof Button button) || !button.getStyleClass().contains("top-nav-button")) continue;
            double compact = compactTopButtonWidth(button);
            double expanded = expandedTopButtonWidth(button);
            button.setMinWidth(wide ? expanded : compact);
            button.setPrefWidth(wide ? expanded : compact);
        }
    }

    private static double compactTopButtonWidth(Button button) {
        String text = normalized(button.getText());
        if (text.contains("tool") || text.contains("strument")) return 88;
        if (text.contains("guide") || text.contains("guida")) return 68;
        return 78;
    }

    private static double expandedTopButtonWidth(Button button) {
        String text = normalized(button.getText());
        if (text.contains("tool") || text.contains("strument")) return 116;
        if (text.contains("guide") || text.contains("guida")) return 100;
        return 108;
    }

    /* ---------------- Population sizing ---------------- */

    private static void applyPopulationLayout(Region population, boolean wide) {
        TabPane tabs = findPopulationTabs(population);
        if (tabs == null) return;
        Button restore = findButtonWithStyle(population, "population-filter-restore");
        boolean collapsed = restore != null && restore.isVisible() && restore.isManaged();

        double pageHeight = population.getHeight();
        if (pageHeight < 520 && population.getScene() != null) {
            pageHeight = Math.max(pageHeight, population.getScene().getHeight() - 74);
        }
        double targetTabs;
        if (collapsed) {
            targetTabs = Math.max(640, pageHeight - (wide ? 105 : 125));
            targetTabs = Math.min(targetTabs, 940);
        } else {
            targetTabs = Math.max(560, Math.min(wide ? 690 : 650, pageHeight - 285));
        }
        tabs.setMinHeight(targetTabs);
        tabs.setPrefHeight(targetTabs);
        tabs.setMaxHeight(Double.MAX_VALUE);

        if (tabs.getParent() instanceof StackPane host) {
            host.setMinHeight(targetTabs);
            host.setPrefHeight(targetTabs);
            host.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(host, Priority.ALWAYS);
        }

        for (Tab tab : tabs.getTabs()) {
            Node content = tab.getContent();
            if (content == null) continue;
            LineChart<?, ?> line = findPopulationLineChart(content);
            if (line != null) {
                resizePopulationProfile(line, targetTabs, collapsed);
                continue;
            }
            List<VBox> cards = new ArrayList<>();
            collectVBoxWithStyle(content, "population-histogram-card", cards);
            if (!cards.isEmpty()) {
                resizePopulationHistograms(cards, targetTabs, collapsed);
                continue;
            }
            TableView<?> table = findPopulationTable(content);
            if (table != null) {
                table.setMinHeight(Math.max(390, targetTabs - 100));
                table.setPrefHeight(Math.max(430, targetTabs - 80));
                table.setMaxHeight(Double.MAX_VALUE);
                if (table.getParent() instanceof VBox box) VBox.setVgrow(table, Priority.ALWAYS);
            }
        }
    }

    private static void resizePopulationProfile(LineChart<?, ?> chart, double tabsHeight, boolean collapsed) {
        double chartHeight = Math.max(collapsed ? 455 : 350, tabsHeight - 165);
        chart.setMinHeight(collapsed ? 380 : 300);
        chart.setPrefHeight(chartHeight);
        chart.setMaxHeight(Double.MAX_VALUE);
        if (chart.getParent() instanceof VBox chartArea) {
            chartArea.setMinHeight(0);
            chartArea.setPrefHeight(chartHeight + 60);
            chartArea.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(chart, Priority.ALWAYS);
            if (chartArea.getParent() instanceof HBox body) {
                body.setMinHeight(0);
                body.setPrefHeight(Math.max(chartHeight + 65, tabsHeight - 85));
                body.setMaxHeight(Double.MAX_VALUE);
                if (body.getParent() instanceof VBox card) {
                    card.setMinHeight(0);
                    card.setPrefHeight(Math.max(chartHeight + 105, tabsHeight - 45));
                    card.setMaxHeight(Double.MAX_VALUE);
                    VBox.setVgrow(body, Priority.ALWAYS);
                }
            }
        }
    }

    private static void resizePopulationHistograms(List<VBox> cards, double tabsHeight, boolean collapsed) {
        double cardHeight = Math.max(collapsed ? 515 : 430, tabsHeight - 88);
        for (VBox card : cards) {
            card.setMinHeight(collapsed ? 455 : 400);
            card.setPrefHeight(cardHeight);
            card.setMaxHeight(Double.MAX_VALUE);
            BarChart<?, ?> chart = findBarChart(card);
            if (chart == null) continue;
            chart.setMinHeight(collapsed ? 370 : 300);
            chart.setPrefHeight(Math.max(330, cardHeight - 105));
            chart.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(chart, Priority.ALWAYS);
            if (chart.getParent() instanceof VBox content) {
                content.setMinHeight(0);
                content.setMaxHeight(Double.MAX_VALUE);
                VBox.setVgrow(content, Priority.ALWAYS);
            }
        }
    }

    /* ---------------- Included GRBs table ---------------- */

    private static void normalizePopulationExport(TableView<?> table) {
        if (!isPopulationResultTable(table)) return;
        if (!table.getStyleClass().contains("population-result-table")) {
            table.getStyleClass().add("population-result-table");
        }
        if (!(table.getParent() instanceof VBox container)) return;

        Button selectedExport = null;
        FlowPane hiddenBar = null;
        List<Node> exportRows = new ArrayList<>();
        for (Node child : List.copyOf(container.getChildren())) {
            if (child instanceof FlowPane flow && flow.getStyleClass().contains("hidden-column-bar")) {
                hiddenBar = flow;
            }
            if (!(child instanceof HBox row)) continue;
            List<Button> exports = new ArrayList<>();
            collectExcelButtons(row, exports);
            if (exports.isEmpty()) continue;
            exportRows.add(row);
            for (Button candidate : exports) {
                if (selectedExport == null || candidate.getStyleClass().contains("excel-export-button")) {
                    selectedExport = candidate;
                }
            }
            FlowPane nested = findFlowPaneWithStyle(row, "hidden-column-bar");
            if (nested != null) hiddenBar = nested;
        }

        if (Boolean.TRUE.equals(container.getProperties().get(EXPORT_NORMALIZED)) && exportRows.size() <= 1) {
            return;
        }

        if (selectedExport == null) {
            selectedExport = UiFactory.button("", "ghost-button");
            I18n.setText(selectedExport, "Esporta Excel", "Export Excel");
            Button export = selectedExport;
            export.setOnAction(event -> ExportSupport.exportTableExcel(
                    export, table, "population_included_grbs.xlsx", I18n.t("GRB inclusi")));
        }

        detach(selectedExport);
        if (hiddenBar != null) detach(hiddenBar);
        for (Node row : exportRows) container.getChildren().remove(row);
        container.getChildren().removeIf(node -> node instanceof HBox row
                && row.getStyleClass().contains("population-simple-export-row"));

        selectedExport.getStyleClass().remove("secondary-button");
        if (!selectedExport.getStyleClass().contains("ghost-button")) selectedExport.getStyleClass().add("ghost-button");
        if (!selectedExport.getStyleClass().contains("excel-export-button")) selectedExport.getStyleClass().add("excel-export-button");
        if (!selectedExport.getStyleClass().contains("population-table-export")) selectedExport.getStyleClass().add("population-table-export");
        selectedExport.setMinWidth(132);

        HBox row = new HBox(selectedExport);
        row.getStyleClass().add("population-simple-export-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(40);
        row.setPrefHeight(40);
        row.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(row, Priority.NEVER);
        container.getChildren().add(0, row);
        if (hiddenBar != null) {
            container.getChildren().remove(hiddenBar);
            container.getChildren().add(Math.min(1, container.getChildren().size()), hiddenBar);
        }
        container.getProperties().put(EXPORT_NORMALIZED, Boolean.TRUE);
        table.refresh();
    }

    private static void resizePopulationColumns(TableView<?> table, boolean wide) {
        if (!isPopulationResultTable(table)) return;
        if (!table.getStyleClass().contains("population-result-table")) {
            table.getStyleClass().add("population-result-table");
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(wide ? 40 : 37);
        double width = table.getWidth() > 900 ? table.getWidth() - 6 : (wide ? 1450 : 980);
        for (TableColumn<?, ?> column : table.getColumns()) {
            String name = visibleColumnName(column).toLowerCase(Locale.ROOT);
            double share;
            double minimum;
            if (name.equals("grb")) {
                share = 0.135; minimum = wide ? 150 : 120;
            } else if (name.contains("t90")) {
                share = 0.130; minimum = wide ? 145 : 110;
            } else if (name.contains("classe") || name.equals("class")) {
                share = 0.190; minimum = wide ? 225 : 190;
            } else if (name.contains("redshift")) {
                share = 0.160; minimum = wide ? 180 : 145;
            } else if (name.contains("copertura") || name.contains("coverage")) {
                share = 0.145; minimum = wide ? 160 : 125;
            } else if (name.contains("qualità") || name.contains("quality") || name.contains("flag")) {
                share = 0.240; minimum = wide ? 285 : 240;
            } else {
                share = 1.0 / Math.max(1, table.getColumns().size()); minimum = 120;
            }
            column.setMinWidth(minimum);
            column.setPrefWidth(Math.max(minimum, width * share));
            column.setMaxWidth(Double.MAX_VALUE);
        }
        table.refresh();
    }

    private static void collectExcelButtons(Node node, List<Button> result) {
        if (node instanceof Button button && isExcelExport(button)) result.add(button);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectExcelButtons(child, result);
        }
    }

    private static boolean isExcelExport(Button button) {
        String text = normalized(button.getText());
        return text.contains("excel") && (text.contains("export") || text.contains("esporta"));
    }

    private static void detach(Node node) {
        if (node == null || node.getParent() == null) return;
        if (node.getParent() instanceof Pane pane) pane.getChildren().remove(node);
    }

    /* ---------------- Search helpers ---------------- */

    private static List<TableView<?>> populationTables(Parent root) {
        List<TableView<?>> result = new ArrayList<>();
        collectPopulationTables(root, result);
        return result;
    }

    private static void collectPopulationTables(Node node, List<TableView<?>> result) {
        if (node instanceof TableView<?> table && isPopulationResultTable(table)) result.add(table);
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) collectPopulationTables(tab.getContent(), result);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectPopulationTables(child, result);
        }
    }

    private static boolean isPopulationResultTable(TableView<?> table) {
        if (table == null || table.getColumns().size() < 5) return false;
        boolean grb = false, t90 = false, redshift = false, coverage = false, quality = false;
        for (TableColumn<?, ?> column : table.getColumns()) {
            String name = visibleColumnName(column).toLowerCase(Locale.ROOT);
            grb |= name.equals("grb");
            t90 |= name.contains("t90");
            redshift |= name.contains("redshift");
            coverage |= name.contains("copertura") || name.contains("coverage");
            quality |= name.contains("qualità") || name.contains("quality") || name.contains("flag");
        }
        return grb && t90 && redshift && coverage && quality;
    }

    private static String visibleColumnName(TableColumn<?, ?> column) {
        String text = column.getText();
        if (text != null && !text.isBlank()) return text.trim();
        String graphic = firstLabelText(column.getGraphic());
        if (!graphic.isBlank()) return graphic;
        String exported = TablePreferences.exportColumnName(column);
        return exported == null ? "" : exported.trim();
    }

    private static String firstLabelText(Node node) {
        if (node == null) return "";
        if (node instanceof Label label && label.getText() != null && !label.getText().isBlank()) {
            return label.getText().trim();
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                String result = firstLabelText(child);
                if (!result.isBlank()) return result;
            }
        }
        return "";
    }

    private static TabPane findPopulationTabs(Parent root) {
        if (root instanceof TabPane tabs && tabs.getStyleClass().contains("main-tabs")) return tabs;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof TabPane tabs && tabs.getStyleClass().contains("main-tabs")) return tabs;
            if (child instanceof Parent parent) {
                TabPane nested = findPopulationTabs(parent);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static TableView<?> findPopulationTable(Node node) {
        if (node instanceof TableView<?> table && isPopulationResultTable(table)) return table;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TableView<?> nested = findPopulationTable(child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static LineChart<?, ?> findPopulationLineChart(Node node) {
        if (node instanceof LineChart<?, ?> chart && chart.getStyleClass().contains("population-chart")) return chart;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                LineChart<?, ?> nested = findPopulationLineChart(child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static BarChart<?, ?> findBarChart(Node node) {
        if (node instanceof BarChart<?, ?> chart) return chart;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                BarChart<?, ?> nested = findBarChart(child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static void collectVBoxWithStyle(Node node, String style, List<VBox> result) {
        if (node instanceof VBox box && box.getStyleClass().contains(style)) result.add(box);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectVBoxWithStyle(child, style, result);
        }
    }

    private static Region findRegionBySimpleName(Node node, String simpleName) {
        if (node instanceof Region region && node.getClass().getSimpleName().equals(simpleName)) return region;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Region nested = findRegionBySimpleName(child, simpleName);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static HBox findHBoxWithStyle(Node node, String style) {
        if (node instanceof HBox box && box.getStyleClass().contains(style)) return box;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                HBox nested = findHBoxWithStyle(child, style);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static VBox findVBoxWithStyle(Node node, String style) {
        if (node instanceof VBox box && box.getStyleClass().contains(style)) return box;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                VBox nested = findVBoxWithStyle(child, style);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static TextField findTextFieldWithStyle(Node node, String style) {
        if (node instanceof TextField field && field.getStyleClass().contains(style)) return field;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TextField nested = findTextFieldWithStyle(child, style);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static Button findButtonWithStyle(Node node, String style) {
        if (node instanceof Button button && button.getStyleClass().contains(style)) return button;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button nested = findButtonWithStyle(child, style);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static FlowPane findFlowPaneWithStyle(Node node, String style) {
        if (node instanceof FlowPane flow && flow.getStyleClass().contains(style)) return flow;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                FlowPane nested = findFlowPaneWithStyle(child, style);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static boolean hasAncestorNamed(Node node, String simpleName) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (current.getClass().getSimpleName().equals(simpleName)) return true;
            current = current.getParent();
        }
        return false;
    }

    private static void setStyle(Parent root, String style, boolean enabled) {
        if (enabled) {
            if (!root.getStyleClass().contains(style)) root.getStyleClass().add(style);
        } else {
            root.getStyleClass().remove(style);
        }
    }

    private static String normalized(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }
}
