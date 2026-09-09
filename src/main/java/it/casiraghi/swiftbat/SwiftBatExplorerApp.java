package it.casiraghi.swiftbat;

import it.casiraghi.swiftbat.ui.MainView;
import it.casiraghi.swiftbat.ui.UiBugFixes;
import it.casiraghi.swiftbat.ui.UiRefinements;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class SwiftBatExplorerApp extends Application {
    @Override
    public void start(Stage stage) {
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

        stage.setTitle("SwiftBAT Explorer 1.3.0 — Reference UI Preview");
        var iconStream = SwiftBatExplorerApp.class.getResourceAsStream("/app-icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }
        stage.setMinWidth(1240);
        stage.setMinHeight(790);
        stage.setScene(scene);
        stage.show();

        UiRefinements.install(mainView.getRoot());
        UiBugFixes.install(mainView.getRoot());
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
