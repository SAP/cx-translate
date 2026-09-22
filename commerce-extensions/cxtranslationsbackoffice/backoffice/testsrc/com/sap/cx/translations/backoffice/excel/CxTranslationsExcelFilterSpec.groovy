/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.excel

import com.hybris.backoffice.excel.template.filter.ExcelFilter
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.type.AttributeDescriptorModel
import de.hybris.platform.core.model.type.ComposedTypeModel
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxTranslationsExcelFilterSpec extends Specification {

    @Subject
    CxTranslationsExcelFilter filter = new CxTranslationsExcelFilter()

    private AttributeDescriptorModel attr(String typeCode, String qualifier) {
        def type = Mock(ComposedTypeModel) { getCode() >> typeCode }
        return Mock(AttributeDescriptorModel) {
            getEnclosingType() >> type
            getQualifier() >> qualifier
        }
    }

    // -------------------------------------------------------------------------
    // Default state — no map configured: filter is permissive
    // -------------------------------------------------------------------------

    def "test returns true when attributesForTypeMap was never set (default empty map)"() {
        expect:
        filter.test(attr("Product", "name")) == true
    }

    def "test returns true when setter is called with null (defensively treated as empty)"() {
        given:
        filter.setAttributesForTypeMap(null)

        expect:
        filter.test(attr("Product", "name")) == true
    }

    // -------------------------------------------------------------------------
    // Type not in map → no filter, attribute passes through
    // -------------------------------------------------------------------------

    def "test returns true for type not present in attributesForTypeMap"() {
        given:
        filter.setAttributesForTypeMap(["Category": ["description"] as Set])

        expect:
        filter.test(attr("Product", "name")) == true
    }

    // -------------------------------------------------------------------------
    // Type in map → only listed qualifiers pass
    // -------------------------------------------------------------------------

    def "test returns true when attribute qualifier is in the allowed set for its type"() {
        given:
        filter.setAttributesForTypeMap(["Product": ["name", "summary"] as Set])

        expect:
        filter.test(attr("Product", "name")) == true
        filter.test(attr("Product", "summary")) == true
    }

    def "test returns false when attribute qualifier is NOT in the allowed set for its type"() {
        given:
        filter.setAttributesForTypeMap(["Product": ["name"] as Set])

        expect:
        filter.test(attr("Product", "description")) == false
    }
}
