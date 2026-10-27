# Esperimento: apertura cinematografica a buco nero

Questo documento riguarda **solo** il branch `experiment/black-hole-opening`.

Il branch stabile `feature/cross-platform-expert-bootstrap` non viene modificato
dall'esperimento. Non è stata creata una seconda finestra: un overlay temporaneo
viene mostrato sulla normale `Scene` JavaFX, nello stesso `Stage` dell'app.

## Pacchetto grafico fotorealistico (richiesto per vedere il nuovo buco nero)

Il codice sperimentale utilizza ora due risorse JPEG prerenderizzate:
`src/main/resources/opening-cinematic/black-hole-cinematic.jpg` e
`src/main/resources/opening-cinematic/event-horizon-tunnel.jpg`.

**Le immagini cinematografiche vengono consegnate a parte nel file
`SwiftBAT-Opening-Cinematic-Assets.zip` di questa conversazione**:
sono asset binari generati per l'esperimento, non contenuti nel repository
GitHub. Dopo aver aggiornato il branch:

- **Windows:** copia lo ZIP dentro `D:\\GitHub\\SwiftBAT-Explorer`
  (la radice del progetto); apri PowerShell lì ed esegui:
  ```powershell
  Expand-Archive -LiteralPath ".\\SwiftBAT-Opening-Cinematic-Assets.zip" -DestinationPath "." -Force
  ```
- **macOS:** copia lo ZIP nella radice del progetto ed esegui:
  ```bash
  unzip -o SwiftBAT-Opening-Cinematic-Assets.zip
  ```

Poi avvia l'applicazione **dai sorgenti di questo branch**, così Maven
include automaticamente le due immagini nelle risorse. Non aprire un
vecchio `dist` già estratto, che non si aggiorna da solo.

Il loader di `OpeningBlackHolePane` è difensivo: se le due immagini
non esistono, l'app si apre con il disegno procedurale precedente, senza
errori. Per vedere l'effetto fotografico, lo ZIP è indispensabile.

Le due immagini sono state compresse in JPEG ad alta qualità. Non è
richiesto alcun codec video o collegamento a Internet durante la
riproduzione; la memoria usata dalle immagini decodificate è limitata
a qualche decina di MB, mentre solo i 27 puntini sul Canvas cambiano
a ~25 fps. Durante l'ingresso il Canvas animato si ferma e il fondale
viene trasformato come texture.

## Provarlo in locale (Windows)

Aprire PowerShell nella cartella principale del progetto, poi eseguire:

```powershell
git fetch origin
git switch experiment/black-hole-opening
git pull origin experiment/black-hole-opening
.\AVVIA_APP.bat
```

## Provarlo in locale (macOS)

Aprire Terminale nella cartella principale del progetto:

```bash
git fetch origin
git switch experiment/black-hole-opening
git pull origin experiment/black-hole-opening
bash AVVIA_APP_MAC.command
```

Se si usa normalmente un altro launcher del progetto, è possibile continuare
a usare quel launcher; occorre però **avviare i sorgenti di questo branch** e
non un vecchio pacchetto `dist` già estratto.

## Comportamento

- Sullo sfondo appare un grande buco nero con luce gravitazionale, disco di
  accrescimento, stelle e particelle che cadono verso l'orizzonte.
- Il titolo appare con una breve dissolvenza; il Canvas anima le particelle
  a circa 25 fps. La geometria complessa è disegnata una sola volta.
- **Inizia l'esplorazione** (oppure Invio) avvia uno zoom di circa 1,2 secondi
  verso l'orizzonte fotografico, sfuma nel tunnel prerenderizzato e lascia
  apparire la Home dell'app con la sua costruzione progressiva.
- **Salta l'apertura** (oppure Esc) entra subito nella Home.
- Dopo l'ingresso, l'overlay e il suo AnimationTimer vengono rimossi, quindi
  il resto dell'applicazione è quello già collaudato.

I dati scientifici offline non vengono spostati né modificati da questo
esperimento. Il branch non contiene un nuovo archivio di GRB e continua a
usare la cache personale e la cartella `offline-data` presenti sul computer.

## Tornare immediatamente all'app originale

```bash
git switch feature/cross-platform-expert-bootstrap
```

Riavviando l'app si torna direttamente alla Home, senza apertura
cinematografica. Nessun merge è necessario.

## Verifiche

La workflow GitHub Actions `verify.yml` è abilitata anche per questo
branch di prova: compilazione Maven Java 17, test JavaFX su Xvfb, creazione
ZIP Windows/macOS e test dei launcher. I test JavaFX controllano in
particolare che Invio/Salta non cambino `Stage` o `Scene`.
