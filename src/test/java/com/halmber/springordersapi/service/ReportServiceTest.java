package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.ReportFileTypeEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import com.halmber.springordersapi.repository.OrderRepository;
import com.halmber.springordersapi.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("ReportService Unit Tests")
class ReportServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CsvReportService csvReportService;

    @Mock
    private XlsxReportService xlsxReportService;

    @Autowired
    private ReportService reportService;

    @Test
    @DisplayName("Should generate CSV report with filters")
    void shouldGenerateCsvReportWithFilters() throws IOException {
        UUID customerId = UUID.randomUUID();
        StatusEnum status = StatusEnum.NEW;
        PaymentEnum paymentMethod = PaymentEnum.CARD;
        OutputStream outputStream = new ByteArrayOutputStream();

        Customer customer = TestDataBuilder.createCustomer("John", "Doe", "john@test.com");
        Order order = TestDataBuilder.createOrder(customer, 100.0, StatusEnum.NEW);

        Stream<Order> orderStream = Stream.of(order);

        when(orderRepository.streamByFilters(customerId, status, paymentMethod))
                .thenReturn(orderStream);

        reportService.generateReport(customerId, status, paymentMethod,
                ReportFileTypeEnum.CSV, outputStream);

        verify(orderRepository).streamByFilters(customerId, status, paymentMethod);
        verify(csvReportService).generateReport(any(Stream.class), any());
        verify(xlsxReportService, never()).generateReport(any(), any());
    }

    @Test
    @DisplayName("Should generate XLSX report with filters")
    void shouldGenerateXlsxReportWithFilters() throws IOException {
        UUID customerId = UUID.randomUUID();
        OutputStream outputStream = new ByteArrayOutputStream();

        Customer customer = TestDataBuilder.createCustomer("Jane", "Smith", "jane@test.com");
        Order order = TestDataBuilder.createOrder(customer, 200.0, StatusEnum.PROCESSING);

        Stream<Order> orderStream = Stream.of(order);

        when(orderRepository.streamByFilters(customerId, null, null))
                .thenReturn(orderStream);

        reportService.generateReport(customerId, null, null,
                ReportFileTypeEnum.XLSX, outputStream);

        verify(orderRepository).streamByFilters(customerId, null, null);
        verify(xlsxReportService).generateReport(any(Stream.class), any());
        verify(csvReportService, never()).generateReport(any(), any());
    }

    @Test
    @DisplayName("Should generate report with null filters")
    void shouldGenerateReportWithNullFilters() throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();

        Customer customer = TestDataBuilder.createCustomer("Test", "User", "test@test.com");
        Order order = TestDataBuilder.createOrder(customer, 150.0, StatusEnum.NEW);

        Stream<Order> orderStream = Stream.of(order);

        when(orderRepository.streamByFilters(null, null, null))
                .thenReturn(orderStream);

        reportService.generateReport(null, null, null,
                ReportFileTypeEnum.CSV, outputStream);

        verify(orderRepository).streamByFilters(null, null, null);
        verify(csvReportService).generateReport(any(Stream.class), any());
    }

    @Test
    @DisplayName("Should throw exception for unsupported file type")
    void shouldThrowExceptionForUnsupportedFileType() {
        OutputStream outputStream = new ByteArrayOutputStream();

        Stream<Order> orderStream = Stream.empty();
        when(orderRepository.streamByFilters(any(), any(), any()))
                .thenReturn(orderStream);

        assertThatThrownBy(() -> {
            // Simulate unsupported file type scenario
            reportService.generateReport(null, null, null, null, outputStream);
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should handle IOException from CSV generation")
    void shouldHandleIoExceptionFromCsvGeneration() throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();

        Stream<Order> orderStream = Stream.empty();
        when(orderRepository.streamByFilters(any(), any(), any()))
                .thenReturn(orderStream);

        doThrow(new IOException("Write error"))
                .when(csvReportService).generateReport(any(), any());

        assertThatThrownBy(() ->
                reportService.generateReport(null, null, null,
                        ReportFileTypeEnum.CSV, outputStream)
        ).isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);

        verify(csvReportService).generateReport(any(), any());
    }

    @Test
    @DisplayName("Should handle IOException from XLSX generation")
    void shouldHandleIoExceptionFromXlsxGeneration() throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();

        Stream<Order> orderStream = Stream.empty();
        when(orderRepository.streamByFilters(any(), any(), any()))
                .thenReturn(orderStream);

        doThrow(new IOException("Write error"))
                .when(xlsxReportService).generateReport(any(), any());

        assertThatThrownBy(() ->
                reportService.generateReport(null, null, null,
                        ReportFileTypeEnum.XLSX, outputStream)
        ).isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);

        verify(xlsxReportService).generateReport(any(), any());
    }

    @Test
    @DisplayName("Should pass correct parameters to repository")
    void shouldPassCorrectParametersToRepository() throws IOException {
        UUID customerId = UUID.randomUUID();
        StatusEnum status = StatusEnum.DONE;
        PaymentEnum paymentMethod = PaymentEnum.PAYPAL;
        OutputStream outputStream = new ByteArrayOutputStream();

        Stream<Order> orderStream = Stream.empty();
        when(orderRepository.streamByFilters(customerId, status, paymentMethod))
                .thenReturn(orderStream);

        reportService.generateReport(customerId, status, paymentMethod,
                ReportFileTypeEnum.CSV, outputStream);

        ArgumentCaptor<UUID> customerIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<StatusEnum> statusCaptor = ArgumentCaptor.forClass(StatusEnum.class);
        ArgumentCaptor<PaymentEnum> paymentCaptor = ArgumentCaptor.forClass(PaymentEnum.class);

        verify(orderRepository).streamByFilters(
                customerIdCaptor.capture(),
                statusCaptor.capture(),
                paymentCaptor.capture()
        );

        assertThat(customerIdCaptor.getValue()).isEqualTo(customerId);
        assertThat(statusCaptor.getValue()).isEqualTo(status);
        assertThat(paymentCaptor.getValue()).isEqualTo(paymentMethod);
    }

    @Test
    @DisplayName("Should close stream after processing")
    void shouldCloseStreamAfterProcessing() throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();

        @SuppressWarnings("unchecked")
        Stream<Order> orderStream = mock(Stream.class);
        when(orderStream.onClose(any())).thenReturn(orderStream);

        when(orderRepository.streamByFilters(any(), any(), any()))
                .thenReturn(orderStream);

        reportService.generateReport(null, null, null,
                ReportFileTypeEnum.CSV, outputStream);

        verify(orderStream).close();
    }
}

