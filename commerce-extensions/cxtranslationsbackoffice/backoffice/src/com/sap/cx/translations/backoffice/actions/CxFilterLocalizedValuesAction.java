/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.engine.impl.AbstractComponentWidgetAdapterAware;
import com.sap.cx.translations.enums.CxTranslateBackofficeSearchInclusion;
import com.sap.cx.translations.model.CxLocalizedObjectConfigModel;
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Action for filtering localized values in the list view.
 * The actual UI is rendered by {@link com.sap.cx.translations.backoffice.actions.renderer.CxFilterLocalizedValuesFilterRender}.
 * This action serves as the entry point; filtering logic will be added later.
 */
public class CxFilterLocalizedValuesAction extends AbstractComponentWidgetAdapterAware
		implements CockpitAction<Collection<Object>, Object> {

	private static final Logger LOG = LoggerFactory.getLogger(CxFilterLocalizedValuesAction.class);

	@Resource
	private CxLocalizedValueConfigService cxLocalizedValueConfigService;

	/**
	 * Returns the filter options available for selection.
	 * Each entry maps a config code to its display name.
	 * Options are lazily loaded from configs where backoffice search inclusion is EXCLUDED_BY_DEFAULT.
	 *
	 * @return an unmodifiable map of filter option code to display name
	 */
	public Map<String, String> getOptionsToChoose() {
		return Collections.unmodifiableMap(loadOptionsToChoose());
	}

	/**
	 * Loads the filter options from all localized object configs that are excluded from backoffice search by default.
	 *
	 * @return a map of config code to display name
	 */
	protected Map<String, String> loadOptionsToChoose() {
		final List<CxLocalizedObjectConfigModel> allConfigs = cxLocalizedValueConfigService.findAllConfigs();
		return allConfigs.stream()
				.filter(config -> config.getUseInBackofficeSearch() == CxTranslateBackofficeSearchInclusion.EXCLUDED_BY_DEFAULT)
				.collect(Collectors.toMap(CxLocalizedObjectConfigModel::getCode, CxLocalizedObjectConfigModel::getName));
	}

	@Override
	public ActionResult<Object> perform(final ActionContext<Collection<Object>> ctx) {
		LOG.debug("CxFilterLocalizedValuesAction performed with data: {}", ctx.getData());
		// Placeholder — actual filtering is handled by the renderer's onFilterSelectionChanged
		return new ActionResult<>(ActionResult.SUCCESS);
	}

	@Override
	public boolean canPerform(final ActionContext<Collection<Object>> ctx) {
		return !getOptionsToChoose().isEmpty();
	}

	public void setCxLocalizedValueConfigService(final CxLocalizedValueConfigService cxLocalizedValueConfigService) {
		this.cxLocalizedValueConfigService = cxLocalizedValueConfigService;
	}
}

