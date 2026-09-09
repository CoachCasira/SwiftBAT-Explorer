package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.List;

/**
 * Keeps the whole live scene bilingual, including controls created after a GRB,
 * map, population analysis or spectroscopy result has been loaded.
 */
public final class UiLocalizationWatcher {
    private static final String WATCHED = UiLocalizationWatcher.class.getName() + ".watched";

    private UiLocalizationWatcher() { }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> UiTranslations.localizeTree(root));
        I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                Platform.runLater(() -> UiTranslations.localizeTree(root)));
    }

    private static void watch(Node node) {
        if (node == null) return;
        UiTranslations.localizeTree(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) {
                    watch(added);
                    Platform.runLater(() -> UiTranslations.localizeTree(added));
                }
            }
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }
}
