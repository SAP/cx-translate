/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.dynamicattributes

import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import jakarta.annotation.Resource

/**
 * Integration tests for {@link CxTranslateModelParametersHandler} — the dynamic attribute
 * behind {@code CxAiTranslationServiceSettingsModel.parameterValues}.
 *
 * <p>The handler merges saved parameter values with defaults from the API so that all
 * configurable parameters are always present in the returned map.</p>
 */
@IntegrationTest
class CxTranslateModelParametersHandlerIntSpec extends ServicelayerTransactionalSpockSpecification {

    @Resource
    ModelService modelService

    private CxTranslationTestDataBuilder builder

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService)
    }

    // -----------------------------------------------------------------------
    // parameterValues get
    // -----------------------------------------------------------------------

    def "getParameterValues returns all configurable parameters with saved values when set"() {
        given: "an API with two configurable parameters and saved values for both"
        def api = builder.createAiServiceApi("api-params-saved")
        api.setConfigurableParameters(["temperature", "max_tokens"])
        api.setDefaultParameterValues(["temperature": "0.7", "max_tokens": "1024"])
        modelService.save(api)

        def aiModel = builder.createAiModel("model-params-saved")
        def settings = builder.createAiServiceSettings("settings-params-saved", api, aiModel)
        settings.setSavedParameterValuesInternal(["temperature": "0.3", "max_tokens": "512"])
        modelService.save(settings)

        when:
        def result = settings.getParameterValues()

        then: "saved values take precedence over defaults"
        result["temperature"] == "0.3"
        result["max_tokens"] == "512"
        result.size() == 2
    }

    def "getParameterValues fills defaults for parameters not yet saved"() {
        given: "an API with a default for 'temperature' but no saved value"
        def api = builder.createAiServiceApi("api-params-defaults")
        api.setConfigurableParameters(["temperature", "top_p"])
        api.setDefaultParameterValues(["temperature": "0.7", "top_p": "0.9"])
        modelService.save(api)

        def aiModel = builder.createAiModel("model-params-defaults")
        def settings = builder.createAiServiceSettings("settings-params-defaults", api, aiModel)

        when:
        def result = settings.getParameterValues()

        then: "defaults are used when no saved value exists"
        result["temperature"] == "0.7"
        result["top_p"] == "0.9"
    }

    def "getParameterValues fills empty string for parameters with no default"() {
        given: "an API with a parameter that has no default"
        def api = builder.createAiServiceApi("api-params-no-default")
        api.setConfigurableParameters(["system_prompt"])
        api.setDefaultParameterValues([:])
        modelService.save(api)

        def aiModel = builder.createAiModel("model-params-no-default")
        def settings = builder.createAiServiceSettings("settings-params-no-default", api, aiModel)

        when:
        def result = settings.getParameterValues()

        then:
        result["system_prompt"] == ""
        result.size() == 1
    }

    def "getParameterValues returns empty map when there are no configurable parameters"() {
        given:
        def api = builder.createAiServiceApi("api-params-empty")
        def aiModel = builder.createAiModel("model-params-empty")
        def settings = builder.createAiServiceSettings("settings-params-empty", api, aiModel)

        when:
        def result = settings.getParameterValues()

        then:
        result.isEmpty()
    }

    // -----------------------------------------------------------------------
    // parameterValues set — only allowed keys are persisted
    // -----------------------------------------------------------------------

    def "setParameterValues persists only keys that are in configurableParameters"() {
        given:
        def api = builder.createAiServiceApi("api-params-set-filter")
        api.setConfigurableParameters(["temperature"])
        modelService.save(api)

        def aiModel = builder.createAiModel("model-params-set-filter")
        def settings = builder.createAiServiceSettings("settings-params-set-filter", api, aiModel)

        when: "saving a map that contains an extra key not in configurableParameters"
        settings.setParameterValues(["temperature": "0.5", "unknown_param": "ignored"])
        modelService.save(settings)

        then: "only the allowed key is stored internally"
        settings.getSavedParameterValuesInternal()["temperature"] == "0.5"
        !settings.getSavedParameterValuesInternal().containsKey("unknown_param")
    }

    def "setParameterValues discards all keys when none match configurableParameters"() {
        given:
        def api = builder.createAiServiceApi("api-params-set-all-filtered")
        api.setConfigurableParameters(["temperature"])
        modelService.save(api)

        def aiModel = builder.createAiModel("model-params-set-all-filtered")
        def settings = builder.createAiServiceSettings("settings-params-set-all-filtered", api, aiModel)

        when:
        settings.setParameterValues(["unknown_a": "x", "unknown_b": "y"])
        modelService.save(settings)

        then:
        settings.getSavedParameterValuesInternal().isEmpty()
    }

    def "setParameterValues round-trips correctly through get"() {
        given:
        def api = builder.createAiServiceApi("api-params-roundtrip")
        api.setConfigurableParameters(["temperature", "max_tokens"])
        api.setDefaultParameterValues(["temperature": "0.7", "max_tokens": "1024"])
        modelService.save(api)

        def aiModel = builder.createAiModel("model-params-roundtrip")
        def settings = builder.createAiServiceSettings("settings-params-roundtrip", api, aiModel)

        when:
        settings.setParameterValues(["temperature": "0.1", "max_tokens": "256"])
        modelService.save(settings)

        then: "getParameterValues returns the saved overrides, not the defaults"
        settings.getParameterValues()["temperature"] == "0.1"
        settings.getParameterValues()["max_tokens"] == "256"
    }
}
