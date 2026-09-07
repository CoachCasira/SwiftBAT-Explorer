from pathlib import Path

POP = Path('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java')
SKY = Path('src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java')
CSS = Path('src/main/resources/app.css')


def rep(path, old, new, label):
    text = path.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Patch non applicabile: {label}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

# ---------------------------------------------------------------------------
# Analisi di popolazione: porta FRACEXP nello spazio a destra dei filtri base.
# ---------------------------------------------------------------------------
rep(POP,
'''        FlowPane primary = new FlowPane(10, 7);
        primary.getStyleClass().add("population-filter-grid");
        primary.getChildren().addAll(
                filterGroup("Durata T90", "Classe temporale", duration, 200),
                filterGroup("Redshift", "Disponibilità della misura z", redshiftAvailability, 200),
                filterGroup("Finestra temporale", "Secondi attorno al trigger", window, 160),
                filterGroup("Campione massimo", "GRB più recenti dopo i filtri", limit, 160));''',
'''        FlowPane primary = new FlowPane(9, 7);
        primary.getStyleClass().add("population-filter-grid");
        primary.getChildren().addAll(
                filterGroup("Durata T90", "Classe temporale", duration, 185),
                filterGroup("Redshift", "Disponibilità della misura z", redshiftAvailability, 190),
                filterGroup("Finestra temporale", "Secondi attorno al trigger", window, 150),
                filterGroup("Campione massimo", "GRB più recenti dopo i filtri", limit, 150));
        primary.setMinWidth(650);
        primary.setPrefWrapLength(690);''',
'filtri primari più compatti')

rep(POP,
'''        VBox minimum = exposureControl("Minimo ammesso", exposureMinSlider, exposureMin, true);
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
        HBox.setHgrow(exposureControls, Priority.ALWAYS);''',
'''        VBox minimum = exposureControl("Minimo ammesso", exposureMinSlider, exposureMin, true);
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
        HBox.setHgrow(exposureBox, Priority.ALWAYS);''',
'FRACEXP laterale')

rep(POP,
'''        VBox card = new VBox(9, primary, advanced, advancedContent, exposureBox, footer);''',
'''        VBox card = new VBox(9, topFilters, advanced, advancedContent, footer);''',
'ordine filtri popolazione')

rep(POP,
'''        box.setMinWidth(260);
        box.setPrefWidth(340);
        box.setMaxWidth(Double.MAX_VALUE);''',
'''        box.setMinWidth(185);
        box.setPrefWidth(215);
        box.setMaxWidth(Double.MAX_VALUE);''',
'card FRACEXP compatte')

# ---------------------------------------------------------------------------
# Mappa celeste: metriche a tutta larghezza, filtri chiari, fullscreen 2D/3D.
# ---------------------------------------------------------------------------
rep(SKY,
'''    private SkyBurst selectedBurst;

    private final MollweideSkyPane mollweide = new MollweideSkyPane();''',
'''    private SkyBurst selectedBurst;
    private boolean sphereView;

    private final MollweideSkyPane mollweide = new MollweideSkyPane();''',
'stato vista mappa')

rep(SKY,
'''        FlowPane metrics = new FlowPane(12, 12);
        metrics.getChildren().addAll(
                skyMetric("VISIBILI", shownMetric, "dopo i filtri"),
                skyMetric("SHORT", shortMetric, "T90 ≤ 2 s"),
                skyMetric("LONG", longMetric, "T90 > 2 s"),
                skyMetric("SENZA T90", noT90Metric, "durata non disponibile"));''',
'''        HBox metrics = new HBox(12);
        metrics.getStyleClass().add("sky-metric-row");
        VBox visibleMetric = skyMetric("VISIBILI", shownMetric, "dopo i filtri");
        VBox shortMetricCard = skyMetric("SHORT", shortMetric, "T90 ≤ 2 s");
        VBox longMetricCard = skyMetric("LONG", longMetric, "T90 > 2 s");
        VBox unknownMetricCard = skyMetric("SENZA T90", noT90Metric, "durata non disponibile");
        for (VBox metric : List.of(visibleMetric, shortMetricCard, longMetricCard, unknownMetricCard)) {
            metric.setMinWidth(0);
            metric.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(metric, Priority.ALWAYS);
        }
        metrics.getChildren().addAll(visibleMetric, shortMetricCard, longMetricCard, unknownMetricCard);''',
'metriche larghezza completa')

rep(SKY,
'''        HBox mapHead = new HBox(12,
                UiFactory.label("Cielo", "card-title"),
                UiFactory.spacer(),
                resetView,
                viewSwitch);''',
'''        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "secondary-button");
        fullscreen.setOnAction(event -> openMapFullscreen());
        HBox mapHead = new HBox(10,
                UiFactory.label("Cielo", "card-title"),
                resetView,
                fullscreen,
                UiFactory.spacer(),
                viewSwitch);''',
'header mappa con fullscreen')

rep(SKY,
'''        ToggleButton advanced = new ToggleButton("RA / DEC");
        advanced.getStyleClass().add("sky-toggle");''',
'''        ToggleButton advanced = new ToggleButton("Filtri avanzati  ▾");
        advanced.getStyleClass().add("sky-toggle");''',
'bottone filtri avanzati')

rep(SKY,
'''        firstRow.getChildren().addAll(search, durationFilter, redshiftFilter, galacticPlane,
                UiFactory.spacer(), advanced, apply, reset);''',
'''        firstRow.getChildren().addAll(search, durationFilter, redshiftFilter, galacticPlane,
                UiFactory.spacer(), apply, reset);

        HBox advancedHeader = new HBox(8, advanced,
                UiFactory.label("RA, DEC e intervallo di redshift", "sky-filter-help"));
        advancedHeader.setAlignment(Pos.CENTER_LEFT);''',
'layout filtri base')

rep(SKY,
'''        advanced.selectedProperty().addListener((obs, oldValue, selected) -> {
            rangeRow.setVisible(selected);
            rangeRow.setManaged(selected);
        });

        card.getChildren().addAll(firstRow, rangeRow);''',
'''        advanced.selectedProperty().addListener((obs, oldValue, selected) -> {
            rangeRow.setVisible(selected);
            rangeRow.setManaged(selected);
            advanced.setText(selected ? "Nascondi filtri avanzati  ▴" : "Filtri avanzati  ▾");
        });

        card.getChildren().addAll(firstRow, advancedHeader, rangeRow);''',
'filtri avanzati espliciti')

rep(SKY,
'''            boolean showSphere = newToggle == sphereButton;
            mapHost.getChildren().setAll(showSphere ? sphere : mollweide);
            javafx.application.Platform.runLater(() -> {
                if (showSphere) sphere.resetView(); else mollweide.resetView();
            });''',
'''            boolean showSphere = newToggle == sphereButton;
            sphereView = showSphere;
            mapHost.getChildren().setAll(showSphere ? sphere : mollweide);
            javafx.application.Platform.runLater(() -> {
                if (showSphere) sphere.resetView(); else mollweide.resetView();
            });''',
'salva vista attiva')

rep(SKY,
'''    private VBox buildDetailsPanel() {
        VBox details = new VBox(14);''',
'''    private void openMapFullscreen() {
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

    private VBox buildDetailsPanel() {
        VBox details = new VBox(14);''',
'metodo fullscreen mappa')

rep(SKY,
'''        Label scientificNote = UiFactory.wrappedLabel(
                "La soglia a 2 s è mostrata soltanto come riferimento descrittivo tradizionale. La mappa non assegna da sola una classificazione scientifica definitiva.",
                "sky-science-note");''',
'''        Label scientificNote = UiFactory.wrappedLabel(
                "La soglia a 2 s è mostrata soltanto come riferimento descrittivo tradizionale. La mappa non assegna da sola una classificazione scientifica definitiva.\n\n"
                        + "Seleziona un punto per leggere coordinate, T90, classe descrittiva e redshift. Le viste Mollweide 2D e Sfera 3D rappresentano lo stesso campione: cambia soltanto il modo in cui la distribuzione celeste viene esplorata.",
                "sky-science-note");
        scientificNote.setMaxWidth(Double.MAX_VALUE);
        scientificNote.setMaxHeight(Double.MAX_VALUE);
        scientificNote.setPrefHeight(155);''',
'nota scientifica ampliata')

rep(SKY,
'''        VBox box = new VBox(4,
                UiFactory.label(title, "sky-metric-label"),
                value,
                UiFactory.label(detail, "sky-metric-detail"));
        box.getStyleClass().add("sky-metric-card");
        box.setPrefWidth(190);
        return box;''',
'''        VBox box = new VBox(4,
                UiFactory.label(title, "sky-metric-label"),
                value,
                UiFactory.label(detail, "sky-metric-detail"));
        box.getStyleClass().add("sky-metric-card");
        box.setPrefWidth(230);
        return box;''',
'metriche più larghe')

# CSS minime rifiniture.
css = CSS.read_text(encoding='utf-8')
css += '''

/* ---------- Layout refinements population + sky ---------- */
.population-filter-side {
    -fx-padding: 9px 10px;
}
.population-filter-side .percentage-control {
    -fx-padding: 7px 8px;
}
.sky-metric-row {
    -fx-alignment: center-left;
}
.sky-metric-row .sky-metric-card {
    -fx-padding: 13px 18px;
}
'''
CSS.write_text(css, encoding='utf-8')

print('Rifiniture layout applicate.')
