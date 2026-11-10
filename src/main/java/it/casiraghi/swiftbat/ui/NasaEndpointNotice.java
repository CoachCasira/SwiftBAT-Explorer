package it.casiraghi.swiftbat.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.EnumSet;

/**
 * Non-modal, single-instance warning for unavailable NASA/Swift endpoints.
 *
 * The banner is integrated in the existing main window, not a separate
 * Alert/Stage. Repeated failures cannot generate stacked dialogs or repeated
 * notifications. Dismissal persists until the failing endpoints recover.
 */
public final class NasaEndpointNotice extends HBox {
    public enum Endpoint {
        CATALOG,
        SKY_METADATA,
        REDSHIFT,
        SPECTROSCOPY,
        LIGHT_CURVES
    }

    private final EnumSet<Endpoint> unavailable = EnumSet.noneOf(Endpoint.class);
    private boolean dismissed;

    public NasaEndpointNotice() {
        getStyleClass().add("nasa-endpoint-notice");
        setId("nasa-endpoint-notice");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(10, 18, 10, 18));
        setMaxWidth(Double.MAX_VALUE);
        setVisible(false);
        managedProperty().bind(visibleProperty());

        Label warningIcon = new Label("⚠");
        warningIcon.getStyleClass().add("nasa-endpoint-notice-icon");

        Label heading = UiFactory.label("", "nasa-endpoint-notice-heading");
        I18n.setText(heading,
                "Endpoint NASA non disponibili",
                "NASA endpoints unavailable");

        Label description = UiFactory.wrappedLabel("", "nasa-endpoint-notice-description");
        I18n.setText(description,
                "Alcuni collegamenti Swift/BAT non rispondono. "
                        + "Sono utilizzabili i dati salvati in locale; "
                        + "le funzioni che richiedono dati online potrebbero essere limitate.",
                "Some Swift/BAT data links are not responding. "
                        + "Locally saved data remain available; "
                        + "features requiring online data may be limited.");

        VBox texts = new VBox(2, heading, description);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Button dismiss = UiFactory.button("×", "nasa-endpoint-notice-dismiss");
        dismiss.setId("dismiss-nasa-endpoint-notice");
        dismiss.setFocusTraversable(false);
        dismiss.setOnAction(event -> dismiss());
        dismiss.setTooltip(UiFactory.quickTooltip("Chiudi avviso endpoint NASA"));

        getChildren().addAll(warningIcon, texts, dismiss);
        I18n.languageProperty().addListener((obs, previous, next) -> {
            I18n.localizeLabeled(heading);
            I18n.localizeLabeled(description);
        });
    }

    public void reportUnavailable(Endpoint endpoint) {
        if (endpoint == null) return;
        unavailable.add(endpoint);
        setVisible(!dismissed);
    }

    public void reportAvailable(Endpoint endpoint) {
        if (endpoint == null) return;
        unavailable.remove(endpoint);
        if (unavailable.isEmpty()) {
            // A future, distinct outage should be visible even after dismissal.
            dismissed = false;
            setVisible(false);
        }
    }

    public void dismiss() {
        if (unavailable.isEmpty()) return;
        dismissed = true;
        setVisible(false);
    }

    boolean hasUnavailableEndpoints() {
        return !unavailable.isEmpty();
    }
}
