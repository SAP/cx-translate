/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.util

import de.hybris.bootstrap.annotations.UnitTest
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.BiPredicate
import java.util.function.Predicate

@UnitTest
class CxTranslateListSplitUtilSpec extends Specification {

    // -------------------------------------------------------------------------
    // null guards
    // -------------------------------------------------------------------------

    def "splitWithBoundaryConstraint throws NPE when input is null"() {
        when:
        CxTranslateListSplitUtil.splitWithBoundaryConstraint(null, { it.size() >= 2 } as Predicate, null)

        then:
        thrown(NullPointerException)
    }

    def "splitWithBoundaryConstraint throws NPE when splitWhen is null"() {
        when:
        CxTranslateListSplitUtil.splitWithBoundaryConstraint(["a"], null, null)

        then:
        thrown(NullPointerException)
    }

    def "splitWithBoundaryConstraint accepts null mustStayTogether and defaults to no constraint"() {
        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(
                ["a", "b", "c", "d"],
                { it.size() >= 2 } as Predicate,
                null)

        then:
        result == [["a", "b"], ["c", "d"]]
    }

    // -------------------------------------------------------------------------
    // empty / single-element input
    // -------------------------------------------------------------------------

    def "splitWithBoundaryConstraint returns empty list when input is empty"() {
        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(
                [],
                { true } as Predicate,
                null)

        then:
        result.isEmpty()
    }

    def "splitWithBoundaryConstraint returns single-element sublist for single-element input"() {
        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(
                ["only"],
                { true } as Predicate,
                null)

        then:
        result == [["only"]]
    }

    // -------------------------------------------------------------------------
    // basic splitting
    // -------------------------------------------------------------------------

    def "splitWithBoundaryConstraint splits when splitWhen always returns true (every element becomes its own batch)"() {
        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(
                ["a", "b", "c"],
                { true } as Predicate,
                null)

        then:
        result == [["a"], ["b"], ["c"]]
    }

    def "splitWithBoundaryConstraint never splits when splitWhen always returns false"() {
        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(
                ["a", "b", "c", "d", "e"],
                { false } as Predicate,
                null)

        then:
        result == [["a", "b", "c", "d", "e"]]
    }

    @Unroll
    def "splitWithBoundaryConstraint splits #input by max-size #maxSize into #expected"() {
        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(
                input,
                { it.size() >= maxSize } as Predicate,
                null)

        then:
        result == expected

        where:
        input                                | maxSize | expected
        ["a", "b", "c", "d", "e", "f"]       | 2       | [["a", "b"], ["c", "d"], ["e", "f"]]
        ["a", "b", "c", "d", "e", "f"]       | 3       | [["a", "b", "c"], ["d", "e", "f"]]
        ["a", "b", "c", "d", "e"]            | 2       | [["a", "b"], ["c", "d"], ["e"]]
        ["a", "b", "c"]                      | 5       | [["a", "b", "c"]]
    }

    // -------------------------------------------------------------------------
    // boundary constraint — Javadoc example
    // -------------------------------------------------------------------------

    def "splitWithBoundaryConstraint keeps mustStayTogether pair together across split boundary"() {
        given:
        def input = ["a", "b", "c", "d", "e", "f"]
        def splitWhen = { it.size() >= 2 } as Predicate
        def mustStayTogether = { String a, String b -> a == "d" && b == "e" } as BiPredicate

        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(input, splitWhen, mustStayTogether)

        then:
        // [a, b] splits normally. [c, d] would split before e, but d-e must stay together so split is deferred.
        // Result: [[a, b], [c, d, e], [f]]
        result == [["a", "b"], ["c", "d", "e"], ["f"]]
    }

    def "splitWithBoundaryConstraint defers split repeatedly when boundary constraint keeps firing"() {
        given:
        // Every adjacent pair must stay together — should never split
        def input = ["a", "b", "c", "d"]
        def splitWhen = { it.size() >= 1 } as Predicate
        def mustStayTogether = { a, b -> true } as BiPredicate

        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(input, splitWhen, mustStayTogether)

        then:
        result == [["a", "b", "c", "d"]]
    }

    def "splitWithBoundaryConstraint splits at non-constrained boundaries only"() {
        given:
        // Pair (b,c) must stay together; other splits allowed
        def input = ["a", "b", "c", "d", "e"]
        def splitWhen = { it.size() >= 1 } as Predicate
        def mustStayTogether = { String x, String y -> x == "b" && y == "c" } as BiPredicate

        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(input, splitWhen, mustStayTogether)

        then:
        // After [a] consider b: splitWhen([a])=true, boundary(a,b)=false → split. current=[b]
        // After [b] consider c: splitWhen([b])=true, boundary(b,c)=true → keep. current=[b,c]
        // After [b,c] consider d: splitWhen([b,c])=true, boundary(c,d)=false → split. current=[d]
        // After [d] consider e: splitWhen([d])=true, boundary(d,e)=false → split. current=[e]
        result == [["a"], ["b", "c"], ["d"], ["e"]]
    }

    // -------------------------------------------------------------------------
    // result preserves order and contains all original elements
    // -------------------------------------------------------------------------

    def "splitWithBoundaryConstraint preserves element order and includes every element exactly once"() {
        given:
        def input = (1..10).collect { it.toString() }

        when:
        def result = CxTranslateListSplitUtil.splitWithBoundaryConstraint(
                input,
                { it.size() >= 3 } as Predicate,
                null)

        then:
        result.flatten() == input
    }
}
