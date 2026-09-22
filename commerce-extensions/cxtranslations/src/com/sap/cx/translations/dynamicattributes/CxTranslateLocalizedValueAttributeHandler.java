/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */
package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.enums.CxTranslateLocalizedValueTargetType;
import com.sap.cx.translations.model.CxAbstractLocalizedValueModel;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedClassificationService;
import de.hybris.platform.catalog.model.ProductFeatureModel;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.model.attribute.DynamicLocalizedAttributeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Attribute value handler for {@link CxAbstractLocalizedValueModel}. It gets/sets requested values from/on target object.
 */
public class CxTranslateLocalizedValueAttributeHandler implements DynamicLocalizedAttributeHandler<String, CxAbstractLocalizedValueModel> {
    private static final Logger LOG = LoggerFactory.getLogger(CxTranslateLocalizedValueAttributeHandler.class);
    private ModelService modelService;

    private CommonI18NService commonI18NService;

    private CxLocalizedClassificationService  cxLocalizedClassificationService;


    @Override
    public String get(final CxAbstractLocalizedValueModel model, final Locale locale) {
        final ItemModel target = model.getTarget();

        if (target == null) {
            return null;
        }
        if (model.getTargetType() == CxTranslateLocalizedValueTargetType.PRODUCT_FEATURES) {
            if (model.getTarget() instanceof final ProductFeatureModel featureModel) {
                return cxLocalizedClassificationService.getRelatedFeatures(featureModel)
                        .filter(f -> commonI18NService.getLocaleForLanguage(f.getLanguage()).equals(locale))
                        .map(o -> (String) o.getValue())
                        .findFirst()
                        .orElse(null);
            }
            return null;
        }

        return modelService.getAttributeValue(target, model.getAttribute(), locale);
    }

    @Override
    public void set(final CxAbstractLocalizedValueModel model, final String value, final Locale locale) {
        final ItemModel target = model.getTarget();

        if (model.getTargetType() == CxTranslateLocalizedValueTargetType.PRODUCT_FEATURES) {
            if (model.getTarget() instanceof final ProductFeatureModel featureModel) {
                setProductFeatureValue(value, locale, featureModel);
            }
            return;
        }
        if (target != null) {
            final Map<Locale, Object> attributeValues = new HashMap<>(modelService.getAttributeValues(target, model.getAttribute()));
            attributeValues.put(locale, value);
            modelService.setAttributeValue(target, model.getAttribute(), attributeValues);
        }
    }

    private void setProductFeatureValue(String value, Locale locale, ProductFeatureModel featureModel) {
        final List<ProductFeatureModel> relatedFeaturesInLanguage = cxLocalizedClassificationService.getRelatedFeatures(featureModel).
                filter(f -> commonI18NService.getLocaleForLanguage(f.getLanguage()).equals(locale))
                .toList();
        if (relatedFeaturesInLanguage.isEmpty()) {
            final ProductFeatureModel newFeature = modelService.clone(featureModel);
            final LanguageModel language = commonI18NService.getLanguage(locale.getLanguage());
            newFeature.setLanguage(language);
            newFeature.setValue(value);
            modelService.save(newFeature);
        } else {
            if (relatedFeaturesInLanguage.size() > 1) {
                LOG.error("Multiple related features found for the same language. {} / {}", featureModel.getProduct().getCode(), featureModel.getQualifier());
            }
            relatedFeaturesInLanguage.getFirst().setValue(value);
        }
    }

    @Override
    public String get(final CxAbstractLocalizedValueModel model) {
        if (model.getTarget() == null) {
            return null;
        }
        if (model.getTargetType() == CxTranslateLocalizedValueTargetType.PRODUCT_FEATURES) {
            if (model.getTarget() instanceof final ProductFeatureModel featureModel) {
                return cxLocalizedClassificationService.getRelatedFeatures(featureModel)
                        .filter(f -> f.getLanguage().equals(commonI18NService.getCurrentLanguage()))
                        .map(o -> (String) o.getValue())
                        .findFirst().orElse(null);
            }
            return null;
        }
        return modelService.getAttributeValue(model.getTarget(), model.getAttribute());
    }

    @Override
    public void set(final CxAbstractLocalizedValueModel model, final String value) {
        set(model, value, commonI18NService.getLocaleForLanguage(commonI18NService.getCurrentLanguage()));
    }

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setCommonI18NService(final CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }

    public void setCxLocalizedClassificationService(CxLocalizedClassificationService cxLocalizedClassificationService) {
        this.cxLocalizedClassificationService = cxLocalizedClassificationService;
    }
}
