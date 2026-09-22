/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.services.impl;

import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import de.hybris.platform.commerceservices.i18n.impl.DefaultCommerceCommonI18NService;
import de.hybris.platform.core.model.c2l.LanguageModel;

import java.util.Locale;

/**
 * Cx translation specific extension of {@link DefaultCommerceCommonI18NService} to retrieve Locale for given string
 */
public class CxTranslateCommerceCommonI18NServiceImpl extends DefaultCommerceCommonI18NService implements CxTranslateCommerceCommonI18NService {

	/**
	 * Retrieve {@link Locale} for given localeString. Expose the protected method from super class.
	 *
	 * @param localeString representing the lang or locale string
	 * @return {@link Locale}
	 */
	@Override
	public Locale getLocale(final String localeString) {
		return super.createLocale(localeString);
	}
    
    //override because DefaultCommerceCommonI18NService implementation is not fit for general use
    @Override
    public Locale getLocaleForLanguage(final LanguageModel language) {
        return getLocale(language.getIsocode());
    }
}
