/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.strategy;

import com.hybris.backoffice.widgets.processes.renderer.DefaultProcessItemRenderingStrategy;
import com.sap.cx.translations.enums.CxApprovalTranslationStatus;
import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobHistoryModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections4.CollectionUtils;
import org.zkoss.util.resource.Labels;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Strategy responsible for rendering the progress of the {@link CxTranslationCronJobModel} execution in Backoffice
 */
public class CxTranslationCronJobProgressRenderingStrategy extends DefaultProcessItemRenderingStrategy {

	private static final String LABEL_PREPARING_DATA = "cxtranslations.translation.preparing.data";
	private static final String LABEL_PROGRESS = "cxtranslations.translation.progress";
	private static final String LABEL_PROCESSES_FAILED = "cxtranslations.translation.progress.failed";
	private static final String LABEL_PROCESSES_COMPLETED = "cxtranslations.translation.progress.finished";
	private static final String LABEL_PROCESSES_COMPLETED_SUCCESS = "cxtranslations.translation.progress.finished.success";
	private static final String LABEL_PROCESSES_COMPLETED_FAILED = "cxtranslations.translation.progress.finished.error";

	private ModelService modelService;

	@Override
	public boolean canHandle(final CronJobHistoryModel cronJobHistory) {
		return cronJobHistory.getCronJob() instanceof CxTranslationCronJobModel;
	}

	@Override
	public String getTitle(final CronJobHistoryModel cronJobHistory) {
		if (cronJobHistory.getCronJob() instanceof final CxTranslationCronJobModel cronJob) {

			final Collection<ItemModel> items = cronJob.getItems();
            final Collection<CxProposedTranslationModel> valuesToTranslate = cronJob.getValuesToTranslate();

			final int totalElements = valuesToTranslate.size();
			if (totalElements == 0 && CollectionUtils.isEmpty(items)) {
				return Labels.getLabel(LABEL_PROCESSES_COMPLETED);
			}


			// translating items, values to translate not yet created
			if (CollectionUtils.isEmpty(valuesToTranslate) && CollectionUtils.isNotEmpty(items) && CronJobStatus.FINISHED != cronJob.getStatus()) {
				return MessageFormat.format(Labels.getLabel(LABEL_PREPARING_DATA), items.size());
			}

			// Count the elements that are left to process (with status PENDING)
			final long elementsLeftToProcess = valuesToTranslate.stream()
																.filter(value -> cronJob.getSaveForApproval() ? CxTranslationStatus.PENDING == getTranslationStatus(value) : CxApprovalTranslationStatus.PENDING == getApprovalStatus(value))
																.count();

			// Count the elements for which translation failed (with status ERROR)
			final long failedElements = valuesToTranslate.stream()
														 .filter(value -> CxTranslationStatus.ERROR == getTranslationStatus(value) || CxApprovalTranslationStatus.ERROR == getApprovalStatus(value))
														 .count();

			// Calculate the number of elements that have been processed
			final long elementsProcessed = totalElements - elementsLeftToProcess;

			// Calculate the progress percentage, handle division by zero
			final double progressPercentage = (totalElements == 0) ? 100d : ((double) elementsProcessed / totalElements) * 100d;

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

	private CxTranslationStatus getTranslationStatus(final CxProposedTranslationModel value) {
		if (!modelService.isRemoved(value)) {
			return value.getTranslationStatus();
		}
		return CxTranslationStatus.SUCCESS;
	}

	private CxApprovalTranslationStatus getApprovalStatus(final CxProposedTranslationModel value) {
		if (!modelService.isRemoved(value)) {
			return value.getApprovalStatus();
		}
		return CxApprovalTranslationStatus.SUCCESS;
	}


	@Override
	public boolean isProgressSupported(final CronJobHistoryModel cronJobHistory) {
		return cronJobHistory.getCronJob() instanceof CxTranslationCronJobModel;
	}

		public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}
}
