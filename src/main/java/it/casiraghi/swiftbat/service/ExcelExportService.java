package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.GrbData;
import it.casiraghi.swiftbat.model.MetadataItem;
import it.casiraghi.swiftbat.model.TabularData;
import it.casiraghi.swiftbat.ui.TablePreferences;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Esporta prodotti già interpretati dall'app in cartelle Excel autonome. */
public final class ExcelExportService {
    public void exportAscii(GrbData data, Path destination) throws IOException {
        if (data == null || data.asciiData().isEmpty()) {
            throw new IOException("Il prodotto ASCII non è disponibile per questo GRB.");
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            writeTable(workbook, "ASCII_4CH_1S", data.asciiData(), "explorer.data." + data.grbName());
            write(workbook, destination);
        }
    }

    public void exportFitsAndMetadata(GrbData data, Path destination) throws IOException {
        if (data == null || data.fitsData().isEmpty()) {
            throw new IOException("Il prodotto FITS non è disponibile per questo GRB.");
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            writeTable(workbook, "FITS_1CH_1S", data.fitsData(), "explorer.data." + data.grbName());
            writeMetadata(workbook, data.metadata(), "explorer.metadata." + data.grbName());
            write(workbook, destination);
        }
    }

    private void writeTable(Workbook workbook, String name, TabularData data, String tableKey) {
        Sheet sheet = workbook.createSheet(name);
        CellStyle headerStyle = headerStyle(workbook);
        List<Integer> visibleColumns = new ArrayList<>();
        for (int sourceColumn = 0; sourceColumn < data.headers().size(); sourceColumn++) {
            String headerName = data.headers().get(sourceColumn);
            if (TablePreferences.isColumnVisible(tableKey, headerName)) visibleColumns.add(sourceColumn);
        }

        Row header = sheet.createRow(0);
        for (int outputColumn = 0; outputColumn < visibleColumns.size(); outputColumn++) {
            Cell cell = header.createCell(outputColumn);
            cell.setCellValue(data.headers().get(visibleColumns.get(outputColumn)));
            cell.setCellStyle(headerStyle);
        }
        for (int rowIndex = 0; rowIndex < data.rows().size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<String> values = data.rows().get(rowIndex);
            for (int outputColumn = 0; outputColumn < visibleColumns.size(); outputColumn++) {
                int sourceColumn = visibleColumns.get(outputColumn);
                String value = sourceColumn < values.size() ? values.get(sourceColumn) : "";
                writeTypedCell(row.createCell(outputColumn), value);
            }
        }
        finishSheet(sheet, visibleColumns.size(), data.rows().size());
    }

    private void writeMetadata(Workbook workbook, List<MetadataItem> metadata, String tableKey) {
        Sheet sheet = workbook.createSheet("FITS_METADATA");
        CellStyle headerStyle = headerStyle(workbook);
        boolean showHdu = TablePreferences.isColumnVisible(tableKey, "HDU");
        boolean showKeyword = TablePreferences.isColumnVisible(tableKey, "Keyword");
        boolean showValue = TablePreferences.isColumnVisible(tableKey, "Valore");
        boolean showComment = TablePreferences.isColumnVisible(tableKey, "Commento originale");

        List<String> headers = new ArrayList<>();
        if (showHdu) {
            headers.add("HDU_INDEX");
            headers.add("HDU_NAME");
        }
        if (showKeyword) headers.add("KEYWORD");
        if (showValue) headers.add("VALUE");
        if (showComment) headers.add("COMMENT");

        Row header = sheet.createRow(0);
        for (int index = 0; index < headers.size(); index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(headers.get(index));
            cell.setCellStyle(headerStyle);
        }
        for (int index = 0; index < metadata.size(); index++) {
            MetadataItem item = metadata.get(index);
            Row row = sheet.createRow(index + 1);
            int column = 0;
            if (showHdu) {
                row.createCell(column++).setCellValue(item.hduIndex());
                row.createCell(column++).setCellValue(item.hduName());
            }
            if (showKeyword) row.createCell(column++).setCellValue(item.keyword());
            if (showValue) row.createCell(column++).setCellValue(item.value());
            if (showComment) row.createCell(column).setCellValue(item.comment());
        }
        finishSheet(sheet, headers.size(), metadata.size());
    }

    private void finishSheet(Sheet sheet, int columns, int dataRows) {
        sheet.createFreezePane(0, 1);
        if (columns > 0) {
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, dataRows), 0, columns - 1));
        }
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
            int width = Math.min(sheet.getColumnWidth(column) + 600, 18_000);
            sheet.setColumnWidth(column, width);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeTypedCell(Cell cell, String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!value.isEmpty()) {
            try {
                double numeric = Double.parseDouble(value);
                if (Double.isFinite(numeric)) {
                    cell.setCellValue(numeric);
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        cell.setCellValue(value);
    }

    private void write(Workbook workbook, Path destination) throws IOException {
        if (destination == null) {
            throw new IOException("Percorso di esportazione non valido.");
        }
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (OutputStream output = Files.newOutputStream(destination)) {
            workbook.write(output);
        }
    }
}
