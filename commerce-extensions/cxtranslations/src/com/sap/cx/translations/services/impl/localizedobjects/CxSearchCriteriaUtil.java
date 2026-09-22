/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedValueModel;

import java.util.List;
import java.util.function.Predicate;

/**
 * Façade that assembles all predicate types from a {@link CxLocalizedValueSearchData}.
 *
 * <p>The individual predicate builders have been extracted to focused classes:
 * <ul>
 *   <li>{@link CxKeyPredicates} — key and object-type predicates</li>
 *   <li>{@link CxAttributePredicates} — attribute predicates (from ATTRIBUTE and KEY criteria)</li>
 *   <li>{@link CxValuePredicates} — translated-value predicates</li>
 * </ul>
 * This class keeps the existing single-entry-point API for callers that import it directly.
 */
public class CxSearchCriteriaUtil {

    private CxSearchCriteriaUtil() {
    }

    public static List<Predicate<String>> getObjectTypePredicates(final CxLocalizedValueSearchData searchData) {
        return CxKeyPredicates.getObjectTypePredicates(searchData);
    }

    public static List<Predicate<String>> getAttributePredicates(final CxLocalizedValueSearchData searchData) {
        return CxAttributePredicates.getAttributePredicates(searchData);
    }

    public static List<Predicate<CxLocalizedValueModel>> getValuePredicates(final CxLocalizedValueSearchData searchData) {
        return CxValuePredicates.getValuePredicates(searchData);
    }

    public static List<Predicate<String>> getKeyPredicates(final CxLocalizedValueSearchData searchData) {
        return CxKeyPredicates.getKeyPredicates(searchData);
    }
}
