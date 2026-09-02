package it.casiraghi.swiftbat;

import it.casiraghi.swiftbat.ui.MainView;
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

        stage.setTitle("SwiftBAT Explorer 1.2.0 — Event Horizon");
        var iconStream = SwiftBatExplorerApp.class.getResourceAsStream("/app-icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }
        stage.setMinWidth(1240);
        stage.setMinHeight(790);
        stage.setScene(scene);
        stage.show();

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
