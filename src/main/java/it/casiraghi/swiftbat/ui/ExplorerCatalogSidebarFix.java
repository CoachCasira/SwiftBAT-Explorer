package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Final, lightweight fix for the Explorer catalog sidebar.
 *
 * <p>It keeps the catalog status synchronized with the real filtered list and
 * avoids the stale "Loading catalog..." caption after the catalog is already
 * available. The watcher is attached only to the page host, not to the whole
 * application tree.</p>
 */
public final class ExplorerCatalogSidebarFix {
    private static final String HOST_INSTALLED = ExplorerCatalogSidebarFix.class.getName() + ".hostInstalled";
    private static final String EXPLORER_INSTALLED = ExplorerCatalogSidebarFix.class.getName() + ".explorerInstalled";
    private static final String CATALOG_READY = ExplorerCatalogSidebarFix.class.getName() + ".catalogReady";

    private ExplorerCatalogSidebarFix() { }

    public static void install(Parent root) {
        if (root == null) return;
        Platform.runLater(() -> installOnPageHost(root));
    }

    private static void installOnPageHost(Parent root) {
        Parent pageHost = findParentWithStyleClass(root, "page-host");
        if (pageHost == null) {
            Platform.runLater(() -> installOnPageHost(root));
            return;
        }
        if (Boolean.TRUE.equals(pageHost.getProperties().get(HOST_INSTALLED))) {
            inspectChildren(pageHost);
            return;
        }
        pageHost.getProperties().put(HOST_INSTALLED, Boolean.TRUE);
        inspectChildren(pageHost);
        pageHost.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (Node node : change.getAddedSubList()) {
                    if (node instanceof ExplorerPage explorer) installExplorer(explorer);
                }
            }
        });
    }

    private static void inspectChildren(Parent pageHost) {
        for (Node node : pageHost.getChildrenUnmodifiable()) {
            if (node instanceof ExplorerPage explorer) installExplorer(explorer);
        }
    }

    private static void installExplorer(ExplorerPage explorer) {
        if (explorer == null || Boolean.TRUE.equals(explorer.getProperties().get(EXPLORER_INSTALLED))) return;
        explorer.getProperties().put(EXPLORER_INSTALLED, Boolean.TRUE);

        Platform.runLater(() -> {
            ListView<?> catalog = findCatalogList(explorer);
            Label caption = findCaption(explorer);
            if (catalog == null || caption == null) return;

            Runnable refresh = () -> refreshCaption(catalog, caption);
            catalog.getItems().addListener((ListChangeListener<Object>) change -> refresh.run());
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> refresh.run());

            if (!catalog.getItems().isEmpty()) {
                caption.getProperties().put(CATALOG_READY, Boolean.TRUE);
            }
            refresh.run();
        });
    }

    private static void refreshCaption(ListView<?> catalog, Label caption) {
        if (catalog == null || caption == null) return;
        int count = catalog.getItems() == null ? 0 : catalog.getItems().size();
        boolean ready = Boolean.TRUE.equals(caption.getProperties().get(CATALOG_READY));

        if (!ready && count > 0) {
            ready = true;
            caption.getProperties().put(CATALOG_READY, Boolean.TRUE);
        }
        if (!ready) return;

        I18n.setText(caption,
                count + " GRB visualizzati",
                count + " GRBs shown");
    }

    private static ListView<?> findCatalogList(Parent root) {
        Node node = findFirst(root, candidate -> candidate instanceof ListView<?>
                && candidate.getStyleClass().contains("catalog-list"));
        return node instanceof ListView<?> list ? list : null;
    }

    private static Label findCaption(Parent root) {
        Node node = findFirst(root, candidate -> candidate instanceof Label
                && candidate.getStyleClass().contains("sidebar-caption"));
        return node instanceof Label label ? label : null;
    }

    private static Parent findParentWithStyleClass(Parent root, String styleClass) {
        Node node = findFirst(root, candidate -> candidate instanceof Parent
                && candidate.getStyleClass().contains(styleClass));
        return node instanceof Parent parent ? parent : null;
    }

    private static Node findFirst(Node root, java.util.function.Predicate<Node> predicate) {
        if (root == null || predicate == null) return null;
        Deque<Node> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Node current = queue.removeFirst();
            if (predicate.test(current)) return current;
            if (current instanceof Parent parent) {
                queue.addAll(parent.getChildrenUnmodifiable());
            }
        }
        return null;
    }
}
