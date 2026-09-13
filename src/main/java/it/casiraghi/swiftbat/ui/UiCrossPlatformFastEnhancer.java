package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Low-overhead cross-platform polish pass.
 *
 * <p>Each node is visited once when it enters the scene graph. Unlike the old
 * pass, a child insertion never schedules a recursive rescan of its whole
 * parent subtree. This matters in Explorer, where JavaFX charts create many
 * internal nodes while their skin is being laid out.</p>
 */
public final class UiCrossPlatformFastEnhancer {
    private static final String WATCHED = UiCrossPlatformFastEnhancer.class.getName() + ".watched";
    private static final String LOCK_DONE = UiCrossPlatformFastEnhancer.class.getName() + ".lockDone";
    private static final String LOCK_TEXT_SYNC = UiCrossPlatformFastEnhancer.class.getName() + ".lockTextSync";
    private static final String SPECTRO_DONE = UiCrossPlatformFastEnhancer.class.getName() + ".spectroDone";
    private static final String OVERVIEW_DONE = UiCrossPlatformFastEnhancer.class.getName() + ".overviewDone";
    private static final String OVERVIEW_RIGHT = UiCrossPlatformFastEnhancer.class.getName() + ".overviewRight";
    private static final Set<Scene> WATCHED_SCENES = Collections.newSetFromMap(new WeakHashMap<>());

    private static final String LOCKED_PATH =
            "M18 8h-1V6a5 5 0 0 0-10 0v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zm-9 0V6a3 3 0 0 1 6 0v2H9zm3 9a2 2 0 1 1 0-4 2 2 0 0 1 0 4z";
    private static final String UNLOCKED_PATH =
            "M18 8h-8V6a3 3 0 0 1 5.83-1H18a5 5 0 0 0-9.9 1v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zm0 12H6V10h12v10zm-6-3a2 2 0 1 0 0-4 2 2 0 0 0 0 4z";

    private UiCrossPlatformFastEnhancer() { }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root);
        observeScene(root);
    }

    private static void observeScene(Parent root) {
        if (root.getScene() != null) watchScene(root.getScene());
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) watchScene(newScene);
        });
    }

    private static void watchScene(Scene scene) {
        if (scene == null || !WATCHED_SCENES.add(scene)) return;
        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (newRoot != null) Platform.runLater(() -> watch(newRoot));
        });
        if (scene.getRoot() != null) watch(scene.getRoot());
    }

    private static void watch(Node node) {
        if (node == null) return;
        enhance(node);
        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added);
            }
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child);
    }

    private static void enhance(Node node) {
        if (node instanceof ToggleButton toggle && toggle.getStyleClass().contains("population-curve-lock")) {
            installVectorLock(toggle);
        }
        if (node instanceof TabPane tabs && tabs.getStyleClass().contains("spectroscopy-tabs")) {
            installSpectroscopyPolish(tabs);
        }
        if (node instanceof VBox box && box.getStyleClass().contains("population-fracexp-inline")) {
            polishFracexp(box);
        }
        if (node instanceof VBox box && box.getStyleClass().contains("population-filter-group")) {
            polishPopulationFilterGroup(box);
        }
        if (node instanceof VBox box && box.getStyleClass().contains("overview-chart-card")) {
            installExplorerOverviewLayout(box);
        }
    }

    private static void installVectorLock(ToggleButton toggle) {
        if (!Boolean.TRUE.equals(toggle.getProperties().get(LOCK_DONE))) {
            toggle.getProperties().put(LOCK_DONE, Boolean.TRUE);
            toggle.selectedProperty().addListener((obs, oldValue, newValue) -> refreshLockGraphic(toggle));
            toggle.textProperty().addListener((obs, oldValue, newValue) -> {
                if (Boolean.TRUE.equals(toggle.getProperties().get(LOCK_TEXT_SYNC))) return;
                if (newValue == null || newValue.isEmpty()) return;
                toggle.getProperties().put(LOCK_TEXT_SYNC, Boolean.TRUE);
                try {
                    toggle.setText("");
                } finally {
                    toggle.getProperties().remove(LOCK_TEXT_SYNC);
                }
            });
        }
        toggle.setText("");
        toggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toggle.setGraphicTextGap(0);
        toggle.setMinWidth(44);
        toggle.setPrefWidth(44);
        toggle.setMaxWidth(44);
        refreshLockGraphic(toggle);
    }

    private static void refreshLockGraphic(ToggleButton toggle) {
        SVGPath icon = toggle.getGraphic() instanceof SVGPath existing ? existing : new SVGPath();
        icon.setContent(toggle.isSelected() ? LOCKED_PATH : UNLOCKED_PATH);
        icon.setScaleX(0.72);
        icon.setScaleY(0.72);
        icon.setMouseTransparent(true);
        icon.setStyle(toggle.isSelected() ? "-fx-fill: #ffffff;" : "-fx-fill: #9fc5e8;");
        toggle.setGraphic(icon);
        toggle.setText("");
    }

    private static void installSpectroscopyPolish(TabPane tabs) {
        if (!Boolean.TRUE.equals(tabs.getProperties().get(SPECTRO_DONE))) {
            tabs.getProperties().put(SPECTRO_DONE, Boolean.TRUE);
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) ->
                    Platform.runLater(() -> polishSpectroscopy(tabs)));
        }
        polishSpectroscopy(tabs);
    }

    private static void polishSpectroscopy(TabPane tabs) {
        boolean english = I18n.language() == I18n.Language.EN;
        for (Tab tab : tabs.getTabs()) {
            tab.setText(fixSpectroscopyText(tab.getText(), english));
            if (tab.getContent() != null) polishSpectroscopyNode(tab.getContent(), english);
        }
    }

    private static void polishSpectroscopyNode(Node node, boolean english) {
        if (node instanceof Labeled labeled && labeled.getText() != null) {
            labeled.setText(fixSpectroscopyText(labeled.getText(), english));
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                polishSpectroscopyNode(child, english);
            }
        }
    }

    private static String fixSpectroscopyText(String text, boolean english) {
        if (text == null || text.isBlank()) return text;
        String result = text;
        if (english) {
            result = replaceAny(result, "Time–energy rate map",
                    "Mappa tempo–energia dei rate", "Mappa time–energy dei rate", "Rate time–energy map");
            result = replaceAny(result, "Open 3D rate view",
                    "Apri vista 3D dei rate", "Open 3D view dei rate");
            result = replaceAny(result, "Time–energy map",
                    "Mappa tempo–energia", "Mappa time–energy");
        } else {
            result = replaceAny(result, "Mappa tempo–energia dei rate",
                    "Time–energy rate map", "Rate time–energy map", "Mappa time–energy dei rate");
            result = replaceAny(result, "Apri vista 3D dei rate",
                    "Open 3D rate view", "Open 3D view dei rate");
            result = replaceAny(result, "Mappa tempo–energia",
                    "Time–energy map", "Mappa time–energy");
        }
        return result;
    }

    private static String replaceAny(String source, String replacement, String... candidates) {
        String result = source;
        for (String candidate : candidates) result = result.replace(candidate, replacement);
        return result;
    }

    private static void polishFracexp(VBox box) {
        box.setMinWidth(250);
        box.setPrefWidth(258);
        box.setMaxWidth(266);
        for (Node child : box.getChildren()) {
            if (!(child instanceof HBox row)) continue;
            for (Node item : row.getChildren()) {
                if (item instanceof VBox control && control.getStyleClass().contains("percentage-control")) {
                    control.setMinWidth(116);
                    control.setPrefWidth(120);
                    control.setMaxWidth(Double.MAX_VALUE);
                }
            }
        }
    }

    private static void polishPopulationFilterGroup(VBox box) {
        String caption = firstLabelText(box).toLowerCase(Locale.ROOT);
        if (caption.contains("finestra temporale") || caption.contains("time window")) {
            resizeFilterGroup(box, 228);
            Node control = box.getChildren().size() > 1 ? box.getChildren().get(1) : null;
            if (control instanceof HBox radios) {
                radios.setSpacing(4);
                for (Node child : radios.getChildren()) {
                    if (child instanceof RadioButton radio) {
                        radio.setMinWidth(70);
                        radio.setPrefWidth(72);
                        radio.setMaxWidth(74);
                        radio.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
                    }
                }
            }
        } else if (caption.equals("redshift")) {
            resizeFilterGroup(box, 175);
        }
    }

    private static void resizeFilterGroup(VBox box, double width) {
        box.setMinWidth(width);
        box.setPrefWidth(width);
        box.setMaxWidth(width);
        if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof Region control) {
            control.setMinWidth(width);
            control.setPrefWidth(width);
            control.setMaxWidth(width);
        }
    }

    private static String firstLabelText(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Label label && label.getText() != null && !label.getText().isBlank()) {
                return label.getText().trim();
            }
        }
        return "";
    }

    /** Keep the original Explorer composition: chart left, compact cards right. */
    private static void installExplorerOverviewLayout(VBox chartCard) {
        if (ExplorerOverviewPane.owns(chartCard)) return;
        if (!(chartCard.getParent() instanceof BorderPane pane)) return;
        Node sidebar = pane.getRight();
        if (sidebar == null) {
            Object stored = pane.getProperties().get(OVERVIEW_RIGHT);
            if (stored instanceof Node node) sidebar = node;
        }
        if (sidebar == null) return;
        pane.getProperties().put(OVERVIEW_RIGHT, sidebar);

        if (!Boolean.TRUE.equals(pane.getProperties().get(OVERVIEW_DONE))) {
            pane.getProperties().put(OVERVIEW_DONE, Boolean.TRUE);
            pane.widthProperty().addListener((obs, oldWidth, newWidth) -> applyExplorerOverviewLayout(pane, chartCard));
        }
        applyExplorerOverviewLayout(pane, chartCard);
    }

    private static void applyExplorerOverviewLayout(BorderPane pane, VBox chartCard) {
        Object stored = pane.getProperties().get(OVERVIEW_RIGHT);
        if (!(stored instanceof Node sidebar)) return;

        // Never stack the cards below the graph. The former delayed switch to
        // BorderPane.bottom was the source of the visible layout jump on macOS.
        if (pane.getBottom() == sidebar) pane.setBottom(null);
        if (pane.getRight() != sidebar) pane.setRight(sidebar);
        BorderPane.setMargin(sidebar, new Insets(0, 0, 0, 10));

        chartCard.setMinWidth(0);
        chartCard.setMaxWidth(Double.MAX_VALUE);

        double available = pane.getWidth();
        double width = available > 0 && available < 1120 ? 238
                : available > 0 && available < 1360 ? 252 : 276;
        compactSidebar(sidebar, width);
        pane.requestLayout();
    }

    private static void compactSidebar(Node sidebar, double width) {
        if (sidebar instanceof Region region) {
            region.setMinWidth(width);
            region.setPrefWidth(width);
            region.setMaxWidth(width);
        }
        if (!(sidebar instanceof VBox box)) return;
        box.setFillWidth(true);
        box.setSpacing(9);
        for (Node child : box.getChildren()) {
            if (child instanceof Region card) {
                card.setMinWidth(0);
                card.setPrefWidth(width);
                card.setMaxWidth(width);
            }
            compactInfoRows(child);
            compactActionButtons(child);
        }
    }

    private static void compactInfoRows(Node node) {
        if (node instanceof HBox row && row.getChildren().size() >= 2
                && row.getChildren().get(0) instanceof Label key
                && key.getStyleClass().contains("info-key")) {
            row.setSpacing(8);
            key.setMinWidth(88);
            key.setPrefWidth(94);
            key.setMaxWidth(100);
            Node valueNode = row.getChildren().get(1);
            if (valueNode instanceof Label value) {
                value.setMinWidth(0);
                value.setMaxWidth(Double.MAX_VALUE);
                value.setWrapText(true);
                HBox.setHgrow(value, Priority.ALWAYS);
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) compactInfoRows(child);
        }
    }

    private static void compactActionButtons(Node node) {
        if (node instanceof HBox row) {
            List<Button> buttons = row.getChildren().stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .toList();
            if (buttons.size() >= 2) {
                row.setSpacing(6);
                row.setAlignment(Pos.CENTER_RIGHT);
                for (Button button : buttons) {
                    button.setMinWidth(82);
                    button.setPrefWidth(96);
                    button.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(button, Priority.ALWAYS);
                }
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) compactActionButtons(child);
        }
    }
}
