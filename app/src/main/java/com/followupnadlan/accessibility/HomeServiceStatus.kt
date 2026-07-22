package com.followupnadlan.accessibility

import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel
import com.followupnadlan.missedcall.MissedCallWhatsAppMode

internal enum class HomeServiceStatusTone {
    ACTIVE,
    NEUTRAL,
    DISABLED
}

internal enum class HomeServiceStatusKind {
    BRIDGE,
    MODE,
    WHATSAPP,
    SMS,
    SAFETY,
    DUPLICATE_PROTECTION
}

internal data class HomeServiceStatusInput(
    val bridgeEnabled: Boolean,
    val whatsappMode: MissedCallWhatsAppMode,
    val primaryChannel: MissedCallResponsePrimaryChannel,
    val whatsappAvailable: Boolean,
    val smsFallbackEnabled: Boolean,
    val readPhoneStateGranted: Boolean = true,
    val readCallLogGranted: Boolean = true,
    val detectorEnabled: Boolean = true,
    // Stage 2 (plan §1): the single status line also needs to know whether the
    // auto-send system consent (accessibility service) is granted, and whether a
    // message template actually exists — otherwise the home line can promise a send
    // that cannot happen. Default true so existing row-only callers/tests are unaffected.
    val autoSendConsentGranted: Boolean = true,
    val hasTemplate: Boolean = true
)

internal data class HomeServiceStatusRow(
    val label: String,
    val kind: HomeServiceStatusKind,
    val tone: HomeServiceStatusTone
)

/**
 * The single home status line (plan §1, §4, §9 Stage 2a) — the most important text in
 * the product. Returns the first *blocking* problem by a fixed severity order, or the
 * healthy line when nothing blocks. The multi-row [HomeServiceStatus.rows] detail drops
 * below this line; it is not replaced.
 */
internal data class HomeServiceStatusLine(
    val label: String,
    val tone: HomeServiceStatusTone
)

internal object HomeServiceStatus {
    private const val MAX_ROWS = 4

    const val HEALTHY_LINE = "האפליקציה פעילה ומוכנה לשלוח הודעות"

    /**
     * The single ranked status line (plan §1 severity order):
     * חסרות הרשאות זיהוי → גישור כבוי → אין תבנית → הערוץ הראשי לא זמין →
     * שליחה אוטומטית בלי אישור מערכת → תקין.
     * Returns the first blocking problem; otherwise the healthy line.
     */
    fun statusLine(input: HomeServiceStatusInput): HomeServiceStatusLine =
        when {
            !input.readPhoneStateGranted || !input.readCallLogGranted || !input.detectorEnabled ->
                HomeServiceStatusLine("זיהוי שיחות לא פעיל — צריך להשלים הרשאה", HomeServiceStatusTone.DISABLED)
            !input.bridgeEnabled ->
                HomeServiceStatusLine("כבוי — לא תישלח הודעה עד שתפעיל/י", HomeServiceStatusTone.DISABLED)
            !input.hasTemplate ->
                HomeServiceStatusLine("חסרה הודעה — צריך לכתוב נוסח לפני שליחה", HomeServiceStatusTone.DISABLED)
            !primaryChannelReachable(input) ->
                HomeServiceStatusLine("WhatsApp לא זמין — צריך לבחור ערוץ אחר", HomeServiceStatusTone.DISABLED)
            input.whatsappMode == MissedCallWhatsAppMode.ACCESSIBILITY_AUTO && !input.autoSendConsentGranted ->
                HomeServiceStatusLine("השליחה האוטומטית זקוקה לאישור — השלם/י הגדרה", HomeServiceStatusTone.DISABLED)
            else ->
                HomeServiceStatusLine(HEALTHY_LINE, HomeServiceStatusTone.ACTIVE)
        }

    /**
     * Is the configured primary route usable? SMS-only is always reachable (it's the SIM).
     * WhatsApp-first is reachable when WhatsApp is available, or when SMS fallback covers it.
     */
    private fun primaryChannelReachable(input: HomeServiceStatusInput): Boolean =
        when (input.primaryChannel) {
            MissedCallResponsePrimaryChannel.SMS_ONLY -> true
            MissedCallResponsePrimaryChannel.WHATSAPP_FIRST ->
                input.whatsappAvailable || input.smsFallbackEnabled
        }

    fun rows(input: HomeServiceStatusInput): List<HomeServiceStatusRow> {
        if (!input.bridgeEnabled) {
            return listOf(
                HomeServiceStatusRow("גישור כבוי", HomeServiceStatusKind.BRIDGE, HomeServiceStatusTone.DISABLED),
                HomeServiceStatusRow(
                    "לא תישלח הודעה עד שתפעיל/י את הגישור",
                    HomeServiceStatusKind.SAFETY,
                    HomeServiceStatusTone.NEUTRAL
                )
            )
        }

        val rows = mutableListOf(bridgeRow(input))

        if (input.whatsappMode == MissedCallWhatsAppMode.PREPARED_MANUAL) {
            rows += HomeServiceStatusRow("שאל אותי לפני שליחה", HomeServiceStatusKind.MODE, HomeServiceStatusTone.NEUTRAL)
            rows += HomeServiceStatusRow(
                "לא תישלח הודעה בלי אישור שלך",
                HomeServiceStatusKind.SAFETY,
                HomeServiceStatusTone.NEUTRAL
            )
            rows += manualRouteRow(input)
        } else {
            rows += HomeServiceStatusRow("שליחה אוטומטית", HomeServiceStatusKind.MODE, HomeServiceStatusTone.ACTIVE)
            rows += automaticRouteRow(input)
            rows += automaticFourthRow(input)
        }

        return rows.take(MAX_ROWS)
    }

    private fun bridgeRow(input: HomeServiceStatusInput): HomeServiceStatusRow =
        when {
            !input.readPhoneStateGranted || !input.readCallLogGranted ->
                HomeServiceStatusRow("זיהוי שיחות לא פעיל — חסרה הרשאה", HomeServiceStatusKind.BRIDGE, HomeServiceStatusTone.DISABLED)
            !input.detectorEnabled ->
                HomeServiceStatusRow("גישור דורש תיקון", HomeServiceStatusKind.BRIDGE, HomeServiceStatusTone.DISABLED)
            else ->
                HomeServiceStatusRow("גישור פעיל", HomeServiceStatusKind.BRIDGE, HomeServiceStatusTone.ACTIVE)
        }

    private fun manualRouteRow(input: HomeServiceStatusInput): HomeServiceStatusRow =
        when {
            input.primaryChannel == MissedCallResponsePrimaryChannel.SMS_ONLY ->
                HomeServiceStatusRow("SMS בלבד", HomeServiceStatusKind.SMS, HomeServiceStatusTone.NEUTRAL)
            input.whatsappAvailable && input.smsFallbackEnabled ->
                HomeServiceStatusRow("SMS כגיבוי", HomeServiceStatusKind.SMS, HomeServiceStatusTone.ACTIVE)
            input.whatsappAvailable ->
                HomeServiceStatusRow("WhatsApp זמין", HomeServiceStatusKind.WHATSAPP, HomeServiceStatusTone.ACTIVE)
            input.smsFallbackEnabled ->
                HomeServiceStatusRow("SMS בלבד", HomeServiceStatusKind.SMS, HomeServiceStatusTone.NEUTRAL)
            else ->
                HomeServiceStatusRow("WhatsApp לא זמין", HomeServiceStatusKind.WHATSAPP, HomeServiceStatusTone.DISABLED)
        }

    private fun automaticRouteRow(input: HomeServiceStatusInput): HomeServiceStatusRow =
        when {
            input.primaryChannel == MissedCallResponsePrimaryChannel.SMS_ONLY ->
                HomeServiceStatusRow("SMS בלבד", HomeServiceStatusKind.SMS, HomeServiceStatusTone.NEUTRAL)
            input.whatsappAvailable ->
                HomeServiceStatusRow("WhatsApp ראשי", HomeServiceStatusKind.WHATSAPP, HomeServiceStatusTone.ACTIVE)
            input.smsFallbackEnabled ->
                HomeServiceStatusRow("SMS בלבד", HomeServiceStatusKind.SMS, HomeServiceStatusTone.NEUTRAL)
            else ->
                HomeServiceStatusRow("WhatsApp לא זמין", HomeServiceStatusKind.WHATSAPP, HomeServiceStatusTone.DISABLED)
        }

    private fun automaticFourthRow(input: HomeServiceStatusInput): HomeServiceStatusRow =
        when {
            input.primaryChannel == MissedCallResponsePrimaryChannel.WHATSAPP_FIRST &&
                input.whatsappAvailable &&
                input.smsFallbackEnabled ->
                HomeServiceStatusRow("SMS כגיבוי", HomeServiceStatusKind.SMS, HomeServiceStatusTone.ACTIVE)
            !input.smsFallbackEnabled ->
                HomeServiceStatusRow("ללא SMS גיבוי", HomeServiceStatusKind.SMS, HomeServiceStatusTone.DISABLED)
            else ->
                HomeServiceStatusRow(
                    "הגנת כפילויות פעילה",
                    HomeServiceStatusKind.DUPLICATE_PROTECTION,
                    HomeServiceStatusTone.ACTIVE
                )
        }
}
