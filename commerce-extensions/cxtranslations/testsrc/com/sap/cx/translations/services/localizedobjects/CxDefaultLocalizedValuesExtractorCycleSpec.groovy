/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel
import com.sap.cx.translations.services.impl.localizedobjects.CxDefaultLocalizedValuesExtractor
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValuesSearchSupport
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.type.TypeService
import spock.lang.Specification

/**
 * Unit tests for the cycle-detection guard in {@link CxDefaultLocalizedValuesExtractor}.
 *
 * The extractor recurses through {@code config.extendedBy} children in its constructor.
 * Without detection a cyclic {@code extendedBy} relation would cause a {@link StackOverflowError}.
 * With the fix it throws {@link IllegalStateException} early with a diagnostic message listing
 * the cycle (review finding #14).
 */
@UnitTest
class CxDefaultLocalizedValuesExtractorCycleSpec extends Specification {

    // CxLocalizedValueSearchData is final — construct a real minimal instance
    def searchData = new CxLocalizedValueSearchData(
            CxLocalizedValueSearchData.SortField.CODE,
            CxLocalizedValueSearchData.SortDirection.Ascending,
            [],
            10)
    // CxLocalizedValuesSearchSupport is constructed from searchData; both are cheap to create
    def searchSupport = new CxLocalizedValuesSearchSupport(searchData)
    def modelService = Mock(ModelService)
    def typeService = Mock(TypeService)

    private CxStandardLocalizedObjectConfigModel configWith(String code, Set<CxStandardLocalizedObjectConfigModel> children) {
        def cfg = Stub(CxStandardLocalizedObjectConfigModel)
        cfg.code >> code
        cfg.extendedBy >> children
        cfg.localizedAttributes >> []
        cfg.catalogAware >> false
        return cfg
    }

    def "no cycle — chain A extendedBy B extendedBy C — constructs successfully"() {
        given:
        def c = configWith("C", [] as Set)
        def b = configWith("B", [c] as Set)
        def a = configWith("A", [b] as Set)

        when:
        new CxDefaultLocalizedValuesExtractor(searchData, a, searchSupport, modelService, typeService)

        then:
        noExceptionThrown()
    }

    def "direct self-cycle A extendedBy A — throws IllegalStateException"() {
        given:
        def a = Stub(CxStandardLocalizedObjectConfigModel)
        a.code >> "A"
        a.extendedBy >> ([a] as Set)
        a.localizedAttributes >> []
        a.catalogAware >> false

        when:
        new CxDefaultLocalizedValuesExtractor(searchData, a, searchSupport, modelService, typeService)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains("Cyclic config extension detected")
        ex.message.contains("A")
    }

    def "indirect cycle A extendedBy B extendedBy A — throws IllegalStateException"() {
        given:
        def aChildren = [] as Set
        def bChildren = [] as Set

        def a = Stub(CxStandardLocalizedObjectConfigModel)
        a.code >> "configA"
        a.extendedBy >> aChildren
        a.localizedAttributes >> []
        a.catalogAware >> false

        def b = Stub(CxStandardLocalizedObjectConfigModel)
        b.code >> "configB"
        b.extendedBy >> bChildren
        b.localizedAttributes >> []
        b.catalogAware >> false

        aChildren.add(b)
        bChildren.add(a)

        when:
        new CxDefaultLocalizedValuesExtractor(searchData, a, searchSupport, modelService, typeService)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains("Cyclic config extension detected")
        ex.message.contains("configA")
    }

    def "diamond — A extendedBy B and C, B and C both extendedBy D — no cycle, constructs successfully"() {
        given:
        def d = configWith("D", [] as Set)
        def b = configWith("B", [d] as Set)
        def c = configWith("C", [d] as Set)
        def a = configWith("A", [b, c] as Set)

        when:
        new CxDefaultLocalizedValuesExtractor(searchData, a, searchSupport, modelService, typeService)

        then:
        noExceptionThrown()
    }
}
