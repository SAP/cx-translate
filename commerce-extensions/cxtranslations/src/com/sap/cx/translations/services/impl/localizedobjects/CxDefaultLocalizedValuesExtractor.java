/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.model.CxAbstractLocalizedValueModel;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValuesExtractor;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.DescriptorModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.type.TypeService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;



/**
 * Class responsible for getting {@link CxLocalizedValueModel} values from Items based on {@link CxStandardLocalizedObjectConfigModel}
 */
public class CxDefaultLocalizedValuesExtractor implements CxLocalizedValuesExtractor {

	private final ModelService modelService;
	private final CxStandardLocalizedObjectConfigModel config;
	private final Map<String, CxDefaultLocalizedValuesExtractor> extendedBy = new HashMap<>();
	private final TypeService typeService;
	private final List<AttributeDescriptorModel> filteredAttributes;
	private boolean isDescendingSort;

	public CxDefaultLocalizedValuesExtractor(final CxLocalizedValueSearchData searchData, final CxStandardLocalizedObjectConfigModel config, final CxLocalizedValuesSearchSupport searchSupport, final ModelService modelService, final TypeService typeService) {
		this(searchData, config, searchSupport, modelService, typeService, new HashSet<>());
	}

	private CxDefaultLocalizedValuesExtractor(final CxLocalizedValueSearchData searchData, final CxStandardLocalizedObjectConfigModel config, final CxLocalizedValuesSearchSupport searchSupport, final ModelService modelService, final TypeService typeService, final Set<String> visitedConfigCodes) {
		this.modelService = modelService;
		this.typeService = typeService;
		this.config = config;
		this.filteredAttributes = config.getLocalizedAttributes()
										.stream()
										.filter(f -> searchSupport.getAttributePredicates()
																  .stream()
																  .allMatch(predicate -> predicate.test(f.getQualifier())))
										.sorted(Comparator.comparing(DescriptorModel::getQualifier))
										.toList();
		if (searchData.sortDirection() == CxLocalizedValueSearchData.SortDirection.Descending) {
			this.isDescendingSort = true;
		}
		visitedConfigCodes.add(config.getCode());
		final Set<CxStandardLocalizedObjectConfigModel> extendedByConfigs = config.getExtendedBy();
		extendedByConfigs.forEach(childConfig -> {
			if (visitedConfigCodes.contains(childConfig.getCode())) {
				throw new IllegalStateException(
						"Cyclic config extension detected: config '" + childConfig.getCode()
								+ "' is already in the extension chain " + visitedConfigCodes
								+ ". Fix the extendedBy relation in your configuration.");
			}
			extendedBy.put(childConfig.getCode(), new CxDefaultLocalizedValuesExtractor(searchData, childConfig, searchSupport, modelService, typeService, new HashSet<>(visitedConfigCodes)));
		});
	}

	/**
	 * Gets {@link CxLocalizedValueModel} based on given model.
	 *
	 * @return
	 */
	@Override
	public List<CxLocalizedValueModel> extractValues(final CxLocalizedObjectProvider.ItemWithIds itemWithId) {
		final ItemModel model = itemWithId.item();
		final List<CxLocalizedValueModel> result = filteredAttributes.stream().map(attr->getValueFromModel(itemWithId, model, attr)).collect(Collectors.toCollection(ArrayList::new));

		extendedBy.forEach((childItemType, child) -> {
			if (typeService.isAssignableFrom(childItemType, model.getItemtype())) {
				result.addAll(child.extractValues(itemWithId));
			}
		});
		if (!isDescendingSort) {
			result.sort(Comparator.comparing(CxAbstractLocalizedValueModel::getAttribute));
		} else {
			result.sort(Comparator.comparing(CxAbstractLocalizedValueModel::getAttribute).reversed());
		}

		return result;

	}

	private CxLocalizedValueModel getValueFromModel(
			CxLocalizedObjectProvider.ItemWithIds itemWithId,
			ItemModel model,
			AttributeDescriptorModel attr) {

		final CxLocalizedValueModel wrapper = modelService.create(CxLocalizedValueModel.class);
		modelService.detach(wrapper); // CxLocalizedValueModel are transient, detaching to avoid accidental persistence
		wrapper.setCreationtime(model.getCreationtime());
		wrapper.setModifiedtime(model.getModifiedtime());
		wrapper.setTarget(model);
		wrapper.setConfig(config);
		wrapper.setKey(itemWithId.keyWithoutAttribute() + "." + attr.getQualifier());
		wrapper.setObjectType(itemWithId.item().getItemtype());
		wrapper.setObjectId(itemWithId.objectId());

		if (config.isCatalogAware()) {
			final String catalogVersionField = config.getCatalogVersionField();
			if (StringUtils.isNotBlank(catalogVersionField)) {
				final CatalogVersionModel cv = modelService.getAttributeValue(model, catalogVersionField);
				wrapper.setCatalogVersionId(cv.getVersion());
				wrapper.setCatalogId(cv.getCatalog().getId());
			}
		}

		final String qualifier = attr.getQualifier();
		wrapper.setAttribute(qualifier);
		return wrapper;
	}

	/**
	 * @return how many values are per single item
	 */
	@Override
	public int valuesPerItemCount(final String s) {
		if (typeService.isAssignableFrom(this.config.getType().getCode(), s)) {
			return filteredAttributes.size() + this.extendedBy.values()
															  .stream()
															  .mapToInt(child -> child.valuesPerItemCount(s))
															  .sum();
		} else {
			return 0;
		}
	}

}
