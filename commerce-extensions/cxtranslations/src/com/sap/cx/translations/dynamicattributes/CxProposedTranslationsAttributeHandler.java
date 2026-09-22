/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.daos.CxProposedTranslationDao;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;

import java.util.Collection;
import java.util.Collections;

/**
 * A dynamic attribute handler returning all {@link CxProposedTranslationModel}s created by {@link CxLocalizedValueModel}.
 */
public class CxProposedTranslationsAttributeHandler implements DynamicAttributeHandler<Collection<CxProposedTranslationModel>, CxLocalizedValueModel> {

	private CxProposedTranslationDao cxProposedTranslationDao;

	@Override
	public Collection<CxProposedTranslationModel> get(final CxLocalizedValueModel localizedValue) {
		if (localizedValue != null) {
			return cxProposedTranslationDao.findByKey(localizedValue.getKey());
		}
		return Collections.emptyList();
	}

	@Override
	public void set(final CxLocalizedValueModel model, final Collection<CxProposedTranslationModel> cxProposedTranslationModelSet) {
		throw new UnsupportedOperationException("Read only attribute");
	}

	public void setCxProposedTranslationDao(final CxProposedTranslationDao cxProposedTranslationDao) {
		this.cxProposedTranslationDao = cxProposedTranslationDao;
	}
}
