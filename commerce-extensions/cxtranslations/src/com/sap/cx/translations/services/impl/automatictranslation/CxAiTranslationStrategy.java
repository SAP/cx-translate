/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.automatictranslation;

import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;

import java.util.List;
import java.util.function.Consumer;

/**
 * Defines a translation strategy backed by an AI provider.
 * Strategies decide if they support given settings and perform single or bulk translations.
 */
public interface CxAiTranslationStrategy {
    /**
     * Signals whether this strategy can handle the provided settings instance.
     * Normally specific type of settings is expected to be handled by a specific strategy.
     *
     * @param settings translation service configuration to evaluate
     * @return {@code true} when the strategy can process requests with these settings
     */
    boolean supports(CxTranslationServiceSettingsModel settings);

    /**
     * Translates a single proposed value using an AI provider.
     *
     * @param value            proposed translation to process
     * @param globalHint       optional hint shared across translations
     * @param settings         strategy-specific configuration and credentials
     * @param afterTranslation callback invoked once translation completes
     */
    void aiTranslate(
            CxProposedTranslationModel value,
            String globalHint, CxTranslationServiceSettingsModel settings,
            final Consumer<CxProposedTranslationModel> afterTranslation);

    /**
     * Translates multiple proposed values. Translations are expected to have a single source language.
     *
     * @param value            proposed translations to process
     * @param globalHint       optional hint shared across translations
     * @param settings         strategy-specific configuration and credentials
     * @param afterTranslation callback invoked for each translated value
     */
    default void aiTranslate(
            final List<CxProposedTranslationModel> value,
            final String globalHint, final CxTranslationServiceSettingsModel settings, final Consumer<CxProposedTranslationModel> afterTranslation
    ) {
        value.forEach(v -> aiTranslate(v, globalHint, settings, afterTranslation));
    }
}
