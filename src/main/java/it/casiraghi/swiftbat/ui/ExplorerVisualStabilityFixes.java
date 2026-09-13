package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Last-mile fixes for Explorer controls that depend on the first JavaFX layout pass.
 * Keeps the 2D fullscreen chart populated from the currently visible curve, gives the
 * time-window selector enough room to display its complete value, and normalizes the
 * visual size of vertical scrollbars and their thumbs across lists/tables/pages.
 */
public final class ExplorerVisualStabilityFixes {
    private static final String WATCHED = ExplorerVisualStabilityFixes.class.getName() + ".watched";
    private static final String WINDOW_DONE = ExplorerVisualStabilityFixes.class.getName() + ".windowDone";
    private static final String CHART_DONE = ExplorerVisualStabilityFixes.class.getName() + ".chartDone";
    private static final String SCROLL_DONE = ExplorerVisualStabilityFixes.class.getName() + ".scrollDone";
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final double SCROLLBAR_THICKNESS = 12.0;
    private static final double THUMB_MIN_LENGTH = 46.0;

    private static Parent installedRoot;

    private ExplorerVisualStabilityFixes() { }

    public static void install(Parent root) {
        if (root == null) return;
        installedRoot = root;
        watch(root);
        observeScene(root);
        Platform.runLater(() -> {
            scan(root);
            polishAllScrollbars(root);
        });
    }

    private static void observeScene(Parent root) {
        if (root.getScene() != null) watchScene(root.getScene());
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) watchScene(newScene);
        });
    }

    private static void watchScene(Scene scene) {
        if (scene == null || !WATCHED_SCENES.add(scene)) return;
        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (newRoot == null) return;
            Platform.runLater(() -> repairDynamicRoot(newRoot));
        });
        Parent current = scene.getRoot();
        if (current != null) Platform.runLater(() -> repairDynamicRoot(current));
    }

    private static void repairDynamicRoot(Parent root) {
        watch(root);
        scan(root);
        repairExplorerCharts(root);
        polishAllScrollbars(root);
        Platform.runLater(() -> {
            repairExplorerCharts(root);
            polishAllScrollbars(root);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void repairExplorerCharts(Node node) {
        if (node == null) return;
        if (node instanceof LineChart<?, ?> raw && isExplorerLightCurve(raw)) {
            ensureScientificSeries((LineChart<Number, Number>) raw);
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) repairExplorerCharts(child);
        }
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
            }
            Platform.runLater(() -> {
                scan(parent);
                polishAllScrollbars(parent);
            });
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void scan(Node node) {
        if (node == null) return;
        enhance(node);
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) scan(child);
        }
    }

    private static void enhance(Node node) {
        if (node instanceof ChoiceBox<?> choice) polishTimeWindowChoice(choice);
        if (node instanceof ComboBox<?> combo) polishTimeWindowCombo(combo);
        if (node instanceof LineChart<?, ?> chart && isExplorerLightCurve(chart)) prepareExplorerChart(chart);
        if (node instanceof ScrollBar bar) polishScrollBar(bar);
        if (node instanceof ScrollPane || node instanceof TableView<?> || node instanceof ListView<?>) {
            Platform.runLater(() -> polishAllScrollbars(node));
        }
    }

    private static boolean containsTriggerWindow(Object value) {
        if (value == null) return false;
        String text = value.toString().toLowerCase(Locale.ROOT);
        return text.contains("trigger") && (text.contains("±") || text.contains("+/-"));
    }

    private static boolean looksLikeTriggerWindowItems(List<?> items) {
        if (items == null || items.isEmpty()) return false;
        int matches = 0;
        for (Object item : items) if (containsTriggerWindow(item)) matches++;
        return matches >= 2;
    }

    private static void polishTimeWindowChoice(ChoiceBox<?> choice) {
        if (Boolean.TRUE.equals(choice.getProperties().get(WINDOW_DONE))) return;
        if (!looksLikeTriggerWindowItems(choice.getItems())) return;
        choice.getProperties().put(WINDOW_DONE, Boolean.TRUE);
        choice.setMinWidth(168);
        choice.setPrefWidth(178);
        choice.setMaxWidth(190);
        HBox.setHgrow(choice, Priority.NEVER);
        compactMovingAverageSibling(choice);
    }

    private static void polishTimeWindowCombo(ComboBox<?> combo) {
        if (Boolean.TRUE.equals(combo.getProperties().get(WINDOW_DONE))) return;
        if (!looksLikeTriggerWindowItems(combo.getItems())) return;
        combo.getProperties().put(WINDOW_DONE, Boolean.TRUE);
        combo.setMinWidth(168);
        combo.setPrefWidth(178);
        combo.setMaxWidth(190);
        HBox.setHgrow(combo, Priority.NEVER);
        compactMovingAverageSibling(combo);
    }

    private static void compactMovingAverageSibling(Node selector) {
        Parent parent = selector.getParent();
        if (!(parent instanceof HBox row)) return;
        for (Node child : row.getChildren()) {
            if (!(child instanceof CheckBox check)) continue;
            String text = check.getText() == null ? "" : check.getText().toLowerCase(Locale.ROOT);
            if (!text.contains("5-bin") && !text.contains("media mobile")) continue;
            check.setMinWidth(118);
            check.setPrefWidth(132);
            check.setMaxWidth(145);
            HBox.setHgrow(check, Priority.NEVER);
        }
    }

    private static boolean isExplorerLightCurve(LineChart<?, ?> chart) {
        String title = chart.getTitle() == null ? "" : chart.getTitle().toLowerCase(Locale.ROOT);
        return title.contains("binning di 1 secondo") || title.contains("1-second-binned");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void prepareExplorerChart(LineChart<?, ?> rawChart) {
        if (Boolean.TRUE.equals(rawChart.getProperties().get(CHART_DONE))) return;
        rawChart.getProperties().put(CHART_DONE, Boolean.TRUE);
        LineChart<Number, Number> chart = (LineChart) rawChart;
        Platform.runLater(() -> ensureScientificSeries(chart));
        chart.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) Platform.runLater(() -> ensureScientificSeries(chart));
        });
    }

    private static void ensureScientificSeries(LineChart<Number, Number> target) {
        if (target == null || target.getScene() == null || hasScientificSeries(target)) return;
        LineChart<Number, Number> source = findBestSourceCurve(installedRoot, target);
        if (source == null) return;

        List<XYChart.Series<Number, Number>> copied = new ArrayList<>();
        for (XYChart.Series<Number, Number> series : source.getData()) {
            if (isTriggerSeries(series)) continue;
            XYChart.Series<Number, Number> clone = cloneSeries(series);
            if (!clone.getData().isEmpty()) copied.add(clone);
        }
        if (copied.isEmpty()) return;

        List<XYChart.Series<Number, Number>> trigger = new ArrayList<>();
        for (XYChart.Series<Number, Number> series : target.getData()) {
            if (isTriggerSeries(series)) trigger.add(series);
        }
        copied.addAll(trigger);
        target.getData().setAll(copied);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static LineChart<Number, Number> findBestSourceCurve(Node node, LineChart<Number, Number> target) {
        if (node == null) return null;
        if (node instanceof LineChart<?, ?> raw && raw != target && isExplorerLightCurve(raw)) {
            LineChart<Number, Number> candidate = (LineChart) raw;
            if (hasScientificSeries(candidate)) return candidate;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                LineChart<Number, Number> found = findBestSourceCurve(child, target);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean hasScientificSeries(LineChart<Number, Number> chart) {
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            if (!isTriggerSeries(series) && series.getData() != null && !series.getData().isEmpty()) return true;
        }
        return false;
    }

    private static boolean isTriggerSeries(XYChart.Series<Number, Number> series) {
        String name = series == null || series.getName() == null ? "" : series.getName().toLowerCase(Locale.ROOT);
        return name.startsWith("trigger") || name.contains("t = 0");
    }

    private static XYChart.Series<Number, Number> cloneSeries(XYChart.Series<Number, Number> source) {
        XYChart.Series<Number, Number> clone = new XYChart.Series<>();
        clone.setName(source.getName());
        for (XYChart.Data<Number, Number> point : source.getData()) {
            clone.getData().add(new XYChart.Data<>(point.getXValue(), point.getYValue()));
        }
        return clone;
    }

    private static void polishAllScrollbars(Node root) {
        if (root == null || root.getScene() == null) return;
        try {
            root.applyCss();
            if (root instanceof Parent parent) parent.layout();
        } catch (RuntimeException ignored) {
            // A skin can still be attaching during dynamic page construction.
        }
        for (Node candidate : root.lookupAll(".scroll-bar")) {
            if (candidate instanceof ScrollBar bar) polishScrollBar(bar);
        }
    }

    private static void polishScrollBar(ScrollBar bar) {
        if (bar == null) return;
        if (!Boolean.TRUE.equals(bar.getProperties().get(SCROLL_DONE))) {
            bar.getProperties().put(SCROLL_DONE, Boolean.TRUE);
            if (bar.getOrientation() == Orientation.VERTICAL) {
                bar.setMinWidth(SCROLLBAR_THICKNESS);
                bar.setPrefWidth(SCROLLBAR_THICKNESS);
                bar.setMaxWidth(SCROLLBAR_THICKNESS);
            } else {
                bar.setMinHeight(SCROLLBAR_THICKNESS);
                bar.setPrefHeight(SCROLLBAR_THICKNESS);
                bar.setMaxHeight(SCROLLBAR_THICKNESS);
            }
            bar.skinProperty().addListener((obs, oldSkin, newSkin) -> scheduleThumbPolish(bar));
            bar.heightProperty().addListener((obs, oldHeight, newHeight) -> scheduleThumbPolish(bar));
            bar.widthProperty().addListener((obs, oldWidth, newWidth) -> scheduleThumbPolish(bar));
            bar.visibleAmountProperty().addListener((obs, oldValue, newValue) -> scheduleThumbPolish(bar));
        }
        scheduleThumbPolish(bar);
    }

    private static void scheduleThumbPolish(ScrollBar bar) {
        Platform.runLater(() -> {
            polishThumb(bar);
            Platform.runLater(() -> polishThumb(bar));
        });
    }

    private static void polishThumb(ScrollBar bar) {
        if (bar == null) return;
        Node thumb = bar.lookup(".thumb");
        if (!(thumb instanceof Region region)) return;
        if (bar.getOrientation() == Orientation.VERTICAL) {
            region.setMinHeight(THUMB_MIN_LENGTH);
            region.setMinWidth(10);
            region.setPrefWidth(10);
        } else {
            region.setMinWidth(THUMB_MIN_LENGTH);
            region.setMinHeight(10);
            region.setPrefHeight(10);
        }
        bar.requestLayout();
    }
}
