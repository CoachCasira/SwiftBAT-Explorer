from pathlib import Path

ROOT = Path('.')

def patch(path, old, new, count=1):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Pattern not found in {path}: {old[:120]!r}')
    text2 = text.replace(old, new, count)
    p.write_text(text2, encoding='utf-8')

# -----------------------------------------------------------------------------
# ComparePage: guard against recursive ComboBox ActionEvent -> StackOverflow,
# show only two suggestions, and add a stable loading state before chart rebuild.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/ComparePage.java'
patch(path,
'''import javafx.scene.control.Label;\nimport javafx.scene.control.TextFormatter;''',
'''import javafx.scene.control.Label;\nimport javafx.scene.control.ProgressIndicator;\nimport javafx.scene.control.TextFormatter;''')
patch(path,
'''    private static final int MAX_SUGGESTIONS = 15;\n    private static final int VISIBLE_SUGGESTIONS = 4;\n    private static final String GHOST_KEY = ComparePage.class.getName() + ".ghostSuggestion";''',
'''    private static final int MAX_SUGGESTIONS = 15;\n    private static final int VISIBLE_SUGGESTIONS = 2;\n    private static final String GHOST_KEY = ComparePage.class.getName() + ".ghostSuggestion";\n    private static final String COMMITTING_KEY = ComparePage.class.getName() + ".committing";''')
patch(path,
'''    private final Set<String> requestedLoads = new LinkedHashSet<>();\n    private List<String> availableNames = List.of();''',
'''    private final Set<String> requestedLoads = new LinkedHashSet<>();\n    private List<String> availableNames = List.of();\n    private int comparisonVersion;''')
patch(path,
'''        combo.setOnAction(event -> {\n            String value = combo.getValue();\n            if (value != null && availableNames.contains(value)) commitSelection(combo, value);\n        });''',
'''        combo.setOnAction(event -> {\n            if (Boolean.TRUE.equals(combo.getProperties().get(COMMITTING_KEY))) return;\n            String value = combo.getValue();\n            if (value != null && availableNames.contains(value)) commitSelection(combo, value);\n        });''')
patch(path,
'''        if (matches.size() <= MAX_SUGGESTIONS && !matches.isEmpty() && normalized.length() > 3) {\n            combo.setItems(FXCollections.observableArrayList(matches));\n            if (combo.getEditor().isFocused() && !combo.isShowing()) Platform.runLater(combo::show);\n        } else {\n            combo.hide();\n            combo.setItems(FXCollections.observableArrayList());\n        }''',
'''        if (!combo.getEditor().isFocused()) {\n            combo.hide();\n            return;\n        }\n        if (matches.size() <= MAX_SUGGESTIONS && !matches.isEmpty() && normalized.length() > 3) {\n            List<String> visible = matches.stream().limit(VISIBLE_SUGGESTIONS).toList();\n            combo.setItems(FXCollections.observableArrayList(visible));\n            if (!combo.isShowing()) Platform.runLater(combo::show);\n        } else {\n            combo.hide();\n            combo.setItems(FXCollections.observableArrayList());\n        }''')
patch(path,
'''    private void commitSelection(ComboBox<String> combo, String value) {\n        if (value == null || !availableNames.contains(value)) return;\n        combo.setValue(value);\n        combo.getEditor().setText(value);\n        combo.getEditor().positionCaret(value.length());\n        combo.hide();\n        combo.getProperties().put(GHOST_KEY, "");\n        ensureLoaded(value);\n        refreshChoices();\n        refreshComparison();\n    }''',
'''    private void commitSelection(ComboBox<String> combo, String value) {\n        if (value == null || !availableNames.contains(value)\n                || Boolean.TRUE.equals(combo.getProperties().get(COMMITTING_KEY))) return;\n        combo.getProperties().put(COMMITTING_KEY, Boolean.TRUE);\n        try {\n            combo.setValue(value);\n            combo.getEditor().setText(value);\n            combo.getEditor().positionCaret(value.length());\n            combo.hide();\n            combo.getProperties().put(GHOST_KEY, "");\n        } finally {\n            combo.getProperties().remove(COMMITTING_KEY);\n        }\n        ensureLoaded(value);\n        refreshComparison();\n    }''')
patch(path,
'''    private void refreshComparison() {\n        String aName = selected(first);\n        String bName = selected(second);\n        if (aName != null) ensureLoaded(aName);\n        if (bName != null) ensureLoaded(bName);\n        if (aName != null && aName.equals(bName)) {\n            showEmpty("Scegli due GRB diversi", "Choose two different GRBs");\n            return;\n        }\n        if (aName == null || bName == null) {\n            showEmpty("Apri o seleziona almeno due GRB", "Open or select at least two GRBs");\n            return;\n        }\n        GrbData a = sessionData.get(aName);\n        GrbData b = sessionData.get(bName);\n        if (a == null || b == null) {\n            showEmpty("Caricamento dei GRB selezionati…", "Loading selected GRBs…");\n            return;\n        }\n        content.getChildren().setAll(buildComparison(a, b));\n        I18n.localizeTree(content);\n    }''',
'''    private void refreshComparison() {\n        int version = ++comparisonVersion;\n        String aName = selected(first);\n        String bName = selected(second);\n        if (aName != null) ensureLoaded(aName);\n        if (bName != null) ensureLoaded(bName);\n        if (aName != null && aName.equals(bName)) {\n            showEmpty("Scegli due GRB diversi", "Choose two different GRBs");\n            return;\n        }\n        if (aName == null || bName == null) {\n            showEmpty("Apri o seleziona almeno due GRB", "Open or select at least two GRBs");\n            return;\n        }\n        GrbData a = sessionData.get(aName);\n        GrbData b = sessionData.get(bName);\n        if (a == null || b == null) {\n            showComparisonLoading(aName, bName, true);\n            return;\n        }\n        showComparisonLoading(aName, bName, false);\n        Platform.runLater(() -> {\n            if (version != comparisonVersion) return;\n            if (!aName.equals(selected(first)) || !bName.equals(selected(second))) return;\n            GrbData currentA = sessionData.get(aName);\n            GrbData currentB = sessionData.get(bName);\n            if (currentA == null || currentB == null) return;\n            content.getChildren().setAll(buildComparison(currentA, currentB));\n            I18n.localizeTree(content);\n        });\n    }\n\n    private void showComparisonLoading(String aName, String bName, boolean downloading) {\n        VBox loading = new VBox(12);\n        loading.getStyleClass().add("loading-state");\n        loading.setAlignment(Pos.CENTER);\n        ProgressIndicator spinner = new ProgressIndicator();\n        spinner.setMaxSize(48, 48);\n        Label title = UiFactory.label("", "loading-title");\n        I18n.setText(title, downloading ? "Caricamento confronto…" : "Preparazione confronto…",\n                downloading ? "Loading comparison…" : "Preparing comparison…");\n        Label detail = UiFactory.label("", "loading-detail");\n        I18n.setText(detail, aName + " · " + bName, aName + " · " + bName);\n        loading.getChildren().addAll(spinner, title, detail);\n        content.getChildren().setAll(loading);\n    }''')
patch(path,
'''    private VBox compareMetric(String title, String a, String b, String nameA, String nameB) {\n        VBox card = new VBox(8);\n        card.getStyleClass().add("metric-card");\n        card.setMinWidth(230);\n        card.getChildren().addAll(\n                UiFactory.label(title, "metric-eyebrow"),\n                UiFactory.label(nameA + "  " + a, "compare-value-a"),\n                UiFactory.label(nameB + "  " + b, "compare-value-b"));\n        return card;\n    }''',
'''    private VBox compareMetric(String title, String a, String b, String nameA, String nameB) {\n        VBox card = new VBox(8);\n        card.getStyleClass().add("metric-card");\n        card.setMinWidth(230);\n        Label titleLabel = UiFactory.label("", "metric-eyebrow");\n        I18n.setText(titleLabel, title, I18n.english(title));\n        Label valueA = UiFactory.label("", "compare-value-a");\n        Label valueB = UiFactory.label("", "compare-value-b");\n        I18n.setText(valueA, nameA + "  " + a, nameA + "  " + I18n.english(a));\n        I18n.setText(valueB, nameB + "  " + b, nameB + "  " + I18n.english(b));\n        card.getChildren().addAll(titleLabel, valueA, valueB);\n        return card;\n    }''')

# -----------------------------------------------------------------------------
# PopulationPage: compact single-row filters and localize runtime-generated text.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java'
patch(path,
'''        exposureBox.setMinWidth(300);\n        exposureBox.setPrefWidth(350);\n        exposureBox.setMaxWidth(Double.MAX_VALUE);\n\n        VBox durationGroup = filterGroup("Durata T90", "", duration, 170);\n        VBox redshiftGroup = filterGroup("Redshift", "", redshiftControl, 220);\n        VBox windowGroup = filterGroup("Finestra temporale", "", windowControl, 215);\n        VBox limitGroup = filterGroup("Campione massimo", "", limit, 135);\n        HBox topFilters = new HBox(8, durationGroup, redshiftGroup, windowGroup, exposureBox, limitGroup);\n        topFilters.setAlignment(Pos.TOP_LEFT);\n        HBox.setHgrow(exposureBox, Priority.ALWAYS);''',
'''        exposureBox.setMinWidth(270);\n        exposureBox.setPrefWidth(285);\n        exposureBox.setMaxWidth(300);\n\n        VBox durationGroup = filterGroup("Durata T90", "", duration, 155);\n        VBox redshiftGroup = filterGroup("Redshift", "", redshiftControl, 205);\n        VBox windowGroup = filterGroup("Finestra temporale", "", windowControl, 195);\n        VBox limitGroup = filterGroup("Campione massimo", "", limit, 125);\n        HBox topFilters = new HBox(7, durationGroup, redshiftGroup, windowGroup, exposureBox, limitGroup);\n        topFilters.setAlignment(Pos.TOP_LEFT);\n        HBox.setHgrow(exposureBox, Priority.NEVER);''')
patch(path,
'''                        updateMessage("Caricamento parallelo · " + completed + "/" + selected.size());''',
'''                        updateMessage(I18n.dynamic("Caricamento parallelo · " + completed + "/" + selected.size(),\n                                "Parallel loading · " + completed + "/" + selected.size()));''')
patch(path,
'''        insightHeadline.setText(narrative.headline());''',
'''        I18n.setText(insightHeadline, narrative.headline(), I18n.english(narrative.headline()));''')
patch(path,
'''        Label copy = expanded\n                ? UiFactory.wrappedLabel(text, "population-insight-text")\n                : UiFactory.label(text, "population-insight-text");''',
'''        Label copy = expanded\n                ? UiFactory.wrappedLabel("", "population-insight-text")\n                : UiFactory.label("", "population-insight-text");\n        I18n.setText(copy, text, I18n.english(text));''')
patch(path,
'''        curveChart.setTitle("Nessuna analisi eseguita");''',
'''        curveChart.setTitle(I18n.t("Nessuna analisi eseguita"));''', count=2)
patch(path,
'''        curveChart.setTitle(result.curves().size() + " curve normalizzate e allineate a t = 0");''',
'''        curveChart.setTitle(I18n.t(result.curves().size() + " curve normalizzate e allineate a t = 0"));''')
patch(path,
'''    private void setStatus(String text, String style) {\n        status.setText(text);\n        status.getStyleClass().removeAll("status-neutral", "status-online", "status-warning");''',
'''    private void setStatus(String text, String style) {\n        I18n.setText(status, text, I18n.english(text));\n        status.getStyleClass().removeAll("status-neutral", "status-online", "status-warning");''')
patch(path,
'''            candidatePreview.setText("Attendo catalogo e metadati scientifici…");''',
'''            I18n.setText(candidatePreview, "Attendo catalogo e metadati scientifici…",\n                    "Waiting for catalog and scientific metadata…");''')
patch(path,
'''            candidatePreview.setText(matches.size() + " GRB corrispondono ai filtri preliminari · "\n                    + selected + " saranno esaminati · " + inMemory + " già in RAM");''',
'''            String italian = matches.size() + " GRB corrispondono ai filtri preliminari · "\n                    + selected + " saranno esaminati · " + inMemory + " già in RAM";\n            String english = matches.size() + " GRBs match the preliminary filters · "\n                    + selected + " will be examined · " + inMemory + " already in RAM";\n            I18n.setText(candidatePreview, italian, english);''')
patch(path,
'''            candidatePreview.setText(error.getMessage());''',
'''            I18n.setText(candidatePreview, error.getMessage(), I18n.english(error.getMessage()));''')

# -----------------------------------------------------------------------------
# Spectroscopy fullscreen time-energy: only pass the chart; standard fullscreen
# wrapper now owns the collapsible explanation, avoiding the delayed HBox resize.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java'
old = '''        VBox reading = new VBox(12);\n        reading.getStyleClass().addAll("card", "spectroscopy-assistant");\n        reading.setPadding(new Insets(18));\n        reading.setMinWidth(300);\n        reading.setPrefWidth(350);\n        reading.setMaxWidth(390);\n        reading.getChildren().addAll(\n                UiFactory.label("Come leggere la mappa", "card-title"),\n                fullscreenReading("Assi — X rappresenta il tempo rispetto al trigger t = 0; Y separa le quattro bande energetiche BAT."),\n                fullscreenReading("Colore — arancio indica un rate netto positivo, blu una fluttuazione negativa dopo la sottrazione del fondo; i toni scuri indicano valori vicini a zero."),\n                fullscreenReading("Dettaglio — spostando il mouse sulla mappa puoi leggere banda energetica, centro del bin, rate e larghezza della banda nel punto osservato."),\n                fullscreenReading("Scala temporale — ogni cella deriva dai rate ASCII a bin di 1 secondo e la finestra visualizzata è la stessa scelta nella scheda Spettroscopia."),\n                fullscreenReading("Da ricordare — questa mappa descrive i rate BAT nel tempo: non è un fit XSPEC e non converte direttamente i conteggi in flusso fisico."));\n\n        reading.setVisible(false);\n        reading.setManaged(false);\n        ToggleButton help = new ToggleButton(I18n.t("Mostra spiegazione"));\n        help.getStyleClass().addAll("ghost-button", "help-toggle");\n        help.selectedProperty().addListener((obs, oldValue, selected) -> {\n            reading.setVisible(selected);\n            reading.setManaged(selected);\n            help.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));\n        });\n        HBox helpBar = new HBox(UiFactory.spacer(), help);\n        helpBar.setAlignment(Pos.CENTER_RIGHT);\n\n        HBox body = new HBox(14, enlarged, reading);\n        body.setPadding(new Insets(0, 14, 12, 14));\n        body.setMinSize(0, 0);\n        body.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);\n        HBox.setHgrow(enlarged, Priority.ALWAYS);\n        VBox fullscreen = new VBox(8, helpBar, body);\n        fullscreen.setMinSize(0, 0);\n        fullscreen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);\n        VBox.setVgrow(body, Priority.ALWAYS);\n        InPlaceFullscreen.show(this, grbData.grbName() + " · Mappa tempo–energia dei rate", fullscreen);'''
new = '''        InPlaceFullscreen.show(this, grbData.grbName() + " · Mappa tempo–energia dei rate", enlarged);'''
patch(path, old, new)

# -----------------------------------------------------------------------------
# InPlaceFullscreen: stable reusable time-energy explanation and bilingual
# split sections. Dynamic fullscreen titles are stored as explicit IT/EN text.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/InPlaceFullscreen.java'
patch(path,
'''            Label heading = UiFactory.label(title == null || title.isBlank() ? "Schermo intero" : title,\n                    "fullscreen-title");''',
'''            String italianTitle = title == null || title.isBlank() ? "Schermo intero" : title;\n            Label heading = UiFactory.label("", "fullscreen-title");\n            I18n.setText(heading, italianTitle, I18n.english(italianTitle));''')
patch(path,
'''            if (normalized.contains("Mappa tempo–energia")) {\n                polishSpectroscopyAssistant(content, normalized);\n            }\n            return content;''',
'''            if (normalized.contains("Mappa tempo–energia")) {\n                return wrapTimeEnergy(content);\n            }\n            return content;''')
needle = '''        private Node wrapFlux3D(Node content) {\n            VBox reading = spectroscopyReadingCard(\n                    "Come leggere il flusso 3D",'''
if needle not in (ROOT/path).read_text(encoding='utf-8'):
    raise SystemExit('wrapFlux3D anchor not found')
# Insert wrapper immediately before responsiveReadingLayout.
p = ROOT/path
text = p.read_text(encoding='utf-8')
anchor = '''        private Node responsiveReadingLayout(Node content, VBox reading) {'''
insert = '''        private Node wrapTimeEnergy(Node content) {\n            VBox reading = spectroscopyReadingCard(\n                    "Come leggere la mappa",\n                    "Assi — X rappresenta il tempo rispetto al trigger t = 0; Y separa le quattro bande energetiche BAT.",\n                    "Colore — arancio indica un rate netto positivo, blu una fluttuazione negativa dopo la sottrazione del fondo; i toni scuri indicano valori vicini a zero.",\n                    "Dettaglio — spostando il mouse sulla mappa puoi leggere banda energetica, centro del bin, rate e larghezza della banda nel punto osservato.",\n                    "Scala temporale — ogni cella deriva dai rate ASCII a bin di 1 secondo e la finestra visualizzata è la stessa scelta nella scheda Spettroscopia.",\n                    "Confronto tra bande — leggendo verticalmente lo stesso istante puoi confrontare come il rate si distribuisce tra 15–25, 25–50, 50–100 e 100–350 keV. Le differenze di colore evidenziano variazioni relative del segnale tra i canali.",\n                    "Interpretazione — una zona arancione intensa individua un intervallo temporale in cui il rate netto è elevato in quella banda. Il confronto resta descrittivo: per ottenere un flusso fisico servono risposta strumentale e fit spettroscopico.",\n                    "Da ricordare — questa mappa descrive i rate BAT nel tempo: non è un fit XSPEC e non converte direttamente i conteggi in flusso fisico."\n            );\n            return responsiveReadingLayout(content, reading);\n        }\n\n'''
if anchor not in text:
    raise SystemExit('responsiveReadingLayout anchor missing')
text = text.replace(anchor, insert + anchor, 1)
p.write_text(text, encoding='utf-8')

patch(path,
'''        private VBox readingSection(String paragraph, int index) {\n            String heading = paragraph == null ? "" : paragraph;\n            String body = "";\n            int separator = heading.indexOf(" — ");\n            if (separator >= 0) {\n                body = heading.substring(separator + 3).trim();\n                heading = heading.substring(0, separator).trim();\n            }\n\n            Label sectionTitle = UiFactory.label(heading, "assistant-copy");''',
'''        private VBox readingSection(String paragraph, int index) {\n            String italian = paragraph == null ? "" : paragraph;\n            String english = I18n.english(italian);\n            String[] itParts = splitReadingParagraph(italian);\n            String[] enParts = splitReadingParagraph(english);\n\n            Label sectionTitle = UiFactory.label("", "assistant-copy");\n            I18n.setText(sectionTitle, itParts[0], enParts[0]);''')
patch(path,
'''            Label sectionBody = UiFactory.wrappedLabel(body, "assistant-copy");''',
'''            Label sectionBody = UiFactory.wrappedLabel("", "assistant-copy");\n            I18n.setText(sectionBody, itParts[1], enParts[1]);''')
# Insert helper before styleReadingTitle.
p = ROOT/path
text = p.read_text(encoding='utf-8')
anchor = '''        private void styleReadingTitle(Label label) {'''
helper = '''        private String[] splitReadingParagraph(String paragraph) {\n            String value = paragraph == null ? "" : paragraph;\n            int separator = value.indexOf(" — ");\n            if (separator < 0) return new String[]{value, ""};\n            return new String[]{value.substring(0, separator).trim(), value.substring(separator + 3).trim()};\n        }\n\n'''
if anchor not in text:
    raise SystemExit('styleReadingTitle anchor missing')
text = text.replace(anchor, helper + anchor, 1)
p.write_text(text, encoding='utf-8')

# -----------------------------------------------------------------------------
# I18n: make english() use composed translation too and add dynamic fragments
# required by runtime values, population narratives and fullscreen titles.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/I18n.java'
patch(path,
'''    public static String english(String italian) {\n        if (italian == null) return null;\n        String translated = translateDirect(italian, EN, IT);\n        return translated.equals(italian) && requiresTranslation(italian)\n                ? "[Missing English translation]" : translated;\n    }''',
'''    public static String english(String italian) {\n        if (italian == null) return null;\n        String translated = translateDirect(italian, EN, IT);\n        if (!translated.equals(italian)) return translated;\n        if (IT.containsKey(italian)) return italian;\n        String composed = translateComposedEnglish(italian);\n        if (!composed.equals(italian)) return composed;\n        return requiresTranslation(italian) ? "[Missing English translation]" : italian;\n    }''')
# Add a final mapping block before end of static initializer.
p = ROOT/path
text = p.read_text(encoding='utf-8')
anchor = '''        put("ASCII e FITS", "ASCII and FITS");\n    }'''
block = '''        put("ASCII e FITS", "ASCII and FITS");\n        // UX / i18n hardening 1.3.0 - runtime-generated text and fullscreen copy\n        put("Senza z", "No z");\n        put("non disponibile", "unavailable");\n        put("dal trigger", "from trigger");\n        put("s dal trigger", "s from trigger");\n        put("rispetto al trigger", "relative to trigger");\n        put("Confronta la forma temporale di un gruppo di GRB e descrivi durata, distanza e qualità del campione.",\n                "Compare the temporal shape of a GRB sample and inspect duration, distance and data quality.");\n        put("linee normalizzate e allineate al trigger", "normalized lines aligned to the trigger");\n        put("comportamento centrale del gruppo a ogni secondo", "central behavior of the group at each second");\n        put("tra i due limiti cade il 50% centrale delle curve", "the central 50% of curves lies between the two limits");\n        put("Rate normalizzato (picco = 1)", "Normalized rate (peak = 1)");\n        put("curve normalizzate e allineate a t = 0", "curves normalized and aligned at t = 0");\n        put("GRB corrispondono ai filtri preliminari", "GRBs match the preliminary filters");\n        put("saranno esaminati", "will be examined");\n        put("già in RAM", "already in RAM");\n        put("GRB inclusi su", "GRBs included out of");\n        put("esaminati", "examined");\n        put("copertura rilevata", "observed coverage");\n        put("fuori dal filtro", "outside filter");\n        put("non leggibili", "unreadable");\n        put("Attendo catalogo e metadati scientifici…", "Waiting for catalog and scientific metadata…");\n        put("Caricamento parallelo", "Parallel loading");\n        put("Preparazione confronto…", "Preparing comparison…");\n        put("Modello spettrale", "Spectral model");\n        put("Flusso energetico 3D", "3D energy flux");\n        put("Rate nel tempo per banda", "Rate over time by band");\n        put("vista 3D", "3D view");\n        put("quattro bande ASCII a 1 s", "four 1-s ASCII bands");\n        put("Confronto tra bande — leggendo verticalmente lo stesso istante puoi confrontare come il rate si distribuisce tra 15–25, 25–50, 50–100 e 100–350 keV. Le differenze di colore evidenziano variazioni relative del segnale tra i canali.",\n                "Band comparison — read vertically at the same instant to compare how rate is distributed across 15–25, 25–50, 50–100 and 100–350 keV. Color differences highlight relative signal changes between channels.");\n        put("Interpretazione — una zona arancione intensa individua un intervallo temporale in cui il rate netto è elevato in quella banda. Il confronto resta descrittivo: per ottenere un flusso fisico servono risposta strumentale e fit spettroscopico.",\n                "Interpretation — an intense orange region marks a time interval with high net rate in that band. The comparison is descriptive: instrumental response and spectral fitting are required to obtain physical flux.");\n        put("Esegui un'analisi per ottenere un commento automatico sul campione.",\n                "Run an analysis to obtain an automatic summary of the sample.");\n        put("Il testo deriva solo dalle statistiche del grafico e non sostituisce l'interpretazione scientifica.",\n                "The text is derived only from chart statistics and does not replace scientific interpretation.");\n        put("ANALISI LOCALE · RIPRODUCIBILE", "LOCAL · REPRODUCIBLE ANALYSIS");\n        put("curve incluse: il profilo mediano raggiunge il massimo a t =",\n                "curves included: the median profile reaches its maximum at t =");\n        put("Posizione temporale — Il massimo della mediana cade", "Timing — The median maximum occurs");\n        put("in prossimità del trigger", "near the trigger");\n        put("prima del trigger", "before the trigger");\n        put("dopo il trigger", "after the trigger");\n        put("resta sopra metà massimo per circa", "stays above half maximum for about");\n        put("Il massimo cade sul bordo della finestra: prova una finestra più ampia.",\n                "The maximum lies at the edge of the window: try a wider window.");\n        put("Forma prima/dopo il trigger — Il segnale positivo non è sufficiente per stimare lo sbilanciamento.",\n                "Pre/post-trigger shape — Positive signal is insufficient to estimate the asymmetry.");\n        put("Forma prima/dopo il trigger — La mediana ha più area positiva dopo t = 0: nel campione selezionato prevale una coda post-trigger.",\n                "Pre/post-trigger shape — The median has more positive area after t = 0: the selected sample is dominated by a post-trigger tail.");\n        put("Forma prima/dopo il trigger — La mediana ha più area positiva prima di t = 0: il profilo selezionato è sbilanciato verso il pre-trigger.",\n                "Pre/post-trigger shape — The median has more positive area before t = 0: the selected profile is skewed toward the pre-trigger interval.");\n        put("Forma prima/dopo il trigger — Le aree positive prima e dopo t = 0 sono relativamente bilanciate.",\n                "Pre/post-trigger shape — Positive areas before and after t = 0 are relatively balanced.");\n        put("Variabilità — I percentili disponibili non bastano per stimare la dispersione tra le curve.",\n                "Variability — Available percentiles are insufficient to estimate dispersion between curves.");\n        put("Variabilità — La fascia 25°–75° ha ampiezza media",\n                "Variability — The 25th–75th percentile band has mean width");\n        put("contiene il 50% centrale delle curve e indica una dispersione",\n                "contains the central 50% of curves and indicates");\n        put("contenuta", "low dispersion");\n        put("moderata", "moderate dispersion");\n        put("elevata", "high dispersion");\n        put("Campione —", "Sample —");\n        put("nessun redshift disponibile", "no redshift available");\n        put("redshift per", "redshift for");\n        put("eventi (mediana z =", "events (median z =");\n        put("copertura FRACEXP mediana", "median FRACEXP coverage");\n        put("copertura FRACEXP non stimabile", "FRACEXP coverage cannot be estimated");\n        put("senza T90", "without T90");\n        put("Campione piccolo: mediana e percentili possono cambiare molto aggiungendo pochi eventi.",\n                "Small sample: median and percentiles may change substantially when only a few events are added.");\n        put("eventi esaminati non sono stati letti e non contribuiscono al profilo.",\n                "examined events could not be read and do not contribute to the profile.");\n        put("Il redshift è disponibile solo per il", "Redshift is available for only");\n        put("del campione: la distribuzione z non è completa.", "of the sample: the z distribution is incomplete.");\n        put("Le curve sono divise per il proprio picco: il confronto riguarda la forma relativa, non la luminosità assoluta.",\n                "Curves are divided by their own peak: the comparison concerns relative shape, not absolute luminosity.");\n    }'''
if anchor not in text:
    raise SystemExit('I18n static initializer anchor missing')
text = text.replace(anchor, block, 1)
p.write_text(text, encoding='utf-8')

# -----------------------------------------------------------------------------
# CSS: avoid the JavaFX 17 Modena looked-up-color ClassCastException on chart bars;
# compact radio controls/FRACEXP and keep compare ComboBox popup dark.
# -----------------------------------------------------------------------------
path = 'src/main/resources/app.css'
patch(path,
''' .bar-chart .chart-bar {''',
''' .bar-chart .chart-bar {''') if False else None
p = ROOT/path
text = p.read_text(encoding='utf-8')
old = '''.bar-chart .chart-bar {\n    -fx-background-color: linear-gradient(to top, #7b56c9, #4fd5ef);\n    -fx-background-radius: 5px 5px 0 0;\n}'''
new = '''.bar-chart .chart-bar {\n    -fx-bar-fill: #7767d7;\n    -fx-background-radius: 5px 5px 0 0;\n}'''
if old not in text:
    raise SystemExit('bar-chart CSS block missing')
text = text.replace(old, new, 1)
text += '''\n\n/* ---------- UX stabilization 1.3.0 ---------- */\n.compact-radio {\n    -fx-padding: 6px 7px;\n    -fx-font-size: 9.5px;\n}\n.population-fracexp-inline {\n    -fx-padding: 7px 8px;\n}\n.population-fracexp-inline .percentage-control {\n    -fx-padding: 5px 6px;\n}\n.population-fracexp-inline .percentage-field {\n    -fx-min-width: 48px;\n    -fx-pref-width: 52px;\n    -fx-max-width: 56px;\n}\n.population-fracexp-inline .filter-reset-button {\n    -fx-min-width: 25px;\n    -fx-min-height: 25px;\n    -fx-pref-width: 25px;\n    -fx-pref-height: 25px;\n}\n.compare-combo, .compare-combo .text-field, .compare-combo > .list-cell {\n    -fx-background-color: #0b1019;\n    -fx-text-fill: #e7ebf2;\n}\n.compare-combo .arrow-button {\n    -fx-background-color: rgba(255,255,255,0.045);\n}\n.compare-combo .combo-box-popup .list-view {\n    -fx-background-color: #090d15;\n    -fx-control-inner-background: #090d15;\n}\n.compare-combo .combo-box-popup .list-cell {\n    -fx-background-color: #090d15;\n    -fx-text-fill: #dfe5ee;\n}\n.compare-combo .combo-box-popup .list-cell:hover,\n.compare-combo .combo-box-popup .list-cell:selected {\n    -fx-background-color: rgba(255,174,91,0.12);\n    -fx-text-fill: #fff3e4;\n}\n'''
p.write_text(text, encoding='utf-8')

print('UI / compare / population / i18n fixes applied successfully')
