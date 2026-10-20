/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.automatictranslation.impl;

import com.sap.cx.translations.generated.dto.translation.deepl.request.CxTranslationsDeepLRequest;
import com.sap.cx.translations.generated.dto.translation.deepl.request.CxTranslationsDeepLRequest.SourceLanguageText;
import com.sap.cx.translations.generated.dto.translation.deepl.request.CxTranslationsDeepLRequest.TargetLanguageText;
import com.sap.cx.translations.generated.dto.translation.deepl.response.CxTranslationsDeepLResponse;
import com.sap.cx.translations.enums.CxTranslationStatus;
import com.sap.cx.translations.exceptions.CxTranslationException;
import com.sap.cx.translations.model.CxDeepLTranslationServiceSettingsModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslateGlossaryModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import com.sap.cx.translations.serviceinterfaces.deepl.CxTranslationsDeeplInterfaceService;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import com.sap.cx.translations.services.impl.automatictranslation.CxAiTranslationStrategy;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CxDeepLTranslationStrategyImpl implements CxAiTranslationStrategy {
    private Map<String, TargetLanguageText> targetLanguageMap = new HashMap<>();
    private Map<String, SourceLanguageText> sourceLanguageMap = new HashMap<>();

    private CxTranslationsDeeplInterfaceService interfaceService;
    private CxTranslateCommerceCommonI18NService i18nService;
    private ModelService modelService;

    @Override
    public boolean supports(final CxTranslationServiceSettingsModel settings) {
        return settings instanceof CxDeepLTranslationServiceSettingsModel;
    }

    @Override
    public void aiTranslate(final CxProposedTranslationModel value,
                            final String globalHint, final CxTranslationServiceSettingsModel settings,
                            final Consumer<CxProposedTranslationModel> afterTranslation) {
        final LanguageModel sourceLanguage = value.getSourceLanguage();
        final CxDeepLTranslationServiceSettingsModel deepLSettings = (CxDeepLTranslationServiceSettingsModel) settings;
        final String context = joinWithNewLine(globalHint, value.getValueHint());
        final Map<Locale, CxTranslationsDeepLResponse> responsesForLangs = value.getFilteredTargetLanguages().stream().collect(Collectors
                .toMap(lang -> i18nService.getLocale(lang.getIsocode()), targetLang -> translateSingleLanguage(value, deepLSettings, i18nService.getLocale(sourceLanguage.getIsocode()), i18nService.getLocale(targetLang.getIsocode()), context))
        );
        responsesForLangs.forEach((locale, deeplResponse) -> {
            if (deeplResponse == null || CollectionUtils.isEmpty(deeplResponse.getTranslations())) {
                throw new CxTranslationException("DeepL returned no translation for locale " + locale);
            }
            value.setProposedTranslationValue(deeplResponse.getTranslations().get(0).getText(), locale);
        });
        value.setTranslatedLanguages(new ArrayList<>(value.getFilteredTargetLanguages()));
        value.setTranslationStatus(CxTranslationStatus.SUCCESS);
        modelService.save(value);
        if (afterTranslation != null) {
            afterTranslation.accept(value);
        }
    }

    private CxTranslationsDeepLResponse translateSingleLanguage(final CxProposedTranslationModel value, final CxDeepLTranslationServiceSettingsModel deepLSettings, final Locale sourceLocale, final Locale targetLocale, final String context) {
        final CxTranslationsDeepLRequest deepLRequest = new CxTranslationsDeepLRequest();
        deepLRequest.setText(List.of(value.getValue(sourceLocale)));
        deepLRequest.setSourceLang(mapToSourceDeepLLanguage(sourceLocale));
        deepLRequest.setTargetLang(mapToTargetDeepLLanguage(targetLocale));
        if (deepLSettings.getFormality() != null) {
            deepLRequest.setFormality(CxTranslationsDeepLRequest.Formality.valueOf(deepLSettings.getFormality().getCode()));
        }

        deepLRequest.setGlossaryId(resolveGlossaryRef(deepLSettings, sourceLocale, targetLocale));
        deepLRequest.setIgnoreTags(deepLSettings.getIgnoreTags());
        deepLRequest.setNonSplittingTags(deepLSettings.getNonSplittingTags());
        deepLRequest.setSplittingTags(deepLSettings.getSplittingTags());

        if (deepLSettings.getModelType() != null) {
            deepLRequest.setModelType(CxTranslationsDeepLRequest.ModelType.valueOf(deepLSettings.getModelType().getCode()));
        }

        deepLRequest.setOutlineDetection(deepLSettings.getOutlineDetection());
        deepLRequest.setPreserveFormatting(deepLSettings.getPreserveFormatting());
        if (deepLSettings.getSplitSentences() != null) {
            deepLRequest.setSplitSentences(CxTranslationsDeepLRequest.SplitSentencesOption.valueOf(deepLSettings.getSplitSentences().getCode()));
        }

        if (deepLSettings.getTagHandling() != null) {
            deepLRequest.setTagHandling(CxTranslationsDeepLRequest.TagHandlingOption.valueOf(deepLSettings.getTagHandling().getCode()));
        }

        return interfaceService.translate(deepLRequest, deepLSettings);
    }

    private String resolveGlossaryRef(final CxDeepLTranslationServiceSettingsModel settings,
                                      final Locale sourceLocale, final Locale targetLocale) {
        if (CollectionUtils.isEmpty(settings.getGlossaries())) {
            return null;
        }
        return settings.getGlossaries().stream()
                .filter(g -> glossaryMatchesLanguages(g, sourceLocale, targetLocale))
                .findFirst()
                .map(CxTranslateGlossaryModel::getExternalGlossaryRef)
                .orElse(null);
    }

    private boolean glossaryMatchesLanguages(final CxTranslateGlossaryModel glossary,
                                             final Locale sourceLocale, final Locale targetLocale) {
        if (glossary.getSourceLanguage() == null || glossary.getTargetLanguage() == null) {
            return false;
        }
        return sourceLocale.getLanguage().equalsIgnoreCase(glossary.getSourceLanguage().getIsocode())
                && targetLocale.getLanguage().equalsIgnoreCase(glossary.getTargetLanguage().getIsocode());
    }

    private TargetLanguageText mapToTargetDeepLLanguage(final Locale locale) {
        final String language = locale.getLanguage().toLowerCase(Locale.ROOT);
        final String country = locale.getCountry().toUpperCase(Locale.ROOT);
        String key = language;

        if (!country.isEmpty()) {
            key = language + "_" + country;
        }

        if (targetLanguageMap.containsKey(key)) {
            return targetLanguageMap.get(key);
        }

        try {
            return TargetLanguageText.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new CxTranslationException("Cannot find DeepL Target Language for locale " + key);
        }
    }

    private SourceLanguageText mapToSourceDeepLLanguage(final Locale locale) {
        final String language = locale.getLanguage().toLowerCase(Locale.ROOT);
        final String country = locale.getCountry().toUpperCase(Locale.ROOT);

        if (!country.isEmpty()) {
            final String key = language + "_" + country;

            if (sourceLanguageMap.containsKey(key)) {
                return sourceLanguageMap.get(key);
            }

            try {
                return SourceLanguageText.valueOf(key.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException e) {
                throw new CxTranslationException("Cannot find DeepL Source Language for locale " + locale);
            }
        }

        try {
            return SourceLanguageText.valueOf(language.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new CxTranslationException("Cannot find DeepL Source Language for locale " + locale);
        }
    }

    private String joinWithNewLine(final String a, final String b) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a + "\n" + b;
    }

    public void setInterfaceService(final CxTranslationsDeeplInterfaceService interfaceService) {
        this.interfaceService = interfaceService;
    }

    public void setI18nService(final CxTranslateCommerceCommonI18NService i18nService) {
        this.i18nService = i18nService;
    }

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setTargetLanguageMap(final Map<String, TargetLanguageText> map) {
        this.targetLanguageMap = map;
    }

    public void setSourceLanguageMap(final Map<String, SourceLanguageText> map) {
        this.sourceLanguageMap = map;
    }
}
