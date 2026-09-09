from pathlib import Path
import re

ROOT = Path('.')


def read(path):
    return (ROOT / path).read_text(encoding='utf-8')


def write(path, text):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError(f'Marker not found for {label}')
    return text.replace(old, new, 1)


def java(s):
    return s.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')

# -----------------------------------------------------------------------------
# 1) I18n: reverse mapping, strict English fallback, dynamic bilingual labels,
#    chart/menu traversal and first large translation batch.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/I18n.java'
text = read(path)

if 'private static final Map<String, String> IT = new LinkedHashMap<>();' not in text:
    text = replace_once(
        text,
        '    private static final Map<String, String> EN = new LinkedHashMap<>();\n',
        '    private static final Map<String, String> EN = new LinkedHashMap<>();\n'
        '    private static final Map<String, String> IT = new LinkedHashMap<>();\n'
        '    private static final String LOCALIZED_IT = I18n.class.getName() + ".localized.it";\n'
        '    private static final String LOCALIZED_EN = I18n.class.getName() + ".localized.en";\n',
        'I18n maps')

translations = {
    # Main navigation / shell
    'Dati scientifici online\nNASA/GSFC Swift/BAT': 'Online scientific data\nNASA/GSFC Swift/BAT',
    'Fonte ufficiale  ↗': 'Official source  ↗',
    'Coordinate celesti…': 'Sky coordinates…',
    'Coordinate e T90 caricati; redshift temporaneamente non disponibile': 'Coordinates and T90 loaded; redshift is temporarily unavailable',
    'Coordinate celesti non disponibili': 'Sky coordinates unavailable',
    'Recupero i prodotti Swift/BAT online.': 'Retrieving Swift/BAT products online.',
    'In memoria': 'In memory',
    # Home
    'Esplora i Gamma-Ray Burst dal catalogo al cielo. Curve di luce, dati FITS e coordinate celesti in un\'unica app.': 'Explore Gamma-Ray Bursts from catalog to sky. Light curves, FITS data and sky coordinates in one app.',
    'Esplora i GRB  →': 'Explore GRBs  →',
    'Apri la mappa celeste': 'Open sky map',
    'Cerca un evento e apri curve, dati e metadati.': 'Search for an event and open curves, data and metadata.',
    'Apri catalogo': 'Open catalog',
    'Guarda i GRB sulla Mollweide o sulla sfera 3D.': 'View GRBs on the Mollweide map or on the 3D sphere.',
    'Esplora il cielo': 'Explore the sky',
    'Sovrapponi due eventi già aperti nella sessione.': 'Overlay two events already open in the session.',
    'Confronta eventi': 'Compare events',
    'Tre passaggi, niente file manuali': 'Three steps, no manual files',
    'Scegli un GRB': 'Choose a GRB',
    'Cerca nome o Trigger ID.': 'Search by name or Trigger ID.',
    'Aprilo': 'Open it',
    'L\'app recupera e interpreta i prodotti Swift/BAT online.': 'The app retrieves and interprets Swift/BAT products online.',
    'Passa da curva, 3D, tabelle, metadati e mappa celeste.': 'Move between curves, 3D views, tables, metadata and the sky map.',
    'Serve una spiegazione?': 'Need an explanation?',
    'Le schermate mantengono il dato originale e affiancano spiegazioni brevi per trigger, rate, errori, FRACEXP, FITS, RA, DEC e T90.': 'Screens preserve the original data and provide short explanations for trigger, rate, errors, FRACEXP, FITS, RA, DEC and T90.',
    'Apri info e guida': 'Open info and guide',
    # About
    'SwiftBAT Explorer è l\'applicazione sviluppata per la tesi di Matteo Casiraghi per consultare e comprendere i prodotti pubblici Swift/BAT dei Gamma-Ray Burst.': 'SwiftBAT Explorer is the application developed for Matteo Casiraghi\'s thesis to inspect and understand public Swift/BAT Gamma-Ray Burst products.',
    'Versione 1.2.0 · Java 17 · dati online': 'Version 1.2.0 · Java 17 · online data',
    'Apri guida ai dati  →': 'Open data guide  →',
    'Catalogo ufficiale NASA/GSFC Swift/BAT e prodotti DAT/FITS a binning di 1 secondo. I valori restano riconducibili alle sorgenti pubbliche usate dall\'app.': 'Official NASA/GSFC Swift/BAT catalog and 1-second-binned DAT/FITS products. Values remain traceable to the public sources used by the app.',
    'Curve': 'Curves',
    'Visualizzazione delle curve di luce totali e nelle quattro bande energetiche, con finestre temporali, zoom e viste dedicate per leggere meglio la struttura del burst.': 'Total and four-band light-curve visualization, with time windows, zoom and dedicated views for inspecting burst structure.',
    'Volta celeste': 'Sky distribution',
    'Mollweide 2D e sfera 3D costruite con le coordinate RA/DEC pubblicate da Swift/BAT. Le due viste mostrano lo stesso campione con rappresentazioni differenti.': '2D Mollweide map and 3D sphere built from RA/DEC coordinates published by Swift/BAT. Both views show the same sample with different representations.',
    'Sessione': 'Session',
    'Gli eventi aperti restano in memoria finché l\'app è in esecuzione. La cache locale evita download ripetuti senza modificare i prodotti scientifici sorgente.': 'Open events remain in memory while the app is running. The local cache avoids repeated downloads without modifying the source scientific products.',
    'Scopo scientifico': 'Scientific scope',
    'L\'app facilita consultazione, controllo e confronto descrittivo dei dati. Gli indicatori e la soglia T90 = 2 s mostrati nell\'interfaccia non sostituiscono una classificazione astrofisica validata.': 'The app supports data inspection, checking and descriptive comparison. Indicators and the T90 = 2 s threshold shown in the interface do not replace a validated astrophysical classification.',
    'Lettura dei risultati': 'Reading results',
    'Grafici, mappe, filtri e assistenti di lettura servono a mettere in evidenza pattern e differenze nel campione. Le viste 2D e 3D sono strumenti esplorativi e mantengono sempre separata la rappresentazione grafica dall\'interpretazione fisica.': 'Charts, maps, filters and reading aids highlight patterns and differences in the sample. The 2D and 3D views are exploratory tools and keep graphical representation separate from physical interpretation.',
    'Catalogo Swift/BAT  ↗': 'Swift/BAT catalog  ↗',
    'Tabella redshift BAT  ↗': 'BAT redshift table  ↗',
    # Glossary shell
    'Dizionario dei dati': 'Data dictionary',
    'Ogni voce è spiegata prima in parole semplici e poi in modo tecnico. Qui puoi cercare TIME, RATE, FRACEXP, TRIGTIME, OBS_ID e gli altri campi presenti nelle tabelle e nei FITS.': 'Each entry is explained first in simple terms and then technically. You can search TIME, RATE, FRACEXP, TRIGTIME, OBS_ID and the other fields found in tables and FITS files.',
    'Cerca un campo o un concetto…': 'Search for a field or concept…',
    'senza unità': 'no unit',
    'In parole semplici': 'In simple terms',
    'Descrizione tecnica': 'Technical description',
    'Perché è utile': 'Why it matters',
    'Attenzione a non confonderlo': 'Do not confuse it with',
    # Explorer/common scientific labels that still appear in long panels
    'Azioni grafico': 'Chart actions',
    'In breve': 'At a glance',
    'Trigger': 'Trigger',
    'Il punto zero dell\'allerta': 'The alert zero point',
    'Curva di luce a binning di 1 secondo': '1-second-binned light curve',
    'ASCII — quattro bande': 'ASCII — four bands',
    'FITS — un canale e qualità': 'FITS — one channel and quality',
    'Una lettura guidata dell\'evento': 'Guided event reading',
    'Indicatori calcolati per questo evento': 'Indicators calculated for this event',
    'Limite scientifico importante': 'Important scientific limitation',
    'Picco / errore': 'Peak / error',
    'Durezza proxy': 'Hardness proxy',
    # Dictionary categories
    'Tempo': 'Time',
    'Segnale': 'Signal',
    'Incertezza': 'Uncertainty',
    'Conteggi': 'Counts',
    'Qualità': 'Quality',
    'Segnale per energia': 'Signal by energy',
    'Incertezza per energia': 'Uncertainty by energy',
    'Metadato': 'Metadata field',
    'ASCII e FITS': 'ASCII and FITS',
}

marker = '        put("corrispondenze", "matches");\n'
if '// STRICT_I18N_BATCH_1' not in text:
    lines = ['        // STRICT_I18N_BATCH_1']
    for it, en in translations.items():
        lines.append(f'        put("{java(it)}", "{java(en)}");')
    text = replace_once(text, marker, marker + '\n'.join(lines) + '\n', 'translation batch')

text = text.replace(
    '    private static void put(String it, String en) { EN.put(it, en); }',
    '    private static void put(String it, String en) {\n'
    '        EN.put(it, en);\n'
    '        IT.putIfAbsent(en, it);\n'
    '    }')

method_pattern = re.compile(r'    public static String t\(String text\) \{.*?\n    \}\n\n    public static void localizeLabeled', re.S)
replacement = r'''    public static String t(String text) {
        if (text == null) return null;
        if (language() == Language.IT) {
            return translateDirect(text, IT, EN);
        }
        String translated = translateDirect(text, EN, IT);
        if (!translated.equals(text)) return translated;
        if (IT.containsKey(text)) return text;
        return requiresTranslation(text) ? "[Missing English translation]" : text;
    }

    private static String translateDirect(String text, Map<String, String> target, Map<String, String> reverse) {
        String direct = target.get(text);
        if (direct != null) return direct;
        if (reverse.containsKey(text)) return text;
        int firstLetter = -1;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) { firstLetter = i; break; }
        }
        if (firstLetter > 0) {
            String prefix = text.substring(0, firstLetter);
            String tail = text.substring(firstLetter);
            String translated = target.get(tail);
            if (translated != null) return prefix + translated;
            if (reverse.containsKey(tail)) return text;
        }
        return text;
    }

    public static String english(String italian) {
        if (italian == null) return null;
        String translated = translateDirect(italian, EN, IT);
        return translated.equals(italian) && requiresTranslation(italian)
                ? "[Missing English translation]" : translated;
    }

    public static boolean hasEnglish(String italian) {
        if (italian == null || italian.isBlank()) return true;
        return EN.containsKey(italian) || !requiresTranslation(italian);
    }

    public static boolean requiresTranslation(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("http") || lower.contains("-fx-") || lower.matches("[a-z0-9_./:+#%\\-]+")) return false;
        String[] markers = {
                " il ", " lo ", " la ", " gli ", " le ", " un ", " una ", " di ", " del ", " della ",
                " dei ", " delle ", " e ", " è ", " per ", " con ", " senza ", " non ", " dal ", " nel ",
                " nella ", " nelle ", " mostra", "nascondi", "apri", "scegli", "cerca", "curva", "dati",
                "mappa", "analisi", "durata", "tempo", "valore", "qualità", "spiegazione", "disponibile",
                "intervallo", "flusso", "modello", "energia", "banda", "tabella", "righe", "esposizione",
                "guida", "informazioni", "evento", "confront", "schermo", "descrizione", "caric", "campione",
                "coordinate", "celeste", "sessione", "lettura", "risultati", "errore", "picco", "durezza",
                "fonte", "ufficiale", "filtro", "filtri", "visualizz", "nessun", "nessuna", "tutte", "tutti"
        };
        String padded = " " + lower + " ";
        for (String marker : markers) {
            if (padded.contains(marker)) return true;
        }
        return lower.matches(".*[àèéìòù].*");
    }

    public static String dynamic(String italian, String english) {
        return language() == Language.EN ? english : italian;
    }

    public static void setText(Labeled control, String italian, String english) {
        if (control == null) return;
        control.getProperties().put(LOCALIZED_IT, italian == null ? "" : italian);
        control.getProperties().put(LOCALIZED_EN, english == null ? "" : english);
        control.setText(language() == Language.EN ? english : italian);
    }

    public static void localizeLabeled'''
text, count = method_pattern.subn(replacement, text, count=1)
if count != 1:
    raise RuntimeError('I18n t() replacement failed')

old = '''        String original = (String) control.getProperties().get(ORIGINAL_TEXT);
        if (original == null) {
            original = control.getText();
            control.getProperties().put(ORIGINAL_TEXT, original);
        }
        control.setText(t(original));
'''
new = '''        Object bilingualIt = control.getProperties().get(LOCALIZED_IT);
        Object bilingualEn = control.getProperties().get(LOCALIZED_EN);
        if (bilingualIt instanceof String it && bilingualEn instanceof String en) {
            control.setText(language() == Language.EN ? en : it);
        } else {
            String original = (String) control.getProperties().get(ORIGINAL_TEXT);
            if (original == null) {
                original = control.getText();
                control.getProperties().put(ORIGINAL_TEXT, original);
            }
            control.setText(t(original));
        }
'''
text = replace_once(text, old, new, 'localized labeled pair')

# Extend tree traversal to chart titles/axes/series and MenuButton items.
needle = '        if (node instanceof Labeled labeled) localizeLabeled(labeled);\n'
extra = '''        if (node instanceof javafx.scene.chart.Chart chart && chart.getTitle() != null) {
            chart.setTitle(t(chart.getTitle()));
        }
        if (node instanceof javafx.scene.chart.Axis<?> axis && axis.getLabel() != null) {
            axis.setLabel(t(axis.getLabel()));
        }
        if (node instanceof javafx.scene.chart.XYChart<?, ?> xyChart) {
            for (Object raw : xyChart.getData()) {
                javafx.scene.chart.XYChart.Series<?, ?> series = (javafx.scene.chart.XYChart.Series<?, ?>) raw;
                if (series.getName() != null) series.setName(t(series.getName()));
            }
        }
        if (node instanceof MenuButton menuButton) {
            for (MenuItem item : menuButton.getItems()) {
                Object original = item.getProperties().get(ORIGINAL_TEXT);
                if (original == null) {
                    original = item.getText();
                    item.getProperties().put(ORIGINAL_TEXT, original);
                }
                item.setText(t((String) original));
            }
        }
'''
if extra not in text:
    text = replace_once(text, needle, needle + extra, 'chart/menu localization')
write(path, text)

# -----------------------------------------------------------------------------
# 2) MainView dynamic values: store both languages so switching back/forth is
#    deterministic and does not restore stale startup text.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/MainView.java'
text = read(path)
text = text.replace(
    'I18n.languageProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(() -> I18n.localizeTree(root)));',
    'I18n.languageProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(() -> {\n'
    '            I18n.localizeTree(root);\n'
    '            updateCacheStatus();\n'
    '        }));')
text = text.replace('        catalogStatus.setText("Catalogo…");',
                    '        I18n.setText(catalogStatus, "Catalogo…", "Catalog…");')
text = text.replace('            catalogStatus.setText(entries.size() + " GRB");',
                    '            I18n.setText(catalogStatus, entries.size() + " GRB", entries.size() + " GRBs");')
text = text.replace('            catalogStatus.setText(fallback.size() + " GRB ridotti");',
                    '            I18n.setText(catalogStatus, fallback.size() + " GRB ridotti", fallback.size() + " GRBs · fallback");')
text = text.replace(
    '        explorerPage.showLoading(0.02, "Apro " + entry.grbName(), "Recupero i prodotti Swift/BAT online.");',
    '        explorerPage.showLoading(0.02,\n'
    '                I18n.dynamic("Apro " + entry.grbName(), "Opening " + entry.grbName()),\n'
    '                I18n.dynamic("Recupero i prodotti Swift/BAT online.", "Retrieving Swift/BAT products online."));')
text = text.replace(
    '        sessionStatus.setText(sessionData.size() + " RAM · "\n                + grbService.persistentCachedCount() + " locali");',
    '        I18n.setText(sessionStatus,\n'
    '                sessionData.size() + " RAM · " + grbService.persistentCachedCount() + " locali",\n'
    '                sessionData.size() + " RAM · " + grbService.persistentCachedCount() + " local");')
text = text.replace('        connectionStatus.setText(text);',
                    '        I18n.setText(connectionStatus, text, I18n.english(text));')
write(path, text)

# -----------------------------------------------------------------------------
# 3) Glossary dynamic labels and category/source composition.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/GlossaryPage.java'
text = read(path)
text = text.replace(
    '        Label count = UiFactory.label(definitions.size() + " voci documentate", "sidebar-caption");\n'
    '        filtered.addListener((javafx.collections.ListChangeListener<FieldDefinition>) change ->\n'
    '                count.setText(filtered.size() + " voci visualizzate"));',
    '        Label count = UiFactory.label("", "sidebar-caption");\n'
    '        Runnable refreshCount = () -> I18n.setText(count,\n'
    '                filtered.size() + " voci documentate", filtered.size() + " documented entries");\n'
    '        filtered.addListener((javafx.collections.ListChangeListener<FieldDefinition>) change -> refreshCount.run());\n'
    '        I18n.languageProperty().addListener((obs, oldValue, newValue) -> refreshCount.run());\n'
    '        refreshCount.run();')
text = text.replace(
    '                UiFactory.label(definition.category() + " · " + definition.source(), "definition-kicker"));',
    '                UiFactory.label(I18n.t(definition.category()) + " · " + I18n.t(definition.source()), "definition-kicker"));')
text = text.replace(
    '            Label source = UiFactory.label(item.category() + " · " + item.source(), "dictionary-source");',
    '            Label source = UiFactory.label(I18n.t(item.category()) + " · " + I18n.t(item.source()), "dictionary-source");')
write(path, text)

# -----------------------------------------------------------------------------
# 4) Compare page: normal checkbox participates in I18n tree and counter has
#    deterministic bilingual text.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/ComparePage.java'
text = read(path)
text = text.replace(
    '    private final CheckBox normalize = new CheckBox("Normalizza ogni curva sul proprio picco");',
    '    private final CheckBox normalize = new CheckBox(I18n.t("Normalizza ogni curva sul proprio picco"));')
text = text.replace(
    '        counter.setText(matches.size() + (I18n.language() == I18n.Language.IT ? " corrispondenze" : " matches"));',
    '        I18n.setText(counter, matches.size() + " corrispondenze", matches.size() + " matches");')
write(path, text)

# -----------------------------------------------------------------------------
# 5) Guardrail test. This is intentionally source-based: for migrated core
#    screens Maven verify fails when a visible Italian literal lacks EN.
# -----------------------------------------------------------------------------
test = r'''package it.casiraghi.swiftbat.ui;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

class TranslationCoverageTest {
    private static final List<String> CORE = List.of(
            "MainView.java", "HomePage.java", "AboutPage.java", "GlossaryPage.java", "ComparePage.java");

    private static final Pattern VISIBLE_LITERAL = Pattern.compile(
            "(?:UiFactory\\.(?:label|wrappedLabel|button|quickTooltip|card)|new\\s+(?:Label|Button|ToggleButton|CheckBox|Tooltip)|set(?:Text|PromptText|Title|Label))\\s*\\(\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"\\s*[,)]",
            Pattern.MULTILINE);

    @Test
    void migratedCoreScreensCannotContainUntranslatedItalianLiterals() throws Exception {
        Path root = Path.of("src/main/java/it/casiraghi/swiftbat/ui");
        List<String> missing = new ArrayList<>();
        for (String fileName : CORE) {
            Path file = root.resolve(fileName);
            String source = Files.readString(file, StandardCharsets.UTF_8);
            Matcher matcher = VISIBLE_LITERAL.matcher(source);
            while (matcher.find()) {
                String literal = unescape(matcher.group(1));
                if (I18n.requiresTranslation(literal) && !I18n.hasEnglish(literal)) {
                    int line = 1;
                    for (int i = 0; i < matcher.start(); i++) if (source.charAt(i) == '\n') line++;
                    missing.add(fileName + ":" + line + " -> " + literal);
                }
            }
        }
        if (!missing.isEmpty()) {
            fail("Missing English translations:\n" + String.join("\n", missing));
        }
    }

    private String unescape(String value) {
        return value.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
'''
write('src/test/java/it/casiraghi/swiftbat/ui/TranslationCoverageTest.java', test)

# Clean one-shot files after successful Maven verification (the running workflow
# already has them loaded, so deleting them here is safe for the final commit).
Path('scripts/finish_supervisor_feedback.py').unlink(missing_ok=True)
Path('.github/workflows/finish-supervisor-feedback.yml').unlink(missing_ok=True)

print('Strict I18n batch 1 applied')
