/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.actions

import com.hybris.cockpitng.actions.ActionContext
import com.hybris.cockpitng.actions.ActionResult
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectDeletionException
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectSavingException
import com.hybris.cockpitng.dataaccess.facades.permissions.PermissionFacade
import com.hybris.cockpitng.util.notifications.NotificationService
import com.hybris.cockpitng.util.notifications.event.NotificationEvent
import com.sap.cx.translations.enums.CxTranslationStatus
import com.sap.cx.translations.model.CxProposedTranslationModel
import com.sap.cx.translations.services.CxApproveTranslationsService
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.ItemModel
import spock.lang.Specification
import spock.lang.Subject

@UnitTest
class CxApproveAiTranslationActionSpec extends Specification {

    ObjectFacade objectFacade = Mock()
    CxApproveTranslationsService approveService = Mock()
    PermissionFacade permissionFacade = Mock()
    NotificationService notificationService = Mock()
    ActionContext<CxProposedTranslationModel> ctx = Mock()
    CxProposedTranslationModel proposedTranslation = Mock()
    ItemModel target = Mock()

    @Subject
    CxApproveAiTranslationAction action = new CxApproveAiTranslationAction() {
        // Override sendOutput to suppress NPE — componentWidgetAdapter is only wired by ZK at runtime.
        @Override
        public void sendOutput(final String name, final Object value) {
            // no-op in tests
        }
    }

    def setup() {
        action.objectFacade = objectFacade
        action.cxApproveTranslationsService = approveService
        action.permissionFacade = permissionFacade
        action.notificationService = notificationService
        ctx.getData() >> proposedTranslation
        proposedTranslation.getKey() >> "key1"
        proposedTranslation.getTarget() >> target
    }

    // -------------------------------------------------------------------------
    // Save failure: returns ERROR, never proceeds to approval
    // -------------------------------------------------------------------------

    def "perform returns ERROR and notifies FAILURE when save throws ObjectSavingException"() {
        given:
        objectFacade.save(proposedTranslation) >> { throw new ObjectSavingException("save failed", null) }

        when:
        def result = action.perform(ctx)

        then:
        result.resultCode == ActionResult.ERROR
        // Approval and deletion must NOT be attempted after save failure
        0 * approveService.approveTranslation(_)
        0 * objectFacade.delete(_)
        1 * notificationService.notifyUser(ctx, _, NotificationEvent.Level.FAILURE, _)
    }

    // -------------------------------------------------------------------------
    // Delete failure after successful approval: returns ERROR (not silent SUCCESS)
    // -------------------------------------------------------------------------

    def "perform returns ERROR when approval succeeds but delete throws ObjectDeletionException"() {
        given:
        approveService.approveTranslation(proposedTranslation) >> true
        objectFacade.delete(proposedTranslation) >> { throw new ObjectDeletionException("delete failed", null) }

        when:
        def result = action.perform(ctx)

        then:
        // Save succeeded (no stub == no exception)
        1 * objectFacade.save(proposedTranslation)
        result.resultCode == ActionResult.ERROR
        // OBJECT_DELETED flag must NOT be set when delete actually failed
        !result.statusFlags.contains(com.hybris.cockpitng.actions.ActionResult.StatusFlag.OBJECT_DELETED)
        1 * notificationService.notifyUser(ctx, _, NotificationEvent.Level.FAILURE, _)
    }

    // -------------------------------------------------------------------------
    // Happy path: save, approve, delete → SUCCESS with OBJECT_DELETED
    // -------------------------------------------------------------------------

    def "perform returns SUCCESS with OBJECT_DELETED when save+approve+delete all succeed"() {
        given:
        approveService.approveTranslation(proposedTranslation) >> true

        when:
        def result = action.perform(ctx)

        then:
        1 * objectFacade.save(proposedTranslation)
        1 * objectFacade.delete(proposedTranslation)
        result.resultCode == ActionResult.SUCCESS
        result.statusFlags.contains(com.hybris.cockpitng.actions.ActionResult.StatusFlag.OBJECT_DELETED)
        1 * notificationService.notifyUser(ctx, _, NotificationEvent.Level.SUCCESS, _)
    }

    // -------------------------------------------------------------------------
    // Approval rejects: returns ERROR, no delete attempt
    // -------------------------------------------------------------------------

    def "perform returns ERROR and does not delete when approval returns false"() {
        given:
        approveService.approveTranslation(proposedTranslation) >> false

        when:
        def result = action.perform(ctx)

        then:
        result.resultCode == ActionResult.ERROR
        0 * objectFacade.delete(_)
        1 * notificationService.notifyUser(ctx, _, NotificationEvent.Level.FAILURE, _)
    }

    // -------------------------------------------------------------------------
    // canPerform — guard conditions
    // -------------------------------------------------------------------------

    def "canPerform returns false when item is null"() {
        given:
        def emptyCtx = Mock(ActionContext)
        emptyCtx.getData() >> null

        expect:
        action.canPerform(emptyCtx) == false
    }

    def "canPerform returns false when item is deleted"() {
        given:
        objectFacade.isDeleted(proposedTranslation) >> true

        expect:
        action.canPerform(ctx) == false
    }

    def "canPerform returns false when translation status is PENDING"() {
        given:
        proposedTranslation.getTranslationStatus() >> CxTranslationStatus.PENDING
        objectFacade.isDeleted(proposedTranslation) >> false
        permissionFacade.canChangeType(CxProposedTranslationModel._TYPECODE) >> true

        expect:
        action.canPerform(ctx) == false
    }

    def "canPerform returns true for SUCCESS-status translation with permission"() {
        given:
        proposedTranslation.getTranslationStatus() >> CxTranslationStatus.SUCCESS
        objectFacade.isDeleted(proposedTranslation) >> false
        permissionFacade.canChangeType(CxProposedTranslationModel._TYPECODE) >> true

        expect:
        action.canPerform(ctx) == true
    }

    def "canPerform returns true for MANUAL-status translation with permission"() {
        given:
        proposedTranslation.getTranslationStatus() >> CxTranslationStatus.MANUAL
        objectFacade.isDeleted(proposedTranslation) >> false
        permissionFacade.canChangeType(CxProposedTranslationModel._TYPECODE) >> true

        expect:
        action.canPerform(ctx) == true
    }
}
