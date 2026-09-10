package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

import java.util.Set;

/**
 * Gives Explorer scrollbars a stable cross-platform geometry. macOS can shrink
 * the JavaFX thumb to a small circular dot on long pages; we enforce a readable
 * minimum thumb length after the ScrollPane skin has been realized.
 */
public final class ExplorerScrollbarFix {
    private static final String INSTALLED = ExplorerScrollbarFix.class.getName() + ".installed";

    private ExplorerScrollbarFix() { }

    public static void install(Node root) {
        if (root == null) return;
        if (root instanceof Parent parent) schedule(parent);
        installOnScrollPanes(root);
    }

    private static void installOnScrollPanes(Node node) {
        if (node == null) return;
        if (node instanceof ScrollPane scroll) {
            if (!Boolean.TRUE.equals(scroll.getProperties().get(INSTALLED))) {
                scroll.getProperties().put(INSTALLED, Boolean.TRUE);
                scroll.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule(scroll));
                scroll.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> schedule(scroll));
                scroll.contentProperty().addListener((obs, oldContent, newContent) -> schedule(scroll));
            }
            schedule(scroll);
            if (scroll.getContent() != null) installOnScrollPanes(scroll.getContent());
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) installOnScrollPanes(child);
        }
    }

    private static void schedule(Parent root) {
        Platform.runLater(() -> {
            fixNow(root);
            Platform.runLater(() -> fixNow(root));
        });
    }

    private static void fixNow(Parent root) {
        try {
            root.applyCss();
            Set<Node> bars = root.lookupAll(".scroll-bar");
            for (Node node : bars) {
                if (!(node instanceof ScrollBar bar)) continue;
                if (bar.getOrientation() == Orientation.VERTICAL) {
                    bar.setMinWidth(12);
                    bar.setPrefWidth(12);
                    bar.setMaxWidth(12);
                    for (Node part : bar.lookupAll(".thumb")) {
                        if (part instanceof Region thumb) {
                            thumb.setMinHeight(46);
                            thumb.setMinWidth(10);
                            thumb.setStyle(appendStyle(thumb.getStyle(), "-fx-min-height: 46px; -fx-min-width: 10px;"));
                        }
                    }
                } else {
                    bar.setMinHeight(12);
                    bar.setPrefHeight(12);
                    bar.setMaxHeight(12);
                    for (Node part : bar.lookupAll(".thumb")) {
                        if (part instanceof Region thumb) {
                            thumb.setMinWidth(46);
                            thumb.setMinHeight(10);
                            thumb.setStyle(appendStyle(thumb.getStyle(), "-fx-min-width: 46px; -fx-min-height: 10px;"));
                        }
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // ScrollPane may be between skins during a tab/page replacement.
        }
    }

    private static String appendStyle(String current, String extra) {
        String base = current == null ? "" : current.trim();
        return base.isEmpty() ? extra : base + (base.endsWith(";") ? " " : "; ") + extra;
    }
}
