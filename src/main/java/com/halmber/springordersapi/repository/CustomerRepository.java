package com.halmber.springordersapi.repository;

import com.halmber.springordersapi.model.entity.Customer;

import java.util.UUID;

public interface CustomerRepository extends BaseRepository<Customer, UUID> {
    boolean existsByEmail(String email);
}
