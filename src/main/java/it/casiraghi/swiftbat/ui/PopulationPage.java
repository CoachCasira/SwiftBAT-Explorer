package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.LoadUpdate;
import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.service.CumulativeAnalysisService;
import it.casiraghi.swiftbat.service.PopulationInsightService;
import it.casiraghi.swiftbat.service.QualityMetrics;
import it.casiraghi.swiftbat.ui.components.Java2DGroupedBarPanel;
import it.casiraghi.swiftbat.ui.components.Population3DChartPane;
import it.casiraghi.swiftbat.ui.components.PopulationHistogram3DPane;
import javafx.animation.PauseTransition;
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
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
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
    private final PopulationInsightService insightService = new PopulationInsightService();
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
    private final ScrollBar exposureMinSlider = exposureSlider(0);
    private final ScrollBar exposureMaxSlider = exposureSlider(100);
    private final TextField exposureMin = percentField("0");
    private final TextField exposureMax = percentField("100");
    private final PauseTransition exposureMinDebounce = new PauseTransition(Duration.millis(350));
    private final PauseTransition exposureMaxDebounce = new PauseTransition(Duration.millis(350));
    private boolean syncingExposureControls;
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
    private final Button profile3D = UiFactory.button("Vista 3D interattiva", "secondary-button");
    private final Button exposure3D = UiFactory.button("Vista 3D", "secondary-button");
    private final Button t90ThreeD = UiFactory.button("Vista 3D", "secondary-button");
    private final Button redshift3D = UiFactory.button("Vista 3D", "secondary-button");
    private final Label insightHeadline = UiFactory.wrappedLabel(
            "Esegui un'analisi per ottenere un commento automatico sul campione.", "population-insight-headline");
    private final VBox insightObservations = new VBox(7);
    private final VBox insightCautions = new VBox(6);
    private Java2DGroupedBarPanel.Dataset exposure3DDataset = Java2DGroupedBarPanel.Dataset.empty();
    private Java2DGroupedBarPanel.Dataset t90ThreeDDataset = Java2DGroupedBarPanel.Dataset.empty();
    private Java2DGroupedBarPanel.Dataset redshift3DDataset = Java2DGroupedBarPanel.Dataset.empty();
    private AnalysisResult lastResult;
    private PopulationInsightService.Narrative lastNarrative;
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
        VBox page = new VBox(12);
        page.setPadding(new Insets(18, 24, 24, 24));
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
        resultTabs.setMinHeight(450);
        resultTabs.setPrefHeight(500);
        resultTabs.setMaxHeight(560);

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
            UiFactory.autoTooltip(choice);
        }

        FlowPane primary = new FlowPane(9, 7);
        primary.getStyleClass().add("population-filter-grid");
        primary.getChildren().addAll(
                filterGroup("Durata T90", "Classe temporale", duration, 185),
                filterGroup("Redshift", "Disponibilità della misura z", redshiftAvailability, 190),
                filterGroup("Finestra temporale", "Secondi attorno al trigger", window, 150),
                filterGroup("Campione massimo", "GRB più recenti dopo i filtri", limit, 150));
        primary.setMinWidth(650);
        primary.setPrefWrapLength(690);

        HBox advancedContent = new HBox(10);
        advancedContent.setAlignment(Pos.TOP_LEFT);
        advancedContent.getChildren().addAll(
                filterGroup("Intervallo redshift z", "Applicato ai GRB con z", range(zMin, zMax), 205),
                filterGroup("Ascensione retta RA", "Intervallo 0°–360°", range(raMin, raMax), 205),
                filterGroup("Declinazione DEC", "Intervallo −90°–+90°", range(decMin, decMax), 205));
        advancedContent.setMaxWidth(635);
        advancedContent.setVisible(false);
        advancedContent.setManaged(false);

        ToggleButton advanced = new ToggleButton("Filtri avanzati: z e area di cielo");
        advanced.getStyleClass().add("sky-toggle");
        advanced.selectedProperty().addListener((obs, oldValue, selected) -> {
            advancedContent.setVisible(selected);
            advancedContent.setManaged(selected);
            advanced.setText(selected ? "Nascondi filtri avanzati" : "Filtri avanzati: z e area di cielo");
        });

        VBox minimum = exposureControl("Minimo ammesso", exposureMinSlider, exposureMin, true);
        VBox maximum = exposureControl("Massimo ammesso", exposureMaxSlider, exposureMax, false);
        HBox exposureControls = new HBox(8, minimum, maximum);
        exposureControls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(minimum, Priority.ALWAYS);
        HBox.setHgrow(maximum, Priority.ALWAYS);

        VBox exposureIntro = new VBox(2,
                UiFactory.label("Qualità della copertura FRACEXP", "population-section-title"),
                UiFactory.wrappedLabel(
                        "Percentuale di bin con FRACEXP ≥ 0,999. Trascina oppure scrivi il valore.",
                        "sky-filter-help"));
        VBox exposureBox = new VBox(7, exposureIntro, exposureControls);
        exposureBox.setAlignment(Pos.TOP_LEFT);
        exposureBox.getStyleClass().addAll("population-filter-section", "population-filter-side");
        exposureBox.setMinWidth(430);
        exposureBox.setPrefWidth(500);
        exposureBox.setMaxWidth(Double.MAX_VALUE);

        HBox topFilters = new HBox(12, primary, exposureBox);
        topFilters.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(primary, Priority.ALWAYS);
        HBox.setHgrow(exposureBox, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        progress.setPrefWidth(190);
        progress.setVisible(false);
        progress.setManaged(false);
        cancel.setDisable(true);
        analyze.setOnAction(event -> startAnalysis());
        cancel.setOnAction(event -> cancelAnalysis());
        Button reset = UiFactory.button("Ripristina filtri", "ghost-button");
        reset.setOnAction(event -> resetFilters());
        actions.getChildren().addAll(analyze, cancel, reset, progress);

        VBox preview = new VBox(2,
                candidatePreview,
                UiFactory.wrappedLabel(
                        "T90, redshift e coordinate vengono applicati prima; FRACEXP richiede il FITS e usa la cache locale.",
                        "sky-filter-help"));
        preview.setMinWidth(0);
        preview.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(preview, Priority.ALWAYS);
        HBox footer = new HBox(16, actions, preview);
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(9, topFilters, advanced, advancedContent, footer);
        card.getStyleClass().addAll("card", "population-filter-card", "population-filter-card-compact");
        card.setPadding(new Insets(12));
        return card;
    }

    private Node chartCard() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14, 16, 16, 16));
        box.getStyleClass().add("population-chart-card");

        Label note = UiFactory.wrappedLabel(
                "Asse X: secondi dal trigger. Asse Y: rate relativo, con il picco di ogni GRB posto uguale a 1. "
                        + "Il grafico confronta la forma temporale e non somma i segnali.",
                "explanation-text");
        HBox.setHgrow(note, Priority.ALWAYS);
        profile3D.setDisable(true);
        profile3D.setOnAction(event -> openProfile3D());
        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");
        fullscreen.setOnAction(event -> openProfileFullscreen());
        HBox actions = new HBox(8, profile3D, fullscreen);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(12, note, actions);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox chartArea = new VBox(8, profileLegend(), curveChart);
        HBox.setHgrow(chartArea, Priority.ALWAYS);
        VBox.setVgrow(curveChart, Priority.ALWAYS);
        HBox body = new HBox(14, chartArea, insightCard());
        HBox.setHgrow(chartArea, Priority.ALWAYS);
        box.getChildren().addAll(header, body);
        return box;
    }

    private VBox insightCard() {
        insightHeadline.setMinWidth(0);
        insightHeadline.setMaxWidth(Double.MAX_VALUE);
        insightHeadline.setWrapText(false);
        insightHeadline.setTextOverrun(OverrunStyle.ELLIPSIS);
        UiFactory.autoTooltip(insightHeadline);
        insightObservations.getStyleClass().add("population-insight-list");
        insightCautions.getStyleClass().add("population-insight-cautions");
        VBox content = new VBox(10,
                UiFactory.label("Assistente di lettura", "population-insight-title"),
                UiFactory.label("ANALISI LOCALE · RIPRODUCIBILE", "population-insight-badge"),
                insightHeadline,
                insightObservations,
                insightCautions,
                UiFactory.wrappedLabel(
                        "Il testo deriva solo dalle statistiche del grafico e non sostituisce l'interpretazione scientifica.",
                        "population-insight-footnote"));
        content.getStyleClass().add("population-insight-card");
        content.setFillWidth(true);
        content.setMinWidth(270);
        content.setPrefWidth(330);
        content.setMaxWidth(370);
        return content;
    }

    private FlowPane profileLegend() {
        FlowPane legend = new FlowPane(10, 8);
        legend.getStyleClass().add("population-profile-legend");
        legend.getChildren().addAll(
                profileLegendItem("population-legend-single", "Singoli GRB",
                        "linee normalizzate e allineate al trigger"),
                profileLegendItem("population-legend-median", "Mediana",
                        "comportamento centrale del gruppo a ogni secondo"),
                profileLegendItem("population-legend-quartile", "Fascia centrale 25°–75°",
                        "tra i due limiti cade il 50% centrale delle curve"));
        return legend;
    }

    private HBox profileLegendItem(String lineStyle, String title, String detail) {
        Line sample = new Line(0, 0, 32, 0);
        sample.getStyleClass().addAll("population-legend-line", lineStyle);
        VBox text = new VBox(1,
                UiFactory.label(title, "population-legend-title"),
                UiFactory.label(detail, "population-legend-detail"));
        HBox item = new HBox(9, sample, text);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("population-legend-item");
        if (title.startsWith("Fascia centrale")) {
            Tooltip.install(item, UiFactory.quickTooltip(
                    "A ogni secondo si ordinano i valori delle curve: il 25° percentile lascia sotto di sé il 25% dei valori, "
                            + "il 75° percentile ne lascia sotto il 75%. Tra i due rimane quindi il 50% centrale del campione."));
        }
        return item;
    }

    private void openProfileFullscreen() {
        LineChart<Number, Number> enlarged = copyProfileChart();
        enlarged.setMinHeight(0);
        enlarged.setPrefHeight(760);
        enlarged.setMaxHeight(Double.MAX_VALUE);

        VBox chartColumn = new VBox(12, profileLegend(), enlarged);
        chartColumn.setMinWidth(0);
        chartColumn.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(enlarged, Priority.ALWAYS);
        HBox.setHgrow(chartColumn, Priority.ALWAYS);

        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        HBox content = new HBox(18, chartColumn, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.setMinSize(0, 0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("population-fullscreen-content");
        InPlaceFullscreen.show(this, "Profilo temporale della popolazione", content);
    }

    private VBox insightSnapshotCard(boolean expanded) {
        String headlineText = lastNarrative == null ? insightHeadline.getText() : lastNarrative.headline();
        List<String> observations = lastNarrative == null ? List.of() : lastNarrative.observations();
        List<String> cautions = lastNarrative == null ? List.of() : lastNarrative.cautions();

        Label headline = UiFactory.wrappedLabel(headlineText, "population-insight-headline");
        headline.setMinWidth(0);
        headline.setMaxWidth(Double.MAX_VALUE);
        headline.setTextOverrun(OverrunStyle.CLIP);

        VBox observationBox = new VBox(expanded ? 10 : 7);
        observationBox.getStyleClass().add("population-insight-list");
        observationBox.getChildren().setAll(observations.stream()
                .map(text -> insightLine("●", text, "population-insight-dot", expanded))
                .toList());

        VBox cautionBox = new VBox(expanded ? 9 : 6);
        cautionBox.getStyleClass().add("population-insight-cautions");
        if (!cautions.isEmpty()) {
            cautionBox.getChildren().add(UiFactory.label("Da tenere presente", "population-insight-caution-title"));
            cautions.stream().limit(expanded ? cautions.size() : 3)
                    .map(text -> insightLine("!", text, "population-insight-warning", expanded))
                    .forEach(cautionBox.getChildren()::add);
        }

        VBox card = new VBox(expanded ? 13 : 10,
                UiFactory.label("Assistente di lettura", "population-insight-title"),
                UiFactory.label("ANALISI LOCALE · RIPRODUCIBILE", "population-insight-badge"),
                headline, observationBox, cautionBox,
                UiFactory.wrappedLabel(
                        "Il testo deriva solo dalle statistiche del grafico e non sostituisce l'interpretazione scientifica.",
                        "population-insight-footnote"));
        card.getStyleClass().add("population-insight-card");
        if (expanded) card.getStyleClass().add("population-insight-expanded");
        card.setFillWidth(true);
        card.setMinWidth(expanded ? 390 : 270);
        card.setPrefWidth(expanded ? 480 : 330);
        card.setMaxWidth(expanded ? 560 : 370);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    private void openProfile3D() {
        if (lastResult == null || lastResult.curves().isEmpty()) {
            return;
        }
        Population3DChartPane pane = new Population3DChartPane();
        pane.setData(lastResult.curves(), lastResult.profile(), lastResult.halfWindow());
        pane.setMinWidth(0);
        pane.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pane, Priority.ALWAYS);

        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        HBox content = new HBox(18, pane, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.setMinSize(0, 0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("population-fullscreen-content");
        InPlaceFullscreen.show(this, "Profilo di popolazione 3D", content);
    }

    private LineChart<Number, Number> copyProfileChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<Number, Number> copy = new LineChart<>(xAxis, yAxis);
        configureChart(copy, xAxis, yAxis);
        copy.setTitle(curveChart.getTitle());
        copyAxis(curveXAxis, xAxis);
        copyAxis(curveYAxis, yAxis);

        for (XYChart.Series<Number, Number> source : curveChart.getData()) {
            XYChart.Series<Number, Number> target = new XYChart.Series<>();
            target.setName(source.getName());
            for (XYChart.Data<Number, Number> point : source.getData()) {
                target.getData().add(new XYChart.Data<>(point.getXValue(), point.getYValue()));
            }
            copy.getData().add(target);
            styleSeries(target, profileSeriesStyle(target.getName()));
        }
        return copy;
    }

    private void copyAxis(NumberAxis source, NumberAxis target) {
        target.setAutoRanging(source.isAutoRanging());
        if (!source.isAutoRanging()) {
            target.setLowerBound(source.getLowerBound());
            target.setUpperBound(source.getUpperBound());
            target.setTickUnit(source.getTickUnit());
        }
    }

    private String profileSeriesStyle(String name) {
        if ("Mediana".equals(name)) {
            return "-fx-stroke: #ffae4a; -fx-stroke-width: 4px;";
        }
        if (name != null && name.contains("percentile")) {
            return "-fx-stroke: #aa78db; -fx-stroke-width: 2px; -fx-stroke-dash-array: 7 5;";
        }
        return "-fx-stroke: rgba(84, 215, 255, 0.20); -fx-stroke-width: 1px;";
    }

    private Node distributionPane() {
        exposure3D.setDisable(true);
        t90ThreeD.setDisable(true);
        redshift3D.setDisable(true);
        exposure3D.setOnAction(event -> openHistogram3D(
                "Copertura FRACEXP · vista 3D",
                "Le classi sull'asse X sono intervalli di copertura; la profondità separa short, long e GRB senza T90.",
                exposure3DDataset));
        t90ThreeD.setOnAction(event -> openHistogram3D(
                "Durata T90 · vista 3D",
                "Le classi sull'asse X sono intervalli di T90; la profondità separa gli eventi con e senza redshift disponibile.",
                t90ThreeDDataset));
        redshift3D.setOnAction(event -> openHistogram3D(
                "Redshift · vista 3D",
                "Le classi sull'asse X sono intervalli di redshift; la profondità separa short, long e GRB senza T90.",
                redshift3DDataset));
        VBox exposureCard = histogramCard("Qualità FRACEXP",
                "Quanti GRB hanno una determinata percentuale di bin completamente esposti",
                exposureHistogram, exposure3D);
        VBox t90Card = histogramCard("Durata T90",
                "Quanti GRB inclusi ricadono in ciascun intervallo di durata",
                t90Histogram, t90ThreeD);
        VBox redshiftCard = histogramCard("Distanza cosmologica",
                "Distribuzione del redshift dei GRB inclusi; n.d. indica un valore assente",
                redshiftHistogram, redshift3D);
        HBox row = new HBox(14, exposureCard, t90Card, redshiftCard);
        row.setPadding(new Insets(16));
        for (VBox card : List.of(exposureCard, t90Card, redshiftCard)) {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        return row;
    }

    private BarChart<String, Number> histogram(String title) {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setTitle(title);
        chart.setPrefSize(420, 330);
        chart.setMinWidth(250);
        chart.setMaxWidth(Double.MAX_VALUE);
        return chart;
    }

    private VBox histogramCard(String title, String subtitle, BarChart<String, Number> chart, Button threeDButton) {
        HBox toolbar = new HBox(8, UiFactory.spacer(), threeDButton);
        VBox content = new VBox(6, toolbar, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        VBox card = UiFactory.card(title, subtitle, content);
        card.getStyleClass().add("population-histogram-card");
        return card;
    }

    private void openHistogram3D(String title, String explanation, Java2DGroupedBarPanel.Dataset dataset) {
        if (dataset == null || dataset.isEmpty() || dataset.maximumCount() == 0) {
            return;
        }
        InPlaceFullscreen.show(this, title, new PopulationHistogram3DPane(title, explanation, dataset));
    }

    private void configureControls() {
        configureExposureControl(exposureMinSlider, exposureMin, true, exposureMinDebounce);
        configureExposureControl(exposureMaxSlider, exposureMax, false, exposureMaxDebounce);
        duration.valueProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        redshiftAvailability.valueProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        limit.valueProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        for (TextField field : List.of(zMin, zMax, raMin, raMax, decMin, decMax)) {
            field.textProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        }
    }

    private void configureExposureControl(ScrollBar slider, TextField field, boolean minimum,
                                          PauseTransition debounce) {
        slider.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (syncingExposureControls) return;
            double value = clampExposure(newValue.doubleValue(), minimum);
            syncingExposureControls = true;
            slider.setValue(value);
            field.setText(formatExposure(value));
            syncingExposureControls = false;
            updateCandidatePreview();
        });
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (syncingExposureControls) return;
            debounce.stop();
            debounce.setOnFinished(event -> commitExposureField(slider, field, minimum));
            debounce.playFromStart();
        });
        field.setOnAction(event -> {
            debounce.stop();
            commitExposureField(slider, field, minimum);
        });
        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                debounce.stop();
                commitExposureField(slider, field, minimum);
            }
        });
    }

    private void commitExposureField(ScrollBar slider, TextField field, boolean minimum) {
        double fallback = slider.getValue();
        double value;
        try {
            value = Double.parseDouble(field.getText().trim().replace(',', '.'));
        } catch (Exception error) {
            value = fallback;
        }
        value = clampExposure(value, minimum);
        syncingExposureControls = true;
        slider.setValue(value);
        field.setText(formatExposure(value));
        syncingExposureControls = false;
        updateCandidatePreview();
    }

    private double clampExposure(double value, boolean minimum) {
        value = Math.max(0.0, Math.min(100.0, value));
        if (minimum) {
            return Math.min(value, exposureMaxSlider.getValue());
        }
        return Math.max(value, exposureMinSlider.getValue());
    }

    private String formatExposure(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-9) {
            return Integer.toString((int) Math.rint(rounded));
        }
        return String.format(Locale.ITALY, "%.1f", rounded);
    }

    private void configureChart() {
        configureChart(curveChart, curveXAxis, curveYAxis);
        curveChart.setTitle("Nessuna analisi eseguita");
        curveChart.setMinHeight(300);
        curveChart.setPrefHeight(350);
        curveChart.setMaxHeight(390);
    }

    private void configureChart(LineChart<Number, Number> chart, NumberAxis xAxis, NumberAxis yAxis) {
        xAxis.setLabel("Tempo dal trigger (s)");
        yAxis.setLabel("Rate normalizzato (picco = 1)");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.getStyleClass().addAll("lightcurve-chart", "population-chart");
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
        lastResult = result;
        populateCurveChart(result);
        resultTable.setItems(FXCollections.observableArrayList(result.accepted()));
        populateHistograms(result);
        populateInsight(result);
        profile3D.setDisable(result.curves().isEmpty());
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

    private void populateInsight(AnalysisResult result) {
        Set<String> curveNames = result.curves().stream()
                .map(CumulativeAnalysisService.NormalizedCurve::grbName)
                .collect(java.util.stream.Collectors.toSet());
        List<PopulationInsightService.EventFacts> facts = result.accepted().stream()
                .filter(event -> curveNames.contains(event.grbName()))
                .map(event -> new PopulationInsightService.EventFacts(
                        event.grbName(), event.burst().t90Sec(),
                        event.burst().redshift().representativeValue(), event.exposurePercent()))
                .toList();
        PopulationInsightService.Narrative narrative = insightService.analyze(
                result.curves(), result.profile(), facts, result.examined(), result.failures(), result.halfWindow());
        lastNarrative = narrative;
        insightHeadline.setText(narrative.headline());
        insightObservations.getChildren().setAll(narrative.observations().stream()
                .map(text -> insightLine("●", text, "population-insight-dot"))
                .toList());
        insightCautions.getChildren().clear();
        if (!narrative.cautions().isEmpty()) {
            insightCautions.getChildren().add(UiFactory.label("Da tenere presente", "population-insight-caution-title"));
            narrative.cautions().stream().limit(3)
                    .map(text -> insightLine("!", text, "population-insight-warning"))
                    .forEach(insightCautions.getChildren()::add);
        }
    }

    private HBox insightLine(String marker, String text, String markerStyle) {
        return insightLine(marker, text, markerStyle, false);
    }

    private HBox insightLine(String marker, String text, String markerStyle, boolean expanded) {
        Label bullet = UiFactory.label(marker, markerStyle);
        bullet.setMinWidth(14);
        bullet.setAlignment(Pos.CENTER);
        Label copy = expanded
                ? UiFactory.wrappedLabel(text, "population-insight-text")
                : UiFactory.label(text, "population-insight-text");
        copy.setWrapText(expanded);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        copy.setTextOverrun(expanded ? OverrunStyle.CLIP : OverrunStyle.ELLIPSIS);
        if (expanded) copy.setStyle("-fx-font-size: 13px; -fx-line-spacing: 3px;");
        HBox.setHgrow(copy, Priority.ALWAYS);
        HBox row = new HBox(8, bullet, copy);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(expanded ? Pos.TOP_LEFT : Pos.CENTER_LEFT);
        return row;
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
        Map<String, Integer> exposureBins = bins(
                result.measured().stream().map(PopulationEvent::exposurePercent).toList(),
                new double[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, "%");
        setBars(exposureHistogram, exposureBins);

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

        exposure3DDataset = groupedByT90(result.measured(), new ArrayList<>(exposureBins.keySet()),
                event -> coverageBin(event.exposurePercent()), "Copertura FRACEXP", "Classe T90");
        t90ThreeDDataset = groupedByRedshiftAvailability(result.accepted(), new ArrayList<>(t90Bins.keySet()),
                event -> t90Bin(event.burst().t90Sec()), "Durata T90 (s)", "Disponibilità redshift");
        redshift3DDataset = groupedByT90(result.accepted(), new ArrayList<>(zBins.keySet()),
                event -> redshiftBin(event.burst().redshift().representativeValue()),
                "Redshift z", "Classe T90");
        exposure3D.setDisable(exposure3DDataset.maximumCount() == 0);
        t90ThreeD.setDisable(t90ThreeDDataset.maximumCount() == 0);
        redshift3D.setDisable(redshift3DDataset.maximumCount() == 0);
    }

    private Java2DGroupedBarPanel.Dataset groupedByT90(
            List<PopulationEvent> events, List<String> categories,
            java.util.function.Function<PopulationEvent, String> classifier,
            String xAxis, String depthAxis) {
        String[] groups = {"Short · T90 ≤ 2 s", "Long · T90 > 2 s", "T90 n.d."};
        int[][] counts = new int[groups.length][categories.size()];
        for (PopulationEvent event : events) {
            int group = !event.burst().hasT90() ? 2 : event.burst().isShort() ? 0 : 1;
            int category = categories.indexOf(classifier.apply(event));
            if (category >= 0) counts[group][category]++;
        }
        return new Java2DGroupedBarPanel.Dataset(categories.toArray(String[]::new), groups, counts,
                new java.awt.Color[]{new java.awt.Color(255, 174, 74), new java.awt.Color(82, 216, 255),
                        new java.awt.Color(145, 157, 179)}, xAxis, depthAxis);
    }

    private Java2DGroupedBarPanel.Dataset groupedByRedshiftAvailability(
            List<PopulationEvent> events, List<String> categories,
            java.util.function.Function<PopulationEvent, String> classifier,
            String xAxis, String depthAxis) {
        String[] groups = {"Con redshift", "Senza redshift"};
        int[][] counts = new int[groups.length][categories.size()];
        for (PopulationEvent event : events) {
            int group = event.burst().redshift().available() ? 0 : 1;
            int category = categories.indexOf(classifier.apply(event));
            if (category >= 0) counts[group][category]++;
        }
        return new Java2DGroupedBarPanel.Dataset(categories.toArray(String[]::new), groups, counts,
                new java.awt.Color[]{new java.awt.Color(110, 231, 183), new java.awt.Color(167, 139, 250)},
                xAxis, depthAxis);
    }

    private String coverageBin(double value) {
        int lower = Math.min(90, Math.max(0, (int) Math.floor(value / 10.0) * 10));
        return lower + "–" + (lower + 10) + "%";
    }

    private String t90Bin(Double value) {
        return value == null ? "n.d." : value <= 2 ? "≤2" : value <= 10 ? "2–10"
                : value <= 50 ? "10–50" : value <= 100 ? "50–100" : ">100";
    }

    private String redshiftBin(Double value) {
        return value == null ? "n.d." : value < 1 ? "0–1" : value < 2 ? "1–2" : value < 3 ? "2–3"
                : value < 4 ? "3–4" : value < 6 ? "4–6" : ">6";
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
        double minExposure = percentage(exposureMin, "Copertura FRACEXP minima");
        double maxExposure = percentage(exposureMax, "Copertura FRACEXP massima");
        if (minExposure > maxExposure) {
            throw new IllegalArgumentException("Il minimo FRACEXP non può superare il massimo.");
        }
        return new Filter(duration.getValue(), redshiftAvailability.getValue(), minZ, maxZ,
                minRa, maxRa, minDec, maxDec, minExposure, maxExposure,
                halfWindow, maximumEvents);
    }

    private double number(TextField field, String label) {
        try {
            return Double.parseDouble(field.getText().trim().replace(',', '.'));
        } catch (Exception error) {
            throw new IllegalArgumentException(label + " non è un numero valido.");
        }
    }

    private double percentage(TextField field, String label) {
        double value = number(field, label);
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(label + " deve essere compresa fra 0% e 100%.");
        }
        return value;
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
        lastResult = null;
        lastNarrative = null;
        curveChart.getData().clear();
        resultTable.getItems().clear();
        exposureHistogram.getData().clear();
        t90Histogram.getData().clear();
        redshiftHistogram.getData().clear();
        exposure3DDataset = Java2DGroupedBarPanel.Dataset.empty();
        t90ThreeDDataset = Java2DGroupedBarPanel.Dataset.empty();
        redshift3DDataset = Java2DGroupedBarPanel.Dataset.empty();
        profile3D.setDisable(true);
        exposure3D.setDisable(true);
        t90ThreeD.setDisable(true);
        redshift3D.setDisable(true);
        insightHeadline.setText("Esegui un'analisi per ottenere un commento automatico sul campione.");
        insightObservations.getChildren().clear();
        insightCautions.getChildren().clear();
        curveChart.setTitle("Nessuna analisi eseguita");
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
        syncingExposureControls = true;
        exposureMinSlider.setValue(0);
        exposureMaxSlider.setValue(100);
        exposureMin.setText("0");
        exposureMax.setText("100");
        syncingExposureControls = false;
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

    private static ScrollBar exposureSlider(double value) {
        ScrollBar slider = new ScrollBar();
        slider.getStyleClass().add("fracexp-slider");
        slider.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        slider.setMin(0);
        slider.setMax(100);
        slider.setValue(value);
        slider.setUnitIncrement(1);
        slider.setBlockIncrement(5);
        slider.setVisibleAmount(1);
        slider.setMinWidth(120);
        slider.setPrefWidth(230);
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setMinHeight(20);
        slider.setPrefHeight(20);
        return slider;
    }

    private static TextField percentField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("percentage-field");
        field.setAlignment(Pos.CENTER);
        field.setMinWidth(58);
        field.setPrefWidth(64);
        field.setMaxWidth(72);
        return field;
    }

    private VBox exposureControl(String label, ScrollBar slider, TextField field, boolean minimum) {
        Button reset = UiFactory.button("↺", "filter-reset-button");
        reset.setTooltip(UiFactory.quickTooltip(minimum
                ? "Ripristina il minimo a 0%" : "Ripristina il massimo a 100%"));
        reset.setOnAction(event -> {
            double value = minimum ? 0.0 : 100.0;
            syncingExposureControls = true;
            slider.setValue(value);
            field.setText(formatExposure(value));
            syncingExposureControls = false;
            updateCandidatePreview();
        });

        Label unit = UiFactory.label("%", "percentage-unit");
        HBox valueBox = new HBox(5, field, unit, reset);
        valueBox.setAlignment(Pos.CENTER_RIGHT);
        HBox heading = new HBox(8, UiFactory.label(label, "filter-label"), UiFactory.spacer(), valueBox);
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(slider, Priority.ALWAYS);
        VBox box = new VBox(5, heading, slider);
        box.getStyleClass().add("percentage-control");
        box.setMinWidth(185);
        box.setPrefWidth(215);
        box.setMaxWidth(Double.MAX_VALUE);
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
