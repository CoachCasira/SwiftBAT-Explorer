package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.TimeEnergyHeatmapPane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Makes the spectroscopy Time-Energy tab report its real preferred height from
 * the very first opening. Previously the outer ScrollPane cached the height of
 * the initially selected spectroscopy tab; the Time-Energy content then became
 * correct only after a fullscreen/window resize caused a second layout pass.
 */
public final class SpectroscopyStartupLayoutFix {
    private static final String WATCHED = SpectroscopyStartupLayoutFix.class.getName() + ".watched";
    private static final String TABS_DONE = SpectroscopyStartupLayoutFix.class.getName() + ".tabsDone";
    private static final String HEATMAP_DONE = SpectroscopyStartupLayoutFix.class.getName() + ".heatmapDone";
    private static final String PENDING = SpectroscopyStartupLayoutFix.class.getName() + ".pending";

    private SpectroscopyStartupLayoutFix() {
    }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> scan(root));
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) watch(tab.getContent());
            }
            String key = WATCHED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(key))) {
                tabs.getProperties().put(key, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (!change.wasAdded()) continue;
                        for (Tab tab : change.getAddedSubList()) {
                            if (tab.getContent() != null) watch(tab.getContent());
                        }
                    }
                });
            }
        }

        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
            }
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void scan(Node node) {
        if (node == null) return;
        enhance(node);
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) scan(tab.getContent());
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) scan(child);
        }
    }

    private static void enhance(Node node) {
        if (node instanceof TimeEnergyHeatmapPane heatmap) prepareHeatmap(heatmap);
        if (node instanceof TabPane tabs && tabs.getStyleClass().contains("spectroscopy-tabs")) prepareTabs(tabs);
    }

    private static void prepareHeatmap(TimeEnergyHeatmapPane heatmap) {
        if (Boolean.TRUE.equals(heatmap.getProperties().get(HEATMAP_DONE))) return;
        heatmap.getProperties().put(HEATMAP_DONE, Boolean.TRUE);

        // These values include the axis title, legend and interaction hint drawn
        // inside the Canvas; they must not be inferred from a not-yet-laid-out tab.
        heatmap.setMinHeight(430);
        heatmap.setPrefHeight(455);
        heatmap.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(heatmap, Priority.NEVER);

        heatmap.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) requestStableLayout(heatmap);
        });
        heatmap.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 1) requestStableLayout(heatmap);
        });
        requestStableLayout(heatmap);
    }

    private static void prepareTabs(TabPane tabs) {
        if (Boolean.TRUE.equals(tabs.getProperties().get(TABS_DONE))) return;
        tabs.getProperties().put(TABS_DONE, Boolean.TRUE);

        tabs.setMinWidth(0);
        tabs.setMaxWidth(Double.MAX_VALUE);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) ->
                requestStableLayout(tabs));
        tabs.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) requestStableLayout(tabs);
        });
        requestStableLayout(tabs);
    }

    private static void requestStableLayout(Node source) {
        Parent spectroscopy = ancestorBySimpleName(source, "SpectroscopyPane");
        if (spectroscopy == null || Boolean.TRUE.equals(spectroscopy.getProperties().get(PENDING))) return;
        spectroscopy.getProperties().put(PENDING, Boolean.TRUE);
        Platform.runLater(() -> {
            spectroscopy.getProperties().remove(PENDING);
            applyStableLayout(spectroscopy);
        });
    }

    private static void applyStableLayout(Parent spectroscopy) {
        TabPane tabs = findSpectroscopyTabs(spectroscopy);
        if (tabs == null) return;

        Tab selected = tabs.getSelectionModel().getSelectedItem();
        Node selectedContent = selected == null ? null : selected.getContent();
        TimeEnergyHeatmapPane heatmap = findDescendant(selectedContent, TimeEnergyHeatmapPane.class);

        if (heatmap != null) {
            prepareHeatmap(heatmap);
            Region card = ancestorWithStyle(heatmap, "time-energy-card");
            if (card != null) {
                card.setMinHeight(515);
                card.setPrefHeight(535);
                card.setMaxHeight(Double.MAX_VALUE);
            }
            if (selectedContent instanceof Region content) {
                content.setMinHeight(610);
                content.setPrefHeight(625);
                content.setMaxHeight(Double.MAX_VALUE);
            }
            tabs.setMinHeight(665);
            tabs.setPrefHeight(680);
            tabs.setMaxHeight(Double.MAX_VALUE);
        } else {
            // Do not leave the Time-Energy minimum imposed on the other tabs.
            tabs.setMinHeight(Region.USE_PREF_SIZE);
            tabs.setPrefHeight(Region.USE_COMPUTED_SIZE);
            tabs.setMaxHeight(Double.MAX_VALUE);
        }

        // Force the preferred-height chain to be recomputed while the tab is
        // already selected, not after a later fullscreen/window resize.
        spectroscopy.applyCss();
        spectroscopy.layout();
        tabs.requestLayout();
        Parent current = tabs.getParent();
        while (current != null && current != spectroscopy) {
            current.requestLayout();
            current = current.getParent();
        }
        ScrollPane outer = findDescendant(spectroscopy, ScrollPane.class);
        if (outer != null) outer.requestLayout();
    }

    private static TabPane findSpectroscopyTabs(Node node) {
        if (node instanceof TabPane tabs && tabs.getStyleClass().contains("spectroscopy-tabs")) return tabs;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TabPane found = findSpectroscopyTabs(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Region ancestorWithStyle(Node node, String styleClass) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (current instanceof Region region && current.getStyleClass().contains(styleClass)) return region;
            current = current.getParent();
        }
        return null;
    }

    private static Parent ancestorBySimpleName(Node node, String simpleName) {
        Node current = node;
        while (current != null) {
            if (current instanceof Parent parent && current.getClass().getSimpleName().equals(simpleName)) return parent;
            current = current.getParent();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Node> T findDescendant(Node node, Class<T> type) {
        if (node == null) return null;
        if (type.isInstance(node)) return (T) node;
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                T found = findDescendant(tab.getContent(), type);
                if (found != null) return found;
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findDescendant(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
