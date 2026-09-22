/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.testdata

import com.sap.cx.translations.enums.CxTranslateBackofficeSearchInclusion
import com.sap.cx.translations.model.CxAiTranslationServiceApiModel
import com.sap.cx.translations.model.CxAiTranslationServiceSettingsModel
import com.sap.cx.translations.model.CxLocalizedBundleModel
import com.sap.cx.translations.model.CxLocalizedLabelModel
import com.sap.cx.translations.model.CxLocalizedValueModel
import com.sap.cx.translations.model.CxProductFeatureLocalizedObjectConfigModel
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel
import com.sap.cx.translations.model.CxTranslateAiModelModel
import com.sap.cx.translations.model.CxTranslationCronJobModel
import com.sap.cx.translations.model.CxTranslationJobModel
import de.hybris.platform.catalog.model.CatalogModel
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.catalog.model.classification.ClassAttributeAssignmentModel
import de.hybris.platform.catalog.model.classification.ClassificationAttributeModel
import de.hybris.platform.catalog.model.classification.ClassificationClassModel
import de.hybris.platform.catalog.model.classification.ClassificationSystemModel
import de.hybris.platform.catalog.model.classification.ClassificationSystemVersionModel
import de.hybris.platform.cms2.enums.CmsApprovalStatus
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.core.model.product.ProductModel
import de.hybris.platform.catalog.model.ProductFeatureModel
import de.hybris.platform.core.model.type.AttributeDescriptorModel
import de.hybris.platform.core.model.type.ComposedTypeModel
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.type.TypeService

/**
 * Utility class for creating test data in DAO integration tests.
 * <p>
 * Designed to be instantiated per-test with a fresh {@link ModelService} reference.
 * When tests are wrapped in a {@code @Transactional} spec, all data created here
 * is automatically rolled back after each test.
 * </p>
 *
 * <pre>
 * // typical usage in a Spock setup() block:
 * def builder = new CxTranslationTestDataBuilder(modelService)
 * def versions = builder.createCatalogWithVersions("testCatalog")
 * stagedVersion = versions.staged
 * onlineVersion = versions.online
 * </pre>
 */
class CxTranslationTestDataBuilder {

    private final ModelService modelService
    private final TypeService typeService

    CxTranslationTestDataBuilder(ModelService modelService, TypeService typeService = null) {
        this.modelService = modelService
        this.typeService = typeService
    }

    // -----------------------------------------------------------------------
    // Language
    // -----------------------------------------------------------------------

    /**
     * Creates and persists a {@link LanguageModel} with the given ISO code.
     * Safe to call when the language may already exist — returns existing if found.
     *
     * @param isocode ISO 639-1 code, e.g. {@code "en"}, {@code "de"}
     * @return the saved language model
     */
    LanguageModel createLanguage(String isocode) {
        LanguageModel language = modelService.create(LanguageModel)
        language.setIsocode(isocode)
        language.setActive(true)
        modelService.save(language)
        return language
    }

    // -----------------------------------------------------------------------
    // Catalog / CatalogVersion
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link CatalogModel} with a Staged (inactive) and an Online (active)
     * {@link CatalogVersionModel} and returns them as a map keyed by {@code "staged"}
     * and {@code "online"}.
     *
     * @param catalogId unique ID of the test catalog
     * @return map with keys {@code "staged"} and {@code "online"}
     */
    Map<String, CatalogVersionModel> createCatalogWithVersions(String catalogId) {
        CatalogModel catalog = modelService.create(CatalogModel)
        catalog.setId(catalogId)
        modelService.save(catalog)

        CatalogVersionModel staged = modelService.create(CatalogVersionModel)
        staged.setCatalog(catalog)
        staged.setVersion("Staged")
        staged.setActive(Boolean.FALSE)
        modelService.save(staged)

        CatalogVersionModel online = modelService.create(CatalogVersionModel)
        online.setCatalog(catalog)
        online.setVersion("Online")
        online.setActive(Boolean.TRUE)
        modelService.save(online)

        return [staged: staged, online: online]
    }

    // -----------------------------------------------------------------------
    // CxLocalizedBundle
    // -----------------------------------------------------------------------

    /**
     * Creates and persists a {@link CxLocalizedBundleModel}.
     *
     * @param code           bundle code (unique within the catalog version)
     * @param catalogVersion catalog version to assign the bundle to
     * @return the saved bundle model
     */
    CxLocalizedBundleModel createBundle(String code, CatalogVersionModel catalogVersion) {
        CxLocalizedBundleModel bundle = modelService.create(CxLocalizedBundleModel)
        bundle.setCode(code)
        bundle.setCatalogVersion(catalogVersion)
        modelService.save(bundle)
        return bundle
    }

    // -----------------------------------------------------------------------
    // CxLocalizedLabel
    // -----------------------------------------------------------------------

    /**
     * Creates and persists a {@link CxLocalizedLabelModel} with explicit frontend/backend flags.
     *
     * @param key            label key (unique within the catalog version)
     * @param bundle         owning bundle
     * @param catalogVersion catalog version
     * @param forFrontend    whether the label is intended for frontend use
     * @param forBackend     whether the label is intended for backend use
     * @return the saved label model
     */
    CxLocalizedLabelModel createLabel(String key,
                                      CxLocalizedBundleModel bundle,
                                      CatalogVersionModel catalogVersion,
                                      boolean forFrontend,
                                      boolean forBackend) {
        CxLocalizedLabelModel label = modelService.create(CxLocalizedLabelModel)
        label.setKey(key)
        label.setBundle(bundle)
        label.setCatalogVersion(catalogVersion)
        label.setForFrontend(forFrontend)
        label.setForBackend(forBackend)
        label.setStatus(CmsApprovalStatus.CHECK)
        modelService.save(label)
        return label
    }

    /**
     * Convenience overload — creates a frontend-only label.
     */
    CxLocalizedLabelModel createFrontendLabel(String key,
                                               CxLocalizedBundleModel bundle,
                                               CatalogVersionModel catalogVersion) {
        return createLabel(key, bundle, catalogVersion, true, false)
    }

    /**
     * Convenience overload — creates a backend-only label.
     */
    CxLocalizedLabelModel createBackendLabel(String key,
                                              CxLocalizedBundleModel bundle,
                                              CatalogVersionModel catalogVersion) {
        return createLabel(key, bundle, catalogVersion, false, true)
    }

    // -----------------------------------------------------------------------
    // CxProposedTranslation
    // -----------------------------------------------------------------------

    /**
     * Creates and persists a {@link CxProposedTranslationModel} identified by its key.
     *
     * @param key translation key (must be unique across all proposed translations)
     * @return the saved proposed translation model
     */
    CxProposedTranslationModel createProposedTranslation(String key) {
        CxProposedTranslationModel translation = modelService.create(CxProposedTranslationModel)
        translation.setKey(key)
        modelService.save(translation)
        return translation
    }

    // -----------------------------------------------------------------------
    // CxTranslationCronJob / AI settings (for dynamic attribute handler tests)
    // -----------------------------------------------------------------------

    /**
     * Creates and persists a minimal {@link CxAiTranslationServiceApiModel}.
     * {@code configurableParameters} and {@code defaultParameterValues} default to empty.
     *
     * @param code unique code for the API bean
     * @return the saved API model
     */
    CxAiTranslationServiceApiModel createAiServiceApi(String code) {
        CxAiTranslationServiceApiModel api = modelService.create(CxAiTranslationServiceApiModel)
        api.setCode(code)
        api.setEndpointUrlSuffix("/v1/chat")
        api.setConfigurableParameters([])
        api.setDefaultParameterValues([:])
        modelService.save(api)
        return api
    }

    /**
     * Creates and persists a minimal {@link CxTranslateAiModelModel}.
     *
     * @param code unique code matching the provider model identifier
     * @return the saved model
     */
    CxTranslateAiModelModel createAiModel(String code) {
        CxTranslateAiModelModel aiModel = modelService.create(CxTranslateAiModelModel)
        aiModel.setCode(code)
        modelService.save(aiModel)
        return aiModel
    }

    /**
     * Creates and persists a {@link CxAiTranslationServiceSettingsModel} wired to the
     * given API and model.
     *
     * @param code       unique settings code
     * @param api        AI service API configuration
     * @param aiModel    AI model to use
     * @param costPerUnit optional map of metric keys to per-unit cost (may be null)
     * @return the saved settings model
     */
    CxAiTranslationServiceSettingsModel createAiServiceSettings(String code,
                                                                  CxAiTranslationServiceApiModel api,
                                                                  CxTranslateAiModelModel aiModel,
                                                                  Map<String, Double> costPerUnit = null) {
        CxAiTranslationServiceSettingsModel settings = modelService.create(CxAiTranslationServiceSettingsModel)
        settings.setCode(code)
        settings.setName(code)
        settings.setIsDefault(Boolean.FALSE)
        settings.setAiServiceApi(api)
        settings.setAiModel(aiModel)
        if (costPerUnit != null) {
            settings.setUsageMetricsCost(costPerUnit)
        }
        modelService.save(settings)
        return settings
    }

    /**
     * Creates and persists a {@link CxTranslationCronJobModel} with the minimum required
     * fields.  Creates a backing {@link CxTranslationJobModel} automatically so callers
     * don't need to know about the mandatory {@code job} field on {@code CronJob}.
     *
     * @param code           unique cron-job code (also used as the job code)
     * @param sourceLanguage source language
     * @param targetLanguages target languages
     * @param settings       translation service settings (may be null)
     * @return the saved cron-job model
     */
    CxTranslationCronJobModel createTranslationCronJob(String code,
                                                        LanguageModel sourceLanguage,
                                                        Collection<LanguageModel> targetLanguages,
                                                        CxAiTranslationServiceSettingsModel settings = null) {
        CxTranslationJobModel job = modelService.create(CxTranslationJobModel)
        job.setCode("job-" + code)
        job.setSpringId("cxTranslationJob")
        modelService.save(job)

        CxTranslationCronJobModel cronJob = modelService.create(CxTranslationCronJobModel)
        cronJob.setCode(code)
        cronJob.setJob(job)
        cronJob.setSourceLanguage(sourceLanguage)
        cronJob.setTargetLanguages(targetLanguages)
        if (settings != null) {
            cronJob.setTranslationServiceSettings(settings)
        }
        modelService.save(cronJob)
        return cronJob
    }

    /**
     * Creates and persists a {@link CxLocalizedValueModel} with the given key and
     * optional item target.  The key must be unique in {@code CxLocalizedValue}.
     *
     * @param key    value key (unique)
     * @param config localized object config (may be null)
     * @return the saved localized value model
     */
    CxLocalizedValueModel createLocalizedValue(String key,
                                                CxStandardLocalizedObjectConfigModel config = null) {
        CxLocalizedValueModel value = modelService.create(CxLocalizedValueModel)
        value.setKey(key)
        if (config != null) {
            value.setConfig(config)
        }
        modelService.save(value)
        return value
    }

    /**
     * Creates and persists a {@link CxStandardLocalizedObjectConfigModel}.
     * Requires that this builder was constructed with a {@link TypeService}.
     *
     * @param code           unique config code
     * @param typeCode       item type code to use as the config's {@code type} (e.g. {@code "Product"})
     * @param catalogAware   whether the config is catalog-aware
     * @return the saved config model
     */
    CxStandardLocalizedObjectConfigModel createLocalizedObjectConfig(String code,
                                                                      String typeCode = "Product",
                                                                      boolean catalogAware = false) {
        ComposedTypeModel composedType = typeService.getComposedTypeForCode(typeCode)
        CxStandardLocalizedObjectConfigModel config = modelService.create(CxStandardLocalizedObjectConfigModel)
        config.setCode(code)
        config.setType(composedType)
        config.setCatalogAware(catalogAware)
        config.setCatalogVersionField("catalogVersion")
        modelService.save(config)
        return config
    }

    // -----------------------------------------------------------------------
    // Full standard config with localized attributes
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link CxStandardLocalizedObjectConfigModel} with specific localized attributes
     * and key fields.  Used by localizedobjects integration tests.
     *
     * @param code             unique config code
     * @param typeCode         item type code (e.g. "Product")
     * @param keyFields        list of key field qualifiers (e.g. ["code"] or ["country.isocode","isocode"])
     * @param attributeQualifiers qualifiers of localized attributes to expose
     * @param catalogAware     whether the config is catalog-aware
     * @param catalogVersionField field holding the catalogVersion reference (default "catalogVersion")
     * @param catalogVersions  list of catalog versions to restrict to (only when catalogAware=true)
     * @param useInBackofficeSearch inclusion policy
     * @return the saved config model
     */
    CxStandardLocalizedObjectConfigModel createStandardConfig(
            String code,
            String typeCode,
            List<String> keyFields,
            List<String> attributeQualifiers,
            boolean catalogAware = false,
            String catalogVersionField = "catalogVersion",
            List<CatalogVersionModel> catalogVersions = [],
            CxTranslateBackofficeSearchInclusion useInBackofficeSearch = CxTranslateBackofficeSearchInclusion.INCLUDED) {

        ComposedTypeModel composedType = typeService.getComposedTypeForCode(typeCode)

        List<AttributeDescriptorModel> attrs = attributeQualifiers.collect { qualifier ->
            typeService.getAttributeDescriptor(composedType, qualifier)
        }

        CxStandardLocalizedObjectConfigModel config = modelService.create(CxStandardLocalizedObjectConfigModel)
        config.setCode(code)
        config.setType(composedType)
        config.setCatalogAware(catalogAware)
        config.setCatalogVersionField(catalogVersionField)
        config.setKeyFields(keyFields)
        config.setLocalizedAttributes(attrs)
        config.setUseInBackofficeSearch(useInBackofficeSearch)
        if (catalogVersions) {
            config.setCatalogVersions(catalogVersions as Set)
        }
        modelService.save(config)
        return config
    }

    // -----------------------------------------------------------------------
    // Product
    // -----------------------------------------------------------------------

    /**
     * Creates and persists a {@link ProductModel} with a code and catalog version.
     *
     * @param code           product code (unique within catalog version)
     * @param catalogVersion catalog version
     * @return the saved product model
     */
    ProductModel createProduct(String code, CatalogVersionModel catalogVersion) {
        ProductModel product = modelService.create(ProductModel)
        product.setCode(code)
        product.setCatalogVersion(catalogVersion)
        modelService.save(product)
        return product
    }

    /**
     * Sets a localized string attribute value on a product.
     *
     * @param product   the product
     * @param attribute attribute qualifier (e.g. "name", "description")
     * @param locale    locale to set the value for
     * @param value     text value
     */
    void setLocalizedAttribute(ProductModel product, String attribute, Locale locale, String value) {
        modelService.setAttributeValue(product, attribute, [(locale): value])
        modelService.save(product)
    }

    // -----------------------------------------------------------------------
    // Classification / ProductFeature
    // -----------------------------------------------------------------------

    /**
     * Creates a minimal classification system + version for testing ProductFeature values.
     *
     * @param systemId unique system id
     * @return map with keys "system" and "version"
     */
    Map createClassificationSystem(String systemId) {
        ClassificationSystemModel system = modelService.create(ClassificationSystemModel)
        system.setId(systemId)
        modelService.save(system)

        ClassificationSystemVersionModel version = modelService.create(ClassificationSystemVersionModel)
        version.setCatalog(system)
        version.setVersion("1.0")
        modelService.save(version)

        return [system: system, version: version]
    }

    /**
     * Creates a {@link ClassificationClassModel} in the given classification system version.
     */
    ClassificationClassModel createClassificationClass(String code, ClassificationSystemVersionModel systemVersion) {
        ClassificationClassModel clazz = modelService.create(ClassificationClassModel)
        clazz.setCode(code)
        clazz.setCatalogVersion(systemVersion)
        modelService.save(clazz)
        return clazz
    }

    /**
     * Creates a localized {@link ClassAttributeAssignmentModel} for the given class.
     * Automatically creates the backing {@link ClassificationAttributeModel}.
     */
    ClassAttributeAssignmentModel createLocalizedClassAttribute(
            String attributeCode,
            ClassificationClassModel classificationClass,
            ClassificationSystemVersionModel systemVersion) {

        ClassificationAttributeModel attr = modelService.create(ClassificationAttributeModel)
        attr.setCode(attributeCode)
        attr.setSystemVersion(systemVersion)
        modelService.save(attr)

        ClassAttributeAssignmentModel assignment = modelService.create(ClassAttributeAssignmentModel)
        assignment.setClassificationClass(classificationClass)
        assignment.setClassificationAttribute(attr)
        assignment.setLocalized(Boolean.TRUE)
        assignment.setMultiValued(Boolean.FALSE)
        modelService.save(assignment)

        return assignment
    }

    /**
     * Creates a {@link ProductFeatureModel} (one language row) for the given product and attribute.
     *
     * @param product    owning product
     * @param assignment class attribute assignment
     * @param language   language model
     * @param value      string value
     * @return saved product feature
     */
    ProductFeatureModel createProductFeature(
            ProductModel product,
            ClassAttributeAssignmentModel assignment,
            LanguageModel language,
            String value) {

        ProductFeatureModel feature = modelService.create(ProductFeatureModel)
        feature.setProduct(product)
        feature.setClassificationAttributeAssignment(assignment)
        feature.setLanguage(language)
        feature.setValue(value)
        feature.setQualifier(assignment.getClassificationAttribute().getCode())
        feature.setValuePosition(0)
        modelService.save(feature)
        return feature
    }

    /**
     * Creates a {@link CxProductFeatureLocalizedObjectConfigModel}.
     *
     * @param code                  unique code
     * @param productCatalogVersions list of product catalog versions to include
     * @param useInBackofficeSearch inclusion setting
     * @return saved config
     */
    CxProductFeatureLocalizedObjectConfigModel createProductFeatureConfig(
            String code,
            List<CatalogVersionModel> productCatalogVersions = [],
            CxTranslateBackofficeSearchInclusion useInBackofficeSearch = CxTranslateBackofficeSearchInclusion.INCLUDED) {

        CxProductFeatureLocalizedObjectConfigModel config = modelService.create(CxProductFeatureLocalizedObjectConfigModel)
        config.setCode(code)
        config.setUseInBackofficeSearch(useInBackofficeSearch)
        if (productCatalogVersions) {
            config.setProductCatalogVersions(productCatalogVersions as Set)
        }
        modelService.save(config)
        return config
    }
}

