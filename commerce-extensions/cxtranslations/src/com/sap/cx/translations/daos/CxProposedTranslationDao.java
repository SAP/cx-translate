/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.daos;

import com.sap.cx.translations.model.CxProposedTranslationModel;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;

import java.util.Collection;

/**
 * Localized label DAO for {@link CxProposedTranslationModel}
 */
public interface CxProposedTranslationDao extends GenericDao<CxProposedTranslationModel> {
	/**
	 * Finds {@link CxProposedTranslationModel} for key
	 *
	 * @param key of {@link CxProposedTranslationModel}
	 * @return found {@link CxProposedTranslationModel}s
	 */
	Collection<CxProposedTranslationModel> findByKey(String key);
}
