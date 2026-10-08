# Pacchetto SwiftBAT Explorer 1.3.0 per i relatori Windows

Il relatore riceve il file **SwiftBAT-Explorer-Relatori-Windows.zip**, lo estrae completamente e fa doppio clic su **AVVIA_SWIFTBAT.bat** nella cartella **SwiftBAT Explorer**.

Il launcher usa Windows PowerShell 5.1 incluso in Windows 10/11. **Non** richiede l'installazione preventiva di Java, Maven, GitHub, Python, Visual Studio o privilegi amministrativi.

## Funzionamento

1. Controlla che il pacchetto includa il codice applicativo e il file Maven pom.xml.
2. Crea un ambiente isolato nella sottocartella `.swiftbat-runtime` della cartella `SwiftBAT Explorer` estratta dallo ZIP.
3. Se non esiste, scarica il JDK 17 Temurin ufficiale per **Windows x64 Intel/AMD** e controlla che sia Java 17.
4. Se non esiste, scarica Maven 3.9.16 dagli indirizzi Apache e verifica **SHA-512**.
5. Scarica le dipendenze Maven/JavaFX nella stessa cartella .swiftbat-runtime, compila e avvia SwiftBAT Explorer.

Gli avvii successivi riutilizzano gli strumenti e le librerie scaricati nella cartella estratta. Estrarre lo ZIP in una cartella su cui si abbiano permessi di scrittura. Una connessione Internet è indispensabile al primo avvio e per consultare i servizi online Swift/BAT.

**Compatibilità dichiarata:** Windows 10 e 11 a 64 bit su processori Intel/AMD (x64). Windows ARM e Windows a 32 bit **non sono stati abilitati o verificati**; non è corretto affermare che il pacchetto funzioni su qualsiasi PC.

**Spazio consigliato:** almeno 1 GB libero per JDK, Maven e librerie.

## Creare lo ZIP direttamente dal Mac

Dal branch GitHub `feature/cross-platform-expert-bootstrap`, eseguire su macOS:

    CREA_PACCHETTI_DISTRIBUZIONE_MAC.command

Questo produce **due** ZIP nella cartella `dist/`:

- `SwiftBAT-Explorer-Esperti-macOS.zip` (esperti Mac);
- `SwiftBAT-Explorer-Relatori-Windows.zip` (relatori Windows).

Per generare solo lo ZIP Windows da Mac, eseguire `CREA_PACCHETTO_RELATORI_WINDOWS_MAC.command`. Accanto a ogni ZIP viene creato un file `.sha256.txt` per verificare l'integrità dopo la consegna.

Non inviare i singoli launcher da soli: lo ZIP contiene il codice e i file necessari all'avvio.

## Sicurezza e accessi

L'ambiente Java/Maven, le dipendenze, i temporanei e i log sono mantenuti nella cartella `.swiftbat-runtime` accanto all'applicazione. Non viene eseguito `sudo`, non viene richiesto l'accesso a GitHub e non vengono modificati Java o variabili d'ambiente globali.

`AVVIA_SWIFTBAT.bat` lancia il file PowerShell incluso nello ZIP con `-ExecutionPolicy Bypass`, limitatamente al **singolo processo di avvio**: le policy permanenti del sistema non vengono cambiate. Alcune postazioni aziendali/universitarie possono applicare policy più restrittive o blocchi antivirus; in tali casi contattare l'amministratore e non aggirare i controlli di sicurezza.

Il pacchetto non è firmato digitalmente: Windows può mostrare una richiesta di conferma/SmartScreen. Confermare l'esecuzione soltanto se la provenienza del file è stata verificata.

## Diagnostica

In caso di errore, il launcher mostra il messaggio e lascia aperta la finestra fino alla pressione di un tasto. Il log si trova qui:

    SwiftBAT Explorer\.swiftbat-runtime\logs\expert-launcher.log

Per provare un primo avvio da zero, **chiudere prima l'applicazione**, poi eliminare soltanto `SwiftBAT Explorer\.swiftbat-runtime` dalla cartella estratta.

Per rimuovere runtime, librerie e log dopo la prova, eliminare la cartella estratta. Windows o l’applicazione potrebbero comunque creare proprie cache o impostazioni esterne.

## Limiti e test

La sintassi PowerShell e la preparazione di Java/Maven possono essere verificate automaticamente in GitHub Actions su Windows. Il rendering JavaFX e la disponibilità delle sorgenti scientifiche devono comunque essere collaudati su un computer Windows reale prima della distribuzione ai relatori.
