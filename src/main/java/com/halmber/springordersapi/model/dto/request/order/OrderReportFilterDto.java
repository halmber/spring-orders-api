package com.halmber.springordersapi.model.dto.request.order;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record OrderReportFilterDto(
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "'customerId' must be a valid UUID")
        String customerId,

        String status,

        String paymentMethod,

        @Pattern(regexp = "^(csv|xlsx)$", message = "File type must be 'csv' or 'xlsx'")
        String fileType
) {
}
