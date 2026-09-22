/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cx.translations.serviceinterfaces.ai.CxTranslationRequestBuilder;
import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationRequestDto;
import com.sap.cx.translations.integration.CxTranslateRestRequest;
import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException;
import com.sap.cx.translations.model.CxAiTranslationServiceApiModel;
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of CxTranslateRequestBuilder.
 * Uses Groovy's SimpleTemplateEngine to create a JSON request.
 * * Injects various data into the template context:
 * -  The translation request object itself (`request`).
 * -  The translation request object as a JSON string (`requestJson`).
 * -  The parameters from the AI settings (`parameters`).
 * -  The schema for the response (`responseSchemaJson`).
 * -  The model code from the AI settings (`model`).
 */
public class CxTranslationSimpleTemplateRequestBuilder implements CxTranslationRequestBuilder {
	private final ObjectMapper objectMapper = new ObjectMapper();
	private static final String EXPECTED_RESPONSE_SCHEMA = getSchema();

	@Override
	public CxTranslateRestRequest<String, String> buildRequest(final CxTranslationRequestDto translationRequest, final CxAiTranslationServiceSettingsModel aiSettings) {
		final CxAiTranslationServiceApiModel aiService = aiSettings.getAiServiceApi();

		final SimpleTemplateEngine simpleTemplateEngine = new SimpleTemplateEngine();
		final String requestBody;
		try {
			final Template template = simpleTemplateEngine.createTemplate(aiService.getRequestTemplate());
			final StringWriter writer = new StringWriter();
			final Map<String, Object> context = new HashMap<>();
			//request object "as it is"
			context.put("request", translationRequest);
			final String requestJson = objectMapper.writeValueAsString(translationRequest);
			final String requestJsonEncoded = StringEscapeUtils.escapeJson(requestJson);
			context.put("requestJson", requestJsonEncoded);
			final Map<String, String> parameters = aiSettings.getParameterValues();
			context.put("parameters", parameters);
			final String schemaEncoded = StringEscapeUtils.escapeJson(EXPECTED_RESPONSE_SCHEMA);
			context.put("responseSchemaJson", schemaEncoded);
			context.put("model", aiSettings.getAiModel()
										   .getCode());
			template.make(context)
					.writeTo(writer);
			requestBody = writer.getBuffer()
								.toString();

			final CxTranslateRestRequest<String, String> request = new CxTranslateRestRequest<>(HttpMethod.POST, String.class);
			request.setBody(requestBody);
			return request;
		} catch (final IOException | ClassNotFoundException e) {//exceptions thrown by objectMapper
			throw new CxTranslateIntegrationException("Cannot create request body", e);
		}
	}

	private static String getSchema() {
		try {
			return IOUtils.resourceToString("/cxtranslations/schemas/CxTranslationResponseServiceDto.json", StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new CxTranslateIntegrationException(e);
		}
	}
}
