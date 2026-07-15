package com.followupnadlan.accessibility

import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel
import com.followupnadlan.missedcall.MissedCallWhatsAppMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeServiceStatusTest {
    @Test
    fun bridgeDisabledShowsOffStateAndDoesNotClaimActive() {
        val labels = labels(defaultInput(bridgeEnabled = false))

        assertTrue(labels.contains("גישור כבוי"))
        assertTrue(labels.contains("לא תישלח הודעה עד שתפעיל/י את הגישור"))
        assertFalse(labels.contains("גישור פעיל"))
    }

    @Test
    fun bridgeEnabledWithPermissionsShowsActive() {
        val labels = labels(defaultInput())

        assertTrue(labels.contains("גישור פעיל"))
    }

    @Test
    fun missingReadPhoneStateShowsDetectionPermissionWarning() {
        val labels = labels(defaultInput(readPhoneStateGranted = false))

        assertTrue(labels.contains("זיהוי שיחות לא פעיל — חסרה הרשאה"))
        assertFalse(labels.contains("גישור פעיל"))
    }

    @Test
    fun missingReadCallLogShowsDetectionPermissionWarning() {
        val labels = labels(defaultInput(readCallLogGranted = false))

        assertTrue(labels.contains("זיהוי שיחות לא פעיל — חסרה הרשאה"))
        assertFalse(labels.contains("גישור פעיל"))
    }

    @Test
    fun detectorDisabledShowsRepairNeeded() {
        val labels = labels(defaultInput(detectorEnabled = false))

        assertTrue(labels.contains("גישור דורש תיקון"))
        assertFalse(labels.contains("גישור פעיל"))
    }

    @Test
    fun automaticWhatsAppWithSmsFallbackShowsAutomaticWhatsappAndSmsFallback() {
        val labels = labels(
            defaultInput(
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                whatsappAvailable = true,
                smsFallbackEnabled = true
            )
        )

        assertTrue(labels.contains("שליחה אוטומטית"))
        assertTrue(labels.contains("WhatsApp ראשי"))
        assertTrue(labels.contains("SMS כגיבוי"))
    }

    @Test
    fun manualModeShowsAskBeforeSendingAndApprovalSafety() {
        val labels = labels(defaultInput(whatsappMode = MissedCallWhatsAppMode.PREPARED_MANUAL))

        assertTrue(labels.contains("שאל אותי לפני שליחה"))
        assertTrue(labels.contains("לא תישלח הודעה בלי אישור שלך"))
    }

    @Test
    fun smsFallbackDisabledDoesNotShowSmsFallbackClaim() {
        val labels = labels(
            defaultInput(
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                whatsappAvailable = true,
                smsFallbackEnabled = false
            )
        )

        assertFalse(labels.contains("SMS כגיבוי"))
        assertTrue(labels.contains("ללא SMS גיבוי"))
    }

    @Test
    fun whatsappUnavailableDoesNotShowWhatsappActiveClaim() {
        val labels = labels(
            defaultInput(
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                whatsappAvailable = false,
                smsFallbackEnabled = true
            )
        )

        assertFalse(labels.contains("WhatsApp פעיל"))
        assertTrue(labels.contains("SMS בלבד"))
    }

    @Test
    fun statusCardDoesNotExposeRawTechnicalLabels() {
        val rawLabels = listOf(
            "PREPARED_MANUAL",
            "ACCESSIBILITY_AUTO",
            "WHATSAPP_FIRST",
            "SMS_ONLY",
            "com.whatsapp",
            "com.whatsapp.w4b"
        )
        val labels = listOf(
            defaultInput(whatsappMode = MissedCallWhatsAppMode.PREPARED_MANUAL),
            defaultInput(whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO),
            defaultInput(primaryChannel = MissedCallResponsePrimaryChannel.SMS_ONLY)
        ).flatMap { labels(it) }

        rawLabels.forEach { raw ->
            assertFalse("status leaked raw label $raw", labels.any { it.contains(raw) })
        }
    }

    @Test
    fun statusRowsStayCompact() {
        val scenarios = listOf(
            defaultInput(bridgeEnabled = false),
            defaultInput(whatsappMode = MissedCallWhatsAppMode.PREPARED_MANUAL),
            defaultInput(whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO),
            defaultInput(
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                primaryChannel = MissedCallResponsePrimaryChannel.SMS_ONLY,
                whatsappAvailable = false
            )
        )

        scenarios.forEach { input ->
            assertTrue(HomeServiceStatus.rows(input).size <= 4)
        }
    }

    private fun labels(input: HomeServiceStatusInput): List<String> =
        HomeServiceStatus.rows(input).map { it.label }

    private fun defaultInput(
        bridgeEnabled: Boolean = true,
        whatsappMode: MissedCallWhatsAppMode = MissedCallWhatsAppMode.PREPARED_MANUAL,
        primaryChannel: MissedCallResponsePrimaryChannel = MissedCallResponsePrimaryChannel.WHATSAPP_FIRST,
        whatsappAvailable: Boolean = true,
        smsFallbackEnabled: Boolean = true,
        readPhoneStateGranted: Boolean = true,
        readCallLogGranted: Boolean = true,
        detectorEnabled: Boolean = true
    ): HomeServiceStatusInput =
        HomeServiceStatusInput(
            bridgeEnabled = bridgeEnabled,
            whatsappMode = whatsappMode,
            primaryChannel = primaryChannel,
            whatsappAvailable = whatsappAvailable,
            smsFallbackEnabled = smsFallbackEnabled,
            readPhoneStateGranted = readPhoneStateGranted,
            readCallLogGranted = readCallLogGranted,
            detectorEnabled = detectorEnabled
        )
}
