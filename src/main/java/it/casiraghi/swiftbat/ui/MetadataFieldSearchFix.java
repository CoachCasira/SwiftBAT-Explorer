package it.casiraghi.swiftbat.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Installs the rebuilt MetadataFieldSelector in place of ExplorerPage's legacy
 * FITS-keyword ChoiceBox. The old ChoiceBox remains only as a data bridge for
 * the listeners already defined by ExplorerPage; it is physically removed from
 * the visible scene graph, so its platform popup can never be shown.
 */
public final class MetadataFieldSearchFix {
    private static final String INSTALLED = MetadataFieldSearchFix.class.getName() + ".v2Installed";

    private MetadataFieldSearchFix() { }

    public static void install(Node root) {
        if (root == null) return;
        for (ChoiceBox<?> raw : findAll(root, ChoiceBox.class)) {
            if (looksLikeMetadataKeywordChoice(raw)) replace(raw);
        }
    }

    @SuppressWarnings("unchecked")
    private static void replace(ChoiceBox<?> raw) {
        ChoiceBox<String> original = (ChoiceBox<String>) raw;
        if (Boolean.TRUE.equals(original.getProperties().get(INSTALLED))) return;
        if (!(original.getParent() instanceof VBox parent)) return;

        List<String> values = original.getItems().stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        if (values.isEmpty()) return;

        int index = parent.getChildren().indexOf(original);
        if (index < 0) return;

        original.getProperties().put(INSTALLED, Boolean.TRUE);

        MetadataFieldSelector selector = new MetadataFieldSelector(
                values,
                original.getValue(),
                original::setValue);
        selector.setMinWidth(0);
        selector.setMaxWidth(Double.MAX_VALUE);

        // Synchronize row -> selector. ExplorerPage already performs row ->
        // original ChoiceBox, so this listener closes the bridge in the other
        // direction without duplicating metadata/explanation logic.
        original.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()
                    && !newValue.equals(selector.selectedValue())) {
                selector.setSelectedValue(newValue, false);
            }
        });

        // Remove every legacy compatibility control around the old selector.
        parent.getChildren().removeIf(node -> node != original && (
                node.getStyleClass().contains("metadata-field-toolbar")
                        || node.getStyleClass().contains("metadata-field-selector-v2")));

        // Replace the actual node, not its skin. This is the important difference
        // from the previous attempts: there is no ChoiceBox/ComboBox popup left
        // in the visible hierarchy at all.
        parent.getChildren().set(index, selector);
    }

    private static boolean looksLikeMetadataKeywordChoice(ChoiceBox<?> choice) {
        if (choice == null || choice.getItems() == null || choice.getItems().size() < 3) return false;
        boolean simple = false;
        boolean bitpix = false;
        boolean naxis = false;
        for (Object value : choice.getItems()) {
            String text = value == null ? "" : value.toString().trim().toUpperCase(Locale.ROOT);
            if ("SIMPLE".equals(text)) simple = true;
            else if ("BITPIX".equals(text)) bitpix = true;
            else if ("NAXIS".equals(text)) naxis = true;
        }
        return simple && bitpix && naxis;
    }

    private static <T extends Node> List<T> findAll(Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collect(root, type, result);
        return result;
    }

    private static <T extends Node> void collect(Node node, Class<T> type, List<T> result) {
        if (node == null) return;
        if (type.isInstance(node)) result.add(type.cast(node));
        if (node instanceof ScrollPane scroll && scroll.getContent() != null) collect(scroll.getContent(), type, result);
        if (node instanceof SplitPane split) {
            for (Node item : split.getItems()) collect(item, type, result);
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) collect(tab.getContent(), type, result);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collect(child, type, result);
        }
    }
}
