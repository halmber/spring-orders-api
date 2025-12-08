package com.halmber.springordersapi.repository;

import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import com.halmber.springordersapi.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("OrderRepository Integration Tests")
class OrderRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Customer customer1;
    private Customer customer2;

    @BeforeEach
    void setUp() {
        customer1 = TestDataBuilder.createCustomer("John", "Doe", "john@test.com");
        customer2 = TestDataBuilder.createCustomer("Jane", "Smith", "jane@test.com");

        entityManager.persistAndFlush(customer1);
        entityManager.persistAndFlush(customer2);
    }

    @Test
    @DisplayName("Should save and find order by id")
    void shouldSaveAndFindOrderWithTagsById() {
        Order order = TestDataBuilder.createOrder(customer1, 100.0, StatusEnum.NEW);

        Order saved = entityManager.persistAndFlush(order);
        entityManager.clear();

        Optional<Order> found = orderRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(100.0);
        assertThat(found.get().getStatus()).isEqualTo(StatusEnum.NEW);
        assertThat(found.get().getCustomer().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should find orders by customer id filter")
    void shouldFindOrdersByCustomerIdFilter() {
        Order order1 = TestDataBuilder.createOrder(customer1, 100.0, StatusEnum.NEW);
        Order order2 = TestDataBuilder.createOrder(customer1, 200.0, StatusEnum.PROCESSING);
        Order order3 = TestDataBuilder.createOrder(customer2, 300.0, StatusEnum.DONE);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();
        entityManager.clear();

        Page<Order> result = orderRepository.findByFilters(
                customer1.getId(),
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(order -> order.getCustomer().getId().equals(customer1.getId()));
    }

    @Test
    @DisplayName("Should find orders by status filter")
    void shouldFindOrdersByStatusFilter() {
        Order order1 = TestDataBuilder.createOrder(customer1, 100.0, StatusEnum.NEW);
        Order order2 = TestDataBuilder.createOrder(customer1, 200.0, StatusEnum.NEW);
        Order order3 = TestDataBuilder.createOrder(customer2, 300.0, StatusEnum.DONE);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();
        entityManager.clear();

        Page<Order> result = orderRepository.findByFilters(
                null,
                StatusEnum.NEW,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(order -> order.getStatus() == StatusEnum.NEW);
    }

    @Test
    @DisplayName("Should find orders by payment method filter")
    void shouldFindOrdersByPaymentMethodFilter() {
        Order order1 = TestDataBuilder.createOrder(customer1, 100.0, StatusEnum.NEW);
        order1.setPaymentMethod(PaymentEnum.CARD);

        Order order2 = TestDataBuilder.createOrder(customer1, 200.0, StatusEnum.NEW);
        order2.setPaymentMethod(PaymentEnum.PAYPAL);

        Order order3 = TestDataBuilder.createOrder(customer2, 300.0, StatusEnum.DONE);
        order3.setPaymentMethod(PaymentEnum.CARD);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();
        entityManager.clear();

        Page<Order> result = orderRepository.findByFilters(
                null,
                null,
                PaymentEnum.CARD,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(order -> order.getPaymentMethod() == PaymentEnum.CARD);
    }

    @Test
    @DisplayName("Should find orders by multiple filters")
    void shouldFindOrdersByMultipleFilters() {
        Order order1 = TestDataBuilder.createOrder(customer1, 100.0, StatusEnum.NEW);
        order1.setPaymentMethod(PaymentEnum.CARD);

        Order order2 = TestDataBuilder.createOrder(customer1, 200.0, StatusEnum.NEW);
        order2.setPaymentMethod(PaymentEnum.PAYPAL);

        Order order3 = TestDataBuilder.createOrder(customer2, 300.0, StatusEnum.NEW);
        order3.setPaymentMethod(PaymentEnum.CARD);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();
        entityManager.clear();

        Page<Order> result = orderRepository.findByFilters(
                customer1.getId(),
                StatusEnum.NEW,
                PaymentEnum.CARD,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getAmount()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should find all orders by stream filters without pagination")
    void shouldFindAllOrdersByFilters() {
        Order order1 = TestDataBuilder.createOrder(customer1, 100.0, StatusEnum.NEW);
        Order order2 = TestDataBuilder.createOrder(customer1, 200.0, StatusEnum.PROCESSING);
        Order order3 = TestDataBuilder.createOrder(customer2, 300.0, StatusEnum.NEW);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();
        entityManager.clear();

        List<Order> result = orderRepository.streamByFilters(
                null,
                StatusEnum.NEW,
                null
        ).toList();

        assertThat(result).hasSize(2);
        assertThat(result)
                .allMatch(order -> order.getStatus() == StatusEnum.NEW);
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePaginationCorrectly() {
        for (int i = 0; i < 25; i++) {
            Order order = TestDataBuilder.createOrder(customer1, 100.0 + i, StatusEnum.NEW);
            entityManager.persist(order);
        }
        entityManager.flush();
        entityManager.clear();

        Page<Order> page1 = orderRepository.findByFilters(
                customer1.getId(), null, null, PageRequest.of(0, 10)
        );
        Page<Order> page2 = orderRepository.findByFilters(
                customer1.getId(), null, null, PageRequest.of(1, 10)
        );

        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(10);
        assertThat(page1.getTotalElements()).isEqualTo(25);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }
}

