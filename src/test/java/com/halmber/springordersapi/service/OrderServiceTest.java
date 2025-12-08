package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.OrderFilter;
import com.halmber.springordersapi.model.dto.request.order.OrderCreateDto;
import com.halmber.springordersapi.model.dto.request.order.OrderEditDto;
import com.halmber.springordersapi.model.dto.request.order.OrderFilterDto;
import com.halmber.springordersapi.model.dto.response.order.OrderResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderShortListResponseDto;
import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.StatusEnum;
import com.halmber.springordersapi.model.mapper.OrderMapper;
import com.halmber.springordersapi.repository.OrderRepository;
import com.halmber.springordersapi.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderMapper mapper;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Should get order by id successfully")
    void shouldGetOrderById() {
        UUID orderId = UUID.randomUUID();
        Order order = TestDataBuilder.createOrder(
                TestDataBuilder.createCustomer("John", "Doe", "john@test.com"),
                100.0,
                StatusEnum.NEW
        );
        order.setId(orderId);

        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(orderId)
                .amount(100.0)
                .status(StatusEnum.NEW)
                .build();

        when(repository.findById(orderId)).thenReturn(Optional.of(order));
        when(mapper.toResponse(order)).thenReturn(responseDto);

        OrderResponseDto result = orderService.getById(orderId);

        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.amount()).isEqualTo(100.0);
        verify(repository).findById(orderId);
        verify(mapper).toResponse(order);
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");

        verify(repository).findById(orderId);
    }

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrder() {
        UUID customerId = UUID.randomUUID();
        Customer customer = TestDataBuilder.createCustomer("John", "Doe", "john@test.com");
        customer.setId(customerId);

        OrderCreateDto createDto = TestDataBuilder.createOrderDto(customerId, 100.0);
        Order order = TestDataBuilder.createOrder(customer, 100.0, StatusEnum.NEW);
        order.setId(UUID.randomUUID());

        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(order.getId())
                .amount(100.0)
                .status(StatusEnum.NEW)
                .build();

        when(customerService.findByIdOrThrow(customerId)).thenReturn(customer);
        when(mapper.toEntity(createDto)).thenReturn(order);
        when(repository.saveAndFlush(order)).thenReturn(order);
        when(mapper.toResponse(order)).thenReturn(responseDto);

        OrderResponseDto result = orderService.create(createDto);

        assertThat(result.amount()).isEqualTo(100.0);
        verify(customerService).findByIdOrThrow(customerId);
        verify(repository).saveAndFlush(order);
    }

    @Test
    @DisplayName("Should throw exception when creating order with non-existent customer")
    void shouldThrowExceptionWhenCreatingOrderWithNonExistentCustomer() {
        UUID customerId = UUID.randomUUID();
        OrderCreateDto createDto = TestDataBuilder.createOrderDto(customerId, 100.0);

        when(customerService.findByIdOrThrow(customerId))
                .thenThrow(new IllegalStateException("Customer with id '" + customerId + "' not found"));

        assertThatThrownBy(() -> orderService.create(createDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("not found");

        verify(customerService).findByIdOrThrow(customerId);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should update order successfully")
    void shouldUpdateOrder() {
        UUID orderId = UUID.randomUUID();
        Customer customer = TestDataBuilder.createCustomer("John", "Doe", "john@test.com");
        Order existingOrder = TestDataBuilder.createOrder(customer, 100.0, StatusEnum.NEW);
        existingOrder.setId(orderId);

        OrderEditDto editDto = TestDataBuilder.createOrderEditDto(StatusEnum.PROCESSING, 150.0);

        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(orderId)
                .amount(150.0)
                .status(StatusEnum.PROCESSING)
                .build();

        when(repository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(repository.save(existingOrder)).thenReturn(existingOrder);
        when(mapper.toResponse(existingOrder)).thenReturn(responseDto);

        OrderResponseDto result = orderService.update(orderId, editDto);

        assertThat(result.status()).isEqualTo(StatusEnum.PROCESSING);
        verify(repository).findById(orderId);
        verify(mapper).updateEntityFromDto(editDto, existingOrder);
        verify(repository).save(existingOrder);
    }

    @Test
    @DisplayName("Should get filtered list with pagination")
    void shouldGetFilteredListWithPagination() {
        UUID customerId = UUID.randomUUID();
        OrderFilterDto filterDto = TestDataBuilder.createOrderFilterDto(customerId, 0, 20);
        OrderFilter filter = TestDataBuilder.createOrderFilter(customerId, 0, 20);

        Customer customer = TestDataBuilder.createCustomer("John", "Doe", "john@test.com");
        List<Order> orders = List.of(
                TestDataBuilder.createOrder(customer, 100.0, StatusEnum.NEW),
                TestDataBuilder.createOrder(customer, 200.0, StatusEnum.PROCESSING)
        );
        Page<Order> page = new PageImpl<>(orders, Pageable.unpaged(), 2);

        when(repository.findByFilters(
                eq(customerId), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(page);
        when(mapper.toShortDtoList(orders)).thenReturn(new ArrayList<>());
        when(mapper.toOrderFilter(filterDto)).thenReturn(filter);

        OrderShortListResponseDto result = orderService.getFilteredPaginatedList(filterDto);

        assertThat(result).isNotNull();
        assertThat(result.totalPages()).isEqualTo(1);
        verify(repository).findByFilters(eq(customerId), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should delete order successfully")
    void shouldDeleteOrder() {
        UUID orderId = UUID.randomUUID();
        when(repository.existsById(orderId)).thenReturn(true);

        orderService.delete(orderId);

        verify(repository).existsById(orderId);
        verify(repository).deleteById(orderId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent order")
    void shouldThrowExceptionWhenDeletingNonExistentOrder() {
        UUID orderId = UUID.randomUUID();
        when(repository.existsById(orderId)).thenReturn(false);

        assertThatThrownBy(() -> orderService.delete(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");

        verify(repository).existsById(orderId);
        verify(repository, never()).deleteById(any());
    }
}

