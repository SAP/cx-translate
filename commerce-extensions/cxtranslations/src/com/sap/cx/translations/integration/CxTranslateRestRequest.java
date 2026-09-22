/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes HTTP Request to be sent.
 *
 * @param <REQUEST>  Request body tYPE (can be VOID)
 * @param <RESPONSE> Response type (can be VOID)
 */
public class CxTranslateRestRequest<REQUEST, RESPONSE> {
	private REQUEST body;
	private final HttpHeaders headers = new HttpHeaders();
	private final HttpMethod method;
	private final Map<String, String> urlPathParams = new HashMap<>();
	private final Map<String, String> queryParams = new HashMap<>();
	private final Class<RESPONSE> responseClass;

	/**
	 * Creates a new CxTranslateRestRequest.
	 *
	 * @param method        HTTP Method to be used.
	 * @param responseClass Response class
	 */
	public CxTranslateRestRequest(final HttpMethod method, final Class<RESPONSE> responseClass) {
		this.method = method;
		this.responseClass = responseClass;
	}

	/**
	 * Sets body of the Request.
	 *
	 * @param body Body of the request
	 * @return same instance for chaining calls
	 */
	public CxTranslateRestRequest<REQUEST, RESPONSE> setBody(final REQUEST body) {
		this.body = body;
		return this;
	}

	/**
	 * Adds Http Header
	 *
	 * @param name  Header name
	 * @param value Header value
	 * @return same instance for chaining calls
	 */
	public CxTranslateRestRequest<REQUEST, RESPONSE> addHttpHeader(final String name, final String value) {
		headers.add(name, value);
		return this;
	}

	/**
	 * Adds Path Param value.
	 *
	 * @param key   path param name
	 * @param value path param value
	 * @return same instance for chaining calls
	 */
	public CxTranslateRestRequest<REQUEST, RESPONSE> addPathParam(final String key, final String value) {
		urlPathParams.put(key, value);
		return this;
	}

	/**
	 * Adds query param
	 *
	 * @param key   Query param name
	 * @param value Query param value
	 * @return same instance for chaining calls
	 */
	public CxTranslateRestRequest<REQUEST, RESPONSE> addQueryParam(final String key, final String value) {
		queryParams.put(key, value);
		return this;
	}


	/**
	 * Gets request body
	 *
	 * @return request body
	 */
	public REQUEST getBody() {
		return body;
	}

	/**
	 * Gets HTTP Headers
	 *
	 * @return HTTP Headers Object
	 */
	public HttpHeaders getHeaders() {
		return headers;
	}

	/**
	 * Gets HTTP Method
	 *
	 * @return HTTP Method
	 */
	public HttpMethod getMethod() {
		return method;
	}

	/**
	 * Gets Path Params
	 *
	 * @return Path Params
	 */
	public Map<String, String> getUrlPathParams() {
		return urlPathParams;
	}

	/**
	 * Gets Query Params
	 *
	 * @return Query Params
	 */
	public Map<String, String> getQueryParams() {
		return queryParams;
	}

	/**
	 * Gets Response Class
	 *
	 * @return Response Class
	 */
	public Class<RESPONSE> getResponseClass() {
		return responseClass;
	}
}
