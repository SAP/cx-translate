/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.excel;


import com.hybris.backoffice.excel.imp.wizard.ExcelImportHandler;
import com.hybris.backoffice.excel.jobs.FileContent;
import com.hybris.backoffice.widgets.processes.ProcessesController;
import com.hybris.cockpitng.core.events.impl.DefaultCockpitEvent;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.sap.cx.translations.backoffice.model.CxLocalizedValueExcelImportCronJobModel;
import com.sap.cx.translations.backoffice.model.CxLocalizedValueExcelImportJobModel;
import de.hybris.platform.catalog.model.CatalogUnawareMediaModel;
import de.hybris.platform.core.model.media.MediaFolderModel;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.cronjob.model.JobModel;
import de.hybris.platform.servicelayer.cluster.ClusterService;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.media.MediaService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Custom hadler for Excel import. Starts a {@link CxLocalizedValueExcelImportCronJobModel} in the background.
 */
public class CxTranslationsExcelImportHandler extends ExcelImportHandler {
	private static final Logger LOG = LoggerFactory.getLogger(CxTranslationsExcelImportHandler.class);
	public static final String EXCEL_IMPORT_CRON_JOB_CODE_PREFIX = "CxTranslationsExcelImport";
	public static final String SUCCESSFULL_EXECUTION = "cxtranslations.backoffice.notification.excelImportProcessStarted";
	public static final String NOTIFICATION_SOURCE = "cxtranslations";
	// Use the model's _TYPECODE (PascalCase) as the job code so the same string is used for both
	// the create (setCode) and the lookup (getJob) — previously the camelCase lookup never matched
	// the PascalCase persisted code, causing a duplicate job to be created on every import.
	private static final String JOB_CODE = CxLocalizedValueExcelImportJobModel._TYPECODE;

	private String mediaFolder;
	private MediaService mediaService;
	private ModelService modelService;

	private ClusterService clusterService;
	private UserService userService;

	private String cronJobNodeGroup;
	private int cronJobPriority;

	@Override
	protected void importExcel(final FileContent excelFile, final FileContent foundZipFile) {

		final CxLocalizedValueExcelImportCronJobModel excelImportCronJob = executeExcelImportCronJob(excelFile);

		getNotificationService().notifyUser(NOTIFICATION_SOURCE, SUCCESSFULL_EXECUTION, NotificationEvent.Level.SUCCESS);

		this.getCockpitEventQueue()
			.publishEvent(new DefaultCockpitEvent(ProcessesController.GLOBAL_EVENT_UPDATE_PROCESS_FOR_CRON_JOB, excelImportCronJob.getCode(), null));

	}

	/**
	 * Copied from DefaultExcelCronJobService
	 * Create Media for the given content
	 *
	 * @param data
	 * @param fileName
	 * @param contentType
	 * @return MediaModel
	 */
	protected MediaModel createMedia(byte[] data, String fileName, String contentType) {
		final CatalogUnawareMediaModel mediaModel = modelService.create(CatalogUnawareMediaModel._TYPECODE);
		mediaModel.setCode(this.generateId(fileName));
		final MediaFolderModel importFolder = mediaService.getFolder(this.mediaFolder);
		mediaModel.setFolder(importFolder);
		mediaModel.setRealFileName(fileName);
		mediaModel.setMime(contentType);
		modelService.save(mediaModel);
		mediaService.setDataForMedia(mediaModel, data);
		return mediaModel;
	}


	/**
	 * Create a {@link CxLocalizedValueExcelImportCronJobModel} responsible for executing the excel import in the background in asynchronous way.
	 *
	 * @param excelFile
	 * @return
	 */
	protected CxLocalizedValueExcelImportCronJobModel executeExcelImportCronJob(final FileContent excelFile) {

		final MediaModel excelMedia = createMedia(excelFile.getData(), excelFile.getName(), excelFile.getContentType());

		final CxLocalizedValueExcelImportCronJobModel cronJob = modelService.create(CxLocalizedValueExcelImportCronJobModel.class);
		final UserModel user = userService.getCurrentUser();

		cronJob.setActive(true);
		cronJob.setJob(getOrCreateCxLocalizedValueExcelImportJob());
		cronJob.setSessionLanguage(user.getSessionLanguage());
		cronJob.setSessionUser(user);
		cronJob.setRemoveOnExit(false);
		cronJob.setExcelFile(excelMedia);

		if (clusterService.isClusteringEnabled() && clusterService.getClusterGroups().contains(cronJobNodeGroup)) {
			cronJob.setNodeGroup(cronJobNodeGroup);
		}

		if (cronJobPriority > 0) {
			cronJob.setPriority(cronJobPriority);
		}

		modelService.save(cronJob);

		// execute cron job in asynchronous way
		getCronJobService().performCronJob(cronJob, false);

		return cronJob;
	}


	/**
	 * Get or create a {@link CxLocalizedValueExcelImportJobModel} instance. There should only be one present.
	 * Protected against race conditions: if two threads concurrently try to create the job, the one
	 * that loses the unique-code constraint race catches {@link ModelSavingException} and refetches
	 * the already-persisted instance.
	 *
	 * @return the singleton import job model
	 */
	protected JobModel getOrCreateCxLocalizedValueExcelImportJob() {
		JobModel cxLocalizedValueExcelImportJob;
		try {
			cxLocalizedValueExcelImportJob = getCronJobService().getJob(JOB_CODE);
		} catch (final UnknownIdentifierException e) {
			final CxLocalizedValueExcelImportJobModel newJob = modelService.create(CxLocalizedValueExcelImportJobModel.class);
			newJob.setCode(JOB_CODE);
			newJob.setActive(true);
			try {
				modelService.save(newJob);
				return newJob;
			} catch (final ModelSavingException savingException) {
				LOG.warn("Race condition detected while creating {} – refetching existing instance.", JOB_CODE, savingException);
				try {
					cxLocalizedValueExcelImportJob = getCronJobService().getJob(JOB_CODE);
				} catch (final UnknownIdentifierException refetchEx) {
					throw new IllegalStateException("Failed to persist " + JOB_CODE, savingException);
				}
			}
		}

		cxLocalizedValueExcelImportJob.setActive(true);
		return cxLocalizedValueExcelImportJob;
	}

	/**
	 * Copied from DefaultExcelCronJobService
	 * Generate unique ID for the file
	 *
	 * @param fileName
	 * @return
	 */
	protected String generateId(final String fileName) {
		return String.format("%s%s%s", EXCEL_IMPORT_CRON_JOB_CODE_PREFIX, fileName.replaceAll("[^0-9a-zA-Z]", ""), UUID.randomUUID()
																													   .getMostSignificantBits());
	}

	public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}

	public void setMediaService(final MediaService mediaService) {
		this.mediaService = mediaService;
	}

	public void setMediaFolder(final String mediaFolder) {
		this.mediaFolder = mediaFolder;
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

}
