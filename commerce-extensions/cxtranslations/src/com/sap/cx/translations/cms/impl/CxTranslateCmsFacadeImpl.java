/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.cms.impl;

import com.sap.cx.translations.cms.CxTranslateCmsFacade;
import com.sap.cx.translations.cms.CxTranslateCmsService;
import com.sap.cx.translations.cms.CxTranslateCmsTree;
import com.sap.cx.translations.cms.data.CxCmsApproveRejectRequest;
import com.sap.cx.translations.cms.data.CxCmsApproveRejectResult;
import com.sap.cx.translations.cms.data.CxCmsLanguageEntry;
import com.sap.cx.translations.cms.data.CxCmsLocalizedValue;
import com.sap.cx.translations.cms.data.CxCmsServiceSettingsEntry;
import com.sap.cx.translations.cms.data.CxCmsTranslateRequest;
import com.sap.cx.translations.cms.data.CxCmsTranslationJob;
import com.sap.cx.translations.cms.data.CxCmsTranslationMetadata;
import com.sap.cx.translations.cms.data.CxCmsTranslationNode;
import com.sap.cx.translations.cms.data.CxCmsTranslationTreeResponse;
import com.sap.cx.translations.daos.CxProposedTranslationDao;
import com.sap.cx.translations.daos.CxTranslationServiceSettingsDao;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.model.CxProposedTranslationModel;
import com.sap.cx.translations.model.CxTranslationCronJobModel;
import com.sap.cx.translations.model.CxTranslationServiceSettingsModel;
import com.sap.cx.translations.services.CxApproveTranslationsService;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService;
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForValuesParameters;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.cms2.common.service.SessionSearchRestrictionsDisabler;
import de.hybris.platform.cms2.model.contents.CMSItemModel;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link CxTranslateCmsFacade}.
 *
 * <p>Resolves the requested CMS item by uid within the given catalog version, delegates the recursive tree
 * building to {@link CxTranslateCmsService} and converts the resulting {@link CxTranslateCmsTree} into OCC
 * DTOs.
 */
public class CxTranslateCmsFacadeImpl implements CxTranslateCmsFacade
{

	private static final Logger LOG = LoggerFactory.getLogger(CxTranslateCmsFacadeImpl.class);

	private ModelService modelService;
	private CxTranslateCmsService cxTranslateCmsService;
	private CatalogVersionService catalogVersionService;
	private FlexibleSearchService flexibleSearchService;
	private CommonI18NService commonI18NService;
	private I18NService i18NService;
	private SessionSearchRestrictionsDisabler sessionSearchRestrictionsDisabler;
	private CxLocalizedValueService localizedValueService;
	private CxTranslationServiceSettingsDao translationServiceSettingsDao;
	private CxProposedTranslationDao proposedTranslationDao;
	private CxApproveTranslationsService cxApproveTranslationsService;

	@Override
	public CxCmsTranslationTreeResponse getTranslationTree(final String uid, final String catalogId, final String catalogVersion,
	                                               final boolean includeTemplate)
	{
		if (StringUtils.isAnyBlank(uid, catalogId, catalogVersion))
		{
			throw new IllegalArgumentException("uid, catalogId and catalogVersion must not be blank");
		}

		final CatalogVersionModel catalogVersionModel = catalogVersionService.getCatalogVersion(catalogId, catalogVersion);

		// disable search restrictions so items in non-active (e.g. Staged) catalog versions are found too
		final CMSItemModel rootItem = sessionSearchRestrictionsDisabler.execute(() -> findCmsItem(uid, catalogVersionModel));
		if (rootItem == null)
		{
			throw new UnknownIdentifierException(
					"No CMS item found for uid [" + uid + "] in catalog version [" + catalogId + ":" + catalogVersion + "]");
		}

		final CxTranslateCmsTree tree = cxTranslateCmsService.buildTree(rootItem, includeTemplate);
		final CxCmsTranslationNode treeNode = convertNode(tree);
		final CxCmsTranslationMetadata metadata = buildMetadata();

		final CxCmsTranslationTreeResponse response = new CxCmsTranslationTreeResponse();
		response.setTree(treeNode);
		response.setMetadata(metadata);
		return response;
	}

	/**
	 * Builds the translation metadata: available languages (with session-localized names) and available
	 * service settings.
	 */
	protected CxCmsTranslationMetadata buildMetadata()
	{
		final List<CxCmsLanguageEntry> languages = commonI18NService.getAllLanguages().stream()
		                                                             .map(lang -> {
			                                                             final CxCmsLanguageEntry entry = new CxCmsLanguageEntry();
			                                                             entry.setIsocode(lang.getIsocode());
			                                                             entry.setName(lang.getName());
			                                                             return entry;
		                                                             })
		                                                             .toList();

		final List<CxCmsServiceSettingsEntry> serviceSettings = translationServiceSettingsDao.find().stream()
		                                                                                      .map(settings -> {
			                                                                                      final CxCmsServiceSettingsEntry entry = new CxCmsServiceSettingsEntry();
			                                                                                      entry.setCode(settings.getCode());
			                                                                                      entry.setName(settings.getName());
			                                                                                      return entry;
		                                                                                      })
		                                                                                      .toList();

		final CxCmsTranslationMetadata metadata = new CxCmsTranslationMetadata();
		metadata.setLanguages(languages);
		metadata.setServiceSettings(serviceSettings);
		return metadata;
	}

	@Override
	public CxCmsTranslationJob startTranslation(final CxCmsTranslateRequest request)
	{
		if (request == null || CollectionUtils.isEmpty(request.getValueKeys()))
		{
			throw new IllegalArgumentException("At least one value key must be provided");
		}
		if (StringUtils.isBlank(request.getSourceLanguage()))
		{
			throw new IllegalArgumentException("A source language must be provided");
		}
		if (CollectionUtils.isEmpty(request.getTargetLanguages()))
		{
			throw new IllegalArgumentException("At least one target language must be provided");
		}

		final LanguageModel sourceLanguage = commonI18NService.getLanguage(request.getSourceLanguage());
		final List<LanguageModel> targetLanguages = request.getTargetLanguages().stream()
		                                                   .filter(StringUtils::isNotBlank)
		                                                   .map(commonI18NService::getLanguage)
		                                                   .toList();

		final CxTranslationServiceSettingsModel serviceSettings = resolveServiceSettings(request.getServiceSettingsCode());

		// Resolve values without search restrictions so Staged/non-active catalog values are found.
		final List<CxLocalizedValueModel> values = sessionSearchRestrictionsDisabler.execute(
				() -> resolveValues(request.getValueKeys()));
		if (values.isEmpty())
		{
			throw new UnknownIdentifierException("No localized values found for the provided keys");
		}


		// Remove stale SmartEdit proposals only after the new cron job has been created successfully
		request.getValueKeys().stream().forEach(key -> {
			proposedTranslationDao.findByKey(key)
			                      .stream()
			                      .filter(CxProposedTranslationModel::isSmartEditTranslation)
			                      .forEach(modelService::remove);
		});

		final CxStartTranslationCronJobForValuesParameters parameters =
				CxStartTranslationCronJobForValuesParameters.builder()
				                                            .localizedValues(values)
				                                            .serviceSettings(serviceSettings)
				                                            .sourceLanguage(sourceLanguage)
				                                            .targetLanguages(targetLanguages)
				                                            .userHint(request.getHint())
				                                            .saveForApproval(Boolean.TRUE.equals(request.getSaveForApproval()))
				                                            .skipTranslated(request.getSkipTranslated())
				                                            .smartEditTranslation(true)
				                                            .build();


		final CxTranslationCronJobModel cronJob = localizedValueService.startTranslationCronJob(parameters);



		final CxCmsTranslationJob result = new CxCmsTranslationJob();
		result.setCronJobCode(cronJob.getCode());
		result.setValueCount(values.size());
		return result;
	}

	@Override
	public CxCmsApproveRejectResult approveTranslations(final CxCmsApproveRejectRequest request)
	{
		return processProposals(request, proposal -> cxApproveTranslationsService.approveTranslation(proposal));
	}

	@Override
	public CxCmsApproveRejectResult rejectTranslations(final CxCmsApproveRejectRequest request)
	{
		return processProposals(request, proposal -> {
			modelService.remove(proposal);
			return true;
		});
	}

	/**
	 * Resolves each requested proposed translation by its PK and applies the given operation to it,
	 * collecting a summary of successes and failures. Search restrictions are disabled so proposals in
	 * non-active (e.g. Staged) catalog versions are resolvable too.
	 */
	protected CxCmsApproveRejectResult processProposals(final CxCmsApproveRejectRequest request,
	                                                    final java.util.function.Predicate<CxProposedTranslationModel> operation)
	{
		if (request == null || CollectionUtils.isEmpty(request.getPks()))
		{
			throw new IllegalArgumentException("At least one PK must be provided");
		}

		final List<String> pks = request.getPks().stream().filter(StringUtils::isNotBlank).distinct().toList();

		final CxCmsApproveRejectResult result = new CxCmsApproveRejectResult();
		final List<String> failedPks = new ArrayList<>();
		final int[] succeeded = { 0 };

		sessionSearchRestrictionsDisabler.execute(() -> {
			for (final String pk : pks)
			{
				boolean ok = false;
				try
				{
					final CxProposedTranslationModel proposal = resolveProposedTranslation(pk);
					if (proposal != null)
					{
						ok = operation.test(proposal);
					}
					else
					{
						LOG.warn("No proposed translation found for PK [{}]", pk);
					}
				}
				catch (final Exception ex)
				{
					LOG.error("Failed to process proposed translation for PK [{}]: {}", pk, ex.getMessage(), ex);
				}
				if (ok)
				{
					succeeded[0]++;
				}
				else
				{
					failedPks.add(pk);
				}
			}
			return null;
		});

		result.setRequested(pks.size());
		result.setSucceeded(succeeded[0]);
		result.setFailed(failedPks.size());
		result.setFailedPks(failedPks);
		return result;
	}

	/**
	 * Resolves a {@link CxProposedTranslationModel} by its PK string representation, returning
	 * {@code null} when the PK is invalid or does not point to a proposed translation.
	 */
	protected CxProposedTranslationModel resolveProposedTranslation(final String pkString)
	{
		final PK pk;
		try
		{
			pk = PK.parse(pkString);
		}
		catch (final Exception ex)
		{
			LOG.warn("Invalid PK [{}]: {}", pkString, ex.getMessage());
			return null;
		}
		final Object model = modelService.get(pk);
		if (model instanceof CxProposedTranslationModel proposal)
		{
			return proposal;
		}
		LOG.warn("PK [{}] does not reference a CxProposedTranslation", pkString);
		return null;
	}

	/**
	 * Resolves the {@link CxTranslationServiceSettingsModel} to use: the one matching the provided code, or
	 * the default one, or the first available.
	 */
	protected CxTranslationServiceSettingsModel resolveServiceSettings(final String code)
	{
		final List<CxTranslationServiceSettingsModel> all = translationServiceSettingsDao.find();
		if (CollectionUtils.isEmpty(all))
		{
			throw new IllegalStateException("No CxTranslationServiceSettings configured");
		}
		if (StringUtils.isNotBlank(code))
		{
			return all.stream()
			          .filter(s -> code.equals(s.getCode()))
			          .findFirst()
			          .orElseThrow(() -> new UnknownIdentifierException(
					          "No CxTranslationServiceSettings found for code: " + code));
		}
		return all.stream()
		          .filter(s -> Boolean.TRUE.equals(s.getIsDefault()))
		          .findFirst()
		          .orElse(all.get(0));
	}

	/**
	 * Resolves the {@link CxLocalizedValueModel}s referenced by the given keys.
	 */
	protected List<CxLocalizedValueModel> resolveValues(final List<String> keys)
	{
		final Map<String, CxLocalizedValueModel> byKey = new LinkedHashMap<>();
		keys.stream()
		    .filter(StringUtils::isNotBlank)
		    .distinct()
		    .forEach(key -> localizedValueService.getForKey(key)
		                                         .forEach(value -> byKey.putIfAbsent(value.getKey(), value)));
		return new ArrayList<>(byKey.values());
	}

	/**
	 * Finds a single CMS item by uid within the given catalog version.
	 */
	protected CMSItemModel findCmsItem(final String uid, final CatalogVersionModel catalogVersion)
	{
		final FlexibleSearchQuery query = new FlexibleSearchQuery(
				"SELECT {" + CMSItemModel.PK + "} FROM {" + CMSItemModel._TYPECODE + "} WHERE {" + CMSItemModel.UID
						+ "} = ?uid AND {" + CMSItemModel.CATALOGVERSION + "} = ?catalogVersion");
		query.addQueryParameter("uid", uid);
		query.addQueryParameter("catalogVersion", catalogVersion);
		query.setCount(1);

		final List<CMSItemModel> result = flexibleSearchService.<CMSItemModel>search(query).getResult();
		return result.isEmpty() ? null : result.get(0);
	}

	/**
	 * Recursively converts a {@link CxTranslateCmsTree} node into its DTO representation.
	 */
	protected CxCmsTranslationNode convertNode(final CxTranslateCmsTree node)
	{
		if (node == null)
		{
			return null;
		}

		final CxCmsTranslationNode dto = new CxCmsTranslationNode();

		final CMSItemModel rootItem = node.getRootItem();
		if (rootItem != null)
		{
			dto.setUid(rootItem.getUid());
			dto.setItemType(rootItem.getItemtype());
		}

		final List<CxCmsLocalizedValue> values = new ArrayList<>();
		final List<CxCmsLocalizedValue> translatedValues = new ArrayList<>();
		if (node.getLocalizedValues() != null)
		{
			node.getLocalizedValues().forEach(value -> values.add(convertValue(value)));
			node.getLocalizedValues()
			    .stream()
			    .flatMap(value -> proposedTranslationDao.findByKey(value.getKey())
			                                            .stream()
			                                            .filter(CxProposedTranslationModel::isSmartEditTranslation)
			                                            .max(Comparator.comparing(
					                                            CxProposedTranslationModel::getModifiedtime))
			                                            .stream())
			    .forEach(proposedTranslationModel -> translatedValues.add(convertValue(proposedTranslationModel)));
		}
		dto.setLocalizedValues(values);
		dto.setTranslatedValues(translatedValues);

		final List<CxCmsTranslationNode> children = new ArrayList<>();
		if (node.getChildren() != null)
		{
			node.getChildren().forEach(child -> children.add(convertNode(child)));
		}
		dto.setChildren(children);

		return dto;
	}

	/**
	 * Converts a {@link CxLocalizedValueModel} into a {@link CxCmsLocalizedValue} DTO, exposing the most
	 * important identifying fields and the localized values as a {@code languageIso -> value} map.
	 */
	protected CxCmsLocalizedValue convertValue(final CxLocalizedValueModel value)
	{
		final CxCmsLocalizedValue dto = new CxCmsLocalizedValue();
		dto.setPk(value.getPk() != null ? value.getPk().toString() : null);
		dto.setCode(value.getKey());
		dto.setObjectId(value.getObjectId());
		dto.setAttribute(value.getAttribute());
		dto.setObjectType(value.getObjectType());
		dto.setValues(buildValuesMap(value));
		return dto;
	}

	protected CxCmsLocalizedValue convertValue(final CxProposedTranslationModel value)
	{
		final CxCmsLocalizedValue dto = new CxCmsLocalizedValue();
		dto.setPk(value.getPk() != null ? value.getPk().toString() : null);
		dto.setCode(value.getKey());
		dto.setObjectId(value.getObjectId());
		dto.setAttribute(value.getAttribute());
		dto.setObjectType(value.getObjectType());
		dto.setValues(buildValuesMap(value));
		dto.setStatus(value.getTranslationStatus().getCode());
		return dto;
	}

	/**
	 * Builds a map of {@code languageIsoCode -> localized value} for all configured languages.
	 */
	protected Map<String, String> buildValuesMap(final CxLocalizedValueModel value)
	{
		final Map<String, String> values = new LinkedHashMap<>();
		boolean previousFallbackState = i18NService.isLocalizationFallbackEnabled();
		try
		{
			// Disable language fallback
			i18NService.setLocalizationFallbackEnabled(false);
			localizedValueService.refresh(value);
			for (final LanguageModel language : commonI18NService.getAllLanguages())
			{
				final Locale locale = commonI18NService.getLocaleForLanguage(language);
				final String localizedValue = value.getValue(locale);
				values.put(language.getIsocode(), localizedValue);
			}
		}
		finally
		{
			i18NService.setLocalizationFallbackEnabled(previousFallbackState);
		}
		return values;
	}

	protected Map<String, String> buildValuesMap(final CxProposedTranslationModel value)
	{
		final Map<String, String> values = new LinkedHashMap<>();
		boolean previousFallbackState = i18NService.isLocalizationFallbackEnabled();
		try
		{
			// Disable language fallback
			i18NService.setLocalizationFallbackEnabled(false);
			modelService.refresh(value);
			for (final LanguageModel language : commonI18NService.getAllLanguages())
			{
				final Locale locale = commonI18NService.getLocaleForLanguage(language);
				final String localizedValue = value.getProposedTranslationValue(locale);
				values.put(language.getIsocode(), localizedValue);
			}
		}
		finally
		{
			i18NService.setLocalizationFallbackEnabled(previousFallbackState);
		}
		return values;
	}

	public void setCxTranslateCmsService(final CxTranslateCmsService cxTranslateCmsService)
	{
		this.cxTranslateCmsService = cxTranslateCmsService;
	}

	public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
	{
		this.catalogVersionService = catalogVersionService;
	}

	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

	public void setCommonI18NService(final CommonI18NService commonI18NService)
	{
		this.commonI18NService = commonI18NService;
	}

	public void setSessionSearchRestrictionsDisabler(final SessionSearchRestrictionsDisabler sessionSearchRestrictionsDisabler)
	{
		this.sessionSearchRestrictionsDisabler = sessionSearchRestrictionsDisabler;
	}

	public void setLocalizedValueService(final CxLocalizedValueService localizedValueService)
	{
		this.localizedValueService = localizedValueService;
	}

	public void setTranslationServiceSettingsDao(final CxTranslationServiceSettingsDao translationServiceSettingsDao)
	{
		this.translationServiceSettingsDao = translationServiceSettingsDao;
	}

	public void setI18NService(final I18NService i18NService)
	{
		this.i18NService = i18NService;
	}

	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	public void setProposedTranslationDao(final CxProposedTranslationDao proposedTranslationDao)
	{
		this.proposedTranslationDao = proposedTranslationDao;
	}

	public void setCxApproveTranslationsService(final CxApproveTranslationsService cxApproveTranslationsService)
	{
		this.cxApproveTranslationsService = cxApproveTranslationsService;
	}
}

