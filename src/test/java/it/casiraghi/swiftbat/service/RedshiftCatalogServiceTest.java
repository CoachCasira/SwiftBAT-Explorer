package it.casiraghi.swiftbat.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedshiftCatalogServiceTest {
    @Test
    void preservesAndParsesExactLimitsRangesAndAlternatives() {
        String sample = """
                ## GRBname | z | Method | Uncertainty | Ref.
                GRB250430A | 0.767 | ba | N/A | Reference A
                GRB250207A | >1.3 | UVOT limit | N/A | Reference B
                GRB231111A | 1.179<z<2.0 | b | N/A | Reference C
                GRB211211A | 0.076 or 0.459 | hp | N/A | Reference D
                GRB250108B | 2.197? | ba | N/A | Reference E
                """;

        var values = new RedshiftCatalogService().parseRedshifts(sample);
        assertEquals(5, values.size());
        assertTrue(values.get("GRB250430A").exact());
        assertEquals(0.767, values.get("GRB250430A").representativeValue(), 1e-12);

        var lowerLimit = values.get("GRB250207A");
        assertEquals(1.3, lowerLimit.lowerBound(), 1e-12);
        assertNull(lowerLimit.upperBound());
        assertTrue(lowerLimit.matches(2.0, 3.0));

        var range = values.get("GRB231111A");
        assertEquals(1.179, range.lowerBound(), 1e-12);
        assertEquals(2.0, range.upperBound(), 1e-12);
        assertTrue(range.matches(1.8, 2.2));
        assertFalse(range.matches(2.1, 3.0));

        var alternatives = values.get("GRB211211A");
        assertEquals(2, alternatives.alternatives().size());
        assertFalse(alternatives.matches(0.2, 0.3));
        assertTrue(alternatives.matches(0.4, 0.5));
        assertTrue(values.get("GRB250108B").uncertain());
    }
}
