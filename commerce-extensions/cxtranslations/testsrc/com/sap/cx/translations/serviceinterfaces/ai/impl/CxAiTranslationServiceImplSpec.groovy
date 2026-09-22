/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai.impl

import com.sap.cx.translations.enums.CxTranslateServiceLoggingLevel
import com.sap.cx.translations.generated.dto.translation.response.CxTranslationOutputText
import com.sap.cx.translations.generated.dto.translation.response.CxTranslationResponseDto
import com.sap.cx.translations.integration.CxTranslateRestRequest
import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException
import com.sap.cx.translations.model.CxAiDeploymentConfigModel
import com.sap.cx.translations.model.CxAiTranslationServiceApiModel
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel
import com.sap.cx.translations.model.CxTranslateAiModelModel
import com.sap.cx.translations.serviceinterfaces.ai.CxTranslationRequestBuilder
import com.sap.cx.translations.serviceinterfaces.ai.CxTranslationResponseReader
import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationRequestDto
import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationsInputTextDto
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import spock.lang.Specification

@UnitTest
class CxAiTranslationServiceImplSpec extends Specification {

    CxAiTranslationServiceSettingsModel settings = Mock()
    CxAiTranslationServiceApiModel apiModel = Mock()
    CxAiDeploymentConfigModel deploymentConfig = Mock()
    ConsumedDestinationModel destination = Mock()
    CxTranslateAiModelModel aiModel = Mock()
    CxTranslationRequestBuilder requestBuilder = Mock()
    CxTranslationResponseReader responseReader = Mock()

    /**
     * Subclass that lets us inject mocked builder/reader and stubs the HTTP layer.
     */
    private CxAiTranslationServiceImpl buildService(ResponseEntity<String> stubbedResponse) {
        return new CxAiTranslationServiceImpl() {
            @Override
            protected ResponseEntity getResponseEntity(CxTranslateRestRequest restRequest,
                                                       ConsumedDestinationModel dest,
                                                       String url) {
                return stubbedResponse
            }

            @Override
            protected CxTranslationRequestBuilder getRequestBuilder(CxAiTranslationServiceApiModel api) {
                return requestBuilder
            }

            @Override
            protected CxTranslationResponseReader getResponseReader(CxAiTranslationServiceApiModel api) {
                return responseReader
            }
        }
    }

    def setup() {
        settings.getAiServiceApi() >> apiModel
        settings.getAiModel() >> aiModel
        settings.getParameterValues() >> [:]
        apiModel.getDeploymentConfiguration() >> deploymentConfig
        apiModel.getEndpointUrlSuffix() >> "/translate"
        // NOTE: do not stub apiModel.getLoggingLevel() in setup — Spock first-match-wins
        // would shadow per-test stubs that need a non-NEVER level.
        deploymentConfig.getDestination() >> destination
        deploymentConfig.getDeployments() >> [:]
        destination.getUrl() >> "https://ai.example"
        aiModel.getCode() >> "gpt"

        requestBuilder.buildRequest(_, _) >> new CxTranslateRestRequest<String, String>(HttpMethod.POST, String.class)
    }

    private CxTranslationRequestDto request(List<String> targetLanguages,
                                            List<CxTranslationsInputTextDto> values = []) {
        def dto = new CxTranslationRequestDto()
        dto.setSourceLanguage("en")
        dto.setTargetLanguages(targetLanguages)
        dto.setValuesForTranslation(values)
        return dto
    }

    private CxTranslationsInputTextDto input(String key, String value) {
        def i = new CxTranslationsInputTextDto()
        i.setKey(key)
        i.setValue(value)
        return i
    }

    private CxTranslationOutputText output(String key, Map<String, String> langMap = [:]) {
        def o = new CxTranslationOutputText()
        o.setKey(key)
        o.setLanguageToValueMap(langMap)
        return o
    }

    private CxTranslationResponseDto response(List<CxTranslationOutputText> translations,
                                              CxTranslationResponseDto.Status status = CxTranslationResponseDto.Status.SUCCESS) {
        def r = new CxTranslationResponseDto()
        r.setTranslations(translations)
        r.setStatus(status)
        return r
    }

    // -------------------------------------------------------------------------
    // Single-request path
    // -------------------------------------------------------------------------

    def "translate uses single request when no batching is needed"() {
        given:
        def req = request(["de", "fr"], [input("k", "Hi")])
        settings.getMaxLanguagesInRequest() >> null
        responseReader.readResponse(_, _) >> response([output("k", [de: "Hallo", fr: "Salut"])])

        when:
        def result = service(ResponseEntity.ok("{}")).translate(req, settings)

        then:
        result.translations.size() == 1
        result.translations[0].key == "k"
    }

    def "translate uses single request when targetLanguages is null (no NPE)"() {
        given:
        def req = request(null, [input("k", "Hi")])
        settings.getMaxLanguagesInRequest() >> 2
        responseReader.readResponse(_, _) >> response([output("k")])

        when:
        def result = service(ResponseEntity.ok("{}")).translate(req, settings)

        then:
        noExceptionThrown()
        result != null
    }

    // -------------------------------------------------------------------------
    // Batching path
    // -------------------------------------------------------------------------

    def "translate splits into batches when targetLanguages exceeds max"() {
        given:
        def req = request(["de", "fr", "es"], [input("k", "Hi")])
        settings.getMaxLanguagesInRequest() >> 2
        // Two batches: ["de","fr"] and ["es"]
        responseReader.readResponse(_, _) >>>
                [
                        response([output("k", [de: "Hallo", fr: "Salut"])]),
                        response([output("k", [es: "Hola"])])
                ]

        when:
        def result = service(ResponseEntity.ok("{}")).translate(req, settings)

        then:
        result.translations.size() == 1
        result.translations[0].languageToValueMap == [de: "Hallo", fr: "Salut", es: "Hola"]
    }

    def "translate returns early on error batch (intentionally discards prior success)"() {
        given:
        def req = request(["de", "fr", "es", "it"], [input("k", "Hi")])
        settings.getMaxLanguagesInRequest() >> 2
        // First succeeds, second fails
        responseReader.readResponse(_, _) >>>
                [
                        response([output("k", [de: "Hallo", fr: "Salut"])]),
                        response([output("k")], CxTranslationResponseDto.Status.ERROR)
                ]

        when:
        def result = service(ResponseEntity.ok("{}")).translate(req, settings)

        then:
        result.status == CxTranslationResponseDto.Status.ERROR
    }

    def "translate throws when batch responses have mismatched translation counts"() {
        given:
        def req = request(["de", "fr", "es", "it"], [input("k1", "Hi"), input("k2", "Bye")])
        settings.getMaxLanguagesInRequest() >> 2
        // Use NEVER so the validation/merge exception propagates instead of being caught+wrapped
        apiModel.getLoggingLevel() >> CxTranslateServiceLoggingLevel.NEVER
        // First batch returns 2 entries, second returns 1 — must not silently corrupt data
        responseReader.readResponse(_, _) >>>
                [
                        response([output("k1"), output("k2")]),
                        response([output("k1")])
                ]

        when:
        service(ResponseEntity.ok("{}")).translate(req, settings)

        then:
        thrown(CxTranslateIntegrationException)
    }

    def "translate throws when batch responses have keys in different order"() {
        given:
        def req = request(["de", "fr", "es", "it"], [input("k1", "Hi"), input("k2", "Bye")])
        settings.getMaxLanguagesInRequest() >> 2
        apiModel.getLoggingLevel() >> CxTranslateServiceLoggingLevel.NEVER
        // Second batch reorders the keys — must not silently mix translations
        responseReader.readResponse(_, _) >>>
                [
                        response([output("k1"), output("k2")]),
                        response([output("k2"), output("k1")])
                ]

        when:
        service(ResponseEntity.ok("{}")).translate(req, settings)

        then:
        thrown(CxTranslateIntegrationException)
    }

    // -------------------------------------------------------------------------
    // validateValues — count mismatch
    // -------------------------------------------------------------------------

    def "translateSingleRequest fails validation when response has fewer translations than input"() {
        given:
        def req = request(["de"], [input("k1", "Hi"), input("k2", "Bye")])
        settings.getMaxLanguagesInRequest() >> null
        // Response has only 1 entry
        responseReader.readResponse(_, _) >> response([output("k1")])
        apiModel.getLoggingLevel() >> CxTranslateServiceLoggingLevel.ERROR_ONLY

        when:
        def result = service(ResponseEntity.ok("{}")).translate(req, settings)

        then:
        // Caught by the catch block in translateSingleRequest, returns response with ERROR status
        result.status == CxTranslationResponseDto.Status.ERROR
    }

    // -------------------------------------------------------------------------
    // Null parameterValues guard
    // -------------------------------------------------------------------------

    def "translate succeeds when parameterValues is null"() {
        given:
        def req = request(["de"], [input("k", "Hi")])
        settings.getMaxLanguagesInRequest() >> null
        settings.getParameterValues() >> null
        responseReader.readResponse(_, _) >> response([output("k", [de: "Hallo"])])

        when:
        def result = service(ResponseEntity.ok("{}")).translate(req, settings)

        then:
        noExceptionThrown()
        result.translations.size() == 1
    }

    private CxAiTranslationServiceImpl service(ResponseEntity<String> stub) {
        buildService(stub)
    }
}
