/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.automatictranslation.impl

import com.sap.cx.translations.enums.CxTranslateServiceLoggingLevel
import com.sap.cx.translations.enums.CxTranslationStatus
import com.sap.cx.translations.generated.dto.translation.response.CxTranslationOutputText
import com.sap.cx.translations.generated.dto.translation.response.CxTranslationResponseDto
import com.sap.cx.translations.model.CxAiTranslationServiceApiModel
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.serviceinterfaces.ai.CxTranslationService
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.servicelayer.model.ModelService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxAiTranslationStrategyImplSpec extends Specification {

    CxTranslationService aiTranslationService = Mock()
    ModelService modelService = Mock()
    CxTranslateCommerceCommonI18NService i18nService = Mock()
    CxAiTranslationServiceSettingsModel settings = Mock()
    CxAiTranslationServiceApiModel apiModel = Mock()
    LanguageModel sourceLang = Mock()

    @Subject
    CxAiTranslationStrategyImpl strategy = new CxAiTranslationStrategyImpl()

    def setup() {
        strategy.setAiTranslationService(aiTranslationService)
        strategy.setModelService(modelService)
        strategy.setI18nService(i18nService)
        settings.getAiServiceApi() >> apiModel
        sourceLang.getIsocode() >> "en"
        i18nService.getLocale("en") >> Locale.ENGLISH
    }

    private CxProposedTranslationModel value(String key, String sourceValue, List<LanguageModel> targets = []) {
        def v = Mock(CxProposedTranslationModel)
        v.getKey() >> key
        v.getValue(Locale.ENGLISH) >> sourceValue
        v.getSourceLanguage() >> sourceLang
        v.getFilteredTargetLanguages() >> targets
        v.isAutomaticPlurals() >> false
        v.getValueHint() >> ""
        v
    }

    private CxTranslationOutputText output(String key, Map<String, String> langMap = [:], String feedback = null) {
        def o = new CxTranslationOutputText()
        o.setKey(key)
        o.setLanguageToValueMap(langMap)
        if (feedback != null) o.setFeedback(feedback)
        o
    }

    private CxTranslationResponseDto response(List<CxTranslationOutputText> translations,
                                              Map<String, Double> usage = [:],
                                              CxTranslationResponseDto.Status status = CxTranslationResponseDto.Status.SUCCESS) {
        def r = new CxTranslationResponseDto()
        r.setTranslations(translations)
        r.setUsageInfo(usage)
        r.setStatus(status)
        r
    }

    // -------------------------------------------------------------------------
    // supports
    // -------------------------------------------------------------------------

    def "supports returns true only for CxAiTranslationServiceSettingsModel"() {
        expect:
        strategy.supports(settings) == true
    }

    // -------------------------------------------------------------------------
    // aiTranslate — empty values returns immediately
    // -------------------------------------------------------------------------

    def "aiTranslate returns immediately when values list is empty"() {
        when:
        strategy.aiTranslate([] as List, "hint", settings, {})

        then:
        0 * aiTranslationService.translate(_, _)
    }

    // -------------------------------------------------------------------------
    // aiTranslate — mixed source languages rejected
    // -------------------------------------------------------------------------

    def "aiTranslate throws when batch contains values with different source languages"() {
        given:
        def otherLang = Mock(LanguageModel) { getIsocode() >> "de" }
        def v1 = value("k1", "Hello")
        def v2 = Mock(CxProposedTranslationModel) { getSourceLanguage() >> otherLang }

        when:
        strategy.aiTranslate([v1, v2], "hint", settings, {})

        then:
        thrown(IllegalArgumentException)
    }

    // -------------------------------------------------------------------------
    // aiTranslate — successful translation applies result to all values sharing a key
    // -------------------------------------------------------------------------

    def "aiTranslate applies translation result to every value sharing the same key (no silent drop)"() {
        given:
        def deLang = Mock(LanguageModel) { getIsocode() >> "de" }
        i18nService.getLocale("de") >> Locale.GERMAN
        // Two values with the same key — e.g. variant rows
        def v1 = value("shared.key", "Hello", [deLang])
        def v2 = value("shared.key", "Hello", [deLang])
        settings.getMaxCharactersInRequest() >> null
        apiModel.getLoggingLevel() >> CxTranslateServiceLoggingLevel.NEVER
        aiTranslationService.translate(_, _) >> response([output("shared.key", [de: "Hallo"])])

        when:
        strategy.aiTranslate([v1, v2], "hint", settings, {})

        then:
        // BOTH values get the translated language map applied (previously only first was updated)
        1 * v1.setProposedTranslationValue("Hallo", Locale.GERMAN)
        1 * v2.setProposedTranslationValue("Hallo", Locale.GERMAN)
        1 * modelService.save(v1)
        1 * modelService.save(v2)
    }

    // -------------------------------------------------------------------------
    // aiTranslate — ERROR response marks values as ERROR
    // -------------------------------------------------------------------------

    def "aiTranslate marks values as ERROR when AI returns ERROR status"() {
        given:
        def deLang = Mock(LanguageModel) { getIsocode() >> "de" }
        i18nService.getLocale("de") >> Locale.GERMAN
        def v = value("k", "Hello", [deLang])
        settings.getMaxCharactersInRequest() >> null
        apiModel.getLoggingLevel() >> CxTranslateServiceLoggingLevel.NEVER
        aiTranslationService.translate(_, _) >> response([], [:], CxTranslationResponseDto.Status.ERROR)

        when:
        strategy.aiTranslate([v], "hint", settings, {})

        then:
        1 * v.setTranslationStatus(CxTranslationStatus.ERROR)
    }

    // -------------------------------------------------------------------------
    // aiTranslate — exception during translation marks all values in batch as ERROR
    // -------------------------------------------------------------------------

    def "aiTranslate marks all batch values as ERROR when translation throws"() {
        given:
        def deLang = Mock(LanguageModel) { getIsocode() >> "de" }
        i18nService.getLocale("de") >> Locale.GERMAN
        def v = value("k", "Hello", [deLang])
        v.getNotes() >> null
        settings.getMaxCharactersInRequest() >> null
        aiTranslationService.translate(_, _) >> { throw new RuntimeException("boom") }

        when:
        strategy.aiTranslate([v], "hint", settings, {})

        then:
        1 * v.setTranslationStatus(CxTranslationStatus.ERROR)
        1 * modelService.save(v)
    }

    // -------------------------------------------------------------------------
    // aiTranslate — unknown key in response throws IllegalStateException
    // -------------------------------------------------------------------------

    def "aiTranslate fails when response contains a key that wasn't part of the request"() {
        given:
        def deLang = Mock(LanguageModel) { getIsocode() >> "de" }
        i18nService.getLocale("de") >> Locale.GERMAN
        def v = value("k1", "Hello", [deLang])
        v.getNotes() >> null
        settings.getMaxCharactersInRequest() >> null
        apiModel.getLoggingLevel() >> CxTranslateServiceLoggingLevel.NEVER
        aiTranslationService.translate(_, _) >> response([output("unknown_key", [de: "Hallo"])])

        when:
        strategy.aiTranslate([v], "hint", settings, {})

        then:
        // The IllegalStateException is caught by the outer try and marks value as ERROR
        1 * v.setTranslationStatus(CxTranslationStatus.ERROR)
    }

    // -------------------------------------------------------------------------
    // aiTranslate — empty source values (totalInputLength==0) does not produce NaN/Infinity usage
    // -------------------------------------------------------------------------

    def "aiTranslate does not produce NaN/Infinity usage when all values have empty source text"() {
        given:
        def deLang = Mock(LanguageModel) { getIsocode() >> "de" }
        i18nService.getLocale("de") >> Locale.GERMAN
        def v = value("k", "", [deLang])  // empty source
        settings.getMaxCharactersInRequest() >> null
        apiModel.getLoggingLevel() >> CxTranslateServiceLoggingLevel.NEVER
        aiTranslationService.translate(_, _) >> response([output("k", [de: "Hallo"])], [tokens: 100.0d])

        when:
        strategy.aiTranslate([v], "hint", settings, {})

        then:
        // estimateUsageInfo guard returns empty map when totalInputLength==0
        1 * v.setUsageInfo({ Map m -> m.isEmpty() })
    }
}
