package com.halmber.springordersapi.controller;

import com.halmber.springordersapi.controller.annotation.PageableConstraints;
import com.halmber.springordersapi.model.dto.request.customer.CustomerCreateDto;
import com.halmber.springordersapi.model.dto.request.customer.CustomerEditDto;
import com.halmber.springordersapi.model.dto.response.MessageResponseDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerListResponseDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerResponseDto;
import com.halmber.springordersapi.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public CustomerListResponseDto getPageableList(
            @PageableConstraints(whitelist = {"firstName", "lastName", "city"})
            @PageableDefault(size = 5) Pageable pageable) {
        return customerService.listCustomers(pageable);
    }

    @GetMapping("/{id}")
    public CustomerResponseDto getById(@NotNull @PathVariable("id") UUID id) {
        return customerService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponseDto create(@Valid @RequestBody CustomerCreateDto customer) {
        return customerService.create(customer);
    }

    @PutMapping("/{id}")
    public CustomerResponseDto update(
            @NotNull @PathVariable("id") UUID id,
            @Valid @RequestBody CustomerEditDto updated
    ) {
        return customerService.update(id, updated);
    }

    @DeleteMapping("/{id}")
    public MessageResponseDto delete(@NotNull @PathVariable("id") UUID id) {
        customerService.delete(id);
        return MessageResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("Customer with id '%s' was deleted.".formatted(id))
                .build();
    }
}
