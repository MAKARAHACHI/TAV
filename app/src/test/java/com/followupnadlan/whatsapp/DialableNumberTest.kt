package com.followupnadlan.whatsapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialableNumberTest {
    @Test
    fun acceptsOrdinaryMobileNumbers() {
        assertTrue(DialableNumber.isDialable("0521234567"))
        assertTrue(DialableNumber.isDialable("+972521234567"))
        assertTrue(DialableNumber.isDialable("052-123-4567"))
    }

    @Test
    fun rejectsWithheldAndUnknownCallers() {
        assertFalse(DialableNumber.isDialable("unknown"))
        assertFalse(DialableNumber.isDialable("Private"))
        assertFalse(DialableNumber.isDialable("WITHHELD"))
    }

    @Test
    fun rejectsBlankAndNull() {
        assertFalse(DialableNumber.isDialable(null))
        assertFalse(DialableNumber.isDialable(""))
        assertFalse(DialableNumber.isDialable("   "))
    }

    // wa.me does not resolve for service codes — offering to message them promises nothing.
    @Test
    fun rejectsStarAndHashServiceCodes() {
        assertFalse(DialableNumber.isDialable("*6555"))
        assertFalse(DialableNumber.isDialable("#123"))
        assertFalse(DialableNumber.isDialable("*9"))
    }

    @Test
    fun rejectsEmergencyAndShortCodes() {
        assertFalse(DialableNumber.isDialable("100"))
        assertFalse(DialableNumber.isDialable("112"))
        assertFalse(DialableNumber.isDialable("911"))
        assertFalse(DialableNumber.isDialable("1234"))
    }

    @Test
    fun rejectsTollFreeBusinessLines() {
        assertFalse(DialableNumber.isDialable("1800123456"))
        assertFalse(DialableNumber.isDialable("1-800-123-456"))
        assertFalse(DialableNumber.isDialable("1700700700"))
        assertFalse(DialableNumber.isDialable("8005551234"))
    }

    @Test
    fun rejectsTollFreeEvenWithIsraeliCountryCode() {
        assertFalse(DialableNumber.isDialable("+9721800123456"))
    }

    @Test
    fun acceptsInternationalSubscriberNumbers() {
        assertTrue(DialableNumber.isDialable("+14155551234"))
        assertTrue(DialableNumber.isDialable("+442071234567"))
    }
}
