/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.jobs

import com.sap.cx.translations.enums.CxApprovalTranslationStatus
import com.sap.cx.translations.model.CxApproveTranslationsCronJobModel
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.services.CxApproveTranslationsService
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.cronjob.enums.CronJobResult
import de.hybris.platform.cronjob.enums.CronJobStatus
import de.hybris.platform.cronjob.jalo.AbstractProgressTracker
import de.hybris.platform.servicelayer.model.ModelService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxApproveTranslationsJobSpec extends Specification {

    ModelService modelService = Mock()
    CxApproveTranslationsService approveService = Mock()

    /** Safe no-op tracker — storeProgressInDB is abstract; close() is a no-op here too. */
    AbstractProgressTracker tracker = new AbstractProgressTracker() {
        @Override void storeProgressInDB(Double progress) { /* no-op */ }
        @Override void close() { /* no-op */ }
    }

    @Subject
    CxApproveTranslationsJob job = new CxApproveTranslationsJob() {
        @Override
        protected AbstractProgressTracker createProgressTracker(CxApproveTranslationsCronJobModel cronJob) {
            return tracker
        }
    }

    CxApproveTranslationsCronJobModel cronJob = Mock()

    def setup() {
        job.setCxApproveTranslationsService(approveService)
        job.modelService = modelService
    }

    def "returns SUCCESS when no translations to approve"() {
        given:
        cronJob.getTranslationsToApprove() >> []

        when:
        def result = job.perform(cronJob)

        then:
        result.result == CronJobResult.SUCCESS
        result.status == CronJobStatus.FINISHED
    }

    def "returns SUCCESS when all translations are already non-PENDING"() {
        given:
        def done = Mock(CxProposedTranslationModel) { getApprovalStatus() >> CxApprovalTranslationStatus.SUCCESS }
        cronJob.getTranslationsToApprove() >> [done]

        when:
        def result = job.perform(cronJob)

        then:
        result.result == CronJobResult.SUCCESS
        0 * approveService.approveTranslation(_)
    }

    def "resets ERROR translations to PENDING before processing"() {
        given:
        def t = Mock(CxProposedTranslationModel)
        t.getApprovalStatus() >>> [CxApprovalTranslationStatus.ERROR, CxApprovalTranslationStatus.PENDING]
        cronJob.getTranslationsToApprove() >> [t]
        approveService.approveTranslation(t) >> true

        when:
        job.perform(cronJob)

        then:
        1 * t.setApprovalStatus(CxApprovalTranslationStatus.PENDING)
        1 * modelService.save(t)
    }

    def "executeApproval returns true when all approvals succeed"() {
        given:
        def t = Mock(CxProposedTranslationModel)
        approveService.approveTranslation(t) >> true

        expect:
        job.executeApproval(cronJob, [t], tracker) == true
    }

    def "executeApproval returns false when any approval fails"() {
        given:
        def t = Mock(CxProposedTranslationModel)
        approveService.approveTranslation(t) >> false

        expect:
        job.executeApproval(cronJob, [t], tracker) == false
    }

    def "isAbortable returns true"() {
        expect:
        job.isAbortable()
    }
}
