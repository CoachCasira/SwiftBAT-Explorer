# Creazione delle applicazioni standalone

I pacchetti prodotti contengono gia Java 17, JavaFX e tutte le dipendenze.
Sul computer che usa l'applicazione non devono essere installati Java o Maven.

## Build automatica con GitHub Actions

1. Caricare il progetto in un repository GitHub.
2. Aprire la scheda **Actions** del repository.
3. Selezionare **Build applicazioni standalone**.
4. Premere **Run workflow**.
5. Al termine scaricare il pacchetto necessario dalla sezione **Artifacts**:
   - `SwiftBAT-Explorer-macOS-arm64` per Mac con processore Apple Silicon;
   - `SwiftBAT-Explorer-macOS-x64` per Mac con processore Intel;
   - `SwiftBAT-Explorer-Windows-Installer` per l'installer Windows richiesto;
   - `SwiftBAT-Explorer-Windows-Portable` come versione Windows senza installazione.

Il file scaricato da GitHub e un archivio che contiene a sua volta lo ZIP finale.

## Uso del pacchetto macOS

1. Estrarre `SwiftBAT-Explorer-macOS-arm64.zip` oppure
   `SwiftBAT-Explorer-macOS-x64.zip`.
2. Spostare `SwiftBAT Explorer.app` nella cartella Applicazioni, se desiderato.
3. Aprire l'app con doppio clic.

Il pacchetto e firmato localmente in modalita ad hoc, ma non e notarizzato da
Apple. Al primo avvio macOS potrebbe quindi richiedere di fare clic destro
sull'app, scegliere **Apri** e confermare una sola volta.

## Uso del pacchetto Windows

### Installer EXE autosufficiente

1. Estrarre l'artefatto `SwiftBAT-Explorer-Windows-Installer` scaricato da GitHub.
2. Fare doppio clic su `SwiftBAT-Explorer-Setup-1.2.0.exe`.
3. Completare l'installazione e avviare SwiftBAT Explorer dal collegamento creato.

L'installer ingloba l'applicazione, Java 17, JavaFX e tutte le dipendenze. Non
scarica Java o Maven e non richiede che siano gia installati sul computer.

Il file `SwiftBAT-Explorer-Setup-1.2.0.sha256.txt` contiene l'impronta SHA-256
con cui verificare che l'installer non sia stato modificato durante il
trasferimento.

### Versione portabile di riserva

1. Estrarre completamente `SwiftBAT-Explorer-Windows-x64-portable.zip`.
2. Aprire la cartella `SwiftBAT Explorer`.
3. Fare doppio clic su `SwiftBAT Explorer.exe`.

La cartella `app` e la cartella `runtime` devono restare accanto al file `.exe`.
Non serve eseguire un'installazione.

L'installer non e firmato con un certificato commerciale. Windows SmartScreen
potrebbe quindi mostrare un avviso al primo avvio: scegliere **Ulteriori
informazioni** e poi **Esegui comunque**.

## Build locale su macOS

Serve un JDK 17 completo. Fare doppio clic su `CREA_APP_MAC.command`: il
pacchetto viene creato nella cartella `dist`.

## Build locale su Windows

Serve un JDK 17 completo e WiX Toolset 3. Fare doppio clic su
`CREA_APP_WINDOWS.bat`: l'installer EXE e il pacchetto portabile vengono creati
nella cartella `dist`.
