package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.ThreeDChartPane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Multi-band selector shared by the Explorer 2D curve, its fullscreen copy and
 * the 3D energy-band view. The historical ChoiceBox remains the data refresh
 * engine; dynamic fullscreen roots are watched explicitly because InPlaceFullscreen
 * temporarily replaces the Scene root.
 */
public final class ExplorerBandSelectionEnhancer {
    private static final String WATCHED = ExplorerBandSelectionEnhancer.class.getName() + ".watched";
    private static final String SELECTOR_DONE = ExplorerBandSelectionEnhancer.class.getName() + ".selectorDone";
    private static final String CHART_DONE = ExplorerBandSelectionEnhancer.class.getName() + ".chartDone";
    private static final String STANDALONE_DONE = ExplorerBandSelectionEnhancer.class.getName() + ".standaloneDone";
    private static final String SERIES_CACHE = ExplorerBandSelectionEnhancer.class.getName() + ".seriesCache";
    private static final String FILTERING = ExplorerBandSelectionEnhancer.class.getName() + ".filtering";

    private static final String TOTAL = "Totale 15–350 keV";
    private static final String TOTAL_EN = "Total 15–350 keV";
    private static final String ALL_BANDS = "Tutte le bande";
    private static final List<String> BANDS = List.of(
            "15–25 keV", "25–50 keV", "50–100 keV", "100–350 keV");
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());

    private static Parent installedRoot;
    private static Scene installedScene;
    private static boolean totalMode = true;
    private static final LinkedHashSet<String> selectedBands = new LinkedHashSet<>();
    private static final Map<String, List<SeriesPoint>> globalSeriesData = new LinkedHashMap<>();

    private ExplorerBandSelectionEnhancer() { }

    public static void install(Parent root) {
        if (root == null) return;
        installedRoot = root;
        watch(root);
        observeScene(root);
        Platform.runLater(() -> scan(root));
    }

    /** Effective energy bands that the Explorer 3D view must display. */
    public static Set<String> effectiveBandsFor3D() {
        if (totalMode || selectedBands.isEmpty()) return new LinkedHashSet<>(BANDS);
        return new LinkedHashSet<>(selectedBands);
    }

    private static void observeScene(Parent root) {
        if (root.getScene() != null) watchScene(root.getScene());
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) watchScene(newScene);
        });
    }

    private static void watchScene(Scene scene) {
        if (scene == null) return;
        installedScene = scene;
        if (!WATCHED_SCENES.add(scene)) return;
        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (newRoot == null) return;
            Platform.runLater(() -> {
                watch(newRoot);
                scan(newRoot);
                refreshNode(newRoot);
            });
        });
        Parent current = scene.getRoot();
        if (current != null) Platform.runLater(() -> {
            watch(current);
            scan(current);
            refreshNode(current);
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
        if (node instanceof ChoiceBox<?> rawChoice) installBandSelector(rawChoice);
        if (node instanceof LineChart<?, ?> rawChart && rawChart.getStyleClass().contains("lightcurve-chart")) {
            installChartFilter(rawChart);
        }
    }

    @SuppressWarnings("unchecked")
    private static void installBandSelector(ChoiceBox<?> rawChoice) {
        if (Boolean.TRUE.equals(rawChoice.getProperties().get(SELECTOR_DONE))) return;
        if (!rawChoice.getItems().contains(ALL_BANDS)) return;
        if (!(rawChoice.getParent() instanceof HBox controls)) return;

        ChoiceBox<String> original = (ChoiceBox<String>) rawChoice;
        int index = controls.getChildren().indexOf(original);
        if (index < 0) return;

        original.getProperties().put(SELECTOR_DONE, Boolean.TRUE);
        BandMenuButton selector = new BandMenuButton(original);
        configureSelectorSize(selector);
        original.setVisible(false);
        original.setManaged(false);
        controls.getChildren().add(index, selector);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installChartFilter(LineChart<?, ?> rawChart) {
        if (Boolean.TRUE.equals(rawChart.getProperties().get(CHART_DONE))) {
            ensureFullscreenSelector((LineChart<Number, Number>) rawChart);
            return;
        }
        rawChart.getProperties().put(CHART_DONE, Boolean.TRUE);
        LineChart<Number, Number> chart = (LineChart) rawChart;
        chart.getData().addListener((ListChangeListener<XYChart.Series<Number, Number>>) change -> {
            if (Boolean.TRUE.equals(chart.getProperties().get(FILTERING))) return;
            Platform.runLater(() -> {
                rememberSeries(chart);
                applyBandFilter(chart);
                ensureFullscreenSelector(chart);
            });
        });
        Platform.runLater(() -> {
            rememberSeries(chart);
            applyBandFilter(chart);
            ensureFullscreenSelector(chart);
        });
    }

    private static void ensureFullscreenSelector(LineChart<Number, Number> chart) {
        if (!isExplorerCurve(chart) || Boolean.TRUE.equals(chart.getProperties().get(STANDALONE_DONE))) return;
        if (!(chart.getParent() instanceof VBox box)) return;
        if (containsBandMenu(box)) {
            chart.getProperties().put(STANDALONE_DONE, Boolean.TRUE);
            return;
        }
        ChoiceBox<String> master = findMasterOriginalChoice(installedRoot);
        BandMenuButton selector = new BandMenuButton(master);
        configureSelectorSize(selector);
        HBox toolbar = new HBox(8, selector);
        toolbar.getStyleClass().add("data-toolbar");
        int index = box.getChildren().indexOf(chart);
        if (index < 0) return;
        box.getChildren().add(index, toolbar);
        chart.getProperties().put(STANDALONE_DONE, Boolean.TRUE);
    }

    private static void configureSelectorSize(BandMenuButton selector) {
        selector.setMinWidth(175);
        selector.setPrefWidth(215);
        selector.setMaxWidth(270);
        HBox.setHgrow(selector, Priority.NEVER);
    }

    private static boolean isExplorerCurve(LineChart<?, ?> chart) {
        String title = chart.getTitle() == null ? "" : chart.getTitle().toLowerCase(java.util.Locale.ROOT);
        return title.contains("binning di 1 secondo") || title.contains("1-second-binned");
    }

    private static boolean containsBandMenu(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof BandMenuButton) return true;
            if (child instanceof Parent parent && containsBandMenu(parent)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static ChoiceBox<String> findMasterOriginalChoice(Node node) {
        if (node == null) return null;
        if (node instanceof ChoiceBox<?> choice
                && Boolean.TRUE.equals(choice.getProperties().get(SELECTOR_DONE))
                && choice.getItems().contains(ALL_BANDS)) {
            return (ChoiceBox<String>) choice;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                ChoiceBox<String> result = findMasterOriginalChoice(child);
                if (result != null) return result;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, XYChart.Series<Number, Number>> cache(LineChart<Number, Number> chart) {
        Object stored = chart.getProperties().get(SERIES_CACHE);
        if (stored instanceof Map<?, ?> map) return (Map<String, XYChart.Series<Number, Number>>) map;
        Map<String, XYChart.Series<Number, Number>> created = new LinkedHashMap<>();
        chart.getProperties().put(SERIES_CACHE, created);
        return created;
    }

    private static void rememberSeries(LineChart<Number, Number> chart) {
        if (!isExplorerCurve(chart)) return;
        Map<String, XYChart.Series<Number, Number>> cache = cache(chart);
        for (XYChart.Series<Number, Number> series : List.copyOf(chart.getData())) {
            String key = canonicalSeriesName(series.getName());
            if (key == null) continue;
            cache.put(key, series);
            List<SeriesPoint> points = new ArrayList<>(series.getData().size());
            for (XYChart.Data<Number, Number> point : series.getData()) {
                points.add(new SeriesPoint(point.getXValue(), point.getYValue()));
            }
            if (!points.isEmpty()) globalSeriesData.put(key, points);
        }
    }

    private static String canonicalSeriesName(String name) {
        if (name == null) return null;
        if (TOTAL.equals(name) || TOTAL_EN.equals(name)) return TOTAL;
        return BANDS.contains(name) ? name : null;
    }

    private static void applyBandFilter(LineChart<Number, Number> chart) {
        if (!isExplorerCurve(chart) || Boolean.TRUE.equals(chart.getProperties().get(FILTERING))) return;
        rememberSeries(chart);
        List<String> desiredNames = totalMode ? List.of(TOTAL)
                : selectedBands.isEmpty() ? BANDS : List.copyOf(selectedBands);

        List<XYChart.Series<Number, Number>> desired = new ArrayList<>();
        Map<String, XYChart.Series<Number, Number>> local = cache(chart);
        for (String name : desiredNames) {
            XYChart.Series<Number, Number> series = local.get(name);
            if (series == null) {
                series = seriesFromGlobal(name);
                if (series != null) local.put(name, series);
            }
            if (series != null) desired.add(series);
        }
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            String name = series.getName();
            if (name != null && name.toLowerCase(java.util.Locale.ROOT).startsWith("trigger")) desired.add(series);
        }
        if (desired.isEmpty()) return;
        if (sameSeries(chart.getData(), desired)) return;

        chart.getProperties().put(FILTERING, Boolean.TRUE);
        try {
            chart.getData().setAll(desired);
        } finally {
            chart.getProperties().put(FILTERING, Boolean.FALSE);
        }
    }

    private static XYChart.Series<Number, Number> seriesFromGlobal(String name) {
        List<SeriesPoint> points = globalSeriesData.get(name);
        if (points == null || points.isEmpty()) return null;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(TOTAL.equals(name) ? I18n.dynamic(TOTAL, TOTAL_EN) : name);
        for (SeriesPoint point : points) {
            series.getData().add(new XYChart.Data<>(point.x(), point.y()));
        }
        return series;
    }

    private static boolean sameSeries(List<XYChart.Series<Number, Number>> current,
                                      List<XYChart.Series<Number, Number>> desired) {
        if (current.size() != desired.size()) return false;
        for (int index = 0; index < current.size(); index++) {
            if (current.get(index) != desired.get(index)) return false;
        }
        return true;
    }

    private static void refreshAllLightCurves() {
        if (installedRoot == null) return;
        Platform.runLater(() -> {
            refreshNode(installedRoot);
            if (installedScene != null && installedScene.getRoot() != null && installedScene.getRoot() != installedRoot) {
                refreshNode(installedScene.getRoot());
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void refreshNode(Node node) {
        if (node == null) return;
        if (node instanceof BandMenuButton selector) selector.syncMenuState();
        if (node instanceof LineChart<?, ?> raw && raw.getStyleClass().contains("lightcurve-chart")) {
            LineChart<Number, Number> chart = (LineChart) raw;
            rememberSeries(chart);
            applyBandFilter(chart);
            ensureFullscreenSelector(chart);
        }
        if (node instanceof ThreeDChartPane pane) pane.refreshBandSelection();
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) refreshNode(child);
        }
    }

    private static final class BandMenuButton extends MenuButton {
        private final ChoiceBox<String> original;
        private final CheckMenuItem total = new CheckMenuItem();
        private final MenuItem all = new MenuItem();
        private final Map<String, CheckMenuItem> bandItems = new LinkedHashMap<>();
        private boolean internal;

        BandMenuButton(ChoiceBox<String> original) {
            this.original = original;
            getStyleClass().addAll("choice-box-modern", "band-multi-select");
            setFocusTraversable(false);

            total.setOnAction(event -> {
                if (internal) return;
                selectTotal();
            });
            all.setOnAction(event -> selectAllBands());
            getItems().addAll(total, all, new SeparatorMenuItem());

            for (String band : BANDS) {
                CheckMenuItem item = new CheckMenuItem(band);
                item.setOnAction(event -> {
                    if (internal) return;
                    updateBandsFromMenu(band, item);
                });
                bandItems.put(band, item);
                getItems().add(item);
            }

            I18n.languageProperty().addListener((obs, oldValue, newValue) -> refreshLabels());
            refreshLabels();
            syncMenuState();
        }

        private void syncMenuState() {
            internal = true;
            total.setSelected(totalMode);
            bandItems.forEach((band, item) -> item.setSelected(!totalMode && selectedBands.contains(band)));
            internal = false;
            refreshButtonText();
        }

        private void selectTotal() {
            totalMode = true;
            selectedBands.clear();
            syncMenuState();
            ChoiceBox<String> target = original != null ? original : findMasterOriginalChoice(installedRoot);
            if (target != null) target.setValue(TOTAL);
            refreshAllLightCurves();
        }

        private void selectAllBands() {
            totalMode = false;
            selectedBands.clear();
            selectedBands.addAll(BANDS);
            syncMenuState();
            applyBandsToOriginal();
            Platform.runLater(this::show);
        }

        private void updateBandsFromMenu(String changedBand, CheckMenuItem changedItem) {
            totalMode = false;
            selectedBands.clear();
            for (Map.Entry<String, CheckMenuItem> entry : bandItems.entrySet()) {
                if (entry.getValue().isSelected()) selectedBands.add(entry.getKey());
            }
            if (selectedBands.isEmpty()) {
                changedItem.setSelected(true);
                selectedBands.add(changedBand);
            }
            syncMenuState();
            applyBandsToOriginal();
            Platform.runLater(this::show);
        }

        private void applyBandsToOriginal() {
            refreshButtonText();
            ChoiceBox<String> target = original != null ? original : findMasterOriginalChoice(installedRoot);
            if (target == null) {
                refreshAllLightCurves();
                return;
            }
            if (ALL_BANDS.equals(target.getValue())) {
                target.setValue(TOTAL);
                Platform.runLater(() -> {
                    target.setValue(ALL_BANDS);
                    Platform.runLater(ExplorerBandSelectionEnhancer::refreshAllLightCurves);
                });
            } else {
                target.setValue(ALL_BANDS);
                Platform.runLater(ExplorerBandSelectionEnhancer::refreshAllLightCurves);
            }
        }

        private void refreshLabels() {
            total.setText(I18n.dynamic(TOTAL, TOTAL_EN));
            all.setText(I18n.dynamic("Tutte le bande", "All bands"));
            refreshButtonText();
        }

        private void refreshButtonText() {
            if (totalMode) {
                setText(I18n.dynamic(TOTAL, TOTAL_EN));
            } else if (selectedBands.size() == BANDS.size()) {
                setText(I18n.dynamic("Tutte le bande", "All bands"));
            } else if (selectedBands.size() == 1) {
                setText(selectedBands.iterator().next());
            } else {
                int count = selectedBands.size();
                setText(I18n.dynamic(count + " bande", count + " bands"));
            }
        }
    }

    private record SeriesPoint(Number x, Number y) { }
}
