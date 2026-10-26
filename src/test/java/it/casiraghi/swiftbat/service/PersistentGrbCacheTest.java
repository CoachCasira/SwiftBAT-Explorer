package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.CatalogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentGrbCacheTest {
    @TempDir
    Path temporary;

    @Test
    void manifestWithoutScienceProductIsNotCountedAsCachedCurve() throws Exception {
        PersistentGrbCache cache = new PersistentGrbCache(temporary);
        CatalogEntry empty = new CatalogEntry("GRB250605A", "1321323", "https://example.test/");
        cache.write(empty, new PersistentGrbCache.CachedProducts(
                null, null, empty.dataProductUrl(), "", "", "", "", Instant.EPOCH));
        assertEquals(0, cache.count());
        assertTrue(cache.catalogEntries().isEmpty());
        PersistentGrbCache reopened = new PersistentGrbCache(temporary);
        assertEquals(0, reopened.count());
        assertTrue(reopened.catalogEntries().isEmpty());
    }

    @Test
    void persistsOfficialBytesAndManifest() throws Exception {
        PersistentGrbCache cache = new PersistentGrbCache(temporary);
        CatalogEntry entry = new CatalogEntry("GRBTESTA", "123", "https://example.test/data/");
        byte[] ascii = "0 1 0.1 2 0.1 3 0.1 4 0.1 10 0.2\n".getBytes(StandardCharsets.US_ASCII);

        cache.write(entry, new PersistentGrbCache.CachedProducts(
                ascii, null,
                entry.dataProductUrl(), "https://example.test/results/", "https://example.test/lc/",
                "https://example.test/lc/1s_lc_ascii.dat", "", Instant.EPOCH));

        var restored = cache.read(entry).orElseThrow();
        assertArrayEquals(ascii, restored.asciiBytes());
        assertEquals(Instant.EPOCH, restored.savedAt());
        assertTrue(cache.contains(entry.grbName()));
        assertEquals(1, cache.count());

        PersistentGrbCache reopened = new PersistentGrbCache(temporary);
        assertTrue(reopened.contains(entry.grbName()));
        assertEquals(1, reopened.count());
    }


    @Test
    void packagedOfflineCacheIsReadableWithoutModifyingIt() throws Exception {
        Path userCache = temporary.resolve("user-cache");
        Path bundledCache = temporary.resolve("offline-data").resolve("v1");
        PersistentGrbCache source = new PersistentGrbCache(bundledCache);
        CatalogEntry entry = new CatalogEntry("GRB250605A", "1321323",
                SwiftCatalogService.CATALOG_URL + "GRB250605A/data_product/");
        byte[] ascii = "0 1 0.1 2 0.1 3 0.1 4 0.1 10 0.2\n"
                .getBytes(StandardCharsets.US_ASCII);
        source.write(entry, new PersistentGrbCache.CachedProducts(
                ascii, null, entry.dataProductUrl(), "", "", "", "", Instant.EPOCH));

        PersistentGrbCache recipient = new PersistentGrbCache(userCache, bundledCache);
        assertEquals(1, recipient.count());
        assertEquals(1, recipient.catalogEntries().size());
        assertArrayEquals(ascii, recipient.read(entry).orElseThrow().asciiBytes());
        assertTrue(recipient.contains(entry.grbName()));
        assertTrue(java.nio.file.Files.notExists(userCache.resolve(entry.grbName())));
    }

    @Test
    void mergesBundledMacArchiveAndWindowsCacheWithoutDuplicateEvents() throws Exception {
        Path userCache = temporary.resolve("windows-cache");
        Path macArchive = temporary.resolve("offline-data").resolve("v1");
        PersistentGrbCache mac = new PersistentGrbCache(macArchive);
        PersistentGrbCache windows = new PersistentGrbCache(userCache);
        for (String name : java.util.List.of("GRB250605A", "GRB250603A")) {
            CatalogEntry item = new CatalogEntry(name, "mac-" + name, "https://example.test/mac/");
            mac.write(item, new PersistentGrbCache.CachedProducts(
                    new byte[]{1}, null, item.dataProductUrl(), "", "", "", "", Instant.EPOCH));
        }
        for (String name : java.util.List.of("GRB250603A", "GRB250530C")) {
            CatalogEntry item = new CatalogEntry(name, "win-" + name, "https://example.test/win/");
            windows.write(item, new PersistentGrbCache.CachedProducts(
                    new byte[]{2}, null, item.dataProductUrl(), "", "", "", "", Instant.EPOCH));
        }

        PersistentGrbCache combined = new PersistentGrbCache(userCache, macArchive);
        assertEquals(3, combined.catalogEntries().size());
        assertEquals(3, combined.count());
        CatalogEntry overlapping = combined.catalogEntries().stream()
                .filter(entry -> entry.grbName().equals("GRB250603A")).findFirst().orElseThrow();
        assertEquals("win-GRB250603A", overlapping.triggerId());
        assertArrayEquals(new byte[]{1},
                combined.read(new CatalogEntry("GRB250605A", "", "")).orElseThrow().asciiBytes());
    }

    @Test
    void corruptUserManifestFallsBackToBundledScientificProducts() throws Exception {
        Path userCache = temporary.resolve("user-broken");
        Path bundledCache = temporary.resolve("offline-data").resolve("v1");
        CatalogEntry entry = new CatalogEntry("GRB250605A", "1321323",
                "https://example.test/data/");
        byte[] ascii = "0 1 0.1 2 0.1 3 0.1 4 0.1 10 0.2\n"
                .getBytes(StandardCharsets.US_ASCII);
        new PersistentGrbCache(bundledCache).write(entry,
                new PersistentGrbCache.CachedProducts(
                        ascii, null, entry.dataProductUrl(), "", "", "", "", Instant.EPOCH));
        Path broken = userCache.resolve(entry.grbName());
        java.nio.file.Files.createDirectories(broken);
        String corruptProperties = String.join(System.lineSeparator(),
                "schema=1", "grb=GRB250605A", "savedAt=" + (char) 92 + "uZZZZ", "");
        java.nio.file.Files.writeString(broken.resolve("manifest.properties"),
                corruptProperties);

        PersistentGrbCache reader = new PersistentGrbCache(userCache, bundledCache);
        assertArrayEquals(ascii, reader.read(entry).orElseThrow().asciiBytes());
    }

    @Test
    void metadataLookupDoesNotReadEntireFITSFile() throws Exception {
        PersistentGrbCache cache = new PersistentGrbCache(temporary);
        CatalogEntry entry = new CatalogEntry("GRB250605A", "1321323", "https://example.test/data/");
        byte[] largeFits = new byte[2 * 1024 * 1024];
        largeFits[0] = 'S';
        cache.write(entry, new PersistentGrbCache.CachedProducts(
                null, largeFits, entry.dataProductUrl(), "", "", "", "", Instant.EPOCH));
        byte[] prefix = cache.readFitsHeaderPrefix(entry).orElseThrow();
        assertEquals(128 * 1024, prefix.length);
        assertEquals('S', prefix[0]);
    }

    @Test
    void onlineServiceUsesDiskCacheWithoutNetwork() throws Exception {
        PersistentGrbCache cache = new PersistentGrbCache(temporary);
        CatalogEntry entry = new CatalogEntry("GRBTESTA", "123", "https://invalid.test/data/");
        byte[] ascii = "0 1 0.1 2 0.1 3 0.1 4 0.1 10 0.2\n".getBytes(StandardCharsets.US_ASCII);
        cache.write(entry, new PersistentGrbCache.CachedProducts(
                ascii, null, entry.dataProductUrl(), "", "",
                "https://invalid.test/1s_lc_ascii.dat", "", Instant.EPOCH));

        OnlineGrbService service = new OnlineGrbService(cache);
        var data = service.load(entry, false);

        assertEquals("GRBTESTA", data.grbName());
        assertEquals(1, data.asciiData().rows().size());
        assertEquals("NASA/GSFC · cache locale", data.summaryValue("DATA_SOURCE", ""));
    }
}
