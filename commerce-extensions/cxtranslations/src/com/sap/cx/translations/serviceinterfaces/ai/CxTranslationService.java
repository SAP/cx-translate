/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai;

import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationRequestDto;
import com.sap.cx.translations.generated.dto.translation.response.CxTranslationResponseDto;
import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException;
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;

/**
 * Provides automatic/ai translation service
 */
public interface CxTranslationService {
	/**
	 * Translates provided data
	 *
	 * @param request    translation request
	 * @param aiSettings ai service settings
	 * @return response dto
	 * @throws CxTranslateIntegrationException thrown when there is an integration/interface error
	 */
	CxTranslationResponseDto translate(CxTranslationRequestDto request, CxAiTranslationServiceSettingsModel aiSettings) throws CxTranslateIntegrationException;
}
