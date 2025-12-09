package com.halmber.springordersapi.controller;

import com.halmber.springordersapi.controller.annotation.PageableConstraints;
import com.halmber.springordersapi.model.OrderReportFilter;
import com.halmber.springordersapi.model.dto.request.order.OrderCreateDto;
import com.halmber.springordersapi.model.dto.request.order.OrderEditDto;
import com.halmber.springordersapi.model.dto.request.order.OrderFilterDto;
import com.halmber.springordersapi.model.dto.request.order.OrderReportFilterDto;
import com.halmber.springordersapi.model.dto.response.MessageResponseDto;
import com.halmber.springordersapi.model.dto.response.customer.CustomerListResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderImportResultDto;
import com.halmber.springordersapi.model.dto.response.order.OrderListResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderShortListResponseDto;
import com.halmber.springordersapi.model.mapper.OrderMapper;
import com.halmber.springordersapi.service.OrderImportService;
import com.halmber.springordersapi.service.OrderService;
import com.halmber.springordersapi.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Tag(name = "Orders", description = "Order management APIs")
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ReportService reportService;
    private final OrderMapper orderMapper;
    private final OrderImportService orderImportService;

    @Operation(
            summary = "Get paginated list of orders",
            description = "Retrieves a paginated list of orders. Supports sorting by amount, paymentMethod, status. " +
                    "Can retrieve multiple sorting fields at once if in query params. " +
                    "Pageable is recommended to be used in query (designed for that) or write status in string type.",
            responses = {
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
            }
    )
    @GetMapping
    public OrderListResponseDto getPageableList(
            @Parameter(description = "Pagination and sorting parameters. Recommended to be used in query (designed for that) or write status in string type.")
            @PageableConstraints(whitelist = {"status", "paymentMethod", "amount"})
            @PageableDefault(size = 5) Pageable pageable
    ) {
        return orderService.listOrders(pageable);
    }

    @Operation(
            summary = "Get order by ID",
            description = "Retrieves detailed information about a specific order including customer details",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Order found",
                            content = @Content(schema = @Schema(implementation = OrderResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Order not found",
                            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
                    )
            }
    )
    @GetMapping("/{id}")
    public OrderResponseDto getById(
            @Parameter(description = "Order ID", required = true)
            @NotNull @PathVariable("id") UUID id
    ) {
        return orderService.getById(id);
    }

    @Operation(
            summary = "Create new order",
            description = "Creates a new order for customer with the provided information",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Order created successfully",
                            content = @Content(schema = @Schema(implementation = OrderResponseDto.class))
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
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto create(
            @Parameter(description = "Order data", required = true)
            @Valid @RequestBody OrderCreateDto order
    ) {
        return orderService.create(order);
    }

    @Operation(
            summary = "Update order",
            description = """
                    Updates an existing order's information.
                    Status needs to be a valid value from StatusEnum, like "NEW".
                    PaymentMethod needs to be a valid value from StatusEnum, like "CARD"
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Order updated successfully",
                            content = @Content(schema = @Schema(implementation = OrderResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input data",
                            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Order not found",
                            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
                    )
            }
    )
    @PutMapping("/{id}")
    public OrderResponseDto update(
            @Parameter(description = "Order ID", required = true)
            @NotNull @PathVariable("id") UUID id,
            @Parameter(description = "Updated order data.", required = true)
            @Valid @RequestBody OrderEditDto updated
    ) {
        return orderService.update(id, updated);
    }

    @Operation(
            summary = "Get filtered and paginated list of orders",
            description = "Retrieves a paginated list of orders with optional filters by customerId, status and paymentMethod. " +
                    "Body is required to be json and allowed to be empty.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved list",
                            content = @Content(schema = @Schema(implementation = OrderListResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid filter or pagination parameters",
                            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
                    )
            }
    )
    @PostMapping("/_list")
    public OrderShortListResponseDto getList(
            @Parameter(description = "Order filter and pagination parameters. Required to be json. Allowed to be empty.", required = true)
            @Valid @RequestBody OrderFilterDto filter
    ) {
        return orderService.getFilteredPaginatedList(filter);
    }

    /**
     * Generates and downloads a report file (CSV or XLSX) with all orders matching the filters.
     * Uses streaming to handle large datasets efficiently without loading everything into memory.
     *
     * @param dto      Contains filtering criteria and fileType (csv or xlsx, default: csv)
     * @param response HttpServletResponse to write the file directly
     * @throws IOException if an error occurs during file generation
     */
    @Operation(
            summary = "Generate report of orders",
            description = "Generates and downloads a report file (Excel .xlsx or CSV .csv) with all orders matching the filter criteria. " +
                    "Use fileType to specify output format: 'xlsx' or 'csv' (default). Body is required to be json and allowed to be empty."
    )
    @PostMapping(value = "/_report")
    public void generateReport(
            @Parameter(description = "Filter parameters for report generation. Required to be json. Allowed to be empty.")
            @Valid @RequestBody OrderReportFilterDto dto,
            HttpServletResponse response
    ) throws IOException {

        OrderReportFilter filter = orderMapper.toOrderReportFilter(dto);

        log.info("Generating report: fileType={}, filters: customerId={}, status={}, paymentMethod={}",
                filter.fileType(), filter.customerId(), filter.status(), filter.paymentMethod());

        UUID customerId = orderService.parseAndValidateUUID(dto.customerId());

        String filename = "orders_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + filter.fileType().getExtension();

        response.setContentType(filter.fileType().getMimeType());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        try {
            reportService.generateReport(
                    customerId,
                    filter.status(),
                    filter.paymentMethod(),
                    filter.fileType(),
                    response.getOutputStream()
            );
            response.getOutputStream().flush();
            log.info("Report generated successfully: {}", filename);
        } catch (Exception e) {
            log.error("Error generating report", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            throw e;
        }
    }

    /**
     * Imports orders from JSON file.
     * Accepts JSON array of orders and validates/saves them to database.
     * Uses streaming parser to handle large files efficiently.
     *
     * @param file JSON file containing array of orders
     * @return Import statistics with success/failure counts and error details
     * @throws IOException if file cannot be read
     */
    @Operation(
            summary = "Import orders from JSON file",
            description = "Accepts a JSON file containing an array of orders and validates/saves them to the database. " +
                    "Uses streaming parser to handle large files efficiently. " +
                    "Returns statistics with success/failure counts and detailed error information for failed imports.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Import completed (may contain partial failures)",
                            content = @Content(schema = @Schema(implementation = OrderImportResultDto.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid file format or unreadable file",
                            content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
            }
    )
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OrderImportResultDto uploadOrders(
            @Parameter(
                    description = "JSON file containing array of orders. Each order should have: " +
                            "orderId, customerId, amount, status, paymentMethod",
                    required = true
            )
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.info("Received upload request: filename={}, size={}",
                file.getOriginalFilename(), file.getSize());

        OrderImportResultDto result = orderImportService.importOrders(file);

        log.info("Upload completed: {} successful, {} failed out of {} total",
                result.successfulImports(), result.failedImports(), result.totalRecords());

        return result;
    }

    @Operation(
            summary = "Delete order",
            description = "Deletes an order",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Order deleted successfully",
                            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Order not found",
                            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
                    )
            }
    )
    @DeleteMapping("/{id}")
    public MessageResponseDto delete(
            @Parameter(description = "Order ID", required = true)
            @NotNull @PathVariable("id") UUID id
    ) {
        orderService.delete(id);
        return MessageResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("Order with id '%s' was deleted.".formatted(id))
                .build();
    }
}
