package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.SpectralData.Interval;
import it.casiraghi.swiftbat.model.SpectralData.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectralCatalogServiceTest {
    @Test
    void mergesOfficialModelsParametersAndEnergyFluxes() {
        String best = """
                ## GRBname | Trig_ID | Best-fit model
                GRB250605A | 1321323 | N/A
                GRB250530C | 1319125 | PL
                """;
        String powerLaw = """
                ## GRBname | Trig_ID | alpha | alpha_low | alpha_hi | norm | norm_low | norm_hi | chi2 | dof | reduced_chi2 | null_prob | enorm | Exposure_time | Spectrum_start | Spectrum_stop | comment
                GRB250605A | 1321323 | -1.79818 | -1.84845 | -1.74808 | 5.77844e-03 | 5e-03 | 6e-03 | 77.92 | 57 | 1.36702 | 3.43e-02 | 50 | 216.8 | -100.284 | 116.504 |
                GRB250530C | 1319125 | -1.33320 | -1.54967 | -1.11438 | 1.0179e-03 | 8e-04 | 1.2e-03 | 63.68 | 57 | 1.11719 | 2.53e-01 | 50 | 87.7 | -29.736 | 57.960 |
                """;
        String cutoff = """
                ## GRBname | Trig_ID | alpha | alpha_low | alpha_hi | Epeak | Epeak_low | Epeak_hi | norm | norm_low | norm_hi | chi2 | dof | reduced_chi2 | null_prob | enorm | Exposure_time | Spectrum_start | Spectrum_stop | comment
                GRB250605A | 1321323 | -1.48745 | -1.69525 | -1.26915 | 84.5258 | 68.7251 | 148.755 | 8.17179e-03 | 6e-03 | 1e-02 | 71.30 | 56 | 1.27321 | 8.18e-02 | 50 | 216.8 | -100.284 | 116.504 |
                """;
        String plFlux = """
                ## GRBname | Trig_ID | 15_25kev | 15_25kev_low | 15_25kev_hi | 25_50kev | 25_50kev_low | 25_50kev_hi | 50_100kev | 50_100kev_low | 50_100kev_hi | 100_150kev | 100_150kev_low | 100_150kev_hi | Exposure_time | Spectrum_start | Spectrum_stop | comment
                GRB250605A | 1321323 | 9.7e-09 | 9.2e-09 | 1.0e-08 | 1.4e-08 | 1.3e-08 | 1.5e-08 | 1.7e-08 | 1.6e-08 | 1.8e-08 | 1.1e-08 | 1.0e-08 | 1.2e-08 | 216.8 | -100.284 | 116.504 |
                GRB250530C | 1319125 | 1.0 | 1.0 | 1.0 | 2e-09 | 1e-09 | 3e-09 | 4e-09 | 3e-09 | 5e-09 | 3e-09 | 2e-09 | 4e-09 | 87.7 | -29.736 | 57.960 |
                """;
        String cplFlux = """
                ## GRBname | Trig_ID | 15_25kev | 15_25kev_low | 15_25kev_hi | 25_50kev | 25_50kev_low | 25_50kev_hi | 50_100kev | 50_100kev_low | 50_100kev_hi | 100_150kev | 100_150kev_low | 100_150kev_hi | Exposure_time | Spectrum_start | Spectrum_stop | comment
                GRB250605A | 1321323 | 8e-09 | 7e-09 | 9e-09 | 1.5e-08 | 1.4e-08 | 1.6e-08 | 2e-08 | 1.8e-08 | 2.2e-08 | 9e-09 | 8e-09 | 1e-08 | 216.8 | -100.284 | 116.504 |
                """;

        var parsed = SpectralCatalogService.parseInterval(
                Interval.T100, best, powerLaw, cutoff, plFlux, cplFlux);

        assertEquals(2, parsed.results().size());
        assertEquals("1321323", parsed.triggerIds().get("GRB250605A"));
        var result = parsed.results().get("GRB250605A");
        assertNotNull(result);
        assertNull(result.bestModel());
        assertEquals("N/A", result.bestModelCode());
        assertEquals(-1.79818, result.powerLaw().alpha(), 1e-10);
        assertEquals(57, result.powerLaw().degreesOfFreedom());
        assertEquals(84.5258, result.cutoffPowerLaw().ePeakKeV(), 1e-10);
        assertTrue(result.cutoffPowerLaw().hasConstrainedEPeak());
        assertEquals(4, result.powerLawFluxes().size());
        assertEquals("100–150 keV", result.powerLawFluxes().get(3).label());
        assertEquals(Model.POWER_LAW, parsed.results().get("GRB250530C").bestModel());
        assertEquals(3, parsed.results().get("GRB250530C").powerLawFluxes().size(),
                "Il segnaposto 1.0 del catalogo non deve essere mostrato come flusso fisico");
    }

    @Test
    void leavesMissingCutoffDataExplicitInsteadOfInventingAResult() {
        String best = "GRBTESTA | 123 | PL";
        String powerLaw = """
                ## GRBname | Trig_ID | alpha | alpha_low | alpha_hi | norm | norm_low | norm_hi | chi2 | dof | reduced_chi2 | null_prob | enorm | Exposure_time | Spectrum_start | Spectrum_stop | comment
                TESTA | 123 | -2 | -2.2 | -1.8 | 0.01 | 0.009 | 0.011 | 20 | 18 | 1.11 | 0.3 | 50 | 10 | -2 | 8 |
                """;

        var parsed = SpectralCatalogService.parseInterval(
                Interval.PEAK_ONE_SECOND, best, powerLaw, "", "", "");
        var result = parsed.results().get("GRBTESTA");
        assertNotNull(result.powerLaw());
        assertNull(result.cutoffPowerLaw());
        assertFalse(result.powerLawFluxes().iterator().hasNext());
    }
}
