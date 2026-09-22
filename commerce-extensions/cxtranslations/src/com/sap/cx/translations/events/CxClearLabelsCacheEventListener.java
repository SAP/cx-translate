/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.events;

import com.google.common.base.Suppliers;
import de.hybris.platform.regioncache.CacheController;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.event.impl.AbstractEventListener;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Clear labels cache event listener
 */
public class CxClearLabelsCacheEventListener extends AbstractEventListener<CxClearLabelsCacheEvent> {

	private static final String CACHE_REGION_NAME = "cxtranslations.cache.region";

	private CacheController cacheController;
	private ConfigurationService configurationService;

	private final Supplier<String> cacheRegion = Suppliers.memoizeWithExpiration(() -> configurationService.getConfiguration()
																										   .getString(CACHE_REGION_NAME), 1, TimeUnit.MINUTES);

	@Override
	protected void onEvent(CxClearLabelsCacheEvent clearLabelsCacheEvent) {
		final String cacheRegionName = cacheRegion.get();
		if (StringUtils.isNotBlank(cacheRegionName)) {
			cacheController.getRegions()
						   .stream()
						   .filter(region -> cacheRegionName.equals(region.getName()))
						   .forEach(region -> cacheController.clearCache(region));
		}
	}

	public void setCacheController(final CacheController cacheController) {
		this.cacheController = cacheController;
	}

	public void setConfigurationService(final ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
}
