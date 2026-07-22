package com.followupnadlan.accessibility

import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel
import org.junit.Assert.assertEquals
import org.junit.Test

class FollowUpChannelResolverTest {
    @Test
    fun whatsappFirstWithWhatsappAvailableResolvesWhatsapp() {
        val r = FollowUpChannelResolver.resolve(MissedCallResponsePrimaryChannel.WHATSAPP_FIRST, whatsappAvailable = true)
        assertEquals(FollowUpSendChannel.WHATSAPP, r.channel)
        assertEquals("WhatsApp", r.channelName)
        assertEquals("פתח WhatsApp", r.primaryButtonText)
    }

    @Test
    fun whatsappFirstWithoutWhatsappFallsBackToSms() {
        val r = FollowUpChannelResolver.resolve(MissedCallResponsePrimaryChannel.WHATSAPP_FIRST, whatsappAvailable = false)
        assertEquals(FollowUpSendChannel.SMS, r.channel)
        assertEquals("פתח SMS", r.primaryButtonText)
    }

    @Test
    fun smsOnlyAlwaysResolvesSms() {
        val r = FollowUpChannelResolver.resolve(MissedCallResponsePrimaryChannel.SMS_ONLY, whatsappAvailable = true)
        assertEquals(FollowUpSendChannel.SMS, r.channel)
        assertEquals("SMS", r.channelName)
        assertEquals("פתח SMS", r.primaryButtonText)
    }

    @Test
    fun headerNameAndButtonTextShareTheSameChannelName() {
        // Plan §1: the channel name in the header and on the button must be identical.
        listOf(
            FollowUpChannelResolver.resolve(MissedCallResponsePrimaryChannel.WHATSAPP_FIRST, true),
            FollowUpChannelResolver.resolve(MissedCallResponsePrimaryChannel.SMS_ONLY, true)
        ).forEach { r ->
            assertEquals("פתח ${r.channelName}", r.primaryButtonText)
        }
    }

    @Test
    fun buttonNeverSaysSend() {
        // Both channels only open a composer (§7ב) — never "שלח".
        listOf(true, false).forEach { available ->
            listOf(MissedCallResponsePrimaryChannel.WHATSAPP_FIRST, MissedCallResponsePrimaryChannel.SMS_ONLY).forEach { ch ->
                val r = FollowUpChannelResolver.resolve(ch, available)
                assert(r.primaryButtonText.startsWith("פתח")) { "button must open, not send: ${r.primaryButtonText}" }
            }
        }
    }
}
