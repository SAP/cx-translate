/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.integration.exception;

/**
 * Cx Translate interface timeout exception
 */
public class CxTranslateInterfaceTimeoutException extends CxTranslateIntegrationException {
	public CxTranslateInterfaceTimeoutException() {
	}

	public CxTranslateInterfaceTimeoutException(
			final String message) {
		super(message);
	}

	public CxTranslateInterfaceTimeoutException(
			final String message,
			final Throwable cause) {
		super(message, cause);
	}

	public CxTranslateInterfaceTimeoutException(
			final Throwable cause) {
		super(cause);
	}
}
