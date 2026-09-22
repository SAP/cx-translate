/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.dynamicattributes

import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import jakarta.annotation.Resource

/**
 * Integration tests for {@link CxSourceLanguageForProposedTranslationAttributeHandler}
 * and {@link CxTargetLanguagesForProposedTranslationAttributeHandler} — the dynamic
 * attributes behind {@code CxProposedTranslationModel.sourceLanguage} and
 * {@code .initialTargetLanguages}.
 */
@IntegrationTest
class CxProposedTranslationLanguageAttributeHandlerIntSpec extends ServicelayerTransactionalSpockSpecification {

    @Resource
    ModelService modelService

    private CxTranslationTestDataBuilder builder
    private LanguageModel langEn
    private LanguageModel langDe

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService)
        langEn = builder.createLanguage("en-lang")
        langDe = builder.createLanguage("de-lang")
    }

    // -----------------------------------------------------------------------
    // sourceLanguage
    // -----------------------------------------------------------------------

    def "getSourceLanguage returns the source language from the related cron job"() {
        given:
        def api = builder.createAiServiceApi("api-lang-src-1")
        def aiModel = builder.createAiModel("model-lang-src-1")
        def settings = builder.createAiServiceSettings("settings-lang-src-1", api, aiModel)
        def cronJob = builder.createTranslationCronJob("cronJob-lang-src-1", langEn, [langDe], settings)
        def proposed = builder.createProposedTranslation("key-lang-src-1")
        proposed.setTranslationCronJob(cronJob)
        modelService.save(proposed)

        when:
        def result = proposed.getSourceLanguage()

        then:
        result == langEn
    }

    def "getSourceLanguage returns null when proposed translation has no cron job"() {
        given:
        def proposed = builder.createProposedTranslation("key-lang-src-null-cronjob")

        when:
        def result = proposed.getSourceLanguage()

        then:
        result == null
    }

    def "sourceLanguage handler set throws UnsupportedOperationException"() {
        given:
        def handler = new CxSourceLanguageForProposedTranslationAttributeHandler()
        def proposed = modelService.create(CxProposedTranslationModel)

        when:
        handler.set(proposed, langEn)

        then:
        thrown(UnsupportedOperationException)
    }

    // -----------------------------------------------------------------------
    // initialTargetLanguages
    // -----------------------------------------------------------------------

    def "getInitialTargetLanguages returns target languages from the related cron job"() {
        given:
        def api = builder.createAiServiceApi("api-lang-tgt-1")
        def aiModel = builder.createAiModel("model-lang-tgt-1")
        def settings = builder.createAiServiceSettings("settings-lang-tgt-1", api, aiModel)
        def cronJob = builder.createTranslationCronJob("cronJob-lang-tgt-1", langEn, [langDe], settings)
        def proposed = builder.createProposedTranslation("key-lang-tgt-1")
        proposed.setTranslationCronJob(cronJob)
        modelService.save(proposed)

        when:
        def result = proposed.getInitialTargetLanguages()

        then:
        result.size() == 1
        result.first() == langDe
    }

    def "getInitialTargetLanguages returns multiple languages when cron job has multiple targets"() {
        given:
        def langFr = builder.createLanguage("fr-lang")
        def api = builder.createAiServiceApi("api-lang-tgt-multi")
        def aiModel = builder.createAiModel("model-lang-tgt-multi")
        def settings = builder.createAiServiceSettings("settings-lang-tgt-multi", api, aiModel)
        def cronJob = builder.createTranslationCronJob("cronJob-lang-tgt-multi", langEn, [langDe, langFr], settings)
        def proposed = builder.createProposedTranslation("key-lang-tgt-multi")
        proposed.setTranslationCronJob(cronJob)
        modelService.save(proposed)

        when:
        def result = proposed.getInitialTargetLanguages()

        then:
        result.size() == 2
        result.collect { it.isocode }.containsAll(["de-lang", "fr-lang"])
    }

    def "getInitialTargetLanguages returns empty collection when proposed translation has no cron job"() {
        given:
        def proposed = builder.createProposedTranslation("key-lang-tgt-null-cronjob")

        when:
        def result = proposed.getInitialTargetLanguages()

        then:
        result.isEmpty()
    }

    def "initialTargetLanguages handler set throws UnsupportedOperationException"() {
        given:
        def handler = new CxTargetLanguagesForProposedTranslationAttributeHandler()
        def proposed = modelService.create(CxProposedTranslationModel)

        when:
        handler.set(proposed, [langDe])

        then:
        thrown(UnsupportedOperationException)
    }
}
