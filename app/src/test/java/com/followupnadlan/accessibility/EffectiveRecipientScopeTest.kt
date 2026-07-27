package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The override-on-default resolver: a per-moment override wins; a `null` per-moment value (the
 * "כמו הכללי" state) falls back to the general default. This is the single place that decision
 * lives, so the deciders can stay untouched.
 */
class EffectiveRecipientScopeTest {

    @Test
    fun nullPerMomentFollowsGeneral() {
        assertEquals(
            RecipientScope.NON_CONTACTS_ONLY,
            EffectiveRecipientScope.of(perMoment = null, general = RecipientScope.NON_CONTACTS_ONLY)
        )
        assertEquals(
            RecipientScope.ANY_NUMBER,
            EffectiveRecipientScope.of(perMoment = null, general = RecipientScope.ANY_NUMBER)
        )
    }

    @Test
    fun concretePerMomentOverridesGeneral() {
        assertEquals(
            RecipientScope.CONTACTS_ONLY,
            EffectiveRecipientScope.of(
                perMoment = RecipientScope.CONTACTS_ONLY,
                general = RecipientScope.NON_CONTACTS_ONLY
            )
        )
    }

    @Test
    fun overrideWinsEvenWhenItEqualsGeneral() {
        // A moment pinned to the same value as the general is still an override, not "follow".
        assertEquals(
            RecipientScope.ANY_NUMBER,
            EffectiveRecipientScope.of(
                perMoment = RecipientScope.ANY_NUMBER,
                general = RecipientScope.ANY_NUMBER
            )
        )
    }

    @Test
    fun everyScopeCanOverride() {
        RecipientScope.entries.forEach { scope ->
            assertEquals(
                scope,
                EffectiveRecipientScope.of(perMoment = scope, general = RecipientScope.NON_CONTACTS_ONLY)
            )
        }
    }
}
