/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.localizedvalue;

import com.hybris.cockpitng.core.events.CockpitEvent;
import com.hybris.cockpitng.search.data.SearchQueryCondition;
import com.hybris.cockpitng.search.data.SearchQueryData;
import com.hybris.cockpitng.search.data.SortData;
import com.hybris.cockpitng.search.data.ValueComparisonOperator;
import com.hybris.cockpitng.search.data.pageable.Pageable;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.services.CxTranslationsNotification;
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService;
import com.sap.cx.translations.services.localizedobjects.CxTranslationsPageable;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.*;

/**
 * Adapter class to wrap {@link CxTranslationsPageable} into {@link Pageable} used by Bakoffice.
 */
public final class CxTranslationsPageableAdapter implements Pageable<CxLocalizedValueModel> {
	private final SearchQueryData searchQueryData;
	private final CxLocalizedValueService localizedValueService;
	private CxTranslationsPageable<CxLocalizedValueModel> cxTranslationsPageable;
	private final NotificationService notificationService;
	private final List<CxTranslationsNotification> notifications = new ArrayList<>();
	int pageSize;
	private final Map<Integer, List<CxLocalizedValueModel>> alreadyLoadedCache = new ConcurrentHashMap<>();
    private final Set<String> forceUsingConfigs;

	public CxTranslationsPageableAdapter(final SearchQueryData searchQueryData, final CxLocalizedValueService localizedValueService, final NotificationService notificationService, final Set<String> forceUsingConfigs) {
		this.searchQueryData = searchQueryData;
		this.localizedValueService = localizedValueService;
		this.pageSize = searchQueryData.getPageSize();
		this.notificationService = notificationService;
        this.forceUsingConfigs = forceUsingConfigs;
        refresh();
	}

	@Override
	public void refresh() {
		// Invalidate the per-page cache so callers don't see stale results after a sort/filter/pageSize change.
		alreadyLoadedCache.clear();
		notifications.clear();
		final List<SearchCriteria> criteria = buildCriteria(searchQueryData);

		final CxLocalizedValueSearchData searchData = new CxLocalizedValueSearchData(SortField.CODE, StringUtils.isBlank(searchQueryData.getSortData()
																																		.getSortAttribute()) || searchQueryData.getSortData()
																																											   .isAscending() ? SortDirection.Ascending : SortDirection.Descending, criteria, this.pageSize, this.forceUsingConfigs, false);

		this.cxTranslationsPageable = localizedValueService.search(searchData);

		this.notifications.stream()
						  .distinct()
						  .forEach(msg -> this.notificationService.notifyUser("cxtranslations", msg.getBackofficeMessageKey(), getBackofficeLevel(msg.getLevel()), msg.getParameters()
																																									  .toArray()));

		this.cxTranslationsPageable.getNotifications()
								   .stream()
								   .distinct()
								   .forEach(msg -> this.notificationService.notifyUser("cxtranslations", msg.getBackofficeMessageKey(), getBackofficeLevel(msg.getLevel()), msg.getParameters()
																																											   .toArray()));

	}

	private NotificationEvent.Level getBackofficeLevel(final CxTranslationsNotification.Level level) {
		return switch (level) {
			case Info -> NotificationEvent.Level.INFO;
			case Error -> NotificationEvent.Level.FAILURE;
			case Warning -> NotificationEvent.Level.WARNING;
			default -> NotificationEvent.Level.INFO;
		};
	}

	private List<SearchCriteria> buildCriteria(final SearchQueryData searchQueryData) {
		return searchQueryData.getConditions().stream().map(this::getSearchCriteria).filter(Objects::nonNull).toList();
	}

	private SearchCriteria getSearchCriteria(final SearchQueryCondition searchQueryCondition) {
		final SearchField attributeName = getAttributeName(searchQueryCondition);
		if (attributeName == null) {
			return null;
		}
		final Object value = searchQueryCondition.getValue();
		final boolean hasContent = value != null && (value instanceof String ? StringUtils.isNotBlank(String.valueOf(value)) : isMapWithNonEmptyValue(value));
		final ValueComparisonOperator operator = searchQueryCondition.getOperator();
		final String stringValue = String.valueOf(value);

		return getSearchCriteria(attributeName, hasContent, operator, stringValue, value);
	}

	private SearchCriteria getSearchCriteria(final SearchField attributeName, final boolean hasContent, final ValueComparisonOperator operator, final String stringValue, final Object value) {
		switch (attributeName) {
			case KEY:
				return buildOnlyEqualsCriteria(hasContent, operator, attributeName, stringValue, CxLocalizedValueModel.KEY);
			case PK:
				return buildOnlyEqualsCriteria(hasContent, operator, attributeName, stringValue, CxLocalizedValueModel.PK);
			case CATALOG_ID:
				return buildWithALlCriteria(hasContent, operator, attributeName, stringValue, CxLocalizedValueModel.CATALOGID);
			case CATALOG_VERSION_ID:
				return buildWithALlCriteria(hasContent, operator, attributeName, stringValue, CxLocalizedValueModel.CATALOGVERSIONID);
			case OBJECT_ID:
				return buildWithALlCriteria(hasContent, operator, attributeName, stringValue, CxLocalizedValueModel.OBJECTID);
			case OBJECT_TYPE:
				return buildWithALlCriteria(hasContent, operator, attributeName, stringValue, CxLocalizedValueModel.OBJECTTYPE);
			case ATTRIBUTE:
				return buildWithALlCriteria(hasContent, operator, attributeName, stringValue, CxLocalizedValueModel.ATTRIBUTE);
			case VALUE:
				return buildWithCriteriaForValue(value, hasContent, operator, attributeName);
			default:
				return null;
		}
	}

	private static boolean isMapWithNonEmptyValue(final Object value) {
		return value instanceof Map && StringUtils.isNotBlank(String.valueOf(((Map) value).values()
				.stream()
				.filter(Objects::nonNull)
				.findFirst()
				.orElse("")));
	}

	private SearchCriteria buildWithCriteriaForValue(final Object value, final boolean hasContent, final ValueComparisonOperator operator, final SearchField attributeName) {
		final StringWithLocale stringWithLocale = takeValueAndLocaleFromMap(value);

		final boolean allowNoValue = operator == ValueComparisonOperator.IS_EMPTY || operator == ValueComparisonOperator.IS_NOT_EMPTY;

		if (!hasContent && !allowNoValue) {
			return null;
		}

		if (stringWithLocale == null) {
			return null;
		}
		return switch (operator) {
			case EQUALS -> new SearchCriteria(attributeName, SearchOperator.EQUALS, stringWithLocale);
			case STARTS_WITH -> new SearchCriteria(attributeName, SearchOperator.STARTS_WITH, stringWithLocale);
			case ENDS_WITH -> new SearchCriteria(attributeName, SearchOperator.ENDS_WITH, stringWithLocale);
			case LIKE, CONTAINS -> new SearchCriteria(attributeName, SearchOperator.LIKE, stringWithLocale);
			case IS_EMPTY -> new SearchCriteria(attributeName, SearchOperator.IS_EMPTY, stringWithLocale);
			case IS_NOT_EMPTY -> new SearchCriteria(attributeName, SearchOperator.IS_NOT_EMPTY, stringWithLocale);

			default -> {
				notifications.add(CxTranslationsNotification.notSupportedOperator("Value", List.of("EQUALS", "STARTS_WITH", "ENDS_WITH", "LIKE", "CONTAINS", "IS_EMPTY", "IS_NOT_EMPTY")));
				yield null;
			}
		};
	}

	private static StringWithLocale takeValueAndLocaleFromMap(final Object value) {
		if (value instanceof final Map map) {
			if (map.size() != 1) {
				return null;
			}
			final Map.Entry entry = (Map.Entry) map.entrySet().stream().findFirst().orElse(null);

			if (entry != null && entry.getKey() instanceof final Locale locale) {
				return new StringWithLocale((String) entry.getValue(), locale);
			}
		}
		return null;
	}

	private SearchCriteria buildOnlyEqualsCriteria(final boolean hasContent, final ValueComparisonOperator operator, final SearchField attributeName, final String value, final String fieldName) {
		if (!hasContent) {
			return null;
		}
		return switch (operator) {
			case EQUALS -> new SearchCriteria(attributeName, SearchOperator.EQUALS, new StringWithLocale(value, null));
			default -> {
				notifications.add(CxTranslationsNotification.notSupportedOperator(fieldName, List.of("EQUALS")));
				yield null;
			}
		};
	}

	
	private SearchCriteria buildWithALlCriteria(final boolean hasContent, final ValueComparisonOperator operator, final SearchField attributeName, final String value, final String fieldName) {
		return switch (operator) {
			case EQUALS -> new SearchCriteria(attributeName, SearchOperator.EQUALS, new StringWithLocale(value, null));
			case STARTS_WITH ->
					new SearchCriteria(attributeName, SearchOperator.STARTS_WITH, new StringWithLocale(value, null));
			case ENDS_WITH ->
					new SearchCriteria(attributeName, SearchOperator.ENDS_WITH, new StringWithLocale(value, null));
			case IS_EMPTY ->
					new SearchCriteria(attributeName, SearchOperator.IS_EMPTY, new StringWithLocale(value, null));
			case IS_NOT_EMPTY ->
					new SearchCriteria(attributeName, SearchOperator.IS_NOT_EMPTY, new StringWithLocale(value, null));
			case LIKE -> new SearchCriteria(attributeName, SearchOperator.LIKE, new StringWithLocale(value, null));
			case CONTAINS -> new SearchCriteria(attributeName, SearchOperator.LIKE, new StringWithLocale(value, null));
			default -> {
				notifications.add(CxTranslationsNotification.notSupportedOperator(fieldName, List.of("EQUALS", "STARTS_WITH", "ENDS_WITH", "IS_EMPTY", "IS_NOT_EMPTY", "LIKE", "CONTAINS")));
				yield null;
			}
		};
	}

	private static SearchField getAttributeName(final SearchQueryCondition searchQueryCondition) {
		return switch (searchQueryCondition.getDescriptor().getAttributeName()) {
			case CxLocalizedValueModel.KEY -> SearchField.KEY;
			case CxLocalizedValueModel.VALUEFORSEARCH -> SearchField.VALUE;
			case CxLocalizedValueModel.OBJECTTYPE -> SearchField.OBJECT_TYPE;
			case CxLocalizedValueModel.ATTRIBUTE -> SearchField.ATTRIBUTE;
			case CxLocalizedValueModel.OBJECTID -> SearchField.OBJECT_ID;
			case CxLocalizedValueModel.CATALOGID -> SearchField.CATALOG_ID;
			case CxLocalizedValueModel.CATALOGVERSIONID -> SearchField.CATALOG_VERSION_ID;
			case CxLocalizedValueModel.PK -> SearchField.PK;
			default -> null;
		};
	}

	@Override
	public List<CxLocalizedValueModel> getCurrentPage() {
		return alreadyLoadedCache.computeIfAbsent(getPageNumber(), (idx) -> cxTranslationsPageable.getCurrentPage());
	}

	@Override
	public List<CxLocalizedValueModel> nextPage() {
		cxTranslationsPageable.setPageNumber(Math.min(cxTranslationsPageable.getPageNumber() + 1, cxTranslationsPageable.getTotalPages() - 1));
		return getCurrentPage();
	}

	@Override
	public List<CxLocalizedValueModel> previousPage() {
		cxTranslationsPageable.setPageNumber(Math.max(cxTranslationsPageable.getPageNumber() - 1, 0));
		return getCurrentPage();
	}

	@Override
	public boolean hasNextPage() {
		final int currentPage = cxTranslationsPageable.getPageNumber();
		final int totalPages = cxTranslationsPageable.getTotalPages();
		return currentPage < totalPages - 1;
	}

	@Override
	public boolean hasPreviousPage() {
		return cxTranslationsPageable.getPageNumber() > 0;
	}

	@Override
	public int getPageSize() {
		return cxTranslationsPageable.getPageSize();
	}

	@Override
	public int getTotalCount() {
		return cxTranslationsPageable.getTotalCount();
	}

	@Override
	public int getPageNumber() {
		return cxTranslationsPageable.getPageNumber();
	}

	@Override
	public void setPageNumber(final int pageNumber) {
		cxTranslationsPageable.setPageNumber(pageNumber);
	}

	@Override
	public List<CxLocalizedValueModel> getAllResults() {
		return cxTranslationsPageable.allResults();
	}

	@Override
	public String getTypeCode() {
		return CxLocalizedValueModel._TYPECODE;
	}

	@Override
	public List<CxLocalizedValueModel> setPageSize(final int pageSize) {
		this.pageSize = pageSize;
		this.refresh();
		return getCurrentPage();
	}

	@Override
	public SortData getSortData() {
		return searchQueryData.getSortData();
	}

	@Override
	public void setSortData(final SortData sortData) {
		this.searchQueryData.setSortData(sortData);
		refresh();
	}

	@Override
	public void setQueryId(final String queryId) {
		// No operation
	}

	@Override
	public void setCockpitEvent(final CockpitEvent event) {
		// No operation
	}
}
