package com.halmber.springordersapi.model.dto.response.order;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderImportResultDto(
        int totalRecords,
        int successfulImports,
        int failedImports,
        List<ImportError> errors
) {
    @Builder
    public record ImportError(
            int lineNumber,
            String reason,
            String details
    ) {
    }
}
