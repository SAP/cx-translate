/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.cache;

import de.hybris.platform.regioncache.key.CacheKey;
import de.hybris.platform.regioncache.key.CacheUnitValueType;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Cache key for a bundle (of labels)
 */
public class CxLocalizedBundleCacheKey implements CacheKey {
	private static final String LOCALIZED_BUNDLE_CACHE_UNIT_CODE = "__LOCALIZED_BUNDLE__";

	private final String key;
	private final String tenantId;

	public CxLocalizedBundleCacheKey(String key, String tenantId) {
		this.key = requireNonNull(key, "key must not be null");
		this.tenantId = requireNonNull(tenantId, "tenantId must not be null");
	}

	@Override
	public CacheUnitValueType getCacheValueType() {
		return CacheUnitValueType.SERIALIZABLE;
	}

	@Override
	public Object getTypeCode() {
		return LOCALIZED_BUNDLE_CACHE_UNIT_CODE;
	}

	@Override
	public String getTenantId() {
		return tenantId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CxLocalizedBundleCacheKey that = (CxLocalizedBundleCacheKey) o;
		return Objects.equals(key, that.key) && Objects.equals(tenantId, that.tenantId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, tenantId);
	}

	@Override
	public String toString() {
		return "CxLocalizedBundleCacheKey{key='" + key + "', tenantId='" + tenantId + "'}";
	}
}
