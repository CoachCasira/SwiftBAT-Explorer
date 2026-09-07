from pathlib import Path
import re

BRANCH = "feature/analysis-population-1.3.0"
POP = Path("src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java")
EXP = Path("src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java")
CSS = Path("src/main/resources/app.css")


def sub_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"Patch non applicabile: {label} (match={count})")
    return updated


# -----------------------------------------------------------------------------
# Analisi di popolazione: filtri compatti + due slider indipendenti FRACEXP.
# -----------------------------------------------------------------------------
pop = POP.read_text(encoding="utf-8")

pop = pop.replace("import javafx.application.Platform;\n", "import javafx.animation.PauseTransition;\nimport javafx.application.Platform;\n", 1)
pop = pop.replace("import javafx.scene.control.ScrollPane;\n", "import javafx.scene.control.ScrollPane;\nimport javafx.scene.control.Slider;\n", 1)
pop = pop.replace("import javafx.scene.shape.Line;\n", "import javafx.scene.shape.Line;\nimport javafx.util.Duration;\n", 1)

pop = pop.replace(
'''    private final TextField exposureMin = percentField("0");
    private final TextField exposureMax = percentField("100");''',
'''    private final Slider exposureMinSlider = exposureSlider(0);
    private final Slider exposureMaxSlider = exposureSlider(100);
    private final TextField exposureMin = percentField("0");
    private final TextField exposureMax = percentField("100");
    private final PauseTransition exposureMinDebounce = new PauseTransition(Duration.millis(350));
    private final PauseTransition exposureMaxDebounce = new PauseTransition(Duration.millis(350));
    private boolean syncingExposureControls;''', 1)

pop = pop.replace("        VBox page = new VBox(18);\n        page.setPadding(new Insets(30, 34, 36, 34));",
                  "        VBox page = new VBox(12);\n        page.setPadding(new Insets(18, 24, 24, 24));", 1)
pop = pop.replace("        resultTabs.setMinHeight(480);\n        resultTabs.setPrefHeight(520);\n        resultTabs.setMaxHeight(570);",
                  "        resultTabs.setMinHeight(450);\n        resultTabs.setPrefHeight(500);\n        resultTabs.setMaxHeight(560);", 1)

pop = sub_once(pop,
    r'    private VBox buildFilters\(\) \{.*?\n    \}\n\n    private Node chartCard\(\)',
'''    private VBox buildFilters() {
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

        FlowPane primary = new FlowPane(10, 7);
        primary.getStyleClass().add("population-filter-grid");
        primary.getChildren().addAll(
                filterGroup("Durata T90", "Classe temporale", duration, 200),
                filterGroup("Redshift", "Disponibilità della misura z", redshiftAvailability, 200),
                filterGroup("Finestra temporale", "Secondi attorno al trigger", window, 160),
                filterGroup("Campione massimo", "GRB più recenti dopo i filtri", limit, 160));

        FlowPane advancedContent = new FlowPane(10, 7);
        advancedContent.getStyleClass().addAll("population-filter-grid", "advanced-filter-row");
        advancedContent.getChildren().addAll(
                filterGroup("Intervallo redshift z", "Applicato ai GRB con z", range(zMin, zMax), 205),
                filterGroup("Ascensione retta RA", "Intervallo 0°–360°", range(raMin, raMax), 205),
                filterGroup("Declinazione DEC", "Intervallo −90°–+90°", range(decMin, decMax), 205));
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
        HBox exposureControls = new HBox(12, minimum, maximum);
        exposureControls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(minimum, Priority.ALWAYS);
        HBox.setHgrow(maximum, Priority.ALWAYS);

        VBox exposureIntro = new VBox(3,
                UiFactory.label("Qualità della copertura FRACEXP", "population-section-title"),
                UiFactory.wrappedLabel(
                        "Percentuale di bin con FRACEXP ≥ 0,999. Trascina oppure scrivi il valore.",
                        "sky-filter-help"));
        exposureIntro.setMinWidth(250);
        exposureIntro.setPrefWidth(300);
        HBox exposureBox = new HBox(14, exposureIntro, exposureControls);
        exposureBox.setAlignment(Pos.CENTER_LEFT);
        exposureBox.getStyleClass().addAll("population-filter-section", "population-filter-section-compact");
        HBox.setHgrow(exposureControls, Priority.ALWAYS);

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

        VBox card = new VBox(9, primary, advanced, advancedContent, exposureBox, footer);
        card.getStyleClass().addAll("card", "population-filter-card", "population-filter-card-compact");
        card.setPadding(new Insets(12));
        return card;
    }

    private Node chartCard()''',
    "buildFilters compatto")

pop = sub_once(pop,
    r'    private void configureControls\(\) \{.*?\n    \}\n\n    private void configureChart\(\)',
'''    private void configureControls() {
        configureExposureControl(exposureMinSlider, exposureMin, true, exposureMinDebounce);
        configureExposureControl(exposureMaxSlider, exposureMax, false, exposureMaxDebounce);
        duration.valueProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        redshiftAvailability.valueProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        limit.valueProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        for (TextField field : List.of(zMin, zMax, raMin, raMax, decMin, decMax)) {
            field.textProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        }
    }

    private void configureExposureControl(Slider slider, TextField field, boolean minimum,
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

    private void commitExposureField(Slider slider, TextField field, boolean minimum) {
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

    private void configureChart()''',
    "configureControls FRACEXP")

pop = pop.replace(
'''        exposureMin.setText("0");
        exposureMax.setText("100");''',
'''        syncingExposureControls = true;
        exposureMinSlider.setValue(0);
        exposureMaxSlider.setValue(100);
        exposureMin.setText("0");
        exposureMax.setText("100");
        syncingExposureControls = false;''', 1)

pop = sub_once(pop,
    r'    private static TextField percentField\(String value\) \{.*?\n    \}\n\n    private VBox exposureControl\(String label, TextField field, boolean minimum\) \{.*?\n    \}\n\n    private void adjustExposure\(TextField field, int delta, boolean minimum\) \{.*?\n    \}\n\n    private void normalizeExposureField\(TextField field, boolean minimum\) \{.*?\n    \}\n\n    private Integer parseExposureInteger\(TextField field\) \{.*?\n    \}',
'''    private static Slider exposureSlider(double value) {
        Slider slider = new Slider(0, 100, value);
        slider.getStyleClass().add("fracexp-slider");
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(false);
        slider.setMinWidth(120);
        slider.setPrefWidth(230);
        slider.setMaxWidth(Double.MAX_VALUE);
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

    private VBox exposureControl(String label, Slider slider, TextField field, boolean minimum) {
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
        box.setMinWidth(260);
        box.setPrefWidth(340);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }''',
    "controlli FRACEXP slider")

POP.write_text(pop, encoding="utf-8")

# -----------------------------------------------------------------------------
# Esplora: filtri extra collassabili, metriche compatte, fullscreen 2D in basso.
# -----------------------------------------------------------------------------
exp = EXP.read_text(encoding="utf-8")

exp = exp.replace("import javafx.scene.control.Tooltip;\n", "import javafx.scene.control.Tooltip;\nimport javafx.scene.control.ToggleButton;\n", 1)

exp = exp.replace(
'''    private final ComboBox<String> durationFilter = new ComboBox<>();
    private final ComboBox<String> redshiftFilter = new ComboBox<>();''',
'''    private final ComboBox<String> durationFilter = new ComboBox<>();
    private final ComboBox<String> redshiftFilter = new ComboBox<>();
    private final ComboBox<String> cacheFilter = new ComboBox<>();
    private final TextField t90MinFilter = compactFilterField("min");
    private final TextField t90MaxFilter = compactFilterField("max");
    private final TextField redshiftMinFilter = compactFilterField("min");
    private final TextField redshiftMaxFilter = compactFilterField("max");''', 1)

exp = sub_once(exp,
    r'    private Node buildBody\(\) \{.*?\n    \}\n\n    private void wireCatalog\(\)',
'''    private Node buildBody() {
        VBox sidebar = new VBox(9);
        sidebar.getStyleClass().add("catalog-panel");
        sidebar.setPadding(new Insets(14));
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

        cacheFilter.setItems(FXCollections.observableArrayList("Tutti", "Solo in cache", "Da scaricare"));
        cacheFilter.setValue("Tutti");
        cacheFilter.getStyleClass().add("choice-box-modern");
        cacheFilter.setMaxWidth(Double.MAX_VALUE);

        GridPane extraGrid = new GridPane();
        extraGrid.setHgap(8);
        extraGrid.setVgap(6);
        extraGrid.add(UiFactory.label("T90 (s)", "filter-label"), 0, 0);
        extraGrid.add(UiFactory.label("Redshift z", "filter-label"), 1, 0);
        extraGrid.add(filterRange(t90MinFilter, t90MaxFilter), 0, 1);
        extraGrid.add(filterRange(redshiftMinFilter, redshiftMaxFilter), 1, 1);
        var extraFirst = new javafx.scene.layout.ColumnConstraints();
        extraFirst.setPercentWidth(50);
        extraFirst.setHgrow(Priority.ALWAYS);
        var extraSecond = new javafx.scene.layout.ColumnConstraints();
        extraSecond.setPercentWidth(50);
        extraSecond.setHgrow(Priority.ALWAYS);
        extraGrid.getColumnConstraints().addAll(extraFirst, extraSecond);

        Button clearExtra = UiFactory.button("Azzera filtri extra", "ghost-button");
        clearExtra.setOnAction(event -> {
            cacheFilter.setValue("Tutti");
            t90MinFilter.clear();
            t90MaxFilter.clear();
            redshiftMinFilter.clear();
            redshiftMaxFilter.clear();
            applyCatalogFilters();
        });
        VBox extraBox = new VBox(8,
                UiFactory.label("Cache locale", "filter-label"), cacheFilter,
                extraGrid, clearExtra);
        extraBox.getStyleClass().add("explorer-extra-filters");
        extraBox.setVisible(false);
        extraBox.setManaged(false);

        ToggleButton extraToggle = new ToggleButton("Altri filtri ▾");
        extraToggle.getStyleClass().addAll("sky-toggle", "explorer-extra-toggle");
        extraToggle.setMaxWidth(Double.MAX_VALUE);
        extraToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            extraBox.setVisible(selected);
            extraBox.setManaged(selected);
            extraToggle.setText(selected ? "Nascondi filtri ▴" : "Altri filtri ▾");
        });

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
        sidebar.getChildren().addAll(title, catalogSearch, filterGrid, extraToggle, extraBox,
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

    private void wireCatalog()''',
    "Explorer buildBody")

exp = sub_once(exp,
    r'    private void wireCatalog\(\) \{.*?\n    \}\n\n    private void applyCatalogFilters\(\)',
'''    private void wireCatalog() {
        catalogSearch.textProperty().addListener((observable, oldValue, newValue) -> applyCatalogFilters());
        durationFilter.setOnAction(event -> applyCatalogFilters());
        redshiftFilter.setOnAction(event -> applyCatalogFilters());
        cacheFilter.setOnAction(event -> applyCatalogFilters());
        for (TextField field : List.of(t90MinFilter, t90MaxFilter, redshiftMinFilter, redshiftMaxFilter)) {
            field.textProperty().addListener((obs, oldValue, newValue) -> applyCatalogFilters());
        }
        catalogList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != selectedEntry) {
                selectedEntry = newValue;
                loadRequest.accept(newValue, false);
            }
        });
    }

    private void applyCatalogFilters()''',
    "Explorer wireCatalog")

exp = sub_once(exp,
    r'    private void applyCatalogFilters\(\) \{.*?\n    \}\n\n    private void showEmptyState\(\)',
'''    private void applyCatalogFilters() {
        String query = catalogSearch.getText() == null ? "" : catalogSearch.getText().trim().toLowerCase(Locale.ROOT);
        String duration = durationFilter.getValue() == null ? "Tutte" : durationFilter.getValue();
        String redshift = redshiftFilter.getValue() == null ? "Tutti" : redshiftFilter.getValue();
        String cache = cacheFilter.getValue() == null ? "Tutti" : cacheFilter.getValue();
        Double t90Min = optionalNumber(t90MinFilter);
        Double t90Max = optionalNumber(t90MaxFilter);
        Double zMin = optionalNumber(redshiftMinFilter);
        Double zMax = optionalNumber(redshiftMaxFilter);

        filteredCatalog.setPredicate(entry -> {
            if (!query.isBlank()
                    && !entry.grbName().toLowerCase(Locale.ROOT).contains(query)
                    && !entry.triggerId().toLowerCase(Locale.ROOT).contains(query)) {
                return false;
            }
            boolean cached = cacheLookup.test(entry);
            if (cache.equals("Solo in cache") && !cached) return false;
            if (cache.equals("Da scaricare") && cached) return false;

            SkyBurst burst = scientificMetadata.get(entry.grbName().toUpperCase(Locale.ROOT));
            boolean hasNumericFilters = t90Min != null || t90Max != null || zMin != null || zMax != null;
            if (burst == null) {
                return duration.equals("Tutte") && !redshift.equals("Con z") && !hasNumericFilters;
            }
            if (duration.startsWith("Short") && !burst.isShort()) return false;
            if (duration.startsWith("Long") && !burst.isLong()) return false;
            if (duration.equals("T90 n.d.") && burst.hasT90()) return false;
            if (redshift.equals("Con z") && !burst.redshift().available()) return false;
            if (redshift.equals("Senza z") && burst.redshift().available()) return false;

            Double t90 = burst.t90Sec();
            if (t90Min != null && (t90 == null || t90 < t90Min)) return false;
            if (t90Max != null && (t90 == null || t90 > t90Max)) return false;
            Double z = burst.redshift().representativeValue();
            if (zMin != null && (z == null || z < zMin)) return false;
            return zMax == null || (z != null && z <= zMax);
        });
        catalogCount.setText(filteredCatalog.size() + " GRB visualizzati");
    }

    private void showEmptyState()''',
    "Explorer applyCatalogFilters")

exp = exp.replace("        VBox dashboard = new VBox(18);", "        VBox dashboard = new VBox(10);", 1)
exp = exp.replace("        FlowPane metrics = new FlowPane(12, 12);", "        FlowPane metrics = new FlowPane(8, 8);\n        metrics.getStyleClass().add(\"explorer-metrics\");", 1)
exp = exp.replace(
'''                    UiFactory.metricCard("T90", scientific.formattedT90(), scientific.durationClass()),
                    UiFactory.metricCard("Redshift", scientific.redshift().available()
                            ? scientific.redshift().rawValue() : "n.d.", "z cosmologico · valore BAT"));''',
'''                    compactMetricCard("T90", scientific.formattedT90(), scientific.durationClass()),
                    compactMetricCard("Redshift", scientific.redshift().available()
                            ? scientific.redshift().rawValue() : "n.d.", "z cosmologico · valore BAT"));''', 1)
exp = exp.replace("        tabs.setMinHeight(590);\n        tabs.setPrefHeight(680);",
                  "        tabs.setMinHeight(520);\n        tabs.setPrefHeight(610);", 1)

exp = sub_once(exp,
    r'    private VBox metric\(GrbData data, String key, String title, String detail\) \{.*?\n    \}\n\n    private Tab tab\(String title, Node content\)',
'''    private VBox metric(GrbData data, String key, String title, String detail) {
        SummaryItem item = data.summaryByKey().get(key);
        String value = DisplayFormat.summary(key, item);
        VBox card = compactMetricCard(title, value, detail);
        if (item != null && item.value() != null && !item.value().isBlank()) {
            String complete = item.value() + (item.unit().isBlank() ? "" : " " + item.unit());
            Tooltip.install(card, UiFactory.quickTooltip("Valore completo: " + complete));
        }
        return card;
    }

    private VBox compactMetricCard(String eyebrow, String value, String detail) {
        VBox card = UiFactory.metricCard(eyebrow, value, detail);
        card.getStyleClass().add("metric-card-compact");
        card.setMinWidth(128);
        card.setPrefWidth(146);
        card.setMaxWidth(168);
        return card;
    }

    private Tab tab(String title, Node content)''',
    "Explorer metric compact")

exp = sub_once(exp,
    r'    private Node buildOverview\(GrbData data\) \{.*?\n    \}\n\n    private Node buildThreeD\(GrbData data\)',
'''    private Node buildOverview(GrbData data) {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));
        VBox chartCard = new VBox(9);
        chartCard.getStyleClass().addAll("card", "overview-chart-card");
        chartCard.setPadding(new Insets(12));
        HBox controls = new HBox(9);
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
        UiFactory.autoTooltip(channelChoice);
        UiFactory.autoTooltip(windowChoice);
        CheckBox smooth = new CheckBox("Media mobile 5 bin");
        smooth.getStyleClass().add("modern-check");
        UiFactory.autoTooltip(smooth);
        Label help = UiFactory.label("t = 0 indica il trigger", "subtle-text");
        controls.getChildren().addAll(channelChoice, windowChoice, smooth, UiFactory.spacer(), help);

        LineChart<Number, Number> chart = createLightCurveChart();
        VBox.setVgrow(chart, Priority.ALWAYS);

        Runnable refresh = () -> populateChart(chart, data, channelChoice.getValue(), windowChoice.getValue(), smooth.isSelected());
        channelChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        windowChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        smooth.selectedProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        refresh.run();

        Button export = UiFactory.button("Esporta PNG", "ghost-button");
        export.setOnAction(event -> exportNode(chart, data.grbName() + "_curva_1s.png"));
        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "secondary-button");
        fullscreen.setOnAction(event -> openOverviewFullscreen(
                data, channelChoice.getValue(), windowChoice.getValue(), smooth.isSelected()));
        HBox bottomActions = new HBox(8, UiFactory.spacer(), export, fullscreen);
        bottomActions.setAlignment(Pos.CENTER_RIGHT);
        chartCard.getChildren().addAll(controls, chart, bottomActions);
        pane.setCenter(chartCard);

        VBox right = new VBox(10);
        right.setPrefWidth(310);
        right.getChildren().addAll(
                summaryCard(data, "In breve", List.of("BIN_SIZE", "ENERGY_RANGE", "TIME_RANGE", "ASCII_ROWS", "FITS_ROWS")),
                plainConceptCard("Trigger", "Il punto zero dell'allerta", "Tempi negativi: prima del trigger. Tempi positivi: dopo il trigger. Il trigger non coincide necessariamente con l'inizio fisico esatto del burst."));
        pane.setRight(right);
        BorderPane.setMargin(right, new Insets(0, 0, 0, 12));
        return pane;
    }

    private LineChart<Number, Number> createLightCurveChart() {
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
        return chart;
    }

    private void openOverviewFullscreen(GrbData data, String channel, String window, boolean smooth) {
        LineChart<Number, Number> enlarged = createLightCurveChart();
        enlarged.setMinHeight(0);
        enlarged.setMaxHeight(Double.MAX_VALUE);
        enlarged.setPrefHeight(760);
        populateChart(enlarged, data, channel, window, smooth);
        VBox content = new VBox(8, enlarged);
        content.setMinWidth(0);
        content.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(enlarged, Priority.ALWAYS);
        InPlaceFullscreen.show(this, data.grbName() + " · Curva 2D", content);
    }

    private Node buildThreeD(GrbData data)''',
    "Explorer buildOverview fullscreen")

# Helper per i filtri extra, inserito prima dello stato vuoto.
exp = exp.replace(
'''    private void showEmptyState() {''',
'''    private static TextField compactFilterField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().addAll("search-field", "explorer-compact-field");
        field.setPrefWidth(70);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static HBox filterRange(TextField minimum, TextField maximum) {
        HBox box = new HBox(5, minimum, UiFactory.label("–", "filter-label"), maximum);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(minimum, Priority.ALWAYS);
        HBox.setHgrow(maximum, Priority.ALWAYS);
        return box;
    }

    private static Double optionalNumber(TextField field) {
        String text = field.getText();
        if (text == null || text.isBlank()) return null;
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void showEmptyState() {''', 1)

EXP.write_text(exp, encoding="utf-8")

# -----------------------------------------------------------------------------
# CSS: compattazione senza togliere leggibilità.
# -----------------------------------------------------------------------------
css = CSS.read_text(encoding="utf-8")
marker = "/* ---------- Analisi di popolazione ---------- */"
if marker not in css:
    raise SystemExit("Marker CSS non trovato")

extra_css = r'''

/* ---------- Layout compatto Explorer / Popolazione ---------- */
.population-filter-card-compact {
    -fx-spacing: 9px;
}

.population-filter-card-compact .population-filter-group {
    -fx-padding: 7 9;
}

.population-filter-card-compact .choice-box-modern {
    -fx-padding: 7 10;
}

.population-filter-section-compact {
    -fx-padding: 9 11;
}

.population-filter-section-compact .sky-filter-help {
    -fx-font-size: 9px;
}

.percentage-control {
    -fx-padding: 7 9;
}

.percentage-field {
    -fx-font-size: 13px;
    -fx-padding: 5 7;
}

.fracexp-slider {
    -fx-padding: 0 4 0 4;
}

.fracexp-slider .track {
    -fx-background-color: rgba(255, 255, 255, 0.10);
    -fx-background-radius: 999px;
    -fx-pref-height: 5px;
}

.fracexp-slider .thumb {
    -fx-background-color: linear-gradient(to bottom right, #ffae4a, #a66bdd);
    -fx-background-radius: 999px;
    -fx-pref-width: 15px;
    -fx-pref-height: 15px;
    -fx-effect: dropshadow(gaussian, rgba(255, 174, 74, 0.24), 7, 0.18, 0, 1);
}

.filter-reset-button {
    -fx-background-color: rgba(255, 174, 74, 0.08);
    -fx-text-fill: #ffc47e;
    -fx-border-color: rgba(255, 174, 74, 0.18);
    -fx-background-radius: 8px;
    -fx-border-radius: 8px;
    -fx-min-width: 29px;
    -fx-min-height: 29px;
    -fx-padding: 3px;
    -fx-font-size: 13px;
    -fx-font-weight: bold;
}

.filter-reset-button:hover {
    -fx-background-color: rgba(255, 174, 74, 0.16);
}

.explorer-extra-toggle {
    -fx-padding: 7 10;
}

.explorer-extra-filters {
    -fx-background-color: rgba(255, 255, 255, 0.018);
    -fx-border-color: rgba(255, 255, 255, 0.055);
    -fx-background-radius: 10px;
    -fx-border-radius: 10px;
    -fx-padding: 9px;
}

.explorer-extra-filters .choice-box-modern,
.explorer-compact-field {
    -fx-padding: 6 8;
}

.explorer-metrics {
    -fx-padding: 0;
}

.metric-card-compact {
    -fx-padding: 9 11;
    -fx-background-radius: 13px;
    -fx-border-radius: 13px;
}

.metric-card-compact .metric-eyebrow {
    -fx-font-size: 8px;
}

.metric-card-compact .metric-value {
    -fx-font-size: 15px;
}

.metric-card-compact .metric-detail {
    -fx-font-size: 8px;
}

.overview-chart-card {
    -fx-padding: 12px;
}
'''

if "/* ---------- Layout compatto Explorer / Popolazione ---------- */" not in css:
    css += extra_css
CSS.write_text(css, encoding="utf-8")

print("Patch layout compatto applicata.")
