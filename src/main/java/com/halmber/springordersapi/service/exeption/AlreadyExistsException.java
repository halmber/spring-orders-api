package com.halmber.springordersapi.service.exeption;

public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String message) {
        super(message);
    }

    public AlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyExistsException(String entity, String field, Object value) {
        super("%s with %s '%s' already exists".formatted(entity, field, value));
    }
}
