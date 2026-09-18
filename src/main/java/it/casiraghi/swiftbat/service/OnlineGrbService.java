package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.CatalogEntry;
import it.casiraghi.swiftbat.model.FieldDefinition;
import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.LoadUpdate;
import it.casiraghi.swiftbat.model.MetadataItem;
import it.casiraghi.swiftbat.model.ProductAvailability;
import it.casiraghi.swiftbat.model.SummaryItem;
import it.casiraghi.swiftbat.model.TabularData;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Scarica e interpreta direttamente online i prodotti Swift/BAT a binning di 1 secondo.
 * I dati vengono mantenuti in RAM durante la sessione e, dopo il primo download,
 * anche in una cache locale persistente.
 */
public final class OnlineGrbService {
    private static final String USER_AGENT = "SwiftBAT-Explorer/1.3.0 (academic thesis application; JavaFX)";
    private static final int HTML_TIMEOUT_MS = (int) Duration.ofSeconds(30).toMillis();
    private static final int FILE_TIMEOUT_MS = (int) Duration.ofSeconds(90).toMillis();
    private static final Pattern RESULTS_DIRECTORY = Pattern.compile("^\\d+-results/$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FITS_1CHAN_1S = Pattern.compile(".*_1chan_1s\\.lc$", Pattern.CASE_INSENSITIVE);
    private static final int MAX_PARALLEL_HTTP_REQUESTS = 4;

    public static final List<String> ASCII_HEADERS = List.of(
            "TIME_FROM_TRIGGER_CENTER_S",
            "RATE_15_25_KEV",
            "ERROR_15_25_KEV",
            "RATE_25_50_KEV",
            "ERROR_25_50_KEV",
            "RATE_50_100_KEV",
            "ERROR_50_100_KEV",
            "RATE_100_350_KEV",
            "ERROR_100_350_KEV",
            "RATE_15_350_KEV",
            "ERROR_15_350_KEV");

    public static final List<String> FITS_HEADERS = List.of(
            "TIME_FROM_TRIGGER_CENTER_S",
            "TIME_FROM_TRIGGER_START_S",
            "TIME_MET_S",
            "RATE",
            "ERROR",
            "TOTCOUNTS",
            "FRACEXP");

    private final Map<String, GrbData> sessionCache = new ConcurrentHashMap<>();
    private final PersistentGrbCache persistentCache;
    private final Semaphore httpSlots = new Semaphore(MAX_PARALLEL_HTTP_REQUESTS, true);
    private final ThreadLocal<DecimalFormat> numberFormat;

    public OnlineGrbService() {
        this(new PersistentGrbCache());
    }

    OnlineGrbService(PersistentGrbCache persistentCache) {
        this.persistentCache = persistentCache;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        numberFormat = ThreadLocal.withInitial(() -> new DecimalFormat("0.###############", symbols));
    }

    public GrbData load(CatalogEntry entry, boolean forceRefresh, Consumer<LoadUpdate> progress) throws IOException {
        Consumer<LoadUpdate> notifier = progress == null ? ignored -> { } : progress;
        String cacheKey = entry.grbName().toUpperCase(Locale.ROOT);

        if (!forceRefresh) {
            GrbData cached = sessionCache.get(cacheKey);
            if (cached != null) {
                notifier.accept(new LoadUpdate(1.0, "Dati pronti", "Evento recuperato dalla cache della sessione."));
                return cached;
            }

            try {
                Optional<PersistentGrbCache.CachedProducts> local = persistentCache.read(entry);
                if (local.isPresent()) {
                    notifier.accept(new LoadUpdate(0.18, "Cache locale", "Leggo ASCII e FITS già salvati sul dispositivo."));
                    PersistentGrbCache.CachedProducts products = local.get();
                    ProductLinks links = new ProductLinks(
                            products.dataProductUrl(),
                            products.resultsUrl(),
                            products.lightCurveDirectoryUrl(),
                            products.asciiUrl(),
                            products.fitsUrl());
                    GrbData result = buildData(entry, links, products.asciiBytes(), products.fitsBytes(),
                            products.savedAt(), "NASA/GSFC · cache locale");
                    sessionCache.put(cacheKey, result);
                    notifier.accept(new LoadUpdate(1.0, "Dati pronti", "Evento recuperato dalla cache locale."));
                    return result;
                }
            } catch (IOException localError) {
                notifier.accept(new LoadUpdate(0.05, "Cache non leggibile", "Procedo con il download online."));
            }
        }

        notifier.accept(new LoadUpdate(0.05, "Ricerca dei prodotti", "Apro la pagina Data Product ufficiale."));
        ProductLinks links = resolveProductLinks(entry, notifier);

        byte[] asciiBytes = null;
        byte[] fitsBytes = null;
        IOException asciiError = null;
        IOException fitsError = null;

        if (!links.asciiUrl().isBlank()) {
            try {
                notifier.accept(new LoadUpdate(0.35, "Download ASCII", "Scarico 1s_lc_ascii.dat."));
                asciiBytes = fetchBytes(links.asciiUrl());
            } catch (IOException error) {
                asciiError = error;
            }
        }

        if (!links.fitsUrl().isBlank()) {
            try {
                notifier.accept(new LoadUpdate(0.55, "Download FITS", "Scarico il prodotto *_1chan_1s.lc."));
                fitsBytes = fetchBytes(links.fitsUrl());
            } catch (IOException error) {
                fitsError = error;
            }
        }

        if (asciiBytes == null && fitsBytes == null) {
            StringBuilder message = new StringBuilder("Non sono disponibili prodotti a un secondo leggibili per ")
                    .append(entry.grbName()).append('.');
            if (asciiError != null) {
                message.append(" ASCII: ").append(asciiError.getMessage()).append('.');
            }
            if (fitsError != null) {
                message.append(" FITS: ").append(fitsError.getMessage()).append('.');
            }
            throw new IOException(message.toString());
        }

        notifier.accept(new LoadUpdate(0.70, "Interpretazione", "Converto le curve in tabelle utilizzabili."));
        Instant downloadedAt = Instant.now();
        GrbData result = buildData(entry, links, asciiBytes, fitsBytes, downloadedAt, "NASA/GSFC online");
        try {
            persistentCache.write(entry, new PersistentGrbCache.CachedProducts(
                    asciiBytes, fitsBytes,
                    links.dataProductUrl(), links.resultsUrl(), links.lightCurveDirectoryUrl(),
                    links.asciiUrl(), links.fitsUrl(), downloadedAt));
        } catch (IOException ignored) {
            // Un problema della cache locale non deve rendere inutilizzabili dati online validi.
        }
        sessionCache.put(cacheKey, result);
        notifier.accept(new LoadUpdate(1.0, "Dati pronti", "Curve, metadati e spiegazioni sono disponibili."));
        return result;
    }

    public GrbData load(CatalogEntry entry, boolean forceRefresh) throws IOException {
        return load(entry, forceRefresh, null);
    }

    private GrbData buildData(CatalogEntry entry, ProductLinks links, byte[] asciiBytes, byte[] fitsBytes,
                              Instant loadedAt, String dataSource) throws IOException {
        TabularData ascii = TabularData.empty();
        FitsResult fits = FitsResult.empty();
        IOException asciiParseError = null;
        IOException fitsParseError = null;

        if (asciiBytes != null) {
            try {
                ascii = parseAscii(asciiBytes);
            } catch (IOException error) {
                asciiParseError = error;
            }
        }
        if (fitsBytes != null) {
            try {
                fits = parseFits(fitsBytes);
            } catch (IOException error) {
                fitsParseError = error;
            }
        }

        if (ascii.isEmpty() && fits.table().isEmpty()) {
            StringBuilder message = new StringBuilder("I prodotti sono stati raggiunti, ma nessuno è stato interpretato correttamente per ")
                    .append(entry.grbName()).append('.');
            if (asciiParseError != null) {
                message.append(" ASCII: ").append(asciiParseError.getMessage()).append('.');
            }
            if (fitsParseError != null) {
                message.append(" FITS: ").append(fitsParseError.getMessage()).append('.');
            }
            throw new IOException(message.toString());
        }

        ProductAvailability availability = new ProductAvailability(
                !ascii.isEmpty(),
                !fits.table().isEmpty(),
                ascii.isEmpty() ? "" : fileName(links.asciiUrl()),
                fits.table().isEmpty() ? "" : fileName(links.fitsUrl()),
                links.dataProductUrl(),
                links.resultsUrl(),
                links.lightCurveDirectoryUrl());
        List<SummaryItem> summary = buildSummary(entry, availability, ascii, fits, dataSource);

        return new GrbData(
                entry.grbName().toUpperCase(Locale.ROOT),
                entry.triggerId(),
                loadedAt,
                availability,
                List.copyOf(summary),
                ascii,
                fits.table(),
                fits.metadata(),
                dictionary());
    }

    public void evict(String grbName) {
        if (grbName != null) {
            sessionCache.remove(grbName.toUpperCase(Locale.ROOT));
        }
    }

    public int cachedCount() {
        return sessionCache.size();
    }

    public int persistentCachedCount() {
        return persistentCache.count();
    }

    public boolean cachedLocally(String grbName) {
        return grbName != null && (sessionCache.containsKey(grbName.toUpperCase(Locale.ROOT))
                || persistentCache.contains(grbName));
    }

    public Set<String> cachedNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(sessionCache.keySet()));
    }

    public Optional<GrbData> cached(String grbName) {
        if (grbName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionCache.get(grbName.toUpperCase(Locale.ROOT)));
    }

    ProductLinks resolveProductLinks(CatalogEntry entry, Consumer<LoadUpdate> notifier) throws IOException {
        String dataProductUrl = normalizeDirectoryUrl(entry.dataProductUrl());
        if (dataProductUrl.isBlank()) {
            dataProductUrl = SwiftCatalogService.CATALOG_URL + entry.grbName() + "/data_product/";
        }

        Document dataProduct = fetchDocument(dataProductUrl);
        notifier.accept(new LoadUpdate(0.14, "Cartella risultati", "Individuo la directory *-results/."));
        String resultsUrl = findLink(dataProduct, href -> RESULTS_DIRECTORY.matcher(fileName(href)).matches())
                .orElseThrow(() -> new IOException(
                        "Cartella *-results/ non trovata nel Data Product di " + entry.grbName() + "."));

        Document results = fetchDocument(resultsUrl);
        notifier.accept(new LoadUpdate(0.22, "Cartella curve di luce", "Individuo la directory lc/."));
        String lcUrl = findLink(results, href -> fileName(href).equalsIgnoreCase("lc/"))
                .orElseThrow(() -> new IOException(
                        "Cartella lc/ non trovata nei risultati di " + entry.grbName() + "."));

        Document lcDirectory = fetchDocument(lcUrl);
        notifier.accept(new LoadUpdate(0.29, "Prodotti a un secondo", "Cerco DAT e FITS con binning di 1 s."));
        String asciiUrl = findLink(lcDirectory, href -> fileName(href).equalsIgnoreCase("1s_lc_ascii.dat"))
                .orElse("");
        String fitsUrl = findLink(lcDirectory, href -> FITS_1CHAN_1S.matcher(fileName(href)).matches())
                .orElse("");

        return new ProductLinks(dataProductUrl, resultsUrl, lcUrl, asciiUrl, fitsUrl);
    }

    private Document fetchDocument(String url) throws IOException {
        return withHttpSlot(() -> Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(HTML_TIMEOUT_MS)
                .followRedirects(true)
                .get());
    }

    private byte[] fetchBytes(String url) throws IOException {
        return withHttpSlot(() -> {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(FILE_TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .execute();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Download non riuscito (HTTP " + response.statusCode() + "): " + url);
            }
            return response.bodyAsBytes();
        });
    }

    private <T> T withHttpSlot(IoSupplier<T> action) throws IOException {
        boolean acquired = false;
        try {
            httpSlots.acquire();
            acquired = true;
            return action.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrotto.", interrupted);
        } finally {
            if (acquired) {
                httpSlots.release();
            }
        }
    }

    private Optional<String> findLink(Document document, java.util.function.Predicate<String> predicate) {
        for (Element link : document.select("a[href]")) {
            String absolute = link.absUrl("href");
            if (!absolute.isBlank() && predicate.test(absolute)) {
                return Optional.of(absolute);
            }
        }
        return Optional.empty();
    }

    private String fileName(String url) {
        String withoutQuery = url == null ? "" : url.split("[?#]", 2)[0];
        boolean directory = withoutQuery.endsWith("/");
        String trimmed = directory ? withoutQuery.substring(0, withoutQuery.length() - 1) : withoutQuery;
        int slash = trimmed.lastIndexOf('/');
        String name = slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
        return directory ? name + "/" : name;
    }

    private String normalizeDirectoryUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url : url + "/";
    }

    private TabularData parseAscii(byte[] bytes) throws IOException {
        String text = new String(bytes, StandardCharsets.US_ASCII);
        List<List<String>> rows = new ArrayList<>();
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            String[] tokens = line.split("\\s+");
            if (tokens.length < ASCII_HEADERS.size()) {
                continue;
            }
            rows.add(List.copyOf(Arrays.asList(tokens).subList(0, ASCII_HEADERS.size())));
        }
        if (rows.isEmpty()) {
            throw new IOException("Il file 1s_lc_ascii.dat non contiene righe numeriche leggibili.");
        }
        return new TabularData(ASCII_HEADERS, List.copyOf(rows));
    }

    private FitsResult parseFits(byte[] bytes) throws IOException {
        try (Fits fits = new Fits(new ByteArrayInputStream(bytes))) {
            BasicHDU<?>[] hdus = fits.read();
            BinaryTableHDU rateTable = null;
            List<MetadataItem> metadata = new ArrayList<>();

            for (int index = 0; index < hdus.length; index++) {
                BasicHDU<?> hdu = hdus[index];
                Header header = hdu.getHeader();
                String hduName = header.getStringValue("EXTNAME");
                if (hduName == null || hduName.isBlank()) {
                    hduName = index == 0 ? "PRIMARY" : "HDU " + index;
                }
                addMetadata(metadata, index, hduName, header);
                if (hdu instanceof BinaryTableHDU binary
                        && "RATE".equalsIgnoreCase(header.getStringValue("EXTNAME"))) {
                    rateTable = binary;
                }
            }

            if (rateTable == null) {
                throw new IOException("Nel file FITS non è stata trovata l'estensione binaria RATE.");
            }

            Header rateHeader = rateTable.getHeader();
            Header primaryHeader = hdus.length > 0 ? hdus[0].getHeader() : rateHeader;
            double triggerTime = firstFinite(
                    rateHeader.getDoubleValue("TRIGTIME", Double.NaN),
                    primaryHeader.getDoubleValue("TRIGTIME", Double.NaN),
                    0.0);
            double timeDelta = rateHeader.getDoubleValue("TIMEDEL", 1.0);

            Map<String, Object> columns = new LinkedHashMap<>();
            for (String field : List.of("TIME", "RATE", "ERROR", "TOTCOUNTS", "FRACEXP")) {
                int columnIndex = rateTable.findColumn(field);
                if (columnIndex >= 0) {
                    columns.put(field, rateTable.getColumn(columnIndex));
                }
            }

            List<List<String>> rows = new ArrayList<>(rateTable.getNRows());
            for (int row = 0; row < rateTable.getNRows(); row++) {
                double timeMet = toDouble(valueAt(columns.get("TIME"), row));
                double relativeStart = timeMet - triggerTime;
                double relativeCenter = relativeStart + timeDelta / 2.0;
                rows.add(List.of(
                        format(relativeCenter),
                        format(relativeStart),
                        format(timeMet),
                        format(toDouble(valueAt(columns.get("RATE"), row))),
                        format(toDouble(valueAt(columns.get("ERROR"), row))),
                        format(toDouble(valueAt(columns.get("TOTCOUNTS"), row))),
                        format(toDouble(valueAt(columns.get("FRACEXP"), row)))));
            }

            return new FitsResult(
                    new TabularData(FITS_HEADERS, List.copyOf(rows)),
                    List.copyOf(metadata),
                    triggerTime,
                    timeDelta,
                    firstNonBlank(primaryHeader.getStringValue("OBJECT"), rateHeader.getStringValue("OBJECT"), ""),
                    firstNonBlank(primaryHeader.getStringValue("OBS_ID"), rateHeader.getStringValue("OBS_ID"), ""),
                    firstNonBlank(primaryHeader.getStringValue("DATE-OBS"), rateHeader.getStringValue("DATE-OBS"), ""),
                    firstNonBlank(primaryHeader.getStringValue("DATE-END"), rateHeader.getStringValue("DATE-END"), ""),
                    firstNonBlank(primaryHeader.getStringValue("INSTRUME"), rateHeader.getStringValue("INSTRUME"), "BAT"),
                    firstNonBlank(primaryHeader.getStringValue("TELESCOP"), rateHeader.getStringValue("TELESCOP"), "SWIFT"));
        } catch (RuntimeException error) {
            throw new IOException("Il file FITS è stato scaricato ma non è stato interpretato correttamente.", error);
        }
    }

    private void addMetadata(List<MetadataItem> target, int hduIndex, String hduName, Header header) {
        Cursor<String, HeaderCard> cursor = header.iterator();
        while (cursor.hasNext()) {
            HeaderCard card = cursor.next();
            String keyword = nullSafe(card.getKey());
            String value = nullSafe(card.getValue());
            String comment = nullSafe(card.getComment());
            if (keyword.isBlank() && value.isBlank() && comment.isBlank()) {
                continue;
            }
            target.add(new MetadataItem(Integer.toString(hduIndex), hduName, keyword, value, comment));
        }
    }

    private Object valueAt(Object column, int row) {
        if (column == null || !column.getClass().isArray() || row < 0 || row >= Array.getLength(column)) {
            return null;
        }
        Object value = Array.get(column, row);
        while (value != null && value.getClass().isArray() && Array.getLength(value) == 1) {
            value = Array.get(value, 0);
        }
        return value;
    }

    private List<SummaryItem> buildSummary(
            CatalogEntry entry,
            ProductAvailability availability,
            TabularData ascii,
            FitsResult fits,
            String dataSource) {
        List<SummaryItem> result = new ArrayList<>();
        result.add(item("GRB_NAME", "Evento", entry.grbName(), "", "Nome identificativo del Gamma-Ray Burst."));
        result.add(item("TRIGGER_ID", "Trigger ID", entry.triggerId(), "", "Identificativo dell'allerta automatica Swift/BAT."));
        result.add(item("PRODUCT_STATUS", "Prodotti disponibili", availability.statusText(), "", "Indica quali prodotti a un secondo sono stati trovati online."));
        result.add(item("DATA_SOURCE", "Fonte", dataSource, "", "I dati provengono dai prodotti ufficiali Swift/BAT; la cache locale ne conserva una copia non modificata."));
        result.add(item("OBJECT", "Oggetto FITS", fits.objectName(), "", "Nome dell'oggetto registrato nell'intestazione FITS."));
        result.add(item("OBS_ID", "Observation ID", fits.observationId(), "", "Identificativo dell'osservazione Swift."));
        result.add(item("DATE_OBS", "Inizio osservazione", fits.dateObs(), "UTC", "Data e ora di inizio dell'osservazione."));
        result.add(item("DATE_END", "Fine osservazione", fits.dateEnd(), "UTC", "Data e ora di fine dell'osservazione."));
        result.add(item("MISSION_INSTRUMENT", "Missione / strumento", clean(fits.telescope() + " / " + fits.instrument()), "", "Satellite e rivelatore che hanno prodotto i dati."));
        result.add(item("BIN_SIZE", "Binning temporale", finite(fits.timeDelta()) ? format(fits.timeDelta()) : "1", "s", "Ampiezza di ogni intervallo temporale della curva."));
        result.add(item("ENERGY_RANGE", "Banda complessiva", "15–350", "keV", "Intervallo energetico coperto dal prodotto totale."));
        result.add(item("ASCII_ROWS", "Righe ASCII", Integer.toString(ascii.rows().size()), "bin", "Numero di intervalli temporali nel file a quattro canali."));
        result.add(item("FITS_ROWS", "Righe FITS", Integer.toString(fits.table().rows().size()), "bin", "Numero di intervalli temporali nel prodotto FITS aggregato."));

        TabularData metricData = !ascii.isEmpty() ? ascii : fits.table();
        String timeHeader = !ascii.isEmpty() ? "TIME_FROM_TRIGGER_CENTER_S" : "TIME_FROM_TRIGGER_CENTER_S";
        String rateHeader = !ascii.isEmpty() ? "RATE_15_350_KEV" : "RATE";
        String errorHeader = !ascii.isEmpty() ? "ERROR_15_350_KEV" : "ERROR";
        List<Double> times = numericColumn(metricData, timeHeader);
        List<Double> rates = numericColumn(metricData, rateHeader);
        List<Double> errors = numericColumn(metricData, errorHeader);

        int peakIndex = indexOfMax(rates);
        if (peakIndex >= 0) {
            double peak = rates.get(peakIndex);
            double peakTime = valueAt(times, peakIndex);
            double peakError = valueAt(errors, peakIndex);
            result.add(item("PEAK_RATE", "Picco del rate", format(peak), "count/s", "Valore massimo della curva totale a un secondo."));
            result.add(item("PEAK_TIME", "Tempo del picco", format(peakTime), "s dal trigger", "Istante del massimo rispetto al momento zero dell'allerta."));
            result.add(item("PEAK_ERROR", "Errore al picco", format(peakError), "count/s", "Incertezza statistica associata al bin del picco."));
            result.add(item("PEAK_SNR", "Rapporto picco/errore", peakError > 0 ? format(peak / peakError) : "n.d.", "", "Indicatore descrittivo di quanto il picco supera la propria incertezza."));
        }

        if (!rates.isEmpty()) {
            result.add(item("MEAN_RATE", "Rate medio", format(mean(rates)), "count/s", "Media aritmetica del rate nell'intera finestra scaricata."));
            result.add(item("RATE_STD", "Variabilità globale", format(sampleStd(rates)), "count/s", "Deviazione standard del rate; descrive quanto i valori oscillano nella finestra."));
            result.add(item("NEGATIVE_FRACTION", "Bin con rate negativo", formatPercent(fraction(rates, value -> value < 0)), "%", "Quota di bin sotto zero dopo la sottrazione del fondo."));
        }
        if (!times.isEmpty()) {
            result.add(item("TIME_RANGE", "Intervallo temporale", format(min(times)) + " → " + format(max(times)), "s dal trigger", "Finestra temporale coperta dai dati disponibili."));
        }

        if (!fits.table().isEmpty()) {
            List<Double> fracExp = numericColumn(fits.table(), "FRACEXP");
            if (!fracExp.isEmpty()) {
                result.add(item("FULL_EXPOSURE_FRACTION", "Bin con esposizione completa", formatPercent(fraction(fracExp, value -> value >= 0.999)), "%", "Quota di bin con FRACEXP praticamente uguale a 1."));
                result.add(item("MIN_FRACEXP", "Esposizione minima", format(min(fracExp)), "", "Valore FRACEXP più basso presente nel prodotto FITS."));
            }
        }

        if (!ascii.isEmpty()) {
            double hardness = computeHardnessProxy(ascii);
            result.add(item("HARDNESS_PROXY", "Durezza energetica (proxy)", finite(hardness) ? format(hardness) : "n.d.", "", "Rapporto descrittivo fra contributo positivo ad alte energie (50–350 keV) e basse energie (15–50 keV). Non è una classificazione ufficiale."));
        }

        result.add(item("ASCII_FILE", "File ASCII", availability.asciiFileName().isBlank() ? "Non disponibile" : availability.asciiFileName(), "", "Prodotto testuale con quattro bande energetiche e totale."));
        result.add(item("FITS_FILE", "File FITS", availability.fitsFileName().isBlank() ? "Non disponibile" : availability.fitsFileName(), "", "Prodotto scientifico binario con rate, errore, conteggi, esposizione e metadati."));
        result.add(item("DATA_PRODUCT_URL", "Data Product", availability.dataProductUrl(), "", "Pagina ufficiale dei prodotti scientifici dell'evento."));
        result.add(item("LC_DIRECTORY_URL", "Directory curve", availability.lightCurveDirectoryUrl(), "", "Directory online da cui vengono letti DAT e FITS."));
        return result;
    }

    private double computeHardnessProxy(TabularData ascii) {
        int low1 = ascii.indexOf("RATE_15_25_KEV");
        int low2 = ascii.indexOf("RATE_25_50_KEV");
        int high1 = ascii.indexOf("RATE_50_100_KEV");
        int high2 = ascii.indexOf("RATE_100_350_KEV");
        if (low1 < 0 || low2 < 0 || high1 < 0 || high2 < 0) {
            return Double.NaN;
        }
        double low = 0;
        double high = 0;
        for (List<String> row : ascii.rows()) {
            low += Math.max(0, parse(row.get(low1))) + Math.max(0, parse(row.get(low2)));
            high += Math.max(0, parse(row.get(high1))) + Math.max(0, parse(row.get(high2)));
        }
        return low > 0 ? high / low : Double.NaN;
    }

    private List<Double> numericColumn(TabularData data, String header) {
        int index = data.indexOf(header);
        if (index < 0) {
            return List.of();
        }
        List<Double> values = new ArrayList<>(data.rows().size());
        for (List<String> row : data.rows()) {
            if (index < row.size()) {
                double value = parse(row.get(index));
                if (finite(value)) {
                    values.add(value);
                } else {
                    values.add(Double.NaN);
                }
            }
        }
        return values;
    }

    public static List<FieldDefinition> dictionary() {
        List<FieldDefinition> result = new ArrayList<>();
        result.add(def("Tempo", "ASCII e FITS", "TIME_FROM_TRIGGER_CENTER_S", "s",
                "Quanti secondi prima o dopo il trigger si trova il centro del bin.",
                "Tempo relativo calcolato rispetto a TRIGTIME; il valore rappresenta il centro dell'intervallo di campionamento.",
                "Permette di allineare eventi diversi usando lo stesso momento zero.",
                "Un valore negativo indica un istante precedente al trigger, non un tempo fisicamente impossibile."));
        result.add(def("Tempo", "FITS", "TIME_FROM_TRIGGER_START_S", "s",
                "Quanti secondi prima o dopo il trigger inizia il bin.",
                "Differenza fra TIME MET e TRIGTIME prima dell'aggiunta di metà TIMEDEL.",
                "Serve quando è importante distinguere il bordo iniziale dal centro del bin.",
                "Con bin di 1 s, centro e inizio differiscono di circa 0,5 s."));
        result.add(def("Tempo", "FITS", "TIME_MET_S", "s",
                "L'orologio assoluto interno della missione Swift.",
                "Mission Elapsed Time: secondi trascorsi dall'epoca temporale definita nell'intestazione FITS.",
                "Consente di collegare la misura ad altri prodotti della stessa osservazione.",
                "Per leggere una curva è normalmente più intuitivo il tempo relativo al trigger."));
        result.add(def("Segnale", "FITS", "RATE", "count/s",
                "Quanto segnale netto viene misurato in quel secondo nella banda complessiva.",
                "Rate netto corretto per il fondo nella banda energetica dichiarata dal prodotto.",
                "È la grandezza principale disegnata nella curva di luce.",
                "Può essere negativo per fluttuazioni statistiche dopo la sottrazione del fondo."));
        result.add(def("Incertezza", "FITS", "ERROR", "count/s",
                "Quanto è incerto il valore RATE dello stesso bin.",
                "Errore statistico associato al rate stimato.",
                "Aiuta a capire se un picco è chiaramente distinto dal rumore.",
                "Non rappresenta da solo ogni possibile errore sistematico dello strumento."));
        result.add(def("Conteggi", "FITS", "TOTCOUNTS", "count",
                "Numero totale di eventi registrati nel bin prima dell'interpretazione finale.",
                "Conteggio strumentale totale associato all'intervallo temporale.",
                "È utile per controlli tecnici e per comprendere la statistica disponibile.",
                "Non coincide necessariamente con il rate netto, perché quest'ultimo include correzioni e sottrazione del fondo."));
        result.add(def("Qualità", "FITS", "FRACEXP", "frazione",
                "Quanta parte del secondo è stata realmente utilizzabile.",
                "Esposizione frazionaria del bin, normalmente compresa fra 0 e 1.",
                "Permette di riconoscere bin parziali o meno affidabili.",
                "1 significa esposizione completa; valori inferiori non significano automaticamente dato inutilizzabile."));

        addRatePair(result, "15–25", "fotoni relativamente meno energetici fra le quattro bande");
        addRatePair(result, "25–50", "seconda banda a bassa energia");
        addRatePair(result, "50–100", "banda intermedia-alta");
        addRatePair(result, "100–350", "banda più energetica del file ASCII");
        addRatePair(result, "15–350", "somma complessiva delle quattro bande");

        result.add(def("Metadato", "FITS", "TRIGTIME", "s MET",
                "Il momento zero scelto per l'allerta del GRB.",
                "Tempo missione al quale l'algoritmo BAT ha dichiarato il trigger.",
                "È il riferimento usato per trasformare l'orologio assoluto in secondi prima/dopo l'evento.",
                "Il trigger non è necessariamente l'inizio fisico esatto dell'emissione."));
        result.add(def("Metadato", "FITS", "TIMEDEL", "s",
                "La larghezza di ogni bin temporale.",
                "Passo di campionamento della curva di luce.",
                "Definisce il dettaglio temporale disponibile.",
                "Con 1 s, fenomeni molto più brevi vengono mediati nello stesso bin."));
        result.add(def("Metadato", "FITS", "OBJECT", "",
                "Nome dell'evento scritto nel file.",
                "Identificativo dell'oggetto osservato nel prodotto FITS.",
                "Serve a verificare che il file appartenga al GRB selezionato.",
                "La capitalizzazione può differire dal nome mostrato nel catalogo."));
        result.add(def("Metadato", "FITS", "OBS_ID", "",
                "Identificativo dell'osservazione Swift.",
                "Codice univoco usato nell'archivio della missione.",
                "Permette di rintracciare tutti i prodotti collegati alla stessa osservazione.",
                "Non va confuso con il Trigger ID."));
        result.add(def("Metadato", "FITS", "DATE-OBS", "UTC",
                "Data e ora in cui comincia il prodotto osservativo.",
                "Timestamp UTC di inizio dell'osservazione.",
                "Colloca temporalmente il dataset.",
                "Non indica necessariamente l'istante esatto del picco del GRB."));
        result.add(def("Metadato", "FITS", "DATE-END", "UTC",
                "Data e ora in cui termina il prodotto osservativo.",
                "Timestamp UTC di fine dell'intervallo coperto.",
                "Definisce l'estensione temporale osservata.",
                "La finestra può essere più lunga della fase interessante del burst."));
        result.add(def("Metadato", "FITS", "TELESCOP", "",
                "Il satellite o la missione che ha effettuato l'osservazione.",
                "Nome della piattaforma osservativa dichiarata nel FITS.",
                "Distingue la provenienza dei prodotti.",
                "Nel catalogo considerato il valore atteso è SWIFT."));
        result.add(def("Metadato", "FITS", "INSTRUME", "",
                "Lo strumento che ha raccolto i dati.",
                "Rivelatore responsabile del prodotto scientifico.",
                "È fondamentale per conoscere banda energetica e caratteristiche della misura.",
                "Qui il valore atteso è BAT."));
        result.add(def("Metadato", "FITS", "TSTART", "s MET",
                "Istante assoluto di inizio della tabella.",
                "Tempo missione del primo limite temporale del prodotto.",
                "Permette controlli temporali con altri file.",
                "È meno intuitivo del tempo relativo al trigger."));
        result.add(def("Metadato", "FITS", "TSTOP", "s MET",
                "Istante assoluto di fine della tabella.",
                "Tempo missione dell'ultimo limite temporale del prodotto.",
                "Permette di verificare la durata coperta.",
                "Non è la durata fisica T90 del GRB."));
        result.add(def("Metadato", "FITS", "EXPOSURE", "s",
                "Tempo effettivo complessivo di esposizione.",
                "Somma del tempo utile dopo le correzioni applicate dal prodotto.",
                "Aiuta a valutare quanta osservazione valida è disponibile.",
                "Può differire dalla semplice differenza TSTOP−TSTART."));
        result.add(def("Metadato", "FITS", "RA_OBJ / DEC_OBJ", "deg",
                "Coordinate celesti dell'evento.",
                "Ascensione retta e declinazione dell'oggetto nel sistema indicato dal FITS.",
                "Localizzano il GRB nel cielo.",
                "Non rappresentano una posizione sullo schermo o sulla Terra."));
        result.add(def("Metadato", "FITS", "EXTNAME", "",
                "Nome della sezione del file FITS.",
                "Etichetta dell'Header/Data Unit, per esempio RATE o EBOUNDS.",
                "Permette di capire quale blocco del file si sta leggendo.",
                "Un FITS può contenere più tabelle e intestazioni nello stesso file."));
        return List.copyOf(result);
    }

    private static void addRatePair(List<FieldDefinition> result, String band, String bandMeaning) {
        String token = band.replace("–", "_").replace("-", "_");
        result.add(def("Segnale per energia", "ASCII", "RATE_" + token + "_KEV", "count/s",
                "Intensità netta misurata nella banda " + band + " keV.",
                "Rate della curva di luce per " + bandMeaning + ".",
                "Confrontato con le altre bande mostra se l'emissione è più concentrata a basse o alte energie.",
                "Un valore negativo può comparire per la sottrazione del fondo e non significa conteggi fisicamente negativi."));
        result.add(def("Incertezza per energia", "ASCII", "ERROR_" + token + "_KEV", "count/s",
                "Incertezza del rate nella banda " + band + " keV.",
                "Errore statistico associato al RATE della stessa banda energetica.",
                "Serve per distinguere variazioni robuste da oscillazioni compatibili con il rumore.",
                "Va letto insieme al RATE dello stesso intervallo e della stessa banda."));
    }

    private static FieldDefinition def(String category, String source, String field, String unit,
                                       String simple, String technical, String importance, String caution) {
        return new FieldDefinition(category, source, field, unit, simple, technical, importance, caution);
    }

    private SummaryItem item(String key, String label, String value, String unit, String interpretation) {
        return new SummaryItem(key, label, clean(value), unit, interpretation);
    }

    private String clean(String value) {
        return value == null || value.isBlank() || value.equals(" / ") ? "n.d." : value.trim();
    }

    private double parse(String value) {
        if (value == null || value.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return parse(value == null ? "" : value.toString());
    }

    private String format(double value) {
        return finite(value) ? numberFormat.get().format(value) : "n.d.";
    }

    private String formatPercent(double fraction) {
        return finite(fraction) ? numberFormat.get().format(fraction * 100.0) : "n.d.";
    }

    private boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private int indexOfMax(List<Double> values) {
        int index = -1;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int current = 0; current < values.size(); current++) {
            double value = values.get(current);
            if (finite(value) && value > maximum) {
                maximum = value;
                index = current;
            }
        }
        return index;
    }

    private double valueAt(List<Double> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : Double.NaN;
    }

    private double mean(List<Double> values) {
        double sum = 0;
        int count = 0;
        for (double value : values) {
            if (finite(value)) {
                sum += value;
                count++;
            }
        }
        return count == 0 ? Double.NaN : sum / count;
    }

    private double sampleStd(List<Double> values) {
        double mean = mean(values);
        if (!finite(mean)) {
            return Double.NaN;
        }
        double sum = 0;
        int count = 0;
        for (double value : values) {
            if (finite(value)) {
                double delta = value - mean;
                sum += delta * delta;
                count++;
            }
        }
        return count < 2 ? Double.NaN : Math.sqrt(sum / (count - 1));
    }

    private double min(List<Double> values) {
        double result = Double.POSITIVE_INFINITY;
        for (double value : values) {
            if (finite(value)) {
                result = Math.min(result, value);
            }
        }
        return result == Double.POSITIVE_INFINITY ? Double.NaN : result;
    }

    private double max(List<Double> values) {
        double result = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            if (finite(value)) {
                result = Math.max(result, value);
            }
        }
        return result == Double.NEGATIVE_INFINITY ? Double.NaN : result;
    }

    private double fraction(List<Double> values, java.util.function.DoublePredicate predicate) {
        int count = 0;
        int matches = 0;
        for (double value : values) {
            if (finite(value)) {
                count++;
                if (predicate.test(value)) {
                    matches++;
                }
            }
        }
        return count == 0 ? Double.NaN : (double) matches / count;
    }

    private double firstFinite(double... values) {
        for (double value : values) {
            if (finite(value)) {
                return value;
            }
        }
        return Double.NaN;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }

    private record ProductLinks(
            String dataProductUrl,
            String resultsUrl,
            String lightCurveDirectoryUrl,
            String asciiUrl,
            String fitsUrl) {
    }

    private record FitsResult(
            TabularData table,
            List<MetadataItem> metadata,
            double triggerTime,
            double timeDelta,
            String objectName,
            String observationId,
            String dateObs,
            String dateEnd,
            String instrument,
            String telescope) {
        static FitsResult empty() {
            return new FitsResult(TabularData.empty(), List.of(), Double.NaN, Double.NaN,
                    "", "", "", "", "", "");
        }
    }
}
