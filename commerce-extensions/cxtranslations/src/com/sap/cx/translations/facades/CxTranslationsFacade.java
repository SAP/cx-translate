/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.facades;


import com.sap.cx.translations.labels.CxLocalizedLabel;
import com.sap.cx.translations.labels.UsageType;

import java.util.List;
import java.util.Map;

/**
 * Translations facade for serving and modifying localized labels
 */
public interface CxTranslationsFacade {

	/**
	 * Gets map key:value of translations for given usage types (optional) within the session content catalog and in
	 * session language. Uses cached value if available - see {@literal  cxtranslations.cache.enabled}
	 *
	 * @param usageType of the labels to return: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                  ({@link UsageType#BACKEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return map of translations in session language
	 */
	Map<String, String> getLabelsMap(UsageType... usageType);

	/**
	 * Gets map key:value of translations for given usage types (optional) within the session content catalog and in
	 * session language. Uses cached value if available and the {@literal useCache} parameter is true
	 *
	 * @param useCache  {@literal true} to use cache, {@literal false} to skip cache
	 * @param usageType of the labels to return: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                  ({@link UsageType#BACKEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return map of translations in session language
	 */
	Map<String, String> getLabelsMap(boolean useCache, UsageType... usageType);

	/**
	 * Gets map key:value of translations for the given bundle and usage types (optional) within the session content
	 * catalog and in session language. Uses cached value if available - see {@literal  cxtranslations.cache.enabled}
	 *
	 * @param bundle    of labels to return
	 * @param usageType of the labels to return: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                  ({@link UsageType#BACKEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return map of translations in session language
	 */
	Map<String, String> getLabelsMap(String bundle, UsageType... usageType);

	/**
	 * Gets map key:value of translations for the given bundle and usage types (optional) within the session content
	 * catalog and in session language. Uses cached value if available and the {@literal useCache} parameter is true
	 *
	 * @param bundle    of labels to return
	 * @param useCache  {@literal true} to use cache, {@literal false} to skip cache
	 * @param usageType of the labels to return: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                  ({@link UsageType#BACKEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return map of translations in session language
	 */
	Map<String, String> getLabelsMap(String bundle, boolean useCache, UsageType... usageType);

	/**
	 * Gets the label for the given key and usage type within the session content catalog and in session language. Uses
	 * cached value if available - see {@literal  cxtranslations.cache.enabled}
	 *
	 * @param key       of the label
	 * @param usageType of the label to return: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                  ({@link UsageType#BACKEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return label value in session language
	 */
	String getLabel(String key, UsageType usageType);

	/**
	 * Gets the label for the given key and usage type within the staged content catalog and in session language. Uses
	 * cached value if available - see {@literal  cxtranslations.cache.enabled}
	 *
	 * @param key       of the label
	 * @param usageType of the label to return: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                  ({@link UsageType#BACKEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @return label value in session language
	 */
	String getStagedLabel(String key, UsageType usageType);


	/**
	 * Gets the label for the given key and usage type within the session content catalog and in session language. Uses
	 * cached value if available and the {@literal useCache} parameter is true
	 *
	 * @param key       of the label
	 * @param usageType of the label to return: ({@link UsageType#FRONTEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORFRONTEND} set to {@literal true}
	 *                  ({@link UsageType#BACKEND} to retrieve labels with
	 *                  {@link com.sap.cx.translations.model.CxLocalizedLabelModel#FORBACKEND} set to {@literal true}
	 * @param useCache  {@literal true} to use cache, {@literal false} to skip cache
	 * @return label value in session language
	 */
	String getLabel(String key, UsageType usageType, boolean useCache);

	/**
	 * Edits existing (or creates new) label. Invalidates cache for the (staged) bundle containing the updated label.
	 *
	 * @param label to be saved
	 */
	void editLabel(CxLocalizedLabel label);

	/**
	 * Imports map of translations (Bundle:[Key:Value]). Import can be disallowed by setting
	 * {@literal cxtranslations.import.allowed} property to {@literal false}
	 *
	 * @param translations to be imported
	 * @param language     of translations to import
	 */
	void importLabels(Map<String, Map<String, String>> translations, String language);

	/**
	 * Synchronise labels to online catalog and invalidate cache
	 *
	 * @return true if the operation finished successfully
	 */
	boolean publishTranslations();


	/**
	 * Gets the list of best matching keys for the given key term
	 *
	 * @param keyTerm start of the label key
	 * @return list of best matching keys for the given term
	 */
	List<String> getKeySuggestions(String keyTerm);


	/**
	 * Checks if fronted component for translations should be visible for asm agent from session or if no ASM the currently logged in customer
	 *
	 * @return true if translation should be allowed / component visible
	 */
	Boolean isFrontendTranslationAllowedForUser();

}
