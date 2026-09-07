package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.SpectralData;
import it.casiraghi.swiftbat.model.SpectralData.EnergyFluxBand;
import it.casiraghi.swiftbat.model.SpectralData.Fit;
import it.casiraghi.swiftbat.model.SpectralData.Interval;
import it.casiraghi.swiftbat.model.SpectralData.Model;
import it.casiraghi.swiftbat.service.SpectralCatalogService;
import it.casiraghi.swiftbat.ui.components.ThreeDChartPane;
import it.casiraghi.swiftbat.ui.components.TimeEnergyHeatmapPane;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Scheda spettroscopica di un singolo GRB. */
public final class SpectroscopyPane extends BorderPane {
    private static final String AUTOMATIC_MODEL = "Modello indicato da BAT";
    private static final String POWER_LAW_MODEL = "Power law (PL)";
    private static final String CUTOFF_MODEL = "Cutoff power law (CPL)";

    private final GrbData grbData;
    private final SpectralData spectralData;
    private final HostServices hostServices;
    private final ChoiceBox<Interval> intervalChoice = new ChoiceBox<>();
    private final ChoiceBox<String> modelChoice = new ChoiceBox<>();
    private final StackPane resultHost = new StackPane();

    public SpectroscopyPane(GrbData grbData, SpectralData spectralData, HostServices hostServices) {
        this.grbData = grbData;
        this.spectralData = spectralData;
        this.hostServices = hostServices;
        getStyleClass().add("spectroscopy-pane");
        setPadding(new Insets(12));
        setCenter(buildContent());
        refreshOfficialResult();
    }

    private Node buildContent() {
        VBox root = new VBox(12);
        root.getStyleClass().add("spectroscopy-content");

        HBox heading = new HBox(12);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox copy = new VBox(4,
                UiFactory.label("Analisi spettroscopica", "section-title"),
                UiFactory.wrappedLabel(
                        "Fit PL/CPL e flussi ufficiali Swift/BAT. La mappa tempo–energia usa invece i quattro rate ASCII a bin di 1 secondo.",
                        "section-caption"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label provenance = UiFactory.label("DATI UFFICIALI BAT · FIT XSPEC", "status-pill", "status-online");
        heading.getChildren().addAll(copy, provenance);

        intervalChoice.setItems(FXCollections.observableArrayList(Interval.values()));
        intervalChoice.setValue(preferredInterval());
        intervalChoice.getStyleClass().add("choice-box-modern");
        intervalChoice.setPrefWidth(220);
        UiFactory.autoTooltip(intervalChoice);

        modelChoice.setItems(FXCollections.observableArrayList(
                AUTOMATIC_MODEL, POWER_LAW_MODEL, CUTOFF_MODEL));
        modelChoice.setValue(AUTOMATIC_MODEL);
        modelChoice.getStyleClass().add("choice-box-modern");
        modelChoice.setPrefWidth(240);
        UiFactory.autoTooltip(modelChoice);

        Button source = UiFactory.button("Apri tabella ufficiale ↗", "ghost-button");
        source.setOnAction(event -> hostServices.showDocument(sourceUrl()));
        HBox controls = new HBox(10,
                controlBox("Intervallo", intervalChoice),
                controlBox("Modello visualizzato", modelChoice),
                UiFactory.spacer(), source);
        controls.setAlignment(Pos.BOTTOM_LEFT);
        controls.getStyleClass().add("spectroscopy-controls");

        intervalChoice.valueProperty().addListener((obs, oldValue, newValue) -> refreshOfficialResult());
        modelChoice.valueProperty().addListener((obs, oldValue, newValue) -> refreshOfficialResult());

        TabPane tabs = new TabPane();
        tabs.getStyleClass().addAll("main-tabs", "spectroscopy-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("Risultati ufficiali", resultHost),
                new Tab("Mappa tempo–energia", buildTimeEnergyTab()),
                new Tab("Guida scientifica", buildGuide()));
        tabs.setMinHeight(590);
        tabs.setPrefHeight(720);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        root.getChildren().addAll(heading, controls, tabs);
        ScrollPane scroll = new ScrollPane(root);
        scroll.getStyleClass().add("page-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox controlBox(String label, Node control) {
        VBox box = new VBox(5, UiFactory.label(label, "filter-label"), control);
        box.getStyleClass().add("spectroscopy-control-box");
        return box;
    }

    private Interval preferredInterval() {
        if (spectralData != null && spectralData.result(Interval.T100) == null
                && spectralData.result(Interval.PEAK_ONE_SECOND) != null) {
            return Interval.PEAK_ONE_SECOND;
        }
        return Interval.T100;
    }

    private void refreshOfficialResult() {
        Interval interval = intervalChoice.getValue() == null ? Interval.T100 : intervalChoice.getValue();
        SpectralData.Result result = spectralData == null ? null : spectralData.result(interval);
        if (result == null || !result.available()) {
            resultHost.getChildren().setAll(missingResult(interval));
            return;
        }
        Model model = selectedModel(result);
        Fit fit = result.fit(model);
        List<EnergyFluxBand> fluxes = result.fluxes(model);
        resultHost.getChildren().setAll(buildOfficialResult(result, model, fit, fluxes));
    }

    private Model selectedModel(SpectralData.Result result) {
        String requested = modelChoice.getValue();
        if (CUTOFF_MODEL.equals(requested)) return Model.CUTOFF_POWER_LAW;
        if (POWER_LAW_MODEL.equals(requested)) return Model.POWER_LAW;
        if (result.bestModel() != null) return result.bestModel();
        return result.powerLaw() != null ? Model.POWER_LAW : Model.CUTOFF_POWER_LAW;
    }

    private Node missingResult(Interval interval) {
        VBox box = new VBox(11,
                UiFactory.label("Nessun risultato ufficiale disponibile", "empty-title"),
                UiFactory.wrappedLabel(
                        "Il catalogo spettroscopico BAT non contiene ancora " + grbData.grbName()
                                + " per l'intervallo " + interval.label()
                                + ". La curva di luce e la mappa tempo–energia rimangono comunque utilizzabili. "
                                + "Un aggiornamento dell'app rilegge automaticamente le tabelle online.",
                        "empty-message"));
        box.getStyleClass().add("spectroscopy-empty");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(70, 30, 70, 30));
        return box;
    }

    private Node buildOfficialResult(SpectralData.Result result, Model model, Fit fit,
                                     List<EnergyFluxBand> fluxes) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(12, 4, 18, 4));

        FlowPane metrics = new FlowPane(9, 9);
        metrics.getChildren().addAll(
                metric("Modello migliore BAT", result.bestModelCode(),
                        result.bestModel() == null ? "Nessun modello preferito pubblicato" : "scelta del catalogo"),
                metric("Modello mostrato", model.code(), model.label()),
                metric("Indice α", fit == null ? "n.d." : format(fit.alpha()),
                        fit == null ? "fit non disponibile" : confidence(fit.alphaLow(), fit.alphaHigh())),
                metric("Epeak", fit == null || !fit.hasConstrainedEPeak()
                                ? "n.d." : format(fit.ePeakKeV()) + " keV",
                        model == Model.POWER_LAW ? "non previsto dal modello PL" : "picco νFν del modello CPL"),
                metric("χ² ridotto", fit == null ? "n.d." : format(fit.reducedChiSquare()),
                        fit == null || fit.degreesOfFreedom() == null ? "qualità del fit" : fit.degreesOfFreedom() + " gradi di libertà"),
                metric("Esposizione", fit == null ? "n.d." : format(fit.exposureSeconds()) + " s",
                        fit == null ? "intervallo non disponibile" : intervalText(fit)));

        HBox charts = new HBox(12);
        Node modelChart = buildModelChart(fit);
        Node fluxChart = buildFluxChart(fluxes);
        HBox.setHgrow(modelChart, Priority.ALWAYS);
        HBox.setHgrow(fluxChart, Priority.ALWAYS);
        charts.getChildren().addAll(modelChart, fluxChart);

        HBox lower = new HBox(12,
                buildParameterCard(result, fit, model),
                buildFluxTable(fluxes),
                buildAssistant(result, fit, model, fluxes));
        for (Node node : lower.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);

        Label scientificNote = UiFactory.wrappedLabel(
                "I flussi sono quelli pubblicati dal catalogo Swift/BAT in erg cm⁻² s⁻¹, con limiti al 90%. "
                        + "La curva a sinistra ricostruisce il modello fotonico del fit; non è ricavata sommando i quattro rate della curva di luce.",
                "spectroscopy-note");
        content.getChildren().addAll(metrics, charts, lower, scientificNote);
        return content;
    }

    private VBox metric(String title, String value, String detail) {
        VBox card = UiFactory.metricCard(title, value, detail);
        card.getStyleClass().add("spectroscopy-metric");
        card.setPrefWidth(178);
        return card;
    }

    private Node buildModelChart(Fit fit) {
        VBox card = new VBox(7);
        card.getStyleClass().addAll("card", "spectroscopy-chart-card");
        card.setPadding(new Insets(12));
        Label explanation = UiFactory.wrappedLabel(
                "Forma continua del modello: asse Y = log₁₀ del flusso fotonico differenziale.",
                "card-subtitle");
        NumberAxis xAxis = new NumberAxis(15, 150, 15);
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Energia (keV)");
        yAxis.setLabel("log₁₀ N(E) [ph cm⁻² s⁻¹ keV⁻¹]");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getStyleClass().addAll("lightcurve-chart", "spectral-model-chart");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setTitle(fit == null ? "Modello non disponibile" : "Fit " + fit.model().code() + " · 15–150 keV");
        chart.setMinHeight(300);
        chart.setPrefHeight(340);
        chart.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.ALWAYS);
        if (fit != null && fit.normalization() != null && fit.alpha() != null) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            for (double energy = 15; energy <= 150.001; energy += 2.5) {
                double photons = photonModel(fit, energy);
                if (Double.isFinite(photons) && photons > 0) {
                    series.getData().add(new XYChart.Data<>(energy, Math.log10(photons)));
                }
            }
            chart.getData().add(series);
        }
        card.getChildren().addAll(UiFactory.label("Forma spettrale", "card-title"), explanation, chart);
        return card;
    }

    private Node buildFluxChart(List<EnergyFluxBand> fluxes) {
        VBox card = new VBox(7);
        card.getStyleClass().addAll("card", "spectroscopy-chart-card");
        card.setPadding(new Insets(12));
        Label explanation = UiFactory.wrappedLabel(
                "Ogni barra è il flusso energetico integrato in una banda non sovrapposta.",
                "card-subtitle");
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Flusso energetico (erg cm⁻² s⁻¹)");
        yAxis.setTickLabelFormatter(scientificConverter());
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.getStyleClass().addAll("distribution-chart", "spectral-flux-chart");
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setTitle("Flusso per banda · limiti al 90% nel dettaglio");
        chart.setCategoryGap(18);
        chart.setBarGap(3);
        chart.setMinHeight(300);
        chart.setPrefHeight(340);
        chart.setMaxWidth(Double.MAX_VALUE);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (EnergyFluxBand band : fluxes) {
            if (!band.available()) continue;
            XYChart.Data<String, Number> point = new XYChart.Data<>(band.label(), band.value());
            series.getData().add(point);
            Platform.runLater(() -> {
                if (point.getNode() != null) {
                    Tooltip.install(point.getNode(), UiFactory.quickTooltip(
                            band.label() + "\nFlusso: " + scientific(band.value())
                                    + " erg cm⁻² s⁻¹\nIntervallo 90%: "
                                    + scientific(band.lower90()) + " – " + scientific(band.upper90())));
                }
            });
        }
        chart.getData().add(series);
        VBox.setVgrow(chart, Priority.ALWAYS);
        card.getChildren().addAll(UiFactory.label("Flusso energetico", "card-title"), explanation, chart);
        return card;
    }

    private VBox buildParameterCard(SpectralData.Result result, Fit fit, Model model) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "spectroscopy-detail-card");
        card.setPadding(new Insets(14));
        card.getChildren().addAll(
                UiFactory.label("Parametri del fit", "card-title"),
                info("Intervallo", result.interval().description()),
                info("Modello", model.label() + " (" + model.code() + ")"),
                info("α, 90%", fit == null ? "n.d." : valueAndLimits(fit.alpha(), fit.alphaLow(), fit.alphaHigh())),
                info("Norm a 50 keV", fit == null ? "n.d." : scientific(fit.normalization()) + " ph cm⁻² s⁻¹ keV⁻¹"),
                info("Epeak, 90%", fit == null || !fit.hasConstrainedEPeak() ? "n.d. / non vincolato"
                        : valueAndLimits(fit.ePeakKeV(), fit.ePeakLowKeV(), fit.ePeakHighKeV()) + " keV"),
                info("χ² / dof", fit == null ? "n.d." : format(fit.chiSquare()) + " / " + integer(fit.degreesOfFreedom())),
                info("Probabilità nulla", fit == null ? "n.d." : scientific(fit.nullProbability())));
        return card;
    }

    private VBox buildFluxTable(List<EnergyFluxBand> fluxes) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "spectroscopy-detail-card");
        card.setPadding(new Insets(14));
        card.getChildren().add(UiFactory.label("Flussi e incertezza", "card-title"));
        if (fluxes.isEmpty()) {
            card.getChildren().add(UiFactory.wrappedLabel("Nessun flusso disponibile per il modello scelto.", "card-subtitle"));
            return card;
        }
        GridPane grid = new GridPane();
        grid.getStyleClass().add("spectroscopy-table");
        grid.setHgap(12);
        grid.setVgap(8);
        grid.add(UiFactory.label("Banda", "filter-label"), 0, 0);
        grid.add(UiFactory.label("Flusso", "filter-label"), 1, 0);
        grid.add(UiFactory.label("Intervallo 90%", "filter-label"), 2, 0);
        int row = 1;
        for (EnergyFluxBand band : fluxes) {
            grid.add(UiFactory.label(band.label(), "info-key"), 0, row);
            grid.add(UiFactory.label(scientific(band.value()), "info-value"), 1, row);
            grid.add(UiFactory.label(scientific(band.lower90()) + " – " + scientific(band.upper90()), "info-value"), 2, row);
            row++;
        }
        Label unit = UiFactory.wrappedLabel("Unità: erg cm⁻² s⁻¹. I limiti sono quelli pubblicati da BAT.", "card-subtitle");
        card.getChildren().addAll(grid, unit);
        return card;
    }

    private VBox buildAssistant(SpectralData.Result result, Fit fit, Model model,
                                List<EnergyFluxBand> fluxes) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "spectroscopy-detail-card", "spectroscopy-assistant");
        card.setPadding(new Insets(14));
        card.getChildren().add(UiFactory.label("Assistente di lettura", "card-title"));
        for (String insight : insights(result, fit, model, fluxes)) {
            card.getChildren().add(UiFactory.wrappedLabel("• " + insight, "assistant-copy"));
        }
        card.getChildren().add(UiFactory.wrappedLabel(
                "Descrizione automatica locale: aiuta a leggere i numeri, ma non sostituisce la valutazione spettroscopica dell'esperto.",
                "assistant-disclaimer"));
        return card;
    }

    private List<String> insights(SpectralData.Result result, Fit fit, Model model,
                                  List<EnergyFluxBand> fluxes) {
        List<String> resultLines = new ArrayList<>();
        if (result.bestModel() == null) {
            resultLines.add("BAT riporta “N/A” come modello migliore: PL e CPL sono consultabili, ma il catalogo non ne raccomanda uno.");
        } else {
            resultLines.add("Il catalogo indica " + result.bestModel().code() + " come modello preferito per questo intervallo.");
        }
        if (fit != null && fit.alpha() != null) {
            resultLines.add("L'indice α vale " + format(fit.alpha())
                    + ": valori più negativi descrivono una decrescita più rapida verso le energie alte.");
        }
        if (fit != null && model == Model.CUTOFF_POWER_LAW) {
            resultLines.add(fit.hasConstrainedEPeak()
                    ? "Il picco Epeak è vincolato a circa " + format(fit.ePeakKeV()) + " keV."
                    : "Epeak non è ben vincolato dai limiti pubblicati; non va interpretato come una misura robusta.");
        }
        if (fit != null && fit.reducedChiSquare() != null) {
            double value = fit.reducedChiSquare();
            resultLines.add(value >= 0.7 && value <= 1.3
                    ? "Il χ² ridotto è vicino a 1, quindi il modello è globalmente compatibile con i dati; va comunque letto con la probabilità nulla."
                    : "Il χ² ridotto si discosta da 1: il fit merita un controllo più attento insieme a residui e probabilità nulla.");
        }
        fluxes.stream().filter(EnergyFluxBand::available)
                .max(Comparator.comparingDouble(EnergyFluxBand::value))
                .ifPresent(band -> resultLines.add("Tra le quattro bande non sovrapposte, il flusso maggiore è in " + band.label() + "."));
        if (fit != null && fit.spectrumStartSeconds() != null && fit.spectrumStopSeconds() != null) {
            resultLines.add("Lo spettro integra da " + format(fit.spectrumStartSeconds()) + " s a "
                    + format(fit.spectrumStopSeconds()) + " s rispetto al trigger.");
        }
        return resultLines;
    }

    private Node buildTimeEnergyTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(14));
        Label description = UiFactory.wrappedLabel(
                "Questa è una mappa descrittiva dei rate a 1 secondo: X = tempo dal trigger, Y = quattro bande energetiche, colore = rate. "
                        + "Non è un fit XSPEC e non converte direttamente i conteggi in flusso fisico.",
                "section-caption");
        TimeEnergyHeatmapPane heatmap = new TimeEnergyHeatmapPane();
        heatmap.setData(grbData.asciiData());

        ChoiceBox<String> window = new ChoiceBox<>(FXCollections.observableArrayList(
                "±20 s", "±60 s", "±120 s", "Intera osservazione"));
        window.setValue("±60 s");
        window.getStyleClass().add("choice-box-modern");
        window.valueProperty().addListener((obs, oldValue, value) -> heatmap.setHalfWindowSeconds(switch (value) {
            case "±20 s" -> 20;
            case "±120 s" -> 120;
            case "Intera osservazione" -> Double.POSITIVE_INFINITY;
            default -> 60;
        }));
        Button threeD = UiFactory.button("Vista 3D tempo–energia  ⛶", "primary-button");
        threeD.setDisable(grbData.asciiData().isEmpty());
        threeD.setOnAction(event -> openTimeEnergy3D());
        HBox controls = new HBox(10,
                controlBox("Finestra attorno al trigger", window),
                UiFactory.spacer(), threeD);
        controls.setAlignment(Pos.BOTTOM_LEFT);

        VBox chartCard = new VBox(9,
                UiFactory.label("Mappa tempo–energia dei rate", "card-title"), heatmap);
        chartCard.getStyleClass().addAll("card", "time-energy-card");
        chartCard.setPadding(new Insets(14));
        VBox.setVgrow(heatmap, Priority.ALWAYS);
        box.getChildren().addAll(description, controls, chartCard,
                UiFactory.wrappedLabel(
                        "Nota: i rate BAT sono già corretti per il fondo; piccole celle negative rappresentano fluttuazioni statistiche dopo la sottrazione del fondo.",
                        "spectroscopy-note"));
        return box;
    }

    private void openTimeEnergy3D() {
        ThreeDChartPane chart = new ThreeDChartPane();
        chart.setContextName(grbData.grbName() + " · spettroscopia descrittiva");
        chart.setData(grbData.asciiData());
        VBox content = new VBox(chart);
        content.setMinSize(0, 0);
        VBox.setVgrow(chart, Priority.ALWAYS);
        InPlaceFullscreen.show(this, grbData.grbName() + " · Mappa tempo–energia 3D", content);
    }

    private Node buildGuide() {
        VBox guide = new VBox(12);
        guide.setPadding(new Insets(18));
        guide.getChildren().addAll(
                guideCard("1 · Che cosa arriva dal catalogo BAT",
                        "I parametri PL/CPL, il modello migliore, χ², gradi di libertà, probabilità nulla, intervallo dello spettro e flussi per banda sono letti dalle tabelle ufficiali. L'app non li inventa e non li ricalcola dai quattro canali."),
                guideCard("2 · PL e CPL",
                        "PL descrive lo spettro con una legge di potenza. CPL aggiunge un taglio esponenziale e può fornire Epeak. Se BAT scrive N/A, significa che non pubblica una scelta preferita tra i modelli per quel caso."),
                guideCard("3 · Flusso e frequenza",
                        "Il catalogo usa energia in keV, equivalente alla frequenza tramite E = hν. Le barre mostrano flussi energetici integrati in bande, non il rate della curva di luce."),
                guideCard("4 · Perché non basta il file ASCII",
                        "Per trasformare conteggi in flusso servono spettro PHA, matrice di risposta RSP/DRM, correzioni strumentali e un fit. I prodotti qui visualizzati sono già stati elaborati dalla pipeline BAT con XSPEC."),
                guideCard("5 · T100 e picco di 1 secondo",
                        "T100 riassume l'intero intervallo scelto per lo spettro del burst; il picco di 1 secondo descrive invece la fase più intensa su quella scala temporale. Rispondono a domande diverse e non vanno mescolati."));
        return guide;
    }

    private VBox guideCard(String title, String body) {
        VBox card = new VBox(7,
                UiFactory.label(title, "card-title"),
                UiFactory.wrappedLabel(body, "info-value"));
        card.getStyleClass().add("card");
        card.setPadding(new Insets(15));
        return card;
    }

    private HBox info(String key, String value) {
        return UiFactory.infoRow(key, value);
    }

    private String sourceUrl() {
        return intervalChoice.getValue() == Interval.PEAK_ONE_SECOND
                ? SpectralCatalogService.PEAK_ONE_SECOND_URL
                : SpectralCatalogService.T100_URL;
    }

    private double photonModel(Fit fit, double energyKeV) {
        double normEnergy = fit.normalizationEnergyKeV() == null || fit.normalizationEnergyKeV() <= 0
                ? 50.0 : fit.normalizationEnergyKeV();
        double value = fit.normalization() * Math.pow(energyKeV / normEnergy, fit.alpha());
        if (fit.model() == Model.CUTOFF_POWER_LAW && fit.ePeakKeV() != null && fit.ePeakKeV() > 0) {
            value *= Math.exp(-energyKeV * (2.0 + fit.alpha()) / fit.ePeakKeV());
        }
        return value;
    }

    private String intervalText(Fit fit) {
        if (fit.spectrumStartSeconds() == null || fit.spectrumStopSeconds() == null) return "intervallo spettroscopico";
        return format(fit.spectrumStartSeconds()) + " → " + format(fit.spectrumStopSeconds()) + " s dal trigger";
    }

    private String confidence(Double low, Double high) {
        return low == null || high == null ? "limiti al 90% n.d."
                : "90%: " + format(low) + " – " + format(high);
    }

    private String valueAndLimits(Double value, Double low, Double high) {
        return format(value) + " [" + format(low) + ", " + format(high) + "]";
    }

    private String format(Double value) {
        if (value == null || !Double.isFinite(value)) return "n.d.";
        double absolute = Math.abs(value);
        if (absolute != 0 && (absolute < 0.001 || absolute >= 10_000)) return scientific(value);
        return String.format(Locale.ITALIAN, "%.4g", value);
    }

    private String scientific(Double value) {
        if (value == null || !Double.isFinite(value)) return "n.d.";
        return String.format(Locale.ROOT, "%.4e", value).replace("e", " × 10^");
    }

    private String integer(Integer value) {
        return value == null ? "n.d." : value.toString();
    }

    private StringConverter<Number> scientificConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Number value) {
                if (value == null || value.doubleValue() == 0) return "0";
                return String.format(Locale.ROOT, "%.1e", value.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                try {
                    return Double.parseDouble(string);
                } catch (Exception ignored) {
                    return 0;
                }
            }
        };
    }
}
