package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.LoadUpdate;
import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.service.CumulativeAnalysisService;
import it.casiraghi.swiftbat.service.QualityMetrics;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/** Analisi di gruppi di GRB con filtri scientifici e profilo mediano. */
public final class PopulationPage extends BorderPane {
    private static final String ALL_T90 = "Tutte le durate";
    private static final String SHORT_T90 = "Short · T90 ≤ 2 s";
    private static final String LONG_T90 = "Long · T90 > 2 s";
    private static final String UNKNOWN_T90 = "T90 non disponibile";
    private static final String ALL_Z = "Con e senza redshift";
    private static final String WITH_Z = "Solo con redshift";
    private static final String WITHOUT_Z = "Solo senza redshift";

    private final DataLoader loader;
    private final Executor taskExecutor;
    private final ExecutorService loaderExecutor;
    private final ObservableMap<String, GrbData> sessionData;
    private final CumulativeAnalysisService analysisService = new CumulativeAnalysisService();
    private final Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
    private final Map<String, SkyBurst> metadata = new LinkedHashMap<>();

    private final ChoiceBox<String> duration = new ChoiceBox<>();
    private final ChoiceBox<String> redshiftAvailability = new ChoiceBox<>();
    private final TextField zMin = field("0");
    private final TextField zMax = field("10");
    private final TextField raMin = field("0");
    private final TextField raMax = field("360");
    private final TextField decMin = field("-90");
    private final TextField decMax = field("90");
    private final Slider exposureMin = slider(0);
    private final Slider exposureMax = slider(100);
    private final Label exposureMinValue = UiFactory.label("0%", "filter-value");
    private final Label exposureMaxValue = UiFactory.label("100%", "filter-value");
    private final ChoiceBox<String> window = new ChoiceBox<>();
    private final ChoiceBox<String> limit = new ChoiceBox<>();
    private final Button analyze = UiFactory.button("Analizza il gruppo", "primary-button");
    private final Button cancel = UiFactory.button("Annulla", "ghost-button");
    private final Label candidatePreview = UiFactory.label("Filtri in preparazione…", "population-preview");
    private final Label status = UiFactory.label("Attendo catalogo e metadati", "status-pill", "status-neutral");
    private final ProgressBar progress = new ProgressBar(0);

    private final NumberAxis curveXAxis = new NumberAxis();
    private final NumberAxis curveYAxis = new NumberAxis();
    private final LineChart<Number, Number> curveChart = new LineChart<>(curveXAxis, curveYAxis);
    private final BarChart<String, Number> exposureHistogram = histogram("Copertura completa");
    private final BarChart<String, Number> t90Histogram = histogram("T90");
    private final BarChart<String, Number> redshiftHistogram = histogram("Redshift");
    private final TableView<PopulationEvent> resultTable = new TableView<>();
    private final TabPane resultTabs = new TabPane();
    private Task<AnalysisResult> runningTask;

    public PopulationPage(DataLoader loader, Executor taskExecutor, ExecutorService loaderExecutor,
                          ObservableMap<String, GrbData> sessionData) {
        this.loader = loader;
        this.taskExecutor = taskExecutor;
        this.loaderExecutor = loaderExecutor;
        this.sessionData = sessionData;
        getStyleClass().add("page-root");
        configureControls();
        setCenter(buildPage());
        sessionData.addListener((javafx.collections.MapChangeListener<String, GrbData>) change ->
                updateCandidatePreview());
    }

    public void setCatalog(List<CatalogEntry> entries) {
        catalog.clear();
        if (entries != null) {
            for (CatalogEntry entry : entries) {
                catalog.put(entry.grbName().toUpperCase(Locale.ROOT), entry);
            }
        }
        updateReadyState();
    }

    public void setBursts(List<SkyBurst> bursts) {
        metadata.clear();
        if (bursts != null) {
            for (SkyBurst burst : bursts) {
                metadata.put(burst.grbName().toUpperCase(Locale.ROOT), burst);
            }
        }
        updateReadyState();
    }

    private Node buildPage() {
        VBox page = new VBox(18);
        page.setPadding(new Insets(30, 34, 36, 34));
        page.getStyleClass().add("page-content");

        HBox title = new HBox(14);
        title.setAlignment(Pos.CENTER_LEFT);
        VBox copy = new VBox(5,
                UiFactory.label("Analisi di popolazione", "page-title"),
                UiFactory.wrappedLabel(
                        "Confronta la forma temporale di un gruppo di GRB e descrivi durata, distanza e qualità del campione.",
                        "page-subtitle"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        title.getChildren().addAll(copy, status);

        VBox filterCard = buildFilters();
        configureChart();
        configureTable();

        resultTabs.getStyleClass().add("main-tabs");
        resultTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        resultTabs.getTabs().addAll(
                new Tab("Profilo temporale", chartCard()),
                new Tab("Distribuzioni del campione", distributionPane()),
                new Tab("GRB inclusi", resultTable));
        resultTabs.setMinHeight(620);
        VBox.setVgrow(resultTabs, Priority.ALWAYS);

        page.getChildren().addAll(title, filterCard, resultTabs);
        ScrollPane scroll = new ScrollPane(page);
        scroll.getStyleClass().add("page-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox buildFilters() {
        duration.setItems(FXCollections.observableArrayList(ALL_T90, SHORT_T90, LONG_T90, UNKNOWN_T90));
        duration.setValue(ALL_T90);
        redshiftAvailability.setItems(FXCollections.observableArrayList(ALL_Z, WITH_Z, WITHOUT_Z));
        redshiftAvailability.setValue(ALL_Z);
        window.setItems(FXCollections.observableArrayList("±20 s", "±60 s", "±120 s"));
        window.setValue("±60 s");
        limit.setItems(FXCollections.observableArrayList("10", "25", "50", "100", "Tutti"));
        limit.setValue("25");
        for (ChoiceBox<?> choice : List.of(duration, redshiftAvailability, window, limit)) {
            choice.getStyleClass().add("choice-box-modern");
        }

        FlowPane primary = new FlowPane(12, 12);
        primary.getStyleClass().add("population-filter-grid");
        primary.getChildren().addAll(
                filterGroup("Durata T90", "Seleziona la classe temporale", duration, 210),
                filterGroup("Redshift", "Presenza della misura z", redshiftAvailability, 210),
                filterGroup("Finestra temporale", "Secondi attorno al trigger", window, 170),
                filterGroup("Campione massimo", "GRB più recenti dopo i filtri", limit, 170));

        FlowPane advancedContent = new FlowPane(12, 12);
        advancedContent.getStyleClass().addAll("population-filter-grid", "advanced-filter-row");
        advancedContent.getChildren().addAll(
                filterGroup("Intervallo redshift z", "Applicato ai GRB che hanno z", range(zMin, zMax), 220),
                filterGroup("Ascensione retta RA", "Intervallo 0°–360°", range(raMin, raMax), 220),
                filterGroup("Declinazione DEC", "Intervallo −90°–+90°", range(decMin, decMax), 220));
        advancedContent.setVisible(false);
        advancedContent.setManaged(false);

        ToggleButton advanced = new ToggleButton("Filtri avanzati: z e area di cielo");
        advanced.getStyleClass().add("sky-toggle");
        advanced.selectedProperty().addListener((obs, oldValue, selected) -> {
            advancedContent.setVisible(selected);
            advancedContent.setManaged(selected);
            advanced.setText(selected ? "Nascondi filtri avanzati" : "Filtri avanzati: z e area di cielo");
        });

        VBox minimum = exposureControl("Minimo ammesso", exposureMin, exposureMinValue);
        VBox maximum = exposureControl("Massimo ammesso", exposureMax, exposureMaxValue);
        FlowPane exposureControls = new FlowPane(22, 10, minimum, maximum);
        exposureControls.setAlignment(Pos.CENTER_LEFT);
        VBox exposureBox = new VBox(8,
                UiFactory.label("Qualità della copertura FRACEXP", "population-section-title"),
                UiFactory.wrappedLabel(
                        "Per ogni GRB viene calcolata la percentuale di bin con FRACEXP ≥ 0,999. Imposta qui l'intervallo accettato.",
                        "sky-filter-help"),
                exposureControls);
        exposureBox.getStyleClass().add("population-filter-section");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        progress.setPrefWidth(230);
        progress.setVisible(false);
        progress.setManaged(false);
        cancel.setDisable(true);
        analyze.setOnAction(event -> startAnalysis());
        cancel.setOnAction(event -> cancelAnalysis());
        Button reset = UiFactory.button("Ripristina filtri", "ghost-button");
        reset.setOnAction(event -> resetFilters());
        actions.getChildren().addAll(
                analyze, cancel, reset, progress);

        VBox footer = new VBox(4,
                candidatePreview,
                UiFactory.wrappedLabel(
                        "Prima vengono applicati T90, redshift e coordinate; FRACEXP richiede il FITS. I file scaricati restano nella cache locale anche dopo la chiusura.",
                        "sky-filter-help"));

        VBox card = new VBox(14, primary, advanced, advancedContent, exposureBox, actions, footer);
        card.getStyleClass().add("card");
        card.getStyleClass().add("population-filter-card");
        card.setPadding(new Insets(17));
        return card;
    }

    private Node chartCard() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));
        Label note = UiFactory.wrappedLabel(
                "Linee azzurre: singole GRB allineate al trigger e divise per il proprio picco. "
                        + "Linea arancione: valore mediano del gruppo in ogni secondo. Linee viola: 25° e 75° percentile. "
                        + "Si confronta la forma temporale relativa, non la luminosità assoluta e non la somma dei segnali.",
                "explanation-text");
        VBox.setVgrow(curveChart, Priority.ALWAYS);
        box.getChildren().addAll(note, curveChart);
        return box;
    }

    private Node distributionPane() {
        FlowPane flow = new FlowPane(14, 14);
        flow.setPadding(new Insets(16));
        flow.getChildren().addAll(
                histogramCard("Qualità FRACEXP",
                        "Quanti GRB hanno una determinata percentuale di bin completamente esposti", exposureHistogram),
                histogramCard("Durata T90",
                        "Quanti GRB inclusi ricadono in ciascun intervallo di durata", t90Histogram),
                histogramCard("Distanza cosmologica",
                        "Distribuzione del redshift dei GRB inclusi; n.d. indica un valore assente", redshiftHistogram));
        return flow;
    }

    private BarChart<String, Number> histogram(String title) {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setTitle(title);
        chart.setPrefSize(420, 330);
        return chart;
    }

    private VBox histogramCard(String title, String subtitle, BarChart<String, Number> chart) {
        return UiFactory.card(title, subtitle, chart);
    }

    private void configureControls() {
        exposureMin.valueProperty().addListener((obs, oldValue, value) -> {
            if (value.doubleValue() > exposureMax.getValue()) {
                exposureMin.setValue(exposureMax.getValue());
            }
            updateExposureLabel();
            updateCandidatePreview();
        });
        exposureMax.valueProperty().addListener((obs, oldValue, value) -> {
            if (value.doubleValue() < exposureMin.getValue()) {
                exposureMax.setValue(exposureMin.getValue());
            }
            updateExposureLabel();
            updateCandidatePreview();
        });
        duration.valueProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        redshiftAvailability.valueProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        limit.valueProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        for (TextField field : List.of(zMin, zMax, raMin, raMax, decMin, decMax)) {
            field.textProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        }
    }

    private void configureChart() {
        curveXAxis.setLabel("Tempo dal trigger (s)");
        curveYAxis.setLabel("Rate normalizzato (picco = 1)");
        curveChart.setAnimated(false);
        curveChart.setCreateSymbols(false);
        curveChart.setLegendVisible(false);
        curveChart.setTitle("Nessuna analisi eseguita");
        curveChart.getStyleClass().addAll("lightcurve-chart", "population-chart");
    }

    private void configureTable() {
        resultTable.getStyleClass().add("data-table");
        resultTable.setPlaceholder(UiFactory.wrappedLabel(
                "Nessun GRB incluso. Controlla i filtri oppure esegui una nuova analisi.",
                "empty-message"));
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultTable.getColumns().addAll(
                column("GRB", PopulationEvent::grbName),
                column("T90", event -> event.burst().formattedT90()),
                column("Classe", event -> event.burst().durationClass()),
                column("Redshift", event -> event.burst().redshift().displayValue()),
                column("Copertura", event -> String.format(Locale.ITALY, "%.2f%%", event.exposurePercent())),
                column("Flag qualità", PopulationEvent::qualityFlag));
    }

    private TableColumn<PopulationEvent, String> column(String title,
                                                         java.util.function.Function<PopulationEvent, String> mapper) {
        TableColumn<PopulationEvent, String> column = new TableColumn<>(title);
        column.setCellValueFactory(value -> new ReadOnlyStringWrapper(mapper.apply(value.getValue())));
        return column;
    }

    private void startAnalysis() {
        if (catalog.isEmpty() || metadata.isEmpty() || runningTask != null) {
            return;
        }
        Filter filter;
        try {
            filter = readFilter();
        } catch (IllegalArgumentException error) {
            setStatus(error.getMessage(), "status-warning");
            return;
        }
        List<Candidate> candidates = candidates(filter);
        if (candidates.isEmpty()) {
            setStatus("Nessun GRB corrisponde ai filtri di T90, z e cielo", "status-warning");
            clearResults();
            return;
        }
        int examined = Math.min(filter.limit(), candidates.size());
        List<Candidate> selected = List.copyOf(candidates.subList(0, examined));

        runningTask = new Task<>() {
            @Override
            protected AnalysisResult call() {
                List<PopulationEvent> accepted = new ArrayList<>();
                List<PopulationEvent> measured = new ArrayList<>();
                Map<String, GrbData> loaded = new LinkedHashMap<>();
                int failures = 0;
                ExecutorCompletionService<LoadedCandidate> completion =
                        new ExecutorCompletionService<>(loaderExecutor);
                List<Future<LoadedCandidate>> pending = new ArrayList<>();
                for (int index = 0; index < selected.size(); index++) {
                    int candidateIndex = index;
                    Candidate candidate = selected.get(index);
                    pending.add(completion.submit(() -> loadCandidate(candidateIndex, candidate)));
                }

                List<LoadedCandidate> outcomes = new ArrayList<>();
                try {
                    for (int completed = 0; completed < selected.size() && !isCancelled(); completed++) {
                        updateMessage("Caricamento parallelo · " + completed + "/" + selected.size());
                        try {
                            outcomes.add(completion.take().get());
                        } catch (ExecutionException error) {
                            failures++;
                        }
                        updateProgress(completed + 1, selected.size());
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (isCancelled() || outcomes.size() < selected.size()) {
                        pending.forEach(future -> future.cancel(true));
                    }
                }

                outcomes.sort(Comparator.comparingInt(LoadedCandidate::index));
                for (LoadedCandidate outcome : outcomes) {
                    if (outcome.data() == null) {
                        failures++;
                        continue;
                    }
                    GrbData data = outcome.data();
                    loaded.put(data.grbName(), data);
                    double quality = QualityMetrics.fullExposurePercent(data);
                    if (!Double.isFinite(quality)) {
                        failures++;
                        continue;
                    }
                    PopulationEvent event = new PopulationEvent(
                            data.grbName(), outcome.candidate().burst(), quality, "");
                    measured.add(event);
                    if (quality >= filter.exposureMin() && quality <= filter.exposureMax()) {
                        accepted.add(event);
                    }
                }
                double lowTail = QualityMetrics.percentile(
                        measured.stream().map(PopulationEvent::exposurePercent).toList(), 0.10);
                List<PopulationEvent> flagged = accepted.stream()
                        .map(event -> event.withQualityFlag(event.exposurePercent() <= lowTail
                                ? "Coda bassa (≤ 10° percentile)" : "Regolare"))
                        .toList();
                List<CumulativeAnalysisService.NormalizedCurve> curves = new ArrayList<>();
                for (PopulationEvent event : flagged) {
                    CumulativeAnalysisService.NormalizedCurve curve = analysisService.normalize(
                            loaded.get(event.grbName()), filter.halfWindow());
                    if (!curve.isEmpty()) {
                        curves.add(curve);
                    }
                }
                CumulativeAnalysisService.PopulationProfile profile = analysisService.profile(curves, filter.halfWindow());
                updateProgress(selected.size(), selected.size());
                return new AnalysisResult(flagged, measured, loaded, curves, profile, failures,
                        selected.size(), lowTail, filter.halfWindow(),
                        filter.exposureMin(), filter.exposureMax());
            }
        };
        progress.progressProperty().bind(runningTask.progressProperty());
        status.textProperty().bind(runningTask.messageProperty());
        setRunning(true);
        runningTask.setOnSucceeded(event -> finishAnalysis(runningTask.getValue()));
        runningTask.setOnCancelled(event -> finishCancelled());
        runningTask.setOnFailed(event -> finishFailed(runningTask.getException()));
        taskExecutor.execute(runningTask);
    }

    private LoadedCandidate loadCandidate(int index, Candidate candidate) {
        try {
            return new LoadedCandidate(index, candidate, loader.load(candidate.entry(), update -> { }));
        } catch (IOException | RuntimeException error) {
            return new LoadedCandidate(index, candidate, null);
        }
    }

    private void finishAnalysis(AnalysisResult result) {
        progress.progressProperty().unbind();
        status.textProperty().unbind();
        sessionData.putAll(result.loaded());
        populateCurveChart(result);
        resultTable.setItems(FXCollections.observableArrayList(result.accepted()));
        populateHistograms(result);
        StringBuilder message = new StringBuilder()
                .append(result.accepted().size()).append(" GRB inclusi su ")
                .append(result.examined()).append(" esaminati");
        if (!result.measured().isEmpty()) {
            double observedMinimum = result.measured().stream()
                    .mapToDouble(PopulationEvent::exposurePercent).min().orElse(Double.NaN);
            double observedMaximum = result.measured().stream()
                    .mapToDouble(PopulationEvent::exposurePercent).max().orElse(Double.NaN);
            message.append(String.format(Locale.ITALY, " · copertura rilevata %.1f%%–%.1f%%",
                    observedMinimum, observedMaximum));
            if (result.accepted().isEmpty()) {
                message.append(String.format(Locale.ITALY, " fuori dal filtro %.0f%%–%.0f%%",
                        result.exposureMinimum(), result.exposureMaximum()));
            }
        }
        if (result.failures() > 0) {
            message.append(" · ").append(result.failures()).append(" non leggibili");
        }
        setStatus(message.toString(),
                result.accepted().isEmpty() ? "status-warning" : "status-online");
        updateCandidatePreview();
        setRunning(false);
        runningTask = null;
    }

    private void finishCancelled() {
        progress.progressProperty().unbind();
        status.textProperty().unbind();
        setStatus("Analisi annullata", "status-warning");
        setRunning(false);
        runningTask = null;
    }

    private void finishFailed(Throwable error) {
        progress.progressProperty().unbind();
        status.textProperty().unbind();
        setStatus("Analisi non riuscita: " + (error == null ? "errore sconosciuto" : error.getMessage()), "status-warning");
        setRunning(false);
        runningTask = null;
    }

    private void cancelAnalysis() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }
    }

    private void populateCurveChart(AnalysisResult result) {
        curveChart.getData().clear();
        for (CumulativeAnalysisService.NormalizedCurve curve : result.curves()) {
            XYChart.Series<Number, Number> series = series(curve.grbName(), curve.points());
            curveChart.getData().add(series);
            styleSeries(series, "-fx-stroke: rgba(84, 215, 255, 0.20); -fx-stroke-width: 1px;");
        }
        XYChart.Series<Number, Number> lower = series("25° percentile", result.profile().lowerQuartile());
        XYChart.Series<Number, Number> upper = series("75° percentile", result.profile().upperQuartile());
        XYChart.Series<Number, Number> median = series("Mediana", result.profile().median());
        curveChart.getData().addAll(lower, upper, median);
        styleSeries(lower, "-fx-stroke: #aa78db; -fx-stroke-width: 2px; -fx-stroke-dash-array: 7 5;");
        styleSeries(upper, "-fx-stroke: #aa78db; -fx-stroke-width: 2px; -fx-stroke-dash-array: 7 5;");
        styleSeries(median, "-fx-stroke: #ffae4a; -fx-stroke-width: 4px;");
        curveXAxis.setAutoRanging(false);
        curveXAxis.setLowerBound(-result.halfWindow());
        curveXAxis.setUpperBound(result.halfWindow());
        curveXAxis.setTickUnit(result.halfWindow() <= 20 ? 5 : result.halfWindow() <= 60 ? 15 : 30);
        curveChart.setTitle(result.curves().size() + " curve normalizzate e allineate a t = 0");
    }

    private XYChart.Series<Number, Number> series(String name, List<CumulativeAnalysisService.Point> points) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(name);
        for (CumulativeAnalysisService.Point point : points) {
            series.getData().add(new XYChart.Data<>(point.time(), point.value()));
        }
        return series;
    }

    private void styleSeries(XYChart.Series<Number, Number> series, String style) {
        series.nodeProperty().addListener((obs, oldNode, node) -> {
            if (node != null) {
                Node line = node.lookup(".chart-series-line");
                if (line != null) {
                    line.setStyle(style);
                }
            }
        });
        Platform.runLater(() -> {
            Node node = series.getNode();
            if (node != null && node.lookup(".chart-series-line") != null) {
                node.lookup(".chart-series-line").setStyle(style);
            }
        });
    }

    private void populateHistograms(AnalysisResult result) {
        setBars(exposureHistogram, bins(result.measured().stream().map(PopulationEvent::exposurePercent).toList(),
                new double[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, "%"));

        Map<String, Integer> t90Bins = new LinkedHashMap<>();
        for (String label : List.of("≤2", "2–10", "10–50", "50–100", ">100", "n.d.")) t90Bins.put(label, 0);
        Map<String, Integer> zBins = new LinkedHashMap<>();
        for (String label : List.of("0–1", "1–2", "2–3", "3–4", "4–6", ">6", "n.d.")) zBins.put(label, 0);
        for (PopulationEvent event : result.accepted()) {
            Double value = event.burst().t90Sec();
            increment(t90Bins, value == null ? "n.d." : value <= 2 ? "≤2" : value <= 10 ? "2–10"
                    : value <= 50 ? "10–50" : value <= 100 ? "50–100" : ">100");
            Double z = event.burst().redshift().representativeValue();
            increment(zBins, z == null ? "n.d." : z < 1 ? "0–1" : z < 2 ? "1–2" : z < 3 ? "2–3"
                    : z < 4 ? "3–4" : z < 6 ? "4–6" : ">6");
        }
        setBars(t90Histogram, t90Bins);
        setBars(redshiftHistogram, zBins);
    }

    private void setBars(BarChart<String, Number> chart, Map<String, Integer> counts) {
        if (chart == null) return;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.forEach((label, count) -> {
            XYChart.Data<String, Number> bar = new XYChart.Data<>(label, count);
            bar.nodeProperty().addListener((obs, oldNode, node) -> {
                if (node != null) {
                    Tooltip.install(node, new Tooltip(label + ": " + count + " GRB"));
                }
            });
            series.getData().add(bar);
        });
        chart.getData().setAll(series);
    }

    private Map<String, Integer> bins(List<Double> values, double[] edges, String suffix) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < edges.length - 1; index++) {
            String label = (int) edges[index] + "–" + (int) edges[index + 1] + suffix;
            result.put(label, 0);
        }
        for (Double value : values) {
            if (value == null || !Double.isFinite(value)) continue;
            int bin = Math.min(edges.length - 2, Math.max(0, (int) Math.floor(value / 10.0)));
            String label = (int) edges[bin] + "–" + (int) edges[bin + 1] + suffix;
            increment(result, label);
        }
        return result;
    }

    private void increment(Map<String, Integer> values, String key) {
        values.computeIfPresent(key, (ignored, count) -> count + 1);
    }

    private List<Candidate> candidates(Filter filter) {
        List<Candidate> result = new ArrayList<>();
        for (CatalogEntry entry : catalog.values()) {
            SkyBurst burst = metadata.get(entry.grbName().toUpperCase(Locale.ROOT));
            if (burst == null || !filter.matches(burst)) {
                continue;
            }
            result.add(new Candidate(entry, burst));
        }
        return result;
    }

    private Filter readFilter() {
        double minZ = number(zMin, "Redshift minimo");
        double maxZ = number(zMax, "Redshift massimo");
        double minRa = number(raMin, "RA minima");
        double maxRa = number(raMax, "RA massima");
        double minDec = number(decMin, "DEC minima");
        double maxDec = number(decMax, "DEC massima");
        if (minZ < 0 || minZ > maxZ) throw new IllegalArgumentException("Controlla il range del redshift.");
        if (minRa < 0 || minRa > 360 || maxRa < 0 || maxRa > 360) throw new IllegalArgumentException("RA deve essere fra 0° e 360°.");
        if (minDec < -90 || maxDec > 90 || minDec > maxDec) throw new IllegalArgumentException("DEC deve essere fra −90° e +90°.");
        double halfWindow = window.getValue().startsWith("±20") ? 20 : window.getValue().startsWith("±120") ? 120 : 60;
        int maximumEvents = "Tutti".equals(limit.getValue())
                ? Integer.MAX_VALUE
                : Integer.parseInt(limit.getValue());
        return new Filter(duration.getValue(), redshiftAvailability.getValue(), minZ, maxZ,
                minRa, maxRa, minDec, maxDec, exposureMin.getValue(), exposureMax.getValue(),
                halfWindow, maximumEvents);
    }

    private double number(TextField field, String label) {
        try {
            return Double.parseDouble(field.getText().trim().replace(',', '.'));
        } catch (Exception error) {
            throw new IllegalArgumentException(label + " non è un numero valido.");
        }
    }

    private void updateReadyState() {
        boolean ready = !catalog.isEmpty() && !metadata.isEmpty();
        analyze.setDisable(!ready || runningTask != null);
        updateCandidatePreview();
        if (ready && runningTask == null) {
            setStatus(metadata.size() + " GRB con T90/coordinate · redshift integrato", "status-online");
        }
    }

    private void setRunning(boolean running) {
        analyze.setDisable(running || catalog.isEmpty() || metadata.isEmpty());
        cancel.setDisable(!running);
        progress.setVisible(running);
        progress.setManaged(running);
    }

    private void setStatus(String text, String style) {
        status.setText(text);
        status.getStyleClass().removeAll("status-neutral", "status-online", "status-warning");
        status.getStyleClass().add(style);
    }

    private void clearResults() {
        curveChart.getData().clear();
        resultTable.getItems().clear();
        exposureHistogram.getData().clear();
        t90Histogram.getData().clear();
        redshiftHistogram.getData().clear();
        curveChart.setTitle("Nessuna analisi eseguita");
    }

    private void updateExposureLabel() {
        exposureMinValue.setText(String.format(Locale.ITALY, "%.0f%%", exposureMin.getValue()));
        exposureMaxValue.setText(String.format(Locale.ITALY, "%.0f%%", exposureMax.getValue()));
    }

    private void updateCandidatePreview() {
        if (catalog.isEmpty() || metadata.isEmpty() || duration.getValue() == null
                || redshiftAvailability.getValue() == null || limit.getValue() == null
                || window.getValue() == null) {
            candidatePreview.setText("Attendo catalogo e metadati scientifici…");
            return;
        }
        try {
            Filter filter = readFilter();
            List<Candidate> matches = candidates(filter);
            int selected = Math.min(filter.limit(), matches.size());
            long inMemory = matches.stream()
                    .limit(selected)
                    .filter(candidate -> sessionData.containsKey(candidate.entry().grbName()))
                    .count();
            candidatePreview.setText(matches.size() + " GRB corrispondono ai filtri preliminari · "
                    + selected + " saranno esaminati · " + inMemory + " già in RAM");
        } catch (IllegalArgumentException error) {
            candidatePreview.setText(error.getMessage());
        }
    }

    private void resetFilters() {
        duration.setValue(ALL_T90);
        redshiftAvailability.setValue(ALL_Z);
        zMin.setText("0");
        zMax.setText("10");
        raMin.setText("0");
        raMax.setText("360");
        decMin.setText("-90");
        decMax.setText("90");
        exposureMin.setValue(0);
        exposureMax.setValue(100);
        window.setValue("±60 s");
        limit.setValue("25");
        updateCandidatePreview();
    }

    private static TextField field(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("sky-range-field");
        field.setPrefWidth(68);
        return field;
    }

    private static Slider slider(double value) {
        Slider slider = new Slider(0, 100, value);
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(24);
        slider.setSnapToTicks(true);
        slider.setPrefWidth(240);
        slider.setMinWidth(180);
        slider.setMaxWidth(260);
        return slider;
    }

    private static VBox exposureControl(String label, Slider slider, Label value) {
        HBox heading = new HBox(8, UiFactory.label(label, "filter-label"), UiFactory.spacer(), value);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(4, heading, slider);
        box.setPrefWidth(260);
        box.setMaxWidth(280);
        return box;
    }

    private static VBox filterGroup(String title, String detail, Node control, double width) {
        Label caption = UiFactory.label(title, "filter-label");
        Label note = UiFactory.label(detail, "filter-detail");
        if (control instanceof Region value) {
            value.setPrefWidth(width);
            value.setMaxWidth(width);
        }
        VBox box = new VBox(5, caption, control, note);
        box.getStyleClass().add("population-filter-group");
        box.setPrefWidth(width);
        return box;
    }

    private static HBox range(TextField minimum, TextField maximum) {
        HBox box = new HBox(6, minimum, UiFactory.label("–", "filter-label"), maximum);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private record Candidate(CatalogEntry entry, SkyBurst burst) {
    }

    private record LoadedCandidate(int index, Candidate candidate, GrbData data) {
    }

    private record Filter(String duration, String redshiftAvailability, double zMin, double zMax,
                          double raMin, double raMax, double decMin, double decMax,
                          double exposureMin, double exposureMax, double halfWindow, int limit) {
        boolean matches(SkyBurst burst) {
            if (SHORT_T90.equals(duration) && !burst.isShort()) return false;
            if (LONG_T90.equals(duration) && !burst.isLong()) return false;
            if (UNKNOWN_T90.equals(duration) && burst.hasT90()) return false;
            boolean hasZ = burst.redshift().available();
            if (WITH_Z.equals(redshiftAvailability) && !hasZ) return false;
            if (WITHOUT_Z.equals(redshiftAvailability) && hasZ) return false;
            if (hasZ && !burst.redshift().matches(zMin, zMax)) return false;
            if (burst.decDeg() < decMin || burst.decDeg() > decMax) return false;
            if (raMin == 0 && raMax == 360) return true;
            return raMin <= raMax ? burst.raDeg() >= raMin && burst.raDeg() <= raMax
                    : burst.raDeg() >= raMin || burst.raDeg() <= raMax;
        }
    }

    private record PopulationEvent(String grbName, SkyBurst burst, double exposurePercent, String qualityFlag) {
        PopulationEvent withQualityFlag(String value) {
            return new PopulationEvent(grbName, burst, exposurePercent, value);
        }
    }

    private record AnalysisResult(List<PopulationEvent> accepted, List<PopulationEvent> measured,
                                  Map<String, GrbData> loaded,
                                  List<CumulativeAnalysisService.NormalizedCurve> curves,
                                  CumulativeAnalysisService.PopulationProfile profile,
                                  int failures, int examined, double lowTail, double halfWindow,
                                  double exposureMinimum, double exposureMaximum) {
    }

    @FunctionalInterface
    public interface DataLoader {
        GrbData load(CatalogEntry entry, Consumer<LoadUpdate> progress) throws IOException;
    }
}
