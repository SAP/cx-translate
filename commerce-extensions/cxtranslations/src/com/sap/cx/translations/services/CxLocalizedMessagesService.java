/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.services;

import java.util.Locale;

/**
 * Wrapper for messageSource
 */
public interface CxLocalizedMessagesService {
	/**
	 * Gets localized message from message source in current locale
	 *
	 * @param key        of the message
	 * @param attributes of the message
	 * @return message or exception is thrown if message is not present for the given key
	 */
	String getLocalizedMessage(String key, Object... attributes);

	/**
	 * Gets localized message from message source in the given locale
	 *
	 * @param key        of the message
	 * @param locale     message locale
	 * @param attributes of the message
	 * @return message or exception is thrown if message is not present for the given key
	 */
	String getLocalizedMessage(String key, Locale locale, Object... attributes);

	/**
	 * Gets localized message from message source in current locale
	 *
	 * @param key            of the message
	 * @param defaultMessage in case there is no message for the given key
	 * @param attributes     of the message
	 * @return message or default message
	 */
	String getLocalizedMessage(String key, String defaultMessage, Object... attributes);

	/**
	 * Gets localized message from message source in the given locale
	 *
	 * @param key            of the message
	 * @param defaultMessage in case there is no message for the given key
	 * @param locale         message locale
	 * @param attributes     of the message
	 * @return message or default message
	 */
	String getLocalizedMessage(String key, String defaultMessage, Locale locale, Object... attributes);
}
