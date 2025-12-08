package com.halmber.springordersapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halmber.springordersapi.model.dto.request.customer.CustomerCreateDto;
import com.halmber.springordersapi.model.dto.request.customer.CustomerEditDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerListResponseDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerResponseDto;
import com.halmber.springordersapi.service.CustomerService;
import com.halmber.springordersapi.service.exeption.AlreadyExistsException;
import com.halmber.springordersapi.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
@DisplayName("CustomerController Integration Tests")
class CustomerControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @Test
    @DisplayName("GET /api/customers - Should return paginated list of customers")
    void shouldReturnPaginatedListOfCustomers() throws Exception {
        CustomerResponseDto customer1 = CustomerResponseDto.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .phone("+380501234567")
                .city("Kharkiv")
                .build();

        CustomerResponseDto customer2 = CustomerResponseDto.builder()
                .id(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@test.com")
                .phone("+380509876543")
                .city("Kyiv")
                .build();

        CustomerListResponseDto response = CustomerListResponseDto.builder()
                .customers(List.of(customer1, customer2))
                .totalPages(1)
                .build();

        when(customerService.listCustomers(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers", hasSize(2)))
                .andExpect(jsonPath("$.customers[0].firstName", is("John")))
                .andExpect(jsonPath("$.customers[1].firstName", is("Jane")))
                .andExpect(jsonPath("$.totalPages", is(1)));

        verify(customerService).listCustomers(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/customers/{id} - Should return customer by id")
    void shouldReturnCustomerById() throws Exception {
        UUID customerId = UUID.randomUUID();
        CustomerResponseDto response = CustomerResponseDto.builder()
                .id(customerId)
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .phone("+380501234567")
                .city("Kharkiv")
                .build();

        when(customerService.getById(customerId)).thenReturn(response);

        mockMvc.perform(get("/api/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(customerId.toString())))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.email", is("john@test.com")));

        verify(customerService).getById(customerId);
    }

    @Test
    @DisplayName("GET /api/customers/{id} - Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customerService.getById(customerId))
                .thenThrow(new IllegalStateException("Customer with id '%s' not found".formatted(customerId)));

        mockMvc.perform(get("/api/customers/{id}", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("not found")));

        verify(customerService).getById(customerId);
    }

    @Test
    @DisplayName("POST /api/customers - Should create new customer")
    void shouldCreateNewCustomer() throws Exception {
        CustomerCreateDto createDto = TestDataBuilder.createCustomerDto("John", "Doe", "john@test.com");
        UUID customerId = UUID.randomUUID();

        CustomerResponseDto response = CustomerResponseDto.builder()
                .id(customerId)
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .phone("+380501234567")
                .city("Kharkiv")
                .build();

        when(customerService.create(any(CustomerCreateDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(customerId.toString())))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.email", is("john@test.com")));

        verify(customerService).create(any(CustomerCreateDto.class));
    }

    @Test
    @DisplayName("POST /api/customers - Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        CustomerCreateDto invalidDto = CustomerCreateDto.builder()
                .firstName("J") // Too short
                .lastName("Doe")
                .email("invalid-email") // Invalid email format
                .phone("+380501234567")
                .city("Kharkiv")
                .build();

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));

        verify(customerService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/customers - Should return 409 when email already exists")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        CustomerCreateDto createDto = TestDataBuilder.createCustomerDto("John", "Doe", "john@test.com");

        when(customerService.create(any(CustomerCreateDto.class)))
                .thenThrow(new AlreadyExistsException("Customer", "email", "john@test.com"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("email")));

        verify(customerService).create(any(CustomerCreateDto.class));
    }

    @Test
    @DisplayName("PUT /api/customers/{id} - Should update customer")
    void shouldUpdateCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        CustomerEditDto editDto = TestDataBuilder.createCustomerEditDto("John Updated", "Doe");

        CustomerResponseDto response = CustomerResponseDto.builder()
                .id(customerId)
                .firstName("John Updated")
                .lastName("Doe")
                .email("john@test.com")
                .phone("+380509876543")
                .city("Kyiv")
                .build();

        when(customerService.update(eq(customerId), any(CustomerEditDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(customerId.toString())))
                .andExpect(jsonPath("$.firstName", is("John Updated")))
                .andExpect(jsonPath("$.city", is("Kyiv")));

        verify(customerService).update(eq(customerId), any(CustomerEditDto.class));
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} - Should delete customer")
    void shouldDeleteCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        doNothing().when(customerService).delete(customerId);

        mockMvc.perform(delete("/api/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", containsString("deleted")));

        verify(customerService).delete(customerId);
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} - Should return 404 when customer not found")
    void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        doThrow(new IllegalStateException("Customer with id '%s' not found".formatted(customerId)))
                .when(customerService).delete(customerId);

        mockMvc.perform(delete("/api/customers/{id}", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("not found")));

        verify(customerService).delete(customerId);
    }

    @Test
    @DisplayName("GET /api/customers - Should support sorting by allowed fields")
    void shouldSupportSortingByAllowedFields() throws Exception {
        CustomerListResponseDto response = CustomerListResponseDto.builder()
                .customers(List.of())
                .totalPages(0)
                .build();

        when(customerService.listCustomers(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "firstName,asc"))
                .andExpect(status().isOk());

        verify(customerService).listCustomers(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/customers - Should reject sorting by forbidden fields")
    void shouldRejectSortingByForbiddenFields() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "email,asc")) // email not in whitelist
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not allowed")));

        verify(customerService, never()).listCustomers(any());
    }
}

