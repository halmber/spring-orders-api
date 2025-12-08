package com.halmber.springordersapi.model.dto.request.order;

import lombok.Builder;

/**
 * DTO for importing orders from JSON file.
 * Matches the format from the file parser.
 */
@Builder
public record OrderImportDto(
        String orderId,

        String customerId,

        Double amount,

        String status,

        String paymentMethod
) {
}