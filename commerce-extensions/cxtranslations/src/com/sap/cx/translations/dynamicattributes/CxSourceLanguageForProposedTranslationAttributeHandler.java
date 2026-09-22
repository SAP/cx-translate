/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.model.CxProposedTranslationModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;

/**
 * Dynamic attribute for {@link CxProposedTranslationModel} returning the source translation language based on the related cron job
 */
public class CxSourceLanguageForProposedTranslationAttributeHandler implements DynamicAttributeHandler<LanguageModel, CxProposedTranslationModel> {

	@Override
	public LanguageModel get(final CxProposedTranslationModel model) {
		if (model != null && model.getTranslationCronJob() != null) {
			return model.getTranslationCronJob()
						.getSourceLanguage();
		}
		return null;
	}

	@Override
	public void set(final CxProposedTranslationModel model, final LanguageModel languageModel) {
		throw new UnsupportedOperationException("Read only attribute");
	}
}
