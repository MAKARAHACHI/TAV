package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The channel choice is the §2-critical mapping: the engine stores routing as two flags whose
 * combinations include "silently send SMS instead". These tests pin the property that matters —
 * choosing a channel never enables a substitution behind the user's back.
 */
class FollowUpChannelSettingsTest {
    @Test
    fun offersOnlyInstalledChannels() {
        val withoutBusiness = FollowUpChannelSettings.available(businessInstalled = false)
        assertEquals(listOf(FollowUpChannel.WHATSAPP, FollowUpChannel.SMS), withoutBusiness)
    }

    @Test
    fun offersBusinessOnlyWhenItIsInstalled() {
        val withBusiness = FollowUpChannelSettings.available(businessInstalled = true)
        assertTrue(withBusiness.contains(FollowUpChannel.WHATSAPP_BUSINESS))
        assertEquals(3, withBusiness.size)
    }

    @Test
    fun neverOffersAnOptionThatCannotBeHonored() {
        // An uninstalled WhatsApp Business is exactly the option §2 forbids showing.
        assertFalse(
            FollowUpChannelSettings.available(businessInstalled = false)
                .contains(FollowUpChannel.WHATSAPP_BUSINESS)
        )
    }
}
