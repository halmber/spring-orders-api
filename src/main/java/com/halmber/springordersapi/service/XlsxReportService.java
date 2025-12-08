package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Service for generating XLSX (Excel) reports from order data.
 * Uses Apache POI's SXSSFWorkbook for memory-efficient streaming write operations.
 *
 * <p>Report columns:
 * <ol>
 *   <li>Order ID</li>
 *   <li>Customer ID</li>
 *   <li>Customer Name</li>
 *   <li>Email</li>
 *   <li>Amount</li>
 *   <li>Status</li>
 *   <li>Payment Method</li>
 *   <li>Created At</li>
 * </ol>
 */
@Slf4j
@Service
public class XlsxReportService {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final String[] HEADERS = {
            "Order ID", "Customer ID", "Customer Name", "Email",
            "Amount", "Status", "Payment Method", "Created At"
    };

    /**
     * Generates XLSX report using SXSSFWorkbook for memory-efficient streaming.
     * Only keeps 100 rows in memory at a time.
     */
    public void generateReport(Stream<Order> ordersStream, OutputStream outputStream) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Orders");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            createHeaderRow(sheet, headerStyle);

            final int[] rowNum = {1};
            ordersStream.forEach(order -> {
                Row row = sheet.createRow(rowNum[0]++);
                fillOrderRow(row, order, dataStyle);
            });

            workbook.write(outputStream);
            outputStream.flush();

            workbook.dispose();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillOrderRow(Row row, Order order, CellStyle dataStyle) {
        int colNum = 0;

        createCell(row, colNum++, order.getId().toString(), dataStyle);
        createCell(row, colNum++, order.getCustomer().getId().toString(), dataStyle);
        createCell(row, colNum++, order.getCustomer().getFullName(), dataStyle);
        createCell(row, colNum++, order.getCustomer().getEmail(), dataStyle);
        createCell(row, colNum++, order.getAmount(), dataStyle);
        createCell(row, colNum++, order.getStatus().name(), dataStyle);
        createCell(row, colNum++,
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "",
                dataStyle);
        createCell(row, colNum++, DATE_FORMATTER.format(order.getCreatedAt()), dataStyle);
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}

