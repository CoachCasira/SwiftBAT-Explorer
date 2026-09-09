package it.casiraghi.swiftbat.ui.components;

import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * Logo compatto dell'app ricavato dallo stesso artwork ufficiale usato come
 * identità visiva SwiftBAT Explorer.
 */
public final class OrbitLogoPane extends StackPane {
    private final ImageView imageView = BrandLogoAsset.view(42);

    public OrbitLogoPane() {
        getStyleClass().add("orbit-logo-pane");
        setMouseTransparent(true);
        getChildren().add(imageView);
        setMinSize(34, 34);
        setPrefSize(42, 42);
        setMaxSize(56, 56);
        widthProperty().addListener((obs, oldValue, newValue) -> resizeImage());
        heightProperty().addListener((obs, oldValue, newValue) -> resizeImage());
    }

    private void resizeImage() {
        double size = Math.max(1, Math.min(getWidth(), getHeight()));
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
    }
}
