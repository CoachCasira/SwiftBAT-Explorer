package it.casiraghi.swiftbat.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public final class I18n {
    public enum Language { IT, EN }

    private static final String ORIGINAL_TEXT = I18n.class.getName() + ".originalText";
    private static final Preferences PREFS = Preferences.userNodeForPackage(I18n.class);
    private static final ObjectProperty<Language> LANGUAGE = new SimpleObjectProperty<>(loadLanguage());
    private static final Map<String, String> EN = new LinkedHashMap<>();

    static {
        put("Esplora", "Explore");
        put("Mappa celeste", "Sky map");
        put("Analisi di popolazione", "Population analysis");
        put("Confronta", "Compare");
        put("Informazioni", "Information");
        put("Fonte ufficiale", "Official source");
        put("Fonte ufficiale BAT", "Official BAT source");
        put("Apri tabella ufficiale ↗", "Open official table ↗");
        put("Connessione…", "Connecting…");
        put("Catalogo…", "Catalog…");
        put("in memoria", "in memory");
        put("Online", "Online");
        put("Offline parziale", "Partial offline");
        put("Aggiorna il catalogo online", "Refresh online catalog");
        put("Apri il catalogo ufficiale", "Open official catalog");
        put("Curva 2D", "2D curve");
        put("Vista 3D", "3D view");
        put("Vista 3D interattiva", "Interactive 3D view");
        put("Dati", "Data");
        put("Metadati", "Metadata");
        put("Guida", "Guide");
        put("Spettroscopia", "Spectroscopy");
        put("Analisi spettroscopica", "Spectral analysis");
        put("Risultati ufficiali", "Official results");
        put("Mappa tempo–energia", "Time–energy map");
        put("Guida scientifica", "Scientific guide");
        put("Intervallo del fit", "Fit interval");
        put("Modello del fit", "Fit model");
        put("Modello indicato da BAT", "Model selected by BAT");
        put("Power law (PL)", "Power law (PL)");
        put("Cutoff power law (CPL)", "Cutoff power law (CPL)");
        put("T100", "T100");
        put("Picco 1 s", "1 s peak");
        put("Analizza il gruppo", "Analyze group");
        put("Annulla", "Cancel");
        put("Ripristina filtri", "Reset filters");
        put("Profilo temporale", "Temporal profile");
        put("Distribuzioni del campione", "Sample distributions");
        put("GRB inclusi", "Included GRBs");
        put("Durata T90", "T90 duration");
        put("Durata", "Duration");
        put("Redshift", "Redshift");
        put("Finestra temporale", "Time window");
        put("Campione massimo", "Maximum sample");
        put("Qualità della copertura FRACEXP", "FRACEXP coverage quality");
        put("Minimo ammesso", "Minimum");
        put("Massimo ammesso", "Maximum");
        put("Filtri avanzati: z e area di cielo", "Advanced filters: z and sky area");
        put("Nascondi filtri avanzati", "Hide advanced filters");
        put("Intervallo redshift z", "Redshift z range");
        put("Ascensione retta RA", "Right ascension RA");
        put("Declinazione DEC", "Declination DEC");
        put("Tutte le durate", "All durations");
        put("Short · T90 ≤ 2 s", "Short · T90 ≤ 2 s");
        put("Long · T90 > 2 s", "Long · T90 > 2 s");
        put("T90 non disponibile", "T90 unavailable");
        put("Con e senza redshift", "With and without redshift");
        put("Solo con redshift", "With redshift only");
        put("Solo senza redshift", "Without redshift only");
        put("Tutte", "All");
        put("Tutti", "All");
        put("Con z", "With z");
        put("Senza z", "Without z");
        put("Solo in cache", "Cached only");
        put("Da scaricare", "To download");
        put("Altri filtri ▾", "More filters ▾");
        put("Nascondi filtri ▴", "Hide filters ▴");
        put("Azzera filtri extra", "Reset extra filters");
        put("Cache locale", "Local cache");
        put("Cerca GRB o Trigger ID…", "Search GRB or Trigger ID…");
        put("Cerca keyword, valore, HDU o commento…", "Search keyword, value, HDU or comment…");
        put("Filtra le righe per valore testuale…", "Filter rows by text value…");
        put("Spiega il campo:", "Explain field:");
        put("ASCII → Excel", "ASCII → Excel");
        put("FITS + metadati → Excel", "FITS + metadata → Excel");
        put("Valore", "Value");
        put("Commento originale", "Original comment");
        put("Come leggere i metadati", "How to read metadata");
        put("Picco", "Peak");
        put("Tempo del picco", "Peak time");
        put("Segnale / errore", "Signal / error");
        put("Esposizione completa", "Full exposure");
        put("Durezza", "Hardness");
        put("Ricarica online", "Reload online");
        put("Schermo intero", "Fullscreen");
        put("Mostra spiegazione", "Show explanation");
        put("Nascondi spiegazione", "Hide explanation");
        put("Assistente di lettura", "Reading assistant");
        put("Nessuna analisi eseguita", "No analysis run");
        put("Evento A", "Event A");
        put("Evento B", "Event B");
        put("Normalizza ogni curva sul proprio picco", "Normalize each curve to its own peak");
        put("Apri almeno due GRB", "Open at least two GRBs");
        put("Confronto temporale", "Temporal comparison");
        put("Tempo dal trigger (s)", "Time from trigger (s)");
        put("Rate normalizzato", "Normalized rate");
        put("Rate totale (count/s)", "Total rate (count/s)");
        put("GRB", "GRB");
        put("Classe", "Class");
        put("Copertura", "Coverage");
        put("Flag qualità", "Quality flag");
        put("Mostra colonne", "Show columns");
        put("Ripristina colonne", "Reset columns");
        put("Seleziona colonne dal menu della tabella. La scelta viene salvata automaticamente.",
                "Select columns from the table menu. Your choice is saved automatically.");
        put("Cerca e filtra GRB", "Search and filter GRBs");
        put("Cerca per nome…", "Search by name…");
        put("Intervallo di date", "Date range");
        put("Intervallo di redshift", "Redshift range");
        put("SNR minimo", "Minimum SNR");
        put("Più recenti", "Most recent");
        put("Scegli un Gamma-Ray Burst", "Choose a Gamma-Ray Burst");
        put("Ricarica online", "Reload online");
        put("ASCII + FITS disponibili", "ASCII + FITS available");
        put("Curva di luce", "Light curve");
        put("Totale 15–350 keV", "Total 15–350 keV");
        put("Media mobile 5 bin", "5-bin moving average");
        put("Esporta PNG", "Export PNG");
        put("Centra", "Center");
        put("Sfera 3D", "3D sphere");
        put("Piano galattico", "Galactic plane");
        put("GRB selezionato", "Selected GRB");
        put("Nessun GRB selezionato", "No GRB selected");
        put("Apri curve di luce →", "Open light curves →");
        put("VISIBILI", "VISIBLE");
        put("SHORT", "SHORT");
        put("LONG", "LONG");
        put("SENZA T90", "NO T90");
        put("Tutti i GRB", "All GRBs");
        put("T90 non disponibile", "T90 unavailable");
        put("Reset", "Reset");
        put("Modello migliore BAT", "BAT best model");
        put("Modello mostrato", "Displayed model");
        put("Indice α", "α index");
        put("Esposizione", "Exposure");
        put("Modello spettrale ricostruito", "Reconstructed spectral model");
        put("Energia (keV)", "Energy (keV)");
        put("Modello non disponibile", "Model unavailable");
        put("Flusso energetico", "Energy flux");
        put("Flusso energetico per banda", "Energy flux by band");
        put("Banda energetica (keV)", "Energy band (keV)");
        put("Apri vista 3D dei rate", "Open 3D rate view");
        put("Mappa tempo–energia dei rate", "Rate time–energy map");
        put("Intera osservazione", "Full observation");
        put("Centra vista", "Center view");
        put("Curva", "Curve");
        put("Asse X", "X axis");
        put("Asse Y", "Y axis");
        put("Forma", "Shape");
        put("Confronto", "Comparison");
        put("Da ricordare", "Remember");
        put("Modello", "Model");
        put("Assi", "Axes");
        put("Forma della curva", "Curve shape");
        put("Profondità", "Depth");
        put("Interazione", "Interaction");
        put("Interpretazione", "Interpretation");
        put("Come leggere il modello 2D", "How to read the 2D model");
        put("Come leggere la vista 3D", "How to read the 3D view");
        put("Come leggere l'istogramma", "How to read the histogram");
        put("Come leggere il flusso 3D", "How to read the 3D flux view");
        put("Come leggere la mappa", "How to read the map");
        put("Qualità FRACEXP", "FRACEXP quality");
        put("Distanza cosmologica", "Cosmological distance");
        put("Mediana", "Median");
        put("Singoli GRB", "Individual GRBs");
        put("Fascia centrale 25°–75°", "Central 25th–75th percentile band");
        put("Da tenere presente", "Keep in mind");
        put("Copertura completa", "Full coverage");
        put("Caricamento coordinate…", "Loading coordinates…");
        put("Coordinate non disponibili", "Coordinates unavailable");
        put("Dati scientifici caricati parzialmente", "Scientific data partially loaded");
        put("Esporta mappa celeste", "Export sky map");
        put("Immagine PNG", "PNG image");
        put("Elaborazione del campione…", "Processing sample…");
        put("Analisi annullata", "Analysis cancelled");
        put("Nessuno", "None");
        put("corrispondenze", "matches");
    }

    private I18n() {}

    private static void put(String it, String en) { EN.put(it, en); }

    private static Language loadLanguage() {
        try { return Language.valueOf(PREFS.get("language", "IT")); }
        catch (Exception ignored) { return Language.IT; }
    }

    public static Language language() { return LANGUAGE.get(); }
    public static ObjectProperty<Language> languageProperty() { return LANGUAGE; }

    public static void setLanguage(Language value) {
        Language next = value == null ? Language.IT : value;
        LANGUAGE.set(next);
        PREFS.put("language", next.name());
    }

    public static String t(String text) {
        if (text == null || language() == Language.IT) return text;
        String direct = EN.get(text);
        if (direct != null) return direct;
        // Preserve glyph prefixes used by navigation buttons while translating their label.
        int firstLetter = -1;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) { firstLetter = i; break; }
        }
        if (firstLetter > 0) {
            String tail = text.substring(firstLetter);
            String translated = EN.get(tail);
            if (translated != null) return text.substring(0, firstLetter) + translated;
        }
        return text;
    }

    public static void localizeLabeled(Labeled control) {
        if (control == null) return;
        String original = (String) control.getProperties().get(ORIGINAL_TEXT);
        if (original == null) {
            original = control.getText();
            control.getProperties().put(ORIGINAL_TEXT, original);
        }
        control.setText(t(original));
        if (control.getTooltip() != null) {
            control.getTooltip().setText(t(control.getTooltip().getText()));
        }
    }

    public static <T> void installChoiceBox(ChoiceBox<T> choice) {
        if (choice == null) return;
        StringConverter<T> converter = new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : t(value.toString()); }
            @Override public T fromString(String text) {
                for (T value : choice.getItems()) {
                    if (value != null && (value.toString().equals(text) || t(value.toString()).equals(text))) return value;
                }
                return choice.getValue();
            }
        };
        choice.setConverter(converter);
        LANGUAGE.addListener((obs, oldValue, newValue) -> choice.requestLayout());
    }

    public static <T> void installComboBox(ComboBox<T> combo) {
        if (combo == null) return;
        StringConverter<T> converter = new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : t(value.toString()); }
            @Override public T fromString(String text) {
                for (T value : combo.getItems()) {
                    if (value != null && (value.toString().equals(text) || t(value.toString()).equals(text))) return value;
                }
                return combo.getValue();
            }
        };
        combo.setConverter(converter);
        LANGUAGE.addListener((obs, oldValue, newValue) -> combo.requestLayout());
    }

    public static void localizeTree(Node node) {
        if (node == null) return;
        if (node instanceof Labeled labeled) localizeLabeled(labeled);
        if (node instanceof TextInputControl input) {
            String key = ORIGINAL_TEXT + ".prompt";
            String original = (String) input.getProperties().get(key);
            if (original == null) {
                original = input.getPromptText();
                input.getProperties().put(key, original);
            }
            input.setPromptText(t(original));
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                Object original = tab.getProperties().get(ORIGINAL_TEXT);
                if (original == null) {
                    original = tab.getText();
                    tab.getProperties().put(ORIGINAL_TEXT, original);
                }
                tab.setText(t((String) original));
                if (tab.getContent() != null) localizeTree(tab.getContent());
            }
        }
        if (node instanceof TableView<?> table) {
            for (TableColumn<?, ?> column : table.getColumns()) localizeColumn(column);
        }
        if (node instanceof ChoiceBox<?> choice) choice.requestLayout();
        if (node instanceof ComboBox<?> combo) combo.requestLayout();
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) localizeTree(child);
        }
    }

    private static void localizeColumn(TableColumn<?, ?> column) {
        String original = (String) column.getProperties().get(ORIGINAL_TEXT);
        if (original == null) {
            original = column.getText();
            column.getProperties().put(ORIGINAL_TEXT, original);
        }
        column.setText(t(original));
        if (column.getGraphic() != null) localizeTree(column.getGraphic());
        for (TableColumn<?, ?> child : column.getColumns()) localizeColumn(child);
    }
}
