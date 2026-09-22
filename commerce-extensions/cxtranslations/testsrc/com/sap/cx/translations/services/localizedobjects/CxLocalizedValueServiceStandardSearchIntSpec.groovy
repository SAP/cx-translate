/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.enums.CxTranslateBackofficeSearchInclusion
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.SearchCriteria
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.SearchField
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.SearchOperator
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.StringWithLocale
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.core.model.product.ProductModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.type.TypeService
import jakarta.annotation.Resource
import spock.lang.Subject

/**
 * Integration tests for the standard-provider path of
 * {@link CxLocalizedValueService#search(CxLocalizedValueSearchData)}.
 *
 * Each test creates a minimal {@link CxStandardLocalizedObjectConfigModel} pointing at
 * {@code Product}, inserts product data, and verifies that the search returns the correct
 * localized values — applying filters by attribute, object type, object id, value, key,
 * catalog, etc.
 *
 * <p>All data is rolled back after each test via the transactional spec base.</p>
 */
@IntegrationTest
class CxLocalizedValueServiceStandardSearchIntSpec extends ServicelayerTransactionalSpockSpecification {

    private static final String CATALOG_ID = "searchTestCatalog"
    private static final String CONFIG_CODE = "testProductConfig"
    private static final Locale EN = Locale.ENGLISH
    private static final Locale DE = Locale.GERMAN

    @Subject
    @Resource
    CxLocalizedValueService cxLocalizedValueService

    @Resource
    ModelService modelService

    @Resource
    TypeService typeService

    private CxTranslationTestDataBuilder builder
    private CatalogVersionModel stagedVersion
    private CxStandardLocalizedObjectConfigModel config

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService, typeService)
        def versions = builder.createCatalogWithVersions(CATALOG_ID)
        stagedVersion = versions.staged

        // Standard config: Product, catalog-aware, key field = "code", attributes = name + description
        config = builder.createStandardConfig(
                CONFIG_CODE, "Product", ["code"],
                ["name", "description"],
                true, "catalogVersion",
                [stagedVersion])
    }

    // -----------------------------------------------------------------------
    // Basic search — returns localized values for configured attributes
    // -----------------------------------------------------------------------

    def "search returns one value per localized attribute per product"() {
        given:
        def product = builder.createProduct("p-001", stagedVersion)
        builder.setLocalizedAttribute(product, "name", EN, "Test Product")

        when:
        def pageable = cxLocalizedValueService.search(searchData())
        def results = pageable.allResults()

        then: "2 values returned: name + description for the one product"
        results.size() == 2
        results.every { it.objectType == "Product" }
        results.every { it.objectId == "p-001" }
        results.collect { it.attribute }.containsAll(["name", "description"])
    }

    def "search returns values for multiple products"() {
        given:
        builder.createProduct("p-001", stagedVersion)
        builder.createProduct("p-002", stagedVersion)
        builder.createProduct("p-003", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(searchData()).allResults()

        then: "3 products × 2 attributes = 6 values"
        results.size() == 6
    }

    def "search returns empty when no products exist"() {
        when:
        def results = cxLocalizedValueService.search(searchData()).allResults()

        then:
        results.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Filter by ATTRIBUTE
    // -----------------------------------------------------------------------

    def "search filtered by ATTRIBUTE EQUALS 'name' returns only name values"() {
        given:
        builder.createProduct("p-001", stagedVersion)
        builder.createProduct("p-002", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.ATTRIBUTE, SearchOperator.EQUALS, "name"))).allResults()

        then:
        results.size() == 2
        results.every { it.attribute == "name" }
    }

    def "search filtered by ATTRIBUTE STARTS_WITH 'desc' returns only description values"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.ATTRIBUTE, SearchOperator.STARTS_WITH, "desc"))).allResults()

        then:
        results.size() == 1
        results[0].attribute == "description"
    }

    def "search filtered by ATTRIBUTE EQUALS non-existent attribute returns empty"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.ATTRIBUTE, SearchOperator.EQUALS, "nonExistent"))).allResults()

        then:
        results.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Filter by OBJECT_TYPE
    // -----------------------------------------------------------------------

    def "search filtered by OBJECT_TYPE EQUALS 'Product' returns only Product values"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.OBJECT_TYPE, SearchOperator.EQUALS, "Product"))).allResults()

        then:
        results.size() == 2
        results.every { it.objectType == "Product" }
    }

    def "search filtered by OBJECT_TYPE EQUALS 'Category' returns empty when only Product config exists"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.OBJECT_TYPE, SearchOperator.EQUALS, "Category"))).allResults()

        then:
        results.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Filter by OBJECT_ID
    // -----------------------------------------------------------------------

    def "search filtered by OBJECT_ID EQUALS returns values for that specific product"() {
        given:
        builder.createProduct("p-001", stagedVersion)
        builder.createProduct("p-002", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.OBJECT_ID, SearchOperator.EQUALS, "p-001"))).allResults()

        then:
        results.size() == 2  // name + description
        results.every { it.objectId == "p-001" }
    }

    def "search filtered by OBJECT_ID STARTS_WITH returns matching products only"() {
        given:
        builder.createProduct("alpha-001", stagedVersion)
        builder.createProduct("alpha-002", stagedVersion)
        builder.createProduct("beta-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.OBJECT_ID, SearchOperator.STARTS_WITH, "alpha"))).allResults()

        then:
        results.size() == 4  // 2 alpha products × 2 attributes
        results.every { it.objectId.startsWith("alpha") }
    }

    def "search filtered by OBJECT_ID LIKE returns matching products"() {
        given:
        builder.createProduct("prod-abc-001", stagedVersion)
        builder.createProduct("prod-xyz-002", stagedVersion)
        builder.createProduct("item-abc-003", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.OBJECT_ID, SearchOperator.LIKE, "abc"))).allResults()

        then:
        results.size() == 4  // prod-abc + item-abc, each with 2 attributes
        results.every { it.objectId.contains("abc") }
    }

    // -----------------------------------------------------------------------
    // Filter by KEY
    // -----------------------------------------------------------------------

    def "search filtered by KEY EQUALS matches the exact full key"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.KEY, SearchOperator.EQUALS, "Product.p-001.name"))).allResults()

        then:
        results.size() == 1
        results[0].key == "Product.p-001.name"
        results[0].attribute == "name"
    }

    /**
     * KEY only supports EQUALS at the service layer — STARTS_WITH and other operators are
     * intentionally rejected because neither the SQL path nor the in-memory path implement
     * them reliably for KEY (see {@code buildQueryByKey} in
     * {@link com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedObjectStandardProvider}).
     * The Backoffice adapter ({@code CxTranslationsPageableAdapter#buildOnlyEqualsCriteria})
     * enforces this by silently dropping non-EQUALS KEY criteria; the service layer now
     * enforces it at construction time with an exception so that any direct service caller
     * receives explicit feedback instead of silently wrong results.
     */
    def "constructing search data with KEY STARTS_WITH throws IllegalArgumentException"() {
        when:
        searchData(criterion(SearchField.KEY, SearchOperator.STARTS_WITH, "Product.p-001"))

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("KEY")
        ex.message.contains("EQUALS")
    }

    def "constructing search data with KEY LIKE throws IllegalArgumentException"() {
        when:
        searchData(criterion(SearchField.KEY, SearchOperator.LIKE, "Product"))

        then:
        thrown(IllegalArgumentException)
    }

    def "constructing search data with KEY ENDS_WITH throws IllegalArgumentException"() {
        when:
        searchData(criterion(SearchField.KEY, SearchOperator.ENDS_WITH, ".name"))

        then:
        thrown(IllegalArgumentException)
    }

    def "constructing search data with KEY IS_EMPTY throws IllegalArgumentException"() {
        when:
        searchData(criterion(SearchField.KEY, SearchOperator.IS_EMPTY, null))

        then:
        thrown(IllegalArgumentException)
    }

    def "constructing search data with KEY IS_NOT_EMPTY throws IllegalArgumentException"() {
        when:
        searchData(criterion(SearchField.KEY, SearchOperator.IS_NOT_EMPTY, null))

        then:
        thrown(IllegalArgumentException)
    }

    def "constructing search data with KEY EQUALS is accepted (only supported operator)"() {
        when:
        def sd = searchData(criterion(SearchField.KEY, SearchOperator.EQUALS, "Product.p-001.name"))

        then:
        notThrown(IllegalArgumentException)
        sd.criteria().size() == 1
    }

    def "search filtered by KEY EQUALS non-existent key returns empty"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.KEY, SearchOperator.EQUALS, "Product.nonexistent.name"))).allResults()

        then:
        results.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Filter by VALUE (requires brute-force statistics path)
    // -----------------------------------------------------------------------

    def "search filtered by VALUE IS_EMPTY returns products with blank value for that locale"() {
        given:
        def productWithName = builder.createProduct("p-with-name", stagedVersion)
        builder.setLocalizedAttribute(productWithName, "name", EN, "Shoe")

        def productWithoutName = builder.createProduct("p-no-name", stagedVersion)
        // name left empty for p-no-name

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.ATTRIBUTE, SearchOperator.EQUALS, "name"),
                criterionWithLocale(SearchField.VALUE, SearchOperator.IS_EMPTY, null, EN)
        )).allResults()

        then: "only p-no-name returned (its name is blank)"
        results.every { it.objectId == "p-no-name" }
    }

    def "search filtered by VALUE IS_NOT_EMPTY returns products with non-blank value"() {
        given:
        def p1 = builder.createProduct("p-001", stagedVersion)
        builder.setLocalizedAttribute(p1, "name", EN, "Shoe")

        def p2 = builder.createProduct("p-002", stagedVersion)
        // name left empty for p-002

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.ATTRIBUTE, SearchOperator.EQUALS, "name"),
                criterionWithLocale(SearchField.VALUE, SearchOperator.IS_NOT_EMPTY, null, EN)
        )).allResults()

        then:
        results.size() == 1
        results[0].objectId == "p-001"
    }

    def "search filtered by VALUE EQUALS returns product with matching value"() {
        given:
        def p1 = builder.createProduct("shoe-product", stagedVersion)
        builder.setLocalizedAttribute(p1, "name", EN, "Shoe")

        def p2 = builder.createProduct("hat-product", stagedVersion)
        builder.setLocalizedAttribute(p2, "name", EN, "Hat")

        when:
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.ATTRIBUTE, SearchOperator.EQUALS, "name"),
                criterionWithLocale(SearchField.VALUE, SearchOperator.EQUALS, "Shoe", EN)
        )).allResults()

        then:
        results.size() == 1
        results[0].objectId == "shoe-product"
    }

    // -----------------------------------------------------------------------
    // Filter by PK
    // -----------------------------------------------------------------------

    def "search filtered by PK EQUALS returns exactly that product's values"() {
        given:
        def p1 = builder.createProduct("p-001", stagedVersion)
        builder.createProduct("p-002", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.PK, SearchOperator.EQUALS, p1.pk.toString()))).allResults()

        then:
        results.size() == 2
        results.every { it.objectId == "p-001" }
    }

    // -----------------------------------------------------------------------
    // Filter by CATALOG_VERSION_ID / CATALOG_ID
    // -----------------------------------------------------------------------

    def "search filtered by CATALOG_VERSION_ID EQUALS returns values from that catalog version"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.CATALOG_VERSION_ID, SearchOperator.EQUALS, "Staged"))).allResults()

        then:
        results.size() == 2
    }

    def "search filtered by CATALOG_ID EQUALS returns values from that catalog"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.CATALOG_ID, SearchOperator.EQUALS, CATALOG_ID))).allResults()

        then:
        results.size() == 2
    }

    def "search filtered by CATALOG_ID for wrong catalog returns empty"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(
                searchData(criterion(SearchField.CATALOG_ID, SearchOperator.EQUALS, "nonExistentCatalog"))).allResults()

        then:
        results.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Config inclusion / forceAllConfigs
    // -----------------------------------------------------------------------

    def "search with EXCLUDED_ALWAYS config returns no values for that config"() {
        given:
        // create a second config marked EXCLUDED_ALWAYS
        def catCfg = builder.createStandardConfig(
                "excludedCategoryConfig", "Category", ["code"],
                ["name"],
                false, "catalogVersion", [],
                CxTranslateBackofficeSearchInclusion.EXCLUDED_ALWAYS)

        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(searchData()).allResults()

        then: "only product values, the excluded config contributes nothing"
        results.every { it.objectType == "Product" }
    }

    def "search with forceAllConfigs=true includes EXCLUDED_BY_DEFAULT configs"() {
        given:
        // reconfigure the existing config as EXCLUDED_BY_DEFAULT
        config.setUseInBackofficeSearch(CxTranslateBackofficeSearchInclusion.EXCLUDED_BY_DEFAULT)
        modelService.save(config)

        builder.createProduct("p-001", stagedVersion)

        when: "normal search excludes it"
        def withoutForce = cxLocalizedValueService.search(searchData()).allResults()

        and: "forced search includes it"
        def withForce = cxLocalizedValueService.search(new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                [], 50, [] as Set, true)).allResults()

        then:
        withoutForce.isEmpty()
        !withForce.isEmpty()
    }

    // -----------------------------------------------------------------------
    // getForItem
    // -----------------------------------------------------------------------

    def "getForItem returns all localized values for the given item"() {
        given:
        def product = builder.createProduct("p-001", stagedVersion)
        builder.setLocalizedAttribute(product, "name", EN, "Shoe")

        when:
        def results = cxLocalizedValueService.getForItem(product)

        then:
        results.size() == 2
        results.every { it.objectId == "p-001" }
    }

    def "getForItem returns empty list when no config covers the item type"() {
        given: "a product with no matching config"
        // Remove the product config so nothing covers Product
        modelService.remove(config)
        def product = builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.getForItem(product)

        then:
        results.isEmpty()
    }

    // -----------------------------------------------------------------------
    // getForKey
    // -----------------------------------------------------------------------

    def "getForKey returns the value matching the exact key"() {
        given:
        builder.createProduct("p-001", stagedVersion)

        when:
        def results = cxLocalizedValueService.getForKey("Product.p-001.name")

        then:
        results.size() == 1
        results[0].key == "Product.p-001.name"
    }

    def "getForKey returns empty for a key that does not exist"() {
        when:
        def results = cxLocalizedValueService.getForKey("Product.nonexistent.name")

        then:
        results.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Sorting
    // -----------------------------------------------------------------------

    def "search sorted ascending by CODE returns values in ascending key order"() {
        given:
        builder.createProduct("b-product", stagedVersion)
        builder.createProduct("a-product", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                [], 50)).allResults()

        then: "first two keys start with 'Product.a-product'"
        results[0].key.startsWith("Product.a-product")
        results[1].key.startsWith("Product.a-product")
    }

    def "search sorted descending by CODE returns values in descending key order"() {
        given:
        builder.createProduct("a-product", stagedVersion)
        builder.createProduct("b-product", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Descending,
                [], 50)).allResults()

        then:
        results[0].key.startsWith("Product.b-product")
        results[1].key.startsWith("Product.b-product")
    }

    // -----------------------------------------------------------------------
    // Catalog isolation — products outside configured catalog version not returned
    // -----------------------------------------------------------------------

    def "search only returns products in the catalog version the config is scoped to"() {
        given: "two products in the configured staged version"
        builder.createProduct("staged-p1", stagedVersion)
        builder.createProduct("staged-p2", stagedVersion)

        and: "a second catalog version NOT covered by the config"
        def otherVersions = builder.createCatalogWithVersions(CATALOG_ID + "-other")
        def otherConfig = builder.createStandardConfig(
                CONFIG_CODE + "-other", "Product", ["code"],
                ["name", "description"],
                true, "catalogVersion", [otherVersions.staged])
        builder.createProduct("other-p", otherVersions.staged)

        when: "searching with only the first config active by scoping to its catalogVersionId"
        def results = cxLocalizedValueService.search(searchData(
                criterion(SearchField.CATALOG_VERSION_ID, SearchOperator.EQUALS, "Staged"),
                criterion(SearchField.CATALOG_ID, SearchOperator.EQUALS, CATALOG_ID)
        )).allResults()

        then: "only the two products from CATALOG_ID/Staged are returned"
        results.size() == 4  // 2 products × 2 attributes
        results.every { it.catalogId == CATALOG_ID }
    }

    def "catalog-aware config with catalogVersion NOT a key field still filters by configured versions (bug #3)"() {
        given: "products in two different catalog versions"
        // The default config (in setup) is catalog-aware, scoped to stagedVersion, with keyFields=['code'].
        // catalogVersion is NOT in keyFields — exactly the configuration where the previous code path
        // failed to set shouldFilterByCatalog and silently returned items from outside the configured
        // catalog versions.
        def otherVersions = builder.createCatalogWithVersions(CATALOG_ID + "-other-bug3")
        builder.createProduct("inside-staged", stagedVersion)
        builder.createProduct("outside-staged", otherVersions.staged)

        when: "no explicit catalog filter — relies entirely on the config's catalogVersions setting"
        def results = cxLocalizedValueService.search(searchData()).allResults()

        then: "only products in the configured catalog version are returned"
        results.size() == 2  // 1 product × 2 attributes
        results.every { it.objectId == "inside-staged" }
    }

    // -----------------------------------------------------------------------
    // Statistics and getTotalCount
    // -----------------------------------------------------------------------

    def "pageable.getTotalCount returns the correct total value count"() {
        given: "3 products, 2 attrs each = 6 values total"
        builder.createProduct("p-001", stagedVersion)
        builder.createProduct("p-002", stagedVersion)
        builder.createProduct("p-003", stagedVersion)

        when:
        def pageable = cxLocalizedValueService.search(searchData())

        then:
        pageable.totalCount == 6
    }

    def "pageable.getTotalCount returns 0 when no products exist"() {
        when:
        def pageable = cxLocalizedValueService.search(searchData())

        then:
        pageable.totalCount == 0
    }

    // -----------------------------------------------------------------------
    // Key format characterization (locks SQL key expression for regression gating)
    // Changing the key format is a breaking change — these tests catch it early.
    // -----------------------------------------------------------------------

    def "key format is <TypeCode>.<productCode>.<attribute> for catalog-aware single-version config (characterization)"() {
        given: "default setup: catalog-aware Product config with single catalogVersion and keyFields=['code']"
        builder.createProduct("SKU-CHAR-1", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(searchData()).allResults()
                .findAll { it.objectId == "SKU-CHAR-1" }

        then: "key is Type.productCode.attribute — no catalog segments for single-version catalog-aware config"
        results.any { it.key == "Product.SKU-CHAR-1.name" }
        results.any { it.key == "Product.SKU-CHAR-1.description" }
        results.every { it.key.count(".") == 2 }
    }

    def "key format for catalog-aware single-version config omits catalog segment"() {
        given: "the default setup config is catalog-aware with one version — key should be Type.code.attr"
        builder.createProduct("SKU-CHAR-2", stagedVersion)

        when:
        def results = cxLocalizedValueService.search(searchData()).allResults()
                .findAll { it.objectId == "SKU-CHAR-2" }

        then: "no catalog/version segments in the key — type.code.attribute only"
        results.any { it.key == "Product.SKU-CHAR-2.name" }
        results.every { it.key.count(".") == 2 }
    }

    // -----------------------------------------------------------------------
    // Statistics and getTotalCount
    // -----------------------------------------------------------------------

    private static SearchCriteria criterion(SearchField field, SearchOperator op, String value) {
        new SearchCriteria(field, op, new StringWithLocale(value))
    }

    private static SearchCriteria criterionWithLocale(SearchField field, SearchOperator op, String value, Locale locale) {
        new SearchCriteria(field, op, new StringWithLocale(value, locale))
    }

    private static CxLocalizedValueSearchData searchData(SearchCriteria... criteria) {
        new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Ascending,
                criteria.toList(), 50)
    }
}
