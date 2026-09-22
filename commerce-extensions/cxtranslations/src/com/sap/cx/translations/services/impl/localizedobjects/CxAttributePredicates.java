/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Builds Java {@link Predicate}s that filter on the {@code attribute} qualifier of a localized value
 * from a {@link CxLocalizedValueSearchData}.
 */
public class CxAttributePredicates {

    private CxAttributePredicates() {
    }

    /**
     * Returns predicates from all ATTRIBUTE and KEY criteria (KEY criteria contribute
     * an attribute-segment predicate).
     */
    public static List<Predicate<String>> getAttributePredicates(final CxLocalizedValueSearchData searchData) {
        return searchData.criteria()
                         .stream()
                         .map(crt -> switch (crt.field()) {
                             case ATTRIBUTE -> forAttributeCriteria(crt);
                             case KEY -> CxKeyPredicates.getAttributePredicateForKey(crt);
                             default -> null;
                         })
                         .filter(Objects::nonNull)
                         .toList();
    }

    private static Predicate<String> forAttributeCriteria(final CxLocalizedValueSearchData.SearchCriteria crt) {
        final String value = crt.value() != null ? crt.value().stringValue() : null;
        return switch (crt.operator()) {
            case EQUALS -> s -> StringUtils.equalsIgnoreCase(s, value);
            case LIKE -> s -> StringUtils.containsIgnoreCase(s, value);
            case STARTS_WITH -> s -> StringUtils.startsWithIgnoreCase(s, value);
            case ENDS_WITH -> s -> StringUtils.endsWithIgnoreCase(s, value);
            case IS_EMPTY -> StringUtils::isBlank;
            case IS_NOT_EMPTY -> StringUtils::isNotBlank;
        };
    }
}
