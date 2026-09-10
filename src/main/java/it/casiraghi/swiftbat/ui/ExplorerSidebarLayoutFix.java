package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Final Explorer overview geometry.
 *
 * <p>The supporting cards always remain in the right column. On laptop-sized
 * workspaces the column becomes narrower instead of being moved below the chart.
 * The summary rows and chart-action buttons are compacted locally so the chart
 * keeps as much horizontal room as possible without clipping the side cards.</p>
 */
public final class ExplorerSidebarLayoutFix {
    private static final String WATCHED = ExplorerSidebarLayoutFix.class.getName() + ".watched";
    private static final String DONE = ExplorerSidebarLayoutFix.class.getName() + ".done";
    private static final String SIDEBAR = ExplorerSidebarLayoutFix.class.getName() + ".sidebar";
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());

    private ExplorerSidebarLayoutFix() { }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        observeScene(root);
        Platform.runLater(() -> {
            scan(root);
            Platform.runLater(() -> scan(root));
        });
    }

    private static void observeScene(Parent root) {
        if (root.getScene() != null) watchScene(root.getScene());
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) watchScene(newScene);
        });
    }

    private static void watchScene(Scene scene) {
        if (scene == null || !WATCHED_SCENES.add(scene)) return;
        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (newRoot == null) return;
            Platform.runLater(() -> {
                watch(newRoot);
                scan(newRoot);
            });
        });
        if (scene.getRoot() != null) Platform.runLater(() -> {
            watch(scene.getRoot());
            scan(scene.getRoot());
        });
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
            }
            Platform.runLater(() -> scan(parent));
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void scan(Node node) {
        if (node == null) return;
        enhance(node);
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) scan(child);
        }
    }

    private static void enhance(Node node) {
        if (node instanceof VBox chartCard && chartCard.getStyleClass().contains("overview-chart-card")) {
            install(chartCard);
        }
    }

    private static void install(VBox chartCard) {
        if (!(chartCard.getParent() instanceof BorderPane pane)) return;

        Node sidebar = sidebarFor(pane);
        if (sidebar == null) return;
        pane.getProperties().put(SIDEBAR, sidebar);

        if (!Boolean.TRUE.equals(pane.getProperties().get(DONE))) {
            pane.getProperties().put(DONE, Boolean.TRUE);
            pane.widthProperty().addListener((obs, oldWidth, newWidth) ->
                    Platform.runLater(() -> apply(pane, chartCard)));
            pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) Platform.runLater(() -> apply(pane, chartCard));
            });
        }

        chartCard.setMinWidth(0);
        chartCard.setMaxWidth(Double.MAX_VALUE);
        Platform.runLater(() -> apply(pane, chartCard));
    }

    private static Node sidebarFor(BorderPane pane) {
        Object stored = pane.getProperties().get(SIDEBAR);
        if (stored instanceof Node node) return node;
        if (pane.getRight() != null) return pane.getRight();
        return pane.getBottom();
    }

    private static void apply(BorderPane pane, VBox chartCard) {
        Node sidebar = sidebarFor(pane);
        if (sidebar == null) return;

        // Never stack these cards below the graph: reclaim space by narrowing them.
        if (pane.getBottom() == sidebar) pane.setBottom(null);
        if (pane.getRight() != sidebar) pane.setRight(sidebar);
        BorderPane.setMargin(sidebar, new Insets(0, 0, 0, 10));

        chartCard.setMinWidth(0);
        chartCard.setMaxWidth(Double.MAX_VALUE);

        double available = pane.getWidth();
        double sidebarWidth = available > 0 && available < 1030
                ? 238
                : available > 0 && available < 1220 ? 250 : 275;

        if (sidebar instanceof Region region) {
            region.setMinWidth(sidebarWidth);
            region.setPrefWidth(sidebarWidth);
            region.setMaxWidth(sidebarWidth);
        }
        if (sidebar instanceof VBox box) {
            box.setSpacing(9);
            box.setFillWidth(true);
            reorderCards(box);
            compactCards(box, sidebarWidth);
        }
        pane.requestLayout();
    }

    private static void reorderCards(VBox sidebar) {
        Node glance = null;
        Node trigger = null;
        Node actions = null;
        List<Node> extras = new ArrayList<>();

        for (Node child : List.copyOf(sidebar.getChildren())) {
            String text = descendantText(child).toLowerCase(Locale.ROOT);
            if (glance == null && (text.contains("at a glance") || text.contains("in breve"))) {
                glance = child;
            } else if (trigger == null && text.contains("trigger")) {
                trigger = child;
            } else if (actions == null && (text.contains("chart actions") || text.contains("azioni grafico"))) {
                actions = child;
            } else {
                extras.add(child);
            }
        }

        if (glance == null || trigger == null || actions == null) return;
        List<Node> ordered = new ArrayList<>();
        ordered.add(glance);
        ordered.add(trigger);
        ordered.add(actions);
        ordered.addAll(extras);
        if (!sidebar.getChildren().equals(ordered)) sidebar.getChildren().setAll(ordered);
    }

    private static void compactCards(VBox sidebar, double sidebarWidth) {
        for (Node child : sidebar.getChildren()) {
            if (!(child instanceof Region card)) continue;
            card.setMinWidth(0);
            card.setPrefWidth(sidebarWidth);
            card.setMaxWidth(sidebarWidth);
            if (child instanceof VBox box) {
                String text = descendantText(box).toLowerCase(Locale.ROOT);
                if (text.contains("at a glance") || text.contains("in breve")) {
                    box.setPadding(new Insets(14));
                    compactInfoRows(box);
                } else if (text.contains("trigger")) {
                    box.setPadding(new Insets(14));
                } else if (text.contains("chart actions") || text.contains("azioni grafico")) {
                    box.setPadding(new Insets(12));
                    compactActionButtons(box);
                }
            }
        }
    }

    private static void compactInfoRows(Node node) {
        if (node instanceof HBox row && row.getChildren().size() >= 2
                && row.getChildren().get(0) instanceof Label key
                && key.getStyleClass().contains("info-key")) {
            row.setSpacing(8);
            key.setMinWidth(86);
            key.setPrefWidth(92);
            key.setMaxWidth(98);
            Node valueNode = row.getChildren().get(1);
            if (valueNode instanceof Label value) {
                value.setMinWidth(0);
                value.setMaxWidth(Double.MAX_VALUE);
                value.setWrapText(true);
                HBox.setHgrow(value, Priority.ALWAYS);
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) compactInfoRows(child);
        }
    }

    private static void compactActionButtons(Node node) {
        if (node instanceof HBox row) {
            List<Button> buttons = row.getChildren().stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .toList();
            if (buttons.size() >= 2) {
                row.setSpacing(6);
                row.setAlignment(Pos.CENTER_RIGHT);
                for (Button button : buttons) {
                    button.setMinWidth(88);
                    button.setPrefWidth(102);
                    button.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(button, Priority.ALWAYS);
                }
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) compactActionButtons(child);
        }
    }

    private static String descendantText(Node node) {
        StringBuilder text = new StringBuilder();
        collectText(node, text);
        return text.toString();
    }

    private static void collectText(Node node, StringBuilder target) {
        if (node instanceof Label label && label.getText() != null) {
            target.append(' ').append(label.getText());
        } else if (node instanceof Button button && button.getText() != null) {
            target.append(' ').append(button.getText());
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectText(child, target);
        }
    }
}
