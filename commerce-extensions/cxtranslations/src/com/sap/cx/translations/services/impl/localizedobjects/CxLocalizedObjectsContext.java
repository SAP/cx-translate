/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Contains objects needed to perform operations on Localized Objects/Values.
 */
public final class CxLocalizedObjectsContext {
	private final List<CxLocalizedObjectProvider> providers;
	private final CxLocalizedValuesSearchSupport searchSupport;
	private final List<String> allObjectTypes = new ArrayList<>();
	private final Map<String, CxLocalizedObjectProvider> objectTypeToProviderMap = new HashMap<>();

	/**
	 * @param providers     List of available CxLocalizedObjectProvider
	 * @param searchData    Current Search data
	 * @param searchSupport
	 */
	public CxLocalizedObjectsContext(final List<CxLocalizedObjectProvider> providers, final CxLocalizedValueSearchData searchData, final CxLocalizedValuesSearchSupport searchSupport) {
		this.providers = providers;
		this.searchSupport = searchSupport;

		for (final CxLocalizedObjectProvider provider : this.providers) {
			for (final String objectType : provider.allSupportedObjectTypes()) {
				allObjectTypes.add(objectType);
				objectTypeToProviderMap.put(objectType, provider);
			}
		}
	}

	public List<CxLocalizedObjectProvider> getProviders() {
		return providers;
	}

	public Map<String, CxLocalizedObjectProvider> getObjectTypeToProviderMap() {
		return objectTypeToProviderMap;
	}

	public List<String> allObjectTypes() {
		return allObjectTypes;
	}

	public CxLocalizedValuesSearchSupport getSearchSupport() {
		return searchSupport;
	}
}
