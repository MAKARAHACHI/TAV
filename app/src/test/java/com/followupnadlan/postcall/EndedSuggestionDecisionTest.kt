package com.followupnadlan.postcall

import com.followupnadlan.accessibility.RecipientScope
import org.junit.Assert.assertEquals
import org.junit.Test

class EndedSuggestionDecisionTest {
    private val now = 1_000_000L

    private fun input(
        wasAnswered: Boolean = true,
        callDurationSeconds: Long = 120,
        phoneNumber: String? = "0521234567",
        isSavedContact: Boolean = false,
        contactsPermissionGranted: Boolean = true,
        scope: RecipientScope = RecipientScope.NON_CONTACTS_ONLY,
        allowedNumbers: List<String> = emptyList(),
        lastSuggestedAtEpochMs: Long? = null,
        lastAnyNotificationAtEpochMs: Long? = null
    ) = EndedSuggestionInput(
        wasAnswered = wasAnswered,
        callDurationSeconds = callDurationSeconds,
        phoneNumber = phoneNumber,
        isSavedContact = isSavedContact,
        contactsPermissionGranted = contactsPermissionGranted,
        scope = scope,
        allowedNumbers = allowedNumbers,
        lastSuggestedAtEpochMs = lastSuggestedAtEpochMs,
        lastAnyNotificationAtEpochMs = lastAnyNotificationAtEpochMs,
        nowEpochMs = now
    )

    @Test
    fun suggestsAfterARealConversationWithAnUnsavedNumber() {
        assertEquals(EndedSuggestionDecision.SUGGEST, EndedSuggestionDecider.decide(input()))
    }

    @Test
    fun skipsCallsThatWereNeverAnswered() {
        assertEquals(
            EndedSuggestionDecision.SKIP_NOT_ANSWERED,
            EndedSuggestionDecider.decide(input(wasAnswered = false))
        )
    }

    // 60s threshold: wrong numbers and "sorry, driving" are not follow-up material.
    @Test
    fun skipsConversationsShorterThanTheThreshold() {
        assertEquals(
            EndedSuggestionDecision.SKIP_TOO_SHORT,
            EndedSuggestionDecider.decide(input(callDurationSeconds = 59))
        )
        assertEquals(
            EndedSuggestionDecision.SUGGEST,
            EndedSuggestionDecider.decide(input(callDurationSeconds = 60))
        )
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
}
