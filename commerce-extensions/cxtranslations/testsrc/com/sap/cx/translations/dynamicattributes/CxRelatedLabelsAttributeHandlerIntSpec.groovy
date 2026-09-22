/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.dynamicattributes

import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import jakarta.annotation.Resource

/**
 * Integration tests for {@link CxRelatedLabelsAttributeHandler} — the dynamic attribute
 * behind {@code CxLocalizedLabelModel.relatedLabels}.
 *
 * <p>The handler returns plural-form variants of a base label key (and vice versa), using
 * the i18next suffix list configured in {@code cxtranslations.i18next.suffixes}.  The
 * default suffixes from {@code project.properties} are:
 * {@code _0, _1, _2, _3, _plural, _one, _other, _few, _many}.</p>
 */
@IntegrationTest
class CxRelatedLabelsAttributeHandlerIntSpec extends ServicelayerTransactionalSpockSpecification {

    private static final String CATALOG_ID = "relatedLabelsTestCatalog"

    @Resource
    ModelService modelService

    private CxTranslationTestDataBuilder builder
    private CatalogVersionModel stagedVersion
    private CatalogVersionModel onlineVersion

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService)
        def versions = builder.createCatalogWithVersions(CATALOG_ID)
        stagedVersion = versions.staged
        onlineVersion = versions.online
    }

    // -----------------------------------------------------------------------
    // relatedLabels — base key → plural variants
    // -----------------------------------------------------------------------

    def "getRelatedLabels for a base key returns all existing plural-suffix variants"() {
        given: "a base label 'item.count' and two plural variants"
        def bundle = builder.createBundle("bundle-related-1", stagedVersion)
        def baseLabel = builder.createFrontendLabel("item.count", bundle, stagedVersion)
        builder.createFrontendLabel("item.count_one", bundle, stagedVersion)
        builder.createFrontendLabel("item.count_other", bundle, stagedVersion)

        when:
        def result = baseLabel.getRelatedLabels()

        then:
        result.collect { it.key }.containsAll(["item.count_one", "item.count_other"])
        !result.any { it.key == "item.count" }
    }

    def "getRelatedLabels for a base key returns empty when no plural variants exist"() {
        given: "only the base label, no variants"
        def bundle = builder.createBundle("bundle-related-2", stagedVersion)
        def baseLabel = builder.createFrontendLabel("standalone.key", bundle, stagedVersion)

        when:
        def result = baseLabel.getRelatedLabels()

        then:
        result.isEmpty()
    }

    def "getRelatedLabels for a base key does not return variants from a different catalog version"() {
        given: "base in staged, variant in online"
        def bundleStaged = builder.createBundle("bundle-related-cv-staged", stagedVersion)
        def bundleOnline = builder.createBundle("bundle-related-cv-online", onlineVersion)
        def baseLabel = builder.createFrontendLabel("cv.key", bundleStaged, stagedVersion)
        builder.createFrontendLabel("cv.key_plural", bundleOnline, onlineVersion)

        when:
        def result = baseLabel.getRelatedLabels()

        then:
        result.isEmpty()
    }

    // -----------------------------------------------------------------------
    // relatedLabels — plural variant key → base + other variants
    // -----------------------------------------------------------------------

    def "getRelatedLabels for a plural-suffix label returns the base label and other variants"() {
        given:
        def bundle = builder.createBundle("bundle-related-plural", stagedVersion)
        def baseLabel = builder.createFrontendLabel("cart.items", bundle, stagedVersion)
        def pluralOne = builder.createFrontendLabel("cart.items_one", bundle, stagedVersion)
        builder.createFrontendLabel("cart.items_other", bundle, stagedVersion)

        when: "querying from the _one variant"
        def result = pluralOne.getRelatedLabels()

        then: "base and _other variant are returned but not _one itself"
        result.collect { it.key }.containsAll(["cart.items", "cart.items_other"])
        !result.any { it.key == "cart.items_one" }
    }

    def "getRelatedLabels for a plural-suffix label strips only the trailing suffix, not mid-key occurrences"() {
        given: "key containing the suffix string mid-key: 'product_plural_category' with suffix '_plural'"
        def bundle = builder.createBundle("bundle-related-midkey", stagedVersion)
        // This key ends with '_plural' — so base key should be 'product_plural_category', not 'product_category'
        def pluralLabel = builder.createFrontendLabel("product_plural_category_plural", bundle, stagedVersion)
        def baseLabel = builder.createFrontendLabel("product_plural_category", bundle, stagedVersion)

        when:
        def result = pluralLabel.getRelatedLabels()

        then: "base key is derived by removing only the trailing _plural"
        result.any { it.key == "product_plural_category" }
    }

    // -----------------------------------------------------------------------
    // relatedLabels — usage type filtering
    // -----------------------------------------------------------------------

    def "getRelatedLabels respects frontend usage type — does not return backend-only variants"() {
        given:
        def bundle = builder.createBundle("bundle-related-usage", stagedVersion)
        def frontendBase = builder.createFrontendLabel("usage.key", bundle, stagedVersion)
        builder.createBackendLabel("usage.key_plural", bundle, stagedVersion)

        when:
        def result = frontendBase.getRelatedLabels()

        then: "backend-only variant is not returned for a frontend label"
        result.isEmpty()
    }

    def "getRelatedLabels respects backend usage type — does not return frontend-only variants"() {
        given:
        def bundle = builder.createBundle("bundle-related-usage-be", stagedVersion)
        def backendBase = builder.createBackendLabel("be.usage.key", bundle, stagedVersion)
        builder.createFrontendLabel("be.usage.key_plural", bundle, stagedVersion)

        when:
        def result = backendBase.getRelatedLabels()

        then:
        result.isEmpty()
    }

    def "relatedLabels handler set throws UnsupportedOperationException"() {
        given:
        def handler = applicationContext.getBean("cxRelatedLabelsAttributeHandler") as CxRelatedLabelsAttributeHandler
        def bundle = builder.createBundle("bundle-related-readonly", stagedVersion)
        def label = builder.createFrontendLabel("readonly.key", bundle, stagedVersion)

        when:
        handler.set(label, [])

        then:
        thrown(UnsupportedOperationException)
    }
}
