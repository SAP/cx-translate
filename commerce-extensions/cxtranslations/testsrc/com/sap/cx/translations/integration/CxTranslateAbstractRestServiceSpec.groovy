/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration

import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException
import com.sap.cx.translations.integration.exception.CxTranslateInterfaceTimeoutException
import com.sap.cx.translations.integration.strategy.RestErrorStrategy
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslateAbstractRestServiceSpec extends Specification {

    RestErrorStrategy restErrorStrategy = Mock()

    /**
     * Minimal concrete subclass — just enough to be instantiable and testable.
     */
    @Subject
    CxTranslateAbstractRestService service = new CxTranslateAbstractRestService() {
        @Override
        protected <REQUEST, RESPONSE> ResponseEntity<?> getResponseEntity(
                CxTranslateRestRequest<REQUEST, RESPONSE> restRequest,
                ConsumedDestinationModel destination,
                String url) {
            return null
        }
    }

    def setup() {
        service.setRestErrorStrategy(restErrorStrategy)
        service.setInterfaceEndpoint("http://example.com")
        service.setInterfaceEndpointPath("/api")
    }

    // -------------------------------------------------------------------------
    // executeRequestGetEntityWithErrorStrategy
    // -------------------------------------------------------------------------

    def "returns response entity on success"() {
        given:
        def entity = ResponseEntity.ok("result")

        when:
        def result = service.executeRequestGetEntityWithErrorStrategy({ -> entity }, "test")

        then:
        result == entity
    }

    def "rethrows CxTranslateInterfaceTimeoutException on ResourceAccessException"() {
        when:
        service.executeRequestGetEntityWithErrorStrategy(
                { -> throw new ResourceAccessException("connect timeout") }, "test")

        then:
        thrown(CxTranslateInterfaceTimeoutException)
    }

    def "rethrows CxTranslateIntegrationException on HttpMessageConversionException"() {
        when:
        service.executeRequestGetEntityWithErrorStrategy(
                { -> throw new HttpMessageConversionException("bad json") }, "test")

        then:
        thrown(CxTranslateIntegrationException)
    }

    def "delegates to error strategy for a known HttpClientErrorException"() {
        given:
        def ex = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad request", null, null, null)
        restErrorStrategy.handleHttpStatusCode(HttpStatus.BAD_REQUEST) >> { /* no-op */ }

        when:
        def result = service.executeRequestGetEntityWithErrorStrategy({ -> throw ex }, "test")

        then:
        result == null
        1 * restErrorStrategy.handleHttpStatusCode(HttpStatus.BAD_REQUEST)
    }

    def "throws CxTranslateIntegrationException for unknown/non-standard HTTP status code"() {
        given:
        // HttpStatusCode is sealed in Spring 6 — use valueOf() to get an instance with an unknown code
        // HttpClientErrorException with status 999 triggers the "resolve() returns null" branch
        def ex = new HttpClientErrorException(org.springframework.http.HttpStatusCode.valueOf(999), "unknown")

        when:
        service.executeRequestGetEntityWithErrorStrategy({ -> throw ex }, "test")

        then:
        thrown(CxTranslateIntegrationException)
        0 * restErrorStrategy._
    }

    // -------------------------------------------------------------------------
    // executeRequestWithErrorStrategy — body extraction
    // -------------------------------------------------------------------------

    def "returns body when response has body"() {
        given:
        def entity = ResponseEntity.ok("hello")

        when:
        def result = service.executeRequestWithErrorStrategy({ -> entity }, "test")

        then:
        result == "hello"
    }

    def "returns null when response entity is null"() {
        when:
        def result = service.executeRequestWithErrorStrategy({ -> null }, "test")

        then:
        result == null
    }

    // -------------------------------------------------------------------------
    // getUri — null safety
    // -------------------------------------------------------------------------

    def "getUri returns endpoint + path"() {
        expect:
        service.getUri() == "http://example.com/api"
    }

    def "getUri throws IllegalStateException when interfaceEndpoint is blank"() {
        given:
        service.setInterfaceEndpoint("")

        when:
        service.getUri()

        then:
        thrown(IllegalStateException)
    }

    def "getUri treats null endpointPath as empty string"() {
        given:
        service.setInterfaceEndpointPath(null)

        expect:
        service.getUri() == "http://example.com"
    }
}
