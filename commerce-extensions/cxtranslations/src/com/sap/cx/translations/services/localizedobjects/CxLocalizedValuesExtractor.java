/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider.ItemWithIds;

import java.util.List;

/**
 * 
 * Interface responsible for getting {@link CxLocalizedValueModel} values from Items
 */
public interface CxLocalizedValuesExtractor {
	/**
	 * Gets {@link CxLocalizedValueModel} based on given model.
	 *
	 * @param model
	 * @return
	 */
	List<CxLocalizedValueModel> extractValues(ItemWithIds model);

	/**
	 * @return how many values are per single item
	 */
	int valuesPerItemCount(String s);
}
