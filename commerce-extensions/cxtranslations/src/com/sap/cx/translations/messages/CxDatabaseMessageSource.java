/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.messages;

import com.sap.cx.translations.facades.CxTranslationsFacade;
import com.sap.cx.translations.labels.UsageType;
import de.hybris.platform.commerceservices.i18n.CommerceCommonI18NService;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.session.SessionExecutionBody;
import de.hybris.platform.servicelayer.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractMessageSource;

import jakarta.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * Database message source delegating logic to {@link CxTranslationsFacade}
 */
public class CxDatabaseMessageSource extends AbstractMessageSource {
	private final static Logger LOG = LoggerFactory.getLogger(CxDatabaseMessageSource.class);
	
	private CxTranslationsFacade translationsFacade;
	private SessionService sessionService;
	private CommerceCommonI18NService commerceCommonI18NService;
	private CommonI18NService commonI18NService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nullable
	protected MessageFormat resolveCode(final String code, final Locale locale) {
		return sessionService.executeInLocalView(new SessionExecutionBody() {
			@Override
			public Object execute() {
                final LanguageModel language;
                try {
                    language = commonI18NService.getLanguage(locale.getLanguage());
                } catch (UnknownIdentifierException e) {
					// Demoted from ERROR — this can be hit on every message lookup for unsupported locales
					// (e.g. crawlers sending uncommon Accept-Language), flooding production logs.
					LOG.debug("Could not find language for locale {}, when translating key {}", locale.getLanguage(), code);
					return null;
                }
                final LanguageModel currentLanguage = commerceCommonI18NService.getCurrentLanguage();
				commerceCommonI18NService.setCurrentLanguage(language);

				try {
					final String message = translationsFacade.getLabel(code, UsageType.BACKEND);

					if (message != null) {
						return createMessageFormat(message, locale);
					}

					return null;
				} finally {
					// Only restore the original language if there was one — calling setCurrentLanguage(null)
					// on an uninitialised session would leave it in a broken state.
					if (currentLanguage != null) {
						commerceCommonI18NService.setCurrentLanguage(currentLanguage);
					}
				}
			}
		});
	}

	public void setTranslationsFacade(final CxTranslationsFacade translationsFacade) {
		this.translationsFacade = translationsFacade;
	}

	public void setSessionService(final SessionService sessionService) {
		this.sessionService = sessionService;
	}

	public void setCommerceCommonI18NService(final CommerceCommonI18NService commerceCommonI18NService) {
		this.commerceCommonI18NService = commerceCommonI18NService;
	}

	public void setCommonI18NService(final CommonI18NService commonI18NService) {
		this.commonI18NService = commonI18NService;
	}
}
