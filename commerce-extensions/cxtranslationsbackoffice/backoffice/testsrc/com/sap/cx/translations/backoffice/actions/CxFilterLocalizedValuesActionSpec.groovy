/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.actions

import com.hybris.cockpitng.actions.ActionContext
import com.sap.cx.translations.enums.CxTranslateBackofficeSearchInclusion
import com.sap.cx.translations.model.CxLocalizedObjectConfigModel
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService
import de.hybris.bootstrap.annotations.UnitTest
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxFilterLocalizedValuesActionSpec extends Specification {

    CxLocalizedValueConfigService configService = Mock()

    @Subject
    CxFilterLocalizedValuesAction action = new CxFilterLocalizedValuesAction()

    def setup() {
        action.setCxLocalizedValueConfigService(configService)
    }

    // -------------------------------------------------------------------------
    // The fix: service is injected via setter, no Registry.getApplicationContext() call
    // -------------------------------------------------------------------------

    def "getOptionsToChoose includes only EXCLUDED_BY_DEFAULT configs"() {
        given:
        def included = Mock(CxLocalizedObjectConfigModel) {
            getUseInBackofficeSearch() >> CxTranslateBackofficeSearchInclusion.INCLUDED
            getCode() >> "Included"
            getName() >> "Included Config"
        }
        def excluded = Mock(CxLocalizedObjectConfigModel) {
            getUseInBackofficeSearch() >> CxTranslateBackofficeSearchInclusion.EXCLUDED_BY_DEFAULT
            getCode() >> "Excluded"
            getName() >> "Excluded Config"
        }
        configService.findAllConfigs() >> [included, excluded]

        when:
        def options = action.getOptionsToChoose()

        then:
        options.size() == 1
        options["Excluded"] == "Excluded Config"
        !options.containsKey("Included")
    }

    def "getOptionsToChoose returns empty map when no configs exist"() {
        given:
        configService.findAllConfigs() >> []

        expect:
        action.getOptionsToChoose().isEmpty()
    }

    def "getOptionsToChoose returns unmodifiable map"() {
        given:
        configService.findAllConfigs() >> []

        when:
        def options = action.getOptionsToChoose()
        options.put("k", "v")

        then:
        thrown(UnsupportedOperationException)
    }

    def "canPerform returns false when no excluded-by-default configs exist"() {
        given:
        configService.findAllConfigs() >> []

        expect:
        action.canPerform(Mock(ActionContext)) == false
    }

    def "canPerform returns true when at least one excluded-by-default config exists"() {
        given:
        def excluded = Mock(CxLocalizedObjectConfigModel) {
            getUseInBackofficeSearch() >> CxTranslateBackofficeSearchInclusion.EXCLUDED_BY_DEFAULT
            getCode() >> "Excluded"
            getName() >> "Excluded Config"
        }
        configService.findAllConfigs() >> [excluded]

        expect:
        action.canPerform(Mock(ActionContext)) == true
    }
}
