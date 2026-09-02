package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.MollweidePoint;
import it.casiraghi.swiftbat.model.SkyPoint3D;

/**
 * Trasformazioni geometriche usate dalla mappa celeste. Non dipende da JavaFX.
 */
public final class SkyCoordinates {
    private static final double SQRT_2 = Math.sqrt(2.0);

    // Matrice ICRS -> Galactic (J2000), convenzione standard IAU.
    private static final double[][] ICRS_TO_GAL = {
            {-0.0548755604, -0.8734370902, -0.4838350155},
            {+0.4941094279, -0.4448296300, +0.7469822445},
            {-0.8676661490, -0.1980763734, +0.4559837762}
    };

    private SkyCoordinates() {
    }

    /**
     * Mollweide astronomica: RA cresce verso sinistra; RA=0 è al centro.
     * Il risultato è normalizzato nell'ellisse x∈[-2√2,2√2], y∈[-√2,√2].
     */
    public static MollweidePoint mollweide(double raDeg, double decDeg) {
        double lon = normalizeDegrees180(raDeg);
        double lambda = -Math.toRadians(lon);
        double phi = Math.toRadians(clamp(decDeg, -90.0, 90.0));

        double theta;
        if (Math.abs(Math.abs(phi) - Math.PI / 2.0) < 1e-12) {
            theta = Math.copySign(Math.PI / 2.0, phi);
        } else {
            theta = phi;
            double target = Math.PI * Math.sin(phi);
            for (int i = 0; i < 12; i++) {
                double f = 2.0 * theta + Math.sin(2.0 * theta) - target;
                double fp = 2.0 + 2.0 * Math.cos(2.0 * theta);
                if (Math.abs(fp) < 1e-12) {
                    break;
                }
                double step = f / fp;
                theta -= step;
                if (Math.abs(step) < 1e-12) {
                    break;
                }
            }
        }

        double x = (2.0 * SQRT_2 / Math.PI) * lambda * Math.cos(theta);
        double y = SQRT_2 * Math.sin(theta);
        return new MollweidePoint(x, y);
    }

    /** Posizione su una sfera di raggio r a partire da RA/DEC. */
    public static SkyPoint3D onSphere(double raDeg, double decDeg, double radius) {
        double ra = Math.toRadians(raDeg);
        double dec = Math.toRadians(decDeg);
        double c = Math.cos(dec);
        // y invertito per avere DEC positiva verso l'alto nello spazio JavaFX.
        return new SkyPoint3D(
                radius * c * Math.cos(ra),
                -radius * Math.sin(dec),
                radius * c * Math.sin(ra));
    }

    /**
     * Converte un punto del piano galattico (b=0, longitudine l) in RA/DEC J2000.
     * Usa la trasposta della matrice ICRS->Galactic.
     * @return [RA gradi 0..360, DEC gradi -90..90]
     */
    public static double[] galacticPlaneRaDec(double galacticLongitudeDeg) {
        double l = Math.toRadians(galacticLongitudeDeg);
        double gx = Math.cos(l);
        double gy = Math.sin(l);
        double gz = 0.0;

        // e = M^T * g
        double ex = ICRS_TO_GAL[0][0] * gx + ICRS_TO_GAL[1][0] * gy + ICRS_TO_GAL[2][0] * gz;
        double ey = ICRS_TO_GAL[0][1] * gx + ICRS_TO_GAL[1][1] * gy + ICRS_TO_GAL[2][1] * gz;
        double ez = ICRS_TO_GAL[0][2] * gx + ICRS_TO_GAL[1][2] * gy + ICRS_TO_GAL[2][2] * gz;

        double ra = Math.toDegrees(Math.atan2(ey, ex));
        if (ra < 0.0) {
            ra += 360.0;
        }
        double dec = Math.toDegrees(Math.asin(clamp(ez, -1.0, 1.0)));
        return new double[]{ra, dec};
    }


    public static String raToHms(double raDeg) {
        double normalized = raDeg % 360.0;
        if (normalized < 0.0) {
            normalized += 360.0;
        }
        double totalHours = normalized / 15.0;
        int hours = (int) Math.floor(totalHours);
        double totalMinutes = (totalHours - hours) * 60.0;
        int minutes = (int) Math.floor(totalMinutes);
        double seconds = (totalMinutes - minutes) * 60.0;
        return String.format(java.util.Locale.ROOT, "%02dh %02dm %05.2fs", hours, minutes, seconds);
    }

    public static String decToDms(double decDeg) {
        double value = clamp(decDeg, -90.0, 90.0);
        String sign = value < 0.0 ? "−" : "+";
        double abs = Math.abs(value);
        int degrees = (int) Math.floor(abs);
        double totalMinutes = (abs - degrees) * 60.0;
        int minutes = (int) Math.floor(totalMinutes);
        double seconds = (totalMinutes - minutes) * 60.0;
        return String.format(java.util.Locale.ROOT, "%s%02d° %02d′ %05.2f″", sign, degrees, minutes, seconds);
    }

    public static double normalizeDegrees180(double degrees) {
        double value = degrees % 360.0;
        if (value > 180.0) {
            value -= 360.0;
        } else if (value <= -180.0) {
            value += 360.0;
        }
        return value;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
