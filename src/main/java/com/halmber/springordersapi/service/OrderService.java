package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.OrderFilter;
import com.halmber.springordersapi.model.dto.request.order.OrderCreateDto;
import com.halmber.springordersapi.model.dto.request.order.OrderEditDto;
import com.halmber.springordersapi.model.dto.request.order.OrderFilterDto;
import com.halmber.springordersapi.model.dto.response.order.OrderListResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderShortListResponseDto;
import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.mapper.OrderMapper;
import com.halmber.springordersapi.repository.BaseRepository;
import com.halmber.springordersapi.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing order entities.
 * Provides business logic for creating, reading, updating, and filtering orders
 * with customer validation and relationship management.
 *
 * <p>Support:
 * <ul>
 *   <li>Paginated order listing with sorting support</li>
 *   <li>Advanced filtering by customer, status, and payment method</li>
 *   <li>Customer validation before order creation</li>
 *   <li>Support for both full and short response formats</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrderService extends BaseService<Order, UUID> {
    private final OrderRepository repository;
    private final CustomerService customerService;
    private final OrderMapper mapper;

    @Override
    protected BaseRepository<Order, UUID> getRepository() {
        return repository;
    }

    @Transactional(readOnly = true)
    public OrderListResponseDto listOrders(Pageable pageable) {
        Page<Order> page = repository.findAll(pageable);

        return OrderListResponseDto.builder()
                .orders(mapper.toList(page.toList()))
                .totalPages(page.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getById(UUID id) {
        return mapper.toResponse(findByIdOrThrow(id));
    }

    @Transactional
    public OrderResponseDto create(OrderCreateDto dto) {
        UUID uuid = parseAndValidateUUID(dto.customerId());
        Customer customer = customerService.findByIdOrThrow(uuid);

        Order entity = mapper.toEntity(dto);
        entity.setCustomer(customer);

        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public OrderResponseDto update(UUID id, OrderEditDto dto) {
        Order entity = findByIdOrThrow(id);

        mapper.updateEntityFromDto(dto, entity);

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public OrderShortListResponseDto getFilteredPaginatedList(OrderFilterDto filter) {
        Pageable pageable = PageRequest.of(
                filter.page() != null ? filter.page() : 0,
                filter.size() != null ? filter.size() : 5
        );

        UUID uuid = parseAndValidateUUID(filter.customerId());
        if (uuid != null) {
            customerService.existsByIdOrThrow(uuid);
        }

        OrderFilter entity = mapper.toOrderFilter(filter);

        Page<Order> page = repository.findByFilters(
                uuid,
                entity.status(),
                entity.paymentMethod(),
                pageable
        );

        return OrderShortListResponseDto.builder()
                .orders(mapper.toShortDtoList(page.getContent()))
                .totalPages(page.getTotalPages())
                .build();
    }
}
