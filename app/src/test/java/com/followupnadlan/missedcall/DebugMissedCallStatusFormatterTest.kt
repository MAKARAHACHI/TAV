package com.followupnadlan.missedcall

import org.junit.Assert.assertTrue
import org.junit.Test

class DebugMissedCallStatusFormatterTest {
    @Test
    fun duplicateStatusExplainsCooldownBlock() {
        val message = DebugMissedCallStatusFormatter.format(MissedCallAutoResponseAction.SKIP_DUPLICATE)

        assertTrue(message.contains("cooldown"))
    }

    @Test
    fun disabledStatusExplainsWhatsAppWillNotOpen() {
        val message = DebugMissedCallStatusFormatter.format(MissedCallAutoResponseAction.SKIP_DISABLED)

        assertTrue(message.contains("WhatsApp לא ייפתח"))
    }

    @Test
    fun preparedWhatsAppStatusExplainsExpectedOpen() {
        val message = DebugMissedCallStatusFormatter.format(MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP)

        assertTrue(message.contains("WhatsApp אמור להיפתח"))
    }

    @Test
    fun manualPromptStatusExplainsApprovalRequired() {
        val message = DebugMissedCallStatusFormatter.format(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT)

        assertTrue(message.contains("אישור"))
    }

    @Test
    fun onlySelectedSkipExplainsAllowedList() {
        val message = DebugMissedCallStatusFormatter.format(MissedCallAutoResponseAction.SKIP_NOT_ALLOWED)

        assertTrue(message.contains("רשימת המותרים"))
    }
}
