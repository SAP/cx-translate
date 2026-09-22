/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;

/**
 * Dynamic attribute handler that computes the proposed translation cost for a
 * {@link CxProposedTranslationModel}.
 *
 * <p>The handler looks up the translation cron job attached to the proposed
 * translation and uses the cron job's translation service settings. If the
 * service settings are an instance of {@link CxAiTranslationServiceSettingsModel}
 * it multiplies each usage metric in the proposed translation's {@code usageInfo}
 * by the configured per-unit cost defined in the AI settings and returns the sum.
 * If the cron job or usage info is missing or the service settings are not AI
 * settings, the cost is treated as 0.0.</p>
 *
 * <p>This attribute is read-only; calling {@code set(...)} will throw
 * {@link UnsupportedOperationException}.</p>
 */
public class CxProposedTranslationCostAttributeHandler implements DynamicAttributeHandler<Double, CxProposedTranslationModel> {

    /**
     * Calculate the proposed translation cost for the provided model.
     *
     * @param model the proposed translation model containing usage info and a
     *              reference to the translation cron job
     * @return the computed total cost as a Double; 0.0 when inputs are missing or
     *         settings are not AI settings
     */
    @Override
    public Double get(final CxProposedTranslationModel model) {
        if (model.getTranslationCronJob() == null || model.getUsageInfo() == null) {
            return 0.0;
        }
        final CxTranslationServiceSettingsModel translationServiceSettings = model.getTranslationCronJob().getTranslationServiceSettings();

        if (translationServiceSettings instanceof final CxAiTranslationServiceSettingsModel aiSettings) {
            return model.getUsageInfo().entrySet().stream()
                    .mapToDouble(usageEntry -> {
                        final double costPerUnit = aiSettings.getUsageMetricsCost() != null
                                ? aiSettings.getUsageMetricsCost().getOrDefault(usageEntry.getKey(), 0.0)
                                : 0.0;
                        return costPerUnit * usageEntry.getValue();
                    }).sum();
        }

        return 0.0;
    }

    /**
     * Setting the value of this attribute is not supported. The attribute is read-only
     * and derived from usage data and service settings.
     *
     * @param model the proposed translation model
     * @param aDouble ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void set(final CxProposedTranslationModel model, final Double aDouble) {
        throw new UnsupportedOperationException("Read only attribute");
    }
}
