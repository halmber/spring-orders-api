package com.halmber.springordersapi.controller.annotation;

import com.halmber.springordersapi.controller.exception.InvalidRequestParameterException;
import org.springframework.data.domain.Pageable;

import java.lang.annotation.*;

/**
 * Annotation for validating {@link Pageable} parameters in controller methods.
 * <p>
 * Can be applied to a method parameter of type {@link Pageable}.
 * Ensures that page and size are positive integers, and optionally
 * restricts sorting fields according to whitelist or blacklist rules.
 * </p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * @GetMapping("/customers")
 * public CustomerListResponseDto getPageableList(
 *         @PageableConstraints(whitelist = {"firstName", "lastName"})
 *         Pageable pageable) {
 *     return customerService.list(pageable);
 * }
 * }</pre>
 *
 * <p><b>Rules:</b></p>
 * <ul>
 *     <li>{@link #whitelist()} — allowed fields for sorting. If empty, whitelist check is disabled.</li>
 *     <li>{@link #blacklist()} — forbidden fields for sorting. If empty, blacklist check is disabled.</li>
 *     <li>If both whitelist and blacklist are empty, sorting is not allowed. Any sort parameter will throw {@link InvalidRequestParameterException}.</li>
 * </ul>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PageableConstraints {
    /**
     * Allowed sorting fields.
     */
    String[] whitelist() default {};

    /**
     * Forbidden sorting fields.
     */
    String[] blacklist() default {};
}
