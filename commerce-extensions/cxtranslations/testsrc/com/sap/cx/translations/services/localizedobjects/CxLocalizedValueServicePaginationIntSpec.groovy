/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.type.TypeService
import jakarta.annotation.Resource
import spock.lang.Subject

/**
 * Integration tests verifying the pagination logic in
 * {@link com.sap.cx.translations.services.impl.localizedobjects.CxTranslationsValuesPageableImpl}.
 *
 * These tests exercise the arithmetic that translates "which page of values" into
 * "which slice of objects to load from DB".  They deliberately use small page sizes so
 * every interesting boundary condition can be covered with a small dataset.
 *
 * Guards review issues #1 (NPE on empty type list), #2 (off-by-one in object math), and
 * the general correctness of multi-page iteration.
 */
@IntegrationTest
class CxLocalizedValueServicePaginationIntSpec extends ServicelayerTransactionalSpockSpecification {

    private static final String CATALOG_ID = "pagTestCatalog"

    @Subject
    @Resource
    CxLocalizedValueService cxLocalizedValueService

    @Resource
    ModelService modelService

    @Resource
    TypeService typeService

    private CxTranslationTestDataBuilder builder
    private CatalogVersionModel stagedVersion

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService, typeService)
        def versions = builder.createCatalogWithVersions(CATALOG_ID)
        stagedVersion = versions.staged

        // Config: Product with 2 localized attributes (name, description)
        builder.createStandardConfig("pagProductConfig", "Product", ["code"],
                ["name", "description"], true, "catalogVersion", [stagedVersion])
    }

    // -----------------------------------------------------------------------
    // Basic multi-page iteration
    // -----------------------------------------------------------------------

    def "allResults() gathers all values across all pages"() {
        given: "5 products x 2 attrs = 10 values, page size 3"
        (1..5).each { i -> builder.createProduct("prod-$i", stagedVersion) }

        when:
        def pageable = cxLocalizedValueService.search(searchData(3))
        def all = pageable.allResults()

        then:
        all.size() == 10
    }

    def "page iteration via getPage(n) covers all values without duplicates"() {
        given: "6 products x 2 attrs = 12 values, page size 4 → 3 pages"
        (1..6).each { i -> builder.createProduct("p-$i", stagedVersion) }

        when:
        def pageable = cxLocalizedValueService.search(searchData(4))
        def page0 = pageable.getPage(0)
        def page1 = pageable.getPage(1)
        def page2 = pageable.getPage(2)

        then:
        page0.size() == 4
        page1.size() == 4
        page2.size() == 4

        and: "no duplicates across pages"
        def keys0 = page0.collect { it.key } as Set
        def keys1 = page1.collect { it.key } as Set
        def keys2 = page2.collect { it.key } as Set
        (keys0 + keys1 + keys2).size() == 12
        (keys0.intersect(keys1)).isEmpty()
        (keys0.intersect(keys2)).isEmpty()
        (keys1.intersect(keys2)).isEmpty()
    }

    def "last page may have fewer entries than pageSize"() {
        given: "5 products x 2 attrs = 10 values, page size 4 → 3rd page has 2 values"
        (1..5).each { i -> builder.createProduct("p-$i", stagedVersion) }

        when:
        def pageable = cxLocalizedValueService.search(searchData(4))

        then:
        pageable.getTotalPages() == 3
        pageable.getPage(2).size() == 2
    }

    def "getTotalPages returns 0 when there are no results"() {
        when:
        def pageable = cxLocalizedValueService.search(searchData(10))

        then:
        pageable.getTotalPages() == 0
    }

    def "getTotalPages returns 1 when results fit in one page"() {
        given: "1 product x 2 attrs = 2 values, page size 10"
        builder.createProduct("p-001", stagedVersion)

        when:
        def pageable = cxLocalizedValueService.search(searchData(10))

        then:
        pageable.getTotalPages() == 1
        pageable.getPage(0).size() == 2
    }

    // -----------------------------------------------------------------------
    // Page size = 1 (extreme case: every value is its own page)
    // -----------------------------------------------------------------------

    def "page size 1 yields a separate page per value"() {
        given: "2 products x 2 attrs = 4 values"
        builder.createProduct("a", stagedVersion)
        builder.createProduct("b", stagedVersion)

        when:
        def pageable = cxLocalizedValueService.search(searchData(1))

        then:
        pageable.getTotalPages() == 4
        (0..3).each { n -> assert pageable.getPage(n).size() == 1 }
    }

    // -----------------------------------------------------------------------
    // setPageNumber / getCurrentPage
    // -----------------------------------------------------------------------

    def "setPageNumber changes the page returned by getCurrentPage"() {
        given: "4 products x 2 attrs = 8 values, page size 2"
        (1..4).each { i -> builder.createProduct("p-$i", stagedVersion) }

        when:
        def pageable = cxLocalizedValueService.search(searchData(2))
        pageable.setPageNumber(0)
        def first = pageable.getCurrentPage()
        pageable.setPageNumber(3)
        def fourth = pageable.getCurrentPage()

        then:
        first.size() == 2
        fourth.size() == 2
        first.collect { it.key }.intersect(fourth.collect { it.key }).isEmpty()
    }

    // -----------------------------------------------------------------------
    // Value ordering is stable across pages
    // -----------------------------------------------------------------------

    def "values across pages form a contiguous sorted sequence when sorted by CODE ascending"() {
        given: "3 products, known codes so sort order is predictable"
        builder.createProduct("aaa", stagedVersion)
        builder.createProduct("bbb", stagedVersion)
        builder.createProduct("ccc", stagedVersion)

        when:
        def all = cxLocalizedValueService.search(searchData(2)).allResults()
        def keys = all.collect { it.key }

        then: "keys are sorted ascending (case-insensitive)"
        keys == keys.sort(false, String.CASE_INSENSITIVE_ORDER)
    }

    def "values across pages form a contiguous sorted sequence when sorted by CODE descending"() {
        given:
        builder.createProduct("aaa", stagedVersion)
        builder.createProduct("bbb", stagedVersion)
        builder.createProduct("ccc", stagedVersion)

        when:
        def all = cxLocalizedValueService.search(new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Descending,
                [], 2)).allResults()
        def keys = all.collect { it.key }

        then:
        keys == keys.sort(false, String.CASE_INSENSITIVE_ORDER.reversed())
    }

    // -----------------------------------------------------------------------
    // Page beyond the last page
    // -----------------------------------------------------------------------

    def "requesting a page index beyond total pages returns empty list without exception"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def pageable = cxLocalizedValueService.search(searchData(10))
        def result = pageable.getPage(99)

        then:
        result.isEmpty()
    }

    // -----------------------------------------------------------------------
    // page size exactly matches total count
    // -----------------------------------------------------------------------

    def "page size exactly equal to total count returns all on page 0 and empty on page 1"() {
        given: "2 products x 2 attrs = 4 values, page size = 4"
        builder.createProduct("p-001", stagedVersion)
        builder.createProduct("p-002", stagedVersion)

        when:
        def pageable = cxLocalizedValueService.search(searchData(4))

        then:
        pageable.getPage(0).size() == 4
        pageable.getPage(1).isEmpty()
        pageable.getTotalPages() == 1
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private static CxLocalizedValueSearchData searchData(int pageSize) {
        new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                [], pageSize)
    }
}
