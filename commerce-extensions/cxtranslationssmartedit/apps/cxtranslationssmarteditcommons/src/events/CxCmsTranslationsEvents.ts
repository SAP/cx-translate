/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { CxCmsTranslationsTarget } from '../components/cmsTranslationsPopup/CxCmsTranslationsPopupComponent';

/**
 * Cross-frame event published by the SmartEditContainer (outer frame) and handled inside the
 * SmartEdit (inner iframe) app to open the shared CMS translations popup for a given CMS item.
 *
 * Used to trigger the popup from the perspective toolbar (for the current page) for the same
 * experience the component decorator offers for a selected component.
 */
export const CX_OPEN_CMS_TRANSLATIONS_EVENT = 'CX_OPEN_CMS_TRANSLATIONS';

/** Payload of the {@link CX_OPEN_CMS_TRANSLATIONS_EVENT} cross-frame event. */
export type CxOpenCmsTranslationsEvent = CxCmsTranslationsTarget;
