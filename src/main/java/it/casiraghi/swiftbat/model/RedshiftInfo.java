package it.casiraghi.swiftbat.model;

import java.util.List;

/**
 * Valore di redshift come pubblicato dal catalogo BAT.
 *
 * <p>Il testo originale non viene mai perso: alcuni record sono limiti, intervalli,
 * alternative o valori dubbi e non devono essere presentati come misure esatte.</p>
 */
public record RedshiftInfo(
        String rawValue,
        String method,
        String uncertainty,
        String reference,
        Double lowerBound,
        Double upperBound,
        List<Double> alternatives,
        boolean uncertain) {

    public RedshiftInfo {
        rawValue = clean(rawValue);
        method = clean(method);
        uncertainty = clean(uncertainty);
        reference = clean(reference);
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
    }

    public static RedshiftInfo missing() {
        return new RedshiftInfo("", "", "", "", null, null, List.of(), false);
    }

    public boolean available() {
        return !rawValue.isBlank();
    }

    public boolean exact() {
        return available() && lowerBound != null && upperBound != null
                && Double.compare(lowerBound, upperBound) == 0 && alternatives.size() <= 1;
    }

    /** Valore utilizzabile per grafici descrittivi; non sostituisce il testo originale. */
    public Double representativeValue() {
        if (!alternatives.isEmpty()) {
            return alternatives.get(0);
        }
        if (lowerBound == null && upperBound == null) {
            return null;
        }
        if (lowerBound == null) {
            return upperBound;
        }
        if (upperBound == null) {
            return lowerBound;
        }
        return (lowerBound + upperBound) / 2.0;
    }

    /**
     * Verifica la compatibilità con un intervallo. Per limiti/intervalli basta una
     * sovrapposizione; per valori alternativi ne deve rientrare almeno uno.
     */
    public boolean matches(double minimum, double maximum) {
        if (!available() || minimum > maximum) {
            return false;
        }
        if (!alternatives.isEmpty()) {
            return alternatives.stream().anyMatch(value -> value >= minimum && value <= maximum);
        }
        double low = lowerBound == null ? 0.0 : lowerBound;
        double high = upperBound == null ? Double.POSITIVE_INFINITY : upperBound;
        return high >= minimum && low <= maximum;
    }

    public String displayValue() {
        return available() ? "z = " + rawValue : "z non disponibile";
    }

    public String detail() {
        if (!available()) {
            return "Redshift non disponibile nella tabella BAT.";
        }
        StringBuilder text = new StringBuilder(displayValue());
        if (!method.isBlank() && !method.equalsIgnoreCase("N/A")) {
            text.append(" · metodo ").append(method);
        }
        if (uncertain) {
            text.append(" · valore indicato come incerto");
        }
        return text.toString();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().replace('\u00A0', ' ');
    }
}
