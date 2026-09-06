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
