package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.TimeEnergyHeatmapPane;
import it.casiraghi.swiftbat.ui.components.ThreeDChartPane;
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
import javafx.scene.layout.VBox;

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
 * Keeps interactive scientific views coherent when switching between embedded
 * and in-place fullscreen representations.
 *
 * <p>The fullscreen views used by the application are normally fresh control
 * instances. That is useful for responsive sizing, but it also means that a
 * selection, rotation or zoom performed in one representation would otherwise
 * be lost in the other. This enhancer treats the two representations as two
 * views of one interaction state and copies the state in both directions.</p>
 *
 * <p>It also reserves the actual plot area for data interaction. A click in a
 * plot never opens 3D/fullscreen; a click on the surrounding visualization card
 * can open the current 2D/fullscreen view. 3D remains an explicit button action.</p>
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

    /* Stable private-property keys used by the older enhancers. Marking them is
       intentional: it disables only their automatic card-opening behavior while
       retaining their chart/tooltips/export logic. */
    private static final String OLD_VISUAL_DONE = InteractionPolishEnhancer.class.getName() + ".visualDone";
    private static final String OLD_HISTOGRAM_DONE = PopulationCardEnhancer.class.getName() + ".installed";
    private static final String CHART_FOCUS = ChartInteractionEnhancer.class.getName() + ".focus";

    private static final double HIT_RADIUS = 18.0;

    private InteractiveViewSyncEnhancer() {
    }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> scan(root));
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) watch(tab.getContent());
            }
            String tabsKey = WATCHED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(tabsKey))) {
                tabs.getProperties().put(tabsKey, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (!change.wasAdded()) continue;
                        for (Tab tab : change.getAddedSubList()) {
                            if (tab.getContent() != null) watch(tab.getContent());
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
            if (isInteractiveVisualizationCard(region)) installCardInteraction(region);
            if (region.getStyleClass().contains("population-histogram-card")) {
                // PopulationCardEnhancer may still add export controls, but it must
                // not turn a normal chart click into an implicit 3D navigation.
                region.getProperties().put(OLD_HISTOGRAM_DONE, Boolean.TRUE);
            }
            if (region.getStyleClass().contains("population-filter-card")) installFastFilterCollapse(region);
        }
        if (node instanceof LineChart<?, ?> line) installSourceLineGuard(line);
        if (node instanceof BarChart<?, ?> bar) installBarGuard(bar);
        if (node instanceof Button button) {
            if (button.getStyleClass().contains("population-filter-restore")) installFastFilterRestore(button);
            installFullscreenSynchronization(button);
        }
    }

    /* ---------------- Card versus plot interaction ---------------- */

    private static boolean isInteractiveVisualizationCard(Region region) {
        if (region instanceof ThreeDChartPane) return true;
        return region.getStyleClass().contains("overview-chart-card")
                || region.getStyleClass().contains("spectroscopy-chart-card")
                || region.getStyleClass().contains("time-energy-card")
                || region.getStyleClass().contains("population-chart-card");
    }

    private static void installCardInteraction(Region card) {
        // Must happen before InteractionPolishEnhancer is installed.
        card.getProperties().put(OLD_VISUAL_DONE, Boolean.TRUE);
        if (Boolean.TRUE.equals(card.getProperties().get(CARD_DONE))) return;
        card.getProperties().put(CARD_DONE, Boolean.TRUE);
        if (!card.getStyleClass().contains("interactive-visual-card")) {
            card.getStyleClass().add("interactive-visual-card");
        }
        card.setPickOnBounds(true);
        card.setCursor(Cursor.HAND);

        if (card.getStyleClass().contains("population-chart-card")) {
            movePopulationActionsLeft(card);
        }

        card.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY
                    || event.getClickCount() != 1
                    || !event.isStillSincePress()) return;
            if (insidePlot(event.getTarget(), card) || isActionControl(event.getTarget(), card)) return;

            Button fullscreen = fullscreenForCard(card);
            if (fullscreen == null || fullscreen.isDisabled()) return;
            fullscreen.fire();
            event.consume();
        });
    }

    private static void movePopulationActionsLeft(Region card) {
        HBox actions = findActionRow(card);
        if (actions == null || !(actions.getParent() instanceof HBox header)) return;
        header.getChildren().removeIf(child -> child != actions && child.getClass() == Region.class);
        actions.setAlignment(Pos.CENTER_LEFT);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actions, Priority.NEVER);
    }

    private static HBox findActionRow(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof HBox row) {
                boolean hasFullscreen = findButton(row, true) != null;
                boolean hasThreeD = findButton(row, false) != null;
                if (hasFullscreen && hasThreeD) return row;
            }
            if (child instanceof Parent parent) {
                HBox nested = findActionRow(parent);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static Button fullscreenForCard(Region card) {
        Button local = findButton(card, true);
        if (local != null) return local;
        if (card.getStyleClass().contains("overview-chart-card")) {
            Node current = card.getParent();
            while (current != null) {
                if (current instanceof BorderPane pane) {
                    Button button = findButton(pane, true);
                    if (button != null) return button;
                }
                if (current.getStyleClass().contains("page-root")) break;
                current = current.getParent();
            }
        }
        return null;
    }

    private static boolean insidePlot(Object rawTarget, Node boundary) {
        if (!(rawTarget instanceof Node target)) return false;
        Node current = target;
        while (current != null && current != boundary) {
            if (current instanceof XYChart<?, ?>
                    || current instanceof TimeEnergyHeatmapPane
                    || current instanceof SwingNode
                    || current.getStyleClass().contains("three-d-viewer")) return true;
            current = current.getParent();
        }
        return false;
    }

    private static boolean isActionControl(Object rawTarget, Node boundary) {
        if (!(rawTarget instanceof Node target)) return false;
        Node current = target;
        while (current != null && current != boundary) {
            if (current instanceof ButtonBase
                    || current instanceof ChoiceBox<?>
                    || current instanceof ComboBoxBase<?>
                    || current instanceof TextInputControl
                    || current instanceof ScrollBar
                    || current instanceof Slider) return true;
            current = current.getParent();
        }
        return false;
    }

    /* ---------------- Prevent implicit 3D; keep plot selection ---------------- */

    private static void installSourceLineGuard(LineChart<?, ?> chart) {
        if (!hasInteractiveVisualizationAncestor(chart)
                || Boolean.TRUE.equals(chart.getProperties().get(LINE_DONE))) return;
        chart.getProperties().put(LINE_DONE, Boolean.TRUE);
        if (!chart.getStyleClass().contains("interactive-plot")) chart.getStyleClass().add("interactive-plot");
        chart.setCursor(Cursor.HAND);

        // Single click deliberately passes through to ChartInteractionEnhancer.
        // Double click resets focus locally and must never be interpreted as 3D.
        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
                clearLineFocus(chart);
                event.consume();
            }
        });
    }

    private static void installBarGuard(BarChart<?, ?> chart) {
        if (!hasInteractiveVisualizationAncestor(chart)
                || Boolean.TRUE.equals(chart.getProperties().get(BAR_DONE))) return;
        chart.getProperties().put(BAR_DONE, Boolean.TRUE);
        if (!chart.getStyleClass().contains("interactive-plot")) chart.getStyleClass().add("interactive-plot");
        chart.setCursor(Cursor.HAND);
        chart.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
                event.consume();
            }
        });
    }

    private static boolean hasInteractiveVisualizationAncestor(Node node) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (current instanceof Region region && isInteractiveVisualizationCard(region)) return true;
            if (current.getStyleClass().contains("page-root")) break;
            current = current.getParent();
        }
        return false;
    }

    /* ---------------- Fast Population filters ---------------- */

    private static void installFastFilterCollapse(Region card) {
        if (Boolean.TRUE.equals(card.getProperties().get(FILTER_DONE))) return;
        card.getProperties().put(FILTER_DONE, Boolean.TRUE);
        card.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1 || event.isConsumed()) return;
            if (isActionControl(event.getTarget(), card)) return;
            Button restore = siblingRestoreButton(card);
            if (restore == null) return;

            card.setOpacity(1);
            card.setTranslateY(0);
            card.setVisible(false);
            card.setManaged(false);
            restore.setVisible(true);
            restore.setManaged(true);
            if (card.getParent() != null) card.getParent().requestLayout();
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
            if (card.getParent() != null) card.getParent().requestLayout();
            event.consume();
        });
    }

    private static Button siblingRestoreButton(Node card) {
        if (!(card.getParent() instanceof Parent parent)) return null;
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.getStyleClass().contains("population-filter-restore")) return button;
        }
        return null;
    }

    private static Region siblingFilterCard(Node restore) {
        if (!(restore.getParent() instanceof Parent parent)) return null;
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Region region && region.getStyleClass().contains("population-filter-card")) return region;
        }
        return null;
    }

    /* ---------------- Fullscreen state synchronization ---------------- */

    private enum SyncKind { EXPLORER_LINE, POPULATION_LINE, HEATMAP, THREE_D }

    private static void installFullscreenSynchronization(Button button) {
        if (Boolean.TRUE.equals(button.getProperties().get(FULLSCREEN_DONE)) || !isFullscreenButton(button)) return;
        Source source = sourceFor(button);
        if (source == null) return;
        button.getProperties().put(FULLSCREEN_DONE, Boolean.TRUE);

        button.addEventFilter(ActionEvent.ACTION, event -> {
            if (button.getScene() == null) return;
            Scene scene = button.getScene();
            Parent originalRoot = scene.getRoot();
            Object before = capture(source.node(), source.kind());
            Platform.runLater(() -> connectFullscreenSession(scene, originalRoot, source, before));
        });
    }

    private static Source sourceFor(Button button) {
        ThreeDChartPane threeD = ancestor(button, ThreeDChartPane.class);
        if (threeD != null) return new Source(threeD, SyncKind.THREE_D);

        Parent timeEnergyCard = ancestorWithStyle(button, "time-energy-card");
        if (timeEnergyCard != null) {
            TimeEnergyHeatmapPane heatmap = findDescendant(timeEnergyCard, TimeEnergyHeatmapPane.class);
            return heatmap == null ? null : new Source(heatmap, SyncKind.HEATMAP);
        }

        Parent populationCard = ancestorWithStyle(button, "population-chart-card");
        if (populationCard != null) {
            LineChart<?, ?> line = findLineChart(populationCard, "population-chart");
            return line == null ? null : new Source(line, SyncKind.POPULATION_LINE);
        }

        Parent overviewAction = ancestorWithStyle(button, "overview-action-card");
        if (overviewAction != null) {
            Node current = overviewAction;
            while (current != null) {
                if (current instanceof BorderPane pane) {
                    LineChart<?, ?> line = findExplorerLineChart(pane);
                    if (line != null) return new Source(line, SyncKind.EXPLORER_LINE);
                }
                current = current.getParent();
            }
        }
        return null;
    }

    private static void connectFullscreenSession(Scene scene, Parent originalRoot, Source source, Object initialState) {
        Parent fullscreenRoot = scene.getRoot();
        if (fullscreenRoot == null || fullscreenRoot == originalRoot) return;
        Node fullscreenNode = fullscreenTarget(fullscreenRoot, source.kind(), source.node());
        if (fullscreenNode == null) return;

        apply(fullscreenNode, source.kind(), initialState, true);
        if (fullscreenNode instanceof LineChart<?, ?> line) installFullscreenLineInteraction(line);

        @SuppressWarnings("unchecked")
        ChangeListener<Parent>[] holder = new ChangeListener[1];
        holder[0] = (obs, oldRoot, newRoot) -> {
            if (newRoot != originalRoot) return;
            scene.rootProperty().removeListener(holder[0]);
            Object finalState = capture(fullscreenNode, source.kind());
            Platform.runLater(() -> apply(source.node(), source.kind(), finalState, false));
        };
        scene.rootProperty().addListener(holder[0]);
    }

    private static Node fullscreenTarget(Parent root, SyncKind kind, Node source) {
        return switch (kind) {
            case THREE_D -> findDifferentDescendant(root, ThreeDChartPane.class, source);
            case HEATMAP -> findDifferentDescendant(root, TimeEnergyHeatmapPane.class, source);
            case POPULATION_LINE -> findLineChart(root, "population-chart");
            case EXPLORER_LINE -> findExplorerLineChart(root);
        };
    }

    private static Object capture(Node node, SyncKind kind) {
        if (node == null) return null;
        return switch (kind) {
            case EXPLORER_LINE, POPULATION_LINE -> node instanceof LineChart<?, ?> line ? captureLine(line) : null;
            case HEATMAP -> node instanceof TimeEnergyHeatmapPane heatmap ? captureHeatmap(heatmap) : null;
            case THREE_D -> node instanceof ThreeDChartPane pane ? captureThreeD(pane) : null;
        };
    }

    private static void apply(Node node, SyncKind kind, Object state, boolean openingFullscreen) {
        if (node == null || state == null) return;
        switch (kind) {
            case EXPLORER_LINE -> {
                if (node instanceof LineChart<?, ?> line && state instanceof LineState lineState) {
                    if (openingFullscreen) keepVisibleSeries(line, lineState.visibleSeries());
                    applyLineFocus(line, lineState.focusedSeries());
                }
            }
            case POPULATION_LINE -> {
                if (node instanceof LineChart<?, ?> line && state instanceof LineState lineState) {
                    applyLineFocus(line, lineState.focusedSeries());
                }
            }
            case HEATMAP -> {
                if (node instanceof TimeEnergyHeatmapPane heatmap && state instanceof HeatmapState heatmapState) {
                    applyHeatmap(heatmap, heatmapState);
                }
            }
            case THREE_D -> {
                if (node instanceof ThreeDChartPane pane && state instanceof ThreeDState threeDState) {
                    applyThreeD(pane, threeDState);
                }
            }
        }
    }

    /* ---------------- Line-chart focus ---------------- */

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
    private static void keepVisibleSeries(LineChart<?, ?> rawChart, Set<String> visible) {
        if (visible == null || visible.isEmpty()) return;
        LineChart chart = rawChart;
        List<XYChart.Series> keep = new ArrayList<>();
        for (Object raw : List.copyOf(chart.getData())) {
            XYChart.Series series = (XYChart.Series) raw;
            String name = seriesName(series);
            if (name != null && (isTrigger(name) || visible.contains(name))) keep.add(series);
        }
        if (!keep.isEmpty()) chart.getData().setAll(keep);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyLineFocus(LineChart<?, ?> rawChart, Set<String> names) {
        LineChart chart = rawChart;
        Set<XYChart.Series> focus = new LinkedHashSet<>();
        if (names != null && !names.isEmpty()) {
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
            Node seriesNode = series.getNode();
            if (seriesNode == null) continue;
            String name = seriesName(series);
            boolean trigger = name != null && isTrigger(name);
            seriesNode.setOpacity(focus.isEmpty() || focus.contains(series) ? 1.0 : trigger ? 0.50 : 0.09);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installFullscreenLineInteraction(LineChart<?, ?> rawChart) {
        if (Boolean.TRUE.equals(rawChart.getProperties().get(FULL_LINE_DONE))) return;
        rawChart.getProperties().put(FULL_LINE_DONE, Boolean.TRUE);
        rawChart.setCursor(Cursor.HAND);
        if (!rawChart.getStyleClass().contains("interactive-plot")) rawChart.getStyleClass().add("interactive-plot");
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
            if (hit == null || hit.series() == null) return;
            String name = seriesName(hit.series());
            if (name != null && isTrigger(name)) return;

            Object stored = chart.getProperties().get(CHART_FOCUS);
            Set<XYChart.Series> focus;
            if (stored instanceof Set<?> existing) {
                focus = (Set<XYChart.Series>) existing;
            } else {
                focus = new LinkedHashSet<>();
                chart.getProperties().put(CHART_FOCUS, focus);
            }
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
            if (series.getData() == null) continue;
            for (Object rawData : series.getData()) {
                XYChart.Data data = (XYChart.Data) rawData;
                if (data.getXValue() == null || data.getYValue() == null) continue;
                double xDisplay;
                double yDisplay;
                try {
                    xDisplay = xAxis.getDisplayPosition(data.getXValue());
                    yDisplay = yAxis.getDisplayPosition(data.getYValue());
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (!Double.isFinite(xDisplay) || !Double.isFinite(yDisplay)) continue;
                Point2D xScene = xAxis.localToScene(xDisplay, 0);
                Point2D yScene = yAxis.localToScene(0, yDisplay);
                if (xScene == null || yScene == null) continue;
                Point2D point = chart.sceneToLocal(xScene.getX(), yScene.getY());
                double dx = point.getX() - mouseX;
                double dy = point.getY() - mouseY;
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

    /* ---------------- Time-energy selection ---------------- */

    private static HeatmapState captureHeatmap(TimeEnergyHeatmapPane heatmap) {
        Object value = fieldValue(heatmap, "selectedCells");
        Set<Object> copy = new LinkedHashSet<>();
        if (value instanceof Set<?> set) copy.addAll(set);
        return new HeatmapState(Set.copyOf(copy));
    }

    @SuppressWarnings("unchecked")
    private static void applyHeatmap(TimeEnergyHeatmapPane heatmap, HeatmapState state) {
        Object value = fieldValue(heatmap, "selectedCells");
        if (!(value instanceof Set<?> raw)) return;
        Set<Object> target = (Set<Object>) raw;
        target.clear();
        target.addAll(state.selectedCells());
        invokeNoArgs(heatmap, "draw");
    }

    /* ---------------- Java2D 3D state ---------------- */

    private static ThreeDState captureThreeD(ThreeDChartPane pane) {
        Object choiceValue = fieldValue(pane, "windowChoice");
        String window = choiceValue instanceof ChoiceBox<?> choice && choice.getValue() != null
                ? choice.getValue().toString() : null;
        Object renderer = fieldValue(pane, "renderer");
        return new ThreeDState(window, captureWaterfall(renderer));
    }

    @SuppressWarnings("unchecked")
    private static void applyThreeD(ThreeDChartPane pane, ThreeDState state) {
        Object choiceValue = fieldValue(pane, "windowChoice");
        if (choiceValue instanceof ChoiceBox<?> rawChoice && state.window() != null) {
            ChoiceBox<Object> choice = (ChoiceBox<Object>) rawChoice;
            if (!state.window().equals(String.valueOf(choice.getValue()))) choice.setValue(state.window());
        }
        Object renderer = fieldValue(pane, "renderer");
        applyWaterfall(renderer, state.view());
        Object labelValue = fieldValue(pane, "zoomLabel");
        if (labelValue instanceof Label label && state.view() != null) {
            label.setText("Zoom " + Math.round(state.view().zoom() * 100.0) + "%");
        }
    }

    private static WaterfallState captureWaterfall(Object renderer) {
        if (renderer == null) return WaterfallState.defaults();
        AtomicReference<WaterfallState> result = new AtomicReference<>(WaterfallState.defaults());
        Runnable read = () -> {
            Set<Integer> focused = new LinkedHashSet<>();
            Object raw = fieldValue(renderer, "focusedBands");
            if (raw instanceof Set<?> set) {
                for (Object item : set) if (item instanceof Integer index) focused.add(index);
            }
            result.set(new WaterfallState(
                    Set.copyOf(focused),
                    doubleField(renderer, "yaw", 0.32),
                    doubleField(renderer, "pitch", 0.72),
                    doubleField(renderer, "zoom", 1.0),
                    doubleField(renderer, "panX", 0.0),
                    doubleField(renderer, "panY", 0.0)));
        };
        runOnSwingAndWait(read);
        return result.get();
    }

    @SuppressWarnings("unchecked")
    private static void applyWaterfall(Object renderer, WaterfallState state) {
        if (renderer == null || state == null) return;
        SwingUtilities.invokeLater(() -> {
            Object raw = fieldValue(renderer, "focusedBands");
            if (raw instanceof Set<?> set) {
                Set<Object> focused = (Set<Object>) set;
                focused.clear();
                focused.addAll(state.focusedBands());
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
        } catch (Exception ignored) {
            // A temporary EDT interruption must never prevent the view from opening.
        }
    }

    /* ---------------- Reflection helpers ---------------- */

    private static Object fieldValue(Object owner, String name) {
        if (owner == null || name == null) return null;
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
        Field field = findField(owner == null ? null : owner.getClass(), name);
        if (field == null) return;
        try {
            field.setAccessible(true);
            field.setDouble(owner, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Keep the previous view value if a future renderer changes internals.
        }
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

    private static void invokeNoArgs(Object owner, String methodName) {
        if (owner == null) return;
        Class<?> current = owner.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
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

    /* ---------------- Tree helpers ---------------- */

    private static boolean isFullscreenButton(Button button) {
        String text = button.getText() == null ? "" : button.getText().trim().toLowerCase(Locale.ROOT);
        String compact = text.replace(" ", "");
        return text.contains("schermo intero") || text.contains("full screen") || compact.contains("fullscreen");
    }

    private static Button findButton(Parent root, boolean fullscreen) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.isVisible() && button.isManaged()) {
                String text = button.getText() == null ? "" : button.getText().trim().toLowerCase(Locale.ROOT);
                String compact = text.replace(" ", "");
                boolean match = fullscreen
                        ? text.contains("schermo intero") || text.contains("full screen") || compact.contains("fullscreen")
                        : text.contains("3d");
                if (match) return button;
            }
            if (child instanceof Parent parent) {
                Button nested = findButton(parent, fullscreen);
                if (nested != null) return nested;
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
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof TabPane tabs) {
                for (Tab tab : tabs.getTabs()) {
                    if (tab.getContent() != null) {
                        if (type.isInstance(tab.getContent())) return type.cast(tab.getContent());
                        if (tab.getContent() instanceof Parent tabParent) {
                            T found = findDescendant(tabParent, type);
                            if (found != null) return found;
                        }
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

    private static <T extends Node> T findDifferentDescendant(Parent root, Class<T> type, Node excluded) {
        T found = findDescendant(root, type);
        return found == excluded ? null : found;
    }

    private static LineChart<?, ?> findLineChart(Parent root, String styleClass) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof LineChart<?, ?> line && line.getStyleClass().contains(styleClass)) return line;
            if (child instanceof Parent parent) {
                LineChart<?, ?> nested = findLineChart(parent, styleClass);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static LineChart<?, ?> findExplorerLineChart(Parent root) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof LineChart<?, ?> line
                    && line.getStyleClass().contains("lightcurve-chart")
                    && !line.getStyleClass().contains("population-chart")
                    && !line.getStyleClass().contains("spectral-model-chart")) return line;
            if (child instanceof Parent parent) {
                LineChart<?, ?> nested = findExplorerLineChart(parent);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private record Source(Node node, SyncKind kind) { }
    private record LineState(Set<String> focusedSeries, Set<String> visibleSeries) { }
    private record HeatmapState(Set<Object> selectedCells) { }
    private record ThreeDState(String window, WaterfallState view) { }
    private record WaterfallState(Set<Integer> focusedBands, double yaw, double pitch,
                                  double zoom, double panX, double panY) {
        private static WaterfallState defaults() {
            return new WaterfallState(Set.of(), 0.32, 0.72, 1.0, 0.0, 0.0);
        }
    }
    private record SeriesHit(XYChart.Series<?, ?> series) { }
}
