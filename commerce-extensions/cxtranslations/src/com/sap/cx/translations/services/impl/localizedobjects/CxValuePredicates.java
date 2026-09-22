/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedValueModel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Predicate;

/**
 * Builds Java {@link Predicate}s that filter on the translated {@code value} field of a
 * {@link CxLocalizedValueModel} from a {@link CxLocalizedValueSearchData}.
 */
public class CxValuePredicates {

    private CxValuePredicates() {
    }

    /**
     * Returns predicates for all VALUE criteria in the given search data.
     */
    public static List<Predicate<CxLocalizedValueModel>> getValuePredicates(final CxLocalizedValueSearchData searchData) {
        return searchData.criteria()
                         .stream()
                         .filter(crt -> crt.field() == CxLocalizedValueSearchData.SearchField.VALUE)
                         .map(criteria -> switch (criteria.operator()) {
                             case EQUALS -> (Predicate<CxLocalizedValueModel>) valueItem -> {
                                 final String value = valueItem.getValue(criteria.value().locale());
                                 return criteria.value().stringValue().equalsIgnoreCase(value);
                             };
                             case STARTS_WITH -> (Predicate<CxLocalizedValueModel>) valueItem -> {
                                 final String value = valueItem.getValue(criteria.value().locale());
                                 return value != null && value.toLowerCase()
                                         .startsWith(criteria.value().stringValue().toLowerCase());
                             };
                             case ENDS_WITH -> (Predicate<CxLocalizedValueModel>) valueItem -> {
                                 final String value = valueItem.getValue(criteria.value().locale());
                                 return value != null && value.toLowerCase()
                                         .endsWith(criteria.value().stringValue().toLowerCase());
                             };
                             case LIKE -> (Predicate<CxLocalizedValueModel>) valueItem -> {
                                 final String value = valueItem.getValue(criteria.value().locale());
                                 return value != null && value.toLowerCase()
                                         .contains(criteria.value().stringValue().toLowerCase());
                             };
                             case IS_EMPTY -> (Predicate<CxLocalizedValueModel>) valueItem -> {
                                 final String value = valueItem.getValue(criteria.value().locale());
                                 return StringUtils.isBlank(value);
                             };
                             case IS_NOT_EMPTY -> (Predicate<CxLocalizedValueModel>) valueItem -> {
                                 final String value = valueItem.getValue(criteria.value().locale());
                                 return StringUtils.isNotBlank(value);
                             };
                             default -> null;
                         })
                         .toList();
    }
}
