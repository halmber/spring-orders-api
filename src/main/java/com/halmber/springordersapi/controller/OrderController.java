package com.halmber.springordersapi.controller;

import com.halmber.springordersapi.controller.annotation.PageableConstraints;
import com.halmber.springordersapi.model.OrderReportFilter;
import com.halmber.springordersapi.model.dto.request.order.OrderCreateDto;
import com.halmber.springordersapi.model.dto.request.order.OrderEditDto;
import com.halmber.springordersapi.model.dto.request.order.OrderFilterDto;
import com.halmber.springordersapi.model.dto.request.order.OrderReportFilterDto;
import com.halmber.springordersapi.model.dto.response.MessageResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderImportResultDto;
import com.halmber.springordersapi.model.dto.response.order.OrderListResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderResponseDto;
import com.halmber.springordersapi.model.dto.response.order.OrderShortListResponseDto;
import com.halmber.springordersapi.model.mapper.OrderMapper;
import com.halmber.springordersapi.service.OrderImportService;
import com.halmber.springordersapi.service.OrderService;
import com.halmber.springordersapi.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ReportService reportService;
    private final OrderMapper orderMapper;
    private final OrderImportService orderImportService;

    @GetMapping
    public OrderListResponseDto getPageableList(
            @PageableConstraints(whitelist = {"status", "paymentMethod", "amount"})
            @PageableDefault(size = 5) Pageable pageable) {
        return orderService.listOrders(pageable);
    }

    @GetMapping("/{id}")
    public OrderResponseDto getById(@NotNull @PathVariable("id") UUID id) {
        return orderService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto create(@Valid @RequestBody OrderCreateDto order) {
        return orderService.create(order);
    }

    @PutMapping("/{id}")
    public OrderResponseDto update(
            @NotNull @PathVariable("id") UUID id,
            @Valid @RequestBody OrderEditDto updated
    ) {
        return orderService.update(id, updated);
    }

    @PostMapping("/_list")
    public OrderShortListResponseDto getList(@Valid @RequestBody OrderFilterDto filter) {
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
    @PostMapping("/_report")
    public void generateReport(
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
    @PostMapping("/upload")
    public OrderImportResultDto uploadOrders(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Received upload request: filename={}, size={}",
                file.getOriginalFilename(), file.getSize());

        OrderImportResultDto result = orderImportService.importOrders(file);

        log.info("Upload completed: {} successful, {} failed out of {} total",
                result.successfulImports(), result.failedImports(), result.totalRecords());

        return result;
    }

    @DeleteMapping("/{id}")
    public MessageResponseDto delete(@NotNull @PathVariable("id") UUID id) {
        orderService.delete(id);
        return MessageResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("Order with id '%s' was deleted.".formatted(id))
                .build();
    }
}
