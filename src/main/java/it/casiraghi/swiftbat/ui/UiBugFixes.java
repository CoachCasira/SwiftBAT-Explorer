package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Correzioni puntuali emerse dai test dell'interfaccia 1.3.0.
 *
 * Restano separate dalla logica di localizzazione: questa classe interviene
 * soltanto sul comportamento/layout di controlli gia' costruiti.
 */
public final class UiBugFixes {
    private static final String WATCHED = UiBugFixes.class.getName() + ".watched";
    private static final String METADATA_SORT_FIXED = UiBugFixes.class.getName() + ".metadataSortFixed";
    private static final String METADATA_QUERY = UiBugFixes.class.getName() + ".metadataQuery";

    private UiBugFixes() {
    }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> scan(root));
    }

    private static void watch(Node node) {
        if (node == null) return;
        fixNode(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;

        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node added : change.getAddedSubList()) watch(added);
                }
            }
            Platform.runLater(() -> scan(parent));
        });
        for (Node child : parent.getChildrenUnmodifiable()) watch(child);
    }

    private static void scan(Node node) {
        if (node == null) return;
        fixNode(node);
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) scan(child);
        }
    }

    private static void fixNode(Node node) {
        fixBoundPrompt(node);
        if (node instanceof HBox toolbar && hasStyle(toolbar, "metadata-field-toolbar")) {
            fixMetadataSearch(toolbar);
        }
    }

    /**
     * JavaFX crea internamente un FakeFocusTextField per i ComboBox editabili e ne
     * lega la promptTextProperty al controllo proprietario. I18n.localizeTree visita
     * anche questi nodi interni; chiamare setPromptText su una property bound genera
     * IllegalArgumentException ("A bound value cannot be set").
     *
     * Il nodo interno non ha bisogno di mantenere quel binding: il prompt visibile
     * viene gia' gestito dal ComboBox/editor. Lo sblocchiamo appena compare nello
     * scene graph, prima che il localizzatore possa provare a modificarlo.
     */
    private static void fixBoundPrompt(Node node) {
        if (!(node instanceof TextInputControl input)) return;
        if (!input.promptTextProperty().isBound()) return;
        if (!isInsideEditableCombo(node)) return;
        input.promptTextProperty().unbind();
    }

    private static boolean isInsideEditableCombo(Node node) {
        Parent parent = node == null ? null : node.getParent();
        while (parent != null) {
            if (parent instanceof ComboBox<?> combo) return combo.isEditable();
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Comportamento desiderato del selettore metadata:
     * - il campo di ricerca parte vuoto anche se la tabella ha gia' selezionato SIMPLE;
     * - il testo digitato non viene sostituito dalla selezione corrente quando il focus esce;
     * - il popup mostra due risultati alla volta e usa la scrollbar per gli altri;
     * - A/Z riordina la lista mantenendo query e popup aperti.
     */
    private static void fixMetadataSearch(HBox toolbar) {
        if (Boolean.TRUE.equals(toolbar.getProperties().get(METADATA_SORT_FIXED))) return;

        ComboBox<?> combo = toolbar.getChildren().stream()
                .filter(ComboBox.class::isInstance)
                .map(ComboBox.class::cast)
                .filter(candidate -> hasStyle(candidate, "metadata-search-combo"))
                .findFirst().orElse(null);
        Button sort = toolbar.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(candidate -> hasStyle(candidate, "metadata-sort-button"))
                .findFirst().orElse(null);
        if (combo == null || sort == null || combo.getEditor() == null) return;

        toolbar.getProperties().put(METADATA_SORT_FIXED, Boolean.TRUE);

        // Popup volutamente compatto: due risultati visibili, gli altri via scrollbar.
        combo.setVisibleRowCount(2);
        combo.getProperties().put(METADATA_QUERY, "");

        // Conserva esclusivamente cio' che l'utente vede/digita mentre l'editor e' attivo.
        combo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (combo.getEditor().isFocused()) {
                combo.getProperties().put(METADATA_QUERY, newValue == null ? "" : newValue);
            }
        });

        // Il bridge originale riallinea l'editor al campo selezionato (es. SIMPLE) alla
        // perdita del focus. Questo listener, installato dopo il bridge, ripristina la
        // query dell'utente senza cambiare la riga metadata realmente selezionata.
        combo.getEditor().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) return;
            Object saved = combo.getProperties().get(METADATA_QUERY);
            String query = saved instanceof String text ? text : "";
            Platform.runLater(() -> {
                if (combo.getEditor().isFocused()) return;
                combo.setValue(null);
                combo.getEditor().setText(query);
                combo.getEditor().positionCaret(query.length());
            });
        });

        // Il sort non prende il focus: cosi' il campo resta in modalita' ricerca e il
        // rebuild gia' presente in UiRefinements puo' riordinare la lista senza chiuderla.
        sort.setFocusTraversable(false);
        sort.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            combo.requestFocus();
            combo.getEditor().requestFocus();
        });
        sort.addEventHandler(ActionEvent.ACTION, event -> Platform.runLater(() -> {
            String query = combo.getEditor().getText() == null ? "" : combo.getEditor().getText();
            combo.getProperties().put(METADATA_QUERY, query);
            combo.getEditor().requestFocus();
            combo.getEditor().positionCaret(query.length());
            if (!combo.getItems().isEmpty()) combo.show();
        }));

        // Il campo visibile e' una ricerca, non la rappresentazione della selezione
        // iniziale della tabella: all'apertura deve quindi essere vuoto.
        Platform.runLater(() -> {
            combo.hide();
            combo.setValue(null);
            combo.getEditor().clear();
            combo.getProperties().put(METADATA_QUERY, "");
        });
    }

    private static boolean hasStyle(Node node, String styleClass) {
        return node != null && node.getStyleClass().contains(styleClass);
    }
}
