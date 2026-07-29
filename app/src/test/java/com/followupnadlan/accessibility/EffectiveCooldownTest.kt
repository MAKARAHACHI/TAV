package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The override-on-default resolver for a frequency brake. INHERIT follows the general value (which
 * may itself be null = general off); OFF removes the brake regardless of general; a Value pins its
 * own interval. This is the single place the model is decided, so both send paths stay untouched.
 * Exercised for BOTH brakes by using the two brakes' real defaults as the "general" input.
 */
class EffectiveCooldownTest {

    private val sameNumberGeneral = FollowUpCooldownSettings.DEFAULT_SAME_NUMBER_MILLIS // 24h
    private val globalQuietGeneral = FollowUpCooldownSettings.DEFAULT_GLOBAL_QUIET_MILLIS // 1min

    @Test
    fun inheritFollowsGeneral_sameNumber() {
        assertEquals(
            sameNumberGeneral,
            EffectiveCooldown.resolve(CooldownChoice.Inherit, sameNumberGeneral)
        )
    }

    @Test
    fun inheritFollowsGeneral_globalQuiet() {
        assertEquals(
            globalQuietGeneral,
            EffectiveCooldown.resolve(CooldownChoice.Inherit, globalQuietGeneral)
        )
    }

    @Test
    fun inheritFollowsGeneralEvenWhenGeneralIsOff() {
        // General itself switched off ⇒ inherit resolves to null (no brake), not a default.
        assertNull(EffectiveCooldown.resolve(CooldownChoice.Inherit, null))
    }

    @Test
    fun offResolvesToNull_regardlessOfGeneral_bothBrakes() {
        assertNull(EffectiveCooldown.resolve(CooldownChoice.Off, sameNumberGeneral))
        assertNull(EffectiveCooldown.resolve(CooldownChoice.Off, globalQuietGeneral))
        assertNull(EffectiveCooldown.resolve(CooldownChoice.Off, null))
    }

    @Test
    fun valueResolvesToItself_regardlessOfGeneral_bothBrakes() {
        val hour = 60 * 60 * 1000L
        assertEquals(hour, EffectiveCooldown.resolve(CooldownChoice.Value(hour), sameNumberGeneral))
        assertEquals(30_000L, EffectiveCooldown.resolve(CooldownChoice.Value(30_000L), globalQuietGeneral))
        // A value overrides even when general is off.
        assertEquals(hour, EffectiveCooldown.resolve(CooldownChoice.Value(hour), null))
    }

    @Test
    fun inheritAndOffAreDistinct() {
        // The whole point of the 3-state: with a non-null general, inherit != off.
        val inherit = EffectiveCooldown.resolve(CooldownChoice.Inherit, sameNumberGeneral)
        val off = EffectiveCooldown.resolve(CooldownChoice.Off, sameNumberGeneral)
        assertEquals(sameNumberGeneral, inherit)
        assertNull(off)
    }
}
