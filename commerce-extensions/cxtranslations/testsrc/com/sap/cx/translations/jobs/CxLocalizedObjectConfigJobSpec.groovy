/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.jobs

import com.sap.cx.translations.model.CxLocalizedObjectConfigCronJobModel
import com.sap.cx.translations.model.CxStandardLocalizedObjectConfigModel
import com.sap.cx.translations.services.config.CxLocalizedValueConfigService
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.enumeration.EnumerationValueModel
import de.hybris.platform.core.model.type.AttributeDescriptorModel
import de.hybris.platform.core.model.type.ComposedTypeModel
import de.hybris.platform.cronjob.enums.CronJobResult
import de.hybris.platform.cronjob.enums.CronJobStatus
import de.hybris.platform.cronjob.jalo.AbstractProgressTracker
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.search.FlexibleSearchService
import de.hybris.platform.servicelayer.search.SearchResult
import de.hybris.platform.servicelayer.type.TypeService
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxLocalizedObjectConfigJobSpec extends Specification {

    ModelService modelService = Mock()
    CxLocalizedValueConfigService configService = Mock()
    TypeService typeService = Mock()
    FlexibleSearchService flexibleSearchService = Mock()

    AbstractProgressTracker tracker = new AbstractProgressTracker() {
        @Override void storeProgressInDB(Double progress) { /* no-op */ }
        @Override void close() { /* no-op */ }
    }

    @Subject
    CxLocalizedObjectConfigJob job = new CxLocalizedObjectConfigJob() {
        @Override
        protected AbstractProgressTracker createProgressTracker(CxLocalizedObjectConfigCronJobModel cronJob) {
            return tracker
        }
    }

    CxLocalizedObjectConfigCronJobModel cronJob = Mock()
    ComposedTypeModel enumType = Mock() { getCode() >> EnumerationValueModel._TYPECODE }

    def setup() {
        job.setCxLocalizedValueConfigService(configService)
        job.setTypeService(typeService)
        job.modelService = modelService
        job.flexibleSearchService = flexibleSearchService

        typeService.getComposedTypeForCode(EnumerationValueModel._TYPECODE) >> enumType
        flexibleSearchService.search(_) >> Mock(SearchResult) { getResult() >> [] }
        cronJob.getLocalizedObjectCategories() >> []
        // NOTE: do not stub configService.findConfigByType here — would shadow per-test stubs
        // for the enum-config-creation flow
    }

    def "isAbortable returns true"() {
        expect:
        job.isAbortable()
    }

    def "executeAutoConfiguration: saves type list when includedComposedTypes is empty"() {
        given:
        def type = Mock(ComposedTypeModel) { getCode() >> "Product" }
        cronJob.getIncludedComposedTypes() >> []
        configService.findTypesToConsider() >> [type]
        configService.buildConfig(type, cronJob) >> ([] as Set)
        configService.findConfigByType(enumType) >> Mock(CxStandardLocalizedObjectConfigModel)

        when:
        def ok = job.executeAutoConfiguration(cronJob, tracker)

        then:
        ok == true
        1 * cronJob.setIncludedComposedTypes([type])
        // cronJob is saved at least once: after setting included types,
        // and per-iteration to persist progress counters
        (1.._) * modelService.save(cronJob)
    }

    def "executeAutoConfiguration: returns true when all types configure without errors"() {
        given:
        def type = Mock(ComposedTypeModel) { getCode() >> "Product" }
        cronJob.getIncludedComposedTypes() >> [type]
        configService.buildConfig(type, cronJob) >> ([] as Set)
        configService.findConfigByType(enumType) >> Mock(CxStandardLocalizedObjectConfigModel)

        expect:
        job.executeAutoConfiguration(cronJob, tracker) == true
    }

    def "executeAutoConfiguration: returns false and persists counters when buildConfig throws"() {
        given:
        def type = Mock(ComposedTypeModel) { getCode() >> "Product" }
        cronJob.getIncludedComposedTypes() >> [type]
        configService.buildConfig(type, cronJob) >> { throw new RuntimeException("boom") }
        configService.findConfigByType(enumType) >> Mock(CxStandardLocalizedObjectConfigModel)

        when:
        def ok = job.executeAutoConfiguration(cronJob, tracker)

        then:
        ok == false
        1 * cronJob.setFailedTypes(1)
        1 * cronJob.setAlreadyProcessedTypes(1)
    }

    def "perform: returns SUCCESS end-to-end"() {
        given:
        def type = Mock(ComposedTypeModel) { getCode() >> "Product" }
        cronJob.getIncludedComposedTypes() >> [type]
        configService.buildConfig(type, cronJob) >> ([] as Set)
        configService.findConfigByType(enumType) >> Mock(CxStandardLocalizedObjectConfigModel)

        when:
        def result = job.perform(cronJob)

        then:
        result.result == CronJobResult.SUCCESS
        result.status == CronJobStatus.FINISHED
    }

    def "creates and saves enum configuration when it does not exist"() {
        given:
        cronJob.getIncludedComposedTypes() >> []
        configService.findTypesToConsider() >> []
        configService.findConfigByType(enumType) >> null
        def enumConfig = Mock(CxStandardLocalizedObjectConfigModel)
        modelService.create(_) >> enumConfig
        typeService.getAttributeDescriptor(enumType, EnumerationValueModel.NAME) >> Mock(AttributeDescriptorModel)

        when:
        job.executeAutoConfiguration(cronJob, tracker)

        then:
        1 * modelService.save(enumConfig)
    }
}
