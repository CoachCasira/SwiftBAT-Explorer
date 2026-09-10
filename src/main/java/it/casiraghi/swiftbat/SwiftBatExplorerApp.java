package it.casiraghi.swiftbat;

import it.casiraghi.swiftbat.ui.AdaptiveChromeEnhancer;
import it.casiraghi.swiftbat.ui.ChartInteractionEnhancer;
import it.casiraghi.swiftbat.ui.CurveInteractionLinkEnhancer;
import it.casiraghi.swiftbat.ui.ExplorerBandSelectionEnhancer;
import it.casiraghi.swiftbat.ui.ExplorerCurveInteractionEnhancer;
import it.casiraghi.swiftbat.ui.ExplorerSidebarLayoutFix;
import it.casiraghi.swiftbat.ui.ExplorerVisualStabilityFixes;
import it.casiraghi.swiftbat.ui.FinalRequestedUiFixes;
import it.casiraghi.swiftbat.ui.FinalUiStabilityEnhancer;
import it.casiraghi.swiftbat.ui.InteractionPolishEnhancer;
import it.casiraghi.swiftbat.ui.InteractiveViewSyncEnhancer;
import it.casiraghi.swiftbat.ui.LegacyI18nBridge;
import it.casiraghi.swiftbat.ui.MainView;
import it.casiraghi.swiftbat.ui.SkyMap3DInteractionGuard;
import it.casiraghi.swiftbat.ui.SpectroscopyStartupLayoutFix;
import it.casiraghi.swiftbat.ui.UiBugFixes;
import it.casiraghi.swiftbat.ui.UiCrossPlatformPolishEnhancer;
import it.casiraghi.swiftbat.ui.UiLastMileFixes;
import it.casiraghi.swiftbat.ui.UiLocalizationWatcher;
import it.casiraghi.swiftbat.ui.UiRefinements;
import it.casiraghi.swiftbat.ui.UiTableAndStartupFixes;
import it.casiraghi.swiftbat.ui.components.BrandLogoAsset;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SwiftBatExplorerApp extends Application {
    @Override
    public void start(Stage stage) {
        // Rendiamo il dizionario supplementare disponibile anche ai renderer
        // Java2D/Swing che usano ancora direttamente I18n.t(...).
        LegacyI18nBridge.install();

        MainView mainView = new MainView(getHostServices(), stage);
        Scene scene = new Scene(mainView.getRoot(), 1580, 960);
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/app.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/ui-refinements.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/black-hole-theme.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/reference-redesign.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/reference-redesign-final.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/dropdown-clean.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/responsive-layout.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/interactive-layout-final.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/stability-final.css").toExternalForm());

        stage.setTitle("SwiftBAT Explorer 1.3.0 — Reference UI Preview");
        stage.getIcons().setAll(BrandLogoAsset.image());
        stage.setMinWidth(1240);
        stage.setMinHeight(790);
        stage.setScene(scene);
        stage.show();

        UiLocalizationWatcher.install(mainView.getRoot());
        UiRefinements.install(mainView.getRoot());
        ExplorerBandSelectionEnhancer.install(mainView.getRoot());
        ExplorerVisualStabilityFixes.install(mainView.getRoot());

        InteractiveViewSyncEnhancer.install(mainView.getRoot());
        // Must be registered before InteractionPolishEnhancer: an actual 3D GRB
        // marker click is selection, not a request to open the whole sky card.
        SkyMap3DInteractionGuard.install(mainView.getRoot());
        InteractionPolishEnhancer.install(mainView.getRoot());

        /*
         * Specialized line interactions must be installed before the generic
         * chart enhancer. Population owns lock/spotlight; Explorer owns the
         * continuous segment hover, tooltip and synchronized 2D/fullscreen focus.
         */
        CurveInteractionLinkEnhancer.install(mainView.getRoot());
        ExplorerCurveInteractionEnhancer.install(mainView.getRoot());
        ChartInteractionEnhancer.install(mainView.getRoot());
        UiBugFixes.install(mainView.getRoot());

        AdaptiveChromeEnhancer.install(mainView.getRoot());

        /*
         * Table/first-layout ownership must be registered BEFORE the two legacy
         * stability passes. This order is intentional: dynamically-created tables
         * are marked and wrapped by UiTableAndStartupFixes first, so the older
         * UiLastMileFixes wrapper can never re-parent the same TableView.
         */
        UiTableAndStartupFixes.prepare(mainView.getRoot());
        UiTableAndStartupFixes.install(mainView.getRoot());
        SpectroscopyStartupLayoutFix.install(mainView.getRoot());

        UiLastMileFixes.prepare(mainView.getRoot());
        FinalUiStabilityEnhancer.install(mainView.getRoot());
        UiLastMileFixes.install(mainView.getRoot());

        // Deliberately last: normalize skin-dependent scrollbar geometry first,
        // then apply the final cross-platform icon, translation and laptop-layout
        // polish after every legacy pass has created its dynamic controls.
        FinalRequestedUiFixes.install(mainView.getRoot());
        UiCrossPlatformPolishEnhancer.install(mainView.getRoot());
        // The Explorer overview keeps its supporting cards on the right at every
        // window size; only their width changes on laptop-sized workspaces.
        ExplorerSidebarLayoutFix.install(mainView.getRoot());

        mainView.initialize();
    }

    @Override
    public void stop() {
        MainView.shutdownSharedExecutor();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
