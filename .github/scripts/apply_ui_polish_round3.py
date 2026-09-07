from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Pattern non trovato: {label}")
    return text.replace(old, new, 1)


# -----------------------------------------------------------------------------
# ThreeDChartPane: zoom non sovrapposto, fullscreen più leggibile, export PNG.
# -----------------------------------------------------------------------------
path = Path("src/main/java/it/casiraghi/swiftbat/ui/components/ThreeDChartPane.java")
text = path.read_text(encoding="utf-8")

text = replace_once(text,
"import javafx.embed.swing.SwingNode;\n",
"import javafx.embed.swing.SwingFXUtils;\nimport javafx.embed.swing.SwingNode;\n",
"ThreeD SwingFXUtils import")
text = replace_once(text,
"import javafx.scene.control.Button;\n",
"import javafx.scene.SnapshotParameters;\nimport javafx.scene.control.Alert;\nimport javafx.scene.control.Button;\n",
"ThreeD Snapshot imports")
text = replace_once(text,
"import javafx.scene.layout.VBox;\n",
"import javafx.scene.image.WritableImage;\nimport javafx.scene.layout.VBox;\nimport javafx.stage.FileChooser;\n",
"ThreeD image imports")
text = replace_once(text,
"import javax.swing.SwingUtilities;\nimport java.awt.Color;\n",
"import javax.imageio.ImageIO;\nimport javax.swing.SwingUtilities;\nimport java.awt.Color;\n",
"ThreeD ImageIO import")
text = replace_once(text,
"import java.awt.Dimension;\n",
"import java.awt.Dimension;\nimport java.io.File;\nimport java.io.IOException;\n",
"ThreeD file imports")

text = replace_once(text,
'''    private final SwingNode swingNode = new SwingNode();
    private final Java2DWaterfallPanel renderer = new Java2DWaterfallPanel();
    private final Label contextLabel = UiFactory.label("GRB", "three-d-context");
    private final boolean allowFullscreen;
''',
'''    private final SwingNode swingNode = new SwingNode();
    private final Java2DWaterfallPanel renderer = new Java2DWaterfallPanel();
    private final Label contextLabel = UiFactory.label("GRB", "three-d-context");
    private final Label zoomLabel = UiFactory.label("Zoom 100%", "three-d-zoom-inline");
    private final boolean allowFullscreen;
    private final StackPane viewer;
''',
"ThreeD fields")

text = replace_once(text,
'''        this.allowFullscreen = allowFullscreen;
        getStyleClass().add("three-d-panel");
        setMinHeight(430);
        setPrefHeight(500);
''',
'''        this.allowFullscreen = allowFullscreen;
        getStyleClass().add("three-d-panel");
        if (!allowFullscreen) {
            getStyleClass().add("three-d-panel-fullscreen");
        }
        setMinHeight(allowFullscreen ? 430 : 0);
        setPrefHeight(allowFullscreen ? 500 : 760);
        setMaxHeight(Double.MAX_VALUE);
''',
"ThreeD constructor sizing")

text = replace_once(text,
'''        Label zoomLabel = UiFactory.label("Zoom 100%", "three-d-zoom-label");
        zoomLabel.setMouseTransparent(true);
        renderer.setZoomListener(value -> Platform.runLater(
                () -> zoomLabel.setText("Zoom " + Math.round(value * 100.0) + "%")));

        StackPane viewer = new StackPane(swingNode, zoomLabel);
        StackPane.setAlignment(zoomLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(zoomLabel, new Insets(0, 0, 12, 12));
        viewer.addEventHandler(ScrollEvent.SCROLL, event -> event.consume());
        viewer.getStyleClass().add("three-d-viewer");
        viewer.setMinHeight(330);
        viewer.setPrefHeight(390);
''',
'''        zoomLabel.setMouseTransparent(true);
        renderer.setZoomListener(value -> Platform.runLater(
                () -> zoomLabel.setText("Zoom " + Math.round(value * 100.0) + "%")));

        viewer = new StackPane(swingNode);
        viewer.addEventFilter(ScrollEvent.SCROLL, event -> event.consume());
        viewer.getStyleClass().add("three-d-viewer");
        viewer.setMinHeight(allowFullscreen ? 330 : 0);
        viewer.setPrefHeight(allowFullscreen ? 390 : 650);
        viewer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
''',
"ThreeD zoom overlay removal")

text = replace_once(text,
'''        VBox header = new VBox(8);
        header.getStyleClass().add("three-d-header");
        header.setPadding(new Insets(10, 14, 9, 14));
''',
'''        VBox header = new VBox(allowFullscreen ? 8 : 12);
        header.getStyleClass().add("three-d-header");
        if (!allowFullscreen) {
            header.getStyleClass().add("three-d-header-fullscreen");
        }
        header.setPadding(allowFullscreen ? new Insets(10, 14, 9, 14) : new Insets(18, 22, 16, 22));
''',
"ThreeD fullscreen header")

text = replace_once(text,
'''        footer.getChildren().addAll(note, spacer, windowLabel, windowChoice, reset);
        if (allowFullscreen) {
            Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");
            fullscreen.setOnAction(event -> openFullscreen());
            footer.getChildren().add(fullscreen);
        }
''',
'''        Button export = UiFactory.button("Esporta PNG", "ghost-button");
        export.setOnAction(event -> exportViewerPng());

        footer.getChildren().addAll(zoomLabel, note, spacer, windowLabel, windowChoice, reset, export);
        if (allowFullscreen) {
            Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");
            fullscreen.setOnAction(event -> openFullscreen());
            footer.getChildren().add(fullscreen);
        }
''',
"ThreeD footer actions")

text = replace_once(text,
'''        enlarged.setData(sourceData);

        InPlaceFullscreen.show(this, "Vista 3D · " + contextName, enlarged);
    }

    private void syncRendererSize(StackPane viewer) {
''',
'''        enlarged.setData(sourceData);
        enlarged.setMinSize(0, 0);
        enlarged.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        InPlaceFullscreen.show(this, "Vista 3D · " + contextName, enlarged);
    }

    private void exportViewerPng() {
        if (getScene() == null || viewer.getWidth() <= 1 || viewer.getHeight() <= 1) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Esporta vista 3D");
        chooser.setInitialFileName(contextName.replaceAll("[^A-Za-z0-9._-]", "_") + "_vista_3D.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagine PNG", "*.png"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            WritableImage image = viewer.snapshot(new SnapshotParameters(), null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException | RuntimeException error) {
            new Alert(Alert.AlertType.ERROR,
                    "Esportazione PNG non riuscita: " + error.getMessage()).showAndWait();
        }
    }

    private void syncRendererSize(StackPane viewer) {
''',
"ThreeD PNG export")

path.write_text(text, encoding="utf-8")


# -----------------------------------------------------------------------------
# ExplorerPage: riempimento verticale e toolbar Dati responsive/leggibile.
# -----------------------------------------------------------------------------
path = Path("src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java")
text = path.read_text(encoding="utf-8")

text = replace_once(text,
'''        tabs.setMinHeight(520);
        tabs.setPrefHeight(610);
''',
'''        tabs.setMinHeight(520);
        tabs.setPrefHeight(610);
        tabs.setMaxHeight(Double.MAX_VALUE);
''',
"Explorer tabs max height")

text = replace_once(text,
'''        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(false);
        return scroll;
''',
'''        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(false);
        scroll.viewportBoundsProperty().addListener((obs, oldBounds, bounds) ->
                dashboard.setMinHeight(Math.max(0, bounds.getHeight())));
        return scroll;
''',
"Explorer fill viewport")

text = replace_once(text,
'''        sourceChoice.getStyleClass().add("choice-box-modern");
        if (!sourceChoice.getItems().isEmpty()) {
            sourceChoice.setValue(sourceChoice.getItems().get(0));
        }

        TextField filter = new TextField();
        filter.setPromptText("Filtra le righe per valore testuale…");
        filter.getStyleClass().add("search-field");
        filter.setPrefWidth(300);

        ChoiceBox<String> fieldChoice = new ChoiceBox<>();
        fieldChoice.getStyleClass().add("choice-box-modern");
        fieldChoice.setPrefWidth(260);
''',
'''        sourceChoice.getStyleClass().add("choice-box-modern");
        sourceChoice.setMinWidth(190);
        sourceChoice.setPrefWidth(215);
        sourceChoice.setMaxWidth(250);
        UiFactory.autoTooltip(sourceChoice);
        if (!sourceChoice.getItems().isEmpty()) {
            sourceChoice.setValue(sourceChoice.getItems().get(0));
        }

        TextField filter = new TextField();
        filter.setPromptText("Filtra le righe per valore testuale…");
        filter.getStyleClass().add("search-field");
        filter.setMinWidth(170);
        filter.setPrefWidth(360);
        filter.setMaxWidth(Double.MAX_VALUE);

        ChoiceBox<String> fieldChoice = new ChoiceBox<>();
        fieldChoice.getStyleClass().add("choice-box-modern");
        fieldChoice.setMinWidth(215);
        fieldChoice.setPrefWidth(285);
        fieldChoice.setMaxWidth(340);
        UiFactory.autoTooltip(fieldChoice);
''',
"Explorer data control widths")

text = replace_once(text,
'''        HBox toolbar = new HBox(10, sourceChoice, filter, exportAscii, exportFits,
                UiFactory.spacer(), UiFactory.label("Spiega:", "toolbar-label"), fieldChoice);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        pane.setTop(toolbar);
        BorderPane.setMargin(toolbar, new Insets(0, 0, 14, 0));
''',
'''        HBox sourceRow = new HBox(10, sourceChoice, filter);
        sourceRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filter, Priority.ALWAYS);

        HBox explainControl = new HBox(8,
                UiFactory.label("Spiega il campo:", "toolbar-label"), fieldChoice);
        explainControl.setAlignment(Pos.CENTER_LEFT);
        FlowPane actionRow = new FlowPane(10, 8);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.getChildren().addAll(exportAscii, exportFits, explainControl);

        VBox toolbar = new VBox(8, sourceRow, actionRow);
        toolbar.getStyleClass().add("data-toolbar");
        pane.setTop(toolbar);
        BorderPane.setMargin(toolbar, new Insets(0, 0, 14, 0));
''',
"Explorer responsive data toolbar")

path.write_text(text, encoding="utf-8")


# -----------------------------------------------------------------------------
# PopulationPage: usa tutta l'altezza disponibile, anche fullscreen.
# -----------------------------------------------------------------------------
path = Path("src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java")
text = path.read_text(encoding="utf-8")

text = replace_once(text,
'''        resultTabs.setMinHeight(450);
        resultTabs.setPrefHeight(500);
        resultTabs.setMaxHeight(560);

        page.getChildren().addAll(title, filterCard, resultTabs);
''',
'''        resultTabs.setMinHeight(450);
        resultTabs.setPrefHeight(500);
        resultTabs.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(resultTabs, Priority.ALWAYS);

        page.getChildren().addAll(title, filterCard, resultTabs);
''',
"Population tabs growth")

text = replace_once(text,
'''        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private VBox buildFilters() {
''',
'''        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.viewportBoundsProperty().addListener((obs, oldBounds, bounds) ->
                page.setMinHeight(Math.max(0, bounds.getHeight())));
        return scroll;
    }

    private VBox buildFilters() {
''',
"Population fill viewport")

text = replace_once(text,
'''        HBox content = new HBox(18, chartColumn, insightSnapshotCard(true));
        content.setAlignment(Pos.TOP_LEFT);
        content.getStyleClass().add("population-fullscreen-content");
        InPlaceFullscreen.show(this, "Profilo temporale della popolazione", content);
''',
'''        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        HBox content = new HBox(18, chartColumn, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setMinSize(0, 0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("population-fullscreen-content");
        InPlaceFullscreen.show(this, "Profilo temporale della popolazione", content);
''',
"Population fullscreen 2D fill")

text = replace_once(text,
'''        HBox content = new HBox(18, pane, insightSnapshotCard(true));
        content.setAlignment(Pos.TOP_LEFT);
        content.getStyleClass().add("population-fullscreen-content");
        InPlaceFullscreen.show(this, "Profilo di popolazione 3D", content);
''',
'''        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        HBox content = new HBox(18, pane, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setMinSize(0, 0);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.getStyleClass().add("population-fullscreen-content");
        InPlaceFullscreen.show(this, "Profilo di popolazione 3D", content);
''',
"Population fullscreen 3D fill")

path.write_text(text, encoding="utf-8")


# -----------------------------------------------------------------------------
# SkyMapPage: esportazione PNG contestuale normale e fullscreen.
# -----------------------------------------------------------------------------
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


# -----------------------------------------------------------------------------
# CSS: angolo scrollbar tabelle, zoom uniforme e fullscreen 3D più leggibile.
# -----------------------------------------------------------------------------
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

print("UI polish round 3 applicato")
