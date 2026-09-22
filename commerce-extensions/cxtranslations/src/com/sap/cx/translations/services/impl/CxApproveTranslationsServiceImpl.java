/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.services.impl;

import com.sap.cx.translations.enums.CxApprovalTranslationStatus;
import com.sap.cx.translations.enums.CxTranslateLocalizedValueTargetType;
import com.sap.cx.translations.model.CxApproveTranslationsCronJobModel;
import com.sap.cx.translations.model.CxApproveTranslationsJobModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.services.CxApproveTranslationsService;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedClassificationService;
import de.hybris.platform.catalog.model.ProductFeatureModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.cronjob.model.JobModel;
import de.hybris.platform.servicelayer.cluster.ClusterService;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.exceptions.ModelRemovalException;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Translation approval service
 */
public class CxApproveTranslationsServiceImpl implements CxApproveTranslationsService {

    private static final Logger LOG = LoggerFactory.getLogger(CxApproveTranslationsServiceImpl.class);

    private String cronJobNodeGroup;
    private int cronJobPriority;

    private ModelService modelService;
    private CronJobService cronJobService;
    private ClusterService clusterService;
    private UserService userService;
    private CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService;
    private CxLocalizedClassificationService cxLocalizedClassificationService;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean approveTranslation(final CxProposedTranslationModel translationToApprove) {

        boolean succesfulyApproved = true;

        // in case another process removed the item skip it
        if (modelService.isRemoved(translationToApprove)) {
            LOG.warn("Translation to approve no longer exists!");
            return true;
        }

        final Collection<LanguageModel> targetLanguages = translationToApprove.getTranslatedLanguages();
        final List<Locale> targetLocales = targetLanguages.stream()
                .map(language -> cxTranslateCommerceCommonI18NService.getLocale(language.getIsocode()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(targetLocales)) {
            LOG.error("Failed to approve proposed translation [{}]. No locales found!", translationToApprove.getKey());
            final String note = "Failed to approve proposed translation [" + translationToApprove.getKey() + "] ";
            translationToApprove.setNotes(translationToApprove.getNotes() != null ? translationToApprove.getNotes() + '\n' + note : note);
            translationToApprove.setApprovalStatus(CxApprovalTranslationStatus.ERROR);
            return false;
        }

        final Map<Locale, String> proposedValuesMap = new HashMap<>();
        final Map<Locale, String> manualValuesMap = new HashMap<>();

        targetLocales.forEach(locale -> {
            proposedValuesMap.put(locale, translationToApprove.getProposedTranslationValue(locale));
            manualValuesMap.put(locale, translationToApprove.getManualTranslationValue(locale));
        });

        // set localised values
        for (final Locale locale : targetLocales) {
            try {
                // if manual value has been provided - it has higher priority then the auto generated one
                final String manualValue = manualValuesMap.get(locale);
                if (isValidManualValue(manualValue)) {
                    translationToApprove.setValue(manualValue, locale);
                } else {
                    translationToApprove.setValue(proposedValuesMap.get(locale), locale);
                }
            } catch (final Exception ex) {
                LOG.error("Failed to set value on target item for [{}] proposed translation and locale [{}]!", translationToApprove.getKey(), locale.getISO3Country(), ex);
                final String note = "Failed to set value on target item for [" + translationToApprove.getKey() + "] proposed translation [" + translationToApprove.getAttribute() + "] and locale [" + locale.getISO3Language() + "]. Error message: " + ex.getMessage();
                translationToApprove.setNotes(translationToApprove.getNotes() != null ? translationToApprove.getNotes() + '\n' + note : note);
                translationToApprove.setApprovalStatus(CxApprovalTranslationStatus.ERROR);
                succesfulyApproved = false;
            }
        }

        // try to save the target item
        try {
            if (translationToApprove.getTargetType() != CxTranslateLocalizedValueTargetType.PRODUCT_FEATURES) {
                this.modelService.save(translationToApprove.getTarget());
            } else {
                cxLocalizedClassificationService.getRelatedFeatures((ProductFeatureModel) translationToApprove.getTarget())
                        .forEach(modelService::save);
            }
        } catch (final ModelSavingException ex) {
            LOG.error("Failed to save target item for [{}] proposed translation!", translationToApprove.getKey(), ex);
            final String note = "Failed to save target item for [" + translationToApprove.getKey() + "] proposed translation [" + translationToApprove.getAttribute() + "]. Error message: " + ex.getMessage();
            translationToApprove.setNotes(translationToApprove.getNotes() != null ? translationToApprove.getNotes() + '\n' + note : note);
            translationToApprove.setApprovalStatus(CxApprovalTranslationStatus.ERROR);
            succesfulyApproved = false;
        }

        // remove the item if the approval was successfully
        if (succesfulyApproved) {
            try {
                this.modelService.remove(translationToApprove);
            } catch (final ModelRemovalException ex) {
                LOG.error("Failed to remove proposed translation with key [{}]", translationToApprove.getKey(), ex);
                succesfulyApproved = false;
            }
        } else {
            try {
                this.modelService.save(translationToApprove);
            } catch (final ModelSavingException ex) {
                LOG.error("Failed to save proposed translation [{}]", translationToApprove.getKey(), ex);
            }
        }

        return succesfulyApproved;
    }

    /**
     * Create the {@link CxApproveTranslationsCronJobModel} to approve translations in context of given user
     *
     * @return {@link CxApproveTranslationsCronJobModel} cron job
     */
    @Override
    public CxApproveTranslationsCronJobModel approveTranslations(final List<CxProposedTranslationModel> translationsToApprove) {

        final CxApproveTranslationsCronJobModel cronJob = modelService.create(CxApproveTranslationsCronJobModel.class);

        cronJob.setActive(true);
        cronJob.setJob(getOrCreateCxApproveTranslationsJob());

        final UserModel user = userService.getCurrentUser();
        cronJob.setSessionLanguage(user.getSessionLanguage());
        cronJob.setSessionUser(user);
        cronJob.setRemoveOnExit(false);
        cronJob.setTranslationsToApprove(translationsToApprove);
        cronJob.setElementsToProcessCount(translationsToApprove.size());

        if (clusterService.isClusteringEnabled() && clusterService.getClusterGroups().contains(cronJobNodeGroup)) {
            cronJob.setNodeGroup(cronJobNodeGroup);
        }

        if (cronJobPriority > 0) {
            cronJob.setPriority(cronJobPriority);
        }

        try {
            modelService.save(cronJob);
        } catch (final ModelSavingException ex) {
            cronJob.setTranslationsToApprove(translationsToApprove.stream()
                    .filter(translation -> !modelService.isRemoved(translation))
                    .collect(Collectors.toList()));
            modelService.save(cronJob);
        }

        // execute cron job in asynchronous way
        cronJobService.performCronJob(cronJob, false);

        return cronJob;
    }

    /**
     * Get or create a {@link CxApproveTranslationsJobModel} instance. There should only be one present.
     * Protected against race conditions: if two threads concurrently try to create the job,
     * the one that loses the unique-code constraint race catches {@link ModelSavingException}
     * and refetches the already-persisted instance.
     *
     * @return {@link JobModel} job
     */
    protected JobModel getOrCreateCxApproveTranslationsJob() {
        JobModel approveTranslationsJob;
        try {
            approveTranslationsJob = cronJobService.getJob("cxApproveTranslationsJob");
        } catch (final UnknownIdentifierException e) {
            final CxApproveTranslationsJobModel newJob = modelService.create(CxApproveTranslationsJobModel.class);
            newJob.setCode("cxApproveTranslationsJob");
            newJob.setActive(true);
            try {
                modelService.save(newJob);
                return newJob;
            } catch (final ModelSavingException savingException) {
                LOG.warn("Race condition detected while creating cxApproveTranslationsJob – refetching existing instance.", savingException);
                try {
                    approveTranslationsJob = cronJobService.getJob("cxApproveTranslationsJob");
                } catch (final UnknownIdentifierException ex) {
                    // The save failed for a reason other than the race (e.g., schema mismatch);
                    // surface the original save failure instead of a misleading lookup failure.
                    throw new IllegalStateException("Failed to persist cxApproveTranslationsJob", savingException);
                }
            }
        }

        if (approveTranslationsJob == null) {
            throw new IllegalStateException("cxApproveTranslationsJob could not be loaded or created");
        }

        approveTranslationsJob.setActive(true);

        return approveTranslationsJob;
    }

    /**
     * Returns true if the manually provided value for translation is valid and not empty
     *
     * @param manualValue
     * @return
     */
    protected boolean isValidManualValue(final String manualValue) {
        if (manualValue == null) {
            return false;
        }
        return !StringUtils.isBlank(manualValue);
    }

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setCronJobService(final CronJobService cronJobService) {
        this.cronJobService = cronJobService;
    }

    public void setClusterService(final ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    public void setCronJobNodeGroup(final String cronJobNodeGroup) {
        this.cronJobNodeGroup = cronJobNodeGroup;
    }


    public void setCronJobPriority(final int cronJobPriority) {
        this.cronJobPriority = cronJobPriority;
    }

    public void setUserService(final UserService userService) {
        this.userService = userService;
    }

    public void setCxTranslateCommerceCommonI18NService(final CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService) {
        this.cxTranslateCommerceCommonI18NService = cxTranslateCommerceCommonI18NService;
    }

    public void setCxLocalizedClassificationService(final CxLocalizedClassificationService cxLocalizedClassificationService) {
        this.cxLocalizedClassificationService = cxLocalizedClassificationService;
    }
}
