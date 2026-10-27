# Esperimento: apertura cinematografica a buco nero

Questo documento riguarda **solo** il branch `experiment/black-hole-opening`.

Il branch stabile `feature/cross-platform-expert-bootstrap` non viene modificato
dall'esperimento. Non è stata creata una seconda finestra: un overlay temporaneo
viene mostrato sulla normale `Scene` JavaFX, nello stesso `Stage` dell'app.

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
- **Inizia l'esplorazione** (oppure Invio) avvia uno zoom di 980 ms nel buco nero
  e lascia apparire la Home dell'app.
- **Salta l'apertura** (oppure Esc) entra subito nella Home.
- **Dalla Home, cliccando il buco nero animato**, si torna alla schermata
  iniziale SENZA aprire un'altra finestra. Si può premere di nuovo
  **Inizia l'esplorazione** per rivedere lo zoom e la costruzione
  progressiva della Home, oppure **Salta l'apertura** per tornare subito.
  L'operazione è ripetibile e non cancella i GRB già caricati.
- Dopo l'ingresso, l'overlay e il suo AnimationTimer vengono rimossi, quindi
  il resto dell'applicazione è quello già collaudato.

I dati scientifici offline non vengono spostati né modificati da questo
esperimento. Il branch non contiene un nuovo archivio di GRB e continua a
usare la cache personale e la cartella `offline-data` presenti sul computer.

**Non occorrono più i file JPEG del tentativo fotorealistico**
(`SwiftBAT-Opening-Cinematic-Assets.zip`): il buco nero è di nuovo
disegnato e animato direttamente con Canvas come nella seconda versione
sperimentale. Eventuali vecchi JPEG locali non vengono utilizzati.

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
