from pathlib import Path
import re

ROOT = Path('.')

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def write(path, text):
    (ROOT / path).write_text(text, encoding='utf-8')

def repl(path, old, new):
    text = read(path)
    if old not in text:
        raise SystemExit(f'Pattern not found in {path}: {old[:140]!r}')
    write(path, text.replace(old, new))

# ---------------------------------------------------------------------------
# I18n: prevent already-English runtime text from being mistaken for Italian.
# This is the root cause of many [Missing English translation] placeholders.
# ---------------------------------------------------------------------------
i18n_path = 'src/main/java/it/casiraghi/swiftbat/ui/I18n.java'
i18n = read(i18n_path)

old = '''        String composed = translateComposedEnglish(text);\n        if (!composed.equals(text)) return composed;\n        return requiresTranslation(text) ? "[Missing English translation]" : text;'''
new = '''        String composed = translateComposedEnglish(text);\n        if (!composed.equals(text)) return composed;\n        if (looksLikeEnglish(text)) return text;\n        return requiresTranslation(text) ? "[Missing English translation]" : text;'''
if old not in i18n:
    raise SystemExit('I18n.t fallback pattern not found')
i18n = i18n.replace(old, new, 1)

old2 = '''        String composed = translateComposedEnglish(italian);\n        if (!composed.equals(italian)) return composed;\n        return requiresTranslation(italian) ? "[Missing English translation]" : italian;'''
new2 = '''        String composed = translateComposedEnglish(italian);\n        if (!composed.equals(italian)) return composed;\n        if (looksLikeEnglish(italian)) return italian;\n        return requiresTranslation(italian) ? "[Missing English translation]" : italian;'''
if old2 not in i18n:
    raise SystemExit('I18n.english fallback pattern not found')
i18n = i18n.replace(old2, new2, 1)

anchor = '''    public static boolean requiresTranslation(String text) {\n        if (text == null || text.isBlank()) return false;'''
helper = '''    private static boolean looksLikeEnglish(String text) {\n        if (text == null || text.isBlank()) return false;\n        String padded = " " + text.toLowerCase(java.util.Locale.ROOT)\n                .replaceAll("[^a-z0-9]+", " ") + " ";\n        String[] cues = {\n                " the ", " and ", " is ", " are ", " was ", " were ", " to ", " from ", " with ",\n                " without ", " this ", " that ", " it ", " each ", " used ", " measured ",\n                " between ", " relative ", " not ", " do ", " does ", " in ", " of ", " for ",\n                " by ", " as ", " when ", " while ", " can ", " count ", " counts ", " rate ",\n                " time ", " energy ", " signal ", " data ", " field ", " source ", " selected ",\n                " available ", " description ", " comparison ", " sample ", " trigger ", " bin ",\n                " flux ", " uncertainty ", " mission ", " curve ", " value ", " values "\n        };\n        int score = 0;\n        for (String cue : cues) {\n            if (padded.contains(cue) && ++score >= 2) return true;\n        }\n        return false;\n    }\n\n    public static boolean requiresTranslation(String text) {\n        if (text == null || text.isBlank()) return false;\n        if (looksLikeEnglish(text)) return false;'''
if anchor not in i18n:
    raise SystemExit('I18n requiresTranslation anchor not found')
i18n = i18n.replace(anchor, helper, 1)

# Add a few status/static entries that are currently generated directly by pages.
insert_anchor = '        put("Elaborazione del campione…", "Processing sample…");\n'
extra = '''        put("Elaborazione del campione…", "Processing sample…");\n        put("Filtri in preparazione…", "Preparing filters…");\n        put("Attendo catalogo e metadati", "Waiting for catalog and metadata");\n        put("Coordinate celesti non ancora caricate", "Sky coordinates not loaded yet");\n        put("dopo i filtri", "after filters");\n        put("durata non disponibile", "duration unavailable");\n        put("Cielo", "Sky");\n        put("Preparazione confronto…", "Preparing comparison…");\n        put("Picco / errore", "Peak / error");\n        put("Durezza proxy", "Hardness proxy");\n        put("Min", "Min");\n        put("Max", "Max");\n'''
if insert_anchor not in i18n:
    raise SystemExit('I18n insertion anchor not found')
i18n = i18n.replace(insert_anchor, extra, 1)
write(i18n_path, i18n)

# ---------------------------------------------------------------------------
# Glossary: English scientific descriptions are already localized strings.
# Do not run them through UiFactory/I18n a second time. Refresh on language swap.
# ---------------------------------------------------------------------------
glossary = 'src/main/java/it/casiraghi/swiftbat/ui/GlossaryPage.java'
repl(glossary,
'''        if (!definitions.isEmpty()) {\n            list.getSelectionModel().selectFirst();\n        }''',
'''        if (!definitions.isEmpty()) {\n            list.getSelectionModel().selectFirst();\n        }\n        I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> {\n            list.refresh();\n            FieldDefinition selected = list.getSelectionModel().getSelectedItem();\n            if (selected != null) showDefinition(selected);\n        });''')

repl(glossary,
'''        card.getChildren().addAll(titleRow, UiFactory.wrappedLabel(text, "explanation-text"));\n        return card;''',
'''        Label body = new Label(text);\n        body.getStyleClass().add("explanation-text");\n        body.setWrapText(true);\n        body.setMaxWidth(Double.MAX_VALUE);\n        card.getChildren().addAll(titleRow, body);\n        return card;''')

repl(glossary,
'''            Label simple = UiFactory.wrappedLabel(definitionText(item, 0), "dictionary-preview");''',
'''            Label simple = new Label(definitionText(item, 0));\n            simple.getStyleClass().add("dictionary-preview");\n            simple.setWrapText(true);''')

# ---------------------------------------------------------------------------
# Compare: keep selector pair visually centered, normalize option anchored right.
# ---------------------------------------------------------------------------
compare = 'src/main/java/it/casiraghi/swiftbat/ui/ComparePage.java'
repl(compare,
'''        HBox controls = new HBox(10);\n        controls.getStyleClass().add("compare-control-bar");\n        controls.setPadding(new Insets(13, 15, 13, 15));\n        controls.setAlignment(Pos.CENTER_LEFT);\n        controls.getChildren().addAll(\n                UiFactory.label("Evento A", "toolbar-label"), firstBox,\n                UiFactory.label("vs", "compare-vs"),\n                UiFactory.label("Evento B", "toolbar-label"), secondBox,\n                UiFactory.spacer(), normalize);\n        header.getChildren().addAll(copy, controls);''',
'''        HBox selectors = new HBox(12,\n                UiFactory.label("Evento A", "toolbar-label"), firstBox,\n                UiFactory.label("vs", "compare-vs"),\n                UiFactory.label("Evento B", "toolbar-label"), secondBox);\n        selectors.setAlignment(Pos.CENTER);\n\n        javafx.scene.layout.BorderPane controls = new javafx.scene.layout.BorderPane();\n        controls.getStyleClass().add("compare-control-bar");\n        controls.setPadding(new Insets(13, 15, 13, 15));\n        controls.setCenter(selectors);\n        controls.setRight(normalize);\n        javafx.scene.layout.BorderPane.setAlignment(selectors, Pos.CENTER);\n        javafx.scene.layout.BorderPane.setAlignment(normalize, Pos.CENTER_RIGHT);\n        header.getChildren().addAll(copy, controls);''')

# ---------------------------------------------------------------------------
# Population filters: align the five groups in one centered, readable row.
# FRACEXP uses compact Min/Max labels and no overlapping spacer/value row.
# ---------------------------------------------------------------------------
pop = 'src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java'
repl(pop,
'''        VBox minimum = exposureControl("Minimo ammesso", exposureMinSlider, exposureMin, true);\n        VBox maximum = exposureControl("Massimo ammesso", exposureMaxSlider, exposureMax, false);''',
'''        VBox minimum = exposureControl("Min", exposureMinSlider, exposureMin, true);\n        VBox maximum = exposureControl("Max", exposureMaxSlider, exposureMax, false);''')

repl(pop,
'''        exposureBox.setMinWidth(270);\n        exposureBox.setPrefWidth(280);\n        exposureBox.setMaxWidth(295);\n\n        VBox durationGroup = filterGroup("Durata T90", "", duration, 150);\n        VBox redshiftGroup = filterGroup("Redshift", "", redshiftControl, 185);\n        VBox windowGroup = filterGroup("Finestra temporale", "", windowControl, 180);\n        VBox limitGroup = filterGroup("Campione massimo", "", limit, 125);\n        HBox topFilters = new HBox(14, durationGroup, redshiftGroup, windowGroup, exposureBox, limitGroup);\n        topFilters.setAlignment(Pos.TOP_LEFT);\n        HBox.setHgrow(exposureBox, Priority.NEVER);''',
'''        exposureBox.setMinWidth(325);\n        exposureBox.setPrefWidth(335);\n        exposureBox.setMaxWidth(350);\n\n        VBox durationGroup = filterGroup("Durata T90", "", duration, 155);\n        VBox redshiftGroup = filterGroup("Redshift", "", redshiftControl, 195);\n        VBox windowGroup = filterGroup("Finestra temporale", "", windowControl, 190);\n        VBox limitGroup = filterGroup("Campione massimo", "", limit, 135);\n        HBox topFilters = new HBox(16, durationGroup, redshiftGroup, windowGroup, exposureBox, limitGroup);\n        topFilters.setAlignment(Pos.TOP_CENTER);\n        topFilters.setFillHeight(true);\n        HBox.setHgrow(exposureBox, Priority.NEVER);\n        HBox topFiltersWrapper = new HBox(topFilters);\n        topFiltersWrapper.setAlignment(Pos.TOP_CENTER);\n        topFiltersWrapper.setMaxWidth(Double.MAX_VALUE);''')

repl(pop,
'''        VBox card = new VBox(9, topFilters, advanced, advancedContent, footer);''',
'''        VBox card = new VBox(12, topFiltersWrapper, advanced, advancedContent, footer);''')

repl(pop,
'''        HBox valueBox = new HBox(5, field, unit, reset);\n        valueBox.setAlignment(Pos.CENTER_RIGHT);\n        HBox heading = new HBox(8, UiFactory.label(label, "filter-label"), UiFactory.spacer(), valueBox);\n        heading.setAlignment(Pos.CENTER_LEFT);''',
'''        HBox valueBox = new HBox(5, field, unit, reset);\n        valueBox.setAlignment(Pos.CENTER_LEFT);\n        Label caption = UiFactory.label(label, "filter-label");\n        caption.setMinWidth(28);\n        HBox heading = new HBox(6, caption, valueBox);\n        heading.setAlignment(Pos.CENTER_LEFT);''')

repl(pop,
'''        box.setMinWidth(125);\n        box.setPrefWidth(150);''',
'''        box.setMinWidth(145);\n        box.setPrefWidth(155);''')

repl(pop,
'''        if (ready && runningTask == null) {\n            setStatus(metadata.size() + " GRB con T90/coordinate · redshift integrato", "status-online");\n        }''',
'''        if (ready && runningTask == null) {\n            I18n.setText(status,\n                    metadata.size() + " GRB con T90/coordinate · redshift integrato",\n                    metadata.size() + " GRBs with T90/coordinates · redshift integrated");\n            status.getStyleClass().removeAll("status-neutral", "status-online", "status-warning");\n            status.getStyleClass().add("status-online");\n        }''')

# ---------------------------------------------------------------------------
# Stronger translation coverage test: scan all UI/component Java files, not five.
# ---------------------------------------------------------------------------
test = 'src/test/java/it/casiraghi/swiftbat/ui/TranslationCoverageTest.java'
text = read(test)
text = text.replace('''    private static final List<String> CORE = List.of(\n            "MainView.java", "HomePage.java", "AboutPage.java", "GlossaryPage.java", "ComparePage.java");\n\n''', '')
text = text.replace('''    void migratedCoreScreensCannotContainUntranslatedItalianLiterals() throws Exception {\n        Path root = Path.of("src/main/java/it/casiraghi/swiftbat/ui");\n        List<String> missing = new ArrayList<>();\n        for (String fileName : CORE) {\n            Path file = root.resolve(fileName);\n            String source = Files.readString(file, StandardCharsets.UTF_8);''',
'''    void everyUiScreenCannotContainUntranslatedItalianLiterals() throws Exception {\n        Path root = Path.of("src/main/java/it/casiraghi/swiftbat/ui");\n        List<String> missing = new ArrayList<>();\n        try (var files = Files.walk(root)) {\n            for (Path file : files.filter(path -> path.toString().endsWith(".java"))\n                    .filter(path -> !path.getFileName().toString().equals("I18n.java")).toList()) {\n            String fileName = root.relativize(file).toString();\n            String source = Files.readString(file, StandardCharsets.UTF_8);''')
text = text.replace('''            }\n        }\n        if (!missing.isEmpty()) {''',
'''            }\n            }\n        }\n        if (!missing.isEmpty()) {''', 1)
write(test, text)

print('global i18n/layout patch applied')
