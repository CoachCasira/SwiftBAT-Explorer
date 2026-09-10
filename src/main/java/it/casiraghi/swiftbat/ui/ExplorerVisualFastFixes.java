package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Explorer-only incremental visual fixes.
 *
 * <p>This watcher deliberately stops at charts, selectors and virtualized
 * controls. The old global version descended into TableView/ListView skins and
 * chart internals, registering listeners on large numbers of transient JavaFX
 * nodes. That work was unnecessary and made Explorer scrolling progressively
 * heavier on macOS.</p>
 */
public final class ExplorerVisualFastFixes {
    private static final String WATCHED = ExplorerVisualFastFixes.class.getName() + ".watched";
    private static final String WINDOW_DONE = ExplorerVisualFastFixes.class.getName() + ".windowDone";
    private static final String CHART_DONE = ExplorerVisualFastFixes.class.getName() + ".chartDone";

    private static Parent installedRoot;

    private ExplorerVisualFastFixes() { }

    public static void install(Parent root) {
        if (root == null) return;
        installedRoot = root;
        watch(root);
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);

        // Never watch JavaFX skin internals for these controls. Their transient
        // children can be numerous (chart points / virtualized cells) and none
        // of them is needed by this class.
        if (node instanceof LineChart<?, ?>
                || node instanceof ChoiceBox<?>
                || node instanceof ComboBox<?>
                || node instanceof ListView<?>
                || node instanceof TableView<?>) {
            return;
        }

        if (node instanceof ScrollPane scroll) {
            if (scroll.getContent() != null) watch(scroll.getContent());
            if (!Boolean.TRUE.equals(scroll.getProperties().get(WATCHED))) {
                scroll.getProperties().put(WATCHED, Boolean.TRUE);
                scroll.contentProperty().addListener((obs, oldContent, newContent) -> watch(newContent));
            }
            return;
        }

        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
            }
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void enhance(Node node) {
        if (node instanceof ChoiceBox<?> choice) polishTimeWindowChoice(choice);
        else if (node instanceof ComboBox<?> combo) polishTimeWindowCombo(combo);
        else if (node instanceof LineChart<?, ?> chart && isExplorerLightCurve(chart)) prepareExplorerChart(chart);
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
        return title.contains("binning di 1 secondo") || title.contains("1-second-binned") || title.contains("1 second");
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

        for (XYChart.Series<Number, Number> series : target.getData()) {
            if (isTriggerSeries(series)) copied.add(series);
        }
        target.getData().setAll(copied);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static LineChart<Number, Number> findBestSourceCurve(Node node, LineChart<Number, Number> target) {
        if (node == null) return null;
        if (node instanceof LineChart<?, ?> raw && raw != target && isExplorerLightCurve(raw)) {
            LineChart<Number, Number> candidate = (LineChart) raw;
            if (hasScientificSeries(candidate)) return candidate;
        }
        if (node instanceof ScrollPane scroll) {
            return findBestSourceCurve(scroll.getContent(), target);
        }
        if (node instanceof ListView<?> || node instanceof TableView<?> || node instanceof ChoiceBox<?> || node instanceof ComboBox<?>) {
            return null;
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
}
