/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.localizedvalue;

import com.hybris.cockpitng.dataaccess.context.Context;
import com.hybris.cockpitng.dataaccess.facades.search.FieldSearchFacadeStrategy;
import com.hybris.cockpitng.dataaccess.facades.type.DataType;
import com.hybris.cockpitng.search.data.SearchQueryData;
import com.hybris.cockpitng.search.data.pageable.Pageable;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.sap.cx.translations.backoffice.actions.renderer.CxFilterLocalizedValuesFilterRender;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService;
import de.hybris.platform.servicelayer.session.SessionService;
import org.springframework.core.Ordered;

import java.util.Set;

/**
 * Backoffice Search Facade for {@link CxLocalizedValueModel}.
 */
public class CxLocalizedValueBackofficeSearchFacadeStrategy implements FieldSearchFacadeStrategy<CxLocalizedValueModel>, Ordered {
    private CxLocalizedValueService localizedValueService;
    private NotificationService notificationService;
    private SessionService sessionService;

    @Override
    public boolean canHandle(final String typeCode) {
        return CxLocalizedValueModel._TYPECODE.equals(typeCode);
    }

    @Override
    public Pageable<CxLocalizedValueModel> search(final SearchQueryData searchQueryData) {
        final Set<String> includedConfigs = getForcedConfigs();
        return new CxTranslationsPageableAdapter(searchQueryData, localizedValueService, notificationService, includedConfigs);
    }

    private Set<String> getForcedConfigs() {
        final Set<String> includedInSearch = sessionService.getAttribute(CxFilterLocalizedValuesFilterRender.SESSION_ATTR_INCLUDE_IN_SEARCH);
        return includedInSearch != null ? includedInSearch : Set.of();

    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean isSortable(final DataType type, final String attributeQualifier, final Context context) {
        return attributeQualifier.equals(CxLocalizedValueModel.KEY);
    }

    public void setLocalizedValueService(final CxLocalizedValueService localizedValueService) {
        this.localizedValueService = localizedValueService;
    }

    public void setNotificationService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setSessionService(final SessionService sessionService) {
        this.sessionService = sessionService;
    }
}
