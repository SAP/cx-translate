/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.excel

import com.sap.cx.translations.backoffice.model.CxLocalizedValueExcelImportJobModel
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.cronjob.model.JobModel
import de.hybris.platform.servicelayer.cronjob.CronJobService
import de.hybris.platform.servicelayer.exceptions.ModelSavingException
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException
import de.hybris.platform.servicelayer.model.ModelService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationsExcelImportHandlerSpec extends Specification {

    ModelService modelService = Mock()
    CronJobService mockCronJobService = Mock()

    @Subject
    CxTranslationsExcelImportHandler handler = new CxTranslationsExcelImportHandler() {
        @Override
        de.hybris.platform.servicelayer.cronjob.CronJobService getCronJobService() {
            return mockCronJobService
        }
    }

    def setup() {
        handler.setModelService(modelService)
    }

    // The PascalCase code that BOTH lookup and create must use (matches _TYPECODE).
    private static final String JOB_CODE = CxLocalizedValueExcelImportJobModel._TYPECODE

    // -------------------------------------------------------------------------
    // Returns existing job when one is found
    // -------------------------------------------------------------------------

    def "getOrCreateCxLocalizedValueExcelImportJob returns existing job using PascalCase code"() {
        given:
        def existing = Mock(JobModel)
        mockCronJobService.getJob(JOB_CODE) >> existing

        when:
        def result = handler.getOrCreateCxLocalizedValueExcelImportJob()

        then:
        result.is(existing)
        1 * existing.setActive(true)
        0 * modelService.create(_)
    }

    // -------------------------------------------------------------------------
    // Creates and SAVES a new job when none exists (regression: previous code never saved)
    // -------------------------------------------------------------------------

    def "getOrCreateCxLocalizedValueExcelImportJob creates AND saves new job using PascalCase code"() {
        given:
        def newJob = Mock(CxLocalizedValueExcelImportJobModel)
        mockCronJobService.getJob(JOB_CODE) >> { throw new UnknownIdentifierException("not found") }
        modelService.create(CxLocalizedValueExcelImportJobModel) >> newJob

        when:
        def result = handler.getOrCreateCxLocalizedValueExcelImportJob()

        then:
        result.is(newJob)
        1 * newJob.setCode(JOB_CODE)
        1 * newJob.setActive(true)
        1 * modelService.save(newJob)
    }

    // -------------------------------------------------------------------------
    // Race condition: save fails, lookup wins → refetch existing
    // -------------------------------------------------------------------------

    def "getOrCreateCxLocalizedValueExcelImportJob refetches when save loses the race"() {
        given:
        def newJob = Mock(CxLocalizedValueExcelImportJobModel)
        def winner = Mock(JobModel)
        def callCount = 0
        mockCronJobService.getJob(JOB_CODE) >> { args ->
            callCount++
            if (callCount == 1) {
                throw new UnknownIdentifierException("not found")
            }
            return winner
        }
        modelService.create(CxLocalizedValueExcelImportJobModel) >> newJob
        modelService.save(newJob) >> { throw new ModelSavingException("duplicate code") }

        when:
        def result = handler.getOrCreateCxLocalizedValueExcelImportJob()

        then:
        result.is(winner)
        1 * winner.setActive(true)
    }

    // -------------------------------------------------------------------------
    // Save failure with refetch also failing → IllegalStateException with original cause
    // -------------------------------------------------------------------------

    def "getOrCreateCxLocalizedValueExcelImportJob throws IllegalStateException when refetch also fails"() {
        given:
        def newJob = Mock(CxLocalizedValueExcelImportJobModel)
        mockCronJobService.getJob(JOB_CODE) >> { throw new UnknownIdentifierException("not found") }
        modelService.create(CxLocalizedValueExcelImportJobModel) >> newJob
        modelService.save(newJob) >> { throw new ModelSavingException("real save failure") }

        when:
        handler.getOrCreateCxLocalizedValueExcelImportJob()

        then:
        thrown(IllegalStateException)
    }
}
