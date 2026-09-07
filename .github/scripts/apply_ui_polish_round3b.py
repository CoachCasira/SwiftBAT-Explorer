from pathlib import Path
import runpy


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Pattern non trovato: {label}")
    return text.replace(old, new, 1)


# La prima patch applica correttamente ThreeDChartPane, ExplorerPage e la parte
# iniziale di PopulationPage; nel sorgente corrente si ferma soltanto sul blocco
# fullscreen già rifinito nel round precedente. Manteniamo le modifiche in working tree.
try:
    runpy.run_path(".github/scripts/apply_ui_polish_round3.py", run_name="__main__")
except RuntimeError as error:
    if "Population fullscreen 2D fill" not in str(error):
        raise


# Completa PopulationPage sui blocchi fullscreen già esistenti.
path = Path("src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java")
text = path.read_text(encoding="utf-8")
old = '''        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.getStyleClass().add("population-fullscreen-content");
'''
new = '''        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.setMinSize(0, 0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("population-fullscreen-content");
'''
if text.count(old) < 2:
    raise RuntimeError("Pattern non trovato: Population fullscreen fill round3b")
text = text.replace(old, new, 2)
path.write_text(text, encoding="utf-8")


# SkyMapPage: esportazione PNG contestuale, anche nella vista fullscreen.
path = Path("src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java")
text = path.read_text(encoding="utf-8")

text = replace_once(text,
"import javafx.collections.FXCollections;\n",
"import javafx.collections.FXCollections;\nimport javafx.embed.swing.SwingFXUtils;\n",
"Sky SwingFXUtils import")
text = replace_once(text,
"import javafx.scene.Node;\n",
"import javafx.scene.Node;\nimport javafx.scene.SnapshotParameters;\n",
"Sky Snapshot import")
text = replace_once(text,
"import javafx.scene.control.Button;\n",
"import javafx.scene.control.Alert;\nimport javafx.scene.control.Button;\n",
"Sky Alert import")
text = replace_once(text,
"import javafx.scene.layout.BorderPane;\n",
"import javafx.scene.image.WritableImage;\nimport javafx.scene.layout.BorderPane;\n",
"Sky WritableImage import")
text = replace_once(text,
"import javafx.util.Duration;\n\nimport java.util.ArrayList;\n",
"import javafx.stage.FileChooser;\nimport javafx.util.Duration;\n\nimport javax.imageio.ImageIO;\nimport java.io.File;\nimport java.io.IOException;\nimport java.util.ArrayList;\n",
"Sky export imports")

text = replace_once(text,
'''        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");
        fullscreen.setOnAction(event -> openMapFullscreen());
        HBox mapHead = new HBox(10,
                UiFactory.label("Cielo", "card-title"),
                resetView,
                fullscreen,
                UiFactory.spacer(),
                viewSwitch);
''',
'''        Button exportPng = UiFactory.button("Esporta PNG", "ghost-button");
        exportPng.setOnAction(event -> exportMapNode(
                sphereView ? sphere : mollweide,
                sphereView ? "mappa_celeste_sfera_3D.png" : "mappa_celeste_mollweide_2D.png"));
        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");
        fullscreen.setOnAction(event -> openMapFullscreen());
        HBox mapHead = new HBox(10,
                UiFactory.label("Cielo", "card-title"),
                resetView,
                exportPng,
                fullscreen,
                UiFactory.spacer(),
                viewSwitch);
''',
"Sky normal export button")

text = replace_once(text,
'''            HBox.setHgrow(enlarged, Priority.ALWAYS);
            layout.getChildren().addAll(enlarged, details.node());
            InPlaceFullscreen.show(this, "Mappa celeste · Sfera 3D", layout);
''',
'''            HBox.setHgrow(enlarged, Priority.ALWAYS);
            details.export().setOnAction(event -> exportMapNode(enlarged, "mappa_celeste_sfera_3D.png"));
            layout.getChildren().addAll(enlarged, details.node());
            InPlaceFullscreen.show(this, "Mappa celeste · Sfera 3D", layout);
''',
"Sky fullscreen sphere export")

text = replace_once(text,
'''            HBox.setHgrow(enlarged, Priority.ALWAYS);
            layout.getChildren().addAll(enlarged, details.node());
            InPlaceFullscreen.show(this, "Mappa celeste · Mollweide 2D", layout);
''',
'''            HBox.setHgrow(enlarged, Priority.ALWAYS);
            details.export().setOnAction(event -> exportMapNode(enlarged, "mappa_celeste_mollweide_2D.png"));
            layout.getChildren().addAll(enlarged, details.node());
            InPlaceFullscreen.show(this, "Mappa celeste · Mollweide 2D", layout);
''',
"Sky fullscreen Mollweide export")

text = replace_once(text,
'''        Button open = UiFactory.button("Apri curve di luce →", "primary-button");
        open.setMaxWidth(Double.MAX_VALUE);
        open.setDisable(true);
''',
'''        Button open = UiFactory.button("Apri curve di luce →", "primary-button");
        open.setMaxWidth(Double.MAX_VALUE);
        open.setDisable(true);
        Button export = UiFactory.button("Esporta PNG", "ghost-button");
        export.setMaxWidth(Double.MAX_VALUE);
''',
"Sky fullscreen export control")

text = replace_once(text,
'''                UiFactory.label("GRB selezionato", "card-subtitle"),
                name, rows, catalogInfo, open,
                UiFactory.label("Come leggere la selezione", "card-title"), note);
''',
'''                UiFactory.label("GRB selezionato", "card-subtitle"),
                name, rows, catalogInfo, open, export,
                UiFactory.label("Come leggere la selezione", "card-title"), note);
''',
"Sky fullscreen panel export")

text = replace_once(text,
'''        updater.accept(selectedBurst);
        return new FullscreenDetails(panel, updater);
    }

    private VBox buildDetailsPanel() {
''',
'''        updater.accept(selectedBurst);
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
''',
"Sky export method")

text = replace_once(text,
'''    private record FullscreenDetails(VBox node, Consumer<SkyBurst> update) {
    }
''',
'''    private record FullscreenDetails(VBox node, Consumer<SkyBurst> update, Button export) {
    }
''',
"Sky fullscreen record")

path.write_text(text, encoding="utf-8")


# CSS: elimina il quadratino bianco delle tabelle, uniforma lo zoom e migliora il fullscreen.
path = Path("src/main/resources/app.css")
css = path.read_text(encoding="utf-8")
append = r'''

/* ---------- Rifiniture tabelle / zoom / fullscreen round 3 ---------- */
.table-view .corner,
.data-table .corner {
    -fx-background-color: #090d15;
}

.scroll-pane .corner {
    -fx-background-color: transparent;
}

.sky-zoom-label,
.three-d-zoom-inline {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-text-fill: #7187aa;
    -fx-padding: 0;
    -fx-font-size: 10px;
    -fx-font-weight: normal;
}

.three-d-panel-fullscreen .overlay-title {
    -fx-font-size: 22px;
}

.three-d-panel-fullscreen .overlay-caption {
    -fx-font-size: 13px;
    -fx-line-spacing: 3px;
}

.three-d-panel-fullscreen .legend-item {
    -fx-font-size: 12px;
}

.three-d-panel-fullscreen .three-d-context {
    -fx-font-size: 13px;
    -fx-padding: 6 10;
}

.three-d-panel-fullscreen .three-d-zoom-inline {
    -fx-font-size: 12px;
}

.data-toolbar {
    -fx-background-color: rgba(255, 255, 255, 0.018);
    -fx-border-color: rgba(255, 255, 255, 0.055);
    -fx-background-radius: 11px;
    -fx-border-radius: 11px;
    -fx-padding: 10px;
}
'''
if "Rifiniture tabelle / zoom / fullscreen round 3" not in css:
    css += append
path.write_text(css, encoding="utf-8")

print("UI polish round 3b applicato")
