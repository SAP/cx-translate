/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved
 */

package com.sap.cx.translations.services.impl;

import com.sap.cx.translations.daos.CxLocalizedBundleDao;
import com.sap.cx.translations.daos.CxLocalizedLabelDao;
import com.sap.cx.translations.events.CxClearLabelsCacheEvent;
import com.sap.cx.translations.labels.UsageType;
import com.sap.cx.translations.model.CxLocalizedBundleModel;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import com.sap.cx.translations.services.CxTranslationsService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.cms2.enums.CmsApprovalStatus;
import de.hybris.platform.core.model.security.PrincipalGroupModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.event.EventService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link CxTranslationsService}
 */
public class CxTranslationsServiceImpl implements CxTranslationsService {
	private static final Logger LOG = LoggerFactory.getLogger(CxTranslationsServiceImpl.class);
	private CxLocalizedLabelDao labelDao;
	private CxLocalizedBundleDao bundleDao;
	private ModelService modelService;
	private EventService eventService;

	private Integer keySuggestionsLimit;
	private Set<String> userGroupsAllowedForFrontendTranslation;

	@Override
	public Collection<CxLocalizedLabelModel> getLabels(final CatalogVersionModel catalogVersion, final UsageType... usageType) {
		return labelDao.findLabelsByUsage(catalogVersion, usageType);
	}

	@Override
	public Collection<CxLocalizedLabelModel> getLabels(final String bundleCode, final CatalogVersionModel catalogVersion, final UsageType... usageTypes
    ) {
		final Optional<CxLocalizedBundleModel> bundle = getBundle(bundleCode, catalogVersion);
		return bundle.map(b -> b.getLabels()
                .stream()
                .filter(label->checkUsage(label,usageTypes))
                .toList()
        ).orElseGet(()-> {
			LOG.error("Cannot find bundle {}", bundleCode);
			return Collections.emptyList();
		});
	}

    private boolean checkUsage(final CxLocalizedLabelModel label, final UsageType[] usageTypes) {
        return ArrayUtils.isEmpty(usageTypes)
                || Arrays.stream(usageTypes).anyMatch(usageType -> (label.isForFrontend() && usageType == UsageType.FRONTEND)
                || (label.isForBackend() && usageType == UsageType.BACKEND));
    }

    @Override
	public Optional<CxLocalizedLabelModel> getLabel(final String key, final CatalogVersionModel catalogVersion) {
		return getLabel(key, catalogVersion, null);
	}

	@Override
	public Optional<CxLocalizedLabelModel> getLabel(final String key, final CatalogVersionModel catalogVersion, final UsageType usageType) {
		return labelDao.findLabelByKeyAndUsage(key, catalogVersion, usageType);
	}

	@Override
	public CxLocalizedLabelModel getLabelOrCreate(final String key, final CatalogVersionModel catalogVersion, final UsageType usage) {
		return getLabel(key, catalogVersion, usage).orElseGet(() -> {
			final CxLocalizedLabelModel label = modelService.create(CxLocalizedLabelModel.class);
			label.setKey(key);
			label.setForFrontend(usage == UsageType.FRONTEND);
			label.setForBackend(usage == UsageType.BACKEND);
			label.setCatalogVersion(catalogVersion);
			label.setStatus(CmsApprovalStatus.APPROVED);

			// modelService.save(label); explicitly not saved as it lacks mandatory fields values
			return label;
		});
	}

	@Override
	public Collection<CxLocalizedBundleModel> getBundles(final CatalogVersionModel catalogVersion) {
		return bundleDao.findBundlesByCatalogVersion(catalogVersion);
	}

	@Override
	public Optional<CxLocalizedBundleModel> getBundle(final String code, final CatalogVersionModel catalogVersion) {
		return bundleDao.findBundleByCode(code, catalogVersion);
	}

	@Override
	public CxLocalizedBundleModel getBundleOrCreate(final String code, final CatalogVersionModel catalogVersion) {
		return getBundle(code, catalogVersion).orElseGet(() -> {
			final CxLocalizedBundleModel bundle = modelService.create(CxLocalizedBundleModel.class);
			bundle.setCode(code);
			bundle.setCatalogVersion(catalogVersion);

			modelService.save(bundle);

			return bundle;
		});
	}

	@Override
	public void clearLabelsCache() {
		this.eventService.publishEvent(new CxClearLabelsCacheEvent());
	}


	@Override
	public List<String> getKeys(final CatalogVersionModel catalogVersion, final String keyTerm) {
		return labelDao.findLabelKeys(keyTerm, catalogVersion, keySuggestionsLimit);
	}

	@Override
	public Boolean isFrontendTranslationAllowedForUser(final UserModel user) {
		if (user == null) {
			return false;
		}
		return user.getAllGroups()
				.stream()
				.map(PrincipalGroupModel::getUid)
				.anyMatch(this.userGroupsAllowedForFrontendTranslation::contains);
	}

	public void setLabelDao(final CxLocalizedLabelDao labelDao) {
		this.labelDao = labelDao;
	}

	public void setBundleDao(final CxLocalizedBundleDao bundleDao) {
		this.bundleDao = bundleDao;
	}

	public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}

	public void setEventService(final EventService eventService) {
		this.eventService = eventService;
	}

	public void setKeySuggestionsLimit(final Integer keySuggestionsLimit) {
		this.keySuggestionsLimit = keySuggestionsLimit;
	}

	public void setUserGroupsAllowedForFrontendTranslation(final String userGroupsAllowedForFrontendTranslation) {
		if (StringUtils.isNotBlank(userGroupsAllowedForFrontendTranslation)) {
			this.userGroupsAllowedForFrontendTranslation = Arrays.stream(userGroupsAllowedForFrontendTranslation.split(","))
					.map(String::trim)
					.collect(Collectors.toSet());
		} else {
			this.userGroupsAllowedForFrontendTranslation = Collections.emptySet();
		}
	}
}