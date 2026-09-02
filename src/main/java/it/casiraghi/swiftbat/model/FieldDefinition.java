package it.casiraghi.swiftbat.model;

public record FieldDefinition(
        String category,
        String source,
        String field,
        String unit,
        String simpleExplanation,
        String technicalExplanation,
        String whyItMatters,
        String caution) {

    public String searchableText() {
        return String.join(" ", category, source, field, unit, simpleExplanation,
                technicalExplanation, whyItMatters, caution).toLowerCase();
    }
}
