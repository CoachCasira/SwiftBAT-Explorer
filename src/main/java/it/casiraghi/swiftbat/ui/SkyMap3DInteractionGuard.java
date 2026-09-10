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
 * Owns fullscreen interaction for the sky-map card when the 3D sphere is present.
 *
 * Important rule: the embedded SubScene is never detached while one of its mouse
 * events is being dispatched. Marker interaction is completed inside
 * CelestialSpherePane first; clicks inside the actual sphere surface stay in the
 * embedded view. Only clicks on the surrounding sky-map card may request fullscreen.
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

        // The sky card has dedicated SubScene-safe handling. Do not let the generic
        // visualization enhancer install its capture-phase fullscreen filter here.
        card.getProperties().put(GENERIC_VISUAL_DONE, Boolean.TRUE);
        card.setPickOnBounds(true);

        if (!fullscreen.getStyleClass().contains("sky-fullscreen-stable")) {
            fullscreen.getStyleClass().add("sky-fullscreen-stable");
        }

        card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY
                    || event.getClickCount() != 1
                    || !event.isStillSincePress()) {
                return;
            }

            /*
             * A real GRB marker is selected on MOUSE_RELEASED inside
             * CelestialSpherePane. The following MOUSE_CLICKED must never open
             * fullscreen. Consuming it here also prevents any other card-level
             * handler from interpreting the same gesture.
             */
            if (sphere.consumeMarkerInteractionGuard()) {
                event.consume();
                return;
            }

            /*
             * Any click inside the actual 3D map is an interaction with the map,
             * not a fullscreen command. This includes empty 3D space, rotation and
             * marker picking. Fullscreen is intentionally reserved for the card area
             * outside the graph, exactly like requested.
             */
            if (isInside(event.getTarget(), sphere)) {
                event.consume();
                return;
            }

            if (event.isConsumed() || isControlTarget(event.getTarget(), card)) return;
            if (fullscreen.isDisabled() || card.getScene() == null) return;

            // Run only after the complete input dispatch has ended. At this point no
            // SubScene mouse coordinates are being recomputed, so fullscreen cannot
            // invalidate Scene.getEffectiveCamera() mid-event.
            Platform.runLater(() -> {
                if (card.getScene() != null && !fullscreen.isDisabled()) {
                    fullscreen.fire();
                }
            });
        });
    }

    private static boolean isInside(Object rawTarget, Node ancestor) {
        if (!(rawTarget instanceof Node target) || ancestor == null) return false;
        Node current = target;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.getParent();
        }
        return false;
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
