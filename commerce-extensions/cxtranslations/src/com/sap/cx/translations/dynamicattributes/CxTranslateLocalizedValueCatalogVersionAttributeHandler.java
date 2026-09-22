/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.model.CxAbstractLocalizedValueModel;
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;

/**
 * Catalog Version attribute value handler for {@link CxAbstractLocalizedValueModel}. It gets requested values from target object if its a catalog aware item.
 */
public class CxTranslateLocalizedValueCatalogVersionAttributeHandler implements DynamicAttributeHandler<CatalogVersionModel, CxAbstractLocalizedValueModel> {
	private ModelService modelService;

	@Override
	public CatalogVersionModel get(final CxAbstractLocalizedValueModel model) {
		final ItemModel target = model.getTarget();
		if (target == null) {
			return null;
		}

		if (model.getConfig() instanceof CxStandardLocalizedObjectConfigModel cxStandardLocalizedObjectConfig && cxStandardLocalizedObjectConfig.isCatalogAware()) {
			return modelService.getAttributeValue(target, cxStandardLocalizedObjectConfig.getCatalogVersionField());
		}

		return null;
	}

	@Override
	public void set(final CxAbstractLocalizedValueModel model, final CatalogVersionModel catalogVersion) {
		throw new UnsupportedOperationException("Read only attribute");
	}


	public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}


}
