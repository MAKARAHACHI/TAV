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
}
