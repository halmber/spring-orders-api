package com.halmber.springordersapi.repository;

import com.halmber.springordersapi.model.entity.Customer;
import com.halmber.springordersapi.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CustomerRepository Integration Tests")
class CustomerRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("Should save and find customer by id")
    void shouldSaveAndFindCustomerById() {
        Customer customer = TestDataBuilder.createCustomer("John", "Doe", "john.doe@test.com");

        Customer saved = entityManager.persistAndFlush(customer);
        entityManager.clear();

        Optional<Customer> found = customerRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getLastName()).isEqualTo("Doe");
        assertThat(found.get().getEmail()).isEqualTo("john.doe@test.com");
    }

    @Test
    @DisplayName("Should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        Customer customer = TestDataBuilder.createCustomer("Jane", "Smith", "jane@test.com");
        entityManager.persistAndFlush(customer);

        boolean exists = customerRepository.existsByEmail("jane@test.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = customerRepository.existsByEmail("nonexistent@test.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should delete customer by id")
    void shouldDeleteCustomerById() {
        Customer customer = TestDataBuilder.createCustomer("Test", "User", "test@test.com");
        Customer saved = entityManager.persistAndFlush(customer);

        customerRepository.deleteById(saved.getId());
        entityManager.flush();
        entityManager.clear();

        Optional<Customer> found = customerRepository.findById(saved.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all customers")
    void shouldFindAllCustomers() {
        entityManager.persistAndFlush(TestDataBuilder.createCustomer("User1", "Test1", "user1@test.com"));
        entityManager.persistAndFlush(TestDataBuilder.createCustomer("User2", "Test2", "user2@test.com"));
        entityManager.persistAndFlush(TestDataBuilder.createCustomer("User3", "Test3", "user3@test.com"));
        entityManager.clear();

        var customers = customerRepository.findAll();

        assertThat(customers).hasSize(3);
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void shouldEnforceUniqueEmailConstraint() {
        Customer customer1 = TestDataBuilder.createCustomer("User1", "Test1", "duplicate@test.com");
        entityManager.persistAndFlush(customer1);

        TestDataBuilder.createCustomer("User2", "Test2", "duplicate@test.com");

        assertThat(customerRepository.existsByEmail("duplicate@test.com")).isTrue();
    }
}

