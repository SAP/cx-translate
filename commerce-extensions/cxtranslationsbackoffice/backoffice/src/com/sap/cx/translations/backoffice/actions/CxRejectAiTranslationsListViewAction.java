/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.backoffice.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.search.data.pageable.Pageable;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import de.hybris.platform.core.model.ItemModel;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Gets the selected items or if nothing is selected the full pageable list and sends it on itemsToReject output for further processing (removal).
 */
public class CxRejectAiTranslationsListViewAction extends CxAbstractSelectedOrPageableListViewAction {

    protected static final String SOCKET_OUT_ITEMS_TO_REJECT = "itemsToReject";
    private static final Logger LOG = LoggerFactory.getLogger(CxRejectAiTranslationsListViewAction.class);

    @Resource
    protected ObjectFacade objectFacade;

    @Override
    public ActionResult<Pageable<? extends ItemModel>> perform(final ActionContext<String> ctx) {
        final ActionResult<Pageable<? extends ItemModel>> result = new ActionResult("error");
        final Optional<Pageable<ItemModel>> data = this.getData(ctx);
        if (!data.isPresent()) {
            return result;
        }
        final Pageable<ItemModel> dataToReject = data.get();
        // filter out the already removed items - for parallel executions
        final List<CxProposedTranslationModel> itemsToReject = dataToReject.getAllResults()
                .stream()
                .filter(item -> !objectFacade.isDeleted(item))
                .filter(item -> item instanceof CxProposedTranslationModel)
                .map(item -> (CxProposedTranslationModel) item)
                .collect(Collectors.toList());

        final int totalCount = itemsToReject.size();
        if (this.isMaxRowsExceeded(ctx, totalCount)) {
            this.showMaxRowsExceeded(ctx, totalCount);
        } else {
            this.sendOutput(SOCKET_OUT_ITEMS_TO_REJECT, itemsToReject);
            result.setResultCode("success");
            result.setData(dataToReject);
        }

        return result;
    }

    @Override
    public boolean canPerform(final ActionContext<String> ctx) {
        final String typeCode = StringUtils.isNotBlank(ctx.getData()) ? ctx.getData() : this.getSelectedItemsType(ctx)
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
        return "cxtranslations.translationrejection.confirmation";
    }

    @Override
    public String getMaxRowsExceededMessageKey() {
        return "cxtranslations.translationrejection.max.rows.exceeded.msg";
    }

    @Override
    public String getMaxRowsExceededMessageTitleKey() {
        return "cxtranslations.translationrejection.max.rows.exceeded.title";
    }
}

