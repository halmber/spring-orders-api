package com.halmber.springordersapi.controller.resolver;

import com.halmber.springordersapi.controller.annotation.PageableConstraints;
import com.halmber.springordersapi.controller.exception.InvalidRequestParameterException;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Set;

/**
 * Custom {@link PageableHandlerMethodArgumentResolver} that validates {@link Pageable} parameters.
 * <p>
 * Checks that page and size parameters are positive integers. Validates sorting according to
 * {@link PageableConstraints} annotation if present on the method parameter.
 * </p>
 *
 * <p><b>Behavior:</b></p>
 * <ul>
 *     <li>If page or size is negative or not a valid integer, throws {@link InvalidRequestParameterException}.</li>
 *     <li>If {@link PageableConstraints} annotation is present:
 *         <ul>
 *             <li>Sorting is restricted to fields in the whitelist, if specified.</li>
 *             <li>Sorting cannot include fields in the blacklist, if specified.</li>
 *             <li>If both whitelist and blacklist are empty, sorting is completely forbidden.</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p>This resolver ensures that invalid page, size, or sort parameters
 * are rejected before entering controller logic.</p>
 */
@Slf4j
public class PageableConstraintResolver extends PageableHandlerMethodArgumentResolver {
    @Override
    public @NonNull Pageable resolveArgument(@NonNull MethodParameter methodParameter,
                                             @Nullable ModelAndViewContainer mavContainer,
                                             @NonNull NativeWebRequest webRequest,
                                             @Nullable WebDataBinderFactory binderFactory) {
        validatePageAndSizeOrThrow(webRequest);

        Pageable pageable = super.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);

        PageableConstraints annotation = methodParameter.getParameterAnnotation(PageableConstraints.class);
        if (annotation != null ) {
            validateSorting(annotation, pageable.getSort());
        }

        return pageable;
    }

    private void validatePageAndSizeOrThrow(NativeWebRequest req) {
        parsePositiveInt(req.getParameter("page"), "page");
        parsePositiveInt(req.getParameter("size"), "size");
    }

    private void parsePositiveInt(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        try {
            if (Integer.parseInt(raw) < 0) {
                throw new InvalidRequestParameterException(
                        "'%s' must be >= 0, but got: %s".formatted(field, raw)
                );
            }
        } catch (NumberFormatException e) {
            throw new InvalidRequestParameterException(
                    "'%s' must be a valid integer, but got: %s".formatted(field, raw)
            );
        }
    }

    private void validateSorting(PageableConstraints annotation, Sort sort) {
        Set<String> whitelist = Set.of(annotation.whitelist());
        Set<String> blacklist = Set.of(annotation.blacklist());

        boolean whitelistEmpty = whitelist.isEmpty();
        boolean blacklistEmpty = blacklist.isEmpty();

        if (whitelistEmpty && blacklistEmpty) {
            if (sort.isSorted()) {
                throw new InvalidRequestParameterException("Sorting is forbidden");
            }
            return;
        }

        for (Sort.Order order : sort) {
            String field = order.getProperty();

            if (!whitelistEmpty && !whitelist.contains(field)) {
                throw new InvalidRequestParameterException(
                        "Sorting by field '%s' is not allowed. Allowed fields: %s"
                                .formatted(field, whitelist)
                );
            }

            if (!blacklistEmpty && blacklist.contains(field)) {
                throw new InvalidRequestParameterException(
                        "Sorting by field '%s' is forbidden. Forbidden fields: %s"
                                .formatted(field, blacklist)
                );
            }
        }
    }
}
