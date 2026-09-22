/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.enums.CxTranslateLocalizedValueTargetType;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxProductFeatureLocalizedObjectConfigModel;
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedClassificationService;
import de.hybris.platform.catalog.model.ProductFeatureModel;
import de.hybris.platform.catalog.model.classification.ClassAttributeAssignmentModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link CxLocalizedClassificationService}
 */
public class CxLocalizedClassificationServiceImpl implements CxLocalizedClassificationService {

    private ModelService modelService;
    private CxLocalizedValueConfigService cxLocalizedValueConfigService;

    @Override
    public List<CxLocalizedValueModel> getValuesForProductFeatures(final ProductModel productModel) {
        return CollectionUtils.emptyIfNull(productModel.getFeatures()).stream()
                .filter(CxLocalizedClassificationServiceImpl::isFeatureTranslatable)
                //we group features by unique attributes other than language
                .collect(Collectors.groupingBy(this::getProductFeatureQualifier))
                .entrySet().stream()
                .map(featureList -> createLocalizedValueFromFeatureGroup(productModel, featureList.getValue(), featureList.getKey()))
                .filter(Objects::nonNull).toList();
    }

    @Override
    public Stream<ProductFeatureModel> getRelatedFeatures(final ProductFeatureModel feature) {
        if (feature.getClassificationAttributeAssignment() == null) {
            return Stream.empty();
        }
        final String targetQualifier = getProductFeatureQualifier(feature);
        return feature.getProduct().getFeatures().stream()
                .filter(CxLocalizedClassificationServiceImpl::isFeatureTranslatable)
                .filter(f -> getProductFeatureQualifier(f).equals(targetQualifier));
    }

    public static boolean isFeatureTranslatable(final ProductFeatureModel f) {
        final ClassAttributeAssignmentModel assignment = f.getClassificationAttributeAssignment();
        return assignment != null && assignment.getLocalized() && f.getLanguage() != null && f.getValue() instanceof String;
    }

    @Override
    public CxLocalizedValueModel createLocalizedValueFromFeatureGroup(final ProductModel productModel, final List<ProductFeatureModel> productFeatures, final String attributeCode) {
        if (productFeatures.isEmpty()) {
            return null;
        }
        final CxLocalizedValueModel valueModel = modelService.create(CxLocalizedValueModel.class);
		modelService.detach(valueModel); // CxLocalizedValueModel are transient, detaching to avoid accidental persistence
        valueModel.setAttribute(attributeCode);
        valueModel.setTargetType(CxTranslateLocalizedValueTargetType.PRODUCT_FEATURES);
        // setTarget(dynamic) is a no-op for PRODUCT_FEATURES targetType (see CxLocalizedValueTargetAttributeHandler);
        // we must set the backing field directly.
        valueModel.setItemTarget(productFeatures.get(0));
        valueModel.setObjectType(CxProductFeatureKey.OBJECT_TYPE);
        final CxProductFeatureLocalizedObjectConfigModel configForProductFeatures = cxLocalizedValueConfigService.findConfigForProductFeatures();
        if (configForProductFeatures == null) {
            throw new IllegalStateException("No config found for Product Features. Please check your configuration.");
        }
        valueModel.setConfig(configForProductFeatures);
        valueModel.setObjectId(productModel.getCode());
        valueModel.setCatalogId(productModel.getCatalogVersion().getCatalog().getId());
        valueModel.setCatalogVersionId(productModel.getCatalogVersion().getVersion());
        if (configForProductFeatures.getProductCatalogVersions().size() < 2) {
            valueModel.setKey(CxProductFeatureKey.simple(valueModel.getObjectId(), valueModel.getAttribute()).toRawString());
        } else {
            valueModel.setKey(CxProductFeatureKey.catalogAware(valueModel.getCatalogId(), valueModel.getCatalogVersionId(), valueModel.getObjectId(), valueModel.getAttribute()).toRawString());
        }
        return valueModel;
    }


    @Override
    public String getAttributeQualifier(final ClassAttributeAssignmentModel attributeAssignment) {
        return getReversedQualifier(attributeAssignment);
    }

    @Override
    public String getProductFeatureQualifier(final ProductFeatureModel f) {
        //qualifier is reversed so for the readability attribute code is at the beginning
        String qualifier = getReversedQualifier(f.getClassificationAttributeAssignment());

        if (f.getValuePosition() != null && f.getClassificationAttributeAssignment().getMultiValued()) {
            qualifier = qualifier + "-" + f.getValuePosition();
        }
        if (f.getUnit() != null) {
            qualifier = qualifier + "-" + f.getUnit().getCode();
        }
        return qualifier.replace(".", "-");
    }

    private String getReversedQualifier(final ClassAttributeAssignmentModel classificationAttributeAssignment) {
        final String attributeCode = classificationAttributeAssignment.getClassificationAttribute().getCode();
        final String classCode = classificationAttributeAssignment.getClassificationClass().getCode();
        return attributeCode + "/" + classCode;
    }

    @Override
    public String getProductFeatureKey(final ProductFeatureModel productFeature, final String attributeCode) {
        final ProductModel productModel = productFeature.getProduct();
        final CxProductFeatureLocalizedObjectConfigModel configForProductFeatures = cxLocalizedValueConfigService.findConfigForProductFeatures();
        if (configForProductFeatures == null) {
            throw new IllegalStateException("No config found for Product Features. Please check your configuration.");
        }
        if (configForProductFeatures.getProductCatalogVersions().size() < 2) {
            return CxProductFeatureKey.simple(productModel.getCode(), attributeCode).toRawString();
        }
        return CxProductFeatureKey.catalogAware(
                productModel.getCatalogVersion().getCatalog().getId(),
                productModel.getCatalogVersion().getVersion(),
                productModel.getCode(),
                attributeCode).toRawString();
    }

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setCxLocalizedValueConfigService(final CxLocalizedValueConfigService cxLocalizedValueConfigService) {
        this.cxLocalizedValueConfigService = cxLocalizedValueConfigService;
    }
}

