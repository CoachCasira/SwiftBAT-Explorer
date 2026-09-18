package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.SkyBurst;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Installs the expert-facing filter/search behaviours directly on the already
 * constructed Explorer and Sky-map pages.  Installing from MainView avoids the
 * first-layout race that previously made ComboBox button text correct only
 * after opening the first GRB.
 */
public final class ExpertFilterAndSearchEnhancer {
    private static final String CACHE_DONE = ExpertFilterAndSearchEnhancer.class.getName() + ".cacheDone";
    private static final String THUMB_DONE = ExpertFilterAndSearchEnhancer.class.getName() + ".thumbDone";
    private static final String SKY_DONE = ExpertFilterAndSearchEnhancer.class.getName() + ".skyDone";
    private static final String BAR_WATCHED = ExpertFilterAndSearchEnhancer.class.getName() + ".barWatched";

    private static final double CATALOG_THUMB_LENGTH = 112.0;

    private static final String FILTER_ALL = "Tutti i GRB";
    private static final String FILTER_SHORT = "Short · T90 ≤ 2 s";
    private static final String FILTER_LONG = "Long · T90 > 2 s";
    private static final String FILTER_UNKNOWN = "T90 non disponibile";

    private ExpertFilterAndSearchEnhancer() {
    }

    public static void install(MainView mainView) {
        if (mainView == null) return;

        Object explorerValue = readField(mainView, "explorerPage");
        if (explorerValue instanceof ExplorerPage explorer) {
            installCacheFilter(explorer);
            installCatalogThumb(explorer);
        }

        Object skyValue = readField(mainView, "skyMapPage");
        if (skyValue instanceof SkyMapPage skyMap) {
            installSkyControls(skyMap);
        }
    }

    /* ---------------- Explorer: Local cache text from the first pulse ---------------- */

    @SuppressWarnings("unchecked")
    private static void installCacheFilter(ExplorerPage explorer) {
        Object value = readField(explorer, "cacheFilter");
        if (!(value instanceof ComboBox<?> rawCombo)) return;
        ComboBox<String> combo = (ComboBox<String>) rawCombo;
        if (Boolean.TRUE.equals(combo.getProperties().get(CACHE_DONE))) return;
        combo.getProperties().put(CACHE_DONE, Boolean.TRUE);

        combo.setConverter(new StringConverter<>() {
            @Override public String toString(String item) {
                return cacheText(item);
            }

            @Override public String fromString(String text) {
                if (text == null) return combo.getValue();
                for (String item : combo.getItems()) {
                    if (text.equals(item) || text.equals(cacheText(item))) return item;
                }
                return combo.getValue();
            }
        });

        combo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : cacheText(item));
            }
        });

        /* A bound button-cell text cannot be overwritten by the generic live
           localization watcher.  This is what makes the selected value reliable
           on the very first Explorer opening as well as after workspace rebuilds. */
        ListCell<String> buttonCell = new ListCell<>();
        buttonCell.setAlignment(Pos.CENTER_LEFT);
        buttonCell.textProperty().bind(Bindings.createStringBinding(
                () -> cacheText(combo.getValue()),
                combo.valueProperty(), I18n.languageProperty()));
        combo.setButtonCell(buttonCell);
        combo.requestLayout();
    }

    private static String cacheText(String value) {
        if (value == null) return "";
        return switch (value) {
            case "Tutti" -> I18n.dynamic("Tutti", "All");
            case "Solo in cache" -> I18n.dynamic("Solo in cache", "Cached only");
            case "Da scaricare" -> I18n.dynamic("Da scaricare", "To download");
            default -> I18n.t(value);
        };
    }

    /* ---------------- Explorer: keep working drag, restore a usable thumb ---------------- */

    @SuppressWarnings("unchecked")
    private static void installCatalogThumb(ExplorerPage explorer) {
        Object value = readField(explorer, "catalogList");
        if (!(value instanceof ListView<?> rawList)) return;
        ListView<CatalogEntry> list = (ListView<CatalogEntry>) rawList;
        if (Boolean.TRUE.equals(list.getProperties().get(THUMB_DONE))) return;
        list.getProperties().put(THUMB_DONE, Boolean.TRUE);

        list.skinProperty().addListener((obs, oldSkin, newSkin) -> scheduleCatalogThumb(list));
        list.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) scheduleCatalogThumb(list);
        });
        list.itemsProperty().addListener((obs, oldItems, newItems) -> scheduleCatalogThumb(list));
        list.heightProperty().addListener((obs, oldHeight, newHeight) -> scheduleCatalogThumb(list));
        scheduleCatalogThumb(list);
    }

    private static void scheduleCatalogThumb(ListView<?> list) {
        if (list == null) return;
        Platform.runLater(() -> {
            polishCatalogThumb(list);
            Platform.runLater(() -> {
                polishCatalogThumb(list);
                Platform.runLater(() -> polishCatalogThumb(list));
            });
        });
    }

    private static void polishCatalogThumb(ListView<?> list) {
        if (list == null || list.getScene() == null || list.getSkin() == null) return;
        try {
            list.applyCss();
            for (Node node : list.lookupAll(".scroll-bar")) {
                if (!(node instanceof ScrollBar bar) || bar.getOrientation() != Orientation.VERTICAL) continue;
                watchCatalogBar(list, bar);
                enlargeThumb(bar);
            }
        } catch (RuntimeException ignored) {
            // A VirtualFlow may briefly replace its skin while the page is attached.
        }
    }

    private static void watchCatalogBar(ListView<?> list, ScrollBar bar) {
        if (Boolean.TRUE.equals(bar.getProperties().get(BAR_WATCHED))) return;
        bar.getProperties().put(BAR_WATCHED, Boolean.TRUE);
        bar.skinProperty().addListener((obs, oldSkin, newSkin) -> scheduleCatalogThumb(list));
        bar.heightProperty().addListener((obs, oldHeight, newHeight) -> scheduleCatalogThumb(list));
        bar.visibleAmountProperty().addListener((obs, oldAmount, newAmount) -> scheduleCatalogThumb(list));
    }

    private static void enlargeThumb(ScrollBar bar) {
        Node node = bar.lookup(".thumb");
        Node trackNode = bar.lookup(".track");
        if (!(node instanceof Region thumb) || !(trackNode instanceof Region track)) return;
        double trackHeight = track.getHeight();
        if (trackHeight <= 1.0) return;
        double requested = Math.min(CATALOG_THUMB_LENGTH, trackHeight);
        thumb.setMinHeight(requested);
        thumb.setPrefHeight(requested);
        if (thumb.getHeight() + 0.5 < requested) {
            thumb.resize(thumb.getWidth(), requested);
        }
        /* ExplorerScrollbarFix's StableScrollBarSkin reads the actual visible
           thumb height when mapping a drag to the value range, so enlarging it
           here keeps the now-correct full-range drag behaviour unchanged. */
        bar.requestLayout();
    }

    /* ---------------- Sky map: checkbox duration filter + Compare-like search ---------------- */

    private static void installSkyControls(SkyMapPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(SKY_DONE))) return;
        Object searchValue = readField(page, "search");
        Object durationValue = readField(page, "durationFilter");
        Object debounceValue = readField(page, "filterDebounce");
        if (!(searchValue instanceof TextField search)
                || !(durationValue instanceof ComboBox<?> rawDuration)
                || !(debounceValue instanceof PauseTransition nativeDebounce)) return;

        @SuppressWarnings("unchecked")
        ComboBox<String> backingDuration = (ComboBox<String>) rawDuration;
        page.getProperties().put(SKY_DONE, Boolean.TRUE);
        new SkyController(page, search, backingDuration, nativeDebounce).install();
    }

    private static final class SkyController {
        private final SkyMapPage page;
        private final TextField search;
        private final ComboBox<String> backingDuration;
        private final PauseTransition nativeDebounce;
        private final PauseTransition applyDelay = new PauseTransition(Duration.millis(220));

        private final MenuButton durationMenu = new MenuButton();
        private final CheckMenuItem allItem = new CheckMenuItem();
        private final CheckMenuItem shortItem = new CheckMenuItem();
        private final CheckMenuItem longItem = new CheckMenuItem();
        private final CheckMenuItem unknownItem = new CheckMenuItem();
        private final LinkedHashSet<String> selectedDurations = new LinkedHashSet<>();

        private final Label ghostPrefix = new Label();
        private final Label ghostSuffix = new Label();
        private final HBox ghostBox = new HBox(0, ghostPrefix, ghostSuffix);
        private String ghostSuggestion = "";
        private boolean internalDuration;
        private boolean applying;

        private SkyController(SkyMapPage page, TextField search,
                              ComboBox<String> backingDuration, PauseTransition nativeDebounce) {
            this.page = page;
            this.search = search;
            this.backingDuration = backingDuration;
            this.nativeDebounce = nativeDebounce;
        }

        private void install() {
            installDurationMenu();
            installSearchAssist();
            installFilterOwnership();
            installResetOwnership();
            installCatalogReadyHook();
            applyDelay.setOnFinished(event -> applySelectedDurations());

            selectedDurations.add(FILTER_ALL);
            syncDurationVisuals();
            search.setText("GRB");
            requestApply();
        }

        private void installDurationMenu() {
            Parent parent = backingDuration.getParent();
            if (!(parent instanceof HBox row)) return;
            int index = row.getChildren().indexOf(backingDuration);
            if (index < 0) return;

            durationMenu.getStyleClass().addAll("choice-box-modern", "sky-duration-multi-select");
            durationMenu.setMinWidth(backingDuration.getMinWidth());
            durationMenu.setPrefWidth(backingDuration.getPrefWidth());
            durationMenu.setMaxWidth(Math.max(260.0, backingDuration.getMaxWidth()));
            durationMenu.setFocusTraversable(false);
            durationMenu.getItems().setAll(
                    allItem, new SeparatorMenuItem(), shortItem, longItem, unknownItem);

            Priority grow = HBox.getHgrow(backingDuration);
            Insets margin = HBox.getMargin(backingDuration);
            row.getChildren().set(index, durationMenu);
            if (grow != null) HBox.setHgrow(durationMenu, grow);
            if (margin != null) HBox.setMargin(durationMenu, margin);

            allItem.setOnAction(event -> selectAllDurations());
            shortItem.setOnAction(event -> toggleDuration(FILTER_SHORT, shortItem));
            longItem.setOnAction(event -> toggleDuration(FILTER_LONG, longItem));
            unknownItem.setOnAction(event -> toggleDuration(FILTER_UNKNOWN, unknownItem));
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> relabelDurationMenu());
            relabelDurationMenu();
        }

        private void selectAllDurations() {
            if (internalDuration) return;
            selectedDurations.clear();
            selectedDurations.add(FILTER_ALL);
            syncBackingDuration();
            syncDurationVisuals();
            requestApply();
            durationMenu.hide();
        }

        private void toggleDuration(String value, CheckMenuItem item) {
            if (internalDuration) return;
            selectedDurations.remove(FILTER_ALL);
            if (item.isSelected()) selectedDurations.add(value);
            else selectedDurations.remove(value);

            if (selectedDurations.isEmpty()) {
                selectedDurations.add(FILTER_ALL);
            } else if (selectedDurations.size() == 3) {
                // Selecting every concrete class is exactly equivalent to All.
                selectedDurations.clear();
                selectedDurations.add(FILTER_ALL);
            }

            syncBackingDuration();
            syncDurationVisuals();
            requestApply();
            if (!selectedDurations.contains(FILTER_ALL)) {
                Platform.runLater(() -> {
                    if (durationMenu.getScene() != null && !durationMenu.isShowing()) durationMenu.show();
                });
            }
        }

        private void syncBackingDuration() {
            internalDuration = true;
            try {
                if (!selectedDurations.contains(FILTER_ALL) && selectedDurations.size() == 1) {
                    backingDuration.setValue(selectedDurations.iterator().next());
                } else {
                    backingDuration.setValue(FILTER_ALL);
                }
            } finally {
                internalDuration = false;
                nativeDebounce.stop();
            }
        }

        private void syncDurationVisuals() {
            internalDuration = true;
            try {
                boolean all = selectedDurations.contains(FILTER_ALL);
                allItem.setSelected(all);
                shortItem.setSelected(selectedDurations.contains(FILTER_SHORT));
                longItem.setSelected(selectedDurations.contains(FILTER_LONG));
                unknownItem.setSelected(selectedDurations.contains(FILTER_UNKNOWN));
                updateDurationText();
            } finally {
                internalDuration = false;
            }
        }

        private void relabelDurationMenu() {
            allItem.setText(I18n.dynamic("Tutti i GRB", "All GRBs"));
            shortItem.setText("Short · T90 ≤ 2 s");
            longItem.setText("Long · T90 > 2 s");
            unknownItem.setText(I18n.dynamic("T90 non disponibile", "T90 unavailable"));
            updateDurationText();
        }

        private void updateDurationText() {
            String text;
            if (selectedDurations.contains(FILTER_ALL) || selectedDurations.isEmpty()) {
                text = I18n.dynamic("Tutti i GRB", "All GRBs");
            } else if (selectedDurations.size() == 1) {
                text = displayDuration(selectedDurations.iterator().next());
            } else {
                int count = selectedDurations.size();
                text = I18n.dynamic(count + " classi T90", count + " T90 classes");
            }
            durationMenu.setText(text);
        }

        private String displayDuration(String value) {
            if (FILTER_UNKNOWN.equals(value)) {
                return I18n.dynamic("T90 non disponibile", "T90 unavailable");
            }
            return value;
        }

        private void installSearchAssist() {
            search.setPromptText("");
            search.setTextFormatter(new TextFormatter<String>(change -> {
                String next = change.getControlNewText() == null
                        ? "" : change.getControlNewText().toUpperCase(Locale.ROOT);
                if (!next.startsWith("GRB")) return null;
                String suffix = next.substring(3);
                if (!suffix.matches("[0-9A-Z]*")) return null;
                if (change.getText() != null) change.setText(change.getText().toUpperCase(Locale.ROOT));
                return change;
            }));

            ghostPrefix.setOpacity(0.0);
            ghostSuffix.setTextFill(Color.rgb(144, 166, 205, 0.72));
            ghostPrefix.fontProperty().bind(search.fontProperty());
            ghostSuffix.fontProperty().bind(search.fontProperty());
            ghostBox.setAlignment(Pos.CENTER_LEFT);
            ghostBox.setMouseTransparent(true);
            ghostBox.setPadding(new Insets(0, 14, 0, 13));
            ghostBox.setVisible(false);

            StackPane stack = new StackPane(search, ghostBox);
            StackPane.setAlignment(search, Pos.CENTER_LEFT);
            StackPane.setAlignment(ghostBox, Pos.CENTER_LEFT);
            stack.minWidthProperty().bind(search.minWidthProperty());
            stack.prefWidthProperty().bind(search.prefWidthProperty());
            stack.maxWidthProperty().bind(search.maxWidthProperty());

            Parent parent = search.getParent();
            if (parent instanceof HBox row) {
                int index = row.getChildren().indexOf(search);
                if (index >= 0) {
                    Priority grow = HBox.getHgrow(search);
                    Insets margin = HBox.getMargin(search);
                    row.getChildren().set(index, stack);
                    if (grow != null) HBox.setHgrow(stack, grow);
                    if (margin != null) HBox.setMargin(stack, margin);
                }
            }

            search.focusedProperty().addListener((obs, oldFocused, focused) -> {
                if (focused) {
                    Platform.runLater(() -> search.positionCaret(Math.max(3, search.getCaretPosition())));
                    updateGhostSuggestion(true);
                } else {
                    ghostBox.setVisible(false);
                }
            });
            search.textProperty().addListener((obs, oldText, newText) -> {
                if (search.isFocused()) updateGhostSuggestion(false);
                requestApply();
            });
            search.setOnMouseClicked(event -> Platform.runLater(() -> {
                if (search.getCaretPosition() < 3) search.positionCaret(3);
            }));
            search.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.HOME) {
                    search.positionCaret(3);
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.LEFT && search.getCaretPosition() <= 3
                        && search.getSelection().getLength() == 0) {
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.TAB && !ghostSuggestion.isBlank()) {
                    search.setText(ghostSuggestion);
                    search.positionCaret(ghostSuggestion.length());
                    updateGhostSuggestion(false);
                    event.consume();
                }
            });
            search.setOnAction(event -> {
                nativeDebounce.stop();
                applyDelay.stop();
                applySelectedDurations();
            });
        }

        private void updateGhostSuggestion(boolean randomWhenBarePrefix) {
            String typed = normalizeSearch(search.getText());
            List<String> matches = availableGrbNames().stream()
                    .filter(name -> name.startsWith(typed))
                    .filter(name -> name.length() > typed.length())
                    .toList();

            if (matches.isEmpty()) {
                ghostSuggestion = "";
            } else if (randomWhenBarePrefix && "GRB".equals(typed)) {
                ghostSuggestion = matches.get(ThreadLocalRandom.current().nextInt(matches.size()));
            } else if (!ghostSuggestion.startsWith(typed) || ghostSuggestion.length() <= typed.length()) {
                ghostSuggestion = matches.get(0);
            }

            ghostPrefix.setText(typed);
            ghostSuffix.setText(ghostSuggestion.isBlank() ? "" : ghostSuggestion.substring(typed.length()));
            ghostBox.setVisible(search.isFocused() && !ghostSuggestion.isBlank());
        }

        private String normalizeSearch(String raw) {
            String text = raw == null ? "GRB" : raw.trim().toUpperCase(Locale.ROOT);
            return text.startsWith("GRB") ? text : "GRB";
        }

        @SuppressWarnings("unchecked")
        private List<String> availableGrbNames() {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            Object burstsValue = readField(page, "allBursts");
            if (burstsValue instanceof List<?> bursts) {
                for (Object value : bursts) {
                    if (value instanceof SkyBurst burst && burst.grbName() != null) {
                        names.add(burst.grbName().toUpperCase(Locale.ROOT));
                    }
                }
            }
            Object catalogValue = readField(page, "baseCatalog");
            if (catalogValue instanceof Map<?, ?> catalog) {
                for (Object key : catalog.keySet()) {
                    if (key != null) names.add(key.toString().toUpperCase(Locale.ROOT));
                }
            }
            return names.stream().sorted(Comparator.reverseOrder()).toList();
        }

        private void installFilterOwnership() {
            Object redshiftValue = readField(page, "redshiftFilter");
            if (redshiftValue instanceof ComboBox<?> redshift) {
                redshift.valueProperty().addListener((obs, oldValue, newValue) -> requestApply());
            }
            for (String fieldName : List.of("raMin", "raMax", "decMin", "decMax", "zMin", "zMax")) {
                Object value = readField(page, fieldName);
                if (value instanceof TextField field) {
                    field.textProperty().addListener((obs, oldText, newText) -> requestApply());
                }
            }
        }

        private void installResetOwnership() {
            Button reset = findButton(page, "Reset");
            if (reset == null) return;
            reset.setOnAction(event -> {
                invoke(page, "resetFilters");
                selectedDurations.clear();
                selectedDurations.add(FILTER_ALL);
                syncBackingDuration();
                syncDurationVisuals();
                search.setText("GRB");
                ghostSuggestion = "";
                nativeDebounce.stop();
                applyDelay.stop();
                applySelectedDurations();
            });
        }

        private void installCatalogReadyHook() {
            Object value = readField(page, "status");
            if (!(value instanceof Label status)) return;
            status.textProperty().addListener((obs, oldText, newText) -> {
                if (applying || newText == null) return;
                String normalized = newText.toLowerCase(Locale.ROOT);
                boolean catalogReady = normalized.contains("coordinate")
                        && (normalized.contains("caricat") || normalized.contains("loaded"));
                if (!catalogReady) return;
                Platform.runLater(() -> {
                    if (search.isFocused()) updateGhostSuggestion("GRB".equals(normalizeSearch(search.getText())));
                    applySelectedDurations();
                });
            });
        }

        private void requestApply() {
            if (applying) return;
            nativeDebounce.stop();
            applyDelay.stop();
            applyDelay.playFromStart();
        }

        @SuppressWarnings("unchecked")
        private void applySelectedDurations() {
            if (applying) return;
            applying = true;
            nativeDebounce.stop();
            applyDelay.stop();
            try {
                Object rawBursts = readField(page, "allBursts");
                List<SkyBurst> original = rawBursts instanceof List<?> list
                        ? new ArrayList<>((List<SkyBurst>) list) : List.of();

                if (selectedDurations.contains(FILTER_ALL) || selectedDurations.isEmpty()) {
                    backingDuration.setValue(FILTER_ALL);
                    nativeDebounce.stop();
                    invoke(page, "applyFilters");
                    return;
                }

                if (selectedDurations.size() == 1) {
                    backingDuration.setValue(selectedDurations.iterator().next());
                    nativeDebounce.stop();
                    invoke(page, "applyFilters");
                    return;
                }

                List<SkyBurst> durationSubset = original.stream().filter(this::acceptsDuration).toList();
                String oldBacking = backingDuration.getValue();
                try {
                    writeField(page, "allBursts", List.copyOf(durationSubset));
                    backingDuration.setValue(FILTER_ALL);
                    nativeDebounce.stop();
                    invoke(page, "applyFilters");
                } finally {
                    writeField(page, "allBursts", List.copyOf(original));
                    backingDuration.setValue(oldBacking == null ? FILTER_ALL : oldBacking);
                    nativeDebounce.stop();
                }
                correctMultiSelectionStatus(original.size());
            } finally {
                applying = false;
            }
        }

        private boolean acceptsDuration(SkyBurst burst) {
            if (burst == null) return false;
            if (selectedDurations.contains(FILTER_SHORT) && burst.isShort()) return true;
            if (selectedDurations.contains(FILTER_LONG) && burst.isLong()) return true;
            return selectedDurations.contains(FILTER_UNKNOWN) && !burst.hasT90();
        }

        @SuppressWarnings("unchecked")
        private void correctMultiSelectionStatus(int totalCount) {
            Object statusValue = readField(page, "status");
            Object visibleValue = readField(page, "visibleBursts");
            if (!(statusValue instanceof Label status) || !(visibleValue instanceof List<?> visible)) return;
            if (status.getStyleClass().contains("status-warning")) return;
            int shown = visible.size();
            I18n.setText(status,
                    shown + " / " + totalCount + " GRB visualizzati",
                    shown + " / " + totalCount + " GRBs shown");
        }
    }

    /* ---------------- Small reflection/tree helpers ---------------- */

    private static Button findButton(Node node, String text) {
        if (node instanceof Button button && text.equalsIgnoreCase(button.getText())) return button;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Object readField(Object target, String name) {
        if (target == null || name == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean writeField(Object target, String name, Object value) {
        if (target == null || name == null) return false;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return true;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
        return false;
    }

    private static Object invoke(Object target, String name) {
        if (target == null || name == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }
}