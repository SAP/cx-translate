/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration;

import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel;
import de.hybris.platform.outboundservices.client.IntegrationRestTemplateFactory;
import de.hybris.platform.outboundservices.monitoring.DefaultOutboundRequestResponseInterceptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Wrapper to CxTranslateAbstractRestService to get rest template based on Consumed Destination from standard outboundservices.
 * <p>
 * RestTemplate instances are cached per destination ID so that they are reused across calls for the same destination,
 * while different destinations each get their own properly-configured template. The cache is thread-safe.
 */
public abstract class CxTranslateOutboundAbstractRestService extends CxTranslateAbstractRestService {

	private IntegrationRestTemplateFactory integrationRestTemplateFactory;

	/**
	 * Cache of RestTemplate instances keyed by destination ID.
	 * This avoids recreating (and re-configuring) templates on every request while
	 * keeping templates isolated per destination.
	 */
	private final ConcurrentMap<String, RestTemplate> restTemplateCache = new ConcurrentHashMap<>();

	protected RestTemplate getRestTemplate(final ConsumedDestinationModel destination) {
		final String destinationId = destination.getId();
		return restTemplateCache.computeIfAbsent(destinationId, id -> createRestTemplate(destination));
	}

	/**
	 * Creates and configures a new RestTemplate for the given destination.
	 * Called at most once per destination ID (guarded by {@link ConcurrentMap#computeIfAbsent}).
	 */
	private RestTemplate createRestTemplate(final ConsumedDestinationModel destination) {
		final RestTemplate template = (RestTemplate) integrationRestTemplateFactory.create(destination);
		// Remove interceptors that are redundant or that will be added by setupRestTemplate
		template.getInterceptors()
				.removeIf(DefaultOutboundRequestResponseInterceptor.class::isInstance);

		setupRestTemplate(template);
		return template;
	}

	/**
	 * Evicts the cached RestTemplate for the given destination so the next call will recreate it.
	 * Useful when destination credentials or configuration change at runtime.
	 */
	protected void evictRestTemplate(final ConsumedDestinationModel destination) {
		restTemplateCache.remove(destination.getId());
	}

	/**
	 * Clears the entire RestTemplate cache. All subsequent calls will recreate templates.
	 */
	protected void evictAllRestTemplates() {
		restTemplateCache.clear();
	}

	@Override
	protected <REQUEST, RESPONSE> ResponseEntity<RESPONSE> getResponseEntity(
			final CxTranslateRestRequest<REQUEST, RESPONSE> restRequest, final ConsumedDestinationModel destination, final String url) {
		final HttpEntity<REQUEST> requestEntity = new HttpEntity<>(restRequest.getBody(), restRequest.getHeaders());
		final UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(url);
		restRequest.getQueryParams().forEach(urlBuilder::queryParam);
		final String finalUrl = urlBuilder.buildAndExpand(restRequest.getUrlPathParams()).toUriString();
		return getRestTemplate(destination).exchange(finalUrl, restRequest.getMethod(), requestEntity, restRequest.getResponseClass());
	}

	public void setIntegrationRestTemplateFactory(final IntegrationRestTemplateFactory integrationRestTemplateFactory) {
		this.integrationRestTemplateFactory = integrationRestTemplateFactory;
	}

}
