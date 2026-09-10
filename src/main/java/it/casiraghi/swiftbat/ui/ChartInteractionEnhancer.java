package it.casiraghi.swiftbat.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Shared interactions for the scientific JavaFX charts and population result table. */
public final class ChartInteractionEnhancer {
    private static final String WATCHED = ChartInteractionEnhancer.class.getName() + ".watched";
    private static final String LINE_DONE = ChartInteractionEnhancer.class.getName() + ".lineDone";
    private static final String BAR_DONE = ChartInteractionEnhancer.class.getName() + ".barDone";
    private static final String EXPORT_DONE = ChartInteractionEnhancer.class.getName() + ".exportDone";
    private static final String FILTER_DONE = ChartInteractionEnhancer.class.getName() + ".filterDone";
    private static final String TABLE_WATCHED = ChartInteractionEnhancer.class.getName() + ".tableWatched";
    private static final String TABLE_DONE = ChartInteractionEnhancer.class.getName() + ".tableDone";
    private static final String FOCUS = ChartInteractionEnhancer.class.getName() + ".focus";
    private static final String NORMAL_TABS_HEIGHT = ChartInteractionEnhancer.class.getName() + ".normalTabsHeight";

    private static final double HIT_RADIUS = 18.0;
    private static final DecimalFormat NUMBER_FORMAT;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        NUMBER_FORMAT = new DecimalFormat("0.#####", symbols);
    }

    private ChartInteractionEnhancer() { }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> scan(root));
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node added : change.getAddedSubList()) watch(added);
                }
            }
            Platform.runLater(() -> scan(parent));
        });
        for (Node child : parent.getChildrenUnmodifiable()) watch(child);
    }

    private static void scan(Node node) {
        if (node == null) return;
        enhance(node);
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) scan(child);
        }
    }

    private static void enhance(Node node) {
        if (node instanceof LineChart<?, ?> lineChart) enhanceLineChart(lineChart);
        else if (node instanceof BarChart<?, ?> barChart) enhanceBarChart(barChart);
        else if (node instanceof TableView<?> table) enhanceTableCandidate(table);
        if (node instanceof Region region && region.getStyleClass().contains("population-filter-card")) enhancePopulationFilters(region);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void enhanceLineChart(LineChart<?, ?> rawChart) {
        if (Boolean.TRUE.equals(rawChart.getProperties().get(LINE_DONE))) return;
        rawChart.getProperties().put(LINE_DONE, Boolean.TRUE);
        LineChart chart = rawChart;
        Tooltip tooltip = new Tooltip();
        tooltip.setAutoHide(false);
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setHideDelay(Duration.ZERO);

        chart.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            boolean focusedOnly = isPopulationChart(chart) && CurveInteractionLinkEnhancer.populationFocusLocked();
            SampleHit hit = nearest(chart, event.getX(), event.getY(), focusedOnly);
            if (hit == null) { tooltip.hide(); return; }
            String xLabel = chart.getXAxis().getLabel();
            String yLabel = chart.getYAxis().getLabel();
            String seriesName = hit.series().getName() == null ? I18n.dynamic("Curva", "Curve") : hit.series().getName();
            tooltip.setText(seriesName + "\n"
                    + (xLabel == null || xLabel.isBlank() ? "X" : xLabel) + ": " + format(hit.data().getXValue()) + "\n"
                    + (yLabel == null || yLabel.isBlank() ? "Y" : yLabel) + ": " + format(hit.data().getYValue()));
            if (!tooltip.isShowing()) tooltip.show(chart, event.getScreenX() + 14, event.getScreenY() + 14);
            else { tooltip.setAnchorX(event.getScreenX() + 14); tooltip.setAnchorY(event.getScreenY() + 14); }
        });
        chart.addEventHandler(MouseEvent.MOUSE_EXITED, event -> tooltip.hide());
        chart.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (isPopulationChart(chart) && CurveInteractionLinkEnhancer.populationFocusLocked()) {
                tooltip.hide();
                event.consume();
                return;
            }
            SampleHit hit = nearest(chart, event.getX(), event.getY(), false);
            if (event.getClickCount() >= 2) {
                tooltip.hide();
                if (hit == null && !focusedSeries(chart).isEmpty()) {
                    focusedSeries(chart).clear();
                    applyFocus(chart);
                } else openThreeD(chart);
                event.consume();
                return;
            }
            if (event.getClickCount() == 1 && hit != null && selectable(hit.series())) {
                Set<XYChart.Series> selected = focusedSeries(chart);
                if (!selected.add(hit.series())) selected.remove(hit.series());
                applyFocus(chart);
                event.consume();
            }
        });
        chart.getData().addListener((ListChangeListener<XYChart.Series>) change -> Platform.runLater(() -> applyFocus(chart)));
        Platform.runLater(() -> applyFocus(chart));
        installPopulationExport(chart);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void enhanceBarChart(BarChart<?, ?> rawChart) {
        if (Boolean.TRUE.equals(rawChart.getProperties().get(BAR_DONE))) return;
        rawChart.getProperties().put(BAR_DONE, Boolean.TRUE);
        BarChart chart = rawChart;
        chart.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() >= 2) { openThreeD(chart); event.consume(); }
        });
        if (findAncestorWithStyle(chart, "population-histogram-card") != null) {
            chart.setMinHeight(300); chart.setPrefHeight(340); chart.setMaxHeight(Double.MAX_VALUE);
        }
        installPopulationExport(chart);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SampleHit nearest(LineChart chart, double mouseX, double mouseY, boolean focusedOnly) {
        if (chart.getScene() == null) return null;
        Axis xAxis = chart.getXAxis(); Axis yAxis = chart.getYAxis();
        Set<XYChart.Series> focus = focusedOnly ? focusedSeries(chart) : Set.of();
        double best = HIT_RADIUS * HIT_RADIUS; SampleHit result = null;
        for (Object rawSeries : chart.getData()) {
            XYChart.Series series = (XYChart.Series) rawSeries;
            if (focusedOnly && !focus.contains(series)) continue;
            if (series.getData() == null || series.getData().isEmpty()) continue;
            for (Object rawData : series.getData()) {
                XYChart.Data data = (XYChart.Data) rawData;
                Object xValue = data.getXValue(); Object yValue = data.getYValue();
                if (xValue == null || yValue == null) continue;
                double xDisplay; double yDisplay;
                try { xDisplay = xAxis.getDisplayPosition(xValue); yDisplay = yAxis.getDisplayPosition(yValue); }
                catch (RuntimeException ignored) { continue; }
                if (!Double.isFinite(xDisplay) || !Double.isFinite(yDisplay)) continue;
                Point2D xScene = xAxis.localToScene(xDisplay, 0); Point2D yScene = yAxis.localToScene(0, yDisplay);
                if (xScene == null || yScene == null) continue;
                Point2D point = chart.sceneToLocal(xScene.getX(), yScene.getY());
                double dx = point.getX() - mouseX; double dy = point.getY() - mouseY; double distance = dx * dx + dy * dy;
                if (distance < best) { best = distance; result = new SampleHit(series, data); }
            }
        }
        return result;
    }

    private static boolean isPopulationChart(LineChart<?, ?> chart) {
        return chart != null && chart.getStyleClass().contains("population-chart");
    }

    @SuppressWarnings("rawtypes") private static boolean selectable(XYChart.Series series) {
        String name = series.getName(); return name == null || !name.toLowerCase(Locale.ROOT).startsWith("trigger");
    }
    @SuppressWarnings("rawtypes") private static Set<XYChart.Series> focusedSeries(LineChart chart) {
        Object stored = chart.getProperties().get(FOCUS);
        if (stored instanceof Set<?> set) return (Set<XYChart.Series>) set;
        Set<XYChart.Series> created = new LinkedHashSet<>(); chart.getProperties().put(FOCUS, created); return created;
    }
    @SuppressWarnings("rawtypes") private static void applyFocus(LineChart chart) {
        Set<XYChart.Series> selected = focusedSeries(chart); selected.removeIf(series -> !chart.getData().contains(series));
        for (Object rawSeries : chart.getData()) {
            XYChart.Series series = (XYChart.Series) rawSeries; Node node = series.getNode(); if (node == null) continue;
            String name = series.getName(); boolean trigger = name != null && name.toLowerCase(Locale.ROOT).startsWith("trigger");
            node.setOpacity(selected.isEmpty() || selected.contains(series) ? 1.0 : trigger ? 0.50 : 0.09);
        }
    }

    private static boolean openThreeD(Node source) {
        Node current = source;
        for (int depth = 0; current != null && depth < 12; depth++, current = current.getParent()) {
            if (current instanceof TabPane tabs) {
                for (Tab tab : tabs.getTabs()) {
                    String text = tab.getText() == null ? "" : tab.getText().toLowerCase(Locale.ROOT);
                    if (text.contains("3d")) {
                        tabs.getSelectionModel().select(tab);
                        Platform.runLater(() -> openTabFullscreen(tab));
                        return true;
                    }
                }
            }
            if (current instanceof Parent parent) {
                Button local = findThreeDButton(parent, true);
                if (local != null) { local.fire(); return true; }
            }
            if (current.getStyleClass().contains("page-root")) break;
        }
        return false;
    }

    private static void openTabFullscreen(Tab tab) {
        Node content = tab == null ? null : tab.getContent();
        if (!(content instanceof Parent parent)) return;
        Button fullscreen = findFullscreenButton(parent);
        if (fullscreen != null && !fullscreen.isDisabled()) fullscreen.fire();
    }

    private static Button findFullscreenButton(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.isVisible() && button.isManaged()) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
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

    private static Button findThreeDButton(Parent root, boolean requireEnabled) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                if (text.contains("3d") && button.isVisible() && button.isManaged() && (!requireEnabled || !button.isDisabled())) return button;
            }
            if (child instanceof Parent parent) { Button nested = findThreeDButton(parent, requireEnabled); if (nested != null) return nested; }
        }
        return null;
    }

    private static void installPopulationExport(XYChart<?, ?> chart) {
        if (Boolean.TRUE.equals(chart.getProperties().get(EXPORT_DONE))) return;
        Parent card = findAncestorWithStyle(chart, "population-histogram-card");
        if (card == null && chart.getStyleClass().contains("population-chart")) card = findAncestorWithStyle(chart, "population-chart-card");
        if (card == null) return;
        Button threeD = findThreeDButton(card, false);
        if (threeD == null || !(threeD.getParent() instanceof HBox actions)) return;
        chart.getProperties().put(EXPORT_DONE, Boolean.TRUE);
        Button export = UiFactory.button("", "ghost-button"); I18n.setText(export, "Esporta PNG", "Export PNG");
        String title = chart.getTitle() == null || chart.getTitle().isBlank() ? "population_chart" : chart.getTitle();
        export.setOnAction(event -> ExportSupport.exportPng(export, chart,
                title.replaceAll("[^A-Za-z0-9._-]+", "_") + "_2d.png"));
        int index = actions.getChildren().indexOf(threeD); actions.getChildren().add(Math.max(0, index), export);
        if (card instanceof Region region && chart instanceof BarChart<?, ?>) {
            region.setMinHeight(400); region.setPrefHeight(420); region.setMaxHeight(Double.MAX_VALUE);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void enhanceTableCandidate(TableView<?> rawTable) {
        TableView table = rawTable;
        if (!Boolean.TRUE.equals(table.getProperties().get(TABLE_WATCHED))) {
            table.getProperties().put(TABLE_WATCHED, Boolean.TRUE);
            table.getColumns().addListener((ListChangeListener<TableColumn>) change ->
                    Platform.runLater(() -> enhanceTableCandidate(table)));
        }
        if (Boolean.TRUE.equals(table.getProperties().get(TABLE_DONE)) || !isPopulationResultTable(table)) return;
        if (!(table.getParent() instanceof VBox box) || box.getChildren().isEmpty()) return;

        table.getProperties().put(TABLE_DONE, Boolean.TRUE);
        Button export = UiFactory.button("", "ghost-button");
        I18n.setText(export, "Esporta Excel", "Export Excel");
        export.setOnAction(event -> ExportSupport.exportTableExcel(
                export, table, "population_included_grbs.xlsx", "GRB inclusi"));

        // The old column-preference strip became a wide blue outlined box with the
        // export action pushed to the far right. The Included GRBs tab now keeps only
        // the actual export action, aligned with the table content on the left.
        box.getChildren().remove(0);
        HBox toolbar = new HBox(8, export);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setMinWidth(0);
        toolbar.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(0, toolbar);
    }

    private static boolean isPopulationResultTable(TableView<?> table) {
        if (table.getColumns().size() < 5) return false;
        boolean grb = false, t90 = false, redshift = false, quality = false;
        for (TableColumn<?, ?> column : table.getColumns()) {
            String text = column.getText() == null ? "" : column.getText().toLowerCase(Locale.ROOT);
            grb |= text.equals("grb");
            t90 |= text.contains("t90");
            redshift |= text.contains("redshift");
            quality |= text.contains("qualità") || text.contains("quality") || text.contains("flag");
        }
        return grb && t90 && redshift && quality;
    }

    private static Parent findAncestorWithStyle(Node node, String styleClass) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (current.getStyleClass().contains(styleClass) && current instanceof Parent parent) return parent;
            current = current.getParent();
        }
        return null;
    }

    private static void enhancePopulationFilters(Region card) {
        if (Boolean.TRUE.equals(card.getProperties().get(FILTER_DONE))) return;
        if (!(card.getParent() instanceof VBox page)) return;
        card.getProperties().put(FILTER_DONE, Boolean.TRUE);
        TabPane tabs = findDescendant(page, TabPane.class);
        if (tabs != null) {
            double normalHeight = Math.max(640, tabs.getPrefHeight()); tabs.getProperties().put(NORMAL_TABS_HEIGHT, normalHeight);
            tabs.setMinHeight(560); tabs.setPrefHeight(normalHeight); tabs.setMaxHeight(Double.MAX_VALUE);
        }
        Button restore = UiFactory.button("", "ghost-button"); I18n.setText(restore, "Mostra filtri", "Show filters");
        restore.getStyleClass().add("population-filter-restore"); restore.setVisible(false); restore.setManaged(false); restore.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(restore, Priority.NEVER);
        int index = page.getChildren().indexOf(card); if (index >= 0) page.getChildren().add(index + 1, restore);
        card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() != 1 || event.isConsumed()) return;
            Node target = event.getTarget() instanceof Node n ? n : null;
            if (containsInteractiveControl(target, card)) return;
            collapseFilters(card, restore, tabs); event.consume();
        });
        restore.setOnAction(event -> expandFilters(card, restore, tabs));
    }

    private static boolean containsInteractiveControl(Node target, Node boundary) {
        Node current = target;
        while (current != null && current != boundary) { if (current instanceof Control) return true; current = current.getParent(); }
        return false;
    }

    private static void collapseFilters(Region card, Button restore, TabPane tabs) {
        if (!card.isVisible() || !card.isManaged()) return;
        double height = Math.max(card.getHeight(), card.prefHeight(-1)); card.setMinHeight(0); card.setMaxHeight(height);
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(card.maxHeightProperty(), 0, Interpolator.EASE_BOTH),
                new KeyValue(card.opacityProperty(), 0, Interpolator.EASE_BOTH),
                new KeyValue(card.translateYProperty(), -10, Interpolator.EASE_BOTH)));
        if (tabs != null) {
            double normal = normalTabsHeight(tabs);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(260),
                    new KeyValue(tabs.prefHeightProperty(), normal + Math.min(260, Math.max(150, height * 0.75)), Interpolator.EASE_BOTH)));
        }
        timeline.setOnFinished(event -> { card.setVisible(false); card.setManaged(false); restore.setVisible(true); restore.setManaged(true); });
        timeline.play();
    }

    private static void expandFilters(Region card, Button restore, TabPane tabs) {
        restore.setVisible(false); restore.setManaged(false); card.setVisible(true); card.setManaged(true); card.applyCss();
        Parent parent = card.getParent(); if (parent != null) parent.layout();
        double target = Math.max(170, card.prefHeight(-1)); card.setOpacity(0); card.setTranslateY(-10); card.setMinHeight(0); card.setMaxHeight(0);
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(card.maxHeightProperty(), target, Interpolator.EASE_BOTH),
                new KeyValue(card.opacityProperty(), 1, Interpolator.EASE_BOTH),
                new KeyValue(card.translateYProperty(), 0, Interpolator.EASE_BOTH)));
        if (tabs != null) timeline.getKeyFrames().add(new KeyFrame(Duration.millis(280),
                new KeyValue(tabs.prefHeightProperty(), normalTabsHeight(tabs), Interpolator.EASE_BOTH)));
        timeline.setOnFinished(event -> { card.setMinHeight(Region.USE_COMPUTED_SIZE); card.setMaxHeight(Region.USE_COMPUTED_SIZE); });
        timeline.play();
    }

    private static double normalTabsHeight(TabPane tabs) {
        Object value = tabs.getProperties().get(NORMAL_TABS_HEIGHT);
        return value instanceof Number number ? number.doubleValue() : Math.max(640, tabs.getPrefHeight());
    }

    private static <T extends Node> T findDescendant(Parent root, Class<T> type) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Parent parent) { T nested = findDescendant(parent, type); if (nested != null) return nested; }
        }
        return null;
    }

    private static String format(Object value) {
        if (value instanceof Number number) return NUMBER_FORMAT.format(number.doubleValue());
        return value == null ? "—" : value.toString();
    }

    private record SampleHit(XYChart.Series<?, ?> series, XYChart.Data<?, ?> data) { }
}
