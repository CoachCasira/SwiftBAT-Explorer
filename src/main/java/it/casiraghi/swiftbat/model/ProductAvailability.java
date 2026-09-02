package it.casiraghi.swiftbat.model;

public record ProductAvailability(
        boolean asciiAvailable,
        boolean fitsAvailable,
        String asciiFileName,
        String fitsFileName,
        String dataProductUrl,
        String resultsUrl,
        String lightCurveDirectoryUrl) {

    public String statusText() {
        if (asciiAvailable && fitsAvailable) {
            return "ASCII + FITS disponibili";
        }
        if (asciiAvailable) {
            return "Solo ASCII disponibile";
        }
        if (fitsAvailable) {
            return "Solo FITS disponibile";
        }
        return "Prodotti 1 s non disponibili";
    }
}
