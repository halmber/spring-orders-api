package com.halmber.springordersapi.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportFileTypeEnum {
    CSV("text/csv", ".csv"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String mimeType;
    private final String extension;

    public static ReportFileTypeEnum fromString(String value) {
        if (value == null || value.isBlank()) {
            return CSV; // Default value
        }
        try {
            return ReportFileTypeEnum.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid file type: '%s'. Allowed values: csv, xlsx".formatted(value)
            );
        }
    }
}
