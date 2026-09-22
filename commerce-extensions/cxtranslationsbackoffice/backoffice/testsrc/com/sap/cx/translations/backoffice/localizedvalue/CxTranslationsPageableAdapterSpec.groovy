/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.localizedvalue

import com.hybris.cockpitng.search.data.SearchQueryData
import com.hybris.cockpitng.search.data.SortData
import com.hybris.cockpitng.util.notifications.NotificationService
import com.sap.cx.translations.model.CxLocalizedValueModel
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService
import com.sap.cx.translations.services.localizedobjects.CxTranslationsPageable
import de.hybris.bootstrap.annotations.UnitTest
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationsPageableAdapterSpec extends Specification {

    CxLocalizedValueService localizedValueService = Mock()
    NotificationService notificationService = Mock()
    SearchQueryData searchQueryData = Mock()
    SortData sortData = Mock()
    CxTranslationsPageable<CxLocalizedValueModel> pageable = Mock()

    def setup() {
        searchQueryData.getConditions() >> []
        searchQueryData.getSortData() >> sortData
        searchQueryData.getPageSize() >> 25
        sortData.getSortAttribute() >> ""
        sortData.isAscending() >> true
        localizedValueService.search(_) >> pageable
        pageable.getNotifications() >> []
        pageable.getPageNumber() >> 0
        pageable.getPageSize() >> 25
        pageable.getTotalCount() >> 100
        pageable.getTotalPages() >> 4
    }

    // -------------------------------------------------------------------------
    // The fix: refresh() clears the alreadyLoadedCache
    // -------------------------------------------------------------------------

    def "refresh clears the per-page cache so subsequent getCurrentPage refetches from service"() {
        given:
        final List<CxLocalizedValueModel> page1 = [Mock(CxLocalizedValueModel)]
        final List<CxLocalizedValueModel> page2 = [Mock(CxLocalizedValueModel), Mock(CxLocalizedValueModel)]
        pageable.getCurrentPage() >>> [page1, page2]

        @Subject
        CxTranslationsPageableAdapter adapter = new CxTranslationsPageableAdapter(
                searchQueryData, localizedValueService, notificationService, [] as Set<String>)

        when: "first call populates the cache"
        def first = adapter.getCurrentPage()

        and: "second call returns cached result"
        def second = adapter.getCurrentPage()

        then:
        first.is(page1)
        second.is(page1)  // cached — same content

        when: "refresh clears cache and re-search returns new data"
        adapter.refresh()
        def third = adapter.getCurrentPage()

        then:
        third.is(page2)  // fresh after cache invalidation
    }

    def "setSortData invokes refresh and clears the cache"() {
        given:
        final List<CxLocalizedValueModel> beforeSort = [Mock(CxLocalizedValueModel)]
        final List<CxLocalizedValueModel> afterSort = [Mock(CxLocalizedValueModel), Mock(CxLocalizedValueModel)]
        pageable.getCurrentPage() >>> [beforeSort, afterSort]
        final SortData newSort = Mock(SortData) {
            getSortAttribute() >> "key"
            isAscending() >> false
        }

        @Subject
        CxTranslationsPageableAdapter adapter = new CxTranslationsPageableAdapter(
                searchQueryData, localizedValueService, notificationService, [] as Set<String>)
        adapter.getCurrentPage()  // populate cache

        when:
        adapter.setSortData(newSort)
        def result = adapter.getCurrentPage()

        then:
        result.is(afterSort)  // cache was cleared by refresh()
    }
}
