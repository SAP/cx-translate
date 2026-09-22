/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueResultStatistics
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData
import de.hybris.bootstrap.annotations.UnitTest
import spock.lang.Specification

/**
 * Unit tests for the data/record value classes:
 * <ul>
 *   <li>{@link CxLocalizedValueResultStatistics} — record with objectCount, valueCount, ranges</li>
 *   <li>{@link CxLocalizedValueSearchData} — search query parameters with equals/hashCode/toString</li>
 * </ul>
 */
@UnitTest
class CxLocalizedValueDataClassesSpec extends Specification {

    // -----------------------------------------------------------------------
    // CxLocalizedValueResultStatistics
    // -----------------------------------------------------------------------

    def "CxLocalizedValueResultStatistics two-arg constructor sets objectCount and valueCount"() {
        when:
        def stats = new CxLocalizedValueResultStatistics(42, 126)

        then:
        stats.objectCount() == 42
        stats.valueCount() == 126
        stats.ranges().isEmpty()
    }

    def "CxLocalizedValueResultStatistics zero statistics"() {
        when:
        def stats = new CxLocalizedValueResultStatistics(0, 0)

        then:
        stats.objectCount() == 0
        stats.valueCount() == 0
    }

    def "CxLocalizedValueResultStatistics record equality"() {
        given:
        def a = new CxLocalizedValueResultStatistics(10, 30)
        def b = new CxLocalizedValueResultStatistics(10, 30)
        def c = new CxLocalizedValueResultStatistics(10, 31)

        expect:
        a == b
        a != c
    }

    // -----------------------------------------------------------------------
    // CxLocalizedValueSearchData — construction and accessors
    // -----------------------------------------------------------------------

    def "CxLocalizedValueSearchData 4-arg constructor sets defaults for force* fields"() {
        when:
        def searchData = new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                [],
                20)

        then:
        searchData.sortField() == CxLocalizedValueSearchData.SortField.CODE
        searchData.sortDirection() == CxLocalizedValueSearchData.SortDirection.Ascending
        searchData.criteria().isEmpty()
        searchData.pageSize() == 20
        searchData.forceUsingConfigs().isEmpty()
        searchData.forceAllConfigs() == false
    }

    def "CxLocalizedValueSearchData 6-arg constructor with forceUsingConfigs"() {
        when:
        def searchData = new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.PK,
                CxLocalizedValueSearchData.SortDirection.Descending,
                [],
                5,
                ["myConfig"] as Set,
                false)

        then:
        searchData.sortField() == CxLocalizedValueSearchData.SortField.PK
        searchData.forceUsingConfigs() == ["myConfig"] as Set
        searchData.forceAllConfigs() == false
    }

    def "CxLocalizedValueSearchData 6-arg constructor with null forceUsingConfigs normalises to empty set"() {
        when:
        def searchData = new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                [],
                10,
                null,    // <-- null provided
                false)

        then: "null is silently replaced with empty set (no NPE later)"
        searchData.forceUsingConfigs() != null
        searchData.forceUsingConfigs().isEmpty()
    }

    def "CxLocalizedValueSearchData equals is based on all fields"() {
        given:
        def a = new CxLocalizedValueSearchData(CxLocalizedValueSearchData.SortField.CODE, CxLocalizedValueSearchData.SortDirection.Ascending, [], 10)
        def b = new CxLocalizedValueSearchData(CxLocalizedValueSearchData.SortField.CODE, CxLocalizedValueSearchData.SortDirection.Ascending, [], 10)
        def c = new CxLocalizedValueSearchData(CxLocalizedValueSearchData.SortField.CODE, CxLocalizedValueSearchData.SortDirection.Descending, [], 10)

        expect:
        a == b
        a != c
        a == a  // reflexive
    }

    def "CxLocalizedValueSearchData hashCode is consistent with equals"() {
        given:
        def a = new CxLocalizedValueSearchData(CxLocalizedValueSearchData.SortField.CODE, CxLocalizedValueSearchData.SortDirection.Ascending, [], 10)
        def b = new CxLocalizedValueSearchData(CxLocalizedValueSearchData.SortField.CODE, CxLocalizedValueSearchData.SortDirection.Ascending, [], 10)

        expect:
        a.hashCode() == b.hashCode()
    }

    def "CxLocalizedValueSearchData toString contains field names"() {
        when:
        def s = new CxLocalizedValueSearchData(CxLocalizedValueSearchData.SortField.OBJECT_TYPE,
                CxLocalizedValueSearchData.SortDirection.Descending, [], 7).toString()

        then:
        s.contains("sortField")
        s.contains("OBJECT_TYPE")
    }

    // -----------------------------------------------------------------------
    // SearchCriteria
    // -----------------------------------------------------------------------

    def "SearchCriteria record stores field, operator, and value"() {
        given:
        def value = new CxLocalizedValueSearchData.StringWithLocale("Product", null)

        when:
        def crit = new CxLocalizedValueSearchData.SearchCriteria(
                CxLocalizedValueSearchData.SearchField.OBJECT_TYPE,
                CxLocalizedValueSearchData.SearchOperator.EQUALS,
                value)

        then:
        crit.field() == CxLocalizedValueSearchData.SearchField.OBJECT_TYPE
        crit.operator() == CxLocalizedValueSearchData.SearchOperator.EQUALS
        crit.value().stringValue() == "Product"
        crit.value().locale() == null
    }

    def "StringWithLocale 1-arg constructor normalises locale to null"() {
        when:
        def swl = new CxLocalizedValueSearchData.StringWithLocale("hello")

        then:
        swl.stringValue() == "hello"
        swl.locale() == null
    }

    def "StringWithLocale 2-arg constructor preserves locale"() {
        when:
        def swl = new CxLocalizedValueSearchData.StringWithLocale("shoe", Locale.GERMAN)

        then:
        swl.stringValue() == "shoe"
        swl.locale() == Locale.GERMAN
    }

    // -----------------------------------------------------------------------
    // KEY operator validation — only EQUALS is supported
    // -----------------------------------------------------------------------

    def "CxLocalizedValueSearchData constructor accepts KEY EQUALS without exception"() {
        given:
        def crit = new CxLocalizedValueSearchData.SearchCriteria(
                CxLocalizedValueSearchData.SearchField.KEY,
                CxLocalizedValueSearchData.SearchOperator.EQUALS,
                new CxLocalizedValueSearchData.StringWithLocale("Product.p-001.name"))

        when:
        def sd = new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                [crit], 10)

        then:
        notThrown(IllegalArgumentException)
        sd.criteria().size() == 1
    }

    @spock.lang.Unroll
    def "CxLocalizedValueSearchData constructor rejects KEY #op — only EQUALS is supported"() {
        given:
        def crit = new CxLocalizedValueSearchData.SearchCriteria(
                CxLocalizedValueSearchData.SearchField.KEY,
                op,
                new CxLocalizedValueSearchData.StringWithLocale("Product.p-001"))

        when:
        new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                [crit], 10)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("KEY")
        ex.message.contains("EQUALS")

        where:
        op << [
                CxLocalizedValueSearchData.SearchOperator.STARTS_WITH,
                CxLocalizedValueSearchData.SearchOperator.ENDS_WITH,
                CxLocalizedValueSearchData.SearchOperator.LIKE,
                CxLocalizedValueSearchData.SearchOperator.IS_EMPTY,
                CxLocalizedValueSearchData.SearchOperator.IS_NOT_EMPTY,
        ]
    }

    def "CxLocalizedValueSearchData constructor does not validate non-KEY criteria"() {
        given: "STARTS_WITH on OBJECT_ID is perfectly valid — only KEY is restricted"
        def crit = new CxLocalizedValueSearchData.SearchCriteria(
                CxLocalizedValueSearchData.SearchField.OBJECT_ID,
                CxLocalizedValueSearchData.SearchOperator.STARTS_WITH,
                new CxLocalizedValueSearchData.StringWithLocale("p-"))

        when:
        def sd = new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                [crit], 10)

        then:
        notThrown(IllegalArgumentException)
        sd.criteria().size() == 1
    }
}
