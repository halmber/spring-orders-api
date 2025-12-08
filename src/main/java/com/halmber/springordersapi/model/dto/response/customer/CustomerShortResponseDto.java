package com.halmber.springordersapi.model.dto.response.customer;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CustomerShortResponseDto(
        UUID id,
        String fullName,
        String email
) {
}
