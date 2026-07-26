package com.followupnadlan.accessibility

import com.followupnadlan.missedcall.FollowUpConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The option lists behind the two ⚙️ pickers.
 *
 * The store itself needs a Context, so what is asserted here is the part that can be wrong without
 * anyone noticing: that "off" is actually offered, that the shipped defaults are selectable (a
 * default absent from its own list would render with nothing selected), and that an unrecognised
 * stored value still gets a truthful label instead of silently reading as something else.
 */
class FollowUpCooldownOptionsTest {

    @Test
    fun bothPickersOfferSwitchingTheBrakeOff() {
        assertTrue(
            "per-number cooldown must be switchable off",
            FollowUpCooldownOptions.sameNumber.any { it.millis == null }
        )
        assertTrue(
            "quiet window must be switchable off",
            FollowUpCooldownOptions.globalQuiet.any { it.millis == null }
        )
    }

    /** A default that is not in its own list would render as "nothing selected". */
    @Test
    fun theShippedDefaultsAreSelectableInTheirLists() {
        assertTrue(
            FollowUpCooldownOptions.sameNumber.any {
                it.millis == FollowUpCooldownSettings.DEFAULT_SAME_NUMBER_MILLIS
            }
        )
        assertTrue(
            FollowUpCooldownOptions.globalQuiet.any {
                it.millis == FollowUpCooldownSettings.DEFAULT_GLOBAL_QUIET_MILLIS
            }
        )
    }

    /** The defaults are the constants the engine falls back to, so the two cannot drift apart. */
    @Test
    fun theDefaultsMatchTheEngineConstants() {
        assertEquals(
            FollowUpConstants.SAME_NUMBER_COOLDOWN_MILLIS,
            FollowUpCooldownSettings.DEFAULT_SAME_NUMBER_MILLIS
        )
        assertEquals(
            FollowUpConstants.GLOBAL_NOTIFICATION_QUIET_MILLIS,
            FollowUpCooldownSettings.DEFAULT_GLOBAL_QUIET_MILLIS
        )
    }

    @Test
    fun labelsAKnownValueFromItsOwnList() {
        assertEquals(
            "24 שעות",
            FollowUpCooldownOptions.labelFor(
                FollowUpCooldownOptions.sameNumber,
                24 * 60 * 60 * 1000L
            )
        )
    }

    @Test
    fun labelsSwitchedOffAsTheOffChoice() {
        assertEquals(
            FollowUpCooldownOptions.sameNumber.first().label,
            FollowUpCooldownOptions.labelFor(FollowUpCooldownOptions.sameNumber, null)
        )
    }

    /**
     * An older build's constant or a restored backup can hold a value no longer on the list. It must
     * describe itself truthfully rather than borrow the nearest label.
     */
    @Test
    fun describesAnUnrecognisedValueRatherThanMislabellingIt() {
        assertEquals(
            "2 שעות",
            FollowUpCooldownOptions.labelFor(FollowUpCooldownOptions.sameNumber, 2 * 60 * 60 * 1000L)
        )
        assertEquals(
            "45 שניות",
            FollowUpCooldownOptions.labelFor(FollowUpCooldownOptions.globalQuiet, 45 * 1000L)
        )
    }
}
