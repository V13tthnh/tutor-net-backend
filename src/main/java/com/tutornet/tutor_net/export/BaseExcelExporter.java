package com.tutornet.tutor_net.export;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.List;

public abstract class BaseExcelExporter<T> {

    // TEMPLATE METHOD
    public void export(List<T> dataList, HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(getSheetName());

            // Tạo Dòng Tiêu Đề (Header)
            createHeaderRow(sheet, workbook);

            // Đổ Dữ Liệu
            int rowCount = 1;
            for (T data : dataList) {
                Row row = sheet.createRow(rowCount++);
                writeDataRow(data, row, workbook);
            }

            // Auto-size cột cho đẹp
            for (int i = 0; i < getHeaders().length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 4. Ghi ra luồng phản hồi cho trình duyệt tải về
            workbook.write(response.getOutputStream());
        }
    }

    // Các hàm bắt buộc lớp con phải triển khai
    protected abstract String getSheetName();
    protected abstract String[] getHeaders();
    protected abstract void writeDataRow(T data, Row row, Workbook workbook);

    // Hàm phụ trợ dùng chung
    private void createHeaderRow(Sheet sheet, Workbook workbook) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        String[] headers = getHeaders();
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }
}