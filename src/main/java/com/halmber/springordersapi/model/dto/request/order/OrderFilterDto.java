package com.halmber.springordersapi.model.dto.request.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record OrderFilterDto(

        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "'customerId' must be a valid UUID")
        String customerId,

        String status,

        String paymentMethod,

        @Min(value = 0, message = "Page must be >= 0")
        Integer page,

        @Min(value = 1, message = "Size must be >= 1")
        @Max(value = 100, message = "Size must be <= 100")
        Integer size
) {
}
