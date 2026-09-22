/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.jobs;

import com.sap.cx.translations.enums.CxApprovalTranslationStatus;
import com.sap.cx.translations.model.CxApproveTranslationsCronJobModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.services.CxApproveTranslationsService;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.jalo.AbstractProgressTracker;
import de.hybris.platform.cronjob.jalo.CronJobProgressTracker;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Approve translations job tracking the progress of its execution
 */
public class CxApproveTranslationsJob extends AbstractJobPerformable<CxApproveTranslationsCronJobModel> {

	private static final Logger LOG = LoggerFactory.getLogger(CxApproveTranslationsJob.class);

	private CxApproveTranslationsService cxApproveTranslationsService;

	@Override
	public PerformResult perform(final CxApproveTranslationsCronJobModel approveTranslationsCronJob) {

		final AbstractProgressTracker tracker = createProgressTracker(approveTranslationsCronJob);

		final Collection<CxProposedTranslationModel> allValues = approveTranslationsCronJob.getTranslationsToApprove();

		// in case of cron job rerun mark the previously failed approvals as pending
		allValues.stream()
				 .filter(value -> CxApprovalTranslationStatus.ERROR == value.getApprovalStatus())
				 .forEach(value -> {
					 value.setApprovalStatus(CxApprovalTranslationStatus.PENDING);
					 modelService.save(value);
				 });

		// in case of cron job rerun only process the items marked as PENDING
		final List<CxProposedTranslationModel> translationsToApprove = allValues.stream()
																				.filter(value -> CxApprovalTranslationStatus.PENDING == value.getApprovalStatus())
																				.toList();

		if (CollectionUtils.isEmpty(translationsToApprove)) {
			tracker.close();
			return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
		}

		try {
			// start the HAC progress tracker
			tracker.setProgress(0d);
			if (executeApproval(approveTranslationsCronJob, translationsToApprove, tracker)) {
				return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
			}
			return new PerformResult(CronJobResult.ERROR, CronJobStatus.FINISHED);
		} catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
			LOG.warn("Translation cron job aborted");
			return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.ABORTED);
		} catch (final Exception ex) {
			LOG.error("Error while executing translation approval", ex);
			return new PerformResult(CronJobResult.FAILURE, CronJobStatus.ABORTED);
		} finally {
			// save the progress for HAC tracker
			tracker.close();
		}
	}

	protected boolean executeApproval(final CxApproveTranslationsCronJobModel cronJob, final List<CxProposedTranslationModel> translationsToApprove, final AbstractProgressTracker tracker) throws InterruptedException {

		boolean successfulExecution = true;


		final int numberOfElements = translationsToApprove.size();
		int alreadyProcessed = 0;

		for (CxProposedTranslationModel translationToApprove : translationsToApprove) {
			// check if cron job abort request is present
			if (clearAbortRequestedIfNeeded(cronJob)) {
				throw new InterruptedException("Approve Translations job aborted");
			}

			// execute approval
			if (!cxApproveTranslationsService.approveTranslation(translationToApprove)) {
				successfulExecution = false;
			}

			// update the tracking information for HAC
			tracker.setProgress(calculateProgress(numberOfElements, ++alreadyProcessed));
		}
		return successfulExecution;
	}

	private static double calculateProgress(int totalElements, int alreadyProcessed) {
		// Calculate the progress percentage, handle division by zero
		return (totalElements == 0) ? 100 : (alreadyProcessed / (double) totalElements) * 100;
	}


	@Override
	public boolean isAbortable() {
		return true;
	}

	protected AbstractProgressTracker createProgressTracker(final CxApproveTranslationsCronJobModel cronJob) {
		return new CronJobProgressTracker(modelService.getSource(cronJob));
	}

	public void setCxApproveTranslationsService(final CxApproveTranslationsService cxApproveTranslationsService) {
		this.cxApproveTranslationsService = cxApproveTranslationsService;
	}
}
