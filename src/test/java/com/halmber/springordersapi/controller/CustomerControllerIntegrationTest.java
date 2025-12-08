package com.halmber.springordersapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halmber.springordersapi.model.dto.request.customer.CustomerCreateDto;
import com.halmber.springordersapi.model.dto.request.customer.CustomerEditDto;
import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+380501234567")
                .city("Kyiv")
                .build();
        testCustomer = customerRepository.save(testCustomer);
    }

    @AfterEach
    void tearDown() {
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/customers - Should return paginated list of customers")
    void shouldReturnCustomersWithPagination() throws Exception {
        // Create additional customers
        customerRepository.save(Customer.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("+380501234568")
                .city("Lviv")
                .build());

        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers", hasSize(2)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.customers[0].firstName", notNullValue()))
                .andExpect(jsonPath("$.customers[0].email", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/customers?sort=firstName,asc - Should return paginated list of customers")
    void shouldSupportSorting() throws Exception {
        customerRepository.save(Customer.builder()
                .firstName("Alice")
                .lastName("Brown")
                .email("alice.brown@example.com")
                .phone("+380501234569")
                .city("Odesa")
                .build());

        mockMvc.perform(get("/api/customers")
                        .param("sort", "firstName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers[0].firstName", is("Alice")))
                .andExpect(jsonPath("$.customers[1].firstName", is("John")));
    }

    @Test
    @DisplayName("GET /api/customers/{id} - Should return customer by id")
    void shouldReturnCustomerById() throws Exception {
        mockMvc.perform(get("/api/customers/{id}", testCustomer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testCustomer.getId().toString())))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.phone", is("+380501234567")))
                .andExpect(jsonPath("$.city", is("Kyiv")));
    }

    @Test
    @DisplayName("GET /api/customers/{id} - Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/customers/{id}", randomId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/customers - Should create new customer")
    void shouldCreateNewCustomer() throws Exception {
        CustomerCreateDto createDto = CustomerCreateDto.builder()
                .firstName("Michael")
                .lastName("Johnson")
                .email("michael.johnson@example.com")
                .phone("+380501234570")
                .city("Dnipro")
                .build();

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.firstName", is("Michael")))
                .andExpect(jsonPath("$.lastName", is("Johnson")))
                .andExpect(jsonPath("$.email", is("michael.johnson@example.com")))
                .andExpect(jsonPath("$.phone", is("+380501234570")))
                .andExpect(jsonPath("$.city", is("Dnipro")));
    }

    @Test
    @DisplayName("POST /api/customers - Should return 409 when email already exists")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        CustomerCreateDto createDto = CustomerCreateDto.builder()
                .firstName("Duplicate")
                .lastName("User")
                .email("john.doe@example.com") // Already exists
                .phone("+380501234571")
                .city("Kharkiv")
                .build();

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/customers - Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        CustomerCreateDto createDto = CustomerCreateDto.builder()
                .firstName("Invalid")
                .lastName("Email")
                .email("not-an-email")
                .phone("+380501234572")
                .city("Kharkiv")
                .build();

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/customers - Should return 400 when required field missing")
    void shouldReturn400WhenRequiredFieldMissing() throws Exception {
        CustomerCreateDto createDto = CustomerCreateDto.builder()
                .firstName("Missing")
                .lastName("Fields")
                // Missing email
                .phone("+380501234573")
                .city("Kharkiv")
                .build();

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/customers/{id} - Should update customer")
    void shouldUpdateCustomer() throws Exception {
        CustomerEditDto editDto = CustomerEditDto.builder()
                .firstName("John Updated")
                .lastName("Doe Updated")
                .phone("+380501234999")
                .city("Lviv")
                .build();

        mockMvc.perform(put("/api/customers/{id}", testCustomer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testCustomer.getId().toString())))
                .andExpect(jsonPath("$.firstName", is("John Updated")))
                .andExpect(jsonPath("$.lastName", is("Doe Updated")))
                .andExpect(jsonPath("$.phone", is("+380501234999")))
                .andExpect(jsonPath("$.city", is("Lviv")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com"))); // Email unchanged
    }

    @Test
    @DisplayName("PUT /api/customers/{id} - Should return 404 when not exists")
    void shouldReturn404WhenNotExists() throws Exception {
        UUID randomId = UUID.randomUUID();
        CustomerEditDto editDto = CustomerEditDto.builder()
                .firstName("Not")
                .lastName("Found")
                .phone("+380501234574")
                .city("Kharkiv")
                .build();

        mockMvc.perform(put("/api/customers/{id}", randomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/customers/{id} - Should return 404 when validation fails")
    void shouldReturn400WhenInvalidData() throws Exception {
        CustomerEditDto editDto = CustomerEditDto.builder()
                .firstName("Jo") // Too short
                .lastName("Doe")
                .phone("+380501234575")
                .city("Kharkiv")
                .build();

        mockMvc.perform(put("/api/customers/{id}", testCustomer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} - Should delete customer")
    void shouldDeleteCustomer() throws Exception {
        mockMvc.perform(delete("/api/customers/{id}", testCustomer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", containsString(testCustomer.getId().toString())));

        // Verify customer is deleted
        mockMvc.perform(get("/api/customers/{id}", testCustomer.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} - Should return 404 when customer not found")
    void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(delete("/api/customers/{id}", randomId))
                .andExpect(status().isNotFound());
    }
}
