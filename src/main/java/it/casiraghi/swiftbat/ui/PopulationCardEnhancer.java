package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Piccolo affinamento della sezione "Distribuzioni del campione".
 *
 * Mantiene le tre card perfettamente allineate e rende tutta la superficie
 * del grafico cliccabile per aprire la vista 3D, lasciando comunque disponibile
 * il pulsante dedicato.
 */
final class PopulationCardEnhancer {
    private static final String INSTALLED = PopulationCardEnhancer.class.getName() + ".installed";

    private PopulationCardEnhancer() { }

    static void install(PopulationPage page) {
        if (page == null) return;
        Platform.runLater(() -> enhance(page));
    }

    private static void enhance(PopulationPage page) {
        page.applyCss();
        List<VBox> cards = new ArrayList<>();
        collectCards(page, cards);
        if (cards.isEmpty()) return;

        // Tutte le card condividono la stessa geometria: titolo, toolbar e area grafico.
        // Questo evita che FRACEXP risulti visivamente più basso degli altri istogrammi.
        for (VBox card : cards) {
            card.setMinHeight(420);
            card.setPrefHeight(420);
            card.setMaxHeight(Double.MAX_VALUE);

            BarChart<?, ?> chart = findBarChart(card);
            if (chart != null) {
                chart.setMinHeight(315);
                chart.setPrefHeight(315);
                chart.setMaxHeight(315);
                VBox.setVgrow(chart, javafx.scene.layout.Priority.NEVER);
            }

            Button threeD = find3DButton(card);
            if (threeD == null || Boolean.TRUE.equals(card.getProperties().get(INSTALLED))) continue;
            card.getProperties().put(INSTALLED, Boolean.TRUE);
            card.setCursor(Cursor.HAND);
            card.getStyleClass().add("population-histogram-card-clickable");
            card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if (threeD.isDisabled() || clickedInsideButton(event, threeD)) return;
                threeD.fire();
                event.consume();
            });
        }
    }

    private static void collectCards(Node node, List<VBox> result) {
        if (node instanceof VBox box && box.getStyleClass().contains("population-histogram-card")) {
            result.add(box);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectCards(child, result);
        }
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
            if (text != null && (text.contains("3D") || text.toLowerCase().contains("vista 3d"))) return button;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button button = find3DButton(child);
                if (button != null) return button;
            }
        }
        return null;
    }

    private static boolean clickedInsideButton(MouseEvent event, Button button) {
        if (!(event.getTarget() instanceof Node target)) return false;
        Node current = target;
        while (current != null) {
            if (current == button) return true;
            current = current.getParent();
        }
        return false;
    }
}
