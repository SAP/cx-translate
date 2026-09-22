/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.backoffice.widgets;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.util.DefaultWidgetController;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.sap.cx.translations.daos.CxTranslationServiceSettingsDao;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Textbox;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Controller for action for retrying the proposed translations
 */
public class CxRetryTranslationsWidgetController extends DefaultWidgetController {

	private static final String SOCKET_OUTPUT_CLOSE = "close";
	private static final long serialVersionUID = -4928429349739223092L;
	private static final String WIDGET_TITLE = "label.CxRetryTranslationsWidgetController.title";
	private static final String NOTIFICATION_TYPE = "JustMessage";
	private static final String SUCCESS_MESSAGE = "cxtranslations.retried.success";
	private static final String ERROR_MESSAGE = "cxtranslations.retried.error";

	private static final Logger LOG = LoggerFactory.getLogger(CxRetryTranslationsWidgetController.class);

	@Resource
	protected transient PermissionFacade permissionFacade;

	@Resource
	private transient NotificationService notificationService;
    
    @Resource
    private transient CxLocalizedValueService cxLocalizedValueService;

	@Resource
	protected transient CxTranslationServiceSettingsDao cxAiTranslationServiceSettingsDao;

	@Wire("count")
	private Textbox count;

	@Wire("aiSettingsSelector")
	private Combobox aiSettingsSelector;

	@Wire("translationHint")
	private Textbox translationHint;

	@Wire("cancelButton")
	private Button cancelButton;

	@Wire("retryButton")
	private Button retryButton;

	protected final ListModelList<CxTranslationServiceSettingsModel> aiSettingsSelectorModel = new ListModelList<>();

	protected transient List<CxProposedTranslationModel> itemsToRetry;

	@SocketEvent(socketId = "itemsToRetry")
	public void initializeWithContext(final Object data) {
		itemsToRetry = data instanceof List<?> ? (List<CxProposedTranslationModel>) data : java.util.Collections.emptyList();
		if (itemsToRetry.isEmpty()) {
			retryButton.setDisabled(true);
		}

		setWidgetTitle(getLabel(WIDGET_TITLE));
		count.setValue(String.valueOf(itemsToRetry.size()));
		count.setReadonly(true);

		final List<CxTranslationServiceSettingsModel> aiSettings = this.cxAiTranslationServiceSettingsDao.find();
		aiSettingsSelectorModel.addAll(aiSettings);
		aiSettingsSelector.setModel(aiSettingsSelectorModel);
	}

	@ViewEvent(componentID = "retryButton", eventName = Events.ON_CLICK)
	public void retryButton() {
		final int totalCount = itemsToRetry.size();

		cancelButton.setDisabled(true);
		retryButton.setDisabled(true);

		retryTranslations(itemsToRetry);

		sendOutput(SOCKET_OUTPUT_CLOSE, null);

		notificationService.notifyUser(getWidgetInstanceManager(), NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS,
				getLabel(SUCCESS_MESSAGE, new Object[]{totalCount}));
	}


	protected void retryTranslations(final List<CxProposedTranslationModel> items) {
		final CxTranslationServiceSettingsModel settingsOverride = aiSettingsSelectorModel.getSelection()
				.stream()
				.findFirst()
				.orElse(null);
		final String hintOverride = translationHint.getValue();

        // Collectors.groupingBy forbids null keys; filter out orphaned proposed translations
        // whose cron job reference is null (e.g. cleared by FK cleanup) instead of NPE'ing.
        final List<CxProposedTranslationModel> skipped = items.stream()
                .filter(pt -> pt.getTranslationCronJob() == null)
                .toList();
        if (!skipped.isEmpty()) {
            LOG.warn("Skipping {} proposed translation(s) without a linked cron job during retry", skipped.size());
        }
        final Map<CxTranslationCronJobModel, List<CxProposedTranslationModel>> groupedByCronJob = items.stream()
                .filter(pt -> pt.getTranslationCronJob() != null)
                .collect(Collectors.groupingBy(CxProposedTranslationModel::getTranslationCronJob));
        groupedByCronJob.forEach((key, value) -> {
            cxLocalizedValueService.retryTranslations(key, value, settingsOverride, hintOverride);
        });
    }

	@ViewEvent(componentID = "cancelButton", eventName = Events.ON_CLICK)
	public void cancel() {
		sendOutput(SOCKET_OUTPUT_CLOSE, null);
	}
}

