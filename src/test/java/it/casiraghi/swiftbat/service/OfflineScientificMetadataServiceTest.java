package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.SkyBurst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfflineScientificMetadataServiceTest {
    @TempDir
    Path directory;

    @Test
    void keepsUnknownCoordinatesUnknownAndAllowsTemporalAnalysis() throws Exception {
        PersistentGrbCache cache = new PersistentGrbCache(directory);
        CatalogEntry entry = new CatalogEntry("GRB250605A", "1321323", "https://example.test/");
        cache.write(entry, new PersistentGrbCache.CachedProducts(
                "0 1 0.1 2 0.1 3 0.1 4 0.1 10 0.2\n".getBytes(StandardCharsets.US_ASCII),
                null, entry.dataProductUrl(), "", "", "", "", Instant.EPOCH));
        List<SkyBurst> result = new OfflineScientificMetadataService(cache)
                .fromCachedProducts(cache.catalogEntries());
        assertEquals(1, result.size());
        assertEquals("GRB250605A", result.get(0).grbName());
        assertFalse(result.get(0).hasCoordinates());
        assertFalse(result.get(0).hasT90());
        assertTrue(Double.isNaN(result.get(0).raDeg()));
        assertTrue(Double.isNaN(result.get(0).decDeg()));
    }

    @Test
    void rejectsInventedSkyLocationAndPreservesVerifiedPosition() {
        SkyBurst unavailable = SkyBurst.withoutCoordinates("GRB250605A", "1321323");
        assertFalse(unavailable.hasCoordinates());
        assertThrows(IllegalArgumentException.class,
                () -> new SkyBurst("GRB250605A", "1321323", Double.NaN, 0, null));
        SkyBurst documented = new SkyBurst("GRB250605A", "1321323", 123.4, -12.5, null);
        assertTrue(documented.hasCoordinates());
    }
}
