package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preserves the exact scroll position of the application when an
 * InPlaceFullscreen session temporarily replaces the Scene root.
 *
 * <p>The original page tree is not rebuilt while fullscreen is open, therefore
 * the same ScrollPane instances can be restored after the original root is put
 * back. Two deferred restores are intentional: the first runs after the root
 * swap, the second after JavaFX focus/layout has settled.</p>
 */
public final class FullscreenScrollPositionFix {
    private static final String ROOT_WATCH = FullscreenScrollPositionFix.class.getName() + ".rootWatch";
    private static final String SCENE_WATCH = FullscreenScrollPositionFix.class.getName() + ".sceneWatch";
    private static final String STATE = FullscreenScrollPositionFix.class.getName() + ".state";

    private FullscreenScrollPositionFix() { }

    public static void install(Node root) {
        if (root == null) return;
        if (!Boolean.TRUE.equals(root.getProperties().get(ROOT_WATCH))) {
            root.getProperties().put(ROOT_WATCH, Boolean.TRUE);
            root.sceneProperty().addListener((obs, oldScene, newScene) -> installScene(newScene));
        }
        installScene(root.getScene());
    }

    private static void installScene(Scene scene) {
        if (scene == null || Boolean.TRUE.equals(scene.getProperties().get(SCENE_WATCH))) return;
        scene.getProperties().put(SCENE_WATCH, Boolean.TRUE);

        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            boolean oldFullscreen = isFullscreenRoot(oldRoot);
            boolean newFullscreen = isFullscreenRoot(newRoot);

            if (!oldFullscreen && newFullscreen && oldRoot != null) {
                scene.getProperties().put(STATE, capture(oldRoot));
                return;
            }

            if (oldFullscreen && !newFullscreen && newRoot != null) {
                Object raw = scene.getProperties().remove(STATE);
                if (raw instanceof Snapshot snapshot) restore(snapshot);
            }
        });
    }

    private static boolean isFullscreenRoot(Node node) {
        return node != null && node.getStyleClass().contains("in-place-fullscreen");
    }

    private static Snapshot capture(Node root) {
        List<ScrollState> states = new ArrayList<>();
        Map<Node, Boolean> visited = new IdentityHashMap<>();
        collect(root, states, visited);
        return new Snapshot(List.copyOf(states));
    }

    private static void collect(Node node, List<ScrollState> states, Map<Node, Boolean> visited) {
        if (node == null || visited.put(node, Boolean.TRUE) != null) return;

        if (node instanceof ScrollPane scroll) {
            states.add(new ScrollState(scroll, scroll.getHvalue(), scroll.getVvalue()));
            collect(scroll.getContent(), states, visited);
            return;
        }
        if (node instanceof SplitPane split) {
            for (Node item : split.getItems()) collect(item, states, visited);
            return;
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) collect(tab.getContent(), states, visited);
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collect(child, states, visited);
        }
    }

    private static void restore(Snapshot snapshot) {
        if (snapshot == null || snapshot.states().isEmpty()) return;
        Platform.runLater(() -> {
            apply(snapshot);
            Platform.runLater(() -> apply(snapshot));
        });
    }

    private static void apply(Snapshot snapshot) {
        for (ScrollState state : snapshot.states()) {
            ScrollPane scroll = state.scroll();
            if (scroll == null || scroll.getScene() == null) continue;
            scroll.setHvalue(state.hvalue());
            scroll.setVvalue(state.vvalue());
        }
    }

    private record Snapshot(List<ScrollState> states) { }
    private record ScrollState(ScrollPane scroll, double hvalue, double vvalue) { }
}
