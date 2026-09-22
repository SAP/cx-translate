/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.daos.impl;

import com.sap.cx.translations.daos.CxTranslationServiceSettingsDao;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import de.hybris.platform.servicelayer.internal.dao.DefaultGenericDao;

/**
 * Implementation of DAO for CxTranslationServiceSettings
 */
public class CxTranslationServiceSettingsDaoImpl extends DefaultGenericDao<CxTranslationServiceSettingsModel> implements CxTranslationServiceSettingsDao {
	public CxTranslationServiceSettingsDaoImpl() {
		super(CxTranslationServiceSettingsModel._TYPECODE);
	}
}
