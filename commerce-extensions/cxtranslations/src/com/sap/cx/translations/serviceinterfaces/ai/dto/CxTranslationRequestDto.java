/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.serviceinterfaces.ai.dto;

import java.util.List;

/**
 * Represents Translation request that should be processed by a Translation Service.
 */
public class CxTranslationRequestDto {

	private String sourceLanguage;
	private List<String> targetLanguages;
	private String globalTranslationHint;
	private List<CxTranslationsInputTextDto> valuesForTranslation;


	public String getSourceLanguage() {
		return sourceLanguage;
	}

	public void setSourceLanguage(final String sourceLanguage) {
		this.sourceLanguage = sourceLanguage;
	}

	public List<String> getTargetLanguages() {
		return targetLanguages;
	}

	public void setTargetLanguages(final List<String> targetLanguages) {
		this.targetLanguages = targetLanguages;
	}

	public String getGlobalTranslationHint() {
		return globalTranslationHint;
	}

	public void setGlobalTranslationHint(final String globalTranslationHint) {
		this.globalTranslationHint = globalTranslationHint;
	}

	public List<CxTranslationsInputTextDto> getValuesForTranslation() {
		return valuesForTranslation;
	}

	public void setValuesForTranslation(final List<CxTranslationsInputTextDto> valuesForTranslation) {
		this.valuesForTranslation = valuesForTranslation;
	}
}
