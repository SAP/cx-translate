/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.daos.impl;

import com.sap.cx.translations.daos.CxLocalizedLabelDao;
import com.sap.cx.translations.labels.UsageType;
import com.sap.cx.translations.model.CxLocalizedBundleModel;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.servicelayer.internal.dao.DefaultGenericDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;

import java.util.*;

/**
 * Default implementation of {@link CxLocalizedLabelDao}
 */
public class CxLocalizedLabelDaoImpl extends DefaultGenericDao<CxLocalizedLabelModel> implements CxLocalizedLabelDao {

	public CxLocalizedLabelDaoImpl() {
		super(CxLocalizedLabelModel._TYPECODE);
	}

	@Override
	public List<CxLocalizedLabelModel> findLabelsByUsage(final CatalogVersionModel catalogVersion,
														 final UsageType... usageType) {
		final Map<String, Object> params = new HashMap<>();
		params.put("catalogVersion", catalogVersion);

		final StringBuilder queryBuilder = new StringBuilder(
				"SELECT {pk} FROM {CxLocalizedLabel} WHERE {catalogVersion} = ?catalogVersion");

		appendUsageTypeFilter(queryBuilder, params, usageType);

		return getFlexibleSearchService().<CxLocalizedLabelModel>search(queryBuilder.toString(), params)
										 .getResult();
	}

	@Override
	public Collection<CxLocalizedLabelModel> findLabelsByBundleAndUsage(final CxLocalizedBundleModel bundle,
																		final CatalogVersionModel catalogVersion, final UsageType... usageType) {
		final Map<String, Object> params = new HashMap<>();
		params.put("bundle", bundle);
		params.put("catalogVersion", catalogVersion);

		final StringBuilder queryBuilder = new StringBuilder(
				"SELECT {pk} FROM {CxLocalizedLabel} WHERE {bundle} = ?bundle AND {catalogVersion} = ?catalogVersion");

		appendUsageTypeFilter(queryBuilder, params, usageType);

		return getFlexibleSearchService().<CxLocalizedLabelModel>search(queryBuilder.toString(), params)
										 .getResult();
	}

	@Override
	public Optional<CxLocalizedLabelModel> findLabelByKeyAndUsage(final String key, final CatalogVersionModel catalogVersion,
                                                                  final UsageType usageType) {
		final Map<String, Object> params = new HashMap<>();
		params.put(CxLocalizedLabelModel.KEY, key);
		params.put(CxLocalizedLabelModel.CATALOGVERSION, catalogVersion);

		if (usageType == UsageType.FRONTEND) {
			params.put(CxLocalizedLabelModel.FORFRONTEND, true);
		} else if (usageType == UsageType.BACKEND) {
			params.put(CxLocalizedLabelModel.FORBACKEND, true);
		}

		return find(params).stream()
						   .findFirst();
	}

	@Override
	public Collection<CxLocalizedLabelModel> findLabelsByKeysAndUsage(final Collection<String> keys,
																	  final CatalogVersionModel catalogVersion, final UsageType usageType) {
		if (keys == null || keys.isEmpty()) {
			return Collections.emptyList();
		}

		final Map<String, Object> params = new HashMap<>();
		params.put("keys", keys);
		params.put(CxLocalizedLabelModel.CATALOGVERSION, catalogVersion);

		final StringBuilder queryBuilder = new StringBuilder(
				"SELECT {pk} FROM {CxLocalizedLabel} WHERE {key} in (?keys) AND {catalogVersion} = ?catalogVersion");

		appendUsageTypeFilter(queryBuilder, params, usageType == null ? new UsageType[0] : new UsageType[]{usageType});

		return getFlexibleSearchService().<CxLocalizedLabelModel>search(queryBuilder.toString(), params)
										 .getResult();
	}

	private void appendUsageTypeFilter(final StringBuilder queryBuilder, final Map<String, Object> params,
									   final UsageType... usageType) {
		if (usageType == null || usageType.length == 0) {
			return;
		}

		final List<UsageType> types = Arrays.asList(usageType);
		final boolean hasFrontend = types.contains(UsageType.FRONTEND);
		final boolean hasBackend = types.contains(UsageType.BACKEND);

		if (hasFrontend && hasBackend) {
			queryBuilder.append(" AND ({forFrontend} = ?forFrontend OR {forBackend} = ?forBackend)");
			params.put(CxLocalizedLabelModel.FORFRONTEND, true);
			params.put(CxLocalizedLabelModel.FORBACKEND, true);
		} else if (hasFrontend) {
			queryBuilder.append(" AND {forFrontend} = ?forFrontend");
			params.put(CxLocalizedLabelModel.FORFRONTEND, true);
		} else if (hasBackend) {
			queryBuilder.append(" AND {forBackend} = ?forBackend");
			params.put(CxLocalizedLabelModel.FORBACKEND, true);
		}
	}

	@Override
	public List<String> findLabelKeys(final String keyTerm, final CatalogVersionModel catalogVersion,
									  final Integer resultsSizeLimit) {
		if (keyTerm == null) {
			return Collections.emptyList();
		}

		final FlexibleSearchQuery query = new FlexibleSearchQuery("SELECT {l." + CxLocalizedLabelModel.KEY + "} "
				+ " FROM {" + CxLocalizedLabelModel._TYPECODE + " AS l} "
				+ " WHERE {l." + CxLocalizedLabelModel.CATALOGVERSION + "} = ?catalogVersion "
				+ " AND LOWER({l." + CxLocalizedLabelModel.KEY + "}) LIKE ?key ");

		query.addQueryParameter("key", '%' + keyTerm.toLowerCase() + '%');
		query.addQueryParameter("catalogVersion", catalogVersion);
		query.setResultClassList(List.of(String.class));
		if (resultsSizeLimit != null) {
			query.setCount(resultsSizeLimit);
		}

		return getFlexibleSearchService().<String>search(query).getResult();
	}
}
