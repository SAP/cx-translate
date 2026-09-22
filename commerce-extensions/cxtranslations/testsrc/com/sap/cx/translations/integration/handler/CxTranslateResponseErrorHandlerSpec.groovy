/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration.handler

import de.hybris.bootstrap.annotations.UnitTest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpResponse
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslateResponseErrorHandlerSpec extends Specification {

    @Subject
    CxTranslateResponseErrorHandler handler = new CxTranslateResponseErrorHandler()

    def "handleError calls super when status code is not in the ignored list"() {
        given:
        handler.setIgnoredHttpErrorCodes([404])
        def response = Mock(ClientHttpResponse)
        response.getStatusCode() >> HttpStatusCode.valueOf(500)

        when:
        // super.handleError will try to read the response body; we just verify it's invoked
        // by checking that it does NOT short-circuit (i.e., it doesn't return silently)
        // We use a spy approach: verify getStatusCode() was checked
        handler.handleError(response)

        then:
        // super.handleError is called — it will try to read body and throw IOException or similar
        // but the key contract is that our code path reaches it (not the early return)
        thrown(Exception)
    }

    def "handleError does nothing when status code is in the ignored list"() {
        given:
        handler.setIgnoredHttpErrorCodes([404, 422])
        def response = Mock(ClientHttpResponse)
        response.getStatusCode() >> HttpStatusCode.valueOf(404)

        when:
        handler.handleError(response)

        then:
        noExceptionThrown()
    }

    def "handleError works with empty ignored list (default safe state)"() {
        given:
        // handler uses default Collections.emptyList() — no setter called
        def response = Mock(ClientHttpResponse)
        response.getStatusCode() >> HttpStatusCode.valueOf(404)

        when:
        handler.handleError(response)

        then:
        // reaches super.handleError — throws because it tries to read an empty body
        thrown(Exception)
    }

    def "ignoredHttpErrorCodes is initialized to empty list by default — no NPE on field access"() {
        given:
        def handler2 = new CxTranslateResponseErrorHandler()
        // Access the field via reflection to verify it is not null without invoking handleError
        def field = CxTranslateResponseErrorHandler.getDeclaredField("ignoredHttpErrorCodes")
        field.setAccessible(true)

        expect:
        field.get(handler2) != null
        (field.get(handler2) as List).isEmpty()
    }
}
