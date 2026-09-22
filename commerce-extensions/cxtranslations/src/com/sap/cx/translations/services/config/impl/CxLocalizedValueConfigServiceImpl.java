/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.config.impl;

import com.sap.cx.translations.enums.CxMergeMode;
import com.sap.cx.translations.model.*;
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.catalog.model.classification.ClassificationSystemVersionModel;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.type.*;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.cronjob.model.JobModel;
import de.hybris.platform.servicelayer.cluster.ClusterService;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.search.exceptions.FlexibleSearchException;
import de.hybris.platform.servicelayer.type.TypeService;
import de.hybris.platform.servicelayer.user.UserService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of CxLocalizedValueConfigService.
 */
public class CxLocalizedValueConfigServiceImpl implements CxLocalizedValueConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(CxLocalizedValueConfigServiceImpl.class);
    private static final String INITIAL_CONFIGURATION_CRON_JOB_CODE = "Initial Auto Configuration Cron Job";

    private FlexibleSearchService flexibleSearchService;
    private ModelService modelService;
    private CronJobService cronJobService;
    private String cronJobNodeGroup;
    private int cronJobPriority;
    private ClusterService clusterService;
    private UserService userService;
    private TypeService typeService;

    private List<Predicate<String>> excludedCatalogPatterns;
    private List<String> defaultExcludedTypeCodes;
    private Map<String, List<String>> defaultExcludedPropertiesMap;
    private CxMergeMode defaultMergeMode;
    private Map<String, List<String>> customUniqueKeyFields = Collections.emptyMap();

    /**
     * {@inheritDoc}
     */
    @Override
    public void autoConfigure(final boolean addAttributesOnlyWhenDataExists) {

        CxLocalizedObjectConfigCronJobModel cxLocalizedObjectConfigCronJob;

        try {
            final CronJobModel cronJob = cronJobService.getCronJob(INITIAL_CONFIGURATION_CRON_JOB_CODE);
            cxLocalizedObjectConfigCronJob = (CxLocalizedObjectConfigCronJobModel) cronJob;
        } catch (final UnknownIdentifierException ex) {
            cxLocalizedObjectConfigCronJob = modelService.create(CxLocalizedObjectConfigCronJobModel.class);
            cxLocalizedObjectConfigCronJob.setCode(INITIAL_CONFIGURATION_CRON_JOB_CODE);
            configureLocalizedObjectConfigCronJob(cxLocalizedObjectConfigCronJob);
            modelService.save(cxLocalizedObjectConfigCronJob);
        }

        // execute cron job in asynchronous way
        cronJobService.performCronJob(cxLocalizedObjectConfigCronJob, false);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void configureLocalizedObjectConfigCronJob(final CxLocalizedObjectConfigCronJobModel cronJob) {
        configureCronJobBasicSettings(cronJob);
        configureDefaultExcludedTypes(cronJob);
        configureDefaultExcludedAttributes(cronJob);
        configureDefaultMergeMode(cronJob);
        configureDefaultCatalogVersions(cronJob);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ComposedTypeModel> findTypesToConsider() {
        final FlexibleSearchQuery query = new FlexibleSearchQuery("SELECT {pk} FROM {" + ComposedTypeModel._TYPECODE + "} WHERE {" + ComposedTypeModel.CODE + "} = ?code");
        query.addQueryParameter("code", "GenericItem");
        query.setCount(1);

        final List<ComposedTypeModel> composedTypes = flexibleSearchService.<ComposedTypeModel>search(query)
                .getResult();

        if (CollectionUtils.isEmpty(composedTypes)) {
            throw new IllegalArgumentException("Cannot find GenericItem");
        }

        final ComposedTypeModel genericItemType = composedTypes.get(0);

        return genericItemType.getSubtypes()
                .stream()
                .flatMap(childType -> findTypesThatHaveNewUniques(childType, genericItemType))
                .collect(Collectors.toList());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Set<CxStandardLocalizedObjectConfigModel> buildConfig(final ComposedTypeModel type, final CxLocalizedObjectConfigCronJobModel cronJob) {
        return buildConfig(type, true, cronJob.isAddAttributesOnlyWhenDataExists(), cronJob.getExcludedComposedTypes(), cronJob.getExcludedAttributes(), cronJob.getMergeMode(), cronJob.getCatalogVersions());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CxStandardLocalizedObjectConfigModel findConfigByType(final ComposedTypeModel type) {
        final FlexibleSearchQuery query = new FlexibleSearchQuery("SELECT {c." + ItemModel.PK + "} FROM {" + CxStandardLocalizedObjectConfigModel._TYPECODE + " AS c} WHERE {c." + CxStandardLocalizedObjectConfigModel.TYPE + "} = ?type ");
        query.addQueryParameter("type", type);
        final SearchResult<CxStandardLocalizedObjectConfigModel> result = flexibleSearchService.search(query);
        return (!result.getResult().isEmpty()) ? result.getResult().get(0) : null;
    }

    @Override
    public CxProductFeatureLocalizedObjectConfigModel findConfigForProductFeatures() {
        final List<Object> result = flexibleSearchService.search("SELECT {" + ItemModel.PK + "} FROM {" + CxProductFeatureLocalizedObjectConfigModel._TYPECODE + "}")
                .getResult();
        return result.isEmpty() ? null : (CxProductFeatureLocalizedObjectConfigModel) result.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AttributeDescriptorModel> findTranslatableAttributesForTypeCode(final String typeCode) {
        final ComposedTypeModel composedType = typeService.getComposedTypeForCode(typeCode);

        // all attributes for all SUB types
        final Set<AttributeDescriptorModel> allAttributes = composedType.getAllSubTypes()
                .stream()
                .map(this::getConfig)
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(subConfig -> getAllAttributesIncludingInherited(subConfig).stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // attributes for the current type and inherited from its super types
        final CxStandardLocalizedObjectConfigModel config = getConfig(composedType);
        if (config != null) {
            final List<AttributeDescriptorModel> currentTypeAttributes = getAllAttributesIncludingInherited(config);
            if (CollectionUtils.isNotEmpty(currentTypeAttributes)) {
                allAttributes.addAll(currentTypeAttributes);
            }
        }

        return allAttributes;
    }

    @Override
    public List<CxLocalizedObjectConfigModel> findAllConfigs() {
        return flexibleSearchService.<CxLocalizedObjectConfigModel>search("SELECT {pk} FROM {" + CxLocalizedObjectConfigModel._TYPECODE + "} ORDER BY {"+CxLocalizedObjectConfigModel.CODE+"}").getResult();
    }

    protected CxStandardLocalizedObjectConfigModel getConfig(final ComposedTypeModel composedType) {
        final CxStandardLocalizedObjectConfigModel config = findConfigByType(composedType);
        if (config != null) {
            return config;
        }
        final ComposedTypeModel superComposedType = composedType.getSuperType();
        if (superComposedType != null) {
            return getConfig(superComposedType);
        }
        return null;
    }

    protected List<AttributeDescriptorModel> getAllAttributesIncludingInherited(final CxStandardLocalizedObjectConfigModel config) {
        final List<AttributeDescriptorModel> attributes = new ArrayList<>(config.getLocalizedAttributes());
        final CxStandardLocalizedObjectConfigModel superConfig = config.getExtends();
        if (superConfig != null) {
            attributes.addAll(getAllAttributesIncludingInherited(superConfig));
        }
        return attributes;
    }

    protected Set<CxStandardLocalizedObjectConfigModel> buildConfig(final ComposedTypeModel type, final boolean isRoot, final boolean checkForData, final List<ComposedTypeModel> excludedComposedTypes, final List<AttributeDescriptorModel> excludedProperties, final CxMergeMode mergeMode, final Set<CatalogVersionModel> catalogVersions) {

        if (shouldSkipType(type, isRoot, excludedComposedTypes)) {
            //if it's not root we will ignore everything from its hierarchy
            if (!isRoot) {
                return Collections.emptySet();
            }
            //otherwise we will treat descendants as new roots
            return type.getSubtypes()
                    .stream()
                    .flatMap(child -> buildConfig(child, true, checkForData, excludedComposedTypes, excludedProperties, mergeMode, catalogVersions).stream())
                    .collect(Collectors.toSet());
        }

        final List<AttributeDescriptorModel> localizedAttributes = new ArrayList<>();
        // if config is being created for a root type, which means no parent configuration is created
        // all inherited attributes should be added at the root item level because they won't be inherited from parent config.
        if (isRoot) {
            type.getInheritedattributedescriptors()
                    .stream()
                    .filter(attribute -> isAttributeEligible(attribute, checkForData, excludedProperties))
                    .forEach(localizedAttributes::add);
        }

        type.getDeclaredattributedescriptors()
                .stream()
                .filter(attribute -> isAttributeEligible(attribute, checkForData, excludedProperties))
                .forEach(localizedAttributes::add);

        final Set<CxStandardLocalizedObjectConfigModel> localisedObjectConfigsForSubTypes = type.getSubtypes()
                .stream()
                .flatMap(child -> buildConfig(child, false, checkForData, excludedComposedTypes, excludedProperties, mergeMode, catalogVersions).stream())
                .collect(Collectors.toSet());
        final boolean hasLocalizedAttributes = !localizedAttributes.isEmpty();
        final boolean isRootWithLocalizedChildren = isRoot && !localisedObjectConfigsForSubTypes.isEmpty();

        // we create config if it has localized content or is root with descendants that have it
        if (hasLocalizedAttributes || isRootWithLocalizedChildren) {
            final Optional<CxStandardLocalizedObjectConfigModel> config = getOrCreateLocalizedObjectConfig(type, localizedAttributes, mergeMode, catalogVersions);
            config.ifPresent(cxStandardLocalizedObjectConfigModel -> cxStandardLocalizedObjectConfigModel.setExtendedBy(localisedObjectConfigsForSubTypes));
            return config.map(Collections::singleton).orElse(Collections.emptySet());
        }

        //we skip the current type as it does not have its own localized content ant it's not used as root object
        //but we still return its descendants that have localized content (possibly empty)
        return localisedObjectConfigsForSubTypes;
    }

    /**
     * Concurrently find the all the item type that have a unique fields that parents does not have
     *
     * @param current
     * @param parentType
     * @return stream of {@link ComposedTypeModel}s
     */
    private Stream<ComposedTypeModel> findTypesThatHaveNewUniques(final ComposedTypeModel current, final ComposedTypeModel parentType) {
        final boolean hasASeparateDeployment = !parentType.getTable().equals(current.getTable());
        final boolean hasNoChildrenWithAdditionalUniques = current.getAllSubTypes()
                .stream()
                .noneMatch(child -> child.getDeclaredattributedescriptors()
                        .stream()
                        .anyMatch(AttributeDescriptorModel::getUnique));
        LOG.info("Type {}, has it's own deployment = {}, no children have unique = {}", current.getCode(), hasASeparateDeployment, hasNoChildrenWithAdditionalUniques);
        if (hasNoChildrenWithAdditionalUniques) {
            return Stream.of(current);
        }
        return current.getSubtypes().stream().flatMap(child -> findTypesThatHaveNewUniques(child, current));
    }

    /**
     * Create {@link CxStandardLocalizedObjectConfigModel} for given type and attributes
     *
     * @param composedType
     * @param includedAttributes
     * @return Optional {@link CxStandardLocalizedObjectConfigModel}
     */
    private Optional<CxStandardLocalizedObjectConfigModel> getOrCreateLocalizedObjectConfig(final ComposedTypeModel composedType, final List<AttributeDescriptorModel> includedAttributes, final CxMergeMode mergeMode, final Set<CatalogVersionModel> catalogVersions) {

        CxStandardLocalizedObjectConfigModel config = findConfigByType(composedType);


        // if already exists do not create a new one
        if (config != null) {
            // if the merge mode is set to keep existing do not update the configuration!
            if (CxMergeMode.KEEP_EXISTING == mergeMode) {
                return Optional.of(config);
            }
        } else {
            config = modelService.create(CxStandardLocalizedObjectConfigModel.class);
            config.setCode(composedType.getCode());
        }

        config.setType(composedType);
        config.setName(composedType.getName());

        final String catalogVersionField;
        if (composedType.getCatalogItemType() == Boolean.TRUE) {
            config.setCatalogAware(true);
            catalogVersionField = composedType.getCatalogVersionAttribute().getQualifier();
            config.setCatalogVersionField(catalogVersionField);
            config.setCatalogVersions(detectCatalogVersion(composedType, catalogVersionField, catalogVersions));
        }

        config.setLocalizedAttributes(includedAttributes);

        final List<String> uniqueExpression = createUniqueExpression(composedType, "").toList();
        config.setKeyFields(uniqueExpression);
        return Optional.of(config);
    }


    /**
     * Get all attributes - defined on type and inherited as a map with attribute qualifier as key.
     *
     * @param composedType type
     * @return all attributes for the given type including inherited attributes
     */
    public static Map<String, AttributeDescriptorModel> getAllAttributesIncludingInherited(final ComposedTypeModel composedType) {
        final Collection<AttributeDescriptorModel> allAttributes = new ArrayList<>(composedType.getDeclaredattributedescriptors());
        allAttributes.addAll(composedType.getInheritedattributedescriptors());
        return allAttributes.stream().collect(Collectors.toMap(DescriptorModel::getQualifier, attribute -> attribute));
    }

    /**
     * Create a unique string expression identifying the LocalisedValue
     *
     * @param composedType composed type
     * @param prefix       prefix
     * @return
     */
    private Stream<String> createUniqueExpression(final ComposedTypeModel composedType, final String prefix) {
        // if custom unique key fields are configured for this type, return them directly
        final List<String> customKeys = customUniqueKeyFields.get(composedType.getCode());
        if (CollectionUtils.isNotEmpty(customKeys)) {
            return customKeys.stream().map(key -> prefix + key);
        }

        final Collection<AttributeDescriptorModel> attributes = getAllAttributesIncludingInherited(composedType).values();

        return attributes.stream()
                .filter(AttributeDescriptorModel::getUnique)
                .sorted(Comparator.comparing(a -> a.getAttributeType() instanceof AtomicTypeModel))
                .sorted(Comparator.comparing(a -> a.equals(composedType.getCatalogVersionAttribute()))
                        .reversed())
                .flatMap(attribute -> {
                    if (attribute.getAttributeType() instanceof AtomicTypeModel) {
                        return Stream.of(prefix + attribute.getQualifier());
                    }
                    if (attribute.getAttributeType() instanceof final ComposedTypeModel targetComposed) {
                        return createUniqueExpression(targetComposed, prefix + attribute.getQualifier() + ".");
                    }
                    return Stream.empty();
                });
    }

    /**
     * Check if the attribute is valid for translation purposes - readable, writeable, non-dynamic, localised string.
     * If checkForData is true also check if there are any items in DB with the attribute specified.
     *
     * @param attribute    attribute to check
     * @param checkForData flag indicating if the configuration should be done only if data for this attribute exists
     * @return true if valid for translation
     */
    private boolean isAttributeEligible(final AttributeDescriptorModel attribute, final boolean checkForData, final List<AttributeDescriptorModel> excludedProperties) {

        final String composedTypeCode = attribute.getEnclosingType().getCode();
        final String attributeQualifier = attribute.getQualifier();

        // check if attribute is in the list of excluded attributes
        if (CollectionUtils.isNotEmpty(excludedProperties) && excludedProperties.contains(attribute)) {
            return false;
        }

        // only localised attributes
        if (!Boolean.TRUE.equals(attribute.getLocalized())) {
            return false;
        }
        // leave only readable, writable & non-dynamic attributes
        if (!Boolean.TRUE.equals(attribute.getWritable()) || !Boolean.TRUE.equals(attribute.getReadable()) || !Boolean.TRUE.equals(attribute.getProperty())) {
            return false;
        }

        final TypeModel attributeType = attribute.getAttributeType();
        if (attributeType instanceof final MapTypeModel mapType) {
            final TypeModel returntype = mapType.getReturntype();

            // only localised string attributes
            if (isAtomicType(returntype)) {
                if (!checkForData) {
                    return true;
                }
                // check if items exists with the given attribute specified (not null)
                final FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery("SELECT {PK} FROM {" + composedTypeCode + "} WHERE {" + attributeQualifier + "} IS NOT NULL");
                flexibleSearchQuery.setCount(1);
                final boolean result = !flexibleSearchService.search(flexibleSearchQuery).getResult().isEmpty();
                LOG.info("Items of type [{}] with the attribute [{}] value specified exist: [{}]", composedTypeCode, attributeQualifier, result);
                return result;
            }
        }
        return false;
    }

    private static boolean isAtomicType(final TypeModel returntype) {
        return returntype instanceof AtomicTypeModel && ((AtomicTypeModel) returntype).getJavaClass()
                .equals(String.class);
    }

    /**
     * Detect not active catalog version for the given composed type.
     * If cxtranslations.autoconfig.catalogVersionPatterns provided filter the catalogs based on the pattern.
     *
     * @param composedType        composed type
     * @param catalogVersionField the field holding the catalog version
     * @return set of valid catalog versions for the given composed type
     */
    private Set<CatalogVersionModel> detectCatalogVersion(final ComposedTypeModel composedType, final String catalogVersionField, final Set<CatalogVersionModel> catalogVersions) {

        // find non-active catalog versions or classification system version
        final FlexibleSearchQuery query = new FlexibleSearchQuery("SELECT DISTINCT {i." + catalogVersionField + "} FROM {" + composedType.getCode() + " AS i } WHERE {i." + catalogVersionField + "} IN (?catalogVersions)");
        query.addQueryParameter("catalogVersions", catalogVersions);

        final List<CatalogVersionModel> catalogVersionsForItem = flexibleSearchService.<CatalogVersionModel>search(query)
                .getResult();

        if (catalogVersionsForItem.size() == 1) {
            return Set.of(catalogVersionsForItem.get(0));
        }

        return new HashSet<>(catalogVersionsForItem);
    }

    /**
     * Check if the given type should be skipped when creating the configuration.
     * <ul>
     *     <li>No unique attributes found</li>
     *     <li>System item</li>
     *     <li>Is root item with parent set to ComposedType</li>
     * </ul>
     *
     * @param composedType {@link ComposedTypeModel}
     * @param isRoot
     * @return true if the type should be skipped
     */
    private boolean shouldSkipType(final ComposedTypeModel composedType, final boolean isRoot, final List<ComposedTypeModel> excludedComposedTypes) {

        if (CollectionUtils.isNotEmpty(excludedComposedTypes) && excludedComposedTypes.contains(composedType)) {
            return true;
        }

        final Collection<ComposedTypeModel> superTypes = composedType.getAllSuperTypes();
        if (CollectionUtils.isNotEmpty(superTypes) && superTypes.stream().anyMatch(superType -> excludedComposedTypes.contains(superType))) {
            return true;
        }

        // types with no unique attributes (but allow types with custom unique key fields configured)
        if (getAllAttributesIncludingInherited(composedType).values()
                .stream()
                .noneMatch(AttributeDescriptorModel::getUnique)
                && !customUniqueKeyFields.containsKey(composedType.getCode())) {
            return true;
        }

        // system types
        if (Boolean.TRUE.equals(composedType.getSystemType())) {
            return true;
        }

        // root type under the ComposedType
        final ComposedTypeModel superType = composedType.getSuperType();
        return isRoot && superType != null && superType.getCode().equals(ComposedTypeModel._TYPECODE);
    }

    /**
     * Get or create a {@link CxLocalizedObjectConfigJobModel} instance. There should only be one present.
     * Protected against race conditions: if two threads concurrently try to create the job,
     * the one that loses the unique-code constraint race catches {@link de.hybris.platform.servicelayer.exceptions.ModelSavingException}
     * and refetches the already-persisted instance.
     *
     * @return {@link CxLocalizedObjectConfigJobModel}
     */
    protected JobModel getOrCreateCxLocalizedObjectConfigJob() {
        JobModel cxLocalizedObjectConfigJob;
        try {
            cxLocalizedObjectConfigJob = cronJobService.getJob("cxLocalizedObjectConfigJob");
        } catch (final UnknownIdentifierException e) {
            final CxLocalizedObjectConfigJobModel newJob = modelService.create(CxLocalizedObjectConfigJobModel.class);
            newJob.setCode("cxLocalizedObjectConfigJob");
            newJob.setActive(true);
            try {
                modelService.save(newJob);
                return newJob;
            } catch (final de.hybris.platform.servicelayer.exceptions.ModelSavingException savingException) {
                LOG.warn("Race condition detected while creating cxLocalizedObjectConfigJob – refetching existing instance.", savingException);
                cxLocalizedObjectConfigJob = cronJobService.getJob("cxLocalizedObjectConfigJob");
            }
        }

        cxLocalizedObjectConfigJob.setActive(true);
        return cxLocalizedObjectConfigJob;
    }

    /**
     * Configure the {@link CxLocalizedObjectConfigCronJobModel} basic settings
     *
     * @param cronJob {@link CxLocalizedObjectConfigCronJobModel}
     */
    protected void configureCronJobBasicSettings(final CxLocalizedObjectConfigCronJobModel cronJob) {
        cronJob.setJob(getOrCreateCxLocalizedObjectConfigJob());

        if (clusterService.isClusteringEnabled() && clusterService.getClusterGroups().contains(cronJobNodeGroup)) {
            cronJob.setNodeGroup(cronJobNodeGroup);
        }

        if (cronJobPriority > 0) {
            cronJob.setPriority(cronJobPriority);
        }

        final UserModel user = userService.getCurrentUser();
        cronJob.setSessionLanguage(user.getSessionLanguage());
        cronJob.setSessionUser(user);

        cronJob.setActive(true);
        cronJob.setRemoveOnExit(false);
    }

    /**
     * Configure default merge mode for {@link CxLocalizedObjectConfigCronJobModel}
     *
     * @param cxLocalizedObjectConfigCronJob {@link CxLocalizedObjectConfigCronJobModel}
     */
    protected void configureDefaultMergeMode(final CxLocalizedObjectConfigCronJobModel cxLocalizedObjectConfigCronJob) {
        if (this.defaultMergeMode != null) {
            cxLocalizedObjectConfigCronJob.setMergeMode(this.defaultMergeMode);
        }
    }


    /**
     * Configure default catalog versions for {@link CxLocalizedObjectConfigCronJobModel}
     *
     * @param cxLocalizedObjectConfigCronJob {@link CxLocalizedObjectConfigCronJobModel}
     */
    protected void configureDefaultCatalogVersions(final CxLocalizedObjectConfigCronJobModel cxLocalizedObjectConfigCronJob) {

        // find non-active catalog versions or classification system version
        final FlexibleSearchQuery query = new FlexibleSearchQuery("SELECT DISTINCT {cv." + CatalogVersionModel.PK + "} FROM {" + CatalogVersionModel._TYPECODE + " AS cv " + " JOIN " + ComposedTypeModel._TYPECODE + " AS t ON {cv." + CatalogVersionModel.ITEMTYPE + "} = {t." + ComposedTypeModel.PK + "} " + "} WHERE {cv." + CatalogVersionModel.ACTIVE + "} = ?active OR {t." + ComposedTypeModel.CODE + "} = ?classificationSystemVersion");

        query.addQueryParameter("active", false);
        query.addQueryParameter("classificationSystemVersion", ClassificationSystemVersionModel._TYPECODE);

        final List<CatalogVersionModel> catalogVersions = flexibleSearchService.<CatalogVersionModel>search(query)
                .getResult();

        final Set<CatalogVersionModel> filteredCatalogVersions;

        if (CollectionUtils.isNotEmpty(excludedCatalogPatterns)) {
            filteredCatalogVersions = catalogVersions.stream()
                    .filter(cv -> excludedCatalogPatterns.stream()
                            .noneMatch(pattern -> pattern.test(cv.getCatalog()
                                    .getId())))

                    .collect(Collectors.toSet());


        } else {
            filteredCatalogVersions = new HashSet<>(catalogVersions);
        }

        cxLocalizedObjectConfigCronJob.setCatalogVersions(filteredCatalogVersions);
    }

    /**
     * Configure default attributes for {@link CxLocalizedObjectConfigCronJobModel}
     *
     * @param cxLocalizedObjectConfigCronJob {@link CxLocalizedObjectConfigCronJobModel}
     */
    protected void configureDefaultExcludedAttributes(final CxLocalizedObjectConfigCronJobModel cxLocalizedObjectConfigCronJob) {

        if (MapUtils.isEmpty(this.defaultExcludedPropertiesMap)) {
            return;
        }

        final List<AttributeDescriptorModel> excludedAttributeDescriptors = new ArrayList<>();

        final FlexibleSearchQuery attributesQuery = new FlexibleSearchQuery("SELECT {a." + AttributeDescriptorModel.PK + "} " + " FROM {" + ComposedTypeModel._TYPECODE + " AS t JOIN " + AttributeDescriptorModel._TYPECODE + " AS a " + " ON {a." + AttributeDescriptorModel.ENCLOSINGTYPE + "} = {t." + ComposedTypeModel.PK + "} } " + " WHERE {t." + ComposedTypeModel.CODE + "} = ?typeCode AND {a." + AttributeDescriptorModel.QUALIFIER + "} IN (?attributeQualifiers)");

        defaultExcludedPropertiesMap.entrySet().forEach(entry -> {
            attributesQuery.addQueryParameter("typeCode", entry.getKey());
            attributesQuery.addQueryParameter("attributeQualifiers", entry.getValue());

            final SearchResult<AttributeDescriptorModel> excludedAttributes;
            try {
                excludedAttributes = flexibleSearchService.search(attributesQuery);
            } catch (final FlexibleSearchException ex) {
                LOG.error("Can't find attributes for type {} and qualifiers {}", entry.getKey(), entry.getValue(), ex);
                return;
            }
            if (excludedAttributes.getCount() > 0) {
                excludedAttributeDescriptors.addAll(excludedAttributes.getResult());
            }
        });

        cxLocalizedObjectConfigCronJob.setExcludedAttributes(excludedAttributeDescriptors);
    }

    /**
     * Configure default types for {@link CxLocalizedObjectConfigCronJobModel}
     *
     * @param cxLocalizedObjectConfigCronJob
     */
    protected void configureDefaultExcludedTypes(final CxLocalizedObjectConfigCronJobModel cxLocalizedObjectConfigCronJob) {
        if (CollectionUtils.isEmpty(this.defaultExcludedTypeCodes)) {
            return;
        }

        final FlexibleSearchQuery typesQuery = new FlexibleSearchQuery("SELECT {t." + ComposedTypeModel.PK + "} FROM {" + ComposedTypeModel._TYPECODE + " AS t} WHERE {t." + ComposedTypeModel.CODE + "} IN (?typeCodes)");
        typesQuery.addQueryParameter("typeCodes", this.defaultExcludedTypeCodes);

        final SearchResult<ComposedTypeModel> excludedComposedTypes;
        try {
            excludedComposedTypes = flexibleSearchService.search(typesQuery);
        } catch (final FlexibleSearchException ex) {
            LOG.error("Can't find types for codes {}", this.defaultExcludedTypeCodes, ex);
            return;
        }

        if (excludedComposedTypes.getCount() > 0) {
            cxLocalizedObjectConfigCronJob.setExcludedComposedTypes(excludedComposedTypes.getResult());
        }
    }


    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }


    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setExcludedCatalogPatterns(final String excludedCatalogPatterns) {
        if (StringUtils.isBlank(excludedCatalogPatterns)) {
            this.excludedCatalogPatterns = Collections.emptyList();
            return;
        }
        this.excludedCatalogPatterns = Arrays.stream(excludedCatalogPatterns.split(","))
                .map(StringUtils::trim)
                .filter(StringUtils::isNotBlank)
                .map(Pattern::compile)
                .map(Pattern::asMatchPredicate)
                .toList();
    }

    public void setExcludedTypes(final String excludedTypeCodes) {
        if (StringUtils.isBlank(excludedTypeCodes)) {
            this.defaultExcludedTypeCodes = Collections.emptyList();
            return;
        }
        this.defaultExcludedTypeCodes = Arrays.stream(excludedTypeCodes.split(","))
                .map(StringUtils::trim)
                .filter(StringUtils::isNotBlank)
                .toList();

    }

    public void setDefaultExcludedPropertiesMap(final String defaultExcludedPropertiesMap) {

        if (StringUtils.isBlank(defaultExcludedPropertiesMap)) {
            this.defaultExcludedPropertiesMap = Collections.emptyMap();
            return;
        }

        this.defaultExcludedPropertiesMap = Arrays.stream(defaultExcludedPropertiesMap.split(","))
                .map(StringUtils::trim)
                .filter(StringUtils::isNotBlank)
                .map(entry -> entry.split("\\."))
                .filter(parts -> {
                    if (parts.length < 2) {
                        LOG.warn("Ignoring malformed defaultExcludedPropertiesMap entry (expected TypeCode.attributeQualifier): {}", String.join(".", parts));
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.groupingBy(entry -> entry[0],  // Key extractor: first part before the dot
                        Collectors.mapping(entry -> entry[1],  // Value extractor: second part after the dot
                                Collectors.toList() // Collect values into a list
                        )));
    }

    public void setDefaultMergeMode(final String mergeMode) {
        if (StringUtils.isNotBlank(mergeMode)) {
            if (CxMergeMode.REPLACE.getCode().equals(mergeMode)) {
                this.defaultMergeMode = CxMergeMode.REPLACE;
            } else if (CxMergeMode.KEEP_EXISTING.getCode().equals(mergeMode)) {
                this.defaultMergeMode = CxMergeMode.KEEP_EXISTING;
            }
        }

        if (this.defaultMergeMode == null) {
            LOG.error("Invalid Merge mode {}. Setting the default value {}!", mergeMode, CxMergeMode.KEEP_EXISTING);
            this.defaultMergeMode = CxMergeMode.KEEP_EXISTING;
        }
    }


    public void setCronJobService(final CronJobService cronJobService) {
        this.cronJobService = cronJobService;
    }

    public void setUserService(final UserService userService) {
        this.userService = userService;
    }

    public void setClusterService(final ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    public void setCronJobPriority(final int cronJobPriority) {
        this.cronJobPriority = cronJobPriority;
    }

    public void setCronJobNodeGroup(final String cronJobNodeGroup) {
        this.cronJobNodeGroup = cronJobNodeGroup;
    }

    public void setTypeService(final TypeService typeService) {
        this.typeService = typeService;
    }

    /**
     * Set custom unique key fields from a string property.
     * Expected format: {@code TypeCode1->[field1,field2],TypeCode2->[field3,field4]}
     * Example: {@code SolrIndexProperty->[indexedType.id,name],Type2->[field1,field2]}
     *
     * @param customUniqueKeyFields string representation of the custom unique key fields map
     */
    public void setCustomUniqueKeyFields(final String customUniqueKeyFields) {
        if (StringUtils.isBlank(customUniqueKeyFields)) {
            this.customUniqueKeyFields = Collections.emptyMap();
            return;
        }

        final Pattern entryPattern = Pattern.compile("(\\w+)->\\[([^]]+)]");
        final var matcher = entryPattern.matcher(customUniqueKeyFields);
        final Map<String, List<String>> result = new java.util.HashMap<>();

        while (matcher.find()) {
            final String typeCode = matcher.group(1).trim();
            final List<String> fields = Arrays.stream(matcher.group(2).split(","))
                    .map(StringUtils::trim)
                    .filter(StringUtils::isNotBlank)
                    .toList();
            result.put(typeCode, fields);
        }

        this.customUniqueKeyFields = result;
    }
}
