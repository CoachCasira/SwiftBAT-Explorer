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
    @Test
    void tabCompletedSelectionMustNotReappearAfterDeletingFinalCharacter() {
        String committedByTab = "GRB250605A";
        // Cursor at the end, one Backspace. The old ComboBox selection is
        // still GRB250605A but the visible input is now GRB250605.
        String edited = committedByTab.substring(0, committedByTab.length() - 1);
        assertTrue(CompareSearchInput.isEditable(edited));
        assertFalse(CompareSearchInput.matchesSelection(committedByTab, edited));
        assertTrue(CompareSearchInput.matchesSelection(committedByTab, committedByTab));

        // Also cover deleting a different character and full Ctrl+A deletion.
        assertFalse(CompareSearchInput.matchesSelection(committedByTab, "GRB25060A"));
        assertFalse(CompareSearchInput.matchesSelection(committedByTab, ""));
        assertTrue(CompareSearchInput.matchesSelection(committedByTab, "250605a"));
    }

    @Test
    void originalPrefixRemainsWhileLastCharacterCanBeDeleted() {
        assertTrue(CompareSearchInput.hasFixedPrefix("GRB"));
        assertTrue(CompareSearchInput.hasFixedPrefix("GRB250605"));
        assertTrue(CompareSearchInput.hasFixedPrefix("GRB250605A"));
        assertFalse(CompareSearchInput.hasFixedPrefix("250605A"));
        assertFalse(CompareSearchInput.hasFixedPrefix("RB250605A"));
        assertFalse(CompareSearchInput.hasFixedPrefix(""));
        assertFalse(CompareSearchInput.hasFixedPrefix("GRB250605-"));
        String completedByTab = "GRB250605A";
        String afterOneBackspace = completedByTab.substring(0, completedByTab.length() - 1);
        assertTrue(CompareSearchInput.hasFixedPrefix(afterOneBackspace));
        assertFalse(CompareSearchInput.matchesSelection(completedByTab, afterOneBackspace));
    }

}
