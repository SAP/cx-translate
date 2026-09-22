/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedObjectConfigModel;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProviderFactory;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProviderFactoryStrategy;

import java.util.List;

/**
 * Implementation of Factory for {@link CxLocalizedObjectProvider}s.
 */
public class CxLocalizedObjectProviderFactoryImpl implements CxLocalizedObjectProviderFactory {
    private List<CxLocalizedObjectProviderFactoryStrategy> strategies;

    /**
     * {@inheritDoc}
     */
    @Override
    public CxLocalizedObjectProvider createProvider(final CxLocalizedValueSearchData searchData, final CxLocalizedObjectConfigModel config, final CxLocalizedValuesSearchSupport searchSupport) {
        return strategies.stream()
                .filter(strategy -> strategy.canHandle(config))
                .findFirst()
                .map(strategy -> strategy.createProvider(searchData, config, searchSupport))
                .orElse(null);
    }


    public void setStrategies(final List<CxLocalizedObjectProviderFactoryStrategy> strategies) {
        this.strategies = strategies;
    }
}
