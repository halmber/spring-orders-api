package com.halmber.springordersapi.model.dto.response;

import lombok.Builder;

import java.time.Instant;

/**
 * A DTO that represents a message response with status and timestamp.
 * <p>
 * The timestamp is automatically set to the current time if not provided.
 * </p>
 *
 * @param message   The message to be included in the response.
 * @param status    The status code.
 * @param timestamp The timestamp of when the response was created. If null, the current time is used.
 */
@Builder
public record MessageResponseDto(
        String message,
        int status,
        Instant timestamp
) {
    public MessageResponseDto {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
