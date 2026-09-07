package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.MetadataItem;
import it.casiraghi.swiftbat.model.ProductAvailability;
import it.casiraghi.swiftbat.model.TabularData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExcelExportServiceTest {
    @TempDir
    Path temporary;

    @Test
    void asciiWorkbookContainsOnlyAsciiSheetAndNumericCells() throws Exception {
        Path output = temporary.resolve("ascii.xlsx");
        new ExcelExportService().exportAscii(data(), output);
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(output))) {
            assertEquals(1, workbook.getNumberOfSheets());
            assertNotNull(workbook.getSheet("ASCII_4CH_1S"));
            assertEquals(1.25, workbook.getSheetAt(0).getRow(1).getCell(1).getNumericCellValue(), 1e-12);
        }
    }

    @Test
    void fitsWorkbookKeepsDataAndMetadataTogether() throws Exception {
        Path output = temporary.resolve("fits.xlsx");
        new ExcelExportService().exportFitsAndMetadata(data(), output);
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(output))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertNotNull(workbook.getSheet("FITS_1CH_1S"));
            assertEquals("TRIGTIME", workbook.getSheet("METADATI_FITS").getRow(1).getCell(2).getStringCellValue());
        }
    }

    private GrbData data() {
        TabularData ascii = new TabularData(
                List.of("TIME_FROM_TRIGGER_CENTER_S", "RATE_15_350_KEV"),
                List.of(List.of("0.5", "1.25")));
        TabularData fits = new TabularData(
                List.of("TIME_FROM_TRIGGER_CENTER_S", "FRACEXP"),
                List.of(List.of("0.5", "1")));
        return new GrbData("GRBTESTA", "1", Instant.EPOCH,
                new ProductAvailability(true, true, "a.dat", "a.lc", "", "", ""),
                List.of(), ascii, fits,
                List.of(new MetadataItem("1", "RATE", "TRIGTIME", "100.5", "Trigger time")),
                List.of());
    }
}
