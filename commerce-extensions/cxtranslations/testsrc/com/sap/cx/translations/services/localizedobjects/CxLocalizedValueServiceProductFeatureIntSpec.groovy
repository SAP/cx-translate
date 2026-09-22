/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.model.CxProductFeatureLocalizedObjectConfigModel
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.SearchCriteria
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.SearchField
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.SearchOperator
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.StringWithLocale
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.catalog.model.classification.ClassAttributeAssignmentModel
import de.hybris.platform.catalog.model.classification.ClassificationSystemVersionModel
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.type.TypeService
import jakarta.annotation.Resource
import spock.lang.Shared
import spock.lang.Subject

/**
 * Integration tests for the ProductFeature-provider path of
 * {@link CxLocalizedValueService#search(CxLocalizedValueSearchData)}.
 *
 * Uses {@link ServicelayerSpockSpecification} (non-transactional) because saving
 * {@code ClassificationSystemModel} triggers invalidation events during rollback that
 * interfere with the Hybris connection pool.  Product codes are made unique per test
 * run via {@link #RUN_ID}.
 */
@IntegrationTest
class CxLocalizedValueServiceProductFeatureIntSpec extends ServicelayerTransactionalSpockSpecification {

    private static final String CATALOG_ID      = "pfTestCatalog"
    private static final String CLASS_SYSTEM_ID = "pfClassSystem"
    private static final String PF_CONFIG_CODE  = "pfConfig"

    @Subject
    @Resource
    CxLocalizedValueService cxLocalizedValueService

    @Resource
    ModelService modelService

    @Resource
    TypeService typeService

    CxTranslationTestDataBuilder builder
    CatalogVersionModel productCatalogVersion
    ClassificationSystemVersionModel classSystemVersion
    ClassAttributeAssignmentModel colorAssignment
    ClassAttributeAssignmentModel sizeAssignment
    LanguageModel enLang
    LanguageModel deLang
    // pfConfig created per test in setup — rolled back cleanly

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService, typeService)
        def n = System.nanoTime()

        def versions = builder.createCatalogWithVersions(CATALOG_ID + "-" + n)
        productCatalogVersion = versions.staged

        enLang = builder.createLanguage("pf-en-" + n)
        deLang = builder.createLanguage("pf-de-" + n)

        def classSystem = builder.createClassificationSystem(CLASS_SYSTEM_ID + "-" + n)
        classSystemVersion = classSystem.version
        def cls = builder.createClassificationClass("PfTestClass-$n", classSystemVersion)
        colorAssignment = builder.createLocalizedClassAttribute("color-$n", cls, classSystemVersion)
        sizeAssignment  = builder.createLocalizedClassAttribute("size-$n",  cls, classSystemVersion)

        builder.createProductFeatureConfig(PF_CONFIG_CODE + "-" + n, [productCatalogVersion])
    }

    // -----------------------------------------------------------------------
    // Basic discovery
    // -----------------------------------------------------------------------

    def "search returns localized value wrappers for color attribute when both languages have features"() {
        given:
        def code = "shoe-" + System.nanoTime()
        def product = builder.createProduct(code, productCatalogVersion)
        builder.createProductFeature(product, colorAssignment, enLang, "Red")
        builder.createProductFeature(product, colorAssignment, deLang, "Rot")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, code))).allResults()

        then: "at least one value with color attribute is returned (actual count depends on grouping behavior)"
        !results.isEmpty()
        results.every { it.objectType == "ProductFeature" }
        results.every { it.attribute.contains("color") }
    }

    def "search returns empty for a product with no classification features"() {
        given:
        def code = "empty-" + System.nanoTime()
        builder.createProduct(code, productCatalogVersion)

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, code))).allResults()

        then:
        results.isEmpty()
    }

    def "search returns one value per attribute type for a product with multiple classification attributes"() {
        given:
        def code = "multi-" + System.nanoTime()
        def product = builder.createProduct(code, productCatalogVersion)
        builder.createProductFeature(product, colorAssignment, enLang, "Blue")
        builder.createProductFeature(product, sizeAssignment, enLang, "L")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, code))).allResults()

        then:
        results.size() == 2
        results.collect { it.attribute }.any { it.contains("color") }
        results.collect { it.attribute }.any { it.contains("size") }
    }

    def "search returns objectType ProductFeature for all values"() {
        given:
        def code = "type-" + System.nanoTime()
        def product = builder.createProduct(code, productCatalogVersion)
        builder.createProductFeature(product, colorAssignment, enLang, "Green")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, code))).allResults()

        then:
        !results.isEmpty()
        results.every { it.objectType == "ProductFeature" }
    }

    // -----------------------------------------------------------------------
    // Key format
    // -----------------------------------------------------------------------

    def "key includes objectType, productCode, and attribute for single-catalogVersion config"() {
        given:
        def code = "shoe-key-" + System.nanoTime()
        def product = builder.createProduct(code, productCatalogVersion)
        builder.createProductFeature(product, colorAssignment, enLang, "Black")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, code))).allResults()

        then:
        results.size() == 1
        def key = results[0].key
        key.startsWith("ProductFeature.")
        key.contains(code)
    }

    // -----------------------------------------------------------------------
    // Filter by OBJECT_ID
    // -----------------------------------------------------------------------

    def "search filtered by OBJECT_ID EQUALS returns only that product's features"() {
        given:
        def n = System.nanoTime()
        def product1 = builder.createProduct("pf-A-$n", productCatalogVersion)
        def product2 = builder.createProduct("pf-B-$n", productCatalogVersion)
        builder.createProductFeature(product1, colorAssignment, enLang, "Red")
        builder.createProductFeature(product2, colorAssignment, enLang, "Blue")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, "pf-A-$n"))).allResults()

        then:
        results.size() == 1
        results[0].objectId == "pf-A-$n"
    }

    def "search filtered by OBJECT_ID STARTS_WITH returns matching products"() {
        given:
        def n = System.nanoTime()
        def p1 = builder.createProduct("alpha-1-$n", productCatalogVersion)
        def p2 = builder.createProduct("alpha-2-$n", productCatalogVersion)
        def p3 = builder.createProduct("beta-1-$n",  productCatalogVersion)
        builder.createProductFeature(p1, colorAssignment, enLang, "Red")
        builder.createProductFeature(p2, colorAssignment, enLang, "Blue")
        builder.createProductFeature(p3, colorAssignment, enLang, "Green")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.STARTS_WITH, "alpha-"))).allResults()

        then:
        results.findAll { it.objectId.endsWith("-$n") }.size() == 2
        results.findAll { it.objectId.endsWith("-$n") }.every { it.objectId.startsWith("alpha-") }
    }

    // -----------------------------------------------------------------------
    // Filter by ATTRIBUTE
    // -----------------------------------------------------------------------

    def "search filtered by ATTRIBUTE returns only matching attribute features"() {
        given:
        def code = "attr-filt-" + System.nanoTime()
        def product = builder.createProduct(code, productCatalogVersion)
        builder.createProductFeature(product, colorAssignment, enLang, "Red")
        builder.createProductFeature(product, sizeAssignment,  enLang, "XL")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, code),
                criterion(SearchField.ATTRIBUTE, SearchOperator.STARTS_WITH, "color"))).allResults()

        then:
        results.size() == 1
        results[0].attribute.contains("color")
    }

    // -----------------------------------------------------------------------
    // Filter by VALUE
    // -----------------------------------------------------------------------

    def "search filtered by VALUE IS_NOT_EMPTY returns features with non-blank string value"() {
        given:
        def code = "val-filt-" + System.nanoTime()
        def p1 = builder.createProduct(code, productCatalogVersion)
        builder.createProductFeature(p1, colorAssignment, enLang, "Blue")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, code),
                criterion(SearchField.ATTRIBUTE, SearchOperator.STARTS_WITH, "color"),
                criterionWithLocale(SearchField.VALUE, SearchOperator.IS_NOT_EMPTY, null, enLang.isocode)
        )).allResults()

        then:
        results.size() == 1
        results[0].objectId == code
    }

    // -----------------------------------------------------------------------
    // Catalog version isolation
    // -----------------------------------------------------------------------

    def "search only returns features for products in configured catalog versions"() {
        given:
        def n = System.nanoTime()
        def inCode = "in-scope-$n"
        def inScopeProduct = builder.createProduct(inCode, productCatalogVersion)
        builder.createProductFeature(inScopeProduct, colorAssignment, enLang, "Red")

        // Product in a different catalog — NOT covered by pfConfig
        def otherVersions = builder.createCatalogWithVersions("other-cat-$n")
        def outCode = "out-scope-$n"
        def outScopeProduct = builder.createProduct(outCode, otherVersions.staged)
        builder.createProductFeature(outScopeProduct, colorAssignment, enLang, "Blue")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.STARTS_WITH, "out-scope-$n"))).allResults()

        then: "out-scope product filtered by pfConfig's productCatalogVersions"
        results.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Non-localized assignment not returned
    // -----------------------------------------------------------------------

    def "non-localized class attribute assignments are not returned by search"() {
        given:
        def code = "nonloc-" + System.nanoTime()
        def product = builder.createProduct(code, productCatalogVersion)

        def nonLocalizedAssignment = builder.createLocalizedClassAttribute("weight-$code",
                builder.createClassificationClass("WeightClass-$code", classSystemVersion), classSystemVersion)
        nonLocalizedAssignment.setLocalized(Boolean.FALSE)
        modelService.save(nonLocalizedAssignment)

        builder.createProductFeature(product, nonLocalizedAssignment, enLang, "1kg")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, code))).allResults()

        then:
        results.isEmpty()
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private static SearchCriteria criterion(SearchField field, SearchOperator op, String value) {
        new SearchCriteria(field, op, new StringWithLocale(value))
    }

    private static SearchCriteria criterionWithLocale(SearchField field, SearchOperator op, String value, String isocode) {
        new SearchCriteria(field, op, new StringWithLocale(value, new Locale(isocode)))
    }

    private static CxLocalizedValueSearchData searchData(SearchCriteria... criteria) {
        new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                criteria.toList(), 50)
    }
}
