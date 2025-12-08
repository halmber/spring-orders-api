package com.halmber.springordersapi.util;

import com.halmber.springordersapi.model.OrderFilter;
import com.halmber.springordersapi.model.dto.request.customer.CustomerCreateDto;
import com.halmber.springordersapi.model.dto.request.customer.CustomerEditDto;
import com.halmber.springordersapi.model.dto.request.order.OrderCreateDto;
import com.halmber.springordersapi.model.dto.request.order.OrderEditDto;
import com.halmber.springordersapi.model.dto.request.order.OrderFilterDto;
import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;

import java.util.ArrayList;
import java.util.UUID;

public class TestDataBuilder {

    public static Customer createCustomer(String firstName, String lastName, String email) {
        return Customer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone("+380501234567")
                .city("Kharkiv")
                .orders(new ArrayList<>())
                .build();
    }

    public static CustomerCreateDto createCustomerDto(String firstName, String lastName, String email) {
        return CustomerCreateDto.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone("+380501234567")
                .city("Kharkiv")
                .build();
    }

    public static CustomerEditDto createCustomerEditDto(String firstName, String lastName) {
        return CustomerEditDto.builder()
                .firstName(firstName)
                .lastName(lastName)
                .phone("+380509876543")
                .city("Kyiv")
                .build();
    }

    public static Order createOrder(Customer customer, double amount, StatusEnum status) {
        Order order = Order.builder()
                .customer(customer)
                .amount(amount)
                .status(status)
                .paymentMethod(PaymentEnum.CARD)
                .build();

        customer.getOrders().add(order);
        return order;
    }

    public static OrderCreateDto createOrderDto(UUID customerId, double amount) {
        return OrderCreateDto.builder()
                .customerId(String.valueOf(customerId))
                .amount(amount)
                .status(String.valueOf(StatusEnum.NEW))
                .paymentMethod(String.valueOf(PaymentEnum.CARD))
                .build();
    }

    public static OrderEditDto createOrderEditDto(StatusEnum status, Double amount) {
        return OrderEditDto.builder()
                .status(String.valueOf(status))
                .amount(amount)
                .paymentMethod(String.valueOf(PaymentEnum.PAYPAL))
                .build();
    }

    public static OrderFilterDto createOrderFilterDto(UUID customerId, Integer page, Integer size) {
        return OrderFilterDto.builder()
                .customerId(String.valueOf(customerId))
                .status(null)
                .paymentMethod(null)
                .page(page)
                .size(size)
                .build();
    }

    public static OrderFilter createOrderFilter(UUID customerId, Integer page, Integer size) {
        return OrderFilter.builder()
                .customerId(customerId)
                .status(null)
                .paymentMethod(null)
                .page(page)
                .size(size)
                .build();
    }
}
