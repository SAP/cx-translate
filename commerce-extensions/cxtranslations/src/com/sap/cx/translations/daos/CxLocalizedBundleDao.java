/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.daos;

import com.sap.cx.translations.model.CxLocalizedBundleModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;

import java.util.Collection;
import java.util.Optional;

/**
 * Localized bundle DAO
 */
public interface CxLocalizedBundleDao {
	/**
	 * Finds bundles for the given catalog version
	 *
	 * @param catalogVersion of bundles
	 * @return found bundles
	 */
	Collection<CxLocalizedBundleModel> findBundlesByCatalogVersion(CatalogVersionModel catalogVersion);

	/**
	 * Gets the bundle for the given code and catalog version
	 *
	 * @param code           of the bundle
	 * @param catalogVersion of the bundle
	 * @return found bundle if exists or empty
	 */
	Optional<CxLocalizedBundleModel> findBundleByCode(String code, CatalogVersionModel catalogVersion);
}
