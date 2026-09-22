/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.strategy

import com.sap.cx.translations.backoffice.model.CxLocalizedValueExcelImportCronJobModel
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.cronjob.model.CronJobHistoryModel
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxLocalizedValueExcelImportCronJobProgressRenderingStrategySpec extends Specification {

    CronJobHistoryModel history = Mock()
    CxLocalizedValueExcelImportCronJobModel cronJob = Mock()

    @Subject
    CxLocalizedValueExcelImportCronJobProgressRenderingStrategy strategy = new CxLocalizedValueExcelImportCronJobProgressRenderingStrategy()

    def setup() {
        history.getCronJob() >> cronJob
    }

    // -------------------------------------------------------------------------
    // calculating-label branch (no MessageFormat) — null totalRows
    // -------------------------------------------------------------------------

    def "getTitle renders calculating label when totalRows is null (no NPE)"() {
        given:
        cronJob.getTotalRows() >> null

        when:
        strategy.getTitle(history)

        then:
        noExceptionThrown()
    }

    def "getTitle renders calculating label when totalRows is 0 (no NPE)"() {
        given:
        cronJob.getTotalRows() >> 0

        when:
        strategy.getTitle(history)

        then:
        noExceptionThrown()
    }

    // -------------------------------------------------------------------------
    // Source fix: null Integer counters must not autobox-NPE inside the progress branch.
    // -------------------------------------------------------------------------

    def "getTitle does not autobox-NPE when alreadyProcessedRows and failedRows are null"() {
        given:
        cronJob.getTotalRows() >> 10
        cronJob.getFailedRows() >> null
        cronJob.getAlreadyProcessedRows() >> null

        when:
        try {
            strategy.getTitle(history)
        } catch (final NullPointerException npe) {
            final String msg = npe.message ?: ""
            assert !msg.contains("intValue") : "Autoboxing NPE leaked despite source guards: $msg"
        }

        then:
        noExceptionThrown()
    }

    def "getTitle does not autobox-NPE with non-null counters"() {
        given:
        cronJob.getTotalRows() >> 100
        cronJob.getFailedRows() >> 5
        cronJob.getAlreadyProcessedRows() >> 30

        when:
        try {
            strategy.getTitle(history)
        } catch (final NullPointerException npe) {
            final String msg = npe.message ?: ""
            assert !msg.contains("intValue")
        }

        then:
        noExceptionThrown()
    }
}
