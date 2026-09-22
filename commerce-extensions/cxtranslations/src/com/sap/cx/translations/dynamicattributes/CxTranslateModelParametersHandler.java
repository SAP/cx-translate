/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for ai model parameter values on  CxAiTranslationServiceSettingsModel.
 * Provides support for filling default values from CxAiTranslationServiceApi.defaultParameterValues.
 */
public class CxTranslateModelParametersHandler implements DynamicAttributeHandler<Map<String, String>, CxAiTranslationServiceSettingsModel> {
	@Override
	public Map<String, String> get(final CxAiTranslationServiceSettingsModel model) {
		final Map<String, String> savedParametersInternal = new HashMap<>(MapUtils.emptyIfNull(model.getSavedParameterValuesInternal()));
		fillDefaults(model, savedParametersInternal);
		return savedParametersInternal;
	}

	@Override
	public void set(final CxAiTranslationServiceSettingsModel model, final Map<String, String> stringStringMap) {
		final Map<String, String> toBeSaved = new HashMap<>(stringStringMap);
		toBeSaved.keySet()
				 .removeIf(o -> !model.getAiServiceApi()
									  .getConfigurableParameters()
									  .contains(o));
		model.setSavedParameterValuesInternal(toBeSaved);
	}

	private static void fillDefaults(final CxAiTranslationServiceSettingsModel model, final Map<String, String> savedParametersInternal) {
		final Collection<String> configurableParameters = CollectionUtils.emptyIfNull(model.getAiServiceApi()
																						   .getConfigurableParameters());
		final Map<String, String> defaultParameterValues = MapUtils.emptyIfNull(model.getAiServiceApi()
																					 .getDefaultParameterValues());

		configurableParameters.forEach(key -> {
			savedParametersInternal.putIfAbsent(key, defaultParameterValues.getOrDefault(key, ""));
		});
	}


}
