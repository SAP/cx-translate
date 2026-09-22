/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.jobs

import com.sap.cx.translations.enums.CxTranslationStatus
import com.sap.cx.translations.model.CxLocalizedLabelModel
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.model.CxTranslationCronJobModel
import com.sap.cx.translations.services.CxApproveTranslationsService
import com.sap.cx.translations.services.CxLocalizedLabelsPluralsService
import com.sap.cx.translations.services.localizedobjects.CxLocalizedClassificationService
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.ItemModel
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.cronjob.enums.CronJobResult
import de.hybris.platform.cronjob.enums.CronJobStatus
import de.hybris.platform.cronjob.jalo.AbstractProgressTracker
import de.hybris.platform.servicelayer.i18n.CommonI18NService
import de.hybris.platform.servicelayer.model.ModelService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationJobSpec extends Specification {

    ModelService modelService = Mock()
    CxLocalizedValueService localizedValueService = Mock()
    CxLocalizedClassificationService classificationService = Mock()
    CxApproveTranslationsService approveService = Mock()
    CommonI18NService i18nService = Mock()
    CxLocalizedLabelsPluralsService pluralsService = Mock()

    AbstractProgressTracker tracker = new AbstractProgressTracker() {
        @Override void storeProgressInDB(Double progress) { /* no-op */ }
        @Override void close() { /* no-op */ }
    }

    @Subject
    CxTranslationJob job = new CxTranslationJob() {
        @Override
        protected AbstractProgressTracker createProgressTracker(CxTranslationCronJobModel cronJob) {
            return tracker
        }
    }

    CxTranslationCronJobModel cronJob = Mock()
    LanguageModel sourceLang = Mock()

    def setup() {
        job.setCxLocalizedValueService(localizedValueService)
        job.setCxLocalizedClassificationService(classificationService)
        job.setCxApproveTranslationsService(approveService)
        job.setCommonI18NService(i18nService)
        job.setPluralsService(pluralsService)
        job.modelService = modelService

        cronJob.getSourceLanguage() >> sourceLang
        cronJob.getTargetLanguages() >> []
        cronJob.getAttributes() >> ([] as Set)
        cronJob.isAutomaticPlurals() >> false
        cronJob.isTranslateProductFeatures() >> false
        cronJob.getSaveForApproval() >> true
        i18nService.getLocaleForLanguage(sourceLang) >> Locale.ENGLISH
        // NOTE: do not stub pluralsService.getRequiredPluralForms here — would shadow per-test stubs
    }

    // -------------------------------------------------------------------------
    // perform: early exits
    // -------------------------------------------------------------------------

    def "returns SUCCESS when all proposed translations are already non-PENDING"() {
        given:
        def done = Mock(CxProposedTranslationModel) { getTranslationStatus() >> CxTranslationStatus.SUCCESS }
        cronJob.getValuesToTranslate() >> [done]

        when:
        def result = job.perform(cronJob)

        then:
        result.result == CronJobResult.SUCCESS
        0 * localizedValueService.aiTranslate(_)
    }

    def "returns SUCCESS when items produce no localised values"() {
        given:
        cronJob.getValuesToTranslate() >> []
        def item = Mock(ItemModel)
        cronJob.getItems() >> [item]
        localizedValueService.getForItem(item) >> []

        when:
        def result = job.perform(cronJob)

        then:
        result.result == CronJobResult.SUCCESS
        0 * localizedValueService.aiTranslate(_)
    }

    def "returns SUCCESS without NPE when getItems() is null"() {
        given:
        cronJob.getValuesToTranslate() >> []
        cronJob.getItems() >> null

        when:
        def result = job.perform(cronJob)

        then:
        noExceptionThrown()
        result.result == CronJobResult.SUCCESS
    }

    // -------------------------------------------------------------------------
    // perform: ERROR → PENDING reset
    // -------------------------------------------------------------------------

    def "resets ERROR translations to PENDING before processing"() {
        given:
        def t = Mock(CxProposedTranslationModel)
        t.getTranslationStatus() >>> [CxTranslationStatus.ERROR, CxTranslationStatus.PENDING]
        cronJob.getValuesToTranslate() >> [t]
        localizedValueService.aiTranslate(_) >> null

        when:
        job.perform(cronJob)

        then:
        1 * t.setTranslationStatus(CxTranslationStatus.PENDING)
        1 * modelService.save(t)
    }

    def "isAbortable returns true"() {
        expect:
        job.isAbortable()
    }

    // -------------------------------------------------------------------------
    // addPluralFormsIfNeeded
    // -------------------------------------------------------------------------

    def "addPluralFormsIfNeeded: returns empty for null input"() {
        expect:
        job.addPluralFormsIfNeeded(null, cronJob).isEmpty()
    }

    def "addPluralFormsIfNeeded: returns empty for empty input"() {
        expect:
        job.addPluralFormsIfNeeded([], cronJob).isEmpty()
    }

    def "addPluralFormsIfNeeded: returns items unchanged when collection has non-label items"() {
        given:
        def product = Mock(ItemModel)
        def label = Mock(CxLocalizedLabelModel)

        when:
        def result = job.addPluralFormsIfNeeded([product, label], cronJob)

        then:
        result.containsAll([product, label])
        0 * pluralsService.getRequiredPluralForms(_, _, _)
    }

    def "addPluralFormsIfNeeded: enriches labels-only collection with plural forms"() {
        given:
        def label = Mock(CxLocalizedLabelModel)
        def pluralLabel = Mock(CxLocalizedLabelModel)
        // Use wildcard matchers to avoid collection type mismatch
        pluralsService.getRequiredPluralForms(_, _, _) >> [pluralLabel]

        when:
        def result = job.addPluralFormsIfNeeded([label], cronJob)

        then:
        result.contains(label)
        result.contains(pluralLabel)
    }

    def "addPluralFormsIfNeeded: deduplicates when plural already in source"() {
        given:
        def label = Mock(CxLocalizedLabelModel)
        pluralsService.getRequiredPluralForms(_, _, _) >> [label]

        when:
        def result = job.addPluralFormsIfNeeded([label], cronJob)

        then:
        result.size() == 1
        result.contains(label)
    }
}
