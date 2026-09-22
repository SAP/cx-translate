/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.ActionResult.StatusFlag;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectDeletionException;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectSavingException;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.engine.impl.AbstractComponentWidgetAdapterAware;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.hybris.cockpitng.util.notifications.event.NotificationEventTypes;
import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.services.CxApproveTranslationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.EnumSet;

/**
 * Approves the single proposed value for translation storing it at the target item level.
 */
public class CxApproveAiTranslationAction extends AbstractComponentWidgetAdapterAware implements
		CockpitAction<CxProposedTranslationModel, Object> {

	private static final Logger LOG = LoggerFactory.getLogger(CxApproveAiTranslationAction.class);
	private static final String CONFIRMATION_LABEL_KEY = "cxtranslations.translationapproval.confirmation.singleitem";
	private static final String RESET_OUPUT = "reset";

	@Resource
	protected ObjectFacade objectFacade;

	@Resource
	protected CxApproveTranslationsService cxApproveTranslationsService;

	@Resource
	protected transient PermissionFacade permissionFacade;

	@Resource
	protected transient NotificationService notificationService;


	@Override
	public ActionResult<Object> perform(final ActionContext<CxProposedTranslationModel> ctx) {

		final CxProposedTranslationModel proposedTranslation = ctx.getData();

		try {
			objectFacade.save(proposedTranslation);
		} catch (final ObjectSavingException e) {
			LOG.error("Failed to save proposed translation with key {}", proposedTranslation.getKey(), e);
			notificationService.notifyUser(ctx, NotificationEventTypes.EVENT_TYPE_OBJECT_UPDATE,
					NotificationEvent.Level.FAILURE, Collections.singletonList(proposedTranslation.getTarget()));
			final ActionResult<Object> failureResult = new ActionResult<>(ActionResult.ERROR);
			failureResult.setStatusFlags(EnumSet.of(StatusFlag.OBJECT_MODIFIED));
			this.sendOutput(RESET_OUPUT, null);
			return failureResult;
		}

		ActionResult<Object> actionResult = new ActionResult<>(ActionResult.SUCCESS, proposedTranslation);

		if (cxApproveTranslationsService.approveTranslation(proposedTranslation)) {
			try {
				objectFacade.delete(proposedTranslation);
				actionResult.setStatusFlags(EnumSet.of(StatusFlag.OBJECT_DELETED));
				notificationService.notifyUser(ctx, NotificationEventTypes.EVENT_TYPE_OBJECT_UPDATE,
						NotificationEvent.Level.SUCCESS, Collections.singletonList(proposedTranslation.getTarget()));
			} catch (final ObjectDeletionException e) {
				// Approval succeeded but the proposed translation could not be removed —
				// report this back to the user instead of silently claiming OBJECT_DELETED.
				LOG.error("Approval succeeded but proposedTranslation removal failed for key {}", proposedTranslation.getKey(), e);
				actionResult = new ActionResult<>(ActionResult.ERROR);
				actionResult.setStatusFlags(EnumSet.of(StatusFlag.OBJECT_MODIFIED));
				notificationService.notifyUser(ctx, NotificationEventTypes.EVENT_TYPE_OBJECT_UPDATE,
						NotificationEvent.Level.FAILURE, Collections.singletonList(proposedTranslation.getTarget()));
			}
		} else {
			actionResult = new ActionResult<>(ActionResult.ERROR);
			notificationService.notifyUser(ctx, NotificationEventTypes.EVENT_TYPE_OBJECT_UPDATE,
					NotificationEvent.Level.FAILURE, Collections.singletonList(proposedTranslation.getTarget()));
			actionResult.setStatusFlags(EnumSet.of(StatusFlag.OBJECT_MODIFIED));
		}

		this.sendOutput(RESET_OUPUT, null);
		return actionResult;
	}

	@Override
	public boolean canPerform(final ActionContext<CxProposedTranslationModel> ctx) {
		final CxProposedTranslationModel labelToApprove = ctx.getData();

		if (labelToApprove == null || objectFacade.isDeleted(labelToApprove)) {
			return false;
		}

		return permissionFacade.canChangeType(CxProposedTranslationModel._TYPECODE)
				&& labelToApprove.getTarget() != null && validTranslationStatusForApproval(labelToApprove);
	}

	/**
	 * Only successfully translated or manually provided translations are eligible for approval
	 *
	 * @param labelToApprove
	 * @return
	 */
	protected boolean validTranslationStatusForApproval(final CxProposedTranslationModel labelToApprove) {
		return CxTranslationStatus.SUCCESS == labelToApprove.getTranslationStatus()
				|| CxTranslationStatus.MANUAL == labelToApprove.getTranslationStatus();
	}

	@Override
	public boolean needsConfirmation(final ActionContext<CxProposedTranslationModel> ctx) {
		return true;
	}

	@Override
	public String getConfirmationMessage(final ActionContext<CxProposedTranslationModel> ctx) {
		return Labels.getLabel(CONFIRMATION_LABEL_KEY);
	}
}
