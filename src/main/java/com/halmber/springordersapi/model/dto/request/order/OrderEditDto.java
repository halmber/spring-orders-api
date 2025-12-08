package com.halmber.springordersapi.model.dto.request.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record OrderEditDto(
        @NotNull(message = "Status is required")
        String status,

        @NotNull(message = "Payment method is required")
        String paymentMethod,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        Double amount
) {
}
