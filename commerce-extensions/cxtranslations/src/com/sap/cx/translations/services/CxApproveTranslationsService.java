/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.services;

import com.sap.cx.translations.model.CxApproveTranslationsCronJobModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;

import java.util.List;

/**
 * Translation approval service
 */
public interface CxApproveTranslationsService {

	/**
	 * Approve the given translation and store the value directly on the target item.
	 *
	 * @param translationToApprove translation to approve
	 * @return true if approval was successfully
	 */
	boolean approveTranslation(final CxProposedTranslationModel translationToApprove);

	/**
	 * Approve given translations in asynchronous mode and store the values directly on the target item.
	 *
	 * @param translationsToApprove translations to approve
	 * @return the cron job running translation approval in asynchronous mode
	 */
	CxApproveTranslationsCronJobModel approveTranslations(List<CxProposedTranslationModel> translationsToApprove);
}
