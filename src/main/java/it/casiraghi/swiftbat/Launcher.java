package it.casiraghi.swiftbat;

/**
 * Entry point separato dalla classe JavaFX.
 *
 * <p>Il launcher nativo avvia questa classe dal classpath dell'applicazione;
 * in questo modo JavaFX viene caricato dal runtime incluso nel pacchetto.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        SwiftBatExplorerApp.main(args);
    }
}
