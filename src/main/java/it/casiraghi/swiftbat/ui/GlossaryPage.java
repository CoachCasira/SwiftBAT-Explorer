package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.FieldDefinition;
import it.casiraghi.swiftbat.service.OnlineGrbService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Locale;

public final class GlossaryPage extends BorderPane {
    private final ObservableList<FieldDefinition> definitions = FXCollections.observableArrayList(OnlineGrbService.dictionary());
    private final FilteredList<FieldDefinition> filtered = new FilteredList<>(definitions, ignored -> true);
    private final ListView<FieldDefinition> list = new ListView<>(filtered);
    private final VBox detailHost = new VBox();

    public GlossaryPage() {
        getStyleClass().add("page-root");
        setPadding(new Insets(28, 34, 34, 34));
        setTop(buildHeader());
        setCenter(buildBody());
        BorderPane.setMargin(getCenter(), new Insets(22, 0, 0, 0));
        if (!definitions.isEmpty()) {
            list.getSelectionModel().selectFirst();
        }
    }

    private Node buildHeader() {
        VBox header = new VBox(7);
        header.getChildren().addAll(
                UiFactory.label("Dizionario dei dati", "page-title"),
                UiFactory.wrappedLabel(
                        "Ogni voce è spiegata prima in parole semplici e poi in modo tecnico. Qui puoi cercare TIME, RATE, FRACEXP, TRIGTIME, OBS_ID e gli altri campi presenti nelle tabelle e nei FITS.",
                        "page-subtitle"));
        return header;
    }

    private Node buildBody() {
        VBox left = new VBox(12);
        left.getStyleClass().add("dictionary-sidebar");
        left.setPadding(new Insets(18));
        TextField search = new TextField();
        search.setPromptText("Cerca un campo o un concetto…");
        search.getStyleClass().add("search-field");
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(item -> query.isBlank() || item.searchableText().contains(query));
        });

        Label count = UiFactory.label("", "sidebar-caption");
        Runnable refreshCount = () -> I18n.setText(count,
                filtered.size() + " voci documentate", filtered.size() + " documented entries");
        filtered.addListener((javafx.collections.ListChangeListener<FieldDefinition>) change -> refreshCount.run());
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> refreshCount.run());
        refreshCount.run();

        list.getStyleClass().add("dictionary-list");
        list.setCellFactory(ignored -> new DictionaryCell());
        list.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showDefinition(newValue);
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        left.getChildren().addAll(search, count, list);

        ScrollPane detailScroll = new ScrollPane(detailHost);
        detailScroll.getStyleClass().add("detail-scroll");
        detailScroll.setFitToWidth(true);
        detailHost.setPadding(new Insets(26));
        detailHost.setSpacing(18);

        SplitPane split = new SplitPane(left, detailScroll);
        split.getStyleClass().add("clean-split");
        split.setDividerPositions(0.31);
        return split;
    }

    private void showDefinition(FieldDefinition definition) {
        detailHost.getChildren().clear();
        HBox heading = new HBox(12);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(5,
                UiFactory.label(definition.field(), "definition-title"),
                UiFactory.label(I18n.t(definition.category()) + " · " + I18n.t(definition.source()), "definition-kicker"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label unit = UiFactory.label(definition.unit().isBlank() ? "senza unità" : definition.unit(), "unit-badge");
        heading.getChildren().addAll(titleBox, spacer, unit);

        VBox simple = explanationCard("In parole semplici", "◎", definitionText(definition, 0), "explanation-simple");
        VBox technical = explanationCard("Descrizione tecnica", "⌁", definitionText(definition, 1), "explanation-technical");
        VBox importance = explanationCard("Perché è utile", "↗", definitionText(definition, 2), "explanation-important");
        VBox caution = explanationCard("Attenzione a non confonderlo", "!", definitionText(definition, 3), "explanation-caution");

        detailHost.getChildren().addAll(heading, simple, technical, importance, caution);
    }

    private static String definitionText(FieldDefinition definition, int part) {
        if (definition == null) return "";
        if (I18n.language() == I18n.Language.IT) {
            return switch (part) {
                case 0 -> definition.simpleExplanation();
                case 1 -> definition.technicalExplanation();
                case 2 -> definition.whyItMatters();
                default -> definition.caution();
            };
        }
        String field = definition.field();
        String[] text = englishDefinition(field);
        return text[Math.max(0, Math.min(part, 3))];
    }

    private static String[] englishDefinition(String field) {
        String name = field == null ? "" : field;
        if (name.equals("TIME_FROM_TRIGGER_CENTER_S")) return four(
                "Time at the center of each bin relative to the BAT trigger.",
                "Bin-center time in seconds with trigger time defined as t = 0.",
                "It places every rate sample on the light-curve time axis.",
                "Do not confuse it with the bin start time.");
        if (name.equals("TIME_FROM_TRIGGER_START_S")) return four(
                "Time at the start of each bin relative to the trigger.",
                "Bin-start time in seconds, while the corresponding center is shifted by half a bin.",
                "It defines the exact temporal boundaries of each measurement.",
                "Do not use it as the bin-center coordinate.");
        if (name.equals("TIME_MET_S")) return four(
                "Mission elapsed time recorded in the FITS product.",
                "Absolute mission-time coordinate used by the Swift data product.",
                "It links a row to the mission timing system independently of trigger-relative time.",
                "It is not seconds from the GRB trigger.");
        if (name.equals("RATE")) return four(
                "Net count rate measured in the FITS light curve.",
                "Background-subtracted rate for the FITS time bin, expressed per second.",
                "It is the basic quantity used to follow how the burst intensity changes with time.",
                "It is a detector count rate, not a physical energy flux.");
        if (name.equals("ERROR")) return four(
                "Statistical uncertainty associated with RATE.",
                "Estimated one-bin uncertainty of the background-subtracted rate.",
                "It indicates how precisely the rate in that row is known.",
                "It is an uncertainty, not an additional signal channel.");
        if (name.equals("TOTCOUNTS")) return four(
                "Total counts associated with the FITS bin.",
                "Number of detector counts accumulated in the corresponding time interval.",
                "It helps relate the rate measurement to the amount of detected data.",
                "Do not interpret it as energy or energy flux.");
        if (name.equals("FRACEXP")) return four(
                "Fraction of the time bin that was effectively exposed.",
                "Exposure fraction between 0 and 1; values near 1 indicate nearly complete coverage.",
                "It is useful for identifying bins with incomplete observational coverage.",
                "A lower FRACEXP does not by itself mean a weaker GRB.");
        if (name.startsWith("RATE_") && name.endsWith("_KEV")) {
            String band = band(name.substring(5, name.length()-4));
            return four("Net count rate in the " + band + " energy band.",
                    "Background-subtracted 1-second ASCII rate for photons assigned to " + band + ".",
                    "It lets you compare the temporal signal between energy channels.",
                    "It is a count rate, not the BAT/XSPEC physical flux.");
        }
        if (name.startsWith("ERROR_") && name.endsWith("_KEV")) {
            String band = band(name.substring(6, name.length()-4));
            return four("Uncertainty of the rate in the " + band + " band.",
                    "Statistical error associated with the corresponding 1-second ASCII rate.",
                    "It shows how reliable each energy-channel measurement is.",
                    "Do not compare error amplitude as if it were signal intensity.");
        }
        return switch (name) {
            case "PEAK_RATE" -> four("Highest total rate found in the light curve.", "Maximum of the total 15–350 keV rate series.", "It identifies the strongest observed bin.", "It is a descriptive peak, not a spectral flux.");
            case "PEAK_TIME" -> four("Time of the maximum-rate bin.", "Trigger-relative center time of the bin containing PEAK_RATE.", "It locates the burst maximum with respect to t = 0.", "It is not the trigger time itself.");
            case "PEAK_ERROR" -> four("Uncertainty associated with the peak-rate bin.", "Statistical error read at the bin where PEAK_RATE occurs.", "It helps assess the precision of the measured peak.", "It is not the peak rate divided by error.");
            case "PEAK_SNR" -> four("Signal-to-error ratio at the peak.", "Descriptive ratio between peak rate and its statistical uncertainty.", "Higher values indicate a more significant peak measurement.", "It is a descriptive indicator, not a detection-classification rule.");
            case "MEAN_RATE" -> four("Average rate over the available light-curve rows.", "Arithmetic mean of the selected rate series.", "It summarizes the overall signal level.", "It can be influenced by background-dominated intervals.");
            case "RATE_STD" -> four("Spread of rate values around their mean.", "Standard deviation of the selected rate series.", "It gives a compact measure of temporal variability.", "It is not the uncertainty of a single bin.");
            case "NEGATIVE_FRACTION" -> four("Fraction of bins with a negative net rate.", "Share of background-subtracted bins whose net rate falls below zero.", "It helps characterize background fluctuations in the series.", "Negative net rates are statistical fluctuations, not negative photon emission.");
            case "FULL_EXPOSURE_FRACTION" -> four("Percentage of bins with nearly complete exposure.", "Fraction of FITS rows whose FRACEXP is approximately 1.", "It summarizes the overall coverage quality of the light curve.", "It is a coverage metric, not burst brightness.");
            case "HARDNESS_PROXY" -> four("Simple comparison between higher- and lower-energy signal.", "Descriptive ratio built from the available ASCII energy-band rates.", "It offers a quick view of relative spectral hardness.", "It is only a proxy and does not replace spectral fitting.");
            case "ASCII_ROWS" -> four("Number of rows read from the ASCII product.", "Count of valid one-second ASCII bins loaded by the app.", "It indicates how much temporal data is available in that product.", "It is not the burst duration by itself.");
            case "FITS_ROWS" -> four("Number of rows read from the FITS light-curve table.", "Count of valid FITS rate-table bins loaded by the app.", "It helps verify the extent of the FITS time series.", "It is not the number of photons.");
            case "BIN_SIZE" -> four("Duration represented by one time bin.", "Temporal resolution of the light-curve product, normally about one second here.", "It defines the time sampling of the plotted signal.", "It is not the total duration of the GRB.");
            case "TIME_RANGE" -> four("Temporal interval covered by the loaded light curve.", "Difference between the earliest and latest trigger-relative sample times.", "It tells you how much pre- and post-trigger data are available.", "It is not the official T90 duration.");
            default -> four("Scientific field " + name + ".", "Value documented in the Swift/BAT data product.", "Use it together with its unit and source when interpreting the event.", "Do not infer a physical meaning beyond the field definition and units.");
        };
    }

    private static String band(String token) {
        return token.replace('_', '–') + " keV";
    }

    private static String[] four(String a, String b, String c, String d) {
        return new String[]{a,b,c,d};
    }

    private VBox explanationCard(String title, String icon, String text, String styleClass) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("explanation-card", styleClass);
        HBox titleRow = new HBox(9);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = UiFactory.label(icon, "explanation-icon");
        Label titleLabel = UiFactory.label(title, "explanation-title");
        titleRow.getChildren().addAll(iconLabel, titleLabel);
        card.getChildren().addAll(titleRow, UiFactory.wrappedLabel(text, "explanation-text"));
        return card;
    }

    private static final class DictionaryCell extends ListCell<FieldDefinition> {
        @Override
        protected void updateItem(FieldDefinition item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            VBox box = new VBox(4);
            Label field = UiFactory.label(item.field(), "dictionary-field");
            Label source = UiFactory.label(I18n.t(item.category()) + " · " + I18n.t(item.source()), "dictionary-source");
            Label simple = UiFactory.wrappedLabel(definitionText(item, 0), "dictionary-preview");
            box.getChildren().addAll(field, source, simple);
            setGraphic(box);
        }
    }
}
