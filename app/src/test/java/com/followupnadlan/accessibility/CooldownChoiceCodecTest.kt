package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The pure codec the per-moment override store delegates to. The 3-state [CooldownChoice] maps to
 * distinct stored longs so INHERIT and OFF never collapse:
 *   - INHERIT ↔ absent / UNSET (the store drops the key)
 *   - OFF ↔ the OFF sentinel
 *   - Value ↔ a positive long
 * Pins the sentinel handling without an Android Context.
 */
class CooldownChoiceCodecTest {

    @Test
    fun inheritEncodesToUnset() {
        assertEquals(CooldownChoiceCodec.UNSET, CooldownChoiceCodec.encode(CooldownChoice.Inherit))
    }

    @Test
    fun absentStorageDecodesToInherit() {
        // The store passes UNSET when the key is absent ⇒ follow general.
        assertEquals(CooldownChoice.Inherit, CooldownChoiceCodec.decode(CooldownChoiceCodec.UNSET))
    }

    @Test
    fun offRoundTripsThroughSentinel() {
        val encoded = CooldownChoiceCodec.encode(CooldownChoice.Off)
        assertEquals(CooldownChoiceCodec.OFF_SENTINEL, encoded)
        assertEquals(CooldownChoice.Off, CooldownChoiceCodec.decode(encoded))
    }

    @Test
    fun valueRoundTripsThroughLong() {
        val millis = 6 * 60 * 60 * 1000L
        val encoded = CooldownChoiceCodec.encode(CooldownChoice.Value(millis))
        assertEquals(millis, encoded)
        assertEquals(CooldownChoice.Value(millis), CooldownChoiceCodec.decode(encoded))
    }

    @Test
    fun inheritAndOffEncodeToDistinctSentinels() {
        // The core guarantee: the two states are not the same stored value.
        assertEquals(true, CooldownChoiceCodec.encode(CooldownChoice.Inherit) != CooldownChoiceCodec.encode(CooldownChoice.Off))
    }

    @Test
    fun garbageNonPositiveDecodesToInherit_notOff() {
        // A stale/garbage non-positive value (that isn't the OFF sentinel) degrades to Inherit,
        // never silently turning a brake off.
        assertEquals(CooldownChoice.Inherit, CooldownChoiceCodec.decode(-999L))
        assertEquals(CooldownChoice.Inherit, CooldownChoiceCodec.decode(0L))
    }

    @Test
    fun zeroValueEncodesToUnsetInherit() {
        // A Value(0) is not a real interval; treat it as Inherit rather than a broken brake.
        assertEquals(CooldownChoiceCodec.UNSET, CooldownChoiceCodec.encode(CooldownChoice.Value(0L)))
    }
}
