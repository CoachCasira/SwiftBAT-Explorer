package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.SummaryItem;
import it.casiraghi.swiftbat.model.TabularData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ComparePage extends BorderPane {
    private final ObservableMap<String, GrbData> sessionData;
    private final ComboBox<String> first = new ComboBox<>();
    private final ComboBox<String> second = new ComboBox<>();
    private final Label firstMatches = UiFactory.label("", "compare-match-count");
    private final Label secondMatches = UiFactory.label("", "compare-match-count");
    private final CheckBox normalize = new CheckBox(I18n.t("Normalizza ogni curva sul proprio picco"));
    private final StackPane content = new StackPane();
    private List<String> availableNames = List.of();

    public ComparePage(ObservableMap<String, GrbData> sessionData) {
        this.sessionData = sessionData;
        getStyleClass().add("page-root");
        setPadding(new Insets(28, 34, 34, 34));
        setTop(buildHeader());
        setCenter(content);
        BorderPane.setMargin(content, new Insets(20, 0, 0, 0));
        configureSearch(first, firstMatches);
        configureSearch(second, secondMatches);
        sessionData.addListener((javafx.collections.MapChangeListener<String, GrbData>) change -> refreshChoices());
        first.valueProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        second.valueProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        normalize.selectedProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        refreshChoices();
    }

    private Node buildHeader() {
        VBox header = new VBox(14);
        VBox copy = new VBox(5, UiFactory.label("Confronta", "page-title"));

        first.getStyleClass().add("choice-box-modern");
        second.getStyleClass().add("choice-box-modern");
        first.setPrefWidth(235);
        second.setPrefWidth(235);
        normalize.getStyleClass().add("modern-check");

        VBox firstBox = new VBox(3, first, firstMatches);
        VBox secondBox = new VBox(3, second, secondMatches);
        HBox controls = new HBox(10);
        controls.getStyleClass().add("compare-control-bar");
        controls.setPadding(new Insets(13, 15, 13, 15));
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getChildren().addAll(
                UiFactory.label("Evento A", "toolbar-label"), firstBox,
                UiFactory.label("vs", "compare-vs"),
                UiFactory.label("Evento B", "toolbar-label"), secondBox,
                UiFactory.spacer(), normalize);
        header.getChildren().addAll(copy, controls);
        return header;
    }

    private void configureSearch(ComboBox<String> combo, Label counter) {
        combo.setEditable(true);
        combo.getEditor().setText("GRB");
        combo.getEditor().setPromptText("GRB…");
        combo.getEditor().textProperty().addListener((obs, oldValue, raw) -> updateSuggestions(combo, counter, raw));
        combo.setOnAction(event -> {
            String value = combo.getValue();
            if (value != null && availableNames.contains(value)) {
                combo.getEditor().setText(value);
                refreshComparison();
            }
        });
    }

    private void updateSuggestions(ComboBox<String> combo, Label counter, String raw) {
        String query = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (query.isBlank()) query = "GRB";
        if (!query.startsWith("GRB")) query = "GRB" + query;
        final String normalized = query;
        List<String> matches = availableNames.stream().filter(name -> name.startsWith(normalized)).toList();
        I18n.setText(counter, matches.size() + " corrispondenze", matches.size() + " matches");
        if (matches.size() <= 10 && !matches.isEmpty() && normalized.length() > 3) {
            combo.setItems(FXCollections.observableArrayList(matches));
            if (!combo.isShowing()) combo.show();
        } else {
            combo.hide();
            combo.setItems(FXCollections.observableArrayList());
        }
        if (matches.size() == 1 && matches.get(0).equals(normalized)) combo.setValue(matches.get(0));
    }

    private void refreshChoices() {
        List<String> names = new ArrayList<>(sessionData.keySet());
        names.sort(Comparator.reverseOrder());
        availableNames = List.copyOf(names);
        updateSuggestions(first, firstMatches, first.getEditor().getText());
        updateSuggestions(second, secondMatches, second.getEditor().getText());
        refreshComparison();
    }

    private String selected(ComboBox<String> combo) {
        String value = combo.getValue();
        if (value != null && sessionData.containsKey(value)) return value;
        String editor = combo.getEditor().getText();
        if (editor != null) {
            String normalized = editor.trim().toUpperCase(Locale.ROOT);
            if (!normalized.startsWith("GRB")) normalized = "GRB" + normalized;
            if (sessionData.containsKey(normalized)) return normalized;
        }
        return null;
    }

    private void refreshComparison() {
        String aName = selected(first);
        String bName = selected(second);
        if (sessionData.size() < 2 || aName == null || bName == null || aName.equals(bName)) {
            showEmpty();
            return;
        }
        GrbData a = sessionData.get(aName);
        GrbData b = sessionData.get(bName);
        if (a == null || b == null) { showEmpty(); return; }
        content.getChildren().setAll(buildComparison(a, b));
        I18n.localizeTree(content);
    }

    private void showEmpty() {
        VBox empty = new VBox(13);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        empty.getChildren().addAll(
                UiFactory.label("⇄", "empty-icon"),
                UiFactory.label("Apri almeno due GRB", "empty-title"));
        content.getChildren().setAll(empty);
    }

    private Node buildComparison(GrbData a, GrbData b) {
        VBox page = new VBox(16);
        FlowPane cards = new FlowPane(12, 12);
        cards.getChildren().addAll(
                compareMetric("Picco", value(a, "PEAK_RATE"), value(b, "PEAK_RATE"), a.grbName(), b.grbName()),
                compareMetric("Tempo del picco", value(a, "PEAK_TIME"), value(b, "PEAK_TIME"), a.grbName(), b.grbName()),
                compareMetric("Picco / errore", value(a, "PEAK_SNR"), value(b, "PEAK_SNR"), a.grbName(), b.grbName()),
                compareMetric("Durezza proxy", value(a, "HARDNESS_PROXY"), value(b, "HARDNESS_PROXY"), a.grbName(), b.grbName()),
                compareMetric("Esposizione completa", value(a, "FULL_EXPOSURE_FRACTION"), value(b, "FULL_EXPOSURE_FRACTION"), a.grbName(), b.grbName()));

        NumberAxis xAxis = new NumberAxis(-60, 60, 10);
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(I18n.t("Tempo dal trigger (s)"));
        yAxis.setLabel(I18n.t(normalize.isSelected() ? "Rate normalizzato" : "Rate totale (count/s)"));
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setTitle("±60 s");
        chart.getStyleClass().add("lightcurve-chart");
        chart.getData().add(seriesFor(a, normalize.isSelected()));
        chart.getData().add(seriesFor(b, normalize.isSelected()));
        VBox.setVgrow(chart, Priority.ALWAYS);

        VBox chartCard = UiFactory.card("Confronto temporale", "", chart);
        page.getChildren().add(chartCard);
        return page;
    }

    private VBox compareMetric(String title, String a, String b, String nameA, String nameB) {
        VBox card = new VBox(8);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(230);
        card.getChildren().addAll(
                UiFactory.label(title, "metric-eyebrow"),
                UiFactory.label(nameA + "  " + a, "compare-value-a"),
                UiFactory.label(nameB + "  " + b, "compare-value-b"));
        return card;
    }

    private XYChart.Series<Number, Number> seriesFor(GrbData data, boolean normalized) {
        TabularData table = data.asciiData().isEmpty() ? data.fitsData() : data.asciiData();
        int timeIndex = table.indexOf("TIME_FROM_TRIGGER_CENTER_S");
        int rateIndex = table.indexOf(data.asciiData().isEmpty() ? "RATE" : "RATE_15_350_KEV");
        List<double[]> points = new ArrayList<>();
        double peak = 0;
        for (List<String> row : table.rows()) {
            double time = parse(row, timeIndex);
            double rate = parse(row, rateIndex);
            if (Double.isFinite(time) && Double.isFinite(rate) && Math.abs(time) <= 60) {
                points.add(new double[]{time, rate});
                peak = Math.max(peak, Math.abs(rate));
            }
        }
        double scale = normalized && peak > 0 ? peak : 1;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(data.grbName());
        for (double[] point : points) series.getData().add(new XYChart.Data<>(point[0], point[1] / scale));
        return series;
    }

    private String value(GrbData data, String key) {
        SummaryItem item = data.summaryByKey().get(key);
        return item == null ? "n.d." : DisplayFormat.summary(key, item);
    }

    private double parse(List<String> row, int index) {
        if (index < 0 || index >= row.size()) return Double.NaN;
        try { return Double.parseDouble(row.get(index)); }
        catch (Exception ignored) { return Double.NaN; }
    }
}
