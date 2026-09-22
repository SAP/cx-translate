/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.services.impl;

import com.sap.cx.translations.services.CxLocalizedMessagesService;
import de.hybris.platform.servicelayer.i18n.I18NService;
import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * Default implementation of {@link CxLocalizedMessagesService}
 */
public class CxLocalizedMessagesServiceImpl implements CxLocalizedMessagesService {
	private MessageSource messageSource;
	private I18NService i18NService;

	@Override
	public String getLocalizedMessage(final String key, final Object... attributes) {
		return getLocalizedMessage(key, i18NService.getCurrentLocale(), attributes);
	}

	@Override
	public String getLocalizedMessage(final String key, final Locale locale, final Object... attributes) {
		return messageSource.getMessage(key, attributes, locale);
	}

	@Override
	public String getLocalizedMessage(final String key, final String defaultMessage, final Object... attributes) {
		return getLocalizedMessage(key, defaultMessage, i18NService.getCurrentLocale(), attributes);
	}

	@Override
	public String getLocalizedMessage(final String key, final String defaultMessage, final Locale locale, final Object... attributes) {
		return messageSource.getMessage(key, attributes, defaultMessage, locale);
	}

	public void setMessageSource(final MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setI18NService(final I18NService i18NService) {
		this.i18NService = i18NService;
	}
}
