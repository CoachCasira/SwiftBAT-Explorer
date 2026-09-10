package it.casiraghi.swiftbat.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;

/**
 * Rifiniture di interazione che non modificano la logica scientifica:
 * card di lettura sollevabili/focalizzabili, export affidabile della tabella
 * Population e dimensionamento sicuro delle schede di spettroscopia.
 */
public final class InteractionPolishEnhancer {
    private static final String WATCHED = InteractionPolishEnhancer.class.getName() + ".watched";
    private static final String CARD_DONE = InteractionPolishEnhancer.class.getName() + ".cardDone";
    private static final String CARD_ACTIVE = InteractionPolishEnhancer.class.getName() + ".cardActive";
    private static final String CARD_EFFECT = InteractionPolishEnhancer.class.getName() + ".cardEffect";
    private static final String TABLE_WATCHED = InteractionPolishEnhancer.class.getName() + ".tableWatched";
    private static final String TABLE_DONE = InteractionPolishEnhancer.class.getName() + ".tableDone";
    private static final String SPECTRO_TABS_DONE = InteractionPolishEnhancer.class.getName() + ".spectroTabsDone";
    private static final String ACTIVE_OVERLAY = InteractionPolishEnhancer.class.getName() + ".activeOverlay";
    private static final String CHART_ENHANCER_TABLE_DONE = ChartInteractionEnhancer.class.getName() + ".tableDone";

    private InteractionPolishEnhancer() { }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        Platform.runLater(() -> scan(root));
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node added : change.getAddedSubList()) watch(added);
                }
            }
            Platform.runLater(() -> scan(parent));
        });
        for (Node child : parent.getChildrenUnmodifiable()) watch(child);
    }

    private static void scan(Node node) {
        if (node == null) return;
        enhance(node);
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) scan(child);
        }
    }

    private static void enhance(Node node) {
        if (node instanceof Region region) {
            if (isReadingCard(region)) installReadingCard(region);
            keepSpectroscopyContentInside(region);
        }
        if (node instanceof TableView<?> table) installPopulationTableExport(table);
        if (node instanceof TabPane tabs && tabs.getStyleClass().contains("spectroscopy-tabs")) {
            installSpectroscopyTabSizing(tabs);
        }
    }

    private static boolean isReadingCard(Region region) {
        if (region.getStyleClass().contains("info-static-card")) return true;
        if (region.getStyleClass().contains("learning-card")) return true;
        if (region.getStyleClass().contains("explanation-card")) return true;
        return region.getStyleClass().contains("card")
                && hasAncestorStyle(region, "spectroscopy-pane")
                && hasNumberedGuideTitle(region);
    }

    private static boolean hasNumberedGuideTitle(Region region) {
        if (!(region instanceof Parent parent)) return false;
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Label label) {
                String text = label.getText() == null ? "" : label.getText().trim();
                if (text.matches("\\d+\\s*·.*")) return true;
            }
        }
        return false;
    }

    private static void installReadingCard(Region card) {
        if (Boolean.TRUE.equals(card.getProperties().get(CARD_DONE))) return;
        card.getProperties().put(CARD_DONE, Boolean.TRUE);
        card.getProperties().put(CARD_EFFECT, card.getEffect());

        card.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            if (Boolean.TRUE.equals(card.getProperties().get(CARD_ACTIVE))) return;
            card.setEffect(new DropShadow(24, Color.color(0.05, 0.78, 1.0, 0.22)));
            animateCard(card, -4.0, 1.008, 150);
        });
        card.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            if (Boolean.TRUE.equals(card.getProperties().get(CARD_ACTIVE))) return;
            Object previous = card.getProperties().get(CARD_EFFECT);
            card.setEffect(previous instanceof Effect effect ? effect : null);
            animateCard(card, 0.0, 1.0, 150);
        });
        card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1 || event.isConsumed()) return;
            if (Boolean.TRUE.equals(card.getProperties().get(CARD_ACTIVE))) return;
            showCardFocus(card);
            event.consume();
        });
    }

    private static void animateCard(Region card, double translateY, double scale, double millis) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(millis),
                new KeyValue(card.translateYProperty(), translateY, Interpolator.EASE_BOTH),
                new KeyValue(card.scaleXProperty(), scale, Interpolator.EASE_BOTH),
                new KeyValue(card.scaleYProperty(), scale, Interpolator.EASE_BOTH)));
        timeline.play();
    }

    private static void showCardFocus(Region card) {
        StackPane host = findPageHost(card);
        if (host == null || card.getScene() == null) return;
        Object alreadyOpen = host.getProperties().get(ACTIVE_OVERLAY);
        if (alreadyOpen instanceof Node) return;

        card.applyCss();
        Parent parent = card.getParent();
        if (parent != null) parent.layout();
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        WritableImage image = card.snapshot(parameters, null);
        if (image == null || image.getWidth() <= 1 || image.getHeight() <= 1) return;

        Node page = directChildUnder(host, card);
        Effect oldPageEffect = page == null ? null : page.getEffect();
        double oldPageOpacity = page == null ? 1.0 : page.getOpacity();
        if (page != null) {
            page.setEffect(new GaussianBlur(8.0));
            page.setOpacity(Math.min(oldPageOpacity, 0.46));
        }

        card.getProperties().put(CARD_ACTIVE, Boolean.TRUE);
        animateCard(card, 0, 1.0, 90);

        ImageView focused = new ImageView(image);
        focused.setPreserveRatio(true);
        focused.setSmooth(true);
        double hostWidth = host.getWidth() > 300 ? host.getWidth() : card.getScene().getWidth();
        double hostHeight = host.getHeight() > 300 ? host.getHeight() : card.getScene().getHeight();
        focused.setFitWidth(Math.min(image.getWidth() * 1.045, Math.max(460, hostWidth * 0.84)));
        focused.setFitHeight(Math.min(image.getHeight() * 1.22, Math.max(280, hostHeight * 0.72)));
        focused.setEffect(new DropShadow(42, Color.color(0.05, 0.79, 1.0, 0.42)));

        StackPane focusedCard = new StackPane(focused);
        focusedCard.setPickOnBounds(false);
        focusedCard.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane overlay = new StackPane(focusedCard);
        overlay.setAlignment(Pos.CENTER);
        overlay.setPadding(new Insets(38));
        overlay.setStyle("-fx-background-color: rgba(1, 7, 19, 0.72);");
        overlay.setOpacity(0);
        overlay.setFocusTraversable(true);
        host.getChildren().add(overlay);
        host.getProperties().put(ACTIVE_OVERLAY, overlay);

        Runnable close = () -> {
            if (!host.getChildren().contains(overlay)) return;
            host.getChildren().remove(overlay);
            host.getProperties().remove(ACTIVE_OVERLAY);
            if (page != null) {
                page.setEffect(oldPageEffect);
                page.setOpacity(oldPageOpacity);
            }
            card.getProperties().remove(CARD_ACTIVE);
            Object previous = card.getProperties().get(CARD_EFFECT);
            card.setEffect(previous instanceof Effect effect ? effect : null);
            animateCard(card, 0, 1.0, 120);
        };

        overlay.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                close.run();
                event.consume();
            }
        });
        overlay.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close.run();
                event.consume();
            }
        });

        FadeTransition fade = new FadeTransition(Duration.millis(170), overlay);
        fade.setToValue(1.0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(180), focusedCard);
        scale.setFromX(0.965);
        scale.setFromY(0.965);
        scale.setToX(1.0);
        scale.setToY(1.0);
        fade.play();
        scale.play();
        Platform.runLater(overlay::requestFocus);
    }

    private static StackPane findPageHost(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof StackPane stack && current.getStyleClass().contains("page-host")) return stack;
            current = current.getParent();
        }
        return null;
    }

    private static Node directChildUnder(Parent ancestor, Node descendant) {
        Node current = descendant;
        Node previous = descendant;
        while (current != null && current != ancestor) {
            previous = current;
            current = current.getParent();
        }
        return current == ancestor ? previous : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installPopulationTableExport(TableView<?> rawTable) {
        TableView table = rawTable;
        if (!Boolean.TRUE.equals(table.getProperties().get(TABLE_WATCHED))) {
            table.getProperties().put(TABLE_WATCHED, Boolean.TRUE);
            table.getColumns().addListener((ListChangeListener<TableColumn>) change ->
                    Platform.runLater(() -> installPopulationTableExport(table)));
        }
        if (Boolean.TRUE.equals(table.getProperties().get(TABLE_DONE)) || !isPopulationResultTable(table)) return;
        if (!(table.getParent() instanceof VBox box)) return;

        table.getProperties().put(TABLE_DONE, Boolean.TRUE);
        // Evita che il vecchio enhancer aggiunga un secondo pulsante alla stessa tabella.
        table.getProperties().put(CHART_ENHANCER_TABLE_DONE, Boolean.TRUE);

        Button export = UiFactory.button("", "ghost-button");
        export.getStyleClass().add("excel-export-button");
        I18n.setText(export, "Esporta Excel", "Export Excel");
        export.setMinWidth(132);
        export.setVisible(true);
        export.setManaged(true);
        export.setOnAction(event -> ExportSupport.exportTableExcel(
                export, table, "population_included_grbs.xlsx", I18n.t("GRB inclusi")));

        HBox toolbar = new HBox(8, UiFactory.spacer(), export);
        toolbar.getStyleClass().add("data-toolbar");
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setMinHeight(38);
        toolbar.setPrefHeight(38);
        toolbar.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(toolbar, Priority.NEVER);
        box.getChildren().add(0, toolbar);
    }

    private static boolean isPopulationResultTable(TableView<?> table) {
        if (table.getColumns().size() < 5) return false;
        boolean grb = false;
        boolean t90 = false;
        boolean redshift = false;
        boolean coverage = false;
        boolean quality = false;
        for (TableColumn<?, ?> column : table.getColumns()) {
            String text = column.getText() == null ? "" : column.getText().trim().toLowerCase(Locale.ROOT);
            grb |= text.equals("grb");
            t90 |= text.contains("t90");
            redshift |= text.contains("redshift");
            coverage |= text.contains("copertura") || text.contains("coverage");
            quality |= text.contains("qualità") || text.contains("quality") || text.contains("flag");
        }
        return grb && t90 && redshift && coverage && quality;
    }

    private static void installSpectroscopyTabSizing(TabPane tabs) {
        if (Boolean.TRUE.equals(tabs.getProperties().get(SPECTRO_TABS_DONE))) return;
        tabs.getProperties().put(SPECTRO_TABS_DONE, Boolean.TRUE);
        tabs.setMinWidth(0);
        tabs.setMaxWidth(Double.MAX_VALUE);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) ->
                Platform.runLater(() -> fitSpectroscopyTabs(tabs)));
        tabs.widthProperty().addListener((obs, oldWidth, newWidth) ->
                Platform.runLater(() -> fitSpectroscopyTabs(tabs)));
        Platform.runLater(() -> fitSpectroscopyTabs(tabs));
    }

    private static void fitSpectroscopyTabs(TabPane tabs) {
        Tab selected = tabs.getSelectionModel().getSelectedItem();
        Node content = selected == null ? null : selected.getContent();
        double target = 690;
        if (content instanceof Region region) {
            region.setMinWidth(0);
            region.setMaxWidth(Double.MAX_VALUE);
            region.applyCss();
            double width = tabs.getWidth() > 300 ? Math.max(320, tabs.getWidth() - 26) : 1080;
            double preferred = region.prefHeight(width);
            if (Double.isFinite(preferred) && preferred > 0) {
                target = Math.max(690, Math.min(1500, preferred + 86));
            }
        }
        tabs.setMinHeight(target);
        tabs.setPrefHeight(target);
        tabs.setMaxHeight(Double.MAX_VALUE);
        Parent parent = tabs.getParent();
        if (parent != null) parent.requestLayout();
    }

    private static void keepSpectroscopyContentInside(Region region) {
        boolean relevant = region.getStyleClass().contains("spectroscopy-content")
                || region.getStyleClass().contains("spectroscopy-chart-card")
                || region.getStyleClass().contains("time-energy-card")
                || region.getClass().getSimpleName().equals("TimeEnergyHeatmapPane");
        if (!relevant) return;
        region.setMinWidth(0);
        region.setMaxWidth(Double.MAX_VALUE);
        if (region.getStyleClass().contains("time-energy-card")) {
            region.setMinHeight(Region.USE_PREF_SIZE);
        }
    }

    private static boolean hasAncestorStyle(Node node, String styleClass) {
        Node current = node == null ? null : node.getParent();
        while (current != null) {
            if (current.getStyleClass().contains(styleClass)) return true;
            current = current.getParent();
        }
        return false;
    }
}
