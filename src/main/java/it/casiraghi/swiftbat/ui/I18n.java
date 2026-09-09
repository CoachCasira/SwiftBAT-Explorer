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
        // UX / localizzazione 1.3.0 - stringhe dinamiche e renderer
        put("RATE massimo", "maximum RATE");
        put("rispetto al trigger", "relative to trigger");
        put("proxy alte / basse energie", "high / low energy proxy");
        put("in memoria nella sessione", "in session memory");
        put("Short (T90 ≤ 2 s)", "Short (T90 ≤ 2 s)");
        put("Long (T90 > 2 s)", "Long (T90 > 2 s)");
        put("z non disponibile", "z unavailable");
        put("Binning temporale", "Time binning");
        put("Banda complessiva", "Overall energy band");
        put("Intervallo temporale", "Time range");
        put("Righe ASCII", "ASCII rows");
        put("Righe FITS", "FITS rows");
        put("Campo da spiegare", "Field to explain");
        put("Campo metadata", "Metadata field");
        put("Nascondi colonna", "Hide column");
        put("Manuale…", "Manual…");
        put("Qualità FRACEXP", "FRACEXP quality");
        put("Campione massimo", "Maximum sample");
        put("Intervallo del fit", "Fit interval");
        put("Modello del fit", "Fit model");
        put("Fonte ufficiale BAT", "Official BAT source");
        put("Evento A", "Event A");
        put("Evento B", "Event B");
        put("Confronto temporale", "Time comparison");
        put("Normalizza ogni curva sul proprio picco", "Normalize each curve to its own peak");
        put("Caricamento confronto…", "Loading comparison…");
        put("Errore caricamento confronto", "Comparison load failed");
        put("Scegli due GRB diversi", "Choose two different GRBs");
        put("Apri o seleziona almeno due GRB", "Open or select at least two GRBs");
        put("Caricamento dei GRB selezionati…", "Loading selected GRBs…");
        put("Mappa non disponibile: servono i quattro canali ASCII.", "Map unavailable: the four ASCII channels are required.");
        put("Banda", "Band");
        put("Centro bin", "Bin center");
        put("Larghezza banda", "Band width");
        put("Fluttuazione negativa", "Negative fluctuation");
        put("Rate circa zero", "Rate near zero");
        put("Rate positivo", "Positive rate");
        put("Trascina: ruota prospettiva   ·   Rotella: zoom   ·   Doppio clic: centra", "Drag: rotate perspective   ·   Wheel: zoom   ·   Double-click: center");
        put("Trascina: prospettiva · Rotella: zoom · Doppio clic: centra", "Drag: perspective · Wheel: zoom · Double-click: center");
        put("Modello spettrale non disponibile.", "Spectral model unavailable.");
        put("Curve di luce 3D per banda energetica", "3D light curves by energy band");
        put("Profondità = banda energetica ASCII; non distanza spaziale.", "Depth = ASCII energy band; not spatial distance.");
        put("Asse X = tempo dal trigger; asse Y = rate; profondità = quattro bande energetiche. Ogni linea è una curva di luce a bin di 1 secondo: la vista non rappresenta una distanza nello spazio né uno spettro continuo. Trascina per ruotare e usa la rotella per lo zoom.",
                "X axis = time from trigger; Y axis = rate; depth = the four energy bands. Each line is a 1-second-binned light curve: the view does not represent spatial distance or a continuous spectrum. Drag to rotate and use the wheel to zoom.");
        put("Curva — la linea arancione rappresenta la funzione spettrale ricostruita dal fit ufficiale BAT selezionato. Non è una successione di punti grezzi misurati dal rivelatore.", "Curve — the orange line is the spectral function reconstructed from the selected official BAT fit. It is not a sequence of raw detector measurements.");
        put("Asse X — mostra l'energia dei fotoni in keV, da 15 a 150 keV nella vista corrente.", "X axis — photon energy in keV, from 15 to 150 keV in the current view.");
        put("Asse Y — mostra log₁₀ N(E), cioè il logaritmo del flusso fotonico differenziale. Valori negativi sono perfettamente normali e indicano N(E) < 1 nelle unità riportate.", "Y axis — log₁₀ N(E), the logarithm of the differential photon flux. Negative values are normal and mean N(E) < 1 in the displayed units.");
        put("Forma — la pendenza della curva descrive come il contributo previsto dal modello cambia con l'energia. Un andamento più ripido indica una diminuzione più rapida verso le energie elevate.", "Shape — the curve slope shows how the model contribution changes with energy. A steeper trend means a faster decrease toward higher energies.");
        put("Confronto — questa è la stessa funzione visualizzata nella vista 3D: il 3D aggiunge soltanto prospettiva grafica e non introduce una nuova grandezza fisica.", "Comparison — this is the same function shown in the 3D view: 3D only adds graphical perspective and does not introduce a new physical quantity.");
        put("Da ricordare — il grafico visualizza il modello ricostruito dai parametri del fit BAT; non deriva dalla somma delle quattro curve di luce ASCII.", "Remember — the chart shows the model reconstructed from BAT fit parameters; it is not derived by summing the four ASCII light curves.");
        put("Modello — la curva arancione rappresenta la stessa funzione spettrale ricostruita mostrata nella vista 2D. Non sono aggiunti nuovi punti osservativi.", "Model — the orange curve is the same reconstructed spectral function shown in 2D. No new observed points are added.");
        put("Assi — X indica l'energia dei fotoni in keV; Y indica log₁₀ N(E), il logaritmo del flusso fotonico differenziale previsto dal fit.", "Axes — X is photon energy in keV; Y is log₁₀ N(E), the logarithm of the differential photon flux predicted by the fit.");
        put("Forma della curva — la pendenza mostra come il contributo del modello diminuisce o varia passando verso energie più elevate.", "Curve shape — the slope shows how the model contribution decreases or changes toward higher energies.");
        put("Profondità — il piano arretrato e i collegamenti servono soltanto alla prospettiva. Non rappresentano tempo, distanza, intensità o una terza variabile fisica.", "Depth — the rear plane and connectors are only for perspective. They do not represent time, distance, intensity, or a third physical variable.");
        put("Interazione — trascina per cambiare la prospettiva interna, usa la rotellina per lo zoom e fai doppio clic per ricentrare la vista.", "Interaction — drag to change perspective, use the wheel to zoom, and double-click to recenter the view.");
        put("Interpretazione — zoom e prospettiva cambiano soltanto la visualizzazione: energia, N(E) e parametri del fit rimangono invariati.", "Interpretation — zoom and perspective only change the visualization: energy, N(E), and fit parameters remain unchanged.");
        put("Barre — ogni barra rappresenta il flusso energetico integrato pubblicato da BAT per una specifica banda energetica.", "Bars — each bar is the integrated energy flux published by BAT for a specific energy band.");
        put("Asse X — separa le bande di energia riportate dal catalogo, così da confrontare rapidamente dove il modello concentra più flusso.", "X axis — separates the catalog energy bands so you can quickly compare where the model carries more flux.");
        put("Asse Y — misura il flusso energetico in erg cm⁻² s⁻¹. Una barra più alta indica un flusso integrato maggiore nella banda corrispondente.", "Y axis — energy flux in erg cm⁻² s⁻¹. A taller bar means a larger integrated flux in that band.");
        put("Intervalli al 90% — i limiti di confidenza ufficiali restano disponibili nella tabella e nel tooltip delle barre; l'altezza mostra il valore centrale pubblicato.", "90% intervals — official confidence limits remain available in the table and bar tooltip; bar height shows the published central value.");
        put("Confronto — questo istogramma e la curva spettrale descrivono due aspetti dello stesso fit ufficiale, ma non sono quattro curve di luce sommate.", "Comparison — this histogram and the spectral curve describe two aspects of the same official fit; they are not four summed light curves.");
        put("Da ricordare — le barre derivano dai prodotti spettroscopici BAT/XSPEC e non dai rate ASCII a bin di un secondo.", "Remember — the bars come from BAT/XSPEC spectral products, not from 1-second ASCII rates.");
        put("Barre — sono gli stessi valori dell'istogramma 2D, disposti in prospettiva per facilitare il confronto visivo tra le bande energetiche.", "Bars — these are the same values as the 2D histogram, arranged in perspective to make energy-band comparison easier.");
        put("Assi — X identifica la banda energetica; l'altezza della barra rappresenta il flusso energetico integrato pubblicato da BAT.", "Axes — X identifies the energy band; bar height is the integrated energy flux published by BAT.");
        put("Scala — i valori sono mostrati in unità di 10⁻¹² erg cm⁻² s⁻¹ per mantenere una scala numerica leggibile senza alterare i rapporti tra le bande.", "Scale — values are shown in units of 10⁻¹² erg cm⁻² s⁻¹ to keep the numeric scale readable without changing ratios between bands.");
        put("Profondità — serve esclusivamente a separare graficamente le barre. Non è una distanza e non aggiunge una nuova grandezza fisica.", "Depth — only separates the bars graphically. It is not a distance and does not add a new physical quantity.");
        put("Interazione — trascina per cambiare prospettiva, usa la rotellina per lo zoom e il pulsante Centra vista per tornare all'inquadratura iniziale.", "Interaction — drag to change perspective, use the wheel to zoom, and Center view to return to the initial framing.");
        put("Da ricordare — i limiti al 90% restano consultabili nella tabella 2D; la vista 3D mostra i valori centrali del fit ufficiale.", "Remember — 90% limits remain available in the 2D table; the 3D view shows the official fit central values.");
        put("Assi — X rappresenta il tempo rispetto al trigger t = 0; Y separa le quattro bande energetiche BAT.", "Axes — X is time relative to trigger t = 0; Y separates the four BAT energy bands.");
        put("Colore — arancio indica un rate netto positivo, blu una fluttuazione negativa dopo la sottrazione del fondo; i toni scuri indicano valori vicini a zero.", "Color — orange indicates positive net rate, blue a negative fluctuation after background subtraction; dark tones are values near zero.");
        put("Dettaglio — spostando il mouse sulla mappa puoi leggere banda energetica, centro del bin, rate e larghezza della banda nel punto osservato.", "Detail — move the pointer over the map to read energy band, bin center, rate, and band width at that point.");
        put("Scala temporale — ogni cella deriva dai rate ASCII a bin di 1 secondo e la finestra visualizzata è la stessa scelta nella scheda Spettroscopia.", "Time scale — each cell comes from 1-second ASCII rates and the displayed window is the one selected in the Spectroscopy tab.");
        put("Da ricordare — questa mappa descrive i rate BAT nel tempo: non è un fit XSPEC e non converte direttamente i conteggi in flusso fisico.", "Remember — this map shows BAT rates over time: it is not an XSPEC fit and does not directly convert counts into physical flux.");
        // STRICT_I18N_EXPLORER_BATCH
        put("Catalogo in caricamento…", "Loading catalog…");
        put("Apri Data Product", "Open Data Product");
        put("Fonte ufficiale ↗", "Official source ↗");
        put("indicatore descrittivo", "descriptive indicator");
        put("bin con FRACEXP ≈ 1", "bins with FRACEXP ≈ 1");
        put("z cosmologico · valore BAT", "cosmological z · BAT value");
        put("Totale FITS 15–350 keV", "FITS total 15–350 keV");
        put("±20 s dal trigger", "±20 s from trigger");
        put("±60 s dal trigger", "±60 s from trigger");
        put("±120 s dal trigger", "±120 s from trigger");
        put("t = 0 indica il trigger", "t = 0 marks the trigger");
        put("Vista 3D non disponibile", "3D view unavailable");
        put("Per costruire il paesaggio tempo–energia servono le quattro bande del file ASCII.", "The four ASCII energy bands are required to build the time–energy landscape.");
        put("Keyword", "Keyword");
        put("Tutte le bande", "All bands");
        put("Non è ancora disponibile una spiegazione specifica per questa voce.", "A specific explanation for this field is not available yet.");
        put("Perché serve", "Why it matters");
        put("Attenzione", "Caution");
        put("Voce FITS", "FITS field");
        put("Commento FITS", "FITS comment");
        put("Esporta il grafico", "Export chart");
        put("Esporta FITS e metadati in Excel", "Export FITS and metadata to Excel");
        put("Il trigger è il momento zero", "The trigger is time zero");
        put("Il rate descrive l'intensità nel tempo", "Rate describes intensity over time");
        put("Le energie sono separate in quattro bande", "Energy is separated into four bands");
        put("ERROR e FRACEXP controllano l'affidabilità", "ERROR and FRACEXP describe data reliability");
        put("Il FITS è più di una tabella", "A FITS file is more than a table");
        put("Gli indicatori presenti nell'app sono descrittivi.", "The indicators in the app are descriptive.");
        put("Questa pagina collega le parole tecniche ai dati che stai osservando. Non devi memorizzare tutto: usa le spiegazioni come legenda ragionata.", "This page connects technical terms to the data you are viewing. You do not need to memorize everything: use the explanations as a reasoned reference.");
        put("Lo strumento riconosce un aumento significativo e genera un'allerta. Nei grafici trasformiamo quel momento in t = 0. I dati prima del trigger hanno tempo negativo; quelli dopo hanno tempo positivo.", "The instrument detects a significant increase and generates an alert. In the charts that instant is set to t = 0. Data before the trigger have negative time; data after it have positive time.");
        put("Ogni riga corrisponde a un intervallo di un secondo. RATE indica il segnale netto stimato in quell'intervallo. Più è alto, più la curva è intensa in quel momento.", "Each row corresponds to a one-second interval. RATE is the estimated net signal in that interval. Higher values indicate a stronger light curve at that time.");
        put("Il file ASCII divide il segnale in 15–25, 25–50, 50–100 e 100–350 keV. La curva totale 15–350 keV riunisce queste componenti.", "The ASCII file separates the signal into 15–25, 25–50, 50–100 and 100–350 keV bands. The 15–350 keV total curve combines these components.");
        put("ERROR esprime l'incertezza statistica del rate. FRACEXP indica quanta parte del secondo è stata realmente esposta: 1 significa bin completo.", "ERROR is the statistical uncertainty of the rate. FRACEXP indicates how much of the one-second bin was actually exposed: 1 means full exposure.");
        put("Contiene sia i numeri della curva sia le intestazioni tecniche: strumento, date, identificativi, coordinate, sistema temporale e dettagli di elaborazione.", "It contains both light-curve values and technical headers: instrument, dates, identifiers, coordinates, time system and processing details.");
        put("La vista 3D, il picco, la media, la deviazione standard e la durezza proxy aiutano a esplorare l'evento, ma non sostituiscono il calcolo ufficiale di T90 né una classificazione short/long validata.", "The 3D view, peak, mean, standard deviation and hardness proxy help explore the event, but they do not replace the official T90 calculation or a validated short/long classification.");
        put("Seleziona una riga. I metadati sono il registro tecnico del file FITS: descrivono provenienza, tempi, coordinate, struttura e passaggi di elaborazione.", "Select a row. Metadata are the technical record of the FITS file: they describe provenance, timing, coordinates, structure and processing steps.");
        put("Un FITS può contenere più sezioni. PRIMARY è l'intestazione generale; RATE è la tabella della curva di luce.", "A FITS file can contain multiple sections. PRIMARY is the general header; RATE is the light-curve table.");
        put("È il nome breve del parametro, per esempio OBS_ID, TRIGTIME o TIMEDEL.", "This is the short parameter name, for example OBS_ID, TRIGTIME or TIMEDEL.");
        put("È il contenuto associato alla keyword nella riga selezionata: può essere un numero, una data, un identificativo, una stringa o un valore logico.", "This is the value associated with the keyword in the selected row; it may be a number, date, identifier, string or logical value.");
        put("È la descrizione scritta dal software che ha prodotto il FITS.", "This is the description written by the software that produced the FITS file.");
        put("Questa keyword non è ancora inclusa nel dizionario didattico. Il commento originale del FITS rimane comunque visibile.", "This keyword is not yet included in the data dictionary. The original FITS comment remains visible.");
        put("Tempi negativi: prima del trigger. Tempi positivi: dopo il trigger. Il trigger non coincide necessariamente con l'inizio fisico esatto del burst.", "Negative times are before the trigger; positive times are after it. The trigger does not necessarily coincide with the exact physical onset of the burst.");
        put("ESC per uscire", "ESC to exit");
        put("Mappa non disponibile: ", "Map unavailable: ");
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
        // UX / i18n hardening 1.3.0 - runtime-generated text and fullscreen copy
        put("Senza z", "No z");
        put("non disponibile", "unavailable");
        put("dal trigger", "from trigger");
        put("s dal trigger", "s from trigger");
        put("rispetto al trigger", "relative to trigger");
        put("Confronta la forma temporale di un gruppo di GRB e descrivi durata, distanza e qualità del campione.",
                "Compare the temporal shape of a GRB sample and inspect duration, distance and data quality.");
        put("linee normalizzate e allineate al trigger", "normalized lines aligned to the trigger");
        put("comportamento centrale del gruppo a ogni secondo", "central behavior of the group at each second");
        put("tra i due limiti cade il 50% centrale delle curve", "the central 50% of curves lies between the two limits");
        put("Rate normalizzato (picco = 1)", "Normalized rate (peak = 1)");
        put("curve normalizzate e allineate a t = 0", "curves normalized and aligned at t = 0");
        put("GRB corrispondono ai filtri preliminari", "GRBs match the preliminary filters");
        put("saranno esaminati", "will be examined");
        put("già in RAM", "already in RAM");
        put("GRB inclusi su", "GRBs included out of");
        put("esaminati", "examined");
        put("copertura rilevata", "observed coverage");
        put("fuori dal filtro", "outside filter");
        put("non leggibili", "unreadable");
        put("Attendo catalogo e metadati scientifici…", "Waiting for catalog and scientific metadata…");
        put("Caricamento parallelo", "Parallel loading");
        put("Preparazione confronto…", "Preparing comparison…");
        put("Modello spettrale", "Spectral model");
        put("Flusso energetico 3D", "3D energy flux");
        put("Rate nel tempo per banda", "Rate over time by band");
        put("vista 3D", "3D view");
        put("quattro bande ASCII a 1 s", "four 1-s ASCII bands");
        put("Confronto tra bande — leggendo verticalmente lo stesso istante puoi confrontare come il rate si distribuisce tra 15–25, 25–50, 50–100 e 100–350 keV. Le differenze di colore evidenziano variazioni relative del segnale tra i canali.",
                "Band comparison — read vertically at the same instant to compare how rate is distributed across 15–25, 25–50, 50–100 and 100–350 keV. Color differences highlight relative signal changes between channels.");
        put("Interpretazione — una zona arancione intensa individua un intervallo temporale in cui il rate netto è elevato in quella banda. Il confronto resta descrittivo: per ottenere un flusso fisico servono risposta strumentale e fit spettroscopico.",
                "Interpretation — an intense orange region marks a time interval with high net rate in that band. The comparison is descriptive: instrumental response and spectral fitting are required to obtain physical flux.");
        put("Esegui un'analisi per ottenere un commento automatico sul campione.",
                "Run an analysis to obtain an automatic summary of the sample.");
        put("Il testo deriva solo dalle statistiche del grafico e non sostituisce l'interpretazione scientifica.",
                "The text is derived only from chart statistics and does not replace scientific interpretation.");
        put("ANALISI LOCALE · RIPRODUCIBILE", "LOCAL · REPRODUCIBLE ANALYSIS");
        put("curve incluse: il profilo mediano raggiunge il massimo a t =",
                "curves included: the median profile reaches its maximum at t =");
        put("Posizione temporale — Il massimo della mediana cade", "Timing — The median maximum occurs");
        put("in prossimità del trigger", "near the trigger");
        put("prima del trigger", "before the trigger");
        put("dopo il trigger", "after the trigger");
        put("resta sopra metà massimo per circa", "stays above half maximum for about");
        put("Il massimo cade sul bordo della finestra: prova una finestra più ampia.",
                "The maximum lies at the edge of the window: try a wider window.");
        put("Forma prima/dopo il trigger — Il segnale positivo non è sufficiente per stimare lo sbilanciamento.",
                "Pre/post-trigger shape — Positive signal is insufficient to estimate the asymmetry.");
        put("Forma prima/dopo il trigger — La mediana ha più area positiva dopo t = 0: nel campione selezionato prevale una coda post-trigger.",
                "Pre/post-trigger shape — The median has more positive area after t = 0: the selected sample is dominated by a post-trigger tail.");
        put("Forma prima/dopo il trigger — La mediana ha più area positiva prima di t = 0: il profilo selezionato è sbilanciato verso il pre-trigger.",
                "Pre/post-trigger shape — The median has more positive area before t = 0: the selected profile is skewed toward the pre-trigger interval.");
        put("Forma prima/dopo il trigger — Le aree positive prima e dopo t = 0 sono relativamente bilanciate.",
                "Pre/post-trigger shape — Positive areas before and after t = 0 are relatively balanced.");
        put("Variabilità — I percentili disponibili non bastano per stimare la dispersione tra le curve.",
                "Variability — Available percentiles are insufficient to estimate dispersion between curves.");
        put("Variabilità — La fascia 25°–75° ha ampiezza media",
                "Variability — The 25th–75th percentile band has mean width");
        put("contiene il 50% centrale delle curve e indica una dispersione",
                "contains the central 50% of curves and indicates");
        put("contenuta", "low dispersion");
        put("moderata", "moderate dispersion");
        put("elevata", "high dispersion");
        put("Campione —", "Sample —");
        put("nessun redshift disponibile", "no redshift available");
        put("redshift per", "redshift for");
        put("eventi (mediana z =", "events (median z =");
        put("copertura FRACEXP mediana", "median FRACEXP coverage");
        put("copertura FRACEXP non stimabile", "FRACEXP coverage cannot be estimated");
        put("senza T90", "without T90");
        put("Campione piccolo: mediana e percentili possono cambiare molto aggiungendo pochi eventi.",
                "Small sample: median and percentiles may change substantially when only a few events are added.");
        put("eventi esaminati non sono stati letti e non contribuiscono al profilo.",
                "examined events could not be read and do not contribute to the profile.");
        put("Il redshift è disponibile solo per il", "Redshift is available for only");
        put("del campione: la distribuzione z non è completa.", "of the sample: the z distribution is incomplete.");
        put("Le curve sono divise per il proprio picco: il confronto riguarda la forma relativa, non la luminosità assoluta.",
                "Curves are divided by their own peak: the comparison concerns relative shape, not absolute luminosity.");
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
        String composed = translateComposedEnglish(text);
        if (!composed.equals(text)) return composed;
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

    private static String translateComposedEnglish(String text) {
        String result = text;
        java.util.List<Map.Entry<String, String>> entries = new java.util.ArrayList<>(EN.entrySet());
        entries.sort((left, right) -> Integer.compare(right.getKey().length(), left.getKey().length()));
        for (Map.Entry<String, String> entry : entries) {
            String italian = entry.getKey();
            String english = entry.getValue();
            if (italian == null || english == null || italian.equals(english) || italian.length() < 3) continue;
            if (result.contains(italian)) result = result.replace(italian, english);
        }
        return result;
    }

    public static String english(String italian) {
        if (italian == null) return null;
        String translated = translateDirect(italian, EN, IT);
        if (!translated.equals(italian)) return translated;
        if (IT.containsKey(italian)) return italian;
        String composed = translateComposedEnglish(italian);
        if (!composed.equals(italian)) return composed;
        return requiresTranslation(italian) ? "[Missing English translation]" : italian;
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
