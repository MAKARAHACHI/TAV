package com.followupnadlan.missedcall

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppNumberCheckLogicTest {
    @Test
    fun `no permission is always UNKNOWN`() {
        assertEquals(
            WhatsAppNumberStatus.UNKNOWN,
            WhatsAppNumberCheckLogic.map(permissionGranted = false, isSaved = false, hasWhatsAppProfileRow = false)
        )
        assertEquals(
            WhatsAppNumberStatus.UNKNOWN,
            WhatsAppNumberCheckLogic.map(permissionGranted = false, isSaved = true, hasWhatsAppProfileRow = true)
        )
    }

    @Test
    fun `granted but not saved is UNKNOWN`() {
        assertEquals(
            WhatsAppNumberStatus.UNKNOWN,
            WhatsAppNumberCheckLogic.map(permissionGranted = true, isSaved = false, hasWhatsAppProfileRow = false)
        )
    }

    @Test
    fun `granted, saved, no whatsapp row is NO_WHATSAPP_SAVED`() {
        assertEquals(
            WhatsAppNumberStatus.NO_WHATSAPP_SAVED,
            WhatsAppNumberCheckLogic.map(permissionGranted = true, isSaved = true, hasWhatsAppProfileRow = false)
        )
    }

    @Test
    fun `granted, saved, has whatsapp row is HAS_WHATSAPP`() {
        assertEquals(
            WhatsAppNumberStatus.HAS_WHATSAPP,
            WhatsAppNumberCheckLogic.map(permissionGranted = true, isSaved = true, hasWhatsAppProfileRow = true)
        )
    }

    @Test
    fun `only a CONFIRMED absence takes the honest fallback, never doubt`() {
        assertEquals(
            true,
            WhatsAppNumberCheckLogic.shouldTakeNotOnWhatsAppFallback(WhatsAppNumberStatus.NO_WHATSAPP_SAVED)
        )
        assertEquals(
            false,
            WhatsAppNumberCheckLogic.shouldTakeNotOnWhatsAppFallback(WhatsAppNumberStatus.UNKNOWN)
        )
        assertEquals(
            false,
            WhatsAppNumberCheckLogic.shouldTakeNotOnWhatsAppFallback(WhatsAppNumberStatus.HAS_WHATSAPP)
        )
    }
}
