package it.casiraghi.swiftbat.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Risultati spettroscopici pre-calcolati e pubblicati dal catalogo Swift/BAT.
 *
 * <p>I valori non sono ottenuti dalle curve di luce a quattro canali: derivano
 * dai fit XSPEC ufficiali e conservano i limiti di confidenza al 90% riportati
 * nelle tabelle NASA/GSFC.</p>
 */
public record SpectralData(
        String grbName,
        String triggerId,
        Map<Interval, Result> results) {

    public SpectralData {
        results = results == null ? Map.of() : Map.copyOf(results);
    }

    public Result result(Interval interval) {
        return results.get(interval);
    }

    public boolean hasOfficialResults() {
        return results.values().stream().anyMatch(Result::available);
    }

    public enum Interval {
        T100("T100", "Intero intervallo spettroscopico T100"),
        PEAK_ONE_SECOND("Picco 1 s", "Secondo attorno al picco del burst");

        private final String label;
        private final String description;

        Interval(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Model {
        POWER_LAW("PL", "Power law"),
        CUTOFF_POWER_LAW("CPL", "Cutoff power law");

        private final String code;
        private final String label;

        Model(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String code() {
            return code;
        }

        public String label() {
            return label;
        }

        public static Model fromCode(String value) {
            if (value == null) return null;
            return switch (value.trim().toUpperCase(Locale.ROOT)) {
                case "PL" -> POWER_LAW;
                case "CPL" -> CUTOFF_POWER_LAW;
                default -> null;
            };
        }
    }

    public record Result(
            Interval interval,
            String bestModelCode,
            Fit powerLaw,
            Fit cutoffPowerLaw,
            List<EnergyFluxBand> powerLawFluxes,
            List<EnergyFluxBand> cutoffPowerLawFluxes) {

        public Result {
            bestModelCode = bestModelCode == null || bestModelCode.isBlank()
                    ? "N/A" : bestModelCode.trim().toUpperCase(Locale.ROOT);
            powerLawFluxes = powerLawFluxes == null ? List.of() : List.copyOf(powerLawFluxes);
            cutoffPowerLawFluxes = cutoffPowerLawFluxes == null ? List.of() : List.copyOf(cutoffPowerLawFluxes);
        }

        public Model bestModel() {
            return Model.fromCode(bestModelCode);
        }

        public Fit fit(Model model) {
            return model == Model.CUTOFF_POWER_LAW ? cutoffPowerLaw : powerLaw;
        }

        public List<EnergyFluxBand> fluxes(Model model) {
            return model == Model.CUTOFF_POWER_LAW ? cutoffPowerLawFluxes : powerLawFluxes;
        }

        public boolean available() {
            return available(Model.POWER_LAW) || available(Model.CUTOFF_POWER_LAW);
        }

        public boolean available(Model model) {
            Fit fit = fit(model);
            return (fit != null && fit.canPlot()) || fluxes(model).stream().anyMatch(EnergyFluxBand::available);
        }
    }

    public record Fit(
            Model model,
            Double alpha,
            Double alphaLow,
            Double alphaHigh,
            Double ePeakKeV,
            Double ePeakLowKeV,
            Double ePeakHighKeV,
            Double normalization,
            Double normalizationLow,
            Double normalizationHigh,
            Double chiSquare,
            Integer degreesOfFreedom,
            Double reducedChiSquare,
            Double nullProbability,
            Double normalizationEnergyKeV,
            Double exposureSeconds,
            Double spectrumStartSeconds,
            Double spectrumStopSeconds) {

        public boolean canPlot() {
            return model != null && alpha != null && Double.isFinite(alpha)
                    && normalization != null && Double.isFinite(normalization) && normalization > 0
                    && (model != Model.CUTOFF_POWER_LAW
                    || (ePeakKeV != null && Double.isFinite(ePeakKeV) && ePeakKeV > 0));
        }

        public boolean hasConstrainedEPeak() {
            return model == Model.CUTOFF_POWER_LAW
                    && ePeakKeV != null && ePeakKeV > 0 && ePeakKeV < 9_000
                    && ePeakLowKeV != null && ePeakLowKeV > 0
                    && ePeakHighKeV != null && ePeakHighKeV > ePeakLowKeV;
        }
    }

    /** Flusso energetico integrato nella banda, in erg cm^-2 s^-1. */
    public record EnergyFluxBand(
            String label,
            double lowerEnergyKeV,
            double upperEnergyKeV,
            Double value,
            Double lower90,
            Double upper90) {

        public double centerEnergyKeV() {
            return Math.sqrt(lowerEnergyKeV * upperEnergyKeV);
        }

        public boolean available() {
            return value != null && Double.isFinite(value) && value > 0;
        }
    }
}
