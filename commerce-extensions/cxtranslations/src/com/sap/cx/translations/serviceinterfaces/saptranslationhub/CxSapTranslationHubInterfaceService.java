/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.saptranslationhub;

import com.sap.cx.translations.integration.CxTranslateOutboundAbstractRestService;
import com.sap.cx.translations.integration.CxTranslateRestRequest;
import com.sap.cx.translations.model.CxSapTranslationHubServiceSettingsModel;
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for interfacing with SAP Translation Hub document translation API.
 * Sends XLIFF 1.2 documents and receives translated XLIFF back.
 */
public class CxSapTranslationHubInterfaceService extends CxTranslateOutboundAbstractRestService {

    private static final String XLIFF_MEDIA_TYPE_VALUE = "application/x-xliff+xml";
    private static final MediaType XLIFF_MEDIA_TYPE = new MediaType("application", "x-xliff+xml", StandardCharsets.UTF_8);
    private static final String TEXT_HTML_CONTENT_TYPE_VALUE = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8";
    private static final MediaType TEXT_HTML_MEDIA_TYPE = new MediaType("text", "html", StandardCharsets.UTF_8);
    private static final String DEFAULT_MODEL = "llm";

	/**
	 * Ensure StringHttpMessageConverter supports the XLIFF media type for both request and response.
	 */
    @Override
    protected void addContentTypeHeader(final RestTemplate restTemplate) {
	    restTemplate.getMessageConverters().stream()
	                .filter(StringHttpMessageConverter.class::isInstance)
	                .map(StringHttpMessageConverter.class::cast)
	                .forEach(c -> {
		                c.setDefaultCharset(StandardCharsets.UTF_8);
		                c.setSupportedMediaTypes(new ArrayList<>(List.of(MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, XLIFF_MEDIA_TYPE, MediaType.ALL)));
	                });
    }




    /**
     * Translates an XLIFF document for a single target language.
     *
     * @param xliffBody      XLIFF 1.2 XML string
     * @param targetLanguage BCP-47 target language tag (e.g. "de-DE")
     * @param settings       SAP Translation Hub service settings
     * @return map of trans-unit id to translated text
     */
    public Map<String, String> translateXliff(final String xliffBody, final String targetLanguage,
                                              final CxSapTranslationHubServiceSettingsModel settings) {
        final ConsumedDestinationModel destination = settings.getSapTranslationHubServiceApi().getDestination();

        final CxTranslateRestRequest<String, String> restRequest = createRequest(targetLanguage, settings);
        restRequest.setBody(xliffBody);
        restRequest.getHeaders().setContentType(XLIFF_MEDIA_TYPE);
        restRequest.getHeaders().setAccept(List.of(XLIFF_MEDIA_TYPE));

        final String responseXliff = (String) getResponseEntity(restRequest, destination, destination.getUrl()).getBody();
        return CxSapTranslationHubXliffHelper.parseTranslations(responseXliff);
    }

    /**
     * Translates a single text payload directly using {@code text/html}, skipping XLIFF wrapping.
     *
     * @param textBody        raw text/html body to translate
     * @param targetLanguage  BCP-47 target language tag (e.g. "de-DE")
     * @param settings        SAP Translation Hub service settings
     * @return translated text body
     */
    public String translateText(final String textBody, final String targetLanguage,
                                final CxSapTranslationHubServiceSettingsModel settings) {
        final ConsumedDestinationModel destination = settings.getSapTranslationHubServiceApi().getDestination();

        final CxTranslateRestRequest<String, String> restRequest = createRequest(targetLanguage, settings);
        restRequest.setBody(textBody);
        restRequest.getHeaders().set(HttpHeaders.CONTENT_TYPE, TEXT_HTML_CONTENT_TYPE_VALUE);
        restRequest.getHeaders().setAccept(List.of(TEXT_HTML_MEDIA_TYPE, MediaType.TEXT_PLAIN));

        return (String) getResponseEntity(restRequest, destination, destination.getUrl()).getBody();
    }

    private CxTranslateRestRequest<String, String> createRequest(final String targetLanguage,
                                                                 final CxSapTranslationHubServiceSettingsModel settings) {
        final CxTranslateRestRequest<String, String> restRequest =
                new CxTranslateRestRequest<>(HttpMethod.POST, String.class);
        restRequest.addQueryParam("model", settings.getModel() != null ? settings.getModel().getCode() : DEFAULT_MODEL);
        restRequest.addQueryParam("targetLanguage", targetLanguage);

        if (settings.getDomain() != null) {
            restRequest.addQueryParam("domain", settings.getDomain());
        }

        return restRequest;
    }
}

