package it.casiraghi.swiftbat.ui;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guardrail for the bilingual UI. The complete UI package is scanned, including
 * custom Java2D/3D renderers, so a new visible Italian literal cannot silently
 * remain untranslated.
 */
class TranslationCoverageTest {
    private static final Pattern ITALIAN_WORD = Pattern.compile(
            "(?iu)(?:^|[^\\p{L}])(?:il|lo|la|gli|le|un|una|di|del|della|dei|delle|è|con|senza|non|dal|nel|nella|nelle|"
                    + "mostra|nascondi|apri|scegli|cerca|curva|luce|dati|mappa|analisi|durata|tempo|valore|qualità|"
                    + "spiegazione|intervallo|flusso|modello|energia|banda|tabella|righe|esposizione|guida|informazioni|"
                    + "evento|eventi|confronto|schermo|descrizione|caricamento|campione|celeste|sessione|lettura|"
                    + "risultati|errore|picco|durezza|fonte|ufficiale|filtro|filtri|nessun|nessuna|tutte|tutti|ripristina|"
                    + "profilo|profondità|altezza|disponibilità|redshift|gradi|scelta|vincolato|incerto|visualizzati|"
                    + "caricati|ammesso|massimo|minimo|corrispondono|leggibili|sconosciuto|spettro|barre|asse|assi|"
                    + "trascina|rotella|centra|numero|secondo|secondi|estremo|scienza|strumenti|scorciatoie|"
                    + "catalogo|metadati|probabilità|frequenza|conteggi|limiti|unità|seleziona|punto|distribuzione)(?:$|[^\\p{L}])");

    @Test
    void everyUiItalianLiteralMustHaveEnglishTranslation() throws Exception {
        // Same dictionary configuration used by the real application startup.
        LegacyI18nBridge.install();

        Path root = Path.of("src/main/java/it/casiraghi/swiftbat/ui");
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
        if (lower.matches(".*[àèéìòù].*")) return true;
        return ITALIAN_WORD.matcher(value).find();
    }

    private boolean hasRuntimeEnglish(String value) {
        if (UiTranslations.hasEnglish(value)) return true;
        String legacy = I18n.english(value);
        if (isTranslation(value, legacy)) return true;

        String legacyColorSource = value
                .replace("linea azzurra", "linea arancione")
                .replace("curva azzurra", "curva arancione");
        if (!legacyColorSource.equals(value)) {
            String translated = I18n.english(legacyColorSource);
            if (isTranslation(legacyColorSource, translated)) return true;
        }
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
