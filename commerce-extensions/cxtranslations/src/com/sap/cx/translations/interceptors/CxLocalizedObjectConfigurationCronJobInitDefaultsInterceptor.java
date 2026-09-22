/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cx.translations.interceptors;

import com.sap.cx.translations.model.CxLocalizedObjectConfigCronJobModel;
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService;
import de.hybris.platform.servicelayer.interceptor.InitDefaultsInterceptor;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;

/**
 * Prepare the list of included and excluded attributes and types for CxLocalizedObjectConfigCronJobModel based on spring configuration
 */
public class CxLocalizedObjectConfigurationCronJobInitDefaultsInterceptor implements InitDefaultsInterceptor<CxLocalizedObjectConfigCronJobModel> {


	private CxLocalizedValueConfigService cxLocalizedValueConfigService;

	@Override
	public void onInitDefaults(final CxLocalizedObjectConfigCronJobModel cronJob, final InterceptorContext interceptorContext) throws InterceptorException {
		cxLocalizedValueConfigService.configureLocalizedObjectConfigCronJob(cronJob);
	}

	public void setCxLocalizedValueConfigService(final CxLocalizedValueConfigService cxLocalizedValueConfigService) {
		this.cxLocalizedValueConfigService = cxLocalizedValueConfigService;
	}
}
