/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects;

import com.sap.cx.translations.model.CxLocalizedValueModel;
import de.hybris.platform.catalog.model.ProductFeatureModel;
import de.hybris.platform.catalog.model.classification.ClassAttributeAssignmentModel;
import de.hybris.platform.core.model.product.ProductModel;

import java.util.List;
import java.util.stream.Stream;

/**
 * Service interface for managing {@link CxLocalizedValueModel} objects.
 */
public interface CxLocalizedClassificationService {
	/**
     * Returns list of Localized Value related to Product Features of specific Product
	 * @param product a ProductModel instance
	 * @return list of Localized Value related to Product Features
	 */
	List<CxLocalizedValueModel> getValuesForProductFeatures(ProductModel product);

	/**
     * Gets ProductFeatures values with same unique attributes other than language
	 * @param feature a reference ProductFeatureModel instance
	 * @return a Stream of Product Features
	 */
	Stream<ProductFeatureModel> getRelatedFeatures(ProductFeatureModel feature);

    /**
     * Creates a {@link CxLocalizedValueModel} from a group of product features sharing the same attribute.
     *
     * @param productModel   the product owning the features
     * @param productFeatures the grouped product features
     * @param attributeCode  the classification attribute code
     * @return the created localized value, or {@code null} if the feature list is empty
     */
    CxLocalizedValueModel createLocalizedValueFromFeatureGroup(ProductModel productModel, List<ProductFeatureModel> productFeatures, String attributeCode);

    /**
     * Gets the attribute qualifier for a classification attribute assignment.
     *
     * @param attributeAssignment the classification attribute assignment
     * @return the attribute qualifier string
     */
    String getAttributeQualifier(ClassAttributeAssignmentModel attributeAssignment);

    /**
     * Gets the qualifier string for a product feature, combining attribute code and classification class.
     *
     * @param f the product feature
     * @return the product feature qualifier string
     */
    String getProductFeatureQualifier(ProductFeatureModel f);

    /**
     * Returns the localized value key for a product feature without creating a full model instance.
     *
     * @param productFeature the product feature
     * @param attributeCode  the classification attribute qualifier
     * @return the key string
     */
    String getProductFeatureKey(ProductFeatureModel productFeature, String attributeCode);
}
