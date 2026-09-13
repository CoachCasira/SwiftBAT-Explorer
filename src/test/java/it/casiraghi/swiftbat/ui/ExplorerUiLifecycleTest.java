package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.TabularData;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Point2D;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in real JavaFX tests: -Dswiftbat.uiTests=true with a display or Monocle. */
class ExplorerUiLifecycleTest {
    private static final ConcurrentLinkedQueue<Throwable> FX_ERRORS = new ConcurrentLinkedQueue<>();
    private static I18n.Language previousLanguage;
    private static boolean started;

    @BeforeAll
    static void startToolkit() throws Exception {
        assumeTrue(Boolean.getBoolean("swiftbat.uiTests"), "Requires an explicitly enabled JavaFX test environment");
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> FX_ERRORS.add(error));
            LegacyI18nBridge.install();
            previousLanguage = I18n.language();
            started = true;
            ready.complete(null);
        });
        ready.get(10, TimeUnit.SECONDS);
    }

    @AfterEach
    void noQueuedUiExceptions() throws Exception {
        pulse();
        assertTrue(FX_ERRORS.isEmpty(), () -> "JavaFX failures: " + FX_ERRORS);
    }

    @AfterAll
    static void stopToolkit() throws Exception {
        if (!started) return;
        fx(() -> { I18n.setLanguage(previousLanguage); return null; });
        Platform.exit();
    }

    @Test
    void lateLocalizationCannotRestoreLoadingCaption() throws Exception {
        ExplorerPage page = fx(() -> {
            I18n.setLanguage(I18n.Language.EN);
            ExplorerPage result = new ExplorerPage(null, (entry, refresh) -> { }, entry -> false);
            result.setCatalog(entries(), false); // Load finishes while Home is still visible.
            StackPane host = new StackPane(result);
            new Scene(host, 1200, 800);
            host.applyCss(); // Materialize the SplitPane skin and its sidebar children.
            host.layout();
            UiLocalizationWatcher.install(host);
            UiRefinements.install(host); // The formerly destructive late pass.
            return result;
        });
        pulse();
        fx(() -> {
            assertEquals("2 GRBs in the online catalog", caption(page).getText());
            page.setScientificMetadata(List.of());
            assertEquals("2 GRBs shown", caption(page).getText());
            I18n.setLanguage(I18n.Language.IT);
            return null;
        });
        pulse();
        fx(() -> { assertEquals("2 GRB visualizzati", caption(page).getText());
            I18n.setLanguage(I18n.Language.EN); return null; });
        pulse();
        fx(() -> {
            assertEquals("2 GRBs shown", caption(page).getText());
            Field field = ExplorerPage.class.getDeclaredField("catalogSearch");
            field.setAccessible(true);
            ((TextField) field.get(page)).setText("GRB000000A");
            return null;
        });
        pulse();
        fx(() -> { assertEquals("0 GRBs shown", caption(page).getText()); return null; });
    }

    @Test
    void emptyAndFallbackCountsSurviveLateTranslation() throws Exception {
        fx(() -> {
            I18n.setLanguage(I18n.Language.EN);
            ExplorerPage page = new ExplorerPage(null, (entry, refresh) -> { }, entry -> false);
            page.setCatalog(List.of(), false);
            StackPane host = new StackPane(page);
            new Scene(host, 1200, 800);
            host.applyCss();
            host.layout();
            UiRefinements.install(host);
            assertEquals("0 GRBs in the online catalog", caption(page).getText());
            page.setCatalog(entries(), true);
            UiRefinements.install(host);
            assertEquals("2 fallback GRBs", caption(page).getText());
            return null;
        });
    }

    @Test
    void translationRepairNeverWritesBoundText() throws Exception {
        fx(() -> {
            I18n.setLanguage(I18n.Language.EN);
            Label label = UiFactory.label("Catalogo in caricamento…");
            label.textProperty().bind(new SimpleStringProperty("2 GRBs shown"));
            UiRefinements.install(new StackPane(label));
            assertEquals("2 GRBs shown", label.getText());
            return null;
        });
    }

    @Test
    void nativeScrollbarHasLargeHandleAndFullRangeDrag() throws Exception {
        fx(() -> {
            ListView<String> list = new ListView<>();
            list.getStyleClass().add("catalog-list");
            list.getItems().setAll(IntStream.range(0, 2500).mapToObj(i -> "GRB" + i).toList());
            StackPane root = new StackPane(list);
            root.getStyleClass().add("reference-redesign");
            Scene scene = new Scene(root, 320, 400);
            for (String sheet : List.of("app.css", "ui-refinements.css", "black-hole-theme.css",
                    "reference-redesign.css", "stability-final.css")) {
                scene.getStylesheets().add(getClass().getResource("/" + sheet).toExternalForm());
            }
            root.applyCss();
            root.layout();
            ScrollBar bar = list.lookupAll(".scroll-bar").stream().filter(ScrollBar.class::isInstance)
                    .map(ScrollBar.class::cast).filter(b -> b.getOrientation() == Orientation.VERTICAL)
                    .findFirst().orElseThrow();
            Region thumb = (Region) bar.lookup(".thumb");
            Region track = (Region) bar.lookup(".track");
            assertEquals("javafx.scene.control.skin.ScrollBarSkin", bar.getSkin().getClass().getName());
            assertEquals(9.0, bar.getWidth(), 0.5);
            assertTrue(thumb.getHeight() >= 71.5, "Handle must be at least 72px, not a dot");
            double handleHeight = thumb.getHeight();
            double travel = track.getHeight() - handleHeight;
            thumb.fireEvent(mouse(thumb, MouseEvent.MOUSE_PRESSED, 8, 8, true));
            thumb.fireEvent(mouse(thumb, MouseEvent.MOUSE_DRAGGED, 8, 8 + travel, true));
            thumb.fireEvent(mouse(thumb, MouseEvent.MOUSE_RELEASED, 8, 8, false));
            assertEquals(bar.getMax(), bar.getValue(), 0.002, "One drag must reach the last GRB");
            for (int i = 0; i < 40; i++) root.layout();
            assertEquals(handleHeight, thumb.getHeight(), 0.5, "Layout must not fight native thumb sizing");
            bar.setValue(bar.getMin());
            root.layout();
            assertEquals(bar.getMin(), bar.getValue());
            return null;
        });
    }

    @Test
    void compareSelectionControlsTooltipsAndSurvivesFullscreenAndNormalization() throws Exception {
        ComparePage page = fx(() -> {
            I18n.setLanguage(I18n.Language.EN);
            var data = FXCollections.<String, GrbData>observableHashMap();
            data.put("GRB250605A", compareData("GRB250605A", 3));
            data.put("GRB250603A", compareData("GRB250603A", 1));
            return new ComparePage(data, entry -> { });
        });
        Stage stage = fx(() -> {
            Stage result = showStyled(page, 1240, 860);
            UiLocalizationWatcher.install(page);
            UiRefinements.install(page);
            InteractiveViewSyncEnhancer.install(page);
            InteractionPolishEnhancer.install(page);
            CurveInteractionLinkEnhancer.install(page);
            ChartInteractionEnhancer.install(page);
            @SuppressWarnings("unchecked") ComboBox<String> first = (ComboBox<String>) field(page, "first");
            @SuppressWarnings("unchecked") ComboBox<String> second = (ComboBox<String>) field(page, "second");
            first.setValue("GRB250605A");
            second.setValue("GRB250603A");
            return result;
        });
        try {
            pulse();
            fx(() -> {
                LineChart<Number, Number> chart = compareChart(page);
                pointEvent(chart, 0, MouseEvent.MOUSE_MOVED);
                assertTrue(shownTooltip().getText().contains("GRB250605A"));
                assertTrue(shownTooltip().getText().contains("Original rate error"));
                pointEvent(chart, 0, MouseEvent.MOUSE_CLICKED);
                pointEvent(chart, 1, MouseEvent.MOUSE_MOVED);
                assertNoTooltip();
                assertEquals(1.0, chart.getData().get(1).getNode().getOpacity()); // Hover still lights B.
                pointEvent(chart, 1, MouseEvent.MOUSE_CLICKED);
                assertTrue(shownTooltip().getText().startsWith("GRB250603A"));
                pointEvent(chart, 0, MouseEvent.MOUSE_MOVED);
                assertNoTooltip();
                pointEvent(chart, 0, MouseEvent.MOUSE_CLICKED, 2);
                pointEvent(chart, 0, MouseEvent.MOUSE_MOVED);
                assertTrue(shownTooltip().getText().startsWith("GRB250605A"));
                pointEvent(chart, 1, MouseEvent.MOUSE_CLICKED); // Re-select B before opening.
                Node card = page.lookup(".compare-chart-card");
                assertEquals(javafx.scene.Cursor.HAND, card.getCursor());
                card.fireEvent(mouse(card, MouseEvent.MOUSE_CLICKED, 10, 10, false));
                return null;
            });
            pulse();
            fx(() -> {
                assertNotSame(page, stage.getScene().getRoot());
                LineChart<Number, Number> enlarged = compareChart(stage.getScene().getRoot());
                assertEquals(1, stage.getScene().getRoot().lookupAll(".compare-chart").size());
                assertTrue(stage.getScene().getRoot().lookupAll(".compare-control-bar").isEmpty());
                pointEvent(enlarged, 0, MouseEvent.MOUSE_MOVED);
                assertNoTooltip(); // B selection copied into fullscreen.
                pointEvent(enlarged, 0, MouseEvent.MOUSE_CLICKED, 2);
                pointEvent(enlarged, 0, MouseEvent.MOUSE_MOVED);
                assertTrue(shownTooltip().getText().startsWith("GRB250605A"));
                pointEvent(enlarged, 0, MouseEvent.MOUSE_CLICKED);
                assertTrue(shownTooltip().getText().startsWith("GRB250605A"));
                back(stage.getScene().getRoot()).fire();
                return null;
            });
            pulse();
            awaitCompareChart(page);
            fx(() -> {
                assertSame(page, stage.getScene().getRoot());
                assertNoTooltip();
                LineChart<Number, Number> chart = compareChart(page);
                pointEvent(chart, 1, MouseEvent.MOUSE_MOVED);
                assertNoTooltip(); // A selection returned from fullscreen.
                ((CheckBox) field(page, "normalize")).setSelected(true);
                return null;
            });
            pulse();
            fx(() -> {
                LineChart<Number, Number> chart = compareChart(page);
                assertEquals(1.0, chart.getData().get(0).getData().get(1).getYValue().doubleValue());
                pointEvent(chart, 0, MouseEvent.MOUSE_MOVED);
                assertTrue(shownTooltip().getText().contains("Original rate:"));
                pointEvent(chart, 0, MouseEvent.MOUSE_CLICKED); // Release A.
                // Separate the samples after testing normalized values (both peaks are now 1).
                ((CheckBox) field(page, "normalize")).setSelected(false);
                return null;
            });
            pulse();
            fx(() -> {
                pointEvent(compareChart(page), 1, MouseEvent.MOUSE_MOVED);
                assertTrue(shownTooltip().getText().startsWith("GRB250603A"));
                return null;
            });
        } finally {
            fx(() -> { stage.close(); return null; });
        }
    }

    @Test
    void includedColumnsCanAllBeHiddenRestoredAndRememberedWithThinScrollbar() throws Exception {
        String key = "test.included." + System.nanoTime();
        TableView<String> table = fx(() -> {
            TableView<String> result = new TableView<>();
            for (String name : List.of("GRB", "T90", "Classe", "Redshift", "Copertura", "Flag qualità")) {
                TableColumn<String, String> column = new TableColumn<>(name);
                column.setCellValueFactory(value -> new SimpleStringProperty(value.getValue()));
                result.getColumns().add(column);
            }
            result.getItems().setAll(IntStream.range(0, 500).mapToObj(i -> "GRB" + i).toList());
            return result;
        });
        Stage stage = fx(() -> {
            I18n.setLanguage(I18n.Language.EN);
            VBox host = new VBox(6, TablePreferences.install(table, key), table);
            VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
            Stage result = showStyled(host, 1000, 450);
            InteractionPolishEnhancer.install(host);
            ChartInteractionEnhancer.install(host);
            UiTableAndStartupFixes.prepare(host);
            UiTableAndStartupFixes.install(host);
            UiLastMileFixes.prepare(host);
            FinalUiStabilityEnhancer.install(host);
            UiLastMileFixes.install(host);
            FinalRequestedUiFastFixes.install(host);
            FinalTableAlignmentFix.install(host);
            return result;
        });
        try {
            pulse();
            fx(() -> {
                assertNull(stage.getScene().getRoot().lookup(".included-columns-button"));
                ScrollBar bar = (ScrollBar) stage.getScene().getRoot().lookup(".table-external-scrollbar");
                assertEquals(9, bar.getWidth(), 0.5);
                bar.setValue(bar.getMax());
                ScrollBar internal = table.lookupAll(".scroll-bar").stream().filter(ScrollBar.class::isInstance)
                        .map(ScrollBar.class::cast).filter(b -> b.getOrientation() == Orientation.VERTICAL)
                        .findFirst().orElseThrow();
                assertEquals(internal.getMax(), internal.getValue(), 0.001);
                for (var column : table.getColumns()) {
                    assertNotNull(column.getGraphic());
                    ((Button) column.getGraphic().lookup(".column-hide-action")).fire();
                }
                assertTrue(table.getColumns().stream().noneMatch(TableColumn::isVisible));
                assertFalse(TablePreferences.isColumnVisible(key, "GRB"));
                ((Button) stage.getScene().getRoot().lookup(".hidden-column-restore-all")).fire();
                assertTrue(table.getColumns().stream().allMatch(TableColumn::isVisible));
                assertTrue(TablePreferences.isColumnVisible(key, "GRB"));
                I18n.setLanguage(I18n.Language.IT);
                return null;
            });
            pulse();
            fx(() -> {
                for (var column : table.getColumns()) {
                    assertNotNull(column.getGraphic());
                    assertNotNull(column.getGraphic().lookup(".column-hide-action"));
                }
                return null;
            });
        } finally {
            fx(() -> { stage.close(); return null; });
            java.util.prefs.Preferences.userNodeForPackage(TablePreferences.class).node(key).removeNode();
        }
    }

    private static Stage showStyled(Parent root, int width, int height) {
        root.getStyleClass().addAll("app-root", "reference-redesign", "black-hole-redesign");
        Stage stage = new Stage();
        Scene scene = new Scene(root, width, height);
        for (String sheet : List.of("app.css", "ui-refinements.css", "black-hole-theme.css",
                "reference-redesign.css", "stability-final.css")) {
            scene.getStylesheets().add(ExplorerUiLifecycleTest.class.getResource("/" + sheet).toExternalForm());
        }
        stage.setScene(scene);
        stage.show();
        root.applyCss();
        root.layout();
        return stage;
    }

    @Test
    void filterSliderEndpointReleaseDoesNotCollapseButBlankClickDoes() throws Exception {
        Stage stage = fx(() -> {
            ScrollBar minimum = new ScrollBar();
            ScrollBar maximum = new ScrollBar();
            for (ScrollBar bar : List.of(minimum, maximum)) {
                bar.setOrientation(Orientation.HORIZONTAL);
                bar.setMax(100);
                bar.getStyleClass().add("fracexp-slider");
            }
            VBox card = new VBox(12, minimum, maximum, new Region());
            card.getStyleClass().add("population-filter-card");
            Button restore = new Button("Show filters");
            restore.getStyleClass().add("population-filter-restore");
            restore.setManaged(false);
            restore.setVisible(false);
            VBox root = new VBox(restore, card);
            Stage result = showStyled(root, 800, 500);
            InteractiveViewSyncEnhancer.install(root);
            ChartInteractionEnhancer.install(root);
            return result;
        });
        try {
            pulse();
            fx(() -> {
                VBox card = (VBox) stage.getScene().getRoot().lookup(".population-filter-card");
                for (Node node : card.lookupAll(".fracexp-slider")) {
                    ScrollBar bar = (ScrollBar) node;
                    Node thumb = bar.lookup(".thumb");
                    thumb.fireEvent(mouse(thumb, MouseEvent.MOUSE_PRESSED, 2, 2, true));
                    thumb.fireEvent(mouse(thumb, MouseEvent.MOUSE_DRAGGED, 600, 2, true));
                    bar.setValue(100);
                    card.fireEvent(mouse(card, MouseEvent.MOUSE_RELEASED, 780, 2, false));
                    card.fireEvent(mouse(card, MouseEvent.MOUSE_CLICKED, 780, 2, false));
                    assertTrue(card.isVisible());
                    assertTrue(card.isManaged());
                }
                card.fireEvent(mouse(card, MouseEvent.MOUSE_PRESSED, 10, 250, true));
                card.fireEvent(mouse(card, MouseEvent.MOUSE_RELEASED, 10, 250, false));
                card.fireEvent(mouse(card, MouseEvent.MOUSE_CLICKED, 10, 250, false));
                assertFalse(card.isManaged());
                ((Button) stage.getScene().getRoot().lookup(".population-filter-restore")).fire();
                assertTrue(card.isManaged());
                return null;
            });
        } finally { fx(() -> { stage.close(); return null; }); }
    }

    @Test
    void unavailableOfficialResultsSelectMapAndPeakOnlyResultsRemainUsable() throws Exception {
        fx(() -> {
            var missing = new it.casiraghi.swiftbat.model.SpectralData.Result(
                    it.casiraghi.swiftbat.model.SpectralData.Interval.T100, "PL", spectralFit(null), null, List.of(), List.of());
            var data = new it.casiraghi.swiftbat.model.SpectralData("GRBTEST", "1", java.util.Map.of(missing.interval(), missing));
            SpectroscopyPane pane = new SpectroscopyPane(compareData("GRBTEST", 3), data, null);
            Stage stage = showStyled(pane, 1240, 800);
            try {
                javafx.scene.control.TabPane tabs = (javafx.scene.control.TabPane) pane.lookup(".spectroscopy-tabs");
                assertTrue(tabs.getTabs().get(0).isDisable());
                assertSame(tabs.getTabs().get(1), tabs.getSelectionModel().getSelectedItem());
                assertNull(tabs.getTabs().get(1).getContent().lookup(".spectroscopy-controls"));
            } finally { stage.close(); }
            var peak = new it.casiraghi.swiftbat.model.SpectralData.Result(
                    it.casiraghi.swiftbat.model.SpectralData.Interval.PEAK_ONE_SECOND, "PL", spectralFit(-1.5), null, List.of(), List.of());
            data = new it.casiraghi.swiftbat.model.SpectralData("GRBTEST", "1", java.util.Map.of(missing.interval(), missing, peak.interval(), peak));
            pane = new SpectroscopyPane(compareData("GRBTEST", 3), data, null);
            stage = showStyled(pane, 1240, 800);
            try {
                javafx.scene.control.TabPane tabs = (javafx.scene.control.TabPane) pane.lookup(".spectroscopy-tabs");
                assertFalse(tabs.getTabs().get(0).isDisable(), "PL does not need Epeak to draw its spectrum");
                assertSame(tabs.getTabs().get(0), tabs.getSelectionModel().getSelectedItem());
                assertEquals(peak.interval(), ((javafx.scene.control.ChoiceBox<?>) field(pane, "intervalChoice")).getValue());
                assertNotNull(tabs.getTabs().get(0).getContent().lookup(".spectroscopy-controls"));
                tabs.getSelectionModel().select(1);
                assertNull(tabs.getTabs().get(1).getContent().lookup(".spectroscopy-controls"));
            } finally { stage.close(); }
            return null;
        });
    }

    private static it.casiraghi.swiftbat.model.SpectralData.Fit spectralFit(Double alpha) {
        return new it.casiraghi.swiftbat.model.SpectralData.Fit(
                it.casiraghi.swiftbat.model.SpectralData.Model.POWER_LAW,
                alpha, null, null, null, null, null, alpha == null ? null : 0.01,
                null, null, null, null, null, null, 50.0, 30.0, 0.0, 30.0);
    }

    @Test
    void officialControlsFollowScrollAndReturnToTheirOriginalPosition() throws Exception {
        SpectroscopyPane pane = fx(() -> {
            var interval = it.casiraghi.swiftbat.model.SpectralData.Interval.T100;
            var result = new it.casiraghi.swiftbat.model.SpectralData.Result(interval, "PL", spectralFit(-1.5), null,
                    List.of(new it.casiraghi.swiftbat.model.SpectralData.EnergyFluxBand("15–25 keV", 15, 25, 1e-8, 9e-9, 2e-8)), List.of());
            return new SpectroscopyPane(compareData("GRBTEST", 3),
                    new it.casiraghi.swiftbat.model.SpectralData("GRBTEST", "1", java.util.Map.of(interval, result)), null);
        });
        Stage stage = fx(() -> showStyled(pane, 1200, 650));
        try {
            pulse();
            fx(() -> { ((javafx.scene.control.ScrollPane) pane.getCenter()).setVvalue(0.6); return null; });
            pulse();
            fx(() -> {
                Node controls = pane.lookup(".spectroscopy-controls");
                Node viewport = ((javafx.scene.control.ScrollPane) pane.getCenter()).lookup(".viewport");
                assertTrue(controls.getTranslateY() > 0);
                assertEquals(1, ((javafx.scene.paint.Color) ((Region) controls).getBackground()
                        .getFills().get(0).getFill()).getOpacity(), 0.001, "Sticky controls must obscure the chart below");
                assertEquals(viewport.localToScene(viewport.getBoundsInLocal()).getMinY(),
                        controls.localToScene(0, 0).getY(), 2);
                if (Boolean.getBoolean("swiftbat.uiSnapshots")) {
                    javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(
                            pane.snapshot(null, null), null), "png", new java.io.File("target/spectral-sticky.png"));
                }
                ((javafx.scene.control.ScrollPane) pane.getCenter()).setVvalue(0);
                return null;
            });
            pulse();
            fx(() -> { assertEquals(0, pane.lookup(".spectroscopy-controls").getTranslateY(), 0.5); return null; });
        } finally { fx(() -> { stage.close(); return null; }); }
    }

    @Test
    void stickyControlsAlsoTrackAnOuterExplorerScrollPane() throws Exception {
        javafx.scene.control.ScrollPane outer = fx(() -> {
            Region controls = new Region();
            controls.setPrefHeight(80);
            controls.setMinHeight(80);
            controls.getStyleClass().add("spectroscopy-controls");
            Region charts = new Region();
            charts.setMinHeight(1600);
            VBox content = new VBox(controls, charts);
            StickySpectralControls.install(controls, content);
            javafx.scene.control.ScrollPane inner = new javafx.scene.control.ScrollPane(content);
            inner.setFitToWidth(true);
            inner.setMinHeight(900);
            inner.setMaxHeight(900);
            Region heading = new Region();
            heading.setMinHeight(300);
            javafx.scene.control.ScrollPane result = new javafx.scene.control.ScrollPane(new VBox(heading, inner));
            result.setFitToWidth(true);
            return result;
        });
        Stage stage = fx(() -> showStyled(outer, 1200, 650));
        try {
            pulse();
            fx(() -> {
                outer.setVvalue(0.8);
                ((javafx.scene.control.ScrollPane) ((VBox) outer.getContent()).getChildren().get(1)).setVvalue(0.5);
                return null;
            });
            pulse();
            fx(() -> {
                Node controls = outer.lookup(".spectroscopy-controls");
                assertTrue(controls.getTranslateY() > 0);
                assertEquals(outer.lookup(".viewport").localToScene(0, 0).getY(), controls.localToScene(0, 0).getY(), 2);
                return null;
            });
        } finally { fx(() -> { stage.close(); return null; }); }
    }

    private static GrbData compareData(String name, int peak) {
        TabularData ascii = new TabularData(List.of("TIME_FROM_TRIGGER_CENTER_S", "RATE_15_350_KEV", "ERROR_15_350_KEV"),
                List.of(List.of("-30", "0", "0.1"), List.of("0", String.valueOf(peak), "0.2"),
                        List.of("30", "0", "0.1")));
        return new GrbData(name, "1", Instant.EPOCH, null, List.of(), ascii, TabularData.empty(), List.of(), List.of());
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    @SuppressWarnings("unchecked")
    private static LineChart<Number, Number> compareChart(Parent root) {
        root.applyCss();
        root.layout();
        return (LineChart<Number, Number>) root.lookup(".compare-chart");
    }

    private static void awaitCompareChart(Parent root) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (fx(() -> compareChart(root) != null)) return;
            pulse(); // Compare may rebuild after editable selectors regain their Scene.
        }
        fail("Compare chart did not return after fullscreen");
    }

    private static void pointEvent(LineChart<Number, Number> chart, int seriesIndex,
                                   javafx.event.EventType<MouseEvent> type) {
        pointEvent(chart, seriesIndex, type, 1);
    }

    private static void pointEvent(LineChart<Number, Number> chart, int seriesIndex,
                                   javafx.event.EventType<MouseEvent> type, int clickCount) {
        XYChart.Data<Number, Number> point = chart.getData().get(seriesIndex).getData().get(1);
        Point2D x = chart.getXAxis().localToScene(chart.getXAxis().getDisplayPosition(point.getXValue()), 0);
        Point2D y = chart.getYAxis().localToScene(0, chart.getYAxis().getDisplayPosition(point.getYValue()));
        Point2D screen = chart.localToScreen(chart.sceneToLocal(x.getX(), y.getY()));
        chart.fireEvent(new MouseEvent(type, x.getX(), y.getY(), screen.getX(), screen.getY(), MouseButton.PRIMARY, clickCount,
                false, false, false, false, false, false, false, false, false, true,
                new PickResult(chart, x.getX(), y.getY())));
    }

    private static Tooltip shownTooltip() {
        return Window.getWindows().stream().filter(Tooltip.class::isInstance).map(Tooltip.class::cast)
                .filter(Window::isShowing).findFirst().orElseThrow();
    }

    private static void assertNoTooltip() {
        assertTrue(Window.getWindows().stream().filter(Tooltip.class::isInstance).noneMatch(Window::isShowing));
    }

    @Test
    void backAndEscWaitForNestedParticipantOnlyOnce() throws Exception {
        Harness h = fx(() -> new Harness(true));
        try {
            fx(() -> {
                back(h.scene.getRoot()).fire();
                InPlaceFullscreen.close(h.scene.getRoot());
                h.scene.getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                        false, false, false, false));
                assertEquals(1, h.content.preparations);
                return null;
            });
            pulse();
            fx(() -> { assertNotSame(h.original, h.scene.getRoot()); return null; });
            h.content.ready.complete(null);
            h.restored.get(5, TimeUnit.SECONDS);
            fx(() -> { assertSame(h.original, h.scene.getRoot()); return null; });
        } finally {
            fx(() -> { h.stage.close(); return null; });
        }
    }

    @Test
    void nativeFullscreenExitAlsoWaitsForDisposal() throws Exception {
        Harness h = fx(() -> new Harness(false));
        try {
            fx(() -> { h.stage.setFullScreen(false); return null; });
            pulse();
            fx(() -> {
                assertEquals(1, h.content.preparations);
                assertNotSame(h.original, h.scene.getRoot());
                return null;
            });
            h.content.ready.complete(null);
            h.restored.get(5, TimeUnit.SECONDS);
        } finally {
            fx(() -> { h.stage.close(); return null; });
        }
    }

    @Test
    void repeatedOpenCloseReleasesSessionAndKeepsOriginalRoot() throws Exception {
        for (int i = 0; i < 5; i++) {
            Harness h = fx(() -> new Harness(true));
            try {
                h.content.ready.complete(null);
                fx(() -> { back(h.scene.getRoot()).fire(); return null; });
                h.restored.get(5, TimeUnit.SECONDS);
                CompletableFuture<Void> restoredAgain = new CompletableFuture<>();
                fx(() -> {
                    // Exercise the ordinary, non-Swing path in the same Scene.
                    h.scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
                        if (newRoot == h.original) restoredAgain.complete(null);
                    });
                    InPlaceFullscreen.show(h.original, "2D", new StackPane());
                    assertNotSame(h.original, h.scene.getRoot());
                    back(h.scene.getRoot()).fire();
                    return null;
                });
                restoredAgain.get(5, TimeUnit.SECONDS);
            } finally {
                fx(() -> { h.stage.close(); return null; });
            }
        }
    }

    private static final class DeferredContent extends StackPane implements InPlaceFullscreen.CloseParticipant {
        final CompletableFuture<Void> ready = new CompletableFuture<>();
        int preparations;
        @Override public CompletionStage<Void> prepareForFullscreenExit() {
            preparations++;
            return ready;
        }
    }

    private static final class Harness {
        final StackPane original = new StackPane();
        final Scene scene = new Scene(original, 800, 600);
        final Stage stage = new Stage();
        final DeferredContent content = new DeferredContent();
        final CompletableFuture<Void> restored = new CompletableFuture<>();
        Harness(boolean nested) {
            stage.setScene(scene);
            stage.show();
            scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
                if (newRoot == original) restored.complete(null);
            });
            InPlaceFullscreen.show(original, "3D", nested ? new VBox(content) : content);
            assertNotSame(original, scene.getRoot());
        }
    }

    private static Button back(Parent root) {
        return root.lookupAll(".fullscreen-toolbar").stream().filter(Parent.class::isInstance)
                .map(Parent.class::cast).flatMap(parent -> parent.getChildrenUnmodifiable().stream())
                .filter(Button.class::isInstance).map(Button.class::cast).findFirst().orElseThrow();
    }

    private static MouseEvent mouse(Node node, javafx.event.EventType<MouseEvent> type,
                                    double x, double y, boolean down) {
        return new MouseEvent(node, node, type, x, y, x, y, MouseButton.PRIMARY, 1,
                false, false, false, false, down, false, false, false, false, true,
                new PickResult(node, x, y));
    }

    private static Label caption(ExplorerPage page) throws Exception {
        Field field = ExplorerPage.class.getDeclaredField("catalogCount");
        field.setAccessible(true);
        return (Label) field.get(page);
    }

    private static List<CatalogEntry> entries() {
        return List.of(new CatalogEntry("GRB250605A", "1321323", "https://example.invalid/a"),
                new CatalogEntry("GRB250603A", "1321200", "https://example.invalid/b"));
    }

    private static void pulse() throws Exception {
        CompletableFuture<Void> ready = new CompletableFuture<>();
        fx(() -> {
            PauseTransition delay = new PauseTransition(Duration.millis(200));
            delay.setOnFinished(event -> ready.complete(null));
            delay.play();
            return null;
        });
        ready.get(5, TimeUnit.SECONDS);
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try { result.complete(action.call()); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
