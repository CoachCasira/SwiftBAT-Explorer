package it.casiraghi.swiftbat.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopulationInsightServiceTest {
    private final PopulationInsightService service = new PopulationInsightService();

    @Test
    void identifiesMedianPeakAndPostTriggerTail() {
        var points = List.of(
                new CumulativeAnalysisService.Point(-2, 0.1),
                new CumulativeAnalysisService.Point(-1, 0.2),
                new CumulativeAnalysisService.Point(0, 1.0),
                new CumulativeAnalysisService.Point(1, 0.8),
                new CumulativeAnalysisService.Point(2, 0.6),
                new CumulativeAnalysisService.Point(3, 0.4));
        var lower = points.stream().map(point ->
                new CumulativeAnalysisService.Point(point.time(), point.value() - 0.1)).toList();
        var upper = points.stream().map(point ->
                new CumulativeAnalysisService.Point(point.time(), point.value() + 0.1)).toList();
        var profile = new CumulativeAnalysisService.PopulationProfile(lower, points, upper);
        var curves = List.of(new CumulativeAnalysisService.NormalizedCurve("GRBTESTA", points));

        var narrative = service.analyze(curves, profile,
                List.of(new PopulationInsightService.EventFacts("GRBTESTA", 12.0, 1.2, 99.0)),
                1, 0, 20);

        assertEquals(0.0, narrative.diagnostics().medianPeakTime(), 1e-12);
        assertEquals(2.0, narrative.diagnostics().halfMaximumWidth(), 1e-12);
        assertTrue(narrative.diagnostics().prePostAsymmetry() > 0.2);
        assertTrue(narrative.observations().stream().anyMatch(text -> text.contains("coda post-trigger")));
    }

    @Test
    void reportsCompositionAndSmallSampleCaution() {
        var points = List.of(new CumulativeAnalysisService.Point(0, 1.0));
        var profile = new CumulativeAnalysisService.PopulationProfile(points, points, points);
        var curves = List.of(new CumulativeAnalysisService.NormalizedCurve("GRBTESTA", points));
        var events = List.of(
                new PopulationInsightService.EventFacts("A", 1.0, 0.5, 90.0),
                new PopulationInsightService.EventFacts("B", 10.0, null, 100.0),
                new PopulationInsightService.EventFacts("C", null, null, 80.0));

        var narrative = service.analyze(curves, profile, events, 3, 0, 20);

        assertEquals(1, narrative.diagnostics().shortCount());
        assertEquals(1, narrative.diagnostics().longCount());
        assertEquals(1, narrative.diagnostics().unknownT90Count());
        assertTrue(narrative.cautions().stream().anyMatch(text -> text.contains("Campione piccolo")));
    }
}
