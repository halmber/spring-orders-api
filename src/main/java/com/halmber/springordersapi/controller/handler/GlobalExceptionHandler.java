package com.halmber.springordersapi.controller.handler;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.halmber.springordersapi.controller.exception.InvalidRequestParameterException;
import com.halmber.springordersapi.model.dto.response.MessageResponseDto;
import com.halmber.springordersapi.service.exeption.AlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Builds a {@link MessageResponseDto} API error response and logs it at WARN level.
     */
    private MessageResponseDto buildAndLog(HttpStatus status, String prefix, Exception ex) {
        log.warn("{}: {}", prefix, ex.getMessage());

        return MessageResponseDto.builder()
                .status(status.value())
                .message(ex.getMessage())
                .build();
    }

    /**
     * Handles business-level "not found" cases.
     * Typically, thrown when an entity cannot be located in the database.
     *
     * @param ex thrown IllegalStateException
     * @return 404 NOT_FOUND response with message
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public MessageResponseDto handleNotFound(IllegalStateException ex) {
        return buildAndLog(HttpStatus.NOT_FOUND, "Resource not found", ex);
    }

    /**
     * Handles validation errors coming from @Valid annotated request bodies.
     * Aggregates all field errors into a readable map.
     *
     * @param ex MethodArgumentNotValidException
     * @return 400 BAD_REQUEST with field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponseDto handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

        log.warn("Validation failed: {}", fieldErrors);

        return MessageResponseDto.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed: " + fieldErrors)
                .build();
    }

    /**
     * Handles invalid request parameters and all unexpected exceptions.
     *
     * @param ex any runtime exception
     * @return 400 BAD_REQUEST
     */
    @ExceptionHandler(InvalidRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponseDto handleInvalidParams(RuntimeException ex) {
        return buildAndLog(HttpStatus.BAD_REQUEST, "Invalid request (%s): ".formatted(ex.getClass().getName()), ex);
    }

    /**
     * Handles validation of request parameters annotated with @Valid
     * (e.g. @RequestParam, @PathVariable validation).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponseDto handleMethodValidation(HandlerMethodValidationException ex) {
        log.warn("Method validation failed: {}", ex.getMessage());
        return MessageResponseDto.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation error: " + Arrays.toString(ex.getDetailMessageArguments()))
                .build();
    }

    /**
     * Handles conflicts when trying to create a resource that already exists.
     */
    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public MessageResponseDto handleAlreadyExists(AlreadyExistsException ex) {
        return buildAndLog(HttpStatus.CONFLICT, "Conflict", ex);
    }

    /**
     * Handles Jackson Unknown JSON fields.
     * Triggered when FAIL_ON_UNKNOWN_PROPERTIES is enabled.
     */
    @ExceptionHandler(UnrecognizedPropertyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponseDto handleUnknownProperties(UnrecognizedPropertyException ex) {
        log.warn("Unknown property: {}", ex.getPropertyName());
        return MessageResponseDto.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Unknown property: " + ex.getPropertyName())
                .build();
    }

    /**
     * Handles malformed JSON and unreadable request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponseDto handleRequestReadingFails(HttpMessageNotReadableException ex) {
        log.warn("Request body parsing failed: {}", ex.getMessage());
        return MessageResponseDto.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Malformed JSON or invalid request body.")
                .build();
    }

    /**
     * Handles max file upload size violations.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponseDto handleFileSize(MaxUploadSizeExceededException ex) {
        return buildAndLog(HttpStatus.BAD_REQUEST, "File size too large", ex);
    }

    /**
     * Fallback handler for any other unhandled exceptions.
     * Prevents stacktrace from leaking to clients.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponseDto handleUnexpected(Exception ex) {
        return buildAndLog(HttpStatus.BAD_REQUEST, "Unexpected error", ex);
    }
}
