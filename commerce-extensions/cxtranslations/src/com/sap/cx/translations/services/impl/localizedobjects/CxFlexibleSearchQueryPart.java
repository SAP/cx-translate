/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Immutable holder for a Flexible Search WHERE-clause fragment together with its
 * named parameters.
 *
 * <p>Use the static factory constants and the {@link #and(CxFlexibleSearchQueryPart...)} composer
 * instead of building SQL strings by hand:</p>
 * <pre>
 *   CxFlexibleSearchQueryPart part = CxFlexibleSearchQueryPart.and(
 *       CxFlexibleSearchQueryPart.of("{p.code} = ?pCode", Map.of("pCode", "p-001")),
 *       CxFlexibleSearchQueryPart.of("{p.catalogVersion} IN (?cvs)", Map.of("cvs", cvList))
 *   );
 * </pre>
 */
public final class CxFlexibleSearchQueryPart {

    /** Fragment that matches nothing — use to short-circuit queries when no results are possible. */
    public static final CxFlexibleSearchQueryPart ALWAYS_FALSE = new CxFlexibleSearchQueryPart("0=1", Collections.emptyMap());

    /** Fragment that matches everything — neutral element for AND compositions. */
    public static final CxFlexibleSearchQueryPart ALWAYS_TRUE = new CxFlexibleSearchQueryPart("1=1", Collections.emptyMap());

    private final String queryPart;
    private final Map<String, Object> parameters;

    public CxFlexibleSearchQueryPart(final String queryPart, final Map<String, Object> parameters) {
        this.queryPart = queryPart;
        this.parameters = parameters;
    }

    /** The SQL fragment (may contain {@code !!WHERE_CLAUSE!!} placeholder or sub-expressions). */
    public String queryPart() {
        return queryPart;
    }

    /** Named parameters to bind for this fragment. */
    public Map<String, Object> parameters() {
        return parameters;
    }

    /**
     * Combines multiple parts with {@code AND}, merging their parameter maps.
     * Returns {@link #ALWAYS_TRUE} for an empty input list.
     */
    public static CxFlexibleSearchQueryPart and(final CxFlexibleSearchQueryPart... parts) {
        return and(Arrays.asList(parts));
    }

    /**
     * Combines multiple parts with {@code AND}, merging their parameter maps.
     * Returns {@link #ALWAYS_TRUE} for an empty input list.
     */
    public static CxFlexibleSearchQueryPart and(final List<CxFlexibleSearchQueryPart> parts) {
        if (parts.isEmpty()) {
            return ALWAYS_TRUE;
        }
        final String sql = parts.stream()
                .map(CxFlexibleSearchQueryPart::queryPart)
                .collect(Collectors.joining(" AND "));
        final Map<String, Object> merged = new HashMap<>();
        parts.forEach(p -> merged.putAll(p.parameters()));
        return new CxFlexibleSearchQueryPart(sql, merged);
    }

    @Override
    public String toString() {
        return "CxFlexibleSearchQueryPart[sql=" + queryPart + ", params=" + parameters + "]";
    }
}
