package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;

/**
 * Prevents the two Explorer 2D ChoiceBox labels from painting underneath the
 * dropdown arrow on narrow layouts. The native popup/items are untouched: only
 * the visible selected-value label is constrained and ellipsized.
 */
public final class ExplorerChoiceBoxEllipsisFix {
    private static final String INSTALLED = ExplorerChoiceBoxEllipsisFix.class.getName() + ".installed";
    private static final double ARROW_RESERVE = 42.0;

    private ExplorerChoiceBoxEllipsisFix() { }

    public static void install(Node root) {
        if (root == null) return;
        visit(root);
    }

    private static void visit(Node node) {
        if (node == null) return;

        if (node instanceof ChoiceBox<?> choice && insideOverviewChart(choice)) {
            installChoice(choice);
            return;
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) visit(child);
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

    private static void installChoice(ChoiceBox<?> choice) {
        if (Boolean.TRUE.equals(choice.getProperties().get(INSTALLED))) {
            schedule(choice);
            return;
        }
        choice.getProperties().put(INSTALLED, Boolean.TRUE);

        choice.skinProperty().addListener((obs, oldSkin, newSkin) -> schedule(choice));
        choice.widthProperty().addListener((obs, oldWidth, newWidth) -> schedule(choice));
        choice.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateTooltip(choice);
            schedule(choice);
        });

        updateTooltip(choice);
        schedule(choice);
    }

    private static void schedule(ChoiceBox<?> choice) {
        Platform.runLater(() -> {
            apply(choice);
            Platform.runLater(() -> apply(choice));
        });
    }

    private static void apply(ChoiceBox<?> choice) {
        if (choice == null || choice.getScene() == null) return;
        try {
            choice.applyCss();
            Node labelNode = choice.lookup(".label");
            if (!(labelNode instanceof Label label)) return;

            double available = Math.max(24.0, choice.getWidth() - ARROW_RESERVE);
            label.setMinWidth(0);
            label.setPrefWidth(available);
            label.setMaxWidth(available);
            label.setWrapText(false);
            label.setTextOverrun(OverrunStyle.ELLIPSIS);
            label.setEllipsisString("...");
            label.setClip(null);
            label.requestLayout();
            choice.requestLayout();
        } catch (RuntimeException ignored) {
            // A ChoiceBox can briefly be between skins during the first pulse.
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
}
