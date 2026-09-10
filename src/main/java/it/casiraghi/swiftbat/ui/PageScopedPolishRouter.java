package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Routes the lightweight page-specific polish when MainView swaps a page into
 * the page host. Only that direct children list is observed: no global scans.
 *
 * <p>JavaFX controls such as ScrollPane, SplitPane and TabPane do not expose
 * their logical content reliably through getChildrenUnmodifiable() before the
 * skin has been laid out. The activation bridge below therefore enters those
 * content properties explicitly. This makes the final compact layout effective
 * on the first page opening as well as on macOS.</p>
 */
public final class PageScopedPolishRouter {
    private static final String DONE = PageScopedPolishRouter.class.getName() + ".done";
    private static final String EXPLORER_BRIDGE = PageScopedPolishRouter.class.getName() + ".explorerBridge";
    private static final String POPULATION_BRIDGE = PageScopedPolishRouter.class.getName() + ".populationBridge";

    private PageScopedPolishRouter() { }

    public static void install(Parent root) {
        StackPane pageHost = findPageHost(root);
        if (pageHost == null || Boolean.TRUE.equals(pageHost.getProperties().get(DONE))) return;
        pageHost.getProperties().put(DONE, Boolean.TRUE);

        for (Node child : List.copyOf(pageHost.getChildren())) route(child);
        pageHost.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) route(added);
            }
        });
    }

    private static void route(Node node) {
        if (!(node instanceof Parent parent)) return;
        FinalMacAndPopulationPolish.install(parent);
        DefinitiveLayoutAndManualSelectionFix.install(parent);

        // Final table geometry owner. It deliberately stops at TableView nodes,
        // so VirtualFlow/skin internals are never scanned while scrolling.
        FinalTableAlignmentFix.install(parent);

        if (parent instanceof ExplorerPage explorer) activateExplorer(explorer);
        if (parent instanceof PopulationPage population) activatePopulation(population);
    }

    private static void activateExplorer(ExplorerPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(EXPLORER_BRIDGE))) return;
        StackPane workspace = readWorkspace(page);
        if (workspace == null) return;
        page.getProperties().put(EXPLORER_BRIDGE, Boolean.TRUE);

        for (Node child : List.copyOf(workspace.getChildren())) polishExplorerWorkspaceChild(child);
        workspace.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node added : List.copyOf(change.getAddedSubList())) polishExplorerWorkspaceChild(added);
            }
        });
        Platform.runLater(() -> {
            for (Node child : List.copyOf(workspace.getChildren())) polishExplorerWorkspaceChild(child);
        });
    }

    private static void polishExplorerWorkspaceChild(Node node) {
        Node content = node instanceof ScrollPane scroll && scroll.getContent() != null
                ? scroll.getContent() : node;
        TabPane tabs = findLogical(content, TabPane.class, null);
        if (tabs == null) {
            invokeDefinitive("polishExplorerDashboard", new Class<?>[]{Node.class}, content);
            FinalExpertUiPolish.polishExplorer(content);
            ExplorerOverflowFix.apply(content);
            FinalTableAlignmentFix.install(content);
            MetadataSelectorRestoreFix.install(content);
            Platform.runLater(() -> {
                FinalExpertUiPolish.polishExplorer(content);
                ExplorerOverflowFix.apply(content);
                MetadataSelectorRestoreFix.install(content);
            });
            return;
        }
        for (Tab tab : tabs.getTabs()) {
            Node tabContent = tab.getContent();
            if (tabContent == null) continue;
            VBox chartCard = findLogical(tabContent, VBox.class, "overview-chart-card");
            if (chartCard != null) {
                invokeDefinitive("polishExplorerDashboard", new Class<?>[]{Node.class}, tabContent);
                break;
            }
        }

        // Run on the complete dashboard: fixes the 2D toolbar, all Explorer
        // tables and deterministically restores the searchable/grouped A-Z
        // metadata selector regardless of the active UI language.
        FinalExpertUiPolish.polishExplorer(content);
        ExplorerOverflowFix.apply(content);
        FinalTableAlignmentFix.install(content);
        MetadataSelectorRestoreFix.install(content);

        // Older compatibility passes schedule a post-CSS correction. Reapply the
        // final geometry one pulse later so the laptop layout cannot regress.
        Platform.runLater(() -> {
            FinalExpertUiPolish.polishExplorer(content);
            ExplorerOverflowFix.apply(content);
            MetadataSelectorRestoreFix.install(content);
        });
    }

    private static void activatePopulation(PopulationPage page) {
        if (Boolean.TRUE.equals(page.getProperties().get(POPULATION_BRIDGE))) return;
        VBox filterCard = findLogical(page, VBox.class, "population-filter-card");
        if (filterCard == null) {
            Platform.runLater(() -> activatePopulation(page));
            return;
        }
        page.getProperties().put(POPULATION_BRIDGE, Boolean.TRUE);

        // The watcher remains on the page so result tables created only after
        // Analyze group receive the same header/cell geometry automatically.
        FinalTableAlignmentFix.install(page);

        invokeDefinitive("alignPopulationFilters", new Class<?>[]{VBox.class}, filterCard);
        invokeDefinitive("installManualSelector", new Class<?>[]{PopulationPage.class, VBox.class}, page, filterCard);
        PopulationResetStabilityFix.install(page, filterCard);
        FinalExpertUiPolish.polishPopulation(filterCard);
        Platform.runLater(() -> {
            invokeDefinitive("alignPopulationFilters", new Class<?>[]{VBox.class}, filterCard);
            PopulationResetStabilityFix.install(page, filterCard);
            FinalExpertUiPolish.polishPopulation(filterCard);
            filterCard.requestLayout();
        });
    }

    private static StackPane readWorkspace(ExplorerPage page) {
        try {
            Field field = ExplorerPage.class.getDeclaredField("workspace");
            field.setAccessible(true);
            Object value = field.get(page);
            return value instanceof StackPane pane ? pane : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static void invokeDefinitive(String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = DefinitiveLayoutAndManualSelectionFix.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            method.invoke(null, args);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Keep the page usable even if a compatibility helper changes later.
        }
    }

    private static <T extends Node> T findLogical(Node node, Class<T> type, String styleClass) {
        if (node == null) return null;
        if (type.isInstance(node)
                && (styleClass == null || node.getStyleClass().contains(styleClass))) {
            return type.cast(node);
        }

        if (node instanceof ScrollPane scroll && scroll.getContent() != null) {
            T found = findLogical(scroll.getContent(), type, styleClass);
            if (found != null) return found;
        }
        if (node instanceof SplitPane split) {
            for (Node item : split.getItems()) {
                T found = findLogical(item, type, styleClass);
                if (found != null) return found;
            }
        }
        if (node instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) {
                T found = findLogical(tab.getContent(), type, styleClass);
                if (found != null) return found;
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findLogical(child, type, styleClass);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static StackPane findPageHost(Node node) {
        if (node instanceof StackPane pane && pane.getStyleClass().contains("page-host")) return pane;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                StackPane found = findPageHost(child);
                if (found != null) return found;
            }
        }
        return null;
    }
}
