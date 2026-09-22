/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.interceptors;

import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.PrepareInterceptor;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Mark the {@link CxProposedTranslationModel} as manually modified in case manual value for translation has been provided
 */
public class CxProposedTranslationPrepareInterceptor implements PrepareInterceptor<CxProposedTranslationModel> {

	@Override
	public void onPrepare(final CxProposedTranslationModel model, final InterceptorContext ctx) throws InterceptorException {
		if (!ctx.isNew(model) && !ctx.isRemoved(model)) {
			final Map<String, Set<Locale>> attributes = ctx.getDirtyAttributes(model);
			if (attributes.containsKey(CxProposedTranslationModel.MANUALTRANSLATIONVALUE)) {
				model.setTranslationStatus(CxTranslationStatus.MANUAL);
			}
		}
	}
}
