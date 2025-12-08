package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import com.halmber.springordersapi.repository.CustomerRepository;
import com.halmber.springordersapi.repository.OrderRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class XlsxReportServiceIntegrationTest {

    @Autowired
    private XlsxReportService xlsxReportService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;
    private Order testOrder1;
    private Order testOrder2;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+380501234567")
                .city("Kyiv")
                .build();
        testCustomer = customerRepository.saveAndFlush(testCustomer);

        testOrder1 = Order.builder()
                .customer(testCustomer)
                .amount(100.50)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.CARD)
                .build();
        testOrder1 = orderRepository.saveAndFlush(testOrder1);

        testOrder2 = Order.builder()
                .customer(testCustomer)
                .amount(250.00)
                .status(StatusEnum.PROCESSING)
                .paymentMethod(PaymentEnum.PAYPAL)
                .build();
        testOrder2 = orderRepository.saveAndFlush(testOrder2);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldCreateValidXlsxFile() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        byte[] xlsxBytes = outputStream.toByteArray();
        assertThat(xlsxBytes).isNotEmpty();

        // Verify it's a valid XLSX file
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("Orders");
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldHaveHeaderRow() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            assertThat(headerRow).isNotNull();
            assertThat(getCellValue(headerRow.getCell(0))).isEqualTo("Order ID");
            assertThat(getCellValue(headerRow.getCell(1))).isEqualTo("Customer ID");
            assertThat(getCellValue(headerRow.getCell(2))).isEqualTo("Customer Name");
            assertThat(getCellValue(headerRow.getCell(3))).isEqualTo("Email");
            assertThat(getCellValue(headerRow.getCell(4))).isEqualTo("Amount");
            assertThat(getCellValue(headerRow.getCell(5))).isEqualTo("Status");
            assertThat(getCellValue(headerRow.getCell(6))).isEqualTo("Payment Method");
            assertThat(getCellValue(headerRow.getCell(7))).isEqualTo("Created At");
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldContainOrderData() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);

            // Row 0 is header, rows 1 and 2 are data
            Row row1 = sheet.getRow(1);
            assertThat(row1).isNotNull();
            assertThat(getCellValue(row1.getCell(0))).isEqualTo(testOrder2.getId().toString());
            assertThat(getCellValue(row1.getCell(2))).isEqualTo("John Doe");
            assertThat(getCellValue(row1.getCell(3))).isEqualTo("john.doe@example.com");
            assertThat(row1.getCell(4).getNumericCellValue()).isEqualTo(250.00);
            assertThat(getCellValue(row1.getCell(5))).isEqualTo("PROCESSING");
            assertThat(getCellValue(row1.getCell(6))).isEqualTo("PAYPAL");

            Row row2 = sheet.getRow(2);
            assertThat(row2).isNotNull();
            assertThat(row2.getCell(4).getNumericCellValue()).isEqualTo(100.50);
            assertThat(getCellValue(row2.getCell(5))).isEqualTo("NEW");
            assertThat(getCellValue(row2.getCell(6))).isEqualTo("CARD");
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldHaveCorrectRowCount() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            // Header + 2 data rows
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldFilterByStatus() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, StatusEnum.NEW, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            // Header + 1 data row
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            Row row1 = sheet.getRow(1);
            assertThat(getCellValue(row1.getCell(5))).isEqualTo("NEW");
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldFilterByPaymentMethod() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(
                null, null, PaymentEnum.CARD)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            Row row1 = sheet.getRow(1);
            assertThat(getCellValue(row1.getCell(6))).isEqualTo("CARD");
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldFilterByCustomerId() throws Exception {
        // Create another customer with order
        Customer anotherCustomer = customerRepository.saveAndFlush(Customer.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("+380501234568")
                .city("Lviv")
                .build());

        orderRepository.saveAndFlush(Order.builder()
                .customer(anotherCustomer)
                .amount(500.00)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.CASH)
                .build());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(
                testCustomer.getId(), null, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            // Header + 2 orders from testCustomer
            assertThat(sheet.getLastRowNum()).isEqualTo(2);

            for (int i = 1; i <= 2; i++) {
                Row row = sheet.getRow(i);
                assertThat(getCellValue(row.getCell(2))).isEqualTo("John Doe");
            }
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldApplyHeaderStyling() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            Cell firstCell = headerRow.getCell(0);

            CellStyle style = firstCell.getCellStyle();
            assertThat(style).isNotNull();
            assertThat(style.getFillForegroundColor()).isNotEqualTo(IndexedColors.WHITE.getIndex());
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldApplyDataStyling() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(1);
            Cell cell = dataRow.getCell(0);

            CellStyle style = cell.getCellStyle();
            assertThat(style).isNotNull();
            assertThat(style.getBorderBottom()).isEqualTo(BorderStyle.THIN);
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldHandleEmptyResults() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Filter that returns no results
        try (Stream<Order> stream = orderRepository.streamByFilters(
                null, StatusEnum.CANCELED, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            // Only header row should be present
            assertThat(sheet.getLastRowNum()).isEqualTo(0);
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldHandleLargeDataset() throws Exception {
        // Create 100 orders
        for (int i = 0; i < 100; i++) {
            orderRepository.saveAndFlush(Order.builder()
                    .customer(testCustomer)
                    .amount(100.00 + i)
                    .status(StatusEnum.NEW)
                    .paymentMethod(PaymentEnum.CARD)
                    .build());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            // Header + 102 orders (2 from setup + 100 new)
            assertThat(sheet.getLastRowNum()).isEqualTo(102);
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldHandleMultipleFilters() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(
                testCustomer.getId(), StatusEnum.NEW, PaymentEnum.CARD)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1); // Header + 1 matching order

            Row row1 = sheet.getRow(1);
            assertThat(getCellValue(row1.getCell(0))).contains(testOrder1.getId().toString());
            assertThat(getCellValue(row1.getCell(5))).isEqualTo("NEW");
            assertThat(getCellValue(row1.getCell(6))).isEqualTo("CARD");
        }
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldFormatDateCorrectly() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            xlsxReportService.generateReport(stream, outputStream);
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row1 = sheet.getRow(1);
            String dateValue = getCellValue(row1.getCell(7));

            // Check date format (yyyy-MM-dd HH:mm:ss)
            assertThat(dateValue).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}

