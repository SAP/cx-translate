/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedObjectConfigModel;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueResultStatistics;
import de.hybris.platform.core.model.ItemModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Provider responsible for finding Localized Objects / Values.
 * Method getResults finds objects (for example Product item instance), getValueExtractor returns associated CxLocValuesExtractor
 * which is responsible to get flat list of localized values.
 */
public interface CxLocalizedObjectProvider {

	/**
	 * Aggregates an item loaded from the DB with the pre-computed key fragments.
	 *
	 * @param item              the item loaded from DB (may be {@code null} for unit-test stubs)
	 * @param keyWithoutAttribute the localized-value key <em>without</em> the trailing
	 *                          {@code .<attribute>} suffix, e.g. {@code "Product.p-001"}
	 * @param objectId          the object identifier segment of the key, e.g. {@code "p-001"}
	 */
	record ItemWithIds(ItemModel item, String keyWithoutAttribute, String objectId) {
	}

	/**
	 * Returns the list of object-type codes this provider can produce localized values for.
	 * Used by the factory and the pageable to route requests to the appropriate provider.
	 */
	List<String> allSupportedObjectTypes();

	/**
	 * Returns if there is any possibility of returning a specific Localized Object Type.
	 * Used in optimizations, true value does not mean that a specific result will be returned.
	 * @param objectType Localized Object Type
	 * @return false if the specific type cannot be returned, true otherwise
	 */
	boolean canReturnObjectType(String objectType);

	/**
	 * Returns all {@link CxLocalizedValueModel}s for the given object types that fall within
	 * the requested window.
	 *
	 * @param requestedObjectTypes ordered list of object-type codes to search across
	 * @param howManyValuesToSkip  number of values to skip from the beginning of the result set
	 * @param requestedValueCount  maximum number of values to return; {@code 0} returns an empty list
	 * @return matching values, at most {@code requestedValueCount} entries
	 */
	List<CxLocalizedValueModel> searchForLocalisedValues(final List<String> requestedObjectTypes, final int howManyValuesToSkip, final int requestedValueCount);

	/**
	 * calculates how many objects to get
	 * we need to get object if at lest one value is requested
	 * for example if we have 3 values per object
	 * then
	 * valueCount = 0 -> return 0
	 * valueCount = 1 -> return 1
	 * valueCount = 2 -> return 1
	 * valueCount = 3 -> return 1
	 * valueCount = 4 -> return 2
	 * ...
	 */
	default int calculateHowManyObjectsToGet(final int valueToGetCount, final int valuesPerObject) {
		if (valueToGetCount < 0) {
			throw new IllegalArgumentException("valuesPerObject cannot be negative");
		}
		if (valueToGetCount <= 1 ) {
			return valueToGetCount;
		}
		return 1 + ((valueToGetCount -1) / valuesPerObject);
	}

	/**
	 * calculates how many objects to skip
	 * we can skip an object only if all values for object can be skipped
	 * for example if we have 3 values per object
	 * then
	 * valueCount = 0 -> return 0
	 * valueCount = 1 -> return 0
	 * valueCount = 2 -> return 0
	 * valueCount = 3 -> return 1
	 * valueCount = 4 -> return 1
	 * valueCount = 5 -> return 1
	 * valueCount = 6 -> return 2
	 * ...
	 * ...
	 */
	default int calculateHowManyObjectsToSkip(final int valueToSkipCount, final int valuesPerObject) {
		if (valuesPerObject == 1) {
			return valueToSkipCount;
		}
		return (valueToSkipCount) / valuesPerObject;
	}

	/**
	 * Extracts all localized values from the given item.
	 *
	 * @param itemWithId item together with its pre-computed key and object-id segments
	 * @return stream of localized-value wrappers for this item; never {@code null}
	 */
	Stream<CxLocalizedValueModel> extractValues(ItemWithIds itemWithId);


	/**
	 * Returns how many localized values are returned for given object code.
	 *
	 * @param objectType the localized object type
	 * @return how many localized values are returned for given object code.
	 */
	int howManyValuesPerObjectForObjectType(String objectType);

	/**
	 * {@return Map of statistics aggregated by Object Type}
	 */
	Map<String, CxLocalizedValueResultStatistics> getStatistics();

	/**
	 * Convenience accessor — returns the statistics for a single object type,
	 * or an empty statistics record if the type is not present in the map.
	 * Avoids the verbose {@code getStatistics().getOrDefault(type, EMPTY)} pattern at call sites.
	 *
	 * @param objectType the object type code
	 * @return statistics for the type; never {@code null}
	 */
	default CxLocalizedValueResultStatistics getStatisticsFor(final String objectType) {
		return getStatistics().getOrDefault(objectType, CxLocalizedValueResultStatistics.EMPTY);
	}

	/**
	 * Gets configuration object associated with the provider.
	 *
	 * @return the config object.
	 */
	CxLocalizedObjectConfigModel getConfig();

}
