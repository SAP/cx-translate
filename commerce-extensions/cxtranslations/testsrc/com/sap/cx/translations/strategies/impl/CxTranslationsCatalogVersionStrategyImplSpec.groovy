/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.strategies.impl

import com.sap.cx.translations.strategies.CxTranslationsCatalogVersionStrategy
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.catalog.CatalogService
import de.hybris.platform.catalog.CatalogVersionService
import de.hybris.platform.catalog.model.CatalogModel
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.cms2.common.service.SessionSearchRestrictionsDisabler
import de.hybris.platform.cms2.model.contents.ContentCatalogModel
import de.hybris.platform.cms2.model.site.CMSSiteModel
import de.hybris.platform.cms2.servicelayer.services.CMSSiteService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationsCatalogVersionStrategyImplSpec extends Specification {

    CMSSiteService cmsSiteService = Mock()
    CatalogService catalogService = Mock()
    CatalogVersionService catalogVersionService = Mock()
    SessionSearchRestrictionsDisabler disabler = Mock()

    @Subject
    CxTranslationsCatalogVersionStrategyImpl strategy = new CxTranslationsCatalogVersionStrategyImpl()

    def setup() {
        strategy.setCmsSiteService(cmsSiteService)
        strategy.setCatalogService(catalogService)
        strategy.setCatalogVersionService(catalogVersionService)
        strategy.setSessionSearchRestrictionsDisabler(disabler)
        disabler.execute(_) >> { args -> args[0].get() }
    }

    // ------------------------------------------------------------------------
    // getRootContentCatalog — cycle detection
    // ------------------------------------------------------------------------

    def "getRootContentCatalog returns null for null input"() {
        expect:
        strategy.getRootContentCatalog(null) == null
    }

    def "getRootContentCatalog returns root when chain is linear"() {
        given:
        def root = Mock(ContentCatalogModel) { getSuperCatalog() >> null }
        def child = Mock(ContentCatalogModel) { getSuperCatalog() >> root }

        expect:
        strategy.getRootContentCatalog(child).is(root)
    }

    def "getRootContentCatalog terminates on a cyclic super-catalog chain (no StackOverflowError)"() {
        given:
        // Cycle: a -> b -> a
        def a = Mock(ContentCatalogModel)
        def b = Mock(ContentCatalogModel)
        a.getSuperCatalog() >> b
        b.getSuperCatalog() >> a

        when:
        def result = strategy.getRootContentCatalog(a)

        then:
        noExceptionThrown()
        // Cycle is broken: walk stops once a node is revisited
        result == a || result == b
    }

    def "getRootContentCatalog returns the same catalog when it has no super"() {
        given:
        def only = Mock(ContentCatalogModel) { getSuperCatalog() >> null }

        expect:
        strategy.getRootContentCatalog(only).is(only)
    }

    // ------------------------------------------------------------------------
    // getSessionTranslationsCatalogVersion
    // ------------------------------------------------------------------------

    def "getSessionTranslationsCatalogVersion returns first session content catalog version when present"() {
        given:
        def contentCatalog = Mock(ContentCatalogModel)
        def version = Mock(CatalogVersionModel) { getCatalog() >> contentCatalog }
        catalogVersionService.getSessionCatalogVersions() >> [version]

        expect:
        strategy.getSessionTranslationsCatalogVersion().is(version)
    }

    def "getSessionTranslationsCatalogVersion falls back to active catalog version when session has none"() {
        given:
        catalogVersionService.getSessionCatalogVersions() >> []
        def root = Mock(ContentCatalogModel) {
            getSuperCatalog() >> null
        }
        def active = Mock(CatalogVersionModel)
        root.getActiveCatalogVersion() >> active
        def site = Mock(CMSSiteModel) {
            getContentCatalogs() >> [root]
        }
        cmsSiteService.getCurrentSite() >> site

        expect:
        strategy.getSessionTranslationsCatalogVersion().is(active)
    }

    def "getSessionTranslationsCatalogVersion throws IllegalStateException when nothing resolves"() {
        given:
        catalogVersionService.getSessionCatalogVersions() >> []
        cmsSiteService.getCurrentSite() >> null

        when:
        strategy.getSessionTranslationsCatalogVersion()

        then:
        thrown(IllegalStateException)
    }

    // ------------------------------------------------------------------------
    // getStagedTranslationsCatalogVersion
    // ------------------------------------------------------------------------

    def "getStagedTranslationsCatalogVersion returns first non-active version of the default catalog"() {
        given:
        def active = Mock(CatalogVersionModel)
        def staged = Mock(CatalogVersionModel)
        def root = Mock(ContentCatalogModel) {
            getSuperCatalog() >> null
            getActiveCatalogVersion() >> active
            getCatalogVersions() >> [active, staged]
        }
        def site = Mock(CMSSiteModel) { getContentCatalogs() >> [root] }
        cmsSiteService.getCurrentSite() >> site

        expect:
        strategy.getStagedTranslationsCatalogVersion().is(staged)
    }

    def "getStagedTranslationsCatalogVersion throws IllegalStateException when no non-active version exists"() {
        given:
        def active = Mock(CatalogVersionModel)
        def root = Mock(ContentCatalogModel) {
            getSuperCatalog() >> null
            getActiveCatalogVersion() >> active
            getCatalogVersions() >> [active]
        }
        def site = Mock(CMSSiteModel) { getContentCatalogs() >> [root] }
        cmsSiteService.getCurrentSite() >> site

        when:
        strategy.getStagedTranslationsCatalogVersion()

        then:
        thrown(IllegalStateException)
    }

    // ------------------------------------------------------------------------
    // Fallback catalog from property
    // ------------------------------------------------------------------------

    def "getDefaultContentCatalog falls back to the configured catalog when site has none"() {
        given:
        cmsSiteService.getCurrentSite() >> null
        strategy.setDefaultTranslationCatalogId("globalContent")
        def fallback = Mock(ContentCatalogModel)
        catalogService.getCatalogForId("globalContent") >> fallback

        when:
        def result = strategy.getDefaultContentCatalog()

        then:
        result.isPresent()
        result.get().is(fallback)
    }

    def "getDefaultContentCatalog returns empty when fallback is not a ContentCatalogModel"() {
        given:
        cmsSiteService.getCurrentSite() >> null
        strategy.setDefaultTranslationCatalogId("wrong")
        catalogService.getCatalogForId("wrong") >> Mock(CatalogModel)

        expect:
        !strategy.getDefaultContentCatalog().isPresent()
    }

    def "getDefaultContentCatalog returns empty when fallback ID is blank"() {
        given:
        cmsSiteService.getCurrentSite() >> null
        strategy.setDefaultTranslationCatalogId("")

        expect:
        !strategy.getDefaultContentCatalog().isPresent()
    }
}
