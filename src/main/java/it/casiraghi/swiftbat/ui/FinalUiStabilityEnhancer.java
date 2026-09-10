package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.SkyBurst;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Final low-cost UI pass for real-use stability issues.
 *
 * <p>Important layout rule: this class never listens to min/pref/max height
 * properties and then writes those same properties back. Population histogram
 * geometry is applied once per relevant UI event, after the older responsive
 * layer has run. This avoids resize feedback loops and visible oscillation.</p>
 */
public final class FinalUiStabilityEnhancer {
    private static final String WATCHED = FinalUiStabilityEnhancer.class.getName() + ".watched";
    private static final String LABEL_WATCHED = FinalUiStabilityEnhancer.class.getName() + ".labelWatched";
    private static final String TABLE_DONE = FinalUiStabilityEnhancer.class.getName() + ".tableDone";
    private static final String CATALOG_DONE = FinalUiStabilityEnhancer.class.getName() + ".catalogDone";
    private static final String HISTOGRAM_DONE = FinalUiStabilityEnhancer.class.getName() + ".histogramDone";
    private static final String SIDE_DONE = FinalUiStabilityEnhancer.class.getName() + ".sideDone";
    private static final String FILTER_DONE = FinalUiStabilityEnhancer.class.getName() + ".filterDone";
    private static final String POPULATION_TABS_DONE = FinalUiStabilityEnhancer.class.getName() + ".populationTabsDone";
    private static final String POPULATION_STABILITY_PENDING = FinalUiStabilityEnhancer.class.getName() + ".populationStabilityPending";
    private static final String REPAIRING = FinalUiStabilityEnhancer.class.getName() + ".repairing";

    private static final String MISSING = "[Missing English translation]";
    private static final String UI_FACTORY_ORIGINAL = "swiftbat.originalText";
    private static final String UI_TRANSLATIONS_ORIGINAL = UiTranslations.class.getName() + ".originalText";
    private static final String I18N_LOCALIZED_IT = I18n.class.getName() + ".localized.it";
    private static final String RESPONSIVE_POPULATION_REFRESH =
            ResponsiveLayoutEnhancer.class.getName() + ".populationRefresh";

    private static final Map<String, String> REQUIRED_ENGLISH = Map.ofEntries(
            Map.entry("Mappa tempo–energia", "Time–energy map"),
            Map.entry("Mappa tempo-energia", "Time-energy map"),
            Map.entry("Mappa tempo–energia dei rate", "Time–energy rate map"),
            Map.entry("Mappa tempo-energia dei rate", "Time-energy rate map"),
            Map.entry("Apri vista 3D dei rate", "Open 3D rate view"),
            Map.entry("Commento originale", "Original comment"),
            Map.entry("Commento FITS", "FITS comment")
    );

    private FinalUiStabilityEnhancer() {
    }

    public static void install(Parent root) {
        if (root == null) return;
        watch(root, root);
        installScenePolish(root);
        I18n.languageProperty().addListener((obs, oldValue, newValue) ->
                Platform.runLater(() -> repairTree(root)));
        Platform.runLater(() -> {
            repairTree(root);
            stopDelayedPopulationRelayout(root);
            stabilizePopulationHistograms(root);
            polishFullscreenReading(root.getScene() == null ? null : root.getScene().getRoot());
        });
    }

    private static void installScenePolish(Parent appRoot) {
        Scene scene = appRoot.getScene();
        if (scene == null) {
            appRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) installSceneRootListener(newScene);
            });
        } else {
            installSceneRootListener(scene);
        }
    }

    private static void installSceneRootListener(Scene scene) {
        String key = FinalUiStabilityEnhancer.class.getName() + ".sceneRootListener";
        if (Boolean.TRUE.equals(scene.getProperties().get(key))) return;
        scene.getProperties().put(key, Boolean.TRUE);
        scene.rootProperty().addListener((obs, oldRoot, newRoot) ->
                Platform.runLater(() -> polishFullscreenReading(newRoot)));
    }

    private static void watch(Node node, Parent appRoot) {
        if (node == null) return;
        installMissingTranslationGuard(node);

        if (node instanceof TableView<?> table) installTablePolish(table);
        if (node instanceof ListView<?> list && list.getStyleClass().contains("catalog-list")) {
            installStableCatalogCells(list);
        }
        if (node instanceof VBox box && box.getStyleClass().contains("population-histogram-card")) {
            installHistogramStability(box, appRoot);
        }
        if (node instanceof Region region && region.getStyleClass().contains("overview-action-card")) {
            polishExplorerSide(region);
        }
        if (node instanceof Region region && region.getStyleClass().contains("population-filter-card")) {
            installPopulationRelayoutGuard(region, appRoot);
        }
        if (node instanceof Button button && button.getStyleClass().contains("population-filter-restore")) {
            installPopulationRestoreGuard(button, appRoot);
        }
        if (node instanceof TabPane tabs && hasAncestorNamed(tabs, "PopulationPage")) {
            installPopulationTabGuard(tabs, appRoot);
        }

        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) watch(tab.getContent(), appRoot);
            }
            String tabKey = WATCHED + ".tabs";
            if (!Boolean.TRUE.equals(tabs.getProperties().get(tabKey))) {
                tabs.getProperties().put(tabKey, Boolean.TRUE);
                tabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
                    while (change.next()) {
                        if (!change.wasAdded()) continue;
                        for (Tab tab : change.getAddedSubList()) {
                            if (tab.getContent() != null) watch(tab.getContent(), appRoot);
                        }
                    }
                });
            }
        }

        if (!(node instanceof Parent parent)) return;
        if (Boolean.TRUE.equals(parent.getProperties().get(WATCHED))) return;
        parent.getProperties().put(WATCHED, Boolean.TRUE);
        parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) watch(added, appRoot);
            }
            Platform.runLater(() -> polishNearby(parent));
        });
        for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) watch(child, appRoot);
    }

    /* ---------------- Translation placeholder safety ---------------- */

    private static void installMissingTranslationGuard(Node node) {
        if (!(node instanceof Labeled labeled)) return;
        repairMissingLabel(labeled);
        if (Boolean.TRUE.equals(labeled.getProperties().get(LABEL_WATCHED))) return;
        labeled.getProperties().put(LABEL_WATCHED, Boolean.TRUE);
        labeled.textProperty().addListener((obs, oldText, newText) -> {
            if (MISSING.equals(newText)) repairMissingLabel(labeled);
        });
    }

    private static void repairMissingLabel(Labeled labeled) {
        if (labeled == null || !MISSING.equals(labeled.getText())
                || Boolean.TRUE.equals(labeled.getProperties().get(REPAIRING))) return;
        String source = sourceText(labeled);
        if (source == null || source.isBlank() || MISSING.equals(source)) return;
        String replacement = REQUIRED_ENGLISH.get(source);
        if (replacement == null) replacement = UiTranslations.t(source);
        if (replacement == null || replacement.isBlank() || MISSING.equals(replacement)) replacement = source;
        labeled.getProperties().put(REPAIRING, Boolean.TRUE);
        try {
            labeled.setText(replacement);
        } finally {
            labeled.getProperties().remove(REPAIRING);
        }
    }

    private static String sourceText(Labeled labeled) {
        for (String key : List.of(UI_FACTORY_ORIGINAL, UI_TRANSLATIONS_ORIGINAL, I18N_LOCALIZED_IT)) {
            Object value = labeled.getProperties().get(key);
            if (value instanceof String text && !text.isBlank() && !MISSING.equals(text)) return text;
        }
        return null;
    }

    private static void repairTree(Node node) {
        if (node == null) return;
        if (node instanceof Labeled labeled) repairMissingLabel(labeled);
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (MISSING.equals(tab.getText())) {
                    Object source = tab.getProperties().get(UI_TRANSLATIONS_ORIGINAL);
                    if (source instanceof String text && !text.isBlank()) {
                        String replacement = REQUIRED_ENGLISH.getOrDefault(text, UiTranslations.t(text));
                        tab.setText(MISSING.equals(replacement) ? text : replacement);
                    }
                }
                if (tab.getContent() != null) repairTree(tab.getContent());
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) repairTree(child);
        }
    }

    /* ---------------- All table scrollbars ---------------- */

    private static void installTablePolish(TableView<?> table) {
        if (Boolean.TRUE.equals(table.getProperties().get(TABLE_DONE))) return;
        table.getProperties().put(TABLE_DONE, Boolean.TRUE);
        if (!table.getStyleClass().contains("stable-table-scroll")) {
            table.getStyleClass().add("stable-table-scroll");
        }
        table.skinProperty().addListener((obs, oldSkin, newSkin) ->
                Platform.runLater(() -> polishTableBars(table)));
        table.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) Platform.runLater(() -> polishTableBars(table));
        });
        Platform.runLater(() -> polishTableBars(table));
    }

    private static void polishTableBars(TableView<?> table) {
        if (table.getSkin() == null) return;
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (!(node instanceof ScrollBar bar) || bar.getOrientation() != Orientation.VERTICAL) continue;
            bar.setMinWidth(5.5);
            bar.setPrefWidth(5.5);
            bar.setMaxWidth(5.5);
            bar.setOpacity(0.76);
            bar.toFront();
        }
    }

    /* ---------------- Stable Explorer catalog virtualization ---------------- */

    @SuppressWarnings("unchecked")
    private static void installStableCatalogCells(ListView<?> rawList) {
        if (Boolean.TRUE.equals(rawList.getProperties().get(CATALOG_DONE))) return;
        rawList.getProperties().put(CATALOG_DONE, Boolean.TRUE);
        ListView<CatalogEntry> list = (ListView<CatalogEntry>) (ListView<?>) rawList;
        Parent explorer = ancestorNamed(list, "ExplorerPage");
        Map<String, SkyBurst> metadata = metadata(explorer);
        Predicate<CatalogEntry> cache = cachePredicate(explorer);
        list.setFixedCellSize(68);
        list.setCellFactory(ignored -> new StableCatalogCell(metadata, cache));
    }

    private static final class StableCatalogCell extends ListCell<CatalogEntry> {
        private final Map<String, SkyBurst> metadata;
        private final Predicate<CatalogEntry> cache;
        private final Label star = UiFactory.label("✦", "catalog-star");
        private final Label name = UiFactory.label("", "catalog-name");
        private final Label trigger = UiFactory.label("", "catalog-trigger");
        private final Label science = UiFactory.label("", "catalog-science");
        private final Label cached = UiFactory.label("IN CACHE", "cache-badge");
        private final VBox copy = new VBox(2, name, trigger, science);
        private final HBox row = new HBox(10, star, copy, cached);

        private StableCatalogCell(Map<String, SkyBurst> metadata, Predicate<CatalogEntry> cache) {
            this.metadata = metadata;
            this.cache = cache;
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(copy, Priority.ALWAYS);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(CatalogEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            name.setText(item.grbName());
            trigger.setText("Trigger " + item.triggerId());
            SkyBurst burst = metadata.get(item.grbName().toUpperCase(Locale.ROOT));
            boolean hasScience = burst != null;
            science.setText(hasScience ? burst.formattedT90() + " · " + burst.redshift().displayValue() : "");
            science.setVisible(hasScience);
            science.setManaged(hasScience);
            boolean isCached;
            try {
                isCached = cache.test(item);
            } catch (RuntimeException ignored) {
                isCached = false;
            }
            cached.setVisible(isCached);
            cached.setManaged(isCached);
            setText(null);
            setGraphic(row);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, SkyBurst> metadata(Parent explorer) {
        Object value = fieldValue(explorer, "scientificMetadata");
        if (value instanceof Map<?, ?> map) return (Map<String, SkyBurst>) map;
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static Predicate<CatalogEntry> cachePredicate(Parent explorer) {
        Object value = fieldValue(explorer, "cacheLookup");
        if (value instanceof Predicate<?> predicate) return (Predicate<CatalogEntry>) predicate;
        return ignored -> false;
    }

    /* ---------------- Explorer right-side hierarchy ---------------- */

    private static void polishExplorerSide(Region actionCard) {
        if (Boolean.TRUE.equals(actionCard.getProperties().get(SIDE_DONE))) return;
        actionCard.getProperties().put(SIDE_DONE, Boolean.TRUE);
        Platform.runLater(() -> {
            if (!(actionCard.getParent() instanceof VBox side)) return;
            int index = side.getChildren().indexOf(actionCard);
            if (index > 0) {
                side.getChildren().remove(actionCard);
                side.getChildren().add(0, actionCard);
            }
            side.setFillWidth(true);
            side.setMinWidth(315);
            side.setPrefWidth(326);
            side.setMaxWidth(350);
            for (Node child : side.getChildren()) {
                if (!(child instanceof Region region)) continue;
                region.setMinWidth(0);
                region.setMaxWidth(Double.MAX_VALUE);
                if (child == actionCard) {
                    VBox.setVgrow(child, Priority.NEVER);
                } else {
                    region.setMaxHeight(Double.MAX_VALUE);
                    VBox.setVgrow(child, Priority.ALWAYS);
                }
            }
            side.requestLayout();
        });
    }

    /* ---------------- Population distributions ---------------- */

    private static void installHistogramStability(VBox card, Parent appRoot) {
        if (Boolean.TRUE.equals(card.getProperties().get(HISTOGRAM_DONE))) return;
        card.getProperties().put(HISTOGRAM_DONE, Boolean.TRUE);
        requestPopulationStability(appRoot);
    }

    private static void requestPopulationStability(Parent appRoot) {
        if (appRoot == null || Boolean.TRUE.equals(appRoot.getProperties().get(POPULATION_STABILITY_PENDING))) return;
        appRoot.getProperties().put(POPULATION_STABILITY_PENDING, Boolean.TRUE);
        Platform.runLater(() -> {
            appRoot.getProperties().remove(POPULATION_STABILITY_PENDING);
            stopDelayedPopulationRelayout(appRoot);
            stabilizePopulationHistograms(appRoot);
        });
    }

    private static void stabilizePopulationHistograms(Node node) {
        if (node == null) return;
        if (node instanceof VBox box && box.getStyleClass().contains("population-histogram-card")) {
            stabilizeHistogram(box);
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) stabilizePopulationHistograms(tab.getContent());
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) {
                stabilizePopulationHistograms(child);
            }
        }
    }

    private static void stabilizeHistogram(VBox card) {
        double sceneHeight = card.getScene() == null || card.getScene().getHeight() <= 0
                ? 900.0 : card.getScene().getHeight();
        double cardHeight = Math.max(385.0, Math.min(495.0, sceneHeight * 0.47));
        double chartHeight = Math.max(295.0, Math.min(390.0, cardHeight - 100.0));

        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMinHeight(0);
        card.setPrefHeight(cardHeight);
        card.setMaxHeight(cardHeight);

        BarChart<?, ?> chart = findBarChart(card);
        if (chart != null) {
            chart.setMinWidth(0);
            chart.setMaxWidth(Double.MAX_VALUE);
            chart.setMinHeight(0);
            chart.setPrefHeight(chartHeight);
            chart.setMaxHeight(chartHeight);
            VBox.setVgrow(chart, Priority.NEVER);
        }

        if (card.getParent() instanceof HBox row) {
            row.setFillHeight(false);
            row.setAlignment(Pos.TOP_LEFT);
        }
    }

    private static BarChart<?, ?> findBarChart(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof BarChart<?, ?> chart) return chart;
            if (child instanceof Parent parent) {
                BarChart<?, ?> nested = findBarChart(parent);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    /* ---------------- Population relayout coalescing ---------------- */

    private static void installPopulationRelayoutGuard(Region filterCard, Parent appRoot) {
        if (Boolean.TRUE.equals(filterCard.getProperties().get(FILTER_DONE))) return;
        filterCard.getProperties().put(FILTER_DONE, Boolean.TRUE);
        filterCard.visibleProperty().addListener((obs, oldValue, newValue) -> requestPopulationStability(appRoot));
        filterCard.managedProperty().addListener((obs, oldValue, newValue) -> requestPopulationStability(appRoot));
    }

    private static void installPopulationRestoreGuard(Button restore, Parent appRoot) {
        String key = FILTER_DONE + ".restore";
        if (Boolean.TRUE.equals(restore.getProperties().get(key))) return;
        restore.getProperties().put(key, Boolean.TRUE);
        restore.visibleProperty().addListener((obs, oldValue, newValue) -> requestPopulationStability(appRoot));
        restore.managedProperty().addListener((obs, oldValue, newValue) -> requestPopulationStability(appRoot));
    }

    private static void installPopulationTabGuard(TabPane tabs, Parent appRoot) {
        if (Boolean.TRUE.equals(tabs.getProperties().get(POPULATION_TABS_DONE))) return;
        tabs.getProperties().put(POPULATION_TABS_DONE, Boolean.TRUE);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) ->
                requestPopulationStability(appRoot));
    }

    private static void stopDelayedPopulationRelayout(Parent appRoot) {
        if (appRoot == null) return;
        Object value = appRoot.getProperties().get(RESPONSIVE_POPULATION_REFRESH);
        if (value instanceof PauseTransition pause) pause.stop();
    }

    /* ---------------- Fullscreen reading panels ---------------- */

    private static void polishFullscreenReading(Node node) {
        if (node == null) return;
        if (node instanceof ScrollPane scroll
                && scroll.getContent() instanceof VBox reading
                && reading.getStyleClass().contains("spectroscopy-assistant")) {
            boolean sidePanel = scroll.getParent() instanceof BorderPane split && split.getRight() == scroll;
            if (sidePanel) {
                scroll.setFitToHeight(true);
                scroll.setMinHeight(0);
                scroll.setMaxHeight(Double.MAX_VALUE);
                reading.setMinHeight(0);
                reading.setMaxHeight(Double.MAX_VALUE);
                for (Node child : reading.getChildren()) {
                    if (child instanceof VBox section) {
                        section.setMaxHeight(Double.MAX_VALUE);
                        VBox.setVgrow(section, Priority.ALWAYS);
                    }
                }
            }
            polishFullscreenReading(scroll.getContent());
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : List.copyOf(parent.getChildrenUnmodifiable())) polishFullscreenReading(child);
        }
    }

    private static void polishNearby(Parent parent) {
        Node current = parent;
        while (current != null) {
            if (current instanceof Region region && region.getStyleClass().contains("overview-action-card")) {
                polishExplorerSide(region);
            }
            current = current.getParent();
        }
    }

    /* ---------------- Reflection/tree helpers ---------------- */

    private static Parent ancestorNamed(Node node, String simpleName) {
        Node current = node;
        while (current != null) {
            if (current instanceof Parent parent && current.getClass().getSimpleName().equals(simpleName)) return parent;
            current = current.getParent();
        }
        return null;
    }

    private static boolean hasAncestorNamed(Node node, String simpleName) {
        return ancestorNamed(node, simpleName) != null;
    }

    private static Object fieldValue(Object owner, String name) {
        if (owner == null || name == null) return null;
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }
}
