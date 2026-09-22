/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.deepl

import com.sap.cx.translations.generated.dto.translation.deepl.request.CxTranslationsDeepLRequest
import com.sap.cx.translations.generated.dto.translation.deepl.response.CxTranslationsDeepLResponse
import com.sap.cx.translations.integration.CxTranslateRestRequest
import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException
import com.sap.cx.translations.model.CxDeepLTranslationServiceApiModel
import com.sap.cx.translations.model.CxDeepLTranslationServiceSettingsModel
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationsDeeplInterfaceServiceSpec extends Specification {

    CxDeepLTranslationServiceSettingsModel settings = Mock()
    CxDeepLTranslationServiceApiModel api = Mock()
    ConsumedDestinationModel destination = Mock()

    /**
     * Subclass that exposes the protected getResponseEntity so we can stub it.
     */
    CxTranslationsDeeplInterfaceService service

    def setup() {
        settings.getDeepLServiceApi() >> api
        api.getDestination() >> destination
        destination.getUrl() >> "https://api.deepl.example/v2/translate"
    }

    def "translate returns body from response entity"() {
        given:
        def expectedResponse = new CxTranslationsDeepLResponse()
        ResponseEntity captured = ResponseEntity.ok(expectedResponse)

        service = new CxTranslationsDeeplInterfaceService() {
            @Override
            protected ResponseEntity getResponseEntity(CxTranslateRestRequest restRequest,
                                                       ConsumedDestinationModel dest,
                                                       String url) {
                return captured
            }
        }

        when:
        def result = service.translate(new CxTranslationsDeepLRequest(), settings)

        then:
        result.is(expectedResponse)
    }

    def "translate throws CxTranslateIntegrationException when response entity is null"() {
        given:
        service = new CxTranslationsDeeplInterfaceService() {
            @Override
            protected ResponseEntity getResponseEntity(CxTranslateRestRequest restRequest,
                                                       ConsumedDestinationModel dest,
                                                       String url) {
                return null
            }
        }

        when:
        service.translate(new CxTranslationsDeepLRequest(), settings)

        then:
        thrown(CxTranslateIntegrationException)
    }
}
