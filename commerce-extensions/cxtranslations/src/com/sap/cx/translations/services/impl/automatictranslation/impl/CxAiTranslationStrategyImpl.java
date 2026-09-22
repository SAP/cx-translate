/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.automatictranslation.impl;

import com.sap.cx.translations.generated.dto.translation.response.CxTranslationResponseDto;
import com.sap.cx.translations.enums.CxTranslateServiceLoggingLevel;
import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.*;
import com.sap.cx.translations.serviceinterfaces.ai.CxTranslationService;
import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationRequestDto;
import com.sap.cx.translations.serviceinterfaces.ai.dto.CxTranslationsInputTextDto;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import com.sap.cx.translations.services.impl.automatictranslation.CxAiTranslationStrategy;
import com.sap.cx.translations.util.CxTranslateListSplitUtil;
import de.hybris.platform.core.model.c2l.C2LItemModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Strategy for {@link CxAiTranslationServiceSettingsModel}.
 * Contains the full “single‑call” and “chunked” translation logic
 * migrated from the original service class.
 *
 * <p><strong>PRE‑CONDITIONS</strong> (guaranteed by the service layer):<br>
 * – {@code sourceText} is NOT blank<br>
 * – {@code filteredTargetLanguages} is NOT empty</p>
 */
public class CxAiTranslationStrategyImpl implements CxAiTranslationStrategy {

    private static final int DEFAULT_MAX_CHARS_IN_REQUEST = 1000;

    private record ValueWithHintOverride(CxProposedTranslationModel value, String hint) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(CxAiTranslationStrategyImpl.class);

    private CxTranslationService aiTranslationService;
    private ModelService modelService;
    private CxTranslateCommerceCommonI18NService i18nService;


    @Override
    public boolean supports(final CxTranslationServiceSettingsModel settings) {
        return settings instanceof CxAiTranslationServiceSettingsModel;
    }

    @Override
    public void aiTranslate(final List<CxProposedTranslationModel> values,
                            final String globalHint,
                            final CxTranslationServiceSettingsModel settings, final Consumer<CxProposedTranslationModel> afterTranslation) {
        if (values.isEmpty()) {
            return;
        }
        if (values.stream().map(CxProposedTranslationModel::getSourceLanguage).distinct().count() != 1) {
            throw new IllegalArgumentException("All proposed translations in a batch must have the same source language");
        }
        final LanguageModel sourceLanguage = values.getFirst().getSourceLanguage();
        final CxAiTranslationServiceSettingsModel cfg = (CxAiTranslationServiceSettingsModel) settings;
        final String sourceIso = sourceLanguage.getIsocode();
        final Locale sourceLocale = i18nService.getLocale(sourceIso);

        //one reason for sorting by key is to keep plural forms of same key together.
        final List<CxProposedTranslationModel> sortedByKey = values.stream()
                .sorted(Comparator.comparing(CxAbstractLocalizedValueModel::getKey))
                .toList();

        final int maxCharactersInRequest = cfg.getMaxCharactersInRequest() != null ? cfg.getMaxCharactersInRequest() : DEFAULT_MAX_CHARS_IN_REQUEST;

        //we are splitting into batches, but we want to keep plural forms of the same key in the same batch.
        final List<List<CxProposedTranslationModel>> translationBatches = CxTranslateListSplitUtil.splitWithBoundaryConstraint(sortedByKey, cxProposedTranslationModels -> shouldBatchBeSplit(cxProposedTranslationModels, maxCharactersInRequest, sourceLocale),
                this::belongToSamePluralGroup);

        for (final List<CxProposedTranslationModel> batch : translationBatches) {
            try {
                final List<String> batchTargetIso = batch.stream()
                        .flatMap(v -> v.getFilteredTargetLanguages().stream())
                        .map(C2LItemModel::getIsocode)
                        .distinct()
                        .toList();

                final List<ValueWithHintOverride> withHints = batch.stream().map(v -> {
                    String valueHint = "";
                    if (v.isAutomaticPlurals() && v.getTarget() instanceof final CxLocalizedLabelModel label) {
                        final String relatedValues = label.getRelatedLabels()
                                .stream()
                                .map(relatedLabel -> "[%s]=[%s]".formatted(relatedLabel.getKey(),
                                        relatedLabel.getValue(sourceLocale)))
                                .collect(Collectors.joining());
                        valueHint += "Other plural forms for this key are: %s\n".formatted(relatedValues);
                    }
                    return new ValueWithHintOverride(v, valueHint);
                }).toList();


                performTranslation(withHints, sourceIso, batchTargetIso, globalHint, cfg, sourceLocale);
                batch.stream()
                        .filter(v -> v.getTranslationStatus() != CxTranslationStatus.ERROR)
                        .forEach(afterTranslation);
            } catch (final Exception ex) {
                LOG.error("AI translation failed\n{}", ExceptionUtils.getStackTrace(ex));
                for (final CxProposedTranslationModel v : batch) {
                    v.setTranslationStatus(CxTranslationStatus.ERROR);
                    appendNote(v, "Error during AI translation: " + ex.getMessage());
                    modelService.save(v);
                }
            }
        }
    }

    private boolean shouldBatchBeSplit(final List<CxProposedTranslationModel> cxProposedTranslationModels, final int batchSize, final Locale sourceLocale) {
        if (cxProposedTranslationModels.stream().map(CxProposedTranslationModel::getConfig).distinct().count() > 1) {
            return true;
        }
        return cxProposedTranslationModels.stream().mapToInt(value ->
                StringUtils.length(value.getValue(sourceLocale)) + StringUtils.length(value.getValueHint())).sum() > batchSize;
    }

    private boolean belongToSamePluralGroup(final CxProposedTranslationModel cxProposedTranslationModel,
                                            final CxProposedTranslationModel cxProposedTranslationModel1) {
        if (cxProposedTranslationModel.getTarget() instanceof final CxLocalizedLabelModel label1
                && cxProposedTranslationModel1.getTarget() instanceof final CxLocalizedLabelModel label2) {
            return label1.getRelatedLabels().contains(label2);
        }
        return false;
    }

    @Override
    public void aiTranslate(final CxProposedTranslationModel value,
                            final String globalHint, final CxTranslationServiceSettingsModel settings,
                            final Consumer<CxProposedTranslationModel> afterTranslation) {

        aiTranslate(List.of(value), globalHint, settings, afterTranslation);
    }

    private void performTranslation(
            final List<ValueWithHintOverride> valueWithHint,
            final String sourceIso,
            final List<String> targetIso,
            final String globalHint, final CxAiTranslationServiceSettingsModel cfg, final Locale sourceLocale) {


        final String configHint = valueWithHint.stream().map(v -> v.value.getConfig()).filter(Objects::nonNull).findAny().map(CxAiTranslationStrategyImpl::buildHintForConfig).orElse("");
        final String finalHint = globalHint + (StringUtils.isNotBlank(configHint) ? "\nAdditional hints: \n" + configHint : "");
        final CxTranslationRequestDto req =
                buildRequest(valueWithHint, sourceIso, targetIso, finalHint, cfg, sourceLocale);

        final CxTranslationResponseDto resp = aiTranslationService.translate(req, cfg);

        if (resp.getStatus() == CxTranslationResponseDto.Status.ERROR) {
            valueWithHint.forEach(value -> {
                value.value().setTranslationStatus(CxTranslationStatus.ERROR);
                if (cfg.getAiServiceApi().getLoggingLevel() != CxTranslateServiceLoggingLevel.NEVER) {
                    value.value().setNotes(resp.getRawMessage());
                }
            });

        } else {
            //calculating total input length for all values in the batch, to be used for usage cost calculation
            final long totalInputLength = valueWithHint.stream().mapToLong(value -> StringUtils.length(value.value().getValue(sourceLocale)) + StringUtils.length(value.hint())).sum();

            resp.getTranslations().forEach(translationResult -> {
                final String key = translationResult.getKey();
                final List<ValueWithHintOverride> correspondingVars = valueWithHint.stream()
                        .filter(vwh -> vwh.value().getKey().equals(key))
                        .toList();
                if (correspondingVars.isEmpty()) {
                    throw new IllegalStateException(
                            "Translation result contains key '%s' which was not part of the request".formatted(
                                    key));
                }
                // Apply the result to every value sharing this key (e.g., variant catalog rows or plural forms)
                for (final ValueWithHintOverride correspondingVar : correspondingVars) {
                    final CxProposedTranslationModel value = correspondingVar.value();
                    if (StringUtils.isNotBlank(translationResult.getFeedback())) {
                        appendNote(value, MessageFormat.format("AI translation feedback: {0}", translationResult.getFeedback()));
                    }
                    final ArrayList<LanguageModel> translatedLanguages = new ArrayList<>();
                    value.setTranslatedLanguages(translatedLanguages);
                    translationResult.getLanguageToValueMap().forEach((iso, txt) -> {
                        value.setProposedTranslationValue(txt, i18nService.getLocale(iso));
                        value.getFilteredTargetLanguages().stream().filter(lang -> lang.getIsocode().equals(iso))
                                .findFirst()
                                .ifPresent(translatedLanguages::add);
                    });

                    if (cfg.getAiServiceApi().getLoggingLevel() == CxTranslateServiceLoggingLevel.ALWAYS) {
                        appendNote(value, "Response:\n" + resp.getRawMessage());
                    }
                    value.setUsageInfo(estimateUsageInfo(correspondingVar, sourceLocale, resp, totalInputLength));
                    value.setTranslationStatus(CxTranslationStatus.SUCCESS);

                    modelService.save(value);
                }

            });
            // Get the CronJob from the first value for usage tracking at job level
            final CxTranslationCronJobModel cronJob = valueWithHint.stream()
                    .map(v -> v.value().getTranslationCronJob())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            updateUsageCosts(cfg, resp.getUsageInfo(), cronJob);
        }
    }

    /**
     * Estimates the usage info (cost metrics) for a single value by calculating its proportional share
     * based on the input length ratio relative to the total batch input length.
     * If total input length is zero (all values empty), distributes usage evenly across values.
     */
    private Map<String, Double> estimateUsageInfo(final ValueWithHintOverride value, final Locale sourceLocale, final CxTranslationResponseDto resp, final long totalInputLength) {
        if (totalInputLength <= 0L) {
            return Collections.emptyMap();
        }
        final double singleValueInputLength = StringUtils.length(value.value.getValue(sourceLocale)) + StringUtils.length(value.hint());
        final double proportionRatio = singleValueInputLength / (double) totalInputLength;
        return resp.getUsageInfo().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (double) Math.round(proportionRatio * entry.getValue())));
    }

    private void updateUsageCosts(final CxAiTranslationServiceSettingsModel serviceSettings, final Map<String, Double> translationUsage,
                                   final CxTranslationCronJobModel cronJob) {
        // Update usage on AI Service Settings (global tracking)
        modelService.refresh(serviceSettings);
        Map<String, Double> settingsUsage = serviceSettings.getUsageInfo();
        if (settingsUsage == null || settingsUsage.isEmpty()) {
            settingsUsage = new HashMap<>();
        } else {
            // Wrap in mutable HashMap in case it's an immutable map
            settingsUsage = new HashMap<>(settingsUsage);
        }
        final Map<String, Double> finalSettingsUsage = settingsUsage;
        translationUsage.forEach((key, value) -> finalSettingsUsage.merge(key, value, Double::sum));
        serviceSettings.setUsageInfo(settingsUsage);
        modelService.save(serviceSettings);

        // Update usage on CronJob (job-level tracking)
        if (cronJob != null) {
            modelService.refresh(cronJob);
            Map<String, Double> cronJobUsage = cronJob.getUsageInfo();
            if (cronJobUsage == null || cronJobUsage.isEmpty()) {
                cronJobUsage = new HashMap<>();
            } else {
                cronJobUsage = new HashMap<>(cronJobUsage);
            }
            final Map<String, Double> finalCronJobUsage = cronJobUsage;
            translationUsage.forEach((key, value) -> finalCronJobUsage.merge(key, value, Double::sum));
            cronJob.setUsageInfo(cronJobUsage);
            modelService.save(cronJob);
        }
    }

    /* ================================================================== */
    /* ================  DTO builders & utils  ========================== */
    /* ================================================================== */

    private CxTranslationRequestDto buildRequest(final List<ValueWithHintOverride> values,
                                                 final String sourceIso,
                                                 final List<String> targetIso,
                                                 final String globalHint, final CxAiTranslationServiceSettingsModel cfg,
                                                 final Locale srcLocale) {

        final List<CxTranslationsInputTextDto> inputTextDtos = values.stream().map(v -> {
            final CxTranslationsInputTextDto in = new CxTranslationsInputTextDto();
            in.setKey(v.value().getKey());
            in.setValue(v.value.getValue(srcLocale));
            in.setValueTranslationHint(v.hint());

            final List<String> valueTargetIsos = v.value().getFilteredTargetLanguages().stream().map(C2LItemModel::getIsocode).toList();
            if (!valueTargetIsos.equals(targetIso)) {
                in.setTargetLanguagesOverride(valueTargetIsos);
            }
            return in;
        }).toList();


        final CxTranslationRequestDto req = new CxTranslationRequestDto();
        req.setSourceLanguage(sourceIso);
        req.setTargetLanguages(targetIso);
        req.setGlobalTranslationHint(globalHint);
        req.setValuesForTranslation(inputTextDtos);
        return req;
    }

    protected static String buildHintForConfig(final CxLocalizedObjectConfigModel valueConfig) {
        String valueTranslationHint = valueConfig
                .getCategories()
                .stream()
                .map(CxLocalizedObjectCategoryModel::getTranslationHint)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(";"));

        if (StringUtils.isNotBlank(valueConfig.getTranslationHint())) {
            valueTranslationHint += ";" + valueConfig.getTranslationHint();
        }
        return valueTranslationHint;
    }

    private static void appendNote(final CxProposedTranslationModel v, final String note) {
        v.setNotes(v.getNotes() == null ? note : v.getNotes() + '\n' + note);
    }

    public void setAiTranslationService(final CxTranslationService aiTranslationService) {
        this.aiTranslationService = aiTranslationService;
    }

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setI18nService(final CxTranslateCommerceCommonI18NService i18nService) {
        this.i18nService = i18nService;
    }
}

