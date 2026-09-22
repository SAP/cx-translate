/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.renderers;

import com.hybris.backoffice.attributechooser.AttributeChooserRenderer;
import org.apache.commons.lang3.StringUtils;

/**
 * Keep the order of the attributes selected. Only sort the attributes to be selected. Not the selected ones.
 */
public class CxTranslationsAttributeChooserRenderer extends AttributeChooserRenderer {

	/**
	 * Only sort the available attributes, not the selected ones
	 *
	 * @param buttonId
	 * @return
	 */
	@Override
	protected boolean isNeedSort(final String buttonId) {
		return StringUtils.equals(BUTTON_ID_REMOVE, buttonId) || StringUtils.equals(SECTION_ID_AVAILABLE, buttonId);
	}
}
