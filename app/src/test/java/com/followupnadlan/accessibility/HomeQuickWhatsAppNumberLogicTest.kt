package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.templates.SprintOneTemplates
import com.followupnadlan.templates.TemplateStoreLogic
import androidx.compose.ui.text.input.KeyboardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeQuickWhatsAppNumberLogicTest {
    @Test
    fun latestMissedCallerAutoFillsEmptyField() {
        val state = HomeQuickWhatsAppNumberLogic.applyLatestMissedCaller(
            currentText = "",
            currentLastMissedPhone = null,
            latestMissedPhone = "0501234567",
            isActivelyEditing = false
        )

        assertEquals("0501234567", state.text)
        assertEquals("0501234567", state.lastMissedPhone)
    }

    @Test
    fun manualEditStaysWhileSameMissedCallerIsCurrent() {
        val state = HomeQuickWhatsAppNumberLogic.applyLatestMissedCaller(
            currentText = "0527654321",
            currentLastMissedPhone = "0501234567",
            latestMissedPhone = "0501234567",
            isActivelyEditing = false
        )

        assertEquals("0527654321", state.text)
        assertEquals("0501234567", state.lastMissedPhone)
    }

    @Test
    fun newerMissedCallerDoesNotOverwriteWhileActivelyEditing() {
        val state = HomeQuickWhatsAppNumberLogic.applyLatestMissedCaller(
            currentText = "0527654321",
            currentLastMissedPhone = "0501234567",
            latestMissedPhone = "0541112222",
            isActivelyEditing = true
        )

        assertEquals("0527654321", state.text)
        assertEquals("0501234567", state.lastMissedPhone)
    }

    @Test
    fun newerMissedCallerUpdatesWhenNotEditing() {
        val state = HomeQuickWhatsAppNumberLogic.applyLatestMissedCaller(
            currentText = "0527654321",
            currentLastMissedPhone = "0501234567",
            latestMissedPhone = "0541112222",
            isActivelyEditing = false
        )

        assertEquals("0541112222", state.text)
        assertEquals("0541112222", state.lastMissedPhone)
    }

    @Test
    fun invalidNumberBlocksOpening() {
        val plan = HomeQuickWhatsAppOpenPlanner.plan(
            phoneInput = "not a phone",
            message = "hello"
        )

        assertEquals(HomeQuickWhatsAppOpenPlan.InvalidNumber, plan)
    }

    @Test
    fun validNumberPlansComposerOnly() {
        val plan = HomeQuickWhatsAppOpenPlanner.plan(
            phoneInput = "050-1234567",
            message = "hello world"
        ) as HomeQuickWhatsAppOpenPlan.OpenComposer

        assertEquals("972501234567", plan.normalizedPhone)
        assertTrue(plan.link.startsWith("https://wa.me/972501234567?text="))
        assertTrue(plan.link.contains("hello%20world"))
        assertEquals(FollowUpActionType.MANUAL_WHATSAPP_COMPOSER_OPENED, plan.successLogAction)
        assertEquals(FollowUpActionType.WHATSAPP_REPLY_FAILED, plan.failureLogAction)
    }

    @Test
    fun quickHomeOpenNeverUsesAutoSendSmsFallbackOrSentLogs() {
        val plan = HomeQuickWhatsAppOpenPlanner.plan(
            phoneInput = "0501234567",
            message = "hello"
        ) as HomeQuickWhatsAppOpenPlan.OpenComposer

        val actualActions = setOf(plan.successLogAction, plan.failureLogAction)
        assertFalse(actualActions.contains(FollowUpActionType.WHATSAPP_AUTO_SENT))
        assertFalse(actualActions.contains(FollowUpActionType.AUTO_SMS_SENT))
        assertFalse(actualActions.contains(FollowUpActionType.FALLBACK_SMS_SENT))
        assertTrue(HomeQuickWhatsAppOpenPlanner.blockedLogActions.none { it in actualActions })
    }

    @Test
    fun savedCustomMessageIsUsedForComposerText() {
        val customBody = "custom saved body for WhatsApp"
        val templates = TemplateStoreLogic.applySavedBodies(
            builtInTemplates = SprintOneTemplates.all,
            savedBodiesById = mapOf(SprintOneTemplates.MISSED_ID to customBody)
        )
        val selected = TemplateStoreLogic.selectedTemplate(
            templates = templates,
            selectedTemplateId = SprintOneTemplates.MISSED_ID
        )

        val plan = HomeQuickWhatsAppOpenPlanner.plan(
            phoneInput = "0501234567",
            message = selected?.body.orEmpty()
        ) as HomeQuickWhatsAppOpenPlan.OpenComposer

        assertEquals(customBody, plan.message)
        assertTrue(plan.link.contains("custom%20saved%20body%20for%20WhatsApp"))
    }

    @Test
    fun quickHomeCardSpecHasVisibleButtonAndPhoneKeyboard() {
        assertEquals("פתח WhatsApp", HomeQuickWhatsAppUiSpec.BUTTON_TEXT)
        assertEquals(KeyboardType.Phone, HomeQuickWhatsAppUiSpec.phoneKeyboardType)
    }

    @Test
    fun quickHomeCardHelperTextIsRemovedToAvoidClipping() {
        assertNull(HomeQuickWhatsAppUiSpec.helperText)
    }
}
