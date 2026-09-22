/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.actions;

import com.google.common.collect.Lists;
import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.core.model.WidgetModel;
import com.hybris.cockpitng.dataaccess.context.impl.DefaultContext;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacadeOperationResult;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.engine.impl.AbstractComponentWidgetAdapterAware;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.hybris.cockpitng.util.notifications.event.NotificationEventTypes;
import com.hybris.cockpitng.util.type.BackofficeTypeUtils;
import com.hybris.cockpitng.widgets.collectionbrowser.CollectionBrowserController;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import de.hybris.platform.core.model.ItemModel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hybris.cockpitng.widgets.baseeditorarea.DefaultEditorAreaController.MODEL_VALUE_CHANGED;

/**
 * Save all action for list view saving all selected modified or new items
 */
public class CxSaveAllAction extends AbstractComponentWidgetAdapterAware implements CockpitAction<Object, List> {

	private static final Logger LOG = LoggerFactory.getLogger(CxSaveAllAction.class);

	@Resource
	private ObjectFacade objectFacade;

	@Resource
	private NotificationService notificationService;

	@Resource
	protected PermissionFacade permissionFacade;

	@Resource
	private BackofficeTypeUtils backofficeTypeUtils;

	@Override
	public boolean canPerform(final ActionContext<Object> ctx) {
		if (ctx.getData() instanceof Collection<?>) {
			final String typeCode = getSelectedItemsType(getData(ctx)).orElse("");
			return CollectionUtils.isNotEmpty((Collection) ctx.getData()) && StringUtils.isNotBlank(typeCode) && permissionFacade.canChangeType(typeCode);
		}
		return false;
	}


	protected Optional<String> getSelectedItemsType(final List<ItemModel> items) {
		return CollectionUtils.isNotEmpty(items) ? Optional.ofNullable(this.backofficeTypeUtils.findClosestSuperType(new ArrayList(items))) : Optional.empty();
	}


	@Override
	public ActionResult<List> perform(final ActionContext<Object> ctx) {

		final ActionResult<List> result = new ActionResult<>(ActionResult.ERROR);

		final List<ItemModel> data = getData(ctx);

		if (CollectionUtils.isEmpty(data)) {
			result.setResultCode(ActionResult.SUCCESS);
		} else {

			result.setData(data);

			final DefaultContext context = new DefaultContext();
			context.addAttribute(CollectionBrowserController.SHOULD_RELOAD_AFTER_UPDATE, Boolean.TRUE);
			context.addAttribute(ObjectFacade.CTX_PARAM_SUPPRESS_EVENT, Boolean.FALSE);

			final ObjectFacadeOperationResult<ItemModel> saveResult = objectFacade.save(data, context);

			if (saveResult != null && !saveResult.hasError()) {
				result.setResultCode(ActionResult.SUCCESS);
				getWidgetModel(ctx).setValue(MODEL_VALUE_CHANGED, Boolean.TRUE);
				getWidgetModel(ctx).changed();
				this.handleObjectSavingSuccess(data, ctx);
			} else {
				if (saveResult != null) {
					LOG.error("Could not save {} items.", saveResult.getFailedObjects());
				}
				this.handleObjectSavingFailure(data, ctx);
			}
		}
		return result;
	}


	/**
	 * Return selected object that have been modified or are newly created
	 *
	 * @param context
	 * @return
	 */
	protected List<ItemModel> getData(final ActionContext<Object> context) {
		if (context.getData() instanceof Collection) {
			final Collection<Object> data = (Collection) context.getData();

			return data.stream()
					   .filter(ItemModel.class::isInstance)
					   .map(ItemModel.class::cast)
					   .filter(this::isModified)
					   .collect(Collectors.toList());

		} else if (context.getData() != null && context.getData() instanceof ItemModel) {
			return Lists.newArrayList((ItemModel) context.getData());
		}
		return Collections.emptyList();
	}


	protected boolean isModified(final ItemModel item) {
		if (item instanceof CxLocalizedValueModel && ((CxLocalizedValueModel) item).getTarget() != null) {
			return objectFacade.isModified(((CxLocalizedValueModel) item).getTarget()) || objectFacade.isNew(((CxLocalizedValueModel) item).getTarget());
		}
		return objectFacade.isModified(item) || objectFacade.isNew(item);
	}

	protected void handleObjectSavingFailure(final Collection<ItemModel> savedObjects, final ActionContext<Object> context) {
		notificationService.notifyUser(context, NotificationEventTypes.EVENT_TYPE_OBJECT_UPDATE, NotificationEvent.Level.FAILURE, savedObjects);
	}

	protected void handleObjectSavingSuccess(final Collection<ItemModel> savedObjects, final ActionContext<Object> context) {
		notificationService.notifyUser(context, NotificationEventTypes.EVENT_TYPE_OBJECT_UPDATE, NotificationEvent.Level.SUCCESS, savedObjects);
	}

	protected WidgetModel getWidgetModel(final ActionContext<Object> ctx) {
		return (WidgetModel) ctx.getParameter(ActionContext.PARENT_WIDGET_MODEL);
	}
}
