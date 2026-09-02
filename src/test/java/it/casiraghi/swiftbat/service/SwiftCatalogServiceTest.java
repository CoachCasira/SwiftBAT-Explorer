package it.casiraghi.swiftbat.service;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwiftCatalogServiceTest {
    @Test
    void parsesCatalogRows() throws Exception {
        String html = """
                <html><body><table>
                <tr><th>GRB Name</th><th>Trigger ID</th><th>Results</th></tr>
                <tr><td>GRB250605A</td><td>1321323</td><td><a href='GRB250605A/data_product/'>Data Product</a></td></tr>
                </table></body></html>
                """;
        var result = new SwiftCatalogService().parseCatalog(
                Jsoup.parse(html, SwiftCatalogService.CATALOG_URL));
        assertEquals(1, result.size());
        assertEquals("GRB250605A", result.get(0).grbName());
        assertEquals("1321323", result.get(0).triggerId());
    }
}
