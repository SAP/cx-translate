/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueResultStatistics;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Abstract {@link CxLocalizedObjectProvider} that retrieves localized values by loading
 * {@link ItemWithIds} objects from a backing store and extracting per-item values via
 * {@link #extractValues(ItemWithIds)}.
 *
 * <p>Subclassing this class is a supported integration pattern — it handles all pagination
 * math (skip/take across object boundaries) and guards against empty-type-list inputs.
 *
 * <p><strong>Per-search lifecycle:</strong> instances are created per {@code search()} call
 * and must not be shared across threads or requests.
 *
 * @see CxLocalizedObjectProvider
 */
public abstract class CxAbstractItemBasedLocalizedObjectProvider implements CxLocalizedObjectProvider {

	@Override
	public List<CxLocalizedValueModel> searchForLocalisedValues(final List<String> requestedObjectTypes, final int howManyValuesToSkip, final int requestedValueCount) {
		// Defensive guard: review issue #1 — pageables sometimes narrow the requested types
		// to a provider that ends up with no matching types.
		if (requestedObjectTypes == null || requestedObjectTypes.isEmpty() || requestedValueCount <= 0) {
			return List.of();
		}

		final Map<String, CxLocalizedValueResultStatistics> statistics = getStatistics();

		final int howManyValuesForFirstObjectType = getValueCount(statistics, requestedObjectTypes.get(0));
		final int valuesPerObjectInFirst = howManyValuesPerObjectForObjectType(requestedObjectTypes.get(0));

		final int howManyValuesToTakeForFirstObjectType = Math.min(howManyValuesForFirstObjectType - howManyValuesToSkip, requestedValueCount);
		int howManyValuesStillLeft = requestedValueCount - howManyValuesToTakeForFirstObjectType;
		if (howManyValuesStillLeft < 0) {
			howManyValuesStillLeft = 0;
		}

		int howManyObjectToGet = calculateHowManyObjectsToGet(howManyValuesToTakeForFirstObjectType, valuesPerObjectInFirst);
		final int howManyObjectsToSkip = calculateHowManyObjectsToSkip(howManyValuesToSkip, valuesPerObjectInFirst);

		// valuesToSkipForFirstObject: how many values to skip within the first loaded object.
		// E.g. 3 objects × 3 values/object, skip=5 → objectsToSkip=1, valuesToSkipInFirstObject=2.
		final int valuesToSkipForFirstObject = howManyValuesToSkip % valuesPerObjectInFirst;

		for (int i = 1; i < requestedObjectTypes.size() - 1; i++) {
			final String objectType = requestedObjectTypes.get(i);
			final int howManyValuesFound = getValueCount(statistics, objectType);
			howManyValuesStillLeft -= howManyValuesFound;
			if (howManyValuesStillLeft < 0) {
				howManyValuesStillLeft = 0;
			}
			howManyObjectToGet += calculateHowManyObjectsToGet(howManyValuesFound, howManyValuesPerObjectForObjectType(objectType));
		}

		// For multi-type calls: account for the trailing type's remaining values.
		// For single-type calls howManyValuesStillLeft is already 0 so this adds nothing.
		if (requestedObjectTypes.size() > 1) {
			howManyObjectToGet += calculateHowManyObjectsToGet(howManyValuesStillLeft,
					howManyValuesPerObjectForObjectType(requestedObjectTypes.get(requestedObjectTypes.size() - 1)));
		}

		final List<CxLocalizedObjectProvider.ItemWithIds> itemWithIds = searchForLocalisedObjects(requestedObjectTypes, howManyObjectsToSkip, howManyObjectToGet);
		return itemWithIds.stream()
						  .flatMap(this::extractValues)
						  .skip(valuesToSkipForFirstObject)
						  .limit(requestedValueCount)
						  .toList();
	}

	/**
	 * Null-safe statistics lookup — returns 0 rather than NPE-ing when a type has no entry.
	 */
	private static int getValueCount(final Map<String, CxLocalizedValueResultStatistics> statistics, final String objectType) {
		final CxLocalizedValueResultStatistics stat = statistics.get(objectType);
		return stat != null ? stat.valueCount() : 0;
	}

	/**
	 * Fetches the objects that back the requested value window.
	 *
	 * @param objectTypes object type codes to include in the query
	 * @param start       zero-based object offset
	 * @param count       maximum number of objects to return
	 * @return the matching items together with their pre-computed key fragments
	 */
	public abstract List<ItemWithIds> searchForLocalisedObjects(Collection<String> objectTypes, int start, int count);
}
