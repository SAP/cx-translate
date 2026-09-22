/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration

import de.hybris.bootstrap.annotations.UnitTest
import org.springframework.http.HttpMethod
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslateRestRequestSpec extends Specification {

    @Subject
    CxTranslateRestRequest<String, String> request =
            new CxTranslateRestRequest<>(HttpMethod.POST, String)

    def "constructor stores method and responseClass"() {
        expect:
        request.getMethod() == HttpMethod.POST
        request.getResponseClass() == String
    }

    def "setBody stores body and returns same instance for chaining"() {
        when:
        def returned = request.setBody("payload")

        then:
        request.getBody() == "payload"
        returned.is(request)
    }

    def "addHttpHeader stores header and returns same instance"() {
        when:
        def returned = request.addHttpHeader("X-Tenant", "master")

        then:
        request.getHeaders().getFirst("X-Tenant") == "master"
        returned.is(request)
    }

    def "addPathParam stores param and returns same typed instance"() {
        when:
        CxTranslateRestRequest<String, String> returned = request.addPathParam("id", "42")

        then:
        request.getUrlPathParams() == ["id": "42"]
        returned.is(request)
    }

    def "addQueryParam stores param and returns same typed instance"() {
        when:
        CxTranslateRestRequest<String, String> returned = request.addQueryParam("page", "1")

        then:
        request.getQueryParams() == ["page": "1"]
        returned.is(request)
    }

    def "fluent chain preserves type across addPathParam and addQueryParam"() {
        when:
        def result = request
                .addPathParam("id", "1")
                .addQueryParam("lang", "en")
                .setBody("body")

        then:
        result.is(request)
        request.getUrlPathParams() == ["id": "1"]
        request.getQueryParams() == ["lang": "en"]
        request.getBody() == "body"
    }

    def "multiple headers can be added"() {
        when:
        request.addHttpHeader("X-A", "a").addHttpHeader("X-B", "b")

        then:
        request.getHeaders().getFirst("X-A") == "a"
        request.getHeaders().getFirst("X-B") == "b"
    }

    def "urlPathParams and queryParams are independent maps"() {
        when:
        request.addPathParam("key", "path-val")
        request.addQueryParam("key", "query-val")

        then:
        request.getUrlPathParams() == ["key": "path-val"]
        request.getQueryParams() == ["key": "query-val"]
    }
}
