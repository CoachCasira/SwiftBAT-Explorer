package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.ChoiceBoxSkin;

import java.util.List;

/** Keeps Explorer compact controls readable without adding layout churn. */
public final class ExplorerChoiceBoxEllipsisFix {
    private static final String INSTALLED = ExplorerChoiceBoxEllipsisFix.class.getName() + ".installed";
    private static final String LABEL_CACHE = ExplorerChoiceBoxEllipsisFix.class.getName() + ".label";
    private static final String SCHEDULED = ExplorerChoiceBoxEllipsisFix.class.getName() + ".scheduled";
    private static final String RADIO_TOOLTIP = ExplorerChoiceBoxEllipsisFix.class.getName() + ".radioTooltip";
    private static final double RIGHT_TEXT_RESERVE = 38.0;
    private static final Insets LABEL_PADDING = new Insets(0, RIGHT_TEXT_RESERVE, 0, 0);

    private ExplorerChoiceBoxEllipsisFix() { }

    public static void install(Node root) {
        if (root == null) return;
        visitLogical(root);
    }

    private static void visitLogical(Node node) {
        if (node == null) return;
        if (node instanceof RadioButton radio && insideSpectroscopy(node)) {
            installRadioTooltip(radio);
            return;
        }
        if (node instanceof ChoiceBox<?> choice && insideOverviewChart(choice)) {
            installChoice(choice);
            return;
        }
        if (node instanceof ScrollPane scroll) {
            if (scroll.getContent() != null) visitLogical(scroll.getContent());
            return;
        }
        if (node instanceof SplitPane split) {
            for (Node item : List.copyOf(split.getItems())) visitLogical(item);
            return;
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : List.copyOf(tabs.getTabs())) visitLogical(tab.getContent());
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) visitLogical(child);
        }
    }

    private static boolean insideOverviewChart(Node node) {
        Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains("overview-chart-card")) return true;
            current = current.getParent();
        }
        return false;
    }

    private static boolean insideSpectroscopy(Node node) {
        Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains("spectroscopy-pane")) return true;
            current = current.getParent();
        }
        return false;
    }

    private static void installRadioTooltip(RadioButton radio) {
        if (radio == null || Boolean.TRUE.equals(radio.getProperties().get(RADIO_TOOLTIP))) return;
        radio.getProperties().put(RADIO_TOOLTIP, Boolean.TRUE);
        // UiFactory computes clipping after the first layout pulse and refreshes
        // the tooltip whenever width, text, font or language changes. Therefore
        // ellipsized fit filters expose their complete localized label even on
        // the first Spectroscopy opening.
        UiFactory.autoTooltip(radio);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installChoice(ChoiceBox<?> choice) {
        if (!Boolean.TRUE.equals(choice.getProperties().get(INSTALLED))) {
            choice.getProperties().put(INSTALLED, Boolean.TRUE);
            try {
                choice.setSkin(new EllipsisChoiceBoxSkin((ChoiceBox) choice));
            } catch (RuntimeException ignored) {
                // A later skin callback will apply the same geometry.
            }

            choice.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                choice.getProperties().remove(LABEL_CACHE);
                schedule(choice);
            });
            choice.widthProperty().addListener((obs, oldWidth, newWidth) -> schedule(choice));
            choice.valueProperty().addListener((obs, oldValue, newValue) -> {
                updateTooltip(choice);
                schedule(choice);
            });
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> {
                updateTooltip(choice);
                schedule(choice);
            });
        }
        updateTooltip(choice);
        schedule(choice);
    }

    private static void schedule(ChoiceBox<?> choice) {
        if (choice == null || Boolean.TRUE.equals(choice.getProperties().get(SCHEDULED))) return;
        choice.getProperties().put(SCHEDULED, Boolean.TRUE);
        Platform.runLater(() -> {
            choice.getProperties().remove(SCHEDULED);
            applySelectedLabel(choice);
        });
    }

    private static void applySelectedLabel(ChoiceBox<?> choice) {
        if (choice == null || choice.getSkin() == null) return;
        try {
            Label label = cachedLabel(choice);
            if (label == null) return;

            if (label.getMinWidth() != 0) label.setMinWidth(0);
            if (label.getMaxWidth() != Double.MAX_VALUE) label.setMaxWidth(Double.MAX_VALUE);
            if (label.isWrapText()) label.setWrapText(false);
            if (label.getTextOverrun() != OverrunStyle.ELLIPSIS) label.setTextOverrun(OverrunStyle.ELLIPSIS);
            if (!"...".equals(label.getEllipsisString())) label.setEllipsisString("...");
            if (!LABEL_PADDING.equals(label.getPadding())) label.setPadding(LABEL_PADDING);
            if (label.getClip() != null) label.setClip(null);
        } catch (RuntimeException ignored) {
            // The ChoiceBox can briefly be between skins during tab replacement.
        }
    }

    private static Label cachedLabel(ChoiceBox<?> choice) {
        Object cached = choice.getProperties().get(LABEL_CACHE);
        if (cached instanceof Label label && label.getScene() == choice.getScene()) return label;
        Node found = choice.lookup(".label");
        if (found instanceof Label label) {
            choice.getProperties().put(LABEL_CACHE, label);
            return label;
        }
        return null;
    }

    private static void updateTooltip(ChoiceBox<?> choice) {
        Object value = choice.getValue();
        String source = value == null ? "" : value.toString();
        if (source.isBlank()) {
            choice.setTooltip(null);
            return;
        }

        // Always derive the tooltip from the original model value and the current
        // application language. Previously an already-created tooltip was replaced
        // with value.toString(), which reintroduced the Italian source text while
        // the ChoiceBox itself was correctly rendered in English.
        String localized = I18n.t(source);
        Tooltip tooltip = choice.getTooltip();
        if (tooltip == null) {
            choice.setTooltip(UiFactory.quickTooltip(source));
        } else if (!localized.equals(tooltip.getText())) {
            tooltip.setText(localized);
        }
    }

    private static final class EllipsisChoiceBoxSkin<T> extends ChoiceBoxSkin<T> {
        private EllipsisChoiceBoxSkin(ChoiceBox<T> control) {
            super(control);
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            super.layoutChildren(x, y, w, h);
            // No applyCss()/requestLayout() here: calling either from layoutChildren
            // creates unnecessary CSS/layout churn while the Explorer is scrolled.
            applySelectedLabel(getSkinnable());
        }
    }
}
