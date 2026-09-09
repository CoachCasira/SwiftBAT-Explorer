from pathlib import Path
import re

ROOT = Path('.')


def read(path):
    return (ROOT / path).read_text(encoding='utf-8')


def write(path, text):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError(f'Marker not found for {label}')
    return text.replace(old, new, 1)


# -----------------------------------------------------------------------------
# I18n runtime: IT/EN without changing scientific/raw data values.
# -----------------------------------------------------------------------------
i18n = r'''package it.casiraghi.swiftbat.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public final class I18n {
    public enum Language { IT, EN }

    private static final String ORIGINAL_TEXT = I18n.class.getName() + ".originalText";
    private static final Preferences PREFS = Preferences.userNodeForPackage(I18n.class);
    private static final ObjectProperty<Language> LANGUAGE = new SimpleObjectProperty<>(loadLanguage());
    private static final Map<String, String> EN = new LinkedHashMap<>();

    static {
        put("Esplora", "Explore");
        put("Mappa celeste", "Sky map");
        put("Analisi di popolazione", "Population analysis");
        put("Confronta", "Compare");
        put("Informazioni", "Information");
        put("Fonte ufficiale", "Official source");
        put("Fonte ufficiale BAT", "Official BAT source");
        put("Apri tabella ufficiale ↗", "Open official table ↗");
        put("Connessione…", "Connecting…");
        put("Catalogo…", "Catalog…");
        put("in memoria", "in memory");
        put("Online", "Online");
        put("Offline parziale", "Partial offline");
        put("Aggiorna il catalogo online", "Refresh online catalog");
        put("Apri il catalogo ufficiale", "Open official catalog");
        put("Curva 2D", "2D curve");
        put("Vista 3D", "3D view");
        put("Vista 3D interattiva", "Interactive 3D view");
        put("Dati", "Data");
        put("Metadati", "Metadata");
        put("Guida", "Guide");
        put("Spettroscopia", "Spectroscopy");
        put("Analisi spettroscopica", "Spectral analysis");
        put("Risultati ufficiali", "Official results");
        put("Mappa tempo–energia", "Time–energy map");
        put("Guida scientifica", "Scientific guide");
        put("Intervallo del fit", "Fit interval");
        put("Modello del fit", "Fit model");
        put("Modello indicato da BAT", "Model selected by BAT");
        put("Power law (PL)", "Power law (PL)");
        put("Cutoff power law (CPL)", "Cutoff power law (CPL)");
        put("T100", "T100");
        put("Picco 1 s", "1 s peak");
        put("Analizza il gruppo", "Analyze group");
        put("Annulla", "Cancel");
        put("Ripristina filtri", "Reset filters");
        put("Profilo temporale", "Temporal profile");
        put("Distribuzioni del campione", "Sample distributions");
        put("GRB inclusi", "Included GRBs");
        put("Durata T90", "T90 duration");
        put("Durata", "Duration");
        put("Redshift", "Redshift");
        put("Finestra temporale", "Time window");
        put("Campione massimo", "Maximum sample");
        put("Qualità della copertura FRACEXP", "FRACEXP coverage quality");
        put("Minimo ammesso", "Minimum");
        put("Massimo ammesso", "Maximum");
        put("Filtri avanzati: z e area di cielo", "Advanced filters: z and sky area");
        put("Nascondi filtri avanzati", "Hide advanced filters");
        put("Intervallo redshift z", "Redshift z range");
        put("Ascensione retta RA", "Right ascension RA");
        put("Declinazione DEC", "Declination DEC");
        put("Tutte le durate", "All durations");
        put("Short · T90 ≤ 2 s", "Short · T90 ≤ 2 s");
        put("Long · T90 > 2 s", "Long · T90 > 2 s");
        put("T90 non disponibile", "T90 unavailable");
        put("Con e senza redshift", "With and without redshift");
        put("Solo con redshift", "With redshift only");
        put("Solo senza redshift", "Without redshift only");
        put("Tutte", "All");
        put("Tutti", "All");
        put("Con z", "With z");
        put("Senza z", "Without z");
        put("Solo in cache", "Cached only");
        put("Da scaricare", "To download");
        put("Altri filtri ▾", "More filters ▾");
        put("Nascondi filtri ▴", "Hide filters ▴");
        put("Azzera filtri extra", "Reset extra filters");
        put("Cache locale", "Local cache");
        put("Cerca GRB o Trigger ID…", "Search GRB or Trigger ID…");
        put("Cerca keyword, valore, HDU o commento…", "Search keyword, value, HDU or comment…");
        put("Filtra le righe per valore testuale…", "Filter rows by text value…");
        put("Spiega il campo:", "Explain field:");
        put("ASCII → Excel", "ASCII → Excel");
        put("FITS + metadati → Excel", "FITS + metadata → Excel");
        put("Valore", "Value");
        put("Commento originale", "Original comment");
        put("Come leggere i metadati", "How to read metadata");
        put("Picco", "Peak");
        put("Tempo del picco", "Peak time");
        put("Segnale / errore", "Signal / error");
        put("Esposizione completa", "Full exposure");
        put("Durezza", "Hardness");
        put("Ricarica online", "Reload online");
        put("Schermo intero", "Fullscreen");
        put("Mostra spiegazione", "Show explanation");
        put("Nascondi spiegazione", "Hide explanation");
        put("Assistente di lettura", "Reading assistant");
        put("Nessuna analisi eseguita", "No analysis run");
        put("Evento A", "Event A");
        put("Evento B", "Event B");
        put("Normalizza ogni curva sul proprio picco", "Normalize each curve to its own peak");
        put("Apri almeno due GRB", "Open at least two GRBs");
        put("Confronto temporale", "Temporal comparison");
        put("Tempo dal trigger (s)", "Time from trigger (s)");
        put("Rate normalizzato", "Normalized rate");
        put("Rate totale (count/s)", "Total rate (count/s)");
        put("GRB", "GRB");
        put("Classe", "Class");
        put("Copertura", "Coverage");
        put("Flag qualità", "Quality flag");
        put("Mostra colonne", "Show columns");
        put("Ripristina colonne", "Reset columns");
        put("Seleziona colonne dal menu della tabella. La scelta viene salvata automaticamente.",
                "Select columns from the table menu. Your choice is saved automatically.");
    }

    private I18n() {}

    private static void put(String it, String en) { EN.put(it, en); }

    private static Language loadLanguage() {
        try { return Language.valueOf(PREFS.get("language", "IT")); }
        catch (Exception ignored) { return Language.IT; }
    }

    public static Language language() { return LANGUAGE.get(); }
    public static ObjectProperty<Language> languageProperty() { return LANGUAGE; }

    public static void setLanguage(Language value) {
        Language next = value == null ? Language.IT : value;
        LANGUAGE.set(next);
        PREFS.put("language", next.name());
    }

    public static String t(String text) {
        if (text == null || language() == Language.IT) return text;
        String direct = EN.get(text);
        if (direct != null) return direct;
        // Preserve glyph prefixes used by navigation buttons while translating their label.
        int firstLetter = -1;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) { firstLetter = i; break; }
        }
        if (firstLetter > 0) {
            String tail = text.substring(firstLetter);
            String translated = EN.get(tail);
            if (translated != null) return text.substring(0, firstLetter) + translated;
        }
        return text;
    }

    public static void localizeLabeled(Labeled control) {
        if (control == null) return;
        String original = (String) control.getProperties().get(ORIGINAL_TEXT);
        if (original == null) {
            original = control.getText();
            control.getProperties().put(ORIGINAL_TEXT, original);
        }
        control.setText(t(original));
        if (control.getTooltip() != null) {
            control.getTooltip().setText(t(control.getTooltip().getText()));
        }
    }

    public static <T> void installChoiceBox(ChoiceBox<T> choice) {
        if (choice == null) return;
        StringConverter<T> converter = new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : t(value.toString()); }
            @Override public T fromString(String text) {
                for (T value : choice.getItems()) {
                    if (value != null && (value.toString().equals(text) || t(value.toString()).equals(text))) return value;
                }
                return choice.getValue();
            }
        };
        choice.setConverter(converter);
        LANGUAGE.addListener((obs, oldValue, newValue) -> choice.requestLayout());
    }

    public static <T> void installComboBox(ComboBox<T> combo) {
        if (combo == null) return;
        StringConverter<T> converter = new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : t(value.toString()); }
            @Override public T fromString(String text) {
                for (T value : combo.getItems()) {
                    if (value != null && (value.toString().equals(text) || t(value.toString()).equals(text))) return value;
                }
                return combo.getValue();
            }
        };
        combo.setConverter(converter);
        LANGUAGE.addListener((obs, oldValue, newValue) -> combo.requestLayout());
    }

    public static void localizeTree(Node node) {
        if (node == null) return;
        if (node instanceof Labeled labeled) localizeLabeled(labeled);
        if (node instanceof TextInputControl input) {
            String key = ORIGINAL_TEXT + ".prompt";
            String original = (String) input.getProperties().get(key);
            if (original == null) {
                original = input.getPromptText();
                input.getProperties().put(key, original);
            }
            input.setPromptText(t(original));
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                Object original = tab.getProperties().get(ORIGINAL_TEXT);
                if (original == null) {
                    original = tab.getText();
                    tab.getProperties().put(ORIGINAL_TEXT, original);
                }
                tab.setText(t((String) original));
                if (tab.getContent() != null) localizeTree(tab.getContent());
            }
        }
        if (node instanceof TableView<?> table) {
            for (TableColumn<?, ?> column : table.getColumns()) localizeColumn(column);
        }
        if (node instanceof ChoiceBox<?> choice) choice.requestLayout();
        if (node instanceof ComboBox<?> combo) combo.requestLayout();
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) localizeTree(child);
        }
    }

    private static void localizeColumn(TableColumn<?, ?> column) {
        String original = (String) column.getProperties().get(ORIGINAL_TEXT);
        if (original == null) {
            original = column.getText();
            column.getProperties().put(ORIGINAL_TEXT, original);
        }
        column.setText(t(original));
        if (column.getGraphic() != null) localizeTree(column.getGraphic());
        for (TableColumn<?, ?> child : column.getColumns()) localizeColumn(child);
    }
}
'''
write('src/main/java/it/casiraghi/swiftbat/ui/I18n.java', i18n)


multi_select = r'''package it.casiraghi.swiftbat.ui;

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
                if (!internal) {
                    refreshText();
                    changeListener.run();
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
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isSelected()) selected.add(values.get(i));
        }
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
'''
write('src/main/java/it/casiraghi/swiftbat/ui/MultiSelectMenuButton.java', multi_select)


table_prefs = r'''package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;

import java.util.Locale;
import java.util.prefs.Preferences;

/** Preferenze persistenti per visibilità/larghezza colonne e convenzioni di allineamento. */
public final class TablePreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(TablePreferences.class);

    private TablePreferences() {}

    public static void install(TableView<?> table, String tableKey) {
        if (table == null || tableKey == null) return;
        table.setTableMenuButtonVisible(true);
        Runnable apply = () -> {
            for (TableColumn<?, ?> column : table.getColumns()) installColumn(column, tableKey);
        };
        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<?, ?>>) change -> apply.run());
        Platform.runLater(apply);

        MenuItem reset = new MenuItem(I18n.t("Ripristina colonne"));
        reset.setOnAction(event -> {
            try { PREFS.node(tableKey).clear(); } catch (Exception ignored) {}
            for (TableColumn<?, ?> column : table.getColumns()) resetColumn(column);
        });
        ContextMenu menu = table.getContextMenu();
        if (menu == null) menu = new ContextMenu();
        menu.getItems().add(reset);
        table.setContextMenu(menu);
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> reset.setText(I18n.t("Ripristina colonne")));
    }

    private static void installColumn(TableColumn<?, ?> column, String tableKey) {
        String id = columnId(column);
        Preferences node = PREFS.node(tableKey);
        column.setVisible(node.getBoolean(id + ".visible", true));
        double savedWidth = node.getDouble(id + ".width", -1);
        if (savedWidth > 40) column.setPrefWidth(savedWidth);
        column.visibleProperty().addListener((obs, oldValue, newValue) -> node.putBoolean(id + ".visible", newValue));
        column.widthProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() > 40) node.putDouble(id + ".width", newValue.doubleValue());
        });
        for (TableColumn<?, ?> child : column.getColumns()) installColumn(child, tableKey + "." + id);
    }

    private static void resetColumn(TableColumn<?, ?> column) {
        column.setVisible(true);
        for (TableColumn<?, ?> child : column.getColumns()) resetColumn(child);
    }

    private static String columnId(TableColumn<?, ?> column) {
        String text = column.getText();
        if ((text == null || text.isBlank()) && column.getGraphic() instanceof Label label) text = label.getText();
        if (text == null || text.isBlank()) text = "column" + System.identityHashCode(column);
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
'''
write('src/main/java/it/casiraghi/swiftbat/ui/TablePreferences.java', table_prefs)


# -----------------------------------------------------------------------------
# UiFactory: no problematic fullscreen glyph, runtime i18n, collapsible help.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/UiFactory.java'
text = read(path)
text = replace_once(text,
'''    public static Button button(String text, String styleClass) {
        String displayText = "Schermo intero".equals(text) ? "Schermo intero ⛶" : text;
        Button button = new Button(displayText);
        button.getStyleClass().add(styleClass);
        button.setCursor(javafx.scene.Cursor.HAND);
        autoTooltip(button);
        return button;
    }
''',
'''    public static Button button(String text, String styleClass) {
        Button button = new Button(I18n.t(text == null ? "" : text.replace("  ⛶", "").replace(" ⛶", "")));
        button.getProperties().put("swiftbat.originalText", text == null ? "" : text.replace("  ⛶", "").replace(" ⛶", ""));
        button.getStyleClass().add(styleClass);
        button.setCursor(javafx.scene.Cursor.HAND);
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> {
            Object original = button.getProperties().get("swiftbat.originalText");
            if (original instanceof String source) button.setText(I18n.t(source));
        });
        autoTooltip(button);
        return button;
    }
''', 'UiFactory button')
text = replace_once(text,
'''    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        autoTooltip(label);
        return label;
    }
''',
'''    public static Label label(String text, String... styleClasses) {
        Label label = new Label(I18n.t(text));
        label.getProperties().put("swiftbat.originalText", text);
        label.getStyleClass().addAll(styleClasses);
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> {
            Object original = label.getProperties().get("swiftbat.originalText");
            if (original instanceof String source) label.setText(I18n.t(source));
        });
        autoTooltip(label);
        return label;
    }
''', 'UiFactory label')
text = replace_once(text,
'''    public static <T> ChoiceBox<T> autoTooltip(ChoiceBox<T> choice) {
        if (choice == null) return null;
''',
'''    public static <T> ChoiceBox<T> autoTooltip(ChoiceBox<T> choice) {
        if (choice == null) return null;
        I18n.installChoiceBox(choice);
''', 'UiFactory choice i18n')
# Append collapsible helper before final brace.
insert = r'''
    public static VBox collapsibleHelp(String title, Node content) {
        ToggleButton toggle = new ToggleButton(I18n.t("Mostra spiegazione"));
        toggle.getStyleClass().addAll("ghost-button", "help-toggle");
        content.setVisible(false);
        content.setManaged(false);
        toggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            content.setVisible(selected);
            content.setManaged(selected);
            toggle.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
        });
        I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                toggle.setText(I18n.t(toggle.isSelected() ? "Nascondi spiegazione" : "Mostra spiegazione")));
        VBox box = new VBox(8, toggle, content);
        box.getStyleClass().add("collapsible-help");
        return box;
    }
'''
text = text.rsplit('\n}', 1)[0] + insert + '\n}\n'
write(path, text)


# -----------------------------------------------------------------------------
# MainView: IT / EN toggle + localization after navigation/data refresh.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/MainView.java'
text = read(path)
text = text.replace('import javafx.scene.control.Separator;\n', 'import javafx.scene.control.Separator;\nimport javafx.scene.control.ToggleButton;\nimport javafx.scene.control.ToggleGroup;\n')
old = '''        Button official = UiFactory.iconButton("↗", "Apri il catalogo ufficiale");
        official.setOnAction(event -> hostServices.showDocument(SwiftCatalogService.CATALOG_URL));

        bar.getChildren().addAll(product, live, spacer, catalogStatus, sessionStatus, connectionStatus, refresh, official);
        return bar;
'''
new = '''        Button official = UiFactory.iconButton("↗", "Apri il catalogo ufficiale");
        official.setOnAction(event -> hostServices.showDocument(SwiftCatalogService.CATALOG_URL));

        ToggleButton italian = new ToggleButton("IT");
        ToggleButton english = new ToggleButton("EN");
        italian.getStyleClass().add("language-toggle");
        english.getStyleClass().add("language-toggle");
        ToggleGroup languages = new ToggleGroup();
        italian.setToggleGroup(languages);
        english.setToggleGroup(languages);
        if (I18n.language() == I18n.Language.EN) english.setSelected(true); else italian.setSelected(true);
        italian.setOnAction(event -> I18n.setLanguage(I18n.Language.IT));
        english.setOnAction(event -> I18n.setLanguage(I18n.Language.EN));
        HBox languageBox = new HBox(2, italian, english);
        languageBox.getStyleClass().add("language-switch");
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(() -> I18n.localizeTree(root)));

        bar.getChildren().addAll(product, live, spacer, catalogStatus, sessionStatus, connectionStatus,
                languageBox, refresh, official);
        return bar;
'''
text = replace_once(text, old, new, 'MainView language switch')
text = replace_once(text,
'''        pageHost.getChildren().setAll(node);
        boolean foundVisibleButton = false;
''',
'''        pageHost.getChildren().setAll(node);
        Platform.runLater(() -> I18n.localizeTree(node));
        boolean foundVisibleButton = false;
''', 'MainView localize navigation')
write(path, text)


# -----------------------------------------------------------------------------
# Explorer: multi-select duration/redshift filters, less helper text, table prefs.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java'
text = read(path)
text = replace_once(text,
'''    private final ComboBox<String> durationFilter = new ComboBox<>();
    private final ComboBox<String> redshiftFilter = new ComboBox<>();
''',
'''    private final MultiSelectMenuButton durationFilter = new MultiSelectMenuButton(
            "Tutte", List.of("Short ≤ 2 s", "Long > 2 s", "T90 n.d."));
    private final MultiSelectMenuButton redshiftFilter = new MultiSelectMenuButton(
            "Tutti", List.of("Con z", "Senza z"));
''', 'Explorer multi-select fields')
old = '''        durationFilter.setItems(FXCollections.observableArrayList(
                "Tutte", "Short ≤ 2 s", "Long > 2 s", "T90 n.d."));
        durationFilter.setValue("Tutte");
        durationFilter.getStyleClass().add("choice-box-modern");
        redshiftFilter.setItems(FXCollections.observableArrayList(
                "Tutti", "Con z", "Senza z"));
        redshiftFilter.setValue("Tutti");
        redshiftFilter.getStyleClass().add("choice-box-modern");
        durationFilter.setMaxWidth(Double.MAX_VALUE);
        redshiftFilter.setMaxWidth(Double.MAX_VALUE);
'''
new = '''        durationFilter.setMaxWidth(Double.MAX_VALUE);
        redshiftFilter.setMaxWidth(Double.MAX_VALUE);
'''
text = replace_once(text, old, new, 'Explorer remove combo setup')
text = replace_once(text,
'''        Label hint = UiFactory.wrappedLabel(
                "Dopo il primo download, ASCII e FITS restano nella cache locale anche ai successivi avvii.",
                "sidebar-hint");
        sidebar.getChildren().addAll(title, catalogSearch, filterGrid, extraToggle, extraBox,
                catalogCount, catalogList, separator, hint);
''',
'''        sidebar.getChildren().addAll(title, catalogSearch, filterGrid, extraToggle, extraBox,
                catalogCount, catalogList, separator);
''', 'Explorer remove sidebar hint')
text = replace_once(text,
'''        durationFilter.valueProperty().addListener((obs, oldValue, newValue) -> scheduleCatalogFilters());
        redshiftFilter.valueProperty().addListener((obs, oldValue, newValue) -> scheduleCatalogFilters());
''',
'''        durationFilter.setOnSelectionChanged(this::scheduleCatalogFilters);
        redshiftFilter.setOnSelectionChanged(this::scheduleCatalogFilters);
''', 'Explorer wire multi-select')
old = '''        String duration = durationFilter.getValue() == null ? "Tutte" : durationFilter.getValue();
        String redshift = redshiftFilter.getValue() == null ? "Tutti" : redshiftFilter.getValue();
        String cache = cacheFilter.getValue() == null ? "Tutti" : cacheFilter.getValue();
'''
new = '''        java.util.Set<String> durations = durationFilter.selectedValues();
        java.util.Set<String> redshifts = redshiftFilter.selectedValues();
        boolean durationAll = durationFilter.isAllSelected();
        boolean redshiftAll = redshiftFilter.isAllSelected();
        String cache = cacheFilter.getValue() == null ? "Tutti" : cacheFilter.getValue();
'''
text = replace_once(text, old, new, 'Explorer apply filter vars')
old = '''            if (burst == null) {
                return duration.equals("Tutte") && !redshift.equals("Con z") && !hasNumericFilters;
            }
            if (duration.startsWith("Short") && !burst.isShort()) return false;
            if (duration.startsWith("Long") && !burst.isLong()) return false;
            if (duration.equals("T90 n.d.") && burst.hasT90()) return false;
            if (redshift.equals("Con z") && !burst.redshift().available()) return false;
            if (redshift.equals("Senza z") && burst.redshift().available()) return false;
'''
new = '''            if (burst == null) {
                return durationAll && redshiftAll && !hasNumericFilters;
            }
            if (!durationAll) {
                boolean durationMatches = (durations.contains("Short ≤ 2 s") && burst.isShort())
                        || (durations.contains("Long > 2 s") && burst.isLong())
                        || (durations.contains("T90 n.d.") && !burst.hasT90());
                if (!durationMatches) return false;
            }
            if (!redshiftAll) {
                boolean redshiftMatches = (redshifts.contains("Con z") && burst.redshift().available())
                        || (redshifts.contains("Senza z") && !burst.redshift().available());
                if (!redshiftMatches) return false;
            }
'''
text = replace_once(text, old, new, 'Explorer filter predicate')
text = text.replace('UiFactory.wrappedLabel(\n                        "Cerca un GRB e apri curve di luce, dati e metadati senza gestire file manualmente.",\n                        "page-subtitle")', 'UiFactory.label("", "page-subtitle")')
# Table persistence and numeric alignment.
text = replace_once(text,
'''        TableView<ObservableList<String>> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
''',
'''        TableView<ObservableList<String>> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        TablePreferences.install(table, "explorer.data");
''', 'Explorer data table preferences')
text = replace_once(text,
'''        TableView<MetadataItem> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
''',
'''        TableView<MetadataItem> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TablePreferences.install(table, "explorer.metadata");
''', 'Explorer metadata table preferences')
text = replace_once(text,
'''                    setText(value);
                    double numeric = parse(value);
''',
'''                    setText(value);
                    TablePreferences.alignCell(this, value);
                    double numeric = parse(value);
''', 'Explorer raw numeric alignment')
text = replace_once(text,
'''                } else {
                    setText(value);
                    setTooltip(value.length() > 20 ? new Tooltip(value) : null);
                }
''',
'''                } else {
                    setText(value);
                    TablePreferences.alignCell(this, value);
                    setTooltip(value.length() > 20 ? new Tooltip(value) : null);
                }
''', 'Explorer metadata alignment')
text = replace_once(text,
'''    private void setWorkspace(Node node) {
        workspace.getChildren().setAll(node);
        StackPane.setAlignment(node, Pos.CENTER);
    }
''',
'''    private void setWorkspace(Node node) {
        workspace.getChildren().setAll(node);
        StackPane.setAlignment(node, Pos.CENTER);
        Platform.runLater(() -> I18n.localizeTree(node));
    }
''', 'Explorer localize workspace')
write(path, text)


# -----------------------------------------------------------------------------
# Compare: editable GRB search fields, suggestions only when <=10 matches.
# -----------------------------------------------------------------------------
compare = r'''package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.SummaryItem;
import it.casiraghi.swiftbat.model.TabularData;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ComparePage extends BorderPane {
    private final ObservableMap<String, GrbData> sessionData;
    private final ComboBox<String> first = new ComboBox<>();
    private final ComboBox<String> second = new ComboBox<>();
    private final Label firstMatches = UiFactory.label("", "compare-match-count");
    private final Label secondMatches = UiFactory.label("", "compare-match-count");
    private final CheckBox normalize = new CheckBox("Normalizza ogni curva sul proprio picco");
    private final StackPane content = new StackPane();
    private List<String> availableNames = List.of();

    public ComparePage(ObservableMap<String, GrbData> sessionData) {
        this.sessionData = sessionData;
        getStyleClass().add("page-root");
        setPadding(new Insets(28, 34, 34, 34));
        setTop(buildHeader());
        setCenter(content);
        BorderPane.setMargin(content, new Insets(20, 0, 0, 0));
        configureSearch(first, firstMatches);
        configureSearch(second, secondMatches);
        sessionData.addListener((javafx.collections.MapChangeListener<String, GrbData>) change -> refreshChoices());
        first.valueProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        second.valueProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        normalize.selectedProperty().addListener((obs, oldValue, newValue) -> refreshComparison());
        refreshChoices();
    }

    private Node buildHeader() {
        VBox header = new VBox(14);
        VBox copy = new VBox(5, UiFactory.label("Confronta", "page-title"));

        first.getStyleClass().add("choice-box-modern");
        second.getStyleClass().add("choice-box-modern");
        first.setPrefWidth(235);
        second.setPrefWidth(235);
        normalize.getStyleClass().add("modern-check");

        VBox firstBox = new VBox(3, first, firstMatches);
        VBox secondBox = new VBox(3, second, secondMatches);
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

    private void configureSearch(ComboBox<String> combo, Label counter) {
        combo.setEditable(true);
        combo.getEditor().setText("GRB");
        combo.getEditor().setPromptText("GRB…");
        combo.getEditor().textProperty().addListener((obs, oldValue, raw) -> updateSuggestions(combo, counter, raw));
        combo.setOnAction(event -> {
            String value = combo.getValue();
            if (value != null && availableNames.contains(value)) {
                combo.getEditor().setText(value);
                refreshComparison();
            }
        });
    }

    private void updateSuggestions(ComboBox<String> combo, Label counter, String raw) {
        String query = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (query.isBlank()) query = "GRB";
        if (!query.startsWith("GRB")) query = "GRB" + query;
        final String normalized = query;
        List<String> matches = availableNames.stream().filter(name -> name.startsWith(normalized)).toList();
        counter.setText(matches.size() + (I18n.language() == I18n.Language.IT ? " corrispondenze" : " matches"));
        if (matches.size() <= 10 && !matches.isEmpty() && normalized.length() > 3) {
            combo.setItems(FXCollections.observableArrayList(matches));
            if (!combo.isShowing()) combo.show();
        } else {
            combo.hide();
            combo.setItems(FXCollections.observableArrayList());
        }
        if (matches.size() == 1 && matches.get(0).equals(normalized)) combo.setValue(matches.get(0));
    }

    private void refreshChoices() {
        List<String> names = new ArrayList<>(sessionData.keySet());
        names.sort(Comparator.reverseOrder());
        availableNames = List.copyOf(names);
        updateSuggestions(first, firstMatches, first.getEditor().getText());
        updateSuggestions(second, secondMatches, second.getEditor().getText());
        refreshComparison();
    }

    private String selected(ComboBox<String> combo) {
        String value = combo.getValue();
        if (value != null && sessionData.containsKey(value)) return value;
        String editor = combo.getEditor().getText();
        if (editor != null) {
            String normalized = editor.trim().toUpperCase(Locale.ROOT);
            if (!normalized.startsWith("GRB")) normalized = "GRB" + normalized;
            if (sessionData.containsKey(normalized)) return normalized;
        }
        return null;
    }

    private void refreshComparison() {
        String aName = selected(first);
        String bName = selected(second);
        if (sessionData.size() < 2 || aName == null || bName == null || aName.equals(bName)) {
            showEmpty();
            return;
        }
        GrbData a = sessionData.get(aName);
        GrbData b = sessionData.get(bName);
        if (a == null || b == null) { showEmpty(); return; }
        content.getChildren().setAll(buildComparison(a, b));
        I18n.localizeTree(content);
    }

    private void showEmpty() {
        VBox empty = new VBox(13);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        empty.getChildren().addAll(
                UiFactory.label("⇄", "empty-icon"),
                UiFactory.label("Apri almeno due GRB", "empty-title"));
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
        page.getChildren().add(chartCard);
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
}
'''
write('src/main/java/it/casiraghi/swiftbat/ui/ComparePage.java', compare)


# -----------------------------------------------------------------------------
# Spectroscopy: radio buttons for 2/3-option controls + remove redundant helper text.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java'
text = read(path)
text = text.replace('import javafx.scene.control.Label;\n', 'import javafx.scene.control.Label;\nimport javafx.scene.control.RadioButton;\nimport javafx.scene.control.ToggleGroup;\n')
text = replace_once(text,
'''        GridPane controls = responsiveGrid(1120, 3,
                controlBox("Intervallo del fit", intervalChoice,
                        "T100 usa l'intervallo complessivo del burst; Picco 1 s usa il secondo più intenso."),
                controlBox("Modello del fit", modelChoice,
                        "Automatico segue la scelta BAT; PL e CPL permettono di confrontare i due fit pubblicati."),
                sourceControlBox(source));
''',
'''        Node intervalRadios = radioChoice(List.of(Interval.values()), intervalChoice);
        Node modelRadios = radioChoice(List.of(AUTOMATIC_MODEL, POWER_LAW_MODEL, CUTOFF_MODEL), modelChoice);
        GridPane controls = responsiveGrid(1120, 3,
                controlBox("Intervallo del fit", intervalRadios, ""),
                controlBox("Modello del fit", modelRadios, ""),
                sourceControlBox(source));
''', 'Spectroscopy radio controls')
# Compact heading: title + provenance only.
text = replace_once(text,
'''        VBox copy = new VBox(4,
                UiFactory.label("Analisi spettroscopica", "section-title"),
                UiFactory.wrappedLabel(
                        "Confronta i fit PL/CPL e i flussi già pubblicati da Swift/BAT. "
                                + "La mappa tempo–energia, tenuta separata, descrive invece i quattro rate ASCII a bin di 1 secondo.",
                        "section-caption"));
''',
'''        VBox copy = new VBox(4, UiFactory.label("Analisi spettroscopica", "section-title"));
''', 'Spectroscopy compact heading')
# controlBox no explanatory line.
text = replace_once(text,
'''    private VBox controlBox(String label, Node control, String help, double preferredWidth) {
        Label explanation = UiFactory.wrappedLabel(help, "spectroscopy-control-help");
        explanation.setPrefWidth(preferredWidth);
        explanation.setMinHeight(Region.USE_PREF_SIZE);
        explanation.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(5, UiFactory.label(label, "filter-label"), control, explanation);
        box.getStyleClass().add("spectroscopy-control-box");
        box.setPrefWidth(preferredWidth);
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
''',
'''    private VBox controlBox(String label, Node control, String help, double preferredWidth) {
        VBox box = new VBox(5, UiFactory.label(label, "filter-label"), control);
        box.getStyleClass().add("spectroscopy-control-box");
        box.setPrefWidth(preferredWidth);
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
''', 'Spectroscopy remove control help')
text = replace_once(text,
'''    private VBox sourceControlBox(Button source) {
        Label help = UiFactory.wrappedLabel(
                "Apre direttamente la tabella BAT da cui provengono i parametri mostrati in questa scheda.",
                "spectroscopy-control-help");
        help.setMinHeight(Region.USE_PREF_SIZE);
        help.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(5,
                UiFactory.label("Fonte ufficiale BAT", "filter-label"),
                source,
                help);
''',
'''    private VBox sourceControlBox(Button source) {
        VBox box = new VBox(5,
                UiFactory.label("Fonte ufficiale BAT", "filter-label"),
                source);
''', 'Spectroscopy source help')
# Remove global reading intro/scientific note from normal results.
text = re.sub(r'\n        Label readingIntro = UiFactory\.wrappedLabel\(.*?readingIntro\.setMaxWidth\(Double\.MAX_VALUE\);\n', '\n', text, count=1, flags=re.S)
text = re.sub(r'\nLabel scientificNote = UiFactory\.wrappedLabel\(.*?"spectroscopy-note"\);\n        content\.getChildren\(\)\.addAll\(metrics, readingIntro, charts, scientificNote\);',
              '\n        content.getChildren().addAll(metrics, charts);', text, count=1, flags=re.S)
# Add radioChoice helpers before preferredInterval.
marker = '    private Interval preferredInterval() {'
if marker not in text:
    raise RuntimeError('Spectroscopy helper marker missing')
helper = r'''    private <T> Node radioChoice(List<T> values, ChoiceBox<T> backing) {
        HBox row = new HBox(8);
        row.getStyleClass().add("compact-radio-group");
        ToggleGroup group = new ToggleGroup();
        for (T value : values) {
            RadioButton radio = new RadioButton(value == null ? "" : I18n.t(value.toString()));
            radio.getStyleClass().add("compact-radio");
            radio.setToggleGroup(group);
            radio.setUserData(value);
            if (value != null && value.equals(backing.getValue())) radio.setSelected(true);
            radio.setOnAction(event -> backing.setValue(value));
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) ->
                    radio.setText(value == null ? "" : I18n.t(value.toString())));
            row.getChildren().add(radio);
        }
        backing.valueProperty().addListener((obs, oldValue, newValue) -> {
            for (javafx.scene.control.Toggle toggle : group.getToggles()) {
                if (java.util.Objects.equals(toggle.getUserData(), newValue)) {
                    group.selectToggle(toggle);
                    break;
                }
            }
        });
        return row;
    }

'''
text = text.replace(marker, helper + marker, 1)
write(path, text)


# -----------------------------------------------------------------------------
# Population: remove filter helper lines, table preferences, large loading overlay.
# -----------------------------------------------------------------------------
path = 'src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java'
text = read(path)
text = replace_once(text,
'''    private final ProgressBar progress = new ProgressBar(0);
''',
'''    private final ProgressBar progress = new ProgressBar(0);
    private final StackPane resultHost = new StackPane();
    private final VBox analysisOverlay = new VBox(14);
    private final Label analysisOverlayTitle = UiFactory.label("Elaborazione del campione…", "loading-title");
    private final Label analysisOverlayDetail = UiFactory.label("", "loading-detail");
    private final ProgressBar analysisOverlayProgress = new ProgressBar(0);
''', 'Population overlay fields')
# Need StackPane import.
text = text.replace('import javafx.scene.layout.Region;\n', 'import javafx.scene.layout.Region;\nimport javafx.scene.layout.StackPane;\n')
text = replace_once(text,
'''        page.getChildren().addAll(title, filterCard, resultTabs);
''',
'''        configureAnalysisOverlay();
        resultHost.getChildren().setAll(resultTabs, analysisOverlay);
        VBox.setVgrow(resultHost, Priority.ALWAYS);
        page.getChildren().addAll(title, filterCard, resultHost);
''', 'Population host result tabs')
# Remove filter detail labels globally through filterGroup implementation.
text = replace_once(text,
'''    private static VBox filterGroup(String title, String detail, Node control, double width) {
        Label caption = UiFactory.label(title, "filter-label");
        Label note = UiFactory.label(detail, "filter-detail");
        if (control instanceof Region value) {
            value.setPrefWidth(width);
            value.setMaxWidth(width);
        }
        VBox box = new VBox(5, caption, control, note);
        box.getStyleClass().add("population-filter-group");
        box.setPrefWidth(width);
        return box;
    }
''',
'''    private static VBox filterGroup(String title, String detail, Node control, double width) {
        Label caption = UiFactory.label(title, "filter-label");
        if (control instanceof Region value) {
            value.setPrefWidth(width);
            value.setMaxWidth(width);
        }
        VBox box = new VBox(5, caption, control);
        box.getStyleClass().add("population-filter-group");
        box.setPrefWidth(width);
        return box;
    }
''', 'Population filter hints')
# Compact FRACEXP intro and preview footer.
text = replace_once(text,
'''        VBox exposureIntro = new VBox(2,
                UiFactory.label("Qualità della copertura FRACEXP", "population-section-title"),
                UiFactory.wrappedLabel(
                        "Percentuale di bin con FRACEXP ≥ 0,999. Trascina oppure scrivi il valore.",
                        "sky-filter-help"));
''',
'''        VBox exposureIntro = new VBox(2,
                UiFactory.label("Qualità della copertura FRACEXP", "population-section-title"));
''', 'Population fracexp help')
text = replace_once(text,
'''        VBox preview = new VBox(2,
                candidatePreview,
                UiFactory.wrappedLabel(
                        "T90, redshift e coordinate vengono applicati prima; FRACEXP richiede il FITS e usa la cache locale.",
                        "sky-filter-help"));
''',
'''        VBox preview = new VBox(2, candidatePreview);
''', 'Population preview help')
# Remove chart explanatory note, add collapsible assistant toggle.
old = '''        Label note = UiFactory.wrappedLabel(
                "Asse X: secondi dal trigger. Asse Y: rate relativo, con il picco di ogni GRB posto uguale a 1. "
                        + "Il grafico confronta la forma temporale e non somma i segnali.",
                "explanation-text");
        HBox.setHgrow(note, Priority.ALWAYS);
        profile3D.setDisable(true);
        profile3D.setOnAction(event -> openProfile3D());
        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");
        fullscreen.setOnAction(event -> openProfileFullscreen());
        HBox actions = new HBox(8, profile3D, fullscreen);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(12, note, actions);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox chartArea = new VBox(8, profileLegend(), curveChart);
        HBox.setHgrow(chartArea, Priority.ALWAYS);
        VBox.setVgrow(curveChart, Priority.ALWAYS);
        HBox body = new HBox(14, chartArea, insightCard());
        HBox.setHgrow(chartArea, Priority.ALWAYS);
        box.getChildren().addAll(header, body);
'''
new = '''        profile3D.setDisable(true);
        profile3D.setOnAction(event -> openProfile3D());
        Button fullscreen = UiFactory.button("Schermo intero", "primary-button");
        fullscreen.setOnAction(event -> openProfileFullscreen());
        VBox assistant = insightCard();
        assistant.setVisible(false);
        assistant.setManaged(false);
        ToggleButton help = new ToggleButton(I18n.t("Mostra spiegazione"));
        help.getStyleClass().addAll("ghost-button", "help-toggle");
        help.selectedProperty().addListener((obs, oldValue, selected) -> {
            assistant.setVisible(selected);
            assistant.setManaged(selected);
            help.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
        });
        HBox actions = new HBox(8, help, profile3D, fullscreen);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(12, UiFactory.spacer(), actions);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox chartArea = new VBox(8, profileLegend(), curveChart);
        HBox.setHgrow(chartArea, Priority.ALWAYS);
        VBox.setVgrow(curveChart, Priority.ALWAYS);
        HBox body = new HBox(14, chartArea, assistant);
        HBox.setHgrow(chartArea, Priority.ALWAYS);
        box.getChildren().addAll(header, body);
'''
text = replace_once(text, old, new, 'Population collapsible insight')
# Table preferences.
text = replace_once(text,
'''        resultTable.getStyleClass().add("data-table");
''',
'''        resultTable.getStyleClass().add("data-table");
        TablePreferences.install(resultTable, "population.results");
''', 'Population table prefs')
# Configure numeric/right alignment in generic string column.
old = '''        TableColumn<PopulationEvent, String> column = new TableColumn<>(title);
        column.setCellValueFactory(value -> new ReadOnlyStringWrapper(mapper.apply(value.getValue())));
        return column;
'''
new = '''        TableColumn<PopulationEvent, String> column = new TableColumn<>(title);
        column.setCellValueFactory(value -> new ReadOnlyStringWrapper(mapper.apply(value.getValue())));
        column.setCellFactory(ignored -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                if (!empty) TablePreferences.alignCell(this, value);
            }
        });
        return column;
'''
text = replace_once(text, old, new, 'Population numeric alignment')
# Add overlay helper before startAnalysis.
marker = '    private void startAnalysis() {'
if marker not in text: raise RuntimeError('Population startAnalysis marker missing')
overlay_helper = r'''    private void configureAnalysisOverlay() {
        analysisOverlay.getStyleClass().add("population-analysis-overlay");
        analysisOverlay.setAlignment(Pos.CENTER);
        analysisOverlay.setPadding(new Insets(36));
        analysisOverlayProgress.setPrefWidth(440);
        analysisOverlayProgress.setMaxWidth(520);
        analysisOverlayProgress.getStyleClass().add("modern-progress");
        analysisOverlayDetail.setWrapText(true);
        analysisOverlayDetail.setMaxWidth(620);
        analysisOverlay.getChildren().setAll(
                UiFactory.label("⌁", "loading-icon"),
                analysisOverlayTitle,
                analysisOverlayDetail,
                analysisOverlayProgress);
        analysisOverlay.setVisible(false);
        analysisOverlay.setManaged(false);
    }

    private void showAnalysisOverlay(Task<?> task) {
        analysisOverlay.setVisible(true);
        analysisOverlay.setManaged(true);
        analysisOverlay.toFront();
        analysisOverlayProgress.progressProperty().unbind();
        analysisOverlayDetail.textProperty().unbind();
        analysisOverlayProgress.progressProperty().bind(task.progressProperty());
        analysisOverlayDetail.textProperty().bind(task.messageProperty());
    }

    private void hideAnalysisOverlay() {
        analysisOverlayProgress.progressProperty().unbind();
        analysisOverlayDetail.textProperty().unbind();
        analysisOverlay.setVisible(false);
        analysisOverlay.setManaged(false);
    }

'''
text = text.replace(marker, overlay_helper + marker, 1)
# Show overlay in start after setRunning and hide in finish methods.
text = replace_once(text,
'''        setRunning(true);
        runningTask.setOnSucceeded(event -> finishAnalysis(runningTask.getValue()));
''',
'''        setRunning(true);
        showAnalysisOverlay(runningTask);
        runningTask.setOnSucceeded(event -> finishAnalysis(runningTask.getValue()));
''', 'Population show overlay')
text = text.replace('    private void finishAnalysis(AnalysisResult result) {\n        progress.progressProperty().unbind();',
                    '    private void finishAnalysis(AnalysisResult result) {\n        hideAnalysisOverlay();\n        progress.progressProperty().unbind();', 1)
text = text.replace('    private void finishCancelled() {\n        progress.progressProperty().unbind();',
                    '    private void finishCancelled() {\n        hideAnalysisOverlay();\n        progress.progressProperty().unbind();', 1)
text = text.replace('    private void finishFailed(Throwable error) {\n        progress.progressProperty().unbind();',
                    '    private void finishFailed(Throwable error) {\n        hideAnalysisOverlay();\n        progress.progressProperty().unbind();', 1)
write(path, text)


# -----------------------------------------------------------------------------
# Global CSS: left table headers, language switch, radios, overlay.
# -----------------------------------------------------------------------------
path = 'src/main/resources/app.css'
css = read(path)
css += r'''

/* ---------- Feedback relatori: dense expert UI ---------- */
.table-view .column-header .label,
.tree-table-view .column-header .label {
    -fx-alignment: CENTER-LEFT;
    -fx-text-alignment: left;
}

.language-switch {
    -fx-background-color: rgba(20, 30, 54, 0.78);
    -fx-background-radius: 10px;
    -fx-border-color: rgba(110, 145, 210, 0.22);
    -fx-border-radius: 10px;
    -fx-padding: 2px;
}

.language-toggle {
    -fx-background-color: transparent;
    -fx-text-fill: #8494b4;
    -fx-font-weight: bold;
    -fx-font-size: 10px;
    -fx-padding: 6 9;
    -fx-background-radius: 8px;
}

.language-toggle:selected {
    -fx-background-color: linear-gradient(to right, rgba(45, 190, 225, 0.32), rgba(121, 91, 225, 0.34));
    -fx-text-fill: #f4f8ff;
}

.compact-radio-group {
    -fx-alignment: center-left;
    -fx-spacing: 8px;
}

.compact-radio {
    -fx-text-fill: #c8d5ef;
    -fx-font-size: 11px;
    -fx-padding: 7 9;
    -fx-background-color: rgba(23, 34, 59, 0.72);
    -fx-background-radius: 9px;
    -fx-border-color: rgba(105, 139, 201, 0.20);
    -fx-border-radius: 9px;
}

.compact-radio:selected {
    -fx-text-fill: #ffffff;
    -fx-background-color: rgba(101, 84, 205, 0.35);
    -fx-border-color: rgba(130, 114, 244, 0.50);
}

.help-toggle {
    -fx-font-size: 10px;
    -fx-padding: 8 11;
}

.population-analysis-overlay {
    -fx-background-color: rgba(5, 9, 20, 0.94);
    -fx-background-radius: 18px;
    -fx-border-color: rgba(88, 210, 243, 0.22);
    -fx-border-radius: 18px;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 28, 0.25, 0, 8);
}

.compare-match-count {
    -fx-text-fill: #6f7f9f;
    -fx-font-size: 9px;
    -fx-padding: 0 2;
}
'''
write(path, css)


# -----------------------------------------------------------------------------
# UTF-8 at runtime and remove the fullscreen glyph from all source literals.
# -----------------------------------------------------------------------------
# pom already declares UTF-8; enforce runtime file encoding too.
path = 'pom.xml'
pom = read(path)
if '<option>-Dfile.encoding=UTF-8</option>' not in pom:
    pom = pom.replace('<option>--enable-native-access=javafx.graphics,javafx.swing</option>',
                      '<option>--enable-native-access=javafx.graphics,javafx.swing</option>\n                        <option>-Dfile.encoding=UTF-8</option>')
write(path, pom)

# Remove problematic fullscreen symbol from Java sources (button styling remains primary).
for java in (ROOT / 'src/main/java').rglob('*.java'):
    source = java.read_text(encoding='utf-8')
    source = source.replace('Schermo intero  ⛶', 'Schermo intero').replace('Schermo intero ⛶', 'Schermo intero')
    java.write_text(source, encoding='utf-8')

# Remove temporary patch files before final commit.
for temporary in [ROOT / 'scripts/apply_professor_feedback.py', ROOT / '.github/workflows/apply-professor-feedback.yml']:
    if temporary.exists():
        temporary.unlink()

print('Professor feedback patch applied successfully')
