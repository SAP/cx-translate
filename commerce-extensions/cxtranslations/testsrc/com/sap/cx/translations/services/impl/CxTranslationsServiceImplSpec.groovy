/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.impl

import com.sap.cx.translations.daos.CxLocalizedBundleDao
import com.sap.cx.translations.daos.CxLocalizedLabelDao
import com.sap.cx.translations.events.CxClearLabelsCacheEvent
import com.sap.cx.translations.labels.UsageType
import com.sap.cx.translations.model.CxLocalizedBundleModel
import com.sap.cx.translations.model.CxLocalizedLabelModel
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.core.model.security.PrincipalGroupModel
import de.hybris.platform.core.model.user.UserModel
import de.hybris.platform.servicelayer.event.EventService
import de.hybris.platform.servicelayer.model.ModelService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationsServiceImplSpec extends Specification {

    CxLocalizedLabelDao labelDao = Mock()
    CxLocalizedBundleDao bundleDao = Mock()
    ModelService modelService = Mock()
    EventService eventService = Mock()

    @Subject
    CxTranslationsServiceImpl service = new CxTranslationsServiceImpl()

    CatalogVersionModel catalogVersion = Mock()

    def setup() {
        service.setLabelDao(labelDao)
        service.setBundleDao(bundleDao)
        service.setModelService(modelService)
        service.setEventService(eventService)
        service.setKeySuggestionsLimit(10)
        service.setUserGroupsAllowedForFrontendTranslation("translators,admins")
    }

    // -------------------------------------------------------------------------
    // getLabels by catalog
    // -------------------------------------------------------------------------

    def "getLabels delegates to DAO with usage type"() {
        given:
        def label = Mock(CxLocalizedLabelModel)
        labelDao.findLabelsByUsage(catalogVersion, UsageType.FRONTEND) >> [label]

        expect:
        service.getLabels(catalogVersion, UsageType.FRONTEND) == [label]
    }

    // -------------------------------------------------------------------------
    // getLabels by bundle — checkUsage filter
    // -------------------------------------------------------------------------

    def "getLabels(bundle) returns empty list and logs when bundle is missing"() {
        given:
        bundleDao.findBundleByCode("missing", catalogVersion) >> Optional.empty()

        expect:
        service.getLabels("missing", catalogVersion, UsageType.FRONTEND).isEmpty()
    }

    def "getLabels(bundle) filters labels by FRONTEND usage"() {
        given:
        def frontendLabel = Mock(CxLocalizedLabelModel) { isForFrontend() >> true; isForBackend() >> false }
        def backendLabel = Mock(CxLocalizedLabelModel) { isForFrontend() >> false; isForBackend() >> true }
        def bundle = Mock(CxLocalizedBundleModel) { getLabels() >> [frontendLabel, backendLabel] }
        bundleDao.findBundleByCode("b", catalogVersion) >> Optional.of(bundle)

        when:
        def result = service.getLabels("b", catalogVersion, UsageType.FRONTEND)

        then:
        result == [frontendLabel]
    }

    def "getLabels(bundle) returns all labels when no usage types provided"() {
        given:
        def l1 = Mock(CxLocalizedLabelModel)
        def l2 = Mock(CxLocalizedLabelModel)
        def bundle = Mock(CxLocalizedBundleModel) { getLabels() >> [l1, l2] }
        bundleDao.findBundleByCode("b", catalogVersion) >> Optional.of(bundle)

        when:
        def result = service.getLabels("b", catalogVersion)

        then:
        result.containsAll([l1, l2])
    }

    // -------------------------------------------------------------------------
    // getLabel
    // -------------------------------------------------------------------------

    def "getLabel by key delegates to DAO"() {
        given:
        def label = Mock(CxLocalizedLabelModel)
        labelDao.findLabelByKeyAndUsage("k", catalogVersion, null) >> Optional.of(label)

        expect:
        service.getLabel("k", catalogVersion).get().is(label)
    }

    // -------------------------------------------------------------------------
    // getLabelOrCreate
    // -------------------------------------------------------------------------

    def "getLabelOrCreate returns existing label when found"() {
        given:
        def existing = Mock(CxLocalizedLabelModel)
        labelDao.findLabelByKeyAndUsage("k", catalogVersion, UsageType.FRONTEND) >> Optional.of(existing)

        when:
        def result = service.getLabelOrCreate("k", catalogVersion, UsageType.FRONTEND)

        then:
        result.is(existing)
        0 * modelService.create(_)
    }

    def "getLabelOrCreate creates new FRONTEND label when none exists"() {
        given:
        def newLabel = Mock(CxLocalizedLabelModel)
        labelDao.findLabelByKeyAndUsage("k", catalogVersion, UsageType.FRONTEND) >> Optional.empty()
        modelService.create(CxLocalizedLabelModel) >> newLabel

        when:
        service.getLabelOrCreate("k", catalogVersion, UsageType.FRONTEND)

        then:
        1 * newLabel.setKey("k")
        1 * newLabel.setForFrontend(true)
        1 * newLabel.setForBackend(false)
        1 * newLabel.setCatalogVersion(catalogVersion)
    }

    def "getLabelOrCreate with BACKEND usage flips flags correctly"() {
        given:
        def newLabel = Mock(CxLocalizedLabelModel)
        labelDao.findLabelByKeyAndUsage("k", catalogVersion, UsageType.BACKEND) >> Optional.empty()
        modelService.create(CxLocalizedLabelModel) >> newLabel

        when:
        service.getLabelOrCreate("k", catalogVersion, UsageType.BACKEND)

        then:
        1 * newLabel.setForFrontend(false)
        1 * newLabel.setForBackend(true)
    }

    // -------------------------------------------------------------------------
    // getBundleOrCreate
    // -------------------------------------------------------------------------

    def "getBundleOrCreate creates and saves a new bundle when none exists"() {
        given:
        def newBundle = Mock(CxLocalizedBundleModel)
        bundleDao.findBundleByCode("b", catalogVersion) >> Optional.empty()
        modelService.create(CxLocalizedBundleModel) >> newBundle

        when:
        def result = service.getBundleOrCreate("b", catalogVersion)

        then:
        result.is(newBundle)
        1 * newBundle.setCode("b")
        1 * newBundle.setCatalogVersion(catalogVersion)
        1 * modelService.save(newBundle)
    }

    // -------------------------------------------------------------------------
    // clearLabelsCache
    // -------------------------------------------------------------------------

    def "clearLabelsCache publishes CxClearLabelsCacheEvent"() {
        when:
        service.clearLabelsCache()

        then:
        1 * eventService.publishEvent({ it instanceof CxClearLabelsCacheEvent })
    }

    // -------------------------------------------------------------------------
    // isFrontendTranslationAllowedForUser
    // -------------------------------------------------------------------------

    def "isFrontendTranslationAllowedForUser returns false when user is null"() {
        expect:
        service.isFrontendTranslationAllowedForUser(null) == false
    }

    def "isFrontendTranslationAllowedForUser returns true when user is in allowed group"() {
        given:
        def user = Mock(UserModel)
        def group = Mock(PrincipalGroupModel) { getUid() >> "translators" }
        user.getAllGroups() >> [group]

        expect:
        service.isFrontendTranslationAllowedForUser(user) == true
    }

    def "isFrontendTranslationAllowedForUser returns false when user is not in any allowed group"() {
        given:
        def user = Mock(UserModel)
        def group = Mock(PrincipalGroupModel) { getUid() >> "viewers" }
        user.getAllGroups() >> [group]

        expect:
        service.isFrontendTranslationAllowedForUser(user) == false
    }

    // -------------------------------------------------------------------------
    // setUserGroupsAllowedForFrontendTranslation parsing
    // -------------------------------------------------------------------------

    def "setUserGroupsAllowedForFrontendTranslation handles blank config as empty set"() {
        given:
        service.setUserGroupsAllowedForFrontendTranslation("  ")
        def user = Mock(UserModel)
        user.getAllGroups() >> [Mock(PrincipalGroupModel) { getUid() >> "anyone" }]

        expect:
        service.isFrontendTranslationAllowedForUser(user) == false
    }
}
