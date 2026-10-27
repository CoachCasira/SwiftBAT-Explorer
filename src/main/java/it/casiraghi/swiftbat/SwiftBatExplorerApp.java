package it.casiraghi.swiftbat;

import it.casiraghi.swiftbat.ui.AdaptiveChromeEnhancer;
import it.casiraghi.swiftbat.ui.ChartInteractionEnhancer;
import it.casiraghi.swiftbat.ui.CurveInteractionLinkEnhancer;
import it.casiraghi.swiftbat.ui.ExplorerRegressionFixes;
import it.casiraghi.swiftbat.ui.ExplorerUiRegressionFix;
import it.casiraghi.swiftbat.ui.FinalMacAndPopulationPolish;
import it.casiraghi.swiftbat.ui.FinalRequestedUiFastFixes;
import it.casiraghi.swiftbat.ui.FinalUiStabilityEnhancer;
import it.casiraghi.swiftbat.ui.GlobalSearchAssistEnhancer;
import it.casiraghi.swiftbat.ui.InteractionPolishEnhancer;
import it.casiraghi.swiftbat.ui.InteractiveViewSyncEnhancer;
import it.casiraghi.swiftbat.ui.LegacyI18nBridge;
import it.casiraghi.swiftbat.ui.MainView;
import it.casiraghi.swiftbat.ui.BlackHoleOpening;
import it.casiraghi.swiftbat.ui.HomeEntranceReveal;
import it.casiraghi.swiftbat.ui.PageScopedPolishRouter;
import it.casiraghi.swiftbat.ui.PopulationDurationMultiSelectEnhancer;
import it.casiraghi.swiftbat.ui.PopulationFracexpSliderFix;
import it.casiraghi.swiftbat.ui.SkyMap3DInteractionGuard;
import it.casiraghi.swiftbat.ui.SpectroscopyStartupLayoutFix;
import it.casiraghi.swiftbat.ui.TargetedLayoutPolish;
import it.casiraghi.swiftbat.ui.UiBugFixes;
import it.casiraghi.swiftbat.ui.UiCrossPlatformFastEnhancer;
import it.casiraghi.swiftbat.ui.UiLastMileFixes;
import it.casiraghi.swiftbat.ui.UiLocalizationWatcher;
import it.casiraghi.swiftbat.ui.UiRefinements;
import it.casiraghi.swiftbat.ui.UiTableAndStartupFixes;
import it.casiraghi.swiftbat.ui.components.BrandLogoAsset;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SwiftBatExplorerApp extends Application {
    @Override
    public void start(Stage stage) {
        LegacyI18nBridge.install();

        MainView mainView = new MainView(getHostServices(), stage);
        GlobalSearchAssistEnhancer.install(mainView);
        PopulationDurationMultiSelectEnhancer.install(mainView);
        PopulationFracexpSliderFix.install(mainView);
        // Experimental game-style opening. One Stage, one Scene: the
        // already-built app sits behind an opaque cinematic overlay, so the
        // user's first click never launches another window. The overlay and
        // its animation timer are disposed when Enter/Skip completes.
        StackPane applicationLayers = new StackPane(mainView.getRoot());
        HomeEntranceReveal arrival = new HomeEntranceReveal(mainView.getRoot(), mainView.getHomePage());
        BlackHoleOpening opening = new BlackHoleOpening(arrival::play, arrival::showImmediately);
        applicationLayers.getChildren().add(opening);
        Scene scene = new Scene(applicationLayers, 1580, 960);

        /*
         * CSS performance: four base/theme sheets plus one consolidated final
         * layer. The late reference/dropdown/responsive/interactive sheets are
         * bundled in stability-final.css in their original cascade order.
         */
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/app.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/ui-refinements.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/black-hole-theme.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/reference-redesign.css").toExternalForm());
        scene.getStylesheets().add(
                SwiftBatExplorerApp.class.getResource("/stability-final.css").toExternalForm());

        stage.setTitle("SwiftBAT Explorer 1.3.0");
        stage.getIcons().setAll(BrandLogoAsset.image());
        stage.setMinWidth(1240);
        stage.setMinHeight(790);
        stage.setScene(scene);
        stage.show();

        UiLocalizationWatcher.install(mainView.getRoot());
        UiRefinements.install(mainView.getRoot());

        InteractiveViewSyncEnhancer.install(mainView.getRoot());
        SkyMap3DInteractionGuard.install(mainView.getRoot());
        InteractionPolishEnhancer.install(mainView.getRoot());

        /* Explorer-specific band/curve/scroll visual watchers are deliberately
           NOT installed on the whole application anymore. PageScopedPolishRouter
           activates them only when Explorer exists, so Home/Population/etc. do
           not carry Explorer scene-graph listeners. */
        CurveInteractionLinkEnhancer.install(mainView.getRoot());
        ChartInteractionEnhancer.install(mainView.getRoot());
        UiBugFixes.install(mainView.getRoot());

        AdaptiveChromeEnhancer.install(mainView.getRoot());

        UiTableAndStartupFixes.prepare(mainView.getRoot());
        UiTableAndStartupFixes.install(mainView.getRoot());
        SpectroscopyStartupLayoutFix.install(mainView.getRoot());

        UiLastMileFixes.prepare(mainView.getRoot());
        FinalUiStabilityEnhancer.install(mainView.getRoot());
        UiLastMileFixes.install(mainView.getRoot());

        FinalRequestedUiFastFixes.install(mainView.getRoot());
        UiCrossPlatformFastEnhancer.install(mainView.getRoot());
        TargetedLayoutPolish.install(mainView.getRoot());

        /* Final targeted regressions: cached-filter label, removable table
           columns and metadata horizontal scrolling. Fullscreen owns its exit. */
        ExplorerRegressionFixes.install(mainView.getRoot());

        mainView.initialize();
        // Hide only the controls that should assemble AFTER the horizon dive.
        // MainView's real scientific services are already initialized.
        arrival.prepare();

        // Reopen this SAME cinematic overlay from the rotating black hole
        // inside Home. Each replay gets fresh one-shot timelines; Begin
        // reconstructs the Home reveal and Skip restores it immediately.
        // Never replace the existing Scene, Stage or scientific session.
        mainView.getHomePage().setOnOpeningRequested(() -> {
            if (applicationLayers.getChildren().stream()
                    .anyMatch(BlackHoleOpening.class::isInstance)) return;
            HomeEntranceReveal replayArrival = new HomeEntranceReveal(
                    mainView.getRoot(), mainView.getHomePage());
            BlackHoleOpening replayOpening = new BlackHoleOpening(
                    replayArrival::play, replayArrival::showImmediately);
            applicationLayers.getChildren().add(replayOpening);
            replayArrival.prepare();
            Platform.runLater(replayOpening::requestFocus);
        });

        FinalMacAndPopulationPolish.install(mainView.getRoot());
        PageScopedPolishRouter.install(mainView.getRoot());

        /* Adds only the scoped spectroscopy hover labels. Catalogue search,
           filtering and scrollbar behaviour are owned by the page itself. */
        ExplorerUiRegressionFix.install(mainView);

        // MainView and its services initialize normally in the background.
        // Focus stays on the opening until Enter or Skip is pressed.
        Platform.runLater(opening::requestFocus);
    }

    @Override
    public void stop() {
        MainView.shutdownSharedExecutor();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
