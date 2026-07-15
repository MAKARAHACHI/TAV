package com.followupnadlan.whatsapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberNormalizerTest {
    @Test
    fun normalizesIsraeliLocalMobile() {
        assertEquals("972501234567", PhoneNumberNormalizer.normalizeForWhatsApp("050-1234567"))
    }

    @Test
    fun normalizesPlusInternationalNumber() {
        assertEquals("972501234567", PhoneNumberNormalizer.normalizeForWhatsApp("+972501234567"))
    }

    @Test
    fun keepsInternationalNumberWithoutPlus() {
        assertEquals("972501234567", PhoneNumberNormalizer.normalizeForWhatsApp("972501234567"))
    }

    @Test
    fun rejectsLetters() {
        assertNull(PhoneNumberNormalizer.normalizeForWhatsApp("050-abc-4567"))
    }

    @Test
    fun localDisplayConvertsBareIsraeliPrefixToZero() {
        assertEquals("0501234567", PhoneNumberNormalizer.toLocalIsraeliDisplay("972501234567"))
    }

    @Test
    fun localDisplayConvertsPlusIsraeliPrefixToZero() {
        assertEquals("0501234567", PhoneNumberNormalizer.toLocalIsraeliDisplay("+972-50-123-4567"))
    }

    @Test
    fun localDisplayKeepsAlreadyLocalNumber() {
        assertEquals("0501234567", PhoneNumberNormalizer.toLocalIsraeliDisplay("050-123-4567"))
    }

    @Test
    fun localDisplayLeavesOtherInternationalNumberAsIs() {
        assertEquals("15551234567", PhoneNumberNormalizer.toLocalIsraeliDisplay("+1 555 123 4567"))
    }
}
