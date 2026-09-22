/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.strategy;

import com.hybris.backoffice.widgets.processes.renderer.DefaultProcessItemRenderingStrategy;
import com.sap.cx.translations.model.CxLocalizedObjectConfigCronJobModel;
import de.hybris.platform.cronjob.model.CronJobHistoryModel;
import org.zkoss.util.resource.Labels;

import java.text.MessageFormat;

/**
 * Strategy responsible for rendering the progress of the {@link CxLocalizedObjectConfigCronJobModel} execution in Backoffice
 */
public class CxLocalizedObjectConfigCronJobProgressRenderingStrategy extends DefaultProcessItemRenderingStrategy {

	private static final String LABEL_PROGRESS = "cxtranslations.autoconfigure.progress";
	private static final String LABEL_PROCESSES_FAILED = "cxtranslations.autoconfigure.progress.failed";
	private static final String LABEL_PROCESSES_CALCULATING = "cxtranslations.autoconfigure.progress.calculating";


	@Override
	public boolean canHandle(CronJobHistoryModel cronJobHistory) {
		return cronJobHistory.getCronJob() instanceof CxLocalizedObjectConfigCronJobModel;
	}

	@Override
	public String getTitle(CronJobHistoryModel cronJobHistory) {
		if (cronJobHistory.getCronJob() instanceof final CxLocalizedObjectConfigCronJobModel cronJob) {

			final Integer totalRows = cronJob.getIncludedComposedTypes().size();
			final Integer failed = cronJob.getFailedTypes();
			final Integer alreadyProcessed = cronJob.getAlreadyProcessedTypes();

			if (totalRows == null || totalRows < 1) {
				return Labels.getLabel(LABEL_PROCESSES_CALCULATING);
			}

			// Counters can be null on first run before the job has incremented them — coerce to 0 for display.
			final int processedSafe = alreadyProcessed != null ? alreadyProcessed : 0;
			final int failedSafe = failed != null ? failed : 0;

			// Calculate the progress percentage, handle division by zero
			final double progressPercentage = ((double) processedSafe / totalRows) * 100d;

			if (failedSafe > 0) {
				return MessageFormat.format(Labels.getLabel(LABEL_PROCESSES_FAILED), (int) progressPercentage, processedSafe, totalRows, failedSafe);
			}
			return MessageFormat.format(Labels.getLabel(LABEL_PROGRESS), (int) progressPercentage, processedSafe, totalRows, failedSafe);

		} else {
			return super.getTitle(cronJobHistory);
		}
	}

	@Override
	public boolean isProgressSupported(final CronJobHistoryModel cronJobHistory) {
		return cronJobHistory.getCronJob() instanceof CxLocalizedObjectConfigCronJobModel;
	}

}
