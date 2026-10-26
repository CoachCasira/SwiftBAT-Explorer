package it.casiraghi.swiftbat.service;

import org.jsoup.Jsoup;
import it.casiraghi.swiftbat.model.CatalogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwiftCatalogServiceTest {
    @Test
    void parsesCatalogRows() throws Exception {
        String html = """
                <html><body><table>
                <tr><th>GRB Name</th><th>Trigger ID</th><th>Results</th></tr>
                <tr><td>GRB250605A</td><td>1321323</td><td><a href='GRB250605A/data_product/'>Data Product</a></td></tr>
                </table></body></html>
                """;
        var result = new SwiftCatalogService().parseCatalog(
                Jsoup.parse(html, SwiftCatalogService.CATALOG_URL));
        assertEquals(1, result.size());
        assertEquals("GRB250605A", result.get(0).grbName());
        assertEquals("1321323", result.get(0).triggerId());
    }

    @Test
    void offlineFallbackUsesEveryCachedGrbAndPreservesTriggerIds(@TempDir Path temporary) throws Exception {
        PersistentGrbCache cache = new PersistentGrbCache(temporary);
        for (String name : List.of("GRB250605A", "GRB240101A", "GRB230101B")) {
            CatalogEntry entry = new CatalogEntry(name, "trigger-" + name,
                    "https://swift.gsfc.nasa.gov/results/batgrbcat/" + name + "/data_product/");
            cache.write(entry, new PersistentGrbCache.CachedProducts(
                    "0 1 0.1 2 0.1 3 0.1 4 0.1 10 0.2\n".getBytes(StandardCharsets.US_ASCII),
                    null, entry.dataProductUrl(), "", "", "", "", Instant.EPOCH));
        }
        // Reopen the disk cache: this is what happens on the next app launch.
        var entries = new SwiftCatalogService(new PersistentGrbCache(temporary)).fallbackCatalog();
        assertEquals(3, entries.size());
        assertEquals(List.of("GRB250605A", "GRB240101A", "GRB230101B"),
                entries.stream().map(CatalogEntry::grbName).toList());
        assertEquals("trigger-GRB240101A", entries.get(1).triggerId());
        assertTrue(entries.get(0).dataProductUrl().endsWith("/GRB250605A/data_product/"));
    }

    @Test
    void offlineFallbackSkipsDamagedOrEmptyCachedEntries(@TempDir Path temporary) throws Exception {
        PersistentGrbCache cache = new PersistentGrbCache(temporary);
        CatalogEntry entry = new CatalogEntry("GRB250605A", "123", "https://example.test/data/");
        cache.write(entry, new PersistentGrbCache.CachedProducts(
                new byte[]{1}, null, entry.dataProductUrl(), "", "", "", "", Instant.EPOCH));
        Path damaged = temporary.resolve("GRB240101A");
        Files.createDirectories(damaged);
        Files.writeString(damaged.resolve("manifest.properties"),
                "schema=1\ngrb=GRB240101A\ntriggerId=456\n");
        // No ASCII/FITS bytes -> not an event that can be opened offline.
        assertEquals(1, new SwiftCatalogService(new PersistentGrbCache(temporary))
                .fallbackCatalog().size());
    }

    @Test
    void originalTenSamplesRemainOnlyWhenLocalCacheIsEmpty(@TempDir Path temporary) {
        var entries = new SwiftCatalogService(new PersistentGrbCache(temporary)).fallbackCatalog();
        assertEquals(10, entries.size());
    }
}
