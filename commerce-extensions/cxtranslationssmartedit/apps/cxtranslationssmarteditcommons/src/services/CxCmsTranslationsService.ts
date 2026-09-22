/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { Injectable } from '@angular/core';
import { IRestServiceFactory } from 'smarteditcommons';
import {
    CxCmsApproveRejectRequest,
    CxCmsApproveRejectResult,
    CxCmsTranslateRequest,
    CxCmsTranslationJob,
    CxCmsTranslationTreeResponse
} from './CxCmsTranslationTree';

const CMS_TRANSLATION_TREE_URI = '/cmssmarteditwebservices/v1/sites/:baseSiteId/cxtranslations/cms/tree';
const CMS_TRANSLATE_URI = '/cmssmarteditwebservices/v1/sites/:baseSiteId/cxtranslations/cms/translate';
const CMS_APPROVE_URI = '/cmssmarteditwebservices/v1/sites/:baseSiteId/cxtranslations/cms/approve';
const CMS_REJECT_URI = '/cmssmarteditwebservices/v1/sites/:baseSiteId/cxtranslations/cms/reject';

@Injectable()
export class CxCmsTranslationsService {
    constructor(private readonly restServiceFactory: IRestServiceFactory) {}

    /**
     * Calls the `/cms/tree` endpoint and returns the CMS translation tree
     * together with metadata (languages, service settings).
     *
     * @param baseSiteId the base site id
     * @param uid the uid of the CMS item
     * @param catalogId the content catalog id
     * @param catalogVersion the content catalog version (e.g. Staged / Online)
     * @param includeTemplate whether to include template content slots in the tree
     */
    public getCmsTranslationTree(
        baseSiteId: string,
        uid: string,
        catalogId: string,
        catalogVersion: string,
        includeTemplate: boolean = false
    ): Promise<CxCmsTranslationTreeResponse> {
        const uri = CMS_TRANSLATION_TREE_URI.replace(':baseSiteId', baseSiteId);
        return this.restServiceFactory
            .get<CxCmsTranslationTreeResponse>(uri)
            .get({ uid, catalogId, catalogVersion, includeTemplate });
    }

    /**
     * Starts an AI translation cron job (in the background) for the given localized values.
     *
     * @param baseSiteId the base site id
     * @param request the translation request
     */
    public startTranslation(
        baseSiteId: string,
        request: CxCmsTranslateRequest
    ): Promise<CxCmsTranslationJob> {
        const uri = CMS_TRANSLATE_URI.replace(':baseSiteId', baseSiteId);
        const service = this.restServiceFactory.get<CxCmsTranslationJob>(uri);
        // `save` expects a `Payload` (index-signature) type; the request is cast to that parameter type.
        return service.save(request as unknown as Parameters<typeof service.save>[0]);
    }

    /**
     * Approves the proposed translations identified by the given PKs (synchronously on the backend).
     *
     * @param baseSiteId the base site id
     * @param pks the PKs of the proposed translations to approve
     */
    public approveTranslations(
        baseSiteId: string,
        pks: string[]
    ): Promise<CxCmsApproveRejectResult> {
        return this.postApproveReject(CMS_APPROVE_URI, baseSiteId, pks);
    }

    /**
     * Rejects (removes) the proposed translations identified by the given PKs.
     *
     * @param baseSiteId the base site id
     * @param pks the PKs of the proposed translations to reject
     */
    public rejectTranslations(
        baseSiteId: string,
        pks: string[]
    ): Promise<CxCmsApproveRejectResult> {
        return this.postApproveReject(CMS_REJECT_URI, baseSiteId, pks);
    }

    private postApproveReject(
        uriTemplate: string,
        baseSiteId: string,
        pks: string[]
    ): Promise<CxCmsApproveRejectResult> {
        const uri = uriTemplate.replace(':baseSiteId', baseSiteId);
        const service = this.restServiceFactory.get<CxCmsApproveRejectResult>(uri);
        const request: CxCmsApproveRejectRequest = { pks };
        return service.save(request as unknown as Parameters<typeof service.save>[0]);
    }
}

