package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.service.SkyCoordinates;
import it.casiraghi.swiftbat.ui.components.CelestialSpherePane;
import it.casiraghi.swiftbat.ui.components.MollweideSkyPane;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
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
    private final PauseTransition filterDebounce = new PauseTransition(Duration.millis(900));

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
                UiFactory.label("Mappa celeste", "page-title"));
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
        Button exportPng = UiFactory.button("Esporta PNG", "ghost-button");
        exportPng.setOnAction(event -> exportMapNode(
                sphereView ? sphere : mollweide,
                sphereView ? "mappa_celeste_sfera_3D.png" : "mappa_celeste_mollweide_2D.png"));
        Button fullscreen = UiFactory.button("Schermo intero", "primary-button");
        fullscreen.setOnAction(event -> openMapFullscreen());
        HBox mapHead = new HBox(10,
                UiFactory.label("Cielo", "card-title"),
                resetView,
                exportPng,
                fullscreen,
                UiFactory.spacer(),
                viewSwitch);
        mapHead.setAlignment(Pos.CENTER_LEFT);
        mapHost.getChildren().setAll(mollweide);
        mapHost.setMinHeight(390);
        mapHost.setPrefHeight(500);
        VBox.setVgrow(mapHost, Priority.ALWAYS);
        HBox.setHgrow(mapCard, Priority.ALWAYS);
        mapCard.getChildren().addAll(mapHead, buildSkyLegend(), mapHost);

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
        VBox card = new VBox(7);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12, 14, 12, 14));

        HBox row = new HBox(7);
        row.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("Cerca GRB…");
        search.getStyleClass().add("modern-text-field");
        search.setMinWidth(145);
        search.setPrefWidth(165);
        search.setMaxWidth(175);

        durationFilter.setItems(FXCollections.observableArrayList(
                FILTER_ALL, FILTER_SHORT, FILTER_LONG, FILTER_UNKNOWN));
        durationFilter.setValue(FILTER_ALL);
        durationFilter.getStyleClass().add("choice-box-modern");
        durationFilter.setMinWidth(135);
        durationFilter.setPrefWidth(145);

        redshiftFilter.setItems(FXCollections.observableArrayList(
                "Con e senza redshift", "Solo con redshift", "Solo senza redshift"));
        redshiftFilter.setValue("Con e senza redshift");
        redshiftFilter.getStyleClass().add("choice-box-modern");
        redshiftFilter.setMinWidth(145);
        redshiftFilter.setPrefWidth(155);

        galacticPlane.getStyleClass().add("modern-check");
        Node redshiftControl = compactRedshiftRadios();
        Button reset = UiFactory.button("Reset", "ghost-button");
        reset.setOnAction(event -> resetFilters());
        search.setOnAction(event -> {
            filterDebounce.stop();
            applyFilters();
        });

        HBox raRange = compactSkyRange("RA", raMin, raMax);
        HBox decRange = compactSkyRange("DEC", decMin, decMax);
        HBox zRange = compactSkyRange("z", zMin, zMax);
        row.getChildren().addAll(search, durationFilter, redshiftControl,
                raRange, decRange, zRange, galacticPlane, reset);
        card.getChildren().add(row);
        return card;
    }

    private Node compactRedshiftRadios() {
        ToggleGroup group = new ToggleGroup();
        HBox row = new HBox(4);
        row.getStyleClass().add("compact-radio-group");
        String[] values = {"Con e senza redshift", "Solo con redshift", "Solo senza redshift"};
        String[] labels = {"Tutti", "Con z", "Senza z"};
        for (int index = 0; index < values.length; index++) {
            final String value = values[index];
            final String label = labels[index];
            javafx.scene.control.RadioButton radio = new javafx.scene.control.RadioButton(I18n.t(label));
            radio.getStyleClass().add("compact-radio");
            radio.setToggleGroup(group);
            radio.setUserData(value);
            radio.setSelected(value.equals(redshiftFilter.getValue()));
            radio.setOnAction(event -> redshiftFilter.setValue(value));
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> radio.setText(I18n.t(label)));
            row.getChildren().add(radio);
        }
        redshiftFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            for (javafx.scene.control.Toggle toggle : group.getToggles()) {
                if (java.util.Objects.equals(toggle.getUserData(), newValue)) {
                    group.selectToggle(toggle);
                    break;
                }
            }
        });
        return row;
    }

    private HBox compactSkyRange(String label, TextField minimum, TextField maximum) {
        minimum.setMinWidth(42);
        minimum.setPrefWidth(46);
        minimum.setMaxWidth(52);
        maximum.setMinWidth(42);
        maximum.setPrefWidth(46);
        maximum.setMaxWidth(52);
        HBox box = new HBox(4,
                UiFactory.label(label, "filter-label"),
                minimum,
                UiFactory.label("–", "filter-label"),
                maximum);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
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
        FullscreenDetails details = createFullscreenDetails();
        HBox layout = new HBox(14);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setMinSize(0, 0);

        if (sphereView) {
            CelestialSpherePane enlarged = new CelestialSpherePane();
            enlarged.setBursts(visibleBursts);
            enlarged.setShowGalacticPlane(galacticPlane.isSelected());
            enlarged.setOnSelect(burst -> {
                enlarged.select(burst);
                selectBurst(burst);
                details.update().accept(burst);
            });
            if (selectedBurst != null) enlarged.select(selectedBurst);
            enlarged.setMinSize(520, 420);
            enlarged.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            HBox.setHgrow(enlarged, Priority.ALWAYS);
            details.export().setOnAction(event -> exportMapNode(enlarged, "mappa_celeste_sfera_3D.png"));
            layout.getChildren().addAll(enlarged, details.node());
            InPlaceFullscreen.show(this, "Mappa celeste · Sfera 3D", layout);
        } else {
            MollweideSkyPane enlarged = new MollweideSkyPane();
            enlarged.setBursts(visibleBursts);
            enlarged.setShowGalacticPlane(galacticPlane.isSelected());
            enlarged.setOnSelect(burst -> {
                enlarged.select(burst);
                selectBurst(burst);
                details.update().accept(burst);
            });
            if (selectedBurst != null) enlarged.select(selectedBurst);
            enlarged.setMinSize(520, 420);
            enlarged.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            HBox.setHgrow(enlarged, Priority.ALWAYS);
            details.export().setOnAction(event -> exportMapNode(enlarged, "mappa_celeste_mollweide_2D.png"));
            layout.getChildren().addAll(enlarged, details.node());
            InPlaceFullscreen.show(this, "Mappa celeste · Mollweide 2D", layout);
        }
    }

    private FullscreenDetails createFullscreenDetails() {
        Label name = UiFactory.label("Nessun GRB selezionato", "sky-selected-title");
        Label trigger = UiFactory.label("—", "info-value");
        Label ra = UiFactory.wrappedLabel("—", "info-value");
        Label dec = UiFactory.wrappedLabel("—", "info-value");
        Label t90 = UiFactory.label("—", "info-value");
        Label clazz = UiFactory.wrappedLabel("—", "info-value");
        Label redshift = UiFactory.wrappedLabel("—", "info-value");
        Label catalogInfo = UiFactory.wrappedLabel("Seleziona un punto sulla mappa.", "sky-detail-note");
        Button open = UiFactory.button("Apri curve di luce →", "primary-button");
        open.setMaxWidth(Double.MAX_VALUE);
        open.setDisable(true);
        Button export = UiFactory.button("Esporta PNG", "ghost-button");
        export.setMaxWidth(Double.MAX_VALUE);
        final SkyBurst[] current = new SkyBurst[1];

        VBox rows = new VBox(14,
                detailRow("Trigger", trigger),
                detailRow("RA (J2000)", ra),
                detailRow("DEC (J2000)", dec),
                detailRow("T90", t90),
                detailRow("Classe descrittiva", clazz),
                detailRow("Redshift", redshift));
        Label note = UiFactory.wrappedLabel(
                "Seleziona un GRB direttamente nella vista a schermo intero. RA e DEC descrivono la direzione sulla volta celeste; T90 riassume la durata dell'evento e il redshift, quando disponibile, fornisce l'informazione cosmologica. I dettagli rimangono visibili mentre esplori la mappa e puoi aprire subito le relative curve di luce.",
                "sky-science-note", "sky-fullscreen-note");
        VBox panel = new VBox(18,
                UiFactory.label("GRB selezionato", "card-subtitle"),
                name, rows, catalogInfo, open, export,
                UiFactory.label("Come leggere la selezione", "card-title"), note);
        panel.getStyleClass().addAll("card", "sky-fullscreen-details");
        panel.setPadding(new Insets(22));
        panel.setMinWidth(360);
        panel.setPrefWidth(410);
        panel.setMaxWidth(450);
        panel.setMinHeight(0);
        panel.setMaxHeight(Double.MAX_VALUE);

        Consumer<SkyBurst> updater = burst -> {
            current[0] = burst;
            if (burst == null) {
                name.setText("Nessun GRB selezionato");
                trigger.setText("—");
                ra.setText("—");
                dec.setText("—");
                t90.setText("—");
                clazz.setText("—");
                redshift.setText("—");
                catalogInfo.setText("Seleziona un punto sulla mappa.");
                open.setDisable(true);
                return;
            }
            name.setText(burst.grbName());
            trigger.setText(burst.triggerId().isBlank() ? "n.d." : burst.triggerId());
            ra.setText(String.format(Locale.ITALY, "%.5f°", burst.raDeg())
                    + "  ·  " + SkyCoordinates.raToHms(burst.raDeg()));
            dec.setText(String.format(Locale.ITALY, "%+.5f°", burst.decDeg())
                    + "  ·  " + SkyCoordinates.decToDms(burst.decDeg()));
            t90.setText(burst.formattedT90());
            clazz.setText(I18n.t(burst.durationClass()));
            redshift.setText(localizedRedshift(burst));
            CatalogEntry entry = baseCatalog.get(burst.grbName().toUpperCase(Locale.ROOT));
            if (entry != null) {
                catalogInfo.setText("Evento presente nel catalogo Swift/BAT: puoi aprire direttamente curve, FITS e metadati.");
                open.setDisable(false);
            } else {
                catalogInfo.setText("Coordinate disponibili, ma l'evento non è presente nel catalogo BAT caricato dall'Explorer.");
                open.setDisable(true);
            }
        };
        open.setOnAction(event -> {
            SkyBurst burst = current[0];
            if (burst == null) return;
            CatalogEntry entry = baseCatalog.get(burst.grbName().toUpperCase(Locale.ROOT));
            if (entry != null) {
                InPlaceFullscreen.close(open);
                openGrb.accept(entry);
            }
        });
        updater.accept(selectedBurst);
        return new FullscreenDetails(panel, updater, export);
    }

    private void exportMapNode(Node node, String suggestedName) {
        if (node == null || getScene() == null || node.getBoundsInLocal().getWidth() <= 1
                || node.getBoundsInLocal().getHeight() <= 1) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Esporta mappa celeste");
        chooser.setInitialFileName(suggestedName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagine PNG", "*.png"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            WritableImage image = node.snapshot(new SnapshotParameters(), null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException | RuntimeException error) {
            new Alert(Alert.AlertType.ERROR,
                    "Esportazione PNG non riuscita: " + error.getMessage()).showAndWait();
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

        openButton.setDisable(true);
        openButton.setMaxWidth(Double.MAX_VALUE);

        details.getChildren().addAll(
                UiFactory.label("GRB selezionato", "card-subtitle"),
                selectedName, rows, openButton);
        return details;
    }

    private String localizedRedshift(SkyBurst burst) {
        if (burst == null || !burst.redshift().available()) {
            return I18n.dynamic("Redshift non disponibile nella tabella BAT.", "Redshift unavailable in the BAT table.");
        }
        StringBuilder value = new StringBuilder("z = ").append(burst.redshift().rawValue());
        if (!burst.redshift().method().isBlank() && !burst.redshift().method().equalsIgnoreCase("N/A")) {
            value.append(I18n.dynamic(" · metodo ", " · method ")).append(burst.redshift().method());
        }
        if (burst.redshift().uncertain()) {
            value.append(I18n.dynamic(" · valore indicato come incerto", " · value marked as uncertain"));
        }
        return value.toString();
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
        filterDebounce.setOnFinished(event -> applyFilters());
        search.textProperty().addListener((obs, oldValue, newValue) -> scheduleFilterApply());
        durationFilter.valueProperty().addListener((obs, oldValue, newValue) -> scheduleFilterApply());
        redshiftFilter.valueProperty().addListener((obs, oldValue, newValue) -> scheduleFilterApply());
        for (TextField field : List.of(raMin, raMax, decMin, decMax, zMin, zMax)) {
            field.textProperty().addListener((obs, oldValue, newValue) -> scheduleFilterApply());
        }
    }

    private void scheduleFilterApply() {
        filterDebounce.stop();
        filterDebounce.playFromStart();
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
        filterDebounce.stop();
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
        selectedClass.setText(I18n.t(selectedBurst.durationClass()));
        selectedRedshift.setText(localizedRedshift(selectedBurst));
        CatalogEntry entry = baseCatalog.get(selectedBurst.grbName().toUpperCase(Locale.ROOT));
        if (entry != null) {
            openButton.setDisable(false);
        } else {
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

    private record FullscreenDetails(VBox node, Consumer<SkyBurst> update, Button export) {
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
