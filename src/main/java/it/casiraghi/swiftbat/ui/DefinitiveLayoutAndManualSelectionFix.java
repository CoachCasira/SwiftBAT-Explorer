package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
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
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Page-scoped final corrections for the compact Explorer and Population layouts.
 * It observes only Explorer's workspace children and never scans the global scene
 * graph repeatedly, keeping scrolling and chart creation light on macOS.
 */
public final class DefinitiveLayoutAndManualSelectionFix {
    private static final String EXPLORER_INSTALLED = DefinitiveLayoutAndManualSelectionFix.class.getName() + ".explorer";
    private static final String POPULATION_INSTALLED = DefinitiveLayoutAndManualSelectionFix.class.getName() + ".population";
    private static final String DASHBOARD_DONE = DefinitiveLayoutAndManualSelectionFix.class.getName() + ".dashboard";
    private static final String MANUAL_STYLE = "population-manual-grb-v2";

    private DefinitiveLayoutAndManualSelectionFix() { }

    public static void install(Parent root) {
        if (root == null) return;
        ExplorerPage explorer = find(root, ExplorerPage.class);
        if (explorer != null) installExplorer(explorer);
        PopulationPage population = find(root, PopulationPage.class);
        if (population != null) installPopulation(population);
    }

    /* ---------------- Explorer ---------------- */

    private static void installExplorer(ExplorerPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(EXPLORER_INSTALLED))) return;
        page.getProperties().put(EXPLORER_INSTALLED, Boolean.TRUE);
        StackPane workspace = findByStyle(page, StackPane.class, "workspace-host");
        if (workspace == null) return;

        for (Node child : List.copyOf(workspace.getChildren())) polishExplorerDashboard(child);
        workspace.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) polishExplorerDashboard(added);
            }
        });
    }

    private static void polishExplorerDashboard(Node root) {
        VBox chartCard = findByStyle(root, VBox.class, "overview-chart-card");
        if (chartCard == null) return;
        if (Boolean.TRUE.equals(chartCard.getProperties().get(DASHBOARD_DONE))) return;
        chartCard.getProperties().put(DASHBOARD_DONE, Boolean.TRUE);

        BorderPane overview = nearestBorderPane(chartCard);
        VBox right = overview == null ? null : asVBox(overview.getRight());
        if (right != null) {
            right.setMinWidth(238);
            right.setPrefWidth(248);
            right.setMaxWidth(258);
            BorderPane.setMargin(right, new Insets(0, 0, 0, 10));
            for (Node child : right.getChildren()) {
                if (child instanceof Region region) {
                    region.setMinWidth(0);
                    region.setMaxWidth(Double.MAX_VALUE);
                }
            }
            VBox actionCard = findByStyle(right, VBox.class, "overview-action-card");
            if (actionCard != null) compactActionButtons(actionCard);
        }

        HBox controls = directHBoxWithCheckBox(chartCard);
        if (controls != null) {
            controls.setSpacing(8);
            controls.setAlignment(Pos.CENTER_LEFT);
            List<ComboBox<?>> combos = new ArrayList<>();
            List<javafx.scene.control.ChoiceBox<?>> choices = new ArrayList<>();
            for (Node child : controls.getChildren()) {
                if (child instanceof ComboBox<?> combo) combos.add(combo);
                if (child instanceof javafx.scene.control.ChoiceBox<?> choice) choices.add(choice);
            }
            if (!choices.isEmpty()) setWidth(choices.get(0), 174, 188, 196);
            if (choices.size() > 1) setWidth(choices.get(1), 168, 182, 190);

            CheckBox smooth = controls.getChildren().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .findFirst().orElse(null);
            if (smooth != null) {
                smooth.setMinWidth(158);
                smooth.setPrefWidth(168);
                smooth.setMaxWidth(174);
                smooth.setWrapText(false);
                smooth.setTextOverrun(OverrunStyle.CLIP);
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
                    }).findFirst().orElse(null);
            if (trigger != null) {
                trigger.setText("t = 0 · trigger");
                trigger.setMinWidth(88);
                trigger.setPrefWidth(98);
                trigger.setMaxWidth(106);
                trigger.setWrapText(false);
                trigger.setTextOverrun(OverrunStyle.CLIP);
                trigger.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                        "t = 0 indica il trigger", "t = 0 marks the trigger")));
            }

            // A single post-CSS pass fixes the very first dashboard layout on macOS.
            Platform.runLater(() -> {
                if (right != null) {
                    right.setMinWidth(238);
                    right.setPrefWidth(248);
                    right.setMaxWidth(258);
                }
                controls.requestLayout();
                chartCard.requestLayout();
            });
        }
    }

    private static void compactActionButtons(VBox actionCard) {
        HBox row = findFirst(actionCard, HBox.class);
        if (row == null) return;
        row.setSpacing(8);
        row.setAlignment(Pos.CENTER_RIGHT);
        for (Node child : row.getChildren()) {
            if (!(child instanceof Button button)) continue;
            String text = safe(button.getText()).toLowerCase(Locale.ROOT);
            if (text.contains("export") || text.contains("esporta")) {
                setWidth(button, 92, 98, 104);
            } else if (text.contains("fullscreen") || text.contains("schermo")) {
                setWidth(button, 104, 112, 118);
            }
        }
    }

    /* ---------------- Population top filters ---------------- */

    private static void installPopulation(PopulationPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(POPULATION_INSTALLED))) return;
        page.getProperties().put(POPULATION_INSTALLED, Boolean.TRUE);
        VBox card = findByStyle(page, VBox.class, "population-filter-card");
        if (card == null) return;

        alignPopulationFilters(card);
        installManualSelector(page, card);
        Platform.runLater(() -> {
            alignPopulationFilters(card);
            card.requestLayout();
        });
    }

    private static void alignPopulationFilters(VBox card) {
        HBox row = findTopPopulationRow(card);
        if (row == null || row.getChildren().size() < 5) return;
        row.setSpacing(8);
        row.setAlignment(Pos.TOP_LEFT);

        List<Node> children = row.getChildren();
        configureTopGroup(children.get(0), 132, 164, 174, false); // T90
        configureTopGroup(children.get(1), 205, 238, 252, true);  // redshift
        configureTopGroup(children.get(2), 220, 252, 268, true);  // time window
        configureFracexp(children.get(3), 338, 374, 392);
        configureTopGroup(children.get(4), 104, 126, 136, false); // maximum sample

        for (Node child : children) {
            if (child instanceof Region region) {
                region.setMinHeight(130);
                region.setPrefHeight(130);
            }
        }
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

    private static void configureTopGroup(Node node, double min, double pref, double max, boolean radios) {
        if (!(node instanceof VBox group)) return;
        group.setFillWidth(true);
        group.setMinWidth(min);
        group.setPrefWidth(pref);
        group.setMaxWidth(max);
        HBox.setHgrow(group, Priority.NEVER);

        // Never give the child the same fixed width as a padded card: that was
        // the cause of radio buttons visually escaping from their rectangle.
        if (group.getChildren().size() > 1 && group.getChildren().get(1) instanceof Region control) {
            control.setMinWidth(0);
            control.setPrefWidth(Region.USE_COMPUTED_SIZE);
            control.setMaxWidth(Double.MAX_VALUE);
        }

        if (radios) {
            HBox radioRow = findByStyle(group, HBox.class, "compact-radio-group");
            if (radioRow != null) {
                radioRow.setSpacing(4);
                radioRow.setMinWidth(0);
                radioRow.setPrefWidth(Region.USE_COMPUTED_SIZE);
                radioRow.setMaxWidth(Double.MAX_VALUE);
                for (Node child : radioRow.getChildren()) {
                    if (!(child instanceof RadioButton radio)) continue;
                    radio.setMinWidth(0);
                    radio.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    radio.setMaxWidth(Double.MAX_VALUE);
                    radio.setWrapText(false);
                    radio.setTextOverrun(OverrunStyle.CLIP);
                    HBox.setHgrow(radio, Priority.ALWAYS);
                }
            }
        }
    }

    private static void configureFracexp(Node node, double min, double pref, double max) {
        if (!(node instanceof VBox exposure)) return;
        exposure.setMinWidth(min);
        exposure.setPrefWidth(pref);
        exposure.setMaxWidth(max);
        exposure.setFillWidth(true);
        HBox.setHgrow(exposure, Priority.NEVER);

        HBox controls = null;
        for (Node child : exposure.getChildren()) {
            if (child instanceof HBox candidate && candidate.getChildren().stream()
                    .anyMatch(n -> n.getStyleClass().contains("percentage-control"))) {
                controls = candidate;
                break;
            }
        }
        if (controls == null) return;
        controls.setSpacing(8);
        controls.setMinWidth(0);
        controls.setPrefWidth(Region.USE_COMPUTED_SIZE);
        controls.setMaxWidth(Double.MAX_VALUE);

        for (Node child : controls.getChildren()) {
            if (!(child instanceof VBox percentage)
                    || !percentage.getStyleClass().contains("percentage-control")) continue;
            percentage.setMinWidth(0);
            percentage.setPrefWidth(Region.USE_COMPUTED_SIZE);
            percentage.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(percentage, Priority.ALWAYS);
            for (Node inner : percentage.getChildren()) {
                if (!(inner instanceof HBox heading)) continue;
                heading.setSpacing(4);
                heading.setMinWidth(0);
                heading.setMaxWidth(Double.MAX_VALUE);
                for (Node item : heading.getChildren()) {
                    if (item instanceof Label label && label.getStyleClass().contains("filter-label")) {
                        label.setMinWidth(54);
                        label.setPrefWidth(62);
                        label.setMaxWidth(68);
                        label.setWrapText(false);
                        label.setTextOverrun(OverrunStyle.CLIP);
                    } else if (item instanceof HBox valueBox) {
                        valueBox.setSpacing(3);
                        valueBox.setMinWidth(86);
                        valueBox.setPrefWidth(96);
                        valueBox.setMaxWidth(Double.MAX_VALUE);
                        HBox.setHgrow(valueBox, Priority.ALWAYS);
                        for (Node valueItem : valueBox.getChildren()) {
                            if (valueItem instanceof TextField field
                                    && field.getStyleClass().contains("percentage-field")) {
                                setWidth(field, 42, 48, 54);
                            } else if (valueItem instanceof Button button
                                    && button.getStyleClass().contains("filter-reset-button")) {
                                setWidth(button, 30, 30, 30);
                            }
                        }
                    }
                }
            }
        }
    }

    /* ---------------- Manual GRB selector ---------------- */

    private static void installManualSelector(PopulationPage page, VBox filterCard) {
        HBox advanced = findAdvancedFilterRow(filterCard);
        if (advanced == null) return;

        // Remove a partial/older injected selector if present, then install one
        // deterministic implementation directly before z/RA/DEC.
        advanced.getChildren().removeIf(node -> node.getStyleClass().contains("population-manual-grb-filter")
                || node.getStyleClass().contains(MANUAL_STYLE));

        ManualSelector controller = new ManualSelector(page);
        VBox selector = controller.build();
        advanced.setMaxWidth(Double.MAX_VALUE);
        advanced.setSpacing(10);
        advanced.setAlignment(Pos.TOP_LEFT);
        advanced.getChildren().add(0, selector);
        controller.attachAnalyze();
        controller.attachReset(filterCard);
    }

    private static HBox findAdvancedFilterRow(VBox card) {
        for (Node child : card.getChildren()) {
            if (!(child instanceof HBox row)) continue;
            if (countDescendantsWithStyle(row, "sky-range-field") >= 6) return row;
        }
        return findHBoxWithRangeFields(card);
    }

    private static HBox findHBoxWithRangeFields(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof HBox row && countDescendantsWithStyle(row, "sky-range-field") >= 6) return row;
            if (child instanceof Parent nested) {
                HBox found = findHBoxWithRangeFields(nested);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int countDescendantsWithStyle(Node node, String style) {
        int count = node.getStyleClass().contains(style) ? 1 : 0;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) count += countDescendantsWithStyle(child, style);
        }
        return count;
    }

    private static final class ManualSelector {
        private final PopulationPage page;
        private final LinkedHashSet<String> selected = new LinkedHashSet<>();
        private final ComboBox<String> search = new ComboBox<>();
        private final Label ghost = new Label();
        private final Label saved = new Label();
        private final FlowPane chips = new FlowPane(6, 6);
        private String lastHint;
        private boolean internalEdit;

        ManualSelector(PopulationPage page) {
            this.page = page;
        }

        VBox build() {
            Label title = UiFactory.label(I18n.dynamic("Gruppo GRB manuale", "Manual GRB group"), "filter-label");
            Label caption = UiFactory.label(I18n.dynamic(
                    "Cerca e salva uno o più GRB", "Search and save one or more GRBs"), "subtle-text");

            search.setEditable(true);
            search.setVisibleRowCount(4);
            search.getStyleClass().add("choice-box-modern");
            search.setMinWidth(220);
            search.setPrefWidth(250);
            search.setMaxWidth(Double.MAX_VALUE);
            search.getEditor().setText("GRB");
            search.getEditor().positionCaret(3);
            search.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                    "Il prefisso GRB è già inserito. Digita i numeri e scegli dal menu.",
                    "The GRB prefix is already inserted. Type the digits and choose from the menu.")));

            ghost.getStyleClass().add("subtle-text");
            ghost.setOpacity(0.45);
            ghost.setMouseTransparent(true);
            ghost.setPadding(new Insets(0, 28, 0, 48));
            StackPane searchStack = new StackPane(search, ghost);
            StackPane.setAlignment(search, Pos.CENTER_LEFT);
            StackPane.setAlignment(ghost, Pos.CENTER_LEFT);
            HBox.setHgrow(searchStack, Priority.ALWAYS);

            saved.getStyleClass().add("subtle-text");
            saved.setMinWidth(74);
            saved.setAlignment(Pos.CENTER_LEFT);
            HBox searchRow = new HBox(7, searchStack, saved);
            searchRow.setAlignment(Pos.CENTER_LEFT);

            chips.setAlignment(Pos.CENTER_LEFT);
            chips.setPrefWrapLength(300);
            chips.setMinHeight(26);

            TextField editor = search.getEditor();
            editor.textProperty().addListener((obs, oldValue, newValue) -> {
                normalizePrefix(newValue);
                refreshMatches();
                updateGhostVisibility();
            });
            editor.focusedProperty().addListener((obs, oldValue, focused) -> {
                if (focused) {
                    rotateHint();
                    refreshMatches();
                } else {
                    search.hide();
                }
            });
            editor.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    rotateHint();
                    refreshMatches();
                }
            });
            editor.setOnAction(event -> addBestMatch());
            search.setOnAction(event -> {
                String choice = search.getSelectionModel().getSelectedItem();
                if (choice != null) add(choice);
            });

            Runnable relabel = () -> {
                title.setText(I18n.dynamic("Gruppo GRB manuale", "Manual GRB group"));
                caption.setText(I18n.dynamic("Cerca e salva uno o più GRB", "Search and save one or more GRBs"));
                refreshSaved();
            };
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> relabel.run());

            VBox box = new VBox(5, title, caption, searchRow, chips);
            box.getStyleClass().addAll("population-filter-group", MANUAL_STYLE);
            box.setMinWidth(300);
            box.setPrefWidth(330);
            box.setMaxWidth(350);
            refreshSaved();
            updateGhostVisibility();
            return box;
        }

        private void normalizePrefix(String value) {
            if (internalEdit) return;
            String raw = safe(value).toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
            String normalized;
            if (raw.isBlank() || raw.equals("G") || raw.equals("GR")) normalized = "GRB";
            else if (raw.startsWith("GRB")) normalized = raw;
            else normalized = "GRB" + raw.replaceFirst("^GRB", "");
            if (Objects.equals(value, normalized)) return;
            setEditor(normalized);
        }

        private void setEditor(String text) {
            internalEdit = true;
            try {
                search.getEditor().setText(text);
                search.getEditor().positionCaret(text.length());
            } finally {
                internalEdit = false;
            }
        }

        private void refreshMatches() {
            Map<String, CatalogEntry> catalog = catalog();
            if (catalog.isEmpty() || !search.getEditor().isFocused()) {
                search.hide();
                return;
            }
            String text = safe(search.getEditor().getText()).toUpperCase(Locale.ROOT);
            String suffix = text.startsWith("GRB") ? text.substring(3) : text;
            List<String> matches = new ArrayList<>();
            for (CatalogEntry entry : catalog.values()) {
                String name = entry.grbName().toUpperCase(Locale.ROOT);
                if (selected.contains(name)) continue;
                String searchable = name.startsWith("GRB") ? name.substring(3) : name;
                if (suffix.isBlank() || searchable.contains(suffix)) matches.add(name);
            }
            search.setItems(FXCollections.observableArrayList(matches));
            search.setVisibleRowCount(Math.max(1, Math.min(4, matches.size())));
            if (!matches.isEmpty()) search.show();
            else search.hide();
        }

        private void rotateHint() {
            List<String> available = new ArrayList<>();
            for (CatalogEntry entry : catalog().values()) {
                String name = entry.grbName().toUpperCase(Locale.ROOT);
                if (!selected.contains(name) && !Objects.equals(name, lastHint)) available.add(name);
            }
            if (available.isEmpty()) {
                lastHint = null;
                ghost.setText("");
                return;
            }
            lastHint = available.get(ThreadLocalRandom.current().nextInt(available.size()));
            ghost.setText(lastHint.startsWith("GRB") ? lastHint.substring(3) : lastHint);
            updateGhostVisibility();
        }

        private void updateGhostVisibility() {
            boolean show = "GRB".equalsIgnoreCase(safe(search.getEditor().getText())) && lastHint != null;
            ghost.setVisible(show);
            ghost.setManaged(show);
        }

        private void addBestMatch() {
            String typed = safe(search.getEditor().getText()).toUpperCase(Locale.ROOT);
            if (catalog().containsKey(typed)) add(typed);
            else if (!search.getItems().isEmpty()) add(search.getItems().get(0));
        }

        private void add(String name) {
            if (name == null) return;
            String normalized = name.toUpperCase(Locale.ROOT);
            if (!catalog().containsKey(normalized) || !selected.add(normalized)) return;
            search.hide();
            search.getSelectionModel().clearSelection();
            setEditor("GRB");
            rebuildChips();
            rotateHint();
        }

        private void remove(String name) {
            if (!selected.remove(name)) return;
            rebuildChips();
            rotateHint();
        }

        private void clear() {
            selected.clear();
            search.hide();
            search.getSelectionModel().clearSelection();
            setEditor("GRB");
            rebuildChips();
            rotateHint();
        }

        private void rebuildChips() {
            chips.getChildren().clear();
            for (String name : selected) {
                Button chip = UiFactory.button(name + "  ×", "ghost-button");
                chip.getStyleClass().add("population-grb-chip");
                chip.setMinHeight(26);
                chip.setPrefHeight(26);
                chip.setMaxHeight(26);
                chip.setOnAction(event -> remove(name));
                chips.getChildren().add(chip);
            }
            refreshSaved();
        }

        private void refreshSaved() {
            saved.setText(selected.isEmpty()
                    ? I18n.dynamic("0 salvati", "0 saved")
                    : I18n.dynamic(selected.size() + " salvati", selected.size() + " saved"));
            saved.setTooltip(selected.isEmpty() ? null : UiFactory.quickTooltip(I18n.dynamic(
                    "Analyze Group userà il gruppo GRB salvato; FRACEXP e finestra temporale restano applicati.",
                    "Analyze Group will use the saved GRB group; FRACEXP and the time window still apply.")));
        }

        @SuppressWarnings("unchecked")
        private Map<String, CatalogEntry> catalog() {
            Object value = readField(page, "catalog");
            return value instanceof Map<?, ?> map ? (Map<String, CatalogEntry>) map : Map.of();
        }

        private void attachAnalyze() {
            Object value = readField(page, "analyze");
            if (!(value instanceof Button analyze)) return;
            analyze.addEventFilter(ActionEvent.ACTION, event -> {
                if (selected.isEmpty()) return;
                Map<String, CatalogEntry> targetCatalog = catalog();
                if (targetCatalog.isEmpty()) return;

                LinkedHashMap<String, CatalogEntry> originalCatalog = new LinkedHashMap<>(targetCatalog);
                LinkedHashMap<String, CatalogEntry> manual = new LinkedHashMap<>();
                for (String name : selected) {
                    CatalogEntry entry = originalCatalog.get(name);
                    if (entry != null) manual.put(name, entry);
                }
                if (manual.isEmpty()) return;

                javafx.scene.control.ChoiceBox<String> duration = choice("duration");
                javafx.scene.control.ChoiceBox<String> redshift = choice("redshiftAvailability");
                javafx.scene.control.ChoiceBox<String> limit = choice("limit");
                TextField zMin = text("zMin");
                TextField zMax = text("zMax");
                TextField raMin = text("raMin");
                TextField raMax = text("raMax");
                TextField decMin = text("decMin");
                TextField decMax = text("decMax");

                String oldDuration = duration == null ? null : duration.getValue();
                String oldRedshift = redshift == null ? null : redshift.getValue();
                String oldLimit = limit == null ? null : limit.getValue();
                String oldZMin = value(zMin), oldZMax = value(zMax);
                String oldRaMin = value(raMin), oldRaMax = value(raMax);
                String oldDecMin = value(decMin), oldDecMax = value(decMax);

                targetCatalog.clear();
                targetCatalog.putAll(manual);
                if (duration != null && !duration.getItems().isEmpty()) duration.setValue(duration.getItems().get(0));
                if (redshift != null && !redshift.getItems().isEmpty()) redshift.setValue(redshift.getItems().get(0));
                if (limit != null && limit.getItems().contains("Tutti")) limit.setValue("Tutti");
                set(zMin, "0"); set(zMax, "10");
                set(raMin, "0"); set(raMax, "360");
                set(decMin, "-90"); set(decMax, "90");

                Platform.runLater(() -> {
                    targetCatalog.clear();
                    targetCatalog.putAll(originalCatalog);
                    restore(duration, oldDuration);
                    restore(redshift, oldRedshift);
                    restore(limit, oldLimit);
                    set(zMin, oldZMin); set(zMax, oldZMax);
                    set(raMin, oldRaMin); set(raMax, oldRaMax);
                    set(decMin, oldDecMin); set(decMax, oldDecMax);
                });
            });
        }

        private void attachReset(VBox card) {
            for (Button button : findAll(card, Button.class)) {
                String text = safe(button.getText()).toLowerCase(Locale.ROOT);
                if (text.contains("ripristina filtri") || text.contains("reset filters")) {
                    button.addEventFilter(ActionEvent.ACTION, event -> clear());
                }
            }
        }

        @SuppressWarnings("unchecked")
        private javafx.scene.control.ChoiceBox<String> choice(String name) {
            Object value = readField(page, name);
            return value instanceof javafx.scene.control.ChoiceBox<?> c
                    ? (javafx.scene.control.ChoiceBox<String>) c : null;
        }

        private TextField text(String name) {
            Object value = readField(page, name);
            return value instanceof TextField field ? field : null;
        }

        private static String value(TextField field) {
            return field == null ? null : field.getText();
        }

        private static void set(TextField field, String value) {
            if (field != null && value != null) field.setText(value);
        }

        private static void restore(javafx.scene.control.ChoiceBox<String> choice, String value) {
            if (choice != null && value != null) choice.setValue(value);
        }
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
            if (child instanceof HBox row && row.getChildren().stream().anyMatch(CheckBox.class::isInstance)) return row;
        }
        return null;
    }

    private static VBox asVBox(Node node) {
        return node instanceof VBox box ? box : null;
    }

    private static <T extends Node> T find(Node root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = find(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Node> T findByStyle(Node root, Class<T> type, String style) {
        if (type.isInstance(root) && root.getStyleClass().contains(style)) return type.cast(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findByStyle(child, type, style);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Node> T findFirst(Node root, Class<T> type) {
        return find(root, type);
    }

    private static <T extends Node> List<T> findAll(Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collect(root, type, result);
        return result;
    }

    private static <T extends Node> void collect(Node root, Class<T> type, List<T> out) {
        if (type.isInstance(root)) out.add(type.cast(root));
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collect(child, type, out);
        }
    }

    private static Object readField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void setWidth(Region region, double min, double pref, double max) {
        region.setMinWidth(min);
        region.setPrefWidth(pref);
        region.setMaxWidth(max);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
