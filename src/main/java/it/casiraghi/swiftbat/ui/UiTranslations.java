package it.casiraghi.swiftbat.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.util.StringConverter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Supplemental localization layer for every visible UI surface.
 *
 * <p>It deliberately sits on top of the historical {@link I18n} dictionary:
 * existing translations keep working, while redesign/runtime strings and the
 * custom Java2D/3D renderers share one extra dictionary. The tree localizer is
 * binding-safe, therefore switching language can never write into a bound
 * JavaFX property.</p>
 */
public final class UiTranslations {
    private static final String UI_FACTORY_ORIGINAL = "swiftbat.originalText";
    private static final String ORIGINAL_TEXT = UiTranslations.class.getName() + ".originalText";
    private static final String PROMPT_TEXT = UiTranslations.class.getName() + ".promptText";
    private static final String I18N_LOCALIZED_IT = I18n.class.getName() + ".localized.it";
    private static final String I18N_LOCALIZED_EN = I18n.class.getName() + ".localized.en";
    private static final Map<String, String> EN = new LinkedHashMap<>();
    private static final Map<String, String> IT = new LinkedHashMap<>();

    static {
        put("Passa sulla curva per leggere i dati; clic per selezionarla. Doppio clic nel grafico per azzerare la selezione. Clic sul riquadro esterno per lo schermo intero.",
                "Hover over a curve for data; click to select it. Double-click inside the plot to clear the selection. Click the surrounding card for fullscreen.");
        put("Rate originale", "Original rate");
        put("Errore sul rate originale", "Original rate error");
        // Home / shell.
        put("Benvenuto su", "Welcome to");
        put("Esplora, analizza e interpreta i lampi di raggi gamma con i dati di Swift/BAT.",
                "Explore, analyze and interpret gamma-ray bursts with Swift/BAT data.");
        put("“Dove l'Universo diventa estremo, inizia la scoperta.”",
                "“Where the Universe becomes extreme, discovery begins.”");
        put("GRB nel catalogo", "GRBs in catalog");
        put("Eventi Swift/BAT", "Swift/BAT events");
        put("Connessione", "Connection");
        put("Accesso ai dati online", "Online data access");
        put("File in cache", "Cached files");
        put("Sessione e cache locale", "Session and local cache");
        put("Binning curve di luce", "Light-curve binning");
        put("AI CONFINI DELL'UNIVERSO PIÙ ESTREMO", "AT THE EDGE OF THE MOST EXTREME UNIVERSE");
        put("I lampi di raggi gamma", "Gamma-ray bursts");
        put("illuminano l'Universo estremo", "light up the extreme Universe");
        put("Esplora il catalogo Swift/BAT, visualizza gli eventi sulla mappa celeste e analizza curve di luce, spettroscopia e proprietà di popolazione senza uscire dall'app.",
                "Explore the Swift/BAT catalog, view events on the sky map and analyze light curves, spectroscopy and population properties without leaving the app.");
        put("Apri Esplora", "Open Explore");
        put("Nuova analisi", "New analysis");
        put("Sessione", "Session");
        put("Eventi già aperti restano in RAM", "Already opened events remain in RAM");
        put("I prodotti locali vengono riutilizzati", "Local products are reused");
        put("Il confronto usa i GRB della sessione", "Comparison uses GRBs from the current session");
        put("Catalogo scientifico online", "Online scientific catalog");
        put("Curve a binning di 1 secondo", "1-second-binned light curves");
        put("ASCII, FITS e metadati integrati", "Integrated ASCII, FITS and metadata");
        put("Scorciatoie", "Shortcuts");
        put("Cerca un GRB", "Search for a GRB");
        put("Apri mappa celeste", "Open sky map");
        put("Confronta eventi", "Compare events");
        put("Strumenti", "Tools");
        put("Mollweide 2D e sfera 3D", "2D Mollweide and 3D sphere");
        put("Confronto e spettroscopia", "Comparison and spectroscopy");
        put("Apri", "Open");
        put("Un progetto per la scienza aperta", "A project for open science");
        put("Dati scientifici NASA/GSFC Swift/BAT", "NASA/GSFC Swift/BAT scientific data");
        put("Esplora i lampi di raggi gamma", "Explore gamma-ray bursts");
        put("Cerca un GRB (es. GRB250605A, 231107A, …)",
                "Search for a GRB (e.g. GRB250605A, 231107A, …)");

        // Explorer and generic runtime messages.
        put("GRB di emergenza", "fallback GRBs");
        put("GRB nel catalogo online", "GRBs in the online catalog");
        put("Dati non disponibili per", "Data unavailable for");
        put("L'evento rimane nel catalogo, ma la struttura online può essere incompleta o diversa da quella standard.",
                "The event remains in the catalog, but the online structure may be incomplete or differ from the standard layout.");
        put("GRB visualizzati", "GRBs displayed");
        put("Usa il catalogo a sinistra. Non devi scaricare o caricare manualmente alcun file: l'app raggiunge i prodotti online e costruisce l'area di lavoro.",
                "Use the catalog on the left. You do not need to download or upload files manually: the app retrieves the online products and builds the workspace.");
        put("in memoria nella sessione", "in session memory");
        put("Valore completo:", "Full value:");
        put("Esportazione non riuscita:", "Export failed:");
        put("Esportazione Excel non riuscita:", "Excel export failed:");
        put("Esportazione PNG non riuscita:", "PNG export failed:");

        // Population analysis.
        put("Filtri in preparazione…", "Preparing filters…");
        put("Attendo catalogo e metadati", "Waiting for catalog and metadata");
        put("Applicato ai GRB con z", "Applied to GRBs with z");
        put("Intervallo 0°–360°", "Range 0°–360°");
        put("Intervallo −90°–+90°", "Range −90°–+90°");
        put("A ogni secondo si ordinano i valori delle curve: il 25° percentile lascia sotto di sé il 25% dei valori, il 75° percentile ne lascia sotto il 75%. Tra i due rimane quindi il 50% centrale del campione.",
                "At each second the curve values are ordered: the 25th percentile leaves 25% of values below it and the 75th percentile leaves 75% below it. The interval between them therefore contains the central 50% of the sample.");
        put("Profilo temporale della popolazione", "Population temporal profile");
        put("Profilo di popolazione 3D", "3D population profile");
        put("Le classi sull'asse X sono intervalli di copertura; la profondità separa short, long e GRB senza T90.",
                "X-axis classes are coverage intervals; depth separates short, long and GRBs without T90.");
        put("Durata T90 · vista 3D", "T90 duration · 3D view");
        put("Le classi sull'asse X sono intervalli di T90; la profondità separa gli eventi con e senza redshift disponibile.",
                "X-axis classes are T90 intervals; depth separates events with and without an available redshift.");
        put("Le classi sull'asse X sono intervalli di redshift; la profondità separa short, long e GRB senza T90.",
                "X-axis classes are redshift intervals; depth separates short, long and GRBs without T90.");
        put("Quanti GRB hanno una determinata percentuale di bin completamente esposti",
                "How many GRBs have a given percentage of fully exposed bins");
        put("Quanti GRB inclusi ricadono in ciascun intervallo di durata",
                "How many included GRBs fall in each duration interval");
        put("Distribuzione del redshift dei GRB inclusi; n.d. indica un valore assente",
                "Redshift distribution of included GRBs; n/a indicates a missing value");
        put("Nessun GRB incluso. Controlla i filtri oppure esegui una nuova analisi.",
                "No GRBs included. Check the filters or run a new analysis.");
        put("Nessun GRB corrisponde ai filtri di T90, z e cielo",
                "No GRB matches the T90, z and sky filters");
        put("Caricamento parallelo", "Parallel loading");
        put("fuori dal filtro", "outside the filter");
        put("non leggibili", "unreadable");
        put("Analisi non riuscita:", "Analysis failed:");
        put("errore sconosciuto", "unknown error");
        put("curve normalizzate e allineate a t = 0", "curves normalized and aligned at t = 0");
        put("Durata T90 (s)", "T90 duration (s)");
        put("Disponibilità redshift", "Redshift availability");
        put("Con redshift", "With redshift");
        put("Senza redshift", "Without redshift");
        put("Controlla il range del redshift.", "Check the redshift range.");
        put("RA deve essere fra 0° e 360°.", "RA must be between 0° and 360°.");
        put("DEC deve essere fra −90° e +90°.", "DEC must be between −90° and +90°.");
        put("Il minimo FRACEXP non può superare il massimo.", "The minimum FRACEXP cannot exceed the maximum.");
        put("non è un numero valido.", "is not a valid number.");
        put("deve essere compresa fra 0% e 100%.", "must be between 0% and 100%.");
        put("GRB con T90/coordinate · redshift integrato", "GRBs with T90/coordinates · integrated redshift");
        put("GRB corrispondono ai filtri preliminari", "GRBs match the preliminary filters");
        put("già in RAM", "already in RAM");
        put("Ripristina il minimo a 0%", "Reset minimum to 0%");
        put("Ripristina il massimo a 100%", "Reset maximum to 100%");

        // Sky map.
        put("Coordinate celesti non ancora caricate", "Sky coordinates not loaded yet");
        put("GRB con coordinate BAT caricati", "GRBs with BAT coordinates loaded");
        put("dopo i filtri", "after filters");
        put("durata non disponibile", "duration unavailable");
        put("Cerca GRB…", "Search GRB…");
        put("Mappa celeste · Sfera 3D", "Sky map · 3D sphere");
        put("Mappa celeste · Mollweide 2D", "Sky map · 2D Mollweide");
        put("Seleziona un punto sulla mappa.", "Select a point on the map.");
        put("Seleziona un GRB direttamente nella vista a schermo intero. RA e DEC descrivono la direzione sulla volta celeste; T90 riassume la durata dell'evento e il redshift, quando disponibile, fornisce l'informazione cosmologica. I dettagli rimangono visibili mentre esplori la mappa e puoi aprire subito le relative curve di luce.",
                "Select a GRB directly in fullscreen view. RA and DEC describe its direction on the sky; T90 summarizes event duration and redshift, when available, provides cosmological information. Details remain visible while you explore the map and you can immediately open the corresponding light curves.");
        put("Come leggere la selezione", "How to read the selection");
        put("Evento presente nel catalogo Swift/BAT: puoi aprire direttamente curve, FITS e metadati.",
                "Event present in the Swift/BAT catalog: you can directly open curves, FITS and metadata.");
        put("Coordinate disponibili, ma l'evento non è presente nel catalogo BAT caricato dall'Explorer.",
                "Coordinates are available, but the event is not present in the BAT catalog loaded by Explorer.");
        put("Redshift non disponibile nella tabella BAT.", "Redshift unavailable in the BAT table.");
        put("valore indicato come incerto", "value marked as uncertain");
        put("RA deve essere compresa tra 0° e 360°.", "RA must be between 0° and 360°.");
        put("Controlla il range DEC: deve essere tra −90° e +90°.",
                "Check the DEC range: it must be between −90° and +90°.");

        // Spectroscopy and its fullscreen/3D views.
        put("DATI UFFICIALI BAT · FIT XSPEC", "OFFICIAL BAT DATA · XSPEC FIT");
        put("Nessun risultato ufficiale disponibile", "No official result available");
        put("Il catalogo spettroscopico BAT non contiene ancora", "The BAT spectral catalog does not yet contain");
        put("per l'intervallo", "for the interval");
        put("La curva di luce e la mappa tempo–energia rimangono comunque utilizzabili.",
                "The light curve and time–energy map remain available.");
        put("Un aggiornamento dell'app rilegge automaticamente le tabelle online.",
                "Refreshing the app automatically reloads the online tables.");
        put("Nessun modello preferito pubblicato", "No preferred model published");
        put("scelta del catalogo", "catalog choice");
        put("fit non disponibile", "fit unavailable");
        put("non previsto dal modello PL", "not defined by the PL model");
        put("picco νFν del modello CPL", "νFν peak of the CPL model");
        put("qualità del fit", "fit quality");
        put("gradi di libertà", "degrees of freedom");
        put("intervallo non disponibile", "interval unavailable");
        put("Curva del fit", "Fit curve");
        put("Flusso energetico (erg cm⁻² s⁻¹)", "Energy flux (erg cm⁻² s⁻¹)");
        put("Flusso:", "Flux:");
        put("Intervallo 90%:", "90% interval:");
        put("Modello spettrale 3D", "3D spectral model");
        put("Banda energetica", "Energy band");
        put("Flusso energetico [10⁻¹² erg cm⁻² s⁻¹]", "Energy flux [10⁻¹² erg cm⁻² s⁻¹]");
        put("Le barre sono le stesse della vista 2D. X = banda, altezza = flusso energetico; la profondità è solo prospettica e non aggiunge una nuova variabile fisica.",
                "The bars contain the same values as the 2D view. X = band, height = energy flux; depth is only perspective and adds no physical variable.");
        put("Scala dell'altezza: unità di 10⁻¹² erg cm⁻² s⁻¹. I limiti al 90% restano consultabili nella tabella 2D.",
                "Height scale: units of 10⁻¹² erg cm⁻² s⁻¹. The 90% limits remain available in the 2D table.");
        put("Parametri del fit", "Fit parameters");
        put("n.d. / non vincolato", "n/a / unconstrained");
        put("Probabilità nulla", "Null probability");
        put("Flussi e incertezza", "Fluxes and uncertainty");
        put("Nessun flusso disponibile per il modello scelto.", "No flux available for the selected model.");
        put("Intervallo 90%", "90% interval");
        put("Unità: erg cm⁻² s⁻¹. I limiti sono quelli pubblicati da BAT.",
                "Units: erg cm⁻² s⁻¹. Limits are those published by BAT.");
        put("Descrizione automatica locale: aiuta a leggere i numeri, ma non sostituisce la valutazione spettroscopica dell'esperto.",
                "Local automatic description: it helps read the numbers but does not replace expert spectral assessment.");
        put("BAT riporta “N/A” come modello migliore: PL e CPL sono consultabili, ma il catalogo non ne raccomanda uno.",
                "BAT reports “N/A” as the best model: PL and CPL can be inspected, but the catalog recommends neither.");
        put("Il catalogo indica", "The catalog identifies");
        put("come modello preferito per questo intervallo.", "as the preferred model for this interval.");
        put("valori più negativi descrivono una decrescita più rapida verso le energie alte.",
                "more negative values describe a faster decrease toward high energies.");
        put("Il picco Epeak è vincolato a circa", "Epeak is constrained to about");
        put("Epeak non è ben vincolato dai limiti pubblicati; non va interpretato come una misura robusta.",
                "Epeak is not well constrained by the published limits and should not be interpreted as a robust measurement.");
        put("Il χ² ridotto è vicino a 1, quindi il modello è globalmente compatibile con i dati; va comunque letto con la probabilità nulla.",
                "Reduced χ² is close to 1, so the model is globally compatible with the data; it should still be read together with the null probability.");
        put("Il χ² ridotto si discosta da 1: il fit merita un controllo più attento insieme a residui e probabilità nulla.",
                "Reduced χ² differs from 1: the fit deserves closer inspection together with residuals and null probability.");
        put("Tra le quattro bande non sovrapposte, il flusso maggiore è in", "Among the four non-overlapping bands, the largest flux is in");
        put("Lo spettro integra da", "The spectrum integrates from");
        put("intervallo spettroscopico", "spectral interval");
        put("1 · Che cosa arriva dal catalogo BAT", "1 · What comes from the BAT catalog");
        put("I parametri PL/CPL, il modello migliore, χ², gradi di libertà, probabilità nulla, intervallo dello spettro e flussi per banda sono letti dalle tabelle ufficiali. L'app non li inventa e non li ricalcola dai quattro canali.",
                "PL/CPL parameters, best model, χ², degrees of freedom, null probability, spectral interval and band fluxes are read from official tables. The app neither invents them nor recalculates them from the four channels.");
        put("2 · PL e CPL", "2 · PL and CPL");
        put("PL descrive lo spettro con una legge di potenza. CPL aggiunge un taglio esponenziale e può fornire Epeak. Se BAT scrive N/A, significa che non pubblica una scelta preferita tra i modelli per quel caso.",
                "PL describes the spectrum with a power law. CPL adds an exponential cutoff and can provide Epeak. If BAT reports N/A, it does not publish a preferred model for that case.");
        put("3 · Flusso e frequenza", "3 · Flux and frequency");
        put("Il catalogo usa energia in keV, equivalente alla frequenza tramite E = hν. Le barre mostrano flussi energetici integrati in bande, non il rate della curva di luce.",
                "The catalog uses energy in keV, equivalent to frequency through E = hν. Bars show band-integrated energy fluxes, not the light-curve count rate.");
        put("4 · Perché non basta il file ASCII", "4 · Why the ASCII file is not enough");
        put("Per trasformare conteggi in flusso servono spettro PHA, matrice di risposta RSP/DRM, correzioni strumentali e un fit. I prodotti qui visualizzati sono già stati elaborati dalla pipeline BAT con XSPEC.",
                "Converting counts to flux requires a PHA spectrum, RSP/DRM response matrix, instrumental corrections and a fit. The products shown here have already been processed by the BAT pipeline with XSPEC.");
        put("5 · T100 e picco di 1 secondo", "5 · T100 and 1-second peak");
        put("T100 riassume l'intero intervallo scelto per lo spettro del burst; il picco di 1 secondo descrive invece la fase più intensa su quella scala temporale. Rispondono a domande diverse e non vanno mescolati.",
                "T100 summarizes the full interval selected for the burst spectrum; the 1-second peak describes the most intense phase on that time scale. They answer different questions and should not be mixed.");
        put("Cerca un campo metadata…", "Search a metadata field…");
        put("tempo–energia", "time–energy");
        put("Ripristina tutte", "Reset all");

        // 3D renderers.
        put("Nessun dato disponibile per questa distribuzione 3D.", "No data available for this 3D distribution.");
        put("Nessun dato disponibile per la vista 3D.", "No data available for the 3D view.");
        put("Nessun dato a quattro bande disponibile per la vista 3D.", "No four-band data available for the 3D view.");
        put("Nessun campione", "No sample");
        put("Nessuna curva normalizzata disponibile per la vista 3D.", "No normalized curve available for the 3D view.");
        put("Elementi del profilo 2D", "2D profile elements");
        put("La vista 3D riproduce gli stessi elementi del grafico 2D: singoli GRB in azzurro, mediana in magenta e limiti 25°/75° in viola. La profondità serve solo a separare visivamente le curve e non rappresenta T90, distanza o posizione nello spazio.",
                "The 3D view reproduces the same elements as the 2D chart: individual GRBs in cyan, median in magenta and 25th/75th limits in purple. Depth only separates curves visually and does not represent T90, distance or spatial position.");
        put("Trascina per ruotare · rotella per zoom · doppio clic per centrare. La profondità è puramente grafica.",
                "Drag to rotate · wheel to zoom · double-click to center. Depth is purely graphical.");
        put("Altezza = numero di GRB; profondità = seconda suddivisione indicata nella legenda.",
                "Height = number of GRBs; depth = the second subdivision shown in the legend.");
        put("Asse X = tempo dal trigger; asse Y = rate; profondità = quattro bande energetiche.",
                "X axis = time from trigger; Y axis = rate; depth = four energy bands.");
        put("Ogni linea è una curva di luce a bin di 1 secondo: la vista non rappresenta una distanza nello spazio né uno spettro continuo. Trascina per ruotare e usa la rotella per lo zoom.",
                "Each line is a 1-second-binned light curve: the view does not represent spatial distance or a continuous spectrum. Drag to rotate and use the wheel to zoom.");
        put("Confronto 3D dei rate", "3D rate comparison");
        put("Confronto tra bande", "Band comparison");
    }

    private UiTranslations() { }

    private static void put(String italian, String english) {
        EN.put(italian, english);
        IT.putIfAbsent(english, italian);
    }

    public static String t(String text) {
        if (text == null || text.isBlank()) return text;
        /*
         * LegacyI18nBridge merges this supplemental dictionary into I18n at
         * application startup. Delegating first to the unified dictionary is
         * important: I18n checks complete sentences before composing dynamic
         * fragments. The previous order composed short entries such as
         * "Apri"/"Dati" too early and produced mixed-language captions even
         * when an exact translation was already available.
         */
        String translated = I18n.t(text);
        if (!"[Missing English translation]".equals(translated)) return translated;

        // Defensive fallback for callers used before application bootstrap.
        Map<String, String> direct = I18n.language() == I18n.Language.EN ? EN : IT;
        translated = directWithWhitespace(text, direct);
        if (!translated.equals(text)) return translated;
        return text;
    }

    public static boolean hasEnglish(String italian) {
        if (italian == null || italian.isBlank()) return true;
        String trimmed = italian.trim();
        return EN.containsKey(trimmed) || I18n.hasEnglish(italian);
    }

    public static <T> void installChoiceBox(javafx.scene.control.ChoiceBox<T> choice) {
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
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> choice.requestLayout());
    }

    /** Binding-safe equivalent of the legacy I18n.localizeTree. */
    public static void localizeTree(Node node) {
        if (node == null) return;
        if (node instanceof Labeled labeled) localizeLabeled(labeled);
        if (node instanceof Chart chart && chart.getTitle() != null && !chart.titleProperty().isBound()) {
            String source = original(chart, ORIGINAL_TEXT + ".chartTitle", chart.getTitle());
            chart.setTitle(t(source));
        }
        if (node instanceof Axis<?> axis && axis.getLabel() != null && !axis.labelProperty().isBound()) {
            String source = original(axis, ORIGINAL_TEXT + ".axisLabel", axis.getLabel());
            axis.setLabel(t(source));
        }
        if (node instanceof XYChart<?, ?> xyChart) {
            for (Object raw : xyChart.getData()) {
                XYChart.Series<?, ?> series = (XYChart.Series<?, ?>) raw;
                if (series.getName() != null) series.setName(t(series.getName()));
            }
        }
        if (node instanceof MenuButton menuButton) {
            for (MenuItem item : menuButton.getItems()) {
                if (item.textProperty().isBound()) continue;
                Object source = item.getProperties().computeIfAbsent(ORIGINAL_TEXT, key -> item.getText());
                item.setText(t((String) source));
            }
        }
        if (node instanceof TextInputControl input && !input.promptTextProperty().isBound()) {
            Object source = input.getProperties().computeIfAbsent(PROMPT_TEXT, key -> input.getPromptText());
            if (source instanceof String value) input.setPromptText(t(value));
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (!tab.textProperty().isBound()) {
                    Object source = tab.getProperties().computeIfAbsent(ORIGINAL_TEXT, key -> tab.getText());
                    tab.setText(t((String) source));
                }
                if (tab.getContent() != null) localizeTree(tab.getContent());
            }
        }
        if (node instanceof TableView<?> table) {
            for (TableColumn<?, ?> column : table.getColumns()) localizeColumn(column);
        }
        if (node instanceof javafx.scene.control.ChoiceBox<?> choice) choice.requestLayout();
        if (node instanceof ComboBox<?> combo) combo.requestLayout();
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) localizeTree(child);
        }
    }

    public static void localizeLabeled(Labeled control) {
        if (control == null || control.textProperty().isBound()) return;
        Object bilingualIt = control.getProperties().get(I18N_LOCALIZED_IT);
        Object bilingualEn = control.getProperties().get(I18N_LOCALIZED_EN);
        if (bilingualIt instanceof String it && bilingualEn instanceof String en) {
            control.setText(I18n.language() == I18n.Language.EN ? en : it);
        } else {
            Object source = control.getProperties().get(UI_FACTORY_ORIGINAL);
            if (!(source instanceof String)) {
                source = control.getProperties().computeIfAbsent(ORIGINAL_TEXT, key -> control.getText());
            }
            if (source instanceof String value) control.setText(t(value));
        }
        if (control.getTooltip() != null && !control.getTooltip().textProperty().isBound()) {
            control.getTooltip().setText(t(control.getTooltip().getText()));
        }
    }

    private static void localizeColumn(TableColumn<?, ?> column) {
        if (!column.textProperty().isBound()) {
            Object source = column.getProperties().computeIfAbsent(ORIGINAL_TEXT, key -> column.getText());
            column.setText(t((String) source));
        }
        if (column.getGraphic() != null) localizeTree(column.getGraphic());
        for (TableColumn<?, ?> child : column.getColumns()) localizeColumn(child);
    }

    private static String original(Node node, String key, String value) {
        Object source = node.getProperties().computeIfAbsent(key, ignored -> value);
        return source instanceof String text ? text : value;
    }

    private static String directWithWhitespace(String text, Map<String, String> map) {
        String direct = map.get(text);
        if (direct != null) return direct;
        String trimmed = text.trim();
        direct = map.get(trimmed);
        if (direct == null) return text;
        int start = text.indexOf(trimmed);
        if (start < 0) return direct;
        return text.substring(0, start) + direct + text.substring(start + trimmed.length());
    }

}
