package com.halmber.springordersapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halmber.springordersapi.model.OrderReportFilter;
import com.halmber.springordersapi.model.dto.request.order.OrderCreateDto;
import com.halmber.springordersapi.model.dto.request.order.OrderEditDto;
import com.halmber.springordersapi.model.dto.request.order.OrderFilterDto;
import com.halmber.springordersapi.model.dto.request.order.OrderReportFilterDto;
import com.halmber.springordersapi.model.dto.response.order.*;
import com.halmber.springordersapi.model.enums.PaymentEnum;
import com.halmber.springordersapi.model.enums.ReportFileTypeEnum;
import com.halmber.springordersapi.model.enums.StatusEnum;
import com.halmber.springordersapi.model.mapper.OrderMapper;
import com.halmber.springordersapi.service.OrderImportService;
import com.halmber.springordersapi.service.OrderService;
import com.halmber.springordersapi.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
@DisplayName("OrderController Integration Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderMapper mapper;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private OrderImportService orderImportService;

    @Test
    @DisplayName("GET /api/orders - Should return paginated list of orders")
    void shouldReturnPaginatedListOfOrders() throws Exception {
        OrderResponseDto order1 = OrderResponseDto.builder()
                .id(UUID.randomUUID())
                .amount(100.0)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.CARD)
                .createdAt(Instant.now())
                .build();

        OrderResponseDto order2 = OrderResponseDto.builder()
                .id(UUID.randomUUID())
                .amount(200.0)
                .status(StatusEnum.PROCESSING)
                .paymentMethod(PaymentEnum.PAYPAL)
                .createdAt(Instant.now())
                .build();

        OrderListResponseDto response = OrderListResponseDto.builder()
                .orders(List.of(order1, order2))
                .totalPages(1)
                .build();

        when(orderService.listOrders(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(2)))
                .andExpect(jsonPath("$.orders[0].amount", is(100.0)))
                .andExpect(jsonPath("$.orders[1].amount", is(200.0)))
                .andExpect(jsonPath("$.totalPages", is(1)));

        verify(orderService).listOrders(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/orders - Should support sorting by allowed fields")
    void shouldSupportSortingByAllowedFields() throws Exception {
        OrderListResponseDto response = OrderListResponseDto.builder()
                .orders(List.of())
                .totalPages(0)
                .build();

        when(orderService.listOrders(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "status,asc"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "amount,desc"))
                .andExpect(status().isOk());

        verify(orderService, times(2)).listOrders(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/orders - Should reject sorting by forbidden fields")
    void shouldRejectSortingByForbiddenFields() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "customerId,asc")) // Not in whitelist
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not allowed")));

        verify(orderService, never()).listOrders(any());
    }

    @Test
    @DisplayName("GET /api/orders/{id} - Should return order by id")
    void shouldReturnOrderById() throws Exception {
        UUID orderId = UUID.randomUUID();

        OrderResponseDto response = OrderResponseDto.builder()
                .id(orderId)
                .amount(150.0)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.CARD)
                .createdAt(Instant.now())
                .build();

        when(orderService.getById(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.toString())))
                .andExpect(jsonPath("$.amount", is(150.0)))
                .andExpect(jsonPath("$.status", is("NEW")));

        verify(orderService).getById(orderId);
    }

    @Test
    @DisplayName("GET /api/orders/{id} - Should return 404 when order not found")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getById(orderId))
                .thenThrow(new IllegalStateException("Order with id '%s' not found".formatted(orderId)));

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("not found")));

        verify(orderService).getById(orderId);
    }

    @Test
    @DisplayName("POST /api/orders - Should create new order")
    void shouldCreateNewOrder() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OrderCreateDto createDto = OrderCreateDto.builder()
                .customerId(customerId.toString())
                .amount(150.0)
                .status(StatusEnum.NEW.name())
                .paymentMethod(PaymentEnum.CARD.name())
                .build();

        OrderResponseDto response = OrderResponseDto.builder()
                .id(orderId)
                .amount(150.0)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.CARD)
                .createdAt(Instant.now())
                .build();

        when(orderService.create(any(OrderCreateDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(orderId.toString())))
                .andExpect(jsonPath("$.amount", is(150.0)))
                .andExpect(jsonPath("$.status", is("NEW")));

        verify(orderService).create(any(OrderCreateDto.class));
    }

    @Test
    @DisplayName("POST /api/orders - Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        OrderCreateDto invalidDto = OrderCreateDto.builder()
                .customerId(null) // Required field
                .amount(-10.0) // Must be positive
                .status(StatusEnum.NEW.name())
                .paymentMethod(PaymentEnum.CARD.name())
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));

        verify(orderService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/orders - Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        UUID customerId = UUID.randomUUID();
        OrderCreateDto createDto = OrderCreateDto.builder()
                .customerId(customerId.toString())
                .amount(150.0)
                .status(StatusEnum.NEW.name())
                .paymentMethod(PaymentEnum.CARD.name())
                .build();

        when(orderService.create(any(OrderCreateDto.class)))
                .thenThrow(new IllegalStateException("Customer with id '%s' not found".formatted(customerId)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Customer")));

        verify(orderService).create(any(OrderCreateDto.class));
    }

    @Test
    @DisplayName("PUT /api/orders/{id} - Should update order")
    void shouldUpdateOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderEditDto editDto = OrderEditDto.builder()
                .amount(250.0)
                .status(StatusEnum.PROCESSING.name())
                .paymentMethod(PaymentEnum.PAYPAL.name())
                .build();

        OrderResponseDto response = OrderResponseDto.builder()
                .id(orderId)
                .amount(250.0)
                .status(StatusEnum.PROCESSING)
                .paymentMethod(PaymentEnum.PAYPAL)
                .createdAt(Instant.now())
                .build();

        when(orderService.update(eq(orderId), any(OrderEditDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.toString())))
                .andExpect(jsonPath("$.amount", is(250.0)))
                .andExpect(jsonPath("$.status", is("PROCESSING")))
                .andExpect(jsonPath("$.paymentMethod", is("PAYPAL")));

        verify(orderService).update(eq(orderId), any(OrderEditDto.class));
    }

    @Test
    @DisplayName("PUT /api/orders/{id} - Should return 404 when order not found")
    void shouldReturn404WhenUpdatingNonExistentOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderEditDto editDto = OrderEditDto.builder()
                .amount(250.0)
                .status(StatusEnum.PROCESSING.name())
                .paymentMethod(PaymentEnum.PAYPAL.name())
                .build();

        when(orderService.update(eq(orderId), any(OrderEditDto.class)))
                .thenThrow(new IllegalStateException("Order with id '%s' not found".formatted(orderId)));

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        verify(orderService).update(eq(orderId), any(OrderEditDto.class));
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} - Should delete order")
    void shouldDeleteOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        doNothing().when(orderService).delete(orderId);

        mockMvc.perform(delete("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", containsString("deleted")));

        verify(orderService).delete(orderId);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} - Should return 404 when order not found")
    void shouldReturn404WhenDeletingNonExistentOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        doThrow(new IllegalStateException("Order with id '%s' not found".formatted(orderId)))
                .when(orderService).delete(orderId);

        mockMvc.perform(delete("/api/orders/{id}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("not found")));

        verify(orderService).delete(orderId);
    }

    @Test
    @DisplayName("POST /api/orders/_list - Should return filtered and paginated list")
    void shouldReturnFilteredAndPaginatedList() throws Exception {
        UUID customerId = UUID.randomUUID();
        OrderFilterDto filter = OrderFilterDto.builder()
                .customerId(customerId.toString())
                .status(StatusEnum.NEW.name())
                .page(0)
                .size(20)
                .build();

        OrderShortResponseDto order1 = OrderShortResponseDto.builder()
                .id(UUID.randomUUID())
                .amount(100.0)
                .status(StatusEnum.NEW)
                .paymentMethod(PaymentEnum.CARD)
                .createdAt(Instant.now())
                .build();

        OrderShortListResponseDto response = OrderShortListResponseDto.builder()
                .orders(List.of(order1))
                .totalPages(1)
                .build();

        when(orderService.getFilteredPaginatedList(any(OrderFilterDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders/_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].amount", is(100.0)))
                .andExpect(jsonPath("$.totalPages", is(1)));

        verify(orderService).getFilteredPaginatedList(any(OrderFilterDto.class));
    }

    @Test
    @DisplayName("POST /api/orders/_list - Should handle empty filters")
    void shouldHandleEmptyFilters() throws Exception {
        OrderFilterDto filter = OrderFilterDto.builder()
                .page(0)
                .size(20)
                .build();

        OrderShortListResponseDto response = OrderShortListResponseDto.builder()
                .orders(List.of())
                .totalPages(0)
                .build();

        when(orderService.getFilteredPaginatedList(any(OrderFilterDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders/_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders", hasSize(0)))
                .andExpect(jsonPath("$.totalPages", is(0)));

        verify(orderService).getFilteredPaginatedList(any(OrderFilterDto.class));
    }

    @Test
    @DisplayName("POST /api/orders/_report - Should generate CSV report")
    void shouldGenerateCsvReport() throws Exception {
        UUID customerId = UUID.randomUUID();
        OrderReportFilterDto dto = OrderReportFilterDto.builder()
                .customerId(customerId.toString())
                .status(StatusEnum.NEW.name())
                .fileType(ReportFileTypeEnum.CSV.name().toLowerCase())
                .build();
        OrderReportFilter filter = OrderReportFilter.builder()
                .customerId(customerId)
                .status(StatusEnum.NEW)
                .fileType(ReportFileTypeEnum.CSV)
                .build();

        when(orderService.parseAndValidateUUID(customerId.toString()))
                .thenReturn(customerId);
        when(mapper.toOrderReportFilter(any())).thenReturn(filter);

        doNothing().when(reportService).generateReport(
                any(UUID.class),
                any(StatusEnum.class),
                any(),
                any(ReportFileTypeEnum.class),
                any()
        );

        mockMvc.perform(post("/api/orders/_report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition", containsString("orders_report")))
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(header().string("Content-Type", containsString("text/csv")));

        verify(reportService).generateReport(
                any(UUID.class),
                any(StatusEnum.class),
                any(),
                eq(ReportFileTypeEnum.CSV),
                any()
        );
    }

    @Test
    @DisplayName("POST /api/orders/_report - Should generate XLSX report")
    void shouldGenerateXlsxReport() throws Exception {
        UUID customerId = UUID.randomUUID();
        OrderReportFilterDto dto = OrderReportFilterDto.builder()
                .customerId(customerId.toString())
                .fileType(ReportFileTypeEnum.XLSX.name().toLowerCase())
                .build();

        OrderReportFilter filter = OrderReportFilter.builder()
                .fileType(ReportFileTypeEnum.XLSX)
                .customerId(customerId)
                .status(null)
                .paymentMethod(null)
                .build();

        when(mapper.toOrderReportFilter(any())).thenReturn(filter);
        doNothing().when(reportService).generateReport(
                any(),
                any(),
                any(),
                any(ReportFileTypeEnum.class),
                any()
        );

        mockMvc.perform(post("/api/orders/_report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));

        verify(reportService).generateReport(
                any(),
                any(),
                any(),
                eq(ReportFileTypeEnum.XLSX),
                any()
        );
    }

    @Test
    @DisplayName("POST /api/orders/_report - Should handle report generation error")
    void shouldHandleReportGenerationError() {
        UUID customerId = UUID.randomUUID();
        OrderReportFilterDto dto = OrderReportFilterDto.builder()
                .fileType("csv")
                .customerId(customerId.toString())
                .build();

        OrderReportFilter filter = OrderReportFilter.builder()
                .fileType(ReportFileTypeEnum.CSV)
                .customerId(customerId)
                .status(null)
                .paymentMethod(null)
                .build();

        when(mapper.toOrderReportFilter(any())).thenReturn(filter);
        doThrow(new RuntimeException("Report generation failed"))
                .when(reportService).generateReport(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> mockMvc.perform(post("/api/orders/_report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))))
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Report generation failed");

        verify(reportService).generateReport(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/orders/upload - Should import orders from JSON file")
    void shouldImportOrdersFromJsonFile() throws Exception {
        String jsonContent = """
                [
                    {
                        "customerId": "550e8400-e29b-41d4-a716-446655440000",
                        "amount": 100.0,
                        "status": "NEW",
                        "paymentMethod": "CARD"
                    }
                ]
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.json",
                "application/json",
                jsonContent.getBytes()
        );

        OrderImportResultDto result = OrderImportResultDto.builder()
                .totalRecords(1)
                .successfulImports(1)
                .failedImports(0)
                .errors(List.of())
                .build();

        when(orderImportService.importOrders(any())).thenReturn(result);

        mockMvc.perform(multipart("/api/orders/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", is(1)))
                .andExpect(jsonPath("$.successfulImports", is(1)))
                .andExpect(jsonPath("$.failedImports", is(0)))
                .andExpect(jsonPath("$.errors", hasSize(0)));

        verify(orderImportService).importOrders(any());
    }

    @Test
    @DisplayName("POST /api/orders/upload - Should handle import with errors")
    void shouldHandleImportWithErrors() throws Exception {
        String jsonContent = """
                [
                    {
                        "customerId": "invalid-uuid",
                        "amount": 100.0,
                        "status": "NEW",
                        "paymentMethod": "CARD"
                    }
                ]
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.json",
                "application/json",
                jsonContent.getBytes()
        );

        OrderImportResultDto.ImportError error = OrderImportResultDto.ImportError.builder()
                .lineNumber(1)
                .reason("Invalid customer ID format")
                .details("Expected UUID")
                .build();

        OrderImportResultDto result = OrderImportResultDto.builder()
                .totalRecords(1)
                .successfulImports(0)
                .failedImports(1)
                .errors(List.of(error))
                .build();

        when(orderImportService.importOrders(any())).thenReturn(result);

        mockMvc.perform(multipart("/api/orders/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", is(1)))
                .andExpect(jsonPath("$.successfulImports", is(0)))
                .andExpect(jsonPath("$.failedImports", is(1)))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].lineNumber", is(1)))
                .andExpect(jsonPath("$.errors[0].reason", is("Invalid customer ID format")));

        verify(orderImportService).importOrders(any());
    }

    @Test
    @DisplayName("POST /api/orders/upload - Should handle invalid file type")
    void shouldHandleInvalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.txt",
                "text/plain",
                "invalid content".getBytes()
        );

        when(orderImportService.importOrders(any()))
                .thenThrow(new IllegalArgumentException("Only JSON files are allowed"));

        mockMvc.perform(multipart("/api/orders/upload")
                        .file(file))
                .andExpect(status().isBadRequest());

        verify(orderImportService).importOrders(any());
    }

    @Test
    @DisplayName("POST /api/orders/upload - Should handle empty file")
    void shouldHandleEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.json",
                "application/json",
                new byte[0]
        );

        when(orderImportService.importOrders(any()))
                .thenThrow(new IllegalArgumentException("Uploaded file is empty"));

        mockMvc.perform(multipart("/api/orders/upload")
                        .file(file))
                .andExpect(status().isBadRequest());

        verify(orderImportService).importOrders(any());
    }
}
