package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.SummaryItem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formattazione esclusivamente visiva dei valori di sintesi.
 * I dati scientifici nelle tabelle restano invariati; qui si limita soltanto
 * il numero di cifre mostrato nell'interfaccia.
 */
public final class DisplayFormat {
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    private DisplayFormat() {
    }

    public static String summary(String key, SummaryItem item) {
        if (item == null) {
            return "n.d.";
        }
        String value = value(key, item.value());
        if (item.unit() == null || item.unit().isBlank()) {
            return value;
        }
        String unit = I18n.t(item.unit());
        return value + (item.unit().equals("%") ? "%" : " " + unit);
    }

    public static String value(String key, String rawValue) {
        if (rawValue == null || rawValue.isBlank() || rawValue.equalsIgnoreCase("n.d.")) {
            return "n.d.";
        }

        if (rawValue.contains("→")) {
            String[] parts = rawValue.split("→", -1);
            if (parts.length == 2) {
                return format(parts[0].trim(), decimalsFor(key)) + " → "
                        + format(parts[1].trim(), decimalsFor(key));
            }
        }

        return format(rawValue.trim(), decimalsFor(key));
    }

    private static int decimalsFor(String key) {
        if (key == null) {
            return 4;
        }
        return switch (key) {
            case "PEAK_TIME" -> 3;
            case "PEAK_RATE", "PEAK_ERROR", "MEAN_RATE", "RATE_STD" -> 4;
            case "PEAK_SNR", "FULL_EXPOSURE_FRACTION", "NEGATIVE_FRACTION" -> 2;
            case "HARDNESS_PROXY", "MIN_FRACEXP", "BIN_SIZE", "TIME_RANGE" -> 3;
            case "ASCII_ROWS", "FITS_ROWS" -> 0;
            default -> 4;
        };
    }

    private static String format(String raw, int decimals) {
        try {
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value)) {
                return raw;
            }
            DecimalFormat formatter = new DecimalFormat(pattern(decimals), SYMBOLS);
            formatter.setGroupingUsed(false);
            return formatter.format(value);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private static String pattern(int decimals) {
        if (decimals <= 0) {
            return "0";
        }
        return "0." + "#".repeat(decimals);
    }
}
