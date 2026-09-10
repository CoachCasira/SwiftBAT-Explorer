package it.casiraghi.swiftbat.ui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Piccolo dizionario di compatibilità per testi dinamici e frammenti storici.
 * Viene fuso nel dizionario I18n all'avvio, così anche i controlli che cambiano
 * testo dopo un click e i renderer Java2D seguono sempre IT/EN.
 */
public final class UiTranslationExtras {
    private static final Map<String, String> EN = new LinkedHashMap<>();
    private static final Map<String, String> IT = new LinkedHashMap<>();

    static {
        // Explorer filters.
        put("Cerca GRB o Trigger ID…", "Search GRB or Trigger ID…");
        put("Durata", "Duration");
        put("Tutti", "All");
        put("Solo in cache", "Cached only");
        put("Da scaricare", "To download");
        put("Manuale…", "Manual…");
        put("Azzera filtri extra", "Reset extra filters");
        put("Cache locale", "Local cache");
        put("Altri filtri ▾", "More filters ▾");
        put("Nascondi filtri ▴", "Hide filters ▴");

        // Population filters and dynamic toggle captions.
        put("Filtri avanzati: z e area di cielo", "Advanced filters: z and sky area");
        put("Nascondi filtri avanzati", "Hide advanced filters");
        put("Mostra filtri", "Show filters");
        put("Intervallo redshift z", "Redshift z range");
        put("Ascensione retta RA", "Right ascension RA");
        put("Declinazione DEC", "Declination DEC");
        put("Minimo ammesso", "Minimum allowed");
        put("Massimo ammesso", "Maximum allowed");
        put("Qualità FRACEXP", "FRACEXP quality");
        put("Durata T90", "T90 duration");
        put("Finestra temporale", "Time window");
        put("Campione massimo", "Maximum sample");
        put("Ripristina filtri", "Reset filters");
        put("Analizza il gruppo", "Analyze group");
        put("Annulla", "Cancel");

        // Compact radio labels used in Population Analysis.
        put("Con z", "With z");
        put("Senza z", "Without z");
        put("Tutte le durate", "All durations");
        put("T90 non disponibile", "T90 unavailable");
        put("Con e senza redshift", "With and without redshift");
        put("Solo con redshift", "With redshift only");
        put("Solo senza redshift", "Without redshift only");

        // Sky map labels and controls.
        put("Cielo", "Sky");
        put("Mappa celeste", "Sky map");
        put("Sfera 3D", "3D sphere");
        put("Centra", "Center");
        put("Esporta PNG", "Export PNG");
        put("Schermo intero", "Fullscreen");
        put("Piano galattico", "Galactic plane");
        put("Nessun GRB selezionato", "No GRB selected");
        put("GRB selezionato", "Selected GRB");
        put("Classe descrittiva", "Descriptive class");
        put("Apri curve di luce →", "Open light curves →");
        put("Coordinate celesti non ancora caricate", "Sky coordinates not loaded yet");
        put("Coordinate non disponibili", "Coordinates unavailable");
        put("Caricamento coordinate…", "Loading coordinates…");
        put("T90 non disponibile", "T90 unavailable");

        // Export dialogs and result tables.
        put("Esportazione PNG non riuscita", "PNG export failed");
        put("Nessuna colonna visibile", "No visible columns");
        put("Mostra almeno una colonna prima di esportare.",
                "Show at least one column before exporting.");
        put("Esportazione Excel non riuscita", "Excel export failed");
        put("Esporta Excel", "Export Excel");
        put("GRB inclusi", "Included GRBs");

        // Interactive time-energy map.
        put("Mappa tempo–energia", "Time–energy map");
        put("Mappa tempo-energia", "Time-energy map");
        put("Mappa tempo–energia dei rate", "Time–energy rate map");
        put("Mappa tempo-energia dei rate", "Time-energy rate map");
        put("Apri vista 3D dei rate", "Open 3D rate view");
        put("Mappa non disponibile: servono i quattro canali ASCII.",
                "Map unavailable: all four ASCII channels are required.");
        put("Tempo dal trigger (s)", "Time from trigger (s)");
        put("Banda", "Band");
        put("Centro bin", "Bin center");
        put("Larghezza banda", "Band width");
        put("Fluttuazione negativa", "Negative fluctuation");
        put("Rate circa zero", "Rate near zero");
        put("Rate positivo", "Positive rate");
        put("Clic: fissa · trascina: aggiungi · clic in alto: seleziona l'istante · doppio clic: azzera",
                "Click: pin · drag: add · click the top strip: select time · double-click: reset");
        put("Area selezionata", "Selected area");
        put("Celle selezionate", "Selected cells");
        put("Rate medio", "Mean rate");
        put("Intervallo temporale", "Time interval");
        put("Istante", "Time bin");
        put("Banda dominante", "Dominant band");

        // Literal fragments produced by multiline Java strings. The complete
        // sentences already have translations; these entries keep the source
        // audit strict without reporting harmless compile-time fragments.
        put("il 75° percentile ne lascia sotto il 75%. Tra i due rimane quindi il 50% centrale del campione.",
                "the 75th percentile leaves 75% below it. The interval between them therefore contains the central 50% of the sample.");
        put("la profondità è solo prospettica e non aggiunge una nuova variabile fisica.",
                "depth is only perspective and does not add a new physical variable.");
        put("La profondità serve solo a separare visivamente le curve e non rappresenta T90, distanza o posizione nello spazio.",
                "Depth only separates curves visually and does not represent T90, distance or spatial position.");
        put("una distanza nello spazio né uno spettro continuo. Trascina per ruotare e usa la rotella per lo zoom.",
                "spatial distance or a continuous spectrum. Drag to rotate and use the wheel to zoom.");
    }

    private UiTranslationExtras() { }

    private static void put(String italian, String english) {
        EN.put(italian, english);
        IT.putIfAbsent(english, italian);
    }

    public static Map<String, String> english() {
        return Map.copyOf(EN);
    }

    public static Map<String, String> italian() {
        return Map.copyOf(IT);
    }
}
