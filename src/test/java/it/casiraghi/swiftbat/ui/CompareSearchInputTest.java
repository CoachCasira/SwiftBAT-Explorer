package it.casiraghi.swiftbat.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompareSearchInputTest {
    @Test
    void fullDeletionAndPartialPrefixAreValidEdits() {
        assertTrue(CompareSearchInput.isEditable(""));
        assertTrue(CompareSearchInput.isEditable("G"));
        assertTrue(CompareSearchInput.isEditable("GR"));
        assertTrue(CompareSearchInput.isEditable("GRB"));
        assertTrue(CompareSearchInput.isEditable("GRB250605A"));
        assertTrue(CompareSearchInput.isEditable("250605A"));
        assertTrue(CompareSearchInput.isEditable("RB250605A"));
        assertFalse(CompareSearchInput.isEditable("GRB250605-"));
        assertFalse(CompareSearchInput.isEditable("GRB 250605"));
    }

    @Test
    void normalizesEmptyAndPartialInputsWithoutBlockingBackspace() {
        assertEquals("GRB", CompareSearchInput.normalize(""));
        assertEquals("GRB", CompareSearchInput.normalize("G"));
        assertEquals("GRB", CompareSearchInput.normalize("GR"));
        assertEquals("GRB", CompareSearchInput.normalize("GRB"));
        assertEquals("GRB250605A", CompareSearchInput.normalize("250605a"));
        assertEquals("GRB250605A", CompareSearchInput.normalize("grb250605a"));
        assertEquals("GRB250605A", CompareSearchInput.normalize(" 250605a "));
    }

    @Test
    void deletingFromStartOrEndDoesNotRequireAnUnremovablePrefix() {
        String original = "GRB250605A";
        assertTrue(CompareSearchInput.isEditable(original.substring(1)));
        assertTrue(CompareSearchInput.isEditable(original.substring(0, 3)));
        assertTrue(CompareSearchInput.isEditable(""));
        assertEquals("GRB", CompareSearchInput.normalize(""));
        assertEquals("GRB240101B", CompareSearchInput.normalize("240101b"));
    }
}
