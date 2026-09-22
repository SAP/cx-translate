/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { OccConfig } from '@spartacus/core';

/**
 * Configuration of OCC endpoints related to the CX Translations
 */
export const cxTranslationsOccConfig: OccConfig = {
  backend: {
    occ: {
      endpoints: {
        getChunk: '/translations/chunks/${chunkName}?chunkLang=${lang}',
        getStagedChunk: '/translations/staged/chunks/${chunkName}?chunkLang=${lang}',
        uploadTranslations: '/translations/${lang}',
        editLabel: '/translations/labels',
        publishTranslations: '/translations/publish',
        labelKeySuggestions: '/translations/keySuggestions/${key}',
        labelValue: '/translations/labelValue/${key}',
        isAllowedToTranslate: '/translations/isAllowedToTranslate',
      },
    },
  },
} as OccConfig;
