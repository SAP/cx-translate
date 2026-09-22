/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.services.CxTranslationsNotification;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;
import com.sap.cx.translations.services.localizedobjects.CxTranslationsPageable;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gives search results in a pageable manner.
 */
public class CxTranslationsValuesPageableImpl implements CxTranslationsPageable<CxLocalizedValueModel> {
	private final CxLocalizedObjectsContext context;
	private final CxLocalizedValueSearchData searchData;
	private int currentPageIndex = 0;

	/**
	 * @param context    - context containing settings and objects needed to perform the search
	 * @param searchData - data of search query
	 */
	public CxTranslationsValuesPageableImpl(final CxLocalizedObjectsContext context, final CxLocalizedValueSearchData searchData) {
		this.context = context;
		this.searchData = searchData;
	}

	@Override
	public List<CxLocalizedValueModel> getCurrentPage() {
		return getPage(currentPageIndex);
	}

	private List<CxLocalizedValueModel> getSortedByKey(final int startIndex, final int pageSize) {
		// As key of provider/configuration is first element of key of localized value, results will be always
		// from a consecutive group of providers if we are sorting by key.

		List<String> usedObjectTypes = new ArrayList<>(context.allObjectTypes());

		final Comparator<String> objectTypeComparator;
		final Comparator<CxLocalizedValueModel> keyComparator;

		if (searchData.sortDirection() == CxLocalizedValueSearchData.SortDirection.Descending) {
			keyComparator = Comparator.comparing((CxLocalizedValueModel valueModel) -> StringUtils.substringBefore(valueModel.getKey(), "."), String.CASE_INSENSITIVE_ORDER)
									  .reversed();
			objectTypeComparator = String.CASE_INSENSITIVE_ORDER.reversed();
		} else {
			keyComparator = Comparator.comparing((CxLocalizedValueModel valueModel) -> StringUtils.substringBefore(valueModel.getKey(), "."), String.CASE_INSENSITIVE_ORDER);
			objectTypeComparator = String.CASE_INSENSITIVE_ORDER;
		}

		usedObjectTypes.sort(objectTypeComparator);

		final CxTranslationsValuesPagePlanner.PagePlan plan = CxTranslationsValuesPagePlanner.plan(
				startIndex, pageSize, usedObjectTypes, context.getObjectTypeToProviderMap());

		final List<String> finalUsedObjectTypes = plan.usedObjectTypes().subList(0, plan.howManyToUse());

		if (finalUsedObjectTypes.isEmpty()) {
			return Collections.emptyList();
		}

		final Map<CxLocalizedObjectProvider, List<String>> groupedByProvider = finalUsedObjectTypes.stream()
																								   .collect(Collectors.groupingBy(plan.codeToProviderMap()::get));

		final String firstObjectType = finalUsedObjectTypes.get(0);
		final String lastObjectType = finalUsedObjectTypes.get(finalUsedObjectTypes.size() - 1);

		return groupedByProvider.entrySet()
								.stream()
								.flatMap(entry -> {
									final List<String> objectTypesFromCurrentProvider = entry.getValue();
									objectTypesFromCurrentProvider.sort(objectTypeComparator);

									final CxLocalizedObjectProvider provider = entry.getKey();
									final int start = objectTypesFromCurrentProvider.contains(firstObjectType) ? plan.howManyToSkipFromFirst() : 0;
									final int count = CxTranslationsValuesPagePlanner.countForProvider(
											provider, objectTypesFromCurrentProvider, firstObjectType, lastObjectType, plan);

									return entry.getKey()
												.searchForLocalisedValues(objectTypesFromCurrentProvider, start, count)
												.stream();
								})
								.sorted(keyComparator)
								.limit(pageSize)
								.toList();
	}

	@Override
	public List<CxLocalizedValueModel> getPage(final int pageNumber) {
		final int pageSize = getPageSize();
		final int startFromIndex = pageNumber * pageSize;

		final CxLocalizedValueSearchData.SortField sortField = searchData.sortField();
		if (sortField == CxLocalizedValueSearchData.SortField.CODE) {
			return getSortedByKey(startFromIndex, pageSize);
		}
		throw new IllegalArgumentException("not implemented yet");
	}

	@Override
	public int getPageSize() {
		return searchData.pageSize();
	}

	@Override
	public int getTotalCount() {
		return context.getProviders()
					  .stream()
					  .flatMapToInt(p -> p.allSupportedObjectTypes()
										  .stream()
										  .mapToInt(t -> p.getStatisticsFor(t).valueCount()))
					  .sum();
	}

	@Override
	public int getPageNumber() {
		return currentPageIndex;
	}

	@Override
	public void setPageNumber(final int pageNumber) {
		this.currentPageIndex = pageNumber;
	}

	@Override
	public List<CxTranslationsNotification> getNotifications() {
		return this.context.getSearchSupport()
						   .getNotifications();
	}
}
