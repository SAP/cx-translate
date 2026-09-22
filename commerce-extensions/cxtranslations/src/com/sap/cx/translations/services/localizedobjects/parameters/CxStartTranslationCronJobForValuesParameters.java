/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects.parameters;

import com.sap.cx.translations.model.CxLocalizedValueModel;

import java.util.Collection;

/**
 * Parameters for kicking off a translation cron job that targets CxLocalizedValueModel objects.
 */
public class CxStartTranslationCronJobForValuesParameters extends CxAutomaticTranslationParameters {
    private final Collection<CxLocalizedValueModel> localizedValues;

    private CxStartTranslationCronJobForValuesParameters(final Builder builder) {
        super(builder);
        this.localizedValues = builder.localizedValues;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Collection<CxLocalizedValueModel> getLocalizedValues() {
        return localizedValues;
    }

    public static final class Builder extends CxAutomaticTranslationParameters.Builder<Builder> {
        private Collection<CxLocalizedValueModel> localizedValues;

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder localizedValues(final Collection<CxLocalizedValueModel> localizedValues) {
            this.localizedValues = localizedValues;
            return this;
        }

        public CxStartTranslationCronJobForValuesParameters build() {
            return new CxStartTranslationCronJobForValuesParameters(this);
        }
    }
}
