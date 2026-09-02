package it.casiraghi.swiftbat.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class UiFactory {
    private UiFactory() {
    }

    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    public static Label wrappedLabel(String text, String... styleClasses) {
        Label label = label(text, styleClasses);
        label.setWrapText(true);
        return label;
    }

    public static Button button(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setCursor(javafx.scene.Cursor.HAND);
        return button;
    }

    public static Button iconButton(String glyph, String tooltip) {
        Button button = button(glyph, "icon-button");
        button.setTooltip(new Tooltip(tooltip));
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
            Label heading = label(title, "card-title");
            card.getChildren().add(heading);
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
