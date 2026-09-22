/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.core.model.WidgetModel;
import com.hybris.cockpitng.engine.impl.AbstractComponentWidgetAdapterAware;
import de.hybris.platform.servicelayer.type.TypeService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Resource;

/**
 * Action opening the parent widget for main and secondary widgets selection
 */
public class CxTranslationLanguagesAction extends AbstractComponentWidgetAdapterAware implements CockpitAction<String, String> {
	public static final String OPEN_WIDGET = "openWidget";

	@Resource
	private TypeService typeService;

	@Override
	public ActionResult<String> perform(final ActionContext<String> actionContext) {
		sendOutput(OPEN_WIDGET, actionContext.getData());
		return new ActionResult<>(ActionResult.SUCCESS);
	}

	@Override
	public boolean canPerform(final ActionContext<String> ctx) {
		return StringUtils.isNotEmpty(ctx.getData());
	}
}
