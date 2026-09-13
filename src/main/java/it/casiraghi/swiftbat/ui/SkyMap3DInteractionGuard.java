package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.CelestialSpherePane;
import it.casiraghi.swiftbat.ui.components.MollweideSkyPane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Locale;

/**
 * Stabilizza le interazioni della mappa celeste.
 *
 * La sfera 3D usa una JavaFX SubScene. Aprire il fullscreen da un click generico
 * sulla card mentre la SubScene sta elaborando il mouse puo' invalidare la camera
 * durante il dispatch dell'evento e causare Scene.getEffectiveCamera() NPE.
 *
 * Per la Sky map il fullscreen e' quindi intenzionalmente button-only: i click
 * sulla Mollweide e sulla sfera restano dedicati alla selezione/esplorazione dei
 * GRB, mentre il pulsante Fullscreen/Schermo intero continua a funzionare normalmente.
 */
public final class SkyMap3DInteractionGuard {
    private static final String WATCHED = SkyMap3DInteractionGuard.class.getName() + ".watched";
    private static final String CARD_DONE = SkyMap3DInteractionGuard.class.getName() + ".cardDone";
    private static final String GENERIC_VISUAL_DONE = InteractionPolishEnhancer.class.getName() + ".visualDone";

    private SkyMap3DInteractionGuard() { }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> scan(root));
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;

        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node added : List.copyOf(change.getAddedSubList())) {
                        watch(added);
                    }
                }
            }
            Platform.runLater(() -> scan(parent));
        });

        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
            watch(child);
        }
    }

    private static void scan(Node node) {
        if (node == null) return;
        enhance(node);
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                scan(child);
            }
        }
    }

    private static void enhance(Node node) {
        if (!(node instanceof Region card)) return;
        if (!card.getStyleClass().contains("card")) return;
        if (Boolean.TRUE.equals(card.getProperties().get(CARD_DONE))) return;

        Button fullscreen = findFullscreenButton(card);
        if (fullscreen == null) return;

        boolean skyMapCard = findDescendant(card, MollweideSkyPane.class) != null
                || findDescendant(card, CelestialSpherePane.class) != null;
        if (!skyMapCard) return;

        card.getProperties().put(CARD_DONE, Boolean.TRUE);

        /*
         * Questo flag viene letto da InteractionPolishEnhancer, che viene installato
         * subito dopo questa classe. In questo modo sulla Sky map NON viene aggiunto
         * alcun event-filter generico che possa chiamare fullscreen.fire() in seguito
         * a un click sulla superficie 2D/3D.
         */
        card.getProperties().put(GENERIC_VISUAL_DONE, Boolean.TRUE);

        if (!fullscreen.getStyleClass().contains("sky-fullscreen-button-only")) {
            fullscreen.getStyleClass().add("sky-fullscreen-button-only");
        }
    }

    private static Button findFullscreenButton(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.isVisible() && button.isManaged()) {
                String text = button.getText() == null ? "" : button.getText().trim().toLowerCase(Locale.ROOT);
                String compact = text.replace(" ", "");
                if (text.contains("schermo intero")
                        || text.contains("full screen")
                        || compact.contains("fullscreen")) {
                    return button;
                }
            }
            if (child instanceof Parent parent) {
                Button nested = findFullscreenButton(parent);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static <T extends Node> T findDescendant(Parent root, Class<T> type) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Parent parent) {
                T nested = findDescendant(parent, type);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
