/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.cache;

import com.sap.cx.translations.labels.UsageType;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.regioncache.key.CacheKey;

/**
 * Cache key provider for localized labels and bundles
 */
public interface CxLocalizedLabelCacheKeyProvider {
	/**
	 * Creates the cache key for a single label
	 *
	 * @param labelKey       key of the label
	 * @param usageType      usage of the label
	 * @param catalogVersion catalog version of the label
	 * @return cache key
	 */
	CacheKey getSingleLabelKey(String labelKey, UsageType usageType, CatalogVersionModel catalogVersion);

	/**
	 * Creates the cache key for a bundle per usage types
	 *
	 * @param bundleKey      key/code of the bundle
	 * @param usageTypes     usage of the labels inside the bundle
	 * @param catalogVersion catalog version of the bundle
	 * @return cache key
	 */
	CacheKey getBundleKey(String bundleKey, UsageType[] usageTypes, CatalogVersionModel catalogVersion);

	/**
	 * Creates the cache key for all labels within given catalog version per usage types
	 *
	 * @param usageTypes     usage of the labels inside the bundle
	 * @param catalogVersion catalog version of labels
	 * @return cache key
	 */
	CacheKey getAllLabelsKey(UsageType[] usageTypes, CatalogVersionModel catalogVersion);
}
