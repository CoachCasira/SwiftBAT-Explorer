package it.casiraghi.swiftbat.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrbSearchAssistTest {

    @Test
    void protectsAndNormalizesTheGrbPrefix() {
        assertEquals("GRB", GrbSearchAssist.normalize(null));
        assertEquals("GRB", GrbSearchAssist.normalize("  "));
        assertEquals("GRB250605A", GrbSearchAssist.normalize("250605a"));
        assertEquals("GRB250605A", GrbSearchAssist.normalize("grb250605a"));
    }

    @Test
    void returnsOnlyDistinctPrefixMatchesInStableOrder() {
        List<String> names = List.of("GRB250603A", "grb250605a", "GRB250605A", "OTHER");

        assertEquals(
                List.of("GRB250605A", "GRB250603A"),
                GrbSearchAssist.matchingNames(names, "GRB2506"));
        assertEquals(List.of("GRB250605A"),
                GrbSearchAssist.matchingNames(names, "GRB250605A"));
    }
}
