package it.casiraghi.swiftbat.ui;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guardrail for the bilingual UI. Every Italian string literal in the UI layer
 * that looks user-visible must have an English mapping in I18n.
 *
 * <p>The scan is recursive and includes custom Java2D/3D renderers, so a new
 * label cannot silently remain in Italian just because it is not a JavaFX Label.</p>
 */
class TranslationCoverageTest {
    @Test
    void everyUiItalianLiteralMustHaveEnglishTranslation() throws Exception {
        Path root = Path.of("src/main/java/it/casiraghi/swiftbat/ui");
        List<String> missing = new ArrayList<>();

        try (var files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("I18n.java"))
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
                if (I18n.requiresTranslation(value) && !I18n.hasEnglish(value)) {
                    missing.add(path.getFileName() + ":" + lineOf(source, literal.offset()) + " -> " + value);
                }
            }
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
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
