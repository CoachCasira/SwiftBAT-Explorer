package it.casiraghi.swiftbat.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TablePreferencesTest {
    @Test
    void scientificAndQualifiedNumbersAreRecognizedForRightAlignment() {
        assertTrue(TablePreferences.isNumeric("12,300 s"));
        assertTrue(TablePreferences.isNumeric("z = <0.5"));
        assertTrue(TablePreferences.isNumeric("0.5–1.2"));
        assertTrue(TablePreferences.isNumeric("1.2 ± 0.3"));
        assertTrue(TablePreferences.isNumeric("1.2 × 10⁻⁸"));
        assertTrue(TablePreferences.isNumeric("42%"));
    }

    @Test
    void identifiersDescriptionsAndDatesRemainTextual() {
        assertFalse(TablePreferences.isNumeric("GRB250605A"));
        assertFalse(TablePreferences.isNumeric("Short (T90 ≤ 2 s)"));
        assertFalse(TablePreferences.isNumeric("n.d."));
        assertFalse(TablePreferences.isNumeric("2025-06-05"));
    }
}
