/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import de.hybris.platform.catalog.model.CatalogModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility that builds {@link CxFlexibleSearchQueryPart} fragments for catalog-version and
 * catalog-id filtering.
 *
 * <p>The same SQL pattern appears in both {@link CxLocalizedObjectStandardProvider} and
 * {@link CxProductFeatureLocalizedObjectProvider} — the only variable is the FlexibleSearch
 * alias used for the catalog-version reference on the root item.  Centralising here eliminates
 * the duplication (improvement #3).
 */
public final class CxCatalogFilterQueryParts {

    private CxCatalogFilterQueryParts() {
    }

    /**
     * Builds a CATALOG_VERSION_ID filter: restricts results to catalog versions whose
     * {@code version} attribute matches the search criterion.
     *
     * @param cvFieldRef  FlexibleSearch reference to the catalog-version field on the root item,
     *                    e.g. {@code "{locItem.catalogVersion}"} or {@code "{p.catalogVersion}"}
     * @param criteria    the search criterion
     * @param counter     parameter name generator
     * @return list containing the query part, or empty list when the criterion produces no SQL
     */
    public static Collection<CxFlexibleSearchQueryPart> forCatalogVersionId(
            final String cvFieldRef,
            final CxLocalizedValueSearchData.SearchCriteria criteria,
            final CxParamNameGenerator counter) {

        final String value = criteria.value() != null ? criteria.value().stringValue() : null;
        final boolean hasValue = StringUtils.isNotBlank(value);
        final String paramName = counter.next("catalogVersionId");
        final Map<String, Object> params = hasValue ? Map.of(paramName, value) : Collections.emptyMap();

        final String cvType = CatalogVersionModel._TYPECODE;
        final String cvVersion = CatalogVersionModel.VERSION;

        final CxFlexibleSearchQueryPart part = switch (criteria.operator()) {
            case IS_NOT_EMPTY ->
                    new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + cvType + " AS cv} }} ) ", params);
            case IS_EMPTY ->
                    CxFlexibleSearchQueryPart.ALWAYS_FALSE;
            case EQUALS ->
                    hasValue ? new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + cvType + " AS cv} WHERE {cv." + cvVersion + "} = ?" + paramName + " }} ) ", params)
                             : CxFlexibleSearchQueryPart.ALWAYS_FALSE;
            case STARTS_WITH ->
                    hasValue ? new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + cvType + " AS cv} WHERE {cv." + cvVersion + "} LIKE CONCAT(?" + paramName + " ,'%') }} ) ", params)
                             : null;
            case LIKE ->
                    hasValue ? new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + cvType + " AS cv} WHERE {cv." + cvVersion + "} LIKE CONCAT('%', CONCAT(?" + paramName + " ,'%')) }} ) ", params)
                             : null;
            case ENDS_WITH ->
                    hasValue ? new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + cvType + " AS cv} WHERE {cv." + cvVersion + "} LIKE CONCAT('%', ?" + paramName + ") }} ) ", params)
                             : null;
        };

        return part != null ? List.of(part) : Collections.emptyList();
    }

    /**
     * Builds a CATALOG_ID filter: restricts results to catalog versions whose parent catalog's
     * {@code id} attribute matches the search criterion.
     *
     * @param cvFieldRef  FlexibleSearch reference to the catalog-version field on the root item
     * @param criteria    the search criterion
     * @param counter     parameter name generator
     * @return list containing the query part, or empty list when the criterion produces no SQL
     */
    public static Collection<CxFlexibleSearchQueryPart> forCatalogId(
            final String cvFieldRef,
            final CxLocalizedValueSearchData.SearchCriteria criteria,
            final CxParamNameGenerator counter) {

        final String value = criteria.value() != null ? criteria.value().stringValue() : null;
        final boolean hasValue = StringUtils.isNotBlank(value);
        final String paramName = counter.next("catalogId");
        final Map<String, Object> params = hasValue ? Map.of(paramName, value) : Collections.emptyMap();

        final String cvType = CatalogVersionModel._TYPECODE;
        final String catType = CatalogModel._TYPECODE;
        final String catJoin = cvType + " AS cv JOIN " + catType + " AS c ON {c.PK} = {cv." + CatalogVersionModel.CATALOG + "}";

        final CxFlexibleSearchQueryPart part = switch (criteria.operator()) {
            case IS_NOT_EMPTY ->
                    new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + catJoin + "} }} ) ", params);
            case IS_EMPTY ->
                    CxFlexibleSearchQueryPart.ALWAYS_FALSE;
            case EQUALS ->
                    hasValue ? new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + catJoin + "} WHERE {c." + CatalogModel.ID + "} = ?" + paramName + " }} ) ", params)
                             : CxFlexibleSearchQueryPart.ALWAYS_FALSE;
            case STARTS_WITH ->
                    hasValue ? new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + catJoin + "} WHERE {c." + CatalogModel.ID + "} LIKE CONCAT(?" + paramName + " ,'%') }} ) ", params)
                             : null;
            case LIKE ->
                    hasValue ? new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + catJoin + "} WHERE {c." + CatalogModel.ID + "} LIKE CONCAT('%', CONCAT(?" + paramName + " ,'%')) }} )", params)
                             : null;
            case ENDS_WITH ->
                    hasValue ? new CxFlexibleSearchQueryPart(cvFieldRef + " IN ( {{ SELECT {cv.PK} FROM {" + catJoin + "} WHERE {c." + CatalogModel.ID + "} LIKE CONCAT('%', ?" + paramName + ") }} )", params)
                             : null;
        };

        return part != null ? List.of(part) : Collections.emptyList();
    }
}
