package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.ThreeDChartPane;
import it.casiraghi.swiftbat.ui.components.TimeEnergyHeatmapPane;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.embed.swing.SwingNode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Synchronizes interactive state between embedded and in-place fullscreen
 * scientific views. The plot itself is reserved for data interaction: opening
 * fullscreen is allowed from the surrounding card, while 3D is always explicit.
 */
public final class InteractiveViewSyncEnhancer {
    private static final String WATCHED = InteractiveViewSyncEnhancer.class.getName() + ".watched";
    private static final String CARD_DONE = InteractiveViewSyncEnhancer.class.getName() + ".cardDone";
    private static final String LINE_DONE = InteractiveViewSyncEnhancer.class.getName() + ".lineDone";
    private static final String BAR_DONE = InteractiveViewSyncEnhancer.class.getName() + ".barDone";
    private static final String FULLSCREEN_DONE = InteractiveViewSyncEnhancer.class.getName() + ".fullscreenDone";
    private static final String FILTER_DONE = InteractiveViewSyncEnhancer.class.getName() + ".filterDone";
    private static final String RESTORE_DONE = InteractiveViewSyncEnhancer.class.getName() + ".restoreDone";
    private static final String FULL_LINE_DONE = InteractiveViewSyncEnhancer.class.getName() + ".fullLineDone";

    private static final String OLD_VISUAL_DONE = InteractionPolishEnhancer.class.getName() + ".visualDone";
    private static final String OLD_HISTOGRAM_DONE = PopulationCardEnhancer.class.getName() + ".installed";
    private static final String CHART_FOCUS = ChartInteractionEnhancer.class.getName() + ".focus";
    private static final double HIT_RADIUS = 18.0;

    private InteractiveViewSyncEnhancer() { }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> scan(root));
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) watch(tab.getContent());
            String key = WATCHED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(key))) {
                tabs.getProperties().put(key, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (!change.wasAdded()) continue;
                        for (Tab tab : change.getAddedSubList()) if (tab.getContent() != null) watch(tab.getContent());
                    }
                });
            }
        }

        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
            }
            Platform.runLater(() -> scan(parent));
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void scan(Node node) {
        if (node == null) return;
        enhance(node);
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) scan(tab.getContent());
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) scan(child);
        }
    }

    private static void enhance(Node node) {
        if (node instanceof Region region) {
            if (isVisualizationCard(region)) installCardInteraction(region);
            if (region.getStyleClass().contains("population-histogram-card")) {
                region.getProperties().put(OLD_HISTOGRAM_DONE, Boolean.TRUE);
            }
            if (region.getStyleClass().contains("population-filter-card")) installFastFilterCollapse(region);
        }
        if (node instanceof LineChart<?, ?> line) installSourceLineGuard(line);
        if (node instanceof BarChart<?, ?> bar) installBarGuard(bar);
        if (node instanceof TimeEnergyHeatmapPane heatmap) heatmap.setCursor(Cursor.HAND);
        if (node instanceof Button button) {
            if (button.getStyleClass().contains("population-filter-restore")) installFastFilterRestore(button);
            installFullscreenSync(button);
        }
    }

    /* ---------------- Interaction routing ---------------- */

    private static boolean isVisualizationCard(Region region) {
        return region instanceof ThreeDChartPane
                || region.getStyleClass().contains("overview-chart-card")
                || region.getStyleClass().contains("spectroscopy-chart-card")
                || region.getStyleClass().contains("time-energy-card")
                || region.getStyleClass().contains("population-chart-card");
    }

    private static void installCardInteraction(Region card) {
        card.getProperties().put(OLD_VISUAL_DONE, Boolean.TRUE);
        if (Boolean.TRUE.equals(card.getProperties().get(CARD_DONE))) return;
        card.getProperties().put(CARD_DONE, Boolean.TRUE);
        if (!card.getStyleClass().contains("interactive-visual-card")) card.getStyleClass().add("interactive-visual-card");
        card.setPickOnBounds(true);
        card.setCursor(Cursor.HAND);
        if (card.getStyleClass().contains("population-chart-card")) movePopulationActionsLeft(card);

        card.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1 || !event.isStillSincePress()) return;
            if (insidePlot(event.getTarget(), card) || isActionControl(event.getTarget(), card)) return;
            Button fullscreen = fullscreenForCard(card);
            if (fullscreen == null || fullscreen.isDisabled()) return;
            fullscreen.fire();
            event.consume();
        });
    }

    private static void movePopulationActionsLeft(Region card) {
        HBox actions = findActionRow(card);
        if (actions == null) return;
        Parent rawParent = actions.getParent();
        if (!(rawParent instanceof HBox header)) return;
        header.getChildren().removeIf(child -> child != actions && child.getClass() == Region.class);
        header.setAlignment(Pos.CENTER_LEFT);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actions, Priority.NEVER);
    }

    private static HBox findActionRow(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof HBox row && findButton(row, true) != null && findButton(row, false) != null) return row;
            if (child instanceof Parent parent) {
                HBox found = findActionRow(parent);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Button fullscreenForCard(Region card) {
        Button local = findButton(card, true);
        if (local != null) return local;
        if (!card.getStyleClass().contains("overview-chart-card")) return null;
        Node current = card.getParent();
        while (current != null) {
            if (current instanceof BorderPane pane) {
                Button found = findButton(pane, true);
                if (found != null) return found;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean insidePlot(Object rawTarget, Node boundary) {
        if (!(rawTarget instanceof Node target)) return false;
        Node current = target;
        while (current != null && current != boundary) {
            if (current instanceof XYChart<?, ?> || current instanceof TimeEnergyHeatmapPane
                    || current instanceof SwingNode || current.getStyleClass().contains("three-d-viewer")) return true;
            current = current.getParent();
        }
        return false;
    }

    private static boolean isActionControl(Object rawTarget, Node boundary) {
        if (!(rawTarget instanceof Node target)) return false;
        Node current = target;
        while (current != null && current != boundary) {
            if (current instanceof ButtonBase || current instanceof ChoiceBox<?> || current instanceof ComboBoxBase<?>
                    || current instanceof TextInputControl || current instanceof ScrollBar || current instanceof Slider) return true;
            current = current.getParent();
        }
        return false;
    }

    private static boolean hasVisualizationAncestor(Node node) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (current instanceof Region region && isVisualizationCard(region)) return true;
            current = current.getParent();
        }
        return false;
    }

    private static void installSourceLineGuard(LineChart<?, ?> chart) {
        if (!hasVisualizationAncestor(chart) || Boolean.TRUE.equals(chart.getProperties().get(LINE_DONE))) return;
        chart.getProperties().put(LINE_DONE, Boolean.TRUE);
        if (!chart.getStyleClass().contains("interactive-plot")) chart.getStyleClass().add("interactive-plot");
        chart.setCursor(Cursor.HAND);
        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
                clearLineFocus(chart);
                event.consume();
            }
        });
    }

    private static void installBarGuard(BarChart<?, ?> chart) {
        if (!hasVisualizationAncestor(chart) || Boolean.TRUE.equals(chart.getProperties().get(BAR_DONE))) return;
        chart.getProperties().put(BAR_DONE, Boolean.TRUE);
        if (!chart.getStyleClass().contains("interactive-plot")) chart.getStyleClass().add("interactive-plot");
        chart.setCursor(Cursor.HAND);
        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) event.consume();
        });
    }

    /* ---------------- Population filter toggle without layout animation ---------------- */

    private static void installFastFilterCollapse(Region card) {
        if (Boolean.TRUE.equals(card.getProperties().get(FILTER_DONE))) return;
        card.getProperties().put(FILTER_DONE, Boolean.TRUE);
        card.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1 || event.isConsumed()) return;
            if (isActionControl(event.getTarget(), card)) return;
            Button restore = siblingRestore(card);
            if (restore == null) return;
            card.setOpacity(1);
            card.setTranslateY(0);
            card.setVisible(false);
            card.setManaged(false);
            restore.setVisible(true);
            restore.setManaged(true);
            Parent parent = card.getParent();
            if (parent != null) parent.requestLayout();
            event.consume();
        });
    }

    private static void installFastFilterRestore(Button restore) {
        if (Boolean.TRUE.equals(restore.getProperties().get(RESTORE_DONE))) return;
        restore.getProperties().put(RESTORE_DONE, Boolean.TRUE);
        restore.addEventFilter(ActionEvent.ACTION, event -> {
            Region card = siblingFilterCard(restore);
            if (card == null) return;
            restore.setVisible(false);
            restore.setManaged(false);
            card.setMinHeight(Region.USE_COMPUTED_SIZE);
            card.setPrefHeight(Region.USE_COMPUTED_SIZE);
            card.setMaxHeight(Region.USE_COMPUTED_SIZE);
            card.setOpacity(1);
            card.setTranslateY(0);
            card.setVisible(true);
            card.setManaged(true);
            Parent parent = card.getParent();
            if (parent != null) parent.requestLayout();
            event.consume();
        });
    }

    private static Button siblingRestore(Node card) {
        Parent parent = card.getParent();
        if (parent == null) return null;
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.getStyleClass().contains("population-filter-restore")) return button;
        }
        return null;
    }

    private static Region siblingFilterCard(Node restore) {
        Parent parent = restore.getParent();
        if (parent == null) return null;
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Region region && region.getStyleClass().contains("population-filter-card")) return region;
        }
        return null;
    }

    /* ---------------- Fullscreen synchronization ---------------- */

    private enum Kind { EXPLORER_LINE, POPULATION_LINE, HEATMAP, THREE_D }
    private record Source(Node node, Kind kind) { }
    private record LineState(Set<String> focused, Set<String> visible) { }
    private record HeatmapState(Set<Object> selected) { }
    private record WaterfallState(Set<Integer> bands, double yaw, double pitch, double zoom, double panX, double panY) { }
    private record ThreeDState(String window, WaterfallState view) { }
    private record SeriesHit(XYChart.Series<?, ?> series) { }

    private static void installFullscreenSync(Button button) {
        if (!isFullscreenButton(button) || Boolean.TRUE.equals(button.getProperties().get(FULLSCREEN_DONE))) return;
        Source source = sourceFor(button);
        if (source == null) return;
        button.getProperties().put(FULLSCREEN_DONE, Boolean.TRUE);
        button.addEventFilter(ActionEvent.ACTION, event -> {
            Scene scene = button.getScene();
            if (scene == null) return;
            Parent originalRoot = scene.getRoot();
            Object state = capture(source.node(), source.kind());
            Platform.runLater(() -> bindFullscreen(scene, originalRoot, source, state));
        });
    }

    private static Source sourceFor(Button button) {
        ThreeDChartPane threeD = ancestor(button, ThreeDChartPane.class);
        if (threeD != null) return new Source(threeD, Kind.THREE_D);

        Parent heatmapCard = ancestorWithStyle(button, "time-energy-card");
        if (heatmapCard != null) {
            TimeEnergyHeatmapPane heatmap = findDescendant(heatmapCard, TimeEnergyHeatmapPane.class);
            return heatmap == null ? null : new Source(heatmap, Kind.HEATMAP);
        }

        Parent populationCard = ancestorWithStyle(button, "population-chart-card");
        if (populationCard != null) {
            LineChart<?, ?> line = findLineChart(populationCard, "population-chart");
            return line == null ? null : new Source(line, Kind.POPULATION_LINE);
        }

        Parent overviewAction = ancestorWithStyle(button, "overview-action-card");
        if (overviewAction != null) {
            Node current = overviewAction;
            while (current != null) {
                if (current instanceof BorderPane pane) {
                    LineChart<?, ?> line = findExplorerLineChart(pane);
                    if (line != null) return new Source(line, Kind.EXPLORER_LINE);
                }
                current = current.getParent();
            }
        }
        return null;
    }

    private static void bindFullscreen(Scene scene, Parent originalRoot, Source source, Object state) {
        Parent fullscreenRoot = scene.getRoot();
        if (fullscreenRoot == null || fullscreenRoot == originalRoot) return;
        Node fullscreen = fullscreenTarget(fullscreenRoot, source.kind());
        if (fullscreen == null) return;
        apply(fullscreen, source.kind(), state, true);
        if (fullscreen instanceof LineChart<?, ?> line) installFullscreenLineSelection(line);

        @SuppressWarnings("unchecked")
        ChangeListener<Parent>[] holder = new ChangeListener[1];
        holder[0] = (obs, oldRoot, newRoot) -> {
            if (newRoot != originalRoot) return;
            scene.rootProperty().removeListener(holder[0]);
            Object finalState = capture(fullscreen, source.kind());
            Platform.runLater(() -> apply(source.node(), source.kind(), finalState, false));
        };
        scene.rootProperty().addListener(holder[0]);
    }

    private static Node fullscreenTarget(Parent root, Kind kind) {
        return switch (kind) {
            case THREE_D -> findDescendant(root, ThreeDChartPane.class);
            case HEATMAP -> findDescendant(root, TimeEnergyHeatmapPane.class);
            case POPULATION_LINE -> findLineChart(root, "population-chart");
            case EXPLORER_LINE -> findExplorerLineChart(root);
        };
    }

    private static Object capture(Node node, Kind kind) {
        return switch (kind) {
            case EXPLORER_LINE, POPULATION_LINE -> node instanceof LineChart<?, ?> line ? captureLine(line) : null;
            case HEATMAP -> node instanceof TimeEnergyHeatmapPane heatmap ? captureHeatmap(heatmap) : null;
            case THREE_D -> node instanceof ThreeDChartPane pane ? captureThreeD(pane) : null;
        };
    }

    private static void apply(Node node, Kind kind, Object state, boolean opening) {
        if (node == null || state == null) return;
        if ((kind == Kind.EXPLORER_LINE || kind == Kind.POPULATION_LINE)
                && node instanceof LineChart<?, ?> line && state instanceof LineState lineState) {
            if (opening && kind == Kind.EXPLORER_LINE) keepVisibleSeries(line, lineState.visible());
            applyLineFocus(line, lineState.focused());
        } else if (kind == Kind.HEATMAP && node instanceof TimeEnergyHeatmapPane heatmap && state instanceof HeatmapState heatmapState) {
            applyHeatmap(heatmap, heatmapState);
        } else if (kind == Kind.THREE_D && node instanceof ThreeDChartPane pane && state instanceof ThreeDState threeDState) {
            applyThreeD(pane, threeDState);
        }
    }

    /* ---------------- Line focus state ---------------- */

    private static LineState captureLine(LineChart<?, ?> chart) {
        Set<String> visible = new LinkedHashSet<>();
        for (XYChart.Series<?, ?> series : chart.getData()) {
            String name = seriesName(series);
            if (name != null && !isTrigger(name)) visible.add(name);
        }
        Set<String> focused = new LinkedHashSet<>();
        Object raw = chart.getProperties().get(CHART_FOCUS);
        if (raw instanceof Set<?> set) {
            for (Object item : set) {
                if (item instanceof XYChart.Series<?, ?> series) {
                    String name = seriesName(series);
                    if (name != null) focused.add(name);
                }
            }
        }
        return new LineState(Set.copyOf(focused), Set.copyOf(visible));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void keepVisibleSeries(LineChart<?, ?> rawChart, Set<String> names) {
        if (names == null || names.isEmpty()) return;
        LineChart chart = rawChart;
        List<XYChart.Series> keep = new ArrayList<>();
        for (Object raw : List.copyOf(chart.getData())) {
            XYChart.Series series = (XYChart.Series) raw;
            String name = seriesName(series);
            if (name != null && (isTrigger(name) || names.contains(name))) keep.add(series);
        }
        if (!keep.isEmpty()) chart.getData().setAll(keep);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyLineFocus(LineChart<?, ?> rawChart, Set<String> names) {
        LineChart chart = rawChart;
        Set<XYChart.Series> focus = new LinkedHashSet<>();
        if (names != null) {
            for (Object raw : chart.getData()) {
                XYChart.Series series = (XYChart.Series) raw;
                String name = seriesName(series);
                if (name != null && names.contains(name)) focus.add(series);
            }
        }
        chart.getProperties().put(CHART_FOCUS, focus);
        applyFocusOpacity(chart, focus);
        Platform.runLater(() -> applyFocusOpacity(chart, focus));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void clearLineFocus(LineChart<?, ?> rawChart) {
        LineChart chart = rawChart;
        Set<XYChart.Series> focus = new LinkedHashSet<>();
        chart.getProperties().put(CHART_FOCUS, focus);
        applyFocusOpacity(chart, focus);
    }

    @SuppressWarnings("rawtypes")
    private static void applyFocusOpacity(LineChart chart, Set<XYChart.Series> focus) {
        for (Object raw : chart.getData()) {
            XYChart.Series series = (XYChart.Series) raw;
            Node node = series.getNode();
            if (node == null) continue;
            String name = seriesName(series);
            node.setOpacity(focus.isEmpty() || focus.contains(series) ? 1.0 : isTrigger(name) ? 0.50 : 0.09);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installFullscreenLineSelection(LineChart<?, ?> rawChart) {
        if (Boolean.TRUE.equals(rawChart.getProperties().get(FULL_LINE_DONE))) return;
        rawChart.getProperties().put(FULL_LINE_DONE, Boolean.TRUE);
        rawChart.setCursor(Cursor.HAND);
        LineChart chart = rawChart;
        chart.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            if (event.getClickCount() >= 2) {
                clearLineFocus(chart);
                event.consume();
                return;
            }
            if (event.getClickCount() != 1) return;
            SeriesHit hit = nearest(chart, event.getX(), event.getY());
            if (hit == null || hit.series() == null || isTrigger(seriesName(hit.series()))) return;
            Object raw = chart.getProperties().get(CHART_FOCUS);
            Set<XYChart.Series> focus = raw instanceof Set<?> ? (Set<XYChart.Series>) raw : new LinkedHashSet<>();
            chart.getProperties().put(CHART_FOCUS, focus);
            if (!focus.add(hit.series())) focus.remove(hit.series());
            applyFocusOpacity(chart, focus);
            event.consume();
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SeriesHit nearest(LineChart chart, double mouseX, double mouseY) {
        if (chart.getScene() == null) return null;
        Axis xAxis = chart.getXAxis();
        Axis yAxis = chart.getYAxis();
        double best = HIT_RADIUS * HIT_RADIUS;
        SeriesHit result = null;
        for (Object rawSeries : chart.getData()) {
            XYChart.Series series = (XYChart.Series) rawSeries;
            for (Object rawPoint : series.getData()) {
                XYChart.Data point = (XYChart.Data) rawPoint;
                if (point.getXValue() == null || point.getYValue() == null) continue;
                double x;
                double y;
                try {
                    x = xAxis.getDisplayPosition(point.getXValue());
                    y = yAxis.getDisplayPosition(point.getYValue());
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
                Point2D sx = xAxis.localToScene(x, 0);
                Point2D sy = yAxis.localToScene(0, y);
                if (sx == null || sy == null) continue;
                Point2D local = chart.sceneToLocal(sx.getX(), sy.getY());
                double dx = local.getX() - mouseX;
                double dy = local.getY() - mouseY;
                double distance = dx * dx + dy * dy;
                if (distance < best) {
                    best = distance;
                    result = new SeriesHit(series);
                }
            }
        }
        return result;
    }

    private static String seriesName(XYChart.Series<?, ?> series) {
        if (series == null || series.getName() == null || series.getName().isBlank()) return null;
        return series.getName().trim();
    }

    private static boolean isTrigger(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).startsWith("trigger");
    }

    /* ---------------- Heatmap state ---------------- */

    private static HeatmapState captureHeatmap(TimeEnergyHeatmapPane heatmap) {
        Set<Object> copy = new LinkedHashSet<>();
        Object raw = fieldValue(heatmap, "selectedCells");
        if (raw instanceof Set<?> set) copy.addAll(set);
        return new HeatmapState(Set.copyOf(copy));
    }

    @SuppressWarnings("unchecked")
    private static void applyHeatmap(TimeEnergyHeatmapPane heatmap, HeatmapState state) {
        Object raw = fieldValue(heatmap, "selectedCells");
        if (!(raw instanceof Set<?>)) return;
        Set<Object> target = (Set<Object>) raw;
        target.clear();
        target.addAll(state.selected());
        invokeNoArgs(heatmap, "draw");
    }

    /* ---------------- 3D renderer state ---------------- */

    private static ThreeDState captureThreeD(ThreeDChartPane pane) {
        Object rawChoice = fieldValue(pane, "windowChoice");
        String window = rawChoice instanceof ChoiceBox<?> choice && choice.getValue() != null
                ? choice.getValue().toString() : null;
        return new ThreeDState(window, captureWaterfall(fieldValue(pane, "renderer")));
    }

    @SuppressWarnings("unchecked")
    private static void applyThreeD(ThreeDChartPane pane, ThreeDState state) {
        Object rawChoice = fieldValue(pane, "windowChoice");
        if (rawChoice instanceof ChoiceBox<?> choice && state.window() != null
                && !state.window().equals(String.valueOf(choice.getValue()))) {
            ((ChoiceBox<Object>) choice).setValue(state.window());
        }
        applyWaterfall(fieldValue(pane, "renderer"), state.view());
        Object rawLabel = fieldValue(pane, "zoomLabel");
        if (rawLabel instanceof Label label && state.view() != null) {
            label.setText("Zoom " + Math.round(state.view().zoom() * 100.0) + "%");
        }
    }

    private static WaterfallState captureWaterfall(Object renderer) {
        WaterfallState defaults = new WaterfallState(Set.of(), 0.32, 0.72, 1.0, 0.0, 0.0);
        if (renderer == null) return defaults;
        AtomicReference<WaterfallState> result = new AtomicReference<>(defaults);
        runOnSwingAndWait(() -> {
            Set<Integer> bands = new LinkedHashSet<>();
            Object raw = fieldValue(renderer, "focusedBands");
            if (raw instanceof Set<?> set) for (Object item : set) if (item instanceof Integer index) bands.add(index);
            result.set(new WaterfallState(Set.copyOf(bands),
                    doubleField(renderer, "yaw", 0.32), doubleField(renderer, "pitch", 0.72),
                    doubleField(renderer, "zoom", 1.0), doubleField(renderer, "panX", 0.0),
                    doubleField(renderer, "panY", 0.0)));
        });
        return result.get();
    }

    @SuppressWarnings("unchecked")
    private static void applyWaterfall(Object renderer, WaterfallState state) {
        if (renderer == null || state == null) return;
        SwingUtilities.invokeLater(() -> {
            Object raw = fieldValue(renderer, "focusedBands");
            if (raw instanceof Set<?>) {
                Set<Object> bands = (Set<Object>) raw;
                bands.clear();
                bands.addAll(state.bands());
            }
            setDoubleField(renderer, "yaw", state.yaw());
            setDoubleField(renderer, "pitch", state.pitch());
            setDoubleField(renderer, "zoom", state.zoom());
            setDoubleField(renderer, "panX", state.panX());
            setDoubleField(renderer, "panY", state.panY());
            if (renderer instanceof Component component) component.repaint();
        });
    }

    private static void runOnSwingAndWait(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (Exception ignored) { }
    }

    /* ---------------- Reflection/tree helpers ---------------- */

    private static Object fieldValue(Object owner, String name) {
        if (owner == null) return null;
        Field field = findField(owner.getClass(), name);
        if (field == null) return null;
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static double doubleField(Object owner, String name, double fallback) {
        Object value = fieldValue(owner, name);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static void setDoubleField(Object owner, String name, double value) {
        if (owner == null) return;
        Field field = findField(owner.getClass(), name);
        if (field == null) return;
        try {
            field.setAccessible(true);
            field.setDouble(owner, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static void invokeNoArgs(Object owner, String name) {
        if (owner == null) return;
        Class<?> current = owner.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name);
                method.setAccessible(true);
                method.invoke(owner);
                return;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return;
            }
        }
    }

    private static boolean isFullscreenButton(Button button) {
        String text = normalize(button.getText());
        return text.contains("schermointero") || text.contains("fullscreen");
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private static Button findButton(Parent root, boolean fullscreen) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.isVisible() && button.isManaged()) {
                String text = normalize(button.getText());
                if (fullscreen ? text.contains("schermointero") || text.contains("fullscreen") : text.contains("3d")) return button;
            }
            if (child instanceof Parent parent) {
                Button found = findButton(parent, fullscreen);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Parent ancestorWithStyle(Node node, String styleClass) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (current instanceof Parent parent && current.getStyleClass().contains(styleClass)) return parent;
            current = current.getParent();
        }
        return null;
    }

    private static <T> T ancestor(Node node, Class<T> type) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (type.isInstance(current)) return type.cast(current);
            current = current.getParent();
        }
        return null;
    }

    private static <T extends Node> T findDescendant(Parent root, Class<T> type) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof TabPane tabs) {
                for (Tab tab : tabs.getTabs()) {
                    Node content = tab.getContent();
                    if (content != null && type.isInstance(content)) return type.cast(content);
                    if (content instanceof Parent parent) {
                        T found = findDescendant(parent, type);
                        if (found != null) return found;
                    }
                }
            }
            if (child instanceof Parent parent) {
                T found = findDescendant(parent, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static LineChart<?, ?> findLineChart(Parent root, String styleClass) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof LineChart<?, ?> line && line.getStyleClass().contains(styleClass)) return line;
            if (child instanceof Parent parent) {
                LineChart<?, ?> found = findLineChart(parent, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static LineChart<?, ?> findExplorerLineChart(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof LineChart<?, ?> line && line.getStyleClass().contains("lightcurve-chart")
                    && !line.getStyleClass().contains("population-chart")
                    && !line.getStyleClass().contains("spectral-model-chart")) return line;
            if (child instanceof Parent parent) {
                LineChart<?, ?> found = findExplorerLineChart(parent);
                if (found != null) return found;
            }
        }
        return null;
    }
}
