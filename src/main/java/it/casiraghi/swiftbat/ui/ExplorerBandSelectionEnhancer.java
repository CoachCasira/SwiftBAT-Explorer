package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Estende il selettore delle bande della curva 2D dell'Explorer senza toccare
 * la logica scientifica di {@link ExplorerPage}. Il ChoiceBox storico resta il
 * motore di refresh del grafico; questo controllo gli aggiunge la possibilita'
 * di scegliere un sottoinsieme arbitrario delle quattro bande ASCII.
 *
 * <p>La modalita' "Totale 15–350 keV" resta esclusiva, mentre le quattro bande
 * possono essere combinate liberamente. Quando si selezionano piu' bande il
 * ChoiceBox originale viene portato su "Tutte le bande" e le serie non scelte
 * vengono eliminate dalla vista e dalla legenda. Lo stesso filtro viene
 * applicato anche alle copie fullscreen del grafico.</p>
 */
public final class ExplorerBandSelectionEnhancer {
    private static final String WATCHED = ExplorerBandSelectionEnhancer.class.getName() + ".watched";
    private static final String SELECTOR_DONE = ExplorerBandSelectionEnhancer.class.getName() + ".selectorDone";
    private static final String CHART_DONE = ExplorerBandSelectionEnhancer.class.getName() + ".chartDone";

    private static final String TOTAL = "Totale 15–350 keV";
    private static final String ALL_BANDS = "Tutte le bande";
    private static final List<String> BANDS = List.of(
            "15–25 keV", "25–50 keV", "50–100 keV", "100–350 keV");

    private static Parent installedRoot;
    private static boolean totalMode = true;
    private static final LinkedHashSet<String> selectedBands = new LinkedHashSet<>();

    private ExplorerBandSelectionEnhancer() { }

    public static void install(Parent root) {
        if (root == null) return;
        installedRoot = root;
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
        if (node instanceof ChoiceBox<?> rawChoice) {
            installBandSelector(rawChoice);
        }
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
        selector.setMinWidth(Math.max(175, original.getMinWidth()));
        selector.setPrefWidth(Math.max(205, original.getPrefWidth()));
        selector.setMaxWidth(255);
        HBox.setHgrow(selector, Priority.NEVER);

        original.setVisible(false);
        original.setManaged(false);
        controls.getChildren().add(index, selector);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installChartFilter(LineChart<?, ?> rawChart) {
        if (Boolean.TRUE.equals(rawChart.getProperties().get(CHART_DONE))) return;
        rawChart.getProperties().put(CHART_DONE, Boolean.TRUE);
        LineChart<Number, Number> chart = (LineChart) rawChart;
        chart.getData().addListener((ListChangeListener<XYChart.Series<Number, Number>>) change ->
                Platform.runLater(() -> applyBandFilter(chart)));
        Platform.runLater(() -> applyBandFilter(chart));
    }

    private static void applyBandFilter(LineChart<Number, Number> chart) {
        if (chart == null || totalMode || selectedBands.isEmpty()) return;
        List<XYChart.Series<Number, Number>> remove = new ArrayList<>();
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            String name = series.getName();
            if (name == null || name.startsWith("Trigger")) continue;
            if (BANDS.contains(name) && !selectedBands.contains(name)) {
                remove.add(series);
            }
        }
        if (!remove.isEmpty()) chart.getData().removeAll(remove);
    }

    private static void refreshAllLightCurves() {
        if (installedRoot == null) return;
        Platform.runLater(() -> scan(installedRoot));
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

            if (!totalMode && !selectedBands.isEmpty()) {
                applyBandsToOriginal();
            } else {
                selectTotal();
            }
        }

        private void selectTotal() {
            totalMode = true;
            selectedBands.clear();
            internal = true;
            total.setSelected(true);
            bandItems.values().forEach(item -> item.setSelected(false));
            internal = false;
            original.setValue(TOTAL);
            refreshButtonText();
            refreshAllLightCurves();
        }

        private void selectAllBands() {
            totalMode = false;
            selectedBands.clear();
            selectedBands.addAll(BANDS);
            internal = true;
            total.setSelected(false);
            bandItems.forEach((band, item) -> item.setSelected(true));
            internal = false;
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
            internal = true;
            total.setSelected(false);
            internal = false;
            applyBandsToOriginal();
            Platform.runLater(this::show);
        }

        /**
         * Il grafico storico si ricostruisce soltanto quando cambia davvero il
         * valore del ChoiceBox. Dopo la prima selezione multipla il suo valore e'
         * gia' "Tutte le bande": impostarlo di nuovo allo stesso valore non emette
         * alcun change event e quindi non ripristina le serie eliminate dal filtro.
         *
         * Forziamo percio' un refresh completo passando brevemente dal totale e poi
         * tornando a tutte le bande. Solo dopo la ricostruzione applichiamo il
         * sottoinsieme scelto dall'utente. In questo modo aggiungere una seconda,
         * terza o quarta banda rende visibili davvero tutte le curve selezionate.
         */
        private void applyBandsToOriginal() {
            refreshButtonText();
            if (ALL_BANDS.equals(original.getValue())) {
                original.setValue(TOTAL);
                Platform.runLater(() -> {
                    original.setValue(ALL_BANDS);
                    Platform.runLater(ExplorerBandSelectionEnhancer::refreshAllLightCurves);
                });
            } else {
                original.setValue(ALL_BANDS);
                Platform.runLater(ExplorerBandSelectionEnhancer::refreshAllLightCurves);
            }
        }

        private void refreshLabels() {
            total.setText(I18n.dynamic("Totale 15–350 keV", "Total 15–350 keV"));
            all.setText(I18n.dynamic("Tutte le bande", "All bands"));
            refreshButtonText();
        }

        private void refreshButtonText() {
            if (totalMode) {
                setText(I18n.dynamic("Totale 15–350 keV", "Total 15–350 keV"));
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
}
