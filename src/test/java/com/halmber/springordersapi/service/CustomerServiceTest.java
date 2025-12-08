package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.dto.request.customer.CustomerCreateDto;
import com.halmber.springordersapi.model.dto.request.customer.CustomerEditDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerListResponseDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerResponseDto;
import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.mapper.CustomerMapper;
import com.halmber.springordersapi.repository.CustomerRepository;
import com.halmber.springordersapi.service.exeption.AlreadyExistsException;
import com.halmber.springordersapi.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerMapper mapper;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("Should list customers with pagination")
    void shouldListCustomersWithPagination() {
        Pageable pageable = PageRequest.of(0, 5);
        List<Customer> customers = List.of(
                TestDataBuilder.createCustomer("John", "Doe", "john@test.com"),
                TestDataBuilder.createCustomer("Jane", "Smith", "jane@test.com")
        );
        Page<Customer> page = new PageImpl<>(customers, pageable, 2);

        List<CustomerResponseDto> responseDtos = List.of(
                CustomerResponseDto.builder().firstName("John").build(),
                CustomerResponseDto.builder().firstName("Jane").build()
        );

        when(repository.findAll(pageable)).thenReturn(page);
        when(mapper.toList(customers)).thenReturn(responseDtos);

        CustomerListResponseDto result = customerService.listCustomers(pageable);

        assertThat(result.customers()).hasSize(2);
        assertThat(result.totalPages()).isEqualTo(1);
        verify(repository).findAll(pageable);
        verify(mapper).toList(customers);
    }

    @Test
    @DisplayName("Should get customer by id")
    void shouldGetCustomerById() {
        UUID customerId = UUID.randomUUID();
        Customer customer = TestDataBuilder.createCustomer("John", "Doe", "john@test.com");
        customer.setId(customerId);

        CustomerResponseDto responseDto = CustomerResponseDto.builder()
                .id(customerId)
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .build();

        when(repository.findById(customerId)).thenReturn(Optional.of(customer));
        when(mapper.toResponse(customer)).thenReturn(responseDto);

        CustomerResponseDto result = customerService.getById(customerId);

        assertThat(result.id()).isEqualTo(customerId);
        assertThat(result.firstName()).isEqualTo("John");
        verify(repository).findById(customerId);
        verify(mapper).toResponse(customer);
    }

    @Test
    @DisplayName("Should throw exception when customer not found by id")
    void shouldThrowExceptionWhenCustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        when(repository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(customerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");

        verify(repository).findById(customerId);
        verify(mapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should save new customer successfully")
    void shouldSaveNewCustomer() {
        CustomerCreateDto createDto = TestDataBuilder.createCustomerDto("John", "Doe", "john@test.com");
        Customer customer = TestDataBuilder.createCustomer("John", "Doe", "john@test.com");
        customer.setId(UUID.randomUUID());

        CustomerResponseDto responseDto = CustomerResponseDto.builder()
                .id(customer.getId())
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .build();

        when(mapper.toEntity(createDto)).thenReturn(customer);
        when(repository.existsByEmail("john@test.com")).thenReturn(false);
        when(repository.save(customer)).thenReturn(customer);
        when(mapper.toResponse(customer)).thenReturn(responseDto);

        CustomerResponseDto result = customerService.create(createDto);

        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.email()).isEqualTo("john@test.com");
        verify(repository).existsByEmail("john@test.com");
        verify(repository).save(customer);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        CustomerCreateDto createDto = TestDataBuilder.createCustomerDto("John", "Doe", "john@test.com");
        Customer customer = TestDataBuilder.createCustomer("John", "Doe", "john@test.com");

        when(mapper.toEntity(createDto)).thenReturn(customer);
        when(repository.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(createDto))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(repository).existsByEmail("john@test.com");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should update customer successfully")
    void shouldUpdateCustomer() {
        UUID customerId = UUID.randomUUID();
        CustomerEditDto editDto = TestDataBuilder.createCustomerEditDto("John Updated", "Doe");
        Customer existingCustomer = TestDataBuilder.createCustomer("John", "Doe", "john@test.com");
        existingCustomer.setId(customerId);

        CustomerResponseDto responseDto = CustomerResponseDto.builder()
                .id(customerId)
                .firstName("John Updated")
                .lastName("Doe")
                .build();

        when(repository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(repository.save(existingCustomer)).thenReturn(existingCustomer);
        when(mapper.toResponse(existingCustomer)).thenReturn(responseDto);

        CustomerResponseDto result = customerService.update(customerId, editDto);

        assertThat(result.firstName()).isEqualTo("John Updated");
        verify(repository).findById(customerId);
        verify(mapper).updateEntityFromDto(editDto, existingCustomer);
        verify(repository).save(existingCustomer);
    }

    @Test
    @DisplayName("Should delete customer successfully")
    void shouldDeleteCustomer() {
        UUID customerId = UUID.randomUUID();
        when(repository.existsById(customerId)).thenReturn(true);

        customerService.delete(customerId);

        verify(repository).existsById(customerId);
        verify(repository).deleteById(customerId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent customer")
    void shouldThrowExceptionWhenDeletingNonExistentCustomer() {
        UUID customerId = UUID.randomUUID();
        when(repository.existsById(customerId)).thenReturn(false);

        assertThatThrownBy(() -> customerService.delete(customerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");

        verify(repository).existsById(customerId);
        verify(repository, never()).deleteById(any());
    }
}

