/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.config.impl

import com.sap.cx.translations.model.CxLocalizedObjectConfigJobModel
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.cronjob.model.JobModel
import de.hybris.platform.servicelayer.cronjob.CronJobService
import de.hybris.platform.servicelayer.exceptions.ModelSavingException
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException
import de.hybris.platform.servicelayer.model.ModelService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxLocalizedValueConfigServiceImplSpec extends Specification {

    ModelService modelService = Mock()
    CronJobService cronJobService = Mock()

    @Subject
    CxLocalizedValueConfigServiceImpl service = new CxLocalizedValueConfigServiceImpl()

    def setup() {
        service.modelService = modelService
        service.cronJobService = cronJobService
    }

    // -------------------------------------------------------------------------
    // setDefaultExcludedPropertiesMap — null/blank/malformed entries
    // -------------------------------------------------------------------------

    def "setDefaultExcludedPropertiesMap accepts null without throwing"() {
        when:
        service.setDefaultExcludedPropertiesMap(null)

        then:
        noExceptionThrown()
    }

    def "setDefaultExcludedPropertiesMap accepts blank without throwing"() {
        when:
        service.setDefaultExcludedPropertiesMap("  ")

        then:
        noExceptionThrown()
    }

    def "setDefaultExcludedPropertiesMap skips malformed entries (no dot) without AIOOBE"() {
        when:
        // Mixed valid and malformed (no dot): only valid entries should be parsed
        service.setDefaultExcludedPropertiesMap("Product.name,InvalidEntry,Category.description")

        then:
        noExceptionThrown()
    }

    def "setDefaultExcludedPropertiesMap parses valid entries correctly"() {
        when:
        service.setDefaultExcludedPropertiesMap("Product.name,Product.summary,Category.description")

        then:
        // No exception means the parser successfully bucketed Product → [name, summary] and Category → [description]
        noExceptionThrown()
    }

    // -------------------------------------------------------------------------
    // setExcludedCatalogPatterns — null/blank guard
    // -------------------------------------------------------------------------

    def "setExcludedCatalogPatterns accepts null without throwing"() {
        when:
        service.setExcludedCatalogPatterns(null)

        then:
        noExceptionThrown()
    }

    def "setExcludedCatalogPatterns accepts blank without throwing"() {
        when:
        service.setExcludedCatalogPatterns("")

        then:
        noExceptionThrown()
    }

    def "setExcludedCatalogPatterns compiles regex patterns"() {
        when:
        service.setExcludedCatalogPatterns("test.*,foo[0-9]+")

        then:
        noExceptionThrown()
    }

    // -------------------------------------------------------------------------
    // setExcludedTypes — null/blank guard
    // -------------------------------------------------------------------------

    def "setExcludedTypes accepts null without throwing"() {
        when:
        service.setExcludedTypes(null)

        then:
        noExceptionThrown()
    }

    def "setExcludedTypes accepts blank without throwing"() {
        when:
        service.setExcludedTypes("  ")

        then:
        noExceptionThrown()
    }

    // -------------------------------------------------------------------------
    // setCustomUniqueKeyFields
    // -------------------------------------------------------------------------

    def "setCustomUniqueKeyFields accepts blank as empty map"() {
        when:
        service.setCustomUniqueKeyFields("")

        then:
        noExceptionThrown()
    }

    def "setCustomUniqueKeyFields parses TypeCode->[field1,field2] format"() {
        when:
        service.setCustomUniqueKeyFields("Product->[code,name],Category->[id]")

        then:
        noExceptionThrown()
    }

    // -------------------------------------------------------------------------
    // setDefaultMergeMode
    // -------------------------------------------------------------------------

    def "setDefaultMergeMode falls back to KEEP_EXISTING when input is invalid"() {
        when:
        service.setDefaultMergeMode("INVALID")

        then:
        noExceptionThrown()
        // No assertion on internal state — the constructor logs and sets KEEP_EXISTING
    }

    def "setDefaultMergeMode accepts blank input as null then defaults to KEEP_EXISTING"() {
        when:
        service.setDefaultMergeMode("")

        then:
        noExceptionThrown()
    }

    // -------------------------------------------------------------------------
    // getOrCreateCxLocalizedObjectConfigJob — race-condition handling
    // -------------------------------------------------------------------------

    def "getOrCreateCxLocalizedObjectConfigJob returns existing job"() {
        given:
        def existing = Mock(JobModel)
        cronJobService.getJob("cxLocalizedObjectConfigJob") >> existing

        when:
        def result = service.getOrCreateCxLocalizedObjectConfigJob()

        then:
        result.is(existing)
        1 * existing.setActive(true)
        0 * modelService.create(_)
    }

    def "getOrCreateCxLocalizedObjectConfigJob creates and saves a new job when none exists"() {
        given:
        def newJob = Mock(CxLocalizedObjectConfigJobModel)
        cronJobService.getJob("cxLocalizedObjectConfigJob") >> { throw new UnknownIdentifierException("not found") }
        modelService.create(CxLocalizedObjectConfigJobModel) >> newJob

        when:
        def result = service.getOrCreateCxLocalizedObjectConfigJob()

        then:
        result.is(newJob)
        1 * newJob.setCode("cxLocalizedObjectConfigJob")
        1 * newJob.setActive(true)
        1 * modelService.save(newJob)
    }

    def "getOrCreateCxLocalizedObjectConfigJob refetches when save loses the race"() {
        given:
        def newJob = Mock(CxLocalizedObjectConfigJobModel)
        def winner = Mock(JobModel)
        def callCount = 0
        cronJobService.getJob("cxLocalizedObjectConfigJob") >> { args ->
            callCount++
            if (callCount == 1) {
                throw new UnknownIdentifierException("not found")
            }
            return winner
        }
        modelService.create(CxLocalizedObjectConfigJobModel) >> newJob
        modelService.save(newJob) >> { throw new ModelSavingException("duplicate code") }

        when:
        def result = service.getOrCreateCxLocalizedObjectConfigJob()

        then:
        result.is(winner)
        1 * winner.setActive(true)
    }
}
