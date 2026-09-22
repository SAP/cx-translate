/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.daos;

import com.sap.cx.translations.labels.UsageType;
import com.sap.cx.translations.model.CxLocalizedBundleModel;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Localized label DAO
 */
public interface CxLocalizedLabelDao extends GenericDao<CxLocalizedLabelModel> {
	/**
	 * Finds labels for the given catalog version and usage type (optional)
	 *
	 * @param catalogVersion of labels to return
	 * @param usageType      of the labels to return:
	 *                       ({@link UsageType#FRONTEND} to retrieve labels with {@link CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                       ({@link UsageType#BACKEND} to retrieve labels with {@link CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return found labels
	 */
	Collection<CxLocalizedLabelModel> findLabelsByUsage(CatalogVersionModel catalogVersion, UsageType... usageType);

	/**
	 * Finds labels for the given bundle, catalog version and usage type (optional)
	 *
	 * @param bundle         for which labels should be retrieved
	 * @param catalogVersion of labels to return
	 * @param usageType      of the labels to return:
	 *                       ({@link UsageType#FRONTEND} to retrieve labels with {@link CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                       ({@link UsageType#BACKEND} to retrieve labels with {@link CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return found labels
	 */
	Collection<CxLocalizedLabelModel> findLabelsByBundleAndUsage(CxLocalizedBundleModel bundle, CatalogVersionModel catalogVersion, UsageType... usageType);

	/**
	 * Finds the label for the given key, catalog version and usage type
	 *
	 * @param key            of the label
	 * @param catalogVersion of the label
	 * @param usageType      of the label:
	 *                       ({@link UsageType#FRONTEND} to retrieve labels with {@link CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                       ({@link UsageType#BACKEND} to retrieve labels with {@link CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return found label if exists or empty
	 */
	Optional<CxLocalizedLabelModel> findLabelByKeyAndUsage(String key, CatalogVersionModel catalogVersion, UsageType usageType);

	/**
	 * Finds the label for the given key, catalog version and usage type
	 *
	 * @param keys           of labels
	 * @param catalogVersion of labels
	 * @param usageType      of labels:
	 *                       ({@link UsageType#FRONTEND} to retrieve labels with {@link CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                       ({@link UsageType#BACKEND} to retrieve labels with {@link CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return found labels
	 */
	Collection<CxLocalizedLabelModel> findLabelsByKeysAndUsage(Collection<String> keys, CatalogVersionModel catalogVersion, UsageType usageType);


	/**
	 * Find best matching label keys for given label key term
	 *
	 * @param keyTerm          part of the label key
	 * @param catalogVersion   Catalog version
	 * @param resultsSizeLimit limit for the number of results, for null - no limits
	 * @return list of best matching labels
	 */
	List<String> findLabelKeys(String keyTerm, CatalogVersionModel catalogVersion, Integer resultsSizeLimit);
}
