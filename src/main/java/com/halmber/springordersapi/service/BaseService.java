package com.halmber.springordersapi.service;

import com.halmber.springordersapi.controller.exception.InvalidRequestParameterException;
import com.halmber.springordersapi.repository.BaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public abstract class BaseService<T, ID> {
    protected abstract BaseRepository<T, ID> getRepository();

    protected IllegalStateException notFound(ID id) {
        String source = this.getClass().getSimpleName().split("(?=[A-Z])")[0];

        return new IllegalStateException("%s with id '%s' not found".formatted(source, id));
    }

    protected T findByIdOrThrow(ID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> notFound(id));
    }

    private boolean notExistsById(ID id) {
        return !getRepository().existsById(id);
    }

    /**
     * Checks if entity exists by ID, throws exception if not found.
     *
     * @param id Entity ID
     * @throws IllegalStateException if entity doesn't exist
     */
    public void existsByIdOrThrow(ID id) {
        if (notExistsById(id)) {
            throw notFound(id);
        }
    }

    /**
     * Parses and validates UUID string.
     *
     * @param uuidString String representation of UUID
     * @return Valid UUID or null if input is null/blank
     * @throws IllegalArgumentException if UUID format is invalid
     */
    public UUID parseAndValidateUUID(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestParameterException(
                    "Invalid UUID format: %s".formatted(uuidString)
            );
        }
    }

    @Transactional
    public void delete(ID id) {
        existsByIdOrThrow(id);
        getRepository().deleteById(id);
    }
}
