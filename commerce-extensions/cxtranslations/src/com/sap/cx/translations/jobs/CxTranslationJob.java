/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.jobs;

import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import com.sap.cx.translations.services.CxApproveTranslationsService;
import com.sap.cx.translations.services.CxLocalizedLabelsPluralsService;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedClassificationService;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService;
import com.sap.cx.translations.services.localizedobjects.parameters.CxPerformTranslationParameters;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.DescriptorModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.jalo.AbstractProgressTracker;
import de.hybris.platform.cronjob.jalo.CronJobProgressTracker;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Translation job tracking the progress of its execution
 */
public class CxTranslationJob extends AbstractJobPerformable<CxTranslationCronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(CxTranslationJob.class);

    private CxLocalizedValueService cxLocalizedValueService;
    private CxLocalizedClassificationService cxLocalizedClassificationService;
    private CxApproveTranslationsService cxApproveTranslationsService;
    private CommonI18NService commonI18NService;

    private CxLocalizedLabelsPluralsService pluralsService;

    @Override
    public PerformResult perform(final CxTranslationCronJobModel cxTranslationCronJob) {

        final AbstractProgressTracker tracker = createProgressTracker(cxTranslationCronJob);

        Collection<CxProposedTranslationModel> allValues = cxTranslationCronJob.getValuesToTranslate();


        if (CollectionUtils.isEmpty(allValues)) {
            final Collection<? extends ItemModel> items = addPluralFormsIfNeeded(cxTranslationCronJob.getItems(), cxTranslationCronJob);
            final Set<AttributeDescriptorModel> attributes = cxTranslationCronJob.getAttributes();

            final List<String> attributeQualifiers = attributes.stream().map(DescriptorModel::getQualifier).toList();
            final Locale sourceLocale = commonI18NService.getLocaleForLanguage(cxTranslationCronJob.getSourceLanguage());
            final List<CxLocalizedValueModel> localizedValues = items.stream()
                    .flatMap(item -> cxLocalizedValueService.getForItem(item)
                            .stream())
                    .filter(value -> attributeQualifiers.contains(value.getAttribute()))
                    .filter(value -> StringUtils.isNotBlank(value.getValue(sourceLocale))
                            || (cxTranslationCronJob.isAutomaticPlurals() && checkPluralInput(value, sourceLocale)))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (cxTranslationCronJob.isTranslateProductFeatures()) {
                localizedValues.addAll(items.stream()
                        .filter(ProductModel.class::isInstance)
                        .flatMap(product -> cxLocalizedClassificationService.getValuesForProductFeatures((ProductModel) product).stream())
                        .filter(value -> StringUtils.isNotBlank(value.getValue(sourceLocale)))
                        .toList());
            }

            // convert virtual CxLocalizedValue to db stored CxProposedTranslation and assign to cron job
            allValues = localizedValues.stream()
                    .map(value -> cxLocalizedValueService.createCxProposedTranslation(value, cxTranslationCronJob))
                    .filter(Objects::nonNull)
                    .toList();

            if (allValues.isEmpty()) {
                tracker.close();
                return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
            }

            modelService.refresh(cxTranslationCronJob);
            cxTranslationCronJob.setElementsToProcessCount(allValues.size());

            modelService.save(cxTranslationCronJob);
        }

        // in case of cron job rerun mark the previously failed translations as pending
        allValues.stream()
                .filter(value -> CxTranslationStatus.ERROR == value.getTranslationStatus())
                .forEach(value -> {
                    value.setTranslationStatus(CxTranslationStatus.PENDING);
                    modelService.save(value);
                });

        // in case of cron job rerun only process the items marked as PENDING
        final List<CxProposedTranslationModel> labelsToTranslate = allValues.stream()
                .filter(value -> CxTranslationStatus.PENDING == value.getTranslationStatus())
                .toList();

        if (CollectionUtils.isEmpty(labelsToTranslate)) {
            tracker.close();
            return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
        }

        try {
            // start the HAC progress tracker
            tracker.setProgress(0d);
            if (executeTranslation(cxTranslationCronJob, labelsToTranslate, tracker)) {
                return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
            }
            return new PerformResult(CronJobResult.ERROR, CronJobStatus.FINISHED);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.warn("Translation cron job aborted");
            return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.ABORTED);
        } catch (final Exception ex) {
            LOG.error("Error while executing AI translation", ex);
            return new PerformResult(CronJobResult.FAILURE, CronJobStatus.ABORTED);
        } finally {
            // save the progress for HAC tracker
            tracker.close();
        }
    }

    // check if any of the plural forms has a value for the source locale, if yes the singular form should also be translated even if it doesn't have a value
    private boolean checkPluralInput(final CxLocalizedValueModel value, final Locale sourceLocale) {
        final ItemModel target = value.getTarget();
        if (target instanceof final CxLocalizedLabelModel label) {
            return label.getRelatedLabels().stream().anyMatch(plural -> StringUtils.isNotBlank(plural.getValue(sourceLocale)));
        }
        return false;
    }

    protected Collection<? extends ItemModel> addPluralFormsIfNeeded(final Collection<ItemModel> items,
                                                                   final CxTranslationCronJobModel cronJob) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        if (items.stream().anyMatch(item -> !(item instanceof CxLocalizedLabelModel))) {
            return items;
        }
        final List<CxLocalizedLabelModel> labels = items.stream().map(item -> (CxLocalizedLabelModel) item).toList();

        return Stream.concat(items.stream(), pluralsService.getRequiredPluralForms(labels, cronJob.getTargetLanguages(), true).stream())
                .distinct()
                .toList();

    }

    protected boolean executeTranslation(final CxTranslationCronJobModel cronJob, final List<CxProposedTranslationModel> valuesToTranslate, final AbstractProgressTracker tracker) throws InterruptedException {

        final AtomicBoolean successfulExecution = new AtomicBoolean(true);
        final int numberOfElements = valuesToTranslate.size();
        final AtomicInteger alreadyProcessed = new AtomicInteger();

        final CxPerformTranslationParameters parameters = CxPerformTranslationParameters.buildFromCronJob(cronJob)
                .values(valuesToTranslate)
                .afterTranslationCallback(translatedValue -> {
                    if (!modelService.isRemoved(translatedValue)) {
                        // check the result status of the single value translation
                        if (CxTranslationStatus.ERROR == translatedValue.getTranslationStatus()) {
                            successfulExecution.set(false);
                        }

                        // If the item should not be stored for later approval - save the proposed translation directly at target item
                        if (Boolean.FALSE.equals(cronJob.getSaveForApproval())
                                && CxTranslationStatus.SUCCESS == translatedValue.getTranslationStatus()) {
                            cxApproveTranslationsService.approveTranslation(translatedValue);
                        }
                    }
                    // update the tracking information for HAC
                    tracker.setProgress(calculateProgress(numberOfElements, alreadyProcessed.incrementAndGet()));
                })
                .build();

        cxLocalizedValueService.aiTranslate(parameters);
        return successfulExecution.get();
    }

    private static double calculateProgress(final int totalElements, final int alreadyProcessed) {
        // Calculate the progress percentage, handle division by zero
        return (totalElements == 0) ? 100 : (alreadyProcessed / (double) totalElements) * 100;
    }


    @Override
    public boolean isAbortable() {
        return true;
    }

    protected AbstractProgressTracker createProgressTracker(final CxTranslationCronJobModel cronJob) {
        return new CronJobProgressTracker(modelService.getSource(cronJob));
    }

    public void setCxLocalizedValueService(final CxLocalizedValueService cxLocalizedValueService) {
        this.cxLocalizedValueService = cxLocalizedValueService;
    }

    public void setCxLocalizedClassificationService(final CxLocalizedClassificationService cxLocalizedClassificationService) {
        this.cxLocalizedClassificationService = cxLocalizedClassificationService;
    }

    public void setCxApproveTranslationsService(final CxApproveTranslationsService cxApproveTranslationsService) {
        this.cxApproveTranslationsService = cxApproveTranslationsService;
    }

    public void setCommonI18NService(final CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }

    public void setPluralsService(final CxLocalizedLabelsPluralsService pluralsService) {
        this.pluralsService = pluralsService;
    }
}
