/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.occ.controllers

import com.sap.cx.translations.exceptions.CxTranslationException
import com.sap.cx.translations.facades.CxTranslationsFacade
import com.sap.cx.translations.labels.CxLocalizedLabel
import com.sap.cx.translations.labels.UsageType
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException
import de.hybris.platform.servicelayer.i18n.CommonI18NService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationsControllerSpec extends Specification {

    CxTranslationsFacade translationsFacade = Mock()
    CommonI18NService commonI18NService = Mock()

    @Subject
    CxTranslationsController controller = new CxTranslationsController()

    def setup() {
        controller.translationsFacade = translationsFacade
        controller.commonI18NService = commonI18NService
    }

    // -------------------------------------------------------------------------
    // getTranslations — public chunk endpoint
    // -------------------------------------------------------------------------

    def "getTranslations sets session language and returns labels map"() {
        given:
        def language = Mock(LanguageModel)
        commonI18NService.getLanguage("en") >> language
        translationsFacade.getLabelsMap("ns", UsageType.FRONTEND) >> ["k": "v"]

        when:
        def result = controller.getTranslations("ns", "en")

        then:
        result == ["k": "v"]
        1 * commonI18NService.setCurrentLanguage(language)
    }

    def "getTranslations throws CxTranslationException when language code is blank"() {
        when:
        controller.getTranslations("ns", "")

        then:
        thrown(CxTranslationException)
        0 * commonI18NService.setCurrentLanguage(_)
    }

    def "getTranslations throws CxTranslationException when language is unknown"() {
        given:
        commonI18NService.getLanguage("xx") >> { throw new UnknownIdentifierException("not found") }

        when:
        controller.getTranslations("ns", "xx")

        then:
        thrown(CxTranslationException)
        0 * commonI18NService.setCurrentLanguage(_)
    }

    // -------------------------------------------------------------------------
    // getStagedTranslations
    // -------------------------------------------------------------------------

    def "getStagedTranslations bypasses cache and returns labels map"() {
        given:
        def language = Mock(LanguageModel)
        commonI18NService.getLanguage("de") >> language
        translationsFacade.getLabelsMap("ns", false, UsageType.FRONTEND) >> ["a": "b"]

        when:
        def result = controller.getStagedTranslations("ns", "de")

        then:
        result == ["a": "b"]
        1 * commonI18NService.setCurrentLanguage(language)
    }

    // -------------------------------------------------------------------------
    // editLabel
    // -------------------------------------------------------------------------

    def "editLabel delegates to facade for a valid label"() {
        given:
        def label = Mock(CxLocalizedLabel) {
            getKey() >> "btn.ok"
            getBundle() >> "checkout"
            getValue() >> "OK"
        }

        when:
        controller.editLabel(label)

        then:
        1 * translationsFacade.editLabel(label)
    }

    def "editLabel throws CxTranslationException when label is null"() {
        when:
        controller.editLabel(null)

        then:
        thrown(CxTranslationException)
        0 * translationsFacade.editLabel(_)
    }

    def "editLabel throws CxTranslationException when key is blank"() {
        given:
        def label = Mock(CxLocalizedLabel) { getKey() >> ""; getBundle() >> "b" }

        when:
        controller.editLabel(label)

        then:
        thrown(CxTranslationException)
        0 * translationsFacade.editLabel(_)
    }

    def "editLabel throws CxTranslationException when bundle is blank"() {
        given:
        def label = Mock(CxLocalizedLabel) { getKey() >> "k"; getBundle() >> "" }

        when:
        controller.editLabel(label)

        then:
        thrown(CxTranslationException)
        0 * translationsFacade.editLabel(_)
    }

    def "editLabel wraps facade exception as CxTranslationException"() {
        given:
        def label = Mock(CxLocalizedLabel) { getKey() >> "k"; getBundle() >> "b" }
        translationsFacade.editLabel(label) >> { throw new RuntimeException("boom") }

        when:
        controller.editLabel(label)

        then:
        thrown(CxTranslationException)
    }

    // -------------------------------------------------------------------------
    // importTranslations
    // -------------------------------------------------------------------------

    def "importTranslations flattens nested map and delegates to facade"() {
        given:
        Map<String, Object> body = ["checkout": ["btn": ["ok": "OK", "cancel": "Cancel"]]]
        Map<String, Map<String, String>> captured = null
        translationsFacade.importLabels(_, _) >> { Map m, String l -> captured = m }

        when:
        controller.importTranslations("en", body)

        then:
        captured == ["checkout": ["btn.ok": "OK", "btn.cancel": "Cancel"]]
    }

    def "importTranslations throws CxTranslationException when lang is blank"() {
        when:
        controller.importTranslations("", ["ns": ["k": "v"]])

        then:
        thrown(CxTranslationException)
        0 * translationsFacade.importLabels(_, _)
    }

    def "importTranslations throws CxTranslationException when translations body is null"() {
        when:
        controller.importTranslations("en", null)

        then:
        thrown(CxTranslationException)
        0 * translationsFacade.importLabels(_, _)
    }

    def "importTranslations throws CxTranslationException when namespace value is not a map"() {
        when:
        controller.importTranslations("en", ["ns": "not-a-map"] as Map<String, Object>)

        then:
        thrown(CxTranslationException)
        0 * translationsFacade.importLabels(_, _)
    }

    def "importTranslations wraps facade exception as CxTranslationException"() {
        given:
        translationsFacade.importLabels(_, _) >> { throw new IllegalStateException("import disabled") }

        when:
        controller.importTranslations("en", ["ns": ["k": "v"]])

        then:
        thrown(CxTranslationException)
    }

    // -------------------------------------------------------------------------
    // publishTranslations
    // -------------------------------------------------------------------------

    def "publishTranslations returns without exception on success"() {
        given:
        translationsFacade.publishTranslations() >> true

        when:
        controller.publishTranslations()

        then:
        noExceptionThrown()
    }

    def "publishTranslations throws CxTranslationException when facade returns false"() {
        given:
        translationsFacade.publishTranslations() >> false

        when:
        controller.publishTranslations()

        then:
        thrown(CxTranslationException)
    }

    def "publishTranslations wraps facade exception as CxTranslationException"() {
        given:
        translationsFacade.publishTranslations() >> { throw new RuntimeException("boom") }

        when:
        controller.publishTranslations()

        then:
        thrown(CxTranslationException)
    }

    // -------------------------------------------------------------------------
    // labelValue
    // -------------------------------------------------------------------------

    def "labelValue returns value from facade with FRONTEND usage type"() {
        given:
        translationsFacade.getStagedLabel("key1", UsageType.FRONTEND) >> "Hello"

        expect:
        controller.labelValue("key1") == "Hello"
    }

    def "labelValue returns empty string when facade returns null"() {
        given:
        translationsFacade.getStagedLabel("missing", UsageType.FRONTEND) >> null

        expect:
        controller.labelValue("missing") == ""
    }

    def "labelValue wraps facade exception as CxTranslationException"() {
        given:
        translationsFacade.getStagedLabel(_, _) >> { throw new RuntimeException("boom") }

        when:
        controller.labelValue("k")

        then:
        thrown(CxTranslationException)
    }

    // -------------------------------------------------------------------------
    // keySuggestions
    // -------------------------------------------------------------------------

    def "keySuggestions returns list from facade"() {
        given:
        translationsFacade.getKeySuggestions("btn") >> ["btn.ok", "btn.cancel"]

        expect:
        controller.keySuggestions("btn") == ["btn.ok", "btn.cancel"]
    }

    def "keySuggestions wraps facade exception as CxTranslationException"() {
        given:
        translationsFacade.getKeySuggestions(_) >> { throw new RuntimeException("boom") }

        when:
        controller.keySuggestions("btn")

        then:
        thrown(CxTranslationException)
    }

    // -------------------------------------------------------------------------
    // isAllowedToTranslate
    // -------------------------------------------------------------------------

    def "isAllowedToTranslate returns facade result"() {
        given:
        translationsFacade.isFrontendTranslationAllowedForUser() >> true

        expect:
        controller.isAllowedToTranslate() == true
    }

    def "isAllowedToTranslate wraps facade exception as CxTranslationException"() {
        given:
        translationsFacade.isFrontendTranslationAllowedForUser() >> { throw new RuntimeException("boom") }

        when:
        controller.isAllowedToTranslate()

        then:
        thrown(CxTranslationException)
    }
}
