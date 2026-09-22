/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates unique FlexibleSearch parameter names for a single query-building scope.
 *
 * <p>Each call to {@link #next(String)} appends an auto-incrementing counter to the given seed,
 * producing names like {@code objectType0}, {@code objectType1}, etc. This replaces the
 * threaded {@code MutableInt counter} pattern where a single counter was passed across many
 * methods to synthesise unique names.
 *
 * <p><strong>Design contract:</strong> All query parts whose parameters will be merged into
 * the same {@link de.hybris.platform.servicelayer.search.FlexibleSearchQuery} must either share
 * one {@code CxParamNameGenerator} instance <em>or</em> use non-overlapping seed prefixes.
 * When multiple instances are necessary (e.g. the recursive value-building in
 * {@link CxLocalizedObjectStandardProvider#buildQueryPartByValue}), use a distinctive seed
 * prefix (e.g. {@code "val_"}) to guarantee uniqueness.
 *
 * <p>Not thread-safe — one instance per query-building invocation.
 */
public class CxParamNameGenerator {

    private final Map<String, Integer> seedCounts = new HashMap<>();

    /**
     * Returns the next unique name for the given seed.
     * First call for {@code "catalogId"} returns {@code "catalogId0"},
     * second call returns {@code "catalogId1"}, and so on.
     *
     * @param seed a descriptive base name for the parameter (e.g. {@code "objectType"}, {@code "catalogId"})
     * @return a unique parameter name for this scope
     */
    public String next(final String seed) {
        final int count = seedCounts.merge(seed, 0, (existing, ignored) -> existing + 1);
        return seed + count;
    }
}
