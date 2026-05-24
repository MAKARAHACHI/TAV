package com.followupnadlan.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OemGuidanceTest {
    @Test
    fun samsungMapsToSamsungGuidance() {
        val guidance = OemGuidance.forManufacturer("SAMSUNG")

        assertEquals("samsung", guidance.key)
        assertTrue(guidance.body.contains("חיסכון בסוללה"))
        assertNonEmptyHebrew(guidance)
    }

    @Test
    fun xiaomiMapsToXiaomiGuidance() {
        val guidance = OemGuidance.forManufacturer("xiaomi")

        assertEquals("xiaomi", guidance.key)
        assertTrue(guidance.body.contains("Autostart"))
        assertNonEmptyHebrew(guidance)
    }

    @Test
    fun redmiMapsToRedmiGuidance() {
        val guidance = OemGuidance.forManufacturer("Redmi")

        assertEquals("redmi", guidance.key)
        assertTrue(guidance.body.contains("Xiaomi/Redmi"))
        assertNonEmptyHebrew(guidance)
    }

    @Test
    fun realmeMapsToRealmeGuidance() {
        val guidance = OemGuidance.forManufacturer("realme")

        assertEquals("realme", guidance.key)
        assertTrue(guidance.body.contains("Realme"))
        assertNonEmptyHebrew(guidance)
    }

    @Test
    fun onePlusMapsToOnePlusGuidance() {
        val guidance = OemGuidance.forManufacturer("OnePlus")

        assertEquals("oneplus", guidance.key)
        assertTrue(guidance.body.contains("OnePlus"))
        assertNonEmptyHebrew(guidance)
    }

    @Test
    fun unknownMapsToGenericFallback() {
        val guidance = OemGuidance.forManufacturer("unknown")

        assertEquals("generic", guidance.key)
        assertTrue(guidance.body.contains("לפעול ברקע"))
        assertNonEmptyHebrew(guidance)
    }

    @Test
    fun knownGuidanceStringsAreDistinctWhereExpected() {
        assertNotEquals(OemGuidance.samsung.body, OemGuidance.xiaomi.body)
        assertNotEquals(OemGuidance.samsung.body, OemGuidance.realme.body)
        assertNotEquals(OemGuidance.xiaomi.body, OemGuidance.generic.body)
        assertNotEquals(OemGuidance.onePlus.body, OemGuidance.generic.body)
    }

    private fun assertNonEmptyHebrew(guidance: OemGuidanceBlock) {
        assertTrue(guidance.title.isNotBlank())
        assertTrue(guidance.body.isNotBlank())
        assertTrue(guidance.body.any { it in '\u0590'..'\u05FF' })
    }
}
