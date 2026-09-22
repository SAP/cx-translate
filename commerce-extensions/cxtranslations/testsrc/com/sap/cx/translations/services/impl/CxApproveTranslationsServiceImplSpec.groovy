/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl

import com.sap.cx.translations.enums.CxApprovalTranslationStatus
import com.sap.cx.translations.enums.CxTranslateLocalizedValueTargetType
import com.sap.cx.translations.model.CxApproveTranslationsCronJobModel
import com.sap.cx.translations.model.CxApproveTranslationsJobModel
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService
import com.sap.cx.translations.services.localizedobjects.CxLocalizedClassificationService
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.ItemModel
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.cronjob.model.JobModel
import de.hybris.platform.servicelayer.cluster.ClusterService
import de.hybris.platform.servicelayer.cronjob.CronJobService
import de.hybris.platform.servicelayer.exceptions.ModelSavingException
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.user.UserService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxApproveTranslationsServiceImplSpec extends Specification {

    ModelService modelService = Mock()
    CronJobService cronJobService = Mock()
    ClusterService clusterService = Mock()
    UserService userService = Mock()
    CxTranslateCommerceCommonI18NService i18nService = Mock()
    CxLocalizedClassificationService classificationService = Mock()

    @Subject
    CxApproveTranslationsServiceImpl service = new CxApproveTranslationsServiceImpl()

    def setup() {
        service.modelService = modelService
        service.cronJobService = cronJobService
        service.clusterService = clusterService
        service.userService = userService
        service.cxTranslateCommerceCommonI18NService = i18nService
        service.cxLocalizedClassificationService = classificationService
    }

    // -------------------------------------------------------------------------
    // approveTranslation
    // -------------------------------------------------------------------------

    def "approveTranslation returns true and skips work when item is already removed"() {
        given:
        def translation = Mock(CxProposedTranslationModel)
        modelService.isRemoved(translation) >> true

        when:
        def result = service.approveTranslation(translation)

        then:
        result == true
        0 * translation.setApprovalStatus(_)
    }

    def "approveTranslation marks ERROR and returns false when no target locales are resolvable"() {
        given:
        def translation = Mock(CxProposedTranslationModel)
        def lang = Mock(LanguageModel) { getIsocode() >> "xx" }
        translation.getTranslatedLanguages() >> [lang]
        translation.getKey() >> "k"
        translation.getNotes() >> null
        modelService.isRemoved(translation) >> false
        i18nService.getLocale("xx") >> null  // unresolvable

        when:
        def result = service.approveTranslation(translation)

        then:
        result == false
        1 * translation.setApprovalStatus(CxApprovalTranslationStatus.ERROR)
    }

    def "approveTranslation applies manual value when present and valid"() {
        given:
        def translation = Mock(CxProposedTranslationModel)
        def lang = Mock(LanguageModel) { getIsocode() >> "en" }
        def target = Mock(ItemModel)
        def locale = Locale.ENGLISH
        translation.getTranslatedLanguages() >> [lang]
        translation.getManualTranslationValue(locale) >> "Manual"
        translation.getProposedTranslationValue(locale) >> "AI generated"
        translation.getTarget() >> target
        translation.getTargetType() >> CxTranslateLocalizedValueTargetType.ITEM
        modelService.isRemoved(translation) >> false
        i18nService.getLocale("en") >> locale

        when:
        def result = service.approveTranslation(translation)

        then:
        result == true
        1 * translation.setValue("Manual", locale)
        0 * translation.setValue("AI generated", _)
        1 * modelService.save(target)
        1 * modelService.remove(translation)
    }

    def "approveTranslation falls back to proposed value when manual is blank"() {
        given:
        def translation = Mock(CxProposedTranslationModel)
        def lang = Mock(LanguageModel) { getIsocode() >> "en" }
        def target = Mock(ItemModel)
        def locale = Locale.ENGLISH
        translation.getTranslatedLanguages() >> [lang]
        translation.getManualTranslationValue(locale) >> "  "
        translation.getProposedTranslationValue(locale) >> "AI generated"
        translation.getTarget() >> target
        translation.getTargetType() >> CxTranslateLocalizedValueTargetType.ITEM
        modelService.isRemoved(translation) >> false
        i18nService.getLocale("en") >> locale

        when:
        service.approveTranslation(translation)

        then:
        1 * translation.setValue("AI generated", locale)
    }

    def "approveTranslation marks ERROR and persists translation when target save fails"() {
        given:
        def translation = Mock(CxProposedTranslationModel)
        def lang = Mock(LanguageModel) { getIsocode() >> "en" }
        def target = Mock(ItemModel)
        def locale = Locale.ENGLISH
        translation.getTranslatedLanguages() >> [lang]
        translation.getProposedTranslationValue(locale) >> "AI generated"
        translation.getTarget() >> target
        translation.getTargetType() >> CxTranslateLocalizedValueTargetType.ITEM
        translation.getKey() >> "k"
        translation.getAttribute() >> "name"
        modelService.isRemoved(translation) >> false
        i18nService.getLocale("en") >> locale
        modelService.save(target) >> { throw new ModelSavingException("save failed") }

        when:
        def result = service.approveTranslation(translation)

        then:
        result == false
        1 * translation.setApprovalStatus(CxApprovalTranslationStatus.ERROR)
        // The proposed translation itself should still be saved with the ERROR status
        1 * modelService.save(translation)
        0 * modelService.remove(translation)
    }

    // -------------------------------------------------------------------------
    // getOrCreateCxApproveTranslationsJob — race condition handling
    // -------------------------------------------------------------------------

    def "getOrCreateCxApproveTranslationsJob returns existing job from cronJobService"() {
        given:
        def existing = Mock(JobModel)
        cronJobService.getJob("cxApproveTranslationsJob") >> existing

        when:
        def result = service.getOrCreateCxApproveTranslationsJob()

        then:
        result.is(existing)
        1 * existing.setActive(true)
        0 * modelService.create(_)
    }

    def "getOrCreateCxApproveTranslationsJob creates and saves a new job when none exists"() {
        given:
        def newJob = Mock(CxApproveTranslationsJobModel)
        cronJobService.getJob("cxApproveTranslationsJob") >> { throw new UnknownIdentifierException("not found") }
        modelService.create(CxApproveTranslationsJobModel) >> newJob

        when:
        def result = service.getOrCreateCxApproveTranslationsJob()

        then:
        result.is(newJob)
        1 * newJob.setCode("cxApproveTranslationsJob")
        1 * newJob.setActive(true)
        1 * modelService.save(newJob)
    }

    def "getOrCreateCxApproveTranslationsJob refetches existing job when save loses race"() {
        given:
        def newJob = Mock(CxApproveTranslationsJobModel)
        def winner = Mock(JobModel)
        // First call: not found (triggers create+save). Second call (in catch block): returns winner.
        def callCount = 0
        cronJobService.getJob("cxApproveTranslationsJob") >> { args ->
            callCount++
            if (callCount == 1) {
                throw new UnknownIdentifierException("not found")
            }
            return winner
        }
        modelService.create(CxApproveTranslationsJobModel) >> newJob
        modelService.save(newJob) >> { throw new ModelSavingException("duplicate code") }

        when:
        def result = service.getOrCreateCxApproveTranslationsJob()

        then:
        result.is(winner)
        1 * winner.setActive(true)
    }

    def "getOrCreateCxApproveTranslationsJob throws IllegalStateException when refetch fails after save failure"() {
        given:
        def newJob = Mock(CxApproveTranslationsJobModel)
        cronJobService.getJob("cxApproveTranslationsJob") >> { throw new UnknownIdentifierException("not found") }
        modelService.create(CxApproveTranslationsJobModel) >> newJob
        modelService.save(newJob) >> { throw new ModelSavingException("real save failure") }

        when:
        service.getOrCreateCxApproveTranslationsJob()

        then:
        thrown(IllegalStateException)
    }
}
