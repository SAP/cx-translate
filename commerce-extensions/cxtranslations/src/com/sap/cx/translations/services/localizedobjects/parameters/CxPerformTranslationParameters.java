/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects.parameters;

import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;

import java.util.List;
import java.util.function.Consumer;

/**
 * Parameters passed when executing a translation job for specific proposed translations, typically
 * derived from an existing cron job definition.
 */
public class CxPerformTranslationParameters extends CxAutomaticTranslationParameters {
    private final List<CxProposedTranslationModel> values;
    private final Consumer<CxProposedTranslationModel> afterTranslationCallback;

    private CxPerformTranslationParameters(final Builder builder) {
        super(builder);
        this.values = builder.values;
        this.afterTranslationCallback = builder.afterTranslationCallback;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder buildFromCronJob(final CxTranslationCronJobModel cronJob) {
        return builder()
                .sourceLanguage(cronJob.getSourceLanguage())
                .targetLanguages(cronJob.getTargetLanguages())
                .userHint(cronJob.getHint())
                .serviceSettings(cronJob.getTranslationServiceSettings())
                .skipTranslated(cronJob.getSkipTranslated())
                .saveForApproval(cronJob.getSaveForApproval())
                .automaticPlurals(cronJob.isAutomaticPlurals());
    }


    public List<CxProposedTranslationModel> getValues() {
        return values;
    }

    public Consumer<CxProposedTranslationModel> getAfterTranslationCallback() {
        return afterTranslationCallback;
    }

    public static final class Builder extends CxAutomaticTranslationParameters.Builder<Builder> {
        private List<CxProposedTranslationModel> values;
        private Consumer<CxProposedTranslationModel> afterTranslationCallback;

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }


        public CxPerformTranslationParameters build() {
            return new CxPerformTranslationParameters(this);
        }

        public Builder values(final List<CxProposedTranslationModel> valuesToTranslate) {
            this.values = valuesToTranslate;
            return this;
        }

        public Builder afterTranslationCallback(final Consumer<CxProposedTranslationModel> afterTranslation) {
            this.afterTranslationCallback = afterTranslation;
            return this;
        }
    }
}
