/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.automatictranslation.impl;

import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.model.*;
import com.sap.cx.translations.serviceinterfaces.saptranslationhub.CxSapTranslationHubInterfaceService;
import com.sap.cx.translations.serviceinterfaces.saptranslationhub.CxSapTranslationHubPluralHelper;
import com.sap.cx.translations.serviceinterfaces.saptranslationhub.CxSapTranslationHubXliffHelper;
import com.sap.cx.translations.serviceinterfaces.saptranslationhub.CxSapTranslationHubXliffHelper.TransUnit;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import com.sap.cx.translations.services.impl.automatictranslation.CxAiTranslationStrategy;
import de.hybris.platform.core.model.c2l.C2LItemModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Translation strategy for SAP Translation Hub.
 * Builds XLIFF 1.2 documents from batches of proposed translations and sends them
 * to the SAP Translation Hub document translation API (one call per target language).
 * <p>
 * Supports plural forms: when {@code automaticPlurals} is enabled and labels have related labels,
 * individual plural form labels are composed into a single ICU plural message for translation,
 * then the translated message is parsed back into individual plural forms.
 */
public class CxSapTranslationHubTranslationStrategyImpl implements CxAiTranslationStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(CxSapTranslationHubTranslationStrategyImpl.class);
    private static final int DEFAULT_DIRECT_TEXT_TRANSLATION_THRESHOLD_BYTES = 1024;
    private static final int DEFAULT_MAX_XLIFF_BATCH_SIZE_BYTES = 4096;
    private static final String DIRECT_TEXT_TRANSLATION_THRESHOLD_BYTES_ATTRIBUTE = "directTextTranslationThresholdBytes";
    private static final String MAX_XLIFF_BATCH_SIZE_BYTES_ATTRIBUTE = "maxXliffBatchSizeBytes";

    private record RequestUnit(CxProposedTranslationModel value, String sourceText, TransUnit transUnit) {
    }

    /**
     * Represents a plural group: multiple proposed translations that belong to the same plural group,
     * sent as a single ICU plural message trans-unit.
     */
    private record PluralGroupUnit(String compositeKey, List<CxProposedTranslationModel> values,
                                   Map<String, CxProposedTranslationModel> categoryToValue,
                                   TransUnit transUnit) {
    }

    private record TargetRequest(List<RequestUnit> units, List<PluralGroupUnit> pluralUnits, boolean directText) {
    }

    private CxSapTranslationHubInterfaceService interfaceService;
    private CxTranslateCommerceCommonI18NService i18nService;
    private ModelService modelService;

    /**
     * Configurable mapping from hybris isocode (e.g. "pl", "zh_cn", "en") to SAP Translation Hub BCP-47 code
     * (e.g. "pl-PL", "zh-CN", "en-US"). Entries here override the automatic resolution.
     */
    private Map<String, String> languageMap = new HashMap<>();

    /**
     * Configurable plural suffixes used to identify plural forms from label keys.
     * Matches the suffixes configured in {@code cxtranslations.i18next.suffixes}.
     */
    private Set<String> pluralSuffixes = Set.of("_zero", "_one", "_two", "_few", "_many", "_other");

    @Override
    public boolean supports(final CxTranslationServiceSettingsModel settings) {
        return settings instanceof CxSapTranslationHubServiceSettingsModel;
    }

    @Override
    public void aiTranslate(final List<CxProposedTranslationModel> values,
                            final String globalHint,
                            final CxTranslationServiceSettingsModel settings,
                            final Consumer<CxProposedTranslationModel> afterTranslation) {
        if (values.isEmpty()) {
            return;
        }

        final CxSapTranslationHubServiceSettingsModel hubSettings = (CxSapTranslationHubServiceSettingsModel) settings;
        final List<CxProposedTranslationModel> sortedValues = values.stream()
                .sorted(Comparator.comparing(CxAbstractLocalizedValueModel::getKey))
                .toList();

        final LanguageModel sourceLanguage = sortedValues.getFirst().getSourceLanguage();
        final String sourceIso = sourceLanguage.getIsocode();
        final Locale sourceLocale = i18nService.getLocale(sourceIso);
        final boolean escapeHtml = Boolean.TRUE.equals(hubSettings.getEnableHtmlTagHandling());
        final int directTextTranslationThresholdBytes = getConfiguredThreshold(hubSettings,
                DIRECT_TEXT_TRANSLATION_THRESHOLD_BYTES_ATTRIBUTE,
                DEFAULT_DIRECT_TEXT_TRANSLATION_THRESHOLD_BYTES);
        final int maxXliffBatchSizeBytes = getConfiguredThreshold(hubSettings,
                MAX_XLIFF_BATCH_SIZE_BYTES_ATTRIBUTE,
                DEFAULT_MAX_XLIFF_BATCH_SIZE_BYTES);

        // Collect all distinct target languages across the batch
        final List<String> allTargetIsos = sortedValues.stream()
                .flatMap(v -> v.getFilteredTargetLanguages().stream())
                .map(C2LItemModel::getIsocode)
                .distinct()
                .toList();

        final Set<CxProposedTranslationModel> translated = Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        // For each target language, build one XLIFF and send
        for (final String targetIso : allTargetIsos) {
            // Filter values that actually need this target language
            final List<CxProposedTranslationModel> valuesForTarget = sortedValues.stream()
                    .filter(v -> v.getFilteredTargetLanguages().stream()
                            .anyMatch(lang -> lang.getIsocode().equals(targetIso)))
                    .toList();

            try {
                translateForTargetLanguage(valuesForTarget, globalHint, hubSettings, sourceLocale, sourceIso,
                        targetIso, escapeHtml, directTextTranslationThresholdBytes, maxXliffBatchSizeBytes,
                        translated);
            } catch (final Exception ex) {
                LOG.error("SAP Translation Hub translation failed for target language {}\n{}",
                        targetIso, ExceptionUtils.getStackTrace(ex));
                for (final CxProposedTranslationModel v : valuesForTarget) {
                    v.setTranslationStatus(CxTranslationStatus.ERROR);
                    appendNote(v, "Error during SAP Translation Hub translation for " + targetIso + ": " + ex.getMessage());
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
                                            final CxSapTranslationHubServiceSettingsModel hubSettings,
                                            final Locale sourceLocale,
                                            final String sourceIso,
                                            final String targetIso,
                                            final boolean escapeHtml,
                                            final int directTextTranslationThresholdBytes,
                                            final int maxXliffBatchSizeBytes,
                                            final Set<CxProposedTranslationModel> translated) {
        final String sourceBcp47 = toBcp47(sourceIso);
        final String targetBcp47 = toBcp47(targetIso);
        final Locale targetLocale = i18nService.getLocale(targetIso);
        final LanguageModel targetLanguageModel = resolveTargetLanguageModel(valuesForTarget, targetIso);

        final List<TargetRequest> requests = createTargetRequests(valuesForTarget, globalHint, sourceLocale,
                sourceBcp47, targetBcp47, escapeHtml, directTextTranslationThresholdBytes, maxXliffBatchSizeBytes,
                targetLocale);

        for (final TargetRequest request : requests) {
            try {
                if (request.directText()) {
                    final RequestUnit unit = request.units().getFirst();
                    final String translatedText = interfaceService.translateText(unit.sourceText(), targetBcp47, hubSettings);
                    applyTranslatedText(unit.value(), translatedText, targetLocale, targetLanguageModel, false, translated);
                } else {
                    final List<TransUnit> allTransUnits = new ArrayList<>();
                    for (final RequestUnit unit : request.units()) {
                        allTransUnits.add(unit.transUnit());
                    }
                    for (final PluralGroupUnit pluralUnit : request.pluralUnits()) {
                        allTransUnits.add(pluralUnit.transUnit());
                    }

                    final String xliff = CxSapTranslationHubXliffHelper.buildXliff(sourceBcp47, targetBcp47, allTransUnits);
                    final Map<String, String> translations = interfaceService.translateXliff(xliff, targetBcp47, hubSettings);

                    // Apply regular (non-plural) translations
                    for (final RequestUnit unit : request.units()) {
                        applyTranslatedText(unit.value(), translations.get(unit.value().getKey()), targetLocale,
                                targetLanguageModel, escapeHtml, translated);
                    }

                    // Apply plural group translations by parsing ICU message back
                    for (final PluralGroupUnit pluralUnit : request.pluralUnits()) {
                        final String translatedIcu = translations.get(pluralUnit.compositeKey());
                        applyPluralTranslation(pluralUnit, translatedIcu, targetLocale, targetLanguageModel, escapeHtml, translated);
                    }
                }
            } catch (final Exception ex) {
                final String requestType = request.directText() ? "direct text" : "XLIFF batch";
                LOG.error("SAP Translation Hub {} translation failed for target language {}\n{}",
                        requestType, targetIso, ExceptionUtils.getStackTrace(ex));
                for (final RequestUnit unit : request.units()) {
                    unit.value().setTranslationStatus(CxTranslationStatus.ERROR);
                    appendNote(unit.value(), "Error during SAP Translation Hub translation for " + targetIso + ": " + ex.getMessage());
                    modelService.save(unit.value());
                }
                for (final PluralGroupUnit pluralUnit : request.pluralUnits()) {
                    for (final CxProposedTranslationModel v : pluralUnit.values()) {
                        v.setTranslationStatus(CxTranslationStatus.ERROR);
                        appendNote(v, "Error during SAP Translation Hub translation for " + targetIso + ": " + ex.getMessage());
                        modelService.save(v);
                    }
                }
            }
        }
    }

    private List<TargetRequest> createTargetRequests(final List<CxProposedTranslationModel> valuesForTarget,
                                                     final String globalHint,
                                                     final Locale sourceLocale,
                                                     final String sourceBcp47,
                                                     final String targetBcp47,
                                                     final boolean escapeHtml,
                                                     final int directTextTranslationThresholdBytes,
                                                     final int maxXliffBatchSizeBytes,
                                                     final Locale targetLocale) {
        final List<TargetRequest> requests = new ArrayList<>();
        final List<RequestUnit> currentBatch = new ArrayList<>();
        final List<PluralGroupUnit> currentPluralBatch = new ArrayList<>();
        final Set<String> processedPluralKeys = new HashSet<>();

        for (final CxProposedTranslationModel value : valuesForTarget) {
            // Check if this value is part of a plural group
            if (value.isAutomaticPlurals() && value.getTarget() instanceof final CxLocalizedLabelModel label
                    && !label.getRelatedLabels().isEmpty()) {

                final String baseKey = getKeyWithoutPluralSuffix(label.getKey());
                if (processedPluralKeys.contains(baseKey)) {
                    continue; // Already handled as part of the group
                }
                processedPluralKeys.add(baseKey);

                // Collect all values in this plural group from the batch
                final PluralGroupUnit pluralUnit = buildPluralGroupUnit(value, valuesForTarget, sourceLocale,
                        globalHint, escapeHtml, targetLocale);
                if (pluralUnit != null) {
                    // Plural groups always use XLIFF mode (never direct text) because the ICU plural
                    // message structure must be preserved through the XLIFF round-trip.
                    // If adding this plural unit would exceed batch size, flush first.
                    if (!currentBatch.isEmpty() || !currentPluralBatch.isEmpty()) {
                        final List<PluralGroupUnit> expandedPluralBatch = new ArrayList<>(currentPluralBatch);
                        expandedPluralBatch.add(pluralUnit);
                        if (exceedsXliffBatchSize(currentBatch, expandedPluralBatch, sourceBcp47, targetBcp47, maxXliffBatchSizeBytes)) {
                            flushCurrentBatch(requests, currentBatch, currentPluralBatch);
                        }
                    }
                    currentPluralBatch.add(pluralUnit);
                    continue;
                }
            }

            final RequestUnit requestUnit = createRequestUnit(value, globalHint, sourceLocale, escapeHtml);

            if (shouldTranslateDirectly(requestUnit.sourceText(), directTextTranslationThresholdBytes)) {
                flushCurrentBatch(requests, currentBatch, currentPluralBatch);
                requests.add(new TargetRequest(List.of(requestUnit), List.of(), true));
                continue;
            }

            if (!currentBatch.isEmpty() || !currentPluralBatch.isEmpty()) {
                final List<RequestUnit> expandedBatch = new ArrayList<>(currentBatch);
                expandedBatch.add(requestUnit);
                if (exceedsXliffBatchSize(expandedBatch, currentPluralBatch, sourceBcp47, targetBcp47, maxXliffBatchSizeBytes)) {
                    flushCurrentBatch(requests, currentBatch, currentPluralBatch);
                }
            }

            if (exceedsXliffBatchSize(List.of(requestUnit), List.of(), sourceBcp47, targetBcp47, maxXliffBatchSizeBytes)) {
                requests.add(new TargetRequest(List.of(requestUnit), List.of(), true));
                continue;
            }

            currentBatch.add(requestUnit);
        }

        flushCurrentBatch(requests, currentBatch, currentPluralBatch);
        return requests;
    }

    /**
     * Builds a PluralGroupUnit by composing an ICU plural message from all related plural labels.
     * The ICU message will be placed in a single XLIFF trans-unit source element.
     */
    private PluralGroupUnit buildPluralGroupUnit(final CxProposedTranslationModel value,
                                                 final List<CxProposedTranslationModel> valuesForTarget,
                                                 final Locale sourceLocale,
                                                 final String globalHint,
                                                 final boolean escapeHtml,
                                                 final Locale targetLocale) {
        final CxLocalizedLabelModel label = (CxLocalizedLabelModel) value.getTarget();
        final String baseKey = getKeyWithoutPluralSuffix(label.getKey());

        // Collect all proposed translations that belong to this plural group
        final Set<String> groupKeys = new HashSet<>();
        groupKeys.add(label.getKey());
        label.getRelatedLabels().forEach(r -> groupKeys.add(r.getKey()));

        final List<CxProposedTranslationModel> groupValues = valuesForTarget.stream()
                .filter(v -> v.getTarget() instanceof CxLocalizedLabelModel targetLabel
                        && groupKeys.contains(targetLabel.getKey()))
                .toList();

        if (groupValues.isEmpty()) {
            return null;
        }

        // Build map of plural category -> source text
        final Map<String, String> sourceForms = new LinkedHashMap<>();
        final Map<String, CxProposedTranslationModel> categoryToValue = new LinkedHashMap<>();

        for (final CxProposedTranslationModel gv : groupValues) {
            final String labelKey = ((CxLocalizedLabelModel) gv.getTarget()).getKey();
            final String category = getPluralCategory(labelKey, baseKey);
            String sourceText = gv.getValue(sourceLocale);
            if (escapeHtml && sourceText != null) {
                sourceText = escapeHtmlTags(sourceText);
            }
            // Only include forms with actual source text in sourceForms;
            // blank forms (auto-created plural labels) will use fallback resolution in composePluralMessage
            if (StringUtils.isNotBlank(sourceText)) {
                sourceForms.put(category, sourceText);
            }
            categoryToValue.put(category, gv);
        }

        // Compose ICU plural message including all target-required categories.
        // Each form's full text is placed inside the ICU braces with no prefix/suffix extraction.
        final String icuMessage = CxSapTranslationHubPluralHelper.composePluralMessage(sourceForms, targetLocale);

        // Build note
        final String note = buildPluralNote(value, globalHint, sourceLocale);
        final TransUnit transUnit = new TransUnit(baseKey, icuMessage, note);

        return new PluralGroupUnit(baseKey, groupValues, categoryToValue, transUnit);
    }

    /**
     * Applies the translated ICU plural message back to individual plural form values.
     */
    private void applyPluralTranslation(final PluralGroupUnit pluralUnit,
                                        final String translatedIcu,
                                        final Locale targetLocale,
                                        final LanguageModel targetLanguageModel,
                                        final boolean unescapeHtml,
                                        final Set<CxProposedTranslationModel> translated) {
        if (StringUtils.isBlank(translatedIcu)) {
            LOG.warn("No translation received for plural group {}", pluralUnit.compositeKey());
            return;
        }

        final Map<String, String> translatedForms = CxSapTranslationHubPluralHelper.parsePluralMessage(translatedIcu);
        if (translatedForms.isEmpty()) {
            // Fallback: if not a valid ICU message, try to apply the whole text to the "other" form
            LOG.warn("Could not parse ICU plural response for {}, applying raw text to 'other' form", pluralUnit.compositeKey());
            final CxProposedTranslationModel otherValue = pluralUnit.categoryToValue().get("other");
            if (otherValue != null) {
                applyTranslatedText(otherValue, translatedIcu, targetLocale, targetLanguageModel, unescapeHtml, translated);
            }
            return;
        }

        // Distribute translated forms to individual values
        for (final Map.Entry<String, CxProposedTranslationModel> entry : pluralUnit.categoryToValue().entrySet()) {
            final String category = entry.getKey();
            final CxProposedTranslationModel value = entry.getValue();
            final String translatedText = translatedForms.getOrDefault(category,
                    translatedForms.get("other")); // fallback to "other" if category missing in response
            if (translatedText != null) {
                applyTranslatedText(value, translatedText, targetLocale, targetLanguageModel, unescapeHtml, translated);
            }
        }
    }

    /**
     * Extracts the plural category from a label key given the base key.
     * E.g., key="cart.total_few", baseKey="cart.total" -> "few"
     * If key equals baseKey (no suffix), returns "other" per i18next convention
     * where the base/unsuffixed label represents the default (other) form.
     */
    private String getPluralCategory(final String key, final String baseKey) {
        if (key.equals(baseKey)) {
            return "other";
        }
        if (key.startsWith(baseKey + "_")) {
            return key.substring(baseKey.length() + 1);
        }
        return "other";
    }

    /**
     * Removes the plural suffix from a key to get the base key.
     */
    private String getKeyWithoutPluralSuffix(final String key) {
        for (final String suffix : pluralSuffixes) {
            if (key.endsWith(suffix)) {
                return key.substring(0, key.length() - suffix.length());
            }
        }
        return key;
    }


    private String buildPluralNote(final CxProposedTranslationModel value, final String globalHint,
                                   final Locale sourceLocale) {
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

    private RequestUnit createRequestUnit(final CxProposedTranslationModel value,
                                          final String globalHint,
                                          final Locale sourceLocale,
                                          final boolean escapeHtml) {
        final String rawSourceText = StringUtils.defaultString(value.getValue(sourceLocale));
        String xliffSourceText = rawSourceText;
        if (escapeHtml) {
            xliffSourceText = escapeHtmlTags(xliffSourceText);
        }

        return new RequestUnit(value, rawSourceText, new TransUnit(value.getKey(), xliffSourceText,
                buildNote(value, globalHint, sourceLocale)));
    }

    private static void flushCurrentBatch(final List<TargetRequest> requests,
                                          final List<RequestUnit> currentBatch,
                                          final List<PluralGroupUnit> currentPluralBatch) {
        if (!currentBatch.isEmpty() || !currentPluralBatch.isEmpty()) {
            requests.add(new TargetRequest(List.copyOf(currentBatch), List.copyOf(currentPluralBatch), false));
            currentBatch.clear();
            currentPluralBatch.clear();
        }
    }

    private static boolean shouldTranslateDirectly(final String sourceText, final int directTextTranslationThresholdBytes) {
        return utf8Size(sourceText) > directTextTranslationThresholdBytes;
    }

    private static boolean exceedsXliffBatchSize(final List<RequestUnit> units,
                                                 final List<PluralGroupUnit> pluralUnits,
                                                 final String sourceBcp47,
                                                 final String targetBcp47,
                                                 final int maxXliffBatchSizeBytes) {
        final List<TransUnit> allTransUnits = new ArrayList<>();
        units.forEach(u -> allTransUnits.add(u.transUnit()));
        pluralUnits.forEach(p -> allTransUnits.add(p.transUnit()));
        return utf8Size(CxSapTranslationHubXliffHelper.buildXliff(sourceBcp47, targetBcp47, allTransUnits)) > maxXliffBatchSizeBytes;
    }

    private void applyTranslatedText(final CxProposedTranslationModel value,
                                     String translatedText,
                                     final Locale targetLocale,
                                     final LanguageModel targetLanguageModel,
                                     final boolean unescapeHtml,
                                     final Set<CxProposedTranslationModel> translated) {
        if (translatedText == null) {
            return;
        }

        if (unescapeHtml) {
            translatedText = unescapeHtmlTags(translatedText);
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

    private static int utf8Size(final String value) {
        return StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8).length;
    }

    private int getConfiguredThreshold(final CxSapTranslationHubServiceSettingsModel settings,
                                       final String attributeQualifier,
                                       final int defaultValue) {
        final Integer configuredValue = modelService.getAttributeValue(settings, attributeQualifier);
        return configuredValue != null && configuredValue > 0 ? configuredValue : defaultValue;
    }

    private static LanguageModel resolveTargetLanguageModel(final List<CxProposedTranslationModel> valuesForTarget,
                                                            final String targetIso) {
        return valuesForTarget.getFirst().getFilteredTargetLanguages().stream()
                .filter(l -> l.getIsocode().equals(targetIso))
                .findFirst()
                .orElse(null);
    }

    /**
     * Builds the note for a trans-unit, combining global hint and value-specific hint.
     * For plural forms (i18next labels with related labels), adds context about other plural forms.
     */
    private String buildNote(final CxProposedTranslationModel value, final String globalHint,
                             final Locale sourceLocale) {
        final List<String> parts = new ArrayList<>();

        if (StringUtils.isNotBlank(globalHint)) {
            parts.add(globalHint);
        }
        if (StringUtils.isNotBlank(value.getValueHint())) {
            parts.add(value.getValueHint());
        }

        // Add plural context if available
        if (value.isAutomaticPlurals() && value.getTarget() instanceof final CxLocalizedLabelModel label) {
            final String relatedValues = label.getRelatedLabels()
                    .stream()
                    .map(relatedLabel -> "[%s]=[%s]".formatted(relatedLabel.getKey(),
                            relatedLabel.getValue(sourceLocale)))
                    .collect(Collectors.joining("; "));
            if (StringUtils.isNotBlank(relatedValues)) {
                parts.add("Other plural forms for this key: " + relatedValues);
            }
        }

        return parts.isEmpty() ? null : String.join("\n", parts);
    }

    /**
     * Escapes HTML/XML special characters in source text so SAP Translation Hub treats them as literal text
     * and not as markup. Uses XML 1.0 escaping (e.g. {@code <} → {@code &lt;}).
     */
    private static String escapeHtmlTags(final String text) {
        if (text == null) {
            return null;
        }
        return StringEscapeUtils.escapeXml10(text);
    }

    /**
     * Unescapes XML-escaped characters that were preserved through the XLIFF round-trip
     * (e.g. {@code &lt;} → {@code <}).
     */
    private static String unescapeHtmlTags(final String text) {
        if (text == null) {
            return null;
        }
        return StringEscapeUtils.unescapeXml(text);
    }

    /**
     * Converts a hybris isocode to SAP Translation Hub BCP-47 format.
     */
    String toBcp47(final String isocode) {
        if (isocode == null) {
            return null;
        }

        final String normalizedKey = normalizeIsocodeKey(isocode);
        final String configuredLanguage = languageMap.get(normalizedKey);
        if (StringUtils.isNotBlank(configuredLanguage)) {
            return canonicalizeBcp47(configuredLanguage);
        }

        final String normalized = canonicalizeBcp47(isocode);
        if (normalized.contains("-")) {
            return normalized;
        }

        final String lang = normalized.toLowerCase(Locale.ROOT);
        LOG.warn("No explicit SAP Translation Hub language mapping found for isocode '{}'. Falling back to legacy pattern.",
                isocode);
        return lang + "-" + lang.toUpperCase(Locale.ROOT);
    }

    private static String normalizeIsocodeKey(final String isocode) {
        return isocode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String canonicalizeBcp47(final String languageTag) {
        final String[] segments = languageTag.trim().replace('_', '-').split("-");
        if (segments.length == 0) {
            return languageTag;
        }

        final List<String> normalizedSegments = new ArrayList<>(segments.length);
        for (int i = 0; i < segments.length; i++) {
            final String segment = segments[i];
            if (i == 0) {
                normalizedSegments.add(segment.toLowerCase(Locale.ROOT));
            } else if (segment.length() == 4 && StringUtils.isAlpha(segment)) {
                normalizedSegments.add(StringUtils.capitalize(segment.toLowerCase(Locale.ROOT)));
            } else if ((segment.length() == 2 && StringUtils.isAlpha(segment))
                    || (segment.length() == 3 && StringUtils.isNumeric(segment))) {
                normalizedSegments.add(segment.toUpperCase(Locale.ROOT));
            } else {
                normalizedSegments.add(segment.toLowerCase(Locale.ROOT));
            }
        }

        return String.join("-", normalizedSegments);
    }


    private static void appendNote(final CxProposedTranslationModel v, final String note) {
        v.setNotes(v.getNotes() == null ? note : v.getNotes() + '\n' + note);
    }

    public void setInterfaceService(final CxSapTranslationHubInterfaceService interfaceService) {
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
        this.pluralSuffixes = Arrays.stream(pluralSuffixes.split(",")).map(String::trim).collect(Collectors.toSet());
    }
}

