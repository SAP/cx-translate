/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.occ.filters;

import com.sap.cx.translations.strategies.CxTranslationsCatalogVersionStrategy;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Filter for replacing online content catalog with staged version for requests for staged version of localized labels
 */
public class CxLabelsPreviewCatalogVersionFilter extends OncePerRequestFilter {
	private Pattern regexp;
	private CxTranslationsCatalogVersionStrategy translationsCatalogVersionStrategy;
	private CatalogVersionService catalogVersionService;

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {
		if (!matchesUrl(request, regexp)) {
			filterChain.doFilter(request, response);
			return;
		}

		final CatalogVersionModel stagedTranslationsCatalogVersion = translationsCatalogVersionStrategy.getStagedTranslationsCatalogVersion();
		if (stagedTranslationsCatalogVersion == null) {
			throw new IllegalStateException("Staged translations catalog version not found. Cannot proceed with request.");
		}

		// Snapshot the original session catalog versions so we can restore them after the request,
		// preventing the staged catalog from bleeding into subsequent requests on the same session.
		final Collection<CatalogVersionModel> originalSessionCatalogVersions = catalogVersionService.getSessionCatalogVersions();

		final ArrayList<CatalogVersionModel> filteredSessionCatalogVersions = originalSessionCatalogVersions
				.stream()
				.filter(catalog -> !Objects.equals(catalog.getCatalog(), stagedTranslationsCatalogVersion.getCatalog())) // remove same catalog as the staged translations catalog version
				.collect(Collectors.toCollection(ArrayList::new));

		filteredSessionCatalogVersions.add(stagedTranslationsCatalogVersion);
		catalogVersionService.setSessionCatalogVersions(filteredSessionCatalogVersions);

		try {
			filterChain.doFilter(request, response);
		} finally {
			// Restore the original session catalog versions to avoid leaking the staged catalog
			catalogVersionService.setSessionCatalogVersions(originalSessionCatalogVersions);
		}
	}

	protected boolean matchesUrl(final HttpServletRequest request, final Pattern regexp) {
		final Matcher matcher = getMatcher(request, regexp);
		return matcher.find();
	}

	protected Matcher getMatcher(final HttpServletRequest request, final Pattern regexp) {
		final String path = getPath(request);
		return regexp.matcher(path);
	}

	protected String getPath(final HttpServletRequest request) {
		return StringUtils.defaultString(request.getPathInfo());
	}

	public void setRegexp(final Pattern regexp) {
		this.regexp = regexp;
	}

	public void setTranslationsCatalogVersionStrategy(final CxTranslationsCatalogVersionStrategy translationsCatalogVersionStrategy) {
		this.translationsCatalogVersionStrategy = translationsCatalogVersionStrategy;
	}

	public void setCatalogVersionService(final CatalogVersionService catalogVersionService) {
		this.catalogVersionService = catalogVersionService;
	}
}
