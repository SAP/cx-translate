/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.model.CxTranslationCronJobModel
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel
import com.sap.cx.translations.services.localizedobjects.parameters.CxAutomaticTranslationParameters
import com.sap.cx.translations.services.localizedobjects.parameters.CxPerformTranslationParameters
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForItemsParameters
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForValuesParameters
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.ItemModel
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.core.model.type.AttributeDescriptorModel
import spock.lang.Specification

/**
 * Unit tests for the parameter builder classes in the {@code parameters} subpackage:
 * <ul>
 *   <li>{@link CxAutomaticTranslationParameters}</li>
 *   <li>{@link CxStartTranslationCronJobForValuesParameters}</li>
 *   <li>{@link CxStartTranslationCronJobForItemsParameters}</li>
 *   <li>{@link CxPerformTranslationParameters}</li>
 * </ul>
 *
 * Tests focus on: builder fluency, field propagation, default values, and the
 * {@code buildFromCronJob} factory on {@link CxPerformTranslationParameters}.
 */
@UnitTest
class CxTranslationParametersSpec extends Specification {

    // -----------------------------------------------------------------------
    // CxStartTranslationCronJobForValuesParameters
    // -----------------------------------------------------------------------

    def "ForValuesParameters builder copies all CxAutomaticTranslationParameters fields"() {
        given:
        def lang = Mock(LanguageModel)
        def settings = Mock(CxTranslationServiceSettingsModel)
        def targets = [Mock(LanguageModel)]
        def values = [Mock(Object)]  // Collection<CxLocalizedValueModel>

        when:
        def params = CxStartTranslationCronJobForValuesParameters.builder()
                .sourceLanguage(lang)
                .targetLanguages(targets)
                .userHint("my hint")
                .serviceSettings(settings)
                .saveForApproval(Boolean.TRUE)
                .skipTranslated(Boolean.FALSE)
                .automaticPlurals(true)
                .localizedValues([])
                .build()

        then:
        params.sourceLanguage == lang
        params.targetLanguages == targets
        params.userHint == "my hint"
        params.serviceSettings == settings
        params.saveForApproval == Boolean.TRUE
        params.skipTranslated == Boolean.FALSE
        params.automaticPlurals == true
    }

    def "ForValuesParameters builder with empty localizedValues returns empty collection"() {
        when:
        def params = CxStartTranslationCronJobForValuesParameters.builder()
                .localizedValues([])
                .build()

        then:
        params.localizedValues.isEmpty()
    }

    def "ForValuesParameters defaults: automaticPlurals defaults to false when not set"() {
        when:
        def params = CxStartTranslationCronJobForValuesParameters.builder()
                .localizedValues([])
                .build()

        then:
        params.automaticPlurals == false
    }

    // -----------------------------------------------------------------------
    // CxStartTranslationCronJobForItemsParameters
    // -----------------------------------------------------------------------

    def "ForItemsParameters builder copies items, attributes and translateProductFeatures"() {
        given:
        def item1 = Mock(ItemModel)
        def item2 = Mock(ItemModel)
        def attr = Mock(AttributeDescriptorModel)

        when:
        def params = CxStartTranslationCronJobForItemsParameters.builder()
                .items([item1, item2])
                .attributes([attr] as Set)
                .translateProductFeatures(true)
                .userHint("hint")
                .automaticPlurals(false)
                .build()

        then:
        params.items.size() == 2
        params.attributes.size() == 1
        params.translateProductFeatures == true
        params.userHint == "hint"
        params.automaticPlurals == false
    }

    def "ForItemsParameters defaults: translateProductFeatures defaults to false"() {
        when:
        def params = CxStartTranslationCronJobForItemsParameters.builder()
                .items([])
                .attributes([] as Set)
                .build()

        then:
        params.translateProductFeatures == false
    }

    // -----------------------------------------------------------------------
    // CxPerformTranslationParameters
    // -----------------------------------------------------------------------

    def "CxPerformTranslationParameters builder carries values and callback"() {
        given:
        def value = Mock(CxProposedTranslationModel)
        def callback = { pt -> }

        when:
        def params = CxPerformTranslationParameters.builder()
                .values([value])
                .afterTranslationCallback(callback)
                .userHint("hint")
                .build()

        then:
        params.values.size() == 1
        params.afterTranslationCallback != null
        params.userHint == "hint"
    }

    def "buildFromCronJob factory copies all cron-job fields into parameters"() {
        given:
        def srcLang = Mock(LanguageModel)
        def tgtLang = Mock(LanguageModel)
        def settings = Mock(CxTranslationServiceSettingsModel)
        def cronJob = Mock(CxTranslationCronJobModel)
        cronJob.sourceLanguage >> srcLang
        cronJob.targetLanguages >> [tgtLang]
        cronJob.hint >> "cron hint"
        cronJob.translationServiceSettings >> settings
        cronJob.skipTranslated >> Boolean.TRUE
        cronJob.saveForApproval >> Boolean.FALSE
        cronJob.isAutomaticPlurals() >> true

        when:
        def builder = CxPerformTranslationParameters.buildFromCronJob(cronJob)
        def params = builder.build()

        then:
        params.sourceLanguage == srcLang
        params.targetLanguages == [tgtLang]
        params.userHint == "cron hint"
        params.serviceSettings == settings
        params.skipTranslated == Boolean.TRUE
        params.saveForApproval == Boolean.FALSE
        params.automaticPlurals == true
    }

    def "buildFromCronJob afterTranslationCallback defaults to null when not explicitly set"() {
        given:
        def cronJob = Mock(CxTranslationCronJobModel)
        cronJob.sourceLanguage >> Mock(LanguageModel)
        cronJob.targetLanguages >> []
        cronJob.hint >> null
        cronJob.translationServiceSettings >> null
        cronJob.skipTranslated >> null
        cronJob.saveForApproval >> null
        cronJob.isAutomaticPlurals() >> false

        when:
        def params = CxPerformTranslationParameters.buildFromCronJob(cronJob).build()

        then:
        params.afterTranslationCallback == null
    }

    // -----------------------------------------------------------------------
    // Null tolerance
    // -----------------------------------------------------------------------

    def "saveForApproval and skipTranslated default to false when passed null (null-safety fix #14)"() {
        when:
        def params = CxStartTranslationCronJobForValuesParameters.builder()
                .saveForApproval(null)
                .skipTranslated(null)
                .localizedValues([])
                .build()

        then: "no NPE; null maps to false rather than staying nullable"
        params.saveForApproval == false
        params.skipTranslated == false
    }
}
