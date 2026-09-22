/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai.dto;

import java.util.List;

/**
 * DTO for a single value that should be translated.
 */
public class CxTranslationsInputTextDto {
	private String key;
	private String value;
	private String valueTranslationHint;
	private List<String> targetLanguagesOverride;

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public String getValueTranslationHint() {
		return valueTranslationHint;
	}

	public void setValueTranslationHint(final String valueTranslationHint) {
		this.valueTranslationHint = valueTranslationHint;
	}

	public List<String> getTargetLanguagesOverride()
	{
		return targetLanguagesOverride;
	}

	public void setTargetLanguagesOverride(final List<String> targetLanguagesOverride)
	{
		this.targetLanguagesOverride = targetLanguagesOverride;
	}
}
