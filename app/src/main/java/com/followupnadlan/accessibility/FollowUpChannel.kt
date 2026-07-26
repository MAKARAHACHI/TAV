package com.followupnadlan.accessibility

import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel
import com.followupnadlan.missedcall.WhatsAppPackageResolver

/**
 * The channel the client actually receives the message on — one explicit choice.
 *
 * §2, and the reason this type exists: the engine stores the routing as two independent flags
 * (`primaryChannel` + `smsFallbackEnabled`), whose combinations include "WhatsApp, but silently
 * send an SMS instead if WhatsApp isn't installed". That combination makes the user believe they
 * chose WhatsApp while a different channel is used behind their back. The code cannot detect a
 * *delivery* failure at all (only whether the app is installed), so an honest "fallback" is not
 * available either way.
 *
 * So the UI offers exactly three states, and each maps to a flag pair with fallback OFF. Picking
 * a channel means that channel, or an honest failure notification — never a silent substitution.
 */
enum class FollowUpChannel {
    WHATSAPP,
    WHATSAPP_BUSINESS,
    SMS
}

object FollowUpChannelSettings {
    /** Reads the stored flag pair back as the single choice the user made. */
    fun current(settings: MissedCallAutoResponseSettings): FollowUpChannel =
        if (settings.primaryChannel == MissedCallResponsePrimaryChannel.SMS_ONLY) {
            FollowUpChannel.SMS
        } else if (settings.preferredWhatsAppPackage == WhatsAppPackageResolver.WHATSAPP_BUSINESS_PACKAGE) {
            FollowUpChannel.WHATSAPP_BUSINESS
        } else {
            FollowUpChannel.WHATSAPP
        }

    /**
     * Persists the choice. `smsFallbackEnabled` is forced off for the WhatsApp options: without
     * that, choosing WhatsApp would still route to SMS whenever WhatsApp isn't installed.
     */
    fun apply(settings: MissedCallAutoResponseSettings, channel: FollowUpChannel) {
        when (channel) {
            FollowUpChannel.WHATSAPP -> {
                settings.primaryChannel = MissedCallResponsePrimaryChannel.WHATSAPP_FIRST
                settings.preferredWhatsAppPackage = WhatsAppPackageResolver.WHATSAPP_MESSENGER_PACKAGE
                settings.smsFallbackEnabled = false
            }
            FollowUpChannel.WHATSAPP_BUSINESS -> {
                settings.primaryChannel = MissedCallResponsePrimaryChannel.WHATSAPP_FIRST
                settings.preferredWhatsAppPackage = WhatsAppPackageResolver.WHATSAPP_BUSINESS_PACKAGE
                settings.smsFallbackEnabled = false
            }
            FollowUpChannel.SMS -> {
                settings.primaryChannel = MissedCallResponsePrimaryChannel.SMS_ONLY
                settings.smsFallbackEnabled = true
            }
        }
    }

    /**
     * The choices worth offering. WhatsApp Business appears only when it is actually installed —
     * an option that cannot be honored is exactly the §2 gap this whole type exists to close.
     */
    fun available(businessInstalled: Boolean): List<FollowUpChannel> =
        FollowUpChannel.entries.filter {
            it != FollowUpChannel.WHATSAPP_BUSINESS || businessInstalled
        }
}
