/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.widgets;

import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import de.hybris.platform.core.model.c2l.LanguageModel;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds saved translation settings for user session persistence.
 */
public class SavedSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<LanguageModel> sourceLanguage;
    private final Set<LanguageModel> targetLanguages;
    private final Set<CxTranslationServiceSettingsModel> translationSettings;
    private final Boolean approval;
    private final Boolean skipTranslated;
    private final Boolean translateClassification;
    private final Boolean automaticPlurals;

    private SavedSettings(final Builder builder) {
        this.sourceLanguage = builder.sourceLanguage;
        this.targetLanguages = builder.targetLanguages;
        this.translationSettings = builder.translationSettings;
        this.approval = builder.approval;
        this.skipTranslated = builder.skipTranslated;
        this.translateClassification = builder.translateClassification;
        this.automaticPlurals = builder.automaticPlurals;
    }

    public Set<LanguageModel> getSourceLanguage() {
        return sourceLanguage;
    }

    public Set<LanguageModel> getTargetLanguages() {
        return targetLanguages;
    }

    public Set<CxTranslationServiceSettingsModel> getTranslationSettings() {
        return translationSettings;
    }

    public Boolean getApproval() {
        return approval;
    }

    public Boolean getSkipTranslated() {
        return skipTranslated;
    }

    public Boolean getTranslateClassification() {
        return translateClassification;
    }

    public Boolean getAutomaticPlurals() {
        return automaticPlurals;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Set<LanguageModel> sourceLanguage = new HashSet<>();
        private Set<LanguageModel> targetLanguages = new HashSet<>();
        private Set<CxTranslationServiceSettingsModel> translationSettings = new HashSet<>();
        private Boolean approval = false;
        private Boolean skipTranslated = false;
        private Boolean translateClassification = false;
        private Boolean automaticPlurals = true;

        public Builder sourceLanguage(final Set<LanguageModel> sourceLanguage) {
            this.sourceLanguage = sourceLanguage;
            return this;
        }

        public Builder targetLanguages(final Set<LanguageModel> targetLanguages) {
            this.targetLanguages = targetLanguages;
            return this;
        }

        public Builder translationSettings(final Set<CxTranslationServiceSettingsModel> translationSettings) {
            this.translationSettings = translationSettings;
            return this;
        }

        public Builder approval(final Boolean approval) {
            this.approval = approval;
            return this;
        }

        public Builder skipTranslated(final Boolean skipTranslated) {
            this.skipTranslated = skipTranslated;
            return this;
        }

        public Builder translateClassification(final Boolean translateClassification) {
            this.translateClassification = translateClassification;
            return this;
        }

        public Builder automaticPlurals(final Boolean automaticPlurals) {
            this.automaticPlurals = automaticPlurals;
            return this;
        }

        public SavedSettings build() {
            return new SavedSettings(this);
        }
    }
}

