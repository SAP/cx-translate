/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.services.impl.localizedobjects.CxFlexibleSearchQueryPart
import com.sap.cx.translations.services.impl.localizedobjects.CxParamNameGenerator
import de.hybris.bootstrap.annotations.UnitTest
import spock.lang.Specification

/**
 * Unit tests for {@link CxParamNameGenerator} and {@link CxFlexibleSearchQueryPart}.
 */
@UnitTest
class CxQueryBuildingUtilsSpec extends Specification {

    // -----------------------------------------------------------------------
    // CxParamNameGenerator
    // -----------------------------------------------------------------------

    def "next returns seed+0 on first call"() {
        given:
        def gen = new CxParamNameGenerator()

        expect:
        gen.next("objectType") == "objectType0"
    }

    def "next increments per seed independently"() {
        given:
        def gen = new CxParamNameGenerator()

        when:
        def a0 = gen.next("objectType")
        def b0 = gen.next("catalogId")
        def a1 = gen.next("objectType")
        def b1 = gen.next("catalogId")

        then:
        a0 == "objectType0"
        b0 == "catalogId0"
        a1 == "objectType1"
        b1 == "catalogId1"
    }

    def "next produces unique names across many calls"() {
        given:
        def gen = new CxParamNameGenerator()
        def names = (0..<20).collect { gen.next("pk") }

        expect:
        names == (0..<20).collect { "pk${it}" }
        names.toSet().size() == 20   // all distinct
    }

    // -----------------------------------------------------------------------
    // CxFlexibleSearchQueryPart
    // -----------------------------------------------------------------------

    def "ALWAYS_TRUE has no parameters and matching SQL"() {
        expect:
        CxFlexibleSearchQueryPart.ALWAYS_TRUE.queryPart() == "1=1"
        CxFlexibleSearchQueryPart.ALWAYS_TRUE.parameters().isEmpty()
    }

    def "ALWAYS_FALSE has no parameters and non-matching SQL"() {
        expect:
        CxFlexibleSearchQueryPart.ALWAYS_FALSE.queryPart() == "0=1"
        CxFlexibleSearchQueryPart.ALWAYS_FALSE.parameters().isEmpty()
    }

    def "and() with empty list returns ALWAYS_TRUE"() {
        when:
        def result = CxFlexibleSearchQueryPart.and([])

        then:
        result.queryPart() == "1=1"
        result.parameters().isEmpty()
    }

    def "and() with single part returns that part's SQL and parameters"() {
        given:
        def part = new CxFlexibleSearchQueryPart("{p.code} = ?code0", ["code0": "p-001"])

        when:
        def result = CxFlexibleSearchQueryPart.and([part])

        then:
        result.queryPart() == "{p.code} = ?code0"
        result.parameters() == ["code0": "p-001"]
    }

    def "and() merges SQL with AND and merges parameter maps"() {
        given:
        def p1 = new CxFlexibleSearchQueryPart("{p.code} = ?code0", ["code0": "p-001"])
        def p2 = new CxFlexibleSearchQueryPart("{p.catalogVersion} IN (?cv0)", ["cv0": "Staged"])

        when:
        def result = CxFlexibleSearchQueryPart.and([p1, p2])

        then:
        result.queryPart() == "{p.code} = ?code0 AND {p.catalogVersion} IN (?cv0)"
        result.parameters() == ["code0": "p-001", "cv0": "Staged"]
    }

    def "and() varargs form works the same as list form"() {
        given:
        def p1 = new CxFlexibleSearchQueryPart("a=?a", ["a": 1])
        def p2 = new CxFlexibleSearchQueryPart("b=?b", ["b": 2])

        when:
        def listResult = CxFlexibleSearchQueryPart.and([p1, p2])
        def varargResult = CxFlexibleSearchQueryPart.and(p1, p2)

        then:
        listResult.queryPart() == varargResult.queryPart()
        listResult.parameters() == varargResult.parameters()
    }
}
