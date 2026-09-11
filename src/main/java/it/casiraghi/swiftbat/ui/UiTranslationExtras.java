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
        put("Media mobile su 5 bin", "5-bin moving average");
        put("t = 0 indica il trigger", "t = 0 marks the trigger");

        // Population filters, focus lock and dynamic toggle captions.
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
        put("ripristina filtri", "reset filters");
        put("Analizza il gruppo", "Analyze group");
        put("Annulla", "Cancel");
        put("Blocca le curve selezionate", "Lock selected curves");
        put("Sblocca la selezione delle curve", "Unlock curve selection");
        put("Gruppo GRB manuale", "Manual GRB group");
        put("Scrivi il nome e seleziona uno o più GRB", "Type a name and select one or more GRBs");
        put("Cerca e salva uno o più GRB", "Search and save one or more GRBs");
        put("Il prefisso GRB viene mantenuto automaticamente. Invio seleziona il primo risultato.",
                "The GRB prefix is kept automatically. Enter selects the first result.");
        put("Il prefisso GRB è già inserito. Digita i numeri e scegli dal menu.",
                "The GRB prefix is already inserted. Type the digits and choose from the menu.");
        put("Analyze Group userà esattamente i GRB salvati; FRACEXP e finestra temporale restano applicati.",
                "Analyze Group will use exactly the saved GRBs; FRACEXP and the time window still apply.");
        put("Analyze Group userà il gruppo GRB salvato; FRACEXP e finestra temporale restano applicati.",
                "Analyze Group will use the saved GRB group; FRACEXP and the time window still apply.");

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
        // Runtime leftovers produced by the historical composed translator. They
        // are normalized by UiCrossPlatformPolishEnhancer, but remain registered
        // here too so the strict source-level translation audit stays exhaustive.
        put("Mappa time–energy dei rate", "Time–energy rate map");
        put("Open 3D view dei rate", "Open 3D rate view");
        put("Mappa time–energy", "Time–energy map");
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

        // Runtime loading states emitted outside the JavaFX scene graph.
        put("Dati pronti", "Data ready");
        put("Evento recuperato dalla cache della sessione.",
                "Event retrieved from the session cache.");
        put("Leggo ASCII e FITS già salvati sul dispositivo.",
                "Reading ASCII and FITS files already saved on this device.");
        put("Evento recuperato dalla cache locale.",
                "Event retrieved from the local cache.");
        put("Cache non leggibile", "Unreadable cache");
        put("Procedo con il download online.", "Continuing with the online download.");
        put("Ricerca dei prodotti", "Searching for products");
        put("Apro la pagina Data Product ufficiale.", "Opening the official Data Product page.");
        put("Download ASCII", "Downloading ASCII");
        put("Scarico 1s_lc_ascii.dat.", "Downloading 1s_lc_ascii.dat.");
        put("Download FITS", "Downloading FITS");
        put("Scarico il prodotto *_1chan_1s.lc.", "Downloading the *_1chan_1s.lc product.");
        put("Converto le curve in tabelle utilizzabili.", "Converting light curves into usable tables.");
        put("Curve, metadati e spiegazioni sono disponibili.",
                "Light curves, metadata and explanations are available.");
        put("Cartella risultati", "Results directory");
        put("Individuo la directory *-results/.", "Locating the *-results/ directory.");
        put("Cartella curve di luce", "Light-curve directory");
        put("Individuo la directory lc/.", "Locating the lc/ directory.");
        put("Prodotti a un secondo", "One-second products");
        put("Cerco DAT e FITS con binning di 1 s.", "Searching for 1-s-binned DAT and FITS files.");

        // Product and spectroscopy values returned by model classes.
        put("Solo ASCII disponibile", "ASCII only available");
        put("Solo FITS disponibile", "FITS only available");
        put("Prodotti 1 s non disponibili", "1-s products unavailable");
        put("Intero intervallo spettroscopico T100", "Full T100 spectral interval");
        put("Secondo attorno al picco del burst", "One second around the burst peak");
        put("Redshift minimo", "Minimum redshift");
        put("Redshift massimo", "Maximum redshift");
        put("Intervallo", "Interval");
        put("valore", "value");

        // File choosers and export feedback are not localized by tree walking.
        put("Esporta", "Export");
        put("Esporta vista 3D", "Export 3D view");
        put("Esporta ASCII in Excel", "Export ASCII to Excel");
        put("Immagine PNG", "PNG image");
        put("File Excel", "Excel file");
        put("Esportazione completata", "Export completed");
        put("File creato correttamente:", "File created successfully:");
        put("Il prodotto ASCII non è disponibile per questo GRB.",
                "The ASCII product is not available for this GRB.");
        put("Il prodotto FITS non è disponibile per questo GRB.",
                "The FITS product is not available for this GRB.");
        put("Percorso di esportazione non valido.", "Invalid export path.");

        // Summary values and the data dictionary are built by the service layer,
        // outside JavaFX's automatic tree localization.
        put("Evento", "Event");
        put("Nome identificativo del Gamma-Ray Burst.", "Identifier of the Gamma-Ray Burst.");
        put("Prodotti disponibili", "Available products");
        put("Indica quali prodotti a un secondo sono stati trovati online.",
                "Indicates which one-second products were found online.");
        put("Fonte", "Source");
        put("I dati provengono dai prodotti ufficiali Swift/BAT; la cache locale ne conserva una copia non modificata.",
                "Data come from official Swift/BAT products; the local cache keeps an unmodified copy.");
        put("Data e ora di inizio dell'osservazione.", "Observation start date and time.");
        put("Intervallo energetico coperto dal prodotto totale.",
                "Energy range covered by the total product.");
        put("Numero di intervalli temporali nel file a quattro canali.",
                "Number of time intervals in the four-channel file.");
        put("Numero di intervalli temporali nel prodotto FITS aggregato.",
                "Number of time intervals in the aggregated FITS product.");
        put("Picco del rate", "Peak rate");
        put("Valore massimo della curva totale a un secondo.",
                "Maximum value of the one-second total light curve.");
        put("Istante del massimo rispetto al momento zero dell'allerta.",
                "Time of the maximum relative to the alert zero point.");
        put("Errore al picco", "Peak error");
        put("Incertezza statistica associata al bin del picco.",
                "Statistical uncertainty associated with the peak bin.");
        put("Rapporto picco/errore", "Peak/error ratio");
        put("Indicatore descrittivo di quanto il picco supera la propria incertezza.",
                "Descriptive indicator of how far the peak exceeds its uncertainty.");
        put("Media aritmetica del rate nell'intera finestra scaricata.",
                "Arithmetic mean of the rate over the full downloaded window.");
        put("Variabilità globale", "Overall variability");
        put("Deviazione standard del rate; descrive quanto i valori oscillano nella finestra.",
                "Rate standard deviation; it describes how much values fluctuate within the window.");
        put("Bin con rate negativo", "Negative-rate bins");
        put("Quota di bin sotto zero dopo la sottrazione del fondo.",
                "Fraction of bins below zero after background subtraction.");
        put("Bin con esposizione completa", "Fully exposed bins");
        put("Quota di bin con FRACEXP praticamente uguale a 1.",
                "Fraction of bins with FRACEXP approximately equal to 1.");
        put("Valore FRACEXP più basso presente nel prodotto FITS.",
                "Lowest FRACEXP value in the FITS product.");
        put("Rapporto descrittivo fra contributo positivo ad alte energie (50–350 keV) e basse energie (15–50 keV). Non è una classificazione ufficiale.",
                "Descriptive ratio between positive high-energy (50–350 keV) and low-energy (15–50 keV) contributions. It is not an official classification.");
        put("Non disponibile", "Unavailable");
        put("Prodotto testuale con quattro bande energetiche e totale.",
                "Text product containing four energy bands and their total.");
        put("Prodotto scientifico binario con rate, errore, conteggi, esposizione e metadati.",
                "Binary scientific product containing rate, error, counts, exposure, and metadata.");
        put("Pagina ufficiale dei prodotti scientifici dell'evento.",
                "Official page for the event's scientific products.");
        put("Quanti secondi prima o dopo il trigger si trova il centro del bin.",
                "Number of seconds before or after the trigger at which the bin center lies.");
        put("Tempo relativo calcolato rispetto a TRIGTIME; il valore rappresenta il centro dell'intervallo di campionamento.",
                "Relative time calculated from TRIGTIME; the value represents the center of the sampling interval.");
        put("Permette di allineare eventi diversi usando lo stesso momento zero.",
                "Allows different events to be aligned to the same zero point.");
        put("Un valore negativo indica un istante precedente al trigger, non un tempo fisicamente impossibile.",
                "A negative value indicates a time before the trigger, not a physically impossible time.");
        put("Differenza fra TIME MET e TRIGTIME prima dell'aggiunta di metà TIMEDEL.",
                "Difference between TIME MET and TRIGTIME before adding half of TIMEDEL.");
        put("Quanti secondi prima o dopo il trigger inizia il bin.",
                "Number of seconds before or after the trigger at which the bin starts.");
        put("Serve quando è importante distinguere il bordo iniziale dal centro del bin.",
                "Useful when the start edge must be distinguished from the bin center.");
        put("Con bin di 1 s, centro e inizio differiscono di circa 0,5 s.",
                "With 1-s bins, center and start differ by approximately 0.5 s.");
        put("L'orologio assoluto interno della missione Swift.",
                "Swift mission's internal absolute clock.");
        put("Mission Elapsed Time: secondi trascorsi dall'epoca temporale definita nell'intestazione FITS.",
                "Mission Elapsed Time: seconds elapsed since the time epoch defined in the FITS header.");
        put("Consente di collegare la misura ad altri prodotti della stessa osservazione.",
                "Links the measurement to other products from the same observation.");
        put("Per leggere una curva è normalmente più intuitivo il tempo relativo al trigger.",
                "Time relative to the trigger is usually more intuitive when reading a light curve.");
        put("Quanto segnale netto viene misurato in quel secondo nella banda complessiva.",
                "Net signal measured in that second over the full energy band.");
        put("Rate netto corretto per il fondo nella banda energetica dichiarata dal prodotto.",
                "Background-corrected net rate in the energy band declared by the product.");
        put("È la grandezza principale disegnata nella curva di luce.",
                "This is the main quantity plotted in the light curve.");
        put("Può essere negativo per fluttuazioni statistiche dopo la sottrazione del fondo.",
                "It may be negative because of statistical fluctuations after background subtraction.");
        put("Quanto è incerto il valore RATE dello stesso bin.",
                "Uncertainty of the RATE value in the same bin.");
        put("Errore statistico associato al rate stimato.",
                "Statistical error associated with the estimated rate.");
        put("Aiuta a capire se un picco è chiaramente distinto dal rumore.",
                "Helps determine whether a peak is clearly distinct from noise.");
        put("Non rappresenta da solo ogni possibile errore sistematico dello strumento.",
                "It does not by itself represent every possible instrumental systematic error.");
        put("Numero totale di eventi registrati nel bin prima dell'interpretazione finale.",
                "Total number of events recorded in the bin before final interpretation.");
        put("Conteggio strumentale totale associato all'intervallo temporale.",
                "Total instrumental count associated with the time interval.");
        put("È utile per controlli tecnici e per comprendere la statistica disponibile.",
                "Useful for technical checks and for understanding the available statistics.");
        put("Non coincide necessariamente con il rate netto, perché quest'ultimo include correzioni e sottrazione del fondo.",
                "It does not necessarily match the net rate, which includes corrections and background subtraction.");
        put("Quanta parte del secondo è stata realmente utilizzabile.",
                "Fraction of the second that was actually usable.");
        put("Esposizione frazionaria del bin, normalmente compresa fra 0 e 1.",
                "Fractional bin exposure, normally between 0 and 1.");
        put("Permette di riconoscere bin parziali o meno affidabili.",
                "Identifies partial or less reliable bins.");
        put("1 significa esposizione completa; valori inferiori non significano automaticamente dato inutilizzabile.",
                "1 means full exposure; lower values do not automatically mean unusable data.");
        put("fotoni relativamente meno energetici fra le quattro bande",
                "the relatively lower-energy photons among the four bands");
        put("seconda banda a bassa energia", "the second low-energy band");
        put("banda intermedia-alta", "the intermediate-high energy band");
        put("banda più energetica del file ASCII", "the highest-energy band in the ASCII file");
        put("somma complessiva delle quattro bande", "the total of the four bands");
        put("Il momento zero scelto per l'allerta del GRB.",
                "The zero point selected for the GRB alert.");
        put("Tempo missione al quale l'algoritmo BAT ha dichiarato il trigger.",
                "Mission time at which the BAT algorithm declared the trigger.");
        put("È il riferimento usato per trasformare l'orologio assoluto in secondi prima/dopo l'evento.",
                "Reference used to convert absolute mission time into seconds before/after the event.");
        put("Il trigger non è necessariamente l'inizio fisico esatto dell'emissione.",
                "The trigger is not necessarily the exact physical start of the emission.");
        put("Passo di campionamento della curva di luce.", "Light-curve sampling interval.");
        put("La larghezza di ogni bin temporale.", "Width of each time bin.");
        put("Definisce il dettaglio temporale disponibile.", "Defines the available time resolution.");
        put("Con 1 s, fenomeni molto più brevi vengono mediati nello stesso bin.",
                "With 1 s sampling, much shorter phenomena are averaged into the same bin.");
        put("Nome dell'evento scritto nel file.", "Event name stored in the file.");
        put("Identificativo dell'oggetto osservato nel prodotto FITS.",
                "Identifier of the object observed in the FITS product.");
        put("Serve a verificare che il file appartenga al GRB selezionato.",
                "Verifies that the file belongs to the selected GRB.");
        put("La capitalizzazione può differire dal nome mostrato nel catalogo.",
                "Capitalization may differ from the name shown in the catalog.");
        put("Permette di rintracciare tutti i prodotti collegati alla stessa osservazione.",
                "Allows all products linked to the same observation to be traced.");
        put("Codice univoco usato nell'archivio della missione.",
                "Unique code used in the mission archive.");
        put("Non va confuso con il Trigger ID.", "It should not be confused with the Trigger ID.");
        put("Data e ora in cui comincia il prodotto osservativo.",
                "Date and time at which the observation product begins.");
        put("Timestamp UTC di inizio dell'osservazione.", "UTC timestamp at the start of the observation.");
        put("Colloca temporalmente il dataset.", "Places the dataset in time.");
        put("Non indica necessariamente l'istante esatto del picco del GRB.",
                "It does not necessarily indicate the exact time of the GRB peak.");
        put("Timestamp UTC di fine dell'intervallo coperto.", "UTC timestamp at the end of the covered interval.");
        put("Data e ora in cui termina il prodotto osservativo.",
                "Date and time at which the observation product ends.");
        put("La finestra può essere più lunga della fase interessante del burst.",
                "The window may be longer than the relevant burst phase.");
        put("Distingue la provenienza dei prodotti.", "Identifies the origin of the products.");
        put("Il satellite o la missione che ha effettuato l'osservazione.",
                "Satellite or mission that performed the observation.");
        put("Nome della piattaforma osservativa dichiarata nel FITS.",
                "Name of the observing platform declared in the FITS file.");
        put("Nel catalogo considerato il valore atteso è SWIFT.",
                "In this catalog the expected value is SWIFT.");
        put("Lo strumento che ha raccolto i dati.", "Instrument that collected the data.");
        put("Rivelatore responsabile del prodotto scientifico.",
                "Detector responsible for the scientific product.");
        put("È fondamentale per conoscere banda energetica e caratteristiche della misura.",
                "Essential for identifying the energy band and measurement characteristics.");
        put("Qui il valore atteso è BAT.", "Here the expected value is BAT.");
        put("Istante assoluto di inizio della tabella.", "Absolute start time of the table.");
        put("Tempo missione del primo limite temporale del prodotto.",
                "Mission time of the product's first time boundary.");
        put("Permette controlli temporali con altri file.", "Enables time checks against other files.");
        put("È meno intuitivo del tempo relativo al trigger.",
                "It is less intuitive than time relative to the trigger.");
        put("Istante assoluto di fine della tabella.", "Absolute end time of the table.");
        put("Tempo missione dell'ultimo limite temporale del prodotto.",
                "Mission time of the product's last time boundary.");
        put("Permette di verificare la durata coperta.", "Allows the covered duration to be checked.");
        put("Non è la durata fisica T90 del GRB.", "It is not the physical T90 duration of the GRB.");
        put("Tempo effettivo complessivo di esposizione.", "Total effective exposure time.");
        put("Somma del tempo utile dopo le correzioni applicate dal prodotto.",
                "Sum of usable time after the product corrections.");
        put("Aiuta a valutare quanta osservazione valida è disponibile.",
                "Helps assess how much valid observation time is available.");
        put("Può differire dalla semplice differenza TSTOP−TSTART.",
                "It may differ from the simple TSTOP−TSTART difference.");
        put("Coordinate celesti dell'evento.", "Celestial coordinates of the event.");
        put("Ascensione retta e declinazione dell'oggetto nel sistema indicato dal FITS.",
                "Right ascension and declination of the object in the coordinate system declared by the FITS file.");
        put("Localizzano il GRB nel cielo.", "Locate the GRB on the sky.");
        put("Non rappresentano una posizione sullo schermo o sulla Terra.",
                "They do not represent a position on the screen or on Earth.");
        put("Permette di capire quale blocco del file si sta leggendo.",
                "Identifies which block of the file is being read.");
        put("Nome della sezione del file FITS.", "Name of the FITS file section.");
        put("Un FITS può contenere più tabelle e intestazioni nello stesso file.",
                "A FITS file may contain multiple tables and headers.");
        put("Serve per distinguere variazioni robuste da oscillazioni compatibili con il rumore.",
                "Helps distinguish robust variations from fluctuations compatible with noise.");
        put("Va letto insieme al RATE dello stesso intervallo e della stessa banda.",
                "It should be read together with RATE for the same interval and energy band.");
        put("Modifica i filtri oppure amplia il numero massimo di GRB da esaminare.",
                "Change the filters or increase the maximum number of GRBs to examine.");
        put("Non ci sono curve sufficienti per descrivere il profilo temporale.",
                "There are not enough curves to describe the temporal profile.");
        put("Variabilità — La fascia 25°–75° ha ampiezza media %.2f: contiene il 50%% centrale delle curve e indica una dispersione %s.",
                "Variability — The 25th–75th percentile band has mean width %.2f: it contains the central 50%% of curves and indicates %s.");
        put("Flusso", "Flux");

        // Service errors can surface in the Explorer and sky-map status areas.
        put("Non sono disponibili prodotti a un secondo leggibili per ",
                "No readable one-second products are available for ");
        put("I prodotti sono stati raggiunti, ma nessuno è stato interpretato correttamente per ",
                "The products were reached, but none could be parsed correctly for ");
        put("Download spettroscopia non riuscito: ", "Spectroscopy download failed: ");
        put("Download non riuscito", "Download failed");
        put("Download interrotto.", "Download interrupted.");
        put("Il file 1s_lc_ascii.dat non contiene righe numeriche leggibili.",
                "The 1s_lc_ascii.dat file contains no readable numeric rows.");
        put("Nel file FITS non è stata trovata l'estensione binaria RATE.",
                "The binary RATE extension was not found in the FITS file.");
        put("Il file FITS è stato scaricato ma non è stato interpretato correttamente.",
                "The FITS file was downloaded but could not be parsed correctly.");
        put("leggendo la tabella generale Swift/BAT.", "while reading the general Swift/BAT table.");
        put("La tabella generale Swift/BAT è stata scaricata ma non contiene coordinate leggibili.",
                "The general Swift/BAT table was downloaded but contains no readable coordinates.");
        put("Tabella GRB non trovata nella pagina ufficiale Swift/BAT.",
                "GRB table not found on the official Swift/BAT page.");
        put("La tabella Swift/BAT è stata trovata ma non contiene record leggibili.",
                "The Swift/BAT table was found but contains no readable records.");
        put("leggendo la tabella redshift Swift/BAT.", "while reading the Swift/BAT redshift table.");
        put("La tabella redshift BAT non contiene record leggibili.",
                "The BAT redshift table contains no readable records.");
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
