package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.SummaryItem;
import it.casiraghi.swiftbat.model.TabularData;
import javafx.application.Platform;
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
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ComparePage extends javafx.scene.layout.BorderPane {
    private static final int MAX_SUGGESTIONS = 15;
    private static final int VISIBLE_SUGGESTIONS = 4;
    private static final String GHOST_KEY = ComparePage.class.getName() + ".ghostSuggestion";

    private final ObservableMap<String, GrbData> sessionData;
    private final Consumer<CatalogEntry> loadRequest;
    private final ComboBox<String> first = new ComboBox<>();
    private final ComboBox<String> second = new ComboBox<>();
    private final GhostHint firstGhost = ghostHint();
    private final GhostHint secondGhost = ghostHint();
    private final Label firstMatches = UiFactory.label("", "compare-match-count");
    private final Label secondMatches = UiFactory.label("", "compare-match-count");
    private final CheckBox normalize = new CheckBox(I18n.t("Normalizza ogni curva sul proprio picco"));
    private final StackPane content = new StackPane();
    private final Map<String, CatalogEntry> catalogEntries = new LinkedHashMap<>();
    private final Set<String> requestedLoads = new LinkedHashSet<>();
    private List<String> availableNames = List.of();

    public ComparePage(ObservableMap<String, GrbData> sessionData, Consumer<CatalogEntry> loadRequest) {
        this.sessionData = sessionData;
        this.loadRequest = loadRequest == null ? ignored -> {} : loadRequest;
        getStyleClass().add("page-root");
        setPadding(new Insets(28, 34, 34, 34));
        setTop(buildHeader());
        setCenter(content);
        javafx.scene.layout.BorderPane.setMargin(content, new Insets(20, 0, 0, 0));
        configureSearch(first, firstMatches, firstGhost, second);
        configureSearch(second, secondMatches, secondGhost, first);
        sessionData.addListener((javafx.collections.MapChangeListener<String, GrbData>) change -> {
            requestedLoads.remove(change.getKey());
            refreshChoices();
            refreshComparison();
        });
        first.valueProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        second.valueProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        normalize.selectedProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        refreshChoices();
    }

    public void setCatalog(List<CatalogEntry> entries) {
        catalogEntries.clear();
        if (entries != null) {
            for (CatalogEntry entry : entries) {
                if (entry != null && entry.grbName() != null) {
                    catalogEntries.put(entry.grbName().toUpperCase(Locale.ROOT), entry);
                }
            }
        }
        refreshChoices();
    }

    private Node buildHeader() {
        VBox header = new VBox(14);
        VBox copy = new VBox(5, UiFactory.label("Confronta", "page-title"));

        for (ComboBox<String> combo : List.of(first, second)) {
            combo.getStyleClass().addAll("choice-box-modern", "compare-combo");
            combo.setPrefWidth(235);
            combo.setMinWidth(215);
            combo.setVisibleRowCount(VISIBLE_SUGGESTIONS);
        }
        normalize.getStyleClass().add("modern-check");

        VBox firstBox = new VBox(3, searchStack(first, firstGhost), firstMatches);
        VBox secondBox = new VBox(3, searchStack(second, secondGhost), secondMatches);
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

    private StackPane searchStack(ComboBox<String> combo, GhostHint ghost) {
        StackPane stack = new StackPane(combo, ghost.box());
        StackPane.setAlignment(combo, Pos.CENTER_LEFT);
        StackPane.setAlignment(ghost.box(), Pos.CENTER_LEFT);
        ghost.box().setMouseTransparent(true);
        ghost.box().setPadding(new Insets(0, 42, 0, 13));
        stack.setMinWidth(215);
        stack.setPrefWidth(235);
        return stack;
    }

    private static GhostHint ghostHint() {
        Label prefix = new Label();
        prefix.getStyleClass().add("compare-ghost-text");
        prefix.setOpacity(0.0);
        Label suffix = new Label();
        suffix.getStyleClass().add("compare-ghost-text");
        HBox box = new HBox(0, prefix, suffix);
        box.setAlignment(Pos.CENTER_LEFT);
        return new GhostHint(box, prefix, suffix);
    }

    private void configureSearch(ComboBox<String> combo, Label counter, GhostHint ghost, ComboBox<String> other) {
        combo.setEditable(true);
        combo.getEditor().setText("GRB");
        combo.getEditor().setPromptText("");
        combo.getEditor().setTextFormatter(new TextFormatter<String>(change -> {
            String next = change.getControlNewText() == null ? "" : change.getControlNewText().toUpperCase(Locale.ROOT);
            if (!next.startsWith("GRB")) return null;
            String suffix = next.substring(3);
            if (!suffix.matches("[0-9A-Z]*")) return null;
            change.setText(change.getText().toUpperCase(Locale.ROOT));
            return change;
        }));
        combo.getEditor().textProperty().addListener((obs, oldValue, raw) -> updateSuggestions(combo, counter, ghost, other, raw));
        combo.getEditor().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                Platform.runLater(() -> combo.getEditor().positionCaret(Math.max(3, combo.getEditor().getCaretPosition())));
                updateSuggestions(combo, counter, ghost, other, combo.getEditor().getText());
            } else {
                combo.hide();
            }
        });
        combo.getEditor().setOnMouseClicked(event -> Platform.runLater(() -> {
            if (combo.getEditor().getCaretPosition() < 3) combo.getEditor().positionCaret(3);
        }));
        combo.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.HOME) {
                combo.getEditor().positionCaret(3);
                event.consume();
                return;
            }
            if (event.getCode() == KeyCode.LEFT && combo.getEditor().getCaretPosition() <= 3
                    && combo.getEditor().getSelection().getLength() == 0) {
                event.consume();
                return;
            }
            if (event.getCode() == KeyCode.TAB) {
                Object value = combo.getProperties().get(GHOST_KEY);
                if (value instanceof String suggestion && !suggestion.isBlank()) {
                    commitSelection(combo, suggestion);
                    event.consume();
                }
            }
        });
        combo.setOnAction(event -> {
            String value = combo.getValue();
            if (value != null && availableNames.contains(value)) commitSelection(combo, value);
        });
    }

    private void updateSuggestions(ComboBox<String> combo, Label counter, GhostHint ghost,
                                   ComboBox<String> other, String raw) {
        String normalized = normalize(raw);
        String excluded = exactSelection(other);
        List<String> matches = availableNames.stream()
                .filter(name -> excluded == null || !name.equals(excluded))
                .filter(name -> name.startsWith(normalized))
                .toList();
        I18n.setText(counter, matches.size() + " corrispondenze", matches.size() + " matches");

        String ghostValue = matches.stream()
                .filter(name -> name.length() > normalized.length())
                .findFirst().orElse("");
        combo.getProperties().put(GHOST_KEY, ghostValue);
        ghost.prefix().setText(normalized);
        ghost.suffix().setText(ghostValue.isBlank() ? "" : ghostValue.substring(normalized.length()));
        ghost.box().setVisible(combo.getEditor().isFocused() && !ghostValue.isBlank());

        if (matches.size() <= MAX_SUGGESTIONS && !matches.isEmpty() && normalized.length() > 3) {
            combo.setItems(FXCollections.observableArrayList(matches));
            if (combo.getEditor().isFocused() && !combo.isShowing()) Platform.runLater(combo::show);
        } else {
            combo.hide();
            combo.setItems(FXCollections.observableArrayList());
        }
    }

    private void commitSelection(ComboBox<String> combo, String value) {
        if (value == null || !availableNames.contains(value)) return;
        combo.setValue(value);
        combo.getEditor().setText(value);
        combo.getEditor().positionCaret(value.length());
        combo.hide();
        combo.getProperties().put(GHOST_KEY, "");
        ensureLoaded(value);
        refreshChoices();
        refreshComparison();
    }

    private String normalize(String raw) {
        String text = raw == null ? "GRB" : raw.trim().toUpperCase(Locale.ROOT);
        if (text.isBlank()) return "GRB";
        return text.startsWith("GRB") ? text : "GRB" + text;
    }

    private String exactSelection(ComboBox<String> combo) {
        if (combo == null) return null;
        String editor = normalize(combo.getEditor().getText());
        if (availableNames.contains(editor)) return editor;
        String value = combo.getValue();
        return value != null && availableNames.contains(value) ? value : null;
    }

    private void refreshChoices() {
        List<String> names = new ArrayList<>(catalogEntries.isEmpty() ? sessionData.keySet() : catalogEntries.keySet());
        names.replaceAll(name -> name.toUpperCase(Locale.ROOT));
        names.sort(Comparator.reverseOrder());
        availableNames = List.copyOf(names);
        updateSuggestions(first, firstMatches, firstGhost, second, first.getEditor().getText());
        updateSuggestions(second, secondMatches, secondGhost, first, second.getEditor().getText());
    }

    private String selected(ComboBox<String> combo) {
        return exactSelection(combo);
    }

    private void ensureLoaded(String name) {
        if (name == null || sessionData.containsKey(name) || requestedLoads.contains(name)) return;
        CatalogEntry entry = catalogEntries.get(name);
        if (entry == null) return;
        requestedLoads.add(name);
        loadRequest.accept(entry);
    }

    private void refreshComparison() {
        String aName = selected(first);
        String bName = selected(second);
        if (aName != null) ensureLoaded(aName);
        if (bName != null) ensureLoaded(bName);
        if (aName != null && aName.equals(bName)) {
            showEmpty("Scegli due GRB diversi", "Choose two different GRBs");
            return;
        }
        if (aName == null || bName == null) {
            showEmpty("Apri o seleziona almeno due GRB", "Open or select at least two GRBs");
            return;
        }
        GrbData a = sessionData.get(aName);
        GrbData b = sessionData.get(bName);
        if (a == null || b == null) {
            showEmpty("Caricamento dei GRB selezionati…", "Loading selected GRBs…");
            return;
        }
        content.getChildren().setAll(buildComparison(a, b));
        I18n.localizeTree(content);
    }

    private void showEmpty(String italian, String english) {
        VBox empty = new VBox(13);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        Label title = UiFactory.label("", "empty-title");
        I18n.setText(title, italian, english);
        empty.getChildren().addAll(UiFactory.label("⇄", "empty-icon"), title);
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
        page.getChildren().addAll(cards, chartCard);
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

    private record GhostHint(HBox box, Label prefix, Label suffix) {}
}
