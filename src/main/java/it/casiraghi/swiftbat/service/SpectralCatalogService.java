package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.SpectralData;
import it.casiraghi.swiftbat.model.SpectralData.EnergyFluxBand;
import it.casiraghi.swiftbat.model.SpectralData.Fit;
import it.casiraghi.swiftbat.model.SpectralData.Interval;
import it.casiraghi.swiftbat.model.SpectralData.Model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Scarica e combina le tabelle spettroscopiche ufficiali del catalogo BAT.
 *
 * <p>Non esegue un fit locale: espone i risultati PL/CPL già prodotti con
 * XSPEC dal team Swift/BAT. Ogni file viene mantenuto in cache per velocizzare
 * gli avvii successivi e consentire un fallback quando la rete non è disponibile.</p>
 */
public final class SpectralCatalogService {
    public static final String T100_URL =
            "https://swift.gsfc.nasa.gov/results/batgrbcat/summary_cflux/summary_T100/";
    public static final String PEAK_ONE_SECOND_URL =
            "https://swift.gsfc.nasa.gov/results/batgrbcat/summary_cflux/summary_1s_peak/";

    private static final Duration CACHE_VALIDITY = Duration.ofHours(6);
    private static final List<String> FILES = List.of(
            "best_model.txt",
            "summary_pow_parameters.txt",
            "summary_cutpow_parameters.txt",
            "summary_pow_energy_flux.txt",
            "summary_cutpow_energy_flux.txt");

    private final HttpClient client;
    private final Path cacheRoot;

    public SpectralCatalogService() {
        this(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(12))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                Path.of(System.getProperty("user.home"), ".swiftbat-explorer", "cache", "spectroscopy-v1"));
    }

    SpectralCatalogService(HttpClient client, Path cacheRoot) {
        this.client = client;
        this.cacheRoot = cacheRoot;
    }

    public Map<String, SpectralData> fetchCatalog() throws IOException, InterruptedException {
        return fetchCatalog(false);
    }

    public Map<String, SpectralData> fetchCatalog(boolean forceRefresh) throws IOException, InterruptedException {
        ParsedInterval t100 = fetchInterval(Interval.T100, T100_URL, "t100", forceRefresh);
        ParsedInterval peak = fetchInterval(
                Interval.PEAK_ONE_SECOND, PEAK_ONE_SECOND_URL, "peak-1s", forceRefresh);

        Set<String> names = new LinkedHashSet<>();
        names.addAll(t100.results().keySet());
        names.addAll(peak.results().keySet());
        Map<String, SpectralData> combined = new LinkedHashMap<>();
        for (String name : names) {
            EnumMap<Interval, SpectralData.Result> results = new EnumMap<>(Interval.class);
            if (t100.results().containsKey(name)) results.put(Interval.T100, t100.results().get(name));
            if (peak.results().containsKey(name)) results.put(Interval.PEAK_ONE_SECOND, peak.results().get(name));
            String trigger = t100.triggerIds().getOrDefault(name, peak.triggerIds().getOrDefault(name, ""));
            combined.put(name, new SpectralData(name, trigger, results));
        }
        return Map.copyOf(combined);
    }

    private ParsedInterval fetchInterval(Interval interval, String baseUrl, String cacheFolder,
                                         boolean forceRefresh) throws IOException, InterruptedException {
        Map<String, String> content = new LinkedHashMap<>();
        for (String file : FILES) {
            content.put(file, readResource(baseUrl, cacheFolder, file, forceRefresh));
        }
        return parseInterval(interval,
                content.get("best_model.txt"),
                content.get("summary_pow_parameters.txt"),
                content.get("summary_cutpow_parameters.txt"),
                content.get("summary_pow_energy_flux.txt"),
                content.get("summary_cutpow_energy_flux.txt"));
    }

    private String readResource(String baseUrl, String cacheFolder, String file,
                                boolean forceRefresh) throws IOException, InterruptedException {
        Path cached = cacheRoot.resolve(cacheFolder).resolve(file);
        if (!forceRefresh && isFresh(cached)) {
            return Files.readString(cached, StandardCharsets.UTF_8);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + file))
                    .timeout(Duration.ofSeconds(25))
                    .header("User-Agent", "SwiftBAT-Explorer/1.3")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode() + " per " + file);
            }
            writeCache(cached, response.body());
            return response.body();
        } catch (InterruptedException interrupted) {
            throw interrupted;
        } catch (Exception downloadError) {
            if (Files.isRegularFile(cached)) {
                return Files.readString(cached, StandardCharsets.UTF_8);
            }
            if (downloadError instanceof IOException io) throw io;
            throw new IOException("Download spettroscopia non riuscito: " + file, downloadError);
        }
    }

    private boolean isFresh(Path file) {
        try {
            return Files.isRegularFile(file)
                    && Files.getLastModifiedTime(file).toInstant().isAfter(Instant.now().minus(CACHE_VALIDITY));
        } catch (IOException ignored) {
            return false;
        }
    }

    private void writeCache(Path target, String value) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, value, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static ParsedInterval parseInterval(Interval interval, String bestModels,
                                        String powerLawParameters, String cutoffParameters,
                                        String powerLawFlux, String cutoffFlux) {
        Map<String, String> best = parseBestModels(bestModels);
        ParsedTable plParameters = parsePipeTable(powerLawParameters);
        ParsedTable cplParameters = parsePipeTable(cutoffParameters);
        ParsedTable plFluxes = parsePipeTable(powerLawFlux);
        ParsedTable cplFluxes = parsePipeTable(cutoffFlux);

        Map<String, Map<String, String>> plFitByName = plParameters.byName();
        Map<String, Map<String, String>> cplFitByName = cplParameters.byName();
        Map<String, Map<String, String>> plFluxByName = plFluxes.byName();
        Map<String, Map<String, String>> cplFluxByName = cplFluxes.byName();

        Set<String> names = new LinkedHashSet<>();
        names.addAll(best.keySet());
        names.addAll(plFitByName.keySet());
        names.addAll(cplFitByName.keySet());
        names.addAll(plFluxByName.keySet());
        names.addAll(cplFluxByName.keySet());

        Map<String, SpectralData.Result> results = new LinkedHashMap<>();
        Map<String, String> triggerIds = new LinkedHashMap<>();
        for (String name : names) {
            Map<String, String> plRow = plFitByName.get(name);
            Map<String, String> cplRow = cplFitByName.get(name);
            Map<String, String> plFluxRow = plFluxByName.get(name);
            Map<String, String> cplFluxRow = cplFluxByName.get(name);
            String trigger = firstNonBlank(
                    value(plRow, "trig_id"), value(cplRow, "trig_id"),
                    value(plFluxRow, "trig_id"), value(cplFluxRow, "trig_id"));
            if (!trigger.isBlank()) triggerIds.put(name, trigger);

            Fit plFit = fit(Model.POWER_LAW, plRow);
            Fit cplFit = fit(Model.CUTOFF_POWER_LAW, cplRow);
            List<EnergyFluxBand> plBands = fluxBands(plFluxRow);
            List<EnergyFluxBand> cplBands = fluxBands(cplFluxRow);
            results.put(name, new SpectralData.Result(interval,
                    best.getOrDefault(name, "N/A"), plFit, cplFit, plBands, cplBands));
        }
        return new ParsedInterval(Map.copyOf(results), Map.copyOf(triggerIds));
    }

    private static Fit fit(Model model, Map<String, String> row) {
        if (row == null || row.isEmpty()) return null;
        return new Fit(model,
                number(row, "alpha"), number(row, "alpha_low"), number(row, "alpha_hi"),
                model == Model.CUTOFF_POWER_LAW ? number(row, "epeak") : null,
                model == Model.CUTOFF_POWER_LAW ? number(row, "epeak_low") : null,
                model == Model.CUTOFF_POWER_LAW ? number(row, "epeak_hi") : null,
                number(row, "norm"), number(row, "norm_low"), number(row, "norm_hi"),
                number(row, "chi2"), integer(row, "dof"), number(row, "reduced_chi2"),
                number(row, "null_prob"), number(row, "enorm"), number(row, "exposure_time"),
                number(row, "spectrum_start"), number(row, "spectrum_stop"));
    }

    private static List<EnergyFluxBand> fluxBands(Map<String, String> row) {
        if (row == null || row.isEmpty()) return List.of();
        List<EnergyFluxBand> bands = new ArrayList<>();
        addFluxBand(bands, row, "15–25 keV", 15, 25, "15_25kev");
        addFluxBand(bands, row, "25–50 keV", 25, 50, "25_50kev");
        addFluxBand(bands, row, "50–100 keV", 50, 100, "50_100kev");
        addFluxBand(bands, row, "100–150 keV", 100, 150, "100_150kev");
        return List.copyOf(bands);
    }

    private static void addFluxBand(List<EnergyFluxBand> result, Map<String, String> row,
                                    String label, double low, double high, String column) {
        Double value = fluxNumber(row, column);
        Double lower = fluxNumber(row, column + "_low");
        Double upper = fluxNumber(row, column + "_hi");
        if (value != null) {
            result.add(new EnergyFluxBand(label, low, high, value, lower, upper));
        }
    }

    private static Map<String, String> parseBestModels(String content) {
        Map<String, String> result = new LinkedHashMap<>();
        if (content == null) return result;
        for (String line : content.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("|")) continue;
            String[] cells = trimmed.split("\\|", -1);
            if (cells.length < 3) continue;
            String name = canonicalName(cells[0]);
            if (!name.isBlank()) result.put(name, cells[2].trim().toUpperCase(Locale.ROOT));
        }
        return result;
    }

    private static ParsedTable parsePipeTable(String content) {
        if (content == null) return new ParsedTable(Map.of());
        List<String> headers = List.of();
        Map<String, Map<String, String>> rows = new LinkedHashMap<>();
        for (String line : content.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("##") && trimmed.toLowerCase(Locale.ROOT).contains("grbname")
                    && trimmed.contains("|")) {
                headers = splitCells(trimmed.replaceFirst("^#+\\s*", ""))
                        .stream().map(SpectralCatalogService::normalizeColumn).toList();
                continue;
            }
            if (headers.isEmpty() || trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("|")) {
                continue;
            }
            List<String> cells = splitCells(trimmed);
            Map<String, String> row = new LinkedHashMap<>();
            for (int index = 0; index < headers.size(); index++) {
                row.put(headers.get(index), index < cells.size() ? cells.get(index) : "");
            }
            String name = canonicalName(row.get("grbname"));
            if (!name.isBlank()) rows.put(name, Map.copyOf(row));
        }
        return new ParsedTable(Map.copyOf(rows));
    }

    private static List<String> splitCells(String line) {
        String[] parts = line.split("\\|", -1);
        List<String> cells = new ArrayList<>(parts.length);
        for (String part : parts) cells.add(part.trim());
        return cells;
    }

    private static String normalizeColumn(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String canonicalName(String value) {
        if (value == null) return "";
        String result = value.trim().toUpperCase(Locale.ROOT);
        return result.startsWith("GRB") ? result : result.isBlank() ? "" : "GRB" + result;
    }

    private static String value(Map<String, String> row, String key) {
        return row == null ? "" : row.getOrDefault(key, "").trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }

    private static Double number(Map<String, String> row, String key) {
        String value = value(row, key);
        if (value.isBlank() || value.equalsIgnoreCase("N/A") || value.equals("--")) return null;
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double fluxNumber(Map<String, String> row, String key) {
        Double parsed = number(row, key);
        // Nelle tabelle ufficiali 1.0 è il segnaposto per un fit log10(flux)=0 non valido.
        return parsed != null && Math.abs(parsed - 1.0) < 1e-12 ? null : parsed;
    }

    private static Integer integer(Map<String, String> row, String key) {
        Double parsed = number(row, key);
        return parsed == null ? null : parsed.intValue();
    }

    record ParsedInterval(Map<String, SpectralData.Result> results, Map<String, String> triggerIds) {
    }

    private record ParsedTable(Map<String, Map<String, String>> byName) {
    }
}
