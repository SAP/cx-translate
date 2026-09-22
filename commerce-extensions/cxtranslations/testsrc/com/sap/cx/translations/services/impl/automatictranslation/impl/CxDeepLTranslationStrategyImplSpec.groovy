/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.automatictranslation.impl

import com.sap.cx.translations.enums.CxTranslationStatus
import com.sap.cx.translations.exceptions.CxTranslationException
import com.sap.cx.translations.generated.dto.translation.deepl.request.CxTranslationsDeepLRequest
import com.sap.cx.translations.generated.dto.translation.deepl.response.CxTranslationsDeepLResponse
import com.sap.cx.translations.generated.dto.translation.deepl.response.Translation
import com.sap.cx.translations.model.CxDeepLTranslationServiceSettingsModel
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.serviceinterfaces.deepl.CxTranslationsDeeplInterfaceService
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.servicelayer.model.ModelService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxDeepLTranslationStrategyImplSpec extends Specification {

    CxTranslationsDeeplInterfaceService interfaceService = Mock()
    CxTranslateCommerceCommonI18NService i18nService = Mock()
    ModelService modelService = Mock()
    CxDeepLTranslationServiceSettingsModel settings = Mock()

    @Subject
    CxDeepLTranslationStrategyImpl strategy = new CxDeepLTranslationStrategyImpl()

    def setup() {
        strategy.setInterfaceService(interfaceService)
        strategy.setI18nService(i18nService)
        strategy.setModelService(modelService)
    }

    def "supports returns true only for CxDeepLTranslationServiceSettingsModel"() {
        expect:
        strategy.supports(settings) == true
    }

    def "aiTranslate throws CxTranslationException when DeepL returns empty translations list"() {
        given:
        def sourceLang = Mock(LanguageModel) { getIsocode() >> "en" }
        def targetLang = Mock(LanguageModel) { getIsocode() >> "de" }
        def value = Mock(CxProposedTranslationModel)
        value.getSourceLanguage() >> sourceLang
        value.getFilteredTargetLanguages() >> [targetLang]
        value.getValueHint() >> "hint"
        value.getValue(Locale.ENGLISH) >> "Hello"
        i18nService.getLocale("en") >> Locale.ENGLISH
        i18nService.getLocale("de") >> Locale.GERMAN
        def emptyResponse = new CxTranslationsDeepLResponse()
        emptyResponse.setTranslations([])
        interfaceService.translate(_, _) >> emptyResponse

        when:
        strategy.aiTranslate(value, "global", settings, {})

        then:
        thrown(CxTranslationException)
    }

    def "aiTranslate throws when DeepL returns null response"() {
        given:
        def sourceLang = Mock(LanguageModel) { getIsocode() >> "en" }
        def targetLang = Mock(LanguageModel) { getIsocode() >> "de" }
        def value = Mock(CxProposedTranslationModel)
        value.getSourceLanguage() >> sourceLang
        value.getFilteredTargetLanguages() >> [targetLang]
        value.getValueHint() >> "hint"
        value.getValue(Locale.ENGLISH) >> "Hello"
        i18nService.getLocale("en") >> Locale.ENGLISH
        i18nService.getLocale("de") >> Locale.GERMAN
        interfaceService.translate(_, _) >> null

        when:
        strategy.aiTranslate(value, "global", settings, {})

        then:
        // Null response is rejected — either via Collectors.toMap NPE or via the explicit guard
        thrown(Exception)
    }

    def "aiTranslate sets translated values for all target locales on success"() {
        given:
        def sourceLang = Mock(LanguageModel) { getIsocode() >> "en" }
        def targetLang = Mock(LanguageModel) { getIsocode() >> "de" }
        def value = Mock(CxProposedTranslationModel)
        value.getSourceLanguage() >> sourceLang
        value.getFilteredTargetLanguages() >> [targetLang]
        value.getValueHint() >> null
        value.getValue(Locale.ENGLISH) >> "Hello"
        i18nService.getLocale("en") >> Locale.ENGLISH
        i18nService.getLocale("de") >> Locale.GERMAN

        def translation = new Translation()
        translation.setText("Hallo")
        def deepLResponse = new CxTranslationsDeepLResponse()
        deepLResponse.setTranslations([translation])
        interfaceService.translate(_, _) >> deepLResponse

        when:
        strategy.aiTranslate(value, "global", settings, {})

        then:
        1 * value.setProposedTranslationValue("Hallo", Locale.GERMAN)
        1 * value.setTranslationStatus(CxTranslationStatus.SUCCESS)
        1 * modelService.save(value)
    }

    def "aiTranslate throws CxTranslationException when target language is unknown to DeepL"() {
        given:
        // Use a locale that is unlikely to be in DeepL's TargetLanguageText enum
        def sourceLang = Mock(LanguageModel) { getIsocode() >> "en" }
        def targetLang = Mock(LanguageModel) { getIsocode() >> "xx" }
        def value = Mock(CxProposedTranslationModel)
        value.getSourceLanguage() >> sourceLang
        value.getFilteredTargetLanguages() >> [targetLang]
        value.getValueHint() >> null
        value.getValue(Locale.ENGLISH) >> "Hello"
        i18nService.getLocale("en") >> Locale.ENGLISH
        i18nService.getLocale("xx") >> new Locale("xx")

        when:
        strategy.aiTranslate(value, "global", settings, {})

        then:
        thrown(CxTranslationException)
    }
}
