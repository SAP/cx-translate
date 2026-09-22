/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedObjectConfigModel;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValuesSearchSupport;



/**
 * Factory Strategy for {@link CxLocalizedObjectProvider}s.
 */
public interface CxLocalizedObjectProviderFactoryStrategy {
	/**
	 * Return if the strategy is capable of creating of Provider based on the given config object.
	 *
	 * @param config config object.
	 * @return true - if the factory should be created using this strategy.
	 */
	boolean canHandle(CxLocalizedObjectConfigModel config);

	/**
	 * Creates a search provider.
	 *
	 * @param searchData    current search data
	 * @param config        config base on which the Provider should be created.
	 * @param searchSupport support object with useful methods
	 * @return Ready to use CxLocalizedObjectProvider
	 */
	CxLocalizedObjectProvider createProvider(CxLocalizedValueSearchData searchData, CxLocalizedObjectConfigModel config, CxLocalizedValuesSearchSupport searchSupport);
}
