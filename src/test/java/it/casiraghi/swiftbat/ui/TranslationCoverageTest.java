package it.casiraghi.swiftbat.ui;

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
