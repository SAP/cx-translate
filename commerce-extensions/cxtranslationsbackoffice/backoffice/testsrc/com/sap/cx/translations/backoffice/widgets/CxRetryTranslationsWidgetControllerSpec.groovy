/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.widgets

import com.sap.cx.translations.daos.CxTranslationServiceSettingsDao
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.model.CxTranslationCronJobModel
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService
import de.hybris.bootstrap.annotations.UnitTest
import org.zkoss.zul.ListModelList
import org.zkoss.zul.Textbox
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxRetryTranslationsWidgetControllerSpec extends Specification {

    CxLocalizedValueService localizedValueService = Mock()
    CxTranslationServiceSettingsDao settingsDao = Mock()
    Textbox translationHint = Mock()

    @Subject
    CxRetryTranslationsWidgetController controller = new CxRetryTranslationsWidgetController()

    def setup() {
        // Inject via reflection where @Resource and @Wire fields are normally bound by ZK
        controller.cxLocalizedValueService = localizedValueService
        controller.cxAiTranslationServiceSettingsDao = settingsDao
        controller.translationHint = translationHint
        controller.aiSettingsSelectorModel.clear()
        translationHint.getValue() >> "user hint"
    }

    // -------------------------------------------------------------------------
    // The fix: filter null cron jobs before groupingBy (was NPE in Collectors.groupingBy)
    // -------------------------------------------------------------------------

    def "retryTranslations filters out proposed translations with null cron job (no NPE)"() {
        given:
        def cronJob = Mock(CxTranslationCronJobModel)
        def orphanItem = Mock(CxProposedTranslationModel) { getTranslationCronJob() >> null }
        def validItem = Mock(CxProposedTranslationModel) { getTranslationCronJob() >> cronJob }

        when:
        controller.retryTranslations([orphanItem, validItem])

        then:
        noExceptionThrown()
        // Only the valid item should be passed to the service, grouped under its cron job
        1 * localizedValueService.retryTranslations(cronJob, { it == [validItem] }, _, "user hint")
        // Orphan with null cron job is silently skipped
    }

    def "retryTranslations handles list with only null-cron-job items without throwing"() {
        given:
        def orphan1 = Mock(CxProposedTranslationModel) { getTranslationCronJob() >> null }
        def orphan2 = Mock(CxProposedTranslationModel) { getTranslationCronJob() >> null }

        when:
        controller.retryTranslations([orphan1, orphan2])

        then:
        noExceptionThrown()
        0 * localizedValueService.retryTranslations(_, _, _, _)
    }

    def "retryTranslations groups items by their cron job"() {
        given:
        def cronJobA = Mock(CxTranslationCronJobModel)
        def cronJobB = Mock(CxTranslationCronJobModel)
        def itemA1 = Mock(CxProposedTranslationModel) { getTranslationCronJob() >> cronJobA }
        def itemA2 = Mock(CxProposedTranslationModel) { getTranslationCronJob() >> cronJobA }
        def itemB1 = Mock(CxProposedTranslationModel) { getTranslationCronJob() >> cronJobB }

        when:
        controller.retryTranslations([itemA1, itemA2, itemB1])

        then:
        1 * localizedValueService.retryTranslations(cronJobA, { it.containsAll([itemA1, itemA2]) && it.size() == 2 }, _, _)
        1 * localizedValueService.retryTranslations(cronJobB, [itemB1], _, _)
    }

    def "retryTranslations passes settingsOverride from selection when present"() {
        given:
        def cronJob = Mock(CxTranslationCronJobModel)
        def item = Mock(CxProposedTranslationModel) { getTranslationCronJob() >> cronJob }
        def settings = Mock(CxTranslationServiceSettingsModel)
        controller.aiSettingsSelectorModel.addAll([settings])
        controller.aiSettingsSelectorModel.setSelection([settings])

        when:
        controller.retryTranslations([item])

        then:
        1 * localizedValueService.retryTranslations(cronJob, _, settings, "user hint")
    }
}
