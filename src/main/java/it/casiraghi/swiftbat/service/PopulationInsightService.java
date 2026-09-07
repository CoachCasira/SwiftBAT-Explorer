package it.casiraghi.swiftbat.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Produce un commento descrittivo e riproducibile su un'analisi di popolazione.
 *
 * <p>Non usa servizi esterni e non formula una classificazione astrofisica:
 * trasforma in frasi alcune statistiche calcolate esclusivamente sul campione
 * visualizzato.</p>
 */
public final class PopulationInsightService {

    public Narrative analyze(List<CumulativeAnalysisService.NormalizedCurve> curves,
                             CumulativeAnalysisService.PopulationProfile profile,
                             List<EventFacts> events,
                             int examined,
                             int failures,
                             double halfWindowSeconds) {
        List<CumulativeAnalysisService.NormalizedCurve> safeCurves = curves == null ? List.of() : curves;
        List<EventFacts> safeEvents = events == null ? List.of() : events;
        List<CumulativeAnalysisService.Point> median = profile == null || profile.median() == null
                ? List.of() : profile.median();

        if (safeCurves.isEmpty() || median.isEmpty()) {
            return new Narrative(
                    "Non ci sono curve sufficienti per descrivere il profilo temporale.",
                    List.of("Modifica i filtri oppure amplia il numero massimo di GRB da esaminare."),
                    List.of(),
                    Diagnostics.empty());
        }

        CumulativeAnalysisService.Point peak = median.stream()
                .filter(point -> Double.isFinite(point.value()))
                .max(Comparator.comparingDouble(CumulativeAnalysisService.Point::value))
                .orElse(new CumulativeAnalysisService.Point(Double.NaN, Double.NaN));
        double halfMaximumWidth = widthAboveFraction(median, peak.value() * 0.5);
        double preArea = positiveArea(median, true);
        double postArea = positiveArea(median, false);
        double asymmetry = normalizedDifference(postArea, preArea);
        double meanIqr = meanInterquartileWidth(profile);

        int shortCount = 0;
        int longCount = 0;
        int unknownT90 = 0;
        int redshiftCount = 0;
        List<Double> redshifts = new ArrayList<>();
        List<Double> coverage = new ArrayList<>();
        for (EventFacts event : safeEvents) {
            if (event.t90Seconds() == null || !Double.isFinite(event.t90Seconds())) {
                unknownT90++;
            } else if (event.t90Seconds() <= 2.0) {
                shortCount++;
            } else {
                longCount++;
            }
            if (event.redshift() != null && Double.isFinite(event.redshift())) {
                redshiftCount++;
                redshifts.add(event.redshift());
            }
            if (Double.isFinite(event.exposurePercent())) {
                coverage.add(event.exposurePercent());
            }
        }

        double medianCoverage = percentile(coverage, 0.5);
        double medianRedshift = percentile(redshifts, 0.5);
        double redshiftAvailability = safeEvents.isEmpty()
                ? Double.NaN : redshiftCount * 100.0 / safeEvents.size();

        List<String> observations = new ArrayList<>();
        observations.add(timingObservation(peak.time(), halfMaximumWidth, halfWindowSeconds));
        observations.add(asymmetryObservation(asymmetry));
        observations.add(dispersionObservation(meanIqr));
        observations.add(compositionObservation(safeEvents.size(), shortCount, longCount, unknownT90,
                redshiftCount, medianRedshift, medianCoverage));

        List<String> cautions = new ArrayList<>();
        if (safeCurves.size() < 10) {
            cautions.add("Campione piccolo: mediana e percentili possono cambiare molto aggiungendo pochi eventi.");
        }
        if (failures > 0) {
            cautions.add(failures + " dei " + examined
                    + " eventi esaminati non sono stati letti e non contribuiscono al profilo.");
        }
        if (Double.isFinite(redshiftAvailability) && redshiftAvailability < 50.0) {
            cautions.add(String.format(Locale.ITALY,
                    "Il redshift è disponibile solo per il %.0f%% del campione: la distribuzione z non è completa.",
                    redshiftAvailability));
        }
        cautions.add("Le curve sono divise per il proprio picco: il confronto riguarda la forma relativa, non la luminosità assoluta.");

        String headline = String.format(Locale.ITALY,
                "%d curve incluse: il profilo mediano raggiunge il massimo a t = %s s rispetto al trigger.",
                safeCurves.size(), formatSigned(peak.time()));
        Diagnostics diagnostics = new Diagnostics(peak.time(), peak.value(), halfMaximumWidth,
                asymmetry, meanIqr, shortCount, longCount, unknownT90,
                redshiftAvailability, medianRedshift, medianCoverage);
        return new Narrative(headline, List.copyOf(observations), List.copyOf(cautions), diagnostics);
    }

    private String timingObservation(double peakTime, double width, double halfWindowSeconds) {
        String position;
        if (Math.abs(peakTime) <= 1.0) {
            position = "in prossimità del trigger";
        } else if (peakTime < 0.0) {
            position = "prima del trigger";
        } else {
            position = "dopo il trigger";
        }
        String widthText = Double.isFinite(width)
                ? String.format(Locale.ITALY, "; resta sopra metà massimo per circa %.1f s", width)
                : "";
        String boundary = Math.abs(peakTime) >= halfWindowSeconds - 1.0
                ? " Il massimo cade sul bordo della finestra: prova una finestra più ampia." : "";
        return "Posizione temporale — Il massimo della mediana cade " + position
                + " (t = " + formatSigned(peakTime) + " s)" + widthText + "." + boundary;
    }

    private String asymmetryObservation(double asymmetry) {
        if (!Double.isFinite(asymmetry)) {
            return "Forma prima/dopo il trigger — Il segnale positivo non è sufficiente per stimare lo sbilanciamento.";
        }
        if (asymmetry > 0.20) {
            return "Forma prima/dopo il trigger — La mediana ha più area positiva dopo t = 0: nel campione selezionato prevale una coda post-trigger.";
        }
        if (asymmetry < -0.20) {
            return "Forma prima/dopo il trigger — La mediana ha più area positiva prima di t = 0: il profilo selezionato è sbilanciato verso il pre-trigger.";
        }
        return "Forma prima/dopo il trigger — Le aree positive prima e dopo t = 0 sono relativamente bilanciate.";
    }

    private String dispersionObservation(double meanIqr) {
        if (!Double.isFinite(meanIqr)) {
            return "Variabilità — I percentili disponibili non bastano per stimare la dispersione tra le curve.";
        }
        String level = meanIqr < 0.15 ? "contenuta" : meanIqr < 0.35 ? "moderata" : "elevata";
        return String.format(Locale.ITALY,
                "Variabilità — La fascia 25°–75° ha ampiezza media %.2f: contiene il 50%% centrale delle curve e indica una dispersione %s.",
                meanIqr, level);
    }

    private String compositionObservation(int total, int shortCount, int longCount, int unknownT90,
                                          int redshiftCount, double medianRedshift, double medianCoverage) {
        String zText = redshiftCount == 0
                ? "nessun redshift disponibile"
                : String.format(Locale.ITALY, "redshift per %d/%d eventi (mediana z = %.2f)",
                redshiftCount, total, medianRedshift);
        String coverageText = Double.isFinite(medianCoverage)
                ? String.format(Locale.ITALY, "copertura FRACEXP mediana %.1f%%", medianCoverage)
                : "copertura FRACEXP non stimabile";
        return String.format(Locale.ITALY,
                "Campione — %d short, %d long e %d senza T90; %s; %s.",
                shortCount, longCount, unknownT90, zText, coverageText);
    }

    private double widthAboveFraction(List<CumulativeAnalysisService.Point> points, double threshold) {
        if (!Double.isFinite(threshold) || threshold <= 0.0) {
            return Double.NaN;
        }
        double first = Double.NaN;
        double last = Double.NaN;
        for (CumulativeAnalysisService.Point point : points) {
            if (Double.isFinite(point.value()) && point.value() >= threshold) {
                if (!Double.isFinite(first)) {
                    first = point.time();
                }
                last = point.time();
            }
        }
        return Double.isFinite(first) && Double.isFinite(last) ? Math.max(0.0, last - first) : Double.NaN;
    }

    private double positiveArea(List<CumulativeAnalysisService.Point> points, boolean beforeTrigger) {
        double area = 0.0;
        for (CumulativeAnalysisService.Point point : points) {
            if ((beforeTrigger && point.time() < 0.0) || (!beforeTrigger && point.time() > 0.0)) {
                if (Double.isFinite(point.value())) {
                    area += Math.max(0.0, point.value());
                }
            }
        }
        return area;
    }

    private double normalizedDifference(double first, double second) {
        double total = first + second;
        return total > 0.0 ? (first - second) / total : Double.NaN;
    }

    private double meanInterquartileWidth(CumulativeAnalysisService.PopulationProfile profile) {
        if (profile == null) {
            return Double.NaN;
        }
        int length = Math.min(profile.lowerQuartile().size(), profile.upperQuartile().size());
        double total = 0.0;
        int count = 0;
        for (int index = 0; index < length; index++) {
            double lower = profile.lowerQuartile().get(index).value();
            double upper = profile.upperQuartile().get(index).value();
            if (Double.isFinite(lower) && Double.isFinite(upper)) {
                total += Math.max(0.0, upper - lower);
                count++;
            }
        }
        return count == 0 ? Double.NaN : total / count;
    }

    private double percentile(List<Double> values, double fraction) {
        if (values == null || values.isEmpty()) {
            return Double.NaN;
        }
        List<Double> sorted = values.stream().filter(Double::isFinite).sorted().toList();
        if (sorted.isEmpty()) {
            return Double.NaN;
        }
        double position = Math.max(0.0, Math.min(1.0, fraction)) * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double weight = position - lower;
        return sorted.get(lower) * (1.0 - weight) + sorted.get(upper) * weight;
    }

    private String formatSigned(double value) {
        if (!Double.isFinite(value)) {
            return "n.d.";
        }
        if (Math.abs(value) < 0.0005) {
            return "0,0";
        }
        return String.format(Locale.ITALY, "%+.1f", value);
    }

    public record EventFacts(String grbName, Double t90Seconds, Double redshift, double exposurePercent) {
    }

    public record Narrative(String headline, List<String> observations, List<String> cautions,
                            Diagnostics diagnostics) {
        public Narrative {
            observations = observations == null ? List.of() : List.copyOf(observations);
            cautions = cautions == null ? List.of() : List.copyOf(cautions);
            diagnostics = diagnostics == null ? Diagnostics.empty() : diagnostics;
        }
    }

    public record Diagnostics(double medianPeakTime, double medianPeakValue, double halfMaximumWidth,
                              double prePostAsymmetry, double meanInterquartileWidth,
                              int shortCount, int longCount, int unknownT90Count,
                              double redshiftAvailabilityPercent, double medianRedshift,
                              double medianExposurePercent) {
        public static Diagnostics empty() {
            return new Diagnostics(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    0, 0, 0, Double.NaN, Double.NaN, Double.NaN);
        }
    }
}
