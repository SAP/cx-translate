/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.search.data.pageable.Pageable;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel;
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.servicelayer.type.TypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Resource;
import java.util.Set;

/**
 * Gets the selected items or if nothing is selected the full pageable list and sends it on itemsToTranslate output for further processing
 */
public class CxAiTranslateListViewAction extends CxAbstractSelectedOrPageableListViewAction {

	protected static final String SOCKET_OUT_ITEMS_TO_TRANSLATE = "itemsToTranslate";
	protected static final String SOCKET_OUT_VALUES_TO_TRANSLATE = "valuesToTranslate";

	@Resource
	private CxLocalizedValueConfigService cxLocalizedValueConfigService;

	@Resource
	private TypeService typeService;

	@Override
	public ActionResult<Pageable<? extends ItemModel>> perform(final ActionContext<String> ctx) {
		final ActionResult<Pageable<? extends ItemModel>> result = new ActionResult("error");

		this.getData(ctx).ifPresent((dataToTranslate) -> {
			final Pageable<ItemModel> dataPageable = dataToTranslate;


			if (this.isMaxRowsExceeded(ctx, dataPageable.getTotalCount())) {
				this.showMaxRowsExceeded(ctx, dataPageable.getTotalCount());
			} else {
				if (dataPageable.getTotalCount() > 0) {
					// depending on the item type send result to different socket
					if (CxLocalizedValueModel._TYPECODE.equals(dataPageable.getTypeCode())) {
						this.sendOutput(SOCKET_OUT_VALUES_TO_TRANSLATE, dataPageable);
					} else {
						this.sendOutput(SOCKET_OUT_ITEMS_TO_TRANSLATE, dataPageable);
					}
				}
				result.setResultCode("success");
				result.setData(dataPageable);
			}
		});
		return result;
	}

	@Override
	public boolean canPerform(final ActionContext<String> ctx) {

		final String typeCode = StringUtils.isNotBlank(ctx.getData()) ? ctx.getData() : this.getSelectedItemsType(ctx)
																							.orElse("");

		if (StringUtils.isBlank(typeCode) || !getPermissionFacade().canChangeType(typeCode)) {
			return false;
		}

		if (getTypeService().isAssignableFrom(CxLocalizedValueModel._TYPECODE, typeCode)) {
			return true;
		}

		final Pageable<ItemModel> pageable = this.getData(ctx).orElse(null);
		if (pageable == null || pageable.getTotalCount() < 1) {
			return false;
		}

		final Set<AttributeDescriptorModel> attributes = cxLocalizedValueConfigService.findTranslatableAttributesForTypeCode(pageable.getTypeCode());
		return !CollectionUtils.isEmpty(attributes);
	}


	protected final CxStandardLocalizedObjectConfigModel getConfig(final ComposedTypeModel composedType) {
		final CxStandardLocalizedObjectConfigModel config = cxLocalizedValueConfigService.findConfigByType(composedType);
		if (config != null) {
			return config;
		}
		final ComposedTypeModel superComposedType = composedType.getSuperType();
		if (superComposedType != null) {
			return getConfig(superComposedType);
		}
		return null;
	}


	@Override
	public String getThresholdValueParameterKey() {
		return "safety.threshold";
	}

	@Override
	public String getMaxRowsParameterKey() {
		return "max.rows";
	}

	@Override
	public String getConfirmationMessageKey() {
		return "cxtranslations.aitranslation.confirmation";
	}

	@Override
	public String getMaxRowsExceededMessageKey() {
		return "cxtranslations.aitranslation.max.rows.exceeded.msg";
	}

	@Override
	public String getMaxRowsExceededMessageTitleKey() {
		return "cxtranslations.aitranslation.max.rows.exceeded.title";
	}
}
