from pathlib import Path

SKY = Path('src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java')
EXP = Path('src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java')
POP = Path('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java')
FULL = Path('src/main/java/it/casiraghi/swiftbat/ui/InPlaceFullscreen.java')


def rep(path, old, new, label):
    text = path.read_text(encoding='utf-8')
    if old in text:
        path.write_text(text.replace(old, new, 1), encoding='utf-8')
        return
    if new in text:
        print(f'{label}: già applicato')
        return
    raise SystemExit(f'Patch non applicabile: {label}')

# ---------------------------------------------------------------------------
# Mappa celeste: filtri tutti visibili, auto-applicazione e fullscreen con dettagli.
# ---------------------------------------------------------------------------
rep(SKY,
    'import javafx.collections.FXCollections;\n',
    'import javafx.animation.PauseTransition;\nimport javafx.collections.FXCollections;\n',
    'import PauseTransition mappa')
rep(SKY,
    'import javafx.scene.layout.VBox;\n',
    'import javafx.scene.layout.VBox;\nimport javafx.util.Duration;\n',
    'import Duration mappa')
rep(SKY,
    '    private SkyBurst selectedBurst;\n    private boolean sphereView;\n',
    '    private SkyBurst selectedBurst;\n    private boolean sphereView;\n    private final PauseTransition filterDebounce = new PauseTransition(Duration.millis(900));\n',
    'debounce mappa')

old_filters = '''    private VBox buildFilters() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(13, 15, 13, 15));

        HBox firstRow = new HBox(9);
        firstRow.setAlignment(Pos.CENTER_LEFT);
        search.setPromptText("Cerca GRB…");
        search.getStyleClass().add("modern-text-field");
        search.setPrefWidth(220);

        durationFilter.setItems(FXCollections.observableArrayList(
                FILTER_ALL, FILTER_SHORT, FILTER_LONG, FILTER_UNKNOWN));
        durationFilter.setValue(FILTER_ALL);
        durationFilter.getStyleClass().add("choice-box-modern");
        durationFilter.setPrefWidth(190);

        redshiftFilter.setItems(FXCollections.observableArrayList(
                "Con e senza redshift", "Solo con redshift", "Solo senza redshift"));
        redshiftFilter.setValue("Con e senza redshift");
        redshiftFilter.getStyleClass().add("choice-box-modern");
        redshiftFilter.setPrefWidth(185);

        galacticPlane.getStyleClass().add("modern-check");
        ToggleButton advanced = new ToggleButton("Filtri avanzati  ▾");
        advanced.getStyleClass().add("sky-toggle");
        Button apply = UiFactory.button("Applica", "secondary-button");
        Button reset = UiFactory.button("Reset", "ghost-button");
        apply.setOnAction(event -> applyFilters());
        reset.setOnAction(event -> resetFilters());
        search.setOnAction(event -> applyFilters());
        durationFilter.setOnAction(event -> applyFilters());
        redshiftFilter.setOnAction(event -> applyFilters());
        firstRow.getChildren().addAll(search, durationFilter, redshiftFilter, galacticPlane,
                UiFactory.spacer(), apply, reset);

        HBox advancedHeader = new HBox(8, advanced,
                UiFactory.label("RA, DEC e intervallo di redshift", "sky-filter-help"));
        advancedHeader.setAlignment(Pos.CENTER_LEFT);

        HBox rangeRow = new HBox(9);
        rangeRow.getStyleClass().add("advanced-filter-row");
        rangeRow.setAlignment(Pos.CENTER_LEFT);
        Label raLabel = UiFactory.label("RA", "filter-label");
        Label decLabel = UiFactory.label("DEC", "filter-label");
        Label zLabel = UiFactory.label("z", "filter-label");
        Label help = UiFactory.wrappedLabel("RA può attraversare 0°. Limiti/intervalli di z sono inclusi se compatibili.", "sky-filter-help");
        HBox.setHgrow(help, Priority.ALWAYS);
        rangeRow.getChildren().addAll(
                raLabel, raMin, UiFactory.label("–", "filter-label"), raMax,
                decLabel, decMin, UiFactory.label("–", "filter-label"), decMax,
                zLabel, zMin, UiFactory.label("–", "filter-label"), zMax,
                help);
        rangeRow.setVisible(false);
        rangeRow.setManaged(false);
        advanced.selectedProperty().addListener((obs, oldValue, selected) -> {
            rangeRow.setVisible(selected);
            rangeRow.setManaged(selected);
            advanced.setText(selected ? "Nascondi filtri avanzati  ▴" : "Filtri avanzati  ▾");
        });

        card.getChildren().addAll(firstRow, advancedHeader, rangeRow);
        return card;
    }
'''
new_filters = '''    private VBox buildFilters() {
        VBox card = new VBox(7);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12, 14, 12, 14));

        HBox row = new HBox(7);
        row.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("Cerca GRB…");
        search.getStyleClass().add("modern-text-field");
        search.setMinWidth(145);
        search.setPrefWidth(165);
        search.setMaxWidth(175);

        durationFilter.setItems(FXCollections.observableArrayList(
                FILTER_ALL, FILTER_SHORT, FILTER_LONG, FILTER_UNKNOWN));
        durationFilter.setValue(FILTER_ALL);
        durationFilter.getStyleClass().add("choice-box-modern");
        durationFilter.setMinWidth(135);
        durationFilter.setPrefWidth(145);

        redshiftFilter.setItems(FXCollections.observableArrayList(
                "Con e senza redshift", "Solo con redshift", "Solo senza redshift"));
        redshiftFilter.setValue("Con e senza redshift");
        redshiftFilter.getStyleClass().add("choice-box-modern");
        redshiftFilter.setMinWidth(145);
        redshiftFilter.setPrefWidth(155);

        galacticPlane.getStyleClass().add("modern-check");
        Button reset = UiFactory.button("Reset", "ghost-button");
        reset.setOnAction(event -> resetFilters());
        search.setOnAction(event -> {
            filterDebounce.stop();
            applyFilters();
        });

        HBox raRange = compactSkyRange("RA", raMin, raMax);
        HBox decRange = compactSkyRange("DEC", decMin, decMax);
        HBox zRange = compactSkyRange("z", zMin, zMax);
        row.getChildren().addAll(search, durationFilter, redshiftFilter,
                raRange, decRange, zRange, galacticPlane, reset);

        Label help = UiFactory.wrappedLabel(
                "RA può attraversare 0°. I limiti RA/DEC e l'intervallo di redshift vengono applicati automaticamente.",
                "sky-filter-help");
        card.getChildren().addAll(row, help);
        return card;
    }

    private HBox compactSkyRange(String label, TextField minimum, TextField maximum) {
        minimum.setMinWidth(42);
        minimum.setPrefWidth(46);
        minimum.setMaxWidth(52);
        maximum.setMinWidth(42);
        maximum.setPrefWidth(46);
        maximum.setMaxWidth(52);
        HBox box = new HBox(4,
                UiFactory.label(label, "filter-label"),
                minimum,
                UiFactory.label("–", "filter-label"),
                maximum);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
'''
rep(SKY, old_filters, new_filters, 'layout filtri mappa')

old_fullscreen = '''    private void openMapFullscreen() {
        if (getScene() == null) {
            return;
        }
        if (sphereView) {
            CelestialSpherePane enlarged = new CelestialSpherePane();
            enlarged.setBursts(visibleBursts);
            enlarged.setShowGalacticPlane(galacticPlane.isSelected());
            enlarged.setOnSelect(this::selectBurst);
            if (selectedBurst != null) enlarged.select(selectedBurst);
            enlarged.setMinSize(700, 520);
            InPlaceFullscreen.show(this, "Mappa celeste · Sfera 3D", enlarged);
        } else {
            MollweideSkyPane enlarged = new MollweideSkyPane();
            enlarged.setBursts(visibleBursts);
            enlarged.setShowGalacticPlane(galacticPlane.isSelected());
            enlarged.setOnSelect(this::selectBurst);
            if (selectedBurst != null) enlarged.select(selectedBurst);
            enlarged.setMinSize(700, 520);
            InPlaceFullscreen.show(this, "Mappa celeste · Mollweide 2D", enlarged);
        }
    }
'''
new_fullscreen = '''    private void openMapFullscreen() {
        if (getScene() == null) {
            return;
        }
        FullscreenDetails details = createFullscreenDetails();
        HBox layout = new HBox(14);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setMinSize(0, 0);

        if (sphereView) {
            CelestialSpherePane enlarged = new CelestialSpherePane();
            enlarged.setBursts(visibleBursts);
            enlarged.setShowGalacticPlane(galacticPlane.isSelected());
            enlarged.setOnSelect(burst -> {
                enlarged.select(burst);
                selectBurst(burst);
                details.update().accept(burst);
            });
            if (selectedBurst != null) enlarged.select(selectedBurst);
            enlarged.setMinSize(520, 420);
            enlarged.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            HBox.setHgrow(enlarged, Priority.ALWAYS);
            layout.getChildren().addAll(enlarged, details.node());
            InPlaceFullscreen.show(this, "Mappa celeste · Sfera 3D", layout);
        } else {
            MollweideSkyPane enlarged = new MollweideSkyPane();
            enlarged.setBursts(visibleBursts);
            enlarged.setShowGalacticPlane(galacticPlane.isSelected());
            enlarged.setOnSelect(burst -> {
                enlarged.select(burst);
                selectBurst(burst);
                details.update().accept(burst);
            });
            if (selectedBurst != null) enlarged.select(selectedBurst);
            enlarged.setMinSize(520, 420);
            enlarged.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            HBox.setHgrow(enlarged, Priority.ALWAYS);
            layout.getChildren().addAll(enlarged, details.node());
            InPlaceFullscreen.show(this, "Mappa celeste · Mollweide 2D", layout);
        }
    }

    private FullscreenDetails createFullscreenDetails() {
        Label name = UiFactory.label("Nessun GRB selezionato", "sky-selected-title");
        Label trigger = UiFactory.label("—", "info-value");
        Label ra = UiFactory.wrappedLabel("—", "info-value");
        Label dec = UiFactory.wrappedLabel("—", "info-value");
        Label t90 = UiFactory.label("—", "info-value");
        Label clazz = UiFactory.wrappedLabel("—", "info-value");
        Label redshift = UiFactory.wrappedLabel("—", "info-value");
        Label catalogInfo = UiFactory.wrappedLabel("Seleziona un punto sulla mappa.", "sky-detail-note");
        Button open = UiFactory.button("Apri curve di luce →", "primary-button");
        open.setMaxWidth(Double.MAX_VALUE);
        open.setDisable(true);
        final SkyBurst[] current = new SkyBurst[1];

        VBox rows = new VBox(10,
                detailRow("Trigger", trigger),
                detailRow("RA (J2000)", ra),
                detailRow("DEC (J2000)", dec),
                detailRow("T90", t90),
                detailRow("Classe descrittiva", clazz),
                detailRow("Redshift", redshift));
        Label note = UiFactory.wrappedLabel(
                "Seleziona un GRB direttamente nella vista a schermo intero: i dettagli restano visibili qui e puoi aprire subito le relative curve di luce.",
                "sky-science-note");
        VBox panel = new VBox(14,
                UiFactory.label("GRB selezionato", "card-subtitle"),
                name, rows, catalogInfo, open,
                UiFactory.label("Vista interattiva", "card-title"), note);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(18));
        panel.setMinWidth(300);
        panel.setPrefWidth(330);
        panel.setMaxWidth(360);
        panel.setMinHeight(0);

        Consumer<SkyBurst> updater = burst -> {
            current[0] = burst;
            if (burst == null) {
                name.setText("Nessun GRB selezionato");
                trigger.setText("—");
                ra.setText("—");
                dec.setText("—");
                t90.setText("—");
                clazz.setText("—");
                redshift.setText("—");
                catalogInfo.setText("Seleziona un punto sulla mappa.");
                open.setDisable(true);
                return;
            }
            name.setText(burst.grbName());
            trigger.setText(burst.triggerId().isBlank() ? "n.d." : burst.triggerId());
            ra.setText(String.format(Locale.ITALY, "%.5f°", burst.raDeg())
                    + "  ·  " + SkyCoordinates.raToHms(burst.raDeg()));
            dec.setText(String.format(Locale.ITALY, "%+.5f°", burst.decDeg())
                    + "  ·  " + SkyCoordinates.decToDms(burst.decDeg()));
            t90.setText(burst.formattedT90());
            clazz.setText(burst.durationClass());
            redshift.setText(burst.redshift().detail());
            CatalogEntry entry = baseCatalog.get(burst.grbName().toUpperCase(Locale.ROOT));
            if (entry != null) {
                catalogInfo.setText("Evento presente nel catalogo Swift/BAT: puoi aprire direttamente curve, FITS e metadati.");
                open.setDisable(false);
            } else {
                catalogInfo.setText("Coordinate disponibili, ma l'evento non è presente nel catalogo BAT caricato dall'Explorer.");
                open.setDisable(true);
            }
        };
        open.setOnAction(event -> {
            SkyBurst burst = current[0];
            if (burst == null) return;
            CatalogEntry entry = baseCatalog.get(burst.grbName().toUpperCase(Locale.ROOT));
            if (entry != null) {
                InPlaceFullscreen.close(open);
                openGrb.accept(entry);
            }
        });
        updater.accept(selectedBurst);
        return new FullscreenDetails(panel, updater);
    }
'''
rep(SKY, old_fullscreen, new_fullscreen, 'fullscreen mappa con dettagli')

old_config = '''    private void configureFilters() {
        search.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.length() < 2 || newValue.length() > oldValue.length()) {
                applyFilters();
            }
        });
    }
'''
new_config = '''    private void configureFilters() {
        filterDebounce.setOnFinished(event -> applyFilters());
        search.textProperty().addListener((obs, oldValue, newValue) -> scheduleFilterApply());
        durationFilter.valueProperty().addListener((obs, oldValue, newValue) -> scheduleFilterApply());
        redshiftFilter.valueProperty().addListener((obs, oldValue, newValue) -> scheduleFilterApply());
        for (TextField field : List.of(raMin, raMax, decMin, decMax, zMin, zMax)) {
            field.textProperty().addListener((obs, oldValue, newValue) -> scheduleFilterApply());
        }
    }

    private void scheduleFilterApply() {
        filterDebounce.stop();
        filterDebounce.playFromStart();
    }
'''
rep(SKY, old_config, new_config, 'auto applicazione filtri mappa')

rep(SKY,
    '''        galacticPlane.setSelected(true);
        applyFilters();''',
    '''        galacticPlane.setSelected(true);
        filterDebounce.stop();
        applyFilters();''',
    'reset debounce mappa')

rep(SKY,
    '''    private record Range(double raMin, double raMax, double decMin, double decMax) {''',
    '''    private record FullscreenDetails(VBox node, Consumer<SkyBurst> update) {
    }

    private record Range(double raMin, double raMax, double decMin, double decMax) {''',
    'record dettagli fullscreen')

# ---------------------------------------------------------------------------
# Esplora: applicazione filtri automatica con debounce di ~1 secondo.
# ---------------------------------------------------------------------------
rep(EXP,
    'import javafx.application.HostServices;\n',
    'import javafx.animation.PauseTransition;\nimport javafx.application.HostServices;\n',
    'import PauseTransition esplora')
rep(EXP,
    'import javafx.stage.FileChooser;\n',
    'import javafx.stage.FileChooser;\nimport javafx.util.Duration;\n',
    'import Duration esplora')
rep(EXP,
    '    private GrbData currentData;\n',
    '    private GrbData currentData;\n    private final PauseTransition catalogFilterDebounce = new PauseTransition(Duration.millis(900));\n',
    'debounce esplora')

old_wire = '''    private void wireCatalog() {
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
'''
new_wire = '''    private void wireCatalog() {
        catalogFilterDebounce.setOnFinished(event -> applyCatalogFilters());
        catalogSearch.textProperty().addListener((observable, oldValue, newValue) -> scheduleCatalogFilters());
        durationFilter.valueProperty().addListener((obs, oldValue, newValue) -> scheduleCatalogFilters());
        redshiftFilter.valueProperty().addListener((obs, oldValue, newValue) -> scheduleCatalogFilters());
        cacheFilter.valueProperty().addListener((obs, oldValue, newValue) -> scheduleCatalogFilters());
        for (TextField field : List.of(t90MinFilter, t90MaxFilter, redshiftMinFilter, redshiftMaxFilter)) {
            field.textProperty().addListener((obs, oldValue, newValue) -> scheduleCatalogFilters());
        }
        catalogList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != selectedEntry) {
                selectedEntry = newValue;
                loadRequest.accept(newValue, false);
            }
        });
    }

    private void scheduleCatalogFilters() {
        catalogFilterDebounce.stop();
        catalogFilterDebounce.playFromStart();
    }
'''
rep(EXP, old_wire, new_wire, 'debounce filtri esplora')

# ---------------------------------------------------------------------------
# Analisi di popolazione: i filtri avanzati sono solo le tre card, senza
# contenitore vuoto esteso fino al bordo destro.
# ---------------------------------------------------------------------------
old_advanced = '''        FlowPane advancedContent = new FlowPane(10, 7);
        advancedContent.getStyleClass().addAll("population-filter-grid", "advanced-filter-row");
        advancedContent.getChildren().addAll(
                filterGroup("Intervallo redshift z", "Applicato ai GRB con z", range(zMin, zMax), 205),
                filterGroup("Ascensione retta RA", "Intervallo 0°–360°", range(raMin, raMax), 205),
                filterGroup("Declinazione DEC", "Intervallo −90°–+90°", range(decMin, decMax), 205));
        advancedContent.setVisible(false);
        advancedContent.setManaged(false);'''
new_advanced = '''        HBox advancedContent = new HBox(10);
        advancedContent.setAlignment(Pos.TOP_LEFT);
        advancedContent.getChildren().addAll(
                filterGroup("Intervallo redshift z", "Applicato ai GRB con z", range(zMin, zMax), 205),
                filterGroup("Ascensione retta RA", "Intervallo 0°–360°", range(raMin, raMax), 205),
                filterGroup("Declinazione DEC", "Intervallo −90°–+90°", range(decMin, decMax), 205));
        advancedContent.setMaxWidth(635);
        advancedContent.setVisible(false);
        advancedContent.setManaged(false);'''
rep(POP, old_advanced, new_advanced, 'filtri avanzati popolazione compatti')

# ---------------------------------------------------------------------------
# InPlaceFullscreen: consente ai controlli interni di chiudere la modalità
# fullscreen prima di navigare verso un'altra pagina.
# ---------------------------------------------------------------------------
rep(FULL,
    '''    public static void show(Node owner, String title, Node content) {
        if (owner == null || content == null || owner.getScene() == null
                || !(owner.getScene().getWindow() instanceof Stage stage)) {
            return;
        }
        Scene scene = owner.getScene();
        if (scene.getProperties().containsKey(ACTIVE_SESSION)) {
            return;
        }
        new Session(scene, stage, owner, title, content).open();
    }
''',
    '''    public static void show(Node owner, String title, Node content) {
        if (owner == null || content == null || owner.getScene() == null
                || !(owner.getScene().getWindow() instanceof Stage stage)) {
            return;
        }
        Scene scene = owner.getScene();
        if (scene.getProperties().containsKey(ACTIVE_SESSION)) {
            return;
        }
        new Session(scene, stage, owner, title, content).open();
    }

    public static void close(Node owner) {
        if (owner == null || owner.getScene() == null) {
            return;
        }
        Object activeSession = owner.getScene().getProperties().get(ACTIVE_SESSION);
        if (activeSession instanceof Session session) {
            session.close(false);
        }
    }
''',
    'chiusura fullscreen pubblica')

print('Rifiniture filtri e fullscreen applicate.')
