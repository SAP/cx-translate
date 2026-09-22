/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.strategy;

import com.hybris.backoffice.widgets.processes.renderer.DefaultProcessItemRenderingStrategy;
import com.sap.cx.translations.enums.CxApprovalTranslationStatus;
import com.sap.cx.translations.model.CxApproveTranslationsCronJobModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import de.hybris.platform.cronjob.model.CronJobHistoryModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.zkoss.util.resource.Labels;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Strategy responsible for rendering the progress of the {@link CxApproveTranslationsCronJobModel} execution in Backoffice
 */
public class CxApproveTranslationCronJobProgressRenderingStrategy extends DefaultProcessItemRenderingStrategy {

	private static final String LABEL_PROGRESS = "cxtranslations.approvetranslations.progress";
	private static final String LABEL_PROCESSES_FAILED = "cxtranslations.approvetranslations.progress.failed";
	private static final String LABEL_PROCESSES_COMPLETED = "cxtranslations.approvetranslations.progress.finished";
	private static final String LABEL_PROCESSES_COMPLETED_SUCCESS = "cxtranslations.approvetranslations.progress.finished.success";
	private static final String LABEL_PROCESSES_COMPLETED_FAILED = "cxtranslations.approvetranslations.progress.finished.error";

	private ModelService modelService;

	@Override
	public boolean canHandle(CronJobHistoryModel cronJobHistory) {
		return cronJobHistory.getCronJob() instanceof CxApproveTranslationsCronJobModel;
	}

	@Override
	public String getTitle(CronJobHistoryModel cronJobHistory) {
		if (cronJobHistory.getCronJob() instanceof CxApproveTranslationsCronJobModel cronJob) {

			final Integer totalElements = cronJob.getElementsToProcessCount();
			if (totalElements == null || totalElements == 0) {
				return Labels.getLabel(LABEL_PROCESSES_COMPLETED);
			}

			final Collection<CxProposedTranslationModel> translationsToApprove = cronJob.getTranslationsToApprove();

			// Count the elements that are left to process (with status PENDING)
			final long elementsLeftToProcess = translationsToApprove.stream()
																	.filter(value -> CxApprovalTranslationStatus.PENDING == getApprovalStatus(value))
																	.count();

			// Count the elements for which translation failed (with status ERROR)
			final long failedElements = translationsToApprove.stream()
															 .filter(value -> CxApprovalTranslationStatus.ERROR == getApprovalStatus(value))
															 .count();

			// Calculate the number of elements that have been processed
			final long elementsProcessed = totalElements - elementsLeftToProcess;

			// Calculate the progress percentage, handle division by zero
			final double progressPercentage = (double) elementsProcessed / totalElements * 100d;

			if (elementsLeftToProcess > 0) {
				if (failedElements > 0) {
					return MessageFormat.format(Labels.getLabel(LABEL_PROCESSES_FAILED), (int) progressPercentage, elementsProcessed, totalElements, failedElements);
				}
				return MessageFormat.format(Labels.getLabel(LABEL_PROGRESS), (int) progressPercentage, elementsProcessed, totalElements, failedElements);
			}

			if (failedElements > 0) {
				return MessageFormat.format(Labels.getLabel(LABEL_PROCESSES_COMPLETED_FAILED), failedElements, totalElements);
			}
			return MessageFormat.format(Labels.getLabel(LABEL_PROCESSES_COMPLETED_SUCCESS), totalElements);

		} else {
			return super.getTitle(cronJobHistory);
		}
	}

	private CxApprovalTranslationStatus getApprovalStatus(final CxProposedTranslationModel value) {
		if (!modelService.isRemoved(value)) {
			return value.getApprovalStatus();
		}
		return CxApprovalTranslationStatus.SUCCESS;
	}

	@Override
	public boolean isProgressSupported(final CronJobHistoryModel cronJobHistory) {
		return cronJobHistory.getCronJob() instanceof CxApproveTranslationsCronJobModel;
	}

		public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}
}
