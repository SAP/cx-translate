/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.services;

import com.sap.cx.translations.labels.UsageType;
import com.sap.cx.translations.model.CxLocalizedBundleModel;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.user.UserModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Translations service serving labels and bundles
 */
public interface CxTranslationsService {

	/**
	 * Gets all labels for the given catalog version and usage type (optional)
	 *
	 * @param catalogVersion of labels to return
	 * @param usageType      of the labels to return: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                       {@link CxLocalizedLabelModel#FORFRONTEND} set to {@literal true} ({@link UsageType#BACKEND}
	 *                       to retrieve labels with {@link CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return available labels
	 */
	Collection<CxLocalizedLabelModel> getLabels(CatalogVersionModel catalogVersion, UsageType... usageType);

	/**
	 * Gets all labels for the given bundle, catalog version and usage type (optional)
	 *
	 * @param bundleCode     of bundle for which labels should be retrieved
	 * @param catalogVersion of labels to return
	 * @param usageType      of the labels to return: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                       {@link CxLocalizedLabelModel#FORFRONTEND} set to {@literal true} ({@link UsageType#BACKEND}
	 *                       to retrieve labels with {@link CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return available labels for the given bundle
	 */
	Collection<CxLocalizedLabelModel> getLabels(String bundleCode, CatalogVersionModel catalogVersion,
												UsageType... usageType);

	/**
	 * Gets the label for a given key and catalog version
	 *
	 * @param key            of the label
	 * @param catalogVersion of the label
	 * @return found label if exists or empty
	 */
	Optional<CxLocalizedLabelModel> getLabel(String key, CatalogVersionModel catalogVersion);

	/**
	 * Gets the label for the given key, catalog version and usage type
	 *
	 * @param key            of the label
	 * @param catalogVersion of the label
	 * @param usageType      of the label: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                       {@link CxLocalizedLabelModel#FORFRONTEND} set to {@literal true} ({@link UsageType#BACKEND}
	 *                       to retrieve labels with {@link CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return found label if exists or empty
	 */
	Optional<CxLocalizedLabelModel> getLabel(String key, CatalogVersionModel catalogVersion, UsageType usageType);

	/**
	 * Gets or creates the new label (unsaved) for the given key, catalog version and usage type
	 *
	 * @param key            of the label
	 * @param catalogVersion of the label
	 * @param usageType      of the label: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                       {@link CxLocalizedLabelModel#FORFRONTEND} set to {@literal true} ({@link UsageType#BACKEND}
	 *                       to retrieve labels with {@link CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return existing or new (unsaved) label
	 */
	CxLocalizedLabelModel getLabelOrCreate(String key, CatalogVersionModel catalogVersion, UsageType usageType);

	/**
	 * Get all bundles for the given catalog version
	 *
	 * @param catalogVersion of the bundle
	 * @return all available bundles
	 */
	Collection<CxLocalizedBundleModel> getBundles(CatalogVersionModel catalogVersion);

	/**
	 * Gets the bundle for the given code and catalog version
	 *
	 * @param code           of the bundle
	 * @param catalogVersion of the bundle
	 * @return found bundle if exists or empty
	 */
	Optional<CxLocalizedBundleModel> getBundle(String code, CatalogVersionModel catalogVersion);

	/**
	 * Gets or creates the new bundle (saved) for the given code and catalog version
	 *
	 * @param code           of the bundle
	 * @param catalogVersion of the bundle
	 * @return existing or new (saved) bundle
	 */
	CxLocalizedBundleModel getBundleOrCreate(String code, CatalogVersionModel catalogVersion);

	/**
	 * Clear labels cache
	 */
	void clearLabelsCache();


	/**
	 * Gets label keys matching the given key term for the catalog version
	 *
	 * @param catalogVersion of labels to return
	 * @param keyTerm        part of the label key
	 * @return matching keys
	 */
	List<String> getKeys(CatalogVersionModel catalogVersion, String keyTerm);

	/**
	 * Checks if fronted component for translations should be visible for given user
	 *
	 * @param user user to check
	 * @return true if translation should be allowed / component visible
	 */
	Boolean isFrontendTranslationAllowedForUser(UserModel user);
}
