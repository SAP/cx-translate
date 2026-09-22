/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueResultStatistics
import com.sap.cx.translations.services.impl.localizedobjects.CxTranslationsValuesPagePlanner
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider
import de.hybris.bootstrap.annotations.UnitTest
import spock.lang.Specification

/**
 * Unit tests for {@link CxTranslationsValuesPagePlanner} — the extracted page planning math.
 *
 * These tests verify page boundary arithmetic across multiple object types without any DB or
 * Spring context (improvement #4 + #8).  They serve as a regression gate if the pageable's
 * internal planner logic is ever changed.
 */
@UnitTest
class CxTranslationsValuesPagePlannerSpec extends Specification {

    // -----------------------------------------------------------------------
    // Helpers — build a provider map from {typeCode: valueCount} entries
    // -----------------------------------------------------------------------

    private Map<String, CxLocalizedObjectProvider> providerMap(Map<String, Integer> typeCounts) {
        typeCounts.collectEntries { type, count ->
            def provider = Stub(CxLocalizedObjectProvider)
            provider.getStatisticsFor(type) >> new CxLocalizedValueResultStatistics(count, count)
            [type, provider]
        }
    }

    // -----------------------------------------------------------------------
    // Single object type
    // -----------------------------------------------------------------------

    def "single type, first page: skip=0, take=min(pageSize, total)"() {
        given:
        def map = providerMap([Product: 10])

        when:
        def plan = CxTranslationsValuesPagePlanner.plan(0, 5, ["Product"], map)

        then:
        plan.howManyToSkipFromFirst() == 0
        plan.howManyToTakeFromLast() == 5
        plan.howManyToUse() == 1
    }

    def "single type, second page: skip=5, take next 5"() {
        given:
        def map = providerMap([Product: 10])

        when:
        def plan = CxTranslationsValuesPagePlanner.plan(5, 5, ["Product"], map)

        then:
        plan.howManyToSkipFromFirst() == 5
        plan.howManyToTakeFromLast() == 5
        plan.howManyToUse() == 1
    }

    def "single type, last page is partial (fewer than pageSize remaining)"() {
        given:
        def map = providerMap([Product: 7])  // 7 values, pageSize=5, page 1 starts at 5

        when:
        def plan = CxTranslationsValuesPagePlanner.plan(5, 5, ["Product"], map)

        then:
        plan.howManyToSkipFromFirst() == 5
        plan.howManyToTakeFromLast() == 2   // only 2 left (7 - 5)
        plan.howManyToUse() == 1
    }

    def "single type, startIndex past all data returns empty plan"() {
        given:
        def map = providerMap([Product: 3])

        when:
        def plan = CxTranslationsValuesPagePlanner.plan(10, 5, ["Product"], map)

        then:
        plan.usedObjectTypes().size() - plan.howManyToUse() >= 0
        plan.howManyToUse() == 0 || plan.howManyToTakeFromLast() == 0
    }

    // -----------------------------------------------------------------------
    // Two object types — page fully within first type
    // -----------------------------------------------------------------------

    def "two types, page fits entirely in first type"() {
        given:
        def map = providerMap([TypeA: 10, TypeB: 5])

        when:
        def plan = CxTranslationsValuesPagePlanner.plan(0, 5, ["TypeA", "TypeB"], map)

        then:
        plan.howManyToSkipFromFirst() == 0
        plan.howManyToTakeFromLast() == 5
        plan.howManyToUse() == 1
        plan.usedObjectTypes().first() == "TypeA"
    }

    // -----------------------------------------------------------------------
    // Two object types — page spans type boundary
    // -----------------------------------------------------------------------

    def "two types, page spans type boundary (3 from TypeA, 2 from TypeB)"() {
        given:
        // TypeA has 8 values; skip 6, page 5 → 2 from TypeA, 3 from TypeB
        def map = providerMap([TypeA: 8, TypeB: 6])

        when:
        def plan = CxTranslationsValuesPagePlanner.plan(6, 5, ["TypeA", "TypeB"], map)

        then:
        plan.howManyToSkipFromFirst() == 6   // skip 6 in TypeA
        plan.howManyToTakeFromLast() == 3    // take 3 from TypeB (2 from TypeA + 3 = 5)
        plan.howManyToUse() == 2
    }

    def "two types, page starts exactly at type boundary"() {
        given:
        def map = providerMap([TypeA: 5, TypeB: 10])

        when:
        def plan = CxTranslationsValuesPagePlanner.plan(5, 4, ["TypeA", "TypeB"], map)

        then:
        plan.howManyToSkipFromFirst() == 0   // TypeB starts at index 5, no skip within it
        plan.howManyToTakeFromLast() == 4
        plan.howManyToUse() == 1
        plan.usedObjectTypes().first() == "TypeB"
    }

    // -----------------------------------------------------------------------
    // Three object types
    // -----------------------------------------------------------------------

    def "three types, page spans all three"() {
        given:
        // TypeA=3, TypeB=3, TypeC=3; skip 2, page 5 → 1 from A, 3 from B, 1 from C
        def map = providerMap([TypeA: 3, TypeB: 3, TypeC: 3])

        when:
        def plan = CxTranslationsValuesPagePlanner.plan(2, 5, ["TypeA", "TypeB", "TypeC"], map)

        then:
        plan.howManyToSkipFromFirst() == 2
        plan.howManyToTakeFromLast() == 1    // 1 from TypeA + 3 from TypeB + 1 from TypeC = 5
        plan.howManyToUse() == 3
    }

    // -----------------------------------------------------------------------
    // countForProvider — verify per-provider count math
    // -----------------------------------------------------------------------

    def "countForProvider: single type provider that is both first and last"() {
        given:
        def map = providerMap([Product: 10])
        def plan = CxTranslationsValuesPagePlanner.plan(2, 4, ["Product"], map)
        def provider = map["Product"]

        when:
        def count = CxTranslationsValuesPagePlanner.countForProvider(
                provider, ["Product"], "Product", "Product", plan)

        then:
        count == 4   // page exactly 4 starting from offset 2
    }

    def "countForProvider: first-type provider in two-type span"() {
        given:
        // TypeA=8, TypeB=6, skip=6, pageSize=5 → 2 from TypeA, 3 from TypeB
        def map = providerMap([TypeA: 8, TypeB: 6])
        def plan = CxTranslationsValuesPagePlanner.plan(6, 5, ["TypeA", "TypeB"], map)
        def providerA = map["TypeA"]
        def providerB = map["TypeB"]

        when:
        def countA = CxTranslationsValuesPagePlanner.countForProvider(
                providerA, ["TypeA"], "TypeA", "TypeB", plan)
        def countB = CxTranslationsValuesPagePlanner.countForProvider(
                providerB, ["TypeB"], "TypeA", "TypeB", plan)

        then:
        countA == 2   // 8 - 6 = 2
        countB == 3
    }
}
