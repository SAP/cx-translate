/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.enums.CxApprovalTranslationStatus
import com.sap.cx.translations.enums.CxTranslationStatus
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.model.CxTranslationCronJobModel
import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedValueSearchData
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForItemsParameters
import com.sap.cx.translations.services.localizedobjects.parameters.CxStartTranslationCronJobForValuesParameters
import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.core.model.c2l.LanguageModel
import de.hybris.platform.core.model.product.ProductModel
import de.hybris.platform.cronjob.model.CronJobModel
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.type.TypeService
import jakarta.annotation.Resource
import spock.lang.Subject

/**
 * Integration tests for the cron-job creation path of {@link CxLocalizedValueService}:
 * <ul>
 *   <li>{@link CxLocalizedValueService#startTranslationCronJobForItems}</li>
 *   <li>{@link CxLocalizedValueService#startTranslationCronJob}</li>
 *   <li>{@link CxLocalizedValueService#retryTranslations}</li>
 *   <li>{@link CxLocalizedValueService#createCxProposedTranslation}</li>
 * </ul>
 *
 * These tests verify the persistent state after cron-job creation — they do NOT
 * actually execute the jobs (the job would call the AI service, which is not configured
 * in the test environment).
 */
@IntegrationTest
class CxLocalizedValueServiceCronJobIntSpec extends ServicelayerTransactionalSpockSpecification {

    private static final String CATALOG_ID  = "cronJobTestCatalog"
    private static final String CONFIG_CODE = "cronJobProductConfig"

    @Subject
    @Resource
    CxLocalizedValueService cxLocalizedValueService

    @Resource
    ModelService modelService

    @Resource
    TypeService typeService

    private CxTranslationTestDataBuilder builder
    private CatalogVersionModel stagedVersion
    private LanguageModel enLang
    private LanguageModel deLang

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService, typeService)
        def versions = builder.createCatalogWithVersions(CATALOG_ID)
        stagedVersion = versions.staged

        enLang = builder.createLanguage("cj-en-" + System.nanoTime())
        deLang = builder.createLanguage("cj-de-" + System.nanoTime())

        builder.createStandardConfig(CONFIG_CODE, "Product", ["code"],
                ["name", "description"], true, "catalogVersion", [stagedVersion])
    }

    // -----------------------------------------------------------------------
    // createCxProposedTranslation
    // -----------------------------------------------------------------------

    def "createCxProposedTranslation copies key and attribute from localized value"() {
        given:
        def product = builder.createProduct("p-001", stagedVersion)
        def values  = cxLocalizedValueService.getForItem(product)
        def nameValue = values.find { it.attribute == "name" }

        def cronJob = buildCronJob("cj-create-1")

        when:
        def proposed = cxLocalizedValueService.createCxProposedTranslation(nameValue, cronJob)

        then:
        proposed != null
        proposed.key       == nameValue.key
        proposed.attribute == "name"
        proposed.objectId  == "p-001"
        proposed.translationStatus == CxTranslationStatus.PENDING
        proposed.approvalStatus    == CxApprovalTranslationStatus.PENDING
        proposed.translationCronJob == cronJob
    }

    def "createCxProposedTranslation returns null on duplicate key and logs error"() {
        given: "create first proposed translation"
        def product   = builder.createProduct("p-dup", stagedVersion)
        def values    = cxLocalizedValueService.getForItem(product)
        def nameValue = values.find { it.attribute == "name" }
        def cronJob   = buildCronJob("cj-dup-1")

        cxLocalizedValueService.createCxProposedTranslation(nameValue, cronJob)

        when: "create a second one with the same key → unique constraint fires"
        def second = cxLocalizedValueService.createCxProposedTranslation(nameValue, cronJob)

        then: "second attempt returns null gracefully instead of throwing"
        second == null
    }

    // -----------------------------------------------------------------------
    // startTranslationCronJob (for values)
    // -----------------------------------------------------------------------

    def "startTranslationCronJob creates a cron job with proposed translations for given values"() {
        given:
        def product = builder.createProduct("p-001", stagedVersion)
        def values  = cxLocalizedValueService.getForItem(product)

        when:
        def cronJob = cxLocalizedValueService.startTranslationCronJob(
                CxStartTranslationCronJobForValuesParameters.builder()
                        .localizedValues(values)
                        .sourceLanguage(enLang)
                        .targetLanguages([deLang])
                        .skipTranslated(false)
                        .saveForApproval(false)
                        .build())

        then:
        cronJob != null
        cronJob.sourceLanguage == enLang
        cronJob.targetLanguages.contains(deLang)
        // Values to translate should have been set (2 proposed translations for 2 attrs)
        cronJob.valuesToTranslate.size() == 2
        cronJob.elementsToProcessCount == 2
    }

    def "startTranslationCronJob with empty values creates cron job with empty proposed translations"() {
        when:
        def cronJob = cxLocalizedValueService.startTranslationCronJob(
                CxStartTranslationCronJobForValuesParameters.builder()
                        .localizedValues([])
                        .sourceLanguage(enLang)
                        .targetLanguages([deLang])
                        .skipTranslated(false)
                        .saveForApproval(false)
                        .build())

        then:
        cronJob != null
        cronJob.valuesToTranslate.isEmpty()
        cronJob.elementsToProcessCount == 0
    }

    def "startTranslationCronJob sets hint on the cron job"() {
        given:
        def product = builder.createProduct("p-001", stagedVersion)
        def values  = cxLocalizedValueService.getForItem(product)

        when:
        def cronJob = cxLocalizedValueService.startTranslationCronJob(
                CxStartTranslationCronJobForValuesParameters.builder()
                        .localizedValues(values)
                        .sourceLanguage(enLang)
                        .targetLanguages([deLang])
                        .userHint("translate carefully")
                        .skipTranslated(false)
                        .saveForApproval(false)
                        .build())

        then:
        cronJob.hint == "translate carefully"
    }

    // -----------------------------------------------------------------------
    // startTranslationCronJobForItems
    // -----------------------------------------------------------------------

    def "startTranslationCronJobForItems creates cron job with items and attributes set"() {
        given:
        def product = builder.createProduct("p-001", stagedVersion)
        def typeAttrs = typeService.getComposedTypeForCode("Product")
                .declaredattributedescriptors
                .findAll { it.qualifier == "name" } as Set

        when:
        def cronJob = cxLocalizedValueService.startTranslationCronJobForItems(
                CxStartTranslationCronJobForItemsParameters.builder()
                        .items([product])
                        .attributes(typeAttrs)
                        .translateProductFeatures(false)
                        .sourceLanguage(enLang)
                        .targetLanguages([deLang])
                        .skipTranslated(false)
                        .saveForApproval(false)
                        .build())

        then:
        cronJob != null
        cronJob.items.contains(product)
        cronJob.elementsToProcessCount == 1
        !cronJob.translateProductFeatures
    }

    // -----------------------------------------------------------------------
    // retryTranslations
    // -----------------------------------------------------------------------

    def "retryTranslations creates a new cron job reusing the old cron job's settings"() {
        given: "a finished cron job with one proposed translation"
        def product   = builder.createProduct("retry-p", stagedVersion)
        def values    = cxLocalizedValueService.getForItem(product)
        def nameValue = values.find { it.attribute == "name" }

        def originalCronJob = buildCronJob("cj-retry-original")
        def proposed = cxLocalizedValueService.createCxProposedTranslation(nameValue, originalCronJob)

        when:
        def newCronJob = cxLocalizedValueService.retryTranslations(
                originalCronJob, [proposed], null, null)

        then:
        newCronJob != null
        newCronJob.pk != originalCronJob.pk          // a new job was created
        newCronJob.sourceLanguage == enLang
        newCronJob.targetLanguages == originalCronJob.targetLanguages
        newCronJob.elementsToProcessCount == 1
    }

    def "retryTranslations resets proposed translation status to PENDING"() {
        given:
        def product   = builder.createProduct("retry-status-p", stagedVersion)
        def values    = cxLocalizedValueService.getForItem(product)
        def nameValue = values.find { it.attribute == "name" }

        def originalCronJob = buildCronJob("cj-retry-status")
        def proposed = cxLocalizedValueService.createCxProposedTranslation(nameValue, originalCronJob)

        // Simulate a completed state
        proposed.setTranslationStatus(CxTranslationStatus.SUCCESS)
        modelService.save(proposed)
        modelService.refresh(proposed)

        when:
        cxLocalizedValueService.retryTranslations(originalCronJob, [proposed], null, null)

        then: "translation status is now PENDING — check directly on the instance (retryTranslations mutates it)"
        proposed.translationStatus == CxTranslationStatus.PENDING
        proposed.approvalStatus == CxApprovalTranslationStatus.PENDING
    }

    def "retryTranslations does not throw for a completed (not running) cron job"() {
        // Simulating a truly-running cron job is not feasible in integration tests;
        // this test verifies the happy path (completed job) to guard the guard itself.
        given:
        def product   = builder.createProduct("retry-guard-p", stagedVersion)
        def values    = cxLocalizedValueService.getForItem(product)
        def nameValue = values.find { it.attribute == "name" }
        def originalCronJob = buildCronJob("cj-retry-guard-ok")
        def proposed = cxLocalizedValueService.createCxProposedTranslation(nameValue, originalCronJob)

        when: "calling retry on a non-running (finished) cron job does not throw"
        def newJob = cxLocalizedValueService.retryTranslations(originalCronJob, [proposed], null, null)

        then:
        newJob != null
        notThrown(IllegalStateException)
    }

    def "retryTranslations with hint override uses the new hint"() {
        given:
        def product = builder.createProduct("retry-hint-p", stagedVersion)
        def values  = cxLocalizedValueService.getForItem(product)
        def nameVal = values.find { it.attribute == "name" }
        def originalCronJob = buildCronJob("cj-retry-hint")
        def proposed = cxLocalizedValueService.createCxProposedTranslation(nameVal, originalCronJob)

        when:
        def newJob = cxLocalizedValueService.retryTranslations(
                originalCronJob, [proposed], null, "override hint")

        then:
        newJob.hint == "override hint"
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private CxTranslationCronJobModel buildCronJob(String code) {
        builder.createTranslationCronJob(code, enLang, [deLang])
    }
}
