/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.smartedit.controllers;

import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cx.translations.cms.CxTranslateCmsFacade;
import com.sap.cx.translations.cms.data.CxCmsApproveRejectRequest;
import com.sap.cx.translations.cms.data.CxCmsApproveRejectResult;
import com.sap.cx.translations.cms.data.CxCmsTranslateRequest;
import com.sap.cx.translations.cms.data.CxCmsTranslationJob;
import com.sap.cx.translations.cms.data.CxCmsTranslationTreeResponse;
import com.sap.cx.translations.exceptions.CxTranslationException;

import jakarta.annotation.Resource;

/**
 * SmartEdit web controller exposing CMS translation endpoints within the cxtranslationssmartedit webapp.
 * Mirrors the CMS-specific methods from CxTranslationsController, delegating to the shared facade.
 */
@RestController
@RequestMapping(value = "/v1/sites/{baseSiteId}/cxtranslations/cms", produces = MediaType.APPLICATION_JSON_VALUE)
public class CxCmsTranslationsController
{

    @Resource(name = "cxTranslateCmsFacade")
    private CxTranslateCmsFacade cmsTranslateFacade;

	@Secured({"ROLE_CMSTRANSLATORGROUP","ROLE_ADMINGROUP","ROLE_CXTRANSLATIONSTRANSLATORGROUP" , "ROLE_TRUSTED_CLIENT"})
	@GetMapping("/tree")
    public CxCmsTranslationTreeResponse getCmsTranslationTree(
            @RequestParam("uid") final String uid,
            @RequestParam("catalogId") final String catalogId,
            @RequestParam("catalogVersion") final String catalogVersion,
            @RequestParam(value = "includeTemplate", defaultValue = "false") final boolean includeTemplate) {
        try {
            return cmsTranslateFacade.getTranslationTree(uid, catalogId, catalogVersion, includeTemplate);
        } catch (final CxTranslationException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CxTranslationException("CMS translation tree retrieval failed!", ex.getMessage(), ex);
        }
    }

	@Secured({"ROLE_CMSTRANSLATORGROUP","ROLE_ADMINGROUP","ROLE_CXTRANSLATIONSTRANSLATORGROUP" , "ROLE_TRUSTED_CLIENT"})
	@PostMapping("/translate")
    public CxCmsTranslationJob translateCmsValues(@RequestBody final CxCmsTranslateRequest request) {
        if (request == null) {
            throw new CxTranslationException("Translation request must not be null");
        }
        try {
            return cmsTranslateFacade.startTranslation(request);
        } catch (final CxTranslationException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CxTranslationException("Starting CMS translation failed!", ex.getMessage(), ex);
        }
    }

	@Secured({"ROLE_CMSTRANSLATORGROUP","ROLE_ADMINGROUP","ROLE_CXTRANSLATIONSTRANSLATORGROUP" , "ROLE_TRUSTED_CLIENT"})
	@PostMapping("/approve")
    public CxCmsApproveRejectResult approveCmsTranslations(@RequestBody final CxCmsApproveRejectRequest request) {
        if (request == null) {
            throw new CxTranslationException("Approval request must not be null");
        }
        try {
            return cmsTranslateFacade.approveTranslations(request);
        } catch (final CxTranslationException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CxTranslationException("Approving CMS translations failed!", ex.getMessage(), ex);
        }
    }

	@Secured({"ROLE_CMSTRANSLATORGROUP","ROLE_ADMINGROUP","ROLE_CXTRANSLATIONSTRANSLATORGROUP" , "ROLE_TRUSTED_CLIENT"})
	@PostMapping("/reject")
    public CxCmsApproveRejectResult rejectCmsTranslations(@RequestBody final CxCmsApproveRejectRequest request) {
        if (request == null) {
            throw new CxTranslationException("Rejection request must not be null");
        }
        try {
            return cmsTranslateFacade.rejectTranslations(request);
        } catch (final CxTranslationException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CxTranslationException("Rejecting CMS translations failed!", ex.getMessage(), ex);
        }
    }
}
