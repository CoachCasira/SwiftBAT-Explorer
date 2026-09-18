package it.casiraghi.swiftbat.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preserves application scroll positions while InPlaceFullscreen temporarily
 * replaces the Scene root.
 *
 * <p>The important detail is that fullscreen content can be re-parented before
 * Scene.setRoot(...) happens. That re-parenting may already force an outer
 * ScrollPane back to zero, so reading the ScrollPane only from the root-change
 * listener is too late. Each ScrollPane therefore keeps a stable value from the
 * previous JavaFX pulse. When fullscreen starts we snapshot those stable values,
 * not a value that may already have been reset by layout.</p>
 */
public final class FullscreenScrollPositionFix {
    private static final String ROOT_WATCH = FullscreenScrollPositionFix.class.getName() + ".rootWatch";
    private static final String SCENE_WATCH = FullscreenScrollPositionFix.class.getName() + ".sceneWatch";
    private static final String NODE_WATCH = FullscreenScrollPositionFix.class.getName() + ".nodeWatch";
    private static final String SCROLL_WATCH = FullscreenScrollPositionFix.class.getName() + ".scrollWatch";
    private static final String STABLE_PENDING = FullscreenScrollPositionFix.class.getName() + ".stablePending";
    private static final String STABLE_H = FullscreenScrollPositionFix.class.getName() + ".stableH";
    private static final String STABLE_V = FullscreenScrollPositionFix.class.getName() + ".stableV";
    private static final String STATE = FullscreenScrollPositionFix.class.getName() + ".state";

    private FullscreenScrollPositionFix() { }

    public static void install(Node root) {
        if (root == null) return;

        monitor(root, new IdentityHashMap<>());

        if (!Boolean.TRUE.equals(root.getProperties().get(ROOT_WATCH))) {
            root.getProperties().put(ROOT_WATCH, Boolean.TRUE);
            root.sceneProperty().addListener((obs, oldScene, newScene) -> installScene(newScene));
        }
        installScene(root.getScene());
    }

    private static void installScene(Scene scene) {
        if (scene == null || Boolean.TRUE.equals(scene.getProperties().get(SCENE_WATCH))) return;
        scene.getProperties().put(SCENE_WATCH, Boolean.TRUE);

        // Monitor the current root immediately. This makes the behaviour active
        // from the very first page opening, not after the first fullscreen cycle.
        monitor(scene.getRoot(), new IdentityHashMap<>());

        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            boolean oldFullscreen = isFullscreenRoot(oldRoot);
            boolean newFullscreen = isFullscreenRoot(newRoot);

            if (!oldFullscreen && newFullscreen && oldRoot != null) {
                // Use the stable values recorded before fullscreen content was
                // re-parented; current values may already have been reset to 0.
                scene.getProperties().put(STATE, capture(oldRoot));
                return;
            }

            if (oldFullscreen && !newFullscreen && newRoot != null) {
                Object raw = scene.getProperties().remove(STATE);
                monitor(newRoot, new IdentityHashMap<>());
                if (raw instanceof Snapshot snapshot) restore(snapshot);
                return;
            }

            if (newRoot != null && !newFullscreen) {
                monitor(newRoot, new IdentityHashMap<>());
            }
        });
    }

    private static boolean isFullscreenRoot(Node node) {
        return node != null && node.getStyleClass().contains("in-place-fullscreen");
    }

    /** Installs lightweight stable-value listeners on every logical ScrollPane. */
    private static void monitor(Node node, Map<Node, Boolean> visited) {
        if (node == null || visited.put(node, Boolean.TRUE) != null) return;

        if (node instanceof ScrollPane scroll) {
            monitorScroll(scroll);
            monitor(scroll.getContent(), visited);
            if (!Boolean.TRUE.equals(scroll.getProperties().get(NODE_WATCH))) {
                scroll.getProperties().put(NODE_WATCH, Boolean.TRUE);
                scroll.contentProperty().addListener((obs, oldContent, newContent) -> {
                    monitor(newContent, new IdentityHashMap<>());
                    scheduleStable(scroll);
                });
            }
            return;
        }

        if (node instanceof SplitPane split) {
            for (Node item : List.copyOf(split.getItems())) monitor(item, visited);
            if (!Boolean.TRUE.equals(split.getProperties().get(NODE_WATCH))) {
                split.getProperties().put(NODE_WATCH, Boolean.TRUE);
                split.getItems().addListener((ListChangeListener<Node>) change -> {
                    while (change.next()) if (change.wasAdded()) {
                        for (Node added : List.copyOf(change.getAddedSubList())) {
                            monitor(added, new IdentityHashMap<>());
                        }
                    }
                });
            }
            return;
        }

        if (node instanceof TabPane tabs) {
            for (Tab tab : List.copyOf(tabs.getTabs())) monitorTab(tab, visited);
            if (!Boolean.TRUE.equals(tabs.getProperties().get(NODE_WATCH))) {
                tabs.getProperties().put(NODE_WATCH, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) if (change.wasAdded()) {
                        for (Tab tab : List.copyOf(change.getAddedSubList())) {
                            monitorTab(tab, new IdentityHashMap<>());
                        }
                    }
                });
            }
            return;
        }

        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) monitor(child, visited);
            if (!Boolean.TRUE.equals(parent.getProperties().get(NODE_WATCH))) {
                parent.getProperties().put(NODE_WATCH, Boolean.TRUE);
                parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
                    while (change.next()) if (change.wasAdded()) {
                        for (Node added : List.copyOf(change.getAddedSubList())) {
                            monitor(added, new IdentityHashMap<>());
                        }
                    }
                });
            }
        }
    }

    private static void monitorTab(Tab tab, Map<Node, Boolean> visited) {
        if (tab == null) return;
        monitor(tab.getContent(), visited);
        if (!Boolean.TRUE.equals(tab.getProperties().get(NODE_WATCH))) {
            tab.getProperties().put(NODE_WATCH, Boolean.TRUE);
            tab.contentProperty().addListener((obs, oldContent, newContent) ->
                    monitor(newContent, new IdentityHashMap<>()));
        }
    }

    private static void monitorScroll(ScrollPane scroll) {
        if (scroll == null) return;
        if (!Boolean.TRUE.equals(scroll.getProperties().get(SCROLL_WATCH))) {
            scroll.getProperties().put(SCROLL_WATCH, Boolean.TRUE);
            scroll.getProperties().put(STABLE_H, scroll.getHvalue());
            scroll.getProperties().put(STABLE_V, scroll.getVvalue());
            scroll.hvalueProperty().addListener((obs, oldValue, newValue) -> scheduleStable(scroll));
            scroll.vvalueProperty().addListener((obs, oldValue, newValue) -> scheduleStable(scroll));
            scroll.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && !isFullscreenRoot(newScene.getRoot())) scheduleStable(scroll);
            });
        }
        scheduleStable(scroll);
    }

    /**
     * Commits user scroll values one pulse later. A fullscreen re-parent reset
     * also schedules this method, but by the time it runs the fullscreen root is
     * active, so that artificial zero is deliberately not recorded.
     */
    private static void scheduleStable(ScrollPane scroll) {
        if (scroll == null || Boolean.TRUE.equals(scroll.getProperties().get(STABLE_PENDING))) return;
        scroll.getProperties().put(STABLE_PENDING, Boolean.TRUE);
        Platform.runLater(() -> {
            scroll.getProperties().remove(STABLE_PENDING);
            Scene scene = scroll.getScene();
            if (scene == null || isFullscreenRoot(scene.getRoot())) return;
            scroll.getProperties().put(STABLE_H, scroll.getHvalue());
            scroll.getProperties().put(STABLE_V, scroll.getVvalue());
        });
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
            double stableH = number(scroll.getProperties().get(STABLE_H), scroll.getHvalue());
            double stableV = number(scroll.getProperties().get(STABLE_V), scroll.getVvalue());
            states.add(new ScrollState(scroll, stableH, stableV));
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

    private static double number(Object raw, double fallback) {
        return raw instanceof Number value ? value.doubleValue() : fallback;
    }

    private static void restore(Snapshot snapshot) {
        if (snapshot == null || snapshot.states().isEmpty()) return;

        // Reassert the values over several finite pulses. This intentionally
        // brackets JavaFX root layout and the focus restoration performed by
        // InPlaceFullscreen, without installing a timer or permanent loop.
        apply(snapshot);
        Platform.runLater(() -> {
            apply(snapshot);
            Platform.runLater(() -> {
                apply(snapshot);
                Platform.runLater(() -> apply(snapshot));
            });
        });

        // Native macOS fullscreen can finish resizing the window after the JavaFX
        // pulses above. One final delayed write keeps the exact original position.
        PauseTransition finalRestore = new PauseTransition(Duration.millis(180));
        finalRestore.setOnFinished(event -> apply(snapshot));
        finalRestore.play();
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
