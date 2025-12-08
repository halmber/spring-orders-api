package com.halmber.springordersapi.model.dto.response.order;

import com.halmber.springordersapi.model.dto.response.customer.CustomerShortResponseDto;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record OrderShortResponseDto(
        UUID id,
        double amount,
        StatusEnum status,
        PaymentEnum paymentMethod,
        Instant createdAt,
        CustomerShortResponseDto customer
) {
}
