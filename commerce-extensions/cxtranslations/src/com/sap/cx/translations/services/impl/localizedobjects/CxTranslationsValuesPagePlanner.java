/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;

import java.util.List;
import java.util.Map;

/**
 * Pure-math helper that decides which object types to query and how many values to skip/take
 * for a given page within a multi-provider, multi-object-type result set.
 *
 * <p>All state is derived from the statistics already cached in each provider — no I/O here.
 * Extracted from {@link CxTranslationsValuesPageableImpl} so the planning logic can be unit-tested
 * in isolation from the Spring and DB plumbing (improvement #4 + #8).
 */
public class CxTranslationsValuesPagePlanner {

	/**
	 * The result of planning a single page: which object types are touched, how many values to
	 * skip in the first type, how many to take from the last type, and how many types to touch.
	 *
	 * @param usedObjectTypes     ordered list of object types involved in this page (already sliced
	 *                            from the global list to start at the first non-exhausted type)
	 * @param howManyToSkipFromFirst values to skip within the first object type
	 * @param howManyToTakeFromLast  values to take from the last object type
	 * @param howManyToUse        how many entries of {@code usedObjectTypes} to actually use
	 * @param codeToProviderMap   lookup from type code → provider
	 */
	public record PagePlan(
			List<String> usedObjectTypes,
			int howManyToSkipFromFirst,
			int howManyToTakeFromLast,
			int howManyToUse,
			Map<String, CxLocalizedObjectProvider> codeToProviderMap) {
	}

	/**
	 * Computes the page plan for a given value-offset ({@code startIndex}) and {@code pageSize}.
	 *
	 * @param startIndex        zero-based value offset into the sorted, full result set
	 * @param pageSize          number of values requested
	 * @param objectTypes       all object types in sort order
	 * @param codeToProviderMap lookup from type code → provider
	 * @return the page plan
	 */
	public static PagePlan plan(final int startIndex, final int pageSize,
	                            final List<String> objectTypes,
	                            final Map<String, CxLocalizedObjectProvider> codeToProviderMap) {
		final int firstTypeIndex = findFirstTypeIndex(startIndex, objectTypes, codeToProviderMap);
		final int skipInFirst = findSkipInFirstType(startIndex, firstTypeIndex, objectTypes, codeToProviderMap);

		final List<String> remaining = objectTypes.subList(firstTypeIndex, objectTypes.size());
		return buildPagePlan(remaining, skipInFirst, pageSize, codeToProviderMap);
	}

	/**
	 * Computes how many values to request from a specific provider for a given page plan.
	 *
	 * @param provider                      the provider
	 * @param objectTypesFromProvider       the object types from this provider that are in the page plan
	 * @param firstObjectType               the globally first object type in the plan
	 * @param lastObjectType                the globally last object type in the plan
	 * @param plan                          the page plan
	 * @return the number of values to request
	 */
	public static int countForProvider(final CxLocalizedObjectProvider provider,
	                                   final List<String> objectTypesFromProvider,
	                                   final String firstObjectType, final String lastObjectType,
	                                   final PagePlan plan) {
		final boolean containsLast = objectTypesFromProvider.contains(lastObjectType);
		int count = 0;
		final int howManyToTake = containsLast ? objectTypesFromProvider.size() - 1 : objectTypesFromProvider.size();
		for (int j = 0; j < howManyToTake; j++) {
			count += provider.getStatisticsFor(objectTypesFromProvider.get(j)).valueCount();
		}
		if (containsLast) {
			count += plan.howManyToTakeFromLast();
		}
		if (objectTypesFromProvider.contains(firstObjectType) && !firstObjectType.equals(lastObjectType)) {
			count -= plan.howManyToSkipFromFirst();
		}
		return count;
	}

	// -----------------------------------------------------------------------
	// private helpers
	// -----------------------------------------------------------------------

	private static int findFirstTypeIndex(final int startIndex, final List<String> objectTypes,
	                                      final Map<String, CxLocalizedObjectProvider> codeToProviderMap) {
		int skippedSoFar = 0;
		for (int i = 0; i < objectTypes.size(); i++) {
			final int typeCount = codeToProviderMap.get(objectTypes.get(i))
			                                       .getStatisticsFor(objectTypes.get(i))
			                                       .valueCount();
			if (skippedSoFar + typeCount > startIndex) {
				return i;
			}
			skippedSoFar += typeCount;
		}
		return objectTypes.size(); // startIndex is past all data — empty page
	}

	private static int findSkipInFirstType(final int startIndex, final int firstTypeIndex,
	                                       final List<String> objectTypes,
	                                       final Map<String, CxLocalizedObjectProvider> codeToProviderMap) {
		int skippedSoFar = 0;
		for (int i = 0; i < firstTypeIndex; i++) {
			skippedSoFar += codeToProviderMap.get(objectTypes.get(i))
			                                 .getStatisticsFor(objectTypes.get(i))
			                                 .valueCount();
		}
		return startIndex - skippedSoFar;
	}

	private static PagePlan buildPagePlan(final List<String> remaining, final int skipInFirst,
	                                      final int pageSize,
	                                      final Map<String, CxLocalizedObjectProvider> codeToProviderMap) {
		int howManyToTakeFromLast = 0;
		int howManyToUse = 0;
		int howManyFound = 0;

		for (int i = 0; i < remaining.size(); i++) {
			howManyToUse++;
			final String currentType = remaining.get(i);
			int countInCurrent = codeToProviderMap.get(currentType)
			                                      .getStatisticsFor(currentType)
			                                      .valueCount();
			if (i == 0) {
				countInCurrent -= skipInFirst;
			}
			howManyFound += countInCurrent;

			if (howManyFound >= pageSize || i == remaining.size() - 1) {
				final int tooMany = Math.max(0, howManyFound - pageSize);
				howManyToTakeFromLast = countInCurrent - tooMany;
				break;
			}
		}
		return new PagePlan(remaining, skipInFirst, howManyToTakeFromLast, howManyToUse, codeToProviderMap);
	}
}
