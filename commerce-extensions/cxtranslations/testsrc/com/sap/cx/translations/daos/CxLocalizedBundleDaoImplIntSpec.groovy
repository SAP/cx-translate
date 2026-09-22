/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.daos

import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import jakarta.annotation.Resource
import spock.lang.Subject

/**
 * Integration tests for {@link CxLocalizedBundleDao} / {@link com.sap.cx.translations.daos.impl.CxLocalizedBundleDaoImpl}.
 *
 * <p>Each test runs in its own transaction (via the {@code @Transactional} annotation on the
 * parent class) that is rolled back after the test completes, so every test starts with a
 * clean slate using the same fixed catalog IDs.</p>
 */
@IntegrationTest
class CxLocalizedBundleDaoImplIntSpec extends ServicelayerTransactionalSpockSpecification {

    private static final String CATALOG_ID = "bundleDaoTestCatalog"

    @Subject
    @Resource
    CxLocalizedBundleDao cxLocalizedBundleDao

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
    // findBundlesByCatalogVersion
    // -----------------------------------------------------------------------

    def "findBundlesByCatalogVersion returns only bundles belonging to the queried catalog version"() {
        given: "two bundles in staged and one in online"
        builder.createBundle("staged-bundle-1", stagedVersion)
        builder.createBundle("staged-bundle-2", stagedVersion)
        builder.createBundle("online-bundle-1", onlineVersion)

        when:
        def result = cxLocalizedBundleDao.findBundlesByCatalogVersion(stagedVersion)

        then: "only the two staged bundles are returned"
        result.size() == 2
        result.every { it.catalogVersion == stagedVersion }
        result.collect { it.code }.containsAll(["staged-bundle-1", "staged-bundle-2"])
    }

    def "findBundlesByCatalogVersion returns empty collection when no bundles exist for the version"() {
        given: "a bundle only in online"
        builder.createBundle("online-only-bundle", onlineVersion)

        when:
        def result = cxLocalizedBundleDao.findBundlesByCatalogVersion(stagedVersion)

        then:
        result.isEmpty()
    }

    // -----------------------------------------------------------------------
    // findBundleByCode
    // -----------------------------------------------------------------------

    def "findBundleByCode returns the bundle when code and catalog version match"() {
        given:
        def expected = builder.createBundle("myBundle", stagedVersion)

        when:
        def result = cxLocalizedBundleDao.findBundleByCode("myBundle", stagedVersion)

        then:
        result.present
        result.get().pk == expected.pk
        result.get().code == "myBundle"
    }

    def "findBundleByCode returns empty Optional when the code exists only in a different catalog version"() {
        given: "the bundle lives in online only"
        builder.createBundle("onlineOnlyBundle", onlineVersion)

        when: "we query with the staged version"
        def result = cxLocalizedBundleDao.findBundleByCode("onlineOnlyBundle", stagedVersion)

        then:
        !result.present
    }

    def "findBundleByCode returns empty Optional when the code does not exist at all"() {
        when:
        def result = cxLocalizedBundleDao.findBundleByCode("nonExistentBundle", stagedVersion)

        then:
        !result.present
    }

    def "findBundleByCode returns the correct bundle when the same code exists in multiple catalog versions"() {
        given: "a bundle with the same code in both staged and online"
        def stagedBundle = builder.createBundle("sharedCode", stagedVersion)
        builder.createBundle("sharedCode", onlineVersion)

        when: "we query with the staged version"
        def result = cxLocalizedBundleDao.findBundleByCode("sharedCode", stagedVersion)

        then: "the staged bundle is returned"
        result.present
        result.get().pk == stagedBundle.pk
        result.get().catalogVersion == stagedVersion
    }
}


