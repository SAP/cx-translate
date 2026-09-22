/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.deepl;

import com.sap.cx.translations.generated.dto.translation.deepl.request.CxTranslationsDeepLRequest;
import com.sap.cx.translations.generated.dto.translation.deepl.response.CxTranslationsDeepLResponse;
import com.sap.cx.translations.integration.CxTranslateOutboundAbstractRestService;
import com.sap.cx.translations.integration.CxTranslateRestRequest;
import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException;
import com.sap.cx.translations.model.CxDeepLTranslationServiceSettingsModel;
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * A service for interfacing with DeepL translation providers for CX Translate.
 */
public class CxTranslationsDeeplInterfaceService extends CxTranslateOutboundAbstractRestService {
    /**
     * Translates a text.
     * @param request Translation request
     * @param deepLSettings Settings of the service
     * @return Translation response
     */
    public CxTranslationsDeepLResponse translate(final CxTranslationsDeepLRequest request, final CxDeepLTranslationServiceSettingsModel deepLSettings) {
        final ConsumedDestinationModel destination = deepLSettings.getDeepLServiceApi().getDestination();
        final CxTranslateRestRequest<CxTranslationsDeepLRequest, CxTranslationsDeepLResponse> restRequest =
                new CxTranslateRestRequest<>(HttpMethod.POST, CxTranslationsDeepLResponse.class);
        restRequest.setBody(request);
        restRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        final ResponseEntity<?> responseEntity = getResponseEntity(restRequest, destination, destination.getUrl());
        if (responseEntity == null) {
            throw new CxTranslateIntegrationException("DeepL translation service returned no response");
        }
        return (CxTranslationsDeepLResponse) responseEntity.getBody();
    }
}
