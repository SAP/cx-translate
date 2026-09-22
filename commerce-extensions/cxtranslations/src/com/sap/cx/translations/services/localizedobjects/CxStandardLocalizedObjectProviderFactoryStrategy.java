/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedObjectConfigModel;
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedObjectStandardProvider;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValuesSearchSupport;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.type.TypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link CxLocalizedObjectProviderFactoryStrategy} for {@link CxStandardLocalizedObjectConfigModel}.
 */
public class CxStandardLocalizedObjectProviderFactoryStrategy implements CxLocalizedObjectProviderFactoryStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(CxStandardLocalizedObjectProviderFactoryStrategy.class);
	private static final int DEFAULT_VALUE_SEARCH_LIMIT = 50;
	private FlexibleSearchService flexibleSearchService;
	private ModelService modelService;
	private TypeService typeService;
	private int valueSearchLimit = DEFAULT_VALUE_SEARCH_LIMIT;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean canHandle(final CxLocalizedObjectConfigModel config) {
		if (!(config instanceof final CxStandardLocalizedObjectConfigModel standardConfig)) {
            return false;
        }
        if (standardConfig.getExtends() != null) {
			return false;
		}
		if (standardConfig.isCatalogAware() && CollectionUtils.isEmpty(standardConfig.getCatalogVersions())) {
			LOG.warn("Config '{}' is catalog-aware but has no catalog versions configured — no provider will be created. Add at least one catalog version.", standardConfig.getCode());
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CxLocalizedObjectProvider createProvider(final CxLocalizedValueSearchData searchData, final CxLocalizedObjectConfigModel config, final CxLocalizedValuesSearchSupport searchSupport) {
		final CxStandardLocalizedObjectConfigModel theConfig = (CxStandardLocalizedObjectConfigModel) config;
		return new CxLocalizedObjectStandardProvider(theConfig, searchData, searchSupport, flexibleSearchService, modelService, typeService, valueSearchLimit);
	}

	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService) {
		this.flexibleSearchService = flexibleSearchService;
	}

	public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}

	public void setTypeService(final TypeService typeService) {
		this.typeService = typeService;
	}

	public void setValueSearchLimit(final int valueSearchLimit) {
		this.valueSearchLimit = valueSearchLimit;
	}
}
