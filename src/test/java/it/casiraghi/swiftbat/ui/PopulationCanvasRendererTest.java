package it.casiraghi.swiftbat.ui;

import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PopulationCanvasRendererTest {

    @Test
    void ordinaryTwentyFiveGrbAnalysisUsesCanvasInsteadOfFullJavaFxPaths() {
        assertTrue(PopulationCanvasRenderer.MIN_CANVAS_CURVES <= 25,
                "A 25-GRB analysis must not create thousands of JavaFX PathElements");
        assertTrue(PopulationCanvasRenderer.MIN_CANVAS_CURVES >= 1);
    }

    @Test
    void proxyPreservesTrueExtremaWhileKeepingOnlyTwoAxisSamples() {
        List<XYChart.Data<Number, Number>> scientific = new ArrayList<>();
        for (int i = -60; i <= 60; i++) {
            double flux = i == 12 ? 9.5 : i == -13 ? -2.25 : Math.sin(i / 15.0);
            scientific.add(new XYChart.Data<>(i, flux));
        }

        XYChart.Series<Number, Number> proxy =
                PopulationCanvasRenderer.proxy("GRB250605A", scientific);
        assertEquals("GRB250605A", proxy.getName());
        assertEquals(121, scientific.size(), "Original sample collection must stay unchanged");
        assertEquals(2, proxy.getData().size(), "A JavaFX proxy needs extrema, not all 121 samples");
        double lowest = proxy.getData().stream().mapToDouble(p -> p.getYValue().doubleValue())
                .min().orElseThrow();
        double highest = proxy.getData().stream().mapToDouble(p -> p.getYValue().doubleValue())
                .max().orElseThrow();
        assertEquals(-2.25, lowest, 1e-10);
        assertEquals(9.5, highest, 1e-10);
    }
}
