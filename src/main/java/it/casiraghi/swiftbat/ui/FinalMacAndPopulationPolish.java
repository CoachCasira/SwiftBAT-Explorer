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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
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
 * Final, page-scoped layout pass for the compact macOS/Windows layout.
 *
 * <p>This class deliberately does not watch the entire JavaFX scene graph. It
 * touches Population once and only watches Explorer's workspace direct child,
 * so rebuilding a GRB dashboard stays cheap on macOS.</p>
 */
public final class FinalMacAndPopulationPolish {
    private static final String EXPLORER_DONE = FinalMacAndPopulationPolish.class.getName() + ".explorerDone";
    private static final String POPULATION_DONE = FinalMacAndPopulationPolish.class.getName() + ".populationDone";
    private static final String MANUAL_DONE = FinalMacAndPopulationPolish.class.getName() + ".manualDone";
    private static final String OVERVIEW_DONE = FinalMacAndPopulationPolish.class.getName() + ".overviewDone";

    private FinalMacAndPopulationPolish() { }

    public static void install(Parent root) {
        if (root == null) return;
        ExplorerPage explorer = find(root, ExplorerPage.class);
        if (explorer != null) installExplorer(explorer);
        PopulationPage population = find(root, PopulationPage.class);
        if (population != null) installPopulation(population);
    }

    /* ---------------- Explorer: deterministic first layout ---------------- */

    private static void installExplorer(ExplorerPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(EXPLORER_DONE))) return;
        page.getProperties().put(EXPLORER_DONE, Boolean.TRUE);

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
        if (chartCard == null || Boolean.TRUE.equals(chartCard.getProperties().get(OVERVIEW_DONE))) return;
        chartCard.getProperties().put(OVERVIEW_DONE, Boolean.TRUE);
        chartCard.setMinWidth(0);
        chartCard.setMaxWidth(Double.MAX_VALUE);

        HBox controls = null;
        for (Node child : chartCard.getChildren()) {
            if (child instanceof HBox row && row.getChildren().stream().anyMatch(CheckBox.class::isInstance)) {
                controls = row;
                break;
            }
        }
        if (controls == null) return;
        controls.setSpacing(9);
        controls.setAlignment(Pos.CENTER_LEFT);

        List<ChoiceBox<?>> choices = controls.getChildren().stream()
                .filter(ChoiceBox.class::isInstance).map(ChoiceBox.class::cast).toList();
        if (!choices.isEmpty()) setWidth(choices.get(0), 220, 236, 248);
        if (choices.size() > 1) setWidth(choices.get(1), 210, 226, 238);

        CheckBox smooth = controls.getChildren().stream()
                .filter(CheckBox.class::isInstance).map(CheckBox.class::cast).findFirst().orElse(null);
        if (smooth != null) {
            smooth.setMinWidth(190);
            smooth.setPrefWidth(202);
            smooth.setMaxWidth(212);
            smooth.setWrapText(false);
            smooth.setTextOverrun(OverrunStyle.CLIP);
            HBox.setHgrow(smooth, Priority.NEVER);
            smooth.setTooltip(UiFactory.quickTooltip(I18n.dynamic("Media mobile su 5 bin", "5-bin moving average")));
        }

        Label trigger = controls.getChildren().stream()
                .filter(Label.class::isInstance).map(Label.class::cast)
                .filter(label -> {
                    String text = label.getText() == null ? "" : label.getText().toLowerCase(Locale.ROOT);
                    return text.contains("trigger") || text.contains("t = 0");
                }).findFirst().orElse(null);
        if (trigger != null) {
            trigger.setText("t = 0 · trigger");
            trigger.setMinWidth(112);
            trigger.setPrefWidth(124);
            trigger.setMaxWidth(136);
            trigger.setWrapText(false);
            trigger.setTextOverrun(OverrunStyle.CLIP);
            trigger.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                    "t = 0 indica il trigger", "t = 0 marks the trigger")));
        }

        // Apply after CSS has measured fonts too. This is one single deferred pass,
        // not a recurring scene-graph scan.
        HBox finalControls = controls;
        Platform.runLater(() -> {
            if (smooth != null) {
                smooth.setMinWidth(190);
                smooth.setPrefWidth(202);
                smooth.setMaxWidth(212);
            }
            if (trigger != null) {
                trigger.setText("t = 0 · trigger");
                trigger.setMinWidth(112);
                trigger.setPrefWidth(124);
                trigger.setMaxWidth(136);
            }
            finalControls.requestLayout();
        });
    }

    /* ---------------- Population: aligned compact top filters ---------------- */

    private static void installPopulation(PopulationPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(POPULATION_DONE))) return;
        page.getProperties().put(POPULATION_DONE, Boolean.TRUE);

        VBox filterCard = findByStyle(page, VBox.class, "population-filter-card");
        if (filterCard == null) return;
        alignTopFilters(filterCard);
        installManualGrbSelector(page, filterCard);
    }

    private static void alignTopFilters(VBox card) {
        HBox top = null;
        for (Node child : card.getChildren()) {
            if (child instanceof HBox row && row.getChildren().size() >= 5
                    && row.getChildren().stream().anyMatch(n -> n.getStyleClass().contains("population-fracexp-inline"))) {
                top = row;
                break;
            }
        }
        if (top == null) return;

        top.setSpacing(12);
        top.setAlignment(Pos.TOP_LEFT);
        List<Node> children = top.getChildren();
        if (children.size() < 5) return;

        // Stable geometry: enough room for every label, but still fits comfortably
        // in the 1580px reference window without overlaps or ellipses.
        polishFilterGroup(children.get(0), 190, -1); // T90
        polishFilterGroup(children.get(1), 260, 80); // redshift
        polishFilterGroup(children.get(2), 280, 86); // time window
        polishFracexp(children.get(3), 480);
        polishFilterGroup(children.get(4), 145, -1); // maximum sample

        for (Node child : children) {
            if (child instanceof Region region) {
                region.setMinHeight(138);
                region.setPrefHeight(138);
            }
        }
        top.requestLayout();
    }

    private static void polishFilterGroup(Node node, double width, double radioWidth) {
        if (!(node instanceof VBox group)) return;
        setFixedWidth(group, width);
        if (group.getChildren().size() > 1 && group.getChildren().get(1) instanceof Region control) {
            setFixedWidth(control, width);
        }
        HBox radios = findByStyle(group, HBox.class, "compact-radio-group");
        if (radios != null && radioWidth > 0) {
            radios.setSpacing(5);
            radios.setMinWidth(width);
            radios.setPrefWidth(width);
            radios.setMaxWidth(width);
            for (Node child : radios.getChildren()) {
                if (child instanceof RadioButton radio) {
                    radio.setMinWidth(radioWidth);
                    radio.setPrefWidth(radioWidth);
                    radio.setMaxWidth(radioWidth);
                    radio.setWrapText(false);
                    radio.setTextOverrun(OverrunStyle.CLIP);
                }
            }
        }
    }

    private static void polishFracexp(Node node, double width) {
        if (!(node instanceof VBox exposure)) return;
        setFixedWidth(exposure, width);
        HBox controls = null;
        for (Node child : exposure.getChildren()) {
            if (child instanceof HBox row && row.getChildren().stream()
                    .anyMatch(n -> n.getStyleClass().contains("percentage-control"))) {
                controls = row;
                break;
            }
        }
        if (controls == null) return;
        controls.setSpacing(8);
        controls.setMinWidth(width);
        controls.setPrefWidth(width);
        controls.setMaxWidth(width);
        double each = (width - 8) / 2.0;
        for (Node child : controls.getChildren()) {
            if (!(child instanceof VBox percentage) || !percentage.getStyleClass().contains("percentage-control")) continue;
            setFixedWidth(percentage, each);
            HBox.setHgrow(percentage, Priority.NEVER);
            for (Node inner : percentage.getChildren()) {
                if (!(inner instanceof HBox heading)) continue;
                heading.setSpacing(5);
                for (Node item : heading.getChildren()) {
                    if (item instanceof Label label && label.getStyleClass().contains("filter-label")) {
                        label.setMinWidth(72);
                        label.setPrefWidth(78);
                        label.setMaxWidth(84);
                        label.setWrapText(false);
                        label.setTextOverrun(OverrunStyle.CLIP);
                    } else if (item instanceof HBox valueBox) {
                        valueBox.setSpacing(4);
                        valueBox.setMinWidth(108);
                        valueBox.setPrefWidth(116);
                        valueBox.setMaxWidth(124);
                    }
                }
            }
        }
    }

    /* ---------------- Population advanced manual GRB group ---------------- */

    private static void installManualGrbSelector(PopulationPage page, VBox card) {
        if (Boolean.TRUE.equals(page.getProperties().get(MANUAL_DONE))) return;

        HBox advancedRow = findAdvancedRow(card);
        if (advancedRow == null) return;
        page.getProperties().put(MANUAL_DONE, Boolean.TRUE);

        ManualSelectionController controller = new ManualSelectionController(page);
        VBox selector = controller.build();
        advancedRow.setMaxWidth(Double.MAX_VALUE);
        advancedRow.setSpacing(12);
        advancedRow.setAlignment(Pos.TOP_LEFT);
        advancedRow.getChildren().add(0, selector);
        controller.attachAnalyzeOverride();
        controller.attachResetButton(card);
    }

    private static HBox findAdvancedRow(VBox card) {
        List<Node> children = card.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Node node = children.get(i);
            if (!(node instanceof ToggleButton toggle) || !toggle.getStyleClass().contains("sky-toggle")) continue;
            for (int j = i + 1; j < children.size(); j++) {
                if (children.get(j) instanceof HBox row && row.getChildren().size() >= 3) return row;
            }
        }
        return null;
    }

    private static final class ManualSelectionController {
        private final PopulationPage page;
        private final LinkedHashSet<String> selected = new LinkedHashSet<>();
        private final ListView<String> suggestions = new ListView<>();
        private final TextField search = new TextField("GRB");
        private final Label ghost = new Label();
        private final Label saved = new Label();
        private final FlowPane chips = new FlowPane(6, 6);
        private String lastHint;
        private boolean normalizing;

        private ManualSelectionController(PopulationPage page) {
            this.page = page;
        }

        VBox build() {
            Label title = UiFactory.label(I18n.dynamic("Gruppo GRB manuale", "Manual GRB group"), "filter-label");
            Label caption = UiFactory.label(I18n.dynamic(
                    "Scrivi il nome e seleziona uno o più GRB", "Type a name and select one or more GRBs"),
                    "subtle-text");
            caption.setWrapText(false);

            search.getStyleClass().add("search-field");
            search.setMinWidth(190);
            search.setPrefWidth(210);
            search.setMaxWidth(Double.MAX_VALUE);
            search.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                    "Il prefisso GRB viene mantenuto automaticamente. Invio seleziona il primo risultato.",
                    "The GRB prefix is kept automatically. Enter selects the first result.")));

            ghost.getStyleClass().add("subtle-text");
            ghost.setOpacity(0.48);
            ghost.setMouseTransparent(true);
            ghost.setPadding(new Insets(0, 0, 0, 48));
            StackPane searchStack = new StackPane(search, ghost);
            StackPane.setAlignment(search, Pos.CENTER_LEFT);
            StackPane.setAlignment(ghost, Pos.CENTER_LEFT);
            HBox.setHgrow(searchStack, Priority.ALWAYS);

            saved.getStyleClass().add("subtle-text");
            saved.setMinWidth(86);
            saved.setAlignment(Pos.CENTER_LEFT);
            HBox searchLine = new HBox(8, searchStack, saved);
            searchLine.setAlignment(Pos.CENTER_LEFT);
            searchLine.setMinWidth(0);

            suggestions.getStyleClass().add("catalog-list");
            suggestions.setFixedCellSize(30);
            suggestions.setMinHeight(0);
            suggestions.setPrefHeight(122);
            suggestions.setMaxHeight(122);
            suggestions.setVisible(false);
            suggestions.setManaged(false);

            chips.setAlignment(Pos.CENTER_LEFT);
            chips.setPrefWrapLength(280);
            chips.setMinHeight(28);

            search.textProperty().addListener((obs, oldValue, newValue) -> {
                normalizeSearchText(newValue);
                refreshSuggestions();
                refreshGhostVisibility();
            });
            search.focusedProperty().addListener((obs, oldValue, focused) -> {
                if (focused) {
                    rotateHint();
                    refreshSuggestions();
                }
            });
            search.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    rotateHint();
                    refreshSuggestions();
                }
            });
            search.setOnAction(event -> addBestMatch());
            suggestions.setOnMouseClicked(event -> {
                if (event.getButton() != MouseButton.PRIMARY) return;
                String value = suggestions.getSelectionModel().getSelectedItem();
                if (value != null) addSelection(value);
            });

            Runnable relabel = () -> {
                title.setText(I18n.dynamic("Gruppo GRB manuale", "Manual GRB group"));
                caption.setText(I18n.dynamic("Scrivi il nome e seleziona uno o più GRB",
                        "Type a name and select one or more GRBs"));
                refreshSavedText();
            };
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> relabel.run());

            VBox box = new VBox(5, title, caption, searchLine, suggestions, chips);
            box.getStyleClass().addAll("population-filter-group", "population-manual-grb-filter");
            box.setMinWidth(300);
            box.setPrefWidth(315);
            box.setMaxWidth(330);
            refreshSavedText();
            refreshGhostVisibility();
            return box;
        }

        private void normalizeSearchText(String value) {
            if (normalizing) return;
            String raw = value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
            String normalized;
            if (raw.isBlank() || raw.equals("G") || raw.equals("GR")) normalized = "GRB";
            else if (raw.startsWith("GRB")) normalized = raw;
            else normalized = "GRB" + raw.replaceFirst("^GRB", "");
            if (Objects.equals(value, normalized)) return;
            normalizing = true;
            try {
                search.setText(normalized);
                search.positionCaret(normalized.length());
            } finally {
                normalizing = false;
            }
        }

        private void refreshSuggestions() {
            Map<String, CatalogEntry> catalog = catalog();
            if (catalog.isEmpty() || !search.isFocused()) {
                showSuggestions(false);
                return;
            }
            String query = search.getText() == null ? "" : search.getText().toUpperCase(Locale.ROOT);
            String suffix = query.startsWith("GRB") ? query.substring(3) : query;
            List<String> matches = new ArrayList<>();
            for (CatalogEntry entry : catalog.values()) {
                String name = entry.grbName().toUpperCase(Locale.ROOT);
                if (selected.contains(name)) continue;
                if (suffix.isBlank() || name.substring(Math.min(3, name.length())).contains(suffix)) {
                    matches.add(name);
                }
            }
            suggestions.setItems(FXCollections.observableArrayList(matches));
            showSuggestions(!matches.isEmpty());
        }

        private void showSuggestions(boolean show) {
            suggestions.setVisible(show);
            suggestions.setManaged(show);
            if (show) {
                suggestions.setPrefHeight(Math.min(4, suggestions.getItems().size()) * 30.0 + 2.0);
            }
        }

        private void rotateHint() {
            List<String> available = new ArrayList<>();
            for (CatalogEntry entry : catalog().values()) {
                String name = entry.grbName().toUpperCase(Locale.ROOT);
                if (!selected.contains(name) && !Objects.equals(name, lastHint)) available.add(name);
            }
            if (available.isEmpty()) {
                ghost.setText("");
                lastHint = null;
                return;
            }
            String choice = available.get(ThreadLocalRandom.current().nextInt(available.size()));
            lastHint = choice;
            ghost.setText(choice.startsWith("GRB") ? choice.substring(3) : choice);
            refreshGhostVisibility();
        }

        private void refreshGhostVisibility() {
            String text = search.getText() == null ? "" : search.getText();
            ghost.setVisible(text.equalsIgnoreCase("GRB") && lastHint != null);
            ghost.setManaged(ghost.isVisible());
        }

        private void addBestMatch() {
            String typed = search.getText() == null ? "" : search.getText().toUpperCase(Locale.ROOT);
            CatalogEntry exact = catalog().get(typed);
            if (exact != null) {
                addSelection(exact.grbName());
                return;
            }
            if (!suggestions.getItems().isEmpty()) addSelection(suggestions.getItems().get(0));
        }

        private void addSelection(String name) {
            if (name == null) return;
            String normalized = name.toUpperCase(Locale.ROOT);
            if (!catalog().containsKey(normalized) || !selected.add(normalized)) return;
            rebuildChips();
            search.setText("GRB");
            rotateHint();
            refreshSuggestions();
        }

        private void removeSelection(String name) {
            if (!selected.remove(name)) return;
            rebuildChips();
            rotateHint();
            refreshSuggestions();
        }

        private void clearSelections() {
            if (selected.isEmpty()) return;
            selected.clear();
            rebuildChips();
            search.setText("GRB");
            rotateHint();
            refreshSuggestions();
        }

        private void rebuildChips() {
            chips.getChildren().clear();
            for (String name : selected) {
                Button chip = UiFactory.button(name + "  ×", "ghost-button");
                chip.getStyleClass().add("population-grb-chip");
                chip.setMinHeight(28);
                chip.setPrefHeight(28);
                chip.setMaxHeight(28);
                chip.setOnAction(event -> removeSelection(name));
                chips.getChildren().add(chip);
            }
            refreshSavedText();
        }

        private void refreshSavedText() {
            if (selected.isEmpty()) {
                saved.setText(I18n.dynamic("0 salvati", "0 saved"));
                saved.setTooltip(null);
            } else {
                saved.setText(I18n.dynamic(selected.size() + " salvati", selected.size() + " saved"));
                saved.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                        "Analyze Group userà esattamente i GRB salvati; FRACEXP e finestra temporale restano applicati.",
                        "Analyze Group will use exactly the saved GRBs; FRACEXP and the time window still apply.")));
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, CatalogEntry> catalog() {
            Object value = readField(page, "catalog");
            if (!(value instanceof Map<?, ?> map)) return Map.of();
            return (Map<String, CatalogEntry>) map;
        }

        private void attachAnalyzeOverride() {
            Object value = readField(page, "analyze");
            if (!(value instanceof Button analyze)) return;
            analyze.addEventFilter(ActionEvent.ACTION, event -> {
                if (selected.isEmpty()) return;
                Map<String, CatalogEntry> catalog = catalog();
                if (catalog.isEmpty()) return;

                LinkedHashMap<String, CatalogEntry> originalCatalog = new LinkedHashMap<>(catalog);
                LinkedHashMap<String, CatalogEntry> manualCatalog = new LinkedHashMap<>();
                for (String name : selected) {
                    CatalogEntry entry = originalCatalog.get(name);
                    if (entry != null) manualCatalog.put(name, entry);
                }
                if (manualCatalog.isEmpty()) return;

                ChoiceBox<String> duration = choiceField("duration");
                ChoiceBox<String> redshift = choiceField("redshiftAvailability");
                ChoiceBox<String> limit = choiceField("limit");
                TextField zMin = textField("zMin");
                TextField zMax = textField("zMax");
                TextField raMin = textField("raMin");
                TextField raMax = textField("raMax");
                TextField decMin = textField("decMin");
                TextField decMax = textField("decMax");

                String oldDuration = duration == null ? null : duration.getValue();
                String oldRedshift = redshift == null ? null : redshift.getValue();
                String oldLimit = limit == null ? null : limit.getValue();
                String oldZMin = text(zMin), oldZMax = text(zMax);
                String oldRaMin = text(raMin), oldRaMax = text(raMax);
                String oldDecMin = text(decMin), oldDecMax = text(decMax);

                catalog.clear();
                catalog.putAll(manualCatalog);
                if (duration != null) duration.setValue("Tutte le durate");
                if (redshift != null) redshift.setValue("Con e senza redshift");
                if (limit != null) limit.setValue("Tutti");
                setText(zMin, "0"); setText(zMax, "100");
                setText(raMin, "0"); setText(raMax, "360");
                setText(decMin, "-90"); setText(decMax, "90");

                Platform.runLater(() -> {
                    catalog.clear();
                    catalog.putAll(originalCatalog);
                    if (duration != null && oldDuration != null) duration.setValue(oldDuration);
                    if (redshift != null && oldRedshift != null) redshift.setValue(oldRedshift);
                    if (limit != null && oldLimit != null) limit.setValue(oldLimit);
                    setText(zMin, oldZMin); setText(zMax, oldZMax);
                    setText(raMin, oldRaMin); setText(raMax, oldRaMax);
                    setText(decMin, oldDecMin); setText(decMax, oldDecMax);
                });
            });
        }

        private void attachResetButton(Parent card) {
            Button reset = findButton(card, Set.of("ripristina filtri", "reset filters"));
            if (reset != null) reset.addEventHandler(ActionEvent.ACTION, event -> clearSelections());
        }

        @SuppressWarnings("unchecked")
        private ChoiceBox<String> choiceField(String name) {
            Object value = readField(page, name);
            return value instanceof ChoiceBox<?> box ? (ChoiceBox<String>) box : null;
        }

        private TextField textField(String name) {
            Object value = readField(page, name);
            return value instanceof TextField field ? field : null;
        }
    }

    /* ---------------- Small helpers ---------------- */

    private static Object readField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText();
    }

    private static void setText(TextField field, String value) {
        if (field != null && value != null) field.setText(value);
    }

    private static Button findButton(Node root, Set<String> texts) {
        if (root instanceof Button button) {
            String text = button.getText() == null ? "" : button.getText().trim().toLowerCase(Locale.ROOT);
            if (texts.contains(text)) return button;
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button found = findButton(child, texts);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void setWidth(Region region, double min, double pref, double max) {
        region.setMinWidth(min);
        region.setPrefWidth(pref);
        region.setMaxWidth(max);
        HBox.setHgrow(region, Priority.NEVER);
    }

    private static void setFixedWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private static <T extends Node> T find(Node root, Class<T> type) {
        if (root == null) return null;
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = find(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Node> T findByStyle(Node root, Class<T> type, String styleClass) {
        if (root == null) return null;
        if (type.isInstance(root) && root.getStyleClass().contains(styleClass)) return type.cast(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findByStyle(child, type, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }
}
