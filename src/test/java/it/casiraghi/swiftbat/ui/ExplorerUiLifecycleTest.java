package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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
            assertEquals(20.0, bar.getWidth(), 0.5);
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
