from pathlib import Path

POP = Path('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java')
SKY = Path('src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java')
ABOUT = Path('src/main/java/it/casiraghi/swiftbat/ui/AboutPage.java')
THREE = Path('src/main/java/it/casiraghi/swiftbat/ui/components/ThreeDChartPane.java')
WATER = Path('src/main/java/it/casiraghi/swiftbat/ui/components/Java2DWaterfallPanel.java')
SPHERE = Path('src/main/java/it/casiraghi/swiftbat/ui/components/CelestialSpherePane.java')
CSS = Path('src/main/resources/app.css')


def rep(path, old, new, label):
    text = path.read_text(encoding='utf-8')
    if old in text:
        path.write_text(text.replace(old, new, 1), encoding='utf-8')
        print(f'{label}: applicato')
        return
    if new in text:
        print(f'{label}: già applicato')
        return
    raise SystemExit(f'Patch non applicabile: {label}')

# ---------------------------------------------------------------------------
# 1. Corregge il warning CSS JavaFX sulle barre: -fx-bar-fill è un looked-up
#    value non tipizzato e Modena prova a convertirlo in Color.
# ---------------------------------------------------------------------------
rep(CSS,
''' .bar-chart .chart-bar {
    -fx-bar-fill: #67cee9;
    -fx-background-color: linear-gradient(to top, #7b56c9, #4fd5ef);
    -fx-background-radius: 5px 5px 0 0;
}'''.lstrip(),
''' .bar-chart .chart-bar {
    -fx-background-color: linear-gradient(to top, #7b56c9, #4fd5ef);
    -fx-background-radius: 5px 5px 0 0;
}'''.lstrip(),
'warning CSS chart-bar')

# ---------------------------------------------------------------------------
# 2. Assistente di lettura: ellissi + tooltip nella vista compatta, testo
#    completo e più grande nelle viste fullscreen.
# ---------------------------------------------------------------------------
rep(POP,
'''        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "secondary-button");''',
'''        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");''',
'fullscreen popolazione primario')

rep(POP,
'''        insightHeadline.setMinWidth(0);
        insightHeadline.setMaxWidth(Double.MAX_VALUE);
        insightHeadline.setTextOverrun(OverrunStyle.CLIP);''',
'''        insightHeadline.setMinWidth(0);
        insightHeadline.setMaxWidth(Double.MAX_VALUE);
        insightHeadline.setWrapText(false);
        insightHeadline.setTextOverrun(OverrunStyle.ELLIPSIS);
        UiFactory.autoTooltip(insightHeadline);''',
'headline compatto con ellissi')

rep(POP,
'''        HBox content = new HBox(18, chartColumn, insightSnapshotCard(true));
        content.setAlignment(Pos.TOP_LEFT);
        content.getStyleClass().add("population-fullscreen-content");''',
'''        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        HBox content = new HBox(18, chartColumn, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.getStyleClass().add("population-fullscreen-content");''',
'fullscreen profilo usa altezza assistente')

rep(POP,
'''        card.getStyleClass().add("population-insight-card");
        card.setFillWidth(true);
        card.setMinWidth(expanded ? 350 : 270);
        card.setPrefWidth(expanded ? 430 : 330);
        card.setMaxWidth(expanded ? 520 : 370);
        return card;''',
'''        card.getStyleClass().add("population-insight-card");
        if (expanded) card.getStyleClass().add("population-insight-expanded");
        card.setFillWidth(true);
        card.setMinWidth(expanded ? 390 : 270);
        card.setPrefWidth(expanded ? 480 : 330);
        card.setMaxWidth(expanded ? 560 : 370);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;''',
'assistente fullscreen più ampio')

rep(POP,
'''        HBox content = new HBox(18, pane, insightSnapshotCard(true));
        content.setAlignment(Pos.TOP_LEFT);
        content.getStyleClass().add("population-fullscreen-content");''',
'''        VBox assistant = insightSnapshotCard(true);
        assistant.setMaxHeight(Double.MAX_VALUE);
        HBox content = new HBox(18, pane, assistant);
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillHeight(true);
        content.getStyleClass().add("population-fullscreen-content");''',
'fullscreen 3D usa altezza assistente')

rep(POP,
'''    private HBox insightLine(String marker, String text, String markerStyle, boolean expanded) {
        Label bullet = UiFactory.label(marker, markerStyle);
        Label copy = UiFactory.wrappedLabel(text, "population-insight-text");
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        copy.setTextOverrun(OverrunStyle.CLIP);
        if (expanded) copy.setStyle("-fx-font-size: 12px; -fx-line-spacing: 2.5px;");
        HBox.setHgrow(copy, Priority.ALWAYS);
        HBox row = new HBox(8, bullet, copy);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }''',
'''    private HBox insightLine(String marker, String text, String markerStyle, boolean expanded) {
        Label bullet = UiFactory.label(marker, markerStyle);
        bullet.setMinWidth(14);
        bullet.setAlignment(Pos.CENTER);
        Label copy = expanded
                ? UiFactory.wrappedLabel(text, "population-insight-text")
                : UiFactory.label(text, "population-insight-text");
        copy.setWrapText(expanded);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        copy.setTextOverrun(expanded ? OverrunStyle.CLIP : OverrunStyle.ELLIPSIS);
        if (expanded) copy.setStyle("-fx-font-size: 13px; -fx-line-spacing: 3px;");
        HBox.setHgrow(copy, Priority.ALWAYS);
        HBox row = new HBox(8, bullet, copy);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(expanded ? Pos.TOP_LEFT : Pos.CENTER_LEFT);
        return row;
    }''',
'righe assistente ellissi e allineamento')

# ---------------------------------------------------------------------------
# 3. Fullscreen mappa: pannello dettagli più grande e informativo.
# ---------------------------------------------------------------------------
rep(SKY,
'''        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "secondary-button");''',
'''        Button fullscreen = UiFactory.button("Schermo intero  ⛶", "primary-button");''',
'fullscreen mappa primario')

rep(SKY,
'''        VBox rows = new VBox(10,
                detailRow("Trigger", trigger),
                detailRow("RA (J2000)", ra),
                detailRow("DEC (J2000)", dec),
                detailRow("T90", t90),
                detailRow("Classe descrittiva", clazz),
                detailRow("Redshift", redshift));
        Label note = UiFactory.wrappedLabel(
                "Seleziona un GRB direttamente nella vista a schermo intero: i dettagli restano visibili qui e puoi aprire subito le relative curve di luce.",
                "sky-science-note");
        VBox panel = new VBox(14,
                UiFactory.label("GRB selezionato", "card-subtitle"),
                name, rows, catalogInfo, open,
                UiFactory.label("Vista interattiva", "card-title"), note);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(18));
        panel.setMinWidth(300);
        panel.setPrefWidth(330);
        panel.setMaxWidth(360);
        panel.setMinHeight(0);''',
'''        VBox rows = new VBox(14,
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
                name, rows, catalogInfo, open,
                UiFactory.label("Come leggere la selezione", "card-title"), note);
        panel.getStyleClass().addAll("card", "sky-fullscreen-details");
        panel.setPadding(new Insets(22));
        panel.setMinWidth(360);
        panel.setPrefWidth(410);
        panel.setMaxWidth(450);
        panel.setMinHeight(0);
        panel.setMaxHeight(Double.MAX_VALUE);''',
'pannello fullscreen mappa espanso')

# ---------------------------------------------------------------------------
# 4. Vista 3D Esplora: indicatore zoom e blocco dello scroll pagina mentre
#    la rotellina sta interagendo con il grafico.
# ---------------------------------------------------------------------------
rep(WATER,
'''import java.util.Locale;
''',
'''import java.util.Locale;
import java.util.function.DoubleConsumer;
''',
'import DoubleConsumer renderer')

rep(WATER,
'''    private HoverPoint hover;
''',
'''    private HoverPoint hover;
    private DoubleConsumer zoomListener = value -> { };
''',
'listener zoom renderer')

rep(WATER,
'''            public void mouseWheelMoved(MouseWheelEvent event) {
                zoom = clamp(zoom * Math.pow(1.08, -event.getPreciseWheelRotation()), 0.68, 1.55);
                hover = null;
                repaint();
            }''',
'''            public void mouseWheelMoved(MouseWheelEvent event) {
                zoom = clamp(zoom * Math.pow(1.08, -event.getPreciseWheelRotation()), 0.68, 1.55);
                hover = null;
                zoomListener.accept(zoom);
                repaint();
            }''',
'notifica zoom rotella renderer')

rep(WATER,
'''    public void resetView() {
        yaw = 0.32;
        pitch = 0.72;
        zoom = 1.0;
        hover = null;
        repaint();
    }
''',
'''    public void setZoomListener(DoubleConsumer listener) {
        zoomListener = listener == null ? value -> { } : listener;
        zoomListener.accept(zoom);
    }

    public void resetView() {
        yaw = 0.32;
        pitch = 0.72;
        zoom = 1.0;
        hover = null;
        zoomListener.accept(zoom);
        repaint();
    }
''',
'API zoom renderer')

rep(THREE,
'''import javafx.scene.control.Label;
''',
'''import javafx.scene.control.Label;
import javafx.scene.input.ScrollEvent;
''',
'import ScrollEvent 3D')

rep(THREE,
'''        SwingUtilities.invokeLater(() -> swingNode.setContent(renderer));

        StackPane viewer = new StackPane(swingNode);
        viewer.getStyleClass().add("three-d-viewer");''',
'''        SwingUtilities.invokeLater(() -> swingNode.setContent(renderer));

        Label zoomLabel = UiFactory.label("Zoom 100%", "three-d-zoom-label");
        zoomLabel.setMouseTransparent(true);
        renderer.setZoomListener(value -> Platform.runLater(
                () -> zoomLabel.setText("Zoom " + Math.round(value * 100.0) + "%")));

        StackPane viewer = new StackPane(swingNode, zoomLabel);
        StackPane.setAlignment(zoomLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(zoomLabel, new Insets(0, 0, 12, 12));
        viewer.addEventHandler(ScrollEvent.SCROLL, event -> event.consume());
        viewer.getStyleClass().add("three-d-viewer");''',
'indicatore zoom e consume scroll 3D')

# ---------------------------------------------------------------------------
# 5. Sfera celeste 3D: indicatore zoom anche in fullscreen.
# ---------------------------------------------------------------------------
rep(SPHERE,
'''import javafx.scene.control.Tooltip;
''',
'''import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
''',
'import Label sfera')

rep(SPHERE,
'''    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene subScene;
''',
'''    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene subScene;
    private final Label zoomLabel = new Label("Zoom 100%");
''',
'label zoom sfera')

rep(SPHERE,
'''        getChildren().add(subScene);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        installInteraction();''',
'''        zoomLabel.getStyleClass().add("sky-zoom-label");
        zoomLabel.setMouseTransparent(true);
        getChildren().addAll(subScene, zoomLabel);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        installInteraction();''',
'aggiunge zoom label sfera')

rep(SPHERE,
'''    public void resetView() {
        rotateX.setAngle(-14.0);
        rotateY.setAngle(-24.0);
        camera.setTranslateZ(DEFAULT_CAMERA_Z);
    }
''',
'''    public void resetView() {
        rotateX.setAngle(-14.0);
        rotateY.setAngle(-24.0);
        camera.setTranslateZ(DEFAULT_CAMERA_Z);
        updateZoomLabel();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        zoomLabel.autosize();
        double y = Math.max(12.0, getHeight() - zoomLabel.prefHeight(-1) - 14.0);
        zoomLabel.relocate(14.0, y);
    }

    private void updateZoomLabel() {
        double factor = Math.abs(DEFAULT_CAMERA_Z / camera.getTranslateZ());
        zoomLabel.setText("Zoom " + Math.round(factor * 100.0) + "%");
    }
''',
'aggiorna e posiziona zoom sfera')

rep(SPHERE,
'''        subScene.setOnScroll(event -> {
            // Delta positivo = avvicinamento, negativo = allontanamento.
            double next = camera.getTranslateZ() + event.getDeltaY() * 0.85;
            camera.setTranslateZ(clamp(next, -1550.0, -470.0));
            event.consume();
        });''',
'''        subScene.setOnScroll(event -> {
            // Delta positivo = avvicinamento, negativo = allontanamento.
            double next = camera.getTranslateZ() + event.getDeltaY() * 0.85;
            camera.setTranslateZ(clamp(next, -1550.0, -470.0));
            updateZoomLabel();
            event.consume();
        });''',
'aggiorna zoom sfera su rotella')

# ---------------------------------------------------------------------------
# 6. Pagina Info: card statiche (niente hover da elemento cliccabile) e layout
#    2x2 più largo, con due sezioni informative a tutta pagina.
# ---------------------------------------------------------------------------
rep(ABOUT,
'''import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
''',
'''import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
''',
'import layout Info')

rep(ABOUT,
'''        FlowPane cards = new FlowPane(14, 14);
        cards.getChildren().addAll(
                infoCard("Dati", "Catalogo ufficiale NASA/GSFC Swift/BAT e prodotti DAT/FITS a binning di 1 secondo."),
                infoCard("Curve", "Visualizzazione delle curve di luce totali e nelle quattro bande energetiche disponibili."),
                infoCard("Volta celeste", "Mollweide 2D e sfera 3D costruite con le coordinate RA/DEC pubblicate da Swift/BAT."),
                infoCard("Sessione", "Gli eventi aperti restano in memoria finché l'app è in esecuzione, senza modificare i file sorgente."));

        VBox scope = UiFactory.card("Scopo scientifico", null,
                UiFactory.wrappedLabel(
                        "L'app facilita consultazione, controllo e confronto descrittivo dei dati. Gli indicatori e la soglia T90 = 2 s mostrati nell'interfaccia non sostituiscono una classificazione astrofisica validata.",
                        "explanation-text"));''',
'''        GridPane cards = new GridPane();
        cards.setHgap(14);
        cards.setVgap(14);
        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(50);
        firstColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(50);
        secondColumn.setHgrow(Priority.ALWAYS);
        cards.getColumnConstraints().addAll(firstColumn, secondColumn);
        cards.add(infoCard("Dati", "Catalogo ufficiale NASA/GSFC Swift/BAT e prodotti DAT/FITS a binning di 1 secondo. I valori restano riconducibili alle sorgenti pubbliche usate dall'app."), 0, 0);
        cards.add(infoCard("Curve", "Visualizzazione delle curve di luce totali e nelle quattro bande energetiche, con finestre temporali, zoom e viste dedicate per leggere meglio la struttura del burst."), 1, 0);
        cards.add(infoCard("Volta celeste", "Mollweide 2D e sfera 3D costruite con le coordinate RA/DEC pubblicate da Swift/BAT. Le due viste mostrano lo stesso campione con rappresentazioni differenti."), 0, 1);
        cards.add(infoCard("Sessione", "Gli eventi aperti restano in memoria finché l'app è in esecuzione. La cache locale evita download ripetuti senza modificare i prodotti scientifici sorgente."), 1, 1);

        VBox scope = UiFactory.card("Scopo scientifico", null,
                UiFactory.wrappedLabel(
                        "L'app facilita consultazione, controllo e confronto descrittivo dei dati. Gli indicatori e la soglia T90 = 2 s mostrati nell'interfaccia non sostituiscono una classificazione astrofisica validata.",
                        "explanation-text"));
        VBox reading = UiFactory.card("Lettura dei risultati", null,
                UiFactory.wrappedLabel(
                        "Grafici, mappe, filtri e assistenti di lettura servono a mettere in evidenza pattern e differenze nel campione. Le viste 2D e 3D sono strumenti esplorativi e mantengono sempre separata la rappresentazione grafica dall'interpretazione fisica.",
                        "explanation-text"));
        scope.setMaxWidth(Double.MAX_VALUE);
        reading.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(scope, Priority.ALWAYS);
        HBox.setHgrow(reading, Priority.ALWAYS);
        HBox scienceRow = new HBox(14, scope, reading);''',
'layout card Info 2x2')

rep(ABOUT,
'''        page.getChildren().addAll(hero, cards, scope, actions);''',
'''        page.getChildren().addAll(hero, cards, scienceRow, actions);''',
'usa scienceRow Info')

rep(ABOUT,
'''        card.getStyleClass().add("home-action-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(310);''',
'''        card.getStyleClass().add("info-static-card");
        card.setPadding(new Insets(21));
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);''',
'card Info statiche')

# ---------------------------------------------------------------------------
# 7. Stili finali.
# ---------------------------------------------------------------------------
css = CSS.read_text(encoding='utf-8')
addon = r'''

/* ---------- Rifiniture leggibilità / fullscreen ---------- */
.info-static-card {
    -fx-background-color: linear-gradient(to bottom right, rgba(14, 17, 25, 0.96), rgba(9, 12, 19, 0.98));
    -fx-border-color: rgba(255, 255, 255, 0.075);
    -fx-background-radius: 18px;
    -fx-border-radius: 18px;
    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.24), 17, 0.10, 0, 6);
}

.population-insight-expanded {
    -fx-padding: 22px;
}

.population-insight-expanded .population-insight-title {
    -fx-font-size: 20px;
}

.population-insight-expanded .population-insight-badge {
    -fx-font-size: 10px;
}

.population-insight-expanded .population-insight-headline {
    -fx-font-size: 15px;
    -fx-line-spacing: 3px;
}

.population-insight-expanded .population-insight-text {
    -fx-font-size: 13px;
    -fx-line-spacing: 3px;
}

.population-insight-expanded .population-insight-dot,
.population-insight-expanded .population-insight-warning {
    -fx-font-size: 14px;
}

.population-insight-expanded .population-insight-caution-title {
    -fx-font-size: 13px;
}

.population-insight-expanded .population-insight-footnote {
    -fx-font-size: 10px;
    -fx-line-spacing: 2px;
}

.sky-fullscreen-details .sky-selected-title {
    -fx-font-size: 24px;
}

.sky-fullscreen-details .card-subtitle {
    -fx-font-size: 11px;
}

.sky-fullscreen-details .info-key {
    -fx-font-size: 11px;
}

.sky-fullscreen-details .info-value {
    -fx-font-size: 13px;
    -fx-line-spacing: 2px;
}

.sky-fullscreen-details .sky-detail-note,
.sky-fullscreen-details .sky-fullscreen-note {
    -fx-font-size: 12px;
    -fx-line-spacing: 3px;
}

.three-d-zoom-label,
.sky-zoom-label {
    -fx-background-color: rgba(8, 15, 29, 0.82);
    -fx-text-fill: #8fa8ca;
    -fx-background-radius: 8px;
    -fx-border-color: rgba(116, 151, 215, 0.18);
    -fx-border-radius: 8px;
    -fx-padding: 5 8;
    -fx-font-size: 10px;
    -fx-font-weight: bold;
}
'''
if '/* ---------- Rifiniture leggibilità / fullscreen ---------- */' not in css:
    CSS.write_text(css.rstrip() + addon + '\n', encoding='utf-8')
    print('stili rifiniture: applicati')
else:
    print('stili rifiniture: già applicati')

print('Patch visual polish round 2 completata.')
