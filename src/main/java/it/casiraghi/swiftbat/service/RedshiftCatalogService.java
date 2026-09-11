package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.RedshiftInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Legge la tabella ufficiale BAT dei redshift preservando limiti e incertezze. */
public final class RedshiftCatalogService {
    public static final String REDSHIFT_URL =
            "https://swift.gsfc.nasa.gov/results/batgrbcat/summary_cflux/summary_general_info/GRBlist_redshift_BAT.txt";

    private static final String USER_AGENT =
            "SwiftBAT-Explorer/1.3.0 (academic thesis application; redshift-catalog)";
    private static final Pattern NUMBER = Pattern.compile("(?<![A-Za-z])([0-9]+(?:[.,][0-9]+)?)");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public Map<String, RedshiftInfo> fetchRedshifts() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(REDSHIFT_URL))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " leggendo la tabella redshift Swift/BAT.");
        }
        Map<String, RedshiftInfo> result = parseRedshifts(response.body());
        if (result.isEmpty()) {
            throw new IOException("La tabella redshift BAT non contiene record leggibili.");
        }
        return result;
    }

    /** Visibile al package per i test. */
    Map<String, RedshiftInfo> parseRedshifts(String content) {
        Map<String, RedshiftInfo> result = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return result;
        }
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split("\\s*\\|\\s*", 5);
            if (fields.length < 2) {
                continue;
            }
            String name = normalizeName(fields[0]);
            if (!name.matches("GRB\\d{6}[A-Z]?")) {
                continue;
            }
            String rawValue = fields[1].trim();
            if (rawValue.isBlank() || rawValue.equalsIgnoreCase("N/A")) {
                continue;
            }
            String method = fields.length > 2 ? fields[2].trim() : "";
            String uncertainty = fields.length > 3 ? fields[3].trim() : "";
            String reference = fields.length > 4 ? fields[4].trim() : "";
            result.put(name, parseValue(rawValue, method, uncertainty, reference));
        }
        return Map.copyOf(result);
    }

    private RedshiftInfo parseValue(String raw, String method, String uncertainty, String reference) {
        String normalized = raw.toLowerCase(Locale.ROOT).replace(',', '.').replaceAll("\\s+", " ").trim();
        List<Double> numbers = numbers(normalized);
        boolean uncertain = normalized.contains("?") || normalized.contains("~") || normalized.contains("approx");
        Double lower = null;
        Double upper = null;
        List<Double> alternatives = List.of();

        if (!numbers.isEmpty()) {
            if (normalized.startsWith(">")) {
                lower = numbers.get(0);
            } else if (normalized.startsWith("<")) {
                upper = numbers.get(0);
            } else if (normalized.contains(" or ")) {
                alternatives = List.copyOf(numbers);
                lower = numbers.stream().min(Double::compareTo).orElse(null);
                upper = numbers.stream().max(Double::compareTo).orElse(null);
            } else if (numbers.size() >= 2 && (normalized.contains("<z<")
                    || normalized.contains("< z <") || normalized.matches(".*\\d\\s*-\\s*\\d.*"))) {
                lower = Math.min(numbers.get(0), numbers.get(1));
                upper = Math.max(numbers.get(0), numbers.get(1));
            } else {
                lower = numbers.get(0);
                upper = numbers.get(0);
                alternatives = List.of(numbers.get(0));
            }
        }
        return new RedshiftInfo(raw, method, uncertainty, reference, lower, upper, alternatives, uncertain);
    }

    private List<Double> numbers(String value) {
        List<Double> result = new ArrayList<>();
        Matcher matcher = NUMBER.matcher(value);
        while (matcher.find()) {
            try {
                result.add(Double.parseDouble(matcher.group(1).replace(',', '.')));
            } catch (NumberFormatException ignored) {
                // Manteniamo comunque il testo originale anche se un token è anomalo.
            }
        }
        return result;
    }

    private String normalizeName(String value) {
        String name = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return name.startsWith("GRB") ? name : "GRB" + name;
    }
}
