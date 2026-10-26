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
        Double t90Sec,
        RedshiftInfo redshift) {

    public SkyBurst {
        grbName = normalizeName(grbName);
        triggerId = triggerId == null ? "" : triggerId.trim();
        // Unknown coordinates are represented by a NaN pair, never 0,0.
        boolean unknown = Double.isNaN(raDeg) && Double.isNaN(decDeg);
        if (!unknown && (!Double.isFinite(raDeg) || raDeg < 0.0 || raDeg >= 360.0
                || !Double.isFinite(decDeg) || decDeg < -90.0 || decDeg > 90.0)) {
            throw new IllegalArgumentException("Coordinate RA/DEC non valide: " + raDeg + ", " + decDeg);
        }
        if (t90Sec != null && (!Double.isFinite(t90Sec) || t90Sec < 0.0)) {
            t90Sec = null;
        }
        redshift = redshift == null ? RedshiftInfo.missing() : redshift;
    }

    public SkyBurst(String grbName, String triggerId, double raDeg, double decDeg, Double t90Sec) {
        this(grbName, triggerId, raDeg, decDeg, t90Sec, RedshiftInfo.missing());
    }

    public SkyBurst withRedshift(RedshiftInfo value) {
        return new SkyBurst(grbName, triggerId, raDeg, decDeg, t90Sec, value);
    }

    public static SkyBurst withoutCoordinates(String grbName, String triggerId) {
        return new SkyBurst(grbName, triggerId, Double.NaN, Double.NaN, null);
    }

    public boolean hasCoordinates() {
        return Double.isFinite(raDeg) && Double.isFinite(decDeg);
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
