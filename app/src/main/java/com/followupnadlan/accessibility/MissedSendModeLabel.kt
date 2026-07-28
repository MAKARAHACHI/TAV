package com.followupnadlan.accessibility

/**
 * The Home chip on the missed ("אם לא עניתי") moment: does it send by itself, or wait for approval?
 *
 * §2 honesty point — automatic WhatsApp send needs BOTH the user's AUTOMATIC choice AND Android's
 * Accessibility service actually enabled. If the user picked automatic but never granted Accessibility,
 * the app CANNOT send on its own, so the chip must NOT claim "אוטומטי" — it reads "ידני" until both
 * are true. Kept pure (no Android) so it can be asserted in a test.
 *
 * Ended + no-answer never auto-send, so their card always uses [MANUAL] directly (not this resolver).
 */
object MissedSendModeLabel {
    const val AUTOMATIC = "נשלח אוטומטי"
    const val MANUAL = "ידני"

    /** "נשלח אוטומטי" only when the moment is automatic AND Accessibility is enabled; else "ידני". */
    fun of(approvalIsAutomatic: Boolean, accessibilityEnabled: Boolean): String =
        if (approvalIsAutomatic && accessibilityEnabled) AUTOMATIC else MANUAL
}
