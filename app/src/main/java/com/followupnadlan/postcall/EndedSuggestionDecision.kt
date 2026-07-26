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
 * Applies to outgoing calls too: the call you returned is exactly the one that deserves a follow-up.
 *
 * Pure logic, no Android — see EndedSuggestionDecisionTest.
 */
data class EndedSuggestionInput(
    val wasAnswered: Boolean,
    val callDurationSeconds: Long,
    val phoneNumber: String?,
    val isSavedContact: Boolean,
    val contactsPermissionGranted: Boolean,
    val scope: RecipientScope,
    val allowedNumbers: List<String> = emptyList(),
    val lastSuggestedAtEpochMs: Long? = null,
    val lastAnyNotificationAtEpochMs: Long? = null,
    val nowEpochMs: Long
)

enum class EndedSuggestionDecision {
    SUGGEST,
    SKIP_NOT_ANSWERED,
    SKIP_TOO_SHORT,
    SKIP_NO_NUMBER,
    SKIP_OUT_OF_SCOPE,
    SKIP_CONTACT_TYPE_UNVERIFIED,
    SKIP_COOLDOWN,
    SKIP_QUIET_WINDOW
}

object EndedSuggestionDecider {
    fun decide(input: EndedSuggestionInput): EndedSuggestionDecision {
        if (!input.wasAnswered) return EndedSuggestionDecision.SKIP_NOT_ANSWERED
        if (input.callDurationSeconds < FollowUpConstants.ENDED_SUGGESTION_MIN_CALL_SECONDS) {
            return EndedSuggestionDecision.SKIP_TOO_SHORT
        }

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

        if (withinWindow(input.lastSuggestedAtEpochMs, input.nowEpochMs, FollowUpConstants.SAME_NUMBER_COOLDOWN_MILLIS)) {
            return EndedSuggestionDecision.SKIP_COOLDOWN
        }
        if (withinWindow(
                input.lastAnyNotificationAtEpochMs,
                input.nowEpochMs,
                FollowUpConstants.GLOBAL_NOTIFICATION_QUIET_MILLIS
            )
        ) {
            return EndedSuggestionDecision.SKIP_QUIET_WINDOW
        }

        return EndedSuggestionDecision.SUGGEST
    }

    /**
     * Whether [lastAtEpochMs] falls inside the window ending now. Only a missing timestamp counts
     * as "never happened" — a timestamp in the future (clock change, restored backup) is treated
     * as still inside the window, so odd clock state can never *unblock* a repeat notification.
     */
    private fun withinWindow(lastAtEpochMs: Long?, nowEpochMs: Long, windowMillis: Long): Boolean {
        val last = lastAtEpochMs ?: return false
        val elapsed = nowEpochMs - last
        return elapsed < windowMillis
    }
}
