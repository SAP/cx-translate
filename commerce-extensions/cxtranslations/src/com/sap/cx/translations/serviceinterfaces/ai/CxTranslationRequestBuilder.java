/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai;

import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationRequestDto;
import com.sap.cx.translations.integration.CxTranslateRestRequest;
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;

/**
 * Builds CxTranslateRestRequest based on CxTranslationRequestDto and CxAiTranslationServiceSettings.
 */
public interface CxTranslationRequestBuilder {
	/**
	 * Builds CxTranslateRestRequest based on CxTranslationRequestDto and CxAiTranslationServiceSettings.
	 * .
	 *
	 * @param request    a request to transform
	 * @param aiSettings service configuration
	 */
	CxTranslateRestRequest<String, String> buildRequest(CxTranslationRequestDto request, CxAiTranslationServiceSettingsModel aiSettings);
}
