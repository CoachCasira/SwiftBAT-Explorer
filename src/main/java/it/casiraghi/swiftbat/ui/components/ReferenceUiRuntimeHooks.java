package it.casiraghi.swiftbat.ui.components;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Runtime hooks that belong specifically to the reference redesign shell.
 * They operate only on stable style classes and never touch scientific data.
 */
final class ReferenceUiRuntimeHooks {
    private static final String WATCHED = ReferenceUiRuntimeHooks.class.getName() + ".watched";
    private static final String POPULATION_CARD_DONE = ReferenceUiRuntimeHooks.class.getName() + ".populationCardDone";
    private static final Set<Scene> INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());

    private ReferenceUiRuntimeHooks() { }

    static void install(Scene scene) {
        if (scene == null) return;
        synchronized (INSTALLED) {
            if (!INSTALLED.add(scene)) return;
        }
        Platform.runLater(() -> {
            Parent root = scene.getRoot();
            if (root != null) watch(root);
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
                if (change.wasAdded()) {
                    for (Node added : change.getAddedSubList()) watch(added);
                }
            }
        });
        for (Node child : parent.getChildrenUnmodifiable()) watch(child);
    }

    private static void enhance(Node node) {
        if (!(node instanceof VBox card)
                || !card.getStyleClass().contains("population-histogram-card")
                || Boolean.TRUE.equals(card.getProperties().get(POPULATION_CARD_DONE))) {
            return;
        }
        card.getProperties().put(POPULATION_CARD_DONE, Boolean.TRUE);

        // Equal card and chart geometry across FRACEXP, T90 and redshift.
        card.setMinHeight(438);
        card.setPrefHeight(438);
        card.setMaxHeight(438);
        card.setPickOnBounds(true);

        BarChart<?, ?> chart = findBarChart(card);
        if (chart != null) {
            chart.setMinHeight(326);
            chart.setPrefHeight(326);
            chart.setMaxHeight(326);
            VBox.setVgrow(chart, Priority.NEVER);

            // FRACEXP used to consume extra vertical space because JavaFX rotated
            // its ten interval labels automatically. Keep every histogram on the
            // same baseline by forcing compact horizontal labels.
            if (chart.getXAxis() instanceof CategoryAxis axis) {
                axis.setTickLabelRotation(0);
                axis.setTickLabelGap(3);
                axis.setTickLabelFont(Font.font(9.0));
            }
        }

        Button threeD = find3DButton(card);
        if (threeD == null) return;
        card.setCursor(Cursor.HAND);
        card.getStyleClass().add("population-histogram-card-clickable");
        card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (threeD.isDisabled() || isDescendantOf(event.getTarget(), threeD)) return;
            threeD.fire();
            event.consume();
        });
    }

    private static BarChart<?, ?> findBarChart(Node node) {
        if (node instanceof BarChart<?, ?> chart) return chart;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                BarChart<?, ?> chart = findBarChart(child);
                if (chart != null) return chart;
            }
        }
        return null;
    }

    private static Button find3DButton(Node node) {
        if (node instanceof Button button) {
            String text = button.getText();
            if (text != null && text.toLowerCase().contains("3d")) return button;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button button = find3DButton(child);
                if (button != null) return button;
            }
        }
        return null;
    }

    private static boolean isDescendantOf(Object target, Node ancestor) {
        if (!(target instanceof Node node)) return false;
        Node current = node;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.getParent();
        }
        return false;
    }
}
