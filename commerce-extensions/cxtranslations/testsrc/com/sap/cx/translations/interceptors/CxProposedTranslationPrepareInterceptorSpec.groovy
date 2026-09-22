/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.interceptors

import com.sap.cx.translations.enums.CxTranslationStatus
import com.sap.cx.translations.model.CxProposedTranslationModel
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.servicelayer.interceptor.InterceptorContext
import spock.lang.Specification
import spock.lang.Subject

import java.util.Locale

@UnitTest
class CxProposedTranslationPrepareInterceptorSpec extends Specification {

    InterceptorContext ctx = Mock()

    @Subject
    CxProposedTranslationPrepareInterceptor interceptor = new CxProposedTranslationPrepareInterceptor()

    def "sets status to MANUAL when MANUALTRANSLATIONVALUE is dirty on an existing model"() {
        given:
        def model = Mock(CxProposedTranslationModel)
        ctx.isNew(model) >> false
        ctx.isRemoved(model) >> false
        ctx.getDirtyAttributes(model) >> [(CxProposedTranslationModel.MANUALTRANSLATIONVALUE): [Locale.ENGLISH] as Set]

        when:
        interceptor.onPrepare(model, ctx)

        then:
        1 * model.setTranslationStatus(CxTranslationStatus.MANUAL)
    }

    def "does not set status on a new model even if MANUALTRANSLATIONVALUE is dirty (new models are skipped)"() {
        given:
        def model = Mock(CxProposedTranslationModel)
        ctx.isNew(model) >> true
        ctx.isRemoved(model) >> false

        when:
        interceptor.onPrepare(model, ctx)

        then:
        0 * model.setTranslationStatus(_)
    }

    def "does not set status when MANUALTRANSLATIONVALUE is not dirty"() {
        given:
        def model = Mock(CxProposedTranslationModel)
        ctx.isNew(model) >> false
        ctx.isRemoved(model) >> false
        ctx.getDirtyAttributes(model) >> ["otherAttribute": [Locale.ENGLISH] as Set]

        when:
        interceptor.onPrepare(model, ctx)

        then:
        0 * model.setTranslationStatus(_)
    }

    def "does nothing when model is removed"() {
        given:
        def model = Mock(CxProposedTranslationModel)
        ctx.isRemoved(model) >> true

        when:
        interceptor.onPrepare(model, ctx)

        then:
        0 * ctx.getDirtyAttributes(_)
        0 * model.setTranslationStatus(_)
    }

    def "does nothing when dirty attributes map is empty"() {
        given:
        def model = Mock(CxProposedTranslationModel)
        ctx.isNew(model) >> false
        ctx.isRemoved(model) >> false
        ctx.getDirtyAttributes(model) >> [:]

        when:
        interceptor.onPrepare(model, ctx)

        then:
        0 * model.setTranslationStatus(_)
    }
}
