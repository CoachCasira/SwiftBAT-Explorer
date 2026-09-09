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
    private static final Map<String, String> IT = new LinkedHashMap<>();
    private static final String LOCALIZED_IT = I18n.class.getName() + ".localized.it";
    private static final String LOCALIZED_EN = I18n.class.getName() + ".localized.en";

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
        // STRICT_I18N_BATCH_1
        put("Dati scientifici online\nNASA/GSFC Swift/BAT", "Online scientific data\nNASA/GSFC Swift/BAT");
        put("Fonte ufficiale  ↗", "Official source  ↗");
        put("Coordinate celesti…", "Sky coordinates…");
        put("Coordinate e T90 caricati; redshift temporaneamente non disponibile", "Coordinates and T90 loaded; redshift is temporarily unavailable");
        put("Coordinate celesti non disponibili", "Sky coordinates unavailable");
        put("Recupero i prodotti Swift/BAT online.", "Retrieving Swift/BAT products online.");
        put("In memoria", "In memory");
        put("Esplora i Gamma-Ray Burst dal catalogo al cielo. Curve di luce, dati FITS e coordinate celesti in un'unica app.", "Explore Gamma-Ray Bursts from catalog to sky. Light curves, FITS data and sky coordinates in one app.");
        put("Esplora i GRB  →", "Explore GRBs  →");
        put("Apri la mappa celeste", "Open sky map");
        put("Cerca un evento e apri curve, dati e metadati.", "Search for an event and open curves, data and metadata.");
        put("Apri catalogo", "Open catalog");
        put("Guarda i GRB sulla Mollweide o sulla sfera 3D.", "View GRBs on the Mollweide map or on the 3D sphere.");
        put("Esplora il cielo", "Explore the sky");
        put("Sovrapponi due eventi già aperti nella sessione.", "Overlay two events already open in the session.");
        put("Confronta eventi", "Compare events");
        put("Tre passaggi, niente file manuali", "Three steps, no manual files");
        put("Scegli un GRB", "Choose a GRB");
        put("Cerca nome o Trigger ID.", "Search by name or Trigger ID.");
        put("Aprilo", "Open it");
        put("L'app recupera e interpreta i prodotti Swift/BAT online.", "The app retrieves and interprets Swift/BAT products online.");
        put("Passa da curva, 3D, tabelle, metadati e mappa celeste.", "Move between curves, 3D views, tables, metadata and the sky map.");
        put("Serve una spiegazione?", "Need an explanation?");
        put("Le schermate mantengono il dato originale e affiancano spiegazioni brevi per trigger, rate, errori, FRACEXP, FITS, RA, DEC e T90.", "Screens preserve the original data and provide short explanations for trigger, rate, errors, FRACEXP, FITS, RA, DEC and T90.");
        put("Apri info e guida", "Open info and guide");
        put("SwiftBAT Explorer è l'applicazione sviluppata per la tesi di Matteo Casiraghi per consultare e comprendere i prodotti pubblici Swift/BAT dei Gamma-Ray Burst.", "SwiftBAT Explorer is the application developed for Matteo Casiraghi's thesis to inspect and understand public Swift/BAT Gamma-Ray Burst products.");
        put("Versione 1.2.0 · Java 17 · dati online", "Version 1.2.0 · Java 17 · online data");
        put("Apri guida ai dati  →", "Open data guide  →");
        put("Catalogo ufficiale NASA/GSFC Swift/BAT e prodotti DAT/FITS a binning di 1 secondo. I valori restano riconducibili alle sorgenti pubbliche usate dall'app.", "Official NASA/GSFC Swift/BAT catalog and 1-second-binned DAT/FITS products. Values remain traceable to the public sources used by the app.");
        put("Curve", "Curves");
        put("Visualizzazione delle curve di luce totali e nelle quattro bande energetiche, con finestre temporali, zoom e viste dedicate per leggere meglio la struttura del burst.", "Total and four-band light-curve visualization, with time windows, zoom and dedicated views for inspecting burst structure.");
        put("Volta celeste", "Sky distribution");
        put("Mollweide 2D e sfera 3D costruite con le coordinate RA/DEC pubblicate da Swift/BAT. Le due viste mostrano lo stesso campione con rappresentazioni differenti.", "2D Mollweide map and 3D sphere built from RA/DEC coordinates published by Swift/BAT. Both views show the same sample with different representations.");
        put("Sessione", "Session");
        put("Gli eventi aperti restano in memoria finché l'app è in esecuzione. La cache locale evita download ripetuti senza modificare i prodotti scientifici sorgente.", "Open events remain in memory while the app is running. The local cache avoids repeated downloads without modifying the source scientific products.");
        put("Scopo scientifico", "Scientific scope");
        put("L'app facilita consultazione, controllo e confronto descrittivo dei dati. Gli indicatori e la soglia T90 = 2 s mostrati nell'interfaccia non sostituiscono una classificazione astrofisica validata.", "The app supports data inspection, checking and descriptive comparison. Indicators and the T90 = 2 s threshold shown in the interface do not replace a validated astrophysical classification.");
        put("Lettura dei risultati", "Reading results");
        put("Grafici, mappe, filtri e assistenti di lettura servono a mettere in evidenza pattern e differenze nel campione. Le viste 2D e 3D sono strumenti esplorativi e mantengono sempre separata la rappresentazione grafica dall'interpretazione fisica.", "Charts, maps, filters and reading aids highlight patterns and differences in the sample. The 2D and 3D views are exploratory tools and keep graphical representation separate from physical interpretation.");
        put("Catalogo Swift/BAT  ↗", "Swift/BAT catalog  ↗");
        put("Tabella redshift BAT  ↗", "BAT redshift table  ↗");
        put("Dizionario dei dati", "Data dictionary");
        put("Ogni voce è spiegata prima in parole semplici e poi in modo tecnico. Qui puoi cercare TIME, RATE, FRACEXP, TRIGTIME, OBS_ID e gli altri campi presenti nelle tabelle e nei FITS.", "Each entry is explained first in simple terms and then technically. You can search TIME, RATE, FRACEXP, TRIGTIME, OBS_ID and the other fields found in tables and FITS files.");
        put("Cerca un campo o un concetto…", "Search for a field or concept…");
        put("senza unità", "no unit");
        put("In parole semplici", "In simple terms");
        put("Descrizione tecnica", "Technical description");
        put("Perché è utile", "Why it matters");
        put("Attenzione a non confonderlo", "Do not confuse it with");
        put("Azioni grafico", "Chart actions");
        put("In breve", "At a glance");
        put("Trigger", "Trigger");
        put("Il punto zero dell'allerta", "The alert zero point");
        put("Curva di luce a binning di 1 secondo", "1-second-binned light curve");
        put("ASCII — quattro bande", "ASCII — four bands");
        put("FITS — un canale e qualità", "FITS — one channel and quality");
        put("Una lettura guidata dell'evento", "Guided event reading");
        put("Indicatori calcolati per questo evento", "Indicators calculated for this event");
        put("Limite scientifico importante", "Important scientific limitation");
        put("Picco / errore", "Peak / error");
        put("Durezza proxy", "Hardness proxy");
        put("Tempo", "Time");
        put("Segnale", "Signal");
        put("Incertezza", "Uncertainty");
        put("Conteggi", "Counts");
        put("Qualità", "Quality");
        put("Segnale per energia", "Signal by energy");
        put("Incertezza per energia", "Uncertainty by energy");
        put("Metadato", "Metadata field");
        put("ASCII e FITS", "ASCII and FITS");
    }

    private I18n() {}

    private static void put(String it, String en) {
        EN.put(it, en);
        IT.putIfAbsent(en, it);
    }

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
        if (text == null) return null;
        if (language() == Language.IT) {
            return translateDirect(text, IT, EN);
        }
        String translated = translateDirect(text, EN, IT);
        if (!translated.equals(text)) return translated;
        if (IT.containsKey(text)) return text;
        return requiresTranslation(text) ? "[Missing English translation]" : text;
    }

    private static String translateDirect(String text, Map<String, String> target, Map<String, String> reverse) {
        String direct = target.get(text);
        if (direct != null) return direct;
        if (reverse.containsKey(text)) return text;
        int firstLetter = -1;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) { firstLetter = i; break; }
        }
        if (firstLetter > 0) {
            String prefix = text.substring(0, firstLetter);
            String tail = text.substring(firstLetter);
            String translated = target.get(tail);
            if (translated != null) return prefix + translated;
            if (reverse.containsKey(tail)) return text;
        }
        return text;
    }

    public static String english(String italian) {
        if (italian == null) return null;
        String translated = translateDirect(italian, EN, IT);
        return translated.equals(italian) && requiresTranslation(italian)
                ? "[Missing English translation]" : translated;
    }

    public static boolean hasEnglish(String italian) {
        if (italian == null || italian.isBlank()) return true;
        return EN.containsKey(italian) || !requiresTranslation(italian);
    }

    public static boolean requiresTranslation(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("http") || lower.contains("-fx-") || lower.matches("[-a-z0-9_./:+#%]+")) return false;
        String[] markers = {
                " il ", " lo ", " la ", " gli ", " le ", " un ", " una ", " di ", " del ", " della ",
                " dei ", " delle ", " e ", " è ", " per ", " con ", " senza ", " non ", " dal ", " nel ",
                " nella ", " nelle ", " mostra", "nascondi", "apri", "scegli", "cerca", "curva", "dati",
                "mappa", "analisi", "durata", "tempo", "valore", "qualità", "spiegazione", "disponibile",
                "intervallo", "flusso", "modello", "energia", "banda", "tabella", "righe", "esposizione",
                "guida", "informazioni", "evento", "confront", "schermo", "descrizione", "caric", "campione",
                "coordinate", "celeste", "sessione", "lettura", "risultati", "errore", "picco", "durezza",
                "fonte", "ufficiale", "filtro", "filtri", "visualizz", "nessun", "nessuna", "tutte", "tutti"
        };
        String padded = " " + lower + " ";
        for (String marker : markers) {
            if (padded.contains(marker)) return true;
        }
        return lower.matches(".*[àèéìòù].*");
    }

    public static String dynamic(String italian, String english) {
        return language() == Language.EN ? english : italian;
    }

    public static void setText(Labeled control, String italian, String english) {
        if (control == null) return;
        control.getProperties().put(LOCALIZED_IT, italian == null ? "" : italian);
        control.getProperties().put(LOCALIZED_EN, english == null ? "" : english);
        control.setText(language() == Language.EN ? english : italian);
    }

    public static void localizeLabeled(Labeled control) {
        if (control == null) return;
        Object bilingualIt = control.getProperties().get(LOCALIZED_IT);
        Object bilingualEn = control.getProperties().get(LOCALIZED_EN);
        if (bilingualIt instanceof String it && bilingualEn instanceof String en) {
            control.setText(language() == Language.EN ? en : it);
        } else {
            String original = (String) control.getProperties().get(ORIGINAL_TEXT);
            if (original == null) {
                original = control.getText();
                control.getProperties().put(ORIGINAL_TEXT, original);
            }
            control.setText(t(original));
        }
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
        if (node instanceof javafx.scene.chart.Chart chart && chart.getTitle() != null) {
            chart.setTitle(t(chart.getTitle()));
        }
        if (node instanceof javafx.scene.chart.Axis<?> axis && axis.getLabel() != null) {
            axis.setLabel(t(axis.getLabel()));
        }
        if (node instanceof javafx.scene.chart.XYChart<?, ?> xyChart) {
            for (Object raw : xyChart.getData()) {
                javafx.scene.chart.XYChart.Series<?, ?> series = (javafx.scene.chart.XYChart.Series<?, ?>) raw;
                if (series.getName() != null) series.setName(t(series.getName()));
            }
        }
        if (node instanceof MenuButton menuButton) {
            for (MenuItem item : menuButton.getItems()) {
                Object original = item.getProperties().get(ORIGINAL_TEXT);
                if (original == null) {
                    original = item.getText();
                    item.getProperties().put(ORIGINAL_TEXT, original);
                }
                item.setText(t((String) original));
            }
        }
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
