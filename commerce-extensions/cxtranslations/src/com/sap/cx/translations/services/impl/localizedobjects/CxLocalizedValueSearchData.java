/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import java.util.*;

/**
 * Record encapsulating search data for localized value queries.
 * This includes sorting preferences and a list of criteria for filtering results.
 */

public final class CxLocalizedValueSearchData {

    /**
     * Enum defining possible sort directions
     */
    public enum SortDirection {
        Ascending,
        Descending
    }

    /**
     * Enum defining possible fields for sorting localized values.
     */
    public enum SortField {
        PK,
        CODE,
        OBJECT_TYPE,
        ATTRIBUTE,
        OBJECT_ID,
        CATALOG_ID,
        CATALOG_VERSION_ID,
    }

    /**
     * Enum defining the fields that can be used for searching localized values.
     */
    public enum SearchField {
        KEY,
        VALUE,
        CATALOG_VERSION_ID,
        CATALOG_ID,
        OBJECT_ID,
        OBJECT_TYPE,
        ATTRIBUTE,
        PK
    }

    /**
     * Enum representing operators that can be used to compare search criteria.
     */
    public enum SearchOperator {
        EQUALS,
        STARTS_WITH,
        ENDS_WITH,
        LIKE,
        IS_EMPTY,
        IS_NOT_EMPTY
    }

    /**
     * Record defining a single search criterion.
     *
     * @param field    The field to be used for the search.
     * @param operator The operator to be used for comparing the search field.
     */
    public record SearchCriteria(
            SearchField field,
            SearchOperator operator,
            StringWithLocale value
    ) {
    }

    public record StringWithLocale(String stringValue, Locale locale) {
        public StringWithLocale(final String stringValue) {
            this(stringValue, null);
        }
    }

    private final SortField sortField;
    private final SortDirection sortDirection;
    private final List<SearchCriteria> criteria;
    private final int pageSize;
    private final Set<String> forceUsingConfigs;
    private final boolean forceAllConfigs;

    /**
     * @param sortField     Specifies the field by which the results should be sorted.
     * @param sortDirection Indicates sort direction
     * @param criteria      List of search criteria to apply in the query.
     * @param pageSize      Number of results per page.
     */
    public CxLocalizedValueSearchData(
            final SortField sortField,
            final SortDirection sortDirection,
            final List<SearchCriteria> criteria,
            final int pageSize
    ) {
        this(sortField, sortDirection, criteria, pageSize, Collections.emptySet(), false);
    }

    /**
     * @param sortField         Specifies the field by which the results should be sorted.
     * @param sortDirection     Indicates sort direction
     * @param criteria          List of search criteria to apply in the query.
     * @param pageSize          Number of results per page.
     * @param forceUsingConfigs Set of config codes whose configs should be included even when EXCLUDED_BY_DEFAULT.
     * @param forceAllConfigs   If true, all configs are included regardless of their backoffice search inclusion setting
     * @throws IllegalArgumentException if any criterion on the {@link SearchField#KEY} field uses an operator
     *                                  other than {@link SearchOperator#EQUALS} — only exact-match key lookup
     *                                  is supported at the service layer.
     */
    public CxLocalizedValueSearchData(
            final SortField sortField,
            final SortDirection sortDirection,
            final List<SearchCriteria> criteria,
            final int pageSize,
            final Set<String> forceUsingConfigs,
            final boolean forceAllConfigs
    ) {
        this.sortField = sortField;
        this.sortDirection = sortDirection;
        this.criteria = criteria;
        this.pageSize = pageSize;
        this.forceUsingConfigs = forceUsingConfigs != null ? forceUsingConfigs : Collections.emptySet();
        this.forceAllConfigs = forceAllConfigs;

        if (criteria != null) {
            criteria.stream()
                    .filter(c -> c.field() == SearchField.KEY && c.operator() != SearchOperator.EQUALS)
                    .findFirst()
                    .ifPresent(c -> {
                        throw new IllegalArgumentException(
                                "Search by KEY only supports the EQUALS operator. Got: " + c.operator() +
                                ". Use OBJECT_ID, OBJECT_TYPE or ATTRIBUTE criteria for range-style filtering.");
                    });
        }
    }

    public SortField sortField() {
        return sortField;
    }

    public SortDirection sortDirection() {
        return sortDirection;
    }

    public List<SearchCriteria> criteria() {
        return criteria;
    }

    public int pageSize() {
        return pageSize;
    }

    public Set<String> forceUsingConfigs() {
        return forceUsingConfigs;
    }

    public boolean forceAllConfigs() {
        return forceAllConfigs;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final var that = (CxLocalizedValueSearchData) obj;
        return this.sortField == that.sortField &&
                this.sortDirection == that.sortDirection &&
                Objects.equals(this.criteria, that.criteria) &&
                this.pageSize == that.pageSize &&
                Objects.equals(this.forceUsingConfigs, that.forceUsingConfigs) &&
                this.forceAllConfigs == that.forceAllConfigs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sortField, sortDirection, criteria, pageSize, forceUsingConfigs, forceAllConfigs);
    }

    @Override
    public String toString() {
        return "CxLocalizedValueSearchData[" +
                "sortField=" + sortField + ", " +
                "sortDirection=" + sortDirection + ", " +
                "criteria=" + criteria + ", " +
                "pageSize=" + pageSize + ", " +
                "forceUsingConfigs=" + forceUsingConfigs + ", " +
                "forceAllConfigs=" + forceAllConfigs + ']';
    }


}
