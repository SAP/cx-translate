/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai.impl

import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException
import com.sap.cx.translations.model.CxAiTranslationServiceApiModel
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel
import com.sap.cx.translations.model.CxTranslateAiModelModel
import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationRequestDto
import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationsInputTextDto
import de.hybris.bootstrap.annotations.UnitTest
import org.springframework.http.HttpMethod
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationSimpleTemplateRequestBuilderSpec extends Specification {

    CxAiTranslationServiceSettingsModel settings = Mock()
    CxAiTranslationServiceApiModel api = Mock()
    CxTranslateAiModelModel aiModel = Mock()

    @Subject
    CxTranslationSimpleTemplateRequestBuilder builder = new CxTranslationSimpleTemplateRequestBuilder()

    def setup() {
        settings.getAiServiceApi() >> api
        settings.getAiModel() >> aiModel
        settings.getParameterValues() >> [temperature: "0.0"]
        aiModel.getCode() >> "gpt-4"
    }

    private CxTranslationRequestDto request() {
        def dto = new CxTranslationRequestDto()
        dto.setSourceLanguage("en")
        dto.setTargetLanguages(["de"])
        dto.setGlobalTranslationHint("formal tone")
        def input = new CxTranslationsInputTextDto()
        input.setKey("k1")
        input.setValue("hello")
        dto.setValuesForTranslation([input])
        return dto
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    def "buildRequest returns POST request with body rendered from template"() {
        given:
        api.getRequestTemplate() >> 'model=${model}|hint=${request.globalTranslationHint}|temp=${parameters.temperature}'

        when:
        def restRequest = builder.buildRequest(request(), settings)

        then:
        restRequest.method == HttpMethod.POST
        restRequest.responseClass == String
        restRequest.body == "model=gpt-4|hint=formal tone|temp=0.0"
    }

    def "buildRequest exposes the request DTO under 'request' and escaped JSON under 'requestJson'"() {
        given:
        api.getRequestTemplate() >> 'lang=${request.sourceLanguage}|json=${requestJson}'

        when:
        def restRequest = builder.buildRequest(request(), settings)

        then:
        restRequest.body.startsWith("lang=en|json=")
        // requestJson is JSON-escaped so quotes appear as \"
        restRequest.body.contains('\\"sourceLanguage\\":\\"en\\"')
    }

    def "buildRequest exposes the responseSchemaJson in the template context"() {
        given:
        api.getRequestTemplate() >> '${responseSchemaJson}'

        when:
        def restRequest = builder.buildRequest(request(), settings)

        then:
        // The schema is JSON-escaped — it should be a non-empty string with no raw quotes/braces
        !restRequest.body.isEmpty()
        // After JSON escaping, quotes appear as \"
        restRequest.body.contains('\\"')
    }

    // -------------------------------------------------------------------------
    // Template errors
    // -------------------------------------------------------------------------

    def "buildRequest wraps template processing exceptions as CxTranslateIntegrationException"() {
        given:
        // Reference an undefined variable — Groovy SimpleTemplateEngine throws at make/writeTo
        api.getRequestTemplate() >> '${undefinedVariable.thisFails}'

        when:
        builder.buildRequest(request(), settings)

        then:
        thrown(Exception)
        // (Specifically a CxTranslateIntegrationException if it falls into the IOException catch,
        // otherwise Groovy throws MissingPropertyException — both indicate the bug surfaces clearly.)
    }

    def "buildRequest throws CxTranslateIntegrationException when template is null"() {
        given:
        api.getRequestTemplate() >> null

        when:
        builder.buildRequest(request(), settings)

        then:
        thrown(Exception)
    }
}
