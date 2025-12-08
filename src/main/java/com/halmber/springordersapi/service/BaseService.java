package com.halmber.springordersapi.service;

import com.halmber.springordersapi.controller.exception.InvalidRequestParameterException;
import com.halmber.springordersapi.repository.BaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Abstract base service providing common CRUD operations for entities.
 * All service classes should extend this class to inherit basic functionality
 * such as finding, checking existence, and deleting entities.
 *
 * @param <T>  the entity type
 * @param <ID> the type of entity identifier
 */
public abstract class BaseService<T, ID> {
    /**
     * Returns the repository instance for performing database operations.
     * Must be implemented by subclasses to provide their specific repository.
     */
    protected abstract BaseRepository<T, ID> getRepository();

    /**
     * Creates an exception indicating that an entity was not found.
     *
     * @param id the identifier of the entity that was not found
     * @return {@link IllegalStateException} with a descriptive message
     */
    protected IllegalStateException notFound(ID id) {
        String source = this.getClass().getSimpleName().split("(?=[A-Z])")[0];

        return new IllegalStateException("%s with id '%s' not found".formatted(source, id));
    }

    /**
     * Finds an entity by its identifier or throws an exception if not found.
     *
     * @param id the entity identifier
     * @return the found entity
     * @throws IllegalStateException if entity doesn't exist
     */
    protected T findByIdOrThrow(ID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> notFound(id));
    }

    /**
     * Checks if entity does not exist by identifier.
     *
     * @param id the entity identifier
     * @return true if entity doesn't exist, false otherwise
     */
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

    /**
     * Deletes an entity by its identifier.
     * First checks if the entity exists, then performs deletion.
     *
     * @param id the identifier of the entity to delete
     * @throws IllegalStateException if entity doesn't exist
     */
    @Transactional
    public void delete(ID id) {
        existsByIdOrThrow(id);
        getRepository().deleteById(id);
    }
}
