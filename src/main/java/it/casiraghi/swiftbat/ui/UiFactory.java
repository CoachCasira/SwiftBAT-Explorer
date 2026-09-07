package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

public final class UiFactory {
    private static final String AUTO_TOOLTIP_KEY = UiFactory.class.getName() + ".autoTooltip";

    private UiFactory() {
    }

    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        autoTooltip(label);
        return label;
    }

    public static Label wrappedLabel(String text, String... styleClasses) {
        Label label = label(text, styleClasses);
        label.setWrapText(true);
        Platform.runLater(() -> refreshAutoTooltip(label));
        return label;
    }

    public static Button button(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setCursor(javafx.scene.Cursor.HAND);
        autoTooltip(button);
        return button;
    }

    /** Mostra il testo completo al passaggio del mouse solo se il controllo lo tronca. */
    public static <T extends Labeled> T autoTooltip(T control) {
        if (control == null) return null;
        control.widthProperty().addListener((obs, oldValue, newValue) -> refreshAutoTooltip(control));
        control.textProperty().addListener((obs, oldValue, newValue) -> refreshAutoTooltip(control));
        control.fontProperty().addListener((obs, oldValue, newValue) -> refreshAutoTooltip(control));
        Platform.runLater(() -> refreshAutoTooltip(control));
        return control;
    }

    /** Per i menu a scelta il tooltip mostra sempre il valore selezionato per intero. */
    public static <T> ChoiceBox<T> autoTooltip(ChoiceBox<T> choice) {
        if (choice == null) return null;
        Runnable refresh = () -> {
            T value = choice.getValue();
            choice.setTooltip(value == null || value.toString().isBlank() ? null : quickTooltip(value.toString()));
        };
        choice.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        refresh.run();
        return choice;
    }

    private static void refreshAutoTooltip(Labeled control) {
        if (control == null) return;
        String text = control.getText();
        boolean autoTooltip = Boolean.TRUE.equals(control.getProperties().get(AUTO_TOOLTIP_KEY));

        if (text == null || text.isBlank() || control.isWrapText()) {
            if (autoTooltip) {
                control.setTooltip(null);
                control.getProperties().remove(AUTO_TOOLTIP_KEY);
            }
            return;
        }

        double available = control.getWidth()
                - control.getInsets().getLeft() - control.getInsets().getRight() - 10.0;
        if (available <= 0) return;

        Text probe = new Text(text);
        probe.setFont(control.getFont());
        boolean clipped = probe.getLayoutBounds().getWidth() > available;
        if (clipped) {
            if (control.getTooltip() == null || autoTooltip) {
                control.setTooltip(quickTooltip(text));
                control.getProperties().put(AUTO_TOOLTIP_KEY, Boolean.TRUE);
            }
        } else if (autoTooltip) {
            control.setTooltip(null);
            control.getProperties().remove(AUTO_TOOLTIP_KEY);
        }
    }

    public static Tooltip quickTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(500));
        tooltip.setHideDelay(Duration.millis(80));
        tooltip.setShowDuration(Duration.seconds(30));
        return tooltip;
    }

    public static Button iconButton(String glyph, String tooltip) {
        Button button = button(glyph, "icon-button");
        button.getProperties().remove(AUTO_TOOLTIP_KEY);
        button.setTooltip(quickTooltip(tooltip));
        return button;
    }

    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    public static VBox card(String title, String subtitle, Node content) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        if (title != null && !title.isBlank()) {
            card.getChildren().add(label(title, "card-title"));
        }
        if (subtitle != null && !subtitle.isBlank()) {
            card.getChildren().add(wrappedLabel(subtitle, "card-subtitle"));
        }
        if (content != null) {
            VBox.setVgrow(content, Priority.ALWAYS);
            card.getChildren().add(content);
        }
        return card;
    }

    public static VBox metricCard(String eyebrow, String value, String detail) {
        VBox card = new VBox(7);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(170);
        Label eyebrowLabel = label(eyebrow, "metric-eyebrow");
        Label valueLabel = label(value == null || value.isBlank() ? "n.d." : value, "metric-value");
        valueLabel.setWrapText(true);
        Label detailLabel = wrappedLabel(detail == null ? "" : detail, "metric-detail");
        card.getChildren().addAll(eyebrowLabel, valueLabel, detailLabel);
        return card;
    }

    public static HBox infoRow(String labelText, String valueText) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        Label label = label(labelText, "info-key");
        label.setMinWidth(165);
        Label value = wrappedLabel(valueText == null || valueText.isBlank() ? "n.d." : valueText, "info-value");
        HBox.setHgrow(value, Priority.ALWAYS);
        row.getChildren().addAll(label, value);
        return row;
    }
}
