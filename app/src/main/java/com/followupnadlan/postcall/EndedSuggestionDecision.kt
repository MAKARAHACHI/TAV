package com.followupnadlan.postcall

import com.followupnadlan.accessibility.RecipientScope
import com.followupnadlan.missedcall.AllowedRecipientDecisionMatcher
import com.followupnadlan.missedcall.FollowUpConstants
import com.followupnadlan.whatsapp.DialableNumber

/**
 * Decides whether a finished conversation earns a follow-up suggestion.
 *
 * This gates a *suggestion only* — nothing is ever sent automatically after a conversation. The
 * cost of a wrong "yes" is therefore a notification the user ignores, which is why the rules stay
 * simple: perfect filtering is impossible (a lead and opposing counsel look identical to a phone),
 * so the design makes the mistake cheap rather than making the filter clever.
 *
 * Neither length nor outcome is one of the rules. Every call ending gets a suggestion — answered or
 * not, incoming or outgoing, one second or one hour:
 *
 *  - **Length**: a short call is not reliably a worthless one. "Perfect, send me the details" takes
 *    fifteen seconds.
 *  - **Outgoing**: the call you returned is exactly the one that deserves a follow-up.
 *  - **Unanswered**: a call you could not take is the strongest follow-up case there is. The
 *    missed-call engine may also reply to it, but that is a *different* message on a different
 *    channel — [com.followupnadlan.missedcall.MissedCallAutoResponseHandler] sends the "sorry I
 *    missed you" text, while this offers the user's details. The cooldowns below keep the second
 *    one from repeating.
 *
 * The suggestion is silent and dismissible, so a wrong "yes" costs a glance. Every filter removed
 * here was capable of losing a real conversation to save a notification that costs nothing.
 *
 * What remains are the two *timing* brakes, and both are now the user's to set or switch off from
 * ⚙️ — see [com.followupnadlan.accessibility.FollowUpCooldownSettings]. A `null` window means the
 * user turned that brake off, which is honored exactly as written.
 *
 * Pure logic, no Android — see EndedSuggestionDecisionTest.
 */
data class EndedSuggestionInput(
    val callDurationSeconds: Long,
    val phoneNumber: String?,
    val isSavedContact: Boolean,
    val contactsPermissionGranted: Boolean,
    val scope: RecipientScope,
    val allowedNumbers: List<String> = emptyList(),
    val lastSuggestedAtEpochMs: Long? = null,
    val lastAnyNotificationAtEpochMs: Long? = null,
    /** Per-number cooldown; `null` when the user switched it off. */
    val sameNumberCooldownMillis: Long? = FollowUpConstants.SAME_NUMBER_COOLDOWN_MILLIS,
    /** Global anti-burst window; `null` when the user switched it off. */
    val globalQuietMillis: Long? = FollowUpConstants.GLOBAL_NOTIFICATION_QUIET_MILLIS,
    val nowEpochMs: Long,
    val withinWorkingHours: Boolean = true
)

enum class EndedSuggestionDecision {
    SUGGEST,
    SKIP_NO_NUMBER,
    SKIP_OUT_OF_SCOPE,
    SKIP_CONTACT_TYPE_UNVERIFIED,
    SKIP_COOLDOWN,
    SKIP_QUIET_WINDOW,
    SKIP_OUTSIDE_WORKING_HOURS
}

object EndedSuggestionDecider {
    fun decide(input: EndedSuggestionInput): EndedSuggestionDecision {
        val phone = input.phoneNumber.orEmpty().trim()
        if (!DialableNumber.isDialable(phone)) return EndedSuggestionDecision.SKIP_NO_NUMBER

        // Without contacts permission we cannot tell saved from unsaved, so a scope that depends
        // on that distinction cannot be honored — we stay quiet rather than guess (§2).
        val needsContactType = input.scope == RecipientScope.CONTACTS_ONLY ||
            input.scope == RecipientScope.NON_CONTACTS_ONLY
        if (needsContactType && !input.contactsPermissionGranted) {
            return EndedSuggestionDecision.SKIP_CONTACT_TYPE_UNVERIFIED
        }

        val inScope = when (input.scope) {
            RecipientScope.ANY_NUMBER -> true
            RecipientScope.CONTACTS_ONLY -> input.isSavedContact
            RecipientScope.NON_CONTACTS_ONLY -> !input.isSavedContact
            RecipientScope.ONLY_SELECTED ->
                AllowedRecipientDecisionMatcher.isAllowed(input.allowedNumbers, phone)
        }
        if (!inScope) return EndedSuggestionDecision.SKIP_OUT_OF_SCOPE

        if (!input.withinWorkingHours) return EndedSuggestionDecision.SKIP_OUTSIDE_WORKING_HOURS

        // Don't ask about the same person twice in a row.
        if (withinWindow(input.lastSuggestedAtEpochMs, input.nowEpochMs, input.sameNumberCooldownMillis)) {
            return EndedSuggestionDecision.SKIP_COOLDOWN
        }
        // Don't let a burst of calls become a burst of notifications. Checked second so a repeat of
        // the same number reports the reason the user would recognise.
        if (withinWindow(input.lastAnyNotificationAtEpochMs, input.nowEpochMs, input.globalQuietMillis)) {
            return EndedSuggestionDecision.SKIP_QUIET_WINDOW
        }

        return EndedSuggestionDecision.SUGGEST
    }

    /**
     * Whether [lastAtEpochMs] falls inside the window ending now.
     *
     * A `null` [windowMillis] means the user switched this brake off, so nothing is ever inside it.
     * Only a missing timestamp counts as "never happened" — a timestamp in the future (clock change,
     * restored backup) is treated as still inside the window, so odd clock state can never
     * *unblock* a repeat notification.
     */
    private fun withinWindow(lastAtEpochMs: Long?, nowEpochMs: Long, windowMillis: Long?): Boolean {
        val window = windowMillis ?: return false
        val last = lastAtEpochMs ?: return false
        val elapsed = nowEpochMs - last
        return elapsed < window
    }
}
