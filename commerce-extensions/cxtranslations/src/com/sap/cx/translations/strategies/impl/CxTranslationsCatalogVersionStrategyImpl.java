/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.strategies.impl;

import com.sap.cx.translations.strategies.CxTranslationsCatalogVersionStrategy;

import de.hybris.platform.catalog.CatalogService;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.cms2.common.service.SessionSearchRestrictionsDisabler;
import de.hybris.platform.cms2.model.contents.ContentCatalogModel;
import de.hybris.platform.cms2.servicelayer.services.CMSSiteService;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link CxTranslationsCatalogVersionStrategy}
 */
public class CxTranslationsCatalogVersionStrategyImpl implements CxTranslationsCatalogVersionStrategy
{
	private CMSSiteService cmsSiteService;
	private CatalogService catalogService;
	private CatalogVersionService catalogVersionService;
	private SessionSearchRestrictionsDisabler sessionSearchRestrictionsDisabler;

	private String defaultTranslationCatalogId;

	@Override
	public CatalogVersionModel getSessionTranslationsCatalogVersion() {
		return getSessionContentCatalogVersions().stream()
												 .findFirst()
												 .or(() -> getDefaultContentCatalog()
														 .map(ContentCatalogModel::getActiveCatalogVersion))
												 .orElseThrow(() -> new IllegalStateException("Cannot determine active content catalog version"));
	}

	@Override
	public CatalogVersionModel getStagedTranslationsCatalogVersion() {
		return sessionSearchRestrictionsDisabler.execute(() -> {
			final Optional<ContentCatalogModel> contentCatalog = getDefaultContentCatalog();
			return contentCatalog
					.flatMap(catalog -> catalog.getCatalogVersions()
											   .stream()
											   .filter(version -> !Objects.equals(version, catalog.getActiveCatalogVersion()))
											   .findFirst())
					.orElseThrow(() -> new IllegalStateException("Cannot determine staged content catalog version"));
		});
	}

	protected Collection<CatalogVersionModel> getSessionContentCatalogVersions() {
		return catalogVersionService
				.getSessionCatalogVersions()
				.stream()
				.filter((catalogVersion) -> ContentCatalogModel.class.isAssignableFrom(catalogVersion.getCatalog()
																									 .getClass()))
				.collect(Collectors.toList());
	}

	/**
	 * Adjust the default catalog retrieval for labels - if not present on the current site, then fallback to a default catalog
     * specified in the configuration property `cxtranslations.default.translation.catalog.id`
     * for multi country setup the parent (global) content catalog should be selected instead of the country specific ones
	 *
	 * @return content catalog
	 */
	protected Optional<ContentCatalogModel> getDefaultContentCatalog() {

        final Optional<ContentCatalogModel> catalogFromSite = Optional.ofNullable(cmsSiteService.getCurrentSite())
                .flatMap(cmsSite -> cmsSite.getContentCatalogs()
                        .stream()
                        .map(this::getRootContentCatalog)
                        .filter(Objects::nonNull)
                        .findFirst());
        return catalogFromSite
                .or(this::getFallbackCatalogFromProperty);
    }


    private Optional<ContentCatalogModel> getFallbackCatalogFromProperty() {
        if (StringUtils.isBlank(defaultTranslationCatalogId)) {
            return Optional.empty();
        }

        CatalogModel catalogForId = catalogService.getCatalogForId(defaultTranslationCatalogId);
        if (catalogForId instanceof ContentCatalogModel contentCatalogModel) {
            return Optional.of(contentCatalogModel);
        }
        return Optional.empty();
    }

	/**
	 * Walks the super-catalog chain to the root. Uses a visited set to defend against
	 * cyclic supercatalog references (corrupt data) which would otherwise StackOverflow.
	 */
	protected ContentCatalogModel getRootContentCatalog(final ContentCatalogModel catalog) {
		if (catalog == null) {
			return null;
		}
		final Set<ContentCatalogModel> visited = new HashSet<>();
		ContentCatalogModel current = catalog;
		while (current.getSuperCatalog() != null && visited.add(current)) {
			current = current.getSuperCatalog();
		}
		return current;
	}

	public void setCmsSiteService(final CMSSiteService cmsSiteService) {
		this.cmsSiteService = cmsSiteService;
	}

	public void setCatalogVersionService(final CatalogVersionService catalogVersionService) {
		this.catalogVersionService = catalogVersionService;
	}

	public void setSessionSearchRestrictionsDisabler(final SessionSearchRestrictionsDisabler sessionSearchRestrictionsDisabler) {
		this.sessionSearchRestrictionsDisabler = sessionSearchRestrictionsDisabler;
	}

	public void setDefaultTranslationCatalogId(String defaultTranslationCatalogId) {
		this.defaultTranslationCatalogId = defaultTranslationCatalogId;
	}

	public void setCatalogService(CatalogService catalogService) {
		this.catalogService = catalogService;
	}
}
