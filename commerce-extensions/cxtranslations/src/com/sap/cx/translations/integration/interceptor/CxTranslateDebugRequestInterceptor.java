/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.integration.interceptor;

import de.hybris.platform.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Interceptor for RestTemplate to handle request and response to log them for debugging purposes
 */
public class CxTranslateDebugRequestInterceptor implements ClientHttpRequestInterceptor {
	private static final Logger LOG = LoggerFactory.getLogger(CxTranslateDebugRequestInterceptor.class);

	private static final String DEBUG_MODE = "cxtranslations.integrations.debug";
	private static final String REQUEST_LOG = "== REQUEST ID: {} [{}] == || URI: {} || Method: {} || Headers: {} || Body: {}";
	private static final String RESPONSE_LOG = "== RESPONSE ID: {} [{}] == || Status code: {} || Status text: {} || Headers: {} || Body: {}";

	private final String interfaceId;

	public CxTranslateDebugRequestInterceptor(
			final String interfaceId) {
		this.interfaceId = interfaceId;
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest httpRequest, final byte[] bytes,
										final ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {

		final boolean debug = Config.getBoolean(DEBUG_MODE, false);

		String id = null;
		if (debug) {
			id = UUID.randomUUID().toString();
			logRequest(id, httpRequest, bytes);
		}

		final ClientHttpResponse httpResponse = clientHttpRequestExecution.execute(httpRequest, bytes);

		if (debug) {
			final String responseBody;
			try {
				responseBody = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
			} catch (final IOException e) {
				httpResponse.close();
				throw e;
			}
			logResponse(id, httpResponse, responseBody);

			// wrapping response with consumed body
			return new CxTranslateClientHttpResponseWrapper(httpResponse,
					new ByteArrayInputStream(responseBody.getBytes(Charset.defaultCharset())));
		} else {
			return httpResponse;
		}
	}

	private void logRequest(final String id, final HttpRequest request, final byte[] body) throws IOException {
		final String requestBody = new String(body, Charset.defaultCharset());
		LOG.info(REQUEST_LOG, id, interfaceId, request.getURI(), request.getMethod(), request.getHeaders(),
				requestBody);
	}

	private void logResponse(final String id, final ClientHttpResponse response, final String body) throws IOException {
		LOG.info(RESPONSE_LOG, id, interfaceId, response.getStatusCode(), response.getStatusText(),
				response.getHeaders(), body);
	}

	/**
	 * Wrapper for {@link ClientHttpResponse} with copy of consumed body
	 */
	private static class CxTranslateClientHttpResponseWrapper implements ClientHttpResponse {
		private final ClientHttpResponse response;
		private final InputStream body;

		public CxTranslateClientHttpResponseWrapper(
				final ClientHttpResponse response,
				final InputStream body) {
			this.response = response;
			this.body = body;
		}

		@Override
		public InputStream getBody() throws IOException {
			return body;
		}

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return HttpStatus.resolve(response.getStatusCode().value());
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return response.getRawStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return response.getStatusText();
		}

		@Override
		public void close() {
			response.close();
		}

		@Override
		public HttpHeaders getHeaders() {
			return response.getHeaders();
		}
	}
}
