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
    val detectorEnabled: Boolean = true
)

internal data class HomeServiceStatusRow(
    val label: String,
    val kind: HomeServiceStatusKind,
    val tone: HomeServiceStatusTone
)

internal object HomeServiceStatus {
    private const val MAX_ROWS = 4

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
