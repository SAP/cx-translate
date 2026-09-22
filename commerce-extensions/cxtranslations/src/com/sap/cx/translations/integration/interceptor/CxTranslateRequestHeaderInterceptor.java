/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.integration.interceptor;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Interceptor, which adds default header to the request
 */
public class CxTranslateRequestHeaderInterceptor implements ClientHttpRequestInterceptor {

	private final String headerName;
	private final String headerValue;

	public CxTranslateRequestHeaderInterceptor(
			final String headerName,
			final String headerValue) {
		this.headerName = headerName;
		this.headerValue = headerValue;
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
										final ClientHttpRequestExecution execution) throws IOException {
		request.getHeaders().set(headerName, headerValue);
		return execution.execute(request, body);
	}
}
