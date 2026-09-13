package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Incremental version of the final scrollbar/table polish.
 * It never rescans a whole subtree when virtualized list/table children change.
 */
public final class FinalRequestedUiFastFixes {
    private static final String WATCHED = FinalRequestedUiFastFixes.class.getName() + ".watched";
    private static final String BAR_DONE = FinalRequestedUiFastFixes.class.getName() + ".barDone";
    private static final String BAR_SYNC = FinalRequestedUiFastFixes.class.getName() + ".barSync";
    private static final String INTERNAL_WATCHED = FinalRequestedUiFastFixes.class.getName() + ".internalWatched";
    private static final String INCLUDED_DONE = FinalRequestedUiFastFixes.class.getName() + ".includedDone";
    private static final String INTERNAL_BAR_KEY = UiTableAndStartupFixes.class.getName() + ".internalBar";
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());

    private static final double BAR_WIDTH = 9.0;
    private static final double MIN_THUMB_PIXELS = 46.0;
    private static final double MAX_MIN_THUMB_PIXELS = 92.0;

    private FinalRequestedUiFastFixes() { }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        observeScene(root);
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
            if (newRoot != null) Platform.runLater(() -> watch(newRoot));
        });
        if (scene.getRoot() != null) watch(scene.getRoot());
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
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void enhance(Node node) {
        if (node instanceof ScrollBar bar) prepareScrollBar(bar);
        if (node instanceof TableView<?> table && isPopulationResultTable(table)) prepareIncludedTable(table);
    }

    private static void prepareScrollBar(ScrollBar bar) {
        if (bar == null || bar.getOrientation() != Orientation.VERTICAL) return;
        boolean externalTableBar = bar.getStyleClass().contains("table-external-scrollbar");
        boolean listBar = ancestor(bar, ListView.class) != null;
        if (!externalTableBar && !listBar) return;

        bar.setMinWidth(BAR_WIDTH);
        bar.setPrefWidth(BAR_WIDTH);
        bar.setMaxWidth(BAR_WIDTH);
        if (!Boolean.TRUE.equals(bar.getProperties().get(BAR_DONE))) {
            bar.getProperties().put(BAR_DONE, Boolean.TRUE);
            bar.heightProperty().addListener((obs, oldValue, newValue) -> scheduleBarPolish(bar));
            bar.visibleAmountProperty().addListener((obs, oldValue, newValue) -> scheduleBarPolish(bar));
            bar.minProperty().addListener((obs, oldValue, newValue) -> scheduleBarPolish(bar));
            bar.maxProperty().addListener((obs, oldValue, newValue) -> scheduleBarPolish(bar));
            bar.skinProperty().addListener((obs, oldSkin, newSkin) -> scheduleBarPolish(bar));
        }
        scheduleBarPolish(bar);
    }

    private static void scheduleBarPolish(ScrollBar bar) {
        Platform.runLater(() -> polishScrollBar(bar));
    }

    private static void polishScrollBar(ScrollBar bar) {
        if (bar == null || Boolean.TRUE.equals(bar.getProperties().get(BAR_SYNC))) return;
        bar.getProperties().put(BAR_SYNC, Boolean.TRUE);
        try {
            bar.setMinWidth(BAR_WIDTH);
            bar.setPrefWidth(BAR_WIDTH);
            bar.setMaxWidth(BAR_WIDTH);

            double rawVisible = bar.getVisibleAmount();
            if (bar.getStyleClass().contains("table-external-scrollbar")) {
                Object internalObject = bar.getProperties().get(INTERNAL_BAR_KEY);
                if (internalObject instanceof ScrollBar internal) {
                    rawVisible = internal.getVisibleAmount();
                    installInternalWatch(internal, bar);
                }
                if (bar.visibleAmountProperty().isBound()) bar.visibleAmountProperty().unbind();
            } else if (bar.visibleAmountProperty().isBound()) {
                return;
            }

            double floor = minimumVisibleAmount(bar);
            if (Double.isFinite(floor) && floor > 0 && rawVisible < floor) {
                bar.setVisibleAmount(floor);
            } else if (Double.isFinite(rawVisible) && rawVisible >= 0
                    && Math.abs(bar.getVisibleAmount() - rawVisible) > 1e-8) {
                bar.setVisibleAmount(rawVisible);
            }

            try {
                bar.applyCss();
            } catch (RuntimeException ignored) {
                return;
            }
            Node thumb = bar.lookup(".thumb");
            if (thumb instanceof javafx.scene.layout.Region region) {
                region.setMinHeight(targetThumbPixels(bar));
                region.setPrefWidth(BAR_WIDTH);
                region.setMinWidth(BAR_WIDTH);
            }
            bar.requestLayout();
        } finally {
            bar.getProperties().remove(BAR_SYNC);
        }
    }

    private static void installInternalWatch(ScrollBar internal, ScrollBar external) {
        if (Boolean.TRUE.equals(internal.getProperties().get(INTERNAL_WATCHED))) return;
        internal.getProperties().put(INTERNAL_WATCHED, Boolean.TRUE);
        internal.visibleAmountProperty().addListener((obs, oldValue, newValue) -> scheduleBarPolish(external));
        internal.minProperty().addListener((obs, oldValue, newValue) -> scheduleBarPolish(external));
        internal.maxProperty().addListener((obs, oldValue, newValue) -> scheduleBarPolish(external));
    }

    private static double minimumVisibleAmount(ScrollBar bar) {
        double range = bar.getMax() - bar.getMin();
        if (!Double.isFinite(range) || range <= 0) return 0;
        double track = Math.max(80.0, bar.getHeight());
        double fraction = Math.min(0.45, targetThumbPixels(bar) / track);
        if (fraction <= 0 || fraction >= 1) return 0;
        return range * fraction / (1.0 - fraction);
    }

    private static double targetThumbPixels(ScrollBar bar) {
        double track = Math.max(80.0, bar.getHeight());
        return Math.max(MIN_THUMB_PIXELS, Math.min(MAX_MIN_THUMB_PIXELS, track * 0.12));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void prepareIncludedTable(TableView<?> rawTable) {
        if (Boolean.TRUE.equals(rawTable.getProperties().get(INCLUDED_DONE))) return;
        rawTable.getProperties().put(INCLUDED_DONE, Boolean.TRUE);
        Platform.runLater(() -> {
            TableView table = rawTable;
            VBox container = nearestVBox(table);
            if (container == null) {
                rawTable.getProperties().remove(INCLUDED_DONE);
                return;
            }

            Button export = findExcelExport(container);
            if (export == null) {
                export = UiFactory.button("", "ghost-button");
                I18n.setText(export, "Esporta Excel", "Export Excel");
                Button finalExport = export;
                export.setOnAction(event -> ExportSupport.exportTableExcel(
                        finalExport, table, "population_included_grbs.xlsx", "GRB inclusi"));
            }

            List<FlowPane> hiddenBars = descendantsWithStyle(container, FlowPane.class, "hidden-column-bar");
            Node oldDirectChild = directChildUnder(container, export);
            detach(export);
            for (FlowPane hiddenBar : hiddenBars) {
                // Reinsert even direct children so restored-column chips always
                // follow Export Excel instead of retaining their old position.
                detach(hiddenBar);
            }
            if (oldDirectChild != null && oldDirectChild != table && oldDirectChild != table.getParent()) {
                container.getChildren().remove(oldDirectChild);
            }

            HBox toolbar = new HBox(8, export);
            toolbar.setAlignment(Pos.CENTER_LEFT);
            toolbar.setMinWidth(0);
            toolbar.setMaxWidth(Double.MAX_VALUE);
            toolbar.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0 0 4 0;");
            toolbar.getStyleClass().add("included-export-toolbar");

            int tableIndex = directTableIndex(container, table);
            int insertAt = tableIndex < 0 ? 0 : tableIndex;
            container.getChildren().add(Math.max(0, Math.min(insertAt, container.getChildren().size())), toolbar);
            int hiddenInsert = container.getChildren().indexOf(toolbar) + 1;
            for (FlowPane hiddenBar : hiddenBars) {
                if (!container.getChildren().contains(hiddenBar)) {
                    container.getChildren().add(Math.min(hiddenInsert++, container.getChildren().size()), hiddenBar);
                }
            }
            ExplorerRegressionFixes.installManagedTable(table);
        });
    }

    private static VBox nearestVBox(Node node) {
        Node current = node;
        while (current != null) {
            Parent parent = current.getParent();
            if (parent instanceof VBox box) return box;
            current = parent;
        }
        return null;
    }

    private static int directTableIndex(VBox container, TableView<?> table) {
        Node direct = directChildUnder(container, table);
        return direct == null ? -1 : container.getChildren().indexOf(direct);
    }

    private static Node directChildUnder(Parent boundary, Node descendant) {
        if (boundary == null || descendant == null) return null;
        Node current = descendant;
        while (current != null && current.getParent() != boundary) current = current.getParent();
        return current != null && current.getParent() == boundary ? current : null;
    }

    private static void detach(Node node) {
        if (node == null) return;
        Parent parent = node.getParent();
        if (parent instanceof Pane pane) pane.getChildren().remove(node);
    }

    private static Button findExcelExport(Node node) {
        if (node instanceof Button button) {
            String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
            if (text.contains("esporta excel") || text.contains("export excel")) return button;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                Button found = findExcelExport(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean isPopulationResultTable(TableView<?> table) {
        if (table == null || table.getColumns().size() < 5) return false;
        boolean grb = false;
        boolean t90 = false;
        boolean redshift = false;
        boolean quality = false;
        for (TableColumn<?, ?> column : table.getColumns()) {
            String text = columnDisplayText(column).toLowerCase(Locale.ROOT);
            grb |= text.equals("grb");
            t90 |= text.contains("t90");
            redshift |= text.contains("redshift");
            quality |= text.contains("qualità") || text.contains("quality") || text.contains("flag");
        }
        return grb && t90 && redshift && quality;
    }

    private static String columnDisplayText(TableColumn<?, ?> column) {
        if (column == null) return "";
        String text = column.getText();
        if (text != null && !text.isBlank()) return text.trim();
        if (column.getGraphic() instanceof Parent parent) {
            Label label = firstLabel(parent);
            if (label != null && label.getText() != null) return label.getText().trim();
        } else if (column.getGraphic() instanceof Label label && label.getText() != null) {
            return label.getText().trim();
        }
        return "";
    }

    private static Label firstLabel(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Label label) return label;
            if (child instanceof Parent nested) {
                Label found = firstLabel(nested);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Node> T ancestor(Node node, Class<T> type) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (type.isInstance(current)) return type.cast(current);
            current = current.getParent();
        }
        return null;
    }

    private static <T extends Parent> List<T> descendantsWithStyle(Parent root, Class<T> type, String styleClass) {
        List<T> result = new ArrayList<>();
        collectWithStyle(root, type, styleClass, result);
        return result;
    }

    private static <T extends Parent> void collectWithStyle(Parent root, Class<T> type,
                                                             String styleClass, List<T> result) {
        for (Node child : List.copyOf(root.getChildrenUnmodifiable())) {
            if (type.isInstance(child) && child.getStyleClass().contains(styleClass)) result.add(type.cast(child));
            if (child instanceof Parent parent) collectWithStyle(parent, type, styleClass, result);
        }
    }
}
