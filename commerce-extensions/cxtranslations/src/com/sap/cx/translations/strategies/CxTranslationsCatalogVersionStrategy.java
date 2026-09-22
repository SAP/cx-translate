/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.strategies;

import de.hybris.platform.catalog.model.CatalogVersionModel;

/**
 * Translations catalog version selection strategy, which provides catalog version to be used to serve translations
 */
public interface CxTranslationsCatalogVersionStrategy {
	/**
	 * Gets the session content catalog version containing translations
	 *
	 * @return session catalog version
	 */
	CatalogVersionModel getSessionTranslationsCatalogVersion();

	/**
	 * Gets the staged content catalog version containing translations
	 *
	 * @return staged catalog version
	 */
	CatalogVersionModel getStagedTranslationsCatalogVersion();
}
