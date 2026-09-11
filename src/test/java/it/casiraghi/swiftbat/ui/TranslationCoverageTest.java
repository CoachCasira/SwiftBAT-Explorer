package it.casiraghi.swiftbat.ui;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guardrail for the bilingual application. UI, model and service sources are
 * scanned, including custom Java2D/3D renderers and runtime status/error text,
 * so a new visible Italian literal cannot silently remain untranslated.
 */
class TranslationCoverageTest {
    private static final Pattern ITALIAN_WORD = Pattern.compile(
            "(?iu)(?:^|[^\\p{L}])(?:il|lo|la|gli|le|un|una|di|del|della|dei|delle|è|con|senza|non|dal|nel|nella|nelle|"
                    + "mostra|nascondi|apri|scegli|cerca|curva|luce|dati|mappa|analisi|durata|tempo|valore|qualità|"
                    + "spiegazione|intervallo|flusso|modello|energia|banda|tabella|righe|esposizione|guida|informazioni|"
                    + "evento|eventi|confronto|schermo|descrizione|caricamento|campione|celeste|sessione|lettura|"
                    + "risultati|errore|picco|durezza|fonte|ufficiale|filtro|filtri|nessun|nessuna|tutte|tutti|ripristina|"
                    + "profilo|profondità|altezza|disponibilità|gradi|scelta|vincolato|incerto|visualizzati|"
                    + "caricati|ammesso|massimo|minimo|corrispondono|leggibili|sconosciuto|spettro|barre|asse|assi|"
                    + "trascina|rotella|centra|numero|secondo|secondi|estremo|scienza|strumenti|scorciatoie|"
                    + "catalogo|metadati|probabilità|frequenza|conteggi|limiti|unità|seleziona|punto|distribuzione)(?:$|[^\\p{L}])");

    @Test
    void everyUiItalianLiteralMustHaveEnglishTranslation() throws Exception {
        // Same dictionary configuration used by the real application startup.
        LegacyI18nBridge.install();

        Path root = Path.of("src/main/java/it/casiraghi/swiftbat");
        List<String> missing = new ArrayList<>();

        try (var files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("I18n.java"))
                    .filter(path -> !path.getFileName().toString().equals("UiTranslations.java"))
                    .filter(path -> !path.getFileName().toString().equals("UiTranslationExtras.java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> scan(path, missing));
        }

        if (!missing.isEmpty()) {
            fail("Missing English translations in UI layer:\n" + String.join("\n", missing));
        }
    }

    @Test
    void criticalRuntimeTextsTranslateInBothDirections() {
        LegacyI18nBridge.install();
        Map<String, String> pairs = new LinkedHashMap<>();
        pairs.put("Apri il catalogo ufficiale", "Open official catalog");
        pairs.put("Dati pronti", "Data ready");
        pairs.put("Evento", "Event");
        pairs.put("Fonte", "Source");
        pairs.put("Non disponibile", "Unavailable");
        pairs.put("Flusso", "Flux");
        pairs.put("Schermo intero", "Fullscreen");
        pairs.put("Esporta PNG", "Export PNG");
        pairs.put("Esporta Excel", "Export Excel");
        pairs.put("Esportazione completata", "Export completed");
        pairs.put("File creato correttamente:", "File created successfully:");
        pairs.put("Prodotti 1 s non disponibili", "1-s products unavailable");
        pairs.put("Intero intervallo spettroscopico T100", "Full T100 spectral interval");
        pairs.put("Versione 1.3.0 · Java 17 · dati online", "Version 1.3.0 · Java 17 · online data");

        I18n.Language previous = I18n.language();
        try {
            I18n.setLanguage(I18n.Language.EN);
            pairs.forEach((italian, english) -> {
                String actual = UiTranslations.t(italian);
                if (!english.equals(actual)) {
                    fail("Wrong IT→EN translation: " + italian + " -> " + actual + " (expected " + english + ")");
                }
            });

            I18n.setLanguage(I18n.Language.IT);
            pairs.forEach((italian, english) -> {
                String actual = UiTranslations.t(english);
                if (!italian.equals(actual)) {
                    fail("Wrong EN→IT translation: " + english + " -> " + actual + " (expected " + italian + ")");
                }
            });
        } finally {
            I18n.setLanguage(previous);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyRegisteredDictionaryEntryResolvesInBothLanguages() throws Exception {
        LegacyI18nBridge.install();
        Field dictionary = I18n.class.getDeclaredField("EN");
        dictionary.setAccessible(true);
        Map<String, String> entries = (Map<String, String>) dictionary.get(null);

        I18n.Language previous = I18n.language();
        try {
            I18n.setLanguage(I18n.Language.EN);
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String actual = UiTranslations.t(entry.getKey());
                if (!entry.getValue().equals(actual)) {
                    fail("Dictionary IT→EN mismatch: " + entry.getKey() + " -> " + actual
                            + " (expected " + entry.getValue() + ")");
                }
            }

            I18n.setLanguage(I18n.Language.IT);
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                if (entry.getKey().equals(entry.getValue())) continue;
                String actual = UiTranslations.t(entry.getValue());
                if (entry.getValue().equals(actual)) {
                    fail("Dictionary EN→IT value remained untranslated: " + entry.getValue());
                }
            }
        } finally {
            I18n.setLanguage(previous);
        }
    }

    private void scan(Path path, List<String> missing) {
        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            for (Literal literal : stringLiterals(source)) {
                String value = unescape(literal.value());
                if (!looksItalian(value)) continue;
                if (codeOnlyLiteral(source, literal.offset())) continue;
                if (concatenationFragment(source, literal.offset())) continue;
                if (!hasRuntimeEnglish(value)) {
                    missing.add(path.getFileName() + ":" + lineOf(source, literal.offset()) + " -> " + value);
                }
            }
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    private boolean looksItalian(String value) {
        if (value == null || value.isBlank()) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http") || lower.matches("[a-z0-9_./-]+\\.(png|xlsx|fits|dat|lc)")) return false;
        if (lower.startsWith("among the four non-overlapping bands")) return false;
        if (lower.matches(".*[àèéìòù].*")) return true;
        return ITALIAN_WORD.matcher(value).find();
    }

    private boolean hasRuntimeEnglish(String value) {
        I18n.setLanguage(I18n.Language.EN);
        String translated = UiTranslations.t(value);
        if (!"[Missing English translation]".equals(translated) && !looksItalian(translated)) return true;
        System.err.println("TRANSLATION_GAP: " + value.replace('\n', ' ') + " => " + translated.replace('\n', ' '));
        return false;
    }

    private boolean isTranslation(String source, String translated) {
        return translated != null
                && !translated.equals(source)
                && !translated.equals("[Missing English translation]");
    }

    /** Literals used only to recognize/transform code are not user-visible copy. */
    private boolean codeOnlyLiteral(String source, int offset) {
        String line = lineAt(source, offset);
        return line.contains(".contains(")
                || line.contains(".startsWith(")
                || line.contains(".endsWith(")
                || line.contains(".replace(")
                || line.contains(".replaceFirst(")
                || line.contains(".indexOf(")
                || line.contains(".substring(")
                || line.contains(".toLowerCase(")
                || line.contains(".matches(");
    }

    /**
     * Java often splits one visible paragraph across adjacent string literals.
     * The compiler joins those fragments before they reach UiFactory/UiTranslations,
     * so the complete runtime string is what the localization layer translates.
     */
    private boolean concatenationFragment(String source, int offset) {
        int lineStart = source.lastIndexOf('\n', offset) + 1;
        int lineEnd = source.indexOf('\n', offset);
        if (lineEnd < 0) lineEnd = source.length();
        String current = source.substring(lineStart, lineEnd);

        int previousStart = lineStart <= 1 ? 0 : source.lastIndexOf('\n', lineStart - 2) + 1;
        String previous = source.substring(previousStart, Math.max(previousStart, lineStart - 1));
        int nextEnd = source.indexOf('\n', Math.min(source.length(), lineEnd + 1));
        if (nextEnd < 0) nextEnd = source.length();
        String next = lineEnd >= source.length() ? "" : source.substring(lineEnd + 1, nextEnd);
        return current.contains("+") || previous.contains("+") || next.contains("+");
    }

    private String lineAt(String source, int offset) {
        int start = source.lastIndexOf('\n', offset) + 1;
        int end = source.indexOf('\n', offset);
        if (end < 0) end = source.length();
        return source.substring(start, end);
    }

    /** Minimal Java lexer: extracts string literals while ignoring comments and char literals. */
    private List<Literal> stringLiterals(String source) {
        List<Literal> result = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                i += 2;
                while (i < source.length() && source.charAt(i) != '\n') i++;
                continue;
            }
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < source.length()
                        && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) i++;
                i = Math.min(source.length(), i + 2);
                continue;
            }
            if (c == '\'') {
                i++;
                while (i < source.length()) {
                    if (source.charAt(i) == '\\') i += 2;
                    else if (source.charAt(i++) == '\'') break;
                }
                continue;
            }
            if (c != '"') {
                i++;
                continue;
            }

            int start = i;
            i++;
            StringBuilder value = new StringBuilder();
            while (i < source.length()) {
                char current = source.charAt(i++);
                if (current == '"') break;
                if (current == '\\' && i < source.length()) {
                    value.append(current).append(source.charAt(i++));
                } else {
                    value.append(current);
                }
            }
            result.add(new Literal(start, value.toString()));
        }
        return result;
    }

    private int lineOf(String source, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) if (source.charAt(i) == '\n') line++;
        return line;
    }

    private String unescape(String value) {
        return value.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private record Literal(int offset, String value) { }
}
