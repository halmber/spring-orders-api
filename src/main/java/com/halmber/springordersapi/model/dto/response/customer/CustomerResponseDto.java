package com.halmber.springordersapi.model.dto.response.customer;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CustomerResponseDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String city
) {

}
