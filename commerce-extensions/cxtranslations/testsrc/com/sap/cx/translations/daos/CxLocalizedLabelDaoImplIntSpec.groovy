/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.daos

import com.sap.cx.translations.labels.UsageType
import com.sap.cx.translations.model.CxLocalizedBundleModel
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import jakarta.annotation.Resource
import spock.lang.Subject

/**
 * Integration tests for {@link CxLocalizedLabelDao} /
 * {@link com.sap.cx.translations.daos.impl.CxLocalizedLabelDaoImpl}.
 *
 * <p>Each test runs inside a rolled-back transaction so fixture data is
 * isolated between test methods.</p>
 */
@IntegrationTest
class CxLocalizedLabelDaoImplIntSpec extends ServicelayerTransactionalSpockSpecification {

    private static final String CATALOG_ID = "labelDaoTestCatalog"

    @Subject
    @Resource
    CxLocalizedLabelDao cxLocalizedLabelDao

    @Resource
    ModelService modelService

    private CxTranslationTestDataBuilder builder
    private CatalogVersionModel stagedVersion
    private CatalogVersionModel onlineVersion

    /** Default bundle used by tests that don't care about multi-bundle isolation. */
    private CxLocalizedBundleModel defaultBundle

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService)
        def versions = builder.createCatalogWithVersions(CATALOG_ID)
        stagedVersion = versions.staged
        onlineVersion = versions.online
        defaultBundle = builder.createBundle("defaultBundle", stagedVersion)
    }

    // =======================================================================
    // findLabelsByUsage
    // =======================================================================

    def "findLabelsByUsage with no usage filter returns all labels in the catalog version"() {
        given: "2 frontend and 2 backend labels in staged"
        builder.createFrontendLabel("fe.label.1", defaultBundle, stagedVersion)
        builder.createFrontendLabel("fe.label.2", defaultBundle, stagedVersion)
        builder.createBackendLabel("be.label.1", defaultBundle, stagedVersion)
        builder.createBackendLabel("be.label.2", defaultBundle, stagedVersion)

        when: "querying without any usage filter"
        def result = cxLocalizedLabelDao.findLabelsByUsage(stagedVersion)

        then: "all 4 labels are returned"
        result.size() == 4
    }

    def "findLabelsByUsage with FRONTEND usage type returns only frontend labels"() {
        given:
        builder.createFrontendLabel("fe.label.1", defaultBundle, stagedVersion)
        builder.createFrontendLabel("fe.label.2", defaultBundle, stagedVersion)
        builder.createBackendLabel("be.label.1", defaultBundle, stagedVersion)
        builder.createBackendLabel("be.label.2", defaultBundle, stagedVersion)

        when:
        def result = cxLocalizedLabelDao.findLabelsByUsage(stagedVersion, UsageType.FRONTEND)

        then: "only the 2 frontend labels are returned"
        result.size() == 2
        result.every { it.forFrontend }
    }

    def "findLabelsByUsage with BACKEND usage type returns only backend labels"() {
        given:
        builder.createFrontendLabel("fe.label.1", defaultBundle, stagedVersion)
        builder.createFrontendLabel("fe.label.2", defaultBundle, stagedVersion)
        builder.createBackendLabel("be.label.1", defaultBundle, stagedVersion)
        builder.createBackendLabel("be.label.2", defaultBundle, stagedVersion)

        when:
        def result = cxLocalizedLabelDao.findLabelsByUsage(stagedVersion, UsageType.BACKEND)

        then: "only the 2 backend labels are returned"
        result.size() == 2
        result.every { it.forBackend }
    }

    def "findLabelsByUsage with both FRONTEND and BACKEND returns all matching labels via OR filter"() {
        given:
        builder.createFrontendLabel("fe.label.1", defaultBundle, stagedVersion)
        builder.createFrontendLabel("fe.label.2", defaultBundle, stagedVersion)
        builder.createBackendLabel("be.label.1", defaultBundle, stagedVersion)
        builder.createBackendLabel("be.label.2", defaultBundle, stagedVersion)

        when:
        def result = cxLocalizedLabelDao.findLabelsByUsage(stagedVersion, UsageType.FRONTEND, UsageType.BACKEND)

        then: "all 4 labels satisfy the OR condition"
        result.size() == 4
    }

    def "findLabelsByUsage is isolated by catalog version and ignores labels in other versions"() {
        given: "labels in both staged and online"
        builder.createFrontendLabel("fe.staged.1", defaultBundle, stagedVersion)
        builder.createFrontendLabel("fe.staged.2", defaultBundle, stagedVersion)

        def onlineBundle = builder.createBundle("onlineBundle", onlineVersion)
        builder.createFrontendLabel("fe.online.1", onlineBundle, onlineVersion)

        when: "querying staged"
        def result = cxLocalizedLabelDao.findLabelsByUsage(stagedVersion, UsageType.FRONTEND)

        then: "only the 2 staged labels are returned"
        result.size() == 2
        result.every { it.catalogVersion == stagedVersion }
    }

    // =======================================================================
    // findLabelsByBundleAndUsage
    // =======================================================================

    def "findLabelsByBundleAndUsage returns only labels belonging to the queried bundle"() {
        given: "two bundles, each with 3 labels"
        def bundleA = builder.createBundle("bundleA", stagedVersion)
        def bundleB = builder.createBundle("bundleB", stagedVersion)

        (1..3).each { i -> builder.createFrontendLabel("bundleA.label.$i", bundleA, stagedVersion) }
        (1..3).each { i -> builder.createFrontendLabel("bundleB.label.$i", bundleB, stagedVersion) }

        when:
        def result = cxLocalizedLabelDao.findLabelsByBundleAndUsage(bundleA, stagedVersion)

        then: "only the 3 labels of bundleA are returned"
        result.size() == 3
        result.every { it.bundle.pk == bundleA.pk }
    }

    def "findLabelsByBundleAndUsage with FRONTEND filter returns only frontend labels from the bundle"() {
        given: "a bundle with 2 frontend and 2 backend labels"
        def bundle = builder.createBundle("mixedBundle", stagedVersion)
        builder.createFrontendLabel("mixed.fe.1", bundle, stagedVersion)
        builder.createFrontendLabel("mixed.fe.2", bundle, stagedVersion)
        builder.createBackendLabel("mixed.be.1", bundle, stagedVersion)
        builder.createBackendLabel("mixed.be.2", bundle, stagedVersion)

        when:
        def result = cxLocalizedLabelDao.findLabelsByBundleAndUsage(bundle, stagedVersion, UsageType.FRONTEND)

        then:
        result.size() == 2
        result.every { it.forFrontend }
    }

    def "findLabelsByBundleAndUsage returns empty collection for a bundle with no labels"() {
        given: "a bundle with no labels"
        def emptyBundle = builder.createBundle("emptyBundle", stagedVersion)

        when:
        def result = cxLocalizedLabelDao.findLabelsByBundleAndUsage(emptyBundle, stagedVersion)

        then:
        result.isEmpty()
    }

    // =======================================================================
    // findLabelByKeyAndUsage
    // =======================================================================

    def "findLabelByKeyAndUsage finds a label by its key in the given catalog version"() {
        given:
        builder.createFrontendLabel("test.key", defaultBundle, stagedVersion)

        when:
        def result = cxLocalizedLabelDao.findLabelByKeyAndUsage("test.key", stagedVersion, null)

        then:
        result.present
        result.get().key == "test.key"
    }

    def "findLabelByKeyAndUsage with null usageType returns a match regardless of frontend/backend flag"() {
        given: "a frontend-only label"
        builder.createFrontendLabel("front.only.key", defaultBundle, stagedVersion)

        when: "null usageType means no usage filter"
        def result = cxLocalizedLabelDao.findLabelByKeyAndUsage("front.only.key", stagedVersion, null)

        then:
        result.present
    }

    def "findLabelByKeyAndUsage with FRONTEND respects usage filter — returns frontend label, not backend-only"() {
        given: "a frontend-only label and a backend-only label sharing the same key prefix"
        def frontendLabel = builder.createFrontendLabel("shared.prefix.fe", defaultBundle, stagedVersion)
        builder.createBackendLabel("shared.prefix.be", defaultBundle, stagedVersion)

        when: "searching for the frontend label key with FRONTEND filter"
        def feResult = cxLocalizedLabelDao.findLabelByKeyAndUsage("shared.prefix.fe", stagedVersion, UsageType.FRONTEND)

        and: "searching for the backend label key with FRONTEND filter"
        def beResult = cxLocalizedLabelDao.findLabelByKeyAndUsage("shared.prefix.be", stagedVersion, UsageType.FRONTEND)

        then: "frontend label is found"
        feResult.present
        feResult.get().pk == frontendLabel.pk

        and: "backend-only label is NOT returned for FRONTEND filter"
        !beResult.present
    }

    def "findLabelByKeyAndUsage returns empty Optional when key exists only in a different catalog version"() {
        given: "the label lives in online only"
        def onlineBundle = builder.createBundle("onlineBundleForLabel", onlineVersion)
        builder.createFrontendLabel("online.only.key", onlineBundle, onlineVersion)

        when: "querying with staged version"
        def result = cxLocalizedLabelDao.findLabelByKeyAndUsage("online.only.key", stagedVersion, null)

        then:
        !result.present
    }

    def "findLabelByKeyAndUsage returns empty Optional for a non-existent key"() {
        when:
        def result = cxLocalizedLabelDao.findLabelByKeyAndUsage("absolutely.does.not.exist", stagedVersion, null)

        then:
        !result.present
    }

    // =======================================================================
    // findLabelsByKeysAndUsage
    // =======================================================================

    def "findLabelsByKeysAndUsage with empty keys collection returns empty without exception (guards bug fix)"() {
        when:
        def result = cxLocalizedLabelDao.findLabelsByKeysAndUsage([], stagedVersion, null)

        then:
        result.isEmpty()
    }

    def "findLabelsByKeysAndUsage with null keys collection returns empty without exception (guards bug fix)"() {
        when:
        def result = cxLocalizedLabelDao.findLabelsByKeysAndUsage(null, stagedVersion, null)

        then:
        result.isEmpty()
    }

    def "findLabelsByKeysAndUsage returns labels whose keys match those in the supplied set"() {
        given: "3 labels with keys A, B, C"
        builder.createFrontendLabel("key.A", defaultBundle, stagedVersion)
        builder.createFrontendLabel("key.B", defaultBundle, stagedVersion)
        builder.createFrontendLabel("key.C", defaultBundle, stagedVersion)

        when: "querying for A and B only"
        def result = cxLocalizedLabelDao.findLabelsByKeysAndUsage(["key.A", "key.B"], stagedVersion, null)

        then:
        result.size() == 2
        result.collect { it.key }.containsAll(["key.A", "key.B"])
    }

    def "findLabelsByKeysAndUsage with FRONTEND filter returns only frontend labels from the key set"() {
        given: "key A is frontend, key B is backend"
        builder.createFrontendLabel("multi.key.A", defaultBundle, stagedVersion)
        builder.createBackendLabel("multi.key.B", defaultBundle, stagedVersion)

        when: "querying both keys but with FRONTEND filter"
        def result = cxLocalizedLabelDao.findLabelsByKeysAndUsage(
                ["multi.key.A", "multi.key.B"], stagedVersion, UsageType.FRONTEND)

        then: "only the frontend label is returned"
        result.size() == 1
        result.first().key == "multi.key.A"
    }

    def "findLabelsByKeysAndUsage with null usageType returns all matching keys regardless of usage"() {
        given: "key A frontend, key B backend"
        builder.createFrontendLabel("nofilter.key.A", defaultBundle, stagedVersion)
        builder.createBackendLabel("nofilter.key.B", defaultBundle, stagedVersion)

        when:
        def result = cxLocalizedLabelDao.findLabelsByKeysAndUsage(
                ["nofilter.key.A", "nofilter.key.B"], stagedVersion, null)

        then:
        result.size() == 2
    }

    def "findLabelsByKeysAndUsage is isolated by catalog version"() {
        given: "the same keys exist in both staged and online"
        builder.createFrontendLabel("iso.key.1", defaultBundle, stagedVersion)
        builder.createFrontendLabel("iso.key.2", defaultBundle, stagedVersion)

        def onlineBundle = builder.createBundle("isoOnlineBundle", onlineVersion)
        builder.createFrontendLabel("iso.key.1", onlineBundle, onlineVersion)
        builder.createFrontendLabel("iso.key.2", onlineBundle, onlineVersion)

        when: "querying staged version"
        def result = cxLocalizedLabelDao.findLabelsByKeysAndUsage(
                ["iso.key.1", "iso.key.2"], stagedVersion, null)

        then: "only the 2 staged labels are returned"
        result.size() == 2
        result.every { it.catalogVersion == stagedVersion }
    }

    // =======================================================================
    // findLabelKeys
    // =======================================================================

    def "findLabelKeys with null keyTerm returns empty list without NPE (guards bug fix)"() {
        given: "some labels exist"
        builder.createFrontendLabel("some.key", defaultBundle, stagedVersion)

        when:
        def result = cxLocalizedLabelDao.findLabelKeys(null, stagedVersion, null)

        then: "no exception and empty result"
        result.isEmpty()
    }

    def "findLabelKeys matches a substring of the key in a case-insensitive manner"() {
        given: "label with key 'Header.Title'"
        builder.createFrontendLabel("Header.Title", defaultBundle, stagedVersion)

        when: "searching with lowercase substring 'title'"
        def result = cxLocalizedLabelDao.findLabelKeys("title", stagedVersion, null)

        then: "the label is found"
        result.contains("Header.Title")
    }

    def "findLabelKeys returns empty list when no key matches the search term"() {
        given:
        builder.createFrontendLabel("totally.different.key", defaultBundle, stagedVersion)

        when:
        def result = cxLocalizedLabelDao.findLabelKeys("xyzzy.does.not.match", stagedVersion, null)

        then:
        result.isEmpty()
    }

    def "findLabelKeys caps results at the specified size limit"() {
        given: "5 labels all matching the term 'common'"
        (1..5).each { i -> builder.createFrontendLabel("common.key.$i", defaultBundle, stagedVersion) }

        when: "requesting at most 3"
        def result = cxLocalizedLabelDao.findLabelKeys("common", stagedVersion, 3)

        then:
        result.size() <= 3
        result.size() >= 1  // sanity: at least one result
    }

    def "findLabelKeys returns all matching labels when resultsSizeLimit is null"() {
        given: "5 labels all matching the term 'unlimited'"
        (1..5).each { i -> builder.createFrontendLabel("unlimited.key.$i", defaultBundle, stagedVersion) }

        when: "no limit specified"
        def result = cxLocalizedLabelDao.findLabelKeys("unlimited", stagedVersion, null)

        then:
        result.size() == 5
    }

    def "findLabelKeys is isolated by catalog version"() {
        given: "matching labels in both staged and online"
        (1..3).each { i -> builder.createFrontendLabel("shared.term.staged.$i", defaultBundle, stagedVersion) }

        def onlineBundle = builder.createBundle("keysOnlineBundle", onlineVersion)
        (1..2).each { i -> builder.createFrontendLabel("shared.term.online.$i", onlineBundle, onlineVersion) }

        when: "querying staged"
        def result = cxLocalizedLabelDao.findLabelKeys("shared.term.staged", stagedVersion, null)

        then: "only staged keys are returned"
        result.size() == 3
        result.every { it.contains("staged") }
    }
}


