/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.services;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CxTranslationsNotification {
	private final String backofficeMessageKey;
	private final Level level;
	private final List<Object> parameters;

	public enum Level {
		Info,
		Warning,
		Error
	}

	public CxTranslationsNotification(final String backofficeMessageKey, final Level level, final List<Object> parameters) {
		this.backofficeMessageKey = backofficeMessageKey;
		this.level = level;
		this.parameters = parameters;
	}

	public String getBackofficeMessageKey() {
		return this.backofficeMessageKey;
	}

	public Level getLevel() {
		return level;
	}

	public List<Object> getParameters() {
		return parameters;
	}

	public static CxTranslationsNotification byValueLimit() {
		return new CxTranslationsNotification("cxtranslations.backoffice.notification.byValueThresholdExceeded", Level.Warning, Collections.emptyList());
	}

	public static CxTranslationsNotification notSupportedOperator(final String field, final List<String> supportedOperators) {
		return new CxTranslationsNotification("cxtranslations.backoffice.notification.notSupportedOperator", Level.Warning, List.of(field, String.join(",", supportedOperators)));
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof final CxTranslationsNotification other) {
			return getLevel() == other.getLevel()
					&& getBackofficeMessageKey().equals(other.getBackofficeMessageKey())
					&& getParameters().equals(other.getParameters());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getLevel(), getBackofficeMessageKey(), getParameters());
	}
}
