package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.service.OnlineGrbService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Final owner for the Explorer catalogue search and the top cache telemetry.
 *
 * <p>The Explorer search intentionally mirrors Compare: GRB is a fixed prefix,
 * the completion is only a ghost hint, TAB accepts it, and typing filters the
 * catalogue immediately.  It replaces the previously assisted TextField after
 * startup so older random-on-click listeners remain detached from the scene.</p>
 */
public final class ExplorerSearchAndCacheFix {
    private static final String SEARCH_INSTALLED = ExplorerSearchAndCacheFix.class.getName() + ".searchInstalled";
    private static final String CACHE_INSTALLED = ExplorerSearchAndCacheFix.class.getName() + ".cacheInstalled";

    private ExplorerSearchAndCacheFix() { }

    public static void install(MainView mainView) {
        if (mainView == null) return;
        Object explorerValue = readField(mainView, "explorerPage");
        if (explorerValue instanceof ExplorerPage explorer) installExplorerSearch(explorer);
        installCacheTelemetry(mainView);
    }

    @SuppressWarnings("unchecked")
    private static void installExplorerSearch(ExplorerPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(SEARCH_INSTALLED))) return;

        Object oldSearchValue = readField(page, "catalogSearch");
        Object catalogValue = readField(page, "catalog");
        Object listValue = readField(page, "catalogList");
        if (!(oldSearchValue instanceof TextField backingSearch)
                || !(catalogValue instanceof ObservableList<?> rawCatalog)
                || !(listValue instanceof ListView<?> rawList)) return;

        ObservableList<CatalogEntry> catalog = (ObservableList<CatalogEntry>) rawCatalog;
        ListView<CatalogEntry> list = (ListView<CatalogEntry>) rawList;
        Parent parent = backingSearch.getParent();
        if (parent == null) return;

        TextField search = new TextField("GRB");
        search.getStyleClass().setAll(backingSearch.getStyleClass());
        search.setPromptText("");
        search.setMinWidth(Math.max(0.0, backingSearch.getMinWidth()));
        search.setPrefWidth(backingSearch.getPrefWidth());
        search.setMaxWidth(Double.MAX_VALUE);
        search.setTextFormatter(new TextFormatter<String>(change -> {
            String next = change.getControlNewText() == null
                    ? "" : change.getControlNewText().toUpperCase(Locale.ROOT);
            if (!next.startsWith("GRB")) return null;
            String suffix = next.substring(3);
            if (!suffix.matches("[0-9A-Z]*")) return null;
            if (change.getText() != null) change.setText(change.getText().toUpperCase(Locale.ROOT));
            return change;
        }));

        Label ghostPrefix = new Label();
        Label ghostSuffix = new Label();
        HBox ghostBox = new HBox(0, ghostPrefix, ghostSuffix);
        ghostPrefix.setOpacity(0.0);
        ghostSuffix.setTextFill(Color.rgb(144, 166, 205, 0.72));
        ghostPrefix.fontProperty().bind(search.fontProperty());
        ghostSuffix.fontProperty().bind(search.fontProperty());
        ghostBox.setAlignment(Pos.CENTER_LEFT);
        ghostBox.setMouseTransparent(true);
        ghostBox.setPadding(new Insets(0, 14, 0, 13));
        ghostBox.setVisible(false);

        StackPane stack;
        if (parent instanceof StackPane existing) {
            stack = existing;
            stack.getChildren().setAll(search, ghostBox);
        } else {
            int index = childIndex(parent, backingSearch);
            if (index < 0) return;
            Insets margin = parent instanceof VBox box ? VBox.getMargin(backingSearch)
                    : parent instanceof HBox box ? HBox.getMargin(backingSearch) : null;
            removeChild(parent, backingSearch);
            stack = new StackPane(search, ghostBox);
            if (parent instanceof VBox box) {
                box.getChildren().add(index, stack);
                if (margin != null) VBox.setMargin(stack, margin);
            } else if (parent instanceof HBox box) {
                box.getChildren().add(index, stack);
                Priority grow = HBox.getHgrow(backingSearch);
                if (grow != null) HBox.setHgrow(stack, grow);
                if (margin != null) HBox.setMargin(stack, margin);
            } else {
                return;
            }
        }

        stack.setMinWidth(0);
        stack.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(search, Pos.CENTER_LEFT);
        StackPane.setAlignment(ghostBox, Pos.CENTER_LEFT);

        SearchState state = new SearchState(search, ghostPrefix, ghostSuffix, ghostBox, catalog);

        search.focusedProperty().addListener((obs, oldFocused, focused) -> {
            if (focused) {
                Platform.runLater(() -> search.positionCaret(Math.max(3, search.getCaretPosition())));
                state.update();
            } else {
                ghostBox.setVisible(false);
            }
        });

        // Exactly like Compare: clicking never writes a suggestion into the field
        // and never rotates to another random GRB. It only keeps the GRB prefix safe.
        search.setOnMouseClicked(event -> {
            if (search.getCaretPosition() < 3) search.positionCaret(3);
            state.update();
        });

        search.textProperty().addListener((obs, oldText, newText) -> {
            state.update();
            syncAndFilterImmediately(page, backingSearch, newText);
        });

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
            if (event.getCode() == KeyCode.TAB && !state.suggestion.isBlank()) {
                search.setText(state.suggestion);
                search.positionCaret(state.suggestion.length());
                state.update();
                event.consume();
            }
        });

        // Enter remains a convenient explicit open/select action, but it is no
        // longer required for the matching GRB to appear in the catalogue.
        search.setOnAction(event -> selectExactMatch(search, catalog, list));

        catalog.addListener((ListChangeListener<CatalogEntry>) change -> {
            if (search.isFocused()) Platform.runLater(state::update);
        });

        page.getProperties().put(SEARCH_INSTALLED, Boolean.TRUE);
        Platform.runLater(() -> {
            search.positionCaret(3);
            state.update();
            syncAndFilterImmediately(page, backingSearch, search.getText());
            stack.requestLayout();
        });
    }

    private static void syncAndFilterImmediately(ExplorerPage page, TextField backingSearch, String text) {
        String value = text == null || text.isBlank() ? "GRB" : text.toUpperCase(Locale.ROOT);
        if (!Objects.equals(backingSearch.getText(), value)) backingSearch.setText(value);
        Object debounceValue = readField(page, "catalogFilterDebounce");
        if (debounceValue instanceof PauseTransition debounce) debounce.stop();
        invokeNoArgs(page, "applyCatalogFilters");
    }

    private static void selectExactMatch(TextField search, ObservableList<CatalogEntry> catalog,
                                         ListView<CatalogEntry> list) {
        String typed = normalize(search.getText());
        CatalogEntry match = catalog.stream()
                .filter(Objects::nonNull)
                .filter(entry -> entry.grbName() != null && entry.grbName().equalsIgnoreCase(typed))
                .findFirst().orElse(null);
        if (match == null) return;
        list.getSelectionModel().select(match);
        list.scrollTo(match);
    }

    private static final class SearchState {
        private final TextField search;
        private final Label prefix;
        private final Label suffix;
        private final HBox ghostBox;
        private final ObservableList<CatalogEntry> catalog;
        private String suggestion = "";

        private SearchState(TextField search, Label prefix, Label suffix,
                            HBox ghostBox, ObservableList<CatalogEntry> catalog) {
            this.search = search;
            this.prefix = prefix;
            this.suffix = suffix;
            this.ghostBox = ghostBox;
            this.catalog = catalog;
        }

        private void update() {
            String typed = normalize(search.getText());
            suggestion = catalog.stream()
                    .filter(Objects::nonNull)
                    .map(CatalogEntry::grbName)
                    .filter(Objects::nonNull)
                    .map(name -> name.toUpperCase(Locale.ROOT))
                    .distinct()
                    .filter(name -> name.startsWith(typed) && name.length() > typed.length())
                    .sorted(Comparator.reverseOrder())
                    .findFirst()
                    .orElse("");

            prefix.setText(typed);
            suffix.setText(suggestion.isBlank() ? "" : suggestion.substring(typed.length()));
            ghostBox.setVisible(search.isFocused() && !suggestion.isBlank());
        }
    }

    @SuppressWarnings("unchecked")
    private static void installCacheTelemetry(MainView mainView) {
        Object statusValue = readField(mainView, "sessionStatus");
        Object serviceValue = readField(mainView, "grbService");
        Object sessionValue = readField(mainView, "sessionData");
        if (!(statusValue instanceof Label status)
                || !(serviceValue instanceof OnlineGrbService service)) return;

        final boolean[] correcting = {false};
        Runnable refresh = () -> {
            if (correcting[0]) return;
            correcting[0] = true;
            try {
                int cached = service.persistentCachedCount();
                String text = "In cache · " + cached;
                if (!text.equals(status.getText())) I18n.setText(status, text, text);
            } finally {
                correcting[0] = false;
            }
        };

        if (!Boolean.TRUE.equals(status.getProperties().get(CACHE_INSTALLED))) {
            status.getProperties().put(CACHE_INSTALLED, Boolean.TRUE);

            if (sessionValue instanceof ObservableMap<?, ?> rawMap) {
                ObservableMap<String, GrbData> session = (ObservableMap<String, GrbData>) rawMap;
                session.addListener((javafx.collections.MapChangeListener<String, GrbData>) change ->
                        Platform.runLater(refresh));
            }

            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> Platform.runLater(refresh));

            // MainView still updates this historical field after a GRB load. If it
            // writes the old RAM-oriented caption, restore the cache caption on the
            // next pulse without creating a recursive text-property loop.
            status.textProperty().addListener((obs, oldText, newText) -> {
                if (!correcting[0] && (newText == null || !newText.startsWith("In cache · "))) {
                    Platform.runLater(refresh);
                }
            });
        }
        refresh.run();
    }

    private static String normalize(String raw) {
        String text = raw == null ? "GRB" : raw.trim().toUpperCase(Locale.ROOT);
        return text.startsWith("GRB") ? text : "GRB";
    }

    private static int childIndex(Parent parent, Node child) {
        if (parent instanceof VBox box) return box.getChildren().indexOf(child);
        if (parent instanceof HBox box) return box.getChildren().indexOf(child);
        if (parent instanceof StackPane box) return box.getChildren().indexOf(child);
        return -1;
    }

    private static void removeChild(Parent parent, Node child) {
        if (parent instanceof VBox box) box.getChildren().remove(child);
        else if (parent instanceof HBox box) box.getChildren().remove(child);
        else if (parent instanceof StackPane box) box.getChildren().remove(child);
    }

    private static void invokeNoArgs(Object target, String name) {
        if (target == null || name == null) return;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                method.invoke(target);
                return;
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return;
            }
        }
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
}
