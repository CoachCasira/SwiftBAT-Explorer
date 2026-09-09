package it.casiraghi.swiftbat.ui;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.RedshiftInfo;
import it.casiraghi.swiftbat.model.SpectralData;
import it.casiraghi.swiftbat.model.SkyBurst;
import it.casiraghi.swiftbat.service.OnlineGrbService;
import it.casiraghi.swiftbat.service.RedshiftCatalogService;
import it.casiraghi.swiftbat.service.SkyCatalogService;
import it.casiraghi.swiftbat.service.SpectralCatalogService;
import it.casiraghi.swiftbat.service.SwiftCatalogService;
import it.casiraghi.swiftbat.ui.components.BlackHoleBackdropPane;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainView {
    private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "swiftbat-worker");
        thread.setDaemon(true);
        return thread;
    });
    private static final ExecutorService DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "swiftbat-download");
        thread.setDaemon(true);
        return thread;
    });

    private final HostServices hostServices;
    @SuppressWarnings("unused")
    private final Stage owner;
    private final SwiftCatalogService catalogService = new SwiftCatalogService();
    private final SkyCatalogService skyCatalogService = new SkyCatalogService();
    private final RedshiftCatalogService redshiftCatalogService = new RedshiftCatalogService();
    private final SpectralCatalogService spectralCatalogService = new SpectralCatalogService();
    private final OnlineGrbService grbService = new OnlineGrbService();
    private final ObservableMap<String, GrbData> sessionData = FXCollections.observableHashMap();
    private final Set<String> compareLoadsInFlight = ConcurrentHashMap.newKeySet();
    private List<CatalogEntry> currentCatalog = List.of();

    private final BorderPane root = new BorderPane();
    private final StackPane pageHost = new StackPane();
    private final Label connectionStatus = UiFactory.label("Connessione…", "status-pill", "status-neutral");
    private final Label catalogStatus = UiFactory.label("Catalogo…", "top-info");
    private final Label sessionStatus = UiFactory.label("0 in memoria", "top-info");

    private final ExplorerPage explorerPage;
    private final HomePage homePage;
    private final GlossaryPage glossaryPage;
    private final ComparePage comparePage;
    private final PopulationPage populationPage;
    private final SkyMapPage skyMapPage;
    private final AboutPage aboutPage;
    private final VBox navigation = new VBox(7);
    private Button activeNavigationButton;

    public MainView(HostServices hostServices, Stage owner) {
        this.hostServices = hostServices;
        this.owner = owner;
        explorerPage = new ExplorerPage(hostServices, this::loadGrb,
                entry -> grbService.cachedLocally(entry.grbName()));
        glossaryPage = new GlossaryPage();
        comparePage = new ComparePage(sessionData, this::loadCompareGrb);
        populationPage = new PopulationPage(
                (entry, progress) -> grbService.load(entry, false, progress),
                BACKGROUND_EXECUTOR, DOWNLOAD_EXECUTOR, sessionData);
        skyMapPage = new SkyMapPage(entry -> loadGrb(entry, false));
        aboutPage = new AboutPage(hostServices, () -> navigate("glossary"));
        homePage = new HomePage(
                () -> navigate("explorer"),
                () -> navigate("sky"),
                () -> navigate("population"),
                () -> navigate("compare"),
                () -> navigate("about"),
                catalogStatus.textProperty(),
                sessionStatus.textProperty(),
                connectionStatus.textProperty());
        buildLayout();
        sessionData.addListener((javafx.collections.MapChangeListener<String, GrbData>) change -> {
            updateCacheStatus();
            explorerPage.refreshCacheIndicators();
        });
        updateCacheStatus();
    }

    public BorderPane getRoot() {
        return root;
    }

    public void initialize() {
        navigate("home");
        loadCatalog();
        loadSpectralCatalog(false);
    }

    public static void shutdownSharedExecutor() {
        BACKGROUND_EXECUTOR.shutdownNow();
        DOWNLOAD_EXECUTOR.shutdownNow();
    }

    private void buildLayout() {
        root.getStyleClass().addAll("app-root", "black-hole-redesign", "reference-redesign");
        root.setLeft(buildNavigation());
        root.setTop(buildTopBar());
        pageHost.getStyleClass().add("page-host");
        StackPane workspace = new StackPane(new BlackHoleBackdropPane(), pageHost);
        workspace.getStyleClass().add("workspace-shell");
        root.setCenter(workspace);
    }

    private Node buildNavigation() {
        navigation.getStyleClass().add("main-navigation");
        navigation.setPadding(new Insets(18, 12, 14, 12));
        navigation.setPrefWidth(228);
        navigation.setMinWidth(214);

        Button home = navButton("⌂", "Home", "home");
        Button explorer = navButton("⌕", "Esplora", "explorer");
        Button sky = navButton("◇", "Mappa celeste", "sky");
        Button spectroscopy = navButton("⌁", "Spettroscopia", "spectroscopy");
        Button population = navButton("≋", "Analisi di popolazione", "population");
        Button compare = navButton("⇄", "Confronta", "compare");
        Button about = navButton("ⓘ", "Info", "about");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Separator separator = new Separator();
        separator.getStyleClass().add("soft-separator");
        VBox footer = new VBox(4,
                UiFactory.label("◉  SwiftBAT Explorer", "nav-footer-title"),
                UiFactory.label("v1.3.0", "nav-footer-version"),
                UiFactory.label("INAF – OAS Bologna", "nav-footer-line"),
                UiFactory.wrappedLabel("Un progetto per la scienza aperta", "nav-footer-line"));
        footer.getStyleClass().add("nav-footer-card");
        footer.setPadding(new Insets(11));

        Label online = UiFactory.wrappedLabel("Dati scientifici NASA/GSFC Swift/BAT", "nav-source");
        Button source = UiFactory.button("Fonte ufficiale  ↗", "nav-source-button");
        source.setMaxWidth(Double.MAX_VALUE);
        source.setOnAction(event -> hostServices.showDocument(SwiftCatalogService.CATALOG_URL));

        navigation.getChildren().addAll(home, explorer, sky, spectroscopy, population, compare, about,
                spacer, separator, footer, online, source);
        return navigation;
    }

    private Button navButton(String glyph, String text, String page) {
        Button button = UiFactory.button(glyph + "   " + text, "nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> {
            activeNavigationButton = button;
            updateNavigationSelection();
            navigate(page);
        });
        button.setUserData(page);
        return button;
    }

    private void updateNavigationSelection() {
        for (Node node : navigation.getChildren()) {
            if (node instanceof Button button && button.getStyleClass().contains("nav-button")) {
                button.getStyleClass().remove("nav-button-active");
            }
        }
        if (activeNavigationButton != null && !activeNavigationButton.getStyleClass().contains("nav-button-active")) {
            activeNavigationButton.getStyleClass().add("nav-button-active");
        }
    }

    private Node buildTopBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("top-bar");
        bar.setPadding(new Insets(9, 16, 9, 16));
        bar.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(9);
        brand.getStyleClass().add("top-brand");
        brand.setAlignment(Pos.CENTER_LEFT);
        Label logo = UiFactory.label("◉", "top-orbit-logo");
        VBox brandCopy = new VBox(0,
                UiFactory.label("SwiftBAT Explorer", "top-brand-title"),
                UiFactory.label("Esplora i lampi di raggi gamma", "top-brand-subtitle"));
        brand.getChildren().addAll(logo, brandCopy);

        TextField globalSearch = new TextField();
        globalSearch.getStyleClass().add("global-search-field");
        globalSearch.setPromptText("⌕   Cerca un GRB (es. GRB250605A, 231107A, …)");
        globalSearch.setPrefWidth(560);
        globalSearch.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(globalSearch, Priority.ALWAYS);
        globalSearch.setOnAction(event -> runGlobalSearch(globalSearch));

        Button dataset = topActionButton("▱", "Dataset");
        dataset.setOnAction(event -> navigate("explorer"));
        Button tools = topActionButton("⌁", "Strumenti");
        tools.setOnAction(event -> navigate("compare"));
        Button guide = topActionButton("?", "Guida");
        guide.setOnAction(event -> navigate("about"));

        Button refresh = UiFactory.iconButton("↻", "Aggiorna il catalogo online");
        refresh.setOnAction(event -> {
            loadCatalog();
            loadSpectralCatalog(true);
        });
        Button official = UiFactory.iconButton("⚙", "Apri il catalogo ufficiale");
        official.setOnAction(event -> hostServices.showDocument(SwiftCatalogService.CATALOG_URL));

        ToggleButton italian = new ToggleButton("IT");
        ToggleButton english = new ToggleButton("EN");
        italian.getStyleClass().add("language-toggle");
        english.getStyleClass().add("language-toggle");
        ToggleGroup languages = new ToggleGroup();
        italian.setToggleGroup(languages);
        english.setToggleGroup(languages);
        if (I18n.language() == I18n.Language.EN) english.setSelected(true); else italian.setSelected(true);
        italian.setOnAction(event -> I18n.setLanguage(I18n.Language.IT));
        english.setOnAction(event -> I18n.setLanguage(I18n.Language.EN));
        HBox languageBox = new HBox(2, italian, english);
        languageBox.getStyleClass().add("language-switch");
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(() -> {
            I18n.localizeTree(root);
            updateCacheStatus();
        }));

        HBox telemetry = new HBox(5, catalogStatus, sessionStatus, connectionStatus);
        telemetry.getStyleClass().add("top-telemetry");
        telemetry.setAlignment(Pos.CENTER_RIGHT);

        bar.getChildren().addAll(brand, globalSearch, dataset, tools, guide,
                telemetry, languageBox, refresh, official);
        return bar;
    }

    private Button topActionButton(String glyph, String text) {
        Button button = UiFactory.button(glyph + "  " + text, "top-nav-button");
        button.setFocusTraversable(false);
        return button;
    }

    private void runGlobalSearch(TextField field) {
        String raw = field.getText() == null ? "" : field.getText().trim();
        if (raw.isBlank()) {
            navigate("explorer");
            return;
        }
        String query = raw.toUpperCase(Locale.ROOT).replace(" ", "");
        CatalogEntry match = currentCatalog.stream()
                .filter(entry -> {
                    String name = entry.grbName().toUpperCase(Locale.ROOT).replace(" ", "");
                    return name.equals(query)
                            || ("GRB" + name).equals(query)
                            || name.contains(query.replaceFirst("^GRB", ""));
                })
                .findFirst()
                .orElse(null);
        if (match != null) {
            loadGrb(match, false);
            field.selectAll();
        } else {
            navigate("explorer");
        }
    }

    private void navigate(String page) {
        Node node = switch (page) {
            case "explorer" -> {
                explorerPage.showTab("Curva 2D");
                yield explorerPage;
            }
            case "spectroscopy" -> {
                explorerPage.showTab("Spettroscopia");
                yield explorerPage;
            }
            case "compare" -> comparePage;
            case "population" -> populationPage;
            case "sky" -> skyMapPage;
            case "glossary" -> glossaryPage;
            case "about" -> aboutPage;
            default -> homePage;
        };
        pageHost.getChildren().setAll(node);
        Platform.runLater(() -> I18n.localizeTree(node));
        boolean foundVisibleButton = false;
        for (Node navNode : navigation.getChildren()) {
            if (navNode instanceof Button button && page.equals(button.getUserData())) {
                activeNavigationButton = button;
                foundVisibleButton = true;
                break;
            }
        }
        if (!foundVisibleButton && "glossary".equals(page)) {
            for (Node navNode : navigation.getChildren()) {
                if (navNode instanceof Button button && "about".equals(button.getUserData())) {
                    activeNavigationButton = button;
                    break;
                }
            }
        }
        updateNavigationSelection();
    }

    private void loadCatalog() {
        setConnection("Connessione…", "status-neutral");
        I18n.setText(catalogStatus, "Catalogo…", "Catalog…");
        Task<List<CatalogEntry>> task = new Task<>() {
            @Override
            protected List<CatalogEntry> call() throws Exception {
                return catalogService.fetchCatalog();
            }
        };
        task.setOnSucceeded(event -> {
            List<CatalogEntry> entries = task.getValue();
            currentCatalog = List.copyOf(entries);
            explorerPage.setCatalog(entries, false);
            skyMapPage.setBaseCatalog(entries);
            populationPage.setCatalog(entries);
            comparePage.setCatalog(entries);
            I18n.setText(catalogStatus, entries.size() + " GRB", entries.size() + " GRBs");
            setConnection("Online", "status-online");
            loadSkyCatalog();
        });
        task.setOnFailed(event -> {
            List<CatalogEntry> fallback = catalogService.fallbackCatalog();
            currentCatalog = List.copyOf(fallback);
            explorerPage.setCatalog(fallback, true);
            skyMapPage.setBaseCatalog(fallback);
            populationPage.setCatalog(fallback);
            comparePage.setCatalog(fallback);
            I18n.setText(catalogStatus, fallback.size() + " GRB ridotti", fallback.size() + " GRBs · fallback");
            setConnection("Offline parziale", "status-warning");
            loadSkyCatalog();
        });
        BACKGROUND_EXECUTOR.execute(task);
    }

    private void loadSkyCatalog() {
        skyMapPage.showLoading("Coordinate celesti…");
        Task<ScientificCatalog> task = new Task<>() {
            @Override
            protected ScientificCatalog call() throws Exception {
                List<SkyBurst> sky = skyCatalogService.fetchSkyCatalog();
                try {
                    Map<String, RedshiftInfo> redshifts = redshiftCatalogService.fetchRedshifts();
                    List<SkyBurst> merged = sky.stream()
                            .map(burst -> burst.withRedshift(redshifts.getOrDefault(
                                    burst.grbName(), RedshiftInfo.missing())))
                            .toList();
                    return new ScientificCatalog(merged, true);
                } catch (InterruptedException interrupted) {
                    throw interrupted;
                } catch (Exception redshiftError) {
                    return new ScientificCatalog(sky, false);
                }
            }
        };
        task.setOnSucceeded(event -> {
            ScientificCatalog scientific = task.getValue();
            skyMapPage.setSkyBursts(scientific.bursts());
            explorerPage.setScientificMetadata(scientific.bursts());
            populationPage.setBursts(scientific.bursts());
            if (!scientific.redshiftAvailable()) {
                skyMapPage.showWarning("Coordinate e T90 caricati; redshift temporaneamente non disponibile");
            }
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            String detail = error == null || error.getMessage() == null
                    ? "Coordinate celesti non disponibili"
                    : I18n.dynamic("Mappa non disponibile: " + error.getMessage(),
                            "Map unavailable: " + error.getMessage());
            skyMapPage.showError(detail);
        });
        BACKGROUND_EXECUTOR.execute(task);
    }

    private void loadSpectralCatalog(boolean forceRefresh) {
        Task<Map<String, SpectralData>> task = new Task<>() {
            @Override
            protected Map<String, SpectralData> call() throws Exception {
                return spectralCatalogService.fetchCatalog(forceRefresh);
            }
        };
        task.setOnSucceeded(event -> explorerPage.setSpectralCatalog(task.getValue()));
        task.setOnFailed(event -> explorerPage.setSpectralCatalog(Map.of()));
        BACKGROUND_EXECUTOR.execute(task);
    }

    private void loadCompareGrb(CatalogEntry entry) {
        if (entry == null || sessionData.containsKey(entry.grbName())
                || !compareLoadsInFlight.add(entry.grbName())) return;
        setConnection(I18n.dynamic("Caricamento confronto…", "Loading comparison…"), "status-neutral");
        Task<GrbData> task = new Task<>() {
            @Override protected GrbData call() throws Exception {
                return grbService.load(entry, false, update -> { });
            }
        };
        task.setOnSucceeded(event -> {
            compareLoadsInFlight.remove(entry.grbName());
            GrbData data = task.getValue();
            if (data != null) sessionData.put(data.grbName(), data);
            setConnection("Online", "status-online");
        });
        task.setOnFailed(event -> {
            compareLoadsInFlight.remove(entry.grbName());
            setConnection(I18n.dynamic("Errore caricamento confronto", "Comparison load failed"), "status-warning");
        });
        DOWNLOAD_EXECUTOR.execute(task);
    }

    private void loadGrb(CatalogEntry entry, boolean forceRefresh) {
        if (entry == null) {
            return;
        }
        navigate("explorer");
        explorerPage.setSelectedEntry(entry);

        if (!forceRefresh) {
            GrbData cached = sessionData.get(entry.grbName());
            if (cached != null) {
                explorerPage.showData(cached);
                setConnection("In memoria", "status-online");
                return;
            }
        }

        explorerPage.showLoading(0.02,
                I18n.dynamic("Apro " + entry.grbName(), "Opening " + entry.grbName()),
                I18n.dynamic("Recupero i prodotti Swift/BAT online.", "Retrieving Swift/BAT products online."));

        Task<GrbData> task = new Task<>() {
            @Override
            protected GrbData call() throws Exception {
                return grbService.load(entry, forceRefresh, update ->
                        Platform.runLater(() -> explorerPage.showLoading(
                                update.progress(), update.title(), update.detail())));
            }
        };
        task.setOnSucceeded(event -> {
            GrbData data = task.getValue();
            sessionData.put(data.grbName(), data);
            explorerPage.showData(data);
            setConnection("Online", "status-online");
        });
        task.setOnFailed(event -> explorerPage.showError(entry, task.getException()));
        DOWNLOAD_EXECUTOR.execute(task);
    }

    private void updateCacheStatus() {
        I18n.setText(sessionStatus,
                sessionData.size() + " RAM · " + grbService.persistentCachedCount() + " locali",
                sessionData.size() + " RAM · " + grbService.persistentCachedCount() + " local");
    }

    private void setConnection(String text, String styleClass) {
        I18n.setText(connectionStatus, text, I18n.english(text));
        connectionStatus.getStyleClass().removeAll("status-neutral", "status-online", "status-warning");
        connectionStatus.getStyleClass().add(styleClass);
    }

    private record ScientificCatalog(List<SkyBurst> bursts, boolean redshiftAvailable) {
    }
}
