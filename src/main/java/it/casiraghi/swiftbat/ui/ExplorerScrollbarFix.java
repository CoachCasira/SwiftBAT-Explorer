package it.casiraghi.swiftbat.ui;

import javafx.scene.Node;

/**
 * Marks Explorer subtrees that use the native JavaFX scroll implementation.
 *
 * <p>Scrollbar geometry is intentionally CSS-only. Replacing or resizing the
 * JavaFX skin from {@code layoutChildren} creates a layout feedback loop with
 * virtualized lists, which can saturate the UI thread while Explore is open.</p>
 */
public final class ExplorerScrollbarFix {
    private static final String STYLE_CLASS = "explorer-native-scrollbars";

    private ExplorerScrollbarFix() {
    }

    public static void install(Node root) {
        if (root != null && !root.getStyleClass().contains(STYLE_CLASS)) {
            root.getStyleClass().add(STYLE_CLASS);
        }
    }
}
