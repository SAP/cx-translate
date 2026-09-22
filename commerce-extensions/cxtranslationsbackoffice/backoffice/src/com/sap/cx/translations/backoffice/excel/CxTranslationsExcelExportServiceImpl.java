/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.excel;

import com.hybris.backoffice.excel.data.SelectedAttribute;
import com.hybris.backoffice.excel.exporting.DefaultExcelExportService;
import com.hybris.backoffice.excel.template.ExcelTemplateConstants;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import de.hybris.platform.core.model.ItemModel;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Customizing the {@link DefaultExcelExportService} to handle the virtual {@link CxLocalizedValueModel}s.
 */
public class CxTranslationsExcelExportServiceImpl extends DefaultExcelExportService {

	private static final Logger LOG = LoggerFactory.getLogger(CxTranslationsExcelExportServiceImpl.class);
	private static final int CELL_INDEX_PK = 0;
	private static final int CELL_INDEX_SHEET_NAME = 1;
	private static final int CELL_INDEX_ROW_NUM = 2;

	@Override
	protected List<ItemModel> refreshSelectedItems(final List<ItemModel> selectedItems) {
		// skip refreshing the virtual CxLocalizedValueModels
		try {
			selectedItems.stream()
						 .filter(item -> !(item instanceof CxLocalizedValueModel))
						 .forEach(item -> getModelService().refresh(item));
		} catch (RuntimeException ex) {
			LOG.debug("Cannot refresh items", ex);
		}
		return selectedItems;
	}

	/**
	 * Overwritten to execute a custom insertPkRow method.
	 *
	 * @param itemsByType
	 * @param type
	 * @param selectedAttributes
	 * @param sheet
	 */
	@Override
	protected void addValues(final Map<String, Set<ItemModel>> itemsByType, final String type, final Set<SelectedAttribute> selectedAttributes, final Sheet sheet) {
		final Sheet pkSheet = this.getExcelSheetService()
								  .createOrGetUtilitySheet(sheet.getWorkbook(), ExcelTemplateConstants.UtilitySheet.PK.getSheetName());
		itemsByType.get(type)
				   .forEach((item) -> {
					   Row row = this.getExcelSheetService()
									 .createEmptyRow(sheet);
					   AtomicInteger cellIndex = new AtomicInteger(CELL_INDEX_PK);
					   // Custom code start
					   this.customInsertPkRow(pkSheet, row, item);
					   // Custom code end

					   selectedAttributes.forEach(selectedAttribute -> this.getExcelTranslatorRegistry()
																		   .getTranslator(selectedAttribute.getAttributeDescriptor(), item)
																		   .ifPresent((translator) -> {
																			   translator.exportData(selectedAttribute.getAttributeDescriptor(), this.getItemAttribute(item, selectedAttribute))
																						 .ifPresent((excelValue) -> {
																							 this.getExcelCellService()
																								 .insertAttributeValue(row.createCell(cellIndex.get()), excelValue);
																						 });
																			   cellIndex.incrementAndGet();
																		   }));


				   });
	}

	/**
	 * Overwrite the OOTB private method InsertPkRow to skip adding a PK column for the {@link CxLocalizedValueModel} which is a virtual item not to be stored in DB
	 *
	 * @param pkSheet
	 * @param row
	 * @param item
	 */
	private void customInsertPkRow(final Sheet pkSheet, final Row row, final ItemModel item) {

		// Custom code start
		if (item instanceof CxLocalizedValueModel) {
			return;
		}
		// Custom code end

		Row emptyRow = this.getExcelSheetService()
						   .createEmptyRow(pkSheet);
		this.getExcelCellService()
			.insertAttributeValue(emptyRow.createCell(CELL_INDEX_PK), item.getPk()
															  .getLongValue());
		this.getExcelCellService()
			.insertAttributeValue(emptyRow.createCell(CELL_INDEX_SHEET_NAME), row.getSheet()
															 .getSheetName());
		this.getExcelCellService()
			.insertAttributeValue(emptyRow.createCell(CELL_INDEX_ROW_NUM), row.getRowNum());

	}
}
