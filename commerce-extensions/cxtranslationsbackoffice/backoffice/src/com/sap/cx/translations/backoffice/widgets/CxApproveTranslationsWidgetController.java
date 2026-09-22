/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.backoffice.widgets;

import com.hybris.backoffice.widgets.processes.ProcessesController;
import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.core.events.CockpitEventQueue;
import com.hybris.cockpitng.core.events.impl.DefaultCockpitEvent;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.dataaccess.services.PropertyValueService;
import com.hybris.cockpitng.util.DefaultWidgetController;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.sap.cx.translations.model.CxApproveTranslationsCronJobModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.services.CxApproveTranslationsService;
import de.hybris.platform.servicelayer.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;

import jakarta.annotation.Resource;
import java.util.List;


/**
 * AI Controller for action for approving the proposed translations
 */
public class CxApproveTranslationsWidgetController extends DefaultWidgetController {

	private static final String SOCKET_OUTPUT_CLOSE = "close";
	private static final long serialVersionUID = -2828429349739223090L;
	private static final String WIDGET_TITLE = "label.CxApproveTranslationsWidgetController.title";
	private static final String NOTIFICATION_TYPE = "JustMessage";
	private static final String SUCCESS_MESSAGE = "cxtranslations.approved.success";

	@Resource
	protected transient PermissionFacade permissionFacade;

	@Resource
	private PropertyValueService propertyValueService;

	@Resource
	private ObjectFacade objectFacade;

	@Resource
	private transient NotificationService notificationService;

	@Resource
	private CxApproveTranslationsService cxApproveTranslationsService;

	@Resource
	CockpitEventQueue cockpitEventQueue;

	@Resource
	UserService userService;

	@Wire("count")
	private Textbox count;

	@Wire("cancelButton")
	private Button cancelButton;

	@Wire("approveButton")
	private Button approveButton;

	protected transient List<CxProposedTranslationModel> itemsToApprove;

	@SocketEvent(socketId = "itemsToApprove")
	public void initializeWithContext(final Object data) {
		itemsToApprove = data instanceof List<?> ? (List<CxProposedTranslationModel>) data : java.util.Collections.emptyList();
		if (itemsToApprove.isEmpty()) {
			approveButton.setDisabled(true);
		}

		setWidgetTitle(getLabel(WIDGET_TITLE));
		count.setValue(String.valueOf(itemsToApprove.size()));
		count.setReadonly(true);
	}

	@ViewEvent(componentID = "approveButton", eventName = Events.ON_CLICK)
	public void approveButton() {

		int itemsToApproveCount = itemsToApprove.size();

		cancelButton.setDisabled(true);
		approveButton.setDisabled(true);

		final CxApproveTranslationsCronJobModel cronJob = cxApproveTranslationsService.approveTranslations(itemsToApprove);

		cockpitEventQueue.publishEvent(new DefaultCockpitEvent(ProcessesController.GLOBAL_EVENT_UPDATE_PROCESS_FOR_CRON_JOB, cronJob.getCode(), null));

		sendOutput(SOCKET_OUTPUT_CLOSE, null);

		notificationService.notifyUser(getWidgetInstanceManager(), NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS, getLabel(SUCCESS_MESSAGE, new Object[]{itemsToApproveCount}));

	}

	@ViewEvent(componentID = "cancelButton", eventName = Events.ON_CLICK)
	public void cancel() {
		sendOutput(SOCKET_OUTPUT_CLOSE, null);
	}
}
