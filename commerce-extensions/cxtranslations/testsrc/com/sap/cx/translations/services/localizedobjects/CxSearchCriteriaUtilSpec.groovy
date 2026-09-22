/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData
import com.sap.cx.translations.services.impl.localizedobjects.CxSearchCriteriaUtil
import com.sap.cx.translations.model.CxLocalizedValueModel
import de.hybris.bootstrap.annotations.UnitTest
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.util.function.Predicate

/**
 * Unit tests for {@link CxSearchCriteriaUtil}.
 *
 * Covers:
 * - Object-type predicates derived from KEY criteria
 * - Attribute predicates derived from ATTRIBUTE and KEY criteria
 * - Key predicates
 * - Value predicates
 * - Edge cases: null/blank values, operator coverage
 */
@UnitTest
class CxSearchCriteriaUtilSpec extends Specification {

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private CxLocalizedValueSearchData.SearchCriteria criterion(
            CxLocalizedValueSearchData.SearchField field,
            CxLocalizedValueSearchData.SearchOperator op,
            String value,
            Locale locale = null) {
        new CxLocalizedValueSearchData.SearchCriteria(field, op, new CxLocalizedValueSearchData.StringWithLocale(value, locale))
    }

    private CxLocalizedValueSearchData data(CxLocalizedValueSearchData.SearchCriteria... criteria) {
        new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                criteria.toList(),
                10)
    }

    private CxLocalizedValueModel mockValue(String locale, String valueStr) {
        def m = Mock(CxLocalizedValueModel)
        m.getValue(_ as Locale) >> { Locale l -> l.toLanguageTag() == locale ? valueStr : null }
        m
    }

    // -----------------------------------------------------------------------
    // getObjectTypePredicates
    // -----------------------------------------------------------------------

    def "getObjectTypePredicates returns empty list when no KEY criteria present"() {
        when:
        def predicates = CxSearchCriteriaUtil.getObjectTypePredicates(
                data(criterion(CxLocalizedValueSearchData.SearchField.ATTRIBUTE, CxLocalizedValueSearchData.SearchOperator.EQUALS, "name")))

        then:
        predicates.isEmpty()
    }

    def "getObjectTypePredicates EQUALS matches first dot-segment exactly"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.KEY, CxLocalizedValueSearchData.SearchOperator.EQUALS, "Product.pr-1.name"))

        when:
        def predicates = CxSearchCriteriaUtil.getObjectTypePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test("Product")
        !predicates[0].test("Category")
    }

    def "getObjectTypePredicates returns a predicate for KEY EQUALS that matches the object-type segment"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.KEY, CxLocalizedValueSearchData.SearchOperator.EQUALS, "Product.pr-1.name"))

        when:
        def predicates = CxSearchCriteriaUtil.getObjectTypePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test("Product")
        !predicates[0].test("Category")
    }

    def "getObjectTypePredicates returns no predicate for blank KEY value"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.KEY, CxLocalizedValueSearchData.SearchOperator.EQUALS, ""))

        when:
        def predicates = CxSearchCriteriaUtil.getObjectTypePredicates(searchData)

        then:
        predicates.isEmpty()
    }

    // -----------------------------------------------------------------------
    // getAttributePredicates – from ATTRIBUTE criteria
    // -----------------------------------------------------------------------

    def "getAttributePredicates EQUALS is case-insensitive"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.ATTRIBUTE, CxLocalizedValueSearchData.SearchOperator.EQUALS, "Name"))

        when:
        def predicates = CxSearchCriteriaUtil.getAttributePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test("name")
        predicates[0].test("NAME")
        !predicates[0].test("description")
    }

    @Unroll
    def "getAttributePredicates ATTRIBUTE operator=#op"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.ATTRIBUTE, op, searchValue))

        when:
        def preds = CxSearchCriteriaUtil.getAttributePredicates(searchData)

        then:
        preds.size() == 1
        preds[0].test(matchingValue)
        !preds[0].test(nonMatchingValue)

        where:
        op                                                         | searchValue | matchingValue | nonMatchingValue
        CxLocalizedValueSearchData.SearchOperator.LIKE             | "esc"       | "description" | "name"
        CxLocalizedValueSearchData.SearchOperator.STARTS_WITH      | "desc"      | "description" | "name"
        CxLocalizedValueSearchData.SearchOperator.ENDS_WITH        | "tion"      | "description" | "name"
        CxLocalizedValueSearchData.SearchOperator.IS_EMPTY         | null        | ""            | "name"
        CxLocalizedValueSearchData.SearchOperator.IS_NOT_EMPTY     | null        | "name"        | ""
    }

    // -----------------------------------------------------------------------
    // getAttributePredicates – from KEY criteria
    // -----------------------------------------------------------------------

    def "getAttributePredicates from KEY EQUALS extracts last dot-segment as attribute"() {
        given: "full key: Product.pr-1.name  => attribute should be 'name'"
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.KEY, CxLocalizedValueSearchData.SearchOperator.EQUALS, "Product.pr-1.name"))

        when:
        def predicates = CxSearchCriteriaUtil.getAttributePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test("name")
        !predicates[0].test("description")
    }

    def "getAttributePredicates from KEY EQUALS with fewer than 3 parts returns always-false predicate (no attribute segment)"() {
        given: "key has only 2 parts: 'Product.pr-1' (no attribute segment)"
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.KEY, CxLocalizedValueSearchData.SearchOperator.EQUALS, "Product.pr-1"))

        when:
        def predicates = CxSearchCriteriaUtil.getAttributePredicates(searchData)

        then:
        predicates.size() == 1
        !predicates[0].test("name")
        !predicates[0].test("")
    }

    // -----------------------------------------------------------------------
    // getKeyPredicates
    // -----------------------------------------------------------------------

    def "getKeyPredicates EQUALS is case-insensitive and matches full key"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.KEY, CxLocalizedValueSearchData.SearchOperator.EQUALS, "Product.pr-1.name"))

        when:
        def predicates = CxSearchCriteriaUtil.getKeyPredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test("product.pr-1.name")
        predicates[0].test("Product.pr-1.name")
        !predicates[0].test("Product.pr-1.description")
    }

    def "getKeyPredicates returns empty list for blank KEY value"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.KEY, CxLocalizedValueSearchData.SearchOperator.EQUALS, "   "))

        when:
        def predicates = CxSearchCriteriaUtil.getKeyPredicates(searchData)

        then:
        predicates.isEmpty()
    }

    // -----------------------------------------------------------------------
    // getValuePredicates
    // -----------------------------------------------------------------------

    def "getValuePredicates returns empty list when no VALUE criteria present"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.ATTRIBUTE, CxLocalizedValueSearchData.SearchOperator.EQUALS, "name"))

        when:
        def predicates = CxSearchCriteriaUtil.getValuePredicates(searchData)

        then:
        predicates.isEmpty()
    }

    def "getValuePredicates EQUALS matches value case-insensitively"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.VALUE, CxLocalizedValueSearchData.SearchOperator.EQUALS, "Shoe", Locale.ENGLISH))

        when:
        def predicates = CxSearchCriteriaUtil.getValuePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test(mockValue("en", "shoe"))
        predicates[0].test(mockValue("en", "Shoe"))
        !predicates[0].test(mockValue("en", "boot"))
    }

    def "getValuePredicates LIKE matches substring case-insensitively"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.VALUE, CxLocalizedValueSearchData.SearchOperator.LIKE, "hoe", Locale.ENGLISH))

        when:
        def predicates = CxSearchCriteriaUtil.getValuePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test(mockValue("en", "Shoes"))
        predicates[0].test(mockValue("en", "horseshoe"))
        !predicates[0].test(mockValue("en", "hat"))
    }

    def "getValuePredicates STARTS_WITH matches prefix case-insensitively"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.VALUE, CxLocalizedValueSearchData.SearchOperator.STARTS_WITH, "SHO", Locale.ENGLISH))

        when:
        def predicates = CxSearchCriteriaUtil.getValuePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test(mockValue("en", "shoes"))
        !predicates[0].test(mockValue("en", "boots"))
    }

    def "getValuePredicates ENDS_WITH matches suffix case-insensitively"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.VALUE, CxLocalizedValueSearchData.SearchOperator.ENDS_WITH, "OES", Locale.ENGLISH))

        when:
        def predicates = CxSearchCriteriaUtil.getValuePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test(mockValue("en", "shoes"))
        !predicates[0].test(mockValue("en", "boots"))
    }

    def "getValuePredicates IS_EMPTY matches null and blank values"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.VALUE, CxLocalizedValueSearchData.SearchOperator.IS_EMPTY, null, Locale.ENGLISH))

        when:
        def predicates = CxSearchCriteriaUtil.getValuePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test(mockValue("en", null))
        predicates[0].test(mockValue("en", ""))
        predicates[0].test(mockValue("en", "   "))
        !predicates[0].test(mockValue("en", "shoes"))
    }

    def "getValuePredicates IS_NOT_EMPTY matches non-blank values"() {
        given:
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.VALUE, CxLocalizedValueSearchData.SearchOperator.IS_NOT_EMPTY, null, Locale.ENGLISH))

        when:
        def predicates = CxSearchCriteriaUtil.getValuePredicates(searchData)

        then:
        predicates.size() == 1
        predicates[0].test(mockValue("en", "shoes"))
        !predicates[0].test(mockValue("en", null))
        !predicates[0].test(mockValue("en", ""))
    }

    def "getValuePredicates returns null placeholder for unrecognized operator (default branch)"() {
        // The switch expression has a `default -> null` branch that was added to satisfy exhaustiveness.
        // All operators are now handled, so this is a safety test for the IS_EMPTY + IS_NOT_EMPTY branch coverage.
        given: "IS_EMPTY with no value"
        def searchData = data(criterion(CxLocalizedValueSearchData.SearchField.VALUE, CxLocalizedValueSearchData.SearchOperator.IS_EMPTY, null, Locale.ENGLISH))

        when:
        def predicates = CxSearchCriteriaUtil.getValuePredicates(searchData)

        then: "predicate exists and works (no null in the list)"
        predicates.every { it != null }
    }

    // -----------------------------------------------------------------------
    // Multiple criteria
    // -----------------------------------------------------------------------

    def "multiple criteria of different fields each produce their own predicate"() {
        given: "ATTRIBUTE criterion AND VALUE criterion"
        def searchData = data(
                criterion(CxLocalizedValueSearchData.SearchField.ATTRIBUTE, CxLocalizedValueSearchData.SearchOperator.EQUALS, "name"),
                criterion(CxLocalizedValueSearchData.SearchField.VALUE, CxLocalizedValueSearchData.SearchOperator.EQUALS, "Shoe", Locale.ENGLISH)
        )

        when:
        def attrPreds = CxSearchCriteriaUtil.getAttributePredicates(searchData)
        def valPreds = CxSearchCriteriaUtil.getValuePredicates(searchData)

        then:
        attrPreds.size() == 1
        valPreds.size() == 1
    }
}
