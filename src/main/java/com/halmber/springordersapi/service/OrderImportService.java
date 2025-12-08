package com.halmber.springordersapi.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halmber.springordersapi.model.dto.request.order.OrderImportDto;
import com.halmber.springordersapi.model.dto.response.order.OrderImportResultDto;
import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import com.halmber.springordersapi.repository.CustomerRepository;
import com.halmber.springordersapi.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for importing orders from JSON files.
 * Uses streaming Jackson parser to handle large files efficiently without loading entire file into memory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderImportService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    /**
     * Imports orders from JSON file using streaming to avoid memory issues with large files.
     * Processes orders in batches for better database performance.
     *
     * @param file JSON file containing orders array
     * @return Import result with statistics and error details
     * @throws IOException if file cannot be read or parsed
     */
    @Transactional
    public OrderImportResultDto importOrders(MultipartFile file) throws IOException {
        validateFile(file);

        List<OrderImportResultDto.ImportError> errors = new ArrayList<>();
        List<Order> ordersToSave = new ArrayList<>();
        int lineNumber = 0;
        int totalRecords = 0;
        int successfulImports = 0;

        try (InputStream inputStream = file.getInputStream();
             JsonParser parser = objectMapper.createParser(inputStream)) {

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("Expected JSON array at root level");
            }

            // Process each order in the array
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                lineNumber++;
                totalRecords++;

                try {
                    OrderImportDto dto = parser.readValueAs(OrderImportDto.class);
                    Order order = processOrder(dto, lineNumber, errors);

                    if (order != null) {
                        ordersToSave.add(order);
                        successfulImports++;

                        // Batch insert every 50 records
                        if (ordersToSave.size() >= 50) {
                            orderRepository.saveAll(ordersToSave);
                            ordersToSave.clear();
                        }
                    }
                } catch (Exception e) {
                    errors.add(OrderImportResultDto.ImportError.builder()
                            .lineNumber(lineNumber)
                            .reason("Parse error")
                            .details(e.getMessage())
                            .build());
                    log.warn("Error parsing order at line {}: {}", lineNumber, e.getMessage());
                }
            }

            // Save remaining orders
            if (!ordersToSave.isEmpty()) {
                orderRepository.saveAll(ordersToSave);
            }
        }

        int failedImports = totalRecords - successfulImports;
        log.info("Import completed: {} total, {} successful, {} failed",
                totalRecords, successfulImports, failedImports);

        return OrderImportResultDto.builder()
                .totalRecords(totalRecords)
                .successfulImports(successfulImports)
                .failedImports(failedImports)
                .errors(errors)
                .build();
    }

    /**
     * Validates uploaded file.
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("Only JSON files are allowed");
        }

        // Check file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed size of 10MB"
            );
        }
    }

    /**
     * Processes a single order DTO and converts it to Order entity.
     * Validates all fields and checks if customer exists.
     *
     * @param dto        Order data from JSON
     * @param lineNumber Current line number for error reporting
     * @param errors     List to collect errors
     * @return Order entity if valid, null otherwise
     */
    private Order processOrder(
            OrderImportDto dto,
            int lineNumber,
            List<OrderImportResultDto.ImportError> errors
    ) {
        // Validate required fields
        if (dto.customerId() == null || dto.customerId().isBlank()) {
            errors.add(createError(lineNumber, "Missing customer ID", "customerId is required"));
            return null;
        }

        if (dto.amount() == null || dto.amount() <= 0) {
            errors.add(createError(lineNumber, "Invalid amount",
                    "Amount must be positive, got: " + dto.amount()));
            return null;
        }

        if (dto.status() == null || dto.status().isBlank()) {
            errors.add(createError(lineNumber, "Missing status", "status is required"));
            return null;
        }

        // Parse and validate customer ID
        UUID customerId;
        try {
            customerId = UUID.fromString(dto.customerId());
        } catch (IllegalArgumentException e) {
            errors.add(createError(lineNumber, "Invalid customer ID format",
                    "Expected UUID, got: " + dto.customerId()));
            return null;
        }

        // Check if customer exists
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            errors.add(createError(lineNumber, "Customer not found",
                    "No customer with ID: " + customerId));
            return null;
        }

        // Parse status enum
        StatusEnum status;
        try {
            status = StatusEnum.valueOf(dto.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.add(createError(lineNumber, "Invalid status",
                    "Unknown status: " + dto.status()));
            return null;
        }

        // Parse payment method enum (optional)
        PaymentEnum paymentMethod;
        try {
            paymentMethod = PaymentEnum.valueOf(dto.paymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.add(createError(lineNumber, "Invalid payment method",
                    "Unknown payment method: " + dto.paymentMethod()));
            return null;
        }

        // Create order entity
        return Order.builder()
                .customer(customer)
                .amount(dto.amount())
                .status(status)
                .paymentMethod(paymentMethod)
                .build();
    }

    private OrderImportResultDto.ImportError createError(int lineNumber, String reason, String details) {
        return OrderImportResultDto.ImportError.builder()
                .lineNumber(lineNumber)
                .reason(reason)
                .details(details)
                .build();
    }

}

