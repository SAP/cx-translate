/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.config;

import com.sap.cx.translations.model.CxLocalizedObjectConfigCronJobModel;
import com.sap.cx.translations.model.CxLocalizedObjectConfigModel;
import com.sap.cx.translations.model.CxProductFeatureLocalizedObjectConfigModel;
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;

import java.util.List;
import java.util.Set;

/**
 * Service to work with configurations of Localized Objects / Values.
 */
public interface CxLocalizedValueConfigService {
	/**
	 * Does the autoconfiguration based on type system and data
	 *
	 * @param addAttributesOnlyWhenDataExists - adds attributes/config only for values for which data exists
	 */
	void autoConfigure(boolean addAttributesOnlyWhenDataExists);

	/**
	 * Configure the {@link CxLocalizedObjectConfigCronJobModel}
	 * @param cronJob cronjob to configure
	 */
	void configureLocalizedObjectConfigCronJob(CxLocalizedObjectConfigCronJobModel cronJob);

	/**
	 * Find types to consider for autoconfiguration
	 *
	 * @return list of {@link ComposedTypeModel}s
	 */
	List<ComposedTypeModel> findTypesToConsider();

	/**
	 * Build a tree of {@link CxStandardLocalizedObjectConfigModel}s for given type and its subtypes.
	 *
	 * @param type    {@link ComposedTypeModel}
	 * @param cronJob {@link CxLocalizedObjectConfigCronJobModel}
	 * @return a tree of {@link CxStandardLocalizedObjectConfigModel}s for given type and its subtypes.
	 */
	Set<CxStandardLocalizedObjectConfigModel> buildConfig(ComposedTypeModel type, final CxLocalizedObjectConfigCronJobModel cronJob);
	
	/**
	 * Searches for CxStandardLocalizedObjectConfig of given type
	 * @param type {@link ComposedTypeModel}
	 *
	 * @return a CxStandardLocalizedObjectConfig item if found, null otherwise
	 */
	CxStandardLocalizedObjectConfigModel findConfigByType(ComposedTypeModel type);

	/**
	 * Finds the configuration object for Product Features (classification)
	 * @return configuration object for Product Features
	 */
	CxProductFeatureLocalizedObjectConfigModel findConfigForProductFeatures();

	/**
	 * Find translatable attributes for type code based on existing configurations
	 * @param typeCode type code to be used in search
	 * @return set of attributes
	 */
	Set<AttributeDescriptorModel> findTranslatableAttributesForTypeCode(final String typeCode);

    /**
     * Find all Localized Object Config objects
     * @return list of CxLocalizedObjectConfigModel
     */
    List<CxLocalizedObjectConfigModel> findAllConfigs();
}
