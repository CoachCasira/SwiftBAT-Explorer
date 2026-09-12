package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Keeps the Explorer catalog caption synchronized with the actual catalog.
 *
 * <p>The catalog can finish loading before Explorer is first shown. It can also
 * be localized after that first load. This class therefore observes both the
 * list content and the caption itself, so a late localization/layout pass can
 * never put the stale "Loading catalog..." text back once data is available.</p>
 */
public final class ExplorerCatalogSidebarFix {
    private static final String HOST_INSTALLED = ExplorerCatalogSidebarFix.class.getName() + ".hostInstalled";
    private static final String EXPLORER_INSTALLED = ExplorerCatalogSidebarFix.class.getName() + ".explorerInstalled";
    private static final String CATALOG_READY = ExplorerCatalogSidebarFix.class.getName() + ".catalogReady";
    private static final String REFRESHING = ExplorerCatalogSidebarFix.class.getName() + ".refreshing";

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
            attachItemsListener(catalog, refresh);
            catalog.itemsProperty().addListener((obs, oldItems, newItems) -> {
                attachListChangeListener(newItems, refresh);
                refresh.run();
            });
            I18n.languageProperty().addListener((obs, oldLanguage, newLanguage) -> refresh.run());

            /*
             * UiTranslations may run after the async catalog callback on first
             * startup. If it ever restores the constructor text, immediately
             * replace it again once the catalog has already been observed ready.
             */
            caption.textProperty().addListener((obs, oldText, newText) -> {
                if (Boolean.TRUE.equals(caption.getProperties().get(REFRESHING))) return;
                if (!Boolean.TRUE.equals(caption.getProperties().get(CATALOG_READY))) return;
                if (looksLikeLoading(newText)) Platform.runLater(refresh);
            });

            if (catalog.getItems() != null && !catalog.getItems().isEmpty()) {
                caption.getProperties().put(CATALOG_READY, Boolean.TRUE);
            }
            refresh.run();

            // Cover the very first layout/localization pulses without a timer loop.
            Platform.runLater(() -> {
                refresh.run();
                Platform.runLater(refresh);
            });
        });
    }

    private static void attachItemsListener(ListView<?> catalog, Runnable refresh) {
        if (catalog == null) return;
        attachListChangeListener(catalog.getItems(), refresh);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void attachListChangeListener(ObservableList<?> items, Runnable refresh) {
        if (items == null || refresh == null) return;
        ((ObservableList) items).addListener((ListChangeListener) change -> refresh.run());
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

        caption.getProperties().put(REFRESHING, Boolean.TRUE);
        try {
            I18n.setText(caption,
                    count + " GRB visualizzati",
                    count + " GRBs shown");
        } finally {
            caption.getProperties().remove(REFRESHING);
        }
    }

    private static boolean looksLikeLoading(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("loading catalog") || lower.contains("catalogo in caricamento")
                || lower.contains("caricamento catalogo");
    }

    private static ListView<?> findCatalogList(Parent root) {
        Node node = findFirst(root, candidate -> candidate instanceof ListView<?>
                && candidate.getStyleClass().contains("catalog-list"));
        return node instanceof ListView<?> list ? list : null;
    }

    private static Label findCaption(Parent root) {
        Node preferred = findFirst(root, candidate -> candidate instanceof Label label
                && candidate.getStyleClass().contains("sidebar-caption")
                && (looksLikeLoading(label.getText()) || label.getText().toLowerCase(java.util.Locale.ROOT).contains("grb")));
        if (preferred instanceof Label label) return label;
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
