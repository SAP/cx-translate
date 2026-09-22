/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.daos.impl;

import com.sap.cx.translations.daos.CxLocalizedBundleDao;
import com.sap.cx.translations.model.CxLocalizedBundleModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.servicelayer.internal.dao.DefaultGenericDao;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link CxLocalizedBundleDao}
 */
public class CxLocalizedBundleDaoImpl extends DefaultGenericDao<CxLocalizedBundleModel> implements CxLocalizedBundleDao {
	public CxLocalizedBundleDaoImpl() {
		super(CxLocalizedBundleModel._TYPECODE);
	}

	@Override
	public Collection<CxLocalizedBundleModel> findBundlesByCatalogVersion(final CatalogVersionModel catalogVersion) {
		return find(Collections.singletonMap(CxLocalizedBundleModel.CATALOGVERSION, catalogVersion));
	}

	@Override
	public Optional<CxLocalizedBundleModel> findBundleByCode(final String code, final CatalogVersionModel catalogVersion) {
		Map<String, Object> params = new HashMap<>();
		params.put(CxLocalizedBundleModel.CODE, code);
		params.put(CxLocalizedBundleModel.CATALOGVERSION, catalogVersion);
		return find(params).stream()
						   .findFirst();
	}
}
