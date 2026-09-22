/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl.localizedobjects;

import com.sap.cx.translations.model.CxAbstractLocalizedValueModel;
import com.sap.cx.translations.model.CxLocalizedValueModel;
import com.sap.cx.translations.services.CxTranslationsNotification;
import com.sap.cx.translations.services.localizedobjects.CxLocalizedObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData.SearchField;


/**
 * Per-search helper providing predicates and flags derived from the current search criteria.
 *
 * <p>All predicate lists are computed eagerly at construction time and stored in final fields,
 * making this class safe to read from multiple call sites without synchronization worries
 * (the mutable {@code notifications} list is the only non-final field and is only ever
 * written from a single request thread).
 *
 * @see CxLocalizedObjectProvider
 */
public class CxLocalizedValuesSearchSupport {

	private final CxLocalizedValueSearchData searchData;
	private final List<Predicate<String>> objectTypePredicates;
	private final List<Predicate<String>> attributePredicates;
	private final List<Predicate<CxLocalizedValueModel>> valuePredicates;
	private final List<Predicate<String>> keyPredicates;
	private final boolean hasValueCriteria;
	private final boolean isEqualsFilterForAttributeEnabled;

	private final List<CxTranslationsNotification> notifications = new ArrayList<>();

	/**
	 * Constructor — eagerly computes all predicate lists from the given search data.
	 *
	 * @param searchData current Search data
	 */
	public CxLocalizedValuesSearchSupport(final CxLocalizedValueSearchData searchData) {
		this.searchData = searchData;
		this.objectTypePredicates = CxSearchCriteriaUtil.getObjectTypePredicates(searchData);
		this.attributePredicates = CxSearchCriteriaUtil.getAttributePredicates(searchData);
		this.valuePredicates = CxSearchCriteriaUtil.getValuePredicates(searchData);
		this.keyPredicates = CxSearchCriteriaUtil.getKeyPredicates(searchData);
		this.hasValueCriteria = searchData.criteria().stream()
				.anyMatch(c -> c.field() == SearchField.VALUE);
		this.isEqualsFilterForAttributeEnabled = searchData.criteria().stream()
				.anyMatch(c -> c.field() == SearchField.ATTRIBUTE
						&& c.operator() == CxLocalizedValueSearchData.SearchOperator.EQUALS);
	}

	/**
	 * Adds a notification.
	 */
	public void addNotification(final CxTranslationsNotification notification) {
		notifications.add(notification);
	}

	/**
	 * {@return list of notifications}
	 */
	public List<CxTranslationsNotification> getNotifications() {
		return notifications;
	}

	/**
	 * {@return predicates that can be used to filter the list of localized object type codes according to current search.}
	 */
	public List<Predicate<String>> getObjectTypePredicates() {
		return objectTypePredicates;
	}

	public List<Predicate<String>> getKeyPredicates() {
		return keyPredicates;
	}

	/**
	 * {@return predicates that can be used to filter the list of attributes according to current search.}
	 */
	public List<Predicate<String>> getAttributePredicates() {
		return attributePredicates;
	}

	/**
	 * {@return predicates that can be used to filter the list of localized value strings according to current search.}
	 */
	public List<Predicate<CxLocalizedValueModel>> getValuePredicates() {
		return valuePredicates;
	}

	/**
	 * {@return if there is a criteria for 'value' field.}
	 */
	public boolean hasCriteriaForValue() {
		return hasValueCriteria;
	}

	public boolean isEqualsFilterForAttributeEnabled() {
		return isEqualsFilterForAttributeEnabled;
	}

	public CxLocalizedValueResultStatistics getStatistics(final List<CxLocalizedValueModel> value) {
		final int objectCount = (int) value.stream()
										   .map(CxAbstractLocalizedValueModel::getObjectId)
										   .distinct()
										   .count();
		return new CxLocalizedValueResultStatistics(objectCount, value.size());
	}
}
