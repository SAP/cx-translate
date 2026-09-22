/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.enums.CxTranslateLocalizedValueTargetType;
import com.sap.cx.translations.model.CxAbstractLocalizedValueModel;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.enumeration.EnumerationValueModel;
import de.hybris.platform.enumeration.EnumerationService;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;
import de.hybris.platform.servicelayer.type.TypeService;

/**
 * 'target' attribute value handler for {@link CxAbstractLocalizedValueModel}.
 * 'targetType' field decides the logic how 'target' is obtained
 */
public class CxLocalizedValueTargetAttributeHandler implements DynamicAttributeHandler<ItemModel, CxAbstractLocalizedValueModel> {
	private TypeService typeService;
	private EnumerationService enumerationService;

	@Override
	public ItemModel get(final CxAbstractLocalizedValueModel model) {
		if (model.getTargetType() == null) {
			return null;
		}
		return switch (model.getTargetType()) {
			case ITEM -> model.getItemTarget();
			case ENUM -> typeService.getEnumerationValue(model.getEnumTarget());
            case PRODUCT_FEATURES -> model.getItemTarget();
        };
	}

	@Override
	public void set(final CxAbstractLocalizedValueModel model, final ItemModel item) {
		if (model.getTargetType() == CxTranslateLocalizedValueTargetType.PRODUCT_FEATURES) {
			return;
		}
		if (item instanceof final EnumerationValueModel enumValueItem) {
			model.setTargetType(CxTranslateLocalizedValueTargetType.ENUM);
			model.setItemTarget(null);
			model.setEnumTarget(enumerationService.getEnumerationValue(enumValueItem.getItemtype(),enumValueItem.getCode()));
		} else {
			model.setTargetType(CxTranslateLocalizedValueTargetType.ITEM);
			model.setEnumTarget(null);
			model.setItemTarget(item);
		}
	}

	public void setTypeService(final TypeService typeService) {
		this.typeService = typeService;
	}

	public void setEnumerationService(final EnumerationService enumerationService) {
		this.enumerationService = enumerationService;
	}
}
