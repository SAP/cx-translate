/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.backoffice.widgets;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.util.DefaultWidgetController;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.sap.cx.translations.backoffice.CxTranslationsBackofficeConstants;
import de.hybris.platform.commerceservices.i18n.CommerceCommonI18NService;
import de.hybris.platform.core.model.c2l.LanguageModel;
import org.apache.commons.collections4.CollectionUtils;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ListModelList;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Popup widget showing a selection for translation languages. Sending the selected languages on output socket.
 */
public class CxTranslationLanguagesWidgetController extends DefaultWidgetController {
	private static final long serialVersionUID = 1L;
	private static final String WIDGET_TITLE = "label.CxTranslationLanguagesWidgetController.title";

	protected static final String SOCKET_OUTPUT_CANCEL = "cancel";
	protected static final String SOCKET_OUTPUT_TRANSLATION_LANGUAGES = "translationLanguages";

	@Resource
	protected transient CommerceCommonI18NService commerceCommonI18NService;
	@Resource
	protected transient PermissionFacade permissionFacade;
	@Resource
	private transient NotificationService notificationService;

	@Wire
	protected Combobox primaryLanguageSelector;
	@Wire
	protected Combobox secondaryLanguageSelector;

	private final ListModelList<LanguageModel> primaryLanguageSelectorModel = new ListModelList();
	private final ListModelList<LanguageModel> secondaryLanguageSelectorModel = new ListModelList();


	@SocketEvent(socketId = "openWidget")
	public void initializeWithContext(final Object data) {

		setWidgetTitle(getLabel(WIDGET_TITLE));

		final Set<Locale> readableLocales = this.permissionFacade.getAllReadableLocalesForCurrentUser();
		final Set<Locale> writableLocales = this.permissionFacade.getAllWritableLocalesForCurrentUser();

		final Collection<LanguageModel> allLanguages = this.commerceCommonI18NService.getAllLanguages();
		final LanguageModel currentLanguage = this.commerceCommonI18NService.getCurrentLanguage();

		if (CollectionUtils.isNotEmpty(readableLocales)) {
			final Set<String> readableIsoCodes = readableLocales.stream()
																.map(locale -> locale.toString())
																.collect(Collectors.toSet());
			primaryLanguageSelectorModel.addAll(allLanguages.stream()
															.filter(lang -> readableIsoCodes.contains(lang.getIsocode()))
															.collect(Collectors.toSet()));
			primaryLanguageSelectorModel.addToSelection(currentLanguage);
		}

		if (CollectionUtils.isNotEmpty(writableLocales)) {
			final Set<String> writableIsoCodes = writableLocales.stream()
																.map(locale -> locale.toString())
																.collect(Collectors.toSet());
			secondaryLanguageSelectorModel.addAll(allLanguages.stream()
															  .filter(lang -> writableIsoCodes.contains(lang.getIsocode()))
															  .collect(Collectors.toSet()));
		}

		primaryLanguageSelector.setModel(primaryLanguageSelectorModel);
		secondaryLanguageSelector.setModel(secondaryLanguageSelectorModel);
	}

	@ViewEvent(componentID = "saveButton", eventName = Events.ON_CLICK)
	public void save() {
		final Map<String, Object> parameters = new HashMap();
		parameters.put(CxTranslationsBackofficeConstants.PRIMARY_LANGUAGE_PARAM, primaryLanguageSelector.getSelectedItem()
																										.getValue());
		parameters.put(CxTranslationsBackofficeConstants.SECONDARY_LANGUAGE_PARAM, secondaryLanguageSelector.getSelectedItem()
																											.getValue());
		sendOutput(SOCKET_OUTPUT_TRANSLATION_LANGUAGES, parameters);
	}


	@ViewEvent(componentID = "cancelButton", eventName = Events.ON_CLICK)
	public void cancel() {
		sendOutput(SOCKET_OUTPUT_CANCEL, null);
	}
}
