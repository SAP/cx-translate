/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.cache.impl;

import com.sap.cx.translations.cache.CxLocalizedBundleCacheKey;
import com.sap.cx.translations.cache.CxLocalizedLabelCacheKey;
import com.sap.cx.translations.cache.CxLocalizedLabelCacheKeyProvider;
import com.sap.cx.translations.labels.UsageType;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.commerceservices.i18n.CommerceCommonI18NService;
import de.hybris.platform.core.Registry;
import de.hybris.platform.regioncache.key.CacheKey;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class CxLocalizedLabelCacheKeyProviderImpl implements CxLocalizedLabelCacheKeyProvider {
	private static final String KEY_SEPARATOR = "_";

	private CommerceCommonI18NService commerceCommonI18NService;

	@Override
	public CacheKey getSingleLabelKey(final String labelKey, final UsageType usageType, final CatalogVersionModel catalogVersion) {
		final String lang = getCurrentLanguageIsocode();
		final String catalogVersionId = catalogVersion.getPk().getLongValueAsString();
		final String usageTypeCode = usageType != null ? usageType.name() : "ALL";

		final String key = labelKey + KEY_SEPARATOR + lang + KEY_SEPARATOR + catalogVersionId + KEY_SEPARATOR + usageTypeCode;
		return new CxLocalizedLabelCacheKey(key, getCurrentTenantId());
	}

	@Override
	public CacheKey getBundleKey(final String bundleKey, final UsageType[] usageTypes, final CatalogVersionModel catalogVersion) {
		final String lang = getCurrentLanguageIsocode();
		final String catalogVersionId = catalogVersion.getPk().getLongValueAsString();
		final String usageTypeCode = usageTypes != null ? StringUtils.join(usageTypes, "|") : "ALL";

		final String key = bundleKey + KEY_SEPARATOR + lang + KEY_SEPARATOR + catalogVersionId + KEY_SEPARATOR + usageTypeCode;
		return new CxLocalizedBundleCacheKey(key, getCurrentTenantId());
	}

	@Override
	public CacheKey getAllLabelsKey(final UsageType[] usageTypes, final CatalogVersionModel catalogVersion) {
		final String lang = getCurrentLanguageIsocode();
		final String catalogVersionId = catalogVersion.getPk().getLongValueAsString();
		final String usageTypeCode = usageTypes != null ? StringUtils.join(usageTypes, "|") : "ALL";

		// Uses CxLocalizedBundleCacheKey intentionally — "all labels" entries share the bundle key space
		final String key = lang + KEY_SEPARATOR + catalogVersionId + KEY_SEPARATOR + usageTypeCode;
		return new CxLocalizedBundleCacheKey(key, getCurrentTenantId());
	}

	private String getCurrentLanguageIsocode() {
		return Objects.requireNonNull(getCommerceCommonI18NService().getCurrentLanguage(),
				"Current language must not be null").getIsocode();
	}

	protected String getCurrentTenantId() {
		return Registry.getCurrentTenant().getTenantID();
	}

	protected CommerceCommonI18NService getCommerceCommonI18NService() {
		return commerceCommonI18NService;
	}

	public void setCommerceCommonI18NService(final CommerceCommonI18NService commerceCommonI18NService) {
		this.commerceCommonI18NService = commerceCommonI18NService;
	}
}
