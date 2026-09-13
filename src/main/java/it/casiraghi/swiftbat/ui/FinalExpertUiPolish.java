package it.casiraghi.swiftbat.ui;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Very small page-scoped finishing pass for the UI details requested during the
 * final macOS review. It does not install global scene-graph observers.
 */
public final class FinalExpertUiPolish {
    private static final String MANUAL_TAB = FinalExpertUiPolish.class.getName() + ".manualTab";
    private static final String CHIP_WATCH = FinalExpertUiPolish.class.getName() + ".chipWatch";
    private static final String MANUAL_SCROLL = FinalExpertUiPolish.class.getName() + ".manualScroll";
    private static final String EXPLORER_CLIP = FinalExpertUiPolish.class.getName() + ".explorerClip";

    private FinalExpertUiPolish() { }

    public static void polishExplorer(Node root) {
        if (root == null) return;
        VBox chartCard = findLogical(root, VBox.class, "overview-chart-card");
        if (chartCard != null) compactExplorerOverview(chartCard);
        restoreMetadataSearch(root);
    }

    public static void polishPopulation(VBox filterCard) {
        if (filterCard == null) return;
        centerTopPopulationGroups(filterCard);
        centerAdvancedRanges(filterCard);
        polishManualSelector(filterCard);
    }

    /* ---------------- Explorer ---------------- */

    private static void compactExplorerOverview(VBox chartCard) {
        if (ExplorerOverviewPane.owns(chartCard)) return;
        BorderPane overview = nearestBorderPane(chartCard);
        VBox right = overview != null && overview.getRight() instanceof VBox box ? box : null;
        if (right != null) {
            right.setMinWidth(204);
            right.setPrefWidth(212);
            right.setMaxWidth(220);
            BorderPane.setMargin(right, new Insets(0, 0, 0, 8));

            VBox actionCard = findLogical(right, VBox.class, "overview-action-card");
            if (actionCard != null) {
                HBox row = findFirst(actionCard, HBox.class);
                if (row != null) {
                    row.setSpacing(6);
                    row.setAlignment(Pos.CENTER_RIGHT);
                    for (Node child : row.getChildren()) {
                        if (!(child instanceof Button button)) continue;
                        String text = safe(button.getText()).toLowerCase(Locale.ROOT);
                        if (text.contains("export") || text.contains("esporta")) {
                            setWidth(button, 76, 80, 84);
                        } else if (text.contains("fullscreen") || text.contains("schermo")) {
                            setWidth(button, 86, 92, 98);
                        }
                    }
                }
            }
        }

        HBox controls = directHBoxWithCheckBox(chartCard);
        if (controls == null) return;
        controls.setSpacing(6);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setMaxWidth(Double.MAX_VALUE);

        // Hard safety boundary: even if a JavaFX skin temporarily computes a
        // wider text node, nothing from the chart toolbar can paint over the
        // right-side cards.
        if (!Boolean.TRUE.equals(controls.getProperties().get(EXPLORER_CLIP))) {
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(controls.widthProperty());
            clip.heightProperty().bind(controls.heightProperty());
            controls.setClip(clip);
            controls.getProperties().put(EXPLORER_CLIP, Boolean.TRUE);
        }

        List<javafx.scene.control.ChoiceBox<?>> choices = new ArrayList<>();
        for (Node child : controls.getChildren()) {
            if (child instanceof javafx.scene.control.ChoiceBox<?> choice) choices.add(choice);
        }
        if (!choices.isEmpty()) setWidth(choices.get(0), 144, 156, 166);
        if (choices.size() > 1) setWidth(choices.get(1), 140, 152, 162);

        CheckBox smooth = controls.getChildren().stream()
                .filter(CheckBox.class::isInstance)
                .map(CheckBox.class::cast)
                .findFirst().orElse(null);
        if (smooth != null) {
            I18n.setText(smooth, "Media mobile 5 bin…", "5-bin moving…");
            setWidth(smooth, 106, 116, 124);
            smooth.setWrapText(false);
            smooth.setTextOverrun(OverrunStyle.ELLIPSIS);
            smooth.setEllipsisString("…");
            smooth.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                    "Media mobile su 5 bin", "5-bin moving average")));
            HBox.setHgrow(smooth, Priority.NEVER);
        }

        Label trigger = controls.getChildren().stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> {
                    String text = safe(label.getText()).toLowerCase(Locale.ROOT);
                    return text.contains("trigger") || text.contains("t = 0");
                })
                .findFirst().orElse(null);
        if (trigger != null) {
            I18n.setText(trigger, "t = 0…", "t = 0…");
            setWidth(trigger, 42, 46, 50);
            trigger.setWrapText(false);
            trigger.setTextOverrun(OverrunStyle.ELLIPSIS);
            trigger.setEllipsisString("…");
            trigger.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                    "t = 0 indica il trigger", "t = 0 marks the trigger")));
        }

        controls.requestLayout();
        chartCard.requestLayout();
    }

    /** Reuses the already-built A-Z/search/grouped metadata selector. */
    private static void restoreMetadataSearch(Node root) {
        TabPane tabs = findLogical(root, TabPane.class, null);
        if (tabs == null) return;
        for (Tab tab : tabs.getTabs()) {
            String title = safe(tab.getText()).toLowerCase(Locale.ROOT);
            if (!title.contains("metadat")) continue;
            Node content = tab.getContent();
            if (content == null) continue;
            for (VBox box : findAllLogical(content, VBox.class)) {
                invokeMetadataEnhancer(box);
            }
        }
    }

    private static void invokeMetadataEnhancer(VBox box) {
        try {
            Method method = UiRefinements.class.getDeclaredMethod("enhanceMetadataFieldSelector", VBox.class);
            method.setAccessible(true);
            method.invoke(null, box);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // The plain selector remains usable if the compatibility helper changes.
        }
    }

    /* ---------------- Population ---------------- */

    private static void centerTopPopulationGroups(VBox card) {
        HBox row = findTopPopulationRow(card);
        if (row == null || row.getChildren().size() < 5) return;
        int[] centered = {0, 1, 2, 4};
        for (int index : centered) {
            Node node = row.getChildren().get(index);
            if (!(node instanceof VBox group)) continue;

            // Keep the dimensions chosen by the current responsive layout, but
            // center the whole filter content both horizontally and vertically.
            group.setAlignment(Pos.CENTER);
            group.setFillWidth(row instanceof ResponsiveRow);
            for (Node child : group.getChildren()) {
                if (child instanceof HBox childRow) childRow.setAlignment(Pos.CENTER);
            }

            HBox radios = findLogical(group, HBox.class, "compact-radio-group");
            if (radios != null) {
                radios.setAlignment(Pos.CENTER);
                radios.setMinWidth(Region.USE_PREF_SIZE);
                radios.setPrefWidth(Region.USE_COMPUTED_SIZE);
                radios.setMaxWidth(Region.USE_PREF_SIZE);
                for (Node child : radios.getChildren()) {
                    if (child instanceof RadioButton radio) {
                        HBox.setHgrow(radio, Priority.NEVER);
                        radio.setMinWidth(Region.USE_PREF_SIZE);
                        radio.setPrefWidth(Region.USE_COMPUTED_SIZE);
                        radio.setMaxWidth(Region.USE_PREF_SIZE);
                    }
                }
            }
        }
    }

    private static void centerAdvancedRanges(VBox card) {
        HBox advanced = findAdvancedRow(card);
        if (advanced == null) return;
        advanced.setAlignment(Pos.TOP_LEFT);
        for (Node child : advanced.getChildren()) {
            if (!(child instanceof VBox group)) continue;
            if (countStyled(group, "sky-range-field") < 2) continue;

            // Do not resize the boxes: only move their existing content to the
            // visual center requested for z, RA and DEC.
            group.setAlignment(Pos.CENTER);
            group.setFillWidth(advanced instanceof ResponsiveRow);
            for (Node nested : group.getChildren()) {
                if (nested instanceof HBox row) row.setAlignment(Pos.CENTER);
            }
        }
    }

    private static void polishManualSelector(VBox card) {
        VBox manual = findLogical(card, VBox.class, "population-manual-grb-v2");
        if (manual == null) return;

        // The manual selector must stay the same height even with many saved
        // bursts. Only the chip viewport scrolls vertically.
        manual.setMinHeight(205);
        manual.setPrefHeight(225);
        manual.setMaxHeight(225);
        manual.setFillWidth(true);
        manual.setAlignment(Pos.TOP_LEFT);

        FlowPane chips = findFirst(manual, FlowPane.class);
        if (chips != null) {
            chips.setAlignment(Pos.TOP_LEFT);
            chips.setPrefWrapLength(300);
            chips.setMinHeight(Region.USE_PREF_SIZE);
            chips.setPrefHeight(Region.USE_COMPUTED_SIZE);
            chips.setMaxHeight(Double.MAX_VALUE);
            chips.setPadding(new Insets(3, 4, 3, 2));

            for (Node child : List.copyOf(chips.getChildren())) polishChip(child);
            if (!Boolean.TRUE.equals(chips.getProperties().get(CHIP_WATCH))) {
                chips.getProperties().put(CHIP_WATCH, Boolean.TRUE);
                chips.getChildren().addListener((ListChangeListener<Node>) change -> {
                    while (change.next()) {
                        if (!change.wasAdded()) continue;
                        for (Node added : List.copyOf(change.getAddedSubList())) polishChip(added);
                    }
                });
            }

            if (!(chips.getParent() instanceof ScrollPane)
                    && !Boolean.TRUE.equals(manual.getProperties().get(MANUAL_SCROLL))) {
                int index = manual.getChildren().indexOf(chips);
                if (index >= 0) {
                    ScrollPane chipScroll = new ScrollPane(chips);
                    chipScroll.getStyleClass().add("population-grb-chip-scroll");
                    chipScroll.setFitToWidth(true);
                    chipScroll.setFitToHeight(false);
                    chipScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    chipScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    chipScroll.setPannable(true);
                    chipScroll.setFocusTraversable(false);
                    chipScroll.setMinHeight(72);
                    chipScroll.setPrefHeight(78);
                    chipScroll.setMaxHeight(78);
                    chipScroll.setMinWidth(0);
                    chipScroll.setMaxWidth(Double.MAX_VALUE);
                    chipScroll.setStyle(
                            "-fx-background-color: transparent;"
                                    + "-fx-background: transparent;"
                                    + "-fx-border-color: rgba(88, 145, 210, 0.18);"
                                    + "-fx-border-radius: 8;"
                                    + "-fx-background-radius: 8;"
                    );
                    VBox.setVgrow(chipScroll, Priority.NEVER);
                    manual.getChildren().set(index, chipScroll);
                    manual.getProperties().put(MANUAL_SCROLL, Boolean.TRUE);
                }
            }
        }

        ComboBox<?> combo = findEditableCombo(manual);
        if (combo == null || combo.getEditor() == null
                || Boolean.TRUE.equals(combo.getProperties().get(MANUAL_TAB))) return;
        combo.getProperties().put(MANUAL_TAB, Boolean.TRUE);
        TextField editor = combo.getEditor();
        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.TAB) return;
            String completion = manualCompletion(manual, combo, editor);
            if (completion == null || completion.isBlank()) return;
            editor.setText(completion);
            editor.positionCaret(completion.length());
            event.consume();
        });
    }

    private static void polishChip(Node node) {
        if (!(node instanceof Button chip) || !chip.getStyleClass().contains("population-grb-chip")) return;
        chip.setMinHeight(34);
        chip.setPrefHeight(36);
        chip.setMaxHeight(38);
        chip.setMinWidth(Region.USE_PREF_SIZE);
        chip.setPrefWidth(Region.USE_COMPUTED_SIZE);
        chip.setMaxWidth(150);
        chip.setWrapText(false);
        chip.setTextOverrun(OverrunStyle.ELLIPSIS);
        chip.setEllipsisString("…");
    }

    private static String manualCompletion(VBox manual, ComboBox<?> combo, TextField editor) {
        String typed = safe(editor.getText()).trim().toUpperCase(Locale.ROOT);
        Label ghost = findGhostLabel(manual);
        if (ghost != null && ghost.isVisible() && !safe(ghost.getText()).isBlank()) {
            String suffix = ghost.getText().trim().toUpperCase(Locale.ROOT);
            if (typed.equals("GRB")) return "GRB" + suffix;
            if (suffix.startsWith(typed)) return suffix;
        }
        Object selected = combo.getSelectionModel().getSelectedItem();
        if (selected != null) return selected.toString();
        if (!combo.getItems().isEmpty()) {
            Object first = combo.getItems().get(0);
            if (first != null) return first.toString();
        }
        return null;
    }

    private static Label findGhostLabel(VBox manual) {
        for (Label label : findAllLogical(manual, Label.class)) {
            if (label.isMouseTransparent() && label.getOpacity() < 0.8
                    && !safe(label.getText()).isBlank()) return label;
        }
        return null;
    }

    private static ComboBox<?> findEditableCombo(Node root) {
        for (ComboBox<?> combo : findAllLogical(root, ComboBox.class)) {
            if (combo.isEditable()) return combo;
        }
        return null;
    }

    private static HBox findTopPopulationRow(VBox card) {
        for (Node child : card.getChildren()) {
            if (!(child instanceof HBox row)) continue;
            boolean fracexp = row.getChildren().stream()
                    .anyMatch(n -> n.getStyleClass().contains("population-fracexp-inline"));
            if (fracexp && row.getChildren().size() >= 5) return row;
        }
        return null;
    }

    private static HBox findAdvancedRow(VBox card) {
        for (Node child : card.getChildren()) {
            if (child instanceof HBox row && countStyled(row, "sky-range-field") >= 6) return row;
        }
        return null;
    }

    private static int countStyled(Node node, String style) {
        int count = node.getStyleClass().contains(style) ? 1 : 0;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) count += countStyled(child, style);
        }
        return count;
    }

    /* ---------------- helpers ---------------- */

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

    private static HBox directHBoxWithCheckBox(VBox box) {
        for (Node child : box.getChildren()) {
            if (child instanceof HBox row
                    && row.getChildren().stream().anyMatch(CheckBox.class::isInstance)) return row;
        }
        return null;
    }

    private static void setWidth(Region region, double min, double pref, double max) {
        region.setMinWidth(min);
        region.setPrefWidth(pref);
        region.setMaxWidth(max);
        HBox.setHgrow(region, Priority.NEVER);
    }

    private static <T extends Node> T findFirst(Node root, Class<T> type) {
        List<T> all = findAllLogical(root, type);
        return all.isEmpty() ? null : all.get(0);
    }

    private static <T extends Node> T findLogical(Node node, Class<T> type, String styleClass) {
        if (node == null) return null;
        if (type.isInstance(node)
                && (styleClass == null || node.getStyleClass().contains(styleClass))) return type.cast(node);
        for (Node child : logicalChildren(node)) {
            T found = findLogical(child, type, styleClass);
            if (found != null) return found;
        }
        return null;
    }

    private static <T extends Node> List<T> findAllLogical(Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collectLogical(root, type, result);
        return result;
    }

    private static <T extends Node> void collectLogical(Node node, Class<T> type, List<T> out) {
        if (node == null) return;
        if (type.isInstance(node)) out.add(type.cast(node));
        for (Node child : logicalChildren(node)) collectLogical(child, type, out);
    }

    private static List<Node> logicalChildren(Node node) {
        List<Node> children = new ArrayList<>();
        if (node instanceof ScrollPane scroll && scroll.getContent() != null) children.add(scroll.getContent());
        if (node instanceof SplitPane split) children.addAll(split.getItems());
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) children.add(tab.getContent());
        }
        if (node instanceof Parent parent) children.addAll(parent.getChildrenUnmodifiable());
        return children.stream().distinct().toList();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
