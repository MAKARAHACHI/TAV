package com.followupnadlan.accessibility

import com.followupnadlan.postcall.CallDetectionDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Test

class CallDetectionDiagnosticsLabelsTest {
    @Test
    fun translatesKnownPhoneStateEvent() {
        assertEquals("שיחה נכנסת מצלצלת", CallDetectionDiagnosticsLabels.event(CallDetectionDiagnostics.PHONE_STATE_RECEIVED_RINGING))
    }

    @Test
    fun translatesEventIgnoringDetailSuffix() {
        assertEquals(
            "התקבלה החלטה על השליחה",
            CallDetectionDiagnosticsLabels.event("${CallDetectionDiagnostics.MISSED_CALL_DECISION_RESULT}:WHATSAPP")
        )
    }

    @Test
    fun fallsBackToRawUnknownValue() {
        assertEquals("SOMETHING_NEW", CallDetectionDiagnosticsLabels.event("SOMETHING_NEW"))
    }

    @Test
    fun keepsBlankAsBlank() {
        assertEquals("", CallDetectionDiagnosticsLabels.event(""))
    }
}
