/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.backoffice.excel;

import com.hybris.backoffice.excel.template.filter.ExcelFilter;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Filter responsible for filtering out the list of properties shown for Excel export action
 * In case the type is present in the spring configuration only show the properties from the spring configuration.
 */

public class CxTranslationsExcelFilter implements ExcelFilter<AttributeDescriptorModel> {

	private Map<String, Set<String>> attributesForTypeMap = Collections.emptyMap();

	@Override
	public boolean test(final AttributeDescriptorModel attributeDescriptor) {
		final String typeCode = attributeDescriptor.getEnclosingType().getCode();
		if (attributesForTypeMap.containsKey(typeCode)) {
			return attributesForTypeMap.get(typeCode).contains(attributeDescriptor.getQualifier());
		}
		return true;
	}

	public void setAttributesForTypeMap(final Map<String, Set<String>> attributesForTypeMap) {
		this.attributesForTypeMap = attributesForTypeMap != null ? attributesForTypeMap : Collections.emptyMap();
	}
}
