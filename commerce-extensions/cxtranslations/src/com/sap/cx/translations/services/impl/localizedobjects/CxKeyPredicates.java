/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedValueModel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Builds Java {@link Predicate}s that filter on the {@code key} field of a
 * {@link CxLocalizedValueModel} from a {@link CxLocalizedValueSearchData}.
 *
 * <p>Only {@link CxLocalizedValueSearchData.SearchOperator#EQUALS} is a valid operator
 * for KEY criteria — enforced at {@link CxLocalizedValueSearchData} construction time.
 */
public class CxKeyPredicates {

    private CxKeyPredicates() {
    }

    /**
     * Returns predicates that match the full key string.
     */
    public static List<Predicate<String>> getKeyPredicates(final CxLocalizedValueSearchData searchData) {
        return searchData.criteria()
                         .stream()
                         .filter(crt -> crt.field() == CxLocalizedValueSearchData.SearchField.KEY)
                         .filter(crt -> StringUtils.isNotBlank(crt.value().stringValue()))
                         .map(crt -> {
                             if (crt.operator() == CxLocalizedValueSearchData.SearchOperator.EQUALS) {
                                 final String valueToSearchFor = crt.value().stringValue().toLowerCase();
                                 return (Predicate<String>) s -> s.toLowerCase().equals(valueToSearchFor);
                             }
                             return null;
                         })
                         .filter(Objects::nonNull)
                         .toList();
    }

    /**
     * Returns predicates that filter on the object-type segment (first dot-delimited part) of a key.
     */
    public static List<Predicate<String>> getObjectTypePredicates(final CxLocalizedValueSearchData searchData) {
        return searchData.criteria()
                         .stream()
                         .filter(crt -> crt.field() == CxLocalizedValueSearchData.SearchField.KEY)
                         .filter(crt -> StringUtils.isNotBlank(crt.value().stringValue()))
                         .map(crt -> {
                             if (crt.operator() == CxLocalizedValueSearchData.SearchOperator.EQUALS) {
                                 final String[] parts = crt.value().stringValue().split("\\.");
                                 return (Predicate<String>) s -> s.equals(parts[0]);
                             }
                             return null;
                         })
                         .filter(Objects::nonNull)
                         .toList();
    }

    /**
     * Returns a predicate that filters on the attribute segment (last dot-delimited part) of a key.
     * Returns an always-false predicate when the key has fewer than 3 parts.
     */
    public static Predicate<String> getAttributePredicateForKey(final CxLocalizedValueSearchData.SearchCriteria crt) {
        if (crt.operator() != CxLocalizedValueSearchData.SearchOperator.EQUALS) {
            return null;
        }
        if (crt.value() == null || StringUtils.isBlank(crt.value().stringValue())) {
            return s -> false;
        }
        final String[] parts = crt.value().stringValue().split("\\.");
        if (parts.length < 3) {
            return s -> false;
        }
        return s -> s.equals(parts[parts.length - 1]);
    }
}
