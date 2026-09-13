package it.casiraghi.swiftbat.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.List;

/** Owns overview geometry from the first layout, including its reversible info drawer. */
final class ExplorerOverviewPane extends BorderPane {
    private final VBox chartCard;
    private final VBox sidebar;
    private final VBox actionCard;
    private final ResponsiveRow controls;
    private final ResponsiveRow actions;
    private final List<Button> buttons;
    private final Button toggle;
    private final DoubleProperty information = new SimpleDoubleProperty(1);
    private final Rectangle sidebarClip = new Rectangle();
    private Timeline transition;
    private boolean hidden;

    static boolean owns(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current instanceof ExplorerOverviewPane || current.getStyleClass().contains("responsive-overview-chart")) return true;
        }
        return false;
    }

    ExplorerOverviewPane(VBox chartCard, ResponsiveRow controls, VBox sidebar, VBox actionCard,
                         ResponsiveRow actions, Button export, Button toggle, Button fullscreen) {
        this.chartCard = chartCard; this.controls = controls; this.sidebar = sidebar;
        this.actionCard = actionCard; this.actions = actions; this.toggle = toggle;
        buttons = List.of(export, toggle, fullscreen);
        getStyleClass().add("responsive-overview");
        setPadding(new Insets(8));
        setMinWidth(0); setMinHeight(USE_PREF_SIZE);
        setCenter(chartCard); setRight(sidebar);
        sidebar.setClip(sidebarClip);
        information.addListener((obs, before, after) -> { sidebar.setOpacity(after.doubleValue()); requestLayout(); });
        sceneProperty().addListener((obs, before, after) -> {
            if (after == null && transition != null) { transition.stop(); information.set(hidden ? 0 : 1); }
        });
        toggle.setOnAction(event -> toggleInformation());
        updateCaption();
        I18n.languageProperty().addListener((obs, before, after) -> updateCaption());
    }

    private void updateCaption() {
        I18n.setText(toggle, hidden ? "Mostra informazioni" : "Nascondi informazioni",
                hidden ? "Show Information" : "Hide Information");
    }

    private void toggleInformation() {
        hidden = !hidden;
        if (transition != null) transition.stop();
        if (hidden) {
            actions.getChildren().removeAll(buttons);
            actionCard.setManaged(false); actionCard.setVisible(false);
            controls.getChildren().addAll(0, buttons);
        } else {
            controls.getChildren().removeAll(buttons);
            actions.getChildren().setAll(buttons);
            actionCard.setManaged(true); actionCard.setVisible(true);
        }
        updateCaption();
        sidebar.setVisible(true);
        sidebar.setMouseTransparent(hidden);
        transition = new Timeline(new KeyFrame(Duration.millis(230),
                new KeyValue(information, hidden ? 0 : 1, Interpolator.EASE_BOTH)));
        transition.setOnFinished(event -> sidebar.setVisible(!hidden));
        transition.play();
        requestLayout();
    }

    private double chartHeight() {
        return Math.max(400, Math.min(850, getScene() == null ? 480 : getScene().getHeight() - 360));
    }
    private double sidebarWidth(double width) { return Math.min(480, Math.max(320, width * 0.28)); }
    private boolean stacked(double width) { return width < 900; }

    @Override protected double computePrefWidth(double height) { return 1100; }
    @Override protected double computeMinWidth(double height) { return 0; }
    @Override protected double computePrefHeight(double width) {
        double w = Math.max(1, (width < 0 ? 1100 : width) - 16);
        double fraction = information.get();
        double chartWidth = stacked(w) ? w : Math.max(1, w - (sidebarWidth(w) + 12) * fraction);
        double chart = chartHeight() + controls.prefHeight(Math.max(1, chartWidth - 24)) + 33;
        double side = sidebar.prefHeight(stacked(w) ? w : sidebarWidth(w));
        return 16 + (stacked(w) ? chart + (side + 12) * fraction : Math.max(chart, side * fraction));
    }

    @Override protected void layoutChildren() {
        double x = snappedLeftInset(), y = snappedTopInset();
        double w = Math.max(1, getWidth() - x - snappedRightInset());
        double h = Math.max(1, getHeight() - y - snappedBottomInset());
        double fraction = information.get();
        if (stacked(w)) {
            double sideHeight = sidebar.prefHeight(w);
            double chart = Math.max(360, h - (sideHeight + 12) * fraction);
            chartCard.resizeRelocate(x, y, w, chart);
            sidebar.resizeRelocate(x, y + chart + 12 * fraction, w, sideHeight);
            sidebarClip.setWidth(w); sidebarClip.setHeight(sideHeight * fraction);
        } else {
            double sideWidth = sidebarWidth(w);
            double chart = Math.max(1, w - (sideWidth + 12) * fraction);
            chartCard.resizeRelocate(x, y, chart, h);
            sidebar.resizeRelocate(x + chart + 12 * fraction, y, sideWidth, h);
            sidebarClip.setWidth(sideWidth * fraction); sidebarClip.setHeight(h);
        }
    }
}
