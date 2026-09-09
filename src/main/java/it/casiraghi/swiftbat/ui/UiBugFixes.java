package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
        if (node instanceof HBox toolbar && hasStyle(toolbar, "metadata-field-toolbar")) {
            fixMetadataSortPopup(toolbar);
        }
    }

    /**
     * Il pulsante A/Z non deve lasciare il focus nell'editor del ComboBox.
     * In caso contrario il rebuild della lista interpreta il click sul sort
     * come se l'utente stesse ancora digitando e riapre automaticamente un
     * popup molto alto sopra il controllo.
     */
    private static void fixMetadataSortPopup(HBox toolbar) {
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
        if (combo == null || sort == null) return;

        toolbar.getProperties().put(METADATA_SORT_FIXED, Boolean.TRUE);

        // Mantiene il menu compatto anche con centinaia di keyword FITS.
        combo.setVisibleRowCount(7);

        // Il click sul sort deve togliere il focus dal campo di ricerca.
        // Cosi' il rebuild non richiama show() automaticamente.
        sort.setFocusTraversable(true);
        sort.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> combo.hide());
        sort.addEventHandler(ActionEvent.ACTION, event ->
                // Doppio runLater: viene eseguito anche dopo l'eventuale show()
                // gia' accodato dal listener del filtro.
                Platform.runLater(() -> Platform.runLater(combo::hide)));
    }

    private static boolean hasStyle(Node node, String styleClass) {
        return node != null && node.getStyleClass().contains(styleClass);
    }
}
