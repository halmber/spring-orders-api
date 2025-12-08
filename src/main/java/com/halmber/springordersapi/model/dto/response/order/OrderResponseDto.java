package com.halmber.springordersapi.model.dto.response.order;

import com.halmber.springordersapi.model.dto.response.customer.CustomerResponseDto;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record OrderResponseDto(
        UUID id,
        double amount,
        StatusEnum status,
        PaymentEnum paymentMethod,
//        Set<TagEnum> tags,
        Instant createdAt,
        CustomerResponseDto customer
) {
}
