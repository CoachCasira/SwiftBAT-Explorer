package it.casiraghi.swiftbat.ui;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Window;
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

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/** Utility condivise per esportazioni visuali e tabellari dell'interfaccia. */
public final class ExportSupport {
    private ExportSupport() {}

    public static void exportPng(Node owner, Node content, String suggestedName) {
        if (content == null) return;
        File file = choose(owner, suggestedName, "PNG", "*.png");
        if (file == null) return;
        try {
            content.applyCss();
            if (content instanceof Parent parent) parent.layout();
            WritableImage image = content.snapshot(new SnapshotParameters(), null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            show(owner, Alert.AlertType.INFORMATION,
                    I18n.dynamic("Esportazione completata", "Export completed"),
                    file.getAbsolutePath());
        } catch (Exception error) {
            show(owner, Alert.AlertType.ERROR,
                    I18n.dynamic("Esportazione PNG non riuscita", "PNG export failed"),
                    error.getMessage());
        }
    }

    public static void exportSwingPng(Node owner, JComponent content, String suggestedName) {
        if (content == null) return;
        File file = choose(owner, suggestedName, "PNG", "*.png");
        if (file == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                int width = Math.max(1, content.getWidth());
                int height = Math.max(1, content.getHeight());
                if (width <= 1 || height <= 1) {
                    Dimension preferred = content.getPreferredSize();
                    width = Math.max(1, preferred.width);
                    height = Math.max(1, preferred.height);
                    content.setSize(width, height);
                }
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                content.printAll(graphics);
                graphics.dispose();
                ImageIO.write(image, "png", file);
                Platform.runLater(() -> show(owner, Alert.AlertType.INFORMATION,
                        I18n.dynamic("Esportazione completata", "Export completed"),
                        file.getAbsolutePath()));
            } catch (Exception error) {
                Platform.runLater(() -> show(owner, Alert.AlertType.ERROR,
                        I18n.dynamic("Esportazione PNG non riuscita", "PNG export failed"),
                        error.getMessage()));
            }
        });
    }

    public static void exportTableExcel(Node owner, TableView<?> table,
                                        String suggestedName, String sheetName) {
        if (table == null) return;
        File file = choose(owner, suggestedName, "Excel", "*.xlsx");
        if (file == null) return;
        List<TableColumn<?, ?>> columns = TablePreferences.visibleLeafColumns(table);
        if (columns.isEmpty()) {
            show(owner, Alert.AlertType.WARNING,
                    I18n.dynamic("Nessuna colonna visibile", "No visible columns"),
                    I18n.dynamic("Mostra almeno una colonna prima di esportare.",
                            "Show at least one column before exporting."));
            return;
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName == null || sheetName.isBlank() ? "DATA" : sheetName);
            CellStyle headerStyle = headerStyle(workbook);
            Row header = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                Cell cell = header.createCell(columnIndex);
                cell.setCellValue(TablePreferences.exportColumnName(columns.get(columnIndex)));
                cell.setCellStyle(headerStyle);
            }
            for (int rowIndex = 0; rowIndex < table.getItems().size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    Object value = cellValue(columns.get(columnIndex), rowIndex);
                    writeTypedCell(row.createCell(columnIndex), value);
                }
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, table.getItems().size()), 0, columns.size() - 1));
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
                sheet.setColumnWidth(columnIndex, Math.min(sheet.getColumnWidth(columnIndex) + 600, 18_000));
            }
            try (FileOutputStream output = new FileOutputStream(file)) {
                workbook.write(output);
            }
            show(owner, Alert.AlertType.INFORMATION,
                    I18n.dynamic("Esportazione completata", "Export completed"),
                    file.getAbsolutePath());
        } catch (Exception error) {
            show(owner, Alert.AlertType.ERROR,
                    I18n.dynamic("Esportazione Excel non riuscita", "Excel export failed"),
                    error.getMessage());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object cellValue(TableColumn<?, ?> column, int rowIndex) {
        return ((TableColumn) column).getCellData(rowIndex);
    }

    private static void writeTypedCell(Cell cell, Object raw) {
        if (raw instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        String value = raw == null ? "" : raw.toString().trim();
        if (TablePreferences.isNumeric(value)) {
            try {
                double numeric = Double.parseDouble(value.replace(',', '.').replace("%", ""));
                if (Double.isFinite(numeric)) {
                    cell.setCellValue(numeric);
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        cell.setCellValue(value);
    }

    private static CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static File choose(Node owner, String suggestedName, String description, String extensionPattern) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.dynamic("Esporta", "Export"));
        chooser.setInitialFileName(suggestedName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensionPattern));
        Window window = owner == null || owner.getScene() == null ? null : owner.getScene().getWindow();
        File selected = chooser.showSaveDialog(window);
        if (selected == null) return null;
        String expected = extensionPattern.substring(1);
        if (!selected.getName().toLowerCase().endsWith(expected.toLowerCase())) {
            selected = new File(selected.getParentFile(), selected.getName() + expected);
        }
        return selected;
    }

    private static void show(Node owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "" : message);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) {
            alert.initOwner(owner.getScene().getWindow());
        }
        alert.showAndWait();
    }
}
