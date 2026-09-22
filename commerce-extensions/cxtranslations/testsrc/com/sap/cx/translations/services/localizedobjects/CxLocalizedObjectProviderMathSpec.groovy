/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.model.CxLocalizedObjectConfigModel
import com.sap.cx.translations.model.CxLocalizedValueModel
import com.sap.cx.translations.services.localizedobjects.CxAbstractItemBasedLocalizedObjectProvider
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueResultStatistics
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider.ItemWithIds
import de.hybris.bootstrap.annotations.UnitTest
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.util.stream.Stream

/**
 * Unit tests for the default math methods on {@link CxLocalizedObjectProvider} and the
 * {@code searchForLocalisedValues} method of {@link CxAbstractItemBasedLocalizedObjectProvider}.
 */
@UnitTest
class CxLocalizedObjectProviderMathSpec extends Specification {
    /**
     * Minimal concrete implementation — all methods are no-op; we only test
     * the {@code default} methods which are inherited as-is.
     */
    @Subject
    CxLocalizedObjectProvider provider = new CxLocalizedObjectProvider() {
        @Override List<String> allSupportedObjectTypes() { [] }
        @Override boolean canReturnObjectType(String t) { false }
        @Override List<CxLocalizedValueModel> searchForLocalisedValues(List<String> types, int skip, int count) { [] }
        @Override Stream<CxLocalizedValueModel> extractValues(ItemWithIds item) { Stream.empty() }
        @Override int howManyValuesPerObjectForObjectType(String objectType) { 1 }
        @Override Map<String, CxLocalizedValueResultStatistics> getStatistics() { [:] }
        @Override CxLocalizedObjectConfigModel getConfig() { null }
    }

    // -----------------------------------------------------------------------
    // calculateHowManyObjectsToGet
    // -----------------------------------------------------------------------

    def "calculateHowManyObjectsToGet(0, 1) correctly returns 0 (valuesPerObject=1 special-case path)"() {
        expect:
        provider.calculateHowManyObjectsToGet(0, 1) == 0
    }

    def "calculateHowManyObjectsToGet with valuesPerObject=1 is identity"() {
        expect:
        provider.calculateHowManyObjectsToGet(5, 1) == 5
        provider.calculateHowManyObjectsToGet(1, 1) == 1
    }

    @Unroll
    def "calculateHowManyObjectsToGet(#valueCount, 3) == #expected (ceiling division)"() {
        expect:
        provider.calculateHowManyObjectsToGet(valueCount, 3) == expected

        where:
        valueCount | expected
        1          | 1
        2          | 1
        3          | 1
        4          | 2
        5          | 2
        6          | 2
        7          | 3
    }

    @Unroll
    def "calculateHowManyObjectsToGet(#valueCount, 5) == #expected"() {
        expect:
        provider.calculateHowManyObjectsToGet(valueCount, 5) == expected

        where:
        valueCount | expected
        1          | 1
        5          | 1
        6          | 2
        10         | 2
        11         | 3
    }

    // -----------------------------------------------------------------------
    // calculateHowManyObjectsToSkip
    // -----------------------------------------------------------------------

    def "calculateHowManyObjectsToSkip returns 0 when valuesToSkip is 0"() {
        expect:
        provider.calculateHowManyObjectsToSkip(0, 3) == 0
    }

    def "calculateHowManyObjectsToSkip with valuesPerObject=1 is identity"() {
        expect:
        provider.calculateHowManyObjectsToSkip(5, 1) == 5
        provider.calculateHowManyObjectsToSkip(1, 1) == 1
    }

    @Unroll
    def "calculateHowManyObjectsToSkip(#valuesToSkip, 3) == #expected (floor division)"() {
        expect:
        provider.calculateHowManyObjectsToSkip(valuesToSkip, 3) == expected

        where:
        valuesToSkip | expected
        1            | 0
        2            | 0
        3            | 1
        4            | 1
        5            | 1
        6            | 2
        7            | 2
        9            | 3
    }

    @Unroll
    def "calculateHowManyObjectsToSkip(#valuesToSkip, 5) == #expected"() {
        expect:
        provider.calculateHowManyObjectsToSkip(valuesToSkip, 5) == expected

        where:
        valuesToSkip | expected
        4            | 0
        5            | 1
        9            | 1
        10           | 2
    }

    // -----------------------------------------------------------------------
    // Consistency: skip + get should always cover exactly the requested slice
    // -----------------------------------------------------------------------

    @Unroll
    def "skip+get combination is consistent (correct cases): skip=#skip, get=#get, valuesPerObject=#vpo"() {
        expect:
        def objectsToSkip = provider.calculateHowManyObjectsToSkip(skip, vpo)
        def objectsToGet  = provider.calculateHowManyObjectsToGet(get, vpo)
        def intraObjectOffset = skip % vpo
        (objectsToGet * vpo - intraObjectOffset) >= get

        where:
        skip | get | vpo
        0    | 1   | 3
        0    | 3   | 3
        0    | 4   | 3
        2    | 1   | 3
        3    | 3   | 3
        5    | 4   | 3
        0    | 5   | 5
        4    | 1   | 5
        5    | 5   | 5
        7    | 3   | 5
    }

    // -----------------------------------------------------------------------
    // searchForLocalisedValues — pagination correctness
    //
    // These tests use a controlled stub where searchForLocalisedObjects supplies
    // mock items from an infinite pool, returning exactly the requested count.
    // This lets us verify both WHAT was asked of the object store and WHAT values
    // came back — isolating the pagination math from DB behaviour.
    // -----------------------------------------------------------------------

    /**
     * Builds a provider that owns {@code totalValues / vpo} objects, each yielding
     * {@code vpo} values labelled "value-{objectIndex}-{valueIndex}".
     * {@code searchForLocalisedObjects} returns exactly {@code count} items
     * (or fewer if {@code start + count > totalObjects}), mimicking DB behaviour.
     *
     * The returned list of values returned by each call to searchForLocalisedObjects
     * is recorded in {@code fetchedObjectCounts}.
     */
    private CxAbstractItemBasedLocalizedObjectProvider buildProvider(
            int totalObjects,
            int vpo,
            List<Integer> fetchedObjectCounts) {

        return new CxAbstractItemBasedLocalizedObjectProvider() {

            @Override
            List<ItemWithIds> searchForLocalisedObjects(Collection<String> objectTypes, int start, int count) {
                fetchedObjectCounts.add(count)
                // return [start .. min(start+count, totalObjects)]
                int end = Math.min(start + count, totalObjects)
                return (start..<end).collect { idx ->
                    new ItemWithIds(null, "ObjType.obj-$idx", "obj-$idx")
                }
            }

            @Override
            Stream<CxLocalizedValueModel> extractValues(ItemWithIds itemWithId) {
                // extract exactly vpo values per object, labelled by objectId + valueIndex
                def objId = itemWithId.objectId()
                (0..<vpo).collect { valueIdx ->
                    def key = "${objId}.attr${valueIdx}"
                    new CxLocalizedValueModel() {
                        @Override
                        String getKey() { return key }
                    }
                }.stream()
            }

            @Override
            int howManyValuesPerObjectForObjectType(String objectType) { vpo }

            @Override
            Map<String, CxLocalizedValueResultStatistics> getStatistics() {
                ["ObjType": new CxLocalizedValueResultStatistics(totalObjects, totalObjects * vpo)]
            }

            @Override List<String> allSupportedObjectTypes() { ["ObjType"] }
            @Override boolean canReturnObjectType(String t) { true }
            @Override CxLocalizedObjectConfigModel getConfig() { null }
        }
    }

    // -----------------------------------------------------------------------
    // Fixed bugs: review issue #2 (pagination math)
    // -----------------------------------------------------------------------

    def "searchForLocalisedValues(skip=0, get=0) returns empty without hitting the store (fix for review #2)"() {
        given: "3 objects x 2 values each = 6 total values"
        def fetchedCounts = []
        def p = buildProvider(3, 2, fetchedCounts)

        when:
        def result = p.searchForLocalisedValues(["ObjType"], 0, 0)

        then: "no values returned"
        result.isEmpty()

        and: "searchForLocalisedObjects was never called — no DB round-trip for an empty page"
        fetchedCounts.isEmpty()
    }

    def "searchForLocalisedValues(skip=4, get=2) on 3-objects×2-attrs requests exactly 1 object from store (fix for review #2)"() {
        given: "3 objects x 2 values each = 6 total values"
        def fetchedCounts = []
        def p = buildProvider(3, 2, fetchedCounts)

        when:
        def result = p.searchForLocalisedValues(["ObjType"], 4, 2)

        then: "correct values returned — the last 2 values"
        result.size() == 2
        result.collect { it.getKey() } == ["obj-2.attr0", "obj-2.attr1"]

        and: "exactly 1 object fetched (3rd object, at DB offset 2)"
        fetchedCounts == [1]
    }

    // -----------------------------------------------------------------------
    // Defensive guards (review issue #1) — empty / null / zero-count inputs
    // -----------------------------------------------------------------------

    def "searchForLocalisedValues returns empty list for empty types list"() {
        given:
        def fetchedCounts = []
        def p = buildProvider(3, 2, fetchedCounts)

        expect:
        p.searchForLocalisedValues([], 0, 5).isEmpty()
        fetchedCounts.isEmpty()
    }

    def "searchForLocalisedValues returns empty list for null types"() {
        given:
        def fetchedCounts = []
        def p = buildProvider(3, 2, fetchedCounts)

        expect:
        p.searchForLocalisedValues(null, 0, 5).isEmpty()
        fetchedCounts.isEmpty()
    }

    def "searchForLocalisedValues returns empty list when requestedValueCount is zero"() {
        given:
        def fetchedCounts = []
        def p = buildProvider(3, 2, fetchedCounts)

        expect:
        p.searchForLocalisedValues(["ObjType"], 0, 0).isEmpty()
        fetchedCounts.isEmpty()
    }

    def "searchForLocalisedValues returns empty list when requestedValueCount is negative"() {
        given:
        def fetchedCounts = []
        def p = buildProvider(3, 2, fetchedCounts)

        expect:
        p.searchForLocalisedValues(["ObjType"], 0, -1).isEmpty()
        fetchedCounts.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Multi-type-per-provider — two types in one requestedObjectTypes list
    // -----------------------------------------------------------------------

    def "searchForLocalisedValues with two types returns values from both, capping at requestedValueCount"() {
        given: "TypeA has 2 objects×2 values=4, TypeB has 1 object×2 values=2; request all 6"
        def fetchedCounts = []
        def p = buildTwoTypeProvider(fetchedCounts)

        when:
        def result = p.searchForLocalisedValues(["TypeA", "TypeB"], 0, 6)

        then:
        result.size() == 6
        result.collect { it.getKey() } == [
            "obj-0.attr0", "obj-0.attr1",
            "obj-1.attr0", "obj-1.attr1",
            "obj-2.attr0", "obj-2.attr1"
        ]
    }

    def "searchForLocalisedValues with two types respects skip across type boundary"() {
        given: "TypeA has 4 values (objects 0 and 1), TypeB has 2 values (object 2); skip 3, get 3"
        def fetchedCounts = []
        def p = buildTwoTypeProvider(fetchedCounts)

        when:
        def result = p.searchForLocalisedValues(["TypeA", "TypeB"], 3, 3)

        then: "skip 3 means skip obj-0.attr0,attr1 and obj-1.attr0; get obj-1.attr1, obj-2.attr0, obj-2.attr1"
        result.size() == 3
        result.collect { it.getKey() }.containsAll(["obj-1.attr1", "obj-2.attr0", "obj-2.attr1"])
    }

    /**
     * Two-type provider helper: TypeA has 2 objects (idx 0,1) × 2 values = 4 total;
     * TypeB has 1 object (idx 2) × 2 values = 2 total.
     */
    private CxAbstractItemBasedLocalizedObjectProvider buildTwoTypeProvider(List<Integer> fetchedObjectCounts) {
        return new CxAbstractItemBasedLocalizedObjectProvider() {
            @Override
            List<ItemWithIds> searchForLocalisedObjects(Collection<String> objectTypes, int start, int count) {
                fetchedObjectCounts.add(count)
                int total = 3  // 2 TypeA objects + 1 TypeB object
                int end = Math.min(start + count, total)
                return (start..<end).collect { idx ->
                    def type = idx < 2 ? "TypeA" : "TypeB"
                    new ItemWithIds(null, "${type}.obj-$idx", "obj-$idx")
                }
            }

            @Override
            Stream<CxLocalizedValueModel> extractValues(ItemWithIds item) {
                def objId = item.objectId()
                (0..<2).collect { vi ->
                    def key = "${objId}.attr${vi}"
                    new CxLocalizedValueModel() {
                        @Override String getKey() { return key }
                    }
                }.stream()
            }

            @Override int howManyValuesPerObjectForObjectType(String t) { 2 }

            @Override
            Map<String, CxLocalizedValueResultStatistics> getStatistics() {
                ["TypeA": new CxLocalizedValueResultStatistics(2, 4),
                 "TypeB": new CxLocalizedValueResultStatistics(1, 2)]
            }

            @Override List<String> allSupportedObjectTypes() { ["TypeA", "TypeB"] }
            @Override boolean canReturnObjectType(String t) { true }
            @Override CxLocalizedObjectConfigModel getConfig() { null }
        }
    }
}
