/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.cache

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.regioncache.key.CacheUnitValueType
import spock.lang.Specification
import spock.lang.Subject

/**
 * Unit tests for {@link CxLocalizedBundleCacheKey}
 */
@UnitTest
class CxLocalizedBundleCacheKeySpec extends Specification {

    static final String KEY = "myBundle"
    static final String TENANT = "junit"

    @Subject
    CxLocalizedBundleCacheKey cacheKey = new CxLocalizedBundleCacheKey(KEY, TENANT)

    // ---------- constructor null guards ----------

    def "constructor throws NullPointerException with message when key is null"() {
        when:
        new CxLocalizedBundleCacheKey(null, TENANT)

        then:
        def ex = thrown(NullPointerException)
        ex.message == "key must not be null"
    }

    def "constructor throws NullPointerException with message when tenantId is null"() {
        when:
        new CxLocalizedBundleCacheKey(KEY, null)

        then:
        def ex = thrown(NullPointerException)
        ex.message == "tenantId must not be null"
    }

    // ---------- getTypeCode ----------

    def "getTypeCode returns the bundle type code string"() {
        expect:
        cacheKey.getTypeCode() == "__LOCALIZED_BUNDLE__"
    }

    def "getTypeCode does NOT return the label type code string"() {
        expect:
        cacheKey.getTypeCode() != "__LOCALIZED_LABEL__"
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
        def other = new CxLocalizedBundleCacheKey(KEY, TENANT)

        expect:
        cacheKey == other
    }

    def "equals is false when key differs"() {
        given:
        def other = new CxLocalizedBundleCacheKey("differentKey", TENANT)

        expect:
        cacheKey != other
    }

    def "equals is false when tenantId differs"() {
        given:
        def other = new CxLocalizedBundleCacheKey(KEY, "differentTenant")

        expect:
        cacheKey != other
    }

    def "equals is false when compared to null"() {
        expect:
        !cacheKey.equals(null)
    }

    def "equals is false when compared to CxLocalizedLabelCacheKey with same key and tenantId"() {
        given:
        def labelKey = new CxLocalizedLabelCacheKey(KEY, TENANT)

        expect:
        !cacheKey.equals(labelKey)
    }

    // ---------- hashCode ----------

    def "hashCode is consistent with equals — two equal keys have the same hash code"() {
        given:
        def other = new CxLocalizedBundleCacheKey(KEY, TENANT)

        expect:
        cacheKey.hashCode() == other.hashCode()
    }

    def "hashCode differs for keys with different key strings"() {
        given:
        def other = new CxLocalizedBundleCacheKey("differentKey", TENANT)

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


