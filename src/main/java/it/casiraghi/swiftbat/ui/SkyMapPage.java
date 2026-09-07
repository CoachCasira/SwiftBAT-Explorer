package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.service.SkyCoordinates;
import it.casiraghi.swiftbat.ui.components.CelestialSpherePane;
import it.casiraghi.swiftbat.ui.components.MollweideSkyPane;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Esplorazione del catalogo sulla volta celeste: Mollweide 2D e sfera 3D.
 */
public final class SkyMapPage extends BorderPane {
    private static final String FILTER_ALL = "Tutti i GRB";
    private static final String FILTER_SHORT = "Short · T90 ≤ 2 s";
    private static final String FILTER_LONG = "Long · T90 > 2 s";
    private static final String FILTER_UNKNOWN = "T90 non disponibile";

    private final Consumer<CatalogEntry> openGrb;
    private final Map<String, CatalogEntry> baseCatalog = new HashMap<>();
    private List<SkyBurst> allBursts = List.of();
    private List<SkyBurst> visibleBursts = List.of();
    private SkyBurst selectedBurst;
    private boolean sphereView;

    private final MollweideSkyPane mollweide = new MollweideSkyPane();
    private final CelestialSpherePane sphere = new CelestialSpherePane();
    private final StackPane mapHost = new StackPane();

    private final Label status = UiFactory.label("Coordinate celesti non ancora caricate", "status-pill", "status-neutral");
    private final Label shownMetric = UiFactory.label("0", "sky-metric-value");
    private final Label shortMetric = UiFactory.label("0", "sky-metric-value");
    private final Label longMetric = UiFactory.label("0", "sky-metric-value");
    private final Label noT90Metric = UiFactory.label("0", "sky-metric-value");

    private final TextField search = new TextField();
    private final ComboBox<String> durationFilter = new ComboBox<>();
    private final ComboBox<String> redshiftFilter = new ComboBox<>();
    private final TextField raMin = compactField("0");
    private final TextField raMax = compactField("360");
    private final TextField decMin = compactField("-90");
    private final TextField decMax = compactField("90");
    private final TextField zMin = compactField("0");
    private final TextField zMax = compactField("10");
    private final CheckBox galacticPlane = new CheckBox("Piano galattico");

    private final Label selectedName = UiFactory.label("Nessun GRB selezionato", "sky-selected-title");
    private final Label selectedTrigger = UiFactory.label("—", "info-value");
    private final Label selectedRa = UiFactory.label("—", "info-value");
    private final Label selectedDec = UiFactory.label("—", "info-value");
    private final Label selectedT90 = UiFactory.label("—", "info-value");
    private final Label selectedClass = UiFactory.wrappedLabel("—", "info-value");
    private final Label selectedRedshift = UiFactory.wrappedLabel("—", "info-value");
    private final Label selectedCatalog = UiFactory.wrappedLabel("Seleziona un punto sulla mappa.", "sky-detail-note");
    private final Button openButton = UiFactory.button("Apri curve di luce →", "primary-button");

    public SkyMapPage(Consumer<CatalogEntry> openGrb) {
        this.openGrb = openGrb == null ? entry -> { } : openGrb;
        getStyleClass().add("page-root");
        setCenter(buildPage());
        mollweide.setOnSelect(this::selectBurst);
        sphere.setOnSelect(this::selectBurst);
        galacticPlane.setSelected(true);
        galacticPlane.selectedProperty().addListener((obs, oldValue, newValue) -> {
            mollweide.setShowGalacticPlane(newValue);
            sphere.setShowGalacticPlane(newValue);
        });
        configureFilters();
        configureOpenButton();
    }

    public void setBaseCatalog(List<CatalogEntry> entries) {
        baseCatalog.clear();
        if (entries != null) {
            for (CatalogEntry entry : entries) {
                baseCatalog.put(entry.grbName().toUpperCase(Locale.ROOT), entry);
            }
        }
        updateSelectedPanel();
    }

    public void showLoading(String text) {
        status.setText(text == null || text.isBlank() ? "Caricamento coordinate…" : text);
        setStatusStyle("status-neutral");
    }

    public void showError(String message) {
        status.setText(message == null || message.isBlank() ? "Coordinate non disponibili" : message);
        setStatusStyle("status-warning");
    }

    public void showWarning(String message) {
        status.setText(message == null || message.isBlank() ? "Dati scientifici caricati parzialmente" : message);
        setStatusStyle("status-warning");
    }

    public void setSkyBursts(List<SkyBurst> bursts) {
        allBursts = bursts == null ? List.of() : List.copyOf(bursts);
        status.setText(allBursts.size() + " GRB con coordinate BAT caricati");
        setStatusStyle("status-online");
        applyFilters();
    }

    private Node buildPage() {
        VBox page = new VBox(12);
        page.setPadding(new Insets(20, 26, 24, 26));
        page.getStyleClass().add("page-content");

        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleText = new VBox(5,
                UiFactory.label("Mappa celeste", "page-title"),
                UiFactory.wrappedLabel(
                        "Esplora la distribuzione dei GRB nel cielo con Mollweide 2D e sfera 3D interattiva.",
                        "page-subtitle"));
        HBox.setHgrow(titleText, Priority.ALWAYS);
        titleRow.getChildren().addAll(titleText, status);

        HBox metrics = new HBox(12);
        metrics.getStyleClass().add("sky-metric-row");
        VBox visibleMetric = skyMetric("VISIBILI", shownMetric, "dopo i filtri");
        VBox shortMetricCard = skyMetric("SHORT", shortMetric, "T90 ≤ 2 s");
        VBox longMetricCard = skyMetric("LONG", longMetric, "T90 > 2 s");
        VBox unknownMetricCard = skyMetric("SENZA T90", noT90Metric, "durata non disponibile");
        for (VBox metric : List.of(visibleMetric, shortMetricCard, longMetricCard, unknownMetricCard)) {
            metric.setMinWidth(0);
            metric.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(metric, Priority.ALWAYS);
        }
        metrics.getChildren().addAll(visibleMetric, shortMetricCard, longMetricCard, unknownMetricCard);

        VBox filters = buildFilters();
        HBox viewSwitch = buildViewSwitch();

        HBox content = new HBox(16);
        VBox mapCard = new VBox(8);
        mapCard.getStyleClass().add("card");
        mapCard.setPadding(new Insets(12));
        Button resetView = UiFactory.button("Centra", "ghost-button");
        resetView.setOnAction(event -> {
            mollweide.resetView();
            sphere.resetView();
        });
        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "secondary-button");
        fullscreen.setOnAction(event -> openMapFullscreen());
        HBox mapHead = new HBox(10,
                UiFactory.label("Cielo", "card-title"),
                resetView,
                fullscreen,
                UiFactory.spacer(),
                viewSwitch);
        mapHead.setAlignment(Pos.CENTER_LEFT);
        mapHost.getChildren().setAll(mollweide);
        mapHost.setMinHeight(390);
        mapHost.setPrefHeight(500);
        VBox.setVgrow(mapHost, Priority.ALWAYS);
        HBox.setHgrow(mapCard, Priority.ALWAYS);
        mapCard.getChildren().addAll(mapHead, buildSkyLegend(), mapHost,
                UiFactory.wrappedLabel(
                        "Rotellina: zoom · trascina: sposta/ruota · doppio clic: centra. Viola = piano galattico.",
                        "sky-map-caption"));

        VBox details = buildDetailsPanel();
        details.setPrefWidth(330);
        details.setMinWidth(300);
        content.getChildren().addAll(mapCard, details);
        HBox.setHgrow(mapCard, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);

        page.getChildren().addAll(titleRow, metrics, filters, content);

        ScrollPane scroll = new ScrollPane(page);
        scroll.getStyleClass().add("page-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox buildFilters() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(13, 15, 13, 15));

        HBox firstRow = new HBox(9);
        firstRow.setAlignment(Pos.CENTER_LEFT);
        search.setPromptText("Cerca GRB…");
        search.getStyleClass().add("modern-text-field");
        search.setPrefWidth(220);

        durationFilter.setItems(FXCollections.observableArrayList(
                FILTER_ALL, FILTER_SHORT, FILTER_LONG, FILTER_UNKNOWN));
        durationFilter.setValue(FILTER_ALL);
        durationFilter.getStyleClass().add("choice-box-modern");
        durationFilter.setPrefWidth(190);

        redshiftFilter.setItems(FXCollections.observableArrayList(
                "Con e senza redshift", "Solo con redshift", "Solo senza redshift"));
        redshiftFilter.setValue("Con e senza redshift");
        redshiftFilter.getStyleClass().add("choice-box-modern");
        redshiftFilter.setPrefWidth(185);

        galacticPlane.getStyleClass().add("modern-check");
        ToggleButton advanced = new ToggleButton("Filtri avanzati  ▾");
        advanced.getStyleClass().add("sky-toggle");
        Button apply = UiFactory.button("Applica", "secondary-button");
        Button reset = UiFactory.button("Reset", "ghost-button");
        apply.setOnAction(event -> applyFilters());
        reset.setOnAction(event -> resetFilters());
        search.setOnAction(event -> applyFilters());
        durationFilter.setOnAction(event -> applyFilters());
        redshiftFilter.setOnAction(event -> applyFilters());
        firstRow.getChildren().addAll(search, durationFilter, redshiftFilter, galacticPlane,
                UiFactory.spacer(), apply, reset);

        HBox advancedHeader = new HBox(8, advanced,
                UiFactory.label("RA, DEC e intervallo di redshift", "sky-filter-help"));
        advancedHeader.setAlignment(Pos.CENTER_LEFT);

        HBox rangeRow = new HBox(9);
        rangeRow.getStyleClass().add("advanced-filter-row");
        rangeRow.setAlignment(Pos.CENTER_LEFT);
        Label raLabel = UiFactory.label("RA", "filter-label");
        Label decLabel = UiFactory.label("DEC", "filter-label");
        Label zLabel = UiFactory.label("z", "filter-label");
        Label help = UiFactory.wrappedLabel("RA può attraversare 0°. Limiti/intervalli di z sono inclusi se compatibili.", "sky-filter-help");
        HBox.setHgrow(help, Priority.ALWAYS);
        rangeRow.getChildren().addAll(
                raLabel, raMin, UiFactory.label("–", "filter-label"), raMax,
                decLabel, decMin, UiFactory.label("–", "filter-label"), decMax,
                zLabel, zMin, UiFactory.label("–", "filter-label"), zMax,
                help);
        rangeRow.setVisible(false);
        rangeRow.setManaged(false);
        advanced.selectedProperty().addListener((obs, oldValue, selected) -> {
            rangeRow.setVisible(selected);
            rangeRow.setManaged(selected);
            advanced.setText(selected ? "Nascondi filtri avanzati  ▴" : "Filtri avanzati  ▾");
        });

        card.getChildren().addAll(firstRow, advancedHeader, rangeRow);
        return card;
    }


    private FlowPane buildSkyLegend() {
        FlowPane legend = new FlowPane(14, 6);
        legend.getStyleClass().add("sky-legend");
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
                legendItem("●", "Long · T90 > 2 s", "#54d7ff"),
                legendItem("●", "Short · T90 ≤ 2 s", "#ffae4a"),
                legendItem("●", "T90 non disponibile", "#9aa8bf"),
                legendItem("—", "Piano galattico", "#bd82ff"));
        return legend;
    }

    private Label legendItem(String symbol, String text, String color) {
        Label label = UiFactory.label(symbol + "  " + text, "sky-legend-item");
        label.setStyle("-fx-text-fill: " + color + ";");
        return label;
    }

    private HBox buildViewSwitch() {
        ToggleGroup group = new ToggleGroup();
        ToggleButton mollweideButton = new ToggleButton("Mollweide 2D");
        ToggleButton sphereButton = new ToggleButton("Sfera 3D");
        mollweideButton.getStyleClass().add("sky-toggle");
        sphereButton.getStyleClass().add("sky-toggle");
        mollweideButton.setToggleGroup(group);
        sphereButton.setToggleGroup(group);
        mollweideButton.setSelected(true);
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                mollweideButton.setSelected(true);
                return;
            }
            boolean showSphere = newToggle == sphereButton;
            sphereView = showSphere;
            mapHost.getChildren().setAll(showSphere ? sphere : mollweide);
            javafx.application.Platform.runLater(() -> {
                if (showSphere) sphere.resetView(); else mollweide.resetView();
            });
        });
        return new HBox(6, mollweideButton, sphereButton);
    }

    private void openMapFullscreen() {
        if (getScene() == null) {
            return;
        }
        if (sphereView) {
            CelestialSpherePane enlarged = new CelestialSpherePane();
            enlarged.setBursts(visibleBursts);
            enlarged.setShowGalacticPlane(galacticPlane.isSelected());
            enlarged.setOnSelect(this::selectBurst);
            if (selectedBurst != null) enlarged.select(selectedBurst);
            enlarged.setMinSize(700, 520);
            InPlaceFullscreen.show(this, "Mappa celeste · Sfera 3D", enlarged);
        } else {
            MollweideSkyPane enlarged = new MollweideSkyPane();
            enlarged.setBursts(visibleBursts);
            enlarged.setShowGalacticPlane(galacticPlane.isSelected());
            enlarged.setOnSelect(this::selectBurst);
            if (selectedBurst != null) enlarged.select(selectedBurst);
            enlarged.setMinSize(700, 520);
            InPlaceFullscreen.show(this, "Mappa celeste · Mollweide 2D", enlarged);
        }
    }

    private VBox buildDetailsPanel() {
        VBox details = new VBox(14);
        details.getStyleClass().add("card");
        details.setPadding(new Insets(18));
        selectedName.setWrapText(true);

        VBox rows = new VBox(10,
                detailRow("Trigger", selectedTrigger),
                detailRow("RA (J2000)", selectedRa),
                detailRow("DEC (J2000)", selectedDec),
                detailRow("T90", selectedT90),
                detailRow("Classe descrittiva", selectedClass),
                detailRow("Redshift", selectedRedshift));

        Label scientificNote = UiFactory.wrappedLabel(
                "La soglia a 2 s è mostrata soltanto come riferimento descrittivo tradizionale. La mappa non assegna da sola una classificazione scientifica definitiva. "
                        + "Seleziona un punto per leggere coordinate, T90, classe descrittiva e redshift. Le viste Mollweide 2D e Sfera 3D rappresentano lo stesso campione: cambia soltanto il modo in cui la distribuzione celeste viene esplorata.",
                "sky-science-note");
        scientificNote.setMaxWidth(Double.MAX_VALUE);
        scientificNote.setMaxHeight(Double.MAX_VALUE);
        scientificNote.setPrefHeight(155);
        openButton.setDisable(true);
        openButton.setMaxWidth(Double.MAX_VALUE);

        details.getChildren().addAll(
                UiFactory.label("GRB selezionato", "card-subtitle"),
                selectedName, rows, selectedCatalog, openButton,
                UiFactory.label("Nota scientifica", "card-title"), scientificNote);
        return details;
    }

    private HBox detailRow(String key, Label value) {
        HBox row = new HBox(8);
        Label label = UiFactory.label(key, "info-key");
        label.setMinWidth(112);
        value.setWrapText(true);
        HBox.setHgrow(value, Priority.ALWAYS);
        row.getChildren().addAll(label, value);
        return row;
    }

    private VBox skyMetric(String title, Label value, String detail) {
        VBox box = new VBox(4,
                UiFactory.label(title, "sky-metric-label"),
                value,
                UiFactory.label(detail, "sky-metric-detail"));
        box.getStyleClass().add("sky-metric-card");
        box.setPrefWidth(230);
        return box;
    }

    private void configureFilters() {
        search.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.length() < 2 || newValue.length() > oldValue.length()) {
                applyFilters();
            }
        });
    }

    private void configureOpenButton() {
        openButton.setOnAction(event -> {
            if (selectedBurst == null) {
                return;
            }
            CatalogEntry entry = baseCatalog.get(selectedBurst.grbName().toUpperCase(Locale.ROOT));
            if (entry != null) {
                openGrb.accept(entry);
            }
        });
    }

    private void resetFilters() {
        search.clear();
        durationFilter.setValue(FILTER_ALL);
        redshiftFilter.setValue("Con e senza redshift");
        raMin.setText("0");
        raMax.setText("360");
        decMin.setText("-90");
        decMax.setText("90");
        zMin.setText("0");
        zMax.setText("10");
        galacticPlane.setSelected(true);
        applyFilters();
    }

    private void applyFilters() {
        Range range;
        try {
            range = readRange();
        } catch (IllegalArgumentException exception) {
            status.setText(exception.getMessage());
            setStatusStyle("status-warning");
            return;
        }

        String query = search.getText() == null ? "" : search.getText().trim().toUpperCase(Locale.ROOT);
        String duration = durationFilter.getValue() == null ? FILTER_ALL : durationFilter.getValue();
        String redshift = redshiftFilter.getValue() == null ? "Con e senza redshift" : redshiftFilter.getValue();
        double minimumZ;
        double maximumZ;
        try {
            minimumZ = parseField(zMin, 0.0, "Redshift minimo");
            maximumZ = parseField(zMax, 10.0, "Redshift massimo");
            if (minimumZ < 0.0 || minimumZ > maximumZ) {
                throw new IllegalArgumentException("Controlla il range del redshift.");
            }
        } catch (IllegalArgumentException exception) {
            status.setText(exception.getMessage());
            setStatusStyle("status-warning");
            return;
        }
        List<SkyBurst> filtered = new ArrayList<>();
        for (SkyBurst burst : allBursts) {
            if (!query.isEmpty() && !burst.grbName().contains(query) && !burst.triggerId().contains(query)) {
                continue;
            }
            if (duration.equals(FILTER_SHORT) && !burst.isShort()) {
                continue;
            }
            if (duration.equals(FILTER_LONG) && !burst.isLong()) {
                continue;
            }
            if (duration.equals(FILTER_UNKNOWN) && burst.hasT90()) {
                continue;
            }
            boolean hasRedshift = burst.redshift().available();
            if (redshift.equals("Solo con redshift") && !hasRedshift) {
                continue;
            }
            if (redshift.equals("Solo senza redshift") && hasRedshift) {
                continue;
            }
            if (hasRedshift && !burst.redshift().matches(minimumZ, maximumZ)) {
                continue;
            }
            if (!range.containsRa(burst.raDeg()) || burst.decDeg() < range.decMin() || burst.decDeg() > range.decMax()) {
                continue;
            }
            filtered.add(burst);
        }
        visibleBursts = List.copyOf(filtered);
        if (selectedBurst != null && visibleBursts.stream().noneMatch(b -> b.grbName().equals(selectedBurst.grbName()))) {
            selectedBurst = null;
            mollweide.select(null);
            sphere.select(null);
            updateSelectedPanel();
        }
        mollweide.setBursts(visibleBursts);
        sphere.setBursts(visibleBursts);
        updateMetrics();
        if (!allBursts.isEmpty()) {
            status.setText(visibleBursts.size() + " / " + allBursts.size() + " GRB visualizzati");
            setStatusStyle("status-online");
        }
    }

    private Range readRange() {
        double minRa = parseField(raMin, 0.0, "RA minima");
        double maxRa = parseField(raMax, 360.0, "RA massima");
        double minDec = parseField(decMin, -90.0, "DEC minima");
        double maxDec = parseField(decMax, 90.0, "DEC massima");
        if (minRa < 0.0 || minRa > 360.0 || maxRa < 0.0 || maxRa > 360.0) {
            throw new IllegalArgumentException("RA deve essere compresa tra 0° e 360°.");
        }
        if (minDec < -90.0 || minDec > 90.0 || maxDec < -90.0 || maxDec > 90.0 || minDec > maxDec) {
            throw new IllegalArgumentException("Controlla il range DEC: deve essere tra −90° e +90°.");
        }
        return new Range(minRa, maxRa, minDec, maxDec);
    }

    private double parseField(TextField field, double fallback, String name) {
        String value = field.getText() == null ? "" : field.getText().trim().replace(',', '.');
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " non è un numero valido.");
        }
    }

    private void selectBurst(SkyBurst burst) {
        selectedBurst = burst;
        mollweide.select(burst);
        sphere.select(burst);
        updateSelectedPanel();
    }

    private void updateSelectedPanel() {
        if (selectedBurst == null) {
            selectedName.setText("Nessun GRB selezionato");
            selectedTrigger.setText("—");
            selectedRa.setText("—");
            selectedDec.setText("—");
            selectedT90.setText("—");
            selectedClass.setText("—");
            selectedRedshift.setText("—");
            selectedCatalog.setText("Seleziona un punto sulla mappa.");
            openButton.setDisable(true);
            return;
        }
        selectedName.setText(selectedBurst.grbName());
        selectedTrigger.setText(selectedBurst.triggerId().isBlank() ? "n.d." : selectedBurst.triggerId());
        selectedRa.setText(String.format(Locale.ITALY, "%.5f°", selectedBurst.raDeg())
                + "  ·  " + SkyCoordinates.raToHms(selectedBurst.raDeg()));
        selectedDec.setText(String.format(Locale.ITALY, "%+.5f°", selectedBurst.decDeg())
                + "  ·  " + SkyCoordinates.decToDms(selectedBurst.decDeg()));
        selectedT90.setText(selectedBurst.formattedT90());
        selectedClass.setText(selectedBurst.durationClass());
        selectedRedshift.setText(selectedBurst.redshift().detail());
        CatalogEntry entry = baseCatalog.get(selectedBurst.grbName().toUpperCase(Locale.ROOT));
        if (entry != null) {
            selectedCatalog.setText("Questo evento è presente nel catalogo Swift/BAT usato dall'app: puoi aprire direttamente curve, FITS e metadati.");
            openButton.setDisable(false);
        } else {
            selectedCatalog.setText("Coordinate disponibili nella tabella generale, ma questo evento non è presente nel catalogo BAT caricato dall'Explorer.");
            openButton.setDisable(true);
        }
    }

    private void updateMetrics() {
        long shortCount = visibleBursts.stream().filter(SkyBurst::isShort).count();
        long longCount = visibleBursts.stream().filter(SkyBurst::isLong).count();
        long unknownCount = visibleBursts.size() - shortCount - longCount;
        shownMetric.setText(Integer.toString(visibleBursts.size()));
        shortMetric.setText(Long.toString(shortCount));
        longMetric.setText(Long.toString(longCount));
        noT90Metric.setText(Long.toString(unknownCount));
    }

    private void setStatusStyle(String style) {
        status.getStyleClass().removeAll("status-neutral", "status-online", "status-warning");
        status.getStyleClass().add(style);
    }

    private static TextField compactField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("sky-range-field");
        field.setPrefWidth(62);
        return field;
    }

    private record Range(double raMin, double raMax, double decMin, double decMax) {
        boolean containsRa(double ra) {
            if (raMin == 0.0 && raMax == 360.0) {
                return true;
            }
            if (raMin <= raMax) {
                return ra >= raMin && ra <= raMax;
            }
            return ra >= raMin || ra <= raMax;
        }
    }
}
