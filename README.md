# SwiftBAT Explorer 1.2.0 — Event Horizon

Applicazione desktop JavaFX per esplorare **online** le curve di luce dei Gamma-Ray Burst pubblicate nel catalogo Swift/BAT.

Il progetto è configurato per:

- **Java 17**;
- **Eclipse** con importazione Maven;
- Windows 64 bit e macOS per sviluppo/esecuzione Maven;
- creazione di applicazioni standalone con runtime incluso;
- dati letti direttamente dal catalogo NASA/GSFC;
- nessun caricamento manuale di Excel, DAT o FITS.

## Applicazioni standalone

Il workflow GitHub Actions incluso genera automaticamente le applicazioni per
macOS Apple Silicon, macOS Intel e Windows x64. Per Windows produce sia un
installer `.exe` autosufficiente sia una versione portabile. I pacchetti
includono Java 17, JavaFX e tutte le dipendenze: chi usa l'app non deve
installare alcun ambiente di sviluppo o scaricare componenti al primo avvio.

Le istruzioni complete si trovano in `BUILD_ESEGUIBILI.md`.

## Interfaccia 1.2.0

La versione **Event Horizon** mantiene tutte le funzioni scientifiche della 1.1.1 ma presenta una UI più semplice:

- Home con grafica procedurale ispirata a un buco nero;
- sei aree principali: **Home, Esplora, Mappa celeste, Analisi cumulativa, Confronta, Info**;
- tema scuro nero/antracite con accenti ambra e viola;
- filtri avanzati RA/DEC nascosti finché non vengono richiesti;
- tab dell'evento rinominate in **Curva 2D, Vista 3D, Dati, Metadati, Guida**;
- Dizionario raggiungibile dalla pagina Info.

Questa revisione aggiunge inoltre metadati e filtri T90/redshift, analisi di gruppi di GRB,
controlli di qualità basati su `FRACEXP` ed esportazioni Excel.

Tutti i fix della 1.1.1 restano inclusi: zoom/pan Mollweide, sfera 3D, scroll dell'Explorer, metadati leggibili e vista tempo–energia fullscreen.

## Novità della versione 1.0.3

- La vecchia scena 3D a barre è stata sostituita da un nuovo renderer **Java2D** incorporato nell'app tramite `SwingNode`.
- Le quattro bande sono ora mostrate come curve scientifiche a cascata, con linee anti-aliased, riempimenti trasparenti e profondità prospettica.
- Eliminati definitivamente piani opachi, illuminazione disomogenea, clipping e zone metà chiare/metà scure.
- Vista iniziale centrata sulla finestra **±60 secondi dal trigger**.
- Interazioni disponibili: trascinamento per cambiare prospettiva, rotella per lo zoom, doppio clic o pulsante per centrare.
- Passando il mouse vicino a un punto vengono mostrati banda, tempo e rate.
- È possibile cambiare la finestra fra ±20 s, ±60 s, ±120 s e intera osservazione.
- La cache RAM continua a mantenere i GRB già aperti senza nuovi download.
- Inclusa la guida Word completa in `docs/Guida_completa_dati_SwiftBAT_e_presentazione.docx`.


## Avvio rapido su macOS

1. Estrarre completamente lo ZIP.
2. Verificare di avere **Java 17** (`java -version`).
3. Fare doppio clic su `AVVIA_APP_MAC.command`.
4. Se macOS blocca il primo avvio dello script, aprire Terminale nella cartella ed eseguire `chmod +x AVVIA_APP_MAC.command mvnw`, poi `./AVVIA_APP_MAC.command`.
5. Al primo avvio Maven scarica le dipendenze: serve una connessione Internet.

In alternativa, importare il progetto Maven in Eclipse e lanciare il goal `javafx:run`.

## Avvio rapido su Windows

1. Estrarre completamente lo ZIP.
2. Aprire la cartella estratta.
3. Fare doppio clic su `AVVIA_APP.bat`.
4. Al primo avvio attendere il download di Maven e delle dipendenze.

Sono necessarie:

- connessione Internet;
- Java 17 disponibile nel `PATH`;
- accesso al sito Swift/BAT.

## Importazione in Eclipse

1. `File` → `Import...`
2. `Maven` → `Existing Maven Projects`
3. Selezionare la cartella che contiene `pom.xml`
4. Confermare l'importazione
5. Tasto destro sul progetto → `Maven` → `Update Project...`
6. Per avviare: `Run As` → `Maven build...`
7. Goal: `javafx:run`

## Funzioni principali

### Home

La Home permette di entrare direttamente nel catalogo, nella mappa celeste o nel confronto e presenta lo stato online dell’app in modo essenziale.

### Catalogo online

L'app legge la tabella pubblica Swift/BAT e permette di cercare per:

- nome del GRB;
- Trigger ID.

Il catalogo è arricchito con coordinate, T90 e redshift dalle tabelle riepilogative
ufficiali BAT. I valori di redshift non esatti (limiti, intervalli, alternative o
valori dubbi) conservano sempre la notazione originale.

Dopo la selezione segue automaticamente il percorso:

`Data Product → *-results → lc`

Cerca quindi:

- `1s_lc_ascii.dat`;
- `*_1chan_1s.lc`.

### Panoramica dell'evento

Per ogni GRB vengono mostrati:

- picco del rate;
- tempo del picco;
- rapporto picco/errore;
- percentuale di bin con esposizione completa;
- indicatore descrittivo di durezza energetica;
- righe disponibili nei prodotti ASCII e FITS;
- date, Observation ID, missione e strumento.

### Grafico 2D

Il grafico permette di scegliere:

- totale 15–350 keV;
- 15–25 keV;
- 25–50 keV;
- 50–100 keV;
- 100–350 keV;
- tutte le bande contemporaneamente.

Sono disponibili finestre temporali attorno al trigger e una media mobile a 5 bin.
Il grafico può essere esportato in PNG.

### Analisi cumulativa

La pagina **Analisi cumulativa** permette di selezionare gruppi mediante:

- classe di durata short/long e disponibilità del T90;
- presenza e intervallo del redshift `z`;
- area di cielo tramite RA e DEC;
- intervallo 0–100% della copertura completa derivata da `FRACEXP`;
- finestra temporale e numero massimo di eventi da esaminare.

Le curve totali vengono allineate al trigger e ogni curva è divisa per il proprio
picco. L'app mostra le singole curve e i profili di 25° percentile, mediana e 75°
percentile. Non somma direttamente i rate di GRB differenti.

Sono inclusi istogrammi di copertura, T90 e redshift. Il flag “coda bassa” indica
gli eventi sotto il 10° percentile della copertura fra quelli effettivamente letti.

### Mappa celeste

La pagina **Mappa celeste** visualizza la distribuzione dei GRB usando le coordinate BAT J2000 pubblicate online. Offre una proiezione Mollweide 2D e una sfera celeste 3D, filtri T90/redshift/RA/DEC, piano galattico e collegamento diretto al GRB selezionato.

### Vista prospettica tempo–energia

La scheda “Vista 3D” usa un renderer Java2D dedicato e mostra:

- asse orizzontale: tempo dal trigger;
- altezza: rate;
- profondità grafica: separazione tra le quattro bande energetiche.

Le curve sono disegnate come un paesaggio a cascata, molto più leggibile delle precedenti barre solide. È possibile cambiare prospettiva trascinando il mouse, usare la rotella per lo zoom, fare doppio clic per centrare e leggere i valori passando vicino ai punti. La finestra iniziale usa ±60 s dal trigger.
La profondità è soltanto un espediente visivo: non è una coordinata spaziale del GRB.

### Tabelle spiegate

Le tabelle ASCII e FITS sono visualizzate direttamente nell'app.
Ogni campo può essere selezionato nel pannello `Spiega`, che mostra:

- spiegazione semplice;
- descrizione tecnica;
- utilità del campo;
- errori di interpretazione da evitare.

Dalla stessa scheda è possibile creare due file distinti:

- un `.xlsx` contenente soltanto la tabella ASCII a quattro canali;
- un `.xlsx` contenente insieme tabella FITS e metadati FITS.

### Metadati FITS

Il pannello mostra tutte le keyword delle HDU FITS ed evidenzia il significato delle più importanti, fra cui:

- `TRIGTIME`;
- `TIMEDEL`;
- `OBJECT`;
- `OBS_ID`;
- `DATE-OBS`;
- `DATE-END`;
- `TELESCOP`;
- `INSTRUME`;
- `TSTART` e `TSTOP`;
- `EXPOSURE`;
- coordinate celesti;
- `EXTNAME`.

### Confronto fra GRB

Gli eventi già aperti nella sessione possono essere confrontati tramite:

- indicatori principali;
- sovrapposizione delle curve nella finestra ±60 s;
- normalizzazione opzionale sul picco.

## Dipendenze

Le dipendenze sono gestite da Maven:

- OpenJFX 17, incluso il modulo `javafx-swing` per il renderer Java2D;
- jsoup;
- nom-tam-fits;
- Apache POI per le esportazioni `.xlsx`;
- JUnit per i test.

## Privacy e memoria

L'applicazione:

- non richiede account;
- non invia dati personali;
- legge soltanto pagine e file pubblici del catalogo Swift/BAT;
- mantiene i GRB aperti nella RAM della sessione e li riapre istantaneamente;
- non modifica i prodotti scientifici online.

## Nota scientifica

L'app produce indicatori **descrittivi** per l'esplorazione.
Non calcola automaticamente T90 o redshift: li legge dalle tabelle ufficiali BAT.
La soglia short/long a 2 secondi è usata come raggruppamento descrittivo tradizionale, non come classificazione automatica definitiva.
La durezza mostrata è un proxy interno, non una misura ufficiale di catalogo.

## Struttura del progetto

```text
src/main/java/
  it/casiraghi/swiftbat/
    Launcher.java
    SwiftBatExplorerApp.java
    model/
    service/
    ui/
      components/
src/main/resources/
  app.css
  app-icon.png
packaging/
  app-icon.ico
  build-windows.ps1
.github/workflows/
  build-standalone.yml
```

## Diagnostica

Per verificare soltanto la compilazione:

```text
VERIFICA_BUILD.bat
```

Per pulire il progetto e avviarlo nuovamente:

```text
PULISCI_E_AVVIA.bat
```

In caso di errore, copiare l'intero messaggio del terminale, includendo le righe che iniziano con `[ERROR]`.

## Guida completa ai dati

Nel progetto è incluso il documento:

`docs/Guida_completa_dati_SwiftBAT_e_presentazione.docx`

Contiene la spiegazione dettagliata di trigger, bin, rate, errori, bande energetiche, colonne ASCII e FITS, metadati, indicatori dell'app, esempio GRB250605A e un discorso pronto per i professori.
