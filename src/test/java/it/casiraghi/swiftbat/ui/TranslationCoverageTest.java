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
            "(?iu)(?:^|[^\\p{L}])(?:il|lo|la|gli|le|un|una|di|del|della|dei|delle|e|è|per|con|senza|non|dal|nel|nella|nelle|"
                    + "mostra|nascondi|apri|scegli|cerca|curva|curve|luce|dati|mappa|analisi|durata|tempo|valore|qualità|"
                    + "spiegazione|intervallo|flusso|modello|energia|banda|tabella|righe|esposizione|guida|informazioni|"
                    + "evento|eventi|confronto|schermo|descrizione|caricamento|campione|coordinate|celeste|sessione|lettura|"
                    + "risultati|errore|picco|durezza|fonte|ufficiale|filtro|filtri|nessun|nessuna|tutte|tutti|ripristina|"
                    + "profilo|profondità|altezza|disponibilità|redshift|gradi|scelta|vincolato|incerto|visualizzati|"
                    + "caricati|ammesso|massimo|minimo|corrispondono|leggibili|sconosciuto|spettro|barre|asse|assi|"
                    + "trascina|rotella|centra|numero|secondo|secondi|binning|estremo|scienza|strumenti|scorciatoie|"
                    + "catalogo|metadati|probabilità|frequenza|conteggi|limiti|unità|seleziona|punto|distribuzione)(?:$|[^\\p{L}])");

    @Test
    void everyUiItalianLiteralMustHaveEnglishTranslation() throws Exception {
        Path root = Path.of("src/main/java/it/casiraghi/swiftbat/ui");
        List<String> missing = new ArrayList<>();

        try (var files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("I18n.java"))
                    .filter(path -> !path.getFileName().toString().equals("UiTranslations.java"))
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
                if (looksItalian(value) && !UiTranslations.hasEnglish(value)) {
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
