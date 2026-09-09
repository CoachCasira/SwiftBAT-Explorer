from pathlib import Path
import re

ROOT = Path('.')

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def write(path, text):
    (ROOT / path).write_text(text, encoding='utf-8')

def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError(f'Marker not found: {label}')
    return text.replace(old, new, 1)

# -----------------------------------------------------------------------------
# I18n: broaden the visible app vocabulary.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/I18n.java'
text = read(path)
marker = '''        put("Seleziona colonne dal menu della tabella. La scelta viene salvata automaticamente.",
                "Select columns from the table menu. Your choice is saved automatically.");
'''
extra = '''        put("Cerca e filtra GRB", "Search and filter GRBs");
        put("Cerca per nome…", "Search by name…");
        put("Intervallo di date", "Date range");
        put("Intervallo di redshift", "Redshift range");
        put("SNR minimo", "Minimum SNR");
        put("Più recenti", "Most recent");
        put("Scegli un Gamma-Ray Burst", "Choose a Gamma-Ray Burst");
        put("Ricarica online", "Reload online");
        put("ASCII + FITS disponibili", "ASCII + FITS available");
        put("Curva di luce", "Light curve");
        put("Totale 15–350 keV", "Total 15–350 keV");
        put("Media mobile 5 bin", "5-bin moving average");
        put("Esporta PNG", "Export PNG");
        put("Centra", "Center");
        put("Sfera 3D", "3D sphere");
        put("Piano galattico", "Galactic plane");
        put("GRB selezionato", "Selected GRB");
        put("Nessun GRB selezionato", "No GRB selected");
        put("Apri curve di luce →", "Open light curves →");
        put("VISIBILI", "VISIBLE");
        put("SHORT", "SHORT");
        put("LONG", "LONG");
        put("SENZA T90", "NO T90");
        put("Tutti i GRB", "All GRBs");
        put("T90 non disponibile", "T90 unavailable");
        put("Reset", "Reset");
        put("Modello migliore BAT", "BAT best model");
        put("Modello mostrato", "Displayed model");
        put("Indice α", "α index");
        put("Esposizione", "Exposure");
        put("Modello spettrale ricostruito", "Reconstructed spectral model");
        put("Energia (keV)", "Energy (keV)");
        put("Modello non disponibile", "Model unavailable");
        put("Flusso energetico", "Energy flux");
        put("Flusso energetico per banda", "Energy flux by band");
        put("Banda energetica (keV)", "Energy band (keV)");
        put("Apri vista 3D dei rate", "Open 3D rate view");
        put("Mappa tempo–energia dei rate", "Rate time–energy map");
        put("Intera osservazione", "Full observation");
        put("Centra vista", "Center view");
        put("Curva", "Curve");
        put("Asse X", "X axis");
        put("Asse Y", "Y axis");
        put("Forma", "Shape");
        put("Confronto", "Comparison");
        put("Da ricordare", "Remember");
        put("Modello", "Model");
        put("Assi", "Axes");
        put("Forma della curva", "Curve shape");
        put("Profondità", "Depth");
        put("Interazione", "Interaction");
        put("Interpretazione", "Interpretation");
        put("Come leggere il modello 2D", "How to read the 2D model");
        put("Come leggere la vista 3D", "How to read the 3D view");
        put("Come leggere l'istogramma", "How to read the histogram");
        put("Come leggere il flusso 3D", "How to read the 3D flux view");
        put("Come leggere la mappa", "How to read the map");
        put("Qualità FRACEXP", "FRACEXP quality");
        put("Distanza cosmologica", "Cosmological distance");
        put("Mediana", "Median");
        put("Singoli GRB", "Individual GRBs");
        put("Fascia centrale 25°–75°", "Central 25th–75th percentile band");
        put("Da tenere presente", "Keep in mind");
        put("Copertura completa", "Full coverage");
        put("Caricamento coordinate…", "Loading coordinates…");
        put("Coordinate non disponibili", "Coordinates unavailable");
        put("Dati scientifici caricati parzialmente", "Scientific data partially loaded");
        put("Esporta mappa celeste", "Export sky map");
        put("Immagine PNG", "PNG image");
        put("Elaborazione del campione…", "Processing sample…");
        put("Analisi annullata", "Analysis cancelled");
        put("Nessuno", "None");
        put("corrispondenze", "matches");
'''
text = replace_once(text, marker, marker + extra, 'I18n vocabulary')
write(path, text)

# -----------------------------------------------------------------------------
# In-place fullscreen: every spectroscopy reading panel is hidden by default.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/InPlaceFullscreen.java'
text = read(path)
if 'import javafx.scene.control.ToggleButton;' not in text:
    text = text.replace('import javafx.scene.control.ScrollPane;\n', 'import javafx.scene.control.ScrollPane;\nimport javafx.scene.control.ToggleButton;\n')
pattern = re.compile(r'''        private Node responsiveReadingLayout\(Node content, VBox reading\) \{.*?\n        \}\n\n        private void updateResponsiveReadingLayout''', re.S)
replacement = '''        private Node responsiveReadingLayout(Node content, VBox reading) {
            if (content instanceof Region region) {
                region.setMinSize(0, 0);
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }

            ScrollPane readingScroll = new ScrollPane(reading);
            readingScroll.setFitToWidth(true);
            readingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            readingScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            readingScroll.setPannable(true);
            readingScroll.setMinSize(0, 0);
            readingScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
            readingScroll.viewportBoundsProperty().addListener((obs, oldBounds, bounds) ->
                    reading.setMinHeight(Math.max(0, bounds.getHeight())));

            ToggleButton help = new ToggleButton(I18n.t("Mostra spiegazione"));
            help.getStyleClass().addAll("ghost-button", "help-toggle");
            HBox helpBar = new HBox(UiFactory.spacer(), help);
            helpBar.setAlignment(Pos.CENTER_RIGHT);

            BorderPane split = new BorderPane();
            split.setCenter(content);
            split.setMinSize(0, 0);
            split.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            Runnable relayout = () -> {
                if (!help.isSelected()) {
                    split.setRight(null);
                    split.setBottom(null);
                    BorderPane.setMargin(content, Insets.EMPTY);
                } else {
                    updateResponsiveReadingLayout(split, content, reading, readingScroll, split.getWidth());
                }
            };
            help.selectedProperty().addListener((obs, oldValue, selected) -> {
                help.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
                relayout.run();
            });
            I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                    help.setText(I18n.t(help.isSelected() ? "Nascondi spiegazione" : "Mostra spiegazione")));
            split.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                if (help.isSelected()) updateResponsiveReadingLayout(
                        split, content, reading, readingScroll, newWidth.doubleValue());
            });

            VBox wrapper = new VBox(8, helpBar, split);
            wrapper.setMinSize(0, 0);
            wrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(split, Priority.ALWAYS);
            return wrapper;
        }

        private void updateResponsiveReadingLayout'''
text, count = pattern.subn(replacement, text, count=1)
if count != 1:
    raise RuntimeError('InPlaceFullscreen responsiveReadingLayout not replaced')
write(path, text)

# -----------------------------------------------------------------------------
# Spectroscopy: normal descriptions and time-energy description collapse too.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java'
text = read(path)
if 'import javafx.scene.control.ToggleButton;' not in text:
    text = text.replace('import javafx.scene.control.ToggleGroup;\n', 'import javafx.scene.control.ToggleGroup;\nimport javafx.scene.control.ToggleButton;\n')
text = replace_once(text,
'''        card.getChildren().addAll(
                UiFactory.label("Modello spettrale ricostruito", "card-title"),
                explanation);
''',
'''        card.getChildren().add(UiFactory.label("Modello spettrale ricostruito", "card-title"));
        card.getChildren().add(UiFactory.collapsibleHelp("", explanation));
''', 'spectral model collapsible help')
text = replace_once(text,
'''        card.getChildren().addAll(UiFactory.label("Flusso energetico", "card-title"), explanation);
''',
'''        card.getChildren().add(UiFactory.label("Flusso energetico", "card-title"));
        card.getChildren().add(UiFactory.collapsibleHelp("", explanation));
''', 'flux chart collapsible help')
# Replace time-energy controls help text with no helper line (controlBox already ignores help, but keep source clean).
text = text.replace('''                controlBox("Finestra temporale", window,
                        "Limita la mappa ai secondi prima e dopo t = 0; non modifica i parametri del fit ufficiale.",
                        620));''',
                    '''                controlBox("Finestra temporale", window, "", 620));''')
old_tail = '''        box.getChildren().addAll(description, controls, chartCard,
                UiFactory.wrappedLabel(
                        "Nota: i rate BAT sono già corretti per il fondo; piccole celle negative rappresentano fluttuazioni statistiche dopo la sottrazione del fondo.",
                        "spectroscopy-note"));
'''
new_tail = '''        Label note = UiFactory.wrappedLabel(
                "Nota: i rate BAT sono già corretti per il fondo; piccole celle negative rappresentano fluttuazioni statistiche dopo la sottrazione del fondo.",
                "spectroscopy-note");
        VBox explanationBox = new VBox(7, description, note);
        box.getChildren().addAll(controls, chartCard, UiFactory.collapsibleHelp("", explanationBox));
'''
text = replace_once(text, old_tail, new_tail, 'time energy normal collapsible help')
old_full = '''        HBox body = new HBox(14, enlarged, reading);
        body.setPadding(new Insets(12, 14, 12, 14));
        body.setMinSize(0, 0);
        body.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(enlarged, Priority.ALWAYS);
        InPlaceFullscreen.show(this, grbData.grbName() + " · Mappa tempo–energia dei rate", body);
'''
new_full = '''        reading.setVisible(false);
        reading.setManaged(false);
        ToggleButton help = new ToggleButton(I18n.t("Mostra spiegazione"));
        help.getStyleClass().addAll("ghost-button", "help-toggle");
        help.selectedProperty().addListener((obs, oldValue, selected) -> {
            reading.setVisible(selected);
            reading.setManaged(selected);
            help.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
        });
        HBox helpBar = new HBox(UiFactory.spacer(), help);
        helpBar.setAlignment(Pos.CENTER_RIGHT);

        HBox body = new HBox(14, enlarged, reading);
        body.setPadding(new Insets(0, 14, 12, 14));
        body.setMinSize(0, 0);
        body.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(enlarged, Priority.ALWAYS);
        VBox fullscreen = new VBox(8, helpBar, body);
        fullscreen.setMinSize(0, 0);
        fullscreen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        InPlaceFullscreen.show(this, grbData.grbName() + " · Mappa tempo–energia dei rate", fullscreen);
'''
text = replace_once(text, old_full, new_full, 'time energy fullscreen collapsible help')
write(path, text)

# -----------------------------------------------------------------------------
# Population: collapse reading assistant in fullscreen 2D/3D, trim histogram prose,
# and replace the 3-option controls with radio buttons.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java'
text = read(path)
# Build radio nodes after choice setup.
marker = '''        for (ChoiceBox<?> choice : List.of(duration, redshiftAvailability, window, limit)) {
            choice.getStyleClass().add("choice-box-modern");
            UiFactory.autoTooltip(choice);
        }

        FlowPane primary = new FlowPane(9, 7);
'''
replacement = '''        for (ChoiceBox<?> choice : List.of(duration, redshiftAvailability, window, limit)) {
            choice.getStyleClass().add("choice-box-modern");
            UiFactory.autoTooltip(choice);
        }
        Node redshiftControl = compactRadioChoice(redshiftAvailability,
                List.of(ALL_Z, WITH_Z, WITHOUT_Z), List.of("Tutti", "Con z", "Senza z"));
        Node windowControl = compactRadioChoice(window,
                List.of("±20 s", "±60 s", "±120 s"), List.of("±20 s", "±60 s", "±120 s"));

        FlowPane primary = new FlowPane(9, 7);
'''
text = replace_once(text, marker, replacement, 'population radio setup')
text = text.replace('filterGroup("Redshift", "Disponibilità della misura z", redshiftAvailability, 190)',
                    'filterGroup("Redshift", "", redshiftControl, 245)')
text = text.replace('filterGroup("Finestra temporale", "Secondi attorno al trigger", window, 150)',
                    'filterGroup("Finestra temporale", "", windowControl, 230)')
# fullscreen 2D and 3D assistants hidden.
old_2d = '''        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        HBox content = new HBox(18, chartColumn, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.setMinSize(0, 0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("population-fullscreen-content");
        InPlaceFullscreen.show(this, "Profilo temporale della popolazione", content);
'''
new_2d = '''        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        assistant.setVisible(false);
        assistant.setManaged(false);
        ToggleButton help = fullscreenHelpToggle(assistant);
        HBox content = new HBox(18, chartColumn, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.setMinSize(0, 0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("population-fullscreen-content");
        HBox helpBar = new HBox(UiFactory.spacer(), help);
        VBox wrapper = new VBox(8, helpBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        InPlaceFullscreen.show(this, "Profilo temporale della popolazione", wrapper);
'''
text = replace_once(text, old_2d, new_2d, 'population fullscreen 2d help')
old_3d = '''        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        HBox content = new HBox(18, pane, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.setMinSize(0, 0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("population-fullscreen-content");
        InPlaceFullscreen.show(this, "Profilo di popolazione 3D", content);
'''
new_3d = '''        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        assistant.setVisible(false);
        assistant.setManaged(false);
        ToggleButton help = fullscreenHelpToggle(assistant);
        HBox content = new HBox(18, pane, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.setMinSize(0, 0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("population-fullscreen-content");
        HBox helpBar = new HBox(UiFactory.spacer(), help);
        VBox wrapper = new VBox(8, helpBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        InPlaceFullscreen.show(this, "Profilo di popolazione 3D", wrapper);
'''
text = replace_once(text, old_3d, new_3d, 'population fullscreen 3d help')
# Histogram cards: title only, no explanatory subtitle.
text = replace_once(text,
'''        VBox card = UiFactory.card(title, subtitle, content);
''',
'''        VBox card = UiFactory.card(title, "", content);
''', 'population histogram subtitle')
# Helpers before filterGroup.
marker = '''    private static VBox filterGroup(String title, String detail, Node control, double width) {
'''
helpers = '''    private static Node compactRadioChoice(ChoiceBox<String> backing, List<String> values, List<String> labels) {
        ToggleGroup group = new ToggleGroup();
        HBox row = new HBox(5);
        row.getStyleClass().add("compact-radio-group");
        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index);
            String label = labels.get(index);
            javafx.scene.control.RadioButton radio = new javafx.scene.control.RadioButton(I18n.t(label));
            radio.getStyleClass().add("compact-radio");
            radio.setToggleGroup(group);
            radio.setUserData(value);
            radio.setSelected(java.util.Objects.equals(backing.getValue(), value));
            radio.setOnAction(event -> backing.setValue(value));
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> radio.setText(I18n.t(label)));
            row.getChildren().add(radio);
        }
        backing.valueProperty().addListener((obs, oldValue, newValue) -> {
            for (javafx.scene.control.Toggle toggle : group.getToggles()) {
                if (java.util.Objects.equals(toggle.getUserData(), newValue)) {
                    group.selectToggle(toggle);
                    break;
                }
            }
        });
        return row;
    }

    private static ToggleButton fullscreenHelpToggle(Node assistant) {
        ToggleButton help = new ToggleButton(I18n.t("Mostra spiegazione"));
        help.getStyleClass().addAll("ghost-button", "help-toggle");
        help.selectedProperty().addListener((obs, oldValue, selected) -> {
            assistant.setVisible(selected);
            assistant.setManaged(selected);
            help.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
        });
        I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) ->
                help.setText(I18n.t(help.isSelected() ? "Nascondi spiegazione" : "Mostra spiegazione")));
        return help;
    }

'''
if marker not in text:
    raise RuntimeError('Population helpers marker missing')
text = text.replace(marker, helpers + marker, 1)
write(path, text)

# -----------------------------------------------------------------------------
# Sky map: trim redundant text, convert 3-option redshift to radios.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java'
text = read(path)
# Header becomes title only.
old = '''        VBox titleText = new VBox(5,
                UiFactory.label("Mappa celeste", "page-title"),
                UiFactory.wrappedLabel(
                        "Esplora la distribuzione dei GRB nel cielo con Mollweide 2D e sfera 3D interattiva.",
                        "page-subtitle"));
'''
new = '''        VBox titleText = new VBox(5,
                UiFactory.label("Mappa celeste", "page-title"));
'''
text = replace_once(text, old, new, 'sky title prose')
# Map card no interaction caption.
old = '''        mapCard.getChildren().addAll(mapHead, buildSkyLegend(), mapHost,
                UiFactory.wrappedLabel(
                        "Rotellina: zoom · trascina: sposta/ruota · doppio clic: centra. Viola = piano galattico.",
                        "sky-map-caption"));
'''
new = '''        mapCard.getChildren().addAll(mapHead, buildSkyLegend(), mapHost);
'''
text = replace_once(text, old, new, 'sky map caption')
# Redshift compact radios and remove help under filters.
old = '''        galacticPlane.getStyleClass().add("modern-check");
        Button reset = UiFactory.button("Reset", "ghost-button");
'''
new = '''        galacticPlane.getStyleClass().add("modern-check");
        Node redshiftControl = compactRedshiftRadios();
        Button reset = UiFactory.button("Reset", "ghost-button");
'''
text = replace_once(text, old, new, 'sky redshift radios setup')
text = replace_once(text,
'''        row.getChildren().addAll(search, durationFilter, redshiftFilter,
                raRange, decRange, zRange, galacticPlane, reset);

        Label help = UiFactory.wrappedLabel(
                "RA può attraversare 0°. I limiti RA/DEC e l'intervallo di redshift vengono applicati automaticamente.",
                "sky-filter-help");
        card.getChildren().addAll(row, help);
''',
'''        row.getChildren().addAll(search, durationFilter, redshiftControl,
                raRange, decRange, zRange, galacticPlane, reset);
        card.getChildren().add(row);
''', 'sky filter helper removal')
# Scientific note hidden under a toggle, not permanently occupying the panel.
old = '''        details.getChildren().addAll(
                UiFactory.label("GRB selezionato", "card-subtitle"),
                selectedName, rows, selectedCatalog, openButton,
                UiFactory.label("Nota scientifica", "card-title"), scientificNote);
'''
new = '''        details.getChildren().addAll(
                UiFactory.label("GRB selezionato", "card-subtitle"),
                selectedName, rows, selectedCatalog, openButton,
                UiFactory.collapsibleHelp("", scientificNote));
'''
text = replace_once(text, old, new, 'sky scientific note collapse')
# Add radio helper before compactSkyRange.
marker = '''    private HBox compactSkyRange(String label, TextField minimum, TextField maximum) {
'''
helper = '''    private Node compactRedshiftRadios() {
        ToggleGroup group = new ToggleGroup();
        HBox row = new HBox(4);
        row.getStyleClass().add("compact-radio-group");
        String[] values = {"Con e senza redshift", "Solo con redshift", "Solo senza redshift"};
        String[] labels = {"Tutti", "Con z", "Senza z"};
        for (int index = 0; index < values.length; index++) {
            final String value = values[index];
            final String label = labels[index];
            javafx.scene.control.RadioButton radio = new javafx.scene.control.RadioButton(I18n.t(label));
            radio.getStyleClass().add("compact-radio");
            radio.setToggleGroup(group);
            radio.setUserData(value);
            radio.setSelected(value.equals(redshiftFilter.getValue()));
            radio.setOnAction(event -> redshiftFilter.setValue(value));
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> radio.setText(I18n.t(label)));
            row.getChildren().add(radio);
        }
        redshiftFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            for (javafx.scene.control.Toggle toggle : group.getToggles()) {
                if (java.util.Objects.equals(toggle.getUserData(), newValue)) {
                    group.selectToggle(toggle);
                    break;
                }
            }
        });
        return row;
    }

'''
if marker not in text:
    raise RuntimeError('Sky radio helper marker missing')
text = text.replace(marker, helper + marker, 1)
write(path, text)

# -----------------------------------------------------------------------------
# CSS: when help is closed, graph receives the room; keep compact radios dense.
# -----------------------------------------------------------------------------
path = 'src/main/resources/app.css'
css = read(path)
css += '''

.collapsible-help {
    -fx-spacing: 6px;
    -fx-padding: 0;
}

.compact-radio-group .radio-button {
    -fx-cursor: hand;
}

.in-place-fullscreen .help-toggle {
    -fx-padding: 7 11;
}
'''
write(path, css)

# Remove this one-shot script/workflow from final tree.
for temporary in [
    ROOT / 'scripts/apply_professor_feedback_2.py',
    ROOT / '.github/workflows/apply-professor-feedback-2.yml',
]:
    if temporary.exists():
        temporary.unlink()

print('Second professor-feedback pass applied')
