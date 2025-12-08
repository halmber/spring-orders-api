package com.halmber.springordersapi.service;

import com.halmber.springordersapi.model.dto.response.order.OrderImportResultDto;
import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.repository.CustomerRepository;
import com.halmber.springordersapi.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderImportServiceIntegrationTest {

    @Autowired
    private OrderImportService importService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private UUID validCustomerId;

    @BeforeEach
    void setUp() {
        // Create test customer
        Customer customer = Customer.builder()
                .firstName("Test")
                .lastName("Customer")
                .email("test@example.com")
                .phone("+1234567890")
                .city("Test City")
                .build();
        customer = customerRepository.save(customer);
        validCustomerId = customer.getId();
    }

    @Test
    void shouldImportValidOrders() throws IOException {
        MockMultipartFile file = getMockMultipartFile(String.format("""
                [
                  {
                    "orderId": 1001,
                    "customerId": "%s",
                    "amount": 150.50,
                    "status": "NEW",
                    "paymentMethod": "CARD"
                  },
                  {
                    "orderId": 1002,
                    "customerId": "%s",
                    "amount": 75.25,
                    "status": "PROCESSING",
                    "paymentMethod": "PAYPAL"
                  }
                ]
                """, validCustomerId, validCustomerId));

        OrderImportResultDto result = importService.importOrders(file);

        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.successfulImports()).isEqualTo(2);
        assertThat(result.failedImports()).isEqualTo(0);
        assertThat(result.errors()).isEmpty();
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldRejectInvalidCustomerId() throws IOException {
        MockMultipartFile file = getMockMultipartFile("""
                [
                  {
                    "orderId": 1001,
                    "customerId": "invalid-uuid",
                    "amount": 150.50,
                    "status": "NEW",
                    "paymentMethod": "CARD"
                  }
                ]
                """);

        OrderImportResultDto result = importService.importOrders(file);

        assertThat(result.totalRecords()).isEqualTo(1);
        assertThat(result.successfulImports()).isEqualTo(0);
        assertThat(result.failedImports()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().getFirst().reason()).isEqualTo("Invalid customer ID format");
    }

    @Test
    void shouldRejectNonExistentCustomer() throws IOException {
        String nonExistentId = UUID.randomUUID().toString();
        MockMultipartFile file = getMockMultipartFile(String.format("""
                [
                  {
                    "orderId": 1001,
                    "customerId": "%s",
                    "amount": 150.50,
                    "status": "NEW",
                    "paymentMethod": "CARD"
                  }
                ]
                """, nonExistentId));

        OrderImportResultDto result = importService.importOrders(file);

        assertThat(result.totalRecords()).isEqualTo(1);
        assertThat(result.successfulImports()).isEqualTo(0);
        assertThat(result.failedImports()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().getFirst().reason()).isEqualTo("Customer not found");
    }

    private static MockMultipartFile getMockMultipartFile(String nonExistentId) {
        return new MockMultipartFile(
                "file",
                "orders.json",
                "application/json",
                nonExistentId.getBytes()
        );
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.json",
                "application/json",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> importService.importOrders(file));
    }

    @Test
    void shouldRejectNonJsonFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.txt",
                "text/plain",
                "some text".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> importService.importOrders(file));
    }
}

