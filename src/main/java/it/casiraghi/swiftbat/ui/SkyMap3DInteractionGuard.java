package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.ui.components.CelestialSpherePane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Last-mile guard for the 3D sky map.
 *
 * InteractionPolishEnhancer installs fullscreen handling on the whole sky card.
 * A JavaFX SubScene reports itself as the outer mouse-event target even when a
 * 3D Sphere marker was picked, therefore the card-level filter cannot infer that
 * the user clicked a GRB. This guard is installed before InteractionPolishEnhancer
 * and resolves the actual 3D PickResult first: marker clicks select the GRB and
 * are consumed; clicks on empty 3D space keep propagating and may open fullscreen.
 */
public final class SkyMap3DInteractionGuard {
    private static final String WATCHED = SkyMap3DInteractionGuard.class.getName() + ".watched";
    private static final String CARD_DONE = SkyMap3DInteractionGuard.class.getName() + ".cardDone";

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

        // The sky fullscreen button must be visually stable: no pulsing/glitter effect.
        if (!fullscreen.getStyleClass().contains("sky-fullscreen-stable")) {
            fullscreen.getStyleClass().add("sky-fullscreen-stable");
        }

        card.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY
                    || event.getClickCount() != 1
                    || !event.isStillSincePress()) {
                return;
            }

            Node picked = event.getPickResult() == null
                    ? null
                    : event.getPickResult().getIntersectedNode();
            if (!(picked != null && picked.getUserData() instanceof SkyBurst burst)) {
                return;
            }

            CelestialSpherePane owningSphere = findOwningSphere(picked, sphere);
            owningSphere.select(burst);
            notifySelection(owningSphere, burst);

            // Critical: stop the later card fullscreen filter only for an actual GRB pick.
            event.consume();
        });
    }

    private static CelestialSpherePane findOwningSphere(Node picked, CelestialSpherePane fallback) {
        Node current = picked;
        while (current != null) {
            if (current instanceof CelestialSpherePane sphere) return sphere;
            current = current.getParent();
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static void notifySelection(CelestialSpherePane sphere, SkyBurst burst) {
        try {
            Field field = CelestialSpherePane.class.getDeclaredField("onSelect");
            field.setAccessible(true);
            Object value = field.get(sphere);
            if (value instanceof Consumer<?> consumer) {
                ((Consumer<SkyBurst>) consumer).accept(burst);
            }
        } catch (ReflectiveOperationException ignored) {
            // select(...) already keeps the marker highlighted. If the callback field
            // ever changes name, normal marker handling can be updated in one place.
        }
    }

    private static Button findFullscreenButton(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Button button) {
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
