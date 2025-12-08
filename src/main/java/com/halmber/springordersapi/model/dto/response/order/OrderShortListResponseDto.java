package com.halmber.springordersapi.model.dto.response.order;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderShortListResponseDto(
        List<OrderShortResponseDto> orders,
        long totalPages
) {
}
