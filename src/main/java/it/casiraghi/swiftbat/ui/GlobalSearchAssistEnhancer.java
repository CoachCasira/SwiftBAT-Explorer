package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Guided behaviour for the global GRB search in the top bar.
 *
 * <p>The GRB prefix is always present, a faint catalogue example/completion is
 * drawn inside the same search box, TAB accepts that completion, and ENTER is
 * the only action that opens a GRB in Explorer. Merely reaching a complete GRB
 * name never navigates automatically.</p>
 */
public final class GlobalSearchAssistEnhancer {
    private static final String INSTALLED = GlobalSearchAssistEnhancer.class.getName() + ".installed";

    private GlobalSearchAssistEnhancer() { }

    public static void install(MainView mainView) {
        if (mainView == null) return;
        TextField search = findGlobalSearch(mainView.getRoot());
        if (search == null || Boolean.TRUE.equals(search.getProperties().get(INSTALLED))) return;
        search.getProperties().put(INSTALLED, Boolean.TRUE);
        new Controller(mainView, search).install();
    }

    private static final class Controller {
        private final MainView mainView;
        private final TextField search;
        private final Label ghost = new Label();
        private StackPane host;
        private boolean internalEdit;
        private String randomHint;
        private String completion;

        private Controller(MainView mainView, TextField search) {
            this.mainView = mainView;
            this.search = search;
        }

        private void install() {
            wrapSearchField();

            ghost.getStyleClass().add("subtle-text");
            ghost.setOpacity(0.42);
            ghost.setMouseTransparent(true);
            ghost.setManaged(true);
            ghost.setAlignment(Pos.CENTER_LEFT);
            StackPane.setAlignment(ghost, Pos.CENTER_LEFT);

            search.setPromptText("");
            setEditor("GRB");
            search.setTooltip(UiFactory.quickTooltip(I18n.dynamic(
                    "Il prefisso GRB è già inserito. TAB completa il suggerimento; INVIO apre il GRB.",
                    "The GRB prefix is already inserted. TAB completes the suggestion; ENTER opens the GRB.")));

            search.textProperty().addListener((obs, oldValue, newValue) -> {
                normalizePrefix(newValue);
                refreshCompletion(false);
            });
            search.focusedProperty().addListener((obs, oldValue, focused) -> {
                if (focused && "GRB".equalsIgnoreCase(safe(search.getText()))) {
                    rotateRandomHint();
                } else {
                    refreshCompletion(false);
                }
            });
            search.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && "GRB".equalsIgnoreCase(safe(search.getText()))) {
                    rotateRandomHint();
                }
            });

            search.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() != KeyCode.TAB) return;
                refreshCompletion(false);
                String target = completion;
                if (target != null && !target.equalsIgnoreCase(safe(search.getText()))) {
                    setEditor(target);
                    refreshCompletion(false);
                    event.consume();
                }
            });

            // ENTER must navigate only for an exact catalogue GRB. A partial
            // string is intentionally ignored even if it has one obvious match.
            search.addEventFilter(ActionEvent.ACTION, event -> {
                if (findExact(search.getText()) != null) return;
                event.consume();
                refreshCompletion(false);
            });

            Label catalogStatus = readLabel(mainView, "catalogStatus");
            if (catalogStatus != null) {
                catalogStatus.textProperty().addListener((obs, oldValue, newValue) -> {
                    if ("GRB".equalsIgnoreCase(safe(search.getText()))) rotateRandomHint();
                    else refreshCompletion(false);
                });
            }

            search.fontProperty().addListener((obs, oldFont, newFont) -> updateGhostPosition());
            search.widthProperty().addListener((obs, oldWidth, newWidth) -> updateGhostPosition());
            Platform.runLater(() -> {
                rotateRandomHint();
                updateGhostPosition();
            });
        }

        private void wrapSearchField() {
            if (!(search.getParent() instanceof HBox parent)) return;
            int index = parent.getChildren().indexOf(search);
            if (index < 0) return;

            double min = search.getMinWidth();
            double pref = search.getPrefWidth();
            double max = search.getMaxWidth();
            parent.getChildren().remove(index);

            host = new StackPane(search, ghost);
            host.setAlignment(Pos.CENTER_LEFT);
            host.setMinWidth(min);
            host.setPrefWidth(pref);
            host.setMaxWidth(max);
            HBox.setHgrow(host, Priority.ALWAYS);

            search.setMinWidth(0);
            search.setPrefWidth(pref);
            search.setMaxWidth(Double.MAX_VALUE);
            parent.getChildren().add(index, host);
        }

        private void normalizePrefix(String value) {
            if (internalEdit) return;
            String raw = safe(value).toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
            String normalized;
            if (raw.isBlank() || "G".equals(raw) || "GR".equals(raw)) {
                normalized = "GRB";
            } else if (raw.startsWith("GRB")) {
                normalized = raw;
            } else {
                normalized = "GRB" + raw.replaceFirst("^GRB", "");
            }
            if (!Objects.equals(value, normalized)) setEditor(normalized);
        }

        private void setEditor(String text) {
            internalEdit = true;
            try {
                search.setText(text);
                search.positionCaret(text.length());
            } finally {
                internalEdit = false;
            }
        }

        private void rotateRandomHint() {
            List<String> names = catalogNames();
            if (names.isEmpty()) {
                randomHint = null;
                completion = null;
                updateGhost();
                return;
            }
            List<String> alternatives = new ArrayList<>();
            for (String name : names) {
                if (!Objects.equals(name, randomHint)) alternatives.add(name);
            }
            List<String> source = alternatives.isEmpty() ? names : alternatives;
            randomHint = source.get(ThreadLocalRandom.current().nextInt(source.size()));
            refreshCompletion(true);
        }

        private void refreshCompletion(boolean keepRandom) {
            String normalized = normalizeName(search.getText());
            final String typed = normalized == null || normalized.isBlank() ? "GRB" : normalized;
            List<String> names = catalogNames();

            if ("GRB".equals(typed)) {
                if (!keepRandom || randomHint == null || !names.contains(randomHint)) {
                    if (!names.isEmpty()) {
                        List<String> alternatives = names.stream()
                                .filter(name -> !Objects.equals(name, randomHint)).toList();
                        List<String> source = alternatives.isEmpty() ? names : alternatives;
                        randomHint = source.get(ThreadLocalRandom.current().nextInt(source.size()));
                    }
                }
                completion = randomHint;
            } else {
                completion = names.stream()
                        .filter(name -> name.startsWith(typed))
                        .findFirst()
                        .orElse(null);
            }
            updateGhost();
        }

        private void updateGhost() {
            String typed = normalizeName(search.getText());
            String target = completion;
            if (typed == null || target == null || target.length() <= typed.length()
                    || !target.startsWith(typed)) {
                ghost.setText("");
                ghost.setVisible(false);
                return;
            }
            ghost.setText(target.substring(typed.length()));
            ghost.setVisible(true);
            updateGhostPosition();
        }

        private void updateGhostPosition() {
            if (!ghost.isVisible()) return;
            Text measure = new Text(safe(search.getText()));
            measure.setFont(search.getFont());
            double textWidth = measure.getLayoutBounds().getWidth();
            double leftInset = search.getInsets() == null ? 0.0 : search.getInsets().getLeft();
            ghost.setPadding(new Insets(0, 0, 0, Math.max(0, leftInset + textWidth + 2)));
        }

        private CatalogEntry findExact(String value) {
            String query = normalizeName(value);
            if (query == null || "GRB".equals(query)) return null;
            for (CatalogEntry entry : catalog()) {
                if (query.equals(normalizeName(entry.grbName()))) return entry;
            }
            return null;
        }

        private List<String> catalogNames() {
            return catalog().stream()
                    .map(CatalogEntry::grbName)
                    .map(GlobalSearchAssistEnhancer::normalizeName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }

        @SuppressWarnings("unchecked")
        private List<CatalogEntry> catalog() {
            Object value = readField(mainView, "currentCatalog");
            return value instanceof List<?> list ? (List<CatalogEntry>) list : List.of();
        }
    }

    private static TextField findGlobalSearch(Node node) {
        if (node instanceof TextField field && field.getStyleClass().contains("global-search-field")) return field;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TextField found = findGlobalSearch(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Label readLabel(Object target, String fieldName) {
        Object value = readField(target, fieldName);
        return value instanceof Label label ? label : null;
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static String normalizeName(String value) {
        if (value == null) return null;
        String raw = value.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (raw.isBlank()) return "GRB";
        return raw.startsWith("GRB") ? raw : "GRB" + raw;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
