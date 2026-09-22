/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.servicelayer.i18n.I18NService
import org.springframework.context.MessageSource
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxLocalizedMessagesServiceImplSpec extends Specification {

    MessageSource messageSource = Mock()
    I18NService i18nService = Mock()

    @Subject
    CxLocalizedMessagesServiceImpl service = new CxLocalizedMessagesServiceImpl()

    def setup() {
        service.setMessageSource(messageSource)
        service.setI18NService(i18nService)
    }

    def "getLocalizedMessage delegates to MessageSource with current locale"() {
        given:
        i18nService.getCurrentLocale() >> Locale.ENGLISH
        messageSource.getMessage("k", _, Locale.ENGLISH) >> "Hello"

        expect:
        // Pass attributes as an explicit Object[] to avoid Groovy resolving to the
        // (String, String defaultMessage, Object...) overload when multiple String args are passed.
        service.getLocalizedMessage("k", ["a", "b"] as Object[]) == "Hello"
    }

    def "getLocalizedMessage with explicit locale delegates to MessageSource"() {
        given:
        messageSource.getMessage("k", _, Locale.GERMAN) >> "Hallo"

        expect:
        service.getLocalizedMessage("k", Locale.GERMAN) == "Hallo"
    }

    def "getLocalizedMessage with default message uses current locale"() {
        given:
        i18nService.getCurrentLocale() >> Locale.FRENCH
        messageSource.getMessage("k", _, "fallback", Locale.FRENCH) >> "Bonjour"

        expect:
        service.getLocalizedMessage("k", "fallback") == "Bonjour"
    }

    def "getLocalizedMessage with default message and explicit locale delegates correctly"() {
        given:
        messageSource.getMessage("k", _, "default", Locale.ITALIAN) >> "Ciao x"

        expect:
        service.getLocalizedMessage("k", "default", Locale.ITALIAN, "x") == "Ciao x"
    }
}
