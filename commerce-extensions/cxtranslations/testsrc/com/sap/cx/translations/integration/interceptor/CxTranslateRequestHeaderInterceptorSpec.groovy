/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration.interceptor

import de.hybris.bootstrap.annotations.UnitTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslateRequestHeaderInterceptorSpec extends Specification {

    ClientHttpRequestExecution execution = Mock()

    @Subject
    CxTranslateRequestHeaderInterceptor interceptor =
            new CxTranslateRequestHeaderInterceptor("Content-Type", "application/json")

    def "intercept sets the header and delegates to execution"() {
        given:
        def headers = new HttpHeaders()
        def request = Mock(HttpRequest) { getHeaders() >> headers }
        def responseBody = new byte[0]

        when:
        interceptor.intercept(request, responseBody, execution)

        then:
        headers.getFirst("Content-Type") == "application/json"
        1 * execution.execute(request, responseBody)
    }

    def "intercept overrides an existing header value"() {
        given:
        def headers = new HttpHeaders()
        headers.set("Content-Type", "text/plain")
        def request = Mock(HttpRequest) { getHeaders() >> headers }

        when:
        interceptor.intercept(request, new byte[0], execution)

        then:
        headers.getFirst("Content-Type") == "application/json"
    }
}
