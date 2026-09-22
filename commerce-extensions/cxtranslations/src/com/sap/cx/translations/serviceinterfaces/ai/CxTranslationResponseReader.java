/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai;

import com.sap.cx.translations.generated.dto.translation.response.CxTranslationResponseDto;
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;
import org.springframework.http.ResponseEntity;

/**
 * Reads CxTranslationResponseDto from HTTP's ResponseEntity.
 */
public interface CxTranslationResponseReader {
	/**
	 * @param responseEntity - entity to read
	 * @param aiSettings     - service settings
	 */
	CxTranslationResponseDto readResponse(ResponseEntity<String> responseEntity, CxAiTranslationServiceSettingsModel aiSettings);
}
