package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
        // Prima aggancia tutti i watcher e fotografa i valori runtime correnti;
        // solo dopo esegue la localizzazione dell'albero.
        watch(root);
        Platform.runLater(() -> localize(root));
        I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                Platform.runLater(() -> localize(root)));
    }

    private static void watch(Node node) {
        if (node == null) return;
        installDynamicTextWatcher(node);

        // I contenuti dei Tab non selezionati possono non appartenere ancora
        // alla gerarchia visuale: vanno osservati comunque, prima che vengano mostrati.
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) watch(tab.getContent());
            }
            tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                while (change.next()) {
                    if (!change.wasAdded()) continue;
                    for (Tab tab : change.getAddedSubList()) {
                        if (tab.getContent() != null) {
                            watch(tab.getContent());
                            Platform.runLater(() -> localize(tab.getContent()));
                        }
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

            boolean explicitI18n = labeled.getProperties().get(I18N_LOCALIZED_IT) instanceof String
                    && labeled.getProperties().get(I18N_LOCALIZED_EN) instanceof String;
            if (!explicitI18n && !labeled.textProperty().isBound() && labeled.getText() != null) {
                // Fondamentale per pagine costruite fuori scena: il valore può
                // essere già passato da "0" al conteggio reale prima della prima visita.
                rememberSource(labeled, labeled.getText());
            }

            labeled.textProperty().addListener((obs, oldText, newText) -> {
                if (Boolean.TRUE.equals(labeled.getProperties().get(LOCALIZING))
                        || labeled.textProperty().isBound() || newText == null) return;

                // I18n.setText(...) already owns its explicit IT/EN pair.
                if (labeled.getProperties().get(I18N_LOCALIZED_IT) instanceof String
                        && labeled.getProperties().get(I18N_LOCALIZED_EN) instanceof String) return;

                // Anche numeri e valori senza traduzione sono sorgenti runtime reali.
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
            if (!input.promptTextProperty().isBound() && input.getPromptText() != null) {
                input.getProperties().put(SUPPLEMENTAL_PROMPT, input.getPromptText());
            }
            input.promptTextProperty().addListener((obs, oldText, newText) -> {
                if (Boolean.TRUE.equals(input.getProperties().get(LOCALIZING))
                        || input.promptTextProperty().isBound() || newText == null) return;
                input.getProperties().put(SUPPLEMENTAL_PROMPT, newText);
                if (I18n.language() == I18n.Language.IT) return;

                String translated = UiTranslations.t(newText);
                if (!translated.equals(newText)) {
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
        setLocalizing(node, true);
        try {
            UiTranslations.localizeTree(node);
        } finally {
            setLocalizing(node, false);
        }
    }

    private static void setLocalizing(Node node, boolean value) {
        if (node == null) return;
        if (value) node.getProperties().put(LOCALIZING, Boolean.TRUE);
        else node.getProperties().remove(LOCALIZING);

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) setLocalizing(tab.getContent(), value);
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                setLocalizing(child, value);
            }
        }
    }
}
