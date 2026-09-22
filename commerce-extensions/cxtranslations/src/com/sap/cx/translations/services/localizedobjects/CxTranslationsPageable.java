/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects;

import com.sap.cx.translations.services.CxTranslationsNotification;

import java.util.ArrayList;
import java.util.List;



/**
 * Provides the mechanism to paginate through a collection of data of type {@code T}.
 *
 * @param <T> the type of elements this pageable collection contains.
 */
public interface CxTranslationsPageable<T> {

	/**
	 * Retrieves the current page of data as a list.
	 *
	 * @return a list of type {@code T} representing the current page of data.
	 */
	List<T> getCurrentPage();

	/**
	 * Gets a specific page of results.
	 *
	 * @param pageNumber - index of page to get
	 * @return a list of type {@code T} representing the requested page of data.
	 */
	List<T> getPage(int pageNumber);


	/**
	 * Default implementation to retrieve all results from all pages.
	 *
	 * @return a list of type {@code T} representing all data across pages.
	 */
	default List<T> allResults() {
		final List<T> allData = new ArrayList<>();
		final int totalPages = getTotalPages();

		for (int i = 0; i < totalPages; i++) {
			allData.addAll(getPage(i));
		}
		return allData;
	}

	/**
	 * Gets the number of elements in each page.
	 *
	 * @return the page size as an integer.
	 */
	int getPageSize();

	/**
	 * Gets total number of pages
	 *
	 * @return total number of pages
	 */
	default int getTotalPages() {
		final int totalElements = getTotalCount();
		final int pageSize = getPageSize();
		return (int) Math.ceil((double) totalElements / pageSize);
	}

	/**
	 * Returns the total number of elements across all pages.
	 *
	 * @return the total count of elements.
	 */
	int getTotalCount();

	/**
	 * Retrieves the current page number.
	 *
	 * @return the current page number as an integer.
	 */
	int getPageNumber();

	/**
	 * Sets the current page number to a specific page.
	 *
	 * @param pageNumber the page number to set.
	 */
	void setPageNumber(int pageNumber);

	List<CxTranslationsNotification> getNotifications();
}
