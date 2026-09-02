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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ComparePage extends BorderPane {
    private final ObservableMap<String, GrbData> sessionData;
    private final ChoiceBox<String> first = new ChoiceBox<>();
    private final ChoiceBox<String> second = new ChoiceBox<>();
    private final CheckBox normalize = new CheckBox("Normalizza ogni curva sul proprio picco");
    private final StackPane content = new StackPane();

    public ComparePage(ObservableMap<String, GrbData> sessionData) {
        this.sessionData = sessionData;
        getStyleClass().add("page-root");
        setPadding(new Insets(28, 34, 34, 34));
        setTop(buildHeader());
        setCenter(content);
        BorderPane.setMargin(content, new Insets(20, 0, 0, 0));
        sessionData.addListener((javafx.collections.MapChangeListener<String, GrbData>) change -> refreshChoices());
        first.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        second.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        normalize.selectedProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        refreshChoices();
    }

    private Node buildHeader() {
        VBox header = new VBox(14);
        VBox copy = new VBox(5,
                UiFactory.label("Confronta", "page-title"),
                UiFactory.wrappedLabel(
                        "Scegli due GRB già aperti e sovrapponi le loro curve di luce.",
                        "page-subtitle"));

        first.getStyleClass().add("choice-box-modern");
        second.getStyleClass().add("choice-box-modern");
        first.setPrefWidth(205);
        second.setPrefWidth(205);
        normalize.getStyleClass().add("modern-check");

        HBox controls = new HBox(10);
        controls.getStyleClass().add("compare-control-bar");
        controls.setPadding(new Insets(13, 15, 13, 15));
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getChildren().addAll(
                UiFactory.label("Evento A", "toolbar-label"), first,
                UiFactory.label("vs", "compare-vs"),
                UiFactory.label("Evento B", "toolbar-label"), second,
                UiFactory.spacer(), normalize);
        header.getChildren().addAll(copy, controls);
        return header;
    }

    private void refreshChoices() {
        List<String> names = new ArrayList<>(sessionData.keySet());
        names.sort(Comparator.reverseOrder());
        String previousFirst = first.getValue();
        String previousSecond = second.getValue();
        first.setItems(FXCollections.observableArrayList(names));
        second.setItems(FXCollections.observableArrayList(names));
        if (previousFirst != null && names.contains(previousFirst)) {
            first.setValue(previousFirst);
        } else if (!names.isEmpty()) {
            first.setValue(names.get(0));
        }
        if (previousSecond != null && names.contains(previousSecond)) {
            second.setValue(previousSecond);
        } else if (names.size() > 1) {
            second.setValue(names.get(1));
        }
        refreshComparison();
    }

    private void refreshComparison() {
        if (sessionData.size() < 2 || first.getValue() == null || second.getValue() == null
                || first.getValue().equals(second.getValue())) {
            showEmpty();
            return;
        }
        GrbData a = sessionData.get(first.getValue());
        GrbData b = sessionData.get(second.getValue());
        if (a == null || b == null) {
            showEmpty();
            return;
        }
        content.getChildren().setAll(buildComparison(a, b));
    }

    private void showEmpty() {
        VBox empty = new VBox(13);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        empty.getChildren().addAll(
                UiFactory.label("⇄", "empty-icon"),
                UiFactory.label("Apri almeno due GRB", "empty-title"),
                UiFactory.wrappedLabel(
                        "Gli eventi vengono mantenuti nella memoria della sessione. Aprine due dalla pagina Esplora, poi torna qui per sovrapporre le curve e confrontare gli indicatori.",
                        "empty-message"));
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
        xAxis.setLabel("Tempo dal trigger (s)");
        yAxis.setLabel(normalize.isSelected() ? "Rate normalizzato" : "Rate totale (count/s)");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setTitle("Sovrapposizione delle curve totali, finestra ±60 s");
        chart.getStyleClass().add("lightcurve-chart");
        chart.getData().add(seriesFor(a, normalize.isSelected()));
        chart.getData().add(seriesFor(b, normalize.isSelected()));
        VBox.setVgrow(chart, Priority.ALWAYS);

        VBox chartCard = UiFactory.card("Confronto temporale",
                "Normalizzare aiuta a confrontare la forma; lasciare i valori originali permette di confrontare anche l'intensità.", chart);
        VBox note = UiFactory.card("Come leggere il confronto", "",
                UiFactory.wrappedLabel(
                        "Due curve simili non implicano automaticamente la stessa origine fisica. Questo strumento serve a formulare domande e individuare differenze, non ad assegnare da solo una classe short o long.",
                        "explanation-text"));
        page.getChildren().addAll(cards, chartCard, note);
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
        for (double[] point : points) {
            series.getData().add(new XYChart.Data<>(point[0], point[1] / scale));
        }
        return series;
    }

    private String value(GrbData data, String key) {
        SummaryItem item = data.summaryByKey().get(key);
        if (item == null) {
            return "n.d.";
        }
        return DisplayFormat.summary(key, item);
    }

    private double parse(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(row.get(index));
        } catch (Exception ignored) {
            return Double.NaN;
        }
    }
}
