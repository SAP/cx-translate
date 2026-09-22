/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.cache

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.regioncache.key.CacheUnitValueType
import spock.lang.Specification
import spock.lang.Subject

/**
 * Unit tests for {@link CxLocalizedLabelCacheKey}
 */
@UnitTest
class CxLocalizedLabelCacheKeySpec extends Specification {

    static final String KEY = "myLabel"
    static final String TENANT = "junit"

    @Subject
    CxLocalizedLabelCacheKey cacheKey = new CxLocalizedLabelCacheKey(KEY, TENANT)

    // ---------- constructor null guards ----------

    def "constructor throws NullPointerException with message when key is null"() {
        when:
        new CxLocalizedLabelCacheKey(null, TENANT)

        then:
        def ex = thrown(NullPointerException)
        ex.message == "key must not be null"
    }

    def "constructor throws NullPointerException with message when tenantId is null"() {
        when:
        new CxLocalizedLabelCacheKey(KEY, null)

        then:
        def ex = thrown(NullPointerException)
        ex.message == "tenantId must not be null"
    }

    // ---------- getTypeCode ----------

    def "getTypeCode returns the label type code string"() {
        expect:
        cacheKey.getTypeCode() == "__LOCALIZED_LABEL__"
    }

    def "getTypeCode does NOT return the bundle type code string"() {
        expect:
        cacheKey.getTypeCode() != "__LOCALIZED_BUNDLE__"
    }

    // ---------- getCacheValueType ----------

    def "getCacheValueType returns SERIALIZABLE"() {
        expect:
        cacheKey.getCacheValueType() == CacheUnitValueType.SERIALIZABLE
    }

    // ---------- getTenantId ----------

    def "getTenantId returns the tenantId passed to the constructor"() {
        expect:
        cacheKey.getTenantId() == TENANT
    }

    // ---------- equals ----------

    def "equals is reflexive"() {
        expect:
        cacheKey == cacheKey
    }

    def "equals is true for two keys with the same key and tenantId"() {
        given:
        def other = new CxLocalizedLabelCacheKey(KEY, TENANT)

        expect:
        cacheKey == other
    }

    def "equals is false when key differs"() {
        given:
        def other = new CxLocalizedLabelCacheKey("differentKey", TENANT)

        expect:
        cacheKey != other
    }

    def "equals is false when tenantId differs"() {
        given:
        def other = new CxLocalizedLabelCacheKey(KEY, "differentTenant")

        expect:
        cacheKey != other
    }

    def "equals is false when compared to null"() {
        expect:
        !cacheKey.equals(null)
    }

    def "equals is false when compared to CxLocalizedBundleCacheKey with same key and tenantId"() {
        given:
        def bundleKey = new CxLocalizedBundleCacheKey(KEY, TENANT)

        expect:
        !cacheKey.equals(bundleKey)
    }

    // ---------- hashCode ----------

    def "hashCode is consistent with equals — two equal keys have the same hash code"() {
        given:
        def other = new CxLocalizedLabelCacheKey(KEY, TENANT)

        expect:
        cacheKey.hashCode() == other.hashCode()
    }

    def "hashCode differs for keys with different key strings"() {
        given:
        def other = new CxLocalizedLabelCacheKey("differentKey", TENANT)

        expect:
        cacheKey.hashCode() != other.hashCode()
    }

    // ---------- toString ----------

    def "toString contains both key and tenantId values"() {
        expect:
        cacheKey.toString().contains(KEY)
        cacheKey.toString().contains(TENANT)
    }
}


