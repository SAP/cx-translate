/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.integration.handler;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Custom implementation of {@link ResponseErrorHandler}
 * It determinate what Http error codes are ignored to allow response processing.
 */
public class CxTranslateResponseErrorHandler extends DefaultResponseErrorHandler {

	private List<Integer> ignoredHttpErrorCodes = Collections.emptyList();

	@Override
	public void handleError(final ClientHttpResponse response) throws IOException {
		if (!ignoredHttpErrorCodes.contains(response.getStatusCode().value())) {
			super.handleError(response);
		}
	}

	public void setIgnoredHttpErrorCodes(final List<Integer> ignoredHttpErrorCodes) {
		this.ignoredHttpErrorCodes = ignoredHttpErrorCodes;
	}
}
