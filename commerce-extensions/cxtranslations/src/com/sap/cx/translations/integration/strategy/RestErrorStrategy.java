/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.integration.strategy;

import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Strategy for translating {@link HttpStatusCodeException} to {@link CxTranslateIntegrationException} or extending classes
 */
public interface RestErrorStrategy {
	/**
	 * Translates given {@link HttpStatusCodeException} to {@link CxTranslateIntegrationException}
	 *
	 * @param exception to be checked and translated
	 * @throws CxTranslateIntegrationException if HTTP status wrapped in given exception is an error for particular interface
	 */
	void handleHttpStatusCodeException(HttpStatusCodeException exception) throws CxTranslateIntegrationException;

	/**
	 * Checks if given HTTP Status is treated as an error for particular interface
	 *
	 * @param httpStatus to be checked
	 * @throws CxTranslateIntegrationException if HTTP status is an error for particular interface
	 */
	void handleHttpStatusCode(HttpStatus httpStatus) throws CxTranslateIntegrationException;
}
