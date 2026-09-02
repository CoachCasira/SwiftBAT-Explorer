package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.CatalogEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SwiftCatalogService {
    public static final String CATALOG_URL = "https://swift.gsfc.nasa.gov/results/batgrbcat/";
    private static final String USER_AGENT = "SwiftBAT-Explorer/1.2.0 (academic thesis application; JavaFX)";

    public List<CatalogEntry> fetchCatalog() throws IOException {
        Document document = Jsoup.connect(CATALOG_URL)
                .userAgent(USER_AGENT)
                .timeout((int) Duration.ofSeconds(25).toMillis())
                .followRedirects(true)
                .get();
        return parseCatalog(document);
    }

    List<CatalogEntry> parseCatalog(Document document) throws IOException {
        Element catalogTable = null;
        for (Element table : document.select("table")) {
            String text = table.text().toLowerCase(Locale.ROOT);
            if (text.contains("grb name") && text.contains("trigger id")) {
                catalogTable = table;
                break;
            }
        }
        if (catalogTable == null) {
            throw new IOException("Tabella GRB non trovata nella pagina ufficiale Swift/BAT.");
        }

        List<CatalogEntry> entries = new ArrayList<>();
        for (Element row : catalogTable.select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 2) {
                continue;
            }
            String grbName = cells.get(0).text().trim().toUpperCase(Locale.ROOT);
            String triggerId = cells.get(1).text().replace("•", "").trim();
            if (!grbName.startsWith("GRB")) {
                continue;
            }
            Element dataProduct = row.selectFirst("a:matchesOwn((?i)Data Product)");
            String url = dataProduct == null ? "" : dataProduct.absUrl("href");
            entries.add(new CatalogEntry(grbName, triggerId, url));
        }

        if (entries.isEmpty()) {
            throw new IOException("La tabella Swift/BAT è stata trovata ma non contiene record leggibili.");
        }

        entries.sort(Comparator.comparing(CatalogEntry::grbName).reversed());
        return List.copyOf(entries);
    }

    public List<CatalogEntry> fallbackCatalog() {
        return List.of(
                entry("GRB250605A", "1321323"),
                entry("GRB250603A", "1320335"),
                entry("GRB250530C", "1319125"),
                entry("GRB250520A", "1315630"),
                entry("GRB250516A", "1314210"),
                entry("GRB250509A", "1311764"),
                entry("GRB250504A", "1310284"),
                entry("GRB250430A", "1308754"),
                entry("GRB250424A", "1306404"),
                entry("GRB250331C", "1299967"));
    }

    private CatalogEntry entry(String name, String trigger) {
        return new CatalogEntry(name, trigger, CATALOG_URL + name + "/data_product/");
    }
}
