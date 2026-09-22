/*
* Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */

import com.sap.cx.translations.services.config.CxLocalizedValueConfigService

CxLocalizedValueConfigService cxLocalizedValueConfigService = (CxLocalizedValueConfigService) spring.getBean("cxLocalizedValueConfigService")

cxLocalizedValueConfigService.autoConfigure(true)