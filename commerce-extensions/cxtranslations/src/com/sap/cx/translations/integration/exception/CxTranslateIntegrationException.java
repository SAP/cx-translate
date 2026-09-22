/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration.exception;

/**
 * Cx Translations generic interface exception
 */
public class CxTranslateIntegrationException extends RuntimeException {
	public CxTranslateIntegrationException() {
	}

	public CxTranslateIntegrationException(
			final String message) {
		super(message);
	}

	public CxTranslateIntegrationException(
			final String message,
			final Throwable cause) {
		super(message, cause);
	}

	public CxTranslateIntegrationException(
			final Throwable cause) {
		super(cause);
	}
}
