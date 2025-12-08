package com.halmber.springordersapi.model.dto.request.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CustomerEditDto(
        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 3, max = 100)
        String lastName,

        @NotBlank(message = "Phone is required")
        @Size(min = 3, max = 50)
        String phone,

        @NotBlank(message = "City is required")
        @Size(min = 3, max = 255) String city
) {
}