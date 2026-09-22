/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.cache

import com.sap.cx.translations.cache.impl.CxLocalizedLabelCacheKeyProviderImpl
import com.sap.cx.translations.labels.UsageType
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.commerceservices.i18n.CommerceCommonI18NService
import de.hybris.platform.core.PK
import de.hybris.platform.core.model.c2l.LanguageModel
import spock.lang.Specification
import spock.lang.Subject

/**
 * Unit tests for {@link CxLocalizedLabelCacheKeyProviderImpl}
 */
@UnitTest
class CxLocalizedLabelCacheKeyProviderImplSpec extends Specification {

    static final String TENANT_ID = "junit"
    static final String LANG_ISO = "en"
    static final String CATALOG_VERSION_ID = "8796093054977"
    static final String LABEL_KEY = "checkout.title"
    static final String BUNDLE_KEY = "checkout"

    CommerceCommonI18NService commerceCommonI18NService = Mock()
    LanguageModel language = Mock()
    CatalogVersionModel catalogVersion = Mock()

    /**
     * Testable subclass that bypasses the static Registry call by overriding
     * the now-protected getCurrentTenantId() hook.
     */
    @Subject
    CxLocalizedLabelCacheKeyProviderImpl provider = new CxLocalizedLabelCacheKeyProviderImpl() {
        @Override
        protected String getCurrentTenantId() {
            return TENANT_ID
        }
    }

    def setup() {
        provider.setCommerceCommonI18NService(commerceCommonI18NService)

        commerceCommonI18NService.getCurrentLanguage() >> language
        language.getIsocode() >> LANG_ISO

        catalogVersion.getPk() >> PK.fromLong(Long.parseLong(CATALOG_VERSION_ID))
    }

    // ---------- getSingleLabelKey — happy path ----------

    def "getSingleLabelKey returns CxLocalizedLabelCacheKey with correctly formatted key"() {
        when:
        def result = provider.getSingleLabelKey(LABEL_KEY, UsageType.FRONTEND, catalogVersion)

        then:
        result instanceof CxLocalizedLabelCacheKey
        (result as CxLocalizedLabelCacheKey).toString()
                .contains("${LABEL_KEY}_${LANG_ISO}_${CATALOG_VERSION_ID}_FRONTEND")
    }

    def "getSingleLabelKey key string is labelKey_lang_catalogVersionId_usageType"() {
        when:
        def key1 = provider.getSingleLabelKey(LABEL_KEY, UsageType.FRONTEND, catalogVersion)
        def key2 = new CxLocalizedLabelCacheKey(
                "${LABEL_KEY}_${LANG_ISO}_${CATALOG_VERSION_ID}_FRONTEND", TENANT_ID)

        then:
        key1 == key2
    }

    // ---------- getSingleLabelKey — usageType null ----------

    def "getSingleLabelKey with null usageType uses ALL as usage type segment"() {
        when:
        def key1 = provider.getSingleLabelKey(LABEL_KEY, null, catalogVersion)
        def key2 = new CxLocalizedLabelCacheKey(
                "${LABEL_KEY}_${LANG_ISO}_${CATALOG_VERSION_ID}_ALL", TENANT_ID)

        then:
        key1 == key2
    }

    // ---------- getBundleKey — single usageType ----------

    def "getBundleKey with single usageType key ends with the enum name (no pipe)"() {
        when:
        def key1 = provider.getBundleKey(BUNDLE_KEY, [UsageType.BACKEND] as UsageType[], catalogVersion)
        def key2 = new CxLocalizedBundleCacheKey(
                "${BUNDLE_KEY}_${LANG_ISO}_${CATALOG_VERSION_ID}_BACKEND", TENANT_ID)

        then:
        key1 == key2
    }

    // ---------- getBundleKey — multiple usageTypes ----------

    def "getBundleKey with multiple usageTypes key ends with pipe-joined enum names"() {
        when:
        def key1 = provider.getBundleKey(BUNDLE_KEY, [UsageType.FRONTEND, UsageType.BACKEND] as UsageType[], catalogVersion)
        def key2 = new CxLocalizedBundleCacheKey(
                "${BUNDLE_KEY}_${LANG_ISO}_${CATALOG_VERSION_ID}_FRONTEND|BACKEND", TENANT_ID)

        then:
        key1 == key2
    }

    // ---------- getBundleKey — null usageTypes ----------

    def "getBundleKey with null usageTypes uses ALL as usage type segment"() {
        when:
        def key1 = provider.getBundleKey(BUNDLE_KEY, null, catalogVersion)
        def key2 = new CxLocalizedBundleCacheKey(
                "${BUNDLE_KEY}_${LANG_ISO}_${CATALOG_VERSION_ID}_ALL", TENANT_ID)

        then:
        key1 == key2
    }

    // ---------- getBundleKey — empty usageTypes array ----------

    def "getBundleKey with empty usageTypes array produces empty usage type segment (documents edge case)"() {
        // NOTE: this is a documented edge case — passing an empty array yields an empty string
        // rather than "ALL", which may differ from the null case. Consider it a contract test.
        when:
        def key1 = provider.getBundleKey(BUNDLE_KEY, [] as UsageType[], catalogVersion)
        def key2 = new CxLocalizedBundleCacheKey(
                "${BUNDLE_KEY}_${LANG_ISO}_${CATALOG_VERSION_ID}_", TENANT_ID)

        then:
        key1 == key2
    }

    // ---------- getAllLabelsKey — type ----------

    def "getAllLabelsKey returns CxLocalizedBundleCacheKey (not CxLocalizedLabelCacheKey)"() {
        when:
        def result = provider.getAllLabelsKey([UsageType.FRONTEND] as UsageType[], catalogVersion)

        then:
        result instanceof CxLocalizedBundleCacheKey
        !(result instanceof CxLocalizedLabelCacheKey)
    }

    // ---------- getAllLabelsKey — key format ----------

    def "getAllLabelsKey key format is lang_catalogVersionId_usageTypeCode with no bundle prefix"() {
        when:
        def key1 = provider.getAllLabelsKey([UsageType.FRONTEND] as UsageType[], catalogVersion)
        def key2 = new CxLocalizedBundleCacheKey(
                "${LANG_ISO}_${CATALOG_VERSION_ID}_FRONTEND", TENANT_ID)

        then:
        key1 == key2
    }

    def "getAllLabelsKey with null usageTypes uses ALL as usage type segment"() {
        when:
        def key1 = provider.getAllLabelsKey(null, catalogVersion)
        def key2 = new CxLocalizedBundleCacheKey(
                "${LANG_ISO}_${CATALOG_VERSION_ID}_ALL", TENANT_ID)

        then:
        key1 == key2
    }

    // ---------- null language guard ----------

    def "getSingleLabelKey throws NullPointerException with message when current language is null"() {
        given:
        CommerceCommonI18NService nullLangService = Mock() { getCurrentLanguage() >> null }
        provider.setCommerceCommonI18NService(nullLangService)

        when:
        provider.getSingleLabelKey(LABEL_KEY, UsageType.FRONTEND, catalogVersion)

        then:
        def ex = thrown(NullPointerException)
        ex.message == "Current language must not be null"
    }

    def "getBundleKey throws NullPointerException with message when current language is null"() {
        given:
        CommerceCommonI18NService nullLangService = Mock() { getCurrentLanguage() >> null }
        provider.setCommerceCommonI18NService(nullLangService)

        when:
        provider.getBundleKey(BUNDLE_KEY, [UsageType.FRONTEND] as UsageType[], catalogVersion)

        then:
        def ex = thrown(NullPointerException)
        ex.message == "Current language must not be null"
    }

    def "getAllLabelsKey throws NullPointerException with message when current language is null"() {
        given:
        CommerceCommonI18NService nullLangService = Mock() { getCurrentLanguage() >> null }
        provider.setCommerceCommonI18NService(nullLangService)

        when:
        provider.getAllLabelsKey([UsageType.FRONTEND] as UsageType[], catalogVersion)

        then:
        def ex = thrown(NullPointerException)
        ex.message == "Current language must not be null"
    }

    // ---------- tenant isolation ----------

    def "keys built with different tenant IDs are not equal"() {
        given:
        CxLocalizedLabelCacheKeyProviderImpl otherProvider = new CxLocalizedLabelCacheKeyProviderImpl() {
            @Override
            protected String getCurrentTenantId() {
                return "otherTenant"
            }
        }
        otherProvider.setCommerceCommonI18NService(commerceCommonI18NService)

        when:
        def key1 = provider.getSingleLabelKey(LABEL_KEY, UsageType.FRONTEND, catalogVersion)
        def key2 = otherProvider.getSingleLabelKey(LABEL_KEY, UsageType.FRONTEND, catalogVersion)

        then:
        key1 != key2
        key1.getTenantId() == TENANT_ID
        key2.getTenantId() == "otherTenant"
        key1.getTypeCode() == key2.getTypeCode()
    }
}

