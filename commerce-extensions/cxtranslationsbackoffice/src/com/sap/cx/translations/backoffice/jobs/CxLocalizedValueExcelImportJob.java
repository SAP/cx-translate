/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.jobs;

import com.google.common.collect.Table;
import com.hybris.backoffice.excel.data.Impex;
import com.hybris.backoffice.excel.data.ImpexForType;
import com.hybris.backoffice.excel.data.ImpexHeaderValue;
import com.hybris.backoffice.excel.importing.ExcelImportService;
import com.hybris.backoffice.excel.template.workbook.ExcelWorkbookService;
import com.sap.cx.translations.backoffice.model.CxLocalizedValueExcelImportCronJobModel;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.jalo.AbstractProgressTracker;
import de.hybris.platform.cronjob.jalo.CronJobProgressTracker;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.media.MediaService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Job for handling `Excel` import of {@link CxLocalizedValueModel}
 */
public class CxLocalizedValueExcelImportJob extends AbstractJobPerformable<CxLocalizedValueExcelImportCronJobModel> {

	private static final Logger LOG = LoggerFactory.getLogger(CxLocalizedValueExcelImportJob.class);
	private static final String KEY_COLUMN = "key";
	private static final String VALUE_COLUMN = "value";

	private ExcelWorkbookService excelWorkbookService;
	private MediaService mediaService;
	private ExcelImportService excelImportService;
	private CxLocalizedValueService cxLocalizedValueService;
	private CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService;

	@Override
	public PerformResult perform(final CxLocalizedValueExcelImportCronJobModel cronJob) {

		final AbstractProgressTracker tracker = createProgressTracker(cronJob);

		try {
			// start the HAC progress tracker
			tracker.setProgress(0d);
			if (executeImport(cronJob, tracker)) {
				return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
			}
			return new PerformResult(CronJobResult.ERROR, CronJobStatus.FINISHED);
		} catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
			LOG.warn("Excel import job aborted");
			return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.ABORTED);
		} catch (final Exception ex) {
			LOG.error("Error while executing excel import", ex);
			return new PerformResult(CronJobResult.FAILURE, CronJobStatus.ABORTED);
		} finally {
			// save the progress for HAC tracker
			tracker.close();
		}
	}

	/**
	 * Factory method for the progress tracker — extracted so unit tests can supply a mock.
	 */
	protected AbstractProgressTracker createProgressTracker(final CxLocalizedValueExcelImportCronJobModel cronJob) {
		return new CronJobProgressTracker(modelService.getSource(cronJob));
	}

	/**
	 * Execute Excel file import. Only process the tables containing {@link CxLocalizedValueModel} and skip the rest.
	 *
	 * @param cronJob
	 * @param tracker
	 * @return
	 * @throws InterruptedException
	 */
	protected boolean executeImport(final CxLocalizedValueExcelImportCronJobModel cronJob, final AbstractProgressTracker tracker) throws InterruptedException {

		int alreadyProcessedRows = 0;
		int failedRows = 0;

		final MediaModel excelFileMedia = cronJob.getExcelFile();
		if (excelFileMedia == null) {
			LOG.error("Missing excel file!");
			return false;
		}

		final Impex impex;
		try {
			final Workbook workbook = excelWorkbookService.createWorkbook(mediaService.getStreamFromMedia(excelFileMedia));
			impex = excelImportService.convertToImpex(workbook);
		} catch (Exception e) {
			LOG.error("Failed to convert excel file to impex!", e);
			return false;
		}

		final Table<Integer, ImpexHeaderValue, Object> impexTable = impex.getImpexes()
																		 .stream()
																		 .filter(impexForType -> impexForType.getTypeCode()
																											 .equals(CxLocalizedValueModel._TYPECODE))
																		 .map(ImpexForType::getImpexTable)
																		 .findFirst()
																		 .orElse(null);

		if (impexTable == null) {
			LOG.error("Excel does not contain data for the CxLocalizedValue");
			return false;
		}

		// retrieve all relevant columns - key and value
		final Set<ImpexHeaderValue> relevantColumns = retrieveRelevantColumns(impexTable);
		final ImpexHeaderValue keyColumn = findKeyColumn(relevantColumns);
		final Set<ImpexHeaderValue> valueColumns = findValueColumns(relevantColumns);

		if (keyColumn == null) {
			LOG.error("Excel does not contain a [{}] column — cannot match rows to CxLocalizedValue records", KEY_COLUMN);
			return false;
		}

		final int totalRows = impexTable.rowMap().size();

		cronJob.setTotalRows(totalRows);
		modelService.save(cronJob);

		final Collection<Map<ImpexHeaderValue, Object>> rows = impexTable.rowMap().values();

		for (Map<ImpexHeaderValue, Object> row : rows) {

			// check if cron job abort request is present
			if (clearAbortRequestedIfNeeded(cronJob)) {
				throw new InterruptedException("Localized Values Excel Import job aborted");
			}

			final Object keyObject = row.get(keyColumn);
			final Map<Locale, Object> langToValueMap = buildValuesMap(row, valueColumns);

			if (keyObject instanceof String key && StringUtils.isNotBlank(key)) {
				// get CxLocalizedValue for key
				final List<CxLocalizedValueModel> localizedValues = cxLocalizedValueService.getForKey(key);

				if (CollectionUtils.isEmpty(localizedValues)) {
					LOG.error("Cant find CxLocalizedValue for given key [{}]!", key);
					failedRows++;
				} else if (localizedValues.size() > 1) {
					// Multiple CxLocalizedValue records share this key — row skipped.
					LOG.error("More then one CxLocalizedValue found for the key [{}] — row skipped", key);
					failedRows++;
				} else if (!importLocalizedValue(key, localizedValues.get(0), langToValueMap)) {
					failedRows++;
				}
			} else {
				LOG.error("Row skipped — key column value is missing or not a String: [{}]", keyObject);
				failedRows++;
			}

			alreadyProcessedRows++;

			// update tracker progress for HAC
			tracker.setProgress(calculateProgress(totalRows, alreadyProcessedRows));

			// update progress data for Backoffice
			cronJob.setAlreadyProcessedRows(alreadyProcessedRows);
			cronJob.setFailedRows(failedRows);
			modelService.save(cronJob);
		}

		// in case any rows failed return false
		return failedRows == 0;
	}


	protected static double calculateProgress(int totalRows, int alreadyProcessedRows) {
		// Calculate the progress percentage, handle division by zero
		return (totalRows == 0) ? 100 : (alreadyProcessedRows / (double) totalRows) * 100;
	}

	protected Map<Locale, Object> buildValuesMap(final Map<ImpexHeaderValue, Object> row, final Set<ImpexHeaderValue> valueColumns) {
		final Map<Locale, Object> langToValueMap = new HashMap<>();

		valueColumns.forEach(valueColumn -> {
			langToValueMap.put(cxTranslateCommerceCommonI18NService.getLocale(valueColumn.getLang()), row.get(valueColumn));
		});
		return langToValueMap;
	}

	protected Set<ImpexHeaderValue> findValueColumns(final Set<ImpexHeaderValue> relevantColumns) {
		return relevantColumns.stream()
							  .filter(column -> column.getQualifier().equals(VALUE_COLUMN))
							  .collect(Collectors.toSet());
	}

	protected ImpexHeaderValue findKeyColumn(final Set<ImpexHeaderValue> relevantColumns) {
		return relevantColumns.stream()
							  .filter(column -> column.getQualifier().equals(KEY_COLUMN))
							  .findFirst()
							  .orElse(null);
	}

	protected Set<ImpexHeaderValue> retrieveRelevantColumns(final Table<Integer, ImpexHeaderValue, Object> impexTable) {
		return impexTable.columnKeySet()
						 .stream()
						 .filter(impexHeader -> impexHeader.getQualifier() != null)
						 .filter(impexHeader -> impexHeader.getQualifier()
														   .equals(KEY_COLUMN) || impexHeader.getQualifier()
																							 .equals(VALUE_COLUMN))
						 .collect(Collectors.toSet());
	}

	protected boolean importLocalizedValue(final String key, final CxLocalizedValueModel cxLocalizedValue, final Map<Locale, Object> langToValueMap) {

		if (cxLocalizedValue.getTarget() == null) {
			LOG.error("Cant import localizedValue with key [{}] — target item is null", key);
			return false;
		}

		boolean successfullyProcessed = true;

		if (MapUtils.isNotEmpty(langToValueMap)) {
			for (final Map.Entry<Locale, Object> mapEntry : langToValueMap.entrySet()) {
				final Locale locale = mapEntry.getKey();
				final Object value = mapEntry.getValue();

				try {
					cxLocalizedValue.getTarget().setProperty(cxLocalizedValue.getAttribute(), locale, value);
				} catch (Exception ex) {
					LOG.error("Cant set [{}] attribute on the target item [{}] for localizedValue with key [{}]", cxLocalizedValue.getAttribute(), cxLocalizedValue.getTarget(), cxLocalizedValue.getKey());
					successfullyProcessed = false;
				}
			}
		}

		try {
			modelService.save(cxLocalizedValue.getTarget());
		} catch (Exception ex) {
			LOG.error("Cant save localizedValue with key [{}]", cxLocalizedValue.getKey());
			successfullyProcessed = false;
		}

		return successfullyProcessed;
	}


	@Override
	public boolean isAbortable() {
		return true;
	}

	public void setMediaService(final MediaService mediaService) {
		this.mediaService = mediaService;
	}

	public void setExcelWorkbookService(final ExcelWorkbookService excelWorkbookService) {
		this.excelWorkbookService = excelWorkbookService;
	}

	public void setExcelImportService(final ExcelImportService excelImportService) {
		this.excelImportService = excelImportService;
	}

	public void setCxLocalizedValueService(final CxLocalizedValueService cxLocalizedValueService) {
		this.cxLocalizedValueService = cxLocalizedValueService;
	}

	public void setCxTranslateCommerceCommonI18NService(final CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService) {
		this.cxTranslateCommerceCommonI18NService = cxTranslateCommerceCommonI18NService;
	}
}
