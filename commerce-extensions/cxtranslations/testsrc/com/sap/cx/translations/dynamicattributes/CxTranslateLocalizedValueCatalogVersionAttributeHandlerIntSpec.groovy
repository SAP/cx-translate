/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.dynamicattributes

import com.sap.cx.translations.enums.CxTranslateLocalizedValueTargetType
import com.sap.cx.translations.model.CxLocalizedValueModel
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.core.model.product.ProductModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.type.TypeService
import jakarta.annotation.Resource

/**
 * Integration tests for {@link CxTranslateLocalizedValueCatalogVersionAttributeHandler}
 * — the dynamic attribute behind {@code CxAbstractLocalizedValueModel.catalogVersion}.
 *
 * <p>The handler resolves the catalog version from the target item when the
 * {@link com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel} is
 * catalog-aware.  It returns {@code null} otherwise.</p>
 */
@IntegrationTest
class CxTranslateLocalizedValueCatalogVersionAttributeHandlerIntSpec extends ServicelayerTransactionalSpockSpecification {

    private static final String CATALOG_ID = "cvHandlerTestCatalog"

    @Resource
    ModelService modelService

    @Resource
    TypeService typeService

    private CxTranslationTestDataBuilder builder
    private CatalogVersionModel stagedVersion

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService, typeService)
        def versions = builder.createCatalogWithVersions(CATALOG_ID)
        stagedVersion = versions.staged
    }

    // -----------------------------------------------------------------------
    // catalogVersion — catalog-aware config
    // -----------------------------------------------------------------------

    def "getCatalogVersion returns the catalog version of the target item when config is catalog-aware"() {
        given: "a catalog-aware config and a Product as target"
        def config = builder.createLocalizedObjectConfig("cv-config-aware", "Product", true)

        def product = modelService.create(ProductModel)
        product.setCode("cv-product-1")
        product.setCatalogVersion(stagedVersion)
        modelService.save(product)

        def localizedValue = modelService.create(CxLocalizedValueModel)
        localizedValue.setKey("cv-key-1")
        localizedValue.setTargetType(CxTranslateLocalizedValueTargetType.ITEM)
        localizedValue.setItemTarget(product)
        localizedValue.setConfig(config)
        localizedValue.setAttribute("name")
        modelService.save(localizedValue)

        when:
        CatalogVersionModel result = localizedValue.getCatalogVersion()

        then:
        result == stagedVersion
    }

    def "getCatalogVersion returns null when config is not catalog-aware"() {
        given: "a non-catalog-aware config"
        def config = builder.createLocalizedObjectConfig("cv-config-not-aware", "Product", false)

        def product = modelService.create(ProductModel)
        product.setCode("cv-product-2")
        product.setCatalogVersion(stagedVersion)
        modelService.save(product)

        def localizedValue = modelService.create(CxLocalizedValueModel)
        localizedValue.setKey("cv-key-2")
        localizedValue.setTargetType(CxTranslateLocalizedValueTargetType.ITEM)
        localizedValue.setItemTarget(product)
        localizedValue.setConfig(config)
        localizedValue.setAttribute("name")
        modelService.save(localizedValue)

        when:
        def result = localizedValue.getCatalogVersion()

        then:
        result == null
    }

    def "getCatalogVersion returns null when target is null"() {
        given: "a catalog-aware config but no target set"
        def config = builder.createLocalizedObjectConfig("cv-config-no-target", "Product", true)

        def localizedValue = modelService.create(CxLocalizedValueModel)
        localizedValue.setKey("cv-key-no-target")
        localizedValue.setConfig(config)
        modelService.save(localizedValue)

        when:
        def result = localizedValue.getCatalogVersion()

        then:
        result == null
    }

    def "getCatalogVersion returns null when config is null"() {
        given:
        def localizedValue = modelService.create(CxLocalizedValueModel)
        localizedValue.setKey("cv-key-no-config")
        modelService.save(localizedValue)

        when:
        def result = localizedValue.getCatalogVersion()

        then:
        result == null
    }

    def "catalogVersion handler set throws UnsupportedOperationException"() {
        given:
        def handler = new CxTranslateLocalizedValueCatalogVersionAttributeHandler()
        handler.setModelService(modelService)
        def localizedValue = modelService.create(CxLocalizedValueModel)

        when:
        handler.set(localizedValue, stagedVersion)

        then:
        thrown(UnsupportedOperationException)
    }
}
