/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration.strategy.impl

import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException
import com.sap.cx.translations.integration.exception.CxTranslateInterfaceTimeoutException
import de.hybris.bootstrap.annotations.UnitTest
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@UnitTest
class DefaultCxTranslateRestErrorStrategySpec extends Specification {

    @Subject
    DefaultCxTranslateRestErrorStrategy strategy = new DefaultCxTranslateRestErrorStrategy()

    // -------------------------------------------------------------------------
    // handleHttpStatusCode
    // -------------------------------------------------------------------------

    def "handleHttpStatusCode with null throws CxTranslateIntegrationException"() {
        when:
        strategy.handleHttpStatusCode(null)

        then:
        thrown(CxTranslateIntegrationException)
    }

    @Unroll
    def "handleHttpStatusCode does not throw for informational/successful status #status"() {
        when:
        strategy.handleHttpStatusCode(status)

        then:
        noExceptionThrown()

        where:
        status << [HttpStatus.OK, HttpStatus.CREATED, HttpStatus.CONTINUE, HttpStatus.NO_CONTENT]
    }

    def "handleHttpStatusCode throws CxTranslateInterfaceTimeoutException for REQUEST_TIMEOUT"() {
        when:
        strategy.handleHttpStatusCode(HttpStatus.REQUEST_TIMEOUT)

        then:
        thrown(CxTranslateInterfaceTimeoutException)
    }

    def "handleHttpStatusCode throws CxTranslateInterfaceTimeoutException for GATEWAY_TIMEOUT"() {
        when:
        strategy.handleHttpStatusCode(HttpStatus.GATEWAY_TIMEOUT)

        then:
        thrown(CxTranslateInterfaceTimeoutException)
    }

    @Unroll
    def "handleHttpStatusCode throws CxTranslateIntegrationException for non-timeout error #status"() {
        when:
        strategy.handleHttpStatusCode(status)

        then:
        def ex = thrown(CxTranslateIntegrationException)
        !(ex instanceof CxTranslateInterfaceTimeoutException)

        where:
        status << [HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.NOT_FOUND,
                   HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY]
    }

    // -------------------------------------------------------------------------
    // handleHttpStatusCodeException
    // -------------------------------------------------------------------------

    def "handleHttpStatusCodeException wraps REQUEST_TIMEOUT as CxTranslateInterfaceTimeoutException"() {
        given:
        def exception = HttpClientErrorException.create(HttpStatus.REQUEST_TIMEOUT, "timeout", null, null, null)

        when:
        strategy.handleHttpStatusCodeException(exception)

        then:
        thrown(CxTranslateInterfaceTimeoutException)
    }

    def "handleHttpStatusCodeException wraps other HTTP errors as CxTranslateIntegrationException"() {
        given:
        def exception = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad request", null, null, null)

        when:
        strategy.handleHttpStatusCodeException(exception)

        then:
        def ex = thrown(CxTranslateIntegrationException)
        !(ex instanceof CxTranslateInterfaceTimeoutException)
    }
}
