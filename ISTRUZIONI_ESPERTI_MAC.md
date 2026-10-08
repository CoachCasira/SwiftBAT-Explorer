# Pacchetto macOS per gli esperti

Questa variante serve a distribuire SwiftBAT Explorer 1.3.0 a un esperto che usa macOS senza assumere che Java, JDK o Maven siano già installati.

## Obiettivo

L'esperto deve:

1. estrarre lo ZIP;
2. aprire la cartella `SwiftBAT Explorer`;
3. fare doppio clic su `AVVIA_SWIFTBAT.command`.

Il launcher non usa né modifica Java/Maven installati nel sistema. Crea invece un ambiente locale nella cache dell'utente:

```text
~/Library/Caches/SwiftBAT-Explorer/expert-runtime/
```

La prima esecuzione scarica automaticamente:

- Eclipse Temurin JDK 17, nella variante corretta per Apple Silicon o Intel;
- Apache Maven 3.9.16;
- le dipendenze Maven/JavaFX necessarie al progetto.

Le dipendenze Maven vengono conservate nella stessa cache dell'ambiente esperti. Agli avvii successivi vengono riutilizzate.

## Requisiti minimi

- macOS;
- connessione Internet al primo avvio;
- connessione Internet durante l'uso per accedere ai dati Swift/BAT NASA/GSFC;
- spazio libero sufficiente per JDK, Maven e dipendenze (consigliato almeno 1 GB).

Non sono richiesti:

- Java già installato;
- Maven già installato;
- Homebrew;
- Xcode;
- privilegi amministrativi;
- account GitHub.

## Apple Silicon e Intel

Il launcher rileva automaticamente `uname -m`:

- `arm64` -> Temurin JDK 17 aarch64;
- `x86_64` -> Temurin JDK 17 x64.

Questo rende lo stesso script utilizzabile su entrambe le famiglie di Mac.

## Creare lo ZIP da inviare

Sul Mac di sviluppo, dal branch `feature/mac-expert-bootstrap`, fare doppio clic su:

```text
CREA_PACCHETTO_ESPERTI_MAC.command
```

Vengono creati:

```text
dist/SwiftBAT-Explorer-Esperti-macOS.zip
dist/SwiftBAT-Explorer-Esperti-macOS.sha256.txt
```

Lo ZIP mantiene il bit eseguibile del launcher grazie a `ditto`.

Per generare **con un unico doppio clic sia il pacchetto Mac sia quello Windows** per i relatori, utilizzare `CREA_PACCHETTI_DISTRIBUZIONE_MAC.command`; i due ZIP verranno creati nella stessa cartella `dist/`.

## Test consigliato prima dell'invio

Estrarre lo ZIP in una cartella nuova e avviare `AVVIA_SWIFTBAT.command`.

Il launcher è deliberatamente indipendente dal Java installato sul Mac di sviluppo: anche se il computer possiede già Java 17, 21 o 26, viene usato il JDK 17 scaricato nella cache dedicata.

Per simulare nuovamente un primo avvio completamente pulito, eliminare:

```text
~/Library/Caches/SwiftBAT-Explorer/expert-runtime
```

e riaprire il launcher.

## Sicurezza e isolamento

Il bootstrap:

- non esegue `sudo`;
- non modifica `/Library`, `/usr/local` o altre directory di sistema;
- non modifica `JAVA_HOME` in modo permanente;
- non modifica il PATH globale;
- non contatta il repository GitHub;
- non richiede username/password GitHub;
- mantiene runtime e librerie nella cache dell'utente.

## Primo avvio e Gatekeeper

Il pacchetto non è notarizzato con un certificato Apple commerciale. Se macOS impedisce il primo avvio del file `.command`, usare **clic destro -> Apri** e confermare una sola volta.

## Diagnostica

Il launcher salva un log persistente in:

```text
~/Library/Logs/SwiftBAT Explorer/expert-launcher.log
```

In caso di errore è sufficiente inviare quel file per capire in quale fase si è fermato il bootstrap.
