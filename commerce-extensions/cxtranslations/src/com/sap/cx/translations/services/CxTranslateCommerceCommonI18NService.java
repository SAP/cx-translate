/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.services;

import de.hybris.platform.commerceservices.i18n.CommerceCommonI18NService;
import de.hybris.platform.commerceservices.i18n.impl.DefaultCommerceCommonI18NService;

import java.util.Locale;

/**
 * Cx translation specific extension of {@link DefaultCommerceCommonI18NService} to retrieve Locale for given string
 */
public interface CxTranslateCommerceCommonI18NService extends CommerceCommonI18NService {
	/**
	 * Retrieve {@link Locale} for given localeString
	 *
	 * @param localeString representing the lang or locale string
	 * @return {@link Locale}
	 */
	Locale getLocale(String localeString);
}
