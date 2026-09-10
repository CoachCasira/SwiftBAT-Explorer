package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.ChoiceBoxSkin;

import java.util.List;

/**
 * Makes the selected text of the two Explorer 2D ChoiceBoxes stay strictly
 * inside the area before the dropdown arrow, including on the very first
 * JavaFX layout pulse on macOS.
 *
 * <p>The previous implementation changed the Label only through runLater().
 * ChoiceBoxSkin could lay the label out again afterwards and the text would
 * paint under the arrow. This version owns the skin of the two overview
 * ChoiceBoxes and reapplies the text geometry after every native skin layout.
 * The value/items and popup behaviour are otherwise unchanged.</p>
 */
public final class ExplorerChoiceBoxEllipsisFix {
    private static final String INSTALLED = ExplorerChoiceBoxEllipsisFix.class.getName() + ".installed";
    private static final double RIGHT_TEXT_RESERVE = 38.0;

    private ExplorerChoiceBoxEllipsisFix() { }

    public static void install(Node root) {
        if (root == null) return;
        visitLogical(root);
    }

    private static void visitLogical(Node node) {
        if (node == null) return;

        if (node instanceof ChoiceBox<?> choice && insideOverviewChart(choice)) {
            installChoice(choice);
            return;
        }

        if (node instanceof ScrollPane scroll) {
            if (scroll.getContent() != null) visitLogical(scroll.getContent());
            return;
        }
        if (node instanceof SplitPane split) {
            for (Node item : List.copyOf(split.getItems())) visitLogical(item);
            return;
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : List.copyOf(tabs.getTabs())) visitLogical(tab.getContent());
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) visitLogical(child);
        }
    }

    private static boolean insideOverviewChart(Node node) {
        Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains("overview-chart-card")) return true;
            current = current.getParent();
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installChoice(ChoiceBox<?> choice) {
        if (!Boolean.TRUE.equals(choice.getProperties().get(INSTALLED))) {
            choice.getProperties().put(INSTALLED, Boolean.TRUE);

            // Install before the first render whenever possible. The custom skin
            // delegates all behaviour to ChoiceBoxSkin and only fixes the selected
            // label after the skin has completed its normal layout.
            try {
                choice.setSkin(new EllipsisChoiceBoxSkin((ChoiceBox) choice));
            } catch (RuntimeException ignored) {
                // If JavaFX is momentarily changing skins, the listener below
                // applies the same geometry as soon as the skin exists.
            }

            choice.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule(choice));
            choice.widthProperty().addListener((obs, oldWidth, newWidth) -> schedule(choice));
            choice.valueProperty().addListener((obs, oldValue, newValue) -> {
                updateTooltip(choice);
                schedule(choice);
            });
        }

        updateTooltip(choice);
        applySelectedLabel(choice);
        schedule(choice);
    }

    private static void schedule(ChoiceBox<?> choice) {
        Platform.runLater(() -> applySelectedLabel(choice));
    }

    private static void applySelectedLabel(ChoiceBox<?> choice) {
        if (choice == null) return;
        try {
            choice.applyCss();
            Node labelNode = choice.lookup(".label");
            if (!(labelNode instanceof Label label)) return;

            // Do not rely on a maxWidth that ChoiceBoxSkin can overwrite. The
            // right padding is part of the Label itself, therefore the actual
            // text layout always reserves the arrow area and ELLIPSIS is applied
            // before the arrow instead of underneath it.
            label.setMinWidth(0);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setWrapText(false);
            label.setTextOverrun(OverrunStyle.ELLIPSIS);
            label.setEllipsisString("...");
            label.setPadding(new Insets(0, RIGHT_TEXT_RESERVE, 0, 0));
            label.setClip(null);
            label.requestLayout();
        } catch (RuntimeException ignored) {
            // A ChoiceBox can briefly be between skins during tab replacement.
        }
    }

    private static void updateTooltip(ChoiceBox<?> choice) {
        Object value = choice.getValue();
        String text = value == null ? "" : value.toString();
        if (text.isBlank()) return;
        Tooltip tooltip = choice.getTooltip();
        if (tooltip == null) {
            tooltip = UiFactory.quickTooltip(text);
            choice.setTooltip(tooltip);
        } else {
            tooltip.setText(text);
        }
    }

    private static final class EllipsisChoiceBoxSkin<T> extends ChoiceBoxSkin<T> {
        private EllipsisChoiceBoxSkin(ChoiceBox<T> control) {
            super(control);
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            super.layoutChildren(x, y, w, h);
            applySelectedLabel(getSkinnable());
        }
    }
}
