/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;

/**
 * Dynamic attribute handler that computes the total cost for a translation cron job.
 *
 * <p>The computed cost is derived from the cron job's {@code usageInfo} map and the
 * associated translation service settings. When the translation service settings are an
 * instance of {@link CxAiTranslationServiceSettingsModel} the handler multiplies each
 * usage metric value by the corresponding per-unit cost defined in
 * {@code usageMetricsCost} and returns the sum of all such metric costs. If usage
 * information is missing or the service settings are not AI settings, the cost is
 * treated as 0.0.</p>
 *
 * <p>This attribute is read-only; calling {@code set(...)} will throw
 * {@link UnsupportedOperationException}.</p>
 */
public class    CxTranslationCronJobCostAttributeHandler implements DynamicAttributeHandler<Double, CxTranslationCronJobModel> {

    /**
     * Calculate the total cost for the provided cron job model.
     *
     * @param model the cron job model which contains usage information and a reference
     *              to the translation service settings; must not be null
     * @return the computed total cost as a Double. Returns 0.0 when usage info is
     *         missing/empty or when service settings are not AI settings.
     */
    @Override
    public Double get(final CxTranslationCronJobModel model) {
        if (model.getUsageInfo() == null || model.getUsageInfo().isEmpty()) {
            return 0.0;
        }

        final CxTranslationServiceSettingsModel translationServiceSettings = model.getTranslationServiceSettings();

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
     * and is derived from other model data.
     *
     * @param model the cron job model
     * @param aDouble ignored
     * @throws UnsupportedOperationException always thrown to signal the attribute is read-only
     */
    @Override
    public void set(final CxTranslationCronJobModel model, final Double aDouble) {
        throw new UnsupportedOperationException("Read only attribute");
    }
}
