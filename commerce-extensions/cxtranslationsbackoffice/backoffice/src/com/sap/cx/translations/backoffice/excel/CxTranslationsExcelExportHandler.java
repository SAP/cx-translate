/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.excel;

import com.hybris.backoffice.attributechooser.Attribute;
import com.hybris.backoffice.excel.data.SelectedAttribute;
import com.hybris.backoffice.excel.export.wizard.ExcelExportHandler;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.servicelayer.type.AttributeModifierCriteria;
import de.hybris.platform.servicelayer.type.AttributeModifiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extending the ExcelExportHandler to skip sorting the selected attributes and keep the order.
 */
public class CxTranslationsExcelExportHandler extends ExcelExportHandler {


	/**
	 * Copied from the ExcelExportHandler and removed the SORT in stream.
	 *
	 * @param typeCode
	 * @param selectedAttributes
	 * @return list of attributes
	 */
	@Override
	protected List<SelectedAttribute> toSelectedAttributes(final String typeCode, final Set<Attribute> selectedAttributes) {
		final AttributeModifierCriteria criteria = new AttributeModifierCriteria(0, AttributeModifiers.ALL, AttributeModifiers.PRIVATE);
		final Map<String, AttributeDescriptorModel> descriptors = getTypeService().getAttributesForModifiers(typeCode, criteria)
																				  .stream()
																				  .collect(Collectors.toMap(AttributeDescriptorModel::getQualifier, attr -> attr, (a, b) -> a));

		final Map<String, String> langTagToLanguageIsoCode = new HashMap<>();

		return selectedAttributes.stream()//
								 .map(attribute -> toSelectedAttributes(attribute, descriptors.get(attribute.getQualifier()), langTagToLanguageIsoCode))//
								 .flatMap(List::stream)//
								 .map(SelectedAttribute.class::cast)//
								 .collect(Collectors.toList());
	}
}
