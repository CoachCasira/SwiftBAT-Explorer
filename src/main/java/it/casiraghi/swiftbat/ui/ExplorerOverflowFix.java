package it.casiraghi.swiftbat.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Final compact geometry for the Explorer 2D toolbar on laptop-sized windows. */
public final class ExplorerOverflowFix {
    private static final String WIDTH_LISTENER = ExplorerOverflowFix.class.getName() + ".clipListener";

    private ExplorerOverflowFix() { }

    public static void apply(Node root) {
        if (root == null) return;
        VBox chartCard = find(root, VBox.class, "overview-chart-card");
        if (chartCard == null) return;

        chartCard.setMinWidth(0);
        BorderPane overview = nearestBorderPane(chartCard);
        if (overview != null && overview.getRight() instanceof VBox right) {
            right.setMinWidth(180);
            right.setPrefWidth(188);
            right.setMaxWidth(198);
            BorderPane.setMargin(right, new Insets(0, 0, 0, 7));
            compactActions(right);
        }

        HBox controls = directControlRow(chartCard);
        if (controls == null) return;
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setSpacing(5);
        controls.setMinWidth(0);
        controls.setPrefWidth(Region.USE_COMPUTED_SIZE);
        controls.setMaxWidth(Double.MAX_VALUE);

        List<ChoiceBox<?>> choices = new ArrayList<>();
        for (Node child : controls.getChildren()) {
            if (child instanceof ChoiceBox<?> choice) choices.add(choice);
        }
        if (!choices.isEmpty()) width(choices.get(0), 96, 124, 140);
        if (choices.size() > 1) width(choices.get(1), 100, 128, 144);

        CheckBox smooth = controls.getChildren().stream()
                .filter(CheckBox.class::isInstance)
                .map(CheckBox.class::cast)
                .findFirst().orElse(null);
        Label trigger = controls.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> {
                    String t = safe(label.getText()).toLowerCase(Locale.ROOT);
                    return t.contains("trigger") || t.contains("t = 0");
                })
                .findFirst().orElse(null);

        if (smooth != null) {
            I18n.setText(smooth, "Media mobile 5 bin…", "5-bin moving…");
            width(smooth, 72, 96, 104);
            smooth.setWrapText(false);
            smooth.setTextOverrun(OverrunStyle.ELLIPSIS);
            smooth.setEllipsisString("…");
            smooth.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                    "Media mobile su 5 bin", "5-bin moving average")));
        }

        // The original row contains an expanding spacer between moving average
        // and the trigger note. On narrow windows that spacer pushed the note
        // beyond the chart card. Collapse it: both labels now stay inside.
        if (smooth != null && trigger != null) {
            int smoothIndex = controls.getChildren().indexOf(smooth);
            int triggerIndex = controls.getChildren().indexOf(trigger);
            for (int i = smoothIndex + 1; i < triggerIndex; i++) {
                Node node = controls.getChildren().get(i);
                if (node instanceof Region spacer) {
                    HBox.setHgrow(spacer, Priority.NEVER);
                    spacer.setMinWidth(0);
                    spacer.setPrefWidth(2);
                    spacer.setMaxWidth(2);
                }
            }
        }

        if (trigger != null) {
            I18n.setText(trigger, "t = 0…", "t = 0…");
            width(trigger, 30, 36, 42);
            trigger.setWrapText(false);
            trigger.setTextOverrun(OverrunStyle.ELLIPSIS);
            trigger.setEllipsisString("…");
            trigger.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                    "t = 0 indica il trigger", "t = 0 marks the trigger")));
        }

        installHardClip(controls, chartCard);
        controls.requestLayout();
        chartCard.requestLayout();
        if (overview != null) overview.requestLayout();
    }

    private static void compactActions(VBox right) {
        VBox actionCard = find(right, VBox.class, "overview-action-card");
        if (actionCard == null) return;
        HBox row = findFirst(actionCard, HBox.class);
        if (row == null) return;
        row.setSpacing(5);
        row.setAlignment(Pos.CENTER_RIGHT);
        for (Node child : row.getChildren()) {
            if (!(child instanceof Button button)) continue;
            String text = safe(button.getText()).toLowerCase(Locale.ROOT);
            if (text.contains("export") || text.contains("esporta")) width(button, 64, 70, 76);
            else if (text.contains("fullscreen") || text.contains("schermo")) width(button, 78, 84, 90);
        }
    }

    private static void installHardClip(HBox controls, VBox chartCard) {
        if (controls.getClip() instanceof Rectangle clip) {
            updateClip(clip, controls, chartCard);
            return;
        }
        Rectangle clip = new Rectangle();
        controls.setClip(clip);
        updateClip(clip, controls, chartCard);
        if (!Boolean.TRUE.equals(controls.getProperties().get(WIDTH_LISTENER))) {
            controls.getProperties().put(WIDTH_LISTENER, Boolean.TRUE);
            controls.widthProperty().addListener((obs, oldValue, newValue) -> updateClip(clip, controls, chartCard));
            controls.heightProperty().addListener((obs, oldValue, newValue) -> updateClip(clip, controls, chartCard));
            chartCard.widthProperty().addListener((obs, oldValue, newValue) -> updateClip(clip, controls, chartCard));
        }
    }

    private static void updateClip(Rectangle clip, HBox controls, VBox chartCard) {
        double available = Math.max(0, Math.min(controls.getWidth(), chartCard.getWidth() - 24));
        clip.setWidth(available);
        clip.setHeight(Math.max(0, controls.getHeight()));
    }

    private static void width(Region region, double min, double pref, double max) {
        region.setMinWidth(min);
        region.setPrefWidth(pref);
        region.setMaxWidth(max);
        HBox.setHgrow(region, Priority.NEVER);
    }

    private static HBox directControlRow(VBox chartCard) {
        for (Node child : chartCard.getChildren()) {
            if (child instanceof HBox row
                    && row.getChildren().stream().anyMatch(CheckBox.class::isInstance)) return row;
        }
        return null;
    }

    private static BorderPane nearestBorderPane(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof BorderPane pane && pane.getCenter() != null
                    && isDescendant(pane.getCenter(), node)) return pane;
            current = current.getParent();
        }
        return null;
    }

    private static boolean isDescendant(Node ancestor, Node target) {
        if (ancestor == target) return true;
        if (ancestor instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (isDescendant(child, target)) return true;
            }
        }
        return false;
    }

    private static <T extends Node> T findFirst(Node root, Class<T> type) {
        if (root == null) return null;
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findFirst(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Node> T find(Node root, Class<T> type, String styleClass) {
        if (root == null) return null;
        if (type.isInstance(root) && root.getStyleClass().contains(styleClass)) return type.cast(root);
        if (root instanceof javafx.scene.control.ScrollPane scroll && scroll.getContent() != null) {
            T found = find(scroll.getContent(), type, styleClass);
            if (found != null) return found;
        }
        if (root instanceof javafx.scene.control.SplitPane split) {
            for (Node item : split.getItems()) {
                T found = find(item, type, styleClass);
                if (found != null) return found;
            }
        }
        if (root instanceof javafx.scene.control.TabPane tabs) {
            for (javafx.scene.control.Tab tab : tabs.getTabs()) {
                T found = find(tab.getContent(), type, styleClass);
                if (found != null) return found;
            }
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = find(child, type, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }
}
