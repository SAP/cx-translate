/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.widgets;

import com.hybris.backoffice.widgets.processes.ProcessesController;
import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.core.events.CockpitEventQueue;
import com.hybris.cockpitng.core.events.impl.DefaultCockpitEvent;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.search.data.pageable.Pageable;
import com.hybris.cockpitng.util.DefaultWidgetController;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.sap.cx.translations.daos.CxTranslationServiceSettingsDao;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService;
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForValuesParameters;
import de.hybris.platform.commerceservices.i18n.CommerceCommonI18NService;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkmax.zul.Chosenbox;
import org.zkoss.zul.*;

import java.util.*;
import java.util.stream.Collectors;


/**
 * AI Controller for action for translating Localized Values
 */
public class CxAiTranslateValuesWidgetController extends DefaultWidgetController {
	protected static final String SOCKET_OUTPUT_CLOSE = "close";
	protected static final long serialVersionUID = -2828429349739223090L;
	protected static final String WIDGET_TITLE = "label.CxAiTranslateValuesWidgetController.title";
	public static final String SETTINGS_SESSION_KEY = "cxtranslate_ai_settings";

	protected static final String NOTIFICATION_TYPE = "JustMessage";
	protected static final String SUCCESS_MESSAGE = "cxtranslations.translate.success";
	protected transient Pageable<ItemModel> dataPageable;



	@Resource
	protected transient CommerceCommonI18NService commerceCommonI18NService;
	@Resource
	protected transient PermissionFacade permissionFacade;
	@Resource
	protected transient NotificationService notificationService;
	@Resource
	protected CockpitEventQueue cockpitEventQueue;

	@Resource
	protected transient CxTranslationServiceSettingsDao cxAiTranslationServiceSettingsDao;

	@Resource
	protected transient CxLocalizedValueService cxLocalizedValueService;

	@Wire
	protected Combobox primaryLanguageSelector;

	@Wire
	protected Combobox aiSettingsSelector;

	@Wire
	protected Chosenbox secondaryLanguageSelector;

	@Wire
	protected Textbox translationHint;

	@Wire
	protected Button translateButton;

	@Wire
	protected Textbox count;

	@Wire
	protected Checkbox approval;

	@Wire
	protected Checkbox skipTranslated;

	protected final ListModelList<LanguageModel> primaryLanguageSelectorModel = new ListModelList();

	protected final ListModelList<CxTranslationServiceSettingsModel> aiSettingsSelectorModel = new ListModelList();

	protected final ListModelList<LanguageModel> secondaryLanguageSelectorModel = new ListModelList<>();
	protected transient List<LanguageModel> targetLanguages;

	@SocketEvent(socketId = "itemsToTranslate")
	public void initializeWithContext(final Object data) {
		dataPageable = (Pageable<ItemModel>) data;
		secondaryLanguageSelectorModel.setMultiple(true);

		count.setValue(String.valueOf(dataPageable.getTotalCount()));

		setWidgetTitle(getLabel(WIDGET_TITLE));
		final Set<Locale> readableLocales = this.permissionFacade.getAllReadableLocalesForCurrentUser();
		final Set<Locale> writableLocales = this.permissionFacade.getAllWritableLocalesForCurrentUser();

		final List<CxTranslationServiceSettingsModel> aiSettings = this.cxAiTranslationServiceSettingsDao.find();
		aiSettingsSelectorModel.addAll(aiSettings);
		aiSettingsSelectorModel.setSelection(aiSettings.stream().filter(s -> s.getIsDefault()).limit(1).toList());

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
																.map(Locale::toString)
																.collect(Collectors.toSet());
			targetLanguages = allLanguages.stream()
										  .filter(lang -> writableIsoCodes.contains(lang.getIsocode()))
										  .collect(Collectors.toList());
			secondaryLanguageSelectorModel.addAll(targetLanguages);
		}
		
		final SavedSettings savedSettings = (SavedSettings) Sessions.getCurrent().getAttribute(SETTINGS_SESSION_KEY);

		if (savedSettings != null) {
            primaryLanguageSelectorModel.setSelection(savedSettings.getSourceLanguage());
            secondaryLanguageSelectorModel.setSelection(savedSettings.getTargetLanguages());
            aiSettingsSelectorModel.setSelection(savedSettings.getTranslationSettings());
            approval.setChecked(savedSettings.getApproval());
            skipTranslated.setChecked(savedSettings.getSkipTranslated());
			updateTranslateButton();
		}

		primaryLanguageSelector.setModel(primaryLanguageSelectorModel);
		secondaryLanguageSelector.setModel(secondaryLanguageSelectorModel);
		aiSettingsSelector.setModel(aiSettingsSelectorModel);
		removeSourceLanguageFromTargetSelection();
		addEventListeners();
	}

	@ViewEvent(componentID = "translateButton", eventName = Events.ON_CLICK)
	public void translateButton() {

		final CxTranslationServiceSettingsModel translationSettings = aiSettingsSelectorModel.getSelection()
																							   .stream()
																							   .findFirst()
																							   .get();

		final LanguageModel sourceLanguage = primaryLanguageSelectorModel.getSelection().stream().findFirst().get();

		final List<LanguageModel> selectedSecondaryLanguages = secondaryLanguageSelectorModel.getSelection().stream().toList();


		final List<ItemModel> itemsToTranslate = dataPageable.getAllResults();

		final List<CxLocalizedValueModel> valuesToTranslate = itemsToTranslate.stream()
				.filter(item -> item instanceof CxLocalizedValueModel)
				.map(item -> (CxLocalizedValueModel) item)
				.collect(Collectors.toList());

		final Integer elementsToProcess = valuesToTranslate.size();
		final CxStartTranslationCronJobForValuesParameters parameters = CxStartTranslationCronJobForValuesParameters.builder()
				.localizedValues(valuesToTranslate)
				.serviceSettings(translationSettings)
				.sourceLanguage(sourceLanguage)
				.targetLanguages(selectedSecondaryLanguages)
				.userHint(translationHint.getValue())
				.saveForApproval(approval.isChecked())
				.skipTranslated(skipTranslated.isChecked())
				.build();
		final CxTranslationCronJobModel cronJob = cxLocalizedValueService.startTranslationCronJob(parameters);

		cockpitEventQueue.publishEvent(new DefaultCockpitEvent(ProcessesController.GLOBAL_EVENT_UPDATE_PROCESS_FOR_CRON_JOB, cronJob.getCode(), null));

		notificationService.notifyUser(getWidgetInstanceManager(), NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS, getLabel(SUCCESS_MESSAGE, new Object[]{elementsToProcess}));

		Sessions.getCurrent()
                .setAttribute(SETTINGS_SESSION_KEY, SavedSettings.builder()
                        .sourceLanguage(primaryLanguageSelectorModel.getSelection())
                        .targetLanguages(secondaryLanguageSelectorModel.getSelection())
                        .translationSettings(aiSettingsSelectorModel.getSelection())
                        .approval(approval.isChecked())
                        .skipTranslated(skipTranslated.isChecked())
                        .build());
		sendOutput(SOCKET_OUTPUT_CLOSE, null);
	}

	protected void addEventListeners() {
		final EventListener<Event> updateListener = new EventListener<Event>() {
			@Override
			public void onEvent(final Event event) {
				removeSourceLanguageFromTargetSelection();
				updateTranslateButton();
			}
		};

		aiSettingsSelector.addEventListener(Events.ON_SELECT, updateListener);
		primaryLanguageSelector.addEventListener(Events.ON_SELECT, updateListener);
		secondaryLanguageSelector.addEventListener(Events.ON_SELECT, updateListener);
	}

	//removes selected source language from both selection and available items in target language choice
	protected void removeSourceLanguageFromTargetSelection() {
		final Set<LanguageModel> targetLanguagesSelection = new HashSet<>(secondaryLanguageSelectorModel.getSelection());
		final ArrayList<LanguageModel> selectableTargetLanguages = new ArrayList<>(targetLanguages);
		final Set<LanguageModel> primaryLanguageSelectorSelection = primaryLanguageSelectorModel.getSelection();
		selectableTargetLanguages.removeAll(primaryLanguageSelectorSelection);
		targetLanguagesSelection.removeAll(primaryLanguageSelectorSelection);
		secondaryLanguageSelectorModel.clear();
		secondaryLanguageSelectorModel.addAll(selectableTargetLanguages);
		secondaryLanguageSelectorModel.setSelection(targetLanguagesSelection);

	}

	protected void updateTranslateButton() {
		final boolean isAiSettingsSelected = CollectionUtils.isNotEmpty(aiSettingsSelectorModel.getSelection());
		final boolean isPrimaryLanguageSelected = CollectionUtils.isNotEmpty(primaryLanguageSelectorModel.getSelection());
		final boolean isSecondaryLanguageSelected = CollectionUtils.isNotEmpty(secondaryLanguageSelectorModel.getSelection());

		translateButton.setDisabled(!(isAiSettingsSelected && isPrimaryLanguageSelected && isSecondaryLanguageSelected));
	}

	@ViewEvent(componentID = "cancelButton", eventName = Events.ON_CLICK)
	public void cancel() {
		sendOutput(SOCKET_OUTPUT_CLOSE, null);
	}
}
