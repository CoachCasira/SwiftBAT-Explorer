package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Small, targeted layout corrections requested after the macOS regression pass.
 *
 * <p>This enhancer is intentionally incremental: every node is visited once when
 * it enters the scene graph and child mutations only re-check the direct parent.
 * It never performs whole-page rescans, so it is safe for the large virtualized
 * Explorer catalogue and JavaFX charts.</p>
 */
public final class TargetedLayoutPolish {
    private static final String WATCHED = TargetedLayoutPolish.class.getName() + ".watched";
    private static final String FULLSCREEN_SPACER = TargetedLayoutPolish.class.getName() + ".fullscreenSpacer";
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());

    private TargetedLayoutPolish() { }

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
            // Re-check only the mutated container. This is needed when the
            // Population fullscreen toolbar receives lock/export controls later.
            enhance(parent);
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void enhance(Node node) {
        if (node instanceof CheckBox check) polishExplorerMovingAverage(check);
        if (node instanceof VBox box && box.getStyleClass().contains("population-filter-card")) {
            polishPopulationFilters(box);
        }
        if (node instanceof HBox row) arrangePopulationFullscreenToolbar(row);
        if (node instanceof TableView<?> table && isMetadataTable(table)) polishMetadataTable(table);
        if (node instanceof BorderPane pane) polishMetadataSplit(pane);
    }

    /* ---------------- Explorer 2D controls ---------------- */

    private static void polishExplorerMovingAverage(CheckBox check) {
        String text = check.getText() == null ? "" : check.getText().toLowerCase(Locale.ROOT);
        if (!text.contains("5-bin") && !text.contains("media mobile")) return;
        check.setMinWidth(160);
        check.setPrefWidth(178);
        check.setMaxWidth(190);
        check.setTextOverrun(OverrunStyle.CLIP);
        check.setWrapText(false);
        HBox.setHgrow(check, Priority.NEVER);
    }

    /* ---------------- Population filter row ---------------- */

    private static void polishPopulationFilters(VBox card) {
        HBox row = null;
        for (Node child : card.getChildren()) {
            if (child instanceof HBox candidate && containsPopulationFilter(candidate)) {
                row = candidate;
                break;
            }
        }
        if (row == null) return;

        row.setSpacing(8);
        row.setAlignment(Pos.TOP_LEFT);

        for (Node child : row.getChildren()) {
            if (child instanceof VBox exposure && exposure.getStyleClass().contains("population-fracexp-inline")) {
                polishFracexp(exposure);
                continue;
            }
            if (!(child instanceof VBox group) || !group.getStyleClass().contains("population-filter-group")) continue;
            String caption = firstLabelText(group).toLowerCase(Locale.ROOT);
            if (caption.contains("t90") || caption.contains("durata")) {
                setFixedWidth(group, 130);
            } else if (caption.equals("redshift")) {
                setFixedWidth(group, 195);
                polishCompactRadios(group, 60);
            } else if (caption.contains("time window") || caption.contains("finestra temporale")) {
                setFixedWidth(group, 210);
                polishCompactRadios(group, 65);
            } else if (caption.contains("maximum sample") || caption.contains("campione massimo")) {
                setFixedWidth(group, 105);
            }
        }
    }

    private static boolean containsPopulationFilter(HBox row) {
        for (Node child : row.getChildren()) {
            if (child.getStyleClass().contains("population-filter-group")
                    || child.getStyleClass().contains("population-fracexp-inline")) return true;
        }
        return false;
    }

    private static void setFixedWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
        if (region instanceof VBox box && box.getChildren().size() > 1
                && box.getChildren().get(1) instanceof Region control) {
            control.setMinWidth(width);
            control.setPrefWidth(width);
            control.setMaxWidth(width);
        }
    }

    private static void polishCompactRadios(Parent group, double radioWidth) {
        HBox radios = findHBoxWithStyle(group, "compact-radio-group");
        if (radios == null) return;
        radios.setSpacing(5);
        radios.setMinWidth(0);
        radios.setMaxWidth(Double.MAX_VALUE);
        for (Node child : radios.getChildren()) {
            if (!(child instanceof RadioButton radio)) continue;
            radio.setMinWidth(radioWidth);
            radio.setPrefWidth(radioWidth);
            radio.setMaxWidth(radioWidth);
            radio.setTextOverrun(OverrunStyle.CLIP);
            radio.setWrapText(false);
        }
    }

    private static void polishFracexp(VBox exposure) {
        setFixedWidth(exposure, 356);
        for (Node node : exposure.getChildren()) {
            if (!(node instanceof HBox controls)) continue;
            boolean percentageRow = controls.getChildren().stream()
                    .anyMatch(child -> child.getStyleClass().contains("percentage-control"));
            if (!percentageRow) continue;
            controls.setSpacing(8);
            controls.setMinWidth(356);
            controls.setPrefWidth(356);
            controls.setMaxWidth(356);
            for (Node child : controls.getChildren()) {
                if (child instanceof VBox percentage && child.getStyleClass().contains("percentage-control")) {
                    polishPercentageControl(percentage);
                }
            }
        }
    }

    private static void polishPercentageControl(VBox control) {
        control.setMinWidth(174);
        control.setPrefWidth(174);
        control.setMaxWidth(174);
        HBox.setHgrow(control, Priority.NEVER);

        for (Node child : control.getChildren()) {
            if (!(child instanceof HBox heading)) continue;
            heading.setSpacing(3);
            heading.setMinWidth(0);
            heading.setMaxWidth(Double.MAX_VALUE);
            for (Node item : heading.getChildren()) {
                if (item instanceof Label label && label.getStyleClass().contains("filter-label")) {
                    label.setMinWidth(68);
                    label.setPrefWidth(74);
                    label.setMaxWidth(78);
                    label.setTextOverrun(OverrunStyle.CLIP);
                    label.setWrapText(false);
                } else if (item instanceof HBox valueBox) {
                    valueBox.setSpacing(3);
                    valueBox.setMinWidth(90);
                    valueBox.setPrefWidth(94);
                    valueBox.setMaxWidth(98);
                    for (Node valueItem : valueBox.getChildren()) {
                        if (valueItem instanceof TextField field
                                && field.getStyleClass().contains("percentage-field")) {
                            field.setMinWidth(42);
                            field.setPrefWidth(46);
                            field.setMaxWidth(48);
                        } else if (valueItem instanceof Button button
                                && button.getStyleClass().contains("filter-reset-button")) {
                            button.setMinWidth(32);
                            button.setPrefWidth(32);
                            button.setMaxWidth(32);
                        }
                    }
                }
            }
        }
    }

    /* ---------------- Population fullscreen action row ---------------- */

    private static void arrangePopulationFullscreenToolbar(HBox row) {
        if (!hasAncestorStyle(row, "in-place-fullscreen")) return;

        ToggleButton help = null;
        ToggleButton lock = null;
        Button export = null;
        int buttonCount = 0;
        for (Node child : row.getChildren()) {
            if (child instanceof ButtonBase) buttonCount++;
            if (child instanceof ToggleButton toggle && toggle.getStyleClass().contains("help-toggle")) {
                help = toggle;
            } else if (child instanceof ToggleButton toggle
                    && toggle.getStyleClass().contains("population-curve-lock")) {
                lock = toggle;
            } else if (child instanceof Button button) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase(Locale.ROOT);
                if (text.contains("export png") || text.contains("esporta png")) export = button;
            }
        }
        if (help == null || lock == null || export == null || buttonCount != 3) return;

        Region spacer = null;
        for (Node child : row.getChildren()) {
            if (child instanceof Region region && !(child instanceof ButtonBase)) {
                spacer = region;
                break;
            }
        }
        if (spacer == null) {
            spacer = new Region();
            spacer.getProperties().put(FULLSCREEN_SPACER, Boolean.TRUE);
        }
        HBox.setHgrow(spacer, Priority.ALWAYS);

        setToolbarButtonGeometry(export, 112, 42);
        setToolbarButtonGeometry(help, 142, 42);
        setToolbarButtonGeometry(lock, 44, 42);
        row.setSpacing(10);
        row.setAlignment(Pos.CENTER_LEFT);

        List<Node> ordered = List.of(export, lock, spacer, help);
        if (!row.getChildren().equals(ordered)) row.getChildren().setAll(ordered);
    }

    private static void setToolbarButtonGeometry(ButtonBase button, double width, double height) {
        button.setMinWidth(width);
        button.setPrefWidth(width);
        button.setMaxWidth(width);
        button.setMinHeight(height);
        button.setPrefHeight(height);
        button.setMaxHeight(height);
    }

    /* ---------------- Explorer metadata table + explanation ---------------- */

    private static boolean isMetadataTable(TableView<?> table) {
        if (table == null || table.getColumns().size() != 4) return false;
        boolean hdu = false;
        boolean keyword = false;
        boolean comment = false;
        for (TableColumn<?, ?> column : table.getColumns()) {
            String title = columnTitle(column).toLowerCase(Locale.ROOT);
            hdu |= title.equals("hdu");
            keyword |= title.contains("keyword");
            comment |= title.contains("comment") || title.contains("commento");
        }
        return hdu && keyword && comment;
    }

    private static void polishMetadataTable(TableView<?> table) {
        table.setMinWidth(0);
        for (TableColumn<?, ?> column : table.getColumns()) {
            String title = columnTitle(column).toLowerCase(Locale.ROOT);
            if (title.equals("hdu")) {
                column.setMinWidth(68);
                column.setPrefWidth(88);
            } else if (title.contains("keyword")) {
                column.setMinWidth(100);
                column.setPrefWidth(135);
            } else if (title.equals("value") || title.equals("valore")) {
                column.setMinWidth(140);
                column.setPrefWidth(190);
            } else if (title.contains("comment") || title.contains("commento")) {
                column.setMinWidth(205);
                column.setPrefWidth(330);
            }
        }
    }

    private static void polishMetadataSplit(BorderPane pane) {
        Node right = pane.getRight();
        if (!(right instanceof ScrollPane sideScroll)) return;
        TableView<?> table = findMetadataTable(pane.getCenter());
        if (table == null) return;

        polishMetadataTable(table);
        if (pane.getCenter() instanceof Region center) {
            center.setMinWidth(0);
            center.setMaxWidth(Double.MAX_VALUE);
        }

        sideScroll.setMinWidth(270);
        sideScroll.setPrefWidth(300);
        sideScroll.setMaxWidth(320);
        sideScroll.setFitToWidth(true);
        Node content = sideScroll.getContent();
        if (content instanceof Region side) {
            side.setMinWidth(250);
            side.setPrefWidth(285);
            side.setMaxWidth(305);
        }
        if (content instanceof Parent parent) compactMetadataInfoRows(parent);
        pane.requestLayout();
    }

    private static void compactMetadataInfoRows(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof HBox row && row.getChildren().size() >= 2
                    && row.getChildren().get(0) instanceof Label key
                    && key.getStyleClass().contains("info-key")) {
                row.setSpacing(8);
                key.setMinWidth(92);
                key.setPrefWidth(100);
                key.setMaxWidth(106);
                Node valueNode = row.getChildren().get(1);
                if (valueNode instanceof Label value) {
                    value.setMinWidth(0);
                    value.setMaxWidth(Double.MAX_VALUE);
                    value.setWrapText(true);
                    HBox.setHgrow(value, Priority.ALWAYS);
                }
            }
            if (child instanceof Parent nested) compactMetadataInfoRows(nested);
        }
    }

    private static TableView<?> findMetadataTable(Node node) {
        if (node instanceof TableView<?> table && isMetadataTable(table)) return table;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TableView<?> found = findMetadataTable(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static HBox findHBoxWithStyle(Parent root, String styleClass) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof HBox box && box.getStyleClass().contains(styleClass)) return box;
            if (child instanceof Parent nested) {
                HBox found = findHBoxWithStyle(nested, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean hasAncestorStyle(Node node, String styleClass) {
        Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains(styleClass)) return true;
            current = current.getParent();
        }
        return false;
    }

    private static String firstLabelText(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Label label && label.getText() != null && !label.getText().isBlank()) {
                return label.getText().trim();
            }
        }
        return "";
    }

    private static String columnTitle(TableColumn<?, ?> column) {
        if (column == null) return "";
        if (column.getText() != null && !column.getText().isBlank()) return column.getText().trim();
        if (column.getGraphic() instanceof Label label && label.getText() != null) return label.getText().trim();
        if (column.getGraphic() instanceof Parent parent) return firstLabelText(parent);
        return "";
    }
}
