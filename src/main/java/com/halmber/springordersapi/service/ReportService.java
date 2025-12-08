package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.ReportFileTypeEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import com.halmber.springordersapi.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    private final OrderRepository orderRepository;
    private final CsvReportService csvReportService;
    private final XlsxReportService xlsxReportService;

    /**
     * Generates a report file and writes it to the provided OutputStream.
     * Uses streaming to avoid loading all data into memory.
     * Must be called within a transactional context for stream query to work.
     */
    @Transactional(readOnly = true)
    public void generateReport(
            UUID customerId,
            StatusEnum status,
            PaymentEnum paymentMethod,
            ReportFileTypeEnum fileType,
            OutputStream outputStream
    ) {
        log.info("Generating {} report with filters: customerId={}, status={}, paymentMethod={}",
                fileType, customerId, status, paymentMethod);

        try {
            try (Stream<Order> ordersStream = orderRepository.streamByFilters(
                    customerId, status, paymentMethod)) {

                switch (fileType) {
                    case CSV -> generateCsvReport(ordersStream, outputStream);
                    case XLSX -> generateXlsxReport(ordersStream, outputStream);
                    default -> throw new IllegalArgumentException("Unsupported file type: " + fileType);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void generateCsvReport(Stream<Order> ordersStream, OutputStream outputStream) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
            csvReportService.generateReport(ordersStream, writer);
        }
    }

    private void generateXlsxReport(Stream<Order> ordersStream, OutputStream outputStream) throws IOException {
        xlsxReportService.generateReport(ordersStream, outputStream);
    }
}

