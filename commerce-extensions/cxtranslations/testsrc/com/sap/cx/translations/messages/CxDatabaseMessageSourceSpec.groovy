/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.messages

import com.sap.cx.translations.facades.CxTranslationsFacade
import com.sap.cx.translations.labels.UsageType
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.commerceservices.i18n.CommerceCommonI18NService
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException
import de.hybris.platform.servicelayer.i18n.CommonI18NService
import de.hybris.platform.servicelayer.session.SessionExecutionBody
import de.hybris.platform.servicelayer.session.SessionService
import spock.lang.Specification
import spock.lang.Subject

import java.text.MessageFormat

@UnitTest
class CxDatabaseMessageSourceSpec extends Specification {

    CxTranslationsFacade facade = Mock()
    SessionService sessionService = Mock()
    CommerceCommonI18NService commerceCommonI18NService = Mock()
    CommonI18NService commonI18NService = Mock()

    @Subject
    CxDatabaseMessageSource source = new CxDatabaseMessageSource()

    def setup() {
        source.setTranslationsFacade(facade)
        source.setSessionService(sessionService)
        source.setCommerceCommonI18NService(commerceCommonI18NService)
        source.setCommonI18NService(commonI18NService)

        // executeInLocalView just runs the body
        sessionService.executeInLocalView(_ as SessionExecutionBody) >> { args ->
            (args[0] as SessionExecutionBody).execute()
        }
    }

    def "resolveCode returns null when language is unknown"() {
        given:
        commonI18NService.getLanguage("xx") >> { throw new UnknownIdentifierException("not found") }

        when:
        def result = source.resolveCode("any.key", new Locale("xx"))

        then:
        result == null
        0 * commerceCommonI18NService.setCurrentLanguage(_)
        0 * facade.getLabel(_, _)
    }

    def "resolveCode returns MessageFormat when label is found"() {
        given:
        def lang = Mock(LanguageModel)
        def previous = Mock(LanguageModel)
        commonI18NService.getLanguage("en") >> lang
        commerceCommonI18NService.getCurrentLanguage() >> previous
        facade.getLabel("greeting", UsageType.BACKEND) >> "Hello {0}"

        when:
        def result = source.resolveCode("greeting", Locale.ENGLISH)

        then:
        result instanceof MessageFormat
        1 * commerceCommonI18NService.setCurrentLanguage(lang)
        1 * commerceCommonI18NService.setCurrentLanguage(previous)
    }

    def "resolveCode returns null when facade has no label"() {
        given:
        def lang = Mock(LanguageModel)
        commonI18NService.getLanguage("en") >> lang
        commerceCommonI18NService.getCurrentLanguage() >> Mock(LanguageModel)
        facade.getLabel("missing", UsageType.BACKEND) >> null

        expect:
        source.resolveCode("missing", Locale.ENGLISH) == null
    }

    def "resolveCode skips restoring language when previous was null"() {
        given:
        def lang = Mock(LanguageModel)
        commonI18NService.getLanguage("en") >> lang
        commerceCommonI18NService.getCurrentLanguage() >> null
        facade.getLabel("x", UsageType.BACKEND) >> "Hi"

        when:
        source.resolveCode("x", Locale.ENGLISH)

        then:
        1 * commerceCommonI18NService.setCurrentLanguage(lang)
        // No second call to setCurrentLanguage(null)
        0 * commerceCommonI18NService.setCurrentLanguage(null)
    }

    def "resolveCode restores original language even when facade throws"() {
        given:
        def lang = Mock(LanguageModel)
        def previous = Mock(LanguageModel)
        commonI18NService.getLanguage("en") >> lang
        commerceCommonI18NService.getCurrentLanguage() >> previous
        facade.getLabel(_, _) >> { throw new RuntimeException("db down") }

        when:
        source.resolveCode("x", Locale.ENGLISH)

        then:
        thrown(RuntimeException)
        1 * commerceCommonI18NService.setCurrentLanguage(previous)
    }
}
