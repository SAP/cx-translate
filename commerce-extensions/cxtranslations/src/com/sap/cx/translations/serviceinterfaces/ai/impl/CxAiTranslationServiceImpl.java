/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.serviceinterfaces.ai.impl;

import com.sap.cx.translations.generated.dto.translation.response.CxTranslationOutputText;
import com.sap.cx.translations.generated.dto.translation.response.CxTranslationResponseDto;
import com.sap.cx.translations.enums.CxTranslateServiceLoggingLevel;
import com.sap.cx.translations.integration.CxTranslateOutboundAbstractRestService;
import com.sap.cx.translations.integration.CxTranslateRestRequest;
import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException;
import com.sap.cx.translations.model.CxAiTranslationServiceApiModel;
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;
import com.sap.cx.translations.serviceinterfaces.ai.CxTranslationRequestBuilder;
import com.sap.cx.translations.serviceinterfaces.ai.CxTranslationResponseReader;
import com.sap.cx.translations.serviceinterfaces.ai.CxTranslationService;
import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationRequestDto;
import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationsInputTextDto;
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel;
import de.hybris.platform.core.Registry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of CxTranslationService that uses remote AI service.
 */
public class CxAiTranslationServiceImpl extends CxTranslateOutboundAbstractRestService implements CxTranslationService {
	private static final Logger LOG = LoggerFactory.getLogger(CxAiTranslationServiceImpl.class);

	private final CxTranslationRequestBuilder defaultRequestBuilder = new CxTranslationSimpleTemplateRequestBuilder();
	private final CxTranslationResponseJsonPathReader defaultResponseReader = new CxTranslationResponseJsonPathReader();

	@Override
	public CxTranslationResponseDto translate(final CxTranslationRequestDto translationRequest, final CxAiTranslationServiceSettingsModel aiSettings) throws CxTranslateIntegrationException {
        final List<String> targetLanguages = translationRequest.getTargetLanguages();
        final Integer maxLanguagesInRequest = aiSettings.getMaxLanguagesInRequest();

        // If no target languages provided, or no limit configured, or all languages fit in one request, proceed with single request
        if (CollectionUtils.isEmpty(targetLanguages)
                || maxLanguagesInRequest == null
                || maxLanguagesInRequest <= 0
                || targetLanguages.size() <= maxLanguagesInRequest) {
            return translateSingleRequest(translationRequest, aiSettings);
        }

        // Split target languages into batches and make multiple requests
        final List<List<String>> languageBatches = splitIntoBatches(targetLanguages, maxLanguagesInRequest);
        final List<CxTranslationResponseDto> responses = new ArrayList<>();

        for (final List<String> languageBatch : languageBatches) {
            final CxTranslationRequestDto batchRequest = createBatchRequest(translationRequest, languageBatch);
            final CxTranslationResponseDto batchResponse = translateSingleRequest(batchRequest, aiSettings);
            responses.add(batchResponse);

            // If any batch fails, return the error response immediately.
            // Note: previously-successful batches' results are intentionally discarded — partial
            // results would otherwise be persisted incomplete; the caller can retry the full request.
            if (CxTranslationResponseDto.Status.ERROR == batchResponse.getStatus()) {
                return batchResponse;
            }
        }

        return mergeResponses(responses);
    }

    private CxTranslationResponseDto translateSingleRequest(final CxTranslationRequestDto translationRequest, final CxAiTranslationServiceSettingsModel aiSettings) throws CxTranslateIntegrationException {
		final CxAiTranslationServiceApiModel aiService = aiSettings.getAiServiceApi();
		final ConsumedDestinationModel destination = aiService.getDeploymentConfiguration()
															  .getDestination();
		final CxTranslationRequestBuilder requestBuilder = getRequestBuilder(aiService);

		final CxTranslateRestRequest<String, String> request = requestBuilder.buildRequest(translationRequest, aiSettings);

		if (aiService.getDeploymentConfiguration() != null && aiService.getDeploymentConfiguration()
																	   .getDeployments() != null) {
			request.addPathParam("deployment", aiService.getDeploymentConfiguration()
														.getDeployments()
														.get(aiSettings.getAiModel()));
		}

		if (aiSettings.getAiModel() != null) {
			request.addPathParam("model", aiSettings.getAiModel()
													.getCode());
		}

		final Map<String, String> parameterValues = aiSettings.getParameterValues();
		if (parameterValues != null) {
			parameterValues.forEach(request::addPathParam);
		}

		final ResponseEntity<String> responseEntity;
		try {
			responseEntity = getResponseEntity(request, destination, destination.getUrl() + aiService.getEndpointUrlSuffix());
		} catch (final HttpClientErrorException ex) {
			throw new CxTranslateIntegrationException(ex);
		}

		CxTranslationResponseDto cxTranslationResponse = null;
		try {
			final CxTranslationResponseReader responseReader = getResponseReader(aiService);
			cxTranslationResponse = responseReader.readResponse(responseEntity, aiSettings);
			validateValues(translationRequest, cxTranslationResponse);
		} catch (final Exception ex) {
			// Preserve interrupt semantics — never swallow an interrupt request
			if (ex instanceof InterruptedException) {
				Thread.currentThread().interrupt();
				throw ex;
			}
			if (aiService.getLoggingLevel() != CxTranslateServiceLoggingLevel.NEVER) {
				LOG.error("Error while processing request", ex);
				LOG.error(request.getBody());

				if (responseEntity != null) {
					LOG.error(responseEntity.toString());
				}
				if (cxTranslationResponse == null) {
					cxTranslationResponse = new CxTranslationResponseDto();
				}
				if (responseEntity != null) {
					cxTranslationResponse.setStatus(CxTranslationResponseDto.Status.ERROR);
					cxTranslationResponse.setRawMessage(responseEntity.getBody());
				}
				return cxTranslationResponse;
			}
			throw ex;
		}

		if (aiService.getLoggingLevel() == CxTranslateServiceLoggingLevel.ALWAYS) {
			cxTranslationResponse.setRawMessage(responseEntity.getBody());
		}
		return cxTranslationResponse;
	}

	protected CxTranslationRequestBuilder getRequestBuilder(final CxAiTranslationServiceApiModel aiService) {
		final CxTranslationRequestBuilder requestBuilder;

		final String customRequestBuilderBeanIdId = aiService.getCustomRequestBuilderBeanId();
		if (StringUtils.isNotBlank(customRequestBuilderBeanIdId)) {
			requestBuilder = Registry.getApplicationContext()
									 .getBean(customRequestBuilderBeanIdId, CxTranslationRequestBuilder.class);
		} else {
			requestBuilder = defaultRequestBuilder;
		}
		return requestBuilder;
	}

	protected CxTranslationResponseReader getResponseReader(final CxAiTranslationServiceApiModel aiService) {
		final CxTranslationResponseReader responseReader;
		final String customResponseReaderBeanId = aiService.getCustomResponseReaderBeanId();
		if (StringUtils.isNotBlank(customResponseReaderBeanId)) {
			responseReader = Registry.getApplicationContext()
									 .getBean(customResponseReaderBeanId, CxTranslationResponseReader.class);
		} else {
			responseReader = defaultResponseReader;
		}
		return responseReader;
	}

	private static void validateValues(final CxTranslationRequestDto translationRequest, final CxTranslationResponseDto cxTranslationResponse) {
		final List<CxTranslationsInputTextDto> valuesForTranslation = translationRequest.getValuesForTranslation();
		final List<CxTranslationOutputText> translatedValues = cxTranslationResponse.getTranslations();
		if (CollectionUtils.size(valuesForTranslation) != CollectionUtils.size(translatedValues)) {
			throw new CxTranslateIntegrationException("Number of translations does not match!");
		}
		for (int i = 0; i < valuesForTranslation.size(); i++) {
			if (!Objects.equals(valuesForTranslation.get(i).getKey(), translatedValues.get(i).getKey())) {
				throw new CxTranslateIntegrationException("Translation keys do not match!");
			}
		}
	}

    /**
     * Splits a list of target languages into batches of specified size.
     *
     * @param targetLanguages the list of target languages to split
     * @param batchSize       the maximum number of languages per batch
     * @return a list of language batches
     */
    private List<List<String>> splitIntoBatches(final List<String> targetLanguages, final int batchSize) {
        final List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < targetLanguages.size(); i += batchSize) {
            final int endIndex = Math.min(i + batchSize, targetLanguages.size());
            batches.add(new ArrayList<>(targetLanguages.subList(i, endIndex)));
        }
        return batches;
    }

    /**
     * Creates a new translation request with a subset of target languages.
     *
     * @param originalRequest the original translation request
     * @param targetLanguages the subset of target languages for this batch
     * @return a new translation request with the specified target languages
     */
    private CxTranslationRequestDto createBatchRequest(final CxTranslationRequestDto originalRequest, final List<String> targetLanguages) {
        final CxTranslationRequestDto batchRequest = new CxTranslationRequestDto();
        batchRequest.setSourceLanguage(originalRequest.getSourceLanguage());
        batchRequest.setTargetLanguages(targetLanguages);
        batchRequest.setGlobalTranslationHint(originalRequest.getGlobalTranslationHint());
        batchRequest.setValuesForTranslation(originalRequest.getValuesForTranslation());
        return batchRequest;
    }

    /**
     * Merges multiple translation responses into a single response.
     * Combines the languageToValueMap from each response for each translation key.
     * <p>
     * Verifies that each batch response has the same number of translations in the same key order
     * as the first response. If they don't match, throws {@link CxTranslateIntegrationException}
     * rather than silently writing translations to the wrong keys.
     *
     * @param responses the list of translation responses from each batch
     * @return a merged translation response containing all translations
     */
    private CxTranslationResponseDto mergeResponses(final List<CxTranslationResponseDto> responses) {
        if (responses.isEmpty()) {
            final CxTranslationResponseDto emptyResponse = new CxTranslationResponseDto();
            emptyResponse.setTranslations(new ArrayList<>());
            emptyResponse.setStatus(CxTranslationResponseDto.Status.SUCCESS);
            return emptyResponse;
        }

        final CxTranslationResponseDto mergedResponse = responses.get(0);
        mergedResponse.setStatus(CxTranslationResponseDto.Status.SUCCESS);

        // Merge language maps and feedback from subsequent responses into the first response
        final List<CxTranslationOutputText> mergedTranslations = mergedResponse.getTranslations();
        for (int i = 1; i < responses.size(); i++) {
            final List<CxTranslationOutputText> additionalTranslations = responses.get(i).getTranslations();
            if (CollectionUtils.size(mergedTranslations) != CollectionUtils.size(additionalTranslations)) {
                throw new CxTranslateIntegrationException(
                        "Batch response translation count mismatch — cannot merge safely");
            }
            for (int j = 0; j < mergedTranslations.size(); j++) {
                final CxTranslationOutputText mergedOutput = mergedTranslations.get(j);
                final CxTranslationOutputText additionalOutput = additionalTranslations.get(j);

                // Verify positional alignment by key — refuse to merge if AI returned items in a different order
                if (!Objects.equals(mergedOutput.getKey(), additionalOutput.getKey())) {
                    throw new CxTranslateIntegrationException(
                            "Batch response translation key mismatch at index " + j
                                    + " — cannot merge safely (expected '" + mergedOutput.getKey()
                                    + "', got '" + additionalOutput.getKey() + "')");
                }

                final Map<String, String> additionalLanguageMap = additionalOutput.getLanguageToValueMap();
                if (additionalLanguageMap != null) {
                    mergedOutput.getLanguageToValueMap().putAll(additionalLanguageMap);
                }

                final String additionalFeedback = additionalOutput.getFeedback();
                if (additionalFeedback != null && !additionalFeedback.isEmpty()) {
                    final String mergedFeedback = mergedOutput.getFeedback();
                    if (mergedFeedback != null && !mergedFeedback.isEmpty()) {
                        mergedOutput.setFeedback(mergedFeedback + "\n" + additionalFeedback);
                    } else {
                        mergedOutput.setFeedback(additionalFeedback);
                    }
                }
            }
        }

        final Map<String, Double> summedUsage = responses.stream()
                .filter(r -> r.getUsageInfo() != null)
                .flatMap(r -> r.getUsageInfo().entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Double::sum
                ));

        mergedResponse.setUsageInfo(summedUsage);
        return mergedResponse;
    }

}
