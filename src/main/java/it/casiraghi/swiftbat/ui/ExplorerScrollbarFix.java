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
 * Stable Explorer scrollbar geometry for macOS and Windows.
 *
 * <p>JavaFX's macOS ScrollPane skin can lay out a very small thumb when the
 * page is long. Region minHeight alone is not enough because ScrollBarSkin
 * explicitly resizes the thumb. We therefore clamp ScrollBar.visibleAmount to
 * a small minimum fraction of its range; that is the value the skin actually
 * uses when computing thumb length.</p>
 */
public final class ExplorerScrollbarFix {
    private static final String INSTALLED = ExplorerScrollbarFix.class.getName() + ".installed";
    private static final String BAR_INSTALLED = ExplorerScrollbarFix.class.getName() + ".barInstalled";
    private static final String CLAMPING = ExplorerScrollbarFix.class.getName() + ".clamping";
    private static final double MIN_VISIBLE_FRACTION = 0.12;

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
                installBarClamp(bar);
                clampVisibleAmount(bar);
                if (bar.getOrientation() == Orientation.VERTICAL) {
                    bar.setMinWidth(12);
                    bar.setPrefWidth(12);
                    bar.setMaxWidth(12);
                    for (Node part : bar.lookupAll(".thumb")) {
                        if (part instanceof Region thumb) {
                            thumb.setMinWidth(10);
                            thumb.setStyle(appendStyle(thumb.getStyle(),
                                    "-fx-min-width: 10px; -fx-background-radius: 6px;"));
                        }
                    }
                } else {
                    bar.setMinHeight(12);
                    bar.setPrefHeight(12);
                    bar.setMaxHeight(12);
                    for (Node part : bar.lookupAll(".thumb")) {
                        if (part instanceof Region thumb) {
                            thumb.setMinHeight(10);
                            thumb.setStyle(appendStyle(thumb.getStyle(),
                                    "-fx-min-height: 10px; -fx-background-radius: 6px;"));
                        }
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // A ScrollPane may briefly be between skins during a tab replacement.
        }
    }

    private static void installBarClamp(ScrollBar bar) {
        if (Boolean.TRUE.equals(bar.getProperties().get(BAR_INSTALLED))) return;
        bar.getProperties().put(BAR_INSTALLED, Boolean.TRUE);
        bar.visibleAmountProperty().addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(bar.getProperties().get(CLAMPING))) return;
            Platform.runLater(() -> clampVisibleAmount(bar));
        });
        bar.minProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(() -> clampVisibleAmount(bar)));
        bar.maxProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(() -> clampVisibleAmount(bar)));
    }

    private static void clampVisibleAmount(ScrollBar bar) {
        if (bar == null || bar.visibleAmountProperty().isBound()) return;
        double range = bar.getMax() - bar.getMin();
        if (!Double.isFinite(range) || range <= 0) return;
        double minimum = range * MIN_VISIBLE_FRACTION;
        if (bar.getVisibleAmount() >= minimum) return;
        try {
            bar.getProperties().put(CLAMPING, Boolean.TRUE);
            bar.setVisibleAmount(minimum);
        } catch (RuntimeException ignored) {
            // Keep the native value if a platform skin temporarily owns it.
        } finally {
            bar.getProperties().remove(CLAMPING);
        }
    }

    private static String appendStyle(String current, String extra) {
        String base = current == null ? "" : current.trim();
        return base.isEmpty() ? extra : base + (base.endsWith(";") ? " " : "; ") + extra;
    }
}
