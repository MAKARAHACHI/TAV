package com.followupnadlan.accessibility

import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel

/**
 * The channel the send screen's *primary* button acts on, plus the label that must appear
 * identically in the screen header and on that button (plan §1 "רואים למי + באיזה ערוץ",
 * §3ו fallback rules). One global preferred channel drives both flows (§10.1).
 *
 * Both channels here only ever *open* a composer (WhatsApp / SMS) that the user sends
 * themselves, so the label is "פתח …", never "שלח …" (§7ב).
 */
enum class FollowUpSendChannel {
    WHATSAPP,
    SMS
}

data class FollowUpChannelResolution(
    val channel: FollowUpSendChannel,
    /** Identical string for header ("אל: … · <this>") and primary button. */
    val channelName: String,
    val primaryButtonText: String
)

object FollowUpChannelResolver {
    const val WHATSAPP_NAME = "WhatsApp"
    const val SMS_NAME = "SMS"

    /**
     * Resolves the primary channel from the global preference and WhatsApp availability
     * (§3ו): SMS-only → SMS; WhatsApp-first → WhatsApp when available, otherwise SMS
     * (whether or not fallback is on — with fallback off it is still a manual "פתח SMS",
     * never a silent send; the screen only ever opens a composer).
     */
    fun resolve(
        primaryChannel: MissedCallResponsePrimaryChannel,
        whatsappAvailable: Boolean
    ): FollowUpChannelResolution {
        val channel = when (primaryChannel) {
            MissedCallResponsePrimaryChannel.SMS_ONLY -> FollowUpSendChannel.SMS
            MissedCallResponsePrimaryChannel.WHATSAPP_FIRST ->
                if (whatsappAvailable) FollowUpSendChannel.WHATSAPP else FollowUpSendChannel.SMS
        }
        return when (channel) {
            FollowUpSendChannel.WHATSAPP ->
                FollowUpChannelResolution(channel, WHATSAPP_NAME, "פתח $WHATSAPP_NAME")
            FollowUpSendChannel.SMS ->
                FollowUpChannelResolution(channel, SMS_NAME, "פתח $SMS_NAME")
        }
    }
}
