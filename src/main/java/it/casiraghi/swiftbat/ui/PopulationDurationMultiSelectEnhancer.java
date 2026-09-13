package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.SkyBurst;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Replaces only the visible Population T90 ChoiceBox with a checkbox-based
 * multi-select menu, while keeping the original ChoiceBox as the backing state
 * used by PopulationPage and by the manual-GRB compatibility layer.
 *
 * <p>Single selections continue to use PopulationPage's native filter. For a
 * union of two or more T90 classes, the metadata view is narrowed only for the
 * synchronous candidate-selection phase (and for preview calculation), then
 * restored immediately. This keeps the existing analysis pipeline unchanged
 * and avoids duplicating its loading/FRACEXP/redshift/sky logic.</p>
 */
public final class PopulationDurationMultiSelectEnhancer {
    private static final String INSTALLED = PopulationDurationMultiSelectEnhancer.class.getName() + ".installed";
    private static final String RESET_HOOK = PopulationDurationMultiSelectEnhancer.class.getName() + ".resetHook";

    private static final String ALL_T90 = "Tutte le durate";
    private static final String SHORT_T90 = "Short · T90 ≤ 2 s";
    private static final String LONG_T90 = "Long · T90 > 2 s";
    private static final String UNKNOWN_T90 = "T90 non disponibile";

    private PopulationDurationMultiSelectEnhancer() { }

    public static void install(MainView mainView) {
        if (mainView == null) return;
        Object value = readField(mainView, "populationPage");
        if (value instanceof PopulationPage page) install(page);
    }

    public static void install(PopulationPage page) {
        if (page == null || Boolean.TRUE.equals(page.getProperties().get(INSTALLED))) return;
        ChoiceBox<String> backing = choice(page, "duration");
        if (backing == null || !(backing.getParent() instanceof VBox group)) return;

        page.getProperties().put(INSTALLED, Boolean.TRUE);
        new Controller(page, backing, group).install();
    }

    private static final class Controller {
        private final PopulationPage page;
        private final ChoiceBox<String> backing;
        private final VBox group;
        private final MenuButton menu = new MenuButton();
        private final CheckMenuItem all = new CheckMenuItem();
        private final CheckMenuItem shortItem = new CheckMenuItem();
        private final CheckMenuItem longItem = new CheckMenuItem();
        private final CheckMenuItem unknownItem = new CheckMenuItem();
        private final LinkedHashSet<String> selected = new LinkedHashSet<>();

        private boolean internal;
        private boolean previewScheduled;

        private Controller(PopulationPage page, ChoiceBox<String> backing, VBox group) {
            this.page = page;
            this.backing = backing;
            this.group = group;
        }

        private void install() {
            selected.add(ALL_T90);

            int index = group.getChildren().indexOf(backing);
            if (index < 0) return;
            group.getChildren().remove(backing);

            menu.getStyleClass().addAll("choice-box-modern", "population-duration-multi-select");
            menu.setMinWidth(0);
            menu.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(menu, Priority.NEVER);
            menu.setFocusTraversable(false);
            menu.getItems().setAll(all, new SeparatorMenuItem(), shortItem, longItem, unknownItem);
            group.getChildren().add(index, menu);

            all.setOnAction(event -> selectAll());
            shortItem.setOnAction(event -> toggleSpecific(SHORT_T90, shortItem));
            longItem.setOnAction(event -> toggleSpecific(LONG_T90, longItem));
            unknownItem.setOnAction(event -> toggleSpecific(UNKNOWN_T90, unknownItem));

            relabel();
            syncVisualState();
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> relabel());

            Button analyze = button(page, "analyze");
            if (analyze != null) {
                analyze.addEventFilter(ActionEvent.ACTION, event -> prepareMultiSelectionForAnalysis());
            }
            installResetHook();
            installPreviewGuard();
            schedulePreviewCorrection();
        }

        private void selectAll() {
            if (internal) return;
            internal = true;
            selected.clear();
            selected.add(ALL_T90);
            backing.setValue(ALL_T90);
            internal = false;
            syncVisualState();
            invoke(page, "updateCandidatePreview");
        }

        private void toggleSpecific(String value, CheckMenuItem source) {
            if (internal) return;
            internal = true;
            selected.remove(ALL_T90);
            if (source.isSelected()) selected.add(value);
            else selected.remove(value);

            if (selected.isEmpty()) {
                selected.add(ALL_T90);
                backing.setValue(ALL_T90);
            } else if (selected.size() == 1) {
                backing.setValue(selected.iterator().next());
            } else {
                // PopulationPage's native filter has one duration slot. ALL is
                // its neutral backing value; the exact union is applied around
                // preview/candidate selection without changing the pipeline.
                backing.setValue(ALL_T90);
            }
            internal = false;

            syncVisualState();
            if (isMultipleSpecific()) {
                internal = true;
                try {
                    refreshPreviewForUnion();
                } finally {
                    internal = false;
                }
            } else {
                invoke(page, "updateCandidatePreview");
            }

            // Keep the popup available so a second checkbox can be selected
            // immediately, mirroring the Explorer multi-band selector.
            Platform.runLater(() -> {
                if (menu.getScene() != null && menu.isVisible() && !selected.contains(ALL_T90)) menu.show();
            });
        }

        private void syncVisualState() {
            internal = true;
            try {
                all.setSelected(selected.contains(ALL_T90));
                shortItem.setSelected(selected.contains(SHORT_T90));
                longItem.setSelected(selected.contains(LONG_T90));
                unknownItem.setSelected(selected.contains(UNKNOWN_T90));
                updateMenuText();
            } finally {
                internal = false;
            }
        }

        private void relabel() {
            all.setText(I18n.dynamic("Tutte le durate", "All durations"));
            shortItem.setText("Short · T90 ≤ 2 s");
            longItem.setText("Long · T90 > 2 s");
            unknownItem.setText(I18n.dynamic("T90 non disponibile", "T90 unavailable"));
            updateMenuText();
        }

        private void updateMenuText() {
            String text;
            if (selected.contains(ALL_T90) || selected.isEmpty()) {
                text = I18n.dynamic("Tutte le durate", "All durations");
            } else if (selected.size() == 1) {
                text = display(selected.iterator().next());
            } else {
                text = I18n.dynamic(selected.size() + " classi T90", selected.size() + " T90 classes");
            }
            menu.setText(text);

            String detail = selected.contains(ALL_T90)
                    ? I18n.dynamic("Tutte le classi T90 sono incluse", "All T90 classes are included")
                    : selected.stream().map(this::display).reduce((a, b) -> a + "\n" + b).orElse(text);
            menu.setTooltip(UiFactory.quickTooltip(detail));
        }

        private String display(String value) {
            if (SHORT_T90.equals(value)) return "Short · T90 ≤ 2 s";
            if (LONG_T90.equals(value)) return "Long · T90 > 2 s";
            if (UNKNOWN_T90.equals(value)) return I18n.dynamic("T90 non disponibile", "T90 unavailable");
            return I18n.dynamic("Tutte le durate", "All durations");
        }

        private boolean isMultipleSpecific() {
            return !selected.contains(ALL_T90) && selected.size() >= 2;
        }

        private boolean accepts(SkyBurst burst) {
            if (burst == null || selected.contains(ALL_T90)) return true;
            if (selected.contains(UNKNOWN_T90) && !burst.hasT90()) return true;
            if (selected.contains(SHORT_T90) && burst.isShort()) return true;
            return selected.contains(LONG_T90) && burst.isLong();
        }

        @SuppressWarnings("unchecked")
        private Map<String, SkyBurst> metadata() {
            Object value = readField(page, "metadata");
            return value instanceof Map<?, ?> map ? (Map<String, SkyBurst>) map : Map.of();
        }

        private void refreshPreviewForUnion() {
            Map<String, SkyBurst> target = metadata();
            if (target.isEmpty()) {
                invoke(page, "updateCandidatePreview");
                return;
            }
            LinkedHashMap<String, SkyBurst> original = new LinkedHashMap<>(target);
            try {
                target.clear();
                original.forEach((name, burst) -> {
                    if (accepts(burst)) target.put(name, burst);
                });
                String old = backing.getValue();
                backing.setValue(ALL_T90);
                invoke(page, "updateCandidatePreview");
                backing.setValue(old);
            } finally {
                target.clear();
                target.putAll(original);
            }
        }

        private void prepareMultiSelectionForAnalysis() {
            if (!isMultipleSpecific() || hasManualSelection()) return;
            Map<String, SkyBurst> target = metadata();
            if (target.isEmpty()) return;

            LinkedHashMap<String, SkyBurst> original = new LinkedHashMap<>(target);
            target.clear();
            original.forEach((name, burst) -> {
                if (accepts(burst)) target.put(name, burst);
            });
            backing.setValue(ALL_T90);

            // PopulationPage builds the Candidate list synchronously inside the
            // same ActionEvent. Restore shared metadata on the next JavaFX pulse.
            Platform.runLater(() -> {
                target.clear();
                target.putAll(original);
                schedulePreviewCorrection();
            });
        }

        private boolean hasManualSelection() {
            for (Button button : findAll(page, Button.class)) {
                if (button.getStyleClass().contains("population-grb-chip")) return true;
            }
            return false;
        }

        private void installResetHook() {
            for (Button button : findAll(page, Button.class)) {
                String text = safe(button.getText()).toLowerCase(Locale.ROOT);
                if (!text.contains("ripristina filtri") && !text.contains("reset filters")) continue;
                if (Boolean.TRUE.equals(button.getProperties().get(RESET_HOOK))) continue;
                button.getProperties().put(RESET_HOOK, Boolean.TRUE);
                button.addEventFilter(ActionEvent.ACTION, event -> {
                    selected.clear();
                    selected.add(ALL_T90);
                    backing.setValue(ALL_T90);
                    syncVisualState();
                });
            }
        }

        private void installPreviewGuard() {
            Label preview = label(page, "candidatePreview");
            if (preview == null) return;
            preview.textProperty().addListener((obs, oldText, newText) -> {
                if (!internal && isMultipleSpecific()) schedulePreviewCorrection();
            });
        }

        private void schedulePreviewCorrection() {
            if (!isMultipleSpecific() || previewScheduled) return;
            previewScheduled = true;
            Platform.runLater(() -> {
                previewScheduled = false;
                if (!isMultipleSpecific()) return;
                internal = true;
                try {
                    refreshPreviewForUnion();
                } finally {
                    internal = false;
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static ChoiceBox<String> choice(Object target, String name) {
        Object value = readField(target, name);
        return value instanceof ChoiceBox<?> choice ? (ChoiceBox<String>) choice : null;
    }

    private static Button button(Object target, String name) {
        Object value = readField(target, name);
        return value instanceof Button button ? button : null;
    }

    private static Label label(Object target, String name) {
        Object value = readField(target, name);
        return value instanceof Label label ? label : null;
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null || fieldName == null) return null;
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

    private static void invoke(Object target, String methodName) {
        if (target == null) return;
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Compatibility enhancer: preview failures must never block analysis.
        }
    }

    private static <T extends Node> List<T> findAll(Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collect(root, type, result);
        return result;
    }

    private static <T extends Node> void collect(Node node, Class<T> type, List<T> out) {
        if (node == null) return;
        if (type.isInstance(node)) out.add(type.cast(node));

        // Traverse logical content rather than skin internals. This works before
        // the first scene/layout pulse and keeps the enhancer lightweight.
        if (node instanceof ScrollPane scroll) {
            collect(scroll.getContent(), type, out);
            return;
        }
        if (node instanceof SplitPane split) {
            for (Node item : List.copyOf(split.getItems())) collect(item, type, out);
            return;
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : List.copyOf(tabs.getTabs())) collect(tab.getContent(), type, out);
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) collect(child, type, out);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
