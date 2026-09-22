/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.events

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.regioncache.CacheController
import de.hybris.platform.regioncache.region.CacheRegion
import de.hybris.platform.servicelayer.config.ConfigurationService
import org.apache.commons.configuration2.Configuration
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxClearLabelsCacheEventListenerSpec extends Specification {

    CacheController cacheController = Mock()
    ConfigurationService configurationService = Mock()
    Configuration configuration = Mock()

    @Subject
    CxClearLabelsCacheEventListener listener = new CxClearLabelsCacheEventListener()

    def setup() {
        listener.setCacheController(cacheController)
        listener.setConfigurationService(configurationService)
        configurationService.getConfiguration() >> configuration
    }

    def "onEvent clears the matching cache region"() {
        given:
        configuration.getString("cxtranslations.cache.region") >> "cxLabelsRegion"
        def matchingRegion = Mock(CacheRegion) { getName() >> "cxLabelsRegion" }
        def otherRegion = Mock(CacheRegion) { getName() >> "otherRegion" }
        cacheController.getRegions() >> [matchingRegion, otherRegion]

        when:
        listener.onEvent(new CxClearLabelsCacheEvent())

        then:
        1 * cacheController.clearCache(matchingRegion)
        0 * cacheController.clearCache(otherRegion)
    }

    def "onEvent does nothing when configured region name is blank"() {
        given:
        configuration.getString("cxtranslations.cache.region") >> ""

        when:
        listener.onEvent(new CxClearLabelsCacheEvent())

        then:
        0 * cacheController.getRegions()
        0 * cacheController.clearCache(_)
    }

    def "onEvent does nothing when configured region name is null"() {
        given:
        configuration.getString("cxtranslations.cache.region") >> null

        when:
        listener.onEvent(new CxClearLabelsCacheEvent())

        then:
        0 * cacheController.getRegions()
        0 * cacheController.clearCache(_)
    }

    def "onEvent does not throw when no region matches the configured name"() {
        given:
        configuration.getString("cxtranslations.cache.region") >> "nonexistentRegion"
        def region = Mock(CacheRegion) { getName() >> "otherRegion" }
        cacheController.getRegions() >> [region]

        when:
        listener.onEvent(new CxClearLabelsCacheEvent())

        then:
        noExceptionThrown()
        0 * cacheController.clearCache(_)
    }

    def "CxClearLabelsCacheEvent canPublish returns true for any context"() {
        given:
        def event = new CxClearLabelsCacheEvent()

        expect:
        event.canPublish(null) == true
    }
}
