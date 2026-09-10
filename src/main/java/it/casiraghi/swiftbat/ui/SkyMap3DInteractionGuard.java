package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.CelestialSpherePane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Locale;

/**
 * Last-mile interaction owner for the sky-map card when the 3D sphere is present.
 *
 * The generic visualization enhancer used a capturing event filter on the whole
 * card. With a JavaFX SubScene this could schedule fullscreen before the embedded
 * 3D scene had a chance to consume a GRB-marker click. Re-parenting the view while
 * JavaFX was still recomputing mouse coordinates then produced Scene.getEffectiveCamera
 * NullPointerExceptions and could freeze the application.
 *
 * This class marks the sky card as already handled BEFORE InteractionPolishEnhancer
 * is installed and uses a normal bubbling handler instead. Marker clicks are consumed
 * inside CelestialSpherePane, therefore they select the GRB only. A click on unused
 * card / sky background reaches this handler and opens fullscreen.
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
                    for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
                }
            }
            Platform.runLater(() -> scan(parent));
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void scan(Node node) {
        if (node == null) return;
        enhance(node);
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) scan(child);
        }
    }

    private static void enhance(Node node) {
        if (!(node instanceof Region card)) return;
        if (!card.getStyleClass().contains("card")) return;

        CelestialSpherePane sphere = findDescendant(card, CelestialSpherePane.class);
        Button fullscreen = findFullscreenButton(card);
        if (sphere == null || fullscreen == null) return;
        if (Boolean.TRUE.equals(card.getProperties().get(CARD_DONE))) return;

        card.getProperties().put(CARD_DONE, Boolean.TRUE);

        // Prevent InteractionPolishEnhancer from installing its capturing filter here.
        card.getProperties().put(GENERIC_VISUAL_DONE, Boolean.TRUE);

        card.setPickOnBounds(true);

        if (!fullscreen.getStyleClass().contains("sky-fullscreen-stable")) {
            fullscreen.getStyleClass().add("sky-fullscreen-stable");
        }

        /*
         * Bubble phase is intentional.
         * CelestialSpherePane consumes real marker clicks in its SubScene handler,
         * so those clicks never reach us. Empty/background clicks do reach us and
         * may safely request fullscreen after the embedded scene finished handling them.
         */
        card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.isConsumed()
                    || event.getButton() != MouseButton.PRIMARY
                    || event.getClickCount() != 1
                    || !event.isStillSincePress()) {
                return;
            }

            if (isControlTarget(event.getTarget(), card)) return;
            if (fullscreen.isDisabled() || card.getScene() == null) return;

            // Defer only after the complete mouse dispatch has finished; this avoids
            // detaching/re-parenting a SubScene while JavaFX is still resolving its camera.
            Platform.runLater(() -> {
                if (card.getScene() != null && !fullscreen.isDisabled()) {
                    fullscreen.fire();
                }
            });
        });
    }

    private static boolean isControlTarget(Object rawTarget, Node boundary) {
        if (!(rawTarget instanceof Node target)) return false;
        Node current = target;
        while (current != null && current != boundary) {
            if (current instanceof javafx.scene.control.ButtonBase
                    || current instanceof javafx.scene.control.ChoiceBox<?>
                    || current instanceof javafx.scene.control.ComboBoxBase<?>
                    || current instanceof javafx.scene.control.TextInputControl
                    || current instanceof javafx.scene.control.ScrollBar
                    || current instanceof javafx.scene.control.Slider) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static Button findFullscreenButton(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button && button.isVisible() && button.isManaged()) {
                String text = button.getText() == null ? "" : button.getText().trim().toLowerCase(Locale.ROOT);
                String compact = text.replace(" ", "");
                if (text.contains("schermo intero") || text.contains("full screen") || compact.contains("fullscreen")) {
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
