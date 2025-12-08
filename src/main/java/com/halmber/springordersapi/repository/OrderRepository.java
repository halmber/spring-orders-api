package com.halmber.springordersapi.repository;

import com.halmber.springordersapi.model.entity.Order;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface OrderRepository extends BaseRepository<Order, UUID> {
    @EntityGraph(attributePaths = {"customer"})
    Optional<Order> findById(UUID id);

    @EntityGraph(attributePaths = {"customer"})
    @Query("SELECT o FROM Order o WHERE " +
            "(:customerId IS NULL OR o.customer.id = :customerId) AND " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)")
    Page<Order> findByFilters(
            @Param("customerId") UUID customerId,
            @Param("status") StatusEnum status,
            @Param("paymentMethod") PaymentEnum paymentMethod,
            Pageable pageable
    );

    /**
     * Stream-based query for memory-efficient processing of large datasets.
     * Must be used within a transactional context and closed after use.
     */
    @EntityGraph(attributePaths = {"customer"})
    @Query("SELECT o FROM Order o WHERE " +
            "(:customerId IS NULL OR o.customer.id = :customerId) AND " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod) " +
            "ORDER BY o.createdAt DESC")
    @QueryHints(value = {
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "50"),
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, value = "true")
    })
    Stream<Order> streamByFilters(
            @Param("customerId") UUID customerId,
            @Param("status") StatusEnum status,
            @Param("paymentMethod") PaymentEnum paymentMethod
    );
}
