package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Final startup-safe GRB search pass.
 *
 * <p>The Sky-map search assist already existed in ExpertFilterAndSearchEnhancer,
 * but its StackPane could be detached while the TextField was being re-parented.
 * This class deterministically re-attaches that existing assisted control before
 * the duration filter. It also gives Explorer's catalogue search the same
 * GRB-prefix + ghost completion + TAB behaviour from the very first opening.</p>
 */
public final class GrbSearchStartupFix {
    private static final String EXPLORER_DONE = GrbSearchStartupFix.class.getName() + ".explorer";
    private static final String SKY_DONE = GrbSearchStartupFix.class.getName() + ".sky";

    private GrbSearchStartupFix() { }

    public static void install(MainView mainView) {
        if (mainView == null) return;
        Object explorer = readField(mainView, "explorerPage");
        if (explorer instanceof ExplorerPage page) installExplorer(page);
        Object sky = readField(mainView, "skyMapPage");
        if (sky instanceof SkyMapPage page) restoreSkySearch(page);
    }

    /* ---------------- Explorer assisted search ---------------- */

    @SuppressWarnings("unchecked")
    private static void installExplorer(ExplorerPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(EXPLORER_DONE))) return;
        Object searchValue = readField(page, "catalogSearch");
        Object catalogValue = readField(page, "catalog");
        Object listValue = readField(page, "catalogList");
        if (!(searchValue instanceof TextField search)
                || !(catalogValue instanceof ObservableList<?> rawCatalog)
                || !(listValue instanceof ListView<?> rawList)) return;

        ObservableList<CatalogEntry> catalog = (ObservableList<CatalogEntry>) rawCatalog;
        ListView<CatalogEntry> list = (ListView<CatalogEntry>) rawList;
        page.getProperties().put(EXPLORER_DONE, Boolean.TRUE);

        Parent oldParent = search.getParent();
        if (oldParent == null) return;
        int index = childIndex(oldParent, search);
        if (index < 0) return;

        javafx.scene.control.Label ghostPrefix = new javafx.scene.control.Label();
        javafx.scene.control.Label ghostSuffix = new javafx.scene.control.Label();
        HBox ghostBox = new HBox(0, ghostPrefix, ghostSuffix);
        ghostPrefix.setOpacity(0.0);
        ghostSuffix.setTextFill(Color.rgb(144, 166, 205, 0.72));
        ghostPrefix.fontProperty().bind(search.fontProperty());
        ghostSuffix.fontProperty().bind(search.fontProperty());
        ghostBox.setAlignment(Pos.CENTER_LEFT);
        ghostBox.setMouseTransparent(true);
        ghostBox.setPadding(new Insets(0, 14, 0, 13));
        ghostBox.setVisible(false);

        // Detach first; adding a node to StackPane while it still belongs to the
        // VBox can leave only the detached StackPane alive on some JavaFX builds.
        Insets margin = oldParent instanceof VBox ? VBox.getMargin(search)
                : oldParent instanceof HBox ? HBox.getMargin(search) : null;
        removeChild(oldParent, search);
        StackPane stack = new StackPane(search, ghostBox);
        StackPane.setAlignment(search, Pos.CENTER_LEFT);
        StackPane.setAlignment(ghostBox, Pos.CENTER_LEFT);
        stack.setMinWidth(0);
        stack.setMaxWidth(Double.MAX_VALUE);
        if (oldParent instanceof VBox box) {
            box.getChildren().add(index, stack);
            VBox.setVgrow(stack, VBox.getVgrow(search));
            if (margin != null) VBox.setMargin(stack, margin);
        } else if (oldParent instanceof HBox box) {
            box.getChildren().add(index, stack);
            HBox.setHgrow(stack, HBox.getHgrow(search));
            if (margin != null) HBox.setMargin(stack, margin);
        } else {
            return;
        }

        search.setPromptText("");
        search.setText("GRB");
        search.positionCaret(3);
        search.setTextFormatter(new TextFormatter<String>(change -> {
            String next = change.getControlNewText() == null
                    ? "" : change.getControlNewText().toUpperCase(Locale.ROOT);
            if (!next.startsWith("GRB")) return null;
            String suffix = next.substring(3);
            if (!suffix.matches("[0-9A-Z]*")) return null;
            if (change.getText() != null) change.setText(change.getText().toUpperCase(Locale.ROOT));
            return change;
        }));

        SearchState state = new SearchState(search, ghostPrefix, ghostSuffix, ghostBox, catalog);
        search.focusedProperty().addListener((obs, oldFocused, focused) -> {
            if (focused) {
                Platform.runLater(() -> search.positionCaret(Math.max(3, search.getCaretPosition())));
                state.update(true);
            } else {
                ghostBox.setVisible(false);
            }
        });
        search.setOnMouseClicked(event -> {
            if (search.getCaretPosition() < 3) search.positionCaret(3);
            state.update("GRB".equals(normalize(search.getText())));
        });
        search.textProperty().addListener((obs, oldText, newText) -> {
            if (search.isFocused()) state.update(false);
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
                state.update(false);
                event.consume();
            }
        });
        search.setOnAction(event -> openExactExplorerMatch(search, catalog, list));
        catalog.addListener((ListChangeListener<CatalogEntry>) change -> {
            if (search.isFocused()) Platform.runLater(() -> state.update("GRB".equals(normalize(search.getText()))));
        });

        Platform.runLater(() -> {
            if (!search.getText().startsWith("GRB")) search.setText("GRB");
            search.positionCaret(Math.max(3, search.getText().length()));
            stack.requestLayout();
        });
    }

    private static void openExactExplorerMatch(TextField search, ObservableList<CatalogEntry> catalog,
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
        private final javafx.scene.control.Label prefix;
        private final javafx.scene.control.Label suffix;
        private final HBox ghostBox;
        private final ObservableList<CatalogEntry> catalog;
        private String suggestion = "";

        private SearchState(TextField search, javafx.scene.control.Label prefix,
                            javafx.scene.control.Label suffix, HBox ghostBox,
                            ObservableList<CatalogEntry> catalog) {
            this.search = search;
            this.prefix = prefix;
            this.suffix = suffix;
            this.ghostBox = ghostBox;
            this.catalog = catalog;
        }

        private void update(boolean randomWhenBarePrefix) {
            String typed = normalize(search.getText());
            List<String> matches = catalog.stream()
                    .filter(Objects::nonNull)
                    .map(CatalogEntry::grbName)
                    .filter(Objects::nonNull)
                    .map(name -> name.toUpperCase(Locale.ROOT))
                    .distinct()
                    .filter(name -> name.startsWith(typed) && name.length() > typed.length())
                    .sorted(Comparator.reverseOrder())
                    .toList();

            if (matches.isEmpty()) {
                suggestion = "";
            } else if (randomWhenBarePrefix && "GRB".equals(typed)) {
                suggestion = matches.get(ThreadLocalRandom.current().nextInt(matches.size()));
            } else if (!suggestion.startsWith(typed) || suggestion.length() <= typed.length()) {
                suggestion = matches.get(0);
            }

            prefix.setText(typed);
            suffix.setText(suggestion.isBlank() ? "" : suggestion.substring(typed.length()));
            ghostBox.setVisible(search.isFocused() && !suggestion.isBlank());
        }
    }

    /* ---------------- Sky-map search restoration ---------------- */

    private static void restoreSkySearch(SkyMapPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(SKY_DONE))) return;
        Object value = readField(page, "search");
        if (!(value instanceof TextField search)) return;

        // ExpertFilterAndSearchEnhancer already owns the search behaviour. Its
        // assisted StackPane may however have been detached while re-parenting
        // the TextField. Reattach that exact stack so its random hint/TAB logic
        // is preserved rather than installing competing listeners.
        Parent searchParent = search.getParent();
        StackPane assistedStack = searchParent instanceof StackPane stack ? stack : null;
        HBox filterRow = findSkyFilterRow(page);
        if (filterRow == null) return;

        if (assistedStack == null) {
            // Fallback only for a future layout where the enhancer did not wrap
            // the field. Keep the native field visible and preserve filtering.
            assistedStack = new StackPane();
            Parent parent = search.getParent();
            if (parent != null) removeChild(parent, search);
            assistedStack.getChildren().add(search);
            StackPane.setAlignment(search, Pos.CENTER_LEFT);
        } else if (assistedStack.getParent() != null && assistedStack.getParent() != filterRow) {
            removeChild(assistedStack.getParent(), assistedStack);
        }

        if (!filterRow.getChildren().contains(assistedStack)) {
            int durationIndex = indexOfStyle(filterRow, "sky-duration-multi-select");
            if (durationIndex < 0) durationIndex = 0;
            filterRow.getChildren().add(durationIndex, assistedStack);
        }
        assistedStack.setMinWidth(150);
        assistedStack.setPrefWidth(165);
        assistedStack.setMaxWidth(180);
        HBox.setHgrow(assistedStack, Priority.NEVER);
        search.setMinWidth(150);
        search.setPrefWidth(165);
        search.setMaxWidth(180);
        if (search.getText() == null || !search.getText().startsWith("GRB")) search.setText("GRB");
        search.positionCaret(Math.max(3, search.getText().length()));
        page.getProperties().put(SKY_DONE, Boolean.TRUE);

        StackPane finalStack = assistedStack;
        Platform.runLater(() -> {
            if (!filterRow.getChildren().contains(finalStack)) {
                int index = indexOfStyle(filterRow, "sky-duration-multi-select");
                filterRow.getChildren().add(Math.max(0, index), finalStack);
            }
            finalStack.setVisible(true);
            finalStack.setManaged(true);
            search.setVisible(true);
            search.setManaged(true);
            filterRow.requestLayout();
        });
    }

    private static HBox findSkyFilterRow(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof HBox row && indexOfStyle(row, "sky-duration-multi-select") >= 0) return row;
            if (child instanceof Parent nested) {
                HBox found = findSkyFilterRow(nested);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int indexOfStyle(HBox row, String style) {
        for (int i = 0; i < row.getChildren().size(); i++) {
            Node child = row.getChildren().get(i);
            if (child.getStyleClass().contains(style)) return i;
            if (child instanceof MenuButton menu && menu.getStyleClass().contains(style)) return i;
        }
        return -1;
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

    private static String normalize(String raw) {
        String text = raw == null ? "GRB" : raw.trim().toUpperCase(Locale.ROOT);
        return text.startsWith("GRB") ? text : "GRB";
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
