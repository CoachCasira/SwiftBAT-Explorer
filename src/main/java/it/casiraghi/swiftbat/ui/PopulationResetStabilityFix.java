package it.casiraghi.swiftbat.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Makes Reset filters a real state reset, not only a reset of input controls.
 * This is especially important after a manually selected GRB analysis.
 */
public final class PopulationResetStabilityFix {
    private static final String INSTALLED = PopulationResetStabilityFix.class.getName() + ".installed";

    private PopulationResetStabilityFix() { }

    public static void install(PopulationPage page, VBox filterCard) {
        if (page == null || filterCard == null) return;
        for (Button button : findButtons(filterCard)) {
            String text = safe(button.getText()).toLowerCase(Locale.ROOT);
            if (!text.contains("ripristina filtri") && !text.contains("reset filters")) continue;
            if (Boolean.TRUE.equals(button.getProperties().get(INSTALLED))) return;
            button.getProperties().put(INSTALLED, Boolean.TRUE);

            button.addEventFilter(ActionEvent.ACTION, event -> {
                cancelRunningTask(page);

                // A new/default analysis must not inherit curve focus, lock or
                // spotlight from the previous manual sample.
                CurveInteractionLinkEnhancer.setPopulationSpotlightName(null);
                CurveInteractionLinkEnhancer.setPopulationFocusLocked(false);
                CurveInteractionLinkEnhancer.setPopulationFocusedNames(Set.of());

                // The normal PopulationPage handler resets all controls and the
                // manual selector clears its chips. After those handlers have run,
                // also clear every result surface so the page really returns to
                // its initial state before the next Analyze group.
                Platform.runLater(() -> settleReset(page));

                // If Reset was pressed while an analysis task was still cancelling,
                // its onCancelled callback may arrive one pulse later. Reassert the
                // initial state after that transition without polling or scanning.
                PauseTransition settle = new PauseTransition(Duration.millis(120));
                settle.setOnFinished(ignored -> settleReset(page));
                settle.play();
            });
            return;
        }
    }

    private static void cancelRunningTask(PopulationPage page) {
        try {
            Field field = PopulationPage.class.getDeclaredField("runningTask");
            field.setAccessible(true);
            Object value = field.get(page);
            if (value instanceof Task<?> task && !task.isDone()) task.cancel(true);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Reset still proceeds even if the internal task representation changes.
        }
    }

    private static void settleReset(PopulationPage page) {
        invoke(page, "clearResults");
        invoke(page, "updateCandidatePreview");
        invoke(page, "updateReadyState");
    }

    private static void invoke(PopulationPage page, String methodName) {
        try {
            Method method = PopulationPage.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(page);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Compatibility layer: keep the page usable if internals change later.
        }
    }

    private static List<Button> findButtons(Node root) {
        java.util.ArrayList<Button> result = new java.util.ArrayList<>();
        collectButtons(root, result);
        return result;
    }

    private static void collectButtons(Node node, List<Button> out) {
        if (node == null) return;
        if (node instanceof Button button) out.add(button);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectButtons(child, out);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
