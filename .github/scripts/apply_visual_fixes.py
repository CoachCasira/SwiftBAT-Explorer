from pathlib import Path

POP = Path('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java')
EXP = Path('src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java')
TD = Path('src/main/java/it/casiraghi/swiftbat/ui/components/ThreeDChartPane.java')
SKY = Path('src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java')
CSS = Path('src/main/resources/app.css')

def rep(path, old, new, label):
    text = path.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Patch non applicabile: {label}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

# FRACEXP: usa ScrollBar come cursore singolo, senza track Slider che sborda.
rep(POP, 'import javafx.scene.control.ScrollPane;\nimport javafx.scene.control.Slider;\n',
         'import javafx.scene.control.ScrollBar;\nimport javafx.scene.control.ScrollPane;\n', 'import ScrollBar')
rep(POP, 'private final Slider exposureMinSlider = exposureSlider(0);',
         'private final ScrollBar exposureMinSlider = exposureSlider(0);', 'min ScrollBar')
rep(POP, 'private final Slider exposureMaxSlider = exposureSlider(100);',
         'private final ScrollBar exposureMaxSlider = exposureSlider(100);', 'max ScrollBar')
rep(POP, 'private void configureExposureControl(Slider slider, TextField field, boolean minimum,',
         'private void configureExposureControl(ScrollBar slider, TextField field, boolean minimum,', 'configure ScrollBar')
rep(POP, '''    private static Slider exposureSlider(double value) {
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
    }''', '''    private static ScrollBar exposureSlider(double value) {
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
    }''', 'factory ScrollBar')
rep(POP, 'private VBox exposureControl(String label, Slider slider, TextField field, boolean minimum) {',
         'private VBox exposureControl(String label, ScrollBar slider, TextField field, boolean minimum) {', 'exposureControl ScrollBar')

# Explorer 2D: pulsanti nel vuoto sotto Trigger; fullscreen con stile arancio/viola.
rep(EXP, '''        Button export = UiFactory.button("Esporta PNG", "ghost-button");
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
                plainConceptCard("Trigger", "Il punto zero dell'allerta", "Tempi negativi: prima del trigger. Tempi positivi: dopo il trigger. Il trigger non coincide necessariamente con l'inizio fisico esatto del burst."));''', '''        Button export = UiFactory.button("Esporta PNG", "ghost-button");
        export.setOnAction(event -> exportNode(chart, data.grbName() + "_curva_1s.png"));
        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");
        fullscreen.setOnAction(event -> openOverviewFullscreen(
                data, channelChoice.getValue(), windowChoice.getValue(), smooth.isSelected()));
        chartCard.getChildren().addAll(controls, chart);
        pane.setCenter(chartCard);

        HBox graphActions = new HBox(8, export, fullscreen);
        graphActions.setAlignment(Pos.CENTER_RIGHT);
        VBox actionCard = new VBox(7,
                UiFactory.label("Azioni grafico", "card-subtitle"), graphActions);
        actionCard.getStyleClass().addAll("card", "overview-action-card");
        actionCard.setPadding(new Insets(12));

        VBox right = new VBox(10);
        right.setPrefWidth(310);
        right.getChildren().addAll(
                summaryCard(data, "In breve", List.of("BIN_SIZE", "ENERGY_RANGE", "TIME_RANGE", "ASCII_ROWS", "FITS_ROWS")),
                plainConceptCard("Trigger", "Il punto zero dell'allerta", "Tempi negativi: prima del trigger. Tempi positivi: dopo il trigger. Il trigger non coincide necessariamente con l'inizio fisico esatto del burst."),
                actionCard);''', 'azioni 2D a destra')
rep(EXP, '        box.setPadding(new Insets(18));\n        if (data.asciiData().isEmpty()) {',
         '        box.setPadding(new Insets(10, 12, 10, 12));\n        if (data.asciiData().isEmpty()) {', 'padding 3D')

# 3D: riduci altezza minima così il footer resta sempre raggiungibile.
rep(TD, '''        setMinHeight(560);
        setPrefHeight(650);''', '''        setMinHeight(430);
        setPrefHeight(500);''', 'altezza ThreeD pane')
rep(TD, '''        viewer.setMinHeight(500);
        viewer.setPrefHeight(560);''', '''        viewer.setMinHeight(330);
        viewer.setPrefHeight(390);''', 'altezza viewer 3D')
rep(TD, '        header.setPadding(new Insets(15, 17, 13, 17));',
         '        header.setPadding(new Insets(10, 14, 9, 14));', 'header 3D compatto')
rep(TD, '        footer.getStyleClass().add("three-d-footer");',
         '        footer.getStyleClass().add("three-d-footer");\n        footer.setMinHeight(50);', 'footer minimo')

# Mappa: porta contenuto e mappa più in alto e dentro la viewport.
rep(SKY, '''        VBox page = new VBox(18);
        page.setPadding(new Insets(30, 34, 36, 34));''', '''        VBox page = new VBox(12);
        page.setPadding(new Insets(20, 26, 24, 26));''', 'pagina mappa compatta')
rep(SKY, '''        mapHost.setMinHeight(500);
        mapHost.setPrefHeight(650);''', '''        mapHost.setMinHeight(390);
        mapHost.setPrefHeight(500);''', 'altezza mappa')
rep(SKY, '        VBox mapCard = new VBox(12);', '        VBox mapCard = new VBox(8);', 'spacing map card')
rep(SKY, '        mapCard.setPadding(new Insets(15));', '        mapCard.setPadding(new Insets(12));', 'padding map card')
rep(SKY, '''            mapHost.getChildren().setAll(newToggle == sphereButton ? sphere : mollweide);
        });''', '''            boolean showSphere = newToggle == sphereButton;
            mapHost.getChildren().setAll(showSphere ? sphere : mollweide);
            javafx.application.Platform.runLater(() -> {
                if (showSphere) sphere.resetView(); else mollweide.resetView();
            });
        });''', 'centra al cambio vista')

# CSS: cursore FRACEXP dedicato e controlli grafico.
css = CSS.read_text(encoding='utf-8')
css += '''

/* ---------- Refinements 1.2.0 ---------- */
.fracexp-slider {
    -fx-background-color: transparent;
    -fx-padding: 0 2 0 2;
}
.fracexp-slider .track {
    -fx-background-color: rgba(255,255,255,0.10);
    -fx-background-radius: 999px;
    -fx-pref-height: 6px;
}
.fracexp-slider .thumb {
    -fx-background-color: linear-gradient(to bottom right, #ffb15c, #a866dc);
    -fx-background-radius: 999px;
    -fx-pref-width: 16px;
    -fx-pref-height: 16px;
    -fx-effect: dropshadow(gaussian, rgba(255,177,92,0.24), 8, 0.18, 0, 1);
}
.fracexp-slider .increment-button,
.fracexp-slider .decrement-button,
.fracexp-slider .increment-arrow,
.fracexp-slider .decrement-arrow {
    -fx-background-color: transparent;
    -fx-padding: 0;
    -fx-shape: "";
    -fx-pref-width: 0;
    -fx-pref-height: 0;
}
.overview-action-card {
    -fx-background-color: rgba(255,255,255,0.025);
    -fx-border-color: rgba(255,177,92,0.10);
}
.three-d-footer {
    -fx-padding: 9px 12px;
}
'''
CSS.write_text(css, encoding='utf-8')
print('Correzioni visuali applicate.')
