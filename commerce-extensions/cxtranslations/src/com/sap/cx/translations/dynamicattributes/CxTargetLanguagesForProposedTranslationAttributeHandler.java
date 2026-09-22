/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.model.CxProposedTranslationModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;

import java.util.Collection;
import java.util.Collections;

/**
 * Dynamic attribute for {@link CxProposedTranslationModel} returning the target translation languages based on the related cron job
 */
public class CxTargetLanguagesForProposedTranslationAttributeHandler implements DynamicAttributeHandler<Collection<LanguageModel>, CxProposedTranslationModel> {

	@Override
	public Collection<LanguageModel> get(final CxProposedTranslationModel model) {
		if (model != null && model.getTranslationCronJob() != null) {
			return model.getTranslationCronJob()
						.getTargetLanguages();
		}
		return Collections.emptyList();
	}

	@Override
	public void set(final CxProposedTranslationModel model, final Collection<LanguageModel> languageModel) {
		throw new UnsupportedOperationException("Read only attribute");
	}
}
