/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.model.CxAbstractLocalizedValueModel;
import com.sap.cx.translations.model.CxLocalizedObjectConfigModel;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel;
import com.sap.cx.translations.services.CxTranslationsNotification;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Abstract {@link CxLocalizedObjectProvider} that uses Flexible search to find results.
 */
public abstract class CxAbstractLocalizedObjectFlexibleSearchProvider extends CxAbstractItemBasedLocalizedObjectProvider implements CxLocalizedObjectProvider {
    private final int valueSearchLimit;

    private static final Logger LOG = LoggerFactory.getLogger(CxAbstractLocalizedObjectFlexibleSearchProvider.class);
    protected FlexibleSearchService flexibleSearchService;
    protected final CxLocalizedValueSearchData searchData;
    protected final CxLocalizedValuesSearchSupport searchSupport;
    protected String code;
    protected CxStandardLocalizedObjectConfigModel config;
    private Map<String, CxLocalizedValueResultStatistics> statisticsMap;
    private List<CxLocalizedValueModel> foundByValueCache;

    protected boolean typesWereFilteredByPredicates;


    protected CxAbstractLocalizedObjectFlexibleSearchProvider(final FlexibleSearchService flexibleSearchService, final CxLocalizedValueSearchData searchData, final CxLocalizedValuesSearchSupport searchSupport, final int valueSearchLimit) {
        this.flexibleSearchService = flexibleSearchService;
        this.searchData = searchData;
        this.searchSupport = searchSupport;
        this.valueSearchLimit = valueSearchLimit;
    }

    /**
     * Gets Query Parts to used to filter by Object Type
     *
     * @param objectTypes Selected Localized Object Types.
     * @return Relevant {@link CxFlexibleSearchQueryPart}s
     */
    protected Collection<CxFlexibleSearchQueryPart> getWherePartsToFilterObjectTypes(final Collection<String> objectTypes) {
        return List.of();
    }

    /**
     * Gets main part of query used to search for objects.
     *
     * @param objectTypes Selected Localized Object Types.
     * @return A query string
     */
    protected abstract String getMainQuery(Collection<String> objectTypes);

    /**
     * Gets main part of query used to obtain statistics.
     * See getStatistics method for details about expected column layout.
     *
     * @return A query string
     */
    protected abstract String getStatisticsQuery();


    /**
     * Gets List of {@link CxFlexibleSearchQueryPart} for "Where" part of the Flexible search query.
     *
     * @param searchQueryData Search data
     * @return List of relevant {@link CxFlexibleSearchQueryPart}
     */
    protected List<CxFlexibleSearchQueryPart> getWhereConditionsQueryParts(final CxLocalizedValueSearchData searchQueryData) {
        return new ArrayList<>();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<CxLocalizedValueModel> searchForLocalisedValues(final List<String> requestedObjectTypes, final int howManyValuesToSkip, final int requestedValueCount) {
        if (foundByValueCache != null) {
            return foundByValueCache.stream()
                    .filter(value -> requestedObjectTypes == null || requestedObjectTypes.contains(value.getObjectType()))
                    .skip(howManyValuesToSkip)
                    .limit(requestedValueCount)
                    .toList();
        }
        return super.searchForLocalisedValues(requestedObjectTypes, howManyValuesToSkip, requestedValueCount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ItemWithIds> searchForLocalisedObjects(final Collection<String> configCodes, final int start, final int count) {
        String query = getMainQuery(configCodes);
        final CxFlexibleSearchQueryPart wherePart = getWherePart(searchData, configCodes);

        query = query.replaceAll("!!WHERE_CLAUSE!!", wherePart.queryPart());

        final FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(query);
        flexibleSearchQuery.addQueryParameters(wherePart.parameters());

        flexibleSearchQuery.setStart(start);
        flexibleSearchQuery.setCount(count);
        flexibleSearchQuery.setResultClassList(List.of(ItemModel.class, String.class, String.class));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing main search query: {}", query);
            LOG.debug("With parameters: {}", wherePart.parameters());
        }
        final List<List<Object>> result = flexibleSearchService.<List<Object>>search(flexibleSearchQuery)
                .getResult();


        return result.stream()
                .map(res -> new ItemWithIds((ItemModel) res.get(0), (String) res.get(1), (String) res.get(2)))
                .toList();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, CxLocalizedValueResultStatistics> getStatistics() {
        if (statisticsMap != null) {
            return statisticsMap;
        }

        if (shouldCalculateStatisticsByForce()) {
            final Map<String, CxLocalizedValueResultStatistics> bruteForceMap = getStaticsByBruteForceForValues();
            this.statisticsMap = bruteForceMap;
            return bruteForceMap;
        }

        String query = getStatisticsQuery();
        final CxFlexibleSearchQueryPart wherePart = getWherePart(this.searchData, null);
        query = query.replaceAll("!!WHERE_CLAUSE!!", wherePart.queryPart());

        final FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(query);

        flexibleSearchQuery.addQueryParameters(wherePart.parameters());
        flexibleSearchQuery.setResultClassList(List.of(String.class, Integer.class));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing Statistics query: {}", query);
            LOG.debug("With parameters: {}", wherePart.parameters());
        }

        final List<List<Object>> result = flexibleSearchService.<List<Object>>search(flexibleSearchQuery)
                .getResult();
        final Map<String, CxLocalizedValueResultStatistics> calculatedStatisticMap = result.stream()
                .filter(row -> searchSupport.getObjectTypePredicates()
                        .stream()
                        .allMatch(
                                predicate -> predicate.test((String) row.get(0))))
                .filter(obj -> this.howManyValuesPerObjectForObjectType((String) obj.get(0)) > 0)
                .collect(Collectors.toMap(
                        obj -> (String) obj.get(0),
                        obj -> {
                            final int count = (int) obj.get(1);
                            return new CxLocalizedValueResultStatistics(count, count * this.howManyValuesPerObjectForObjectType((String) obj.get(0)));
                        }
                ));
        if (calculatedStatisticMap.size() != result.size()) {
            this.typesWereFilteredByPredicates = true;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Obtained statistics: {}", calculatedStatisticMap);
        }
        this.statisticsMap = Map.copyOf(calculatedStatisticMap);
        return this.statisticsMap;
    }

    protected boolean shouldCalculateStatisticsByForce() {
        return false;
    }

    private Map<String, CxLocalizedValueResultStatistics> getStaticsByBruteForceForValues() {
        final List<ItemWithIds> items = searchForLocalisedObjects(null, 0, valueSearchLimit);
        if (items.size() >= valueSearchLimit) {
            searchSupport.addNotification(CxTranslationsNotification.byValueLimit());
        }
        foundByValueCache = items.stream()
                .flatMap(this::extractValues)
                .filter(value -> searchSupport.getKeyPredicates()
                        .stream()
                        .allMatch(predicate -> predicate.test(value.getKey())))
                .filter(value -> searchSupport.getValuePredicates()
                        .stream()
                        .allMatch(predicate -> predicate.test(value)))
                .toList();


        final Map<String, CxLocalizedValueResultStatistics> result = foundByValueCache.stream()
                .collect(Collectors.groupingBy(CxAbstractLocalizedValueModel::getObjectType))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> searchSupport.getStatistics(entry.getValue())));
        return result;
    }


    /**
     * Builds "WHERE" part for Flexible Search queries
     *
     * @param searchQueryData search data
     * @param objectTypes     object types that should be included
     * @return a relevant {@link CxFlexibleSearchQueryPart}
     */
    private CxFlexibleSearchQueryPart getWherePart(final CxLocalizedValueSearchData searchQueryData, final Collection<String> objectTypes) {
        final List<CxFlexibleSearchQueryPart> whereParts = getWhereConditionsQueryParts(searchQueryData);
        if (objectTypes != null) {
            whereParts.addAll(getWherePartsToFilterObjectTypes(objectTypes));
        }
        if (whereParts.isEmpty()) {
            return new CxFlexibleSearchQueryPart(" ", Collections.emptyMap());
        }
        final CxFlexibleSearchQueryPart combined = CxFlexibleSearchQueryPart.and(whereParts);
        return new CxFlexibleSearchQueryPart(" WHERE " + combined.queryPart(), combined.parameters());
    }


    /**
     * {@return main {@link CxLocalizedObjectConfigModel} object related to this provider.}
     */
    @Override
    public CxLocalizedObjectConfigModel getConfig() {
        return config;
    }

}
