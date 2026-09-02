package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.SkyBurst;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkyCatalogServiceTest {
    @Test
    void parsesRequiredColumnsFromWhitespaceTable() {
        String sample = """
                # GRBname Trig_ID Trig_time_met Trig_time_UTC RA_ground DEC_ground Image_position_err Image_SNR T90 T90_err
                GRB250605A 1321323 802000000 2025-06-05T00:00:00 120.500 -23.250 1.2 12.0 4.70 0.30
                GRB250603A 1320335 801000000 2025-06-03T00:00:00 42.000 11.500 1.0 10.0 n/a n/a
                """;
        List<SkyBurst> bursts = new SkyCatalogService().parseSummary(sample);
        assertEquals(2, bursts.size());
        SkyBurst first = bursts.stream().filter(b -> b.grbName().equals("GRB250605A")).findFirst().orElseThrow();
        assertEquals(120.5, first.raDeg(), 1e-12);
        assertEquals(-23.25, first.decDeg(), 1e-12);
        assertEquals(4.70, first.t90Sec(), 1e-12);
    }

    @Test
    void ignoresRowsWithoutValidCoordinates() {
        String sample = "GRB250605A 1321323 0 UTC n/a -23.0 1 2 3.0";
        assertTrue(new SkyCatalogService().parseSummary(sample).isEmpty());
    }
}
