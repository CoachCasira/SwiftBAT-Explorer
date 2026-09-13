package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * GRB search field with a protected prefix and Compare-style ghost completion.
 *
 * <p>The original {@link TextField} remains the authoritative control: page
 * listeners continue to receive ordinary text changes, while this class only
 * draws the suggestion and handles TAB/ENTER completion.</p>
 */
public final class GrbSearchAssist {
    static final String PREFIX = "GRB";

    private final TextField field;
    private final Supplier<? extends Collection<String>> namesSupplier;
    private final Consumer<String> commitHandler;
    private final Label ghostPrefix = new Label();
    private final Label ghostSuffix = new Label();
    private final HBox ghost = new HBox(0, ghostPrefix, ghostSuffix);
    private final StackPane root;
    private List<String> candidateNames = List.of();
    private String suggestion = "";

    public GrbSearchAssist(TextField field,
                           Supplier<? extends Collection<String>> namesSupplier,
                           Consumer<String> commitHandler) {
        this.field = Objects.requireNonNull(field, "field");
        this.namesSupplier = namesSupplier == null ? List::of : namesSupplier;
        this.commitHandler = commitHandler == null ? ignored -> { } : commitHandler;

        configureField();
        configureGhost();
        root = new StackPane(field, ghost);
        root.getStyleClass().add("grb-search-assist");
        root.setMinWidth(0);
        root.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(field, Pos.CENTER_LEFT);
        StackPane.setAlignment(ghost, Pos.CENTER_LEFT);
        wireInteraction();
        reset();
    }

    public StackPane node() {
        return root;
    }

    public void refresh() {
        candidateNames = matchingNames(namesSupplier.get(), PREFIX);
        updateSuggestion(field.isFocused() && PREFIX.equals(normalize(field.getText())));
    }

    public void reset() {
        field.setText(PREFIX);
        field.positionCaret(PREFIX.length());
        suggestion = "";
        updateSuggestion(field.isFocused());
    }

    private void configureField() {
        field.setPromptText("");
        field.setTextFormatter(new TextFormatter<String>(change -> {
            String next = change.getControlNewText() == null
                    ? "" : change.getControlNewText().toUpperCase(Locale.ROOT);
            if (!next.startsWith(PREFIX)) return null;
            if (!next.substring(PREFIX.length()).matches("[0-9A-Z]*")) return null;
            if (change.getText() != null) {
                change.setText(change.getText().toUpperCase(Locale.ROOT));
            }
            return change;
        }));
    }

    private void configureGhost() {
        ghostPrefix.getStyleClass().add("compare-ghost-text");
        ghostSuffix.getStyleClass().add("compare-ghost-text");
        ghostPrefix.setOpacity(0.0);
        ghostPrefix.fontProperty().bind(field.fontProperty());
        ghostSuffix.fontProperty().bind(field.fontProperty());
        ghost.setAlignment(Pos.CENTER_LEFT);
        ghost.setPadding(new Insets(0, 14, 0, 13));
        ghost.setMouseTransparent(true);
        ghost.setVisible(false);
        ghost.setManaged(false);
    }

    private void wireInteraction() {
        field.focusedProperty().addListener((obs, oldFocused, focused) -> {
            if (focused) {
                Platform.runLater(() -> field.positionCaret(Math.max(PREFIX.length(), field.getCaretPosition())));
                updateSuggestion(PREFIX.equals(normalize(field.getText())));
            } else {
                showGhost(false);
            }
        });
        field.textProperty().addListener((obs, oldText, newText) -> updateSuggestion(false));
        field.setOnMouseClicked(event -> {
            if (field.getCaretPosition() < PREFIX.length()) field.positionCaret(PREFIX.length());
            if (PREFIX.equals(normalize(field.getText()))) updateSuggestion(true);
        });
        field.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.HOME
                    || (event.getCode() == KeyCode.LEFT
                    && field.getCaretPosition() <= PREFIX.length()
                    && field.getSelection().getLength() == 0)) {
                field.positionCaret(PREFIX.length());
                event.consume();
                return;
            }
            if (event.getCode() == KeyCode.TAB && commitSuggestion()) {
                event.consume();
                return;
            }
            if (event.getCode() == KeyCode.ENTER) {
                String exact = normalize(field.getText());
                if (candidateNames.contains(exact)) {
                    commitHandler.accept(exact);
                    event.consume();
                } else if (commitSuggestion()) {
                    event.consume();
                }
            }
        });
    }

    private boolean commitSuggestion() {
        if (suggestion.isBlank()) return false;
        String value = suggestion;
        field.setText(value);
        field.positionCaret(value.length());
        suggestion = "";
        showGhost(false);
        commitHandler.accept(value);
        return true;
    }

    private void updateSuggestion(boolean randomWhenBarePrefix) {
        String typed = normalize(field.getText());
        List<String> matches = matchingNames(candidateNames, typed).stream()
                .filter(name -> name.length() > typed.length())
                .toList();

        if (matches.isEmpty()) {
            suggestion = "";
        } else if (randomWhenBarePrefix && PREFIX.equals(typed)) {
            suggestion = matches.get(ThreadLocalRandom.current().nextInt(matches.size()));
        } else if (!suggestion.startsWith(typed) || suggestion.length() <= typed.length()) {
            suggestion = matches.get(0);
        }

        ghostPrefix.setText(typed);
        ghostSuffix.setText(suggestion.isBlank() ? "" : suggestion.substring(typed.length()));
        showGhost(field.isFocused() && !suggestion.isBlank());
    }

    private void showGhost(boolean visible) {
        ghost.setVisible(visible);
        ghost.setManaged(visible);
    }

    static String normalize(String raw) {
        String text = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (text.isBlank()) return PREFIX;
        return text.startsWith(PREFIX) ? text : PREFIX + text;
    }

    static List<String> matchingNames(Collection<String> names, String rawPrefix) {
        String prefix = normalize(rawPrefix);
        if (names == null || names.isEmpty()) return List.of();
        return names.stream()
                .filter(Objects::nonNull)
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .filter(name -> name.startsWith(PREFIX))
                .filter(name -> name.startsWith(prefix))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }
}
