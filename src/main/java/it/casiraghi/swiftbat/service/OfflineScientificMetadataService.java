package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.SkyBurst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads only the lightweight FITS headers from cached BAT products.
 * No light-curve tables are materialized in order to populate the filters.
 * Unavailable scientific measurements remain explicitly unknown.
 */
public final class OfflineScientificMetadataService {
    private static final int CARD = 80;
    private static final int BLOCK = 2880;
    private final PersistentGrbCache cache;

    public OfflineScientificMetadataService() {
        this(new PersistentGrbCache());
    }

    OfflineScientificMetadataService(PersistentGrbCache cache) {
        this.cache = cache;
    }

    public List<SkyBurst> fromCachedProducts(List<CatalogEntry> entries) {
        List<SkyBurst> bursts = new ArrayList<>(entries.size());
        for (CatalogEntry entry : entries) {
            SkyBurst candidate = SkyBurst.withoutCoordinates(entry.grbName(), entry.triggerId());
            try {
                var prefix = cache.readFitsHeaderPrefix(entry);
                if (prefix.isPresent()) {
                    candidate = fromFitsHeaders(entry, prefix.get());
                }
            } catch (IOException | RuntimeException ignored) {
                // One invalid local product must not block other GRBs.
            }
            bursts.add(candidate);
        }
        return List.copyOf(bursts);
    }

    /**
     * FITS is a sequence of 80-byte header cards in 2880-byte blocks.
     * Inspect the PRIMARY and first extension headers only; never deserialize
     * the potentially huge RATE binary table. Invalid/incomplete headers are
     * deliberately rejected rather than silently assigning sky coordinates.
     */
    static SkyBurst fromFitsHeaders(CatalogEntry entry, byte[] prefix) {
        if (prefix == null || prefix.length < BLOCK) {
            return SkyBurst.withoutCoordinates(entry.grbName(), entry.triggerId());
        }
        double ra = Double.NaN;
        double dec = Double.NaN;
        Double t90 = null;
        long position = 0;
        for (int hdu = 0; hdu < 2 && position + CARD <= prefix.length; hdu++) {
            long headerStart = position;
            Map<String, Double> values = new HashMap<>();
            boolean ended = false;
            while (position + CARD <= prefix.length) {
                int offset = (int) position;
                String key = new String(prefix, offset, 8, StandardCharsets.US_ASCII).trim();
                position += CARD;
                if ("END".equals(key)) {
                    ended = true;
                    break;
                }
                if (prefix[offset + 8] != '=') continue;
                String value = new String(prefix, offset + 10, 70, StandardCharsets.US_ASCII);
                int comment = value.indexOf('/');
                if (comment >= 0) value = value.substring(0, comment);
                try {
                    values.put(key, Double.parseDouble(value.trim().replace('D', 'E')));
                } catch (NumberFormatException ignored) {
                    // Text/string-valued FITS cards are not needed.
                }
            }
            if (!ended) break;
            double candidateRa = values.getOrDefault("RA_OBJ", Double.NaN);
            double candidateDec = values.getOrDefault("DEC_OBJ", Double.NaN);
            if (Double.isFinite(candidateRa) && candidateRa >= 0 && candidateRa < 360
                    && Double.isFinite(candidateDec) && candidateDec >= -90 && candidateDec <= 90) {
                ra = candidateRa;
                dec = candidateDec;
            }
            double candidateT90 = values.getOrDefault("T90", Double.NaN);
            if (Double.isFinite(candidateT90) && candidateT90 >= 0) t90 = candidateT90;

            long headerEnd = headerStart + padded(position - headerStart);
            if (headerEnd < 0 || headerEnd > prefix.length) break;
            long payload = payloadSize(values);
            if (payload < 0) break;
            position = headerEnd + padded(payload);
            if (position < 0 || position > prefix.length) break;
        }
        if (Double.isFinite(ra) && Double.isFinite(dec)) {
            return new SkyBurst(entry.grbName(), entry.triggerId(), ra, dec, t90);
        }
        // T90 can be known even when the FITS position is unavailable.
        return new SkyBurst(entry.grbName(), entry.triggerId(),
                Double.NaN, Double.NaN, t90);
    }

    private static long padded(long bytes) {
        if (bytes < 0 || bytes > Long.MAX_VALUE - (BLOCK - 1)) return -1;
        return ((bytes + BLOCK - 1) / BLOCK) * BLOCK;
    }

    private static long payloadSize(Map<String, Double> headers) {
        int axes = headers.getOrDefault("NAXIS", 0.0).intValue();
        if (axes < 0 || axes > 9) return -1;
        long product = axes == 0 ? 0 : 1;
        try {
            for (int axis = 1; axis <= axes; axis++) {
                double dimension = headers.getOrDefault("NAXIS" + axis, Double.NaN);
                if (!Double.isFinite(dimension) || dimension < 0 || dimension > Integer.MAX_VALUE) return -1;
                product = Math.multiplyExact(product, (long) dimension);
            }
            double bitpix = Math.abs(headers.getOrDefault("BITPIX", 8.0));
            if (!Double.isFinite(bitpix) || bitpix < 8 || bitpix % 8 != 0) return -1;
            long pcount = headers.getOrDefault("PCOUNT", 0.0).longValue();
            long gcount = headers.getOrDefault("GCOUNT", 1.0).longValue();
            if (pcount < 0 || gcount < 1) return -1;
            return Math.multiplyExact(Math.addExact(Math.multiplyExact(product, (long)bitpix / 8), pcount), gcount);
        } catch (ArithmeticException ignored) {
            return -1;
        }
    }
}
