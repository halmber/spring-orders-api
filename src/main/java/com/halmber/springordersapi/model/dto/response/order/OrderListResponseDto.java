package com.halmber.springordersapi.model.dto.response.order;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderListResponseDto(
        List<OrderResponseDto> orders,
        long totalPages
) {
}
