package com.halmber.springordersapi.model.dto.response.customer;

import lombok.Builder;

import java.util.List;

@Builder
public record CustomerListResponseDto(
        List<CustomerResponseDto> customers,
        long totalPages
) {
}
