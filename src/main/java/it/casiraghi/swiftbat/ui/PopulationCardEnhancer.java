package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Rifiniture e comandi di esportazione della Population Analysis. */
final class PopulationCardEnhancer {
    private static final String INSTALLED = PopulationCardEnhancer.class.getName() + ".installed";
    private static final String EXPORT_INSTALLED = PopulationCardEnhancer.class.getName() + ".exportInstalled";
    private static final String PROFILE_EXPORT_INSTALLED = PopulationCardEnhancer.class.getName() + ".profileExportInstalled";
    private static final String TABLE_EXPORT_INSTALLED = PopulationCardEnhancer.class.getName() + ".tableExportInstalled";

    private PopulationCardEnhancer() { }

    static void install(PopulationPage page) {
        if (page == null) return;
        Platform.runLater(() -> {
            enhance(page);
            Platform.runLater(() -> enhance(page));
        });
    }

    private static void enhance(PopulationPage page) {
        page.applyCss();
        alignAndEnhanceHistogramCards(page);
        installProfileExport(page);
        installTableExport(page);
    }

    private static void alignAndEnhanceHistogramCards(PopulationPage page) {
        List<VBox> cards = new ArrayList<>();
        collectCards(page, cards);
        for (VBox card : cards) {
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
            }

            Button threeD = find3DButton(card);
            if (threeD == null) continue;

            if (!Boolean.TRUE.equals(card.getProperties().get(EXPORT_INSTALLED)) && chart != null) {
                card.getProperties().put(EXPORT_INSTALLED, Boolean.TRUE);
                Button export = exportPngButton();
                String filename = histogramFileName(chart);
                export.setOnAction(event -> {
                    event.consume();
                    ExportSupport.exportPng(page, chart, filename);
                });
                HBox toolbar = ancestorHBox(threeD);
                if (toolbar != null) {
                    int index = toolbar.getChildren().indexOf(threeD);
                    toolbar.getChildren().add(Math.max(0, index), export);
                }
            }

            if (Boolean.TRUE.equals(card.getProperties().get(INSTALLED))) continue;
            card.getProperties().put(INSTALLED, Boolean.TRUE);
            card.setCursor(Cursor.HAND);
            card.getStyleClass().add("population-histogram-card-clickable");
            card.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                if (threeD.isDisabled() || clickedInsideAnyButton(event.getTarget())) return;
                threeD.fire();
                event.consume();
            });
        }
    }

    private static void installProfileExport(PopulationPage page) {
        LineChart<?, ?> chart = findPopulationLineChart(page);
        if (chart == null || Boolean.TRUE.equals(chart.getProperties().get(PROFILE_EXPORT_INSTALLED))) return;
        Button threeD = find3DButtonInAncestor(chart);
        if (threeD == null) return;
        HBox toolbar = ancestorHBox(threeD);
        if (toolbar == null) return;
        chart.getProperties().put(PROFILE_EXPORT_INSTALLED, Boolean.TRUE);
        Button export = exportPngButton();
        export.setOnAction(event -> ExportSupport.exportPng(page, chart, "population_temporal_profile.png"));
        int index = toolbar.getChildren().indexOf(threeD);
        toolbar.getChildren().add(Math.max(0, index), export);
    }

    private static void installTableExport(PopulationPage page) {
        TableView<?> table = findTable(page);
        if (table == null || Boolean.TRUE.equals(table.getProperties().get(TABLE_EXPORT_INSTALLED))) return;
        if (!(table.getParent() instanceof VBox container)) return;
        table.getProperties().put(TABLE_EXPORT_INSTALLED, Boolean.TRUE);
        Button export = UiFactory.button("", "secondary-button");
        I18n.setText(export, "Esporta Excel", "Export Excel");
        export.setOnAction(event -> ExportSupport.exportTableExcel(
                page, table, "population_included_grbs.xlsx", "GRB_INCLUDED"));
        HBox actions = new HBox(export);
        actions.setAlignment(Pos.CENTER_RIGHT);
        container.getChildren().add(0, actions);
    }

    private static Button exportPngButton() {
        Button export = UiFactory.button("", "secondary-button");
        I18n.setText(export, "Esporta PNG", "Export PNG");
        export.setFocusTraversable(false);
        return export;
    }

    private static String histogramFileName(BarChart<?, ?> chart) {
        String title = chart.getTitle() == null ? "histogram" : chart.getTitle().toLowerCase(Locale.ROOT);
        if (title.contains("copertura") || title.contains("coverage")) return "population_fracexp_quality.png";
        if (title.contains("t90")) return "population_t90_duration.png";
        if (title.contains("redshift")) return "population_cosmological_distance.png";
        return "population_histogram.png";
    }

    private static void collectCards(Node node, List<VBox> result) {
        if (node instanceof VBox box && box.getStyleClass().contains("population-histogram-card")) result.add(box);
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

    private static LineChart<?, ?> findPopulationLineChart(Node node) {
        if (node instanceof LineChart<?, ?> chart && chart.getStyleClass().contains("population-chart")) return chart;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                LineChart<?, ?> chart = findPopulationLineChart(child);
                if (chart != null) return chart;
            }
        }
        return null;
    }

    private static TableView<?> findTable(Node node) {
        if (node instanceof TableView<?> table) return table;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TableView<?> table = findTable(child);
                if (table != null) return table;
            }
        }
        return null;
    }

    private static Button find3DButton(Node node) {
        if (node instanceof Button button) {
            String text = button.getText();
            if (text != null && text.toLowerCase(Locale.ROOT).contains("3d")) return button;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button button = find3DButton(child);
                if (button != null) return button;
            }
        }
        return null;
    }

    private static Button find3DButtonInAncestor(Node node) {
        Node current = node.getParent();
        while (current != null) {
            Button button = find3DButton(current);
            if (button != null) return button;
            current = current.getParent();
        }
        return null;
    }

    private static HBox ancestorHBox(Node node) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (current instanceof HBox row) return row;
            current = current.getParent();
        }
        return null;
    }

    private static boolean clickedInsideAnyButton(Object target) {
        if (!(target instanceof Node node)) return false;
        Node current = node;
        while (current != null) {
            if (current instanceof Button) return true;
            current = current.getParent();
        }
        return false;
    }
}
