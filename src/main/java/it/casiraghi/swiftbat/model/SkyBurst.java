package it.casiraghi.swiftbat.model;

import java.util.Locale;

/**
 * Posizione celeste e durata di un GRB provenienti dalla tabella generale
 * ufficiale Swift/BAT. RA e DEC sono espresse in gradi (J2000).
 */
public record SkyBurst(
        String grbName,
        String triggerId,
        double raDeg,
        double decDeg,
        Double t90Sec) {

    public SkyBurst {
        grbName = normalizeName(grbName);
        triggerId = triggerId == null ? "" : triggerId.trim();
        if (!Double.isFinite(raDeg) || raDeg < 0.0 || raDeg >= 360.0) {
            throw new IllegalArgumentException("RA fuori intervallo: " + raDeg);
        }
        if (!Double.isFinite(decDeg) || decDeg < -90.0 || decDeg > 90.0) {
            throw new IllegalArgumentException("DEC fuori intervallo: " + decDeg);
        }
        if (t90Sec != null && (!Double.isFinite(t90Sec) || t90Sec < 0.0)) {
            t90Sec = null;
        }
    }

    public boolean hasT90() {
        return t90Sec != null;
    }

    public boolean isShort() {
        return t90Sec != null && t90Sec <= 2.0;
    }

    public boolean isLong() {
        return t90Sec != null && t90Sec > 2.0;
    }

    public String durationClass() {
        if (t90Sec == null) {
            return "T90 non disponibile";
        }
        return isShort() ? "Short (T90 ≤ 2 s)" : "Long (T90 > 2 s)";
    }

    public String formattedT90() {
        return t90Sec == null ? "n.d." : String.format(Locale.ITALY, "%.3f s", t90Sec);
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String name = value.trim().toUpperCase(Locale.ROOT);
        return name.startsWith("GRB") ? name : "GRB" + name;
    }
}
