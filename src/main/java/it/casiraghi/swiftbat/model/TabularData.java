package it.casiraghi.swiftbat.model;

import java.util.List;

public record TabularData(List<String> headers, List<List<String>> rows) {
    public static TabularData empty() {
        return new TabularData(List.of(), List.of());
    }

    public int indexOf(String header) {
        for (int index = 0; index < headers.size(); index++) {
            if (headers.get(index).equalsIgnoreCase(header)) {
                return index;
            }
        }
        return -1;
    }

    public boolean isEmpty() {
        return headers.isEmpty() || rows.isEmpty();
    }
}
