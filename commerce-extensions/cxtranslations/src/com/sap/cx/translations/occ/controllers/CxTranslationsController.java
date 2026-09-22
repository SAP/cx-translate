/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.occ.controllers;

import com.sap.cx.translations.cms.CxTranslateCmsFacade;
import com.sap.cx.translations.cms.data.CxCmsApproveRejectRequest;
import com.sap.cx.translations.cms.data.CxCmsApproveRejectResult;
import com.sap.cx.translations.cms.data.CxCmsTranslateRequest;
import com.sap.cx.translations.cms.data.CxCmsTranslationJob;
import com.sap.cx.translations.cms.data.CxCmsTranslationTreeResponse;
import com.sap.cx.translations.exceptions.CxTranslationException;
import com.sap.cx.translations.facades.CxTranslationsFacade;
import com.sap.cx.translations.labels.CxLocalizedLabel;
import com.sap.cx.translations.labels.UsageType;
import de.hybris.platform.commercewebservicescommons.annotation.SecurePortalUnauthenticatedAccess;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import jakarta.annotation.Resource;
import jakarta.ws.rs.Produces;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for serving translation bundles/chunks for JS storefront
 */
@RestController
@RequestMapping(value = "/{baseSiteId}/translations")
public class CxTranslationsController {

	private static final Logger LOG = LoggerFactory.getLogger(CxTranslationsController.class);

	@Resource(name = "cxTranslationsFacade")
	private CxTranslationsFacade translationsFacade;

	@Resource(name = "cxTranslateCmsFacade")
	private CxTranslateCmsFacade cmsTranslateFacade;

	@Resource
	private CommonI18NService commonI18NService;

	@Produces("application/json")
	@GetMapping("/chunks/{namespace}")
	@SecurePortalUnauthenticatedAccess
	public Map<String, String> getTranslations(@PathVariable("namespace") final String namespace, @RequestParam(name = "chunkLang") final String chunkLanguageCode) throws CxTranslationException {
		applySessionLanguage(chunkLanguageCode);
		return translationsFacade.getLabelsMap(namespace, UsageType.FRONTEND);
	}

	@Produces("application/json")
	@GetMapping("/staged/chunks/{namespace}")
	@Secured({"ROLE_CMSTRANSLATORGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_FRONTENDTRANSLATORGROUP"})
	public Map<String, String> getStagedTranslations(@PathVariable("namespace") final String namespace, @RequestParam(name = "chunkLang") final String chunkLanguageCode) throws CxTranslationException {
		// catalog switch handled by filter
		applySessionLanguage(chunkLanguageCode);
		return translationsFacade.getLabelsMap(namespace, false, UsageType.FRONTEND);
	}

	@PostMapping("/labels")
	@Secured({"ROLE_CMSTRANSLATORGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_FRONTENDTRANSLATORGROUP"})
	public void editLabel(@RequestBody final CxLocalizedLabel label) throws CxTranslationException {
		if (label == null || StringUtils.isBlank(label.getKey()) || StringUtils.isBlank(label.getBundle())) {
			throw new CxTranslationException("Label must have a non-blank key and bundle");
		}
		try {
			translationsFacade.editLabel(label);
		} catch (final Exception ex) {
			throw new CxTranslationException("Editing label failed!", ex.getMessage(), ex);
		}
	}

	@PostMapping("/import/{lang}")
	@Secured({"ROLE_CMSTRANSLATORGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_FRONTENDTRANSLATORGROUP"})
	public void importTranslations(@PathVariable("lang") final String lang, @RequestBody final Map<String, Object> translations) throws CxTranslationException {
		if (StringUtils.isBlank(lang)) {
			throw new CxTranslationException("Language code must not be blank");
		}
		if (translations == null) {
			throw new CxTranslationException("Translations payload must not be null");
		}
		final Map<String, Map<String, String>> mappedJson = new HashMap<>();
		for (final Map.Entry<String, Object> entry : translations.entrySet()) {
			if (entry.getValue() instanceof Map) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> bundleMap = (Map<String, Object>) entry.getValue();
				mappedJson.put(entry.getKey(), flatMap(null, bundleMap));
			} else {
				throw new CxTranslationException("Invalid translation format: expected nested map for namespace '" + entry.getKey() + "'");
			}
		}

		try {
			translationsFacade.importLabels(mappedJson, lang);
		} catch (final Exception ex) {
			throw new CxTranslationException("Importing translations failed!", ex.getMessage(), ex);
		}
	}

	@PostMapping("/publish")
	@Secured({"ROLE_CMSTRANSLATORGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_FRONTENDTRANSLATORGROUP"})
	public void publishTranslations() {

		final boolean result;
		try {
			result = translationsFacade.publishTranslations();
		} catch (final Exception ex) {
			throw new CxTranslationException("Publishing labels translations failed!", ex.getMessage(), ex);
		}

		if (!result) {
			throw new CxTranslationException("Publishing labels translations failed!");
		}
	}

	@ResponseStatus(value = HttpStatus.OK)
	@GetMapping(value = "/labelValue/{key}", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
	@ResponseBody
	@Secured({"ROLE_CMSTRANSLATORGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_FRONTENDTRANSLATORGROUP"})
	public String labelValue(@PathVariable("key") final String key) {

		final String result;
		try {
			result = translationsFacade.getStagedLabel(key, UsageType.FRONTEND);
		} catch (final Exception ex) {
			throw new CxTranslationException("Label value retrieval failed!", ex.getMessage(), ex);
		}
		return StringUtils.defaultString(result);
	}

	@GetMapping("/keySuggestions/{key}")
	@Secured({"ROLE_CMSTRANSLATORGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_FRONTENDTRANSLATORGROUP"})
	public List<String> keySuggestions(@PathVariable("key") final String key) {
		final List<String> result;
		try {
			result = translationsFacade.getKeySuggestions(key);
		} catch (final Exception ex) {
			throw new CxTranslationException("Key suggestions retrieval failed!", ex.getMessage(), ex);
		}
		return result;
	}
	
	@GetMapping("/isAllowedToTranslate")
	@SecurePortalUnauthenticatedAccess
	@ApiBaseSiteIdAndUserIdParam
	public Boolean isAllowedToTranslate() {		Boolean result = false;
		try {
			result = translationsFacade.isFrontendTranslationAllowedForUser();
		} catch (final Exception ex) {
			throw new CxTranslationException("Has access call failed!", ex.getMessage(), ex);
		}
		return result;
	}

	/**
	 * Resolves the language by ISO code and applies it as the session current language.
	 * Wraps the platform's unchecked exception in a {@link CxTranslationException} so callers
	 * receive a structured error rather than a generic 500.
	 */
	private void applySessionLanguage(final String chunkLanguageCode) throws CxTranslationException {
		if (StringUtils.isBlank(chunkLanguageCode)) {
			throw new CxTranslationException("Language code must not be blank");
		}
		final LanguageModel language;
		try {
			language = commonI18NService.getLanguage(chunkLanguageCode);
		} catch (final UnknownIdentifierException ex) {
			throw new CxTranslationException("Unknown language code: " + chunkLanguageCode, ex.getMessage(), ex);
		}
		commonI18NService.setCurrentLanguage(language);
	}

	private static Map<String, String> flatMap(final String parentKey, final Map<String, Object> nestedMap) {
		final Map<String, String> result = new HashMap<>();
		final String prefixKey = parentKey != null ? parentKey + "." : "";
		for (final Map.Entry<String, Object> entry : nestedMap.entrySet()) {
			final Object value = entry.getValue();
			if (value instanceof String) {
				result.put(prefixKey + entry.getKey(), (String) value);
			} else if (value instanceof Map) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> child = (Map<String, Object>) value;
				result.putAll(flatMap(prefixKey + entry.getKey(), child));
			} else {
				LOG.warn("Skipping translation entry '{}{}' with unsupported value type: {}",
						prefixKey, entry.getKey(), value == null ? "null" : value.getClass().getName());
			}
		}
		return result;
	}
}