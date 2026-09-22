/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.cms;

import com.sap.cx.translations.cms.data.CxCmsApproveRejectRequest;
import com.sap.cx.translations.cms.data.CxCmsApproveRejectResult;
import com.sap.cx.translations.cms.data.CxCmsTranslateRequest;
import com.sap.cx.translations.cms.data.CxCmsTranslationJob;
import com.sap.cx.translations.cms.data.CxCmsTranslationTreeResponse;

/**
 * Facade exposing the recursive CMS translation tree ({@link CxTranslateCmsTree}) as OCC friendly DTOs.
 */
public interface CxTranslateCmsFacade {

	/**
	 * Builds the recursive translation tree for the CMS item identified by the given {@code uid} within the
	 * catalog version identified by {@code catalogId} / {@code catalogVersion}.
	 *
	 * @param uid             the uid of the root CMS item
	 * @param catalogId       the id of the catalog
	 * @param catalogVersion  the version of the catalog (e.g. {@code Online} / {@code Staged})
	 * @param includeTemplate whether to include template content slots in the tree
	 * @return the populated tree wrapped in a {@link CxCmsTranslationTreeResponse} that also carries metadata
	 *         (available languages with display names, available service settings)
	 */
	CxCmsTranslationTreeResponse getTranslationTree(String uid, String catalogId, String catalogVersion, boolean includeTemplate);

	/**
	 * Starts an AI translation cron job (in the background) for the localized values referenced by the
	 * request.
	 *
	 * @param request the translation request (value keys, source/target languages, options)
	 * @return information about the started cron job
	 */
	CxCmsTranslationJob startTranslation(CxCmsTranslateRequest request);

	/**
	 * Approves the proposed translations identified by their PKs (synchronously), storing the
	 * translated values directly on the target items.
	 *
	 * @param request the approval request (PKs of the proposed translations)
	 * @return a summary of the operation (requested / succeeded / failed counts)
	 */
	CxCmsApproveRejectResult approveTranslations(CxCmsApproveRejectRequest request);

	/**
	 * Rejects the proposed translations identified by their PKs by removing the proposal items.
	 *
	 * @param request the rejection request (PKs of the proposed translations)
	 * @return a summary of the operation (requested / succeeded / failed counts)
	 */
	CxCmsApproveRejectResult rejectTranslations(CxCmsApproveRejectRequest request);
}

