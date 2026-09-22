/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.enums.CxApprovalTranslationStatus;
import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import com.sap.cx.translations.model.CxTranslationJobModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import com.sap.cx.translations.services.CxLocalizedLabelsPluralsService;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForItemsParameters;
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForValuesParameters;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.cronjob.model.JobModel;
import de.hybris.platform.servicelayer.cluster.ClusterService;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Creates, configures and launches {@link CxTranslationCronJobModel} instances.
 *
 * <p>Extracted from {@code CxLocalizedValueServiceImpl} (improvement #1) to isolate
 * cron-job orchestration from search and AI-translation concerns.
 */
public class CxTranslationCronJobOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(CxTranslationCronJobOrchestrator.class);
    private static final String CX_TRANSLATION_JOB_CODE = "cxTranslationJob";

    private ModelService modelService;
    private CronJobService cronJobService;
    private UserService userService;
    private ClusterService clusterService;
    private CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService;
    private CxLocalizedLabelsPluralsService pluralsService;
    private String cronJobNodeGroup;
    private int cronJobPriority;

    public CxTranslationCronJobModel startTranslationCronJobForItems(
            final CxStartTranslationCronJobForItemsParameters parameters) {
        final CxTranslationCronJobModel cronJob = createTranslationCronJob(
                parameters.getServiceSettings(),
                parameters.getSourceLanguage(),
                parameters.getTargetLanguages(),
                parameters.getUserHint(),
                parameters.getSaveForApproval(),
                parameters.getSkipTranslated());
        modelService.save(cronJob);

        cronJob.setValuesToTranslate(Collections.emptyList());
        cronJob.setItems(parameters.getItems());
        cronJob.setAttributes(parameters.getAttributes());
        cronJob.setElementsToProcessCount(parameters.getItems().size());
        cronJob.setTranslateProductFeatures(parameters.isTranslateProductFeatures());
        cronJob.setAutomaticPlurals(parameters.isAutomaticPlurals());
        modelService.save(cronJob);

        cronJobService.performCronJob(cronJob, false);
        return cronJob;
    }

    public CxTranslationCronJobModel startTranslationCronJob(
            final CxStartTranslationCronJobForValuesParameters parameters) {
        final CxTranslationCronJobModel cronJob = createTranslationCronJob(
                parameters.getServiceSettings(),
                parameters.getSourceLanguage(),
                parameters.getTargetLanguages(),
                parameters.getUserHint(),
                parameters.getSaveForApproval(),
                parameters.getSkipTranslated());
        modelService.save(cronJob);

        final List<CxProposedTranslationModel> proposedTranslations = parameters.getLocalizedValues().stream()
                .map(value -> createCxProposedTranslation(value, cronJob))
                .filter(Objects::nonNull)
                .toList();
		
		if (parameters.isSmartEditTranslation()) {
			proposedTranslations.forEach(translation -> {
				translation.setSmartEditTranslation(true);
				modelService.save(translation);
			});
		}

        cronJob.setValuesToTranslate(proposedTranslations);
        cronJob.setItems(Collections.emptyList());
        cronJob.setAttributes(Collections.emptySet());
        cronJob.setElementsToProcessCount(proposedTranslations.size());
        modelService.save(cronJob);

        cronJobService.performCronJob(cronJob, false);
        return cronJob;
    }

    public CxTranslationCronJobModel retryTranslations(
            final CxTranslationCronJobModel oldCronJob,
            final List<CxProposedTranslationModel> proposedTranslations,
            final CxTranslationServiceSettingsModel serviceOverride,
            final String hintOverride) {
        if (cronJobService.isRunning(oldCronJob)) {
            throw new IllegalStateException("Cannot restart translations from a cron job that is still running");
        }
        final String hint = StringUtils.isNotBlank(hintOverride) ? hintOverride : oldCronJob.getHint();
        final CxTranslationServiceSettingsModel settings = serviceOverride != null ? serviceOverride : oldCronJob.getTranslationServiceSettings();

        final CxTranslationCronJobModel cronJob = createTranslationCronJob(
                settings,
                oldCronJob.getSourceLanguage(),
                oldCronJob.getTargetLanguages(),
                hint,
                oldCronJob.getSaveForApproval(),
                oldCronJob.getSkipTranslated());
        modelService.save(cronJob);

        proposedTranslations.forEach(translation -> {
            translation.setTranslationCronJob(cronJob);
            translation.setTranslatedLanguages(Collections.emptyList());
            translation.setTranslationStatus(CxTranslationStatus.PENDING);
            translation.setApprovalStatus(CxApprovalTranslationStatus.PENDING);
            translation.setApproveTranslationsCronJob(null);
            oldCronJob.getTargetLanguages().forEach(targetLanguage ->
                    translation.setProposedTranslationValue(null, cxTranslateCommerceCommonI18NService.getLocaleForLanguage(targetLanguage)));
            modelService.save(translation);
        });

        cronJob.setItems(Collections.emptyList());
        cronJob.setAttributes(Collections.emptySet());
        cronJob.setElementsToProcessCount(proposedTranslations.size());
        modelService.save(cronJob);

        cronJobService.performCronJob(cronJob, false);
        return cronJob;
    }

    public CxProposedTranslationModel createCxProposedTranslation(
            final CxLocalizedValueModel value,
            final CxTranslationCronJobModel cronJob) {
        final CxProposedTranslationModel valueToTranslate = modelService.create(CxProposedTranslationModel.class);

        valueToTranslate.setTarget(value.getTarget());
        valueToTranslate.setTargetType(value.getTargetType());
        valueToTranslate.setKey(value.getKey());
        valueToTranslate.setObjectId(value.getObjectId());
        valueToTranslate.setCatalogId(value.getCatalogId());
        valueToTranslate.setCatalogVersionId(value.getCatalogVersionId());
        valueToTranslate.setObjectType(value.getObjectType());
        valueToTranslate.setAttribute(value.getAttribute());
        valueToTranslate.setConfig(value.getConfig());
        valueToTranslate.setTranslationCronJob(cronJob);
        valueToTranslate.setTranslationStatus(CxTranslationStatus.PENDING);
        valueToTranslate.setApprovalStatus(CxApprovalTranslationStatus.PENDING);

        if (cronJob.isAutomaticPlurals()
                && value.getTarget() instanceof final CxLocalizedLabelModel label
                && pluralsService.shouldHavePluralForms(label)) {
            valueToTranslate.setAutomaticPlurals(true);
        }

        try {
            modelService.save(valueToTranslate);
        } catch (final ModelSavingException ex) {
            LOG.error("Duplicate entries found for translation for key {}!", valueToTranslate.getKey());
            return null;
        }
        return valueToTranslate;
    }

    protected CxTranslationCronJobModel createTranslationCronJob(
            final CxTranslationServiceSettingsModel serviceSettings,
            final LanguageModel sourceLanguage,
            final Collection<LanguageModel> targetLanguages,
            final String userHint,
            final Boolean saveForApproval,
            final Boolean skipTranslated) {
        final CxTranslationCronJobModel cronJob = modelService.create(CxTranslationCronJobModel.class);
        cronJob.setActive(true);
        cronJob.setSourceLanguage(sourceLanguage);
        cronJob.setTargetLanguages(targetLanguages);
        cronJob.setHint(userHint);
        cronJob.setTranslationServiceSettings(serviceSettings);
        cronJob.setJob(getOrCreateCxTranslationJob(sourceLanguage));
        cronJob.setSessionLanguage(sourceLanguage);
        cronJob.setSessionUser(userService.getCurrentUser());
        cronJob.setRemoveOnExit(false);
        cronJob.setSaveForApproval(saveForApproval);
        cronJob.setSkipTranslated(skipTranslated);

        if (clusterService.isClusteringEnabled() && clusterService.getClusterGroups().contains(cronJobNodeGroup)) {
            cronJob.setNodeGroup(cronJobNodeGroup);
        }
        if (cronJobPriority > 0) {
            cronJob.setPriority(cronJobPriority);
        }
        modelService.save(cronJob);
        return cronJob;
    }

    protected JobModel getOrCreateCxTranslationJob(final LanguageModel sourceLanguage) {
        JobModel cxTranslationJob;
        try {
            cxTranslationJob = cronJobService.getJob(CX_TRANSLATION_JOB_CODE);
        } catch (final UnknownIdentifierException e) {
            final CxTranslationJobModel newJob = modelService.create(CxTranslationJobModel.class);
            newJob.setCode(CX_TRANSLATION_JOB_CODE);
            newJob.setActive(true);
            newJob.setSessionLanguage(sourceLanguage);
            try {
                modelService.save(newJob);
                return newJob;
            } catch (final ModelSavingException savingException) {
                LOG.warn("Race condition detected while creating cxTranslationJob – refetching existing instance.", savingException);
                cxTranslationJob = cronJobService.getJob(CX_TRANSLATION_JOB_CODE);
            }
        }
        cxTranslationJob.setActive(true);
        cxTranslationJob.setSessionLanguage(sourceLanguage);
        return cxTranslationJob;
    }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
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

    public void setCxTranslateCommerceCommonI18NService(final CxTranslateCommerceCommonI18NService service) {
        this.cxTranslateCommerceCommonI18NService = service;
    }

    public void setPluralsService(final CxLocalizedLabelsPluralsService pluralsService) {
        this.pluralsService = pluralsService;
    }

    public void setCronJobNodeGroup(final String cronJobNodeGroup) {
        this.cronJobNodeGroup = cronJobNodeGroup;
    }

    public void setCronJobPriority(final int cronJobPriority) {
        this.cronJobPriority = cronJobPriority;
    }
}
