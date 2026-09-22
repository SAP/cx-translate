/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.interceptors

import com.sap.cx.translations.model.CxLocalizedObjectConfigCronJobModel
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.servicelayer.interceptor.InterceptorContext
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxLocalizedObjectConfigurationCronJobInitDefaultsInterceptorSpec extends Specification {

    CxLocalizedValueConfigService configService = Mock()
    InterceptorContext ctx = Mock()

    @Subject
    CxLocalizedObjectConfigurationCronJobInitDefaultsInterceptor interceptor =
            new CxLocalizedObjectConfigurationCronJobInitDefaultsInterceptor()

    def setup() {
        interceptor.setCxLocalizedValueConfigService(configService)
    }

    def "onInitDefaults delegates to configService to configure the cron job"() {
        given:
        def cronJob = Mock(CxLocalizedObjectConfigCronJobModel)

        when:
        interceptor.onInitDefaults(cronJob, ctx)

        then:
        1 * configService.configureLocalizedObjectConfigCronJob(cronJob)
    }
}
