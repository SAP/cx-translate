/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects.parameters;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;

import java.util.Collection;
import java.util.Set;

/**
 * Parameters for initiating a translation cron job that works on whole items, including selected attributes
 * and optional product feature translation support.
 */
public class CxStartTranslationCronJobForItemsParameters extends CxAutomaticTranslationParameters {
    private final Collection<ItemModel> items;
    private final Set<AttributeDescriptorModel> attributes;
    private final boolean translateProductFeatures;

    private CxStartTranslationCronJobForItemsParameters(final Builder builder) {
        super(builder);
        this.items = builder.items;
        this.attributes = builder.attributes;
        this.translateProductFeatures = builder.translateProductFeatures;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Collection<ItemModel> getItems() {
        return items;
    }

    public Set<AttributeDescriptorModel> getAttributes() {
        return attributes;
    }

    public boolean isTranslateProductFeatures() {
        return translateProductFeatures;
    }

    public static final class Builder extends CxAutomaticTranslationParameters.Builder<Builder> {
        private Collection<ItemModel> items;
        private Set<AttributeDescriptorModel> attributes;
        private boolean translateProductFeatures;

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder items(final Collection<ItemModel> items) {
            this.items = items;
            return this;
        }

        public Builder attributes(final Set<AttributeDescriptorModel> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder translateProductFeatures(final boolean translateProductFeatures) {
            this.translateProductFeatures = translateProductFeatures;
            return this;
        }

        public CxStartTranslationCronJobForItemsParameters build() {
            return new CxStartTranslationCronJobForItemsParameters(this);
        }
    }
}
