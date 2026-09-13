package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

/**
 * Final responsive pass for the application chrome.
 *
 * <p>The original compact style remains appropriate near the application's
 * minimum width and on 13-inch laptops. At ordinary desktop widths (roughly
 * 1580 px and above) there is already enough room to increase typography and
 * useful control widths, so this layer does not wait for OS fullscreen.</p>
 */
public final class AdaptiveChromeEnhancer {
    private static final double ROOMY_WIDTH = 1450.0;
    private static final double VERY_WIDE_WIDTH = 1800.0;
    private static final String WATCHED = AdaptiveChromeEnhancer.class.getName() + ".watched";
    private static final String SCENE_WATCHED = AdaptiveChromeEnhancer.class.getName() + ".sceneWatched";
    private static final String FLUX_POLISHED = AdaptiveChromeEnhancer.class.getName() + ".fluxPolished";

    private AdaptiveChromeEnhancer() {
    }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root, root);
        root.sceneProperty().addListener((obs, oldScene, newScene) -> installSceneListener(root, newScene));
        installSceneListener(root, root.getScene());
        if (root instanceof Region region) {
            region.widthProperty().addListener((obs, oldValue, newValue) -> requestApply(root));
        }
        Platform.runLater(() -> apply(root));
    }

    private static void installSceneListener(Parent root, Scene scene) {
        if (scene == null || Boolean.TRUE.equals(scene.getProperties().get(SCENE_WATCHED))) return;
        scene.getProperties().put(SCENE_WATCHED, Boolean.TRUE);
        scene.widthProperty().addListener((obs, oldValue, newValue) -> requestApply(root));
    }

    private static void watch(Node node, Parent root) {
        if (node == null) return;
        polishFluxChart(node);

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) if (tab.getContent() != null) watch(tab.getContent(), root);
            String key = WATCHED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(key))) {
                tabs.getProperties().put(key, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (!change.wasAdded()) continue;
                        for (Tab tab : change.getAddedSubList()) {
                            if (tab.getContent() != null) watch(tab.getContent(), root);
                        }
                    }
                    requestApply(root);
                });
            }
        }

        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added, root);
            }
            requestApply(root);
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child, root);
    }

    private static void requestApply(Parent root) {
        Platform.runLater(() -> apply(root));
    }

    private static void apply(Parent root) {
        double width = root instanceof Region region && region.getWidth() > 1
                ? region.getWidth()
                : root.getScene() == null ? 0 : root.getScene().getWidth();
        boolean roomy = width >= ROOMY_WIDTH;
        boolean veryWide = width >= VERY_WIDE_WIDTH;
        toggleStyle(root, "layout-roomy", roomy);
        toggleStyle(root, "layout-very-wide", veryWide);
        applyNavigation(root, roomy, veryWide);
        applyTopBar(root, roomy, veryWide);
    }

    private static void applyNavigation(Parent root, boolean roomy, boolean veryWide) {
        VBox navigation = findVBoxWithStyle(root, "main-navigation");
        if (navigation == null) return;
        if (veryWide) {
            navigation.setMinWidth(266);
            navigation.setPrefWidth(282);
            navigation.setMaxWidth(300);
            navigation.setPadding(new Insets(24, 16, 18, 16));
        } else if (roomy) {
            navigation.setMinWidth(238);
            navigation.setPrefWidth(254);
            navigation.setMaxWidth(270);
            navigation.setPadding(new Insets(21, 14, 16, 14));
        } else {
            navigation.setMinWidth(214);
            navigation.setPrefWidth(228);
            navigation.setMaxWidth(242);
            navigation.setPadding(new Insets(18, 12, 14, 12));
        }
        VBox footer = findVBoxWithStyle(navigation, "nav-footer-card");
        if (footer != null) footer.setPadding(veryWide ? new Insets(15) : roomy ? new Insets(13) : new Insets(11));
    }

    private static void applyTopBar(Parent root, boolean roomy, boolean veryWide) {
        HBox topBar = findHBoxWithStyle(root, "top-bar");
        if (topBar == null) return;
        topBar.setPadding(veryWide
                ? new Insets(9, 18, 9, 18)
                : roomy ? new Insets(8, 15, 8, 15)
                : new Insets(7, 12, 7, 12));

        HBox brand = findHBoxWithStyle(topBar, "top-brand");
        if (brand != null) {
            brand.setMinWidth(veryWide ? 255 : roomy ? 232 : 220);
            brand.setPrefWidth(veryWide ? 282 : roomy ? 250 : 232);
            brand.setMaxWidth(veryWide ? 312 : roomy ? 272 : 242);
        }

        TextField search = findTextFieldWithStyle(topBar, "global-search-field");
        if (search != null) {
            search.setMinWidth(veryWide ? 360 : roomy ? 300 : 245);
            search.setPrefWidth(veryWide ? 720 : roomy ? 610 : 410);
            search.setMaxWidth(veryWide ? 840 : roomy ? 720 : 520);
            HBox.setHgrow(search, Priority.ALWAYS);
        }

        for (Node child : topBar.getChildren()) {
            if (!(child instanceof Button button) || !button.getStyleClass().contains("top-nav-button")) continue;
            double width = topButtonWidth(button, roomy, veryWide);
            button.setMinWidth(width);
            button.setPrefWidth(width);
        }
    }

    private static double topButtonWidth(Button button, boolean roomy, boolean veryWide) {
        String text = normalize(button.getText());
        boolean tools = text.contains("tool") || text.contains("strument");
        boolean guide = text.contains("guide") || text.contains("guida");
        if (veryWide) return tools ? 122 : guide ? 104 : 112;
        if (roomy) return tools ? 108 : guide ? 90 : 100;
        return tools ? 88 : guide ? 68 : 78;
    }

    /**
     * The energy-flux graph had accumulated a 68 px bottom card padding plus a
     * 500/560 px forced chart height. In the nested Explorer/Spectroscopy
     * viewport this made the X labels and exponent cross the clipping boundary.
     * Give the axis its own internal room instead of making the whole card taller.
     */
    private static void polishFluxChart(Node node) {
        if (!(node instanceof BarChart<?, ?> chart)
                || !chart.getStyleClass().contains("spectral-flux-chart")
                || Boolean.TRUE.equals(chart.getProperties().get(FLUX_POLISHED))) return;
        chart.getProperties().put(FLUX_POLISHED, Boolean.TRUE);
        chart.setMinHeight(360);
        chart.setPrefHeight(425);
        chart.setMaxHeight(500);
        if (chart.getParent() instanceof VBox card
                && card.getStyleClass().contains("spectroscopy-chart-card")) {
            card.setPadding(new Insets(12));
            card.setMinHeight(Region.USE_PREF_SIZE);
            card.setPrefHeight(Region.USE_COMPUTED_SIZE);
            card.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(chart, Priority.ALWAYS);
            card.requestLayout();
        }
    }

    private static void toggleStyle(Node node, String styleClass, boolean enabled) {
        if (enabled) {
            if (!node.getStyleClass().contains(styleClass)) node.getStyleClass().add(styleClass);
        } else {
            node.getStyleClass().remove(styleClass);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private static HBox findHBoxWithStyle(Parent root, String styleClass) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof HBox box && box.getStyleClass().contains(styleClass)) return box;
            if (child instanceof Parent parent) {
                HBox found = findHBoxWithStyle(parent, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static VBox findVBoxWithStyle(Parent root, String styleClass) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof VBox box && box.getStyleClass().contains(styleClass)) return box;
            if (child instanceof Parent parent) {
                VBox found = findVBoxWithStyle(parent, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static TextField findTextFieldWithStyle(Parent root, String styleClass) {
        if (root == null) return null;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof TextField field && field.getStyleClass().contains(styleClass)) return field;
            if (child instanceof Parent parent) {
                TextField found = findTextFieldWithStyle(parent, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }
}
