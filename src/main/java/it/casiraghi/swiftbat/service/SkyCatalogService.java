package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.SkyBurst;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Legge in un'unica richiesta la tabella generale ufficiale Swift/BAT.
 * I campi usati sono: GRBname, Trig_ID, RA_ground, DEC_ground e T90.
 */
public final class SkyCatalogService {
    public static final String SUMMARY_URL =
            "https://swift.gsfc.nasa.gov/results/batgrbcat/summary_cflux/summary_general_info/summary_general.txt";

    private static final String USER_AGENT =
            "SwiftBAT-Explorer/1.2.0 (academic thesis application; celestial-map)";
    private static final Pattern GRB_PATTERN = Pattern.compile("(?i)(?:GRB)?\\d{6}[A-Z]?");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<SkyBurst> fetchSkyCatalog() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(SUMMARY_URL))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " leggendo la tabella generale Swift/BAT.");
        }
        List<SkyBurst> parsed = parseSummary(response.body());
        if (parsed.isEmpty()) {
            throw new IOException("La tabella generale Swift/BAT è stata scaricata ma non contiene coordinate leggibili.");
        }
        return parsed;
    }

    /** Visibile al package per i test. */
    List<SkyBurst> parseSummary(String content) {
        List<SkyBurst> bursts = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return bursts;
        }

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] fields = splitDataLine(line);
            if (fields.length < 9) {
                continue;
            }

            String rawName = clean(fields[0]);
            if (!GRB_PATTERN.matcher(rawName).matches()) {
                continue;
            }

            Double ra = parseNumber(fields[4]);
            Double dec = parseNumber(fields[5]);
            if (ra == null || dec == null || ra < 0.0 || ra >= 360.0 || dec < -90.0 || dec > 90.0) {
                continue;
            }

            String trigger = clean(fields[1]);
            Double t90 = parseNumber(fields[8]);
            try {
                bursts.add(new SkyBurst(rawName, trigger, ra, dec, t90));
            } catch (IllegalArgumentException ignored) {
                // Una singola riga anomala non deve rendere inutilizzabile l'intera mappa.
            }
        }

        bursts.sort(Comparator.comparing(SkyBurst::grbName).reversed());
        return List.copyOf(bursts);
    }

    private String[] splitDataLine(String line) {
        if (line.indexOf('\t') >= 0) {
            return line.split("\\t+");
        }
        if (line.indexOf('|') >= 0) {
            return line.split("\\s*\\|\\s*");
        }
        return line.split("\\s+");
    }

    private Double parseNumber(String raw) {
        String value = clean(raw).toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.equals("n/a") || value.equals("na") || value.equals("nan")
                || value.equals("-") || value.equals("--") || value.equals("null")) {
            return null;
        }
        value = value.replace(',', '.');
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replace("\u00A0", "");
    }
}
