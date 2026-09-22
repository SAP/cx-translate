/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects.parameters;

import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import de.hybris.platform.core.model.c2l.LanguageModel;

import java.util.Collection;

/**
 * Base parameter object that captures common automatic translation options such as source/target languages,
 * user hint, approval handling and pluralization.
 *
 * <p>The {@code saveForApproval} and {@code skipTranslated} flags default to {@code false}
 * when their builder methods are called with {@code null} (e.g. when sourced from a cron-job model
 * column that has not yet been set).
 */
public class CxAutomaticTranslationParameters {
    private final CxTranslationServiceSettingsModel serviceSettings;
    private final LanguageModel sourceLanguage;
    private final Collection<LanguageModel> targetLanguages;
    private final String userHint;
    private final boolean saveForApproval;
    private final boolean skipTranslated;
    private final boolean automaticPlurals;
	private final boolean smartEditTranslation;

	protected CxAutomaticTranslationParameters(final Builder<?> builder) {
        this.serviceSettings = builder.serviceSettings;
        this.sourceLanguage = builder.sourceLanguage;
        this.targetLanguages = builder.targetLanguages;
        this.userHint = builder.userHint;
        this.saveForApproval = builder.saveForApproval;
        this.skipTranslated = builder.skipTranslated;
        this.automaticPlurals = builder.automaticPlurals;
		this.smartEditTranslation = builder.smartEditTranslation;
    }

    public CxTranslationServiceSettingsModel getServiceSettings() {
        return serviceSettings;
    }

    public LanguageModel getSourceLanguage() {
        return sourceLanguage;
    }

    public Collection<LanguageModel> getTargetLanguages() {
        return targetLanguages;
    }

    public String getUserHint() {
        return userHint;
    }

    public boolean getSaveForApproval() {
        return saveForApproval;
    }

    public boolean getSkipTranslated() {
        return skipTranslated;
    }

    public boolean isAutomaticPlurals() {
        return automaticPlurals;
    }

	public boolean isSmartEditTranslation()
	{
		return smartEditTranslation;
	}

	public abstract static class Builder<T extends Builder<T>> {
        private CxTranslationServiceSettingsModel serviceSettings;
        private LanguageModel sourceLanguage;
        private Collection<LanguageModel> targetLanguages;
        private String userHint;
        private boolean saveForApproval;
        private boolean skipTranslated;
        private boolean automaticPlurals;
	    private boolean smartEditTranslation;

	    protected abstract T self();

        public T serviceSettings(final CxTranslationServiceSettingsModel serviceSettings) {
            this.serviceSettings = serviceSettings;
            return self();
        }

        public T sourceLanguage(final LanguageModel sourceLanguage) {
            this.sourceLanguage = sourceLanguage;
            return self();
        }

        public T targetLanguages(final Collection<LanguageModel> targetLanguages) {
            this.targetLanguages = targetLanguages;
            return self();
        }

        public T userHint(final String userHint) {
            this.userHint = userHint;
            return self();
        }

        /** Accepts a nullable {@code Boolean} from cron-job model columns; {@code null} maps to {@code false}. */
        public T saveForApproval(final Boolean saveForApproval) {
            this.saveForApproval = Boolean.TRUE.equals(saveForApproval);
            return self();
        }

        /** Accepts a nullable {@code Boolean} from cron-job model columns; {@code null} maps to {@code false}. */
        public T skipTranslated(final Boolean skipTranslated) {
            this.skipTranslated = Boolean.TRUE.equals(skipTranslated);
            return self();
        }

        public T automaticPlurals(final boolean automaticPlurals) {
            this.automaticPlurals = automaticPlurals;
            return self();
        }

	    public T smartEditTranslation(final boolean smartEditTranslation)
	    {
		    this.smartEditTranslation = smartEditTranslation;
			return self();
	    }
    }
}
