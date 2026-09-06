# Changelog

## In sviluppo — Analisi di popolazione e qualità

- pagina rinominata **Analisi di popolazione**, con spiegazioni esplicite di curve,
  mediana, quartili e distribuzioni;
- filtri riorganizzati in sezioni compatte, controlli FRACEXP separati e leggibili,
  anteprima del numero di candidati e motivazione quando il risultato è vuoto;
- correzione grafica delle barre degli istogrammi e tooltip con i conteggi;
- filtri di Esplora affiancati e abbreviati per ridurre l'ingombro verticale;
- cache locale persistente dei byte ufficiali ASCII/FITS, riutilizzata fra avvii;
- caricamento parallelo dei gruppi con limite globale alle richieste HTTP;
- integrazione della tabella ufficiale BAT dei redshift, mantenendo testo originale, metodo, incertezza e riferimento;
- filtri espliciti per GRB short/long, disponibilità del redshift e intervallo di `z`;
- filtri redshift aggiunti anche alla mappa celeste e scheda dettaglio del punto selezionato;
- nuova pagina **Analisi di popolazione** con filtri T90, redshift, RA/DEC e finestra temporale;
- sovrapposizione delle curve totali allineate al trigger e normalizzate sul proprio picco;
- profili di 25° percentile, mediana e 75° percentile, senza sommare direttamente rate di eventi diversi;
- due cursori distinti per l'intervallo 0–100% della copertura completa derivata da `FRACEXP`;
- istogrammi di copertura, T90 e redshift e flag automatico per la coda bassa (10° percentile);
- esportazione autonoma del prodotto ASCII in `.xlsx`;
- esportazione congiunta dei dati FITS e dei metadati in un unico `.xlsx`;
- test dedicati a redshift, qualità FRACEXP, profilo cumulativo ed esportazioni Excel.

## 1.2.0 — Event Horizon UI

- nuova Home pensata come vera schermata iniziale dell'app;
- illustrazione procedurale di un buco nero e disco di accrescimento, scalabile e senza asset esterni;
- nuova palette nero / antracite con accenti ambra e viola;
- navigazione principale ridotta a Home, Esplora, Mappa celeste, Confronta e Info;
- Dizionario spostato nella sezione Info per ridurre il rumore nella navigazione;
- top bar semplificata con stato catalogo, memoria di sessione e connessione;
- pagina Esplora alleggerita e tab rinominate in Curva 2D, Vista 3D, Dati, Metadati e Guida;
- pannello laterale della Curva 2D semplificato;
- filtri RA/DEC della mappa celeste resi avanzati e nascosti finché non servono;
- pagina Confronta riorganizzata con una barra di selezione più leggibile;
- pagina Info ridisegnata e collegata alla guida dei dati;
- packaging standalone per macOS Apple Silicon, macOS Intel e Windows x64;
- installer Windows `.exe` autosufficiente con JRE e applicazione incorporate;
- runtime Java 17 e dipendenze inclusi nei pacchetti finali;
- workflow GitHub Actions per generare automaticamente le tre distribuzioni;
- mantenuti integralmente i fix funzionali della 1.1.1.

## 1.1.1 — Correzioni di usabilità e viste 3D

- legenda colori della volta celeste;
- zoom, pan e reset nella Mollweide;
- sfera 3D ricentrata, ruotabile e zoomabile;
- scroll ripristinato nell'Explorer;
- metadati e pannelli laterali resi leggibili;
- vista tempo–energia ridisegnata e apribile a schermo intero;
- warning CSS sui pesi font rimossi.

## 1.1.0 — Mappa celeste

- acquisizione online di RA_ground, DEC_ground e T90;
- Mollweide 2D e sfera celeste 3D;
- piano galattico e filtri;
- apertura diretta del GRB selezionato.

## 1.0.3

- renderer prospettico Java2D per le quattro bande energetiche;
- cache di sessione e consultazione online dei prodotti Swift/BAT.
