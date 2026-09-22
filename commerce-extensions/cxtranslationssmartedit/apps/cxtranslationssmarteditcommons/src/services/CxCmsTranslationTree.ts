/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

/**
 * Runtime shape of the localized values map.
 *
 * Note: SAP Commerce webservices serialize a `Map` as `{ "entry": [ { "key", "value" } ] }`,
 * so at runtime this may be either a plain map object or that entry-array wrapper.
 */
export type CxCmsValueMap =
    | { [languageIso: string]: string }
    | { entry: Array<{ key: string; value: string }> };

/**
 * A single localized value of a CMS item exposed by the `/translations/cms/tree` OCC endpoint.
 */
export interface CxCmsLocalizedValue {
    /** String representation of the underlying item PK (used for approval / rejection). */
    pk: string;
    /** Unique key/code of the localized value. */
    code: string;
    /** Id of the localized object the value belongs to. */
    objectId: string;
    /** Qualifier of the translated attribute. */
    attribute: string;
    /** Type of the localized object. */
    objectType: string;
    /** Localized values keyed by language iso code (see {@link CxCmsValueMap}). */
    values: CxCmsValueMap;
    status: string;
}

/**
 * A node of the recursive CMS translation tree.
 */
export interface CxCmsTranslationNode {
    /** uid of the CMS item. */
    uid: string;
    /** Item type of the CMS item. */
    itemType: string;
    /** Localized values of the CMS item. */
    localizedValues: CxCmsLocalizedValue[];
    translatedValues: CxCmsLocalizedValue[];
    /** Child CMS items referenced by this item. */
    children: CxCmsTranslationNode[];
}

/**
 * A single language entry returned as part of the translation tree metadata.
 */
export interface CxCmsLanguageEntry {
    /** ISO code (e.g. `en`, `de`). */
    isocode: string;
    /** Session-localized display name (e.g. `English`, `Deutsch`). */
    name: string;
}

/**
 * A single translation service settings entry returned as part of the translation tree metadata.
 */
export interface CxCmsServiceSettingsEntry {
    /** Unique code of the settings object. */
    code: string;
    /** Display name. */
    name: string;
}

/**
 * Metadata returned alongside the CMS translation tree.
 */
export interface CxCmsTranslationMetadata {
    /** All configured languages with their display names. */
    languages: CxCmsLanguageEntry[];
    /** All configured translation service settings objects. */
    serviceSettings: CxCmsServiceSettingsEntry[];
}

/**
 * Top-level response from the `/translations/cms/tree` OCC endpoint.
 */
export interface CxCmsTranslationTreeResponse {
    /** The recursive CMS translation tree. */
    tree: CxCmsTranslationNode;
    /** Metadata: available languages and service settings. */
    metadata: CxCmsTranslationMetadata;
}

/**
 * Request to start an AI translation cron job for selected localized values.
 */
export interface CxCmsTranslateRequest {
    /** Keys (codes) of the localized values to translate. */
    valueKeys: string[];
    /** Source language iso code. */
    sourceLanguage: string;
    /** Target language iso codes. */
    targetLanguages: string[];
    /** Optional translation hint. */
    hint?: string;
    /** Optional code of the translation service settings to use (default is used when omitted). */
    serviceSettingsCode?: string;
    /** Whether translations should be saved for approval. */
    saveForApproval?: boolean;
    /** Whether already-translated values should be skipped. */
    skipTranslated?: boolean;
}

/**
 * Result of starting a translation cron job.
 */
export interface CxCmsTranslationJob {
    /** Code of the started cron job. */
    cronJobCode: string;
    /** Number of localized values submitted for translation. */
    valueCount: number;
}

/**
 * Request to approve or reject proposed translations, identified by their item PKs.
 */
export interface CxCmsApproveRejectRequest {
    /** PKs of the proposed translations to approve / reject. */
    pks: string[];
}

/**
 * Result of an approve / reject operation.
 */
export interface CxCmsApproveRejectResult {
    /** Number of proposals requested to be processed. */
    requested: number;
    /** Number of proposals processed successfully. */
    succeeded: number;
    /** Number of proposals that could not be processed. */
    failed: number;
    /** PKs that could not be processed. */
    failedPks: string[];
}

