from pathlib import Path

ROOT = Path('.')

def repl(path, old, new):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'pattern not found in {path}: {old[:120]!r}')
    p.write_text(text.replace(old, new), encoding='utf-8')

# Population: give every filter breathing room and keep FRACEXP compact but readable.
repl('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java',
'''        exposureBox.setMinWidth(225);
        exposureBox.setPrefWidth(240);
        exposureBox.setMaxWidth(255);

        VBox durationGroup = filterGroup("Durata T90", "", duration, 150);
        VBox redshiftGroup = filterGroup("Redshift", "", redshiftControl, 190);
        VBox windowGroup = filterGroup("Finestra temporale", "", windowControl, 185);
        VBox limitGroup = filterGroup("Campione massimo", "", limit, 120);
        HBox topFilters = new HBox(7, durationGroup, redshiftGroup, windowGroup, exposureBox, limitGroup);''',
'''        exposureBox.setMinWidth(270);
        exposureBox.setPrefWidth(280);
        exposureBox.setMaxWidth(295);

        VBox durationGroup = filterGroup("Durata T90", "", duration, 150);
        VBox redshiftGroup = filterGroup("Redshift", "", redshiftControl, 185);
        VBox windowGroup = filterGroup("Finestra temporale", "", windowControl, 180);
        VBox limitGroup = filterGroup("Campione massimo", "", limit, 125);
        HBox topFilters = new HBox(14, durationGroup, redshiftGroup, windowGroup, exposureBox, limitGroup);''')

# Population status: never feed a composed Italian sentence to strict I18n.
repl('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java',
'''        StringBuilder message = new StringBuilder()
                .append(result.accepted().size()).append(" GRB inclusi su ")
                .append(result.examined()).append(" esaminati");
        if (!result.measured().isEmpty()) {
            double observedMinimum = result.measured().stream()
                    .mapToDouble(PopulationEvent::exposurePercent).min().orElse(Double.NaN);
            double observedMaximum = result.measured().stream()
                    .mapToDouble(PopulationEvent::exposurePercent).max().orElse(Double.NaN);
            message.append(String.format(Locale.ITALY, " · copertura rilevata %.1f%%–%.1f%%",
                    observedMinimum, observedMaximum));
            if (result.accepted().isEmpty()) {
                message.append(String.format(Locale.ITALY, " fuori dal filtro %.0f%%–%.0f%%",
                        result.exposureMinimum(), result.exposureMaximum()));
            }
        }
        if (result.failures() > 0) {
            message.append(" · ").append(result.failures()).append(" non leggibili");
        }
        setStatus(message.toString(),
                result.accepted().isEmpty() ? "status-warning" : "status-online");''',
'''        StringBuilder messageIt = new StringBuilder()
                .append(result.accepted().size()).append(" GRB inclusi su ")
                .append(result.examined()).append(" esaminati");
        StringBuilder messageEn = new StringBuilder()
                .append(result.accepted().size()).append(" GRBs included out of ")
                .append(result.examined()).append(" examined");
        if (!result.measured().isEmpty()) {
            double observedMinimum = result.measured().stream()
                    .mapToDouble(PopulationEvent::exposurePercent).min().orElse(Double.NaN);
            double observedMaximum = result.measured().stream()
                    .mapToDouble(PopulationEvent::exposurePercent).max().orElse(Double.NaN);
            messageIt.append(String.format(Locale.ITALY, " · copertura rilevata %.1f%%–%.1f%%",
                    observedMinimum, observedMaximum));
            messageEn.append(String.format(Locale.US, " · observed coverage %.1f%%–%.1f%%",
                    observedMinimum, observedMaximum));
            if (result.accepted().isEmpty()) {
                messageIt.append(String.format(Locale.ITALY, " fuori dal filtro %.0f%%–%.0f%%",
                        result.exposureMinimum(), result.exposureMaximum()));
                messageEn.append(String.format(Locale.US, " outside filter %.0f%%–%.0f%%",
                        result.exposureMinimum(), result.exposureMaximum()));
            }
        }
        if (result.failures() > 0) {
            messageIt.append(" · ").append(result.failures()).append(" non leggibili");
            messageEn.append(" · ").append(result.failures()).append(" unreadable");
        }
        status.setText(I18n.dynamic(messageIt.toString(), messageEn.toString()));
        setStatusStyle(result.accepted().isEmpty() ? "status-warning" : "status-online");''')

# Glossary: localize the scientific definition content explicitly instead of sending whole paragraphs to strict lookup.
repl('src/main/java/it/casiraghi/swiftbat/ui/GlossaryPage.java',
'''        VBox simple = explanationCard("In parole semplici", "◎", definition.simpleExplanation(), "explanation-simple");
        VBox technical = explanationCard("Descrizione tecnica", "⌁", definition.technicalExplanation(), "explanation-technical");
        VBox importance = explanationCard("Perché è utile", "↗", definition.whyItMatters(), "explanation-important");
        VBox caution = explanationCard("Attenzione a non confonderlo", "!", definition.caution(), "explanation-caution");''',
'''        VBox simple = explanationCard("In parole semplici", "◎", definitionText(definition, 0), "explanation-simple");
        VBox technical = explanationCard("Descrizione tecnica", "⌁", definitionText(definition, 1), "explanation-technical");
        VBox importance = explanationCard("Perché è utile", "↗", definitionText(definition, 2), "explanation-important");
        VBox caution = explanationCard("Attenzione a non confonderlo", "!", definitionText(definition, 3), "explanation-caution");''')
repl('src/main/java/it/casiraghi/swiftbat/ui/GlossaryPage.java',
'''            Label simple = UiFactory.wrappedLabel(item.simpleExplanation(), "dictionary-preview");
            box.getChildren().addAll(field, source, simple);''',
'''            Label simple = UiFactory.wrappedLabel(definitionText(item, 0), "dictionary-preview");
            box.getChildren().addAll(field, source, simple);''')
# make nested cell able to call helper by leaving helper static
insert_anchor = '''    private VBox explanationCard(String title, String icon, String text, String styleClass) {
'''
helper = '''    private static String definitionText(FieldDefinition definition, int part) {
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

'''
p = ROOT / 'src/main/java/it/casiraghi/swiftbat/ui/GlossaryPage.java'
text = p.read_text(encoding='utf-8')
if insert_anchor not in text:
    raise SystemExit('Glossary insert anchor not found')
p.write_text(text.replace(insert_anchor, helper + insert_anchor), encoding='utf-8')

# Sky map: remove the explanatory block from the normal selected-GRB card and localize dynamic values.
repl('src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java',
'''        Label scientificNote = UiFactory.wrappedLabel(
                "La soglia a 2 s è mostrata soltanto come riferimento descrittivo tradizionale. La mappa non assegna da sola una classificazione scientifica definitiva. "
                        + "Seleziona un punto per leggere coordinate, T90, classe descrittiva e redshift. Le viste Mollweide 2D e Sfera 3D rappresentano lo stesso campione: cambia soltanto il modo in cui la distribuzione celeste viene esplorata.",
                "sky-science-note");
        scientificNote.setMaxWidth(Double.MAX_VALUE);
        scientificNote.setMaxHeight(Double.MAX_VALUE);
        scientificNote.setPrefHeight(155);
        openButton.setDisable(true);''',
'''        openButton.setDisable(true);''')
repl('src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java',
'''        details.getChildren().addAll(
                UiFactory.label("GRB selezionato", "card-subtitle"),
                selectedName, rows, selectedCatalog, openButton,
                UiFactory.collapsibleHelp("", scientificNote));''',
'''        details.getChildren().addAll(
                UiFactory.label("GRB selezionato", "card-subtitle"),
                selectedName, rows, openButton);''')
# Remove catalog note from normal panel updates if present but keep fullscreen catalogInfo untouched.
p = ROOT / 'src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java'
text = p.read_text(encoding='utf-8')
text = text.replace('    private final Label selectedCatalog = UiFactory.wrappedLabel("Seleziona un punto sulla mappa.", "sky-detail-note");\n', '')
text = text.replace('                selectedCatalog.setText("Seleziona un punto sulla mappa.");\n', '')
text = text.replace('                selectedCatalog.setText("Evento presente nel catalogo Swift/BAT: curve, FITS e metadati sono disponibili.");\n', '')
text = text.replace('                selectedCatalog.setText("Coordinate disponibili, ma il GRB non è presente nel catalogo BAT caricato.");\n', '')
# Localize dynamic class/redshift in both regular/fullscreen assignment paths.
text = text.replace('            clazz.setText(burst.durationClass());\n            redshift.setText(burst.redshift().detail());',
                    '            clazz.setText(I18n.t(burst.durationClass()));\n            redshift.setText(localizedRedshift(burst));')
text = text.replace('        selectedClass.setText(selectedBurst.durationClass());\n        selectedRedshift.setText(selectedBurst.redshift().detail());',
                    '        selectedClass.setText(I18n.t(selectedBurst.durationClass()));\n        selectedRedshift.setText(localizedRedshift(selectedBurst));')
anchor = '    private HBox detailRow(String key, Label value) {\n'
helper2 = '''    private String localizedRedshift(SkyBurst burst) {
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

'''
if anchor not in text:
    raise SystemExit('SkyMap helper anchor not found')
text = text.replace(anchor, helper2 + anchor)
p.write_text(text, encoding='utf-8')

print('i18n/layout fixes applied')