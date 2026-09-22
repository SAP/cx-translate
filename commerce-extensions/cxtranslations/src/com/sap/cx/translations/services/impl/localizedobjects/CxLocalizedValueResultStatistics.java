/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import org.apache.commons.lang3.LongRange;

import java.util.Map;

/**
 * Contains general statistics of query results.
 *
 * @param objectCount how many objects were found
 * @param valueCount  how many values were found
 * @param ranges      ranges of values for selected fields (created and modified time)
 */
public record CxLocalizedValueResultStatistics(
		int objectCount,
		int valueCount,
		Map<CxLocalizedValueSearchData.SortField, LongRange> ranges
) {
    /** Sentinel for a type with no matching objects/values — avoids null checks at call sites. */
    public static final CxLocalizedValueResultStatistics EMPTY = new CxLocalizedValueResultStatistics(0, 0);

    public CxLocalizedValueResultStatistics(final int objectCount, final int valueCount) {
        this(objectCount, valueCount, Map.of());
    }
}
