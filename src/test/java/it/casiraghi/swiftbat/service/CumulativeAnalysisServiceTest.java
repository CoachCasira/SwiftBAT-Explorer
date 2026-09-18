package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.ProductAvailability;
import it.casiraghi.swiftbat.model.TabularData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CumulativeAnalysisServiceTest {
    private final CumulativeAnalysisService service = new CumulativeAnalysisService();

    @Test
    void normalizesEveryCurveOnItsOwnPositivePeak() {
        var curve = service.normalize(data("GRBTESTA", 0, 4, 2), 20);
        assertFalse(curve.isEmpty());
        assertEquals(1.0, curve.points().get(1).value(), 1e-12);
        assertEquals(0.5, curve.points().get(2).value(), 1e-12);
    }

    @Test
    void buildsMedianAndQuartilesOnTriggerAlignedGrid() {
        var first = service.normalize(data("GRBTESTA", 0, 4, 2), 20);
        var second = service.normalize(data("GRBTESTB", 0, 2, 2), 20);
        var profile = service.profile(List.of(first, second), 1);
        assertEquals(3, profile.median().size());
        assertEquals(0.75, profile.median().get(2).value(), 1e-12);
    }

    private GrbData data(String name, double first, double peak, double last) {
        TabularData ascii = new TabularData(
                List.of("TIME_FROM_TRIGGER_CENTER_S", "RATE_15_350_KEV"),
                List.of(List.of("-1", Double.toString(first)),
                        List.of("0", Double.toString(peak)),
                        List.of("1", Double.toString(last))));
        return new GrbData(name, "1", Instant.EPOCH,
                new ProductAvailability(true, false, "a.dat", "", "", "", ""),
                List.of(), ascii, TabularData.empty(), List.of(), List.of());
    }
}
