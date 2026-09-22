/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.services.impl.localizedobjects.CxProductFeatureKey
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.catalog.model.ProductFeatureModel
import spock.lang.Specification

/**
 * Unit tests for {@link CxProductFeatureKey} — the value object that centralises build/parse of
 * Product Feature localized-value keys. Replaces the magic-{@code whichDot} parser previously
 * embedded in {@code CxProductFeatureLocalizedObjectProvider} (review finding #4).
 */
@UnitTest
class CxProductFeatureKeySpec extends Specification {

    def "OBJECT_TYPE constant matches the platform type code"() {
        expect:
        CxProductFeatureKey.OBJECT_TYPE == ProductFeatureModel._TYPECODE
    }

    // -----------------------------------------------------------------------
    // build (simple form)
    // -----------------------------------------------------------------------

    def "simple builds <type>.<product>.<attribute>"() {
        when:
        def key = CxProductFeatureKey.simple("SKU-1", "color")

        then:
        key.toRawString() == "ProductFeature.SKU-1.color"
        key.objectType() == "ProductFeature"
        key.productCode() == "SKU-1"
        key.attribute() == "color"
        !key.isCatalogAware()
        key.catalogId() == null
        key.catalogVersion() == null
    }

    def "simple rejects null arguments"() {
        when:
        CxProductFeatureKey.simple(productCode, attribute)

        then:
        thrown(NullPointerException)

        where:
        productCode | attribute
        null        | "color"
        "SKU"       | null
    }

    // -----------------------------------------------------------------------
    // build (catalog-aware form)
    // -----------------------------------------------------------------------

    def "catalogAware builds <type>.<catalog>.<version>.<product>.<attribute>"() {
        when:
        def key = CxProductFeatureKey.catalogAware("electronics", "Online", "SKU-1", "color")

        then:
        key.toRawString() == "ProductFeature.electronics.Online.SKU-1.color"
        key.isCatalogAware()
        key.catalogId() == "electronics"
        key.catalogVersion() == "Online"
        key.productCode() == "SKU-1"
        key.attribute() == "color"
    }

    def "catalogAware rejects null arguments"() {
        when:
        CxProductFeatureKey.catalogAware(catalogId, version, productCode, attribute)

        then:
        thrown(NullPointerException)

        where:
        catalogId | version  | productCode | attribute
        null      | "Online" | "SKU"       | "color"
        "cat"     | null     | "SKU"       | "color"
        "cat"     | "Online" | null        | "color"
        "cat"     | "Online" | "SKU"       | null
    }

    // -----------------------------------------------------------------------
    // parse (simple form)
    // -----------------------------------------------------------------------

    def "tryParse simple parses round-trip"() {
        given:
        def key = CxProductFeatureKey.simple("SKU-1", "color")

        when:
        def parsed = CxProductFeatureKey.tryParse(key.toRawString(), false)

        then:
        parsed.isPresent()
        parsed.get() == key
    }

    def "tryParse simple returns empty when input has wrong dot count"() {
        expect:
        !CxProductFeatureKey.tryParse(input, false).isPresent()

        where:
        input                                                  | _
        "ProductFeature.SKU"                                   | _   // 1 dot - too few
        "ProductFeature.cat.Online.SKU.color"                  | _   // 4 dots - catalog-aware shape
        ""                                                     | _
        "no-dots-at-all"                                       | _
    }

    def "tryParse simple returns empty when objectType prefix is wrong"() {
        expect:
        !CxProductFeatureKey.tryParse("Product.SKU.color", false).isPresent()
    }

    def "tryParse simple handles null and blank gracefully"() {
        expect:
        !CxProductFeatureKey.tryParse(null, false).isPresent()
        !CxProductFeatureKey.tryParse("   ", false).isPresent()
    }

    // -----------------------------------------------------------------------
    // parse (catalog-aware form)
    // -----------------------------------------------------------------------

    def "tryParse catalogAware parses round-trip"() {
        given:
        def key = CxProductFeatureKey.catalogAware("electronics", "Online", "SKU-1", "color")

        when:
        def parsed = CxProductFeatureKey.tryParse(key.toRawString(), true)

        then:
        parsed.isPresent()
        parsed.get() == key
    }

    def "tryParse catalogAware returns empty when shape is wrong"() {
        expect:
        !CxProductFeatureKey.tryParse(input, true).isPresent()

        where:
        input                                          | _
        "ProductFeature.SKU.color"                     | _   // 2 dots - simple shape
        "ProductFeature.cat.Online.SKU.color.extra"    | _   // 5 dots - too many
        "Wrong.cat.Online.SKU.color"                   | _   // wrong prefix
    }

    // -----------------------------------------------------------------------
    // extractProductCode — the headline use case
    // -----------------------------------------------------------------------

    def "extractProductCode returns product code from simple key"() {
        expect:
        CxProductFeatureKey.extractProductCode("ProductFeature.SKU-1.color", false).orElse(null) == "SKU-1"
    }

    def "extractProductCode returns product code from catalog-aware key"() {
        expect:
        CxProductFeatureKey.extractProductCode("ProductFeature.electronics.Online.SKU-1.color", true).orElse(null) == "SKU-1"
    }

    def "extractProductCode returns empty when the shape does not match the catalog-awareness flag"() {
        // Previously the extract function returned null silently and the caller used null as a
        // 'no filter' marker. Returning Optional.empty() keeps the data invariant explicit.
        expect:
        !CxProductFeatureKey.extractProductCode("ProductFeature.SKU-1.color", true).isPresent()
        !CxProductFeatureKey.extractProductCode("ProductFeature.electronics.Online.SKU-1.color", false).isPresent()
    }

    def "extractProductCode returns empty for malformed input"() {
        expect:
        !CxProductFeatureKey.extractProductCode("garbage", false).isPresent()
        !CxProductFeatureKey.extractProductCode("garbage", true).isPresent()
        !CxProductFeatureKey.extractProductCode(null, false).isPresent()
    }

    // -----------------------------------------------------------------------
    // equality / hash / toString
    // -----------------------------------------------------------------------

    def "equals and hashCode agree on equal simple keys"() {
        given:
        def a = CxProductFeatureKey.simple("SKU", "color")
        def b = CxProductFeatureKey.simple("SKU", "color")

        expect:
        a == b
        a.hashCode() == b.hashCode()
    }

    def "equals distinguishes catalog-aware from simple keys with the same product/attribute"() {
        given:
        def simple = CxProductFeatureKey.simple("SKU", "color")
        def catAware = CxProductFeatureKey.catalogAware("cat", "Online", "SKU", "color")

        expect:
        simple != catAware
    }

    def "toString matches toRawString"() {
        given:
        def key = CxProductFeatureKey.catalogAware("electronics", "Online", "SKU-1", "color")

        expect:
        key.toString() == key.toRawString()
    }
}
