/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.strategy

import com.sap.cx.translations.enums.CxApprovalTranslationStatus
import com.sap.cx.translations.model.CxApproveTranslationsCronJobModel
import com.sap.cx.translations.model.CxProposedTranslationModel
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.cronjob.model.CronJobHistoryModel
import de.hybris.platform.servicelayer.model.ModelService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxApproveTranslationCronJobProgressRenderingStrategySpec extends Specification {

    ModelService modelService = Mock()
    CronJobHistoryModel history = Mock()
    CxApproveTranslationsCronJobModel cronJob = Mock()

    @Subject
    CxApproveTranslationCronJobProgressRenderingStrategy strategy = new CxApproveTranslationCronJobProgressRenderingStrategy()

    def setup() {
        strategy.setModelService(modelService)
        history.getCronJob() >> cronJob
    }

    // -------------------------------------------------------------------------
    // null elements-to-process: must not NPE from Integer autoboxing
    // -------------------------------------------------------------------------

    def "getTitle does not NPE when elementsToProcessCount is null (initial state)"() {
        given:
        cronJob.getElementsToProcessCount() >> null

        when:
        // The actual fix verified here: previously `if (totalElements == 0)` would NPE on null Integer.
        // Now the source guards with `== null || == 0`. Title may be null in tests because Labels.getLabel
        // has no ZK resource bundle loaded — that's a test-runtime artifact, not a source bug.
        strategy.getTitle(history)

        then:
        noExceptionThrown()
    }

    def "getTitle does not NPE when elementsToProcessCount is 0"() {
        given:
        cronJob.getElementsToProcessCount() >> 0

        when:
        strategy.getTitle(history)

        then:
        noExceptionThrown()
    }

    // -------------------------------------------------------------------------
    // canHandle/isProgressSupported
    // -------------------------------------------------------------------------

    def "canHandle returns true for CxApproveTranslationsCronJobModel"() {
        expect:
        strategy.canHandle(history) == true
    }

    def "canHandle returns false for other cron job types"() {
        given:
        def otherCronJob = Mock(de.hybris.platform.cronjob.model.CronJobModel)
        def otherHistory = Mock(CronJobHistoryModel)
        otherHistory.getCronJob() >> otherCronJob

        expect:
        strategy.canHandle(otherHistory) == false
    }

    def "isProgressSupported returns true for CxApproveTranslationsCronJobModel"() {
        expect:
        strategy.isProgressSupported(history) == true
    }
}
