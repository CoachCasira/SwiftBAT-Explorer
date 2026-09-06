package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.FieldDefinition;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.MetadataItem;
import it.casiraghi.swiftbat.model.SummaryItem;
import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.model.TabularData;
import it.casiraghi.swiftbat.service.ExcelExportService;
import it.casiraghi.swiftbat.ui.components.ThreeDChartPane;
import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class ExplorerPage extends BorderPane {
    private static final Map<String, String> CHANNELS = createChannels();
    private static final Map<String, Double> WINDOWS = createWindows();

    private final HostServices hostServices;
    private final BiConsumer<CatalogEntry, Boolean> loadRequest;
    private final Predicate<CatalogEntry> cacheLookup;
    private final ExcelExportService excelExportService = new ExcelExportService();
    private final ObservableList<CatalogEntry> catalog = FXCollections.observableArrayList();
    private final FilteredList<CatalogEntry> filteredCatalog = new FilteredList<>(catalog, ignored -> true);
    private final ListView<CatalogEntry> catalogList = new ListView<>(filteredCatalog);
    private final TextField catalogSearch = new TextField();
    private final ComboBox<String> durationFilter = new ComboBox<>();
    private final ComboBox<String> redshiftFilter = new ComboBox<>();
    private final Map<String, SkyBurst> scientificMetadata = new LinkedHashMap<>();
    private final Label catalogCount = UiFactory.label("Catalogo in caricamento…", "sidebar-caption");
    private final StackPane workspace = new StackPane();
    private CatalogEntry selectedEntry;
    private GrbData currentData;

    public ExplorerPage(HostServices hostServices, BiConsumer<CatalogEntry, Boolean> loadRequest,
                        Predicate<CatalogEntry> cacheLookup) {
        this.hostServices = hostServices;
        this.loadRequest = loadRequest;
        this.cacheLookup = cacheLookup == null ? ignored -> false : cacheLookup;
        getStyleClass().add("page-root");
        setPadding(new Insets(20, 24, 24, 24));
        setTop(buildHeader());
        setCenter(buildBody());
        BorderPane.setMargin(getCenter(), new Insets(16, 0, 0, 0));
        wireCatalog();
        showEmptyState();
    }

    public void setCatalog(List<CatalogEntry> entries, boolean fallback) {
        catalog.setAll(entries);
        applyCatalogFilters();
        catalogCount.setText(entries.size() + (fallback ? " GRB di emergenza" : " GRB nel catalogo online"));
    }

    public void setScientificMetadata(List<SkyBurst> bursts) {
        scientificMetadata.clear();
        if (bursts != null) {
            for (SkyBurst burst : bursts) {
                scientificMetadata.put(burst.grbName().toUpperCase(Locale.ROOT), burst);
            }
        }
        applyCatalogFilters();
        catalogList.refresh();
    }

    public void setSelectedEntry(CatalogEntry entry) {
        selectedEntry = entry;
        catalogList.getSelectionModel().select(entry);
        catalogList.scrollTo(entry);
    }

    public CatalogEntry selectedEntry() {
        return selectedEntry;
    }

    public void refreshCacheIndicators() {
        catalogList.refresh();
    }

    public void showLoading(double progress, String title, String detail) {
        VBox box = new VBox(15);
        box.getStyleClass().add("loading-state");
        box.setAlignment(Pos.CENTER);
        Label icon = UiFactory.label("⌁", "loading-icon");
        Label titleLabel = UiFactory.label(title, "loading-title");
        Label detailLabel = UiFactory.wrappedLabel(detail, "loading-detail");
        detailLabel.setMaxWidth(560);
        ProgressBar progressBar = new ProgressBar(Math.max(0, Math.min(1, progress)));
        progressBar.setPrefWidth(480);
        progressBar.getStyleClass().add("modern-progress");
        Label percent = UiFactory.label(Math.round(progress * 100) + "%", "progress-percent");
        box.getChildren().addAll(icon, titleLabel, detailLabel, progressBar, percent);
        setWorkspace(box);
    }

    public void showError(CatalogEntry entry, Throwable error) {
        VBox box = new VBox(14);
        box.getStyleClass().addAll("empty-state", "error-state");
        box.setAlignment(Pos.CENTER);
        Label icon = UiFactory.label("!", "error-symbol");
        Label title = UiFactory.label("Dati non disponibili per " + entry.grbName(), "empty-title");
        Label message = UiFactory.wrappedLabel(
                readableError(error) + "\n\nL'evento rimane nel catalogo, ma la struttura online può essere incompleta o diversa da quella standard.",
                "empty-message");
        message.setMaxWidth(720);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        Button retry = UiFactory.button("Riprova", "primary-button");
        retry.setOnAction(event -> loadRequest.accept(entry, true));
        Button official = UiFactory.button("Apri Data Product", "secondary-button");
        official.setOnAction(event -> hostServices.showDocument(entry.dataProductUrl()));
        actions.getChildren().addAll(retry, official);
        box.getChildren().addAll(icon, title, message, actions);
        setWorkspace(box);
    }

    public void showData(GrbData data) {
        currentData = data;
        setWorkspace(buildDashboard(data));
    }

    private Node buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox copy = new VBox(5,
                UiFactory.label("Esplora", "page-title"),
                UiFactory.wrappedLabel(
                        "Cerca un GRB e apri curve di luce, dati e metadati senza gestire file manualmente.",
                        "page-subtitle"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        header.getChildren().add(copy);
        return header;
    }

    private Node buildBody() {
        VBox sidebar = new VBox(12);
        sidebar.getStyleClass().add("catalog-panel");
        sidebar.setPadding(new Insets(17));
        sidebar.setMinWidth(290);
        sidebar.setPrefWidth(320);
        sidebar.setMinHeight(0);

        Label title = UiFactory.label("GRB", "panel-title");
        catalogSearch.setPromptText("Cerca GRB o Trigger ID…");
        catalogSearch.getStyleClass().add("search-field");
        durationFilter.setItems(FXCollections.observableArrayList(
                "Tutte", "Short ≤ 2 s", "Long > 2 s", "T90 n.d."));
        durationFilter.setValue("Tutte");
        durationFilter.getStyleClass().add("choice-box-modern");
        redshiftFilter.setItems(FXCollections.observableArrayList(
                "Tutti", "Con z", "Senza z"));
        redshiftFilter.setValue("Tutti");
        redshiftFilter.getStyleClass().add("choice-box-modern");
        durationFilter.setMaxWidth(Double.MAX_VALUE);
        redshiftFilter.setMaxWidth(Double.MAX_VALUE);

        GridPane filterGrid = new GridPane();
        filterGrid.getStyleClass().add("explorer-filter-grid");
        filterGrid.setHgap(8);
        filterGrid.setVgap(5);
        filterGrid.add(UiFactory.label("Durata", "filter-label"), 0, 0);
        filterGrid.add(UiFactory.label("Redshift", "filter-label"), 1, 0);
        filterGrid.add(durationFilter, 0, 1);
        filterGrid.add(redshiftFilter, 1, 1);
        var firstColumn = new javafx.scene.layout.ColumnConstraints();
        firstColumn.setPercentWidth(50);
        firstColumn.setHgrow(Priority.ALWAYS);
        var secondColumn = new javafx.scene.layout.ColumnConstraints();
        secondColumn.setPercentWidth(50);
        secondColumn.setHgrow(Priority.ALWAYS);
        filterGrid.getColumnConstraints().addAll(firstColumn, secondColumn);
        catalogList.getStyleClass().add("catalog-list");
        catalogList.setCellFactory(ignored -> new CatalogCell());
        catalogList.setMinHeight(140);
        catalogList.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(catalogList, Priority.ALWAYS);

        Separator separator = new Separator(Orientation.HORIZONTAL);
        separator.getStyleClass().add("soft-separator");
        Label hint = UiFactory.wrappedLabel(
                "Dopo il primo download, ASCII e FITS restano nella cache locale anche ai successivi avvii.",
                "sidebar-hint");
        sidebar.getChildren().addAll(title, catalogSearch, filterGrid,
                catalogCount, catalogList, separator, hint);

        workspace.getStyleClass().add("workspace-host");
        workspace.setMinWidth(0);
        workspace.setMinHeight(0);
        SplitPane split = new SplitPane(sidebar, workspace);
        split.getStyleClass().add("clean-split");
        split.setMinHeight(0);
        split.setDividerPositions(0.22);
        return split;
    }

    private void wireCatalog() {
        catalogSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyCatalogFilters();
        });
        durationFilter.setOnAction(event -> applyCatalogFilters());
        redshiftFilter.setOnAction(event -> applyCatalogFilters());
        catalogList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != selectedEntry) {
                selectedEntry = newValue;
                loadRequest.accept(newValue, false);
            }
        });
    }

    private void applyCatalogFilters() {
        String query = catalogSearch.getText() == null ? "" : catalogSearch.getText().trim().toLowerCase(Locale.ROOT);
        String duration = durationFilter.getValue() == null ? "Tutte" : durationFilter.getValue();
        String redshift = redshiftFilter.getValue() == null ? "Tutti" : redshiftFilter.getValue();
        filteredCatalog.setPredicate(entry -> {
            if (!query.isBlank()
                    && !entry.grbName().toLowerCase(Locale.ROOT).contains(query)
                    && !entry.triggerId().toLowerCase(Locale.ROOT).contains(query)) {
                return false;
            }
            SkyBurst burst = scientificMetadata.get(entry.grbName().toUpperCase(Locale.ROOT));
            if (burst == null) {
                return duration.equals("Tutte") && !redshift.equals("Con z");
            }
            if (duration.startsWith("Short") && !burst.isShort()) return false;
            if (duration.startsWith("Long") && !burst.isLong()) return false;
            if (duration.equals("T90 n.d.") && burst.hasT90()) return false;
            if (redshift.equals("Con z") && !burst.redshift().available()) return false;
            return !redshift.equals("Senza z") || !burst.redshift().available();
        });
        catalogCount.setText(filteredCatalog.size() + " GRB visualizzati");
    }

    private void showEmptyState() {
        VBox box = new VBox(14);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(
                UiFactory.label("✦", "empty-icon"),
                UiFactory.label("Scegli un Gamma-Ray Burst", "empty-title"),
                UiFactory.wrappedLabel(
                        "Usa il catalogo a sinistra. Non devi scaricare o caricare manualmente alcun file: l'app raggiunge i prodotti online e costruisce l'area di lavoro.",
                        "empty-message"));
        setWorkspace(box);
    }

    private Node buildDashboard(GrbData data) {
        VBox dashboard = new VBox(18);
        dashboard.getStyleClass().add("dashboard");
        SkyBurst scientific = scientificMetadata.get(data.grbName().toUpperCase(Locale.ROOT));
        String scientificLine = scientific == null ? ""
                : " · " + scientific.durationClass() + " · " + scientific.redshift().displayValue();

        HBox eventHeader = new HBox(14);
        eventHeader.setAlignment(Pos.CENTER_LEFT);
        VBox identity = new VBox(4,
                UiFactory.label(data.grbName(), "grb-title"),
                UiFactory.label("Trigger " + data.triggerId() + scientificLine + " · in memoria nella sessione", "grb-subtitle"));
        HBox.setHgrow(identity, Priority.ALWAYS);
        Label status = UiFactory.label(data.availability().statusText(), "status-pill",
                data.availability().asciiAvailable() && data.availability().fitsAvailable() ? "status-online" : "status-warning");
        Button reload = UiFactory.button("Ricarica online", "secondary-button");
        reload.setOnAction(event -> loadRequest.accept(selectedEntry, true));
        Button source = UiFactory.button("Fonte ufficiale ↗", "ghost-button");
        source.setOnAction(event -> hostServices.showDocument(data.availability().dataProductUrl()));
        eventHeader.getChildren().addAll(identity, status, reload, source);

        FlowPane metrics = new FlowPane(12, 12);
        metrics.getChildren().addAll(
                metric(data, "PEAK_RATE", "Picco", "RATE massimo"),
                metric(data, "PEAK_TIME", "Tempo del picco", "rispetto al trigger"),
                metric(data, "PEAK_SNR", "Segnale / errore", "indicatore descrittivo"),
                metric(data, "FULL_EXPOSURE_FRACTION", "Esposizione completa", "bin con FRACEXP ≈ 1"),
                metric(data, "HARDNESS_PROXY", "Durezza", "proxy alte / basse energie"));
        if (scientific != null) {
            metrics.getChildren().addAll(
                    UiFactory.metricCard("T90", scientific.formattedT90(), scientific.durationClass()),
                    UiFactory.metricCard("Redshift", scientific.redshift().available()
                            ? scientific.redshift().rawValue() : "n.d.", "z cosmologico · valore BAT"));
        }

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("main-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setMinHeight(590);
        tabs.setPrefHeight(680);
        tabs.getTabs().addAll(
                tab("Curva 2D", buildOverview(data)),
                tab("Vista 3D", buildThreeD(data)),
                tab("Dati", buildDataWorkspace(data)),
                tab("Metadati", buildMetadata(data)),
                tab("Guida", buildUnderstand(data)));
        VBox.setVgrow(tabs, Priority.ALWAYS);

        dashboard.getChildren().addAll(eventHeader, metrics, tabs);

        ScrollPane scroll = new ScrollPane(dashboard);
        scroll.getStyleClass().addAll("page-scroll", "dashboard-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(false);
        return scroll;
    }

    private VBox metric(GrbData data, String key, String title, String detail) {
        SummaryItem item = data.summaryByKey().get(key);
        String value = DisplayFormat.summary(key, item);
        VBox card = UiFactory.metricCard(title, value, detail);
        if (item != null && item.value() != null && !item.value().isBlank()) {
            String complete = item.value() + (item.unit().isBlank() ? "" : " " + item.unit());
            Tooltip.install(card, new Tooltip("Valore completo: " + complete));
        }
        return card;
    }

    private Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        return tab;
    }

    private Node buildOverview(GrbData data) {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(18));
        VBox chartCard = new VBox(14);
        chartCard.getStyleClass().add("card");
        HBox controls = new HBox(11);
        controls.setAlignment(Pos.CENTER_LEFT);

        ChoiceBox<String> channelChoice = new ChoiceBox<>(FXCollections.observableArrayList(CHANNELS.keySet()));
        channelChoice.getStyleClass().add("choice-box-modern");
        channelChoice.setValue(data.asciiData().isEmpty() ? "Totale FITS 15–350 keV" : "Totale 15–350 keV");
        if (data.asciiData().isEmpty()) {
            channelChoice.getItems().setAll("Totale FITS 15–350 keV");
        }

        ChoiceBox<String> windowChoice = new ChoiceBox<>(FXCollections.observableArrayList(WINDOWS.keySet()));
        windowChoice.getStyleClass().add("choice-box-modern");
        windowChoice.setValue("±60 s dal trigger");
        CheckBox smooth = new CheckBox("Media mobile 5 bin");
        smooth.getStyleClass().add("modern-check");
        Label help = UiFactory.label("Il tratteggio verticale indica il trigger (t = 0).", "subtle-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button export = UiFactory.button("Esporta PNG", "ghost-button");
        controls.getChildren().addAll(channelChoice, windowChoice, smooth, spacer, help, export);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Tempo dal trigger (s)");
        yAxis.setLabel("Rate (count/s)");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getStyleClass().add("lightcurve-chart");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(true);
        chart.setTitle("Curva di luce a binning di 1 secondo");
        VBox.setVgrow(chart, Priority.ALWAYS);

        Runnable refresh = () -> populateChart(chart, data, channelChoice.getValue(), windowChoice.getValue(), smooth.isSelected());
        channelChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        windowChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        smooth.selectedProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        export.setOnAction(event -> exportNode(chart, data.grbName() + "_curva_1s.png"));
        refresh.run();

        chartCard.getChildren().addAll(controls, chart);
        pane.setCenter(chartCard);

        VBox right = new VBox(13);
        right.setPrefWidth(330);
        right.getChildren().addAll(
                summaryCard(data, "In breve", List.of("BIN_SIZE", "ENERGY_RANGE", "TIME_RANGE", "ASCII_ROWS", "FITS_ROWS")),
                plainConceptCard("Trigger", "Il punto zero dell'allerta", "Tempi negativi: prima del trigger. Tempi positivi: dopo il trigger. Il trigger non coincide necessariamente con l'inizio fisico esatto del burst."));
        pane.setRight(right);
        BorderPane.setMargin(right, new Insets(0, 0, 0, 16));
        return pane;
    }

    private Node buildThreeD(GrbData data) {
        VBox box = new VBox(13);
        box.setPadding(new Insets(18));
        if (data.asciiData().isEmpty()) {
            box.getChildren().add(UiFactory.card("Vista 3D non disponibile",
                    "Per costruire il paesaggio tempo–energia servono le quattro bande del file ASCII.", null));
            return box;
        }
        ThreeDChartPane chart = new ThreeDChartPane();
        chart.setContextName(data.grbName());
        chart.setData(data.asciiData());
        VBox.setVgrow(chart, Priority.ALWAYS);
        box.getChildren().add(chart);
        return box;
    }

    private Node buildDataWorkspace(GrbData data) {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(18));

        ChoiceBox<String> sourceChoice = new ChoiceBox<>();
        if (!data.asciiData().isEmpty()) {
            sourceChoice.getItems().add("ASCII — quattro bande");
        }
        if (!data.fitsData().isEmpty()) {
            sourceChoice.getItems().add("FITS — un canale e qualità");
        }
        sourceChoice.getStyleClass().add("choice-box-modern");
        if (!sourceChoice.getItems().isEmpty()) {
            sourceChoice.setValue(sourceChoice.getItems().get(0));
        }

        TextField filter = new TextField();
        filter.setPromptText("Filtra le righe per valore testuale…");
        filter.getStyleClass().add("search-field");
        filter.setPrefWidth(300);

        ChoiceBox<String> fieldChoice = new ChoiceBox<>();
        fieldChoice.getStyleClass().add("choice-box-modern");
        fieldChoice.setPrefWidth(260);

        Button exportAscii = UiFactory.button("ASCII → Excel", "ghost-button");
        exportAscii.setDisable(data.asciiData().isEmpty());
        exportAscii.setOnAction(event -> exportExcel(data, true));
        Button exportFits = UiFactory.button("FITS + metadati → Excel", "ghost-button");
        exportFits.setDisable(data.fitsData().isEmpty());
        exportFits.setOnAction(event -> exportExcel(data, false));
        HBox toolbar = new HBox(10, sourceChoice, filter, exportAscii, exportFits,
                UiFactory.spacer(), UiFactory.label("Spiega:", "toolbar-label"), fieldChoice);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        pane.setTop(toolbar);
        BorderPane.setMargin(toolbar, new Insets(0, 0, 14, 0));

        TableView<ObservableList<String>> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        VBox explanation = new VBox(12);
        explanation.getStyleClass().add("field-explanation-panel");
        explanation.setPadding(new Insets(18));
        explanation.setPrefWidth(350);

        Runnable refresh = () -> {
            TabularData selected = sourceChoice.getValue() != null && sourceChoice.getValue().startsWith("FITS")
                    ? data.fitsData() : data.asciiData();
            populateTable(table, selected, data, filter.getText());
            fieldChoice.setItems(FXCollections.observableArrayList(selected.headers()));
            if (!selected.headers().isEmpty()) {
                fieldChoice.setValue(selected.headers().get(0));
            }
        };
        sourceChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        filter.textProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        fieldChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                showFieldExplanation(explanation, data.definition(newValue), newValue));
        refresh.run();

        ScrollPane explanationScroll = scrollableSide(explanation);
        SplitPane split = new SplitPane(table, explanationScroll);
        split.getStyleClass().add("clean-split");
        split.setDividerPositions(0.72);
        pane.setCenter(split);
        return pane;
    }

    private Node buildMetadata(GrbData data) {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(18));
        TextField search = new TextField();
        search.setPromptText("Cerca keyword, valore, HDU o commento…");
        search.getStyleClass().add("search-field");
        pane.setTop(search);
        BorderPane.setMargin(search, new Insets(0, 0, 13, 0));

        TableView<MetadataItem> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<MetadataItem, String> hdu = metadataColumn("HDU", item -> item.hduName(), 95);
        hdu.setMinWidth(75);
        TableColumn<MetadataItem, String> keyword = metadataColumn("Keyword", item -> item.keyword(), 145);
        keyword.setMinWidth(115);
        TableColumn<MetadataItem, String> value = metadataColumn("Valore", item -> item.value(), 260);
        value.setMinWidth(165);
        TableColumn<MetadataItem, String> comment = metadataColumn("Commento originale", item -> item.comment(), 440);
        comment.setMinWidth(240);
        table.getColumns().addAll(hdu, keyword, value, comment);
        FilteredList<MetadataItem> filtered = new FilteredList<>(FXCollections.observableArrayList(data.metadata()), ignored -> true);
        table.setItems(filtered);
        search.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.toLowerCase(Locale.ROOT).trim();
            filtered.setPredicate(item -> query.isBlank()
                    || (item.hduName() + " " + item.keyword() + " " + item.value() + " " + item.comment())
                    .toLowerCase(Locale.ROOT).contains(query));
        });

        VBox side = new VBox(12);
        side.setPrefWidth(380);
        side.setMinWidth(320);
        side.getStyleClass().add("field-explanation-panel");
        side.setPadding(new Insets(18));
        showMetadataIntro(side);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, item) -> {
            if (item != null) {
                FieldDefinition definition = findMetadataDefinition(data, item.keyword());
                showMetadataExplanation(side, item, definition);
            }
        });

        ScrollPane sideScroll = scrollableSide(side);
        SplitPane split = new SplitPane(table, sideScroll);
        split.getStyleClass().add("clean-split");
        split.setDividerPositions(0.70);
        pane.setCenter(split);
        return pane;
    }

    private Node buildUnderstand(GrbData data) {
        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("detail-scroll");
        scroll.setFitToWidth(true);
        VBox page = new VBox(18);
        page.setPadding(new Insets(22));
        page.getChildren().addAll(
                UiFactory.label("Una lettura guidata dell'evento", "section-title"),
                UiFactory.wrappedLabel(
                        "Questa pagina collega le parole tecniche ai dati che stai osservando. Non devi memorizzare tutto: usa le spiegazioni come legenda ragionata.",
                        "section-caption"),
                learningCard("1", "Il trigger è il momento zero", "Lo strumento riconosce un aumento significativo e genera un'allerta. Nei grafici trasformiamo quel momento in t = 0. I dati prima del trigger hanno tempo negativo; quelli dopo hanno tempo positivo."),
                learningCard("2", "Il rate descrive l'intensità nel tempo", "Ogni riga corrisponde a un intervallo di un secondo. RATE indica il segnale netto stimato in quell'intervallo. Più è alto, più la curva è intensa in quel momento."),
                learningCard("3", "Le energie sono separate in quattro bande", "Il file ASCII divide il segnale in 15–25, 25–50, 50–100 e 100–350 keV. La curva totale 15–350 keV riunisce queste componenti."),
                learningCard("4", "ERROR e FRACEXP controllano l'affidabilità", "ERROR esprime l'incertezza statistica del rate. FRACEXP indica quanta parte del secondo è stata realmente esposta: 1 significa bin completo."),
                learningCard("5", "Il FITS è più di una tabella", "Contiene sia i numeri della curva sia le intestazioni tecniche: strumento, date, identificativi, coordinate, sistema temporale e dettagli di elaborazione."),
                summaryCard(data, "Indicatori calcolati per questo evento", List.of(
                        "PEAK_RATE", "PEAK_TIME", "PEAK_ERROR", "PEAK_SNR", "MEAN_RATE", "RATE_STD",
                        "NEGATIVE_FRACTION", "FULL_EXPOSURE_FRACTION", "HARDNESS_PROXY")),
                UiFactory.card("Limite scientifico importante",
                        "Gli indicatori presenti nell'app sono descrittivi.",
                        UiFactory.wrappedLabel(
                                "La vista 3D, il picco, la media, la deviazione standard e la durezza proxy aiutano a esplorare l'evento, ma non sostituiscono il calcolo ufficiale di T90 né una classificazione short/long validata.",
                                "explanation-text")));
        scroll.setContent(page);
        return scroll;
    }

    private VBox summaryCard(GrbData data, String title, List<String> keys) {
        VBox content = new VBox(10);
        Map<String, SummaryItem> summary = data.summaryByKey();
        for (String key : keys) {
            SummaryItem item = summary.get(key);
            if (item != null) {
                String value = DisplayFormat.summary(key, item);
                content.getChildren().add(UiFactory.infoRow(item.label(), value));
            }
        }
        return UiFactory.card(title, "", content);
    }

    private VBox plainConceptCard(String title, String lead, String text) {
        VBox card = new VBox(8);
        card.getStyleClass().add("concept-card");
        card.getChildren().addAll(
                UiFactory.label(title, "concept-title"),
                UiFactory.wrappedLabel(lead, "concept-lead"),
                UiFactory.wrappedLabel(text, "concept-text"));
        return card;
    }

    private VBox learningCard(String number, String title, String text) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.TOP_LEFT);
        Label badge = UiFactory.label(number, "learning-number");
        VBox copy = new VBox(6,
                UiFactory.label(title, "learning-title"),
                UiFactory.wrappedLabel(text, "learning-text"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        row.getChildren().addAll(badge, copy);
        VBox wrapper = new VBox(row);
        wrapper.getStyleClass().add("learning-card");
        return wrapper;
    }

    private void populateChart(LineChart<Number, Number> chart, GrbData data,
                               String channelLabel, String windowLabel, boolean smooth) {
        chart.getData().clear();
        TabularData table = data.asciiData().isEmpty() ? data.fitsData() : data.asciiData();
        int timeIndex = table.indexOf("TIME_FROM_TRIGGER_CENTER_S");
        if (timeIndex < 0 || table.isEmpty()) {
            return;
        }
        double window = WINDOWS.getOrDefault(windowLabel, Double.POSITIVE_INFINITY);
        List<String> fields = new ArrayList<>();
        if ("Tutte le bande".equals(channelLabel)) {
            fields.addAll(List.of("RATE_15_25_KEV", "RATE_25_50_KEV", "RATE_50_100_KEV", "RATE_100_350_KEV"));
        } else if ("Totale FITS 15–350 keV".equals(channelLabel)) {
            fields.add("RATE");
        } else {
            fields.add(CHANNELS.getOrDefault(channelLabel, "RATE_15_350_KEV"));
        }

        double globalMin = Double.POSITIVE_INFINITY;
        double globalMax = Double.NEGATIVE_INFINITY;
        for (String field : fields) {
            int valueIndex = table.indexOf(field);
            if (valueIndex < 0) {
                continue;
            }
            List<Point> points = new ArrayList<>();
            for (List<String> row : table.rows()) {
                double time = parse(row, timeIndex);
                double value = parse(row, valueIndex);
                if (!Double.isFinite(time) || !Double.isFinite(value) || Math.abs(time) > window) {
                    continue;
                }
                points.add(new Point(time, value));
            }
            if (smooth) {
                points = movingAverage(points, 5);
            }
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(prettyField(field));
            for (Point point : points) {
                series.getData().add(new XYChart.Data<>(point.x(), point.y()));
                globalMin = Math.min(globalMin, point.y());
                globalMax = Math.max(globalMax, point.y());
            }
            chart.getData().add(series);
        }

        if (Double.isFinite(globalMin) && Double.isFinite(globalMax)) {
            XYChart.Series<Number, Number> trigger = new XYChart.Series<>();
            trigger.setName("Trigger t = 0");
            trigger.getData().add(new XYChart.Data<>(0, globalMin));
            trigger.getData().add(new XYChart.Data<>(0, globalMax));
            chart.getData().add(trigger);
        }
    }

    private List<Point> movingAverage(List<Point> points, int window) {
        if (points.size() < window) {
            return points;
        }
        List<Point> result = new ArrayList<>(points.size());
        int half = window / 2;
        for (int index = 0; index < points.size(); index++) {
            int start = Math.max(0, index - half);
            int end = Math.min(points.size(), index + half + 1);
            double sum = 0;
            for (int current = start; current < end; current++) {
                sum += points.get(current).y();
            }
            result.add(new Point(points.get(index).x(), sum / (end - start)));
        }
        return result;
    }

    private void populateTable(TableView<ObservableList<String>> table, TabularData data,
                               GrbData grb, String queryText) {
        table.getColumns().clear();
        String query = queryText == null ? "" : queryText.trim().toLowerCase(Locale.ROOT);
        ObservableList<ObservableList<String>> rows = FXCollections.observableArrayList();
        for (List<String> row : data.rows()) {
            if (query.isBlank() || String.join(" ", row).toLowerCase(Locale.ROOT).contains(query)) {
                rows.add(FXCollections.observableArrayList(row));
            }
        }
        table.setItems(rows);

        for (int index = 0; index < data.headers().size(); index++) {
            final int columnIndex = index;
            String header = data.headers().get(index);
            TableColumn<ObservableList<String>, String> column = new TableColumn<>();
            Label headerLabel = new Label(header);
            headerLabel.getStyleClass().add("table-header-label");
            FieldDefinition definition = grb.definition(header);
            if (definition != null) {
                headerLabel.setTooltip(new Tooltip(definition.simpleExplanation()));
            }
            column.setGraphic(headerLabel);
            column.setText("");
            column.setCellValueFactory(value -> new ReadOnlyStringWrapper(
                    columnIndex < value.getValue().size() ? value.getValue().get(columnIndex) : ""));
            column.setPrefWidth(Math.max(135, Math.min(220, header.length() * 8.5)));
            column.setCellFactory(ignored -> new TableCell<>() {
                @Override
                protected void updateItem(String value, boolean empty) {
                    super.updateItem(value, empty);
                    getStyleClass().removeAll("negative-cell", "warning-cell");
                    if (empty || value == null) {
                        setText(null);
                        return;
                    }
                    setText(value);
                    double numeric = parse(value);
                    if (header.contains("RATE") && Double.isFinite(numeric) && numeric < 0) {
                        getStyleClass().add("negative-cell");
                    }
                    if (header.equals("FRACEXP") && Double.isFinite(numeric) && numeric < 0.8) {
                        getStyleClass().add("warning-cell");
                    }
                }
            });
            table.getColumns().add(column);
        }
    }

    private void showFieldExplanation(VBox host, FieldDefinition definition, String fallbackField) {
        host.getChildren().clear();
        if (definition == null) {
            host.getChildren().addAll(
                    UiFactory.label(fallbackField == null ? "Campo" : fallbackField, "definition-title"),
                    UiFactory.wrappedLabel("Non è ancora disponibile una spiegazione specifica per questa voce.", "explanation-text"));
            return;
        }
        host.getChildren().addAll(
                UiFactory.label(definition.field(), "definition-title"),
                UiFactory.label(definition.category() + " · " + definition.source(), "definition-kicker"),
                miniExplanation("In parole semplici", definition.simpleExplanation()),
                miniExplanation("Descrizione tecnica", definition.technicalExplanation()),
                miniExplanation("Perché serve", definition.whyItMatters()),
                miniExplanation("Attenzione", definition.caution()));
    }

    private VBox miniExplanation(String title, String text) {
        VBox box = new VBox(5,
                UiFactory.label(title, "mini-explanation-title"),
                UiFactory.wrappedLabel(text, "mini-explanation-text"));
        box.getStyleClass().add("mini-explanation");
        return box;
    }

    private void showMetadataIntro(VBox host) {
        Label title = UiFactory.wrappedLabel("Come leggere i metadati", "definition-title");
        title.setMaxWidth(Double.MAX_VALUE);
        host.getChildren().setAll(
                title,
                UiFactory.wrappedLabel(
                        "Seleziona una riga. I metadati sono il registro tecnico del file FITS: descrivono provenienza, tempi, coordinate, struttura e passaggi di elaborazione.",
                        "explanation-text"),
                miniExplanation("HDU", "Un FITS può contenere più sezioni. PRIMARY è l'intestazione generale; RATE è la tabella della curva di luce."),
                miniExplanation("Keyword", "È il nome breve del parametro, per esempio OBS_ID, TRIGTIME o TIMEDEL."),
                miniExplanation("Commento originale", "È la descrizione scritta dal software che ha prodotto il FITS."));
    }

    private void showMetadataExplanation(VBox host, MetadataItem item, FieldDefinition definition) {
        host.getChildren().clear();
        Label metadataTitle = UiFactory.wrappedLabel(item.keyword().isBlank() ? "Voce FITS" : item.keyword(), "definition-title");
        metadataTitle.setMaxWidth(Double.MAX_VALUE);
        host.getChildren().addAll(
                metadataTitle,
                UiFactory.label("HDU " + item.hduIndex() + " · " + item.hduName(), "definition-kicker"),
                UiFactory.infoRow("Valore", item.value()),
                UiFactory.infoRow("Commento FITS", item.comment()));
        if (definition != null) {
            host.getChildren().addAll(
                    miniExplanation("In parole semplici", definition.simpleExplanation()),
                    miniExplanation("Perché serve", definition.whyItMatters()),
                    miniExplanation("Attenzione", definition.caution()));
        } else {
            host.getChildren().add(UiFactory.wrappedLabel(
                    "Questa keyword non è ancora inclusa nel dizionario didattico. Il commento originale del FITS rimane comunque visibile.",
                    "subtle-text"));
        }
    }

    private FieldDefinition findMetadataDefinition(GrbData data, String keyword) {
        if (keyword == null) {
            return null;
        }
        FieldDefinition direct = data.definition(keyword);
        if (direct != null) {
            return direct;
        }
        if (keyword.equalsIgnoreCase("RA_OBJ") || keyword.equalsIgnoreCase("DEC_OBJ")) {
            return data.definition("RA_OBJ / DEC_OBJ");
        }
        return null;
    }

    private <T> TableColumn<T, String> metadataColumn(String title, java.util.function.Function<T, String> mapper, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(value -> new ReadOnlyStringWrapper(mapper.apply(value.getValue())));
        column.setPrefWidth(width);
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(value);
                    setTooltip(value.length() > 20 ? new Tooltip(value) : null);
                }
            }
        });
        return column;
    }

    private ScrollPane scrollableSide(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().addAll("detail-scroll", "side-detail-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinWidth(300);
        scroll.setPrefWidth(380);
        return scroll;
    }

    private void exportNode(Node node, String suggestedName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Esporta il grafico");
        chooser.setInitialFileName(suggestedName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagine PNG", "*.png"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            WritableImage image = node.snapshot(new SnapshotParameters(), null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException error) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Esportazione non riuscita: " + error.getMessage());
            alert.showAndWait();
        }
    }

    private void exportExcel(GrbData data, boolean asciiOnly) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(asciiOnly ? "Esporta ASCII in Excel" : "Esporta FITS e metadati in Excel");
        chooser.setInitialFileName(data.grbName() + (asciiOnly ? "_ASCII_4CH_1S.xlsx" : "_FITS_METADATI.xlsx"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cartella Excel", "*.xlsx"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }
        try {
            if (asciiOnly) {
                excelExportService.exportAscii(data, file.toPath());
            } else {
                excelExportService.exportFitsAndMetadata(data, file.toPath());
            }
            new Alert(Alert.AlertType.INFORMATION,
                    "File creato correttamente:\n" + file.getAbsolutePath()).showAndWait();
        } catch (IOException error) {
            new Alert(Alert.AlertType.ERROR,
                    "Esportazione Excel non riuscita: " + error.getMessage()).showAndWait();
        }
    }

    private void setWorkspace(Node node) {
        workspace.getChildren().setAll(node);
        StackPane.setAlignment(node, Pos.CENTER);
    }

    private String readableError(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private double parse(List<String> row, int index) {
        return index >= 0 && index < row.size() ? parse(row.get(index)) : Double.NaN;
    }

    private double parse(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return Double.NaN;
        }
    }

    private String prettyField(String field) {
        return switch (field) {
            case "RATE_15_25_KEV" -> "15–25 keV";
            case "RATE_25_50_KEV" -> "25–50 keV";
            case "RATE_50_100_KEV" -> "50–100 keV";
            case "RATE_100_350_KEV" -> "100–350 keV";
            case "RATE_15_350_KEV", "RATE" -> "Totale 15–350 keV";
            default -> field;
        };
    }

    private static Map<String, String> createChannels() {
        Map<String, String> channels = new LinkedHashMap<>();
        channels.put("Totale 15–350 keV", "RATE_15_350_KEV");
        channels.put("15–25 keV", "RATE_15_25_KEV");
        channels.put("25–50 keV", "RATE_25_50_KEV");
        channels.put("50–100 keV", "RATE_50_100_KEV");
        channels.put("100–350 keV", "RATE_100_350_KEV");
        channels.put("Tutte le bande", "ALL");
        return channels;
    }

    private static Map<String, Double> createWindows() {
        Map<String, Double> windows = new LinkedHashMap<>();
        windows.put("±20 s dal trigger", 20.0);
        windows.put("±60 s dal trigger", 60.0);
        windows.put("±120 s dal trigger", 120.0);
        windows.put("Intera osservazione", Double.POSITIVE_INFINITY);
        return windows;
    }

    private record Point(double x, double y) {
    }

    private final class CatalogCell extends ListCell<CatalogEntry> {
        @Override
        protected void updateItem(CatalogEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Label star = UiFactory.label("✦", "catalog-star");
            VBox copy = new VBox(2,
                    UiFactory.label(item.grbName(), "catalog-name"),
                    UiFactory.label("Trigger " + item.triggerId(), "catalog-trigger"));
            SkyBurst burst = scientificMetadata.get(item.grbName().toUpperCase(Locale.ROOT));
            if (burst != null) {
                copy.getChildren().add(UiFactory.label(
                        burst.formattedT90() + " · " + burst.redshift().displayValue(), "catalog-science"));
            }
            HBox.setHgrow(copy, Priority.ALWAYS);
            row.getChildren().addAll(star, copy);
            if (cacheLookup.test(item)) {
                Label cached = UiFactory.label("IN CACHE", "cache-badge");
                row.getChildren().add(cached);
            }
            setGraphic(row);
        }
    }
}
