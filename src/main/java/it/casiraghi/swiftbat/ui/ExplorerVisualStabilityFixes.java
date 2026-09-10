package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.TableView;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private static Parent installedRoot;

    private ExplorerVisualStabilityFixes() { }

    public static void install(Parent root) {
        if (root == null) return;
        installedRoot = root;
        watch(root);
        Platform.runLater(() -> {
            scan(root);
            polishAllScrollbars(root);
        });
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
        if (node instanceof LineChart<?, ?> chart && isExplorerLightCurve(chart)) {
            prepareExplorerChart(chart);
        }
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
        for (Object item : items) {
            if (containsTriggerWindow(item)) matches++;
        }
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

        // Fullscreen charts are created dynamically. If their first frame contains
        // only the trigger marker, copy the currently visible scientific series
        // before the first paint instead of waiting for the user to touch a filter.
        Platform.runLater(() -> ensureScientificSeries(chart));
        chart.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) Platform.runLater(() -> ensureScientificSeries(chart));
        });
    }

    private static void ensureScientificSeries(LineChart<Number, Number> target) {
        if (target == null || target.getScene() == null) return;
        if (hasScientificSeries(target)) return;

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
                bar.setMinWidth(14);
                bar.setPrefWidth(14);
                bar.setMaxWidth(14);
            } else {
                bar.setMinHeight(14);
                bar.setPrefHeight(14);
                bar.setMaxHeight(14);
            }
            bar.skinProperty().addListener((obs, oldSkin, newSkin) ->
                    Platform.runLater(() -> polishThumb(bar)));
        }
        Platform.runLater(() -> polishThumb(bar));
    }

    private static void polishThumb(ScrollBar bar) {
        Node thumb = bar.lookup(".thumb");
        if (!(thumb instanceof Region region)) return;
        if (bar.getOrientation() == Orientation.VERTICAL) {
            region.setMinHeight(30);
            region.setPrefWidth(12);
        } else {
            region.setMinWidth(30);
            region.setPrefHeight(12);
        }
    }
}
