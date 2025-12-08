package com.halmber.springordersapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halmber.springordersapi.model.dto.request.order.OrderCreateDto;
import com.halmber.springordersapi.model.dto.request.order.OrderEditDto;
import com.halmber.springordersapi.model.dto.request.order.OrderFilterDto;
import com.halmber.springordersapi.model.dto.request.order.OrderReportFilterDto;
import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import com.halmber.springordersapi.repository.CustomerRepository;
import com.halmber.springordersapi.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;
    private Order testOrder;

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

        testOrder = Order.builder()
                .customer(testCustomer)
                .amount(100.50)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.CARD)
                .build();
        testOrder = orderRepository.save(testOrder);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/orders - Should return paginated list of orders")
    void shouldReturnPaginatedListOfOrders() throws Exception {
        // Create additional order
        orderRepository.save(Order.builder()
                .customer(testCustomer)
                .amount(250.00)
                .status(StatusEnum.PROCESSING)
                .paymentMethod(PaymentEnum.PAYPAL)
                .build());

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(2)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.orders[0].id", notNullValue()))
                .andExpect(jsonPath("$.orders[0].amount", notNullValue()))
                .andExpect(jsonPath("$.orders[0].customer", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/orders?sort=amount,asc - Should support sorting by allowed fields")
    void shouldSupportSortingByAllowedFields() throws Exception {
        orderRepository.save(Order.builder()
                .customer(testCustomer)
                .amount(50.00)
                .status(StatusEnum.DONE)
                .paymentMethod(PaymentEnum.CASH)
                .build());

        mockMvc.perform(get("/api/orders")
                        .param("sort", "amount,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].amount", is(50.00)))
                .andExpect(jsonPath("$.orders[1].amount", is(100.50)));
    }

    @Test
    @DisplayName("GET /api/orders/{id} - Should return order by id")
    void shouldReturnOrderById() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testOrder.getId().toString())))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.status", is("NEW")))
                .andExpect(jsonPath("$.paymentMethod", is("CARD")))
                .andExpect(jsonPath("$.customer.id", is(testCustomer.getId().toString())))
                .andExpect(jsonPath("$.customer.firstName", is("John")));
    }

    @Test
    @DisplayName("GET /api/orders/{id} - Should return 404 when order not found")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/orders/{id}", randomId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/orders - Should create new order")
    void shouldCreateNewOrder() throws Exception {
        OrderCreateDto createDto = OrderCreateDto.builder()
                .customerId(testCustomer.getId().toString())
                .amount(300.00)
                .status("PROCESSING")
                .paymentMethod("GOOGLE_PAY")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.amount", is(300.00)))
                .andExpect(jsonPath("$.status", is("PROCESSING")))
                .andExpect(jsonPath("$.paymentMethod", is("GOOGLE_PAY")))
                .andExpect(jsonPath("$.customer.id", is(testCustomer.getId().toString())));
    }

    @Test
    @DisplayName("POST /api/orders - Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        OrderCreateDto createDto = OrderCreateDto.builder()
                .customerId(UUID.randomUUID().toString())
                .amount(300.00)
                .status("NEW")
                .paymentMethod("CARD")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/orders - Should return 400 when status validation fails")
    void shouldReturn400WhenStatusValidationFails() throws Exception {
        OrderCreateDto createDto = OrderCreateDto.builder()
                .customerId(testCustomer.getId().toString())
                .amount(300.00)
                .status("INVALID_STATUS")
                .paymentMethod("CARD")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - Should return 400 when amount validation fails")
    void shouldReturn400WhenAmountValidationFails() throws Exception {
        OrderCreateDto createDto = OrderCreateDto.builder()
                .customerId(testCustomer.getId().toString())
                .amount(-10.00)
                .status("NEW")
                .paymentMethod("CARD")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/orders/{id} - Should update order")
    void shouldUpdateOrder() throws Exception {
        OrderEditDto editDto = OrderEditDto.builder()
                .amount(200.00)
                .status("DONE")
                .paymentMethod("CASH")
                .build();

        mockMvc.perform(put("/api/orders/{id}", testOrder.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testOrder.getId().toString())))
                .andExpect(jsonPath("$.amount", is(200.00)))
                .andExpect(jsonPath("$.status", is("DONE")))
                .andExpect(jsonPath("$.paymentMethod", is("CASH")));
    }

    @Test
    @DisplayName("PUT /api/orders/{id} - Should return 404 when order not found")
    void shouldReturn404WhenUpdatingNonExistentOrder() throws Exception {
        UUID randomId = UUID.randomUUID();
        OrderEditDto editDto = OrderEditDto.builder()
                .amount(200.00)
                .status("DONE")
                .paymentMethod("CASH")
                .build();

        mockMvc.perform(put("/api/orders/{id}", randomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/orders/_list - Should return filtered and paginated list")
    void shouldReturnFilteredAndPaginatedList() throws Exception {
        // Create orders with different statuses
        orderRepository.save(Order.builder()
                .customer(testCustomer)
                .amount(150.00)
                .status(StatusEnum.DONE)
                .paymentMethod(PaymentEnum.PAYPAL)
                .build());

        OrderFilterDto filterDto = OrderFilterDto.builder()
                .status("NEW")
                .page(0)
                .size(10)
                .build();

        mockMvc.perform(post("/api/orders/_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].status", is("NEW")));
    }

    @Test
    @DisplayName("POST /api/orders/_list - Should return filter by customer id")
    void shouldFilterByCustomerId() throws Exception {
        // Create another customer with order
        Customer anotherCustomer = customerRepository.save(Customer.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("+380501234568")
                .city("Lviv")
                .build());

        orderRepository.save(Order.builder()
                .customer(anotherCustomer)
                .amount(500.00)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.CARD)
                .build());

        OrderFilterDto filterDto = OrderFilterDto.builder()
                .customerId(testCustomer.getId().toString())
                .page(0)
                .size(10)
                .build();

        mockMvc.perform(post("/api/orders/_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].customer.id", is(testCustomer.getId().toString())));
    }

    @Test
    @DisplayName("POST /api/orders/_list - Should return filter by payment method")
    void getList_shouldFilterByPaymentMethod() throws Exception {
        orderRepository.save(Order.builder()
                .customer(testCustomer)
                .amount(150.00)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.PAYPAL)
                .build());

        OrderFilterDto filterDto = OrderFilterDto.builder()
                .paymentMethod("CARD")
                .page(0)
                .size(10)
                .build();

        mockMvc.perform(post("/api/orders/_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].paymentMethod", is("CARD")));
    }

    @Test
    @DisplayName("POST /api/orders/_report - Should generate CSV report")
    void shouldGenerateCsvReport() throws Exception {
        OrderReportFilterDto filterDto = OrderReportFilterDto.builder()
                .fileType("csv")
                .build();

        mockMvc.perform(post("/api/orders/_report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterDto)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("orders_report_")))
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(content().string(containsString("Order ID,Customer ID")));
    }

    @Test
    @DisplayName("POST /api/orders/_report - Should generate XLSX report")
    void shouldGenerateXlsxReport() throws Exception {
        OrderReportFilterDto filterDto = OrderReportFilterDto.builder()
                .fileType("xlsx")
                .build();

        mockMvc.perform(post("/api/orders/_report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterDto)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("orders_report_")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));
    }

    @Test
    @DisplayName("POST /api/orders/_report - Should generate filtered report by status")
    void shouldFilterReportByStatus() throws Exception {
        orderRepository.save(Order.builder()
                .customer(testCustomer)
                .amount(150.00)
                .status(StatusEnum.DONE)
                .paymentMethod(PaymentEnum.PAYPAL)
                .build());

        OrderReportFilterDto filterDto = OrderReportFilterDto.builder()
                .status("NEW")
                .fileType("csv")
                .build();

        mockMvc.perform(post("/api/orders/_report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterDto)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEW")))
                .andExpect(content().string(not(containsString("DONE"))));
    }

    @Test
    @DisplayName("POST /api/orders/upload - Should import orders from JSON file")
    void shouldImportOrders() throws Exception {
        String jsonContent = """
                [
                    {
                        "customerId": "%s",
                        "amount": 100.00,
                        "status": "NEW",
                        "paymentMethod": "CARD"
                    },
                    {
                        "customerId": "%s",
                        "amount": 200.00,
                        "status": "PROCESSING",
                        "paymentMethod": "PAYPAL"
                    }
                ]
                """.formatted(testCustomer.getId(), testCustomer.getId());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.json",
                "application/json",
                jsonContent.getBytes()
        );

        mockMvc.perform(multipart("/api/orders/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", is(2)))
                .andExpect(jsonPath("$.successfulImports", is(2)))
                .andExpect(jsonPath("$.failedImports", is(0)))
                .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/orders/upload - Should handle import with errors")
    void shouldHandleImportWithErrors() throws Exception {
        String jsonContent = """
                [
                    {
                        "customerId": "invalid-uuid",
                        "amount": 100.00,
                        "status": "NEW",
                        "paymentMethod": "CARD"
                    },
                    {
                        "customerId": "%s",
                        "amount": -50.00,
                        "status": "NEW",
                        "paymentMethod": "CARD"
                    }
                ]
                """.formatted(testCustomer.getId());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.json",
                "application/json",
                jsonContent.getBytes()
        );

        mockMvc.perform(multipart("/api/orders/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", is(2)))
                .andExpect(jsonPath("$.successfulImports", is(0)))
                .andExpect(jsonPath("$.failedImports", is(2)))
                .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    @Test
    @DisplayName("POST /api/orders/upload - Should return 400 when not valid JSON")
    void shouldReturn400WFileNotJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.txt",
                "text/plain",
                "not json".getBytes()
        );

        mockMvc.perform(multipart("/api/orders/upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders/upload - Should handle empty file")
    void shouldReturn400WhenFileEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.json",
                "application/json",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/orders/upload")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} - Should delete order")
    void shouldDeleteOrder() throws Exception {
        mockMvc.perform(delete("/api/orders/{id}", testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", containsString(testOrder.getId().toString())));

        // Verify order is deleted
        mockMvc.perform(get("/api/orders/{id}", testOrder.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} - Should return 404 when order not found")
    void shouldReturn404WhileDeletingAndNotExist() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(delete("/api/orders/{id}", randomId))
                .andExpect(status().isNotFound());
    }
}

