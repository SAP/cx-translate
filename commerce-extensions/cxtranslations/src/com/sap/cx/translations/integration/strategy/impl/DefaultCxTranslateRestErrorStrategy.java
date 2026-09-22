/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.integration.strategy.impl;

import com.sap.cx.translations.integration.exception.CxTranslateIntegrationException;
import com.sap.cx.translations.integration.exception.CxTranslateInterfaceTimeoutException;
import com.sap.cx.translations.integration.strategy.RestErrorStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Default REST error strategy
 */
public class DefaultCxTranslateRestErrorStrategy implements RestErrorStrategy {

	@Override
	public void handleHttpStatusCodeException(final HttpStatusCodeException exception) throws CxTranslateIntegrationException {
		final HttpStatus statusCode = HttpStatus.resolve(exception.getStatusCode().value());
		if (isTimeout(statusCode)) {
			throw new CxTranslateInterfaceTimeoutException("Interface timeout: " + statusCode, exception);
		} else {
			throw new CxTranslateIntegrationException("Error occurred caused by HTTP status code: " + statusCode, exception);
		}
	}

	@Override
	public void handleHttpStatusCode(final HttpStatus httpStatus) throws CxTranslateIntegrationException {
		if (httpStatus == null) {
			throw new CxTranslateIntegrationException("Unknown HTTP status code");
		}

		if (httpStatus.is2xxSuccessful() || httpStatus.is1xxInformational()) {
			return;
		}

		if (isTimeout(httpStatus)) {
			throw new CxTranslateInterfaceTimeoutException("Interface timeout: " + httpStatus);
		} else {
			throw new CxTranslateIntegrationException("Error occurred caused by HTTP status code: " + httpStatus);
		}
	}

	private boolean isTimeout(final HttpStatus httpStatus) {
		return HttpStatus.REQUEST_TIMEOUT == httpStatus || HttpStatus.GATEWAY_TIMEOUT == httpStatus;
	}
}
