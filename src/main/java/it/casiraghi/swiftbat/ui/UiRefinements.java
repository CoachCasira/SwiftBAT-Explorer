package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.FieldDefinition;
import it.casiraghi.swiftbat.service.OnlineGrbService;
import it.casiraghi.swiftbat.ui.components.TimeEnergyHeatmapPane;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Piccoli affinamenti trasversali dell'interfaccia 1.3.0.
 *
 * La classe lavora sui controlli gia' costruiti usando le style-class stabili
 * dell'applicazione. In questo modo le correzioni restano centralizzate senza
 * duplicare la logica scientifica delle singole pagine.
 */
public final class UiRefinements {
    private static final String WATCHED = UiRefinements.class.getName() + ".watched";
    private static final String POPULATION_DONE = UiRefinements.class.getName() + ".populationDone";
    private static final String COMPARE_DONE = UiRefinements.class.getName() + ".compareDone";
    private static final String COMPARE_ARROW_DONE = UiRefinements.class.getName() + ".compareArrowDone";
    private static final String METADATA_DONE = UiRefinements.class.getName() + ".metadataDone";
    private static final String SPECTROSCOPY_DONE = UiRefinements.class.getName() + ".spectroscopyDone";
    private static final String HEATMAP_DONE = UiRefinements.class.getName() + ".heatmapDone";
    private static final String UI_FACTORY_ORIGINAL = "swiftbat.originalText";
    private static final String I18N_ORIGINAL = I18n.class.getName() + ".originalText";
    private static final String MISSING = "[Missing English translation]";
    private static final Map<String, String> ENGLISH = buildEnglishMap();

    private UiRefinements() {}

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> polish(root));
        I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                Platform.runLater(() -> polish(root)));
    }

    private static void watch(Node node) {
        if (node == null) return;
        polishNode(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node added : change.getAddedSubList()) watch(added);
                }
            }
            Platform.runLater(() -> polish(parent));
        });
        for (Node child : parent.getChildrenUnmodifiable()) watch(child);
    }

    private static void polish(Node node) {
        if (node == null) return;
        polishNode(node);
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) polish(child);
        }
    }

    private static void polishNode(Node node) {
        fixEnglish(node);
        if (node instanceof HBox row) {
            spreadPopulationFilters(row);
            centerCompareSelectors(row);
        }
        if (node instanceof VBox box) enhanceMetadataFieldSelector(box);
        if (node instanceof ComboBox<?> combo) installCompareArrowGuard(combo);
        if (node instanceof TimeEnergyHeatmapPane heatmap) compactTimeEnergyMap(heatmap);
        if (node instanceof TabPane tabs && hasStyle(tabs, "spectroscopy-tabs")) {
            installSpectroscopyTabSizing(tabs);
        }
    }

    /* ---------------- Population analysis ---------------- */

    private static void spreadPopulationFilters(HBox row) {
        if (Boolean.TRUE.equals(row.getProperties().get(POPULATION_DONE))) return;
        long filterChildren = row.getChildren().stream()
                .filter(child -> hasStyle(child, "population-filter-group")
                        || hasStyle(child, "population-filter-section"))
                .count();
        if (filterChildren < 5) return;

        row.getProperties().put(POPULATION_DONE, Boolean.TRUE);
        row.setMaxWidth(Double.MAX_VALUE);
        for (Node child : row.getChildren()) {
            if (!(child instanceof Region region)) continue;
            String caption = descendantCaption(child);
            if (hasStyle(child, "population-fracexp-inline")) {
                region.setMinWidth(350);
                region.setPrefWidth(390);
                child.getStyleClass().add("population-fracexp-wide");
            } else if ("Durata T90".equals(caption)) {
                region.setMinWidth(175);
                region.setPrefWidth(195);
                child.getStyleClass().add("population-filter-group-wide");
            } else if ("Redshift".equals(caption)) {
                region.setMinWidth(215);
                region.setPrefWidth(235);
                child.getStyleClass().add("population-filter-group-wide");
            } else if ("Finestra temporale".equals(caption)) {
                region.setMinWidth(215);
                region.setPrefWidth(235);
                child.getStyleClass().add("population-filter-group-wide");
            } else if ("Campione massimo".equals(caption)) {
                region.setMinWidth(155);
                region.setPrefWidth(175);
                child.getStyleClass().add("population-filter-group-wide");
            }
            region.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(region, Priority.ALWAYS);
            relaxPopulationControl(child);
        }
    }

    private static void relaxPopulationControl(Node node) {
        if (node instanceof Region region
                && (hasStyle(node, "choice-box-modern")
                || hasStyle(node, "compact-radio-group")
                || hasStyle(node, "percentage-control")
                || hasStyle(node, "fracexp-slider"))) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) relaxPopulationControl(child);
        }
    }

    private static String descendantCaption(Node node) {
        if (node instanceof Labeled labeled) {
            String source = sourceText(labeled);
            if (source != null && !source.isBlank()) return source;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                String text = descendantCaption(child);
                if (text != null && !text.isBlank()) return text;
            }
        }
        return "";
    }

    /* ---------------- Compare ---------------- */

    private static void centerCompareSelectors(HBox row) {
        if (!hasStyle(row, "compare-control-bar")
                || Boolean.TRUE.equals(row.getProperties().get(COMPARE_DONE))) return;
        List<Node> children = new ArrayList<>(row.getChildren());
        if (children.size() < 7 || !(children.get(children.size() - 1) instanceof CheckBox normalize)) return;

        row.getProperties().put(COMPARE_DONE, Boolean.TRUE);
        List<Node> selectors = new ArrayList<>(children.subList(0, children.size() - 2));
        HBox selectorGroup = new HBox(10);
        selectorGroup.getStyleClass().add("compare-selector-group");
        selectorGroup.setAlignment(Pos.CENTER);
        selectorGroup.getChildren().setAll(selectors);

        Region leftBalance = new Region();
        Region rightBalance = new Region();
        leftBalance.minWidthProperty().bind(normalize.widthProperty());
        leftBalance.prefWidthProperty().bind(normalize.widthProperty());
        HBox.setHgrow(leftBalance, Priority.ALWAYS);
        HBox.setHgrow(rightBalance, Priority.ALWAYS);

        row.getChildren().setAll(leftBalance, selectorGroup, rightBalance, normalize);
        row.getStyleClass().add("compare-control-bar-centered");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
    }

    private static void installCompareArrowGuard(ComboBox<?> combo) {
        if (!combo.isEditable() || !isInsideStyle(combo, "compare-control-bar")
                || Boolean.TRUE.equals(combo.getProperties().get(COMPARE_ARROW_DONE))) return;
        combo.getProperties().put(COMPARE_ARROW_DONE, Boolean.TRUE);
        Runnable refresh = () -> refreshCompareArrow(combo);
        combo.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        combo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        combo.skinProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(refresh));
        Platform.runLater(refresh);
    }

    private static void refreshCompareArrow(ComboBox<?> combo) {
        Node arrow = combo.lookup(".arrow-button");
        if (arrow == null) return;
        Object value = combo.getValue();
        String editor = combo.getEditor().getText();
        boolean valid = value != null && editor != null
                && value.toString().equalsIgnoreCase(editor.trim());
        arrow.setDisable(!valid);
        arrow.setMouseTransparent(!valid);
        arrow.setOpacity(valid ? 1.0 : 0.35);
    }

    /* ---------------- Metadata field selector ---------------- */

    private static void enhanceMetadataFieldSelector(VBox box) {
        if (Boolean.TRUE.equals(box.getProperties().get(METADATA_DONE))) return;
        boolean metadataSide = box.getChildren().stream()
                .filter(Labeled.class::isInstance)
                .map(Labeled.class::cast)
                .map(UiRefinements::sourceText)
                .anyMatch("Campo metadata"::equals);
        if (!metadataSide) return;

        ChoiceBox<?> rawChoice = box.getChildren().stream()
                .filter(ChoiceBox.class::isInstance)
                .map(ChoiceBox.class::cast)
                .findFirst().orElse(null);
        if (rawChoice == null) return;

        @SuppressWarnings("unchecked")
        ChoiceBox<String> original = (ChoiceBox<String>) rawChoice;
        box.getProperties().put(METADATA_DONE, Boolean.TRUE);
        int originalIndex = box.getChildren().indexOf(original);
        original.setVisible(false);
        original.setManaged(false);

        MetadataSearchBridge bridge = new MetadataSearchBridge(original);
        HBox toolbar = bridge.build();
        box.getChildren().add(Math.min(originalIndex + 1, box.getChildren().size()), toolbar);
    }

    private static final class MetadataSearchBridge {
        private final ChoiceBox<String> original;
        private final List<String> master;
        private final ComboBox<String> search = new ComboBox<>();
        private final Button sort = new Button("A → Z");
        private boolean descending;
        private boolean syncing;

        MetadataSearchBridge(ChoiceBox<String> original) {
            this.original = original;
            this.master = original.getItems().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .distinct().toList();
        }

        HBox build() {
            search.setEditable(true);
            search.setVisibleRowCount(12);
            search.setMinWidth(220);
            search.setPrefWidth(300);
            search.setMaxWidth(Double.MAX_VALUE);
            search.getStyleClass().addAll("choice-box-modern", "metadata-search-combo");
            search.setPromptText(I18n.dynamic("Cerca un campo metadata…", "Search a metadata field…"));
            search.setCellFactory(list -> groupedCell());
            search.setButtonCell(simpleButtonCell());
            HBox.setHgrow(search, Priority.ALWAYS);

            sort.getStyleClass().addAll("ghost-button", "metadata-sort-button");
            sort.setFocusTraversable(false);
            sort.setOnAction(event -> {
                descending = !descending;
                sort.setText(descending ? "Z → A" : "A → Z");
                rebuild(search.getEditor().getText());
            });

            search.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                if (!syncing) rebuild(newValue);
            });
            search.getEditor().focusedProperty().addListener((obs, oldValue, focused) -> {
                if (focused) {
                    Platform.runLater(search.getEditor()::selectAll);
                    rebuild(search.getEditor().getText());
                } else if (!isKnown(search.getEditor().getText())) {
                    syncFromOriginal(original.getValue());
                }
            });
            search.setOnAction(event -> {
                if (syncing) return;
                String selected = search.getValue();
                if (isKnown(selected)) commit(selected);
            });
            original.valueProperty().addListener((obs, oldValue, newValue) -> syncFromOriginal(newValue));
            I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                    search.setPromptText(I18n.dynamic("Cerca un campo metadata…", "Search a metadata field…")));

            rebuild("");
            syncFromOriginal(original.getValue());

            HBox toolbar = new HBox(7, search, sort);
            toolbar.getStyleClass().add("metadata-field-toolbar");
            toolbar.setAlignment(Pos.CENTER_LEFT);
            toolbar.setMaxWidth(Double.MAX_VALUE);
            return toolbar;
        }

        private ListCell<String> simpleButtonCell() {
            return new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(null);
                    setText(empty || item == null ? null : item);
                }
            };
        }

        private ListCell<String> groupedCell() {
            return new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(null);
                    setGraphic(null);
                    if (empty || item == null) return;

                    VBox content = new VBox(2);
                    int index = getIndex();
                    String currentGroup = group(item);
                    String previousGroup = index > 0 && index - 1 < getListView().getItems().size()
                            ? group(getListView().getItems().get(index - 1)) : "";
                    if (!currentGroup.equals(previousGroup)) {
                        Label groupLabel = new Label(currentGroup);
                        groupLabel.getStyleClass().add("metadata-alpha-header");
                        Separator separator = new Separator();
                        separator.getStyleClass().add("metadata-alpha-separator");
                        separator.setMaxWidth(Double.MAX_VALUE);
                        HBox.setHgrow(separator, Priority.ALWAYS);
                        HBox header = new HBox(7, groupLabel, separator);
                        header.setAlignment(Pos.CENTER_LEFT);
                        content.getChildren().add(header);
                    }
                    Label field = new Label(item);
                    field.getStyleClass().add("metadata-field-name");
                    content.getChildren().add(field);
                    setGraphic(content);
                }
            };
        }

        private void rebuild(String rawQuery) {
            String query = rawQuery == null ? "" : rawQuery.trim().toUpperCase(Locale.ROOT);
            Comparator<String> order = Comparator.comparing(value -> value.toUpperCase(Locale.ROOT));
            if (descending) order = order.reversed();
            List<String> filtered = master.stream()
                    .filter(value -> query.isBlank() || value.toUpperCase(Locale.ROOT).contains(query))
                    .sorted(order)
                    .toList();

            syncing = true;
            try {
                String editorText = search.getEditor().getText();
                search.getItems().setAll(filtered);
                search.getEditor().setText(editorText == null ? "" : editorText);
                search.getEditor().positionCaret(search.getEditor().getText().length());
            } finally {
                syncing = false;
            }
            if (search.getEditor().isFocused() && !filtered.isEmpty()) {
                Platform.runLater(search::show);
            } else if (filtered.isEmpty()) {
                search.hide();
            }
        }

        private void commit(String value) {
            if (!isKnown(value)) return;
            syncing = true;
            try {
                original.setValue(value);
                search.setValue(value);
                search.getEditor().setText(value);
                search.getEditor().positionCaret(value.length());
                search.hide();
            } finally {
                syncing = false;
            }
        }

        private void syncFromOriginal(String value) {
            if (value == null || value.isBlank()) return;
            syncing = true;
            try {
                search.getItems().setAll(sortedMaster());
                search.setValue(value);
                search.getEditor().setText(value);
                search.getEditor().positionCaret(value.length());
            } finally {
                syncing = false;
            }
        }

        private List<String> sortedMaster() {
            Comparator<String> order = Comparator.comparing(value -> value.toUpperCase(Locale.ROOT));
            if (descending) order = order.reversed();
            return master.stream().sorted(order).toList();
        }

        private boolean isKnown(String value) {
            return value != null && master.contains(value.trim());
        }

        private static String group(String value) {
            if (value == null || value.isBlank()) return "#";
            char first = Character.toUpperCase(value.trim().charAt(0));
            return Character.isLetterOrDigit(first) ? String.valueOf(first) : "#";
        }
    }

    /* ---------------- Spectroscopy ---------------- */

    private static void compactTimeEnergyMap(TimeEnergyHeatmapPane heatmap) {
        if (Boolean.TRUE.equals(heatmap.getProperties().get(HEATMAP_DONE))) return;
        heatmap.getProperties().put(HEATMAP_DONE, Boolean.TRUE);
        heatmap.setMinHeight(300);
        heatmap.setPrefHeight(390);
        heatmap.setMaxHeight(410);
        VBox.setVgrow(heatmap, Priority.NEVER);
    }

    private static void installSpectroscopyTabSizing(TabPane tabs) {
        if (Boolean.TRUE.equals(tabs.getProperties().get(SPECTROSCOPY_DONE))) return;
        tabs.getProperties().put(SPECTROSCOPY_DONE, Boolean.TRUE);
        Runnable resize = () -> resizeSpectroscopyTabs(tabs);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                Platform.runLater(resize));
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(resize));
        Platform.runLater(resize);
    }

    private static void resizeSpectroscopyTabs(TabPane tabs) {
        Tab selected = tabs.getSelectionModel().getSelectedItem();
        if (selected != null && isTimeEnergyTab(selected)) {
            tabs.setMinHeight(535);
            tabs.setPrefHeight(535);
            tabs.setMaxHeight(535);
        } else {
            tabs.setMinHeight(Region.USE_PREF_SIZE);
            tabs.setPrefHeight(Region.USE_COMPUTED_SIZE);
            tabs.setMaxHeight(Double.MAX_VALUE);
        }
        tabs.requestLayout();
    }

    private static boolean isTimeEnergyTab(Tab tab) {
        Object original = tab.getProperties().get(I18N_ORIGINAL);
        String text = original instanceof String source ? source : tab.getText();
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("tempo–energia") || lower.contains("tempo-energia")
                || lower.contains("time–energy") || lower.contains("time-energy");
    }

    /* ---------------- English translation repair ---------------- */

    private static void fixEnglish(Node node) {
        if (I18n.language() != I18n.Language.EN) return;
        if (node instanceof Labeled labeled) {
            String source = sourceText(labeled);
            if (source != null && !source.isBlank()) {
                String english = englishFor(source);
                if (english != null) {
                    I18n.setText(labeled, source, english);
                } else if (MISSING.equals(labeled.getText())) {
                    // Mai mostrare un placeholder tecnico all'utente: se una stringa
                    // non e' ancora catalogata, conserva almeno il testo sorgente.
                    labeled.setText(source);
                }
            }
        }
        if (node instanceof TextInputControl input) {
            String prompt = input.getPromptText();
            if (MISSING.equals(prompt)) {
                Object original = input.getProperties().get(I18N_ORIGINAL + ".prompt");
                if (original instanceof String source) {
                    String english = englishFor(source);
                    input.setPromptText(english == null ? source : english);
                }
            }
        }
    }

    private static String sourceText(Labeled labeled) {
        if (labeled == null) return null;
        Object uiSource = labeled.getProperties().get(UI_FACTORY_ORIGINAL);
        if (uiSource instanceof String source) return source;
        Object i18nSource = labeled.getProperties().get(I18N_ORIGINAL);
        if (i18nSource instanceof String source) return source;
        return labeled.getText();
    }

    private static String englishFor(String italian) {
        if (italian == null || italian.isBlank()) return italian;
        String unified = UiTranslations.t(italian);
        if (unified != null && !unified.isBlank()
                && !MISSING.equals(unified) && !unified.equals(italian)) {
            return unified;
        }
        String direct = ENGLISH.get(italian);
        if (direct != null) return direct;

        if (italian.startsWith("Il catalogo indica ") && italian.contains(" come modello preferito")) {
            String model = italian.substring("Il catalogo indica ".length(), italian.indexOf(" come modello preferito"));
            return "The catalog identifies " + model + " as the preferred model for this interval.";
        }
        if (italian.startsWith("L'indice α vale ") && italian.contains(":")) {
            String value = italian.substring("L'indice α vale ".length(), italian.indexOf(':'));
            return "The α index is " + value + ": more negative values describe a faster decrease toward high energies.";
        }
        if (italian.startsWith("Il picco Epeak è vincolato a circa ")) {
            return italian.replace("Il picco Epeak è vincolato a circa ", "Epeak is constrained to about ");
        }
        if (italian.startsWith("Tra le quattro bande non sovrapposte, il flusso maggiore è in ")) {
            return italian.replace("Tra le quattro bande non sovrapposte, il flusso maggiore è in ",
                    "Among the four non-overlapping bands, the largest flux is in ");
        }
        if (italian.startsWith("Lo spettro integra da ") && italian.endsWith(" rispetto al trigger.")) {
            String core = italian.substring("Lo spettro integra da ".length(),
                    italian.length() - " rispetto al trigger.".length());
            return "The spectrum integrates from " + core + " relative to the trigger.";
        }
        if (italian.startsWith("Il catalogo spettroscopico BAT non contiene ancora ")) {
            return "The BAT spectral catalog does not yet contain this GRB for the selected interval. "
                    + "The light curve and time–energy map remain available; an app refresh reloads the online tables.";
        }
        return null;
    }

    private static Map<String, String> buildEnglishMap() {
        Map<String, String> map = new LinkedHashMap<>();

        // Data dictionary / field explanation labels.
        put(map, "In parole semplici", "In simple terms");
        put(map, "Descrizione tecnica", "Technical description");
        put(map, "Perché è utile", "Why it matters");
        put(map, "Perché serve", "Why it matters");
        put(map, "Attenzione a non confonderlo", "Do not confuse it with");
        put(map, "Attenzione", "Caution");
        put(map, "senza unità", "no unit");
        put(map, "Campo", "Field");
        put(map, "Voce FITS", "FITS entry");
        put(map, "Commento FITS", "FITS comment");
        put(map, "Cerca un campo o un concetto…", "Search a field or concept…");
        put(map, "Cerca un campo metadata…", "Search a metadata field…");
        put(map, "Non è ancora disponibile una spiegazione specifica per questa voce.",
                "A specific explanation is not yet available for this field.");

        // Metadata explanation panel.
        put(map, "Seleziona una riga. I metadati sono il registro tecnico del file FITS: descrivono provenienza, tempi, coordinate, struttura e passaggi di elaborazione.",
                "Select a row. Metadata are the technical record of the FITS file: they describe provenance, timing, coordinates, structure and processing steps.");
        put(map, "Un FITS può contenere più sezioni. PRIMARY è l'intestazione generale; RATE è la tabella della curva di luce.",
                "A FITS file can contain multiple sections. PRIMARY is the general header; RATE is the light-curve table.");
        put(map, "È il nome breve del parametro, per esempio OBS_ID, TRIGTIME o TIMEDEL.",
                "This is the short parameter name, for example OBS_ID, TRIGTIME or TIMEDEL.");
        put(map, "È il contenuto associato alla keyword nella riga selezionata: può essere un numero, una data, un identificativo, una stringa o un valore logico.",
                "This is the value associated with the keyword in the selected row: it may be a number, date, identifier, string or logical value.");
        put(map, "È la descrizione scritta dal software che ha prodotto il FITS.",
                "This is the description written by the software that produced the FITS file.");
        put(map, "Questa keyword non è ancora inclusa nel dizionario didattico. Il commento originale del FITS rimane comunque visibile.",
                "This keyword is not yet included in the learning dictionary. The original FITS comment remains visible.");

        // Scientific guide in spectroscopy.
        put(map, "1 · Che cosa arriva dal catalogo BAT", "1 · What comes from the BAT catalog");
        put(map, "2 · PL e CPL", "2 · PL and CPL");
        put(map, "3 · Flusso e frequenza", "3 · Flux and frequency");
        put(map, "4 · Perché non basta il file ASCII", "4 · Why the ASCII file is not enough");
        put(map, "5 · T100 e picco di 1 secondo", "5 · T100 and the 1-second peak");
        put(map, "I parametri PL/CPL, il modello migliore, χ², gradi di libertà, probabilità nulla, intervallo dello spettro e flussi per banda sono letti dalle tabelle ufficiali. L'app non li inventa e non li ricalcola dai quattro canali.",
                "The PL/CPL parameters, best-fit model, χ², degrees of freedom, null probability, spectrum interval and band fluxes are read from the official tables. The app does not invent them or recompute them from the four channels.");
        put(map, "PL descrive lo spettro con una legge di potenza. CPL aggiunge un taglio esponenziale e può fornire Epeak. Se BAT scrive N/A, significa che non pubblica una scelta preferita tra i modelli per quel caso.",
                "PL describes the spectrum with a power law. CPL adds an exponential cutoff and can provide Epeak. If BAT reports N/A, no preferred model is published for that case.");
        put(map, "Il catalogo usa energia in keV, equivalente alla frequenza tramite E = hν. Le barre mostrano flussi energetici integrati in bande, non il rate della curva di luce.",
                "The catalog uses energy in keV, equivalent to frequency through E = hν. The bars show energy fluxes integrated over bands, not the light-curve count rate.");
        put(map, "Per trasformare conteggi in flusso servono spettro PHA, matrice di risposta RSP/DRM, correzioni strumentali e un fit. I prodotti qui visualizzati sono già stati elaborati dalla pipeline BAT con XSPEC.",
                "Converting counts to flux requires a PHA spectrum, an RSP/DRM response matrix, instrumental corrections and a fit. The products shown here have already been processed by the BAT pipeline with XSPEC.");
        put(map, "T100 riassume l'intero intervallo scelto per lo spettro del burst; il picco di 1 secondo descrive invece la fase più intensa su quella scala temporale. Rispondono a domande diverse e non vanno mescolati.",
                "T100 summarizes the full interval selected for the burst spectrum; the 1-second peak instead describes the most intense phase on that time scale. They answer different questions and should not be mixed.");

        // Other spectroscopy strings that are built directly by the view.
        put(map, "DATI UFFICIALI BAT · FIT XSPEC", "OFFICIAL BAT DATA · XSPEC FIT");
        put(map, "Nessun risultato ufficiale disponibile", "No official result available");
        put(map, "Nessun modello preferito pubblicato", "No preferred model published");
        put(map, "scelta del catalogo", "catalog selection");
        put(map, "fit non disponibile", "fit unavailable");
        put(map, "non previsto dal modello PL", "not defined by the PL model");
        put(map, "picco νFν del modello CPL", "νFν peak of the CPL model");
        put(map, "qualità del fit", "fit quality");
        put(map, "intervallo non disponibile", "interval unavailable");
        put(map, "Nessun flusso disponibile per il modello scelto.", "No flux is available for the selected model.");
        put(map, "Flussi e incertezza", "Fluxes and uncertainty");
        put(map, "Flusso", "Flux");
        put(map, "Intervallo 90%", "90% interval");
        put(map, "Unità: erg cm⁻² s⁻¹. I limiti sono quelli pubblicati da BAT.",
                "Unit: erg cm⁻² s⁻¹. The limits are those published by BAT.");
        put(map, "Descrizione automatica locale: aiuta a leggere i numeri, ma non sostituisce la valutazione spettroscopica dell'esperto.",
                "Local automatic description: it helps read the numbers, but does not replace the expert's spectral assessment.");
        put(map, "Epeak non è ben vincolato dai limiti pubblicati; non va interpretato come una misura robusta.",
                "Epeak is not well constrained by the published limits and should not be interpreted as a robust measurement.");
        put(map, "Il χ² ridotto è vicino a 1, quindi il modello è globalmente compatibile con i dati; va comunque letto con la probabilità nulla.",
                "The reduced χ² is close to 1, so the model is globally compatible with the data; it should still be read together with the null probability.");
        put(map, "Il χ² ridotto si discosta da 1: il fit merita un controllo più attento insieme a residui e probabilità nulla.",
                "The reduced χ² differs from 1: the fit deserves closer inspection together with residuals and null probability.");

        // Explorer data/guide texts frequently visible in English mode.
        put(map, "ASCII — quattro bande", "ASCII — four bands");
        put(map, "FITS — un canale e qualità", "FITS — one channel and quality");
        put(map, "indicatore descrittivo", "descriptive indicator");
        put(map, "bin con FRACEXP ≈ 1", "bins with FRACEXP ≈ 1");
        put(map, "z cosmologico · valore BAT", "cosmological z · BAT value");
        put(map, "t = 0 indica il trigger", "t = 0 marks the trigger");
        put(map, "Azioni grafico", "Chart actions");
        put(map, "In breve", "At a glance");
        put(map, "Il punto zero dell'allerta", "The alert zero point");
        put(map, "Tempi negativi: prima del trigger. Tempi positivi: dopo il trigger. Il trigger non coincide necessariamente con l'inizio fisico esatto del burst.",
                "Negative times are before the trigger; positive times are after it. The trigger does not necessarily coincide with the exact physical onset of the burst.");
        put(map, "Curva di luce a binning di 1 secondo", "1-second binned light curve");
        put(map, "Tutte le bande", "All bands");
        put(map, "±20 s dal trigger", "±20 s from trigger");
        put(map, "±60 s dal trigger", "±60 s from trigger");
        put(map, "±120 s dal trigger", "±120 s from trigger");
        put(map, "Una lettura guidata dell'evento", "A guided reading of the event");
        put(map, "Questa pagina collega le parole tecniche ai dati che stai osservando. Non devi memorizzare tutto: usa le spiegazioni come legenda ragionata.",
                "This page connects technical terms to the data you are viewing. You do not need to memorize everything: use the explanations as a reasoned legend.");
        put(map, "Il trigger è il momento zero", "The trigger is time zero");
        put(map, "Lo strumento riconosce un aumento significativo e genera un'allerta. Nei grafici trasformiamo quel momento in t = 0. I dati prima del trigger hanno tempo negativo; quelli dopo hanno tempo positivo.",
                "The instrument recognizes a significant increase and generates an alert. In the plots that instant is set to t = 0. Data before the trigger have negative time; data after it have positive time.");
        put(map, "Il rate descrive l'intensità nel tempo", "Rate describes intensity over time");
        put(map, "Ogni riga corrisponde a un intervallo di un secondo. RATE indica il segnale netto stimato in quell'intervallo. Più è alto, più la curva è intensa in quel momento.",
                "Each row corresponds to a one-second interval. RATE is the estimated net signal in that interval. The higher it is, the stronger the light curve is at that time.");
        put(map, "Le energie sono separate in quattro bande", "Energy is split into four bands");
        put(map, "Il file ASCII divide il segnale in 15–25, 25–50, 50–100 e 100–350 keV. La curva totale 15–350 keV riunisce queste componenti.",
                "The ASCII file divides the signal into 15–25, 25–50, 50–100 and 100–350 keV. The total 15–350 keV curve combines these components.");
        put(map, "ERROR e FRACEXP controllano l'affidabilità", "ERROR and FRACEXP describe reliability");
        put(map, "ERROR esprime l'incertezza statistica del rate. FRACEXP indica quanta parte del secondo è stata realmente esposta: 1 significa bin completo.",
                "ERROR expresses the statistical uncertainty of the rate. FRACEXP indicates how much of the second was actually exposed: 1 means a complete bin.");
        put(map, "Il FITS è più di una tabella", "A FITS file is more than a table");
        put(map, "Contiene sia i numeri della curva sia le intestazioni tecniche: strumento, date, identificativi, coordinate, sistema temporale e dettagli di elaborazione.",
                "It contains both the light-curve numbers and technical headers: instrument, dates, identifiers, coordinates, time system and processing details.");
        put(map, "Indicatori calcolati per questo evento", "Indicators computed for this event");
        put(map, "Limite scientifico importante", "Important scientific limitation");
        put(map, "Gli indicatori presenti nell'app sono descrittivi.", "The indicators in the app are descriptive.");
        put(map, "La vista 3D, il picco, la media, la deviazione standard e la durezza proxy aiutano a esplorare l'evento, ma non sostituiscono il calcolo ufficiale di T90 né una classificazione short/long validata.",
                "The 3D view, peak, mean, standard deviation and hardness proxy help explore the event, but they do not replace the official T90 calculation or a validated short/long classification.");

        // Add exact translations for every data-dictionary explanation.
        for (FieldDefinition definition : OnlineGrbService.dictionary()) {
            String[] english = englishDefinition(definition.field());
            put(map, definition.simpleExplanation(), english[0]);
            put(map, definition.technicalExplanation(), english[1]);
            put(map, definition.whyItMatters(), english[2]);
            put(map, definition.caution(), english[3]);
        }
        return Map.copyOf(map);
    }

    private static String[] englishDefinition(String field) {
        String name = field == null ? "" : field;
        if (name.equals("TIME_FROM_TRIGGER_CENTER_S")) return four(
                "Time at the center of each bin relative to the BAT trigger.",
                "Bin-center time in seconds with trigger time defined as t = 0.",
                "It places every rate sample on the light-curve time axis and aligns different events to the same zero point.",
                "A negative value means a time before the trigger; do not confuse it with an impossible physical time.");
        if (name.equals("TIME_FROM_TRIGGER_START_S")) return four(
                "Time at the start of each bin relative to the trigger.",
                "Bin-start time in seconds, before adding half of TIMEDEL to obtain the center.",
                "It is useful when the start boundary must be distinguished from the bin center.",
                "With 1-second bins, start and center differ by about 0.5 s.");
        if (name.equals("TIME_MET_S")) return four(
                "Swift mission elapsed time recorded for the measurement.",
                "Absolute mission-time coordinate defined by the FITS timing reference.",
                "It links the measurement to other products from the same observation.",
                "For reading a burst light curve, trigger-relative time is usually more intuitive.");
        if (name.equals("RATE")) return four(
                "Net signal rate measured in that bin over the product energy band.",
                "Background-corrected net count rate for the FITS time bin.",
                "It is the main quantity plotted in the light curve.",
                "It can be negative because of statistical fluctuations after background subtraction.");
        if (name.equals("ERROR")) return four(
                "Statistical uncertainty associated with RATE in the same bin.",
                "Estimated statistical error of the background-corrected rate.",
                "It helps assess whether a peak is clearly distinguished from noise.",
                "It does not by itself include every possible instrumental systematic uncertainty.");
        if (name.equals("TOTCOUNTS")) return four(
                "Total detector events recorded in the bin.",
                "Instrumental count accumulated over the corresponding time interval.",
                "It is useful for technical checks and for understanding the available counting statistics.",
                "It is not necessarily equal to net rate because RATE includes corrections and background subtraction.");
        if (name.equals("FRACEXP")) return four(
                "Fraction of the time bin that was effectively exposed.",
                "Fractional exposure, normally between 0 and 1.",
                "It identifies partially exposed bins and summarizes data coverage quality.",
                "A value below 1 does not automatically make the bin unusable.");
        if (name.startsWith("RATE_") && name.endsWith("_KEV")) {
            String band = band(name.substring(5, name.length() - 4));
            return four("Net count rate in the " + band + " energy band.",
                    "Background-subtracted 1-second ASCII rate assigned to " + band + ".",
                    "Comparing it with the other bands shows whether the emission is concentrated at lower or higher energies.",
                    "A negative value can result from background subtraction and does not mean physically negative counts.");
        }
        if (name.startsWith("ERROR_") && name.endsWith("_KEV")) {
            String band = band(name.substring(6, name.length() - 4));
            return four("Uncertainty of the rate in the " + band + " energy band.",
                    "Statistical error associated with RATE in the same energy band.",
                    "It helps distinguish robust variations from fluctuations compatible with noise.",
                    "Read it together with RATE from the same time interval and energy band.");
        }
        return switch (name) {
            case "TRIGTIME" -> four("Mission time chosen as the GRB alert zero point.", "Mission elapsed time at which BAT declared the trigger.", "It is the reference used to convert absolute mission time into seconds before or after the event.", "The trigger is not necessarily the exact physical onset of the emission.");
            case "TIMEDEL" -> four("Width of each time bin.", "Sampling step of the light curve.", "It defines the temporal resolution available in the product.", "With 1-second bins, much shorter phenomena are averaged within the same bin.");
            case "OBJECT" -> four("Event name written in the FITS file.", "Object identifier stored in the FITS product.", "It verifies that the file belongs to the selected GRB.", "Capitalization may differ from the catalog display name.");
            case "OBS_ID" -> four("Swift observation identifier.", "Unique code used in the mission archive.", "It links all products belonging to the same observation.", "Do not confuse it with the Trigger ID.");
            case "DATE-OBS" -> four("Date and time at which the observational product begins.", "UTC timestamp of the observation start.", "It places the dataset on an absolute time line.", "It does not necessarily indicate the exact GRB peak time.");
            case "DATE-END" -> four("Date and time at which the observational product ends.", "UTC timestamp of the end of the covered interval.", "It defines the absolute end of the observation window.", "The window can be much longer than the scientifically interesting burst phase.");
            case "TELESCOP" -> four("Satellite or mission that performed the observation.", "Name of the observing platform declared in the FITS header.", "It identifies the provenance of the data product.", "For this catalog the expected value is SWIFT.");
            case "INSTRUME" -> four("Instrument that collected the data.", "Detector responsible for the scientific product.", "It is essential for understanding the energy band and measurement characteristics.", "For these products the expected value is BAT.");
            case "TSTART" -> four("Absolute mission time at the start of the table.", "Mission elapsed time of the first temporal boundary of the product.", "It supports timing cross-checks with other files.", "It is less intuitive than trigger-relative time for reading the burst.");
            case "TSTOP" -> four("Absolute mission time at the end of the table.", "Mission elapsed time of the final temporal boundary of the product.", "It verifies how much time the product covers.", "It is not the physical T90 duration of the GRB.");
            case "EXPOSURE" -> four("Total effective exposure time.", "Useful observing time after the corrections applied by the data product.", "It indicates how much valid observation is available.", "It can differ from the simple TSTOP − TSTART interval.");
            case "RA_OBJ / DEC_OBJ" -> four("Celestial coordinates of the event.", "Right ascension and declination in the coordinate system declared by the FITS file.", "They locate the GRB on the sky.", "They are not a screen position or a location on Earth.");
            case "EXTNAME" -> four("Name of a FITS file section.", "Label of the Header/Data Unit, for example RATE or EBOUNDS.", "It identifies which block of the FITS file is being read.", "A FITS file can contain several tables and headers in one file.");
            default -> four("Scientific field " + name + ".", "Value documented in the Swift/BAT data product.", "Use it together with its unit, source and original FITS comment.", "Do not infer a physical meaning beyond the field definition and units.");
        };
    }

    private static String band(String token) {
        return token.replace('_', '–') + " keV";
    }

    private static String[] four(String a, String b, String c, String d) {
        return new String[]{a, b, c, d};
    }

    private static void put(Map<String, String> map, String italian, String english) {
        if (italian != null && !italian.isBlank() && english != null && !english.isBlank()) {
            map.put(italian, english);
        }
    }

    private static boolean hasStyle(Node node, String styleClass) {
        return node != null && node.getStyleClass().contains(styleClass);
    }

    private static boolean isInsideStyle(Node node, String styleClass) {
        Node current = node;
        while (current != null) {
            if (hasStyle(current, styleClass)) return true;
            current = current.getParent();
        }
        return false;
    }
}
