/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.exceptions;

import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceException;

public class CxTranslationException extends WebserviceException {
	private static final String TYPE = "TranslationPublishingError";
	private static final String SUBJECT_TYPE = "translation";

	public CxTranslationException(String message) {
		super(message);
	}

	public CxTranslationException(String message, String reason) {
		super(message, reason);
	}

	public CxTranslationException(String message, String reason, Throwable cause) {
		super(message, reason, cause);
	}

	public CxTranslationException(String message, String reason, String subject) {
		super(message, reason, subject);
	}

	public CxTranslationException(String message, String reason, String subject, Throwable cause) {
		super(message, reason, subject, cause);
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getSubjectType() {
		return SUBJECT_TYPE;
	}
}
