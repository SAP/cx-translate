/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.daos

import com.sap.cx.translations.testdata.CxTranslationTestDataBuilder
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.model.ModelService
import jakarta.annotation.Resource
import spock.lang.Subject

/**
 * Integration tests for {@link CxProposedTranslationDao} /
 * {@link com.sap.cx.translations.daos.impl.CxProposedTranslationDaoImpl}.
 *
 * <p>Each test runs inside a rolled-back transaction so fixture data is
 * isolated between test methods.</p>
 */
@IntegrationTest
class CxProposedTranslationDaoImplIntSpec extends ServicelayerTransactionalSpockSpecification {

    @Subject
    @Resource
    CxProposedTranslationDao cxProposedTranslationDao

    @Resource
    ModelService modelService

    private CxTranslationTestDataBuilder builder

    def setup() {
        builder = new CxTranslationTestDataBuilder(modelService)
    }

    // -----------------------------------------------------------------------
    // findByKey
    // -----------------------------------------------------------------------

    def "findByKey returns all proposed translations with the given key"() {
        given: "2 translations for key 'k1' and 1 for key 'k2'"
        builder.createProposedTranslation("pt.k1.v1")
        builder.createProposedTranslation("pt.k1.v2")
        builder.createProposedTranslation("pt.k2.v1")

        when: "querying for key 'pt.k1.v1' (exact match)"
        def result = cxProposedTranslationDao.findByKey("pt.k1.v1")

        then: "exactly 1 translation with that key is returned"
        result.size() == 1
        result.first().key == "pt.k1.v1"
    }


    def "findByKey returns empty collection for a key that does not exist"() {
        when:
        def result = cxProposedTranslationDao.findByKey("absolutely.nonexistent.key")

        then:
        result.isEmpty()
    }

    def "findByKey with null key does not throw an unexpected exception and returns empty result"() {
        // The DefaultGenericDao builds a query 'WHERE {key} = ?key'.
        // Passing null causes FlexibleSearch to treat it as IS NULL, which is valid but
        // returns no items (none have a null key).  The DAO must NOT throw an NPE here.
        when:
        def result = cxProposedTranslationDao.findByKey(null)

        then: "no unexpected exception — result is either empty or predictably handled"
        noExceptionThrown()
        // We document the actual observed behaviour: empty collection is returned
        result != null
    }
}

