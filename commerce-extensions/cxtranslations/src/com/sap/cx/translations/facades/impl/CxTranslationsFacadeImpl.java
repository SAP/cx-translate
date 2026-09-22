/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.facades.impl;

import com.google.common.base.Suppliers;
import com.sap.cx.translations.cache.CxLocalizedLabelCacheKeyProvider;
import com.sap.cx.translations.facades.CxTranslationsFacade;
import com.sap.cx.translations.labels.CxLocalizedLabel;
import com.sap.cx.translations.labels.UsageType;
import com.sap.cx.translations.messages.CxDatabaseMessageSource;
import com.sap.cx.translations.model.CxLocalizedBundleModel;
import com.sap.cx.translations.model.CxLocalizedLabelModel;
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService;
import com.sap.cx.translations.services.CxTranslationsService;
import com.sap.cx.translations.strategies.CxTranslationsCatalogVersionStrategy;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.cms2.common.service.SessionSearchRestrictionsDisabler;
import de.hybris.platform.commerceservices.setup.SetupSyncJobService;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.regioncache.CacheController;
import de.hybris.platform.regioncache.key.CacheKey;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link CxTranslationsFacade}
 */
public class CxTranslationsFacadeImpl implements CxTranslationsFacade {
	private static final String LABELS_CACHE_ENABLED_KEY = "cxtranslations.cache.enabled";
	private static final String LABELS_IMPORT_ALLOWED_KEY = "cxtranslations.import.allowed";
	private static final String INCLUDE_EMPTY_VALUES_KEY = "cxtranslations.include.empty.values";
	private static final String ACTING_USER_UID = "ACTING_USER_UID";

	private static final Logger LOG = LoggerFactory.getLogger(CxTranslationsFacadeImpl.class);

	private CxTranslationsService translationsService;
	private ModelService modelService;
	private CacheController cacheController;
	private CxLocalizedLabelCacheKeyProvider cxLocalizedLabelCacheKeyProvider;
	private ConfigurationService configurationService;
	private CxTranslationsCatalogVersionStrategy translationsCatalogVersionStrategy;
	private SessionSearchRestrictionsDisabler sessionSearchRestrictionsDisabler;
	private SetupSyncJobService setupSyncJobService;
	private CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService;
	private SessionService sessionService;
	private UserService userService;

	private final Supplier<Boolean> useCache = Suppliers.memoizeWithExpiration(() -> configurationService.getConfiguration()
			.getBoolean(LABELS_CACHE_ENABLED_KEY, false), 1, TimeUnit.MINUTES);
	private final Supplier<Boolean> allowImport = Suppliers.memoizeWithExpiration(() -> configurationService.getConfiguration()
			.getBoolean(LABELS_IMPORT_ALLOWED_KEY, false), 1, TimeUnit.MINUTES);
	private final Supplier<Boolean> includeEmptyValues = Suppliers.memoizeWithExpiration(() -> configurationService.getConfiguration()
			.getBoolean(INCLUDE_EMPTY_VALUES_KEY, false), 1, TimeUnit.MINUTES);

	@Override
	public void importLabels(final Map<String, Map<String, String>> translations, final String language) {
		if (!Boolean.TRUE.equals(allowImport.get())) {
			throw new IllegalStateException("Labels import has been disabled - see configuration");
		}

		final Locale locale = cxTranslateCommerceCommonI18NService.getLocale(language);
		final CatalogVersionModel contentCatalogVersion = translationsCatalogVersionStrategy.getStagedTranslationsCatalogVersion();

		if (contentCatalogVersion == null) {
			throw new IllegalStateException("Could not find staged catalog version for labels import!");
		}

		// need to execute without restrictions to find existing items in staged catalogs
		sessionSearchRestrictionsDisabler.execute(() -> {
			translations.forEach((bundle, labels) -> {
				final CxLocalizedBundleModel bundleModel = translationsService.getBundleOrCreate(bundle, contentCatalogVersion);

				labels.forEach((key, label) -> {
					final CxLocalizedLabelModel labelModel = translationsService.getLabelOrCreate(key, contentCatalogVersion, UsageType.FRONTEND);

					labelModel.setBundle(bundleModel);
					labelModel.setValue(label, locale);
					modelService.save(labelModel);
				});
			});

			return null;
		});
	}

	@Override
	public Map<String, String> getLabelsMap(final UsageType... usageType) {
		return getLabelsMap(useCache.get(), usageType);
	}

	@Override
	public Map<String, String> getLabelsMap(final boolean useCache, final UsageType... usageType) {
		final CatalogVersionModel contentCatalogVersion = translationsCatalogVersionStrategy.getSessionTranslationsCatalogVersion();
		if (useCache) {
			final CacheKey allLabelsKey = cxLocalizedLabelCacheKeyProvider.getAllLabelsKey(usageType, contentCatalogVersion);

			return cacheController.getWithLoader(allLabelsKey, cacheKey -> getLabelsMapInternal(usageType, contentCatalogVersion));
		} else {
			return getLabelsMapInternal(usageType, contentCatalogVersion);
		}
	}

	@Override
	public Map<String, String> getLabelsMap(final String bundle, final UsageType... usageType) {
		return getLabelsMap(bundle, useCache.get(), usageType);
	}

	@Override
	public Map<String, String> getLabelsMap(final String bundle, final boolean useCache, final UsageType... usageType) {
		final CatalogVersionModel contentCatalogVersion = translationsCatalogVersionStrategy.getSessionTranslationsCatalogVersion();
		if (useCache) {
			final CacheKey bundleKey = cxLocalizedLabelCacheKeyProvider.getBundleKey(bundle, usageType, contentCatalogVersion);

			return cacheController.getWithLoader(bundleKey, cacheKey -> getLabelsMapInternal(bundle, usageType, contentCatalogVersion));
		} else {
			return getLabelsMapInternal(bundle, usageType, contentCatalogVersion);
		}
	}

	@Override
	public String getLabel(final String key, final UsageType usageType) {
		return getLabel(key, usageType, useCache.get());
	}

	@Override
	public String getLabel(final String key, final UsageType usageType, final boolean useCache) {
		final CatalogVersionModel contentCatalogVersion = translationsCatalogVersionStrategy.getSessionTranslationsCatalogVersion();
		if (useCache) {
			final CacheKey singleLabelKey = cxLocalizedLabelCacheKeyProvider.getSingleLabelKey(key, usageType, contentCatalogVersion);

			return cacheController.getWithLoader(singleLabelKey, cacheKey -> getLabelInternal(key, usageType, contentCatalogVersion));
		} else {
			return getLabelInternal(key, usageType, contentCatalogVersion);
		}
	}

	@Override
	public String getStagedLabel(final String key, final UsageType usageType) {
		final CatalogVersionModel contentCatalogVersion = translationsCatalogVersionStrategy.getStagedTranslationsCatalogVersion();
		return getLabelInternal(key, usageType, contentCatalogVersion);
	}

	@Override
	public void editLabel(CxLocalizedLabel label) {
		final CatalogVersionModel contentCatalogVersion = translationsCatalogVersionStrategy.getStagedTranslationsCatalogVersion();

		if (contentCatalogVersion == null) {
			LOG.error("Could not find staged catalog version for label edit!");
			return;
		}

		// need to execute without restrictions to find existing items in staged catalogs
		sessionSearchRestrictionsDisabler.execute(() -> {
			final CxLocalizedBundleModel bundleModel = translationsService.getBundleOrCreate(label.getBundle(), contentCatalogVersion);
			final CxLocalizedLabelModel labelModel = translationsService.getLabelOrCreate(label.getKey(), contentCatalogVersion, UsageType.FRONTEND);

			labelModel.setValue(label.getValue());
			labelModel.setBundle(bundleModel);

			modelService.save(labelModel);

			return null;
		});

		cacheController.invalidate(cxLocalizedLabelCacheKeyProvider.getBundleKey(label.getBundle(), new UsageType[]{UsageType.FRONTEND}, contentCatalogVersion));
		cacheController.invalidate(cxLocalizedLabelCacheKeyProvider.getAllLabelsKey(new UsageType[]{UsageType.FRONTEND}, contentCatalogVersion));
		cacheController.invalidate(cxLocalizedLabelCacheKeyProvider.getSingleLabelKey(label.getKey(), UsageType.FRONTEND, contentCatalogVersion));
	}

	@Override
	public boolean publishTranslations() {
		return sessionSearchRestrictionsDisabler.execute(() -> {
			final CatalogVersionModel contentCatalogVersion = translationsCatalogVersionStrategy.getSessionTranslationsCatalogVersion();

			if (contentCatalogVersion == null) {
				LOG.error("Could not find labels session catalog version!");
				return false;
			}

			// execute the sync job synchronously
			final PerformResult result = setupSyncJobService.executeCatalogSyncJob(contentCatalogVersion.getCatalog()
					.getId());
			if (result == null || CronJobResult.SUCCESS != result.getResult()) {
				LOG.error("Synchronization of labels catalog {} failed!", contentCatalogVersion.getCatalog().getId());
				return false;
			}

			LOG.info("Executing labels cache invalidation!");
			// invalidate labels cache
			this.translationsService.clearLabelsCache();
			return true;
		});
	}

	@Override
	public List<String> getKeySuggestions(final String keyTerm) {

		return sessionSearchRestrictionsDisabler.execute(() -> {
			final CatalogVersionModel contentCatalogVersion = translationsCatalogVersionStrategy.getStagedTranslationsCatalogVersion();
			if (contentCatalogVersion == null) {
				LOG.error("Could not label key session catalog version!");
				return List.of();
			}
			return this.translationsService.getKeys(contentCatalogVersion, keyTerm);
		});
	}

	@Override
	public Boolean isFrontendTranslationAllowedForUser() {
		final String agentId = this.sessionService.getAttribute(ACTING_USER_UID);
		if (StringUtils.isNotBlank(agentId)) {
			try {
				final UserModel asAgent = this.userService.getUserForUID(agentId);
				return translationsService.isFrontendTranslationAllowedForUser(asAgent);
			} catch (UnknownIdentifierException e) {
				LOG.warn("Could not find ASM agent user with UID '{}', falling back to current user", agentId);
			}
		}
		final UserModel currentUser = this.userService.getCurrentUser();
		if (currentUser != null) {
			return translationsService.isFrontendTranslationAllowedForUser(currentUser);
		}
		return false;
	}

	protected Map<String, String> getLabelsMapInternal(UsageType[] usageType, CatalogVersionModel catalogVersion) {
		final boolean keepEmptyValues = Boolean.TRUE.equals(includeEmptyValues.get());
		return translationsService.getLabels(catalogVersion, usageType)
				.stream()
				.filter(label -> keepEmptyValues || label.getValue() != null)
				.collect(Collectors.toMap(CxLocalizedLabelModel::getKey,
						label -> Optional.ofNullable(label.getValue()).orElse(StringUtils.EMPTY),
						(existing, duplicate) -> {
							LOG.warn("Duplicate label key detected during map collection; keeping first value.");
							return existing;
						}));
	}

	protected Map<String, String> getLabelsMapInternal(String bundle, UsageType[] usageType, CatalogVersionModel catalogVersion) {
		final boolean keepEmptyValues = Boolean.TRUE.equals(includeEmptyValues.get());
		return translationsService.getLabels(bundle, catalogVersion, usageType)
				.stream()
				.filter(label -> keepEmptyValues || label.getValue() != null)
				.collect(Collectors.toMap(CxLocalizedLabelModel::getKey,
						label -> Optional.ofNullable(label.getValue()).orElse(StringUtils.EMPTY),
						(existing, duplicate) -> {
							LOG.warn("Duplicate label key detected during map collection; keeping first value.");
							return existing;
						}));
	}

	/**
	 * Executed without search restrictions to omit session catalog version validation in cases the session is not properly initialized.
	 * Added because {@link CxDatabaseMessageSource} uses common cache, so to always retrieve the correct value.
	 */
	protected String getLabelInternal(String key, UsageType usageType, CatalogVersionModel catalogVersion) {
		return sessionSearchRestrictionsDisabler.execute(() -> translationsService.getLabel(key, catalogVersion, usageType)
				.map(CxLocalizedLabelModel::getValue)
				.orElse(null));
	}

	public void setTranslationsService(final CxTranslationsService translationsService) {
		this.translationsService = translationsService;
	}

	public void setModelService(final ModelService modelService) {
		this.modelService = modelService;
	}

	public void setCacheController(final CacheController cacheController) {
		this.cacheController = cacheController;
	}

	public void setCxLocalizedLabelCacheKeyProvider(final CxLocalizedLabelCacheKeyProvider cxLocalizedLabelCacheKeyProvider) {
		this.cxLocalizedLabelCacheKeyProvider = cxLocalizedLabelCacheKeyProvider;
	}

	public void setConfigurationService(final ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	public void setTranslationsCatalogVersionStrategy(final CxTranslationsCatalogVersionStrategy translationsCatalogVersionStrategy) {
		this.translationsCatalogVersionStrategy = translationsCatalogVersionStrategy;
	}

	public void setSessionSearchRestrictionsDisabler(final SessionSearchRestrictionsDisabler sessionSearchRestrictionsDisabler) {
		this.sessionSearchRestrictionsDisabler = sessionSearchRestrictionsDisabler;
	}

	public void setSetupSyncJobService(SetupSyncJobService setupSyncJobService) {
		this.setupSyncJobService = setupSyncJobService;
	}

	public void setCxTranslateCommerceCommonI18NService(final CxTranslateCommerceCommonI18NService cxTranslateCommerceCommonI18NService) {
		this.cxTranslateCommerceCommonI18NService = cxTranslateCommerceCommonI18NService;
	}

	public void setSessionService(final SessionService sessionService) {
		this.sessionService = sessionService;
	}

	public void setUserService(final UserService userService) {
		this.userService = userService;
	}
}