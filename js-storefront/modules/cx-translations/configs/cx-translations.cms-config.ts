/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

import { CMSComponentConfig, CmsConfig } from '@spartacus/core';
import { CxTranslationsAdminComponent } from '../components/cx-translations-admin/cx-translations-admin.component';

/**
 * Configuration of CMS components related to the CX Translations
 */
export const cxTranslationsCmsConfig: CmsConfig = {
  cmsComponents: {
    CxTranslationsAdminComponent: {
      component: CxTranslationsAdminComponent,
    },
  } as CMSComponentConfig,
};
