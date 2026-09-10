package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;

import java.util.List;

/**
 * Keeps the whole live scene bilingual, including controls created or updated
 * after a GRB, map, population analysis or spectroscopy result has been loaded.
 */
public final class UiLocalizationWatcher {
    private static final String WATCHED = UiLocalizationWatcher.class.getName() + ".watched";
    private static final String TEXT_WATCHED = UiLocalizationWatcher.class.getName() + ".textWatched";
    private static final String PROMPT_WATCHED = UiLocalizationWatcher.class.getName() + ".promptWatched";
    private static final String LOCALIZING = UiLocalizationWatcher.class.getName() + ".localizing";
    private static final String UI_FACTORY_ORIGINAL = "swiftbat.originalText";
    private static final String SUPPLEMENTAL_ORIGINAL = UiTranslations.class.getName() + ".originalText";
    private static final String SUPPLEMENTAL_PROMPT = UiTranslations.class.getName() + ".promptText";
    private static final String I18N_LOCALIZED_IT = I18n.class.getName() + ".localized.it";
    private static final String I18N_LOCALIZED_EN = I18n.class.getName() + ".localized.en";

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
        installDynamicTextWatcher(node);
        localize(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) {
                    watch(added);
                    Platform.runLater(() -> localize(added));
                }
            }
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void installDynamicTextWatcher(Node node) {
        if (node instanceof Labeled labeled
                && !Boolean.TRUE.equals(labeled.getProperties().get(TEXT_WATCHED))) {
            labeled.getProperties().put(TEXT_WATCHED, Boolean.TRUE);
            labeled.textProperty().addListener((obs, oldText, newText) -> {
                if (Boolean.TRUE.equals(labeled.getProperties().get(LOCALIZING))
                        || labeled.textProperty().isBound() || newText == null) return;

                // I18n.setText(...) already owns its explicit IT/EN pair.
                if (labeled.getProperties().get(I18N_LOCALIZED_IT) instanceof String
                        && labeled.getProperties().get(I18N_LOCALIZED_EN) instanceof String) return;

                /*
                 * A runtime value is the new source value even when it has no
                 * translation (numbers are the important case). Previously a
                 * value such as the sky-map counter could remain associated
                 * with its construction-time "0" while the interface was in
                 * English; a later localization pass then restored that stale
                 * zero although the map itself was already populated.
                 */
                rememberSource(labeled, newText);
                if (I18n.language() == I18n.Language.IT) return;

                String translated = UiTranslations.t(newText);
                if (!translated.equals(newText)) {
                    labeled.getProperties().put(LOCALIZING, Boolean.TRUE);
                    try {
                        labeled.setText(translated);
                    } finally {
                        labeled.getProperties().remove(LOCALIZING);
                    }
                }
            });
        }

        if (node instanceof TextInputControl input
                && !Boolean.TRUE.equals(input.getProperties().get(PROMPT_WATCHED))) {
            input.getProperties().put(PROMPT_WATCHED, Boolean.TRUE);
            input.promptTextProperty().addListener((obs, oldText, newText) -> {
                if (Boolean.TRUE.equals(input.getProperties().get(LOCALIZING))
                        || input.promptTextProperty().isBound() || newText == null) return;
                if (I18n.language() == I18n.Language.IT) {
                    input.getProperties().put(SUPPLEMENTAL_PROMPT, newText);
                    return;
                }
                String translated = UiTranslations.t(newText);
                if (!translated.equals(newText)) {
                    input.getProperties().put(SUPPLEMENTAL_PROMPT, newText);
                    input.getProperties().put(LOCALIZING, Boolean.TRUE);
                    try {
                        input.setPromptText(translated);
                    } finally {
                        input.getProperties().remove(LOCALIZING);
                    }
                }
            });
        }
    }

    private static void rememberSource(Labeled labeled, String source) {
        labeled.getProperties().put(UI_FACTORY_ORIGINAL, source);
        labeled.getProperties().put(SUPPLEMENTAL_ORIGINAL, source);
    }

    private static void localize(Node node) {
        if (node == null) return;
        node.getProperties().put(LOCALIZING, Boolean.TRUE);
        try {
            UiTranslations.localizeTree(node);
        } finally {
            node.getProperties().remove(LOCALIZING);
        }
    }
}
