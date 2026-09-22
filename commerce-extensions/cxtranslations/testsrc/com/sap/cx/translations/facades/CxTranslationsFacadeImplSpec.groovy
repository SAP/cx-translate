/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.facades

import com.sap.cx.translations.cache.CxLocalizedLabelCacheKeyProvider
import com.sap.cx.translations.facades.impl.CxTranslationsFacadeImpl
import com.sap.cx.translations.labels.CxLocalizedLabel
import com.sap.cx.translations.labels.UsageType
import com.sap.cx.translations.model.CxLocalizedBundleModel
import com.sap.cx.translations.model.CxLocalizedLabelModel
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService
import com.sap.cx.translations.services.CxTranslationsService
import com.sap.cx.translations.strategies.CxTranslationsCatalogVersionStrategy
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.catalog.model.CatalogModel
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.cms2.common.service.SessionSearchRestrictionsDisabler
import de.hybris.platform.commerceservices.setup.SetupSyncJobService
import de.hybris.platform.core.model.user.UserModel
import de.hybris.platform.cronjob.enums.CronJobResult
import de.hybris.platform.regioncache.CacheController
import de.hybris.platform.regioncache.key.CacheKey
import de.hybris.platform.servicelayer.config.ConfigurationService
import de.hybris.platform.servicelayer.cronjob.PerformResult
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.session.SessionService
import de.hybris.platform.servicelayer.user.UserService
import org.apache.commons.configuration2.Configuration
import spock.lang.Specification

@UnitTest
class CxTranslationsFacadeImplSpec extends Specification {

    // Shared mocks — never stub catalog/config in setup() to avoid Spock interaction priority issues
    CxTranslationsService translationsService = Mock()
    ModelService modelService = Mock()
    CacheController cacheController = Mock()
    CxLocalizedLabelCacheKeyProvider cacheKeyProvider = Mock()
    ConfigurationService configurationService = Mock()
    Configuration configuration = Mock()
    CxTranslationsCatalogVersionStrategy catalogVersionStrategy = Mock()
    SessionSearchRestrictionsDisabler disabler = Mock()
    SetupSyncJobService setupSyncJobService = Mock()
    CxTranslateCommerceCommonI18NService i18nService = Mock()
    SessionService sessionService = Mock()
    UserService userService = Mock()

    // Per-test catalog version mocks — created fresh in each test that needs them
    CatalogVersionModel staged = Mock()
    CatalogVersionModel session = Mock()

    def setup() {
        // Disabler always executes the Supplier directly
        disabler.execute(_) >> { args -> args[0].get() }
        // Config service always returns the configuration mock
        configurationService.getConfiguration() >> configuration
    }

    /** Builds a fresh facade wired with all shared mocks. Call at start of each test. */
    private CxTranslationsFacadeImpl buildFacade(
            boolean cacheEnabled = false,
            boolean importAllowed = true,
            boolean includeEmpty = false) {
        configuration.getBoolean("cxtranslations.cache.enabled", false) >> cacheEnabled
        configuration.getBoolean("cxtranslations.import.allowed", false) >> importAllowed
        configuration.getBoolean("cxtranslations.include.empty.values", false) >> includeEmpty

        def f = new CxTranslationsFacadeImpl()
        f.setTranslationsService(translationsService)
        f.setModelService(modelService)
        f.setCacheController(cacheController)
        f.setCxLocalizedLabelCacheKeyProvider(cacheKeyProvider)
        f.setConfigurationService(configurationService)
        f.setTranslationsCatalogVersionStrategy(catalogVersionStrategy)
        f.setSessionSearchRestrictionsDisabler(disabler)
        f.setSetupSyncJobService(setupSyncJobService)
        f.setCxTranslateCommerceCommonI18NService(i18nService)
        f.setSessionService(sessionService)
        f.setUserService(userService)
        return f
    }

    // -------------------------------------------------------------------------
    // getLabelsMap
    // -------------------------------------------------------------------------

    def "getLabelsMap: duplicate keys — no throw, keeps first value"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        def l1 = Mock(CxLocalizedLabelModel) { getKey() >> "k"; getValue() >> "v1" }
        def l2 = Mock(CxLocalizedLabelModel) { getKey() >> "k"; getValue() >> "v2" }
        translationsService.getLabels(session, _) >> [l1, l2]

        when:
        def result = facade.getLabelsMap(false)

        then:
        noExceptionThrown()
        result["k"] == "v1"
    }

    def "getLabelsMap: null-value labels excluded when includeEmptyValues is false"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        def l1 = Mock(CxLocalizedLabelModel) { getKey() >> "key1"; getValue() >> "hello" }
        def l2 = Mock(CxLocalizedLabelModel) { getKey() >> "key2"; getValue() >> null }
        translationsService.getLabels(session, _) >> [l1, l2]

        when:
        def result = facade.getLabelsMap(false)

        then:
        result == ["key1": "hello"]
    }

    def "getLabelsMap: null-value labels included as empty string when includeEmptyValues is true"() {
        given:
        def facade = buildFacade(false, true, true)
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        def l1 = Mock(CxLocalizedLabelModel) { getKey() >> "key1"; getValue() >> null }
        translationsService.getLabels(session, _) >> [l1]

        when:
        def result = facade.getLabelsMap(false)

        then:
        result == ["key1": ""]
    }

    def "getLabelsMap(bundle): returns only labels for that bundle"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        def l = Mock(CxLocalizedLabelModel) { getKey() >> "btn.ok"; getValue() >> "OK" }
        translationsService.getLabels("myBundle", session, _) >> [l]

        when:
        def result = facade.getLabelsMap("myBundle", false)

        then:
        result == ["btn.ok": "OK"]
    }

    def "getLabelsMap: uses cache when useCache is true"() {
        given:
        def facade = buildFacade(true)
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        def cacheKey = Mock(CacheKey)
        cacheKeyProvider.getAllLabelsKey(_, session) >> cacheKey
        def l = Mock(CxLocalizedLabelModel) { getKey() >> "k"; getValue() >> "v" }
        translationsService.getLabels(session, _) >> [l]

        when:
        def result = facade.getLabelsMap(true)

        then:
        // Combined stub + verification: returns the loader-invoked value AND verifies exactly 1 call
        1 * cacheController.getWithLoader(cacheKey, _) >> { args -> args[1].load(args[0]) }
        result == ["k": "v"]
    }

    // -------------------------------------------------------------------------
    // getLabel
    // -------------------------------------------------------------------------

    def "getLabel: returns value when label exists"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        def lm = Mock(CxLocalizedLabelModel) { getValue() >> "Hello" }
        translationsService.getLabel("greeting", session, UsageType.FRONTEND) >> Optional.of(lm)

        expect:
        facade.getLabel("greeting", UsageType.FRONTEND, false) == "Hello"
    }

    def "getLabel: returns null when label does not exist"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        translationsService.getLabel("missing", session, UsageType.FRONTEND) >> Optional.empty()

        expect:
        facade.getLabel("missing", UsageType.FRONTEND, false) == null
    }

    // -------------------------------------------------------------------------
    // editLabel
    // -------------------------------------------------------------------------

    def "editLabel: returns early when staged catalog version is null"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getStagedTranslationsCatalogVersion() >> null
        def label = Mock(CxLocalizedLabel)

        when:
        facade.editLabel(label)

        then:
        0 * translationsService.getBundleOrCreate(_, _)
        0 * cacheController.invalidate(_)
    }

    def "editLabel: saves label and invalidates three cache keys"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getStagedTranslationsCatalogVersion() >> staged
        def bundleModel = Mock(CxLocalizedBundleModel)
        def labelModel = Mock(CxLocalizedLabelModel)
        def label = Mock(CxLocalizedLabel) { getBundle() >> "b"; getKey() >> "k"; getValue() >> "v" }
        translationsService.getBundleOrCreate("b", staged) >> bundleModel
        translationsService.getLabelOrCreate("k", staged, UsageType.FRONTEND) >> labelModel
        def bundleKey = Mock(CacheKey)
        def allKey = Mock(CacheKey)
        def singleKey = Mock(CacheKey)
        cacheKeyProvider.getBundleKey("b", _, staged) >> bundleKey
        cacheKeyProvider.getAllLabelsKey(_, staged) >> allKey
        cacheKeyProvider.getSingleLabelKey("k", UsageType.FRONTEND, staged) >> singleKey

        when:
        facade.editLabel(label)

        then:
        1 * labelModel.setValue("v")
        1 * labelModel.setBundle(bundleModel)
        1 * modelService.save(labelModel)
        1 * cacheController.invalidate(bundleKey)
        1 * cacheController.invalidate(allKey)
        1 * cacheController.invalidate(singleKey)
    }

    // -------------------------------------------------------------------------
    // importLabels
    // -------------------------------------------------------------------------

    def "importLabels: throws IllegalStateException when staged catalog version is null"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getStagedTranslationsCatalogVersion() >> null

        when:
        facade.importLabels(["bundle": ["key": "val"]], "en")

        then:
        thrown(IllegalStateException)
    }

    def "importLabels: throws IllegalStateException when import is disabled"() {
        given:
        def facade = buildFacade(false, false, false)

        when:
        facade.importLabels(["bundle": ["key": "val"]], "en")

        then:
        thrown(IllegalStateException)
    }

    def "importLabels: creates bundle and label and saves"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getStagedTranslationsCatalogVersion() >> staged
        def locale = Locale.ENGLISH
        i18nService.getLocale("en") >> locale
        def bundleModel = Mock(CxLocalizedBundleModel)
        def labelModel = Mock(CxLocalizedLabelModel)
        translationsService.getBundleOrCreate("b1", staged) >> bundleModel
        translationsService.getLabelOrCreate("k1", staged, UsageType.FRONTEND) >> labelModel

        when:
        facade.importLabels(["b1": ["k1": "v1"]], "en")

        then:
        1 * labelModel.setBundle(bundleModel)
        1 * labelModel.setValue("v1", locale)
        1 * modelService.save(labelModel)
    }

    // -------------------------------------------------------------------------
    // publishTranslations
    // -------------------------------------------------------------------------

    def "publishTranslations: returns false when sync job returns null"() {
        given:
        def facade = buildFacade()
        def catalog = Mock(CatalogModel) { getId() >> "tc" }
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        session.getCatalog() >> catalog
        setupSyncJobService.executeCatalogSyncJob("tc") >> null

        when:
        def result = facade.publishTranslations()

        then:
        !result
        0 * translationsService.clearLabelsCache()
    }

    def "publishTranslations: returns false when sync job fails"() {
        given:
        def facade = buildFacade()
        def catalog = Mock(CatalogModel) { getId() >> "tc" }
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        session.getCatalog() >> catalog
        setupSyncJobService.executeCatalogSyncJob("tc") >> new PerformResult(CronJobResult.FAILURE, null)

        when:
        def result = facade.publishTranslations()

        then:
        !result
        0 * translationsService.clearLabelsCache()
    }

    def "publishTranslations: returns true and clears cache on success"() {
        given:
        def facade = buildFacade()
        def catalog = Mock(CatalogModel) { getId() >> "tc" }
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> session
        session.getCatalog() >> catalog
        setupSyncJobService.executeCatalogSyncJob("tc") >> new PerformResult(CronJobResult.SUCCESS, null)

        when:
        def result = facade.publishTranslations()

        then:
        result
        1 * translationsService.clearLabelsCache()
    }

    def "publishTranslations: returns false when session catalog version is null"() {
        given:
        def facade = buildFacade()
        catalogVersionStrategy.getSessionTranslationsCatalogVersion() >> null

        when:
        def result = facade.publishTranslations()

        then:
        !result
        0 * translationsService.clearLabelsCache()
    }

    // -------------------------------------------------------------------------
    // isFrontendTranslationAllowedForUser
    // -------------------------------------------------------------------------

    def "isFrontendTranslationAllowedForUser: falls back to current user when agent not found"() {
        given:
        def facade = buildFacade()
        def currentUser = Mock(UserModel)
        sessionService.getAttribute("ACTING_USER_UID") >> "deleted"
        userService.getUserForUID("deleted") >> { throw new UnknownIdentifierException("not found") }
        userService.getCurrentUser() >> currentUser
        translationsService.isFrontendTranslationAllowedForUser(currentUser) >> true

        expect:
        facade.isFrontendTranslationAllowedForUser() == true
    }

    def "isFrontendTranslationAllowedForUser: uses ASM agent when found"() {
        given:
        def facade = buildFacade()
        def agentUser = Mock(UserModel)
        sessionService.getAttribute("ACTING_USER_UID") >> "agent1"
        userService.getUserForUID("agent1") >> agentUser
        translationsService.isFrontendTranslationAllowedForUser(agentUser) >> true

        expect:
        facade.isFrontendTranslationAllowedForUser() == true
    }

    def "isFrontendTranslationAllowedForUser: returns false when no agent and no current user"() {
        given:
        def facade = buildFacade()
        sessionService.getAttribute("ACTING_USER_UID") >> null
        userService.getCurrentUser() >> null

        expect:
        facade.isFrontendTranslationAllowedForUser() == false
    }
}
