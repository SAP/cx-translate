/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.daos.impl;

import com.sap.cx.translations.daos.CxProposedTranslationDao;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import de.hybris.platform.servicelayer.internal.dao.DefaultGenericDao;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO for {@link CxProposedTranslationModel}
 */
public class CxProposedTranslationDaoImpl extends DefaultGenericDao<CxProposedTranslationModel> implements CxProposedTranslationDao {

	public CxProposedTranslationDaoImpl() {
		super(CxProposedTranslationModel._TYPECODE);
	}

	@Override
	public Collection<CxProposedTranslationModel> findByKey(final String key) {
		if (key == null) {
			return Collections.emptyList();
		}
		Map<String, Object> params = new HashMap<>();
		params.put(CxProposedTranslationModel.KEY, key);
		return find(params);
	}
}
