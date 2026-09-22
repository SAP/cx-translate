package com.sap.cx.translations.services.impl.plurals;

import com.ibm.icu.text.PluralRules;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import com.sap.cx.translations.services.CxLocalizedLabelsPluralsService;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CxLocalizedLabelsPluralsServiceImpl implements CxLocalizedLabelsPluralsService {
    private final ModelService modelService;
    private final Set<String> pluralSuffixes;
    private final CommonI18NService commonI18NService;


    public CxLocalizedLabelsPluralsServiceImpl(final ModelService modelService, final String pluralSuffixes, final CommonI18NService commonI18NService) {
        this.modelService = modelService;
        this.pluralSuffixes = Arrays.stream(pluralSuffixes.split(",")).map(String::trim).collect(Collectors.toSet());
        this.commonI18NService = commonI18NService;
    }


    @Override
    public boolean shouldHavePluralForms(final CxLocalizedLabelModel localizedLabel) {
        return pluralSuffixes.stream().anyMatch(localizedLabel.getKey()::endsWith)
                || CollectionUtils.isNotEmpty(localizedLabel.getRelatedLabels());
    }

    @Override
    public List<CxLocalizedLabelModel> getRequiredPluralForms(final List<CxLocalizedLabelModel> localizedLabels,
                                                              final Collection<LanguageModel> targetLanguages, final boolean createMissing) {
        return localizedLabels.stream()
                .flatMap(label -> getRequiredPluralForms(label, targetLanguages, createMissing).stream())
                .distinct().toList();
    }


    @Override
    public boolean isRequiredPluralForm(final CxLocalizedLabelModel label, final LanguageModel language) {
        return getRequiredPluralForms(label, List.of(language), false).contains(label);
    }

    private List<CxLocalizedLabelModel> getRequiredPluralForms(final CxLocalizedLabelModel localizedLabel,
                                                               final Collection<LanguageModel> targetLanguages, final boolean createMissing) {
        if (!shouldHavePluralForms(localizedLabel)) {
            return List.of();
        }
        final Set<String> existingCodes = new HashSet<>(localizedLabel.getRelatedLabels().stream().map(CxLocalizedLabelModel::getKey).toList());
        existingCodes.add(localizedLabel.getKey());

        final Set<CxLocalizedLabelModel> result = new HashSet<>();
        for (final LanguageModel targetLanguage : targetLanguages) {
            result.addAll(getRequiredPluralFormsForALanguage(localizedLabel, targetLanguage, existingCodes, createMissing));
            result.stream().map(CxLocalizedLabelModel::getKey).forEach(existingCodes::add);
        }

        return result.stream().toList();
    }


    private List<CxLocalizedLabelModel> getRequiredPluralFormsForALanguage(final CxLocalizedLabelModel localizedLabel,
                                                                           final LanguageModel targetLanguage,
                                                                           final Set<String> existingCodes, final boolean createMissing) {
        final Set<String> requiredSuffixes = new HashSet<>(
                PluralRules.forLocale(commonI18NService.getLocaleForLanguage(targetLanguage)).getKeywords());

        final String keyWithoutSuffix = getKeyWithoutSuffix(localizedLabel);//for lambda



        //if there is only "other" form, we assume that the base label is used
        if (requiredSuffixes.size() == 1) {
            requiredSuffixes.remove("other");
        }


        //if there is no explicit "one" form, we assume that the base label is used for singular and there is no need to create an additional one
        if (existingCodes.stream().noneMatch(code -> code.endsWith("_one"))) {
            requiredSuffixes.remove("one");
        }

        //base label is always required
        requiredSuffixes.add("");


        final List<String> missingSuffixes = requiredSuffixes.stream()
                .filter(suffix -> existingCodes.stream()
                        .noneMatch(code -> code.endsWith("_" + suffix) || code.equals(keyWithoutSuffix) && suffix.isEmpty()))
                .toList();

        final Set<CxLocalizedLabelModel> labelGroup = new HashSet<>(localizedLabel.getRelatedLabels());
        labelGroup.add(localizedLabel);

        final Stream<CxLocalizedLabelModel> requiredExistingLabels = labelGroup
                .stream()
                .filter(label -> isLabelRequired(label, keyWithoutSuffix, requiredSuffixes));

        if (!createMissing) {
            return requiredExistingLabels.toList();
        }

        final Stream<CxLocalizedLabelModel> requiredNewLabels = missingSuffixes.stream().map(suffix -> {
            final CxLocalizedLabelModel newLabel = createAndSaveMissingLabel(localizedLabel, suffix, keyWithoutSuffix);
            return newLabel;
        });
        return Stream.concat(requiredExistingLabels, requiredNewLabels)
                .toList();
    }

    private CxLocalizedLabelModel createAndSaveMissingLabel(final CxLocalizedLabelModel localizedLabel, final String suffix, final String keyWithoutSuffix) {
        final CxLocalizedLabelModel newLabel = modelService.create(CxLocalizedLabelModel.class);
        newLabel.setKey(suffix.isEmpty() ?  keyWithoutSuffix : keyWithoutSuffix + "_" + suffix);
        newLabel.setBundle(localizedLabel.getBundle());
        newLabel.setCatalogVersion(localizedLabel.getCatalogVersion());
        newLabel.setForFrontend(localizedLabel.isForFrontend());
        newLabel.setForBackend(localizedLabel.isForBackend());
        newLabel.setStatus(localizedLabel.getStatus());
        modelService.save(newLabel);
        return newLabel;
    }

    private String getKeyWithoutSuffix(final CxLocalizedLabelModel localizedLabel) {
        String keyWithoutSuffix = null;
        final String labelKey = localizedLabel.getKey();
        for (final String pluralSuffix : pluralSuffixes) {
            if (labelKey.endsWith(pluralSuffix)) {
                keyWithoutSuffix = labelKey.substring(0, labelKey.length() - pluralSuffix.length());
                break;
            }
        }
        if (keyWithoutSuffix == null) {
            keyWithoutSuffix = labelKey;
        }
        return keyWithoutSuffix;
    }

    private boolean isLabelRequired(final CxLocalizedLabelModel label, final String keyWithoutSuffix, final Set<String> requiredSuffixes) {
        return label.getKey()
                .equals(keyWithoutSuffix)
                || requiredSuffixes.stream()
                .anyMatch(
                        suffix -> label.getKey()
                                .equals(keyWithoutSuffix + "_" + suffix));
    }

}
