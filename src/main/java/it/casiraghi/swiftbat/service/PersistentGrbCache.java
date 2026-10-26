package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.CatalogEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Cache persistente dei prodotti ufficiali scaricati.
 *
 * <p>Conserva i byte originali ASCII/FITS e un piccolo manifest. I dati vengono
 * reinterpretati dal parser corrente a ogni apertura, evitando formati Java
 * serializzati fragili fra versioni diverse dell'applicazione.</p>
 */
public final class PersistentGrbCache {
    private static final String SCHEMA_VERSION = "1";
    private static final String MANIFEST = "manifest.properties";
    private static final String ASCII_FILE = "ascii-4chan-1s.dat";
    private static final String FITS_FILE = "fits-1chan-1s.lc";

    private final Path root;
    private final Set<String> cachedNames = ConcurrentHashMap.newKeySet();

    public PersistentGrbCache() {
        this(defaultRoot());
    }

    PersistentGrbCache(Path root) {
        this.root = root.toAbsolutePath().normalize();
        indexExistingEntries();
    }

    public Optional<CachedProducts> read(CatalogEntry entry) throws IOException {
        Path directory = eventDirectory(entry.grbName());
        Path manifestPath = directory.resolve(MANIFEST);
        if (!Files.isRegularFile(manifestPath)) {
            return Optional.empty();
        }

        Properties manifest = new Properties();
        try (var input = Files.newInputStream(manifestPath)) {
            manifest.load(input);
        }
        if (!SCHEMA_VERSION.equals(manifest.getProperty("schema"))
                || !entry.grbName().equalsIgnoreCase(manifest.getProperty("grb", ""))) {
            return Optional.empty();
        }

        byte[] ascii = readOptional(directory.resolve(ASCII_FILE));
        byte[] fits = readOptional(directory.resolve(FITS_FILE));
        if (ascii == null && fits == null) {
            return Optional.empty();
        }
        cachedNames.add(cacheKey(entry.grbName()));

        return Optional.of(new CachedProducts(
                ascii,
                fits,
                manifest.getProperty("dataProductUrl", entry.dataProductUrl()),
                manifest.getProperty("resultsUrl", ""),
                manifest.getProperty("lightCurveDirectoryUrl", ""),
                manifest.getProperty("asciiUrl", ""),
                manifest.getProperty("fitsUrl", ""),
                parseInstant(manifest.getProperty("savedAt"))));
    }

    public void write(CatalogEntry entry, CachedProducts products) throws IOException {
        Path directory = eventDirectory(entry.grbName());
        Files.createDirectories(directory);

        writeOrRemove(directory.resolve(ASCII_FILE), products.asciiBytes());
        writeOrRemove(directory.resolve(FITS_FILE), products.fitsBytes());

        Properties manifest = new Properties();
        manifest.setProperty("schema", SCHEMA_VERSION);
        manifest.setProperty("grb", entry.grbName());
        manifest.setProperty("triggerId", entry.triggerId());
        manifest.setProperty("savedAt", products.savedAt().toString());
        manifest.setProperty("dataProductUrl", products.dataProductUrl());
        manifest.setProperty("resultsUrl", products.resultsUrl());
        manifest.setProperty("lightCurveDirectoryUrl", products.lightCurveDirectoryUrl());
        manifest.setProperty("asciiUrl", products.asciiUrl());
        manifest.setProperty("fitsUrl", products.fitsUrl());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        manifest.store(output, "SwiftBAT Explorer local cache");
        writeAtomically(directory.resolve(MANIFEST), output.toByteArray());
        cachedNames.add(cacheKey(entry.grbName()));
    }

    public boolean contains(String grbName) {
        return cachedNames.contains(cacheKey(grbName));
    }

    public int count() {
        return cachedNames.size();
    }

    /**
     * Reconstructs offline-selectable GRBs from the same manifests used by
     * the persistent light-curve cache. No connection to NASA is required.
     * Only events with at least one product file are exposed.
     */
    public List<CatalogEntry> catalogEntries() {
        if (!Files.isDirectory(root)) return List.of();
        List<CatalogEntry> entries = new ArrayList<>();
        try (Stream<Path> paths = Files.list(root)) {
            for (Path directory : paths.filter(Files::isDirectory).toList()) {
                if (!Files.isRegularFile(directory.resolve(MANIFEST))
                        || (!Files.isRegularFile(directory.resolve(ASCII_FILE))
                        && !Files.isRegularFile(directory.resolve(FITS_FILE)))) {
                    continue;
                }
                Properties manifest = new Properties();
                try (var input = Files.newInputStream(directory.resolve(MANIFEST))) {
                    manifest.load(input);
                } catch (IOException | IllegalArgumentException ignored) {
                    // Skip a damaged entry without hiding the other cached GRBs.
                    continue;
                }
                String name = manifest.getProperty("grb", "").trim().toUpperCase(Locale.ROOT);
                if (!SCHEMA_VERSION.equals(manifest.getProperty("schema"))
                        || !name.matches("GRB\\d{6}[A-Z]?")
                        || !directory.getFileName().toString().equalsIgnoreCase(cacheKey(name))) {
                    continue;
                }
                String trigger = manifest.getProperty("triggerId", "").trim();
                String productUrl = manifest.getProperty("dataProductUrl", "").trim();
                if (productUrl.isBlank()) {
                    productUrl = SwiftCatalogService.CATALOG_URL + name + "/data_product/";
                }
                entries.add(new CatalogEntry(name, trigger, productUrl));
            }
        } catch (IOException ignored) {
            // Offline operation is best effort; the legacy fallback remains.
        }
        entries.sort(Comparator.comparing(CatalogEntry::grbName).reversed());
        return List.copyOf(entries);
    }

    Path root() {
        return root;
    }

    private Path eventDirectory(String grbName) {
        return root.resolve(cacheKey(grbName)).normalize();
    }

    private String cacheKey(String grbName) {
        return grbName == null ? "UNKNOWN"
                : grbName.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "_");
    }

    private void indexExistingEntries() {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> entries = Files.list(root)) {
            entries.filter(Files::isDirectory)
                    .filter(path -> Files.isRegularFile(path.resolve(MANIFEST)))
                    .map(path -> path.getFileName().toString().toUpperCase(Locale.ROOT))
                    .forEach(cachedNames::add);
        } catch (IOException ignored) {
            // The cache is an optimization: an unreadable index must not block startup.
        }
    }

    private byte[] readOptional(Path path) throws IOException {
        return Files.isRegularFile(path) ? Files.readAllBytes(path) : null;
    }

    private void writeOrRemove(Path path, byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            Files.deleteIfExists(path);
            return;
        }
        writeAtomically(path, bytes);
    }

    private void writeAtomically(Path destination, byte[] content) throws IOException {
        Path temporary = Files.createTempFile(destination.getParent(), destination.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            Files.write(temporary, content);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private Instant parseInstant(String value) {
        try {
            return value == null || value.isBlank() ? Instant.now() : Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return Instant.now();
        }
    }

    private static Path defaultRoot() {
        String override = System.getProperty("swiftbat.cache.dir", "").trim();
        if (!override.isEmpty()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".swiftbat-explorer", "cache", "v1");
    }

    public record CachedProducts(
            byte[] asciiBytes,
            byte[] fitsBytes,
            String dataProductUrl,
            String resultsUrl,
            String lightCurveDirectoryUrl,
            String asciiUrl,
            String fitsUrl,
            Instant savedAt) {

        public CachedProducts {
            asciiBytes = asciiBytes == null ? null : asciiBytes.clone();
            fitsBytes = fitsBytes == null ? null : fitsBytes.clone();
            dataProductUrl = safe(dataProductUrl);
            resultsUrl = safe(resultsUrl);
            lightCurveDirectoryUrl = safe(lightCurveDirectoryUrl);
            asciiUrl = safe(asciiUrl);
            fitsUrl = safe(fitsUrl);
            savedAt = savedAt == null ? Instant.now() : savedAt;
        }

        @Override
        public byte[] asciiBytes() {
            return asciiBytes == null ? null : asciiBytes.clone();
        }

        @Override
        public byte[] fitsBytes() {
            return fitsBytes == null ? null : fitsBytes.clone();
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
