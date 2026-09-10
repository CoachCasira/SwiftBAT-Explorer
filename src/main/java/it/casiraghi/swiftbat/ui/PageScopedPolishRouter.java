package it.casiraghi.swiftbat.ui;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * Routes the final lightweight polish only to pages when MainView swaps them
 * into the page host. It observes one small children list, not the scene graph.
 */
public final class PageScopedPolishRouter {
    private static final String DONE = PageScopedPolishRouter.class.getName() + ".done";

    private PageScopedPolishRouter() { }

    public static void install(Parent root) {
        StackPane pageHost = findPageHost(root);
        if (pageHost == null || Boolean.TRUE.equals(pageHost.getProperties().get(DONE))) return;
        pageHost.getProperties().put(DONE, Boolean.TRUE);

        for (Node child : List.copyOf(pageHost.getChildren())) route(child);
        pageHost.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) route(added);
            }
        });
    }

    private static void route(Node node) {
        if (node instanceof Parent parent) FinalMacAndPopulationPolish.install(parent);
    }

    private static StackPane findPageHost(Node node) {
        if (node instanceof StackPane pane && pane.getStyleClass().contains("page-host")) return pane;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                StackPane found = findPageHost(child);
                if (found != null) return found;
            }
        }
        return null;
    }
}
