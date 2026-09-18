package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.TabularData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Calcoli condivisi per la qualità di copertura derivata da FRACEXP. */
public final class QualityMetrics {
    public static final double COMPLETE_BIN_THRESHOLD = 0.999;

    private QualityMetrics() {
    }

    public static double fullExposurePercent(GrbData data) {
        return data == null ? Double.NaN : fullExposurePercent(data.fitsData());
    }

    public static double fullExposurePercent(TabularData fits) {
        if (fits == null || fits.isEmpty()) {
            return Double.NaN;
        }
        int index = fits.indexOf("FRACEXP");
        if (index < 0) {
            return Double.NaN;
        }
        int valid = 0;
        int complete = 0;
        for (List<String> row : fits.rows()) {
            if (index >= row.size()) {
                continue;
            }
            try {
                double value = Double.parseDouble(row.get(index));
                if (Double.isFinite(value)) {
                    valid++;
                    if (value >= COMPLETE_BIN_THRESHOLD) {
                        complete++;
                    }
                }
            } catch (NumberFormatException ignored) {
                // Riga non numerica: non entra nel denominatore.
            }
        }
        return valid == 0 ? Double.NaN : complete * 100.0 / valid;
    }

    public static double percentile(List<Double> values, double quantile) {
        List<Double> sorted = new ArrayList<>();
        if (values != null) {
            for (Double value : values) {
                if (value != null && Double.isFinite(value)) {
                    sorted.add(value);
                }
            }
        }
        if (sorted.isEmpty()) {
            return Double.NaN;
        }
        Collections.sort(sorted);
        double position = Math.max(0.0, Math.min(1.0, quantile)) * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double weight = position - lower;
        return sorted.get(lower) * (1.0 - weight) + sorted.get(upper) * weight;
    }
}
