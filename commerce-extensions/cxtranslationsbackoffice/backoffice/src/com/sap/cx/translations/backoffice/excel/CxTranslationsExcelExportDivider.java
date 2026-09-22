/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.excel;

import com.hybris.backoffice.excel.data.SelectedAttribute;
import com.hybris.backoffice.excel.exporting.DefaultExcelExportDivider;
import com.hybris.backoffice.excel.exporting.ExcelExportDivider;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A custom {@link ExcelExportDivider} keeping the order of exported items, use LinkedHashSet instead od Set
 */
public class CxTranslationsExcelExportDivider extends DefaultExcelExportDivider implements ExcelExportDivider {

	/**
	 * Overwritten method - changed from HAS SET to LINKED HAS SET to keep the order
	 */
	@Override
	public Map<String, Set<ItemModel>> groupItemsByType(List<ItemModel> items) {
		final Stream<ItemModel> stream = items.stream();
		Objects.requireNonNull(getModelService());

		return stream.collect(Collectors.groupingBy(getModelService()::getModelType, Collectors.toCollection(LinkedHashSet::new) // Use LinkedHashSet to maintain order
		));
	}

	/**
	 * Overwritten method - changed from HAS SET to LINKED HAS SET to keep the order
	 */
	@Override
	protected Set<SelectedAttribute> getAttributes(List<SelectedAttribute> selectedAttributes, String typeCode) {
		final ComposedTypeModel composedType = getTypeService().getComposedTypeForCode(typeCode);
		final Set<String> selectedQualifiers = selectedAttributes.stream().map((selectedAttribute) -> {
			return selectedAttribute.getAttributeDescriptor().getQualifier();
		}).collect(Collectors.toCollection(LinkedHashSet::new));
		final Set<SelectedAttribute> attributes = new LinkedHashSet();
		final Set<SelectedAttribute> required = this.getMissingRequiredAndUniqueAttributes(composedType, selectedQualifiers);
		attributes.addAll(this.filterByPermissions(required));
		attributes.addAll(this.createSelectedAttributesForGivenType(typeCode, selectedAttributes));
		return attributes;
	}

	/**
	 * Private method copied without changes from DefaultExcelExportDivider
	 */
	private List<SelectedAttribute> createSelectedAttributesForGivenType(String typeCode, List<SelectedAttribute> attributes) {
		return attributes.stream().map((attr) -> {
			return this.createSelectedAttributeForGivenType(typeCode, attr);
		}).collect(Collectors.toList());
	}
	/**
	 * Private method copied without changes from DefaultExcelExportDivider
	 */
	private SelectedAttribute createSelectedAttributeForGivenType(String typeCode, SelectedAttribute attr) {
		AttributeDescriptorModel attributeDescriptorForType = getTypeService().getAttributeDescriptor(typeCode, attr.getQualifier());
		return new SelectedAttribute(attr.getIsoCode(), attr.getReferenceFormat(), attr.getDefaultValues(), attributeDescriptorForType);
	}
}
