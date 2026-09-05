package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.TabularData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Prepara curve normalizzate e profili robusti allineati al trigger t=0. */
public final class CumulativeAnalysisService {
    public NormalizedCurve normalize(GrbData data, double halfWindowSeconds) {
        if (data == null) {
            return new NormalizedCurve("", List.of());
        }
        TabularData table = data.asciiData().isEmpty() ? data.fitsData() : data.asciiData();
        int timeIndex = table.indexOf("TIME_FROM_TRIGGER_CENTER_S");
        int rateIndex = table.indexOf(data.asciiData().isEmpty() ? "RATE" : "RATE_15_350_KEV");
        if (timeIndex < 0 || rateIndex < 0) {
            return new NormalizedCurve(data.grbName(), List.of());
        }

        List<Point> raw = new ArrayList<>();
        double peak = Double.NEGATIVE_INFINITY;
        for (List<String> row : table.rows()) {
            double time = parse(row, timeIndex);
            double rate = parse(row, rateIndex);
            if (!Double.isFinite(time) || !Double.isFinite(rate) || Math.abs(time) > halfWindowSeconds) {
                continue;
            }
            raw.add(new Point(time, rate));
            peak = Math.max(peak, rate);
        }
        if (raw.isEmpty() || !Double.isFinite(peak) || peak <= 0.0) {
            return new NormalizedCurve(data.grbName(), List.of());
        }
        raw.sort(Comparator.comparingDouble(Point::time));
        double divisor = peak;
        return new NormalizedCurve(data.grbName(), raw.stream()
                .map(point -> new Point(point.time(), point.value() / divisor))
                .toList());
    }

    public PopulationProfile profile(List<NormalizedCurve> curves, double halfWindowSeconds) {
        List<Point> lower = new ArrayList<>();
        List<Point> median = new ArrayList<>();
        List<Point> upper = new ArrayList<>();
        int start = (int) Math.ceil(-halfWindowSeconds);
        int end = (int) Math.floor(halfWindowSeconds);
        for (int second = start; second <= end; second++) {
            List<Double> values = new ArrayList<>();
            for (NormalizedCurve curve : curves) {
                double value = interpolate(curve.points(), second);
                if (Double.isFinite(value)) {
                    values.add(value);
                }
            }
            if (values.isEmpty()) {
                continue;
            }
            lower.add(new Point(second, QualityMetrics.percentile(values, 0.25)));
            median.add(new Point(second, QualityMetrics.percentile(values, 0.50)));
            upper.add(new Point(second, QualityMetrics.percentile(values, 0.75)));
        }
        return new PopulationProfile(List.copyOf(lower), List.copyOf(median), List.copyOf(upper));
    }

    private double interpolate(List<Point> points, double time) {
        if (points == null || points.isEmpty() || time < points.get(0).time()
                || time > points.get(points.size() - 1).time()) {
            return Double.NaN;
        }
        Point previous = points.get(0);
        if (Double.compare(previous.time(), time) == 0) {
            return previous.value();
        }
        for (int index = 1; index < points.size(); index++) {
            Point next = points.get(index);
            if (Double.compare(next.time(), time) == 0) {
                return next.value();
            }
            if (next.time() > time) {
                double span = next.time() - previous.time();
                if (span <= 0.0 || span > 2.5) {
                    return Double.NaN;
                }
                double weight = (time - previous.time()) / span;
                return previous.value() * (1.0 - weight) + next.value() * weight;
            }
            previous = next;
        }
        return Double.NaN;
    }

    private double parse(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(row.get(index));
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    public record Point(double time, double value) {
    }

    public record NormalizedCurve(String grbName, List<Point> points) {
        public NormalizedCurve {
            points = points == null ? List.of() : List.copyOf(points);
        }

        public boolean isEmpty() {
            return points.isEmpty();
        }
    }

    public record PopulationProfile(List<Point> lowerQuartile, List<Point> median, List<Point> upperQuartile) {
    }
}
