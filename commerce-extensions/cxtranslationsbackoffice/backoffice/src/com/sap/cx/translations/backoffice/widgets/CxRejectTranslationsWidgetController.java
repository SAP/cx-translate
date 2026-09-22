/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.backoffice.widgets;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectDeletionException;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.util.DefaultWidgetController;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;

import java.util.List;


/**
 * Controller for action for rejecting (removing) the proposed translations
 */
public class CxRejectTranslationsWidgetController extends DefaultWidgetController {

	private static final String SOCKET_OUTPUT_CLOSE = "close";
	private static final long serialVersionUID = -3928429349739223091L;
	private static final String WIDGET_TITLE = "label.CxRejectTranslationsWidgetController.title";
	private static final String NOTIFICATION_TYPE = "JustMessage";
	private static final String SUCCESS_MESSAGE = "cxtranslations.rejected.success";
	private static final String ERROR_MESSAGE = "cxtranslations.rejected.error";

	private static final Logger LOG = LoggerFactory.getLogger(CxRejectTranslationsWidgetController.class);

	@Resource
	protected transient PermissionFacade permissionFacade;

	@Resource
	private ObjectFacade objectFacade;

	@Resource
	private transient NotificationService notificationService;


	@Wire("count")
	private Textbox count;

	@Wire("cancelButton")
	private Button cancelButton;

	@Wire("rejectButton")
	private Button rejectButton;

	protected transient List<CxProposedTranslationModel> itemsToReject;

	@SocketEvent(socketId = "itemsToReject")
	public void initializeWithContext(final Object data) {
		itemsToReject = data instanceof List<?> ? (List<CxProposedTranslationModel>) data : java.util.Collections.emptyList();
		if (itemsToReject.isEmpty()) {
			rejectButton.setDisabled(true);
		}

		setWidgetTitle(getLabel(WIDGET_TITLE));
		count.setValue(String.valueOf(itemsToReject.size()));
		count.setReadonly(true);
	}

	@ViewEvent(componentID = "rejectButton", eventName = Events.ON_CLICK)
	public void rejectButton() {
		final int totalCount = itemsToReject.size();

		cancelButton.setDisabled(true);
		rejectButton.setDisabled(true);

		int failedCount = 0;
		for (final CxProposedTranslationModel item : itemsToReject) {
			try {
				objectFacade.delete(item);
			} catch (final ObjectDeletionException e) {
				LOG.error("Failed to remove proposed translation item: {}", item.getPk(), e);
				failedCount++;
			}
		}

		sendOutput(SOCKET_OUTPUT_CLOSE, null);

		if (failedCount == 0) {
			notificationService.notifyUser(getWidgetInstanceManager(), NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS,
					getLabel(SUCCESS_MESSAGE, new Object[]{totalCount}));
		} else {
			notificationService.notifyUser(getWidgetInstanceManager(), NOTIFICATION_TYPE, NotificationEvent.Level.WARNING,
					getLabel(ERROR_MESSAGE, new Object[]{failedCount, totalCount}));
		}
	}

	@ViewEvent(componentID = "cancelButton", eventName = Events.ON_CLICK)
	public void cancel() {
		sendOutput(SOCKET_OUTPUT_CLOSE, null);
	}
}

