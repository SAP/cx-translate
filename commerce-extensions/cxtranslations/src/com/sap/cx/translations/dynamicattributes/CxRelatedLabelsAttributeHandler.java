/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.dynamicattributes;

import com.sap.cx.translations.daos.CxLocalizedLabelDao;
import com.sap.cx.translations.labels.UsageType;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Dynamic attribute handler for {@link CxLocalizedLabelModel#RELATEDLABELS}
 */
public class CxRelatedLabelsAttributeHandler implements DynamicAttributeHandler<List<CxLocalizedLabelModel>, CxLocalizedLabelModel> {
	private List<String> i18nextSuffixes;
	private CxLocalizedLabelDao cxLocalizedLabelDao;

	@Override
	public List<CxLocalizedLabelModel> get(final CxLocalizedLabelModel model) {
		final String key = model.getKey();
		if (StringUtils.isNotBlank(key)) {
			final UsageType usageType;
			if (model.isForFrontend()) {
				usageType = UsageType.FRONTEND;
			} else {
				if (model.isForBackend()) {
                    usageType = UsageType.BACKEND;
                } else {
                    usageType = null;
                }
			}
            final Optional<String> foundSuffix = i18nextSuffixes.stream()
																.filter(key::endsWith)
																.findAny();

			if (foundSuffix.isEmpty()) { // base label
				final List<String> keyVariants = i18nextSuffixes.stream()
																.map(key::concat)
																.toList();

				return cxLocalizedLabelDao.findLabelsByKeysAndUsage(keyVariants, model.getCatalogVersion(), usageType)
										  .stream()
										  .toList();
			} else {
				final String existingSuffix = foundSuffix.get();
				final String baseKey = key.substring(0, key.length() - existingSuffix.length());

				final List<String> keyVariants = i18nextSuffixes.stream()
																.filter(Predicate.not(existingSuffix::equals))
																.map(baseKey::concat)
																.toList();

				return cxLocalizedLabelDao.findLabelsByKeysAndUsage(ListUtils.union(Collections.singletonList(baseKey), keyVariants), model.getCatalogVersion(), usageType)
										  .stream()
										  .toList();
			}
		}

		return Collections.emptyList();
	}

	@Override
	public void set(final CxLocalizedLabelModel model, final List<CxLocalizedLabelModel> cxLocalizedLabelModels) {
		throw new UnsupportedOperationException("Read only attribute");
	}

	public void setI18nextSuffixes(final String i18nextSuffixes) {
		this.i18nextSuffixes = Arrays.stream(i18nextSuffixes.split(","))
									 .map(String::trim)
									 .toList();
	}

	public void setCxLocalizedLabelDao(final CxLocalizedLabelDao cxLocalizedLabelDao) {
		this.cxLocalizedLabelDao = cxLocalizedLabelDao;
	}
}
