/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.services.localizedobjects

import com.sap.cx.translations.services.impl.localizedobjects.CxLocalizedClassificationServiceImpl
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.catalog.model.ProductFeatureModel
import de.hybris.platform.catalog.model.classification.ClassAttributeAssignmentModel
import de.hybris.platform.core.model.c2l.LanguageModel
import spock.lang.Specification

/**
 * Unit tests for the pure-logic methods of {@link CxLocalizedClassificationServiceImpl}
 * that cannot be verified through the integration test (because the Hybris model layer blocks
 * writing null to non-writable attributes like {@code language} and
 * {@code classificationAttributeAssignment}).
 *
 * Uses Spock mocks to simulate model state that the DB would normally prevent.
 */
@UnitTest
class CxLocalizedClassificationServiceImplSpec extends Specification {

    // -----------------------------------------------------------------------
    // isFeatureTranslatable — static guard method
    // -----------------------------------------------------------------------

    def "isFeatureTranslatable returns false when classificationAttributeAssignment is null"() {
        given:
        def feature = Mock(ProductFeatureModel)
        feature.classificationAttributeAssignment >> null

        expect:
        !CxLocalizedClassificationServiceImpl.isFeatureTranslatable(feature)
    }

    def "isFeatureTranslatable returns false when assignment.localized is false"() {
        given:
        def assignment = Mock(ClassAttributeAssignmentModel)
        assignment.localized >> false

        def feature = Mock(ProductFeatureModel)
        feature.classificationAttributeAssignment >> assignment
        feature.language >> Mock(LanguageModel)
        feature.value >> "some text"

        expect:
        !CxLocalizedClassificationServiceImpl.isFeatureTranslatable(feature)
    }

    def "isFeatureTranslatable returns false when language is null"() {
        given:
        def assignment = Mock(ClassAttributeAssignmentModel)
        assignment.localized >> true

        def feature = Mock(ProductFeatureModel)
        feature.classificationAttributeAssignment >> assignment
        feature.language >> null     // <-- the edge case the integration test cannot set
        feature.value >> "some text"

        expect:
        // Desired behaviour: feature without language is not translatable.
        !CxLocalizedClassificationServiceImpl.isFeatureTranslatable(feature)
    }

    def "isFeatureTranslatable returns false when value is not a String"() {
        given:
        def assignment = Mock(ClassAttributeAssignmentModel)
        assignment.localized >> true

        def feature = Mock(ProductFeatureModel)
        feature.classificationAttributeAssignment >> assignment
        feature.language >> Mock(LanguageModel)
        feature.value >> 42  // non-String value (e.g. numeric feature)

        expect:
        !CxLocalizedClassificationServiceImpl.isFeatureTranslatable(feature)
    }

    def "isFeatureTranslatable returns true when all conditions are met"() {
        given:
        def assignment = Mock(ClassAttributeAssignmentModel)
        assignment.localized >> true

        def feature = Mock(ProductFeatureModel)
        feature.classificationAttributeAssignment >> assignment
        feature.language >> Mock(LanguageModel)
        feature.value >> "Red"

        expect:
        CxLocalizedClassificationServiceImpl.isFeatureTranslatable(feature)
    }

    // -----------------------------------------------------------------------
    // getRelatedFeatures — null classificationAttributeAssignment guard
    // -----------------------------------------------------------------------

    def "getRelatedFeatures returns empty stream when classificationAttributeAssignment is null"() {
        given:
        def impl = new CxLocalizedClassificationServiceImpl()

        def feature = Mock(ProductFeatureModel)
        feature.classificationAttributeAssignment >> null  // <-- integration test cannot set this

        when:
        def result = impl.getRelatedFeatures(feature).toList()

        then:
        // Desired behaviour: null assignment → empty stream, no NPE.
        result.isEmpty()
    }
}
