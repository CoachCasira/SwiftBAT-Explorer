package it.casiraghi.swiftbat.model;

public record CatalogEntry(
        String grbName,
        String triggerId,
        String dataProductUrl) {

    public String displayName() {
        return grbName + "  ·  Trigger " + triggerId;
    }
}
