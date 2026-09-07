from pathlib import Path


def rep(path: str, old: str, new: str, label: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Pattern non trovato ({label}) in {path}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


population = "src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java"
ui = "src/main/java/it/casiraghi/swiftbat/ui/UiFactory.java"
css = "src/main/resources/app.css"

# Tooltip a 500 ms fin dalla prima visualizzazione.
rep(ui, "import javafx.scene.text.Text;\n", "import javafx.scene.text.Text;\nimport javafx.util.Duration;\n", "import Duration")
rep(ui,
    "choice.setTooltip(value == null || value.toString().isBlank() ? null : new Tooltip(value.toString()));",
    "choice.setTooltip(value == null || value.toString().isBlank() ? null : quickTooltip(value.toString()));",
    "tooltip ChoiceBox")
rep(ui, "control.setTooltip(new Tooltip(text));", "control.setTooltip(quickTooltip(text));", "tooltip label")
rep(ui, "button.setTooltip(new Tooltip(tooltip));", "button.setTooltip(quickTooltip(tooltip));", "tooltip icona")
rep(ui,
    "    public static Button iconButton(String glyph, String tooltip) {",
    """    public static Tooltip quickTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(500));
        tooltip.setHideDelay(Duration.millis(80));
        tooltip.setShowDuration(Duration.seconds(30));
        return tooltip;
    }

    public static Button iconButton(String glyph, String tooltip) {""",
    "helper quickTooltip")

# Sostituisce Slider FRACEXP con campi numerici e pulsanti +/-.
rep(population,
    "import javafx.scene.control.ScrollPane;\nimport javafx.scene.control.Slider;\nimport javafx.scene.control.Tab;",
    "import javafx.scene.control.ScrollPane;\nimport javafx.scene.control.Tab;",
    "rimozione import Slider")
rep(population,
    """    private final Slider exposureMin = slider(0);
    private final Slider exposureMax = slider(100);
    private final Label exposureMinValue = UiFactory.label(\"0%\", \"filter-value\");
    private final Label exposureMaxValue = UiFactory.label(\"100%\", \"filter-value\");""",
    """    private final TextField exposureMin = percentField(\"0\");
    private final TextField exposureMax = percentField(\"100\");""",
    "campi FRACEXP")
rep(population,
    """        VBox minimum = exposureControl(\"Minimo ammesso\", exposureMin, exposureMinValue);
        VBox maximum = exposureControl(\"Massimo ammesso\", exposureMax, exposureMaxValue);""",
    """        VBox minimum = exposureControl(\"Minimo ammesso\", exposureMin, true);
        VBox maximum = exposureControl(\"Massimo ammesso\", exposureMax, false);""",
    "costruzione FRACEXP")
rep(population,
    """    private void configureControls() {
        exposureMin.valueProperty().addListener((obs, oldValue, value) -> {
            if (value.doubleValue() > exposureMax.getValue()) {
                exposureMin.setValue(exposureMax.getValue());
            }
            updateExposureLabel();
            updateCandidatePreview();
        });
        exposureMax.valueProperty().addListener((obs, oldValue, value) -> {
            if (value.doubleValue() < exposureMin.getValue()) {
                exposureMax.setValue(exposureMin.getValue());
            }
            updateExposureLabel();
            updateCandidatePreview();
        });""",
    """    private void configureControls() {
        exposureMin.textProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        exposureMax.textProperty().addListener((obs, oldValue, value) -> updateCandidatePreview());
        exposureMin.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) normalizeExposureField(exposureMin, true);
        });
        exposureMax.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) normalizeExposureField(exposureMax, false);
        });
        exposureMin.setOnAction(event -> normalizeExposureField(exposureMin, true));
        exposureMax.setOnAction(event -> normalizeExposureField(exposureMax, false));""",
    "listener FRACEXP")
rep(population,
    """        return new Filter(duration.getValue(), redshiftAvailability.getValue(), minZ, maxZ,
                minRa, maxRa, minDec, maxDec, exposureMin.getValue(), exposureMax.getValue(),
                halfWindow, maximumEvents);""",
    """        double minExposure = percentage(exposureMin, \"Copertura FRACEXP minima\");
        double maxExposure = percentage(exposureMax, \"Copertura FRACEXP massima\");
        if (minExposure > maxExposure) {
            throw new IllegalArgumentException(\"Il minimo FRACEXP non può superare il massimo.\");
        }
        return new Filter(duration.getValue(), redshiftAvailability.getValue(), minZ, maxZ,
                minRa, maxRa, minDec, maxDec, minExposure, maxExposure,
                halfWindow, maximumEvents);""",
    "lettura FRACEXP")
rep(population,
    """    private double number(TextField field, String label) {
        try {
            return Double.parseDouble(field.getText().trim().replace(',', '.'));
        } catch (Exception error) {
            throw new IllegalArgumentException(label + \" non è un numero valido.\");
        }
    }""",
    """    private double number(TextField field, String label) {
        try {
            return Double.parseDouble(field.getText().trim().replace(',', '.'));
        } catch (Exception error) {
            throw new IllegalArgumentException(label + \" non è un numero valido.\");
        }
    }

    private double percentage(TextField field, String label) {
        double value = number(field, label);
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(label + \" deve essere compresa fra 0% e 100%.\");
        }
        return value;
    }""",
    "validazione percentuale")
rep(population,
    """        exposureMin.setValue(0);
        exposureMax.setValue(100);""",
    """        exposureMin.setText(\"0\");
        exposureMax.setText(\"100\");""",
    "reset FRACEXP")
rep(population,
    """    private void updateExposureLabel() {
        exposureMinValue.setText(String.format(Locale.ITALY, \"%.0f%%\", exposureMin.getValue()));
        exposureMaxValue.setText(String.format(Locale.ITALY, \"%.0f%%\", exposureMax.getValue()));
    }

""",
    "",
    "rimozione updateExposureLabel")
rep(population,
    """    private static Slider slider(double value) {
        Slider slider = new Slider(0, 100, value);
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(false);
        slider.setPadding(new Insets(0, 12, 0, 12));
        slider.setMinWidth(0);
        slider.setPrefWidth(320);
        slider.setMaxWidth(Double.MAX_VALUE);
        return slider;
    }

    private static VBox exposureControl(String label, Slider slider, Label value) {
        HBox heading = new HBox(8, UiFactory.label(label, \"filter-label\"), UiFactory.spacer(), value);
        heading.setAlignment(Pos.CENTER_LEFT);
        slider.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(7, heading, slider);
        box.setMinWidth(220);
        box.setPrefWidth(320);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(\"-fx-background-color: rgba(13, 20, 35, 0.55); -fx-background-radius: 10; -fx-padding: 9 10 8 10;\");
        return box;
    }""",
    """    private static TextField percentField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add(\"percentage-field\");
        field.setAlignment(Pos.CENTER);
        field.setMinWidth(72);
        field.setPrefWidth(86);
        field.setMaxWidth(96);
        return field;
    }

    private VBox exposureControl(String label, TextField field, boolean minimum) {
        Button minus = UiFactory.button(\"−\", \"percentage-step-button\");
        Button plus = UiFactory.button(\"+\", \"percentage-step-button\");
        minus.setOnAction(event -> adjustExposure(field, -1, minimum));
        plus.setOnAction(event -> adjustExposure(field, 1, minimum));
        minus.setTooltip(UiFactory.quickTooltip(\"Riduci di 1%\"));
        plus.setTooltip(UiFactory.quickTooltip(\"Aumenta di 1%\"));

        Label unit = UiFactory.label(\"%\", \"percentage-unit\");
        HBox stepper = new HBox(8, minus, field, unit, plus);
        stepper.setAlignment(Pos.CENTER_LEFT);
        Label hint = UiFactory.label(\"Scrivi un valore da 0 a 100 oppure usa − / +\", \"filter-detail\");
        VBox box = new VBox(7, UiFactory.label(label, \"filter-label\"), stepper, hint);
        box.getStyleClass().add(\"percentage-control\");
        box.setMinWidth(250);
        box.setPrefWidth(330);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void adjustExposure(TextField field, int delta, boolean minimum) {
        int fallback = minimum ? 0 : 100;
        Integer current = parseExposureInteger(field);
        int value = Math.max(0, Math.min(100, (current == null ? fallback : current) + delta));
        Integer other = parseExposureInteger(minimum ? exposureMax : exposureMin);
        if (other != null) {
            value = minimum ? Math.min(value, other) : Math.max(value, other);
        }
        field.setText(Integer.toString(value));
    }

    private void normalizeExposureField(TextField field, boolean minimum) {
        Integer value = parseExposureInteger(field);
        if (value == null) value = minimum ? 0 : 100;
        value = Math.max(0, Math.min(100, value));
        Integer other = parseExposureInteger(minimum ? exposureMax : exposureMin);
        if (other != null) {
            value = minimum ? Math.min(value, other) : Math.max(value, other);
        }
        field.setText(Integer.toString(value));
    }

    private Integer parseExposureInteger(TextField field) {
        try {
            return (int) Math.round(Double.parseDouble(field.getText().trim().replace(',', '.')));
        } catch (Exception error) {
            return null;
        }
    }""",
    "stepper FRACEXP")
rep(population,
    """            Tooltip.install(item, new Tooltip(
                    \"A ogni secondo si ordinano i valori delle curve: il 25° percentile lascia sotto di sé il 25% dei valori, \"
                            + \"il 75° percentile ne lascia sotto il 75%. Tra i due rimane quindi il 50% centrale del campione.\"));""",
    """            Tooltip.install(item, UiFactory.quickTooltip(
                    \"A ogni secondo si ordinano i valori delle curve: il 25° percentile lascia sotto di sé il 25% dei valori, \"
                            + \"il 75° percentile ne lascia sotto il 75%. Tra i due rimane quindi il 50% centrale del campione.\"));""",
    "tooltip quartili")

css_path = Path(css)
css_text = css_path.read_text(encoding="utf-8")
css_text += """

/* FRACEXP numeric steppers */
.percentage-control {
    -fx-background-color: rgba(255, 255, 255, 0.025);
    -fx-border-color: rgba(255, 174, 74, 0.10);
    -fx-background-radius: 11px;
    -fx-border-radius: 11px;
    -fx-padding: 10 12;
}

.percentage-field {
    -fx-background-color: rgba(6, 10, 17, 0.92);
    -fx-text-fill: #fff2e2;
    -fx-font-size: 15px;
    -fx-font-weight: bold;
    -fx-background-radius: 9px;
    -fx-border-radius: 9px;
    -fx-border-color: rgba(255, 174, 74, 0.18);
    -fx-padding: 8 10;
}

.percentage-field:focused {
    -fx-border-color: rgba(255, 183, 101, 0.72);
    -fx-effect: dropshadow(gaussian, rgba(255, 174, 74, 0.13), 10, 0.18, 0, 0);
}

.percentage-step-button {
    -fx-background-color: rgba(255, 174, 74, 0.08);
    -fx-text-fill: #ffc47e;
    -fx-border-color: rgba(255, 174, 74, 0.15);
    -fx-background-radius: 9px;
    -fx-border-radius: 9px;
    -fx-font-size: 16px;
    -fx-font-weight: bold;
    -fx-min-width: 36px;
    -fx-min-height: 36px;
    -fx-padding: 5px;
    -fx-cursor: hand;
}

.percentage-step-button:hover {
    -fx-background-color: rgba(255, 174, 74, 0.15);
    -fx-border-color: rgba(255, 190, 118, 0.30);
}

.percentage-unit {
    -fx-text-fill: #ffbd75;
    -fx-font-size: 13px;
    -fx-font-weight: bold;
    -fx-min-width: 18px;
}
"""
css_path.write_text(css_text, encoding="utf-8")

print("Correzioni FRACEXP e tooltip applicate.")
