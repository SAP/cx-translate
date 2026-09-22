/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.backoffice.strategy;

import com.hybris.cockpitng.core.config.impl.jaxb.listview.ListColumn;
import com.hybris.cockpitng.search.data.SortData;
import com.hybris.cockpitng.widgets.collectionbrowser.mold.impl.listview.ListViewCollectionBrowserMoldStrategy;
import com.sap.cx.translations.backoffice.CxTranslationsBackofficeConstants;
import com.sap.cx.translations.backoffice.renderers.CxLocalizedStringInlineEditWidgetComponentRenderer;
import de.hybris.platform.commerceservices.i18n.CommerceCommonI18NService;
import de.hybris.platform.core.Registry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.zkoss.zul.Listheader;

/**
 * Custom List view collection browser mold strategy adding a locale prefix to columns with configurable languages
 */
public class CxListViewCollectionBrowserMoldStrategy extends ListViewCollectionBrowserMoldStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(CxListViewCollectionBrowserMoldStrategy.class);
	private CommerceCommonI18NService commerceCommonI18NService;

	@Override
	protected Listheader createListHeader(final SortData sortData, final ListColumn column, final String typeCode) {

		final Listheader listHeader = super.createListHeader(sortData, column, typeCode);

		if (StringUtils.isBlank(column.getSpringBean())) {
			return listHeader;
		}

		final Object componentRenderer;
		try {
			componentRenderer = Registry.getApplicationContext()
										.getBean(column.getSpringBean());
		} catch (BeansException e) {
			LOG.error("Can't find spring bean for code {}", column.getSpringBean());
			return listHeader;
		}

		if (componentRenderer instanceof CxLocalizedStringInlineEditWidgetComponentRenderer) {

			final String primaryLanguage = getWidgetModel().getValue(CxTranslationsBackofficeConstants.PRIMARY_LANGUAGE_PARAM, java.lang.String.class);
			final String secondaryLanguage = getWidgetModel().getValue(CxTranslationsBackofficeConstants.SECONDARY_LANGUAGE_PARAM, java.lang.String.class);
			final String currentLanguage = commerceCommonI18NService.getCurrentLanguage()
																	.getIsocode();

			if ((((CxLocalizedStringInlineEditWidgetComponentRenderer) componentRenderer).getPrimaryLanguage())) {
				listHeader.setLabel(listHeader.getLabel() + " [" + (primaryLanguage != null ? primaryLanguage : currentLanguage) + "]");
			} else {
				listHeader.setLabel(listHeader.getLabel() + " [" + (secondaryLanguage != null ? secondaryLanguage : currentLanguage) + "]");
			}
		}

		return listHeader;
	}

		public void setCommerceCommonI18NService(final CommerceCommonI18NService commerceCommonI18NService) {
		this.commerceCommonI18NService = commerceCommonI18NService;
	}
}
