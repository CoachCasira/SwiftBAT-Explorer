package it.casiraghi.swiftbat.ui;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;

import java.util.List;

/**
 * Keeps the live scene bilingual without scheduling a full-tree localization
 * pass every time a single label or child is created. Dynamic controls are
 * translated locally; the complete tree is revisited only when the user really
 * changes language.
 */
public final class UiLocalizationWatcher {
    private static final String WATCHED = UiLocalizationWatcher.class.getName() + ".watched";
    private static final String TABS_WATCHED = UiLocalizationWatcher.class.getName() + ".tabsWatched";
    private static final String TAB_TEXT_WATCHED = UiLocalizationWatcher.class.getName() + ".tabTextWatched";
    private static final String TEXT_WATCHED = UiLocalizationWatcher.class.getName() + ".textWatched";
    private static final String PROMPT_WATCHED = UiLocalizationWatcher.class.getName() + ".promptWatched";
    private static final String LOCALIZING = UiLocalizationWatcher.class.getName() + ".localizing";
    private static final String UI_FACTORY_ORIGINAL = "swiftbat.originalText";
    private static final String SUPPLEMENTAL_ORIGINAL = UiTranslations.class.getName() + ".originalText";
    private static final String SUPPLEMENTAL_PROMPT = UiTranslations.class.getName() + ".promptText";
    private static final String I18N_LOCALIZED_IT = I18n.class.getName() + ".localized.it";
    private static final String I18N_LOCALIZED_EN = I18n.class.getName() + ".localized.en";
    private static final String MISSING = "[Missing English translation]";

    private static boolean localizationPass;

    private UiLocalizationWatcher() {
    }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        localize(root);
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> localize(root));
    }

    private static void watch(Node node) {
        if (node == null) return;
        installDynamicTextWatcher(node);

        if (node instanceof TabPane tabs && !Boolean.TRUE.equals(tabs.getProperties().get(TABS_WATCHED))) {
            tabs.getProperties().put(TABS_WATCHED, Boolean.TRUE);
            for (Tab tab : tabs.getTabs()) {
                installTabTextWatcher(tab);
                if (tab.getContent() != null) watch(tab.getContent());
            }
            tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                while (change.next()) {
                    if (!change.wasAdded()) continue;
                    for (Tab tab : change.getAddedSubList()) {
                        installTabTextWatcher(tab);
                        if (tab.getContent() == null) continue;
                        watch(tab.getContent());
                        localize(tab.getContent());
                    }
                }
            });
        }

        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) {
                    watch(added);
                    localize(added);
                }
            }
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void installTabTextWatcher(Tab tab) {
        if (tab == null || Boolean.TRUE.equals(tab.getProperties().get(TAB_TEXT_WATCHED))) return;
        tab.getProperties().put(TAB_TEXT_WATCHED, Boolean.TRUE);
        if (!(tab.getProperties().get(SUPPLEMENTAL_ORIGINAL) instanceof String)
                && tab.getText() != null && !tab.getText().isBlank() && !MISSING.equals(tab.getText())) {
            tab.getProperties().put(SUPPLEMENTAL_ORIGINAL, tab.getText());
        }
        tab.textProperty().addListener((obs, oldText, newText) -> {
            if (localizationPass || newText == null || tab.textProperty().isBound()) return;
            Object raw = tab.getProperties().get(SUPPLEMENTAL_ORIGINAL);
            String source = raw instanceof String text ? text : null;
            if (source != null && I18n.language() == I18n.Language.EN) {
                String expected = UiTranslations.t(source);
                if (newText.equals(expected)) return;
                if (MISSING.equals(newText)) {
                    tab.setText(MISSING.equals(expected) ? source : expected);
                    return;
                }
            }
            if (!newText.isBlank() && !MISSING.equals(newText)
                    && I18n.language() == I18n.Language.IT) {
                tab.getProperties().put(SUPPLEMENTAL_ORIGINAL, newText);
            }
        });
    }

    private static void installDynamicTextWatcher(Node node) {
        if (node instanceof Labeled labeled
                && !Boolean.TRUE.equals(labeled.getProperties().get(TEXT_WATCHED))) {
            labeled.getProperties().put(TEXT_WATCHED, Boolean.TRUE);

            boolean explicitI18n = hasExplicitPair(labeled);
            if (!explicitI18n && !labeled.textProperty().isBound() && labeled.getText() != null) {
                rememberSource(labeled, labeled.getText());
            }

            labeled.textProperty().addListener((obs, oldText, newText) -> {
                if (localizationPass
                        || Boolean.TRUE.equals(labeled.getProperties().get(LOCALIZING))
                        || labeled.textProperty().isBound()
                        || newText == null
                        || hasExplicitPair(labeled)) return;

                String stored = storedSource(labeled);
                if (I18n.language() == I18n.Language.EN && stored != null) {
                    String expected = UiTranslations.t(stored);
                    if (newText.equals(expected)) return;
                    if (MISSING.equals(newText)) {
                        setLocalizedText(labeled, MISSING.equals(expected) ? stored : expected);
                        return;
                    }
                }

                rememberSource(labeled, newText);
                if (I18n.language() == I18n.Language.EN) {
                    String translated = UiTranslations.t(newText);
                    if (!translated.equals(newText) && !MISSING.equals(translated)) {
                        setLocalizedText(labeled, translated);
                    }
                }
            });
        }

        if (node instanceof TextInputControl input
                && !Boolean.TRUE.equals(input.getProperties().get(PROMPT_WATCHED))) {
            input.getProperties().put(PROMPT_WATCHED, Boolean.TRUE);
            if (!input.promptTextProperty().isBound() && input.getPromptText() != null) {
                input.getProperties().put(SUPPLEMENTAL_PROMPT, input.getPromptText());
            }
            input.promptTextProperty().addListener((obs, oldText, newText) -> {
                if (localizationPass
                        || Boolean.TRUE.equals(input.getProperties().get(LOCALIZING))
                        || input.promptTextProperty().isBound()
                        || newText == null) return;

                Object sourceValue = input.getProperties().get(SUPPLEMENTAL_PROMPT);
                if (I18n.language() == I18n.Language.EN && sourceValue instanceof String source) {
                    String expected = UiTranslations.t(source);
                    if (newText.equals(expected)) return;
                }

                input.getProperties().put(SUPPLEMENTAL_PROMPT, newText);
                if (I18n.language() == I18n.Language.EN) {
                    String translated = UiTranslations.t(newText);
                    if (!translated.equals(newText) && !MISSING.equals(translated)) {
                        input.getProperties().put(LOCALIZING, Boolean.TRUE);
                        try {
                            input.setPromptText(translated);
                        } finally {
                            input.getProperties().remove(LOCALIZING);
                        }
                    }
                }
            });
        }
    }

    private static boolean hasExplicitPair(Labeled labeled) {
        return labeled.getProperties().get(I18N_LOCALIZED_IT) instanceof String
                && labeled.getProperties().get(I18N_LOCALIZED_EN) instanceof String;
    }

    private static String storedSource(Labeled labeled) {
        Object value = labeled.getProperties().get(UI_FACTORY_ORIGINAL);
        if (value instanceof String text && !text.isBlank()) return text;
        value = labeled.getProperties().get(SUPPLEMENTAL_ORIGINAL);
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private static void rememberSource(Labeled labeled, String source) {
        if (source == null || source.isBlank() || MISSING.equals(source)) return;
        labeled.getProperties().put(UI_FACTORY_ORIGINAL, source);
        labeled.getProperties().put(SUPPLEMENTAL_ORIGINAL, source);
    }

    private static void setLocalizedText(Labeled labeled, String value) {
        if (value == null || value.isBlank()) return;
        labeled.getProperties().put(LOCALIZING, Boolean.TRUE);
        try {
            labeled.setText(value);
        } finally {
            labeled.getProperties().remove(LOCALIZING);
        }
    }

    private static void localize(Node node) {
        if (node == null) return;
        boolean previous = localizationPass;
        localizationPass = true;
        try {
            UiTranslations.localizeTree(node);
        } finally {
            localizationPass = previous;
        }
    }
}
