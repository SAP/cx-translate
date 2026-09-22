/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.backoffice.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.core.model.WidgetModel;
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade;
import com.hybris.cockpitng.engine.impl.AbstractComponentWidgetAdapterAware;
import com.hybris.cockpitng.search.data.pageable.Pageable;
import com.hybris.cockpitng.search.data.pageable.PageableList;
import com.hybris.cockpitng.util.type.BackofficeTypeUtils;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.type.TypeService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.zkoss.zul.Messagebox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Abstract action for list view handling either the selected items or all items in the current search if nothing is selected
 */
public abstract class CxAbstractSelectedOrPageableListViewAction extends AbstractComponentWidgetAdapterAware implements CockpitAction<String, Pageable<? extends ItemModel>> {

	protected static final String MODEL_SELECTED_OBJECTS = "selectedObjects";
	protected static final String MODEL_PAGEABLE = "pageable";
	protected static final String PARENT_WIDGET_MODEL = "parentWidgetModel";
	protected static final String POPUP_CSS_STYLE = "z-messagebox-icon z-messagebox-exclamation";

	@Resource
	private TypeService typeService;
	@Resource
	private PermissionFacade permissionFacade;
	@Resource
	private BackofficeTypeUtils backofficeTypeUtils;

	public Optional<Pageable<ItemModel>> getData(final ActionContext<String> ctx) {
		return Optional.ofNullable(this.getSelectedItems(ctx)
									   .orElseGet(() -> {
										   return this.getPageable(ctx);
									   }));
	}

	protected Optional<Pageable<ItemModel>> getSelectedItems(final ActionContext<String> ctx) {
		final Collection<Object> selectedObjects = this.getSelectedItemsFromModel(ctx);
		if (CollectionUtils.isNotEmpty(selectedObjects)) {
			final String typeCode = backofficeTypeUtils.findClosestSuperType(new ArrayList(selectedObjects));
			return Optional.of(new PageableList(new ArrayList(selectedObjects), selectedObjects.size(), typeCode));
		} else {
			return Optional.empty();
		}
	}

	protected Collection<Object> getSelectedItemsFromModel(final ActionContext<String> ctx) {
		final WidgetModel widgetModel = this.getWidgetModel(ctx);
		return widgetModel != null ? widgetModel.getValue(this.getSelectedItemsModelProperty(ctx), Collection.class) : Collections.emptyList();
	}

	protected Optional<String> getSelectedItemsType(final ActionContext<String> ctx) {
		final Collection<Object> selection = this.getSelectedItemsFromModel(ctx);
		return CollectionUtils.isNotEmpty(selection) ? Optional.ofNullable(this.backofficeTypeUtils.findClosestSuperType(new ArrayList(selection))) : Optional.empty();
	}

	protected Pageable<ItemModel> getPageable(final ActionContext<String> ctx) {
		final WidgetModel widgetModel = this.getWidgetModel(ctx);
		return widgetModel != null ? (Pageable) widgetModel.getValue(this.getPageableModelProperty(ctx), Pageable.class) : null;
	}

	public int getSize(final ActionContext<String> ctx) {
		return this.getData(ctx)
				   .map(Pageable::getTotalCount)
				   .orElse(0);
	}

	protected WidgetModel getWidgetModel(final ActionContext<String> ctx) {
		return (WidgetModel) ctx.getParameter(PARENT_WIDGET_MODEL);
	}

	protected String getPageableModelProperty(final ActionContext<String> ctx) {
		final String pageableModelProperty = (String) ctx.getParameter(MODEL_PAGEABLE);
		return StringUtils.isNotEmpty(pageableModelProperty) ? pageableModelProperty : MODEL_PAGEABLE;
	}

	protected String getSelectedItemsModelProperty(final ActionContext<String> ctx) {
		final String selectedItemsModelProperty = (String) ctx.getParameter(MODEL_SELECTED_OBJECTS);
		return StringUtils.isNotEmpty(selectedItemsModelProperty) ? selectedItemsModelProperty : MODEL_SELECTED_OBJECTS;
	}

	protected void showMaxRowsExceeded(final ActionContext<String> ctx, final int rows) {
		Messagebox.show(ctx.getLabel(getMaxRowsExceededMessageKey(), new Integer[]{rows, this.getExportMaxRows(ctx)}), ctx.getLabel(getMaxRowsExceededMessageTitleKey()), 1, POPUP_CSS_STYLE);
	}

	@Override
	public String getConfirmationMessage(final ActionContext<String> ctx) {
		return ctx.getLabel(getConfirmationMessageKey(), new Integer[]{this.getSize(ctx), this.getConfirmationThresholdValue(ctx)});
	}

	protected int getConfirmationThresholdValue(final ActionContext<String> ctx) {
		final String confirmationThreshold = String.valueOf(ctx.getParameter(getThresholdValueParameterKey()));
		try {
			return Integer.parseInt(confirmationThreshold);
		} catch (final NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	protected int getExportMaxRows(final ActionContext<String> ctx) {
		final String maxRows = String.valueOf(ctx.getParameter(getMaxRowsParameterKey()));
		try {
			return Integer.parseInt(maxRows);
		} catch (final NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	protected boolean isMaxRowsExceeded(final ActionContext<String> ctx, final int rows) {
		return rows > this.getExportMaxRows(ctx);
	}


	public abstract String getThresholdValueParameterKey();

	public abstract String getMaxRowsParameterKey();

	public abstract String getConfirmationMessageKey();

	public abstract String getMaxRowsExceededMessageKey();

	public abstract String getMaxRowsExceededMessageTitleKey();


	public TypeService getTypeService() {
		return typeService;
	}

	public PermissionFacade getPermissionFacade() {
		return permissionFacade;
	}
}
