package com.followupnadlan.postcall

import com.followupnadlan.accessibility.RecipientScope
import com.followupnadlan.missedcall.FollowUpConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class EndedSuggestionDecisionTest {
    private val now = 1_000_000L

    private fun input(
        callDurationSeconds: Long = 120,
        phoneNumber: String? = "0521234567",
        isSavedContact: Boolean = false,
        contactsPermissionGranted: Boolean = true,
        scope: RecipientScope = RecipientScope.NON_CONTACTS_ONLY,
        allowedNumbers: List<String> = emptyList(),
        lastSuggestedAtEpochMs: Long? = null,
        lastAnyNotificationAtEpochMs: Long? = null,
        sameNumberCooldownMillis: Long? = FollowUpConstants.SAME_NUMBER_COOLDOWN_MILLIS,
        globalQuietMillis: Long? = FollowUpConstants.GLOBAL_NOTIFICATION_QUIET_MILLIS,
        withinWorkingHours: Boolean = true
    ) = EndedSuggestionInput(
        callDurationSeconds = callDurationSeconds,
        phoneNumber = phoneNumber,
        isSavedContact = isSavedContact,
        contactsPermissionGranted = contactsPermissionGranted,
        scope = scope,
        allowedNumbers = allowedNumbers,
        lastSuggestedAtEpochMs = lastSuggestedAtEpochMs,
        lastAnyNotificationAtEpochMs = lastAnyNotificationAtEpochMs,
        sameNumberCooldownMillis = sameNumberCooldownMillis,
        globalQuietMillis = globalQuietMillis,
        nowEpochMs = now,
        withinWorkingHours = withinWorkingHours
    )

    @Test
    fun skipsOutsideWorkingHours() {
        assertEquals(
            EndedSuggestionDecision.SKIP_OUTSIDE_WORKING_HOURS,
            EndedSuggestionDecider.decide(input(withinWorkingHours = false))
        )
    }

    @Test
    fun suggestsAfterARealConversationWithAnUnsavedNumber() {
        assertEquals(EndedSuggestionDecision.SUGGEST, EndedSuggestionDecider.decide(input()))
    }

    /**
     * A call you could not take is the strongest follow-up case there is, so it suggests like any
     * other. The missed-call engine may also reply to it — that is a different message ("sorry I
     * missed you") on a different channel, not a duplicate of this one.
     */
    @Test
    fun suggestsAfterACallThatWasNeverAnswered() {
        assertEquals(
            EndedSuggestionDecision.SUGGEST,
            EndedSuggestionDecider.decide(input(callDurationSeconds = 0))
        )
    }

    /**
     * No length rule at all. "Perfect, send me the details" is a real conversation and takes
     * seconds; the suggestion is silent, so a threshold could only lose those to save a
     * notification that costs nothing to dismiss.
     */
    @Test
    fun suggestsAfterAnAnsweredCallOfAnyLength() {
        listOf(1L, 5L, 40L, 59L, 60L, 600L).forEach { seconds ->
            assertEquals(
                "expected suggest for ${seconds}s",
                EndedSuggestionDecision.SUGGEST,
                EndedSuggestionDecider.decide(input(callDurationSeconds = seconds))
            )
        }
    }

    @Test
    fun skipsNumbersNobodyCanBeReachedOn() {
        listOf("*6555", "1800123456", "unknown", "").forEach { phone ->
            assertEquals(
                "expected skip for $phone",
                EndedSuggestionDecision.SKIP_NO_NUMBER,
                EndedSuggestionDecider.decide(input(phoneNumber = phone))
            )
        }
    }

    @Test
    fun respectsTheNonContactsOnlyDefault() {
        assertEquals(
            EndedSuggestionDecision.SKIP_OUT_OF_SCOPE,
            EndedSuggestionDecider.decide(input(isSavedContact = true))
        )
    }

    @Test
    fun respectsContactsOnly() {
        assertEquals(
            EndedSuggestionDecision.SUGGEST,
            EndedSuggestionDecider.decide(input(scope = RecipientScope.CONTACTS_ONLY, isSavedContact = true))
        )
        assertEquals(
            EndedSuggestionDecision.SKIP_OUT_OF_SCOPE,
            EndedSuggestionDecider.decide(input(scope = RecipientScope.CONTACTS_ONLY, isSavedContact = false))
        )
    }

    @Test
    fun suggestsAfterEveryConversationWhenScopeIsAnyNumber() {
        assertEquals(
            EndedSuggestionDecision.SUGGEST,
            EndedSuggestionDecider.decide(input(scope = RecipientScope.ANY_NUMBER, isSavedContact = true))
        )
    }

    @Test
    fun respectsAHandPickedList() {
        val onlySelected = input(
            scope = RecipientScope.ONLY_SELECTED,
            allowedNumbers = listOf("0521234567")
        )
        assertEquals(EndedSuggestionDecision.SUGGEST, EndedSuggestionDecider.decide(onlySelected))
        assertEquals(
            EndedSuggestionDecision.SKIP_OUT_OF_SCOPE,
            EndedSuggestionDecider.decide(onlySelected.copy(phoneNumber = "0529999999"))
        )
    }

    // Without contacts permission "saved vs unsaved" is unknowable — staying quiet beats guessing.
    @Test
    fun staysQuietWhenTheScopeCannotBeEvaluated() {
        assertEquals(
            EndedSuggestionDecision.SKIP_CONTACT_TYPE_UNVERIFIED,
            EndedSuggestionDecider.decide(input(contactsPermissionGranted = false))
        )
    }

    @Test
    fun doesNotEvaluateContactTypeWhenTheScopeDoesNotNeedIt() {
        assertEquals(
            EndedSuggestionDecision.SUGGEST,
            EndedSuggestionDecider.decide(
                input(scope = RecipientScope.ANY_NUMBER, contactsPermissionGranted = false)
            )
        )
    }

    @Test
    fun suggestsOncePerNumberPerDay() {
        val justSuggested = input(lastSuggestedAtEpochMs = now - 23 * 60 * 60 * 1000L)
        assertEquals(EndedSuggestionDecision.SKIP_COOLDOWN, EndedSuggestionDecider.decide(justSuggested))

        val dayLater = input(lastSuggestedAtEpochMs = now - 24 * 60 * 60 * 1000L)
        assertEquals(EndedSuggestionDecision.SUGGEST, EndedSuggestionDecider.decide(dayLater))
    }

    // A clock change or restored backup must never *unblock* a repeat notification: an
    // unparseable "last suggested" is treated as still inside the cooldown, not as never.
    @Test
    fun aFutureTimestampStillBlocksARepeatSuggestion() {
        assertEquals(
            EndedSuggestionDecision.SKIP_COOLDOWN,
            EndedSuggestionDecider.decide(input(lastSuggestedAtEpochMs = now + 60 * 60 * 1000L))
        )
    }

    @Test
    fun keepsAGlobalQuietWindowBetweenNotifications() {
        val burst = input(lastAnyNotificationAtEpochMs = now - 30_000L)
        assertEquals(EndedSuggestionDecision.SKIP_QUIET_WINDOW, EndedSuggestionDecider.decide(burst))

        val settled = input(lastAnyNotificationAtEpochMs = now - 61_000L)
        assertEquals(EndedSuggestionDecision.SUGGEST, EndedSuggestionDecider.decide(settled))
    }

    // ===== both brakes are the user's to retime or switch off =====

    /** Switching the per-number cooldown off means the same person can be suggested twice running. */
    @Test
    fun sameNumberCooldownSwitchedOffSuggestsAgainImmediately() {
        val secondsAgo = input(
            lastSuggestedAtEpochMs = now - 1_000L,
            sameNumberCooldownMillis = null
        )

        assertEquals(EndedSuggestionDecision.SUGGEST, EndedSuggestionDecider.decide(secondsAgo))
    }

    /** Switching the quiet window off lets a burst of calls produce a burst of suggestions. */
    @Test
    fun globalQuietWindowSwitchedOffAllowsABurst() {
        val secondsAgo = input(
            lastAnyNotificationAtEpochMs = now - 1_000L,
            globalQuietMillis = null
        )

        assertEquals(EndedSuggestionDecision.SUGGEST, EndedSuggestionDecider.decide(secondsAgo))
    }

    /** With both off, nothing throttles: every eligible call suggests. */
    @Test
    fun bothBrakesOffSuggestsOnEveryCall() {
        val justSuggestedToThisVeryNumber = input(
            lastSuggestedAtEpochMs = now - 1_000L,
            lastAnyNotificationAtEpochMs = now - 1_000L,
            sameNumberCooldownMillis = null,
            globalQuietMillis = null
        )

        assertEquals(EndedSuggestionDecision.SUGGEST, EndedSuggestionDecider.decide(justSuggestedToThisVeryNumber))
    }

    /** A shortened cooldown is honored as written, not clamped to the old 24-hour default. */
    @Test
    fun aShortenedSameNumberCooldownIsHonored() {
        val oneHour = 60 * 60 * 1000L

        assertEquals(
            EndedSuggestionDecision.SKIP_COOLDOWN,
            EndedSuggestionDecider.decide(
                input(lastSuggestedAtEpochMs = now - 59 * 60 * 1000L, sameNumberCooldownMillis = oneHour)
            )
        )
        assertEquals(
            EndedSuggestionDecision.SUGGEST,
            EndedSuggestionDecider.decide(
                input(lastSuggestedAtEpochMs = now - oneHour, sameNumberCooldownMillis = oneHour)
            )
        )
    }

    /** A lengthened cooldown is honored too — a week means a week. */
    @Test
    fun aLengthenedSameNumberCooldownIsHonored() {
        val week = 7 * 24 * 60 * 60 * 1000L

        assertEquals(
            EndedSuggestionDecision.SKIP_COOLDOWN,
            EndedSuggestionDecider.decide(
                input(lastSuggestedAtEpochMs = now - 3 * 24 * 60 * 60 * 1000L, sameNumberCooldownMillis = week)
            )
        )
    }

    /**
     * The per-number cooldown is reported ahead of the quiet window when both apply, so the reason
     * the user would recognise ("I just spoke to them") is the one that surfaces.
     */
    @Test
    fun theSameNumberCooldownIsReportedAheadOfTheQuietWindow() {
        val bothApply = input(
            lastSuggestedAtEpochMs = now - 1_000L,
            lastAnyNotificationAtEpochMs = now - 1_000L
        )

        assertEquals(EndedSuggestionDecision.SKIP_COOLDOWN, EndedSuggestionDecider.decide(bothApply))
    }
}
