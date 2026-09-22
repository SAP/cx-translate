/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai.impl

import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException
import com.sap.cx.translations.model.CxAiTranslationServiceApiModel
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel
import de.hybris.bootstrap.annotations.UnitTest
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationResponseJsonPathReaderSpec extends Specification {

    CxAiTranslationServiceSettingsModel settings = Mock()
    CxAiTranslationServiceApiModel api = Mock()

    @Subject
    CxTranslationResponseJsonPathReader reader = new CxTranslationResponseJsonPathReader()

    def setup() {
        settings.getAiServiceApi() >> api
        // NOTE: do not stub api.getResponsePath() or api.getUsageMetricsPaths() in setup —
        // Spock first-match-wins for shared mocks would shadow per-test stubs.
    }

    // The Status enum maps "SUCCESS" → "Success" and "ERROR" → "Error" in the wire JSON.
    private static final String VALID_INNER_JSON = '{"translations":[{"key":"k1","languageToValueMap":{"de":"Hallo"}}],"status":"Success"}'

    private String wrap(String innerJsonAsString) {
        return '{"message":{"content":' + escapeAsJsonString(innerJsonAsString) + '}}'
    }

    private String escapeAsJsonString(String s) {
        // Escape backslash, double-quote, and newlines so the inner JSON is a valid JSON string literal.
        def escaped = s.replace('\\', '\\\\')
                .replace('"', '\\"')
                .replace('\n', '\\n')
                .replace('\r', '\\r')
        return '"' + escaped + '"'
    }

    // -------------------------------------------------------------------------
    // null / empty body guards
    // -------------------------------------------------------------------------

    def "readResponse throws CxTranslateIntegrationException when responseEntity is null"() {
        when:
        reader.readResponse(null, settings)

        then:
        thrown(CxTranslateIntegrationException)
    }

    def "readResponse throws CxTranslateIntegrationException when body is null"() {
        when:
        reader.readResponse(ResponseEntity.ok(null), settings)

        then:
        thrown(CxTranslateIntegrationException)
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    def "readResponse parses translations from response body via JSON path"() {
        given:
        api.getResponsePath() >> '$.message.content'
        def body = wrap(VALID_INNER_JSON)

        when:
        def result = reader.readResponse(ResponseEntity.ok(body), settings)

        then:
        result.translations.size() == 1
        result.translations[0].key == "k1"
        result.translations[0].languageToValueMap == [de: "Hallo"]
    }

    // -------------------------------------------------------------------------
    // JSON path that produces no value
    // -------------------------------------------------------------------------

    def "readResponse throws CxTranslateIntegrationException when JSON path matches no value"() {
        given:
        api.getResponsePath() >> '$.nonexistent.field'
        def body = wrap(VALID_INNER_JSON)

        when:
        reader.readResponse(ResponseEntity.ok(body), settings)

        then:
        thrown(CxTranslateIntegrationException)
    }

    // -------------------------------------------------------------------------
    // Invalid JSON
    // -------------------------------------------------------------------------

    def "readResponse throws CxTranslateIntegrationException when response body is not JSON"() {
        given:
        api.getResponsePath() >> '$.message.content'

        when:
        reader.readResponse(ResponseEntity.ok("not-json-at-all"), settings)

        then:
        thrown(CxTranslateIntegrationException)
    }

    def "readResponse throws CxTranslateIntegrationException when extracted DTO JSON is malformed"() {
        given:
        api.getResponsePath() >> '$.message.content'
        def body = wrap('{"this is": broken json')

        when:
        reader.readResponse(ResponseEntity.ok(body), settings)

        then:
        thrown(CxTranslateIntegrationException)
    }

    // -------------------------------------------------------------------------
    // Ill-formed feedback field stripping
    // -------------------------------------------------------------------------

    def "readResponse strips ill-formed feedback field with no value"() {
        given:
        // AI occasionally emits `"feedback"\n` inside a translation object with no value at all,
        // producing JSON like {"translations":[{"key":"k1","languageToValueMap":{...},"feedback"\n}],...}.
        // After the strip regex `"feedback"\s*\n` removes that fragment, the JSON becomes valid.
        api.getResponsePath() >> '$.message.content'
        def innerWithBadFeedback = '{"translations":[{"key":"k1","languageToValueMap":{"de":"Hallo"},"feedback"\n}],"status":"Success"}'

        when:
        def result = reader.readResponse(ResponseEntity.ok(wrap(innerWithBadFeedback)), settings)

        then:
        noExceptionThrown()
        result.translations.size() == 1
        result.translations[0].key == "k1"
    }

    // -------------------------------------------------------------------------
    // Usage metrics
    // -------------------------------------------------------------------------

    def "readResponse reads usage metrics when paths are configured"() {
        given:
        api.getResponsePath() >> '$.message.content'
        // Use floating-point JSON numbers so JsonPath can deserialize as Double without coercion issues
        api.getUsageMetricsPaths() >> ["promptTokens": '$.usage.prompt_tokens', "totalTokens": '$.usage.total_tokens']
        def body = '{"message":{"content":' + escapeAsJsonString(VALID_INNER_JSON) + '},"usage":{"prompt_tokens":42.0,"total_tokens":100.0}}'

        when:
        def result = reader.readResponse(ResponseEntity.ok(body), settings)

        then:
        result.usageInfo == ["promptTokens": 42.0d, "totalTokens": 100.0d]
    }

    def "readResponse silently ignores missing usage metric paths"() {
        given:
        api.getResponsePath() >> '$.message.content'
        api.getUsageMetricsPaths() >> ["missing": '$.no.such.path']
        def body = wrap(VALID_INNER_JSON)

        when:
        def result = reader.readResponse(ResponseEntity.ok(body), settings)

        then:
        result.usageInfo.isEmpty()
    }
}
