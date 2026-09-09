from pathlib import Path
import re

ROOT = Path('.')

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def write(path, text):
    (ROOT / path).write_text(text, encoding='utf-8')

def replace_once(path, old, new):
    text = read(path)
    if old not in text:
        raise RuntimeError(f'Pattern non trovato in {path}: {old[:120]!r}')
    write(path, text.replace(old, new, 1))

def replace_between(path, start, end, replacement):
    text = read(path)
    a = text.find(start)
    if a < 0:
        raise RuntimeError(f'Start marker non trovato in {path}: {start!r}')
    b = text.find(end, a)
    if b < 0:
        raise RuntimeError(f'End marker non trovato in {path}: {end!r}')
    write(path, text[:a] + replacement + text[b:])

def ensure_import(path, anchor, imp):
    text = read(path)
    if imp in text:
        return
    if anchor not in text:
        raise RuntimeError(f'Import anchor non trovato in {path}')
    write(path, text.replace(anchor, anchor + imp, 1))

# ---------------------------------------------------------------------------
# Home: l'intera card e' cliccabile; il testo azione resta solo un affordance.
# ---------------------------------------------------------------------------
write('src/main/java/it/casiraghi/swiftbat/ui/HomePage.java', r'''package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.ui.components.BlackHoleHeroPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class HomePage extends ScrollPane {
    public HomePage(Runnable openExplorer, Runnable openSky, Runnable openCompare, Runnable openInfo) {
        getStyleClass().addAll("page-scroll", "home-page-scroll");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setContent(buildContent(openExplorer, openSky, openCompare, openInfo));
    }

    private Node buildContent(Runnable openExplorer, Runnable openSky, Runnable openCompare, Runnable openInfo) {
        VBox page = new VBox(24);
        page.getStyleClass().addAll("page-content", "home-content");
        page.setPadding(new Insets(28, 34, 42, 34));

        HBox hero = new HBox(28);
        hero.getStyleClass().add("event-horizon-hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(34, 34, 34, 38));
        hero.setMinHeight(430);

        VBox copy = new VBox(16);
        copy.setAlignment(Pos.CENTER_LEFT);
        copy.setMaxWidth(690);
        Label kicker = UiFactory.label("SWIFT / BAT  ·  LIVE DATA", "home-kicker");
        Label title = UiFactory.wrappedLabel("SwiftBAT\nExplorer", "home-title");
        Label subtitle = UiFactory.wrappedLabel(
                "Esplora i Gamma-Ray Burst dal catalogo al cielo. Curve di luce, dati FITS e coordinate celesti in un'unica app.",
                "home-subtitle");
        subtitle.setMaxWidth(620);

        HBox actions = new HBox(11);
        Button explore = UiFactory.button("Esplora i GRB  →", "primary-button");
        explore.getStyleClass().add("home-primary-action");
        explore.setOnAction(event -> openExplorer.run());
        Button sky = UiFactory.button("Apri la mappa celeste", "secondary-button");
        sky.setOnAction(event -> openSky.run());
        actions.getChildren().addAll(explore, sky);

        HBox trust = new HBox(12,
                microPill("● Online"),
                microPill("1 s binning"),
                microPill("DAT + FITS"));
        copy.getChildren().addAll(kicker, title, subtitle, actions, trust);
        HBox.setHgrow(copy, Priority.ALWAYS);

        BlackHoleHeroPane graphic = new BlackHoleHeroPane();
        graphic.setMinWidth(300);
        graphic.setPrefWidth(470);
        HBox.setHgrow(graphic, Priority.ALWAYS);
        hero.getChildren().addAll(copy, graphic);

        HBox quickActions = new HBox(14);
        quickActions.getChildren().addAll(
                actionCard("✦", "Esplora", "Cerca un evento e apri curve, dati e metadati.", "Apri catalogo", openExplorer),
                actionCard("◎", "Mappa celeste", "Guarda i GRB sulla Mollweide o sulla sfera 3D.", "Esplora il cielo", openSky),
                actionCard("⇄", "Confronta", "Sovrapponi due eventi già aperti nella sessione.", "Confronta eventi", openCompare));
        for (Node node : quickActions.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);

        HBox lower = new HBox(14);
        VBox workflow = new VBox(14);
        workflow.getStyleClass().add("simple-panel");
        workflow.setPadding(new Insets(22));
        workflow.getChildren().addAll(
                UiFactory.label("Tre passaggi, niente file manuali", "section-title-compact"),
                simpleStep("01", "Scegli un GRB", "Cerca nome o Trigger ID."),
                simpleStep("02", "Aprilo", "L'app recupera e interpreta i prodotti Swift/BAT online."),
                simpleStep("03", "Esplora", "Passa da curva, 3D, tabelle, metadati e mappa celeste."));
        HBox.setHgrow(workflow, Priority.ALWAYS);

        VBox info = new VBox(14);
        info.getStyleClass().add("simple-panel");
        info.setPadding(new Insets(22));
        Label infoTitle = UiFactory.label("Serve una spiegazione?", "section-title-compact");
        Label infoText = UiFactory.wrappedLabel(
                "Le schermate mantengono il dato originale e affiancano spiegazioni brevi per trigger, rate, errori, FRACEXP, FITS, RA, DEC e T90.",
                "simple-panel-text");
        Button infoButton = UiFactory.button("Apri info e guida", "ghost-button");
        infoButton.setOnAction(event -> openInfo.run());
        info.getChildren().addAll(infoTitle, infoText, infoButton);
        HBox.setHgrow(info, Priority.ALWAYS);
        lower.getChildren().addAll(workflow, info);

        page.getChildren().addAll(hero, quickActions, lower);
        return page;
    }

    private VBox actionCard(String glyph, String title, String text, String action, Runnable runnable) {
        VBox card = new VBox(10);
        card.getStyleClass().add("home-action-card");
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) runnable.run();
        });
        Label icon = UiFactory.label(glyph, "home-action-icon");
        Label titleLabel = UiFactory.label(title, "home-action-title");
        Label textLabel = UiFactory.wrappedLabel(text, "home-action-text");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        HBox actionHint = new HBox(5,
                UiFactory.label(action, "home-link-button"),
                UiFactory.label("→", "home-link-button"));
        actionHint.setMouseTransparent(true);
        card.getChildren().addAll(icon, titleLabel, textLabel, spacer, actionHint);
        return card;
    }

    private HBox simpleStep(String number, String title, String detail) {
        HBox row = new HBox(13);
        row.setAlignment(Pos.CENTER_LEFT);
        Label numberLabel = UiFactory.label(number, "simple-step-number");
        VBox copy = new VBox(3,
                UiFactory.label(title, "simple-step-title"),
                UiFactory.wrappedLabel(detail, "simple-step-text"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        row.getChildren().addAll(numberLabel, copy);
        return row;
    }

    private Label microPill(String text) {
        return UiFactory.label(text, "home-micro-pill");
    }
}
''')

# ---------------------------------------------------------------------------
# Tabelle: X nelle intestazioni, chip + per ripristinare, persistenza per key.
# ---------------------------------------------------------------------------
write('src/main/java/it/casiraghi/swiftbat/ui/TablePreferences.java', r'''package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/** Preferenze persistenti per visibilita/larghezza colonne e convenzioni di allineamento. */
public final class TablePreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(TablePreferences.class);
    private static final String NAME_KEY = TablePreferences.class.getName() + ".columnName";
    private static final String INSTALLED_KEY = TablePreferences.class.getName() + ".installed";

    private TablePreferences() {}

    /**
     * Installa intestazioni con X e restituisce la barra dei campi nascosti.
     * Il chiamante puo ignorare il valore di ritorno per mantenere compatibilita.
     */
    public static FlowPane install(TableView<?> table, String tableKey) {
        FlowPane hiddenBar = new FlowPane(6, 6);
        hiddenBar.getStyleClass().add("hidden-column-bar");
        hiddenBar.setVisible(false);
        hiddenBar.setManaged(false);
        if (table == null || tableKey == null) return hiddenBar;

        table.setTableMenuButtonVisible(false);
        Runnable apply = () -> {
            for (TableColumn<?, ?> column : table.getColumns()) installColumn(column, tableKey, table, hiddenBar);
            refreshHiddenBar(table, hiddenBar);
        };
        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<?, ?>>) change -> apply.run());
        Platform.runLater(apply);

        MenuItem reset = new MenuItem(I18n.t("Ripristina colonne"));
        reset.setOnAction(event -> {
            try { PREFS.node(tableKey).clear(); } catch (Exception ignored) {}
            for (TableColumn<?, ?> column : table.getColumns()) resetColumn(column);
            refreshHiddenBar(table, hiddenBar);
        });
        ContextMenu menu = table.getContextMenu();
        if (menu == null) menu = new ContextMenu();
        menu.getItems().add(reset);
        table.setContextMenu(menu);
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> {
            reset.setText(I18n.t("Ripristina colonne"));
            refreshHiddenBar(table, hiddenBar);
        });
        return hiddenBar;
    }

    private static void installColumn(TableColumn<?, ?> column, String tableKey,
                                      TableView<?> table, FlowPane hiddenBar) {
        String name = columnName(column);
        String id = columnId(name);
        String installToken = tableKey + "|" + id;
        if (installToken.equals(column.getProperties().get(INSTALLED_KEY))) return;
        column.getProperties().put(INSTALLED_KEY, installToken);
        column.getProperties().put(NAME_KEY, name);

        Preferences node = PREFS.node(tableKey);
        column.setVisible(node.getBoolean(id + ".visible", true));
        double savedWidth = node.getDouble(id + ".width", -1);
        if (savedWidth > 40) column.setPrefWidth(savedWidth);

        Tooltip inheritedTooltip = null;
        if (column.getGraphic() instanceof Label oldLabel) inheritedTooltip = oldLabel.getTooltip();
        Label label = new Label(I18n.t(name));
        label.getStyleClass().add("table-header-label");
        label.setMaxWidth(Double.MAX_VALUE);
        if (inheritedTooltip != null) label.setTooltip(inheritedTooltip);
        HBox.setHgrow(label, Priority.ALWAYS);

        Button close = new Button("×");
        close.getStyleClass().add("column-close-button");
        close.setFocusTraversable(false);
        close.setOnAction(event -> column.setVisible(false));
        Tooltip.install(close, UiFactory.quickTooltip(I18n.t("Nascondi colonna")));

        HBox header = new HBox(5, label, close);
        header.getStyleClass().add("closable-column-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);
        column.setText("");
        column.setGraphic(header);
        I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> label.setText(I18n.t(name)));

        column.visibleProperty().addListener((obs, oldValue, newValue) -> {
            node.putBoolean(id + ".visible", newValue);
            refreshHiddenBar(table, hiddenBar);
        });
        column.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() > 40) node.putDouble(id + ".width", newValue.doubleValue());
        });
        for (TableColumn<?, ?> child : column.getColumns()) installColumn(child, tableKey + "." + id, table, hiddenBar);
    }

    private static void refreshHiddenBar(TableView<?> table, FlowPane hiddenBar) {
        if (table == null || hiddenBar == null) return;
        List<TableColumn<?, ?>> leaves = new ArrayList<>();
        for (TableColumn<?, ?> column : table.getColumns()) collectLeaves(column, leaves);
        hiddenBar.getChildren().clear();
        for (TableColumn<?, ?> column : leaves) {
            if (column.isVisible()) continue;
            String name = columnName(column);
            Button restore = new Button("+ " + I18n.t(name));
            restore.getStyleClass().add("hidden-column-chip");
            restore.setOnAction(event -> column.setVisible(true));
            hiddenBar.getChildren().add(restore);
        }
        boolean hasHidden = !hiddenBar.getChildren().isEmpty();
        hiddenBar.setVisible(hasHidden);
        hiddenBar.setManaged(hasHidden);
    }

    private static void collectLeaves(TableColumn<?, ?> column, List<TableColumn<?, ?>> leaves) {
        if (column.getColumns().isEmpty()) {
            leaves.add(column);
            return;
        }
        for (TableColumn<?, ?> child : column.getColumns()) collectLeaves(child, leaves);
    }

    private static void resetColumn(TableColumn<?, ?> column) {
        column.setVisible(true);
        for (TableColumn<?, ?> child : column.getColumns()) resetColumn(child);
    }

    private static String columnName(TableColumn<?, ?> column) {
        Object saved = column.getProperties().get(NAME_KEY);
        if (saved instanceof String text && !text.isBlank()) return text;
        String text = column.getText();
        if ((text == null || text.isBlank()) && column.getGraphic() instanceof Label label) text = label.getText();
        if (text == null || text.isBlank()) text = "column" + System.identityHashCode(column);
        return text;
    }

    private static String columnId(String text) {
        return text.replaceAll("[^A-Za-z0-9_]+", "_").toLowerCase(Locale.ROOT);
    }

    public static boolean isNumeric(String text) {
        if (text == null || text.isBlank()) return false;
        String clean = text.trim().replace(',', '.').replace("%", "");
        return clean.matches("[-+]?((\\d+(\\.\\d*)?)|(\\.\\d+))([eE][-+]?\\d+)?")
                || clean.matches("[-+]?\\d+(\\.\\d+)?\\s*[×x]\\s*10\\^?[-+]?\\d+");
    }

    public static void alignCell(TableCell<?, ?> cell, String value) {
        cell.setAlignment(isNumeric(value) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    }
}
''')

# ---------------------------------------------------------------------------
# Multi-select: resta aperto finche' non sono di nuovo selezionate tutte le voci.
# ---------------------------------------------------------------------------
write('src/main/java/it/casiraghi/swiftbat/ui/MultiSelectMenuButton.java', r'''package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Dropdown multi-selezione con checkbox; se tutte le opzioni sono attive equivale a "Tutti". */
public final class MultiSelectMenuButton extends MenuButton {
    private final String allLabel;
    private final List<String> values;
    private final List<CheckMenuItem> items = new ArrayList<>();
    private Runnable changeListener = () -> {};
    private boolean internal;

    public MultiSelectMenuButton(String allLabel, List<String> values) {
        this.allLabel = allLabel;
        this.values = List.copyOf(values);
        getStyleClass().add("choice-box-modern");
        setMaxWidth(Double.MAX_VALUE);
        for (String value : values) {
            CheckMenuItem item = new CheckMenuItem(I18n.t(value));
            item.setSelected(true);
            item.setOnAction(event -> {
                if (internal) return;
                refreshText();
                changeListener.run();
                if (!isAllSelected()) {
                    Platform.runLater(() -> {
                        if (getScene() != null && !isShowing()) show();
                    });
                }
            });
            items.add(item);
            getItems().add(item);
        }
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> refreshLanguage());
        refreshText();
    }

    public void setOnSelectionChanged(Runnable listener) {
        changeListener = listener == null ? () -> {} : listener;
    }

    public Set<String> selectedValues() {
        Set<String> selected = new LinkedHashSet<>();
        for (int i = 0; i < items.size(); i++) if (items.get(i).isSelected()) selected.add(values.get(i));
        return Collections.unmodifiableSet(selected);
    }

    public boolean isAllSelected() {
        return items.stream().allMatch(CheckMenuItem::isSelected);
    }

    public void selectAll() {
        internal = true;
        items.forEach(item -> item.setSelected(true));
        internal = false;
        refreshText();
        changeListener.run();
        hide();
    }

    private void refreshLanguage() {
        for (int i = 0; i < items.size(); i++) items.get(i).setText(I18n.t(values.get(i)));
        refreshText();
    }

    private void refreshText() {
        int count = (int) items.stream().filter(CheckMenuItem::isSelected).count();
        if (count == items.size()) {
            setText(I18n.t(allLabel));
        } else if (count == 0) {
            setText(I18n.language() == I18n.Language.IT ? "Nessuno" : "None");
        } else if (count == 1) {
            for (int i = 0; i < items.size(); i++) if (items.get(i).isSelected()) setText(I18n.t(values.get(i)));
        } else {
            setText(count + (I18n.language() == I18n.Language.IT ? " selezionati" : " selected"));
        }
    }
}
''')

# ---------------------------------------------------------------------------
# Compare: tema scuro, prefisso GRB protetto, ghost completion, TAB e popup 4x15.
# ---------------------------------------------------------------------------
write('src/main/java/it/casiraghi/swiftbat/ui/ComparePage.java', r'''package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.SummaryItem;
import it.casiraghi.swiftbat.model.TabularData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ComparePage extends javafx.scene.layout.BorderPane {
    private static final int MAX_SUGGESTIONS = 15;
    private static final int VISIBLE_SUGGESTIONS = 4;
    private static final String GHOST_KEY = ComparePage.class.getName() + ".ghostSuggestion";

    private final ObservableMap<String, GrbData> sessionData;
    private final Consumer<CatalogEntry> loadRequest;
    private final ComboBox<String> first = new ComboBox<>();
    private final ComboBox<String> second = new ComboBox<>();
    private final GhostHint firstGhost = ghostHint();
    private final GhostHint secondGhost = ghostHint();
    private final Label firstMatches = UiFactory.label("", "compare-match-count");
    private final Label secondMatches = UiFactory.label("", "compare-match-count");
    private final CheckBox normalize = new CheckBox(I18n.t("Normalizza ogni curva sul proprio picco"));
    private final StackPane content = new StackPane();
    private final Map<String, CatalogEntry> catalogEntries = new LinkedHashMap<>();
    private final Set<String> requestedLoads = new LinkedHashSet<>();
    private List<String> availableNames = List.of();

    public ComparePage(ObservableMap<String, GrbData> sessionData, Consumer<CatalogEntry> loadRequest) {
        this.sessionData = sessionData;
        this.loadRequest = loadRequest == null ? ignored -> {} : loadRequest;
        getStyleClass().add("page-root");
        setPadding(new Insets(28, 34, 34, 34));
        setTop(buildHeader());
        setCenter(content);
        javafx.scene.layout.BorderPane.setMargin(content, new Insets(20, 0, 0, 0));
        configureSearch(first, firstMatches, firstGhost, second);
        configureSearch(second, secondMatches, secondGhost, first);
        sessionData.addListener((javafx.collections.MapChangeListener<String, GrbData>) change -> {
            requestedLoads.remove(change.getKey());
            refreshChoices();
            refreshComparison();
        });
        first.valueProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        second.valueProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        normalize.selectedProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        refreshChoices();
    }

    public void setCatalog(List<CatalogEntry> entries) {
        catalogEntries.clear();
        if (entries != null) {
            for (CatalogEntry entry : entries) {
                if (entry != null && entry.grbName() != null) {
                    catalogEntries.put(entry.grbName().toUpperCase(Locale.ROOT), entry);
                }
            }
        }
        refreshChoices();
    }

    private Node buildHeader() {
        VBox header = new VBox(14);
        VBox copy = new VBox(5, UiFactory.label("Confronta", "page-title"));

        for (ComboBox<String> combo : List.of(first, second)) {
            combo.getStyleClass().addAll("choice-box-modern", "compare-combo");
            combo.setPrefWidth(235);
            combo.setMinWidth(215);
            combo.setVisibleRowCount(VISIBLE_SUGGESTIONS);
        }
        normalize.getStyleClass().add("modern-check");

        VBox firstBox = new VBox(3, searchStack(first, firstGhost), firstMatches);
        VBox secondBox = new VBox(3, searchStack(second, secondGhost), secondMatches);
        HBox controls = new HBox(10);
        controls.getStyleClass().add("compare-control-bar");
        controls.setPadding(new Insets(13, 15, 13, 15));
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getChildren().addAll(
                UiFactory.label("Evento A", "toolbar-label"), firstBox,
                UiFactory.label("vs", "compare-vs"),
                UiFactory.label("Evento B", "toolbar-label"), secondBox,
                UiFactory.spacer(), normalize);
        header.getChildren().addAll(copy, controls);
        return header;
    }

    private StackPane searchStack(ComboBox<String> combo, GhostHint ghost) {
        StackPane stack = new StackPane(combo, ghost.box());
        StackPane.setAlignment(combo, Pos.CENTER_LEFT);
        StackPane.setAlignment(ghost.box(), Pos.CENTER_LEFT);
        ghost.box().setMouseTransparent(true);
        ghost.box().setPadding(new Insets(0, 42, 0, 13));
        stack.setMinWidth(215);
        stack.setPrefWidth(235);
        return stack;
    }

    private static GhostHint ghostHint() {
        Label prefix = new Label();
        prefix.getStyleClass().add("compare-ghost-text");
        prefix.setOpacity(0.0);
        Label suffix = new Label();
        suffix.getStyleClass().add("compare-ghost-text");
        HBox box = new HBox(0, prefix, suffix);
        box.setAlignment(Pos.CENTER_LEFT);
        return new GhostHint(box, prefix, suffix);
    }

    private void configureSearch(ComboBox<String> combo, Label counter, GhostHint ghost, ComboBox<String> other) {
        combo.setEditable(true);
        combo.getEditor().setText("GRB");
        combo.getEditor().setPromptText("");
        combo.getEditor().setTextFormatter(new TextFormatter<String>(change -> {
            String next = change.getControlNewText() == null ? "" : change.getControlNewText().toUpperCase(Locale.ROOT);
            if (!next.startsWith("GRB")) return null;
            String suffix = next.substring(3);
            if (!suffix.matches("[0-9A-Z]*")) return null;
            change.setText(change.getText().toUpperCase(Locale.ROOT));
            return change;
        }));
        combo.getEditor().textProperty().addListener((obs, oldValue, raw) -> updateSuggestions(combo, counter, ghost, other, raw));
        combo.getEditor().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                Platform.runLater(() -> combo.getEditor().positionCaret(Math.max(3, combo.getEditor().getCaretPosition())));
                updateSuggestions(combo, counter, ghost, other, combo.getEditor().getText());
            } else {
                combo.hide();
            }
        });
        combo.getEditor().setOnMouseClicked(event -> Platform.runLater(() -> {
            if (combo.getEditor().getCaretPosition() < 3) combo.getEditor().positionCaret(3);
        }));
        combo.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.HOME) {
                combo.getEditor().positionCaret(3);
                event.consume();
                return;
            }
            if (event.getCode() == KeyCode.LEFT && combo.getEditor().getCaretPosition() <= 3
                    && combo.getEditor().getSelection().getLength() == 0) {
                event.consume();
                return;
            }
            if (event.getCode() == KeyCode.TAB) {
                Object value = combo.getProperties().get(GHOST_KEY);
                if (value instanceof String suggestion && !suggestion.isBlank()) {
                    commitSelection(combo, suggestion);
                    event.consume();
                }
            }
        });
        combo.setOnAction(event -> {
            String value = combo.getValue();
            if (value != null && availableNames.contains(value)) commitSelection(combo, value);
        });
    }

    private void updateSuggestions(ComboBox<String> combo, Label counter, GhostHint ghost,
                                   ComboBox<String> other, String raw) {
        String normalized = normalize(raw);
        String excluded = exactSelection(other);
        List<String> matches = availableNames.stream()
                .filter(name -> excluded == null || !name.equals(excluded))
                .filter(name -> name.startsWith(normalized))
                .toList();
        I18n.setText(counter, matches.size() + " corrispondenze", matches.size() + " matches");

        String ghostValue = matches.stream()
                .filter(name -> name.length() > normalized.length())
                .findFirst().orElse("");
        combo.getProperties().put(GHOST_KEY, ghostValue);
        ghost.prefix().setText(normalized);
        ghost.suffix().setText(ghostValue.isBlank() ? "" : ghostValue.substring(normalized.length()));
        ghost.box().setVisible(combo.getEditor().isFocused() && !ghostValue.isBlank());

        if (matches.size() <= MAX_SUGGESTIONS && !matches.isEmpty() && normalized.length() > 3) {
            combo.setItems(FXCollections.observableArrayList(matches));
            if (combo.getEditor().isFocused() && !combo.isShowing()) Platform.runLater(combo::show);
        } else {
            combo.hide();
            combo.setItems(FXCollections.observableArrayList());
        }
    }

    private void commitSelection(ComboBox<String> combo, String value) {
        if (value == null || !availableNames.contains(value)) return;
        combo.setValue(value);
        combo.getEditor().setText(value);
        combo.getEditor().positionCaret(value.length());
        combo.hide();
        combo.getProperties().put(GHOST_KEY, "");
        ensureLoaded(value);
        refreshChoices();
        refreshComparison();
    }

    private String normalize(String raw) {
        String text = raw == null ? "GRB" : raw.trim().toUpperCase(Locale.ROOT);
        if (text.isBlank()) return "GRB";
        return text.startsWith("GRB") ? text : "GRB" + text;
    }

    private String exactSelection(ComboBox<String> combo) {
        if (combo == null) return null;
        String editor = normalize(combo.getEditor().getText());
        if (availableNames.contains(editor)) return editor;
        String value = combo.getValue();
        return value != null && availableNames.contains(value) ? value : null;
    }

    private void refreshChoices() {
        List<String> names = new ArrayList<>(catalogEntries.isEmpty() ? sessionData.keySet() : catalogEntries.keySet());
        names.replaceAll(name -> name.toUpperCase(Locale.ROOT));
        names.sort(Comparator.reverseOrder());
        availableNames = List.copyOf(names);
        updateSuggestions(first, firstMatches, firstGhost, second, first.getEditor().getText());
        updateSuggestions(second, secondMatches, secondGhost, first, second.getEditor().getText());
    }

    private String selected(ComboBox<String> combo) {
        return exactSelection(combo);
    }

    private void ensureLoaded(String name) {
        if (name == null || sessionData.containsKey(name) || requestedLoads.contains(name)) return;
        CatalogEntry entry = catalogEntries.get(name);
        if (entry == null) return;
        requestedLoads.add(name);
        loadRequest.accept(entry);
    }

    private void refreshComparison() {
        String aName = selected(first);
        String bName = selected(second);
        if (aName != null) ensureLoaded(aName);
        if (bName != null) ensureLoaded(bName);
        if (aName != null && aName.equals(bName)) {
            showEmpty("Scegli due GRB diversi", "Choose two different GRBs");
            return;
        }
        if (aName == null || bName == null) {
            showEmpty("Apri o seleziona almeno due GRB", "Open or select at least two GRBs");
            return;
        }
        GrbData a = sessionData.get(aName);
        GrbData b = sessionData.get(bName);
        if (a == null || b == null) {
            showEmpty("Caricamento dei GRB selezionati…", "Loading selected GRBs…");
            return;
        }
        content.getChildren().setAll(buildComparison(a, b));
        I18n.localizeTree(content);
    }

    private void showEmpty(String italian, String english) {
        VBox empty = new VBox(13);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        Label title = UiFactory.label("", "empty-title");
        I18n.setText(title, italian, english);
        empty.getChildren().addAll(UiFactory.label("⇄", "empty-icon"), title);
        content.getChildren().setAll(empty);
    }

    private Node buildComparison(GrbData a, GrbData b) {
        VBox page = new VBox(16);
        FlowPane cards = new FlowPane(12, 12);
        cards.getChildren().addAll(
                compareMetric("Picco", value(a, "PEAK_RATE"), value(b, "PEAK_RATE"), a.grbName(), b.grbName()),
                compareMetric("Tempo del picco", value(a, "PEAK_TIME"), value(b, "PEAK_TIME"), a.grbName(), b.grbName()),
                compareMetric("Picco / errore", value(a, "PEAK_SNR"), value(b, "PEAK_SNR"), a.grbName(), b.grbName()),
                compareMetric("Durezza proxy", value(a, "HARDNESS_PROXY"), value(b, "HARDNESS_PROXY"), a.grbName(), b.grbName()),
                compareMetric("Esposizione completa", value(a, "FULL_EXPOSURE_FRACTION"), value(b, "FULL_EXPOSURE_FRACTION"), a.grbName(), b.grbName()));

        NumberAxis xAxis = new NumberAxis(-60, 60, 10);
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(I18n.t("Tempo dal trigger (s)"));
        yAxis.setLabel(I18n.t(normalize.isSelected() ? "Rate normalizzato" : "Rate totale (count/s)"));
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setTitle("±60 s");
        chart.getStyleClass().add("lightcurve-chart");
        chart.getData().add(seriesFor(a, normalize.isSelected()));
        chart.getData().add(seriesFor(b, normalize.isSelected()));
        VBox.setVgrow(chart, Priority.ALWAYS);

        VBox chartCard = UiFactory.card("Confronto temporale", "", chart);
        page.getChildren().addAll(cards, chartCard);
        return page;
    }

    private VBox compareMetric(String title, String a, String b, String nameA, String nameB) {
        VBox card = new VBox(8);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(230);
        card.getChildren().addAll(
                UiFactory.label(title, "metric-eyebrow"),
                UiFactory.label(nameA + "  " + a, "compare-value-a"),
                UiFactory.label(nameB + "  " + b, "compare-value-b"));
        return card;
    }

    private XYChart.Series<Number, Number> seriesFor(GrbData data, boolean normalized) {
        TabularData table = data.asciiData().isEmpty() ? data.fitsData() : data.asciiData();
        int timeIndex = table.indexOf("TIME_FROM_TRIGGER_CENTER_S");
        int rateIndex = table.indexOf(data.asciiData().isEmpty() ? "RATE" : "RATE_15_350_KEV");
        List<double[]> points = new ArrayList<>();
        double peak = 0;
        for (List<String> row : table.rows()) {
            double time = parse(row, timeIndex);
            double rate = parse(row, rateIndex);
            if (Double.isFinite(time) && Double.isFinite(rate) && Math.abs(time) <= 60) {
                points.add(new double[]{time, rate});
                peak = Math.max(peak, Math.abs(rate));
            }
        }
        double scale = normalized && peak > 0 ? peak : 1;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(data.grbName());
        for (double[] point : points) series.getData().add(new XYChart.Data<>(point[0], point[1] / scale));
        return series;
    }

    private String value(GrbData data, String key) {
        SummaryItem item = data.summaryByKey().get(key);
        return item == null ? "n.d." : DisplayFormat.summary(key, item);
    }

    private double parse(List<String> row, int index) {
        if (index < 0 || index >= row.size()) return Double.NaN;
        try { return Double.parseDouble(row.get(index)); }
        catch (Exception ignored) { return Double.NaN; }
    }

    private record GhostHint(HBox box, Label prefix, Label suffix) {}
}
''')

# ---------------------------------------------------------------------------
# MainView: catalogo completo al Compare + caricamento silenzioso dei suggeriti.
# ---------------------------------------------------------------------------
ensure_import('src/main/java/it/casiraghi/swiftbat/ui/MainView.java', 'import java.util.Map;\n', 'import java.util.Set;\nimport java.util.concurrent.ConcurrentHashMap;\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/MainView.java',
             '    private final ObservableMap<String, GrbData> sessionData = FXCollections.observableHashMap();\n',
             '    private final ObservableMap<String, GrbData> sessionData = FXCollections.observableHashMap();\n    private final Set<String> compareLoadsInFlight = ConcurrentHashMap.newKeySet();\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/MainView.java',
             '        comparePage = new ComparePage(sessionData);\n',
             '        comparePage = new ComparePage(sessionData, this::loadCompareGrb);\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/MainView.java',
             '            explorerPage.setCatalog(entries, false);\n            skyMapPage.setBaseCatalog(entries);\n            populationPage.setCatalog(entries);\n',
             '            explorerPage.setCatalog(entries, false);\n            skyMapPage.setBaseCatalog(entries);\n            populationPage.setCatalog(entries);\n            comparePage.setCatalog(entries);\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/MainView.java',
             '            explorerPage.setCatalog(fallback, true);\n            skyMapPage.setBaseCatalog(fallback);\n            populationPage.setCatalog(fallback);\n',
             '            explorerPage.setCatalog(fallback, true);\n            skyMapPage.setBaseCatalog(fallback);\n            populationPage.setCatalog(fallback);\n            comparePage.setCatalog(fallback);\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/MainView.java',
             '    private void loadGrb(CatalogEntry entry, boolean forceRefresh) {\n',
             r'''    private void loadCompareGrb(CatalogEntry entry) {
        if (entry == null || sessionData.containsKey(entry.grbName())
                || !compareLoadsInFlight.add(entry.grbName())) return;
        setConnection(I18n.dynamic("Caricamento confronto…", "Loading comparison…"), "status-neutral");
        Task<GrbData> task = new Task<>() {
            @Override protected GrbData call() throws Exception {
                return grbService.load(entry, false, update -> { });
            }
        };
        task.setOnSucceeded(event -> {
            compareLoadsInFlight.remove(entry.grbName());
            GrbData data = task.getValue();
            if (data != null) sessionData.put(data.grbName(), data);
            setConnection("Online", "status-online");
        });
        task.setOnFailed(event -> {
            compareLoadsInFlight.remove(entry.grbName());
            setConnection(I18n.dynamic("Errore caricamento confronto", "Comparison load failed"), "status-warning");
        });
        DOWNLOAD_EXECUTOR.execute(task);
    }

    private void loadGrb(CatalogEntry entry, boolean forceRefresh) {
''')

# ---------------------------------------------------------------------------
# Explorer: preset T90/z, area spiegazione collassabile e colonne per singolo GRB.
# ---------------------------------------------------------------------------
old_extra = r'''        GridPane extraGrid = new GridPane();
        extraGrid.setHgap(8);
        extraGrid.setVgap(6);
        extraGrid.add(UiFactory.label("T90 (s)", "filter-label"), 0, 0);
        extraGrid.add(UiFactory.label("Redshift z", "filter-label"), 1, 0);
        extraGrid.add(filterRange(t90MinFilter, t90MaxFilter), 0, 1);
        extraGrid.add(filterRange(redshiftMinFilter, redshiftMaxFilter), 1, 1);
        var extraFirst = new javafx.scene.layout.ColumnConstraints();
        extraFirst.setPercentWidth(50);
        extraFirst.setHgrow(Priority.ALWAYS);
        var extraSecond = new javafx.scene.layout.ColumnConstraints();
        extraSecond.setPercentWidth(50);
        extraSecond.setHgrow(Priority.ALWAYS);
        extraGrid.getColumnConstraints().addAll(extraFirst, extraSecond);
'''
new_extra = r'''        ChoiceBox<String> t90Preset = new ChoiceBox<>(FXCollections.observableArrayList(
                "Tutti", "≤ 2 s", "2–10 s", "10–50 s", "50–100 s", "> 100 s", "Manuale…"));
        ChoiceBox<String> redshiftPreset = new ChoiceBox<>(FXCollections.observableArrayList(
                "Tutti", "0–1", "1–2", "2–3", "3–4", "4–6", "> 6", "Manuale…"));
        for (ChoiceBox<String> preset : List.of(t90Preset, redshiftPreset)) {
            preset.getStyleClass().add("choice-box-modern");
            preset.setMaxWidth(Double.MAX_VALUE);
            UiFactory.autoTooltip(preset);
        }
        t90Preset.setValue("Tutti");
        redshiftPreset.setValue("Tutti");
        HBox t90Manual = filterRange(t90MinFilter, t90MaxFilter);
        HBox redshiftManual = filterRange(redshiftMinFilter, redshiftMaxFilter);
        t90Manual.setVisible(false); t90Manual.setManaged(false);
        redshiftManual.setVisible(false); redshiftManual.setManaged(false);
        t90Preset.valueProperty().addListener((obs, oldValue, value) ->
                applyRangePreset(value, t90MinFilter, t90MaxFilter, t90Manual, true));
        redshiftPreset.valueProperty().addListener((obs, oldValue, value) ->
                applyRangePreset(value, redshiftMinFilter, redshiftMaxFilter, redshiftManual, false));

        GridPane extraGrid = new GridPane();
        extraGrid.setHgap(8);
        extraGrid.setVgap(6);
        extraGrid.add(UiFactory.label("T90 (s)", "filter-label"), 0, 0);
        extraGrid.add(UiFactory.label("Redshift z", "filter-label"), 1, 0);
        extraGrid.add(new VBox(5, t90Preset, t90Manual), 0, 1);
        extraGrid.add(new VBox(5, redshiftPreset, redshiftManual), 1, 1);
        var extraFirst = new javafx.scene.layout.ColumnConstraints();
        extraFirst.setPercentWidth(50);
        extraFirst.setHgrow(Priority.ALWAYS);
        var extraSecond = new javafx.scene.layout.ColumnConstraints();
        extraSecond.setPercentWidth(50);
        extraSecond.setHgrow(Priority.ALWAYS);
        extraGrid.getColumnConstraints().addAll(extraFirst, extraSecond);
'''
replace_once('src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java', old_extra, new_extra)
replace_once('src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java',
             '            redshiftMinFilter.clear();\n            redshiftMaxFilter.clear();\n            applyCatalogFilters();\n',
             '            redshiftMinFilter.clear();\n            redshiftMaxFilter.clear();\n            t90Preset.setValue("Tutti");\n            redshiftPreset.setValue("Tutti");\n            applyCatalogFilters();\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java',
             '    private static Double optionalNumber(TextField field) {\n',
             r'''    private void applyRangePreset(String preset, TextField minimum, TextField maximum,
                                  HBox manualBox, boolean t90) {
        String value = preset == null ? "Tutti" : preset;
        boolean manual = "Manuale…".equals(value);
        manualBox.setVisible(manual);
        manualBox.setManaged(manual);
        if (manual) return;
        minimum.clear();
        maximum.clear();
        if (t90) {
            switch (value) {
                case "≤ 2 s" -> maximum.setText("2");
                case "2–10 s" -> { minimum.setText("2"); maximum.setText("10"); }
                case "10–50 s" -> { minimum.setText("10"); maximum.setText("50"); }
                case "50–100 s" -> { minimum.setText("50"); maximum.setText("100"); }
                case "> 100 s" -> minimum.setText("100");
                default -> { }
            }
        } else {
            switch (value) {
                case "0–1" -> { minimum.setText("0"); maximum.setText("1"); }
                case "1–2" -> { minimum.setText("1"); maximum.setText("2"); }
                case "2–3" -> { minimum.setText("2"); maximum.setText("3"); }
                case "3–4" -> { minimum.setText("3"); maximum.setText("4"); }
                case "4–6" -> { minimum.setText("4"); maximum.setText("6"); }
                case "> 6" -> minimum.setText("6");
                default -> { }
            }
        }
        scheduleCatalogFilters();
    }

    private static Double optionalNumber(TextField field) {
''')

new_data_method = r'''    private Node buildDataWorkspace(GrbData data) {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(18));

        ChoiceBox<String> sourceChoice = new ChoiceBox<>();
        if (!data.asciiData().isEmpty()) sourceChoice.getItems().add("ASCII — quattro bande");
        if (!data.fitsData().isEmpty()) sourceChoice.getItems().add("FITS — un canale e qualità");
        sourceChoice.getStyleClass().add("choice-box-modern");
        sourceChoice.setMinWidth(190);
        sourceChoice.setPrefWidth(215);
        sourceChoice.setMaxWidth(250);
        UiFactory.autoTooltip(sourceChoice);
        if (!sourceChoice.getItems().isEmpty()) sourceChoice.setValue(sourceChoice.getItems().get(0));

        TextField filter = new TextField();
        filter.setPromptText("Filtra le righe per valore testuale…");
        filter.getStyleClass().add("search-field");
        filter.setMinWidth(170);
        filter.setPrefWidth(360);
        filter.setMaxWidth(Double.MAX_VALUE);

        Button exportAscii = UiFactory.button("ASCII → Excel", "ghost-button");
        exportAscii.getStyleClass().add("excel-export-button");
        exportAscii.setDisable(data.asciiData().isEmpty());
        exportAscii.setOnAction(event -> exportExcel(data, true));
        Button exportFits = UiFactory.button("FITS + metadati → Excel", "ghost-button");
        exportFits.getStyleClass().add("excel-export-button");
        exportFits.setDisable(data.fitsData().isEmpty());
        exportFits.setOnAction(event -> exportExcel(data, false));

        HBox toolbar = new HBox(10, sourceChoice, filter, exportAscii, exportFits);
        toolbar.getStyleClass().add("data-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filter, Priority.ALWAYS);
        pane.setTop(toolbar);
        BorderPane.setMargin(toolbar, new Insets(0, 0, 12, 0));

        TableView<ObservableList<String>> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        FlowPane hiddenColumns = TablePreferences.install(table, "explorer.data." + data.grbName());

        ChoiceBox<String> fieldChoice = new ChoiceBox<>();
        fieldChoice.getStyleClass().add("choice-box-modern");
        fieldChoice.setMinWidth(220);
        fieldChoice.setPrefWidth(300);
        fieldChoice.setMaxWidth(Double.MAX_VALUE);
        UiFactory.autoTooltip(fieldChoice);

        VBox explanation = new VBox(12);
        explanation.getStyleClass().add("field-explanation-panel");
        explanation.setPadding(new Insets(16));
        VBox side = new VBox(10,
                UiFactory.label("Campo da spiegare", "filter-label"), fieldChoice, explanation);
        side.setPadding(new Insets(4, 0, 4, 10));
        side.setMinWidth(300);
        side.setPrefWidth(365);
        side.setMaxWidth(410);
        ScrollPane explanationScroll = scrollableSide(side);
        explanationScroll.setMinWidth(300);
        explanationScroll.setPrefWidth(365);
        explanationScroll.setMaxWidth(410);

        ToggleButton explanationToggle = new ToggleButton(I18n.t("Mostra spiegazione"));
        explanationToggle.getStyleClass().addAll("ghost-button", "help-toggle");
        HBox tableTools = new HBox(8, hiddenColumns, UiFactory.spacer(), explanationToggle);
        tableTools.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(hiddenColumns, Priority.ALWAYS);
        VBox tableArea = new VBox(7, tableTools, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        BorderPane content = new BorderPane(tableArea);
        content.setMinWidth(0);

        explanationToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            explanationToggle.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
            if (selected) {
                content.setRight(explanationScroll);
                BorderPane.setMargin(explanationScroll, new Insets(0, 0, 0, 10));
            } else {
                content.setRight(null);
            }
        });
        I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                explanationToggle.setText(I18n.t(explanationToggle.isSelected()
                        ? "Nascondi spiegazione" : "Mostra spiegazione")));

        Runnable refresh = () -> {
            TabularData selected = sourceChoice.getValue() != null && sourceChoice.getValue().startsWith("FITS")
                    ? data.fitsData() : data.asciiData();
            populateTable(table, selected, data, filter.getText());
            String previous = fieldChoice.getValue();
            fieldChoice.setItems(FXCollections.observableArrayList(selected.headers()));
            if (previous != null && selected.headers().contains(previous)) fieldChoice.setValue(previous);
            else if (!selected.headers().isEmpty()) fieldChoice.setValue(selected.headers().get(0));
        };
        sourceChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        filter.textProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        fieldChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                showFieldExplanation(explanation, data.definition(newValue), newValue));
        refresh.run();

        pane.setCenter(content);
        return pane;
    }

'''
replace_between('src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java',
                '    private Node buildDataWorkspace(GrbData data) {\n',
                '    private Node buildMetadata(GrbData data) {\n', new_data_method)

new_metadata_method = r'''    private Node buildMetadata(GrbData data) {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(18));
        TextField search = new TextField();
        search.setPromptText("Cerca keyword, valore, HDU o commento…");
        search.getStyleClass().add("search-field");
        pane.setTop(search);
        BorderPane.setMargin(search, new Insets(0, 0, 12, 0));

        TableView<MetadataItem> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        FlowPane hiddenColumns = TablePreferences.install(table, "explorer.metadata." + data.grbName());
        TableColumn<MetadataItem, String> hdu = metadataColumn("HDU", item -> item.hduName(), 95);
        hdu.setMinWidth(75);
        TableColumn<MetadataItem, String> keyword = metadataColumn("Keyword", item -> item.keyword(), 145);
        keyword.setMinWidth(115);
        TableColumn<MetadataItem, String> value = metadataColumn("Valore", item -> item.value(), 260);
        value.setMinWidth(165);
        TableColumn<MetadataItem, String> comment = metadataColumn("Commento originale", item -> item.comment(), 440);
        comment.setMinWidth(240);
        table.getColumns().addAll(hdu, keyword, value, comment);

        FilteredList<MetadataItem> filtered = new FilteredList<>(FXCollections.observableArrayList(data.metadata()), ignored -> true);
        table.setItems(filtered);
        search.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.toLowerCase(Locale.ROOT).trim();
            filtered.setPredicate(item -> query.isBlank()
                    || (item.hduName() + " " + item.keyword() + " " + item.value() + " " + item.comment())
                    .toLowerCase(Locale.ROOT).contains(query));
        });

        ChoiceBox<String> metadataField = new ChoiceBox<>();
        metadataField.getStyleClass().add("choice-box-modern");
        metadataField.setItems(FXCollections.observableArrayList(
                data.metadata().stream().map(MetadataItem::keyword).filter(valueText -> valueText != null && !valueText.isBlank())
                        .distinct().toList()));
        metadataField.setMinWidth(220);
        metadataField.setPrefWidth(300);
        metadataField.setMaxWidth(Double.MAX_VALUE);
        UiFactory.autoTooltip(metadataField);

        VBox explanation = new VBox(12);
        explanation.setPadding(new Insets(16));
        explanation.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        showMetadataIntro(explanation);
        VBox side = new VBox(10,
                UiFactory.label("Campo metadata", "filter-label"), metadataField, explanation);
        side.setPadding(new Insets(4, 0, 4, 10));
        side.setMinWidth(300);
        side.setPrefWidth(380);
        side.setMaxWidth(420);
        ScrollPane sideScroll = scrollableSide(side);
        sideScroll.setMinWidth(300);
        sideScroll.setPrefWidth(380);
        sideScroll.setMaxWidth(420);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, item) -> {
            if (item == null) return;
            if (!item.keyword().equals(metadataField.getValue())) metadataField.setValue(item.keyword());
            FieldDefinition definition = findMetadataDefinition(data, item.keyword());
            showMetadataExplanation(explanation, item, definition);
        });
        metadataField.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selectedField) -> {
            if (selectedField == null) return;
            data.metadata().stream().filter(item -> selectedField.equals(item.keyword())).findFirst().ifPresent(item -> {
                table.getSelectionModel().select(item);
                table.scrollTo(item);
                showMetadataExplanation(explanation, item, findMetadataDefinition(data, item.keyword()));
            });
        });
        if (!metadataField.getItems().isEmpty()) metadataField.setValue(metadataField.getItems().get(0));

        ToggleButton explanationToggle = new ToggleButton(I18n.t("Mostra spiegazione"));
        explanationToggle.getStyleClass().addAll("ghost-button", "help-toggle");
        HBox tableTools = new HBox(8, hiddenColumns, UiFactory.spacer(), explanationToggle);
        tableTools.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(hiddenColumns, Priority.ALWAYS);
        VBox tableArea = new VBox(7, tableTools, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        BorderPane content = new BorderPane(tableArea);
        content.setMinWidth(0);
        explanationToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            explanationToggle.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
            if (selected) {
                content.setRight(sideScroll);
                BorderPane.setMargin(sideScroll, new Insets(0, 0, 0, 10));
            } else content.setRight(null);
        });
        I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                explanationToggle.setText(I18n.t(explanationToggle.isSelected()
                        ? "Nascondi spiegazione" : "Mostra spiegazione")));

        pane.setCenter(content);
        return pane;
    }

'''
replace_between('src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java',
                '    private Node buildMetadata(GrbData data) {\n',
                '    private Node buildUnderstand(GrbData data) {\n', new_metadata_method)

# ---------------------------------------------------------------------------
# Population: filtri sulla stessa riga + barra X/+ nei GRB inclusi.
# ---------------------------------------------------------------------------
ensure_import('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java',
              'import javafx.scene.layout.BorderPane;\n', 'import javafx.scene.layout.FlowPane;\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java',
             '    private final TableView<PopulationEvent> resultTable = new TableView<>();\n',
             '    private final TableView<PopulationEvent> resultTable = new TableView<>();\n    private FlowPane resultColumnBar;\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java',
             '        configureChart();\n        configureTable();\n\n        resultTabs.getStyleClass().add("main-tabs");\n',
             '        configureChart();\n        configureTable();\n        VBox includedTable = new VBox(6, resultColumnBar, resultTable);\n        VBox.setVgrow(resultTable, Priority.ALWAYS);\n\n        resultTabs.getStyleClass().add("main-tabs");\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java',
             '                new Tab("Distribuzioni del campione", distributionPane()),\n                new Tab("GRB inclusi", resultTable));\n',
             '                new Tab("Distribuzioni del campione", distributionPane()),\n                new Tab("GRB inclusi", includedTable));\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java',
             '        resultTable.getStyleClass().add("data-table");\n        TablePreferences.install(resultTable, "population.results");\n',
             '        resultTable.getStyleClass().add("data-table");\n        resultColumnBar = TablePreferences.install(resultTable, "population.results");\n')

old_filter_block = r'''        FlowPane primary = new FlowPane(9, 7);
        primary.getStyleClass().add("population-filter-grid");
        primary.getChildren().addAll(
                filterGroup("Durata T90", "Classe temporale", duration, 185),
                filterGroup("Redshift", "", redshiftControl, 245),
                filterGroup("Finestra temporale", "", windowControl, 230),
                filterGroup("Campione massimo", "GRB più recenti dopo i filtri", limit, 150));
        primary.setMinWidth(650);
        primary.setPrefWrapLength(690);

'''
replace_once('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java', old_filter_block, '')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java',
             '        box.setMinWidth(185);\n        box.setPrefWidth(215);\n',
             '        box.setMinWidth(125);\n        box.setPrefWidth(150);\n')
old_exposure = r'''        VBox exposureIntro = new VBox(2,
                UiFactory.label("Qualità della copertura FRACEXP", "population-section-title"));
        VBox exposureBox = new VBox(7, exposureIntro, exposureControls);
        exposureBox.setAlignment(Pos.TOP_LEFT);
        exposureBox.getStyleClass().addAll("population-filter-section", "population-filter-side");
        exposureBox.setMinWidth(430);
        exposureBox.setPrefWidth(500);
        exposureBox.setMaxWidth(Double.MAX_VALUE);

        HBox topFilters = new HBox(12, primary, exposureBox);
        topFilters.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(primary, Priority.ALWAYS);
        HBox.setHgrow(exposureBox, Priority.ALWAYS);
'''
new_exposure = r'''        VBox exposureIntro = new VBox(2,
                UiFactory.label("Qualità FRACEXP", "population-section-title"));
        VBox exposureBox = new VBox(6, exposureIntro, exposureControls);
        exposureBox.setAlignment(Pos.TOP_LEFT);
        exposureBox.getStyleClass().addAll("population-filter-section", "population-filter-side", "population-fracexp-inline");
        exposureBox.setMinWidth(300);
        exposureBox.setPrefWidth(350);
        exposureBox.setMaxWidth(Double.MAX_VALUE);

        VBox durationGroup = filterGroup("Durata T90", "", duration, 170);
        VBox redshiftGroup = filterGroup("Redshift", "", redshiftControl, 220);
        VBox windowGroup = filterGroup("Finestra temporale", "", windowControl, 215);
        VBox limitGroup = filterGroup("Campione massimo", "", limit, 135);
        HBox topFilters = new HBox(8, durationGroup, redshiftGroup, windowGroup, exposureBox, limitGroup);
        topFilters.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(exposureBox, Priority.ALWAYS);
'''
replace_once('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java', old_exposure, new_exposure)

# ---------------------------------------------------------------------------
# Spectroscopy: filtri ordinati e niente spiegazione di una riga nelle viste normali.
# ---------------------------------------------------------------------------
old_controls = r'''        Node intervalRadios = radioChoice(List.of(Interval.values()), intervalChoice);
        Node modelRadios = radioChoice(List.of(AUTOMATIC_MODEL, POWER_LAW_MODEL, CUTOFF_MODEL), modelChoice);
        GridPane controls = responsiveGrid(1120, 3,
                controlBox("Intervallo del fit", intervalRadios, ""),
                controlBox("Modello del fit", modelRadios, ""),
                sourceControlBox(source));
        controls.getStyleClass().add("spectroscopy-controls");
'''
new_controls = r'''        Node intervalRadios = radioChoice(List.of(Interval.values()), intervalChoice);
        Node modelRadios = radioChoice(List.of(AUTOMATIC_MODEL, POWER_LAW_MODEL, CUTOFF_MODEL), modelChoice);
        VBox intervalBox = controlBox("Intervallo del fit", intervalRadios, "", 300);
        VBox modelBox = controlBox("Modello del fit", modelRadios, "", 410);
        VBox sourceBox = sourceControlBox(source);
        HBox controls = new HBox(10, intervalBox, modelBox, sourceBox);
        controls.getStyleClass().add("spectroscopy-controls");
        controls.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(intervalBox, Priority.ALWAYS);
        HBox.setHgrow(modelBox, Priority.ALWAYS);
        HBox.setHgrow(sourceBox, Priority.ALWAYS);
'''
replace_once('src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java', old_controls, new_controls)

for path, block in [
('src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java', r'''        Label explanation = UiFactory.wrappedLabel(
                "Asse X = energia dei fotoni. Asse Y = log₁₀ del flusso fotonico differenziale previsto dal fit. "
                        + "La linea è il modello ricostruito, non una serie di misure grezze.",
                "card-subtitle");
        explanation.setMinHeight(Region.USE_PREF_SIZE);
        explanation.setMaxWidth(Double.MAX_VALUE);

'''),
('src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java', r'''        Label explanation = UiFactory.wrappedLabel(
                "Asse X = banda energetica; asse Y = energia ricevuta per unità di area e di tempo. "
                        + "Una barra più alta indica un flusso maggiore; i limiti al 90% sono nella tabella e nel tooltip.",
                "card-subtitle");
        explanation.setMinHeight(Region.USE_PREF_SIZE);
        explanation.setMaxWidth(Double.MAX_VALUE);

''')]:
    replace_once(path, block, '')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java',
             '        card.getChildren().add(UiFactory.collapsibleHelp("", explanation));\n', '',)
replace_once('src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java',
             '        card.getChildren().add(UiFactory.collapsibleHelp("", explanation));\n', '',)

new_time_energy = r'''    private Node buildTimeEnergyTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(14));
        TimeEnergyHeatmapPane heatmap = new TimeEnergyHeatmapPane();
        heatmap.setData(grbData.asciiData());

        ChoiceBox<String> window = new ChoiceBox<>(FXCollections.observableArrayList(
                "±20 s", "±60 s", "±120 s", "Intera osservazione"));
        window.setValue("±60 s");
        window.getStyleClass().add("choice-box-modern");
        window.valueProperty().addListener((obs, oldValue, value) ->
                heatmap.setHalfWindowSeconds(timeWindowSeconds(value)));

        Button fullscreen = UiFactory.button("Schermo intero", "primary-button");
        fullscreen.setDisable(grbData.asciiData().isEmpty());
        fullscreen.setOnAction(event -> openTimeEnergyFullscreen(timeWindowSeconds(window.getValue())));
        Button threeD = UiFactory.button("Apri vista 3D dei rate", "primary-button");
        threeD.setDisable(grbData.asciiData().isEmpty());
        threeD.setOnAction(event -> openTimeEnergy3D());

        FlowPane controls = new FlowPane(10, 10);
        controls.getChildren().add(controlBox("Finestra temporale", window, "", 620));
        controls.setAlignment(Pos.BOTTOM_LEFT);

        FlowPane chartActions = new FlowPane(8, 8);
        chartActions.getStyleClass().add("spectroscopy-chart-actions");
        chartActions.getChildren().addAll(fullscreen, threeD);
        VBox chartCard = new VBox(9,
                UiFactory.label("Mappa tempo–energia dei rate", "card-title"),
                chartActions,
                heatmap);
        chartCard.getStyleClass().addAll("card", "time-energy-card");
        chartCard.setPadding(new Insets(14));
        VBox.setVgrow(heatmap, Priority.ALWAYS);
        box.getChildren().addAll(controls, chartCard);
        return box;
    }

'''
replace_between('src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java',
                '    private Node buildTimeEnergyTab() {\n',
                '    private double timeWindowSeconds(String value) {\n', new_time_energy)

# ---------------------------------------------------------------------------
# I18n: composizione di stringhe dinamiche + traduzioni mancanti viste negli screen.
# ---------------------------------------------------------------------------
replace_once('src/main/java/it/casiraghi/swiftbat/ui/I18n.java',
             r'''        String translated = translateDirect(text, EN, IT);
        if (!translated.equals(text)) return translated;
        if (IT.containsKey(text)) return text;
        return requiresTranslation(text) ? "[Missing English translation]" : text;
''',
             r'''        String translated = translateDirect(text, EN, IT);
        if (!translated.equals(text)) return translated;
        if (IT.containsKey(text)) return text;
        String composed = translateComposedEnglish(text);
        if (!composed.equals(text)) return composed;
        return requiresTranslation(text) ? "[Missing English translation]" : text;
''')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/I18n.java',
             '    public static String english(String italian) {\n',
             r'''    private static String translateComposedEnglish(String text) {
        String result = text;
        java.util.List<Map.Entry<String, String>> entries = new java.util.ArrayList<>(EN.entrySet());
        entries.sort((left, right) -> Integer.compare(right.getKey().length(), left.getKey().length()));
        for (Map.Entry<String, String> entry : entries) {
            String italian = entry.getKey();
            String english = entry.getValue();
            if (italian == null || english == null || italian.equals(english) || italian.length() < 3) continue;
            if (result.contains(italian)) result = result.replace(italian, english);
        }
        return result;
    }

    public static String english(String italian) {
''')
translation_block = r'''        // UX / localizzazione 1.3.0 - stringhe dinamiche e renderer
        put("RATE massimo", "maximum RATE");
        put("rispetto al trigger", "relative to trigger");
        put("proxy alte / basse energie", "high / low energy proxy");
        put("in memoria nella sessione", "in session memory");
        put("Short (T90 ≤ 2 s)", "Short (T90 ≤ 2 s)");
        put("Long (T90 > 2 s)", "Long (T90 > 2 s)");
        put("z non disponibile", "z unavailable");
        put("Binning temporale", "Time binning");
        put("Banda complessiva", "Overall energy band");
        put("Intervallo temporale", "Time range");
        put("Righe ASCII", "ASCII rows");
        put("Righe FITS", "FITS rows");
        put("Campo da spiegare", "Field to explain");
        put("Campo metadata", "Metadata field");
        put("Nascondi colonna", "Hide column");
        put("Manuale…", "Manual…");
        put("Qualità FRACEXP", "FRACEXP quality");
        put("Campione massimo", "Maximum sample");
        put("Intervallo del fit", "Fit interval");
        put("Modello del fit", "Fit model");
        put("Fonte ufficiale BAT", "Official BAT source");
        put("Evento A", "Event A");
        put("Evento B", "Event B");
        put("Confronto temporale", "Time comparison");
        put("Normalizza ogni curva sul proprio picco", "Normalize each curve to its own peak");
        put("Caricamento confronto…", "Loading comparison…");
        put("Errore caricamento confronto", "Comparison load failed");
        put("Scegli due GRB diversi", "Choose two different GRBs");
        put("Apri o seleziona almeno due GRB", "Open or select at least two GRBs");
        put("Caricamento dei GRB selezionati…", "Loading selected GRBs…");
        put("Mappa non disponibile: servono i quattro canali ASCII.", "Map unavailable: the four ASCII channels are required.");
        put("Banda", "Band");
        put("Centro bin", "Bin center");
        put("Larghezza banda", "Band width");
        put("Fluttuazione negativa", "Negative fluctuation");
        put("Rate circa zero", "Rate near zero");
        put("Rate positivo", "Positive rate");
        put("Trascina: ruota prospettiva   ·   Rotella: zoom   ·   Doppio clic: centra", "Drag: rotate perspective   ·   Wheel: zoom   ·   Double-click: center");
        put("Trascina: prospettiva · Rotella: zoom · Doppio clic: centra", "Drag: perspective · Wheel: zoom · Double-click: center");
        put("Modello spettrale non disponibile.", "Spectral model unavailable.");
        put("Curve di luce 3D per banda energetica", "3D light curves by energy band");
        put("Profondità = banda energetica ASCII; non distanza spaziale.", "Depth = ASCII energy band; not spatial distance.");
        put("Asse X = tempo dal trigger; asse Y = rate; profondità = quattro bande energetiche. Ogni linea è una curva di luce a bin di 1 secondo: la vista non rappresenta una distanza nello spazio né uno spettro continuo. Trascina per ruotare e usa la rotella per lo zoom.",
                "X axis = time from trigger; Y axis = rate; depth = the four energy bands. Each line is a 1-second-binned light curve: the view does not represent spatial distance or a continuous spectrum. Drag to rotate and use the wheel to zoom.");
        put("Curva — la linea arancione rappresenta la funzione spettrale ricostruita dal fit ufficiale BAT selezionato. Non è una successione di punti grezzi misurati dal rivelatore.", "Curve — the orange line is the spectral function reconstructed from the selected official BAT fit. It is not a sequence of raw detector measurements.");
        put("Asse X — mostra l'energia dei fotoni in keV, da 15 a 150 keV nella vista corrente.", "X axis — photon energy in keV, from 15 to 150 keV in the current view.");
        put("Asse Y — mostra log₁₀ N(E), cioè il logaritmo del flusso fotonico differenziale. Valori negativi sono perfettamente normali e indicano N(E) < 1 nelle unità riportate.", "Y axis — log₁₀ N(E), the logarithm of the differential photon flux. Negative values are normal and mean N(E) < 1 in the displayed units.");
        put("Forma — la pendenza della curva descrive come il contributo previsto dal modello cambia con l'energia. Un andamento più ripido indica una diminuzione più rapida verso le energie elevate.", "Shape — the curve slope shows how the model contribution changes with energy. A steeper trend means a faster decrease toward higher energies.");
        put("Confronto — questa è la stessa funzione visualizzata nella vista 3D: il 3D aggiunge soltanto prospettiva grafica e non introduce una nuova grandezza fisica.", "Comparison — this is the same function shown in the 3D view: 3D only adds graphical perspective and does not introduce a new physical quantity.");
        put("Da ricordare — il grafico visualizza il modello ricostruito dai parametri del fit BAT; non deriva dalla somma delle quattro curve di luce ASCII.", "Remember — the chart shows the model reconstructed from BAT fit parameters; it is not derived by summing the four ASCII light curves.");
        put("Modello — la curva arancione rappresenta la stessa funzione spettrale ricostruita mostrata nella vista 2D. Non sono aggiunti nuovi punti osservativi.", "Model — the orange curve is the same reconstructed spectral function shown in 2D. No new observed points are added.");
        put("Assi — X indica l'energia dei fotoni in keV; Y indica log₁₀ N(E), il logaritmo del flusso fotonico differenziale previsto dal fit.", "Axes — X is photon energy in keV; Y is log₁₀ N(E), the logarithm of the differential photon flux predicted by the fit.");
        put("Forma della curva — la pendenza mostra come il contributo del modello diminuisce o varia passando verso energie più elevate.", "Curve shape — the slope shows how the model contribution decreases or changes toward higher energies.");
        put("Profondità — il piano arretrato e i collegamenti servono soltanto alla prospettiva. Non rappresentano tempo, distanza, intensità o una terza variabile fisica.", "Depth — the rear plane and connectors are only for perspective. They do not represent time, distance, intensity, or a third physical variable.");
        put("Interazione — trascina per cambiare la prospettiva interna, usa la rotellina per lo zoom e fai doppio clic per ricentrare la vista.", "Interaction — drag to change perspective, use the wheel to zoom, and double-click to recenter the view.");
        put("Interpretazione — zoom e prospettiva cambiano soltanto la visualizzazione: energia, N(E) e parametri del fit rimangono invariati.", "Interpretation — zoom and perspective only change the visualization: energy, N(E), and fit parameters remain unchanged.");
        put("Barre — ogni barra rappresenta il flusso energetico integrato pubblicato da BAT per una specifica banda energetica.", "Bars — each bar is the integrated energy flux published by BAT for a specific energy band.");
        put("Asse X — separa le bande di energia riportate dal catalogo, così da confrontare rapidamente dove il modello concentra più flusso.", "X axis — separates the catalog energy bands so you can quickly compare where the model carries more flux.");
        put("Asse Y — misura il flusso energetico in erg cm⁻² s⁻¹. Una barra più alta indica un flusso integrato maggiore nella banda corrispondente.", "Y axis — energy flux in erg cm⁻² s⁻¹. A taller bar means a larger integrated flux in that band.");
        put("Intervalli al 90% — i limiti di confidenza ufficiali restano disponibili nella tabella e nel tooltip delle barre; l'altezza mostra il valore centrale pubblicato.", "90% intervals — official confidence limits remain available in the table and bar tooltip; bar height shows the published central value.");
        put("Confronto — questo istogramma e la curva spettrale descrivono due aspetti dello stesso fit ufficiale, ma non sono quattro curve di luce sommate.", "Comparison — this histogram and the spectral curve describe two aspects of the same official fit; they are not four summed light curves.");
        put("Da ricordare — le barre derivano dai prodotti spettroscopici BAT/XSPEC e non dai rate ASCII a bin di un secondo.", "Remember — the bars come from BAT/XSPEC spectral products, not from 1-second ASCII rates.");
        put("Barre — sono gli stessi valori dell'istogramma 2D, disposti in prospettiva per facilitare il confronto visivo tra le bande energetiche.", "Bars — these are the same values as the 2D histogram, arranged in perspective to make energy-band comparison easier.");
        put("Assi — X identifica la banda energetica; l'altezza della barra rappresenta il flusso energetico integrato pubblicato da BAT.", "Axes — X identifies the energy band; bar height is the integrated energy flux published by BAT.");
        put("Scala — i valori sono mostrati in unità di 10⁻¹² erg cm⁻² s⁻¹ per mantenere una scala numerica leggibile senza alterare i rapporti tra le bande.", "Scale — values are shown in units of 10⁻¹² erg cm⁻² s⁻¹ to keep the numeric scale readable without changing ratios between bands.");
        put("Profondità — serve esclusivamente a separare graficamente le barre. Non è una distanza e non aggiunge una nuova grandezza fisica.", "Depth — only separates the bars graphically. It is not a distance and does not add a new physical quantity.");
        put("Interazione — trascina per cambiare prospettiva, usa la rotellina per lo zoom e il pulsante Centra vista per tornare all'inquadratura iniziale.", "Interaction — drag to change perspective, use the wheel to zoom, and Center view to return to the initial framing.");
        put("Da ricordare — i limiti al 90% restano consultabili nella tabella 2D; la vista 3D mostra i valori centrali del fit ufficiale.", "Remember — 90% limits remain available in the 2D table; the 3D view shows the official fit central values.");
        put("Assi — X rappresenta il tempo rispetto al trigger t = 0; Y separa le quattro bande energetiche BAT.", "Axes — X is time relative to trigger t = 0; Y separates the four BAT energy bands.");
        put("Colore — arancio indica un rate netto positivo, blu una fluttuazione negativa dopo la sottrazione del fondo; i toni scuri indicano valori vicini a zero.", "Color — orange indicates positive net rate, blue a negative fluctuation after background subtraction; dark tones are values near zero.");
        put("Dettaglio — spostando il mouse sulla mappa puoi leggere banda energetica, centro del bin, rate e larghezza della banda nel punto osservato.", "Detail — move the pointer over the map to read energy band, bin center, rate, and band width at that point.");
        put("Scala temporale — ogni cella deriva dai rate ASCII a bin di 1 secondo e la finestra visualizzata è la stessa scelta nella scheda Spettroscopia.", "Time scale — each cell comes from 1-second ASCII rates and the displayed window is the one selected in the Spectroscopy tab.");
        put("Da ricordare — questa mappa descrive i rate BAT nel tempo: non è un fit XSPEC e non converte direttamente i conteggi in flusso fisico.", "Remember — this map shows BAT rates over time: it is not an XSPEC fit and does not directly convert counts into physical flux.");
'''
replace_once('src/main/java/it/casiraghi/swiftbat/ui/I18n.java',
             '        // STRICT_I18N_EXPLORER_BATCH\n', translation_block + '        // STRICT_I18N_EXPLORER_BATCH\n')

# ---------------------------------------------------------------------------
# Renderer canvas/Swing: localizzazione diretta invece di stringhe italiane fisse.
# ---------------------------------------------------------------------------
ensure_import('src/main/java/it/casiraghi/swiftbat/ui/components/TimeEnergyHeatmapPane.java',
              'import it.casiraghi.swiftbat.model.TabularData;\n', 'import it.casiraghi.swiftbat.ui.I18n;\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/components/TimeEnergyHeatmapPane.java',
             '        heightProperty().addListener(ignored -> draw());\n',
             '        heightProperty().addListener(ignored -> draw());\n        I18n.languageProperty().addListener((obs, oldValue, newValue) -> draw());\n')
for old, new in [
('graphics.fillText("Mappa non disponibile: servono i quattro canali ASCII.", 24, 64);', 'graphics.fillText(I18n.t("Mappa non disponibile: servono i quattro canali ASCII."), 24, 64);'),
('graphics.fillText("Tempo dal trigger (s)", plotLeft + plotWidth / 2 - 54, height - 38);', 'graphics.fillText(I18n.t("Tempo dal trigger (s)"), plotLeft + plotWidth / 2 - 54, height - 38);'),
('drawLegendItem(graphics, plotLeft, legendY, Color.web("#3b82f6"), "Fluttuazione negativa");', 'drawLegendItem(graphics, plotLeft, legendY, Color.web("#3b82f6"), I18n.t("Fluttuazione negativa"));'),
('drawLegendItem(graphics, plotLeft + legendColumnWidth, legendY, Color.web("#101a2b"), "Rate circa zero");', 'drawLegendItem(graphics, plotLeft + legendColumnWidth, legendY, Color.web("#101a2b"), I18n.t("Rate circa zero"));'),
('drawLegendItem(graphics, plotLeft + 2 * legendColumnWidth, legendY, Color.web("#ff9f43"), "Rate positivo");', 'drawLegendItem(graphics, plotLeft + 2 * legendColumnWidth, legendY, Color.web("#ff9f43"), I18n.t("Rate positivo"));')]:
    replace_once('src/main/java/it/casiraghi/swiftbat/ui/components/TimeEnergyHeatmapPane.java', old, new)
replace_once('src/main/java/it/casiraghi/swiftbat/ui/components/TimeEnergyHeatmapPane.java',
             r'''        hoverCard.setText(String.format(Locale.ITALIAN,
                "Banda: %s%nCentro bin: %.3f s%nRate: %.5g count/s%nLarghezza banda: %.0f keV",
                band.label(), nearest.time(), nearest.rates()[bandIndex], band.widthKeV()));
''',
             r'''        hoverCard.setText(I18n.t("Banda") + ": " + band.label()
                + "\n" + I18n.t("Centro bin") + ": " + String.format(Locale.ROOT, "%.3f s", nearest.time())
                + "\nRate: " + String.format(Locale.ROOT, "%.5g count/s", nearest.rates()[bandIndex])
                + "\n" + I18n.t("Larghezza banda") + ": " + String.format(Locale.ROOT, "%.0f keV", band.widthKeV()));
''')

ensure_import('src/main/java/it/casiraghi/swiftbat/ui/components/Java2DWaterfallPanel.java',
              'package it.casiraghi.swiftbat.ui.components;\n\n', 'import it.casiraghi.swiftbat.ui.I18n;\n\n')
for old, new in [
('String xLabel = presentation.xAxisLabel();', 'String xLabel = I18n.t(presentation.xAxisLabel());'),
('g.drawString(presentation.yAxisLabel(),', 'g.drawString(I18n.t(presentation.yAxisLabel()),'),
('int labelWidth = g.getFontMetrics().stringWidth(presentation.depthAxisLabel());', 'String depthLabel = I18n.t(presentation.depthAxisLabel());\n            int labelWidth = g.getFontMetrics().stringWidth(depthLabel);'),
('g.drawString(presentation.depthAxisLabel(), labelX, labelY);', 'g.drawString(depthLabel, labelX, labelY);'),
('String hint = "Trascina: ruota prospettiva   ·   Rotella: zoom   ·   Doppio clic: centra";', 'String hint = I18n.t("Trascina: ruota prospettiva   ·   Rotella: zoom   ·   Doppio clic: centra");'),
('"Tempo: " + VALUE_FORMAT.format(dataset.times()[index]) + " s",', 'I18n.t("Tempo") + ": " + VALUE_FORMAT.format(dataset.times()[index]) + " s",'),
('presentation.valueLabel() + ": " + VALUE_FORMAT.format(dataset.rates()[band][index])', 'I18n.t(presentation.valueLabel()) + ": " + VALUE_FORMAT.format(dataset.rates()[band][index])')]:
    replace_once('src/main/java/it/casiraghi/swiftbat/ui/components/Java2DWaterfallPanel.java', old, new)

ensure_import('src/main/java/it/casiraghi/swiftbat/ui/components/Java2DGroupedBarPanel.java',
              'package it.casiraghi.swiftbat.ui.components;\n\n', 'import it.casiraghi.swiftbat.ui.I18n;\n\n')
for old, new in [
('String xLabel = dataset.xAxisLabel();', 'String xLabel = I18n.t(dataset.xAxisLabel());'),
('String yLabel = dataset.valueLabel();', 'String yLabel = I18n.t(dataset.valueLabel());'),
('g.drawString(dataset.depthAxisLabel(), (float) zEnd.getX() - 20, (float) zEnd.getY() - 10);', 'g.drawString(I18n.t(dataset.depthAxisLabel()), (float) zEnd.getX() - 20, (float) zEnd.getY() - 10);'),
('g.drawString(dataset.groups()[group], (float) point.getX() + 21, (float) point.getY() + 4);', 'g.drawString(I18n.t(dataset.groups()[group]), (float) point.getX() + 21, (float) point.getY() + 4);'),
('g.drawString("Trascina: ruota prospettiva   ·   Rotella: zoom   ·   Doppio clic: centra",', 'g.drawString(I18n.t("Trascina: ruota prospettiva   ·   Rotella: zoom   ·   Doppio clic: centra"),'),
('dataset.groups()[selected.group()],', 'I18n.t(dataset.groups()[selected.group()]),'),
('dataset.xAxisLabel() + ": " + dataset.categories()[selected.category()],', 'I18n.t(dataset.xAxisLabel()) + ": " + I18n.t(dataset.categories()[selected.category()]),'),
('dataset.valueLabel() + ": " + selected.count()', 'I18n.t(dataset.valueLabel()) + ": " + selected.count()')]:
    replace_once('src/main/java/it/casiraghi/swiftbat/ui/components/Java2DGroupedBarPanel.java', old, new)

ensure_import('src/main/java/it/casiraghi/swiftbat/ui/components/SpectralModel3DPane.java',
              'import it.casiraghi.swiftbat.ui.UiFactory;\n', 'import it.casiraghi.swiftbat.ui.I18n;\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/components/SpectralModel3DPane.java',
             '        setBottom(buildFooter());\n        Platform.runLater(() -> syncRendererSize(viewer));\n',
             '        setBottom(buildFooter());\n        I18n.languageProperty().addListener((obs, oldValue, newValue) -> SwingUtilities.invokeLater(renderer::repaint));\n        Platform.runLater(() -> syncRendererSize(viewer));\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/components/SpectralModel3DPane.java',
             '            String text = "Modello spettrale non disponibile.";\n',
             '            String text = I18n.t("Modello spettrale non disponibile.");\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/components/SpectralModel3DPane.java',
             '            String xLabel = "Energia (keV)";\n',
             '            String xLabel = I18n.t("Energia (keV)");\n')
replace_once('src/main/java/it/casiraghi/swiftbat/ui/components/ThreeDChartPane.java',
             '        heightProperty().addListener((observable, oldValue, newValue) -> repaintRenderer());\n',
             '        heightProperty().addListener((observable, oldValue, newValue) -> repaintRenderer());\n        it.casiraghi.swiftbat.ui.I18n.languageProperty().addListener((obs, oldValue, newValue) -> repaintRenderer());\n')

# ---------------------------------------------------------------------------
# CSS: compare scuro, X/+ colonne, ghost, FRACEXP inline.
# ---------------------------------------------------------------------------
css_path = 'src/main/resources/app.css'
css = read(css_path)
css_append = r'''

/* ---------- UX polish 1.3.0 ---------- */
.home-action-card { -fx-cursor: hand; }

.closable-column-header {
    -fx-alignment: center-left;
    -fx-padding: 0 1 0 0;
}
.column-close-button {
    -fx-background-color: transparent;
    -fx-text-fill: #6f7887;
    -fx-font-size: 12px;
    -fx-font-weight: bold;
    -fx-padding: 0 2 0 4;
    -fx-cursor: hand;
}
.column-close-button:hover {
    -fx-text-fill: #ffb15c;
    -fx-background-color: rgba(255,177,92,0.08);
    -fx-background-radius: 7px;
}
.hidden-column-bar {
    -fx-alignment: center-left;
    -fx-padding: 1 0 1 0;
}
.hidden-column-chip {
    -fx-background-color: rgba(255,255,255,0.035);
    -fx-border-color: rgba(255,177,92,0.14);
    -fx-text-fill: #aab2c0;
    -fx-background-radius: 999px;
    -fx-border-radius: 999px;
    -fx-font-size: 9px;
    -fx-padding: 4 8;
    -fx-cursor: hand;
}
.hidden-column-chip:hover {
    -fx-background-color: rgba(255,177,92,0.09);
    -fx-text-fill: #ffd2a0;
}

.compare-combo,
.compare-combo:editable,
.compare-combo .text-field {
    -fx-background-color: #0c111b;
    -fx-control-inner-background: #0c111b;
    -fx-text-fill: #edf1f7;
    -fx-prompt-text-fill: #616b7b;
    -fx-border-color: rgba(255,255,255,0.09);
    -fx-background-radius: 10px;
    -fx-border-radius: 10px;
}
.compare-combo:focused,
.compare-combo .text-field:focused {
    -fx-border-color: rgba(91,220,255,0.55);
}
.compare-combo .arrow-button {
    -fx-background-color: rgba(255,255,255,0.035);
    -fx-background-radius: 0 10px 10px 0;
}
.compare-combo .list-cell {
    -fx-background-color: #0d131e;
    -fx-text-fill: #dfe5ef;
    -fx-padding: 8 10;
}
.compare-combo .list-view {
    -fx-background-color: #0b1019;
    -fx-border-color: rgba(255,255,255,0.10);
}
.compare-ghost-text {
    -fx-text-fill: rgba(147, 164, 190, 0.48);
    -fx-font-size: 13px;
}
.compare-match-count {
    -fx-text-fill: #687487;
    -fx-font-size: 9px;
}

.population-fracexp-inline {
    -fx-padding: 7 8;
}
.population-fracexp-inline .percentage-control {
    -fx-padding: 5 7;
}
.population-fracexp-inline .percentage-field {
    -fx-font-size: 12px;
    -fx-padding: 4 6;
}
.population-fracexp-inline .filter-reset-button {
    -fx-min-width: 25px;
    -fx-min-height: 25px;
    -fx-padding: 2px;
}
'''
if '/* ---------- UX polish 1.3.0 ---------- */' not in css:
    css += css_append
write(css_path, css)

print('Patch UX/localizzazione applicata con successo.')
