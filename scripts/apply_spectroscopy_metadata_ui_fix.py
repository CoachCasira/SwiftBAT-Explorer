from pathlib import Path

spectro = Path('src/main/java/it/casiraghi/swiftbat/ui/SpectroscopyPane.java')
text = spectro.read_text(encoding='utf-8')

old = '''        NumberAxis xAxis = new NumberAxis(15, 150, 15);\n        NumberAxis yAxis = new NumberAxis();\n        xAxis.setLabel("Energia (keV)");\n        yAxis.setLabel("log₁₀ N(E) [ph cm⁻² s⁻¹ keV⁻¹]");'''
new = '''        NumberAxis xAxis = new NumberAxis(15, 150, 15);\n        double[] yBounds = spectralYAxisBounds(fit);\n        NumberAxis yAxis = new NumberAxis(yBounds[0], yBounds[1], yBounds[2]);\n        yAxis.setForceZeroInRange(false);\n        xAxis.setLabel("Energia (keV)");\n        yAxis.setLabel("log₁₀ N(E) [ph cm⁻² s⁻¹ keV⁻¹]");'''
assert old in text, 'spectroscopy axis block not found'
text = text.replace(old, new, 1)

old = '''        chart.setMinHeight(showActions ? 300 : 520);\n        chart.setPrefHeight(showActions ? 340 : 680);'''
new = '''        chart.setMinHeight(showActions ? 410 : 560);\n        chart.setPrefHeight(showActions ? 470 : 720);'''
assert old in text, 'spectroscopy chart height block not found'
text = text.replace(old, new, 1)

marker = '''    private double photonModel(Fit fit, double energyKeV) {'''
helper = '''    private double[] spectralYAxisBounds(Fit fit) {\n        double min = Double.POSITIVE_INFINITY;\n        double max = Double.NEGATIVE_INFINITY;\n        if (fit != null && fit.normalization() != null && fit.alpha() != null) {\n            for (double energy = 15; energy <= 150.001; energy += 2.5) {\n                double photons = photonModel(fit, energy);\n                if (Double.isFinite(photons) && photons > 0) {\n                    double value = Math.log10(photons);\n                    min = Math.min(min, value);\n                    max = Math.max(max, value);\n                }\n            }\n        }\n        if (!Double.isFinite(min) || !Double.isFinite(max)) {\n            return new double[]{-3.5, 0.5, 0.5};\n        }\n        double lower = Math.floor((min - 0.25) * 2.0) / 2.0;\n        double upper = Math.max(0.5, Math.ceil((max + 0.15) * 2.0) / 2.0);\n        if (upper - lower < 1.5) {\n            lower -= 0.5;\n            upper += 0.5;\n        }\n        double tick = upper - lower > 5.0 ? 1.0 : 0.5;\n        return new double[]{lower, upper, tick};\n    }\n\n'''
assert marker in text, 'photonModel marker not found'
text = text.replace(marker, helper + marker, 1)
spectro.write_text(text, encoding='utf-8')

explorer = Path('src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java')
text = explorer.read_text(encoding='utf-8')

old = '''        VBox side = new VBox(12);\n        side.setPrefWidth(380);\n        side.setMinWidth(320);\n        side.getStyleClass().add("field-explanation-panel");\n        side.setPadding(new Insets(18));'''
new = '''        VBox side = new VBox(12);\n        side.setPrefWidth(380);\n        side.setMinWidth(320);\n        side.setPadding(new Insets(16));\n        side.setStyle(\n                "-fx-background-color: transparent;"\n                        + "-fx-border-color: transparent;"\n                        + "-fx-background-radius: 0;"\n                        + "-fx-border-radius: 0;"\n        );'''
assert old in text, 'metadata side panel block not found'
text = text.replace(old, new, 1)

old = '''                miniExplanation("HDU", "Un FITS può contenere più sezioni. PRIMARY è l'intestazione generale; RATE è la tabella della curva di luce."),\n                miniExplanation("Keyword", "È il nome breve del parametro, per esempio OBS_ID, TRIGTIME o TIMEDEL."),\n                miniExplanation("Commento originale", "È la descrizione scritta dal software che ha prodotto il FITS."));'''
new = '''                miniExplanation("HDU", "Un FITS può contenere più sezioni. PRIMARY è l'intestazione generale; RATE è la tabella della curva di luce."),\n                miniExplanation("Keyword", "È il nome breve del parametro, per esempio OBS_ID, TRIGTIME o TIMEDEL."),\n                miniExplanation("Valore", "È il contenuto associato alla keyword nella riga selezionata: può essere un numero, una data, un identificativo, una stringa o un valore logico."),\n                miniExplanation("Commento originale", "È la descrizione scritta dal software che ha prodotto il FITS."));'''
assert old in text, 'metadata intro block not found'
text = text.replace(old, new, 1)

explorer.write_text(text, encoding='utf-8')
