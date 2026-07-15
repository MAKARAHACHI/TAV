package com.followupnadlan.accessibility

import com.followupnadlan.postcall.CallDetectionDiagnostics

/**
 * Maps raw call-detection diagnostic event constants to Hebrew for the "בדיקת זיהוי שיחות" panel.
 * Recorded events may carry a ":detail" suffix; only the base constant is translated.
 * Unknown values fall back to the raw string so nothing is hidden from the user.
 */
object CallDetectionDiagnosticsLabels {
    fun event(raw: String): String {
        if (raw.isBlank()) return raw
        val base = raw.substringBefore(":")
        return EVENTS[base] ?: raw
    }

    private val EVENTS = mapOf(
        CallDetectionDiagnostics.CALL_RECEIVER_REGISTERED to "מקלט שיחות נרשם",
        CallDetectionDiagnostics.PHONE_STATE_RECEIVED_RINGING to "שיחה נכנסת מצלצלת",
        CallDetectionDiagnostics.PHONE_STATE_RECEIVED_OFFHOOK to "שיחה פעילה",
        CallDetectionDiagnostics.PHONE_STATE_RECEIVED_IDLE to "שיחה הסתיימה",
        CallDetectionDiagnostics.INCOMING_NUMBER_PRESENT to "זוהה מספר מתקשר",
        CallDetectionDiagnostics.INCOMING_NUMBER_MISSING to "לא זוהה מספר מתקשר",
        CallDetectionDiagnostics.MISSED_CALL_CANDIDATE_STORED to "שיחה שלא נענתה נרשמה",
        CallDetectionDiagnostics.MISSED_CALL_CONFIRMED to "שיחה שלא נענתה אושרה",
        CallDetectionDiagnostics.MISSED_CALL_IGNORED_ANSWERED to "השיחה נענתה — אין צורך בטיפול",
        CallDetectionDiagnostics.MISSED_CALL_HANDLER_STARTED to "התחיל טיפול בשיחה שלא נענתה",
        CallDetectionDiagnostics.MISSED_CALL_DECISION_RESULT to "התקבלה החלטה על השליחה",
        CallDetectionDiagnostics.BACKGROUND_SERVICE_STARTED to "שירות הרקע פעיל",
        CallDetectionDiagnostics.BACKGROUND_SERVICE_STOPPED to "שירות הרקע כבוי",
        CallDetectionDiagnostics.PERMISSION_MISSING_READ_PHONE_STATE to "חסרה הרשאת מצב טלפון",
        CallDetectionDiagnostics.PERMISSION_MISSING_READ_CALL_LOG to "חסרה הרשאת יומן שיחות"
    )
}
