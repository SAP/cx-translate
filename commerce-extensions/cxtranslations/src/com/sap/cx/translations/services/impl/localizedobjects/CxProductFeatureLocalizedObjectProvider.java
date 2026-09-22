/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxProductFeatureLocalizedObjectConfigModel;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedClassificationService;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;
import de.hybris.platform.catalog.model.ProductFeatureModel;
import de.hybris.platform.catalog.model.classification.ClassAttributeAssignmentModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Stream;

/**
 * {@link CxLocalizedObjectProvider} implementation for Product Feature localized objects.
 * Uses Flexible Search to find Product Features with localized classification attributes.
 */
public class CxProductFeatureLocalizedObjectProvider extends CxAbstractLocalizedObjectFlexibleSearchProvider {
    private final CxProductFeatureLocalizedObjectConfigModel config;
    private final CxLocalizedClassificationService cxLocalizedClassificationService;
    private final CommonI18NService commonI18NService;
    private final List<ClassAttributeAssignmentModel> filteredAssignments;

    public CxProductFeatureLocalizedObjectProvider(final CxProductFeatureLocalizedObjectConfigModel config, final FlexibleSearchService flexibleSearchService, final CxLocalizedValueSearchData searchData, final CxLocalizedValuesSearchSupport searchSupport, final int valueSearchLimit, final CxLocalizedClassificationService cxLocalizedClassificationService, final CommonI18NService commonI18NService) {
        super(flexibleSearchService, searchData, searchSupport, valueSearchLimit);
        this.config = config;
        this.cxLocalizedClassificationService = cxLocalizedClassificationService;
        this.commonI18NService = commonI18NService;
        filteredAssignments = getFilteredAssignments();
    }


    @Override
    public List<ItemWithIds> searchForLocalisedObjects(final Collection<String> configCodes, final int start, final int count) {
        if (filteredAssignments.isEmpty()) {
            return Collections.emptyList();
        }
        // No item-level post-filter on key predicates: the brute-force statistics path applies
        // key predicates at the value level (in the parent's getStaticsByBruteForceForValues),
        // and shouldCalculateStatisticsByForce() returns true whenever a KEY criterion is present
        // so the cache is always populated before per-page reads. Filtering items here would shrink
        // the returned list below the requested page size for callers that bypass the cache —
        // see review finding #27.
        return super.searchForLocalisedObjects(configCodes, start, count);
    }


    @Override
    protected String getMainQuery(final Collection<String> objectTypes) {
        //for Product Features key/object id's are not obtained from the query results
        return """
                SELECT min({pf.pk}), '<empty>', '<empty>'
                    FROM {ProductFeature as pf
                        join Product as p on {pf.Product}={p.pk}
                        join ClassAttributeAssignment as caa on {pf.classificationAttributeAssignment} = {caa.pk}}
                    !!WHERE_CLAUSE!!
                    GROUP BY
                        {pf.product},
                        {p.code},
                        {pf.classificationAttributeAssignment},
                        {pf.valuePosition},
                        {pf.unit},
                        {pf.language}
                    ORDER BY {p.code},{pf.valuePosition},{pf.unit}
                """ + (searchData.sortDirection() == CxLocalizedValueSearchData.SortDirection.Ascending ? "ASC" : "DESC");
    }


    @Override
    protected String getStatisticsQuery() {
        final boolean needsProductJoin = hasSearchCriteriaOnObjectIdOrKey();
        final String productJoin = needsProductJoin ? " join Product as p on {pf.Product}={p.pk}" : "";
        return """
                SELECT 'ProductFeature' , COUNT(*)
                FROM (
                  {{
                    SELECT 1 as one
                    FROM {ProductFeature AS pf""" + productJoin + """
                 join ClassAttributeAssignment as caa on {pf.classificationAttributeAssignment} = {caa.pk}}
                    !!WHERE_CLAUSE!!
                    GROUP BY
                      {pf.product},
                      {pf.classificationAttributeAssignment},
                      {pf.valuePosition},
                      {pf.unit},
                      {pf.language}
                  }}
                ) x""";
    }

    private boolean hasSearchCriteriaOnObjectIdOrKey() {
        return !(config.getProductCatalogVersions().isEmpty()) || searchData.criteria().stream()
                .anyMatch(c -> c.field() == CxLocalizedValueSearchData.SearchField.OBJECT_ID
                        || c.field() == CxLocalizedValueSearchData.SearchField.KEY
                        || c.field() == CxLocalizedValueSearchData.SearchField.CATALOG_ID
                        || c.field() == CxLocalizedValueSearchData.SearchField.CATALOG_VERSION_ID);
    }

    @Override
    protected List<CxFlexibleSearchQueryPart> getWhereConditionsQueryParts(final CxLocalizedValueSearchData searchQueryData) {
        final ArrayList<CxFlexibleSearchQueryPart> queryParts = new ArrayList<>();
        queryParts.add(new CxFlexibleSearchQueryPart("{caa.pk} IN (?allowedAssignments)", Map.of("allowedAssignments", filteredAssignments)));
        // Single shared generator for all criteria — guarantees unique param names across the entire WHERE clause.
        final CxParamNameGenerator paramCounter = new CxParamNameGenerator();
        searchQueryData.criteria().forEach(criteria -> queryParts.addAll(getPartsForCriteria(criteria, paramCounter)));
        if (CollectionUtils.isNotEmpty(config.getProductCatalogVersions())) {
            queryParts.add(new CxFlexibleSearchQueryPart("{p.catalogVersion} IN (?allowedCatalogVersions)", Map.of("allowedCatalogVersions", config.getProductCatalogVersions())));
        }
        return queryParts;
    }

    private Collection<CxFlexibleSearchQueryPart> getPartsForCriteria(final CxLocalizedValueSearchData.SearchCriteria criteria, final CxParamNameGenerator counter) {
        return switch (criteria.field()) {
            case PK -> filterForPk(criteria, counter);
            case OBJECT_ID -> List.of(filterForObjectId(criteria, counter));
            case KEY -> List.of(filterForKey(criteria, counter));
            case VALUE -> filterForValue(criteria, counter);
            case CATALOG_ID -> filterForCatalogId(criteria, counter);
            case CATALOG_VERSION_ID -> filterForCatalogVersionId(criteria, counter);
            default -> List.of();
        };
    }

    private CxFlexibleSearchQueryPart filterForKey(final CxLocalizedValueSearchData.SearchCriteria searchCriteria, final CxParamNameGenerator count) {
        // Use the central key value object instead of magic-dot indexing — keeps build (in
        // CxLocalizedClassificationServiceImpl) and parse (here) in lockstep. If the input does
        // not match the expected shape we substitute a value that will never match a product code,
        // so the resulting WHERE clause filters everything out (instead of silently matching too much).
        final boolean catalogAware = config.getProductCatalogVersions().size() > 1;
        final String objectIdPart = CxProductFeatureKey.extractProductCode(searchCriteria.value().stringValue(), catalogAware)
                .orElse(null);
        return filterForObjectId(new CxLocalizedValueSearchData.SearchCriteria(CxLocalizedValueSearchData.SearchField.OBJECT_ID, searchCriteria.operator(), new CxLocalizedValueSearchData.StringWithLocale(objectIdPart, searchCriteria.value().locale())), count);

    }

    private CxFlexibleSearchQueryPart filterForObjectId(final CxLocalizedValueSearchData.SearchCriteria searchCriteria, final CxParamNameGenerator counter) {
        final String value = searchCriteria.value() != null ? searchCriteria.value().stringValue() : null;
        final boolean hasValue = StringUtils.isNotBlank(value);
        final String paramName = counter.next("objectId");
        final Map<String, Object> params = hasValue ? Map.of(paramName, value) : Collections.emptyMap();
        return switch (searchCriteria.operator()) {
            case IS_EMPTY -> new CxFlexibleSearchQueryPart("{p.code} IS NULL ", params);
            case IS_NOT_EMPTY -> new CxFlexibleSearchQueryPart("{p.code} IS NOT NULL ", params);
            case EQUALS ->
                    hasValue ? new CxFlexibleSearchQueryPart("{p.code} = ?" + paramName, params) : CxFlexibleSearchQueryPart.ALWAYS_FALSE;
            case STARTS_WITH ->
                    hasValue ? new CxFlexibleSearchQueryPart("{p.code} LIKE CONCAT(?" + paramName + " ,'%') ", params) : null;
            case ENDS_WITH ->
                    hasValue ? new CxFlexibleSearchQueryPart("{p.code} LIKE CONCAT('%', ?" + paramName + ") ", params) : null;
            case LIKE ->
                    hasValue ? new CxFlexibleSearchQueryPart("{p.code} LIKE CONCAT('%', CONCAT(?" + paramName + " ,'%')) ", params) : null;
            default -> CxFlexibleSearchQueryPart.ALWAYS_TRUE;
        };

    }

    private Collection<CxFlexibleSearchQueryPart> filterForPk(final CxLocalizedValueSearchData.SearchCriteria criteria, final CxParamNameGenerator counter) {
        if (criteria.operator() == CxLocalizedValueSearchData.SearchOperator.EQUALS) {
            final String valueForPk = criteria.value().stringValue();
            final String paramName = counter.next("pk");

            if (StringUtils.isNotBlank(valueForPk)) {
                return List.of(new CxFlexibleSearchQueryPart("{pf.pk} = ?" + paramName, Map.of(paramName, valueForPk)));
            }
        }
        return Collections.emptyList();
    }

    private Collection<CxFlexibleSearchQueryPart> filterForValue(final CxLocalizedValueSearchData.SearchCriteria criteria, final CxParamNameGenerator counter) {
        final String value = criteria.value() != null ? criteria.value().stringValue() : null;
        final boolean hasValue = StringUtils.isNotBlank(value);
        final String paramName = counter.next("valueParam");

        final CxFlexibleSearchQueryPart valuePart = switch (criteria.operator()) {
            case EQUALS ->
                    hasValue ? new CxFlexibleSearchQueryPart("{pf.stringValue} = ?" + paramName, Map.of(paramName, value)) : CxFlexibleSearchQueryPart.ALWAYS_FALSE;
            case STARTS_WITH ->
                    hasValue ? new CxFlexibleSearchQueryPart("{pf.stringValue} LIKE CONCAT(?" + paramName + " ,'%') ", Map.of(paramName, value)) : null;
            case ENDS_WITH ->
                    hasValue ? new CxFlexibleSearchQueryPart("{pf.stringValue} LIKE CONCAT('%', ?" + paramName + ") ", Map.of(paramName, value)) : null;
            case LIKE ->
                    hasValue ? new CxFlexibleSearchQueryPart("{pf.stringValue} LIKE CONCAT('%', CONCAT(?" + paramName + " ,'%')) ", Map.of(paramName, value)) : null;
            case IS_NOT_EMPTY -> new CxFlexibleSearchQueryPart("{pf.stringValue} IS NOT NULL AND CAST({pf.stringValue} AS VARCHAR(8000)) <> ''", Collections.emptyMap());
            case IS_EMPTY -> new CxFlexibleSearchQueryPart("({pf.stringValue} IS NULL OR CAST({pf.stringValue} AS VARCHAR(8000)) = '')", Collections.emptyMap());
        };

        if (valuePart == null) {
            return Collections.emptyList();
        }

        if (criteria.value() != null && criteria.value().locale() != null) {
            final String langParamName = counter.next("lang");
            final LanguageModel language = commonI18NService.getLanguage(criteria.value().locale().getLanguage());
            final String langCondition = "{pf.language} = ?" + langParamName;
            final Map<String, Object> combinedParams = new HashMap<>(valuePart.parameters());
            combinedParams.put(langParamName, language);
            return List.of(new CxFlexibleSearchQueryPart(valuePart.queryPart() + " AND " + langCondition, combinedParams));
        }

        return List.of(valuePart);
    }

    private Collection<CxFlexibleSearchQueryPart> filterForCatalogVersionId(final CxLocalizedValueSearchData.SearchCriteria criteria, final CxParamNameGenerator counter) {
        return CxCatalogFilterQueryParts.forCatalogVersionId("{p.catalogVersion}", criteria, counter);
    }

    private Collection<CxFlexibleSearchQueryPart> filterForCatalogId(final CxLocalizedValueSearchData.SearchCriteria criteria, final CxParamNameGenerator counter) {
        return CxCatalogFilterQueryParts.forCatalogId("{p.catalogVersion}", criteria, counter);
    }

    @Override
    public List<String> allSupportedObjectTypes() {
        return List.of(ProductFeatureModel._TYPECODE);
    }

    @Override
    public boolean canReturnObjectType(final String objectType) {
        return ProductFeatureModel._TYPECODE.equalsIgnoreCase(objectType);
    }

    @Override
    public Stream<CxLocalizedValueModel> extractValues(final ItemWithIds itemWithId) {
        if (itemWithId.item() instanceof final ProductFeatureModel productFeature) {
            return getLocalizedValues(productFeature);
        }
        return Stream.empty();
    }

    private Stream<CxLocalizedValueModel> getLocalizedValues(final ProductFeatureModel productFeature) {
        final String productFeatureQualifier = cxLocalizedClassificationService.getProductFeatureQualifier(productFeature);
        return cxLocalizedClassificationService.getRelatedFeatures(productFeature)
                .map(f -> cxLocalizedClassificationService.createLocalizedValueFromFeatureGroup(productFeature.getProduct(), List.of(f), productFeatureQualifier));
    }

    @Override
    public int howManyValuesPerObjectForObjectType(final String objectType) {
        return 1;
    }

    @Override
    protected boolean shouldCalculateStatisticsByForce() {
        return searchData.criteria().stream().anyMatch(c->c.field().equals(CxLocalizedValueSearchData.SearchField.KEY));
    }

    private List<ClassAttributeAssignmentModel> getFilteredAssignments() {
        List<ClassAttributeAssignmentModel> localizedAttributes = flexibleSearchService.<ClassAttributeAssignmentModel>search("select {pk} from {ClassAttributeAssignment} where {localized} = 1").getResult();
        localizedAttributes = localizedAttributes.stream()
                .filter(assignment -> searchSupport.getAttributePredicates().stream().allMatch(predicate -> predicate.test(cxLocalizedClassificationService.getAttributeQualifier(assignment))))
                .toList();
        return localizedAttributes;
    }
}
