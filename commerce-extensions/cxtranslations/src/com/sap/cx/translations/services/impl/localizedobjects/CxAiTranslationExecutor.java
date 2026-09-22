/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.enums.CxApprovalTranslationStatus;
import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.*;
import com.sap.cx.translations.services.CxLocalizedLabelsPluralsService;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import com.sap.cx.translations.services.impl.automatictranslation.CxAiTranslationStrategy;
import com.sap.cx.translations.services.localizedobjects.parameters.CxPerformTranslationParameters;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Executes AI translation for a batch of {@link CxProposedTranslationModel} values.
 *
 * <p>Extracted from {@code CxLocalizedValueServiceImpl} (improvement #1) to isolate the
 * AI-translation lifecycle: blank-source filtering, target-language filtering, NOTHING_TO_TRANSLATE
 * bookkeeping, and strategy dispatch.
 */
public class CxAiTranslationExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CxAiTranslationExecutor.class);

    private List<CxAiTranslationStrategy> aiTranslationStrategies;
    private ModelService modelService;
    private CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService;
    private CxLocalizedLabelsPluralsService pluralsService;
    private boolean removeNoTargetLanguages;
    private boolean removeMissingSourceValue;

    public void aiTranslate(final CxPerformTranslationParameters parameters) {
        final List<CxProposedTranslationModel> values = parameters.getValues();
        final String userHint = parameters.getUserHint();
        final CxTranslationServiceSettingsModel serviceSettings = parameters.getServiceSettings();
        final boolean skipTranslated = parameters.getSkipTranslated();
        final boolean automaticPlurals = parameters.isAutomaticPlurals();

        if (values.isEmpty()) {
            return;
        }

        final Locale souceLocale = cxTranslateCommerceCommonI18NService.getLocaleForLanguage(parameters.getSourceLanguage());

        // 1. blank-source check — mark values whose source text is blank and remove them upfront
        values.stream()
                .filter(val -> isBlankSourceText(val, parameters, souceLocale))
                .forEach(val -> processBlankSourceText(val, souceLocale.getLanguage()));

        // 2. Partition surviving values into toSkip / toTranslate
        final List<CxProposedTranslationModel> toSkip = new ArrayList<>();
        final List<CxProposedTranslationModel> toTranslate = new ArrayList<>();

        for (final CxProposedTranslationModel value : values) {
            if (modelService.isRemoved(value)) {
                continue;
            }
            final Collection<LanguageModel> filteredTargetLanguages = getFilteredTargetLanguages(
                    value, parameters.getTargetLanguages(), skipTranslated, automaticPlurals);
            value.setFilteredTargetLanguages(filteredTargetLanguages);

            if (CollectionUtils.isEmpty(filteredTargetLanguages)) {
                toSkip.add(value);
            } else {
                toTranslate.add(value);
            }
        }

        // 3. Apply save/remove for values with no remaining target languages
        for (final CxProposedTranslationModel value : toSkip) {
            value.setTranslationStatus(CxTranslationStatus.NOTHING_TO_TRANSLATE);
            value.setApprovalStatus(CxApprovalTranslationStatus.NOTHING_TO_APPROVE);
            modelService.save(value);
            if (removeNoTargetLanguages) {
                modelService.remove(value);
            }
        }

        // 4. Delegate to the concrete strategy
        if (toTranslate.isEmpty()) {
            return;
        }
        aiTranslationStrategies.stream()
                .filter(s -> s.supports(serviceSettings))
                .findFirst()
                .ifPresentOrElse(
                        s -> s.aiTranslate(toTranslate,
                                buildGlobalHint(serviceSettings, userHint),
                                serviceSettings, parameters.getAfterTranslationCallback()),
                        () -> {
                            throw new IllegalArgumentException(
                                    "No AI translation strategy for " + serviceSettings.getClass());
                        });
    }

    private boolean isBlankSourceText(final CxProposedTranslationModel val,
                                      final CxPerformTranslationParameters parameters,
                                      final Locale souceLocale) {
        final boolean isValueBlank = StringUtils.isBlank(val.getValue(souceLocale));
        if (!isValueBlank) {
            return false;
        }
        if (parameters.isAutomaticPlurals()
                && val.getTarget() instanceof final CxLocalizedLabelModel label
                && pluralsService.shouldHavePluralForms(label)) {
            return label.getRelatedLabels().stream().allMatch(relatedLabel -> StringUtils.isBlank(relatedLabel.getValue(souceLocale)));
        }
        return true;
    }

    protected Collection<LanguageModel> getFilteredTargetLanguages(
            final CxProposedTranslationModel value,
            final Collection<LanguageModel> targets,
            final boolean skipTranslated, final Boolean automaticPlurals) {
        return targets.stream()
                .filter(lang -> !skipTranslated
                        || StringUtils.isBlank(value.getValue(cxTranslateCommerceCommonI18NService.getLocale(lang.getIsocode()))))
                .filter(lang -> !automaticPlurals
                        || !isNotNeededPluralForm(value, lang))
                .toList();
    }

    private boolean isNotNeededPluralForm(final CxProposedTranslationModel value, final LanguageModel lang) {
        if (!(value.getTarget() instanceof final CxLocalizedLabelModel label)) {
            return false;
        }
        if (!pluralsService.shouldHavePluralForms(label)) {
            return false;
        }
        return !pluralsService.isRequiredPluralForm(label, lang);
    }

    private void processBlankSourceText(final CxProposedTranslationModel value, final String sourceIso) {
        final String note = "Missing source value for [" + value.getKey() +
                "] for [" + value.getAttribute() +
                "] for [" + sourceIso + "].";
        value.setNotes(value.getNotes() == null ? note : value.getNotes() + '\n' + note);
        value.setTranslationStatus(CxTranslationStatus.MISSING_SOURCE_VALUE);
        value.setApprovalStatus(CxApprovalTranslationStatus.NOTHING_TO_APPROVE);
        if (removeMissingSourceValue) {
            modelService.remove(value);
        } else {
            modelService.save(value);
        }
    }

    public static String buildGlobalHint(final CxTranslationServiceSettingsModel config, final String userHint) {
        return Stream.of(config.getGlobalTranslationHints(), userHint)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(";"));
    }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    public void setAiTranslationStrategies(final List<CxAiTranslationStrategy> aiTranslationStrategies) {
        this.aiTranslationStrategies = aiTranslationStrategies;
    }

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setCxTranslateCommerceCommonI18NService(final CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService) {
        this.cxTranslateCommerceCommonI18NService = cxTranslateCommerceCommonI18NService;
    }

    public void setPluralsService(final CxLocalizedLabelsPluralsService pluralsService) {
        this.pluralsService = pluralsService;
    }

    public void setRemoveNoTargetLanguages(final boolean removeNoTargetLanguages) {
        this.removeNoTargetLanguages = removeNoTargetLanguages;
    }

    public void setRemoveMissingSourceValue(final boolean removeMissingSourceValue) {
        this.removeMissingSourceValue = removeMissingSourceValue;
    }
}
