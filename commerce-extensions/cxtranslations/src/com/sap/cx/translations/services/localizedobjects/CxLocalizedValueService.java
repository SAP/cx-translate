/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData;
import com.sap.cx.translations.services.localizedobjects.parameters.CxPerformTranslationParameters;
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForItemsParameters;
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForValuesParameters;
import de.hybris.platform.core.model.ItemModel;

import java.util.Collection;
import java.util.List;

/**
 * Service interface for managing {@link CxLocalizedValueModel} objects.
 */
public interface CxLocalizedValueService {

	/**
	 * Refreshes the persistent state of the provided {@link CxLocalizedValueModel} from the database.
	 *
	 * @param value the localized value model to refresh.
	 */
	void refresh(CxLocalizedValueModel value);

	/**
	 * Saves the provided {@link CxLocalizedValueModel} to the database.
	 *
	 * @param value the localized value model to save.
	 */
	void save(CxLocalizedValueModel value);

	/**
	 * Saves a collection of {@link CxLocalizedValueModel} to the database.
	 *
	 * @param values the collection of localized value models to save.
	 */
	void save(Collection<CxLocalizedValueModel> values);

	/**
	 * Retrieves a list of {@link CxLocalizedValueModel} associated with a specific {@link ItemModel}.
	 *
	 * @param item the item model to find associated localized values for.
	 * @return a list of localized value models.
	 */
	List<CxLocalizedValueModel> getForItem(ItemModel item);
	
	/**
	 * Retrieves a list of {@link CxLocalizedValueModel} based on a specific key.
	 *
	 * @param key the key to search localized values for.
	 * @return a list of localized value models that match the given key.
	 */
	List<CxLocalizedValueModel> getForKey(String key);

	/**
	 * Searches for localized value models based on the criteria defined in {@link CxLocalizedValueSearchData}.
	 *
	 * @param searchData the search data criteria used to find localized value models.
	 * @return a pageable collection of localized value models.
	 */
	CxTranslationsPageable<CxLocalizedValueModel> search(CxLocalizedValueSearchData searchData);

	/**
     * Builds LocalizedValues from Items and translates them asynchronously using AI-driven translation services,
	 * configured with specific settings.
	 *
     * @param parameters translation parameters for items and attributes
	 * @return a {@link CxTranslationCronJobModel} responsible for running the translation in the background
	 */
    CxTranslationCronJobModel startTranslationCronJobForItems(CxStartTranslationCronJobForItemsParameters parameters);

	/**
     * Creates a CronJob that translates a collection of localized values using AI-driven translation services,
	 * configured with specific settings.
	 *
     * @param parameters translation parameters for localized values
	 * @return a {@link CxTranslationCronJobModel} responsible for running the translation in the background
	 */
	CxTranslationCronJobModel startTranslationCronJob(CxStartTranslationCronJobForValuesParameters parameters);

    /**
     * Retries translations. Translations are expected to belong to a single existing cron job, 
     * which will be used as a template for the new cron job that will be created to perform the retry. 
     *
     * @param oldCronJob the existing cron job to be restarted
     * @param proposedTranslations the new proposed translations to be associated with the restarted cron job
     * @param serviceOverride an optional new translation service settings to be used for the restarted cron job
     * @param hintOverride  an optional new hint to be used for the restarted cron job
     * @return a new {@link CxTranslationCronJobModel} instance that has been restarted with the provided parameters
     */
    CxTranslationCronJobModel retryTranslations(CxTranslationCronJobModel oldCronJob, List<CxProposedTranslationModel> proposedTranslations, CxTranslationServiceSettingsModel serviceOverride, String hintOverride);

    /**
     * Execute AI translation for a single {@link CxProposedTranslationModel} using AI-driven translation services.
	 *
     * @param parameters translation parameters for a single proposed translation
	 */
    void aiTranslate(CxPerformTranslationParameters parameters);

	/**
	 * Create a {@link CxProposedTranslationModel} to represent the value to be translated
	 *
	 * @param value   {@link CxLocalizedValueModel}
	 * @param cronJob cron job of type {@link CxTranslationCronJobModel}
	 * @return {@link CxProposedTranslationModel}
	 */
	CxProposedTranslationModel createCxProposedTranslation(CxLocalizedValueModel value, CxTranslationCronJobModel cronJob);
}
