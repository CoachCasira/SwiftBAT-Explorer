package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.SkyBurst;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reconstructs partial scientific metadata from authentic, locally cached FITS
 * products. Unknown positions and T90 values stay unknown, never fabricated.
 */
public final class OfflineScientificMetadataService {
    private final PersistentGrbCache cache;

    public OfflineScientificMetadataService() {
        this(new PersistentGrbCache());
    }

    OfflineScientificMetadataService(PersistentGrbCache cache) {
        this.cache = cache;
    }

    public List<SkyBurst> fromCachedProducts(List<CatalogEntry> entries) {
        List<SkyBurst> bursts = new ArrayList<>();
        for (CatalogEntry entry : entries) {
            SkyBurst candidate = SkyBurst.withoutCoordinates(entry.grbName(), entry.triggerId());
            try {
                var product = cache.read(entry);
                if (product.isPresent() && product.get().fitsBytes() != null) {
                    candidate = fromFits(entry, product.get().fitsBytes());
                }
            } catch (IOException | RuntimeException ignored) {
                // A single damaged FITS must not block offline analysis.
            }
            bursts.add(candidate);
        }
        return List.copyOf(bursts);
    }

    private SkyBurst fromFits(CatalogEntry entry, byte[] bytes) throws IOException {
        try (Fits fits = new Fits(new ByteArrayInputStream(bytes))) {
            BasicHDU<?>[] hdus = fits.read();
            double ra = Double.NaN;
            double dec = Double.NaN;
            Double t90 = null;
            for (BasicHDU<?> hdu : hdus) {
                Header header = hdu.getHeader();
                double candidateRa = header.getDoubleValue("RA_OBJ", Double.NaN);
                double candidateDec = header.getDoubleValue("DEC_OBJ", Double.NaN);
                if (Double.isFinite(candidateRa) && candidateRa >= 0 && candidateRa < 360
                        && Double.isFinite(candidateDec) && candidateDec >= -90 && candidateDec <= 90) {
                    ra = candidateRa;
                    dec = candidateDec;
                }
                double candidateT90 = header.getDoubleValue("T90", Double.NaN);
                if (Double.isFinite(candidateT90) && candidateT90 >= 0) {
                    t90 = candidateT90;
                }
            }
            if (Double.isFinite(ra) && Double.isFinite(dec)) {
                return new SkyBurst(entry.grbName(), entry.triggerId(), ra, dec, t90);
            }
            return SkyBurst.withoutCoordinates(entry.grbName(), entry.triggerId());
        } catch (RuntimeException error) {
            throw new IOException("Impossibile leggere le intestazioni FITS offline.", error);
        }
    }
}
