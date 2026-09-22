/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services;

import com.sap.cx.translations.model.CxLocalizedLabelModel;
import de.hybris.platform.core.model.c2l.LanguageModel;

import java.util.Collection;
import java.util.List;

public interface CxLocalizedLabelsPluralsService {
    /**
     * Determines whether the provided label should support plural forms.
     *
     * @param localizedLabel the label to evaluate
     * @return true if plural variants are expected for this label
     */
    boolean shouldHavePluralForms(CxLocalizedLabelModel localizedLabel);

    /**
     * Ensures the all required plural forms exist for the given labels and target languages.
     * When {@code createMissing} is true, missing (not existing in the database) plural labels are created; otherwise only existing ones are returned.
     *
     * @param localizedLabels source labels used to derive plural variants
     * @param targetLanguages languages to check against for plural requirements
     * @param createMissing   whether to create and return missing plural labels
     * @return all required plural labels for the given inputs (existing and optionally newly created)
     */
    List<CxLocalizedLabelModel> getRequiredPluralForms(List<CxLocalizedLabelModel> localizedLabels,
                                                       Collection<LanguageModel> targetLanguages, boolean createMissing);


    /**
     * Checks if the given label instance represents a required plural form for the specified language.
     *
     * @param label    the label to inspect
     * @param language the language to validate against
     * @return true if the label is one of the required plural forms for the language
     */
    boolean isRequiredPluralForm(CxLocalizedLabelModel label, LanguageModel language);
}
