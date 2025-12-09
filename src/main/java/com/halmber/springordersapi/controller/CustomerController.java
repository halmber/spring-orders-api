package com.halmber.springordersapi.controller;

import com.halmber.springordersapi.controller.annotation.PageableConstraints;
import com.halmber.springordersapi.model.dto.request.customer.CustomerCreateDto;
import com.halmber.springordersapi.model.dto.request.customer.CustomerEditDto;
import com.halmber.springordersapi.model.dto.response.MessageResponseDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerListResponseDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerResponseDto;
import com.halmber.springordersapi.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Customers", description = "Customer management APIs")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(
            summary = "Get paginated list of customers",
            description = "Retrieves a paginated list of customers. Supports sorting by firstName, lastName, and city. " +
                    "Can retrieve multiple sorting fields at once if in query params. " +
                    "Pageable is recommended to be used in query (designed for that) or write status in string type"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved list",
                    content = @Content(schema = @Schema(implementation = CustomerListResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination or sort parameters",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
            )
    })
    @GetMapping
    public CustomerListResponseDto getPageableList(
            @Parameter(description = "Pagination and sorting parameters. Recommended to be used in query (designed for that) or write status in string type")
            @PageableConstraints(whitelist = {"firstName", "lastName", "city"})
            @PageableDefault(size = 5) Pageable pageable) {
        return customerService.listCustomers(pageable);
    }

    @Operation(
            summary = "Get customer by ID",
            description = "Retrieves detailed information about a specific customer"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer found",
                    content = @Content(schema = @Schema(implementation = CustomerResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
            )
    })
    @GetMapping("/{id}")
    public CustomerResponseDto getById(
            @Parameter(description = "Customer ID", required = true)
            @NotNull @PathVariable("id") UUID id) {
        return customerService.getById(id);
    }


    @Operation(
            summary = "Create new customer",
            description = "Creates a new customer with the provided information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Customer created successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Customer with this email already exists",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponseDto create(
            @Parameter(description = "Customer data", required = true)
            @Valid @RequestBody CustomerCreateDto customer) {
        return customerService.create(customer);
    }

    @Operation(
            summary = "Update customer",
            description = "Updates an existing customer's information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer updated successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
            )
    })
    @PutMapping("/{id}")
    public CustomerResponseDto update(
            @Parameter(description = "Customer ID", required = true)
            @NotNull @PathVariable("id") UUID id,
            @Parameter(description = "Updated customer data", required = true)
            @Valid @RequestBody CustomerEditDto updated
    ) {
        return customerService.update(id, updated);
    }

    @Operation(
            summary = "Delete customer",
            description = "Deletes a customer and all associated orders"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer deleted successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
            )
    })
    @DeleteMapping("/{id}")
    public MessageResponseDto delete(
            @Parameter(description = "Customer ID", required = true)
            @NotNull @PathVariable("id") UUID id) {
        customerService.delete(id);
        return MessageResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("Customer with id '%s' was deleted.".formatted(id))
                .build();
    }
}
