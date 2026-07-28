package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class MissedSendModeLabelTest {
    @Test
    fun automaticOnlyWhenBothTrue() {
        assertEquals(MissedSendModeLabel.AUTOMATIC, MissedSendModeLabel.of(approvalIsAutomatic = true, accessibilityEnabled = true))
    }

    // §2 honesty: automatic picked but Accessibility off ⇒ the app can't send by itself ⇒ "ידני".
    @Test
    fun automaticPickedButAccessibilityOffReadsManual() {
        assertEquals(MissedSendModeLabel.MANUAL, MissedSendModeLabel.of(approvalIsAutomatic = true, accessibilityEnabled = false))
    }

    @Test
    fun manualPickedAlwaysManual() {
        assertEquals(MissedSendModeLabel.MANUAL, MissedSendModeLabel.of(approvalIsAutomatic = false, accessibilityEnabled = true))
        assertEquals(MissedSendModeLabel.MANUAL, MissedSendModeLabel.of(approvalIsAutomatic = false, accessibilityEnabled = false))
    }
}
