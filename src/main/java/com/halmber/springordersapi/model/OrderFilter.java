package com.halmber.springordersapi.model;

import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import lombok.Builder;

import java.util.UUID;

@Builder
public record OrderFilter(
        UUID customerId,
        StatusEnum status,
        PaymentEnum paymentMethod,
        Integer page,
        Integer size
) {
}
