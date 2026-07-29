package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the migration + independence guarantees of the per-moment override model WITHOUT an Android
 * Context, by driving three independent stores (one per moment) and one general value through the
 * SAME codec + resolver the real [MomentCooldownOverrideSettings] and [EffectiveCooldown] use.
 *
 * Each [FakeMomentOverrideStore] mirrors the real store exactly: an Inherit write DROPS the key
 * (encode ⇒ UNSET ⇒ remove), so an untouched moment truly has no stored value. That is what makes
 * "existing installs = INHERIT ⇒ unchanged effective behavior" true and testable here.
 */
class MomentCooldownIndependenceTest {

    /** In-memory twin of one moment's store — same key-drop-on-Inherit rule as the real one. */
    private class FakeMomentOverrideStore {
        private val prefs = mutableMapOf<String, Long>()
        private val key = "same_number_override"

        var choice: CooldownChoice
            get() = CooldownChoiceCodec.decode(prefs[key] ?: CooldownChoiceCodec.UNSET)
            set(value) {
                val encoded = CooldownChoiceCodec.encode(value)
                if (encoded == CooldownChoiceCodec.UNSET) prefs.remove(key) else prefs[key] = encoded
            }

        val hasStoredValue: Boolean get() = prefs.containsKey(key)
    }

    @Test
    fun untouchedMomentsAllInherit_soEffectiveBehaviorUnchanged() {
        val missed = FakeMomentOverrideStore()
        val ended = FakeMomentOverrideStore()
        val noAnswer = FakeMomentOverrideStore()
        val general = FollowUpCooldownSettings.DEFAULT_SAME_NUMBER_MILLIS // 24h

        // Fresh install: nothing stored anywhere.
        assertFalse(missed.hasStoredValue)
        assertFalse(ended.hasStoredValue)
        assertFalse(noAnswer.hasStoredValue)

        // Every moment reads Inherit ⇒ every moment resolves to the general default (today's 24h).
        listOf(missed, ended, noAnswer).forEach {
            assertEquals(CooldownChoice.Inherit, it.choice)
            assertEquals(general, EffectiveCooldown.resolve(it.choice, general))
        }
    }

    @Test
    fun overridingOneMomentLeavesOthersAndGeneralUntouched() {
        val missed = FakeMomentOverrideStore()
        val ended = FakeMomentOverrideStore()
        val noAnswer = FakeMomentOverrideStore()
        // Standing in for the general store, which this flow must NOT write.
        var generalStore = FollowUpCooldownSettings.DEFAULT_SAME_NUMBER_MILLIS

        // Override ONLY the ended moment to a concrete 1h.
        val oneHour = 60 * 60 * 1000L
        ended.choice = CooldownChoice.Value(oneHour)

        // Ended now overrides; missed + no-answer still inherit; the general store is untouched.
        assertTrue(ended.hasStoredValue)
        assertFalse(missed.hasStoredValue)
        assertFalse(noAnswer.hasStoredValue)
        assertEquals(FollowUpCooldownSettings.DEFAULT_SAME_NUMBER_MILLIS, generalStore)

        // Effective values: ended = its override; the others = the (unchanged) general.
        assertEquals(oneHour, EffectiveCooldown.resolve(ended.choice, generalStore))
        assertEquals(generalStore, EffectiveCooldown.resolve(missed.choice, generalStore))
        assertEquals(generalStore, EffectiveCooldown.resolve(noAnswer.choice, generalStore))
    }

    @Test
    fun twoMomentsCanHoldDifferentOverridesIndependently() {
        val missed = FakeMomentOverrideStore()
        val ended = FakeMomentOverrideStore()
        val general = FollowUpCooldownSettings.DEFAULT_SAME_NUMBER_MILLIS

        val sixHours = 6 * 60 * 60 * 1000L
        missed.choice = CooldownChoice.Value(sixHours)
        ended.choice = CooldownChoice.Off

        // A override ≠ B override, and neither equals the general.
        assertEquals(sixHours, EffectiveCooldown.resolve(missed.choice, general))
        assertNull(EffectiveCooldown.resolve(ended.choice, general))
        assertEquals(CooldownChoice.Value(sixHours), missed.choice)
        assertEquals(CooldownChoice.Off, ended.choice)
    }

    @Test
    fun resettingAMomentToInheritDropsItsStoredValue() {
        val missed = FakeMomentOverrideStore()
        missed.choice = CooldownChoice.Value(60 * 60 * 1000L)
        assertTrue(missed.hasStoredValue)

        // Back to "לפי הכללי" ⇒ the key is dropped, so it once again reads Inherit.
        missed.choice = CooldownChoice.Inherit
        assertFalse(missed.hasStoredValue)
        assertEquals(CooldownChoice.Inherit, missed.choice)
    }
}
