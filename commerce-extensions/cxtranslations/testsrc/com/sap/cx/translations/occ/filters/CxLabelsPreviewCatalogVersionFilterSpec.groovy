/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.occ.filters

import com.sap.cx.translations.strategies.CxTranslationsCatalogVersionStrategy
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.catalog.CatalogVersionService
import de.hybris.platform.catalog.model.CatalogModel
import de.hybris.platform.catalog.model.CatalogVersionModel
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spock.lang.Specification
import spock.lang.Subject

import java.util.regex.Pattern

@UnitTest
class CxLabelsPreviewCatalogVersionFilterSpec extends Specification {

    CxTranslationsCatalogVersionStrategy strategy = Mock()
    CatalogVersionService catalogVersionService = Mock()
    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    FilterChain filterChain = Mock()

    @Subject
    CxLabelsPreviewCatalogVersionFilter filter = new CxLabelsPreviewCatalogVersionFilter()

    def setup() {
        filter.setRegexp(Pattern.compile("/staged/"))
        filter.setTranslationsCatalogVersionStrategy(strategy)
        filter.setCatalogVersionService(catalogVersionService)
    }

    private CatalogVersionModel catalogVersion(String catalogId, String version) {
        def catalog = Mock(CatalogModel) { getId() >> catalogId }
        return Mock(CatalogVersionModel) {
            getCatalog() >> catalog
            getVersion() >> version
        }
    }

    // -------------------------------------------------------------------------
    // URL does not match — filter is a pass-through
    // -------------------------------------------------------------------------

    def "non-matching URL passes through without touching session catalogs"() {
        given:
        request.getPathInfo() >> "/foo/bar"

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        0 * strategy.getStagedTranslationsCatalogVersion()
        0 * catalogVersionService.setSessionCatalogVersions(_)
    }

    // -------------------------------------------------------------------------
    // Matching URL — staged catalog injected and restored
    // -------------------------------------------------------------------------

    def "matching URL injects staged catalog and restores original on success"() {
        given:
        request.getPathInfo() >> "/translations/staged/chunks/ns"
        def staged = catalogVersion("contentCatalog", "Staged")
        def original = catalogVersion("productCatalog", "Online")
        strategy.getStagedTranslationsCatalogVersion() >> staged
        def originalList = [original]
        catalogVersionService.getSessionCatalogVersions() >> originalList

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        // First call: inject staged + filtered originals (original kept because different catalog)
        1 * catalogVersionService.setSessionCatalogVersions({ List versions ->
            versions.containsAll([original, staged]) && versions.size() == 2
        })
        // Then proceed with the filter chain
        1 * filterChain.doFilter(request, response)
        // Finally: restore the original list (session bleed prevention)
        1 * catalogVersionService.setSessionCatalogVersions(originalList)
    }

    def "matching URL removes existing version of same catalog before adding staged"() {
        given:
        request.getPathInfo() >> "/translations/staged/chunks/ns"
        def stagedCatalog = Mock(CatalogModel) { getId() >> "contentCatalog" }
        def staged = Mock(CatalogVersionModel) {
            getCatalog() >> stagedCatalog
            getVersion() >> "Staged"
        }
        // Online version of the SAME catalog should be removed when the staged is added
        def onlineSameCatalog = Mock(CatalogVersionModel) {
            getCatalog() >> stagedCatalog
            getVersion() >> "Online"
        }
        strategy.getStagedTranslationsCatalogVersion() >> staged
        catalogVersionService.getSessionCatalogVersions() >> [onlineSameCatalog]

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        1 * catalogVersionService.setSessionCatalogVersions({ List versions ->
            versions == [staged]
        })
        1 * filterChain.doFilter(request, response)
    }

    def "matching URL restores original catalog list even when filter chain throws"() {
        given:
        request.getPathInfo() >> "/translations/staged/chunks/ns"
        def staged = catalogVersion("contentCatalog", "Staged")
        def originalList = [catalogVersion("productCatalog", "Online")]
        strategy.getStagedTranslationsCatalogVersion() >> staged
        catalogVersionService.getSessionCatalogVersions() >> originalList
        filterChain.doFilter(request, response) >> { throw new RuntimeException("downstream error") }

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        thrown(RuntimeException)
        // Restoration must still happen even on exception
        1 * catalogVersionService.setSessionCatalogVersions(originalList)
    }

    def "matching URL throws IllegalStateException when staged catalog version is not configured"() {
        given:
        request.getPathInfo() >> "/translations/staged/chunks/ns"
        strategy.getStagedTranslationsCatalogVersion() >> null

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        thrown(IllegalStateException)
        0 * filterChain.doFilter(_, _)
        0 * catalogVersionService.setSessionCatalogVersions(_)
    }

    // -------------------------------------------------------------------------
    // Null path info (some containers return null) treated as empty
    // -------------------------------------------------------------------------

    def "null path info does not throw and passes through"() {
        given:
        request.getPathInfo() >> null

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        noExceptionThrown()
        1 * filterChain.doFilter(request, response)
        0 * catalogVersionService.setSessionCatalogVersions(_)
    }
}
