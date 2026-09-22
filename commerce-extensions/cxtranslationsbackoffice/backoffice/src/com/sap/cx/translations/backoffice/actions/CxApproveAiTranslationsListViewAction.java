/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacadeOperationResult;
import com.hybris.cockpitng.search.data.pageable.Pageable;
import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import de.hybris.platform.core.model.ItemModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Gets the selected items or if nothing is selected the full pageable list and sends it on itemsToApprove output for further processing
 */
public class CxApproveAiTranslationsListViewAction extends CxAbstractSelectedOrPageableListViewAction {

    protected static final String SOCKET_OUT_ITEMS_TO_APPROVE = "itemsToApprove";
    private static final Logger LOG = LoggerFactory.getLogger(CxApproveAiTranslationsListViewAction.class);

    @Resource
    protected ObjectFacade objectFacade;

    public ActionResult<Pageable<? extends ItemModel>> perform(final ActionContext<String> ctx) {
        ActionResult<Pageable<? extends ItemModel>> result = new ActionResult("error");
        Optional<Pageable<ItemModel>> data = this.getData(ctx);
        if (!data.isPresent()) {
            return result;
        }
        Pageable<ItemModel> dataToApprove = data.get();
        // filter out the removed items - for paralel executions
        final List<ItemModel> filteredItems = dataToApprove.getAllResults()
                .stream()
                .filter(item -> !objectFacade.isDeleted(item))
                .collect(Collectors.toList());
        // save all the items before starting the execution
        ObjectFacadeOperationResult<ItemModel> saveResult = objectFacade.save(filteredItems);
        if (saveResult.hasError()) {
            LOG.error("Failed to save {} items", saveResult.countFailureObjects());
        }

        // retrieve items eligible for approval - only the successfully translated by AI or manually modified
        final List<CxProposedTranslationModel> itemsToApprove = filteredItems
                .stream()
                .filter(item -> item instanceof CxProposedTranslationModel)
                .map(item -> (CxProposedTranslationModel) item)
                .filter(value -> value.getTarget() != null && (CxTranslationStatus.SUCCESS == value.getTranslationStatus() || CxTranslationStatus.MANUAL == value.getTranslationStatus()))
                .collect(Collectors.toList());
        int totalCount = itemsToApprove.size();
        if (this.isMaxRowsExceeded(ctx, totalCount)) {
            this.showMaxRowsExceeded(ctx, totalCount);
        } else {
            this.sendOutput(SOCKET_OUT_ITEMS_TO_APPROVE, itemsToApprove);
            result.setResultCode("success");
            result.setData(dataToApprove);
        }

        return result;
    }

    @Override
    public boolean canPerform(final ActionContext<String> ctx) {
        String typeCode = StringUtils.isNotBlank(ctx.getData()) ? ctx.getData() : this.getSelectedItemsType(ctx)
                .orElse("");
        return StringUtils.isNotEmpty(typeCode) && getTypeService().isAssignableFrom(CxProposedTranslationModel._TYPECODE, typeCode) && getPermissionFacade().canChangeType(typeCode);
    }

    @Override
    public String getThresholdValueParameterKey() {
        return "safety.threshold";
    }

    @Override
    public String getMaxRowsParameterKey() {
        return "max.rows";
    }

    @Override
    public String getConfirmationMessageKey() {
        return "cxtranslations.translationapproval.confirmation";
    }

    @Override
    public String getMaxRowsExceededMessageKey() {
        return "cxtranslations.translationapproval.max.rows.exceeded.msg";
    }

    @Override
    public String getMaxRowsExceededMessageTitleKey() {
        return "cxtranslations.translationapproval.max.rows.exceeded.title";
    }
}
