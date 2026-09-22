/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.automatictranslation.impl;

import com.sap.cx.translations.enums.CxAzureTranslatorMode;
import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.*;
import com.sap.cx.translations.serviceinterfaces.azuretranslator.CxAzureTranslatorInterfaceService;
import com.sap.cx.translations.serviceinterfaces.saptranslationhub.CxSapTranslationHubPluralHelper;
import com.sap.cx.translations.serviceinterfaces.saptranslationhub.CxSapTranslationHubXliffHelper;
import com.sap.cx.translations.serviceinterfaces.saptranslationhub.CxSapTranslationHubXliffHelper.TransUnit;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import com.sap.cx.translations.services.impl.automatictranslation.CxAiTranslationStrategy;
import de.hybris.platform.core.model.c2l.C2LItemModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Translation strategy for Azure Translator document translation.
 * <p>
 * Regular (non-plural) values are sent as native HTML documents — either one multipart POST per value
 * (SYNCHRONOUS mode) or all values in a single async batch job with one blob per value (ASYNC_BATCH mode).
 * This avoids HTML formatting issues caused by the XLIFF round-trip.
 * <p>
 * Plural-group ICU messages ({@code {CNT, plural, ...}}) are still sent via XLIFF to preserve the ICU
 * syntax that Azure Translator would otherwise mangle.
 */
public class CxAzureTranslatorTranslationStrategyImpl implements CxAiTranslationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(CxAzureTranslatorTranslationStrategyImpl.class);

    private record RequestUnit(CxProposedTranslationModel value, String sourceText) {
    }

    private record PluralGroupUnit(String compositeKey, List<CxProposedTranslationModel> values,
                                   Map<String, CxProposedTranslationModel> categoryToValue,
                                   TransUnit transUnit) {
    }

    private CxAzureTranslatorInterfaceService interfaceService;
    private CxTranslateCommerceCommonI18NService i18nService;
    private ModelService modelService;

    /**
     * Configurable mapping from hybris isocode (e.g. "pl", "zh_cn") to Azure BCP-47 tag (e.g. "pl", "zh-Hans").
     */
    private Map<String, String> languageMap = new HashMap<>();

    /**
     * Configurable plural suffixes used to identify plural forms from label keys.
     */
    private Set<String> pluralSuffixes = Set.of("_zero", "_one", "_two", "_few", "_many", "_other");

    @Override
    public boolean supports(final CxTranslationServiceSettingsModel settings) {
        return settings instanceof CxAzureTranslatorServiceSettingsModel;
    }

    @Override
    public void aiTranslate(final List<CxProposedTranslationModel> values,
                            final String globalHint,
                            final CxTranslationServiceSettingsModel settings,
                            final Consumer<CxProposedTranslationModel> afterTranslation) {
        if (values.isEmpty()) {
            return;
        }

        final CxAzureTranslatorServiceSettingsModel azureSettings = (CxAzureTranslatorServiceSettingsModel) settings;
        final List<CxProposedTranslationModel> sortedValues = values.stream()
                .sorted(Comparator.comparing(CxAbstractLocalizedValueModel::getKey))
                .toList();

        final LanguageModel sourceLanguage = sortedValues.getFirst().getSourceLanguage();
        final String sourceIso = sourceLanguage.getIsocode();
        final Locale sourceLocale = i18nService.getLocale(sourceIso);
        final boolean isAsync = CxAzureTranslatorMode.ASYNC_BATCH.equals(azureSettings.getTranslationMode());

        final List<String> allTargetIsos = sortedValues.stream()
                .flatMap(v -> v.getFilteredTargetLanguages().stream())
                .map(C2LItemModel::getIsocode)
                .distinct()
                .toList();

        final Set<CxProposedTranslationModel> translated = Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        for (final String targetIso : allTargetIsos) {
            final List<CxProposedTranslationModel> valuesForTarget = sortedValues.stream()
                    .filter(v -> v.getFilteredTargetLanguages().stream()
                            .anyMatch(lang -> lang.getIsocode().equals(targetIso)))
                    .toList();

            try {
                translateForTargetLanguage(valuesForTarget, globalHint, azureSettings, sourceLocale,
                        targetIso, isAsync, translated);
            } catch (final Exception ex) {
                LOG.error("Azure Translator translation failed for target language {}\n{}",
                        targetIso, ExceptionUtils.getStackTrace(ex));
                for (final CxProposedTranslationModel v : valuesForTarget) {
                    v.setTranslationStatus(CxTranslationStatus.ERROR);
                    appendNote(v, "Error during Azure Translator translation for " + targetIso + ": " + ex.getMessage());
                    modelService.save(v);
                }
            }
        }

        // Only mark values that actually received a translation result as SUCCESS
        for (final CxProposedTranslationModel v : values) {
            if (v.getTranslationStatus() != CxTranslationStatus.ERROR) {
                if (translated.contains(v)) {
                    v.setTranslationStatus(CxTranslationStatus.SUCCESS);
                    modelService.save(v);
                    afterTranslation.accept(v);
                } else {
                    LOG.warn("No translation result received for value [{}]; status left unchanged", v.getKey());
                }
            }
        }
    }

    @Override
    public void aiTranslate(final CxProposedTranslationModel value,
                            final String globalHint, final CxTranslationServiceSettingsModel settings,
                            final Consumer<CxProposedTranslationModel> afterTranslation) {
        aiTranslate(List.of(value), globalHint, settings, afterTranslation);
    }

    private void translateForTargetLanguage(final List<CxProposedTranslationModel> valuesForTarget,
                                             final String globalHint,
                                             final CxAzureTranslatorServiceSettingsModel azureSettings,
                                             final Locale sourceLocale,
                                             final String targetIso,
                                             final boolean isAsync,
                                             final Set<CxProposedTranslationModel> translated) {
        final String targetBcp47 = toBcp47(targetIso);
        final Locale targetLocale = i18nService.getLocale(targetIso);
        final LanguageModel targetLanguageModel = resolveTargetLanguageModel(valuesForTarget, targetIso);

        // Separate regular values from plural groups
        final List<RequestUnit> regularUnits = new ArrayList<>();
        final List<PluralGroupUnit> pluralUnits = new ArrayList<>();
        collectUnits(valuesForTarget, globalHint, sourceLocale, targetLocale, regularUnits, pluralUnits);

        // ---- Regular values: HTML path ----
        if (!regularUnits.isEmpty()) {
            try {
                translateRegularUnits(regularUnits, targetBcp47, targetLocale, targetLanguageModel,
                        azureSettings, isAsync, translated);
            } catch (final Exception ex) {
                LOG.error("Azure Translator HTML translation failed for target language {}\n{}",
                        targetIso, ExceptionUtils.getStackTrace(ex));
                for (final RequestUnit unit : regularUnits) {
                    unit.value().setTranslationStatus(CxTranslationStatus.ERROR);
                    appendNote(unit.value(), "Error during Azure Translator translation for " + targetIso + ": " + ex.getMessage());
                    modelService.save(unit.value());
                }
            }
        }

        // ---- Plural groups: XLIFF path (preserves ICU syntax) ----
        if (!pluralUnits.isEmpty()) {
            try {
                translatePluralUnits(pluralUnits, toBcp47(valuesForTarget.getFirst().getSourceLanguage().getIsocode()),
                        targetBcp47, targetLocale, targetLanguageModel, azureSettings, isAsync, translated);
            } catch (final Exception ex) {
                LOG.error("Azure Translator XLIFF plural translation failed for target language {}\n{}",
                        targetIso, ExceptionUtils.getStackTrace(ex));
                for (final PluralGroupUnit pluralUnit : pluralUnits) {
                    for (final CxProposedTranslationModel v : pluralUnit.values()) {
                        v.setTranslationStatus(CxTranslationStatus.ERROR);
                        appendNote(v, "Error during Azure Translator translation for " + targetIso + ": " + ex.getMessage());
                        modelService.save(v);
                    }
                }
            }
        }
    }

    // ---- HTML translation path (regular values) --------------------------------

    private void translateRegularUnits(final List<RequestUnit> units,
                                        final String targetBcp47,
                                        final Locale targetLocale,
                                        final LanguageModel targetLanguageModel,
                                        final CxAzureTranslatorServiceSettingsModel settings,
                                        final boolean isAsync,
                                        final Set<CxProposedTranslationModel> translated) {
        // Collect all key→sourceText pairs in insertion order
        final Map<String, String> keysToHtml = new LinkedHashMap<>();
        for (final RequestUnit unit : units) {
            if (StringUtils.isNotBlank(unit.sourceText())) {
                keysToHtml.put(unit.value().getKey(), unit.sourceText());
            }
        }
        if (keysToHtml.isEmpty()) {
            return;
        }

        final Map<String, String> translations = isAsync
                ? interfaceService.translateHtmlAsync(keysToHtml, targetBcp47, settings)
                : interfaceService.translateHtmlSync(keysToHtml, targetBcp47, settings);

        for (final RequestUnit unit : units) {
            final String translatedText = translations.get(unit.value().getKey());
            if (translatedText != null) {
                applyTranslatedText(unit.value(), translatedText, targetLocale, targetLanguageModel, translated);
            }
        }
    }

    // ---- XLIFF translation path (plural ICU messages only) --------------------

    private void translatePluralUnits(final List<PluralGroupUnit> pluralUnits,
                                       final String sourceBcp47,
                                       final String targetBcp47,
                                       final Locale targetLocale,
                                       final LanguageModel targetLanguageModel,
                                       final CxAzureTranslatorServiceSettingsModel settings,
                                       final boolean isAsync,
                                       final Set<CxProposedTranslationModel> translated) {
        final List<TransUnit> allTransUnits = pluralUnits.stream()
                .map(PluralGroupUnit::transUnit)
                .collect(Collectors.toList());

        final String xliff = CxSapTranslationHubXliffHelper.buildXliff(sourceBcp47, targetBcp47, allTransUnits);
        final Map<String, String> translations = isAsync
                ? interfaceService.translatePluralXliffAsync(xliff, targetBcp47, settings)
                : interfaceService.translatePluralXliffSync(xliff, targetBcp47, settings);

        for (final PluralGroupUnit pluralUnit : pluralUnits) {
            applyPluralTranslation(pluralUnit, translations.get(pluralUnit.compositeKey()),
                    targetLocale, targetLanguageModel, translated);
        }
    }

    // ---- Unit collection -------------------------------------------------------

    private void collectUnits(final List<CxProposedTranslationModel> valuesForTarget,
                               final String globalHint,
                               final Locale sourceLocale,
                               final Locale targetLocale,
                               final List<RequestUnit> regularUnits,
                               final List<PluralGroupUnit> pluralUnits) {
        final Set<String> processedPluralKeys = new HashSet<>();

        for (final CxProposedTranslationModel value : valuesForTarget) {
            if (value.isAutomaticPlurals() && value.getTarget() instanceof final CxLocalizedLabelModel label
                    && !label.getRelatedLabels().isEmpty()) {
                final String baseKey = getKeyWithoutPluralSuffix(label.getKey());
                if (processedPluralKeys.contains(baseKey)) {
                    continue;
                }
                processedPluralKeys.add(baseKey);
                final PluralGroupUnit pluralUnit = buildPluralGroupUnit(value, valuesForTarget, sourceLocale,
                        globalHint, targetLocale);
                if (pluralUnit != null) {
                    pluralUnits.add(pluralUnit);
                    continue;
                }
            }

            final String sourceText = StringUtils.defaultString(value.getValue(sourceLocale));
            regularUnits.add(new RequestUnit(value, sourceText));
        }
    }

    // ---- Plural helpers --------------------------------------------------------

    private PluralGroupUnit buildPluralGroupUnit(final CxProposedTranslationModel value,
                                                  final List<CxProposedTranslationModel> valuesForTarget,
                                                  final Locale sourceLocale,
                                                  final String globalHint,
                                                  final Locale targetLocale) {
        final CxLocalizedLabelModel label = (CxLocalizedLabelModel) value.getTarget();
        final String baseKey = getKeyWithoutPluralSuffix(label.getKey());

        final Set<String> groupKeys = new HashSet<>();
        groupKeys.add(label.getKey());
        label.getRelatedLabels().forEach(r -> groupKeys.add(r.getKey()));

        final List<CxProposedTranslationModel> groupValues = valuesForTarget.stream()
                .filter(v -> groupKeys.contains(v.getKey()))
                .toList();

        if (groupValues.isEmpty()) {
            return null;
        }

        final Map<String, String> sourceForms = new LinkedHashMap<>();
        final Map<String, CxProposedTranslationModel> categoryToValue = new LinkedHashMap<>();

        for (final CxProposedTranslationModel gv : groupValues) {
            final String category = getPluralCategory(gv.getKey(), baseKey);
            final String sourceText = StringUtils.defaultString(gv.getValue(sourceLocale));
            sourceForms.put(category, sourceText);
            categoryToValue.put(category, gv);
        }

        final String prefix = extractCommonPrefix(sourceForms);
        final String suffix = extractCommonSuffix(sourceForms);
        final Map<String, String> cleanedForms = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : sourceForms.entrySet()) {
            String text = entry.getValue();
            if (StringUtils.isNotEmpty(prefix) && text.startsWith(prefix)) {
                text = text.substring(prefix.length());
            }
            if (StringUtils.isNotEmpty(suffix) && text.endsWith(suffix)) {
                text = text.substring(0, text.length() - suffix.length());
            }
            cleanedForms.put(entry.getKey(), text);
        }

        final String icuMessage = CxSapTranslationHubPluralHelper.composePluralMessage(cleanedForms, targetLocale, prefix, suffix);
        final String note = buildPluralNote(value, globalHint);
        return new PluralGroupUnit(baseKey, groupValues, categoryToValue, new TransUnit(baseKey, icuMessage, note));
    }

    private void applyPluralTranslation(final PluralGroupUnit pluralUnit,
                                         final String translatedIcu,
                                         final Locale targetLocale,
                                         final LanguageModel targetLanguageModel,
                                         final Set<CxProposedTranslationModel> translated) {
        if (StringUtils.isBlank(translatedIcu)) {
            LOG.warn("No translation received for plural group {}", pluralUnit.compositeKey());
            return;
        }

        final Map<String, String> translatedForms = CxSapTranslationHubPluralHelper.parsePluralMessage(translatedIcu);
        if (translatedForms.isEmpty()) {
            LOG.warn("Could not parse ICU plural response for {}, applying raw text to 'other' form", pluralUnit.compositeKey());
            final CxProposedTranslationModel otherValue = pluralUnit.categoryToValue().get("other");
            if (otherValue != null) {
                applyTranslatedText(otherValue, translatedIcu, targetLocale, targetLanguageModel, translated);
            }
            return;
        }

        for (final Map.Entry<String, CxProposedTranslationModel> entry : pluralUnit.categoryToValue().entrySet()) {
            final String translatedText = translatedForms.getOrDefault(entry.getKey(), translatedForms.get("other"));
            if (translatedText != null) {
                applyTranslatedText(entry.getValue(), translatedText, targetLocale, targetLanguageModel, translated);
            }
        }
    }

    // ---- Apply / note helpers --------------------------------------------------

    private void applyTranslatedText(final CxProposedTranslationModel value,
                                      final String translatedText,
                                      final Locale targetLocale,
                                      final LanguageModel targetLanguageModel,
                                      final Set<CxProposedTranslationModel> translated) {
        if (translatedText == null) {
            return;
        }
        value.setProposedTranslationValue(translatedText, targetLocale);
        final ArrayList<LanguageModel> translatedLangs = value.getTranslatedLanguages() == null
                ? new ArrayList<>()
                : new ArrayList<>(value.getTranslatedLanguages());
        if (targetLanguageModel != null && !translatedLangs.contains(targetLanguageModel)) {
            translatedLangs.add(targetLanguageModel);
        }
        value.setTranslatedLanguages(translatedLangs);
        translated.add(value);
    }

    private String buildPluralNote(final CxProposedTranslationModel value, final String globalHint) {
        final List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(globalHint)) {
            parts.add(globalHint);
        }
        parts.add("This is an ICU plural message. Translate each plural form appropriately for the target language. " +
                "Keep the {CNT, plural, ...} structure intact.");
        if (StringUtils.isNotBlank(value.getValueHint())) {
            parts.add(value.getValueHint());
        }
        return String.join("\n", parts);
    }

    // ---- Plural key helpers ----------------------------------------------------

    private String getPluralCategory(final String key, final String baseKey) {
        if (key.equals(baseKey)) {
            return "other";
        }
        if (key.startsWith(baseKey + "_")) {
            return key.substring(baseKey.length() + 1);
        }
        return "other";
    }

    private String getKeyWithoutPluralSuffix(final String key) {
        for (final String suffix : pluralSuffixes) {
            if (key.endsWith(suffix)) {
                return key.substring(0, key.length() - suffix.length());
            }
        }
        return key;
    }

    // ---- Common prefix/suffix for plural form cleaning -------------------------

    private static String extractCommonPrefix(final Map<String, String> forms) {
        final List<String> texts = new ArrayList<>(forms.values());
        if (texts.size() < 2) {
            return "";
        }
        final String first = texts.getFirst();
        int end = first.length();
        for (int i = 1; i < texts.size(); i++) {
            final String s = texts.get(i);
            end = Math.min(end, s.length());
            for (int j = 0; j < end; j++) {
                if (first.charAt(j) != s.charAt(j)) {
                    end = j;
                    break;
                }
            }
        }
        return first.substring(0, end);
    }

    private static String extractCommonSuffix(final Map<String, String> forms) {
        final List<String> texts = new ArrayList<>(forms.values());
        if (texts.size() < 2) {
            return "";
        }
        final String first = texts.getFirst();
        int len = first.length();
        for (int i = 1; i < texts.size(); i++) {
            final String s = texts.get(i);
            len = Math.min(len, s.length());
            for (int j = 0; j < len; j++) {
                if (first.charAt(first.length() - 1 - j) != s.charAt(s.length() - 1 - j)) {
                    len = j;
                    break;
                }
            }
        }
        return first.substring(first.length() - len);
    }

    // ---- Misc ------------------------------------------------------------------

    private static LanguageModel resolveTargetLanguageModel(final List<CxProposedTranslationModel> valuesForTarget,
                                                             final String targetIso) {
        return valuesForTarget.getFirst().getFilteredTargetLanguages().stream()
                .filter(l -> l.getIsocode().equals(targetIso))
                .findFirst()
                .orElse(null);
    }

    String toBcp47(final String isocode) {
        if (isocode == null) {
            return null;
        }
        final String normalizedKey = isocode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        final String configured = languageMap.get(normalizedKey);
        if (StringUtils.isNotBlank(configured)) {
            return configured;
        }
        return isocode.trim().replace('_', '-');
    }

    private static void appendNote(final CxProposedTranslationModel v, final String note) {
        v.setNotes(v.getNotes() == null ? note : v.getNotes() + '\n' + note);
    }

    // ---- Setters ---------------------------------------------------------------

    public void setInterfaceService(final CxAzureTranslatorInterfaceService interfaceService) {
        this.interfaceService = interfaceService;
    }

    public void setI18nService(final CxTranslateCommerceCommonI18NService i18nService) {
        this.i18nService = i18nService;
    }

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setLanguageMap(final Map<String, String> languageMap) {
        this.languageMap = languageMap;
    }

    public void setPluralSuffixes(final String pluralSuffixes) {
        this.pluralSuffixes = Arrays.stream(pluralSuffixes.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
