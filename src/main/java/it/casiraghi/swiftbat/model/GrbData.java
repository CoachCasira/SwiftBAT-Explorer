package it.casiraghi.swiftbat.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GrbData(
        String grbName,
        String triggerId,
        Instant loadedAt,
        ProductAvailability availability,
        List<SummaryItem> summary,
        TabularData asciiData,
        TabularData fitsData,
        List<MetadataItem> metadata,
        List<FieldDefinition> dictionary) {

    public Map<String, SummaryItem> summaryByKey() {
        Map<String, SummaryItem> result = new LinkedHashMap<>();
        for (SummaryItem item : summary) {
            result.put(item.key(), item);
        }
        return result;
    }

    public String summaryValue(String key, String fallback) {
        SummaryItem item = summaryByKey().get(key);
        return item == null || item.value() == null || item.value().isBlank()
                ? fallback
                : item.value();
    }

    public FieldDefinition definition(String field) {
        for (FieldDefinition definition : dictionary) {
            if (definition.field().equalsIgnoreCase(field)) {
                return definition;
            }
        }
        return null;
    }
}
