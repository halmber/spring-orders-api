package com.halmber.springordersapi.model.dto.request.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record OrderCreateDto(
        @NotNull(message = "Customer ID is required")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "'customerId' must be a valid UUID")
        String customerId,

        @NotNull(message = "Status is required")
        String status,

        @NotNull(message = "Payment method is required")
        String paymentMethod,

//        @Valid
//        Set<TagEnum> tags,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        Double amount
) {
}
