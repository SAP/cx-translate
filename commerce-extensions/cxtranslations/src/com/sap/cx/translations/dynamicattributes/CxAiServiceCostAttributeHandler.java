/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;

/**
 * Dynamic attribute handler that computes the total cost for an AI translation service
 * settings model.
 *
 * <p>The handler multiplies each entry in the model's {@code usageInfo} map by the
 * configured per-unit cost from {@code usageMetricsCost} and returns the sum. If the
 * usage information is missing the cost is 0.0.</p>
 *
 * <p>This attribute is read-only; calling {@code set(...)} will throw
 * {@link UnsupportedOperationException}.</p>
 */
public class CxAiServiceCostAttributeHandler implements DynamicAttributeHandler<Double, CxAiTranslationServiceSettingsModel> {

    /**
     * Calculate the total cost for the provided AI translation service settings model.
     *
     * @param model the AI translation service settings model containing usage info and
     *              cost per metric
     * @return the computed total cost as a Double; 0.0 when usage info is null
     */
    @Override
    public Double get(final CxAiTranslationServiceSettingsModel model) {
        if (model.getUsageInfo() == null) {
            return 0.0;
        }
        return model.getUsageInfo().entrySet().stream()
                .mapToDouble(usageEntry -> {
                    final double costPerUnit = model.getUsageMetricsCost() != null
                            ? model.getUsageMetricsCost().getOrDefault(usageEntry.getKey(), 0.0)
                            : 0.0;
                    return costPerUnit * usageEntry.getValue();
                }).sum();

    }

    /**
     * Setting the value of this attribute is not supported. The attribute is read-only
     * and computed from usage data on the model.
     *
     * @param model the AI service settings model
     * @param aDouble ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void set(final CxAiTranslationServiceSettingsModel model, final Double aDouble) {
        throw new UnsupportedOperationException("Read only attribute");
    }
}
