package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Service for generating CSV (Comma-Separated Values) reports from order data.
 * Produces standard CSV files with proper escaping and formatting.
 *
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
public class CsvReportService {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final String[] HEADERS = {
            "Order ID", "Customer ID", "Customer Name", "Email",
            "Amount", "Status", "Payment Method", "Created At"
    };

    public void generateReport(Stream<Order> ordersStream, Writer writer) throws IOException {
        writer.write(String.join(",", HEADERS));
        writer.write("\n");

        ordersStream.forEach(order -> {
            try {
                writeOrderRow(writer, order);
            } catch (IOException e) {
                log.error("Error writing order to CSV: {}", order.getId(), e);
                throw new RuntimeException("Failed to write order to CSV", e);
            }
        });

        writer.flush();
    }

    private void writeOrderRow(Writer writer, Order order) throws IOException {
        String[] values = {
                escapeCsv(order.getId().toString()),
                escapeCsv(order.getCustomer().getId().toString()),
                escapeCsv(order.getCustomer().getFullName()),
                escapeCsv(order.getCustomer().getEmail()),
                String.valueOf(order.getAmount()),
                escapeCsv(order.getStatus().name()),
                escapeCsv(order.getPaymentMethod().name()),
                escapeCsv(DATE_FORMATTER.format(order.getCreatedAt()))
        };

        writer.write(String.join(",", values));
        writer.write("\n");
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

