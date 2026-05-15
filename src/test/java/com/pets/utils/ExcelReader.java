package com.pets.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ExcelReader {

    private static final String DATASET_PATH = "/dataset/Dataset-Escenarios-PETS-SA.xlsx";

    /**
     * Devuelve la fila cuyo valor en la columna ${dataset_id} coincide con datasetId.
     * @param sheetName  nombre de la hoja (ej. "ESC01_XSS_SQLI")
     * @param datasetId  valor a buscar (ej. "C-I-05")
     * @return Map columna → valor (todas las celdas como String)
     */
    public static Map<String, String> getRowById(String sheetName, String datasetId)
            throws Exception {

        try (InputStream is = ExcelReader.class.getResourceAsStream(DATASET_PATH);
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Hoja no encontrada: " + sheetName);
            }

            Row headerRow = null;
            int datasetIdColIndex = -1;

            // Buscar la fila de encabezados (contiene "${dataset_id}")
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if ("${dataset_id}".equals(getCellValue(cell))) {
                        headerRow = row;
                        datasetIdColIndex = cell.getColumnIndex();
                        break;
                    }
                }
                if (headerRow != null) break;
            }

            if (headerRow == null) {
                throw new IllegalStateException("No se encontró fila de encabezado en: " + sheetName);
            }

            // Recoger nombres de columnas
            Map<Integer, String> headers = new HashMap<>();
            for (Cell cell : headerRow) {
                headers.put(cell.getColumnIndex(), getCellValue(cell));
            }

            // Buscar la fila con el dataset_id buscado
            for (Row row : sheet) {
                if (row.getRowNum() <= headerRow.getRowNum()) continue;
                Cell idCell = row.getCell(datasetIdColIndex);
                if (idCell != null && datasetId.equals(getCellValue(idCell))) {
                    Map<String, String> result = new HashMap<>();
                    for (Map.Entry<Integer, String> entry : headers.entrySet()) {
                        Cell cell = row.getCell(entry.getKey());
                        result.put(entry.getValue(), cell != null ? getCellValue(cell) : "");
                    }
                    return result;
                }
            }

            throw new IllegalArgumentException(
                    "dataset_id '" + datasetId + "' no encontrado en hoja: " + sheetName);
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getStringCellValue().trim();
            default      -> "";
        };
    }
}