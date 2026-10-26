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
    @Test
    void readsRealCoordinatesAndT90FromFITSHeadersWithoutOpeningRateTable() {
        CatalogEntry entry = new CatalogEntry("GRB250605A", "1321323", "https://example.test/");
        byte[] primary = fitsHeader("SIMPLE  = T", "BITPIX  = 8", "NAXIS   = 0");
        byte[] rates = fitsHeader("XTENSION= 'BINTABLE'", "BITPIX  = 8",
                "NAXIS   = 2", "NAXIS1  = 32", "NAXIS2  = 999999",
                "RA_OBJ  = 123.456", "DEC_OBJ = -34.567", "T90     = 18.5");
        byte[] onlyHeaders = new byte[primary.length + rates.length];
        System.arraycopy(primary, 0, onlyHeaders, 0, primary.length);
        System.arraycopy(rates, 0, onlyHeaders, primary.length, rates.length);

        var result = OfflineScientificMetadataService.fromFitsHeaders(entry, onlyHeaders);
        assertTrue(result.hasCoordinates());
        assertEquals(123.456, result.raDeg(), 1e-6);
        assertEquals(-34.567, result.decDeg(), 1e-6);
        assertEquals(18.5, result.t90Sec(), 1e-6);
    }

    @Test
    void doesNotConfuseMissingCoordinatesWithZeroZero() {
        CatalogEntry entry = new CatalogEntry("GRB250605A", "1321323", "");
        var result = OfflineScientificMetadataService.fromFitsHeaders(entry,
                fitsHeader("SIMPLE  = T", "BITPIX  = 8", "NAXIS   = 0", "T90     = 12.0"));
        assertFalse(result.hasCoordinates());
        assertTrue(result.hasT90());
        assertEquals(12.0, result.t90Sec(), 1e-6);
    }

    private static byte[] fitsHeader(String... cards) {
        var builder = new StringBuilder();
        for (String card : cards) {
            builder.append(String.format(java.util.Locale.ROOT, "%-80s", card));
        }
        builder.append(String.format(java.util.Locale.ROOT, "%-80s", "END"));
        while (builder.length() % 2880 != 0) builder.append(' ');
        return builder.toString().getBytes(StandardCharsets.US_ASCII);
    }

}
