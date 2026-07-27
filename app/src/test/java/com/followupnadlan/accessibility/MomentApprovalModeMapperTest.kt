package com.followupnadlan.accessibility

import com.followupnadlan.missedcall.MissedCallWhatsAppMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The moment edit page's "אישור לפני שליחה" select maps to the engine's existing automation flags.
 * These tests pin that mapping so AUTOMATIC really is the auto-send flag pair and MANUAL really is
 * the prepared-manual (approval-sheet) path — the choice the user sees must be the flags stored.
 */
class MomentApprovalModeMapperTest {
    @Test
    fun manualMapsToPreparedManualWithAutomationOff() {
        val flags = MomentApprovalModeMapper.toFlags(MomentApprovalMode.MANUAL)
        assertEquals(MissedCallWhatsAppMode.PREPARED_MANUAL, flags.whatsappMode)
        assertFalse(flags.automationEnabled)
    }

    @Test
    fun automaticMapsToAccessibilityAutoWithAutomationOn() {
        val flags = MomentApprovalModeMapper.toFlags(MomentApprovalMode.AUTOMATIC)
        assertEquals(MissedCallWhatsAppMode.ACCESSIBILITY_AUTO, flags.whatsappMode)
        assertTrue(flags.automationEnabled)
    }

    @Test
    fun readsPreparedManualBackAsManual() {
        assertEquals(
            MomentApprovalMode.MANUAL,
            MomentApprovalModeMapper.fromWhatsAppMode(MissedCallWhatsAppMode.PREPARED_MANUAL)
        )
    }

    @Test
    fun readsAccessibilityAutoBackAsAutomatic() {
        assertEquals(
            MomentApprovalMode.AUTOMATIC,
            MomentApprovalModeMapper.fromWhatsAppMode(MissedCallWhatsAppMode.ACCESSIBILITY_AUTO)
        )
    }

    @Test
    fun roundTripsBothModes() {
        for (mode in MomentApprovalMode.entries) {
            val back = MomentApprovalModeMapper.fromWhatsAppMode(
                MomentApprovalModeMapper.toFlags(mode).whatsappMode
            )
            assertEquals(mode, back)
        }
    }
}
