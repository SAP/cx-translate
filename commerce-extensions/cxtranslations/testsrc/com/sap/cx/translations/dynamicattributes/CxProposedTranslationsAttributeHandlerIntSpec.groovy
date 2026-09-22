/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.dynamicattributes

import com.sap.cx.translations.model.CxLocalizedValueModel
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.i18n.CommonI18NService
import de.hybris.platform.servicelayer.model.ModelService
import jakarta.annotation.Resource

/**
 * Integration tests for {@link CxProposedTranslationsAttributeHandler} — the dynamic
 * attribute behind {@code CxLocalizedValueModel.proposedTranslations}.
 *
 * <p>Tests exercise the handler via the model API (calling {@code getProposedTranslations()})
 * to verify the full wiring through the Spring context.</p>
 *
 * <p>Note: {@code CxProposedTranslation} has a unique index on {@code (key, translationCronJob)},
 * so two records sharing the same key must belong to different cron jobs.</p>
 */
@IntegrationTest
class CxProposedTranslationsAttributeHandlerIntSpec extends ServicelayerTransactionalSpockSpecification {

    @Resource
    ModelService modelService

    @Resource
    CommonI18NService commonI18NService

    private CxTranslationTestDataBuilder builder
    private LanguageModel langEn
    private LanguageModel langDe

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService)
        langEn = builder.createLanguage("en-pt")
        langDe = builder.createLanguage("de-pt")
    }

    // -----------------------------------------------------------------------
    // proposedTranslations (via CxLocalizedValueModel.getProposedTranslations)
    // -----------------------------------------------------------------------

    def "getProposedTranslations returns all proposed translations matching the localized value key"() {
        given: "a localized value and two proposed translations with the same key but different cron jobs"
        def api = builder.createAiServiceApi("api-pt-multi-1")
        def aiModel = builder.createAiModel("model-pt-multi-1")
        def settings = builder.createAiServiceSettings("settings-pt-multi-1", api, aiModel)
        def cronJob1 = builder.createTranslationCronJob("cj-pt-multi-1a", langEn, [langDe], settings)
        def cronJob2 = builder.createTranslationCronJob("cj-pt-multi-1b", langEn, [langDe], settings)

        def localizedValue = builder.createLocalizedValue("product.name.pt-multi")
        def pt1 = builder.createProposedTranslation("product.name.pt-multi")
        pt1.setTranslationCronJob(cronJob1)
        modelService.save(pt1)
        def pt2 = builder.createProposedTranslation("product.name.pt-multi")
        pt2.setTranslationCronJob(cronJob2)
        modelService.save(pt2)

        when:
        def result = localizedValue.getProposedTranslations()

        then:
        result.size() == 2
        result.every { it.key == "product.name.pt-multi" }
    }

    def "getProposedTranslations returns only translations whose key exactly matches"() {
        given: "proposed translations for two different keys"
        def localizedValue = builder.createLocalizedValue("product.desc.pt-exact")
        builder.createProposedTranslation("product.desc.pt-exact")
        builder.createProposedTranslation("product.desc.pt-other")

        when:
        def result = localizedValue.getProposedTranslations()

        then:
        result.size() == 1
        result.first().key == "product.desc.pt-exact"
    }

    def "getProposedTranslations returns empty collection when no proposed translations exist for the key"() {
        given:
        def localizedValue = builder.createLocalizedValue("product.name.pt-no-translations")

        when:
        def result = localizedValue.getProposedTranslations()

        then:
        result.isEmpty()
    }

    def "proposedTranslations handler set throws UnsupportedOperationException"() {
        given:
        def handler = new CxProposedTranslationsAttributeHandler()
        handler.setCxProposedTranslationDao(applicationContext.getBean("cxProposedTranslationDao"))
        def localizedValue = modelService.create(CxLocalizedValueModel)

        when:
        handler.set(localizedValue, [])

        then:
        thrown(UnsupportedOperationException)
    }
}
