package it.casiraghi.swiftbat.ui.components;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.InputStream;

/**
 * Identita visiva dell'app.
 *
 * <p>L'icona di sistema torna a usare lo storico app-icon.png, mentre le viste
 * interne usano un piccolo marchio generato in memoria: un buco nero con disco
 * di accrescimento ciano/magenta, pensato per restare leggibile sul tema scuro.</p>
 */
public final class BrandLogoAsset {
    private static final Image THEMED_IMAGE = createThemedImage(96);
    private static final Image SYSTEM_IMAGE = loadSystemImage();

    private BrandLogoAsset() { }

    /** Icona usata dalla finestra e dal packaging JavaFX. */
    public static Image image() {
        return SYSTEM_IMAGE;
    }

    /** Marchio compatto usato nell'header e nel footer dell'interfaccia. */
    public static ImageView view(double size) {
        ImageView view = new ImageView(THEMED_IMAGE);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setMouseTransparent(true);
        return view;
    }

    private static Image loadSystemImage() {
        try (InputStream stream = BrandLogoAsset.class.getResourceAsStream("/app-icon.png")) {
            if (stream != null) {
                Image image = new Image(stream);
                if (!image.isError()) return image;
            }
        } catch (Exception ignored) {
            // Il logo vettoriale generato sotto resta un fallback sempre disponibile.
        }
        return THEMED_IMAGE;
    }

    private static Image createThemedImage(int size) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter pixels = image.getPixelWriter();
        double center = (size - 1) / 2.0;
        double scale = size / 96.0;
        double rotation = Math.toRadians(-18.0);
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = (x - center) / scale;
                double dy = (y - center) / scale;
                double rx = cos * dx - sin * dy;
                double ry = sin * dx + cos * dy;

                double ellipseRadius = Math.sqrt(rx * rx + Math.pow(ry / 0.43, 2));
                double ring = gaussian(ellipseRadius - 29.0, 2.1);
                double glow = gaussian(ellipseRadius - 29.0, 7.2) * 0.38;
                double radius = Math.hypot(dx, dy);
                double photonRing = gaussian(radius - 15.8, 2.2) * 0.72;
                double core = smoothStep(14.0, 10.7, radius);

                // Da ciano a magenta lungo il disco: richiama i grafici dell'app.
                double colorMix = clamp((rx + 34.0) / 68.0);
                double cyanR = 0.05, cyanG = 0.88, cyanB = 1.00;
                double magR = 0.94, magG = 0.20, magB = 0.90;
                double rr = cyanR * (1.0 - colorMix) + magR * colorMix;
                double gg = cyanG * (1.0 - colorMix) + magG * colorMix;
                double bb = cyanB * (1.0 - colorMix) + magB * colorMix;

                double diskEnergy = clamp(ring + glow);
                double red = rr * diskEnergy + 0.16 * photonRing;
                double green = gg * diskEnergy + 0.72 * photonRing;
                double blue = bb * diskEnergy + 1.00 * photonRing;
                double alpha = clamp(ring * 0.98 + glow * 0.82 + photonRing * 0.68);

                // Disco nero centrale, con un bordo luminoso ben visibile sul tema scuro.
                if (core > 0.0) {
                    double keep = 1.0 - core;
                    red = red * keep + 0.008 * core;
                    green = green * keep + 0.015 * core;
                    blue = blue * keep + 0.035 * core;
                    alpha = Math.max(alpha, 0.97 * core);
                }

                // Piccola scintilla BAT sul lato alto-sinistra per distinguere il marchio.
                double spark = Math.max(
                        gaussian(Math.hypot(dx + 25.0, dy + 21.0), 1.35),
                        Math.max(
                                gaussian(Math.abs(dx + 25.0) + Math.abs(dy + 21.0) * 0.18, 1.55),
                                gaussian(Math.abs(dy + 21.0) + Math.abs(dx + 25.0) * 0.18, 1.55))) * 0.9;
                if (spark > 0.03) {
                    red = clamp(red + spark * 0.42);
                    green = clamp(green + spark * 0.88);
                    blue = clamp(blue + spark);
                    alpha = Math.max(alpha, clamp(spark));
                }

                if (alpha < 0.012) {
                    pixels.setColor(x, y, Color.TRANSPARENT);
                } else {
                    pixels.setColor(x, y, new Color(clamp(red), clamp(green), clamp(blue), clamp(alpha)));
                }
            }
        }
        return image;
    }

    private static double gaussian(double value, double sigma) {
        double ratio = value / sigma;
        return Math.exp(-0.5 * ratio * ratio);
    }

    private static double smoothStep(double outer, double inner, double value) {
        if (value <= inner) return 1.0;
        if (value >= outer) return 0.0;
        double t = (outer - value) / (outer - inner);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
