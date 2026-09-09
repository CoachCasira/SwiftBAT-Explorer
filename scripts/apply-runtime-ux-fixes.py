from pathlib import Path

ROOT = Path('.')

def replace(path, old, new):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Pattern not found in {path}: {old[:100]!r}')
    p.write_text(text.replace(old, new), encoding='utf-8')

# 1) DisplayFormat: localize scientific units at formatting time so dynamic values never become missing translations.
replace('src/main/java/it/casiraghi/swiftbat/ui/DisplayFormat.java',
'''        if (item.unit() == null || item.unit().isBlank()) {
            return value;
        }
        return value + (item.unit().equals("%") ? "%" : " " + item.unit());''',
'''        if (item.unit() == null || item.unit().isBlank()) {
            return value;
        }
        String unit = I18n.t(item.unit());
        return value + (item.unit().equals("%") ? "%" : " " + unit);''')

# 2) Compare: keep exactly two suggestions visible and let the loading state render before creating the chart.
replace('src/main/java/it/casiraghi/swiftbat/ui/ComparePage.java',
'''import javafx.application.Platform;
import javafx.collections.FXCollections;''',
'''import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;''')
replace('src/main/java/it/casiraghi/swiftbat/ui/ComparePage.java',
'''import javafx.scene.layout.VBox;

import java.util.ArrayList;''',
'''import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;''')
replace('src/main/java/it/casiraghi/swiftbat/ui/ComparePage.java',
'''        showComparisonLoading(aName, bName, false);
        Platform.runLater(() -> {
            if (version != comparisonVersion) return;
            if (!aName.equals(selected(first)) || !bName.equals(selected(second))) return;
            GrbData currentA = sessionData.get(aName);
            GrbData currentB = sessionData.get(bName);
            if (currentA == null || currentB == null) return;
            content.getChildren().setAll(buildComparison(currentA, currentB));
            I18n.localizeTree(content);
        });''',
'''        showComparisonLoading(aName, bName, false);
        // Give JavaFX one pulse to paint the loading state before building the chart.
        PauseTransition pause = new PauseTransition(Duration.millis(140));
        pause.setOnFinished(event -> {
            if (version != comparisonVersion) return;
            if (!aName.equals(selected(first)) || !bName.equals(selected(second))) return;
            GrbData currentA = sessionData.get(aName);
            GrbData currentB = sessionData.get(bName);
            if (currentA == null || currentB == null) return;
            content.getChildren().setAll(buildComparison(currentA, currentB));
            I18n.localizeTree(content);
        });
        pause.play();''')

# 3) Population: compact FRACEXP and shorten English radio labels by using the existing localized values.
replace('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java',
'''        exposureBox.setMinWidth(270);
        exposureBox.setPrefWidth(285);
        exposureBox.setMaxWidth(300);

        VBox durationGroup = filterGroup("Durata T90", "", duration, 155);
        VBox redshiftGroup = filterGroup("Redshift", "", redshiftControl, 205);
        VBox windowGroup = filterGroup("Finestra temporale", "", windowControl, 195);
        VBox limitGroup = filterGroup("Campione massimo", "", limit, 125);''',
'''        exposureBox.setMinWidth(225);
        exposureBox.setPrefWidth(240);
        exposureBox.setMaxWidth(255);

        VBox durationGroup = filterGroup("Durata T90", "", duration, 150);
        VBox redshiftGroup = filterGroup("Redshift", "", redshiftControl, 190);
        VBox windowGroup = filterGroup("Finestra temporale", "", windowControl, 185);
        VBox limitGroup = filterGroup("Campione massimo", "", limit, 120);''')

# 4) Fullscreen explanation: force a stable layout immediately when the panel is toggled.
replace('src/main/java/it/casiraghi/swiftbat/ui/InPlaceFullscreen.java',
'''            help.selectedProperty().addListener((obs, oldValue, selected) -> {
                help.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
                relayout.run();
            });''',
'''            help.selectedProperty().addListener((obs, oldValue, selected) -> {
                help.setText(I18n.t(selected ? "Nascondi spiegazione" : "Mostra spiegazione"));
                relayout.run();
                // The time-energy canvas and the reading ScrollPane both depend on their final bounds.
                // Re-layout on the next JavaFX pulse to avoid the temporary tiny panel/text jump.
                Platform.runLater(() -> {
                    relayout.run();
                    split.applyCss();
                    split.requestLayout();
                });
            });''')
replace('src/main/java/it/casiraghi/swiftbat/ui/InPlaceFullscreen.java',
'''            readingScroll.viewportBoundsProperty().addListener((obs, oldBounds, bounds) ->
                    reading.setMinHeight(Math.max(0, bounds.getHeight())));''',
'''            // Do not bind the reading card minimum height to transient viewport bounds: during the
            // first fullscreen pulse those bounds can be tiny and cause the explanation to collapse.
            reading.setMinHeight(Region.USE_PREF_SIZE);''')

# 5) I18n: translations still surfaced by current runtime screenshots and shorter labels.
i18n = ROOT / 'src/main/java/it/casiraghi/swiftbat/ui/I18n.java'
text = i18n.read_text(encoding='utf-8')
anchor = '        put("Le curve sono divise per il proprio picco: il confronto riguarda la forma relativa, non la luminosità assoluta.",\n                "Curves are divided by their own peak: the comparison concerns relative shape, not absolute luminosity.");\n'
extra = '''        // Runtime strings verified from EN screenshots (2026-09-09)\n        put("T90 duration", "T90 duration");\n        put("FRACEXP quality", "FRACEXP quality");\n        put("Maximum sample", "Maximum sample");\n        put("Population analysis", "Population analysis");\n        put("Temporal profile", "Temporal profile");\n        put("Sample distributions", "Sample distributions");\n        put("Included GRBs", "Included GRBs");\n        put("Peak time", "Peak time");\n        put("Peak / error", "Peak / error");\n        put("Hardness proxy", "Hardness proxy");\n        put("Full exposure", "Full exposure");\n        put("No z", "No z");\n        put("Numero di GRB", "Number of GRBs");\n        put("Tempo normalizzato (t / T90)", "Normalized time (t / T90)");\n        put("Rate normalizzato (picco = 1)", "Normalized rate (peak = 1)");\n        put("Finestra", "Window");\n        put("Zoom", "Zoom");\n        put("Bande energetiche", "Energy bands");\n        put("trigger  t = 0", "trigger  t = 0");\n'''
if anchor not in text:
    raise SystemExit('I18n anchor not found')
text = text.replace(anchor, anchor + extra)
i18n.write_text(text, encoding='utf-8')

# 6) CSS: JavaFX Modena derives chart-bar backgrounds from -fx-bar-fill and requires a Color.
# Keep the visible gradient, but provide a real Color token so Modena never receives a String/gradient there.
css = ROOT / 'src/main/resources/app.css'
css_text = css.read_text(encoding='utf-8')
needle = '''.bar-chart .chart-bar {
    -fx-background-color: linear-gradient(to top, #7b56c9, #4fd5ef);
    -fx-background-radius: 5px 5px 0 0;
}'''
replacement = '''.bar-chart .chart-bar {
    -fx-bar-fill: #4fd5ef;
    -fx-background-color: linear-gradient(to top, #7b56c9, #4fd5ef);
    -fx-background-radius: 5px 5px 0 0;
}'''
if needle not in css_text:
    raise SystemExit('bar-chart CSS block not found')
css_text = css_text.replace(needle, replacement)
css_text += '''\n\n/* Runtime polish 1.3.0 */\n.population-fracexp-inline { -fx-padding: 8px 9px; }\n.population-fracexp-inline .percentage-control { -fx-padding: 5px 6px; }\n.population-fracexp-inline .percentage-field { -fx-font-size: 12px; -fx-padding: 4px 5px; }\n.population-fracexp-inline .percentage-step-button { -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; }\n.compare-combo, .compare-combo .text-field { -fx-background-color: rgba(255,255,255,0.045); -fx-text-fill: #e7eaf0; -fx-prompt-text-fill: #646d7d; }\n.compare-combo:focused, .compare-combo .text-field:focused { -fx-border-color: rgba(86,216,250,0.70); }\n'''
css.write_text(css_text, encoding='utf-8')

print('Runtime UX/localization patch applied.')
