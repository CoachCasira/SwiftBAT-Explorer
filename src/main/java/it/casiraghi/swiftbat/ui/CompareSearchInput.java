package it.casiraghi.swiftbat.ui;

import java.util.Locale;

/**
 * Search input rules shared by both editable GRB selectors in Compare.
 * Unlike the historical fixed-prefix formatter, deletion is unrestricted.
 */
final class CompareSearchInput {
    private CompareSearchInput() {
    }

    static boolean isEditable(String text) {
        // Empty and partial prefixes must be allowed while the user edits.
        // Names are normalized only when looking up a scientific event.
        return text != null && text.matches("[A-Z0-9]*");
    }

    static String normalize(String raw) {
        String text = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if ("GRB".startsWith(text)) return "GRB";
        return text.startsWith("GRB") ? text : "GRB" + text;
    }
}
