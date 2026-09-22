/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.jobs;

import com.sap.cx.translations.enums.CxTranslateBackofficeSearchInclusion;
import com.sap.cx.translations.model.CxLocalizedObjectConfigCronJobModel;
import com.sap.cx.translations.model.CxProductFeatureLocalizedObjectConfigModel;
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel;
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService;
import de.hybris.platform.catalog.model.classification.ClassAttributeAssignmentModel;
import de.hybris.platform.core.model.enumeration.EnumerationValueModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.jalo.AbstractProgressTracker;
import de.hybris.platform.cronjob.jalo.CronJobProgressTracker;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.type.TypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Job responsible for autoconfiguring the {@link com.sap.cx.translations.model.CxLocalizedObjectConfigModel}s
 */
public class CxLocalizedObjectConfigJob extends AbstractJobPerformable<CxLocalizedObjectConfigCronJobModel> {

	private static final Logger LOG = LoggerFactory.getLogger(CxLocalizedObjectConfigJob.class);
	private CxLocalizedValueConfigService cxLocalizedValueConfigService;
	private TypeService typeService;


	@Override
	public PerformResult perform(final CxLocalizedObjectConfigCronJobModel cxLocalizedObjectConfigCronJob) {

		final AbstractProgressTracker tracker = createProgressTracker(cxLocalizedObjectConfigCronJob);

		try {
			// start the HAC progress tracker
			tracker.setProgress(0d);
			if (executeAutoConfiguration(cxLocalizedObjectConfigCronJob, tracker)) {
				return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
			}
			return new PerformResult(CronJobResult.ERROR, CronJobStatus.FINISHED);
		} catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
			LOG.warn("Translation cron job aborted");
			return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.ABORTED);
		} catch (final Exception ex) {
			LOG.error("Error while executing auto configuration", ex);
			return new PerformResult(CronJobResult.FAILURE, CronJobStatus.ABORTED);
		} finally {
			// save the progress for HAC tracker
			tracker.close();
		}
	}

	protected boolean executeAutoConfiguration(final CxLocalizedObjectConfigCronJobModel cronJob, final AbstractProgressTracker tracker) throws InterruptedException {

		int alreadyProcessed = 0;
		int failed = 0;

		List<ComposedTypeModel> composedTypes = cronJob.getIncludedComposedTypes();

		// execute for all types in the system
		if (CollectionUtils.isEmpty(composedTypes)) {
			composedTypes = cxLocalizedValueConfigService.findTypesToConsider();
			cronJob.setIncludedComposedTypes(composedTypes);
			modelService.save(cronJob);
		}

		final int numberOfElements = composedTypes.size();


		for (final ComposedTypeModel composedType : composedTypes) {

			// check if cron job abort request is present
			if (clearAbortRequestedIfNeeded(cronJob)) {
				throw new InterruptedException("Localized Object Configuration job aborted");
			}

			// execute autoconfiguration
			final Set<CxStandardLocalizedObjectConfigModel> configs;
			try {
				configs = this.cxLocalizedValueConfigService.buildConfig(composedType, cronJob);
			} catch (final Exception ex) {
				LOG.error("Failed to create CxStandardLocalizedObjectConfigs for type {}", composedType.getCode(), ex);
				failed++;
				// update the tracking information for HAC
				tracker.setProgress(calculateProgress(numberOfElements, ++alreadyProcessed));
				cronJob.setAlreadyProcessedTypes(alreadyProcessed);
				cronJob.setFailedTypes(failed);
				modelService.save(cronJob);
				continue;
			}

			if (CollectionUtils.isNotEmpty(cronJob.getLocalizedObjectCategories())) {
				configs.forEach(config -> config.setCategories(cronJob.getLocalizedObjectCategories()));
			}

			try {
				modelService.saveAll(configs);
			} catch (final ModelSavingException ex) {
				failed++;
				LOG.error("Failed to save CxStandardLocalizedObjectConfigs for type {}", composedType.getCode(), ex);
			}

			// update the tracking information for HAC
			tracker.setProgress(calculateProgress(numberOfElements, ++alreadyProcessed));

			// update progress data for Backoffice
			cronJob.setAlreadyProcessedTypes(alreadyProcessed);
			cronJob.setFailedTypes(failed);
			modelService.save(cronJob);

		}

		//Configuration for Enumeration
		final ComposedTypeModel enumComposedType = typeService.getComposedTypeForCode(EnumerationValueModel._TYPECODE);
		if (cxLocalizedValueConfigService.findConfigByType(enumComposedType) == null) {
			modelService.save(createEnumConfiguration());
		}
        addConfigForProductFeaturesIfNeeded(cronJob);
		return failed < 1;
	}

    private void addConfigForProductFeaturesIfNeeded(final CxLocalizedObjectConfigCronJobModel cronJob) {
        final List<Object> existingConfigs = flexibleSearchService.search("select {pk} from {%s}".formatted(CxProductFeatureLocalizedObjectConfigModel._TYPECODE)).getResult();
        if (!existingConfigs.isEmpty()) {
            return;
        }
        final List<ClassAttributeAssignmentModel> assignments = flexibleSearchService.<ClassAttributeAssignmentModel>search("select {pk} from {" + ClassAttributeAssignmentModel._TYPECODE + "}").getResult();
        if (assignments.stream().anyMatch(ClassAttributeAssignmentModel::getLocalized)) {
            final CxProductFeatureLocalizedObjectConfigModel config = modelService.create(CxProductFeatureLocalizedObjectConfigModel.class);
            config.setCode("ProductFeature");
            config.setName("Product Features");
            config.setUseInBackofficeSearch(CxTranslateBackofficeSearchInclusion.EXCLUDED_BY_DEFAULT);
            modelService.save(config);
        }
    }

    private CxStandardLocalizedObjectConfigModel createEnumConfiguration() {
		final CxStandardLocalizedObjectConfigModel enumConfig = modelService.create(CxStandardLocalizedObjectConfigModel.class);
		enumConfig.setCode(EnumerationValueModel._TYPECODE);
		final ComposedTypeModel enumComposedType = typeService.getComposedTypeForCode(EnumerationValueModel._TYPECODE);
		enumConfig.setType(enumComposedType);
		enumConfig.setKeyFields(List.of("code"));
		enumConfig.setLocalizedAttributes(Collections.singletonList(typeService.getAttributeDescriptor(enumComposedType, EnumerationValueModel.NAME)));
		return enumConfig;
	}

	private static double calculateProgress(final int totalElements, final int alreadyProcessed) {
		// Calculate the progress percentage, handle division by zero
		return (totalElements == 0) ? 100 : (alreadyProcessed / (double) totalElements) * 100;
	}


	@Override
	public boolean isAbortable() {
		return true;
	}

	protected AbstractProgressTracker createProgressTracker(final CxLocalizedObjectConfigCronJobModel cronJob) {
		return new CronJobProgressTracker(modelService.getSource(cronJob));
	}

	public void setCxLocalizedValueConfigService(final CxLocalizedValueConfigService cxLocalizedValueConfigService) {
		this.cxLocalizedValueConfigService = cxLocalizedValueConfigService;
	}

	public void setTypeService(final TypeService typeService) {
		this.typeService = typeService;
	}
}
