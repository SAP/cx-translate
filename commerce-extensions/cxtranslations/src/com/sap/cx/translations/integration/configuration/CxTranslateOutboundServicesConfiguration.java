/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.integration.configuration;

import de.hybris.platform.outboundservices.config.DefaultOutboundServicesConfiguration;


/**
 * Override default outbound services configuration to provide custom timeout for translation service calls, as they can take longer time to process the request comparing to standard outboundservices use cases
 */
public class CxTranslateOutboundServicesConfiguration extends DefaultOutboundServicesConfiguration
{
	@Override
	public long getRequestExecutionTimeout()
	{
		return getConfigurationService().getConfiguration()
		                                .getLong("cxtranslate.outboundservices.request.execution.timeout.millisecs",
				                                1_200_000L);
	}
}
