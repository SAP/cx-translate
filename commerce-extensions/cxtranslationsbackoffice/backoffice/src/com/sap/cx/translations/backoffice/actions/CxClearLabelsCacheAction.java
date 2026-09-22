/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.engine.impl.AbstractComponentWidgetAdapterAware;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import com.sap.cx.translations.services.CxTranslationsService;
import de.hybris.platform.servicelayer.type.TypeService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.Messagebox;

import jakarta.annotation.Resource;

/**
 * Action clearing {@link CxLocalizedLabelModel} cache
 */
public class CxClearLabelsCacheAction extends AbstractComponentWidgetAdapterAware implements CockpitAction<String, String> {
	private static final String SUCCESS_MESSAGE_KEY = "action.labels.clearcache.success";
	private static final String ERROR_MESSAGE_KEY = "action.labels.clearcache.error";

	@Resource
	private CxTranslationsService translationsService;

	@Resource
	private TypeService typeService;

	@Resource
	private PermissionFacade permissionFacade;

	@Override
	public ActionResult<String> perform(final ActionContext<String> ctx) {

		final String typeCode = ctx.getData();

		if (typeService.isAssignableFrom(CxLocalizedLabelModel._TYPECODE, typeCode)) {
			translationsService.clearLabelsCache();

			final String message = ctx.getLabel(SUCCESS_MESSAGE_KEY);
			Messagebox.show(message, message, 1, Messagebox.INFORMATION);
			return new ActionResult<>(ActionResult.SUCCESS);
		}

		final String message = ctx.getLabel(ERROR_MESSAGE_KEY);
		Messagebox.show(message, message, 1, Messagebox.ERROR);
		return new ActionResult<>(ActionResult.ERROR);
	}

	@Override
	public boolean canPerform(final ActionContext<String> ctx) {
		final String typeCode = ctx.getData();
		return StringUtils.isNotEmpty(typeCode) && typeService.isAssignableFrom(CxLocalizedLabelModel._TYPECODE, typeCode) && permissionFacade.canChangeType(typeCode);
	}
}
