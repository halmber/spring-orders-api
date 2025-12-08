package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.dto.request.customer.CustomerCreateDto;
import com.halmber.springordersapi.model.dto.request.customer.CustomerEditDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerListResponseDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerResponseDto;
import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.mapper.CustomerMapper;
import com.halmber.springordersapi.repository.BaseRepository;
import com.halmber.springordersapi.repository.CustomerRepository;
import com.halmber.springordersapi.service.exeption.AlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService extends BaseService<Customer, UUID> {
    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    @Override
    protected BaseRepository<Customer, UUID> getRepository() {
        return repository;
    }

    @Transactional(readOnly = true)
    public CustomerListResponseDto listCustomers(Pageable pageable) {
        Page<Customer> page = repository.findAll(pageable);

        return CustomerListResponseDto.builder()
                .customers(mapper.toList(page.toList()))
                .totalPages(page.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getById(UUID id) {
        return mapper.toResponse(findByIdOrThrow(id));
    }

    @Transactional
    public CustomerResponseDto create(CustomerCreateDto customer) {
        Customer entity = mapper.toEntity(customer);

        if (repository.existsByEmail(entity.getEmail())) {
            throw new AlreadyExistsException(Customer.class.getSimpleName(), "email", entity.getEmail());
        }

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public CustomerResponseDto update(UUID id, CustomerEditDto dto) {
        Customer entity = findByIdOrThrow(id);

        mapper.updateEntityFromDto(dto, entity);

        return mapper.toResponse(repository.save(entity));
    }
}
