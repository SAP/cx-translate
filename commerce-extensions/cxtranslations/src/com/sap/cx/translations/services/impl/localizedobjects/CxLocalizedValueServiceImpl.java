/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.enums.CxTranslateBackofficeSearchInclusion;
import com.sap.cx.translations.model.CxLocalizedObjectConfigModel;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProviderFactory;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService;
import com.sap.cx.translations.services.localizedobjects.CxTranslationsPageable;
import com.sap.cx.translations.services.localizedobjects.parameters.CxPerformTranslationParameters;
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForItemsParameters;
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForValuesParameters;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CxLocalizedValueService}.
 *
 * <p>This class is a façade: it handles search/CRUD directly and delegates AI-translation
 * to {@link CxAiTranslationExecutor} and cron-job orchestration to
 * {@link CxTranslationCronJobOrchestrator}.
 */
public class CxLocalizedValueServiceImpl implements CxLocalizedValueService {

    private ModelService modelService;
    private CxLocalizedObjectProviderFactory providerFactory;
    private CxLocalizedValueConfigService localizedValueConfigService;

    private CxAiTranslationExecutor aiTranslationExecutor;
    private CxTranslationCronJobOrchestrator cronJobOrchestrator;

    // -----------------------------------------------------------------------
    // CRUD
    // -----------------------------------------------------------------------

    @Override
    public void refresh(final CxLocalizedValueModel value) {
        final ItemModel target = value.getTarget();
        if (target != null) {
            modelService.refresh(target);
        }
    }

    @Override
    public void save(final CxLocalizedValueModel value) {
        if (value.getTarget() == null) {
            throw new IllegalStateException(
                    "Cannot save CxLocalizedValueModel: target is null. "
                    + "Set a target item on the value before calling save(). "
                    + "Key: " + value.getKey());
        }
        modelService.save(value.getTarget());
    }

    @Override
    public void save(final Collection<CxLocalizedValueModel> values) {
        if (values != null) {
            values.forEach(this::save);
        }
    }

    // -----------------------------------------------------------------------
    // Search
    // -----------------------------------------------------------------------

    @Override
    public List<CxLocalizedValueModel> getForItem(final ItemModel item) {
        final List<CxLocalizedValueSearchData.SearchCriteria> searchCriterias = getSearchCriteriasForItem(item);
        final CxLocalizedValueSearchData searchData = new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Descending,
                searchCriterias, 1, Collections.emptySet(), true);
        return search(searchData).allResults();
    }

    @Override
    public List<CxLocalizedValueModel> getForKey(final String key) {
        final CxLocalizedValueSearchData.StringWithLocale val = new CxLocalizedValueSearchData.StringWithLocale(key, null);
        final CxLocalizedValueSearchData.SearchCriteria crit = new CxLocalizedValueSearchData.SearchCriteria(
                CxLocalizedValueSearchData.SearchField.KEY,
                CxLocalizedValueSearchData.SearchOperator.EQUALS, val);
        final CxLocalizedValueSearchData searchData = new CxLocalizedValueSearchData(
                CxLocalizedValueSearchData.SortField.CODE,
                CxLocalizedValueSearchData.SortDirection.Descending,
                List.of(crit), 1, Collections.emptySet(), true);
        return search(searchData).allResults();
    }

    @Override
    public CxTranslationsPageable<CxLocalizedValueModel> search(final CxLocalizedValueSearchData searchData) {
        return new CxTranslationsValuesPageableImpl(buildContext(searchData), searchData);
    }

    // -----------------------------------------------------------------------
    // CronJob orchestration — delegate to CxTranslationCronJobOrchestrator
    // -----------------------------------------------------------------------

    @Override
    public CxTranslationCronJobModel startTranslationCronJobForItems(
            final CxStartTranslationCronJobForItemsParameters parameters) {
        return cronJobOrchestrator.startTranslationCronJobForItems(parameters);
    }

    @Override
    public CxTranslationCronJobModel startTranslationCronJob(
            final CxStartTranslationCronJobForValuesParameters parameters) {
        return cronJobOrchestrator.startTranslationCronJob(parameters);
    }

    @Override
    public CxTranslationCronJobModel retryTranslations(
            final CxTranslationCronJobModel oldCronJob,
            final List<CxProposedTranslationModel> proposedTranslations,
            final CxTranslationServiceSettingsModel serviceOverride,
            final String hintOverride) {
        return cronJobOrchestrator.retryTranslations(oldCronJob, proposedTranslations, serviceOverride, hintOverride);
    }

    @Override
    public CxProposedTranslationModel createCxProposedTranslation(
            final CxLocalizedValueModel value,
            final CxTranslationCronJobModel cronJob) {
        return cronJobOrchestrator.createCxProposedTranslation(value, cronJob);
    }

    // -----------------------------------------------------------------------
    // AI translation — delegate to CxAiTranslationExecutor
    // -----------------------------------------------------------------------

    @Override
    public void aiTranslate(final CxPerformTranslationParameters parameters) {
        aiTranslationExecutor.aiTranslate(parameters);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static List<CxLocalizedValueSearchData.SearchCriteria> getSearchCriteriasForItem(final ItemModel item) {
        final CxLocalizedValueSearchData.StringWithLocale pkValue =
                new CxLocalizedValueSearchData.StringWithLocale(item.getPk().toString(), null);
        final CxLocalizedValueSearchData.StringWithLocale typeValue =
                new CxLocalizedValueSearchData.StringWithLocale(item.getItemtype(), null);

        final List<CxLocalizedValueSearchData.SearchCriteria> criteria = new ArrayList<>();
        criteria.add(new CxLocalizedValueSearchData.SearchCriteria(
                CxLocalizedValueSearchData.SearchField.PK, CxLocalizedValueSearchData.SearchOperator.EQUALS, pkValue));
        criteria.add(new CxLocalizedValueSearchData.SearchCriteria(
                CxLocalizedValueSearchData.SearchField.OBJECT_TYPE, CxLocalizedValueSearchData.SearchOperator.EQUALS, typeValue));
        return criteria;
    }

    protected CxLocalizedObjectsContext buildContext(final CxLocalizedValueSearchData searchData) {
        final Set<String> forceConfigs = searchData.forceUsingConfigs();
        final boolean forceAll = searchData.forceAllConfigs();

        final List<CxLocalizedObjectConfigModel> result = localizedValueConfigService.findAllConfigs()
                .stream().filter(config -> {
                    final CxTranslateBackofficeSearchInclusion inclusion = config.getUseInBackofficeSearch();
                    if (forceAll) {
                        return true;
                    }
                    if (inclusion == CxTranslateBackofficeSearchInclusion.EXCLUDED_ALWAYS) {
                        return false;
                    }
                    if (inclusion == CxTranslateBackofficeSearchInclusion.INCLUDED) {
                        return true;
                    }
                    return forceConfigs.contains(config.getCode());
                })
                .toList();

        final CxLocalizedValuesSearchSupport searchSupport = new CxLocalizedValuesSearchSupport(searchData);
        final Set<String> requiredTypes = searchData.criteria().stream()
                .filter(c -> c.field() == CxLocalizedValueSearchData.SearchField.OBJECT_TYPE
                        && c.operator() == CxLocalizedValueSearchData.SearchOperator.EQUALS)
                .map(c -> c.value().stringValue())
                .collect(Collectors.toSet());

        final List<CxLocalizedObjectProvider> providers = result.stream()
                .map(config -> providerFactory.createProvider(searchData, config, searchSupport))
                .filter(Objects::nonNull)
                .filter(provider -> requiredTypes.stream().allMatch(provider::canReturnObjectType))
                .toList();

        return new CxLocalizedObjectsContext(providers, searchData, searchSupport);
    }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setProviderFactory(final CxLocalizedObjectProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    public void setLocalizedValueConfigService(final CxLocalizedValueConfigService localizedValueConfigService) {
        this.localizedValueConfigService = localizedValueConfigService;
    }

    public void setAiTranslationExecutor(final CxAiTranslationExecutor aiTranslationExecutor) {
        this.aiTranslationExecutor = aiTranslationExecutor;
    }

    public void setCronJobOrchestrator(final CxTranslationCronJobOrchestrator cronJobOrchestrator) {
        this.cronJobOrchestrator = cronJobOrchestrator;
    }
}
