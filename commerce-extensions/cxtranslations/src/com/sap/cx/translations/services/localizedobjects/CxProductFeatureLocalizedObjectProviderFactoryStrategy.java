/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedObjectConfigModel;
import com.sap.cx.translations.model.CxProductFeatureLocalizedObjectConfigModel;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValuesSearchSupport;
import com.sap.cx.translations.services.impl.localizedobjects.CxProductFeatureLocalizedObjectProvider;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import org.apache.commons.collections4.CollectionUtils;


/**
 * Implementation of {@link CxLocalizedObjectProviderFactoryStrategy} for {@link CxProductFeatureLocalizedObjectConfigModel}.
 */
public class CxProductFeatureLocalizedObjectProviderFactoryStrategy implements CxLocalizedObjectProviderFactoryStrategy {
	private static final int DEFAULT_VALUE_SEARCH_LIMIT = 50;
	private FlexibleSearchService flexibleSearchService;
	private int valueSearchLimit = DEFAULT_VALUE_SEARCH_LIMIT;
    private CxLocalizedClassificationService cxLocalizedClassificationService;
    private CommonI18NService commonI18NService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean canHandle(final CxLocalizedObjectConfigModel config) {
		return config instanceof final CxProductFeatureLocalizedObjectConfigModel featureConfig 
                && CollectionUtils.isNotEmpty(featureConfig.getProductCatalogVersions());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CxLocalizedObjectProvider createProvider(final CxLocalizedValueSearchData searchData, final CxLocalizedObjectConfigModel config, final CxLocalizedValuesSearchSupport searchSupport) {
		final CxProductFeatureLocalizedObjectConfigModel theConfig = (CxProductFeatureLocalizedObjectConfigModel) config;
		return new CxProductFeatureLocalizedObjectProvider(theConfig, flexibleSearchService, searchData, searchSupport, valueSearchLimit, cxLocalizedClassificationService, commonI18NService);
	}

	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService) {
		this.flexibleSearchService = flexibleSearchService;
	}


    public void setCxLocalizedClassificationService(final CxLocalizedClassificationService cxLocalizedClassificationService) {
        this.cxLocalizedClassificationService = cxLocalizedClassificationService;
    }

    public void setCommonI18NService(final CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }

    public void setValueSearchLimit(final int valueSearchLimit) {
		this.valueSearchLimit = valueSearchLimit;
	}
}
