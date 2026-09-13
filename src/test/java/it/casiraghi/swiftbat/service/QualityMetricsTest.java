package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.TabularData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QualityMetricsTest {
    @Test
    void computesPerBurstPercentageOfCompleteBins() {
        TabularData fits = new TabularData(
                List.of("TIME", "FRACEXP"),
                List.of(
                        List.of("0", "1"),
                        List.of("1", "0.999"),
                        List.of("2", "0.75"),
                        List.of("3", "bad")));
        assertEquals(200.0 / 3.0, QualityMetrics.fullExposurePercent(fits), 1e-12);
    }

    @Test
    void interpolatesPercentiles() {
        assertEquals(1.75, QualityMetrics.percentile(List.of(1.0, 2.0, 3.0, 4.0), 0.25), 1e-12);
        assertEquals(2.5, QualityMetrics.percentile(List.of(1.0, 2.0, 3.0, 4.0), 0.50), 1e-12);
    }
}
