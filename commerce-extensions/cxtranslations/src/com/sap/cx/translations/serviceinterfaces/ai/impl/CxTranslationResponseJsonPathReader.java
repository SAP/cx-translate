/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.sap.cx.translations.generated.dto.translation.response.CxTranslationResponseDto;
import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException;
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;
import com.sap.cx.translations.serviceinterfaces.ai.CxTranslationResponseReader;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of CxTranslateResponseReader.
 * Uses JSON Path to extract the response.
 */
public class CxTranslationResponseJsonPathReader implements CxTranslationResponseReader {
	private final static Logger LOG = LoggerFactory.getLogger(CxTranslationResponseJsonPathReader.class);

	// Strips an ill-formed `"feedback"` field that the AI sometimes emits with no value.
	// Matches the broken fragment INCLUDING a preceding comma/whitespace so the remaining JSON
	// is still valid (no trailing comma left behind).
	// Examples removed: `,"feedback"\n`, `, "feedback"\n`, `"feedback"\n` (at start of object).
	private static final Pattern FEEDBACK_FIX_PATTERN = Pattern.compile(",?\\s*\"feedback\"\\s*\\n");

	private final ObjectMapper objectMapper;

	public CxTranslationResponseJsonPathReader() {
		objectMapper = new ObjectMapper();
	}

	@Override
	public CxTranslationResponseDto readResponse(final ResponseEntity<String> responseEntity, final CxAiTranslationServiceSettingsModel aiSettings) {

		if (responseEntity == null || responseEntity.getBody() == null) {
			throw new CxTranslateIntegrationException("AI service returned no response body");
		}
		final String responseContent = responseEntity.getBody();
		final String translationResponseJsonPath = aiSettings.getAiServiceApi()
				.getResponsePath();

		final Map<String, String> usageMetricsPaths = aiSettings.getAiServiceApi().getUsageMetricsPaths();
		String responseJsonString;
		Map<String, Double> usageInfo = new HashMap<>();

		try {
			final Configuration.ConfigurationBuilder builder = Configuration.builder();
			builder.jsonProvider(new JacksonJsonProvider(objectMapper));
			final Configuration configuration = builder.build();
			final DocumentContext parsedResponseContent = JsonPath.parse(responseContent, configuration);
			responseJsonString = parsedResponseContent
					.read(translationResponseJsonPath, String.class);

			if (usageMetricsPaths != null) {
				usageInfo = usageMetricsPaths.entrySet().stream().map(entry -> {
					try {
						return ImmutablePair.of(entry.getKey(), parsedResponseContent.read(entry.getValue(), Double.class));
					} catch (final Exception e) {
						//ignore if path is not found or value cannot be read as String, just log it.
						//we don't want to fail the whole response parsing just because of usage metrics.
						LOG.error("Cannot read usage metric {} with path {}: {}", entry.getKey(), entry.getValue(), e.getMessage());
					}
					return null;
				}).filter(Objects::nonNull).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
			}
		} catch (final Exception e) {
			throw new CxTranslateIntegrationException("Could not apply JSON Path %s on %s".formatted(translationResponseJsonPath, responseContent), e);
		}

		if (responseJsonString == null) {
			throw new CxTranslateIntegrationException(
					"JSON path '%s' produced no value in AI response".formatted(translationResponseJsonPath));
		}

		try {
			//for some reason AI sometimes likes to return ill-formed "feedback" field with no value at all.
			responseJsonString = FEEDBACK_FIX_PATTERN.matcher(responseJsonString).replaceAll(" ");
			final CxTranslationResponseDto result = objectMapper.readValue(responseJsonString, CxTranslationResponseDto.class);
			result.setUsageInfo(usageInfo);
			return result;
		} catch (final JsonProcessingException e) {
			throw new CxTranslateIntegrationException("Cannot parse Response DTO object!", e);
		}

	}
}
