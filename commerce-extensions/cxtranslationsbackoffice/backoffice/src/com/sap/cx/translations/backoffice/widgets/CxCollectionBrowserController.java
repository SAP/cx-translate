/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.backoffice.widgets;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.widgets.collectionbrowser.CollectionBrowserController;
import de.hybris.platform.core.model.c2l.LanguageModel;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;

/**
 * CUstomized Collection browser reacting to new translation values provided on input socket
 */
public class CxCollectionBrowserController extends CollectionBrowserController {

	private static final String TRANSLATION_LANGUAUGES_SOCKET_IN = "cxTranslationLanguages";

	@SocketEvent(socketId = TRANSLATION_LANGUAUGES_SOCKET_IN)
	public void process(final Map<String, LanguageModel> translationLanguages) {
		if (MapUtils.isNotEmpty(translationLanguages)) {
			translationLanguages.entrySet()
								.forEach(entry ->
										setValue(entry.getKey(), entry.getValue()));
		}
		refreshPageable();
	}

}
