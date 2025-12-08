package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import com.halmber.springordersapi.repository.CustomerRepository;
import com.halmber.springordersapi.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CsvReportServiceIntegrationTest {

    @Autowired
    private CsvReportService csvReportService;

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
    void generateReport_shouldCreateCsvWithHeaders() throws Exception {
        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertThat(lines[0]).contains("Order ID", "Customer ID", "Customer Name",
                "Email", "Amount", "Status", "Payment Method", "Created At");
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldContainOrderData() throws Exception {
        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();

        assertThat(csv).contains(testOrder1.getId().toString());
        assertThat(csv).contains(testOrder2.getId().toString());
        assertThat(csv).contains(testCustomer.getId().toString());
        assertThat(csv).contains("John Doe");
        assertThat(csv).contains("john.doe@example.com");
        assertThat(csv).contains("100.5");
        assertThat(csv).contains("250.0");
        assertThat(csv).contains("NEW");
        assertThat(csv).contains("PROCESSING");
        assertThat(csv).contains("CARD");
        assertThat(csv).contains("PAYPAL");
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldHaveCorrectNumberOfLines() throws Exception {
        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        // Header + 2 orders
        assertThat(lines).hasSize(3);
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldFilterByStatus() throws Exception {
        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, StatusEnum.NEW, null)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertThat(lines).hasSize(2); // Header + 1 order
        assertThat(csv).contains("NEW");
        assertThat(csv).doesNotContain("PROCESSING");
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldFilterByPaymentMethod() throws Exception {
        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(
                null, null, PaymentEnum.CARD)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertThat(lines).hasSize(2); // Header + 1 order
        assertThat(csv).contains("CARD");
        assertThat(csv).doesNotContain("PAYPAL");
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

        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(
                testCustomer.getId(), null, null)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertThat(lines).hasSize(3); // Header + 2 orders from testCustomer
        assertThat(csv).contains("John Doe");
        assertThat(csv).doesNotContain("Jane Smith");
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldEscapeCommasInValues() throws Exception {
        // Create customer with comma in name
        Customer customerWithComma = customerRepository.save(Customer.builder()
                .firstName("Smith, Jr.")
                .lastName("Johnson")
                .email("smith.johnson@example.com")
                .phone("+380501234569")
                .city("Kharkiv")
                .build());

        orderRepository.saveAndFlush(Order.builder()
                .customer(customerWithComma)
                .amount(300.00)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.CARD)
                .build());

        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(
                customerWithComma.getId(), null, null)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();

        // Value with comma should be wrapped in quotes
        assertThat(csv).contains("\"Smith, Jr. Johnson\"");
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldHandleEmptyResults() throws Exception {
        StringWriter writer = new StringWriter();

        // Filter that returns no results
        try (Stream<Order> stream = orderRepository.streamByFilters(
                null, StatusEnum.CANCELED, null)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        // Only header should be present
        assertThat(lines).hasSize(1);
        assertThat(lines[0]).contains("Order ID");
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

        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        // Header + 102 orders (2 from setup + 100 new)
        assertThat(lines).hasSize(103);
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldFormatDateCorrectly() throws Exception {
        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(null, null, null)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();

        // Check date format (yyyy-MM-dd HH:mm:ss)
        assertThat(csv).containsPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    @Transactional(readOnly = true)
    void generateReport_shouldHandleMultipleFilters() throws Exception {
        StringWriter writer = new StringWriter();

        try (Stream<Order> stream = orderRepository.streamByFilters(
                testCustomer.getId(), StatusEnum.NEW, PaymentEnum.CARD)) {
            csvReportService.generateReport(stream, writer);
        }

        String csv = writer.toString();
        String[] lines = csv.split("\n");

        assertThat(lines).hasSize(2); // Header + 1 matching order
        assertThat(csv).contains(testOrder1.getId().toString());
        assertThat(csv).contains("NEW");
        assertThat(csv).contains("CARD");
    }
}

