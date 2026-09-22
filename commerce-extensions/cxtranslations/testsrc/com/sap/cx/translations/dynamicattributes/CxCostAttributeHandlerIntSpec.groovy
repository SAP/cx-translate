/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.dynamicattributes

import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.model.CxTranslationCronJobModel
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import jakarta.annotation.Resource

/**
 * Integration tests for the three cost dynamic attribute handlers:
 * <ul>
 *   <li>{@link CxAiServiceCostAttributeHandler} — {@code CxAiTranslationServiceSettingsModel.totalCost}</li>
 *   <li>{@link CxTranslationCronJobCostAttributeHandler} — {@code CxTranslationCronJobModel.totalCost}</li>
 *   <li>{@link CxProposedTranslationCostAttributeHandler} — {@code CxProposedTranslationModel.totalCost}</li>
 * </ul>
 *
 * <p>All three handlers follow the same formula: for each metric key present in
 * {@code usageInfo}, multiply its value by the corresponding entry in
 * {@code usageMetricsCost} and sum the results.</p>
 */
@IntegrationTest
class CxCostAttributeHandlerIntSpec extends ServicelayerTransactionalSpockSpecification {

    @Resource
    ModelService modelService

    private CxTranslationTestDataBuilder builder
    private LanguageModel langEn
    private LanguageModel langDe

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService)
        langEn = builder.createLanguage("en-cost")
        langDe = builder.createLanguage("de-cost")
    }

    // -----------------------------------------------------------------------
    // CxAiServiceCostAttributeHandler (via CxAiTranslationServiceSettingsModel.totalCost)
    // -----------------------------------------------------------------------

    def "AiServiceSettings.totalCost computes sum of usage multiplied by cost per unit"() {
        given:
        def api = builder.createAiServiceApi("api-cost-ai-1")
        def aiModel = builder.createAiModel("model-cost-ai-1")
        def settings = builder.createAiServiceSettings("settings-cost-ai-1", api, aiModel,
                ["input_tokens": 0.01d, "output_tokens": 0.03d])
        settings.setUsageInfo(["input_tokens": 1000d, "output_tokens": 500d])
        modelService.save(settings)

        when:
        double cost = settings.getTotalCost()

        then: "1000 * 0.01 + 500 * 0.03 = 10.0 + 15.0 = 25.0"
        cost == 25.0d
    }

    def "AiServiceSettings.totalCost returns 0.0 when usageInfo is null"() {
        given:
        def api = builder.createAiServiceApi("api-cost-ai-null-usage")
        def aiModel = builder.createAiModel("model-cost-ai-null-usage")
        def settings = builder.createAiServiceSettings("settings-cost-ai-null-usage", api, aiModel,
                ["input_tokens": 0.01d])

        when:
        double cost = settings.getTotalCost()

        then:
        cost == 0.0d
    }

    def "AiServiceSettings.totalCost returns 0.0 when usageMetricsCost is null"() {
        given:
        def api = builder.createAiServiceApi("api-cost-ai-null-cost")
        def aiModel = builder.createAiModel("model-cost-ai-null-cost")
        def settings = builder.createAiServiceSettings("settings-cost-ai-null-cost", api, aiModel)
        settings.setUsageInfo(["input_tokens": 1000d])
        modelService.save(settings)

        when:
        double cost = settings.getTotalCost()

        then:
        cost == 0.0d
    }

    def "AiServiceSettings.totalCost returns 0.0 for a metric key absent from usageMetricsCost"() {
        given: "usageInfo contains 'unknown_metric' which has no cost entry"
        def api = builder.createAiServiceApi("api-cost-ai-unknown-metric")
        def aiModel = builder.createAiModel("model-cost-ai-unknown-metric")
        def settings = builder.createAiServiceSettings("settings-cost-ai-unknown-metric", api, aiModel,
                ["input_tokens": 0.01d])
        settings.setUsageInfo(["unknown_metric": 999d])
        modelService.save(settings)

        when:
        double cost = settings.getTotalCost()

        then:
        cost == 0.0d
    }

    def "AiServiceSettings.totalCost handler set throws UnsupportedOperationException"() {
        given:
        def handler = new CxAiServiceCostAttributeHandler()
        def settings = modelService.create(CxAiTranslationServiceSettingsModel)

        when:
        handler.set(settings, 99.0d)

        then:
        thrown(UnsupportedOperationException)
    }

    // -----------------------------------------------------------------------
    // CxTranslationCronJobCostAttributeHandler (via CxTranslationCronJobModel.totalCost)
    // -----------------------------------------------------------------------

    def "CronJob.totalCost computes sum of usage multiplied by cost per unit from AI settings"() {
        given:
        def api = builder.createAiServiceApi("api-cost-cj-1")
        def aiModel = builder.createAiModel("model-cost-cj-1")
        def settings = builder.createAiServiceSettings("settings-cost-cj-1", api, aiModel,
                ["input_tokens": 0.002d, "output_tokens": 0.006d])
        def cronJob = builder.createTranslationCronJob("cj-cost-1", langEn, [langDe], settings)
        cronJob.setUsageInfo(["input_tokens": 2000d, "output_tokens": 800d])
        modelService.save(cronJob)

        when:
        double cost = cronJob.getTotalCost()

        then: "2000 * 0.002 + 800 * 0.006 = 4.0 + 4.8 = 8.8"
        cost == 8.8d
    }

    def "CronJob.totalCost returns 0.0 when usageInfo is null"() {
        given:
        def api = builder.createAiServiceApi("api-cost-cj-null-usage")
        def aiModel = builder.createAiModel("model-cost-cj-null-usage")
        def settings = builder.createAiServiceSettings("settings-cost-cj-null-usage", api, aiModel,
                ["input_tokens": 0.002d])
        def cronJob = builder.createTranslationCronJob("cj-cost-null-usage", langEn, [langDe], settings)

        when:
        double cost = cronJob.getTotalCost()

        then:
        cost == 0.0d
    }

    def "CronJob.totalCost returns 0.0 when usageInfo is empty"() {
        given:
        def api = builder.createAiServiceApi("api-cost-cj-empty-usage")
        def aiModel = builder.createAiModel("model-cost-cj-empty-usage")
        def settings = builder.createAiServiceSettings("settings-cost-cj-empty-usage", api, aiModel,
                ["input_tokens": 0.002d])
        def cronJob = builder.createTranslationCronJob("cj-cost-empty-usage", langEn, [langDe], settings)
        cronJob.setUsageInfo([:])
        modelService.save(cronJob)

        when:
        double cost = cronJob.getTotalCost()

        then:
        cost == 0.0d
    }

    def "CronJob.totalCost returns 0.0 when settings are not AI settings"() {
        given: "a cron job with no settings"
        def cronJob = builder.createTranslationCronJob("cj-cost-no-settings", langEn, [langDe])
        cronJob.setUsageInfo(["input_tokens": 500d])
        modelService.save(cronJob)

        when:
        double cost = cronJob.getTotalCost()

        then:
        cost == 0.0d
    }

    def "CronJob.totalCost handler set throws UnsupportedOperationException"() {
        given:
        def handler = new CxTranslationCronJobCostAttributeHandler()
        def cronJob = modelService.create(CxTranslationCronJobModel)

        when:
        handler.set(cronJob, 42.0d)

        then:
        thrown(UnsupportedOperationException)
    }

    // -----------------------------------------------------------------------
    // CxProposedTranslationCostAttributeHandler (via CxProposedTranslationModel.totalCost)
    // -----------------------------------------------------------------------

    def "ProposedTranslation.totalCost computes sum of usage multiplied by cost per unit from AI settings"() {
        given:
        def api = builder.createAiServiceApi("api-cost-pt-1")
        def aiModel = builder.createAiModel("model-cost-pt-1")
        def settings = builder.createAiServiceSettings("settings-cost-pt-1", api, aiModel,
                ["input_tokens": 0.005d, "output_tokens": 0.015d])
        def cronJob = builder.createTranslationCronJob("cj-cost-pt-1", langEn, [langDe], settings)
        def proposed = builder.createProposedTranslation("key-cost-pt-1")
        proposed.setTranslationCronJob(cronJob)
        proposed.setUsageInfo(["input_tokens": 400d, "output_tokens": 200d])
        modelService.save(proposed)

        when:
        double cost = proposed.getTotalCost()

        then: "400 * 0.005 + 200 * 0.015 = 2.0 + 3.0 = 5.0"
        cost == 5.0d
    }

    def "ProposedTranslation.totalCost returns 0.0 when usageInfo is null"() {
        given:
        def api = builder.createAiServiceApi("api-cost-pt-null-usage")
        def aiModel = builder.createAiModel("model-cost-pt-null-usage")
        def settings = builder.createAiServiceSettings("settings-cost-pt-null-usage", api, aiModel,
                ["input_tokens": 0.005d])
        def cronJob = builder.createTranslationCronJob("cj-cost-pt-null-usage", langEn, [langDe], settings)
        def proposed = builder.createProposedTranslation("key-cost-pt-null-usage")
        proposed.setTranslationCronJob(cronJob)
        modelService.save(proposed)

        when:
        double cost = proposed.getTotalCost()

        then:
        cost == 0.0d
    }

    def "ProposedTranslation.totalCost returns 0.0 when usageMetricsCost is null on settings"() {
        given: "settings without cost-per-unit map"
        def api = builder.createAiServiceApi("api-cost-pt-null-cost")
        def aiModel = builder.createAiModel("model-cost-pt-null-cost")
        def settings = builder.createAiServiceSettings("settings-cost-pt-null-cost", api, aiModel)
        def cronJob = builder.createTranslationCronJob("cj-cost-pt-null-cost", langEn, [langDe], settings)
        def proposed = builder.createProposedTranslation("key-cost-pt-null-cost")
        proposed.setTranslationCronJob(cronJob)
        proposed.setUsageInfo(["input_tokens": 400d])
        modelService.save(proposed)

        when:
        double cost = proposed.getTotalCost()

        then:
        cost == 0.0d
    }

    def "ProposedTranslation.totalCost returns 0.0 when cron job is null"() {
        given:
        def proposed = builder.createProposedTranslation("key-cost-pt-null-cj")

        when:
        double cost = proposed.getTotalCost()

        then:
        cost == 0.0d
    }

    def "ProposedTranslation.totalCost handler set throws UnsupportedOperationException"() {
        given:
        def handler = new CxProposedTranslationCostAttributeHandler()
        def proposed = modelService.create(CxProposedTranslationModel)

        when:
        handler.set(proposed, 7.0d)

        then:
        thrown(UnsupportedOperationException)
    }
}
